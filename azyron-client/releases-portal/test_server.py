"""Portal tests use synthetic metadata-only fixtures; never release candidates.

The fixture JARs are not compiled Minecraft mods and do not validate Fabric,
game launch, Mixin application, gameplay, or performance.
"""
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import threading
import unittest
from urllib.error import HTTPError
from urllib.request import Request, urlopen
import zipfile

import server

ROOT = Path(__file__).resolve().parents[1]
VERSION = "1.0.4"
TARGETS = ("1.20.1", "1.21.11")


def load_module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


builder = load_module("portal_test_pack_builder", ROOT / "scripts/build-modpack.py")
bundles = load_module("portal_test_bundles", ROOT / "scripts/prepare-native-candidates.py")


class PortalTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = tempfile.TemporaryDirectory()
        cls.addClassCleanup(cls.tmp.cleanup)
        cls.root = Path(cls.tmp.name)
        cls.releases = cls.root / "releases"
        cls.releases.mkdir()
        cls.original_releases_dir = server.RELEASES_DIR

        jars, packs = {}, {}
        for mc in TARGETS:
            java = ">=17" if mc == "1.20.1" else ">=21"
            jar = cls.root / f"Azyron-Client-{VERSION}-mc{mc}.jar"
            metadata = {
                "id": "azyron-client",
                "version": VERSION,
                "name": f"AZYRON CLIENT v{VERSION}",
                "authors": ["JIGNESH GAMING"],
                "depends": {"minecraft": [mc], "java": java},
            }
            with zipfile.ZipFile(jar, "w", compression=zipfile.ZIP_DEFLATED) as archive:
                archive.writestr("fabric.mod.json", json.dumps(metadata))
                archive.writestr("fixture.txt", "Synthetic metadata fixture; not a compiled Fabric mod.\n")
            jars[mc] = jar
            pack = cls.root / f"Azyron-Modpack-{VERSION}-mc{mc}.mrpack"
            builder.build_mrpack(str(jar), str(pack), mc)
            packs[mc] = pack

        bundles.assemble_download_bundles(jars, packs, cls.releases, run_id="fixture", commit="fixture")
        server.RELEASES_DIR = str(cls.releases)
        cls.httpd = server.ThreadingHTTPServer(("127.0.0.1", 0), server.GitHubReleasesHandler)
        cls.thread = threading.Thread(target=cls.httpd.serve_forever, daemon=True)
        cls.thread.start()
        cls.base = f"http://127.0.0.1:{cls.httpd.server_port}"

    @classmethod
    def tearDownClass(cls):
        cls.httpd.shutdown()
        cls.httpd.server_close()
        cls.thread.join()
        server.RELEASES_DIR = cls.original_releases_dir

    def test_pages_offer_exactly_two_bundle_download_links(self):
        expected = {f"Azyron-Client-v{VERSION}.zip", f"Azyron-Modpack-v{VERSION}.zip"}
        for path in ("/", "/releases", f"/releases/tag/v{VERSION}"):
            with self.subTest(path=path), urlopen(self.base + path) as response:
                html = response.read().decode()
                self.assertIn(f"AZYRON CLIENT v{VERSION}", html)
                self.assertIn("JIGNESH GAMING", html)
                self.assertIn("Draft Pre-release", html)
                self.assertIn("neither is a cross-version JAR or profile", html)
                self.assertNotIn("data-mc-tab=", html)
                download_links = [line for line in html.splitlines() if 'href="/releases/download/v1.0.4/' in line]
                self.assertEqual(len(download_links), 2)
                self.assertEqual({name for name in expected if name in "\n".join(download_links)}, expected)
                self.assertIn("1.20.1/Azyron-Client-1.0.4-mc1.20.1.jar", html)
                self.assertIn("1.21.11/Azyron-Client-1.0.4-mc1.21.11.jar", html)
                self.assertIn("1.20.1/Azyron-Modpack-1.0.4-mc1.20.1.mrpack", html)
                self.assertIn("1.21.11/Azyron-Modpack-1.0.4-mc1.21.11.mrpack", html)

    def test_api_and_exactly_two_bundle_assets(self):
        with urlopen(self.base + "/api/release") as response:
            release = json.load(response)
        self.assertEqual(release["tag_name"], "v1.0.4")
        self.assertEqual(release["version"], VERSION)
        self.assertEqual(release["developer"], "JIGNESH GAMING")
        self.assertEqual(release["supported_minecraft_versions"], list(TARGETS))
        self.assertTrue(release["draft"])
        self.assertEqual(release["gameplay_and_device_benchmarks"], "NOT RUN")
        expected = {f"Azyron-Client-v{VERSION}.zip", f"Azyron-Modpack-v{VERSION}.zip"}
        self.assertEqual({asset["name"] for asset in release["assets"]}, expected)
        for asset in release["assets"]:
            with self.subTest(asset=asset["name"]), urlopen(self.base + asset["browser_download_url"]) as response:
                data = response.read()
                self.assertEqual(len(data), asset["size"])
                self.assertEqual("sha256:" + hashlib.sha256(data).hexdigest(), asset["digest"])
                with zipfile.ZipFile(io.BytesIO(data)) as archive:
                    self.assertIsNone(archive.testzip())
                    self.assertIn("README.txt", archive.namelist())
                    self.assertEqual(len([name for name in archive.namelist() if name.startswith("1.20.1/")]), 1)
                    self.assertEqual(len([name for name in archive.namelist() if name.startswith("1.21.11/")]), 1)

    def test_client_bundle_has_two_matching_native_payloads_and_head_metadata(self):
        filename = f"Azyron-Client-v{VERSION}.zip"
        url = self.base + f"/releases/download/v{VERSION}/{filename}"
        with urlopen(url) as response:
            data = response.read()
            etag = response.headers["ETag"]
            self.assertEqual(response.headers["Content-Disposition"], f'attachment; filename="{filename}"')
        with zipfile.ZipFile(io.BytesIO(data)) as bundle:
            for mc in TARGETS:
                name = f"{mc}/Azyron-Client-{VERSION}-mc{mc}.jar"
                with zipfile.ZipFile(io.BytesIO(bundle.read(name))) as jar:
                    metadata = json.loads(jar.read("fabric.mod.json"))
                    self.assertEqual(metadata["depends"]["minecraft"], [mc])
        with urlopen(Request(url, method="HEAD")) as response:
            self.assertEqual(response.headers["ETag"], etag)
            self.assertEqual(int(response.headers["Content-Length"]), len(data))
            self.assertEqual(response.read(), b"")

    def test_modpack_bundle_keeps_two_standard_single_version_profiles(self):
        filename = f"Azyron-Modpack-v{VERSION}.zip"
        url = self.base + f"/releases/download/v{VERSION}/{filename}?mc=1.20.1"
        with urlopen(url) as response:
            bundle_bytes = response.read()
        with zipfile.ZipFile(io.BytesIO(bundle_bytes)) as bundle:
            self.assertEqual(len(bundle.namelist()), 3)
            for mc in TARGETS:
                pack_name = f"{mc}/Azyron-Modpack-{VERSION}-mc{mc}.mrpack"
                with zipfile.ZipFile(io.BytesIO(bundle.read(pack_name))) as pack:
                    index = json.loads(pack.read("modrinth.index.json"))
                    self.assertEqual(index["versionId"], VERSION)
                    self.assertEqual(index["dependencies"]["minecraft"], mc)
                    embedded = pack.read(f"overrides/mods/Azyron-Client-{VERSION}.jar")
                    with zipfile.ZipFile(io.BytesIO(embedded)) as jar:
                        metadata = json.loads(jar.read("fabric.mod.json"))
                        self.assertEqual(metadata["depends"]["minecraft"], [mc])

    def test_rejects_wrong_target_and_unknown_asset(self):
        bundle_path = self.releases / f"Azyron-Client-v{VERSION}.zip"
        original = bundle_path.read_bytes()
        try:
            with zipfile.ZipFile(io.BytesIO(original)) as archive:
                entries = {name: archive.read(name) for name in archive.namelist()}
            jar_name = f"1.20.1/Azyron-Client-{VERSION}-mc1.20.1.jar"
            wrong = io.BytesIO()
            with zipfile.ZipFile(wrong, "w") as archive:
                archive.writestr("fabric.mod.json", json.dumps({
                    "id": "azyron-client", "version": VERSION,
                    "name": f"AZYRON CLIENT v{VERSION}", "authors": ["JIGNESH GAMING"],
                    "depends": {"minecraft": ["1.21.11"], "java": ">=21"},
                }))
            entries[jar_name] = wrong.getvalue()
            with zipfile.ZipFile(bundle_path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
                for name, data in entries.items():
                    archive.writestr(name, data)
            with self.assertRaisesRegex(ValueError, "Native JAR target mismatch"):
                server.load_live_assets()
        finally:
            bundle_path.write_bytes(original)

        with self.assertRaises(HTTPError) as caught:
            urlopen(self.base + "/downloads/not-a-v1.0.4-bundle.zip")
        self.assertEqual(caught.exception.code, 404)


if __name__ == "__main__":
    unittest.main(verbosity=2)
