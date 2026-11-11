"""Portal integration checks using synthetic metadata-only fixture JARs.

The fixtures exercise the download routes and target guards; they are not runnable
Minecraft mods and do not validate Fabric compatibility or gameplay.
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


def load_builder():
    path = ROOT / "scripts/build-modpack.py"
    spec = importlib.util.spec_from_file_location("portal_test_pack_builder", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


builder = load_builder()


class PortalTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = tempfile.TemporaryDirectory()
        cls.addClassCleanup(cls.tmp.cleanup)
        cls.releases = Path(cls.tmp.name) / "releases"
        cls.releases.mkdir()
        cls.original_releases_dir = server.RELEASES_DIR
        server.RELEASES_DIR = str(cls.releases)

        for mc in ("1.20.1", "1.21.11"):
            jar = cls.releases / f"Azyron-Client-1.0.3-mc{mc}.jar"
            with zipfile.ZipFile(jar, "w", compression=zipfile.ZIP_DEFLATED) as archive:
                archive.writestr("fabric.mod.json", json.dumps({
                    "id": "azyron-client",
                    "version": "1.0.3",
                    "depends": {"minecraft": [mc]},
                }))
            pack = cls.releases / f"Azyron-Modpack-1.0.3-mc{mc}.mrpack"
            builder.build_mrpack(str(jar), str(pack), mc)

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

    def test_pages_show_matching_native_pairs(self):
        for path in ("/", "/releases", "/releases/tag/v1.0.3"):
            with self.subTest(path=path), urlopen(self.base + path) as response:
                html = response.read().decode()
                self.assertIn('data-mc-tab="1.20.1"', html)
                self.assertIn('data-mc-tab="1.21.11"', html)
                self.assertIn("AZYRON CLIENT v1.0.3", html)
                self.assertIn("JIGNESH GAMING", html)
                self.assertIn("generated Minecraft/Fabric stubs", html)
                for mc in ("1.20.1", "1.21.11"):
                    self.assertIn(f"Azyron-Client-1.0.3-mc{mc}.jar", html)
                    self.assertIn(f"Azyron-Modpack-1.0.3-mc{mc}.mrpack", html)
                self.assertNotIn("Selectable Minecraft Version Profile", html)

    def test_api_and_all_native_assets(self):
        with urlopen(self.base + "/api/release") as response:
            release = json.load(response)
        self.assertEqual(release["supported_minecraft_versions"], ["1.21.11", "1.20.1"])
        self.assertTrue(release["draft"])
        self.assertIn("historical generated-stub suites", release["verification_environment"])
        expected = {
            f"Azyron-Client-1.0.3-mc{mc}.jar" for mc in ("1.20.1", "1.21.11")
        } | {
            f"Azyron-Modpack-1.0.3-mc{mc}.mrpack" for mc in ("1.20.1", "1.21.11")
        }
        self.assertEqual({asset["name"] for asset in release["assets"]}, expected)
        for asset in release["assets"]:
            with self.subTest(asset=asset["name"]), urlopen(self.base + asset["browser_download_url"]) as response:
                data = response.read()
                self.assertEqual(len(data), asset["size"])
                self.assertEqual("sha256:" + hashlib.sha256(data).hexdigest(), asset["digest"])
                with zipfile.ZipFile(io.BytesIO(data)) as archive:
                    self.assertIsNone(archive.testzip())
                    if asset["name"].endswith(".jar"):
                        metadata = json.loads(archive.read("fabric.mod.json"))
                        mc = asset["name"].split("-mc", 1)[1].removesuffix(".jar")
                        self.assertEqual(metadata["depends"]["minecraft"], [mc])

    def test_native_packs_and_get_head_metadata(self):
        for mc in ("1.20.1", "1.21.11"):
            filename = f"Azyron-Modpack-1.0.3-mc{mc}.mrpack"
            url = self.base + f"/releases/download/v1.0.3/{filename}"
            with self.subTest(mc=mc), urlopen(url) as response:
                data = response.read()
                etag = response.headers["ETag"]
                self.assertEqual(response.headers["Content-Disposition"], f'attachment; filename="{filename}"')
            with zipfile.ZipFile(io.BytesIO(data)) as pack:
                self.assertIsNone(pack.testzip())
                self.assertEqual(json.loads(pack.read("modrinth.index.json"))["dependencies"]["minecraft"], mc)
                embedded = pack.read("overrides/mods/Azyron-Client-1.0.3.jar")
                with zipfile.ZipFile(io.BytesIO(embedded)) as jar:
                    metadata = json.loads(jar.read("fabric.mod.json"))
                    self.assertEqual(metadata["depends"]["minecraft"], [mc])
            with urlopen(Request(url, method="HEAD")) as response:
                self.assertEqual(response.headers["ETag"], etag)
                self.assertEqual(int(response.headers["Content-Length"]), len(data))
                self.assertEqual(response.read(), b"")

    def test_query_cannot_switch_a_native_pack_target(self):
        filename = "Azyron-Modpack-1.0.3-mc1.21.11.mrpack"
        url = self.base + f"/releases/download/v1.0.3/{filename}?mc=1.20.1"
        with urlopen(url) as response:
            data = response.read()
        with zipfile.ZipFile(io.BytesIO(data)) as pack:
            self.assertEqual(json.loads(pack.read("modrinth.index.json"))["dependencies"]["minecraft"], "1.21.11")

    def test_rejects_wrong_target_and_missing_asset(self):
        jar = self.releases / "Azyron-Client-1.0.3-mc1.20.1.jar"
        original = jar.read_bytes()
        try:
            with zipfile.ZipFile(jar, "w") as archive:
                archive.writestr("fabric.mod.json", json.dumps({
                    "id": "azyron-client",
                    "version": "1.0.3",
                    "depends": {"minecraft": ["1.21.11"]},
                }))
            with self.assertRaisesRegex(ValueError, "Native JAR target mismatch"):
                server.load_live_assets()
        finally:
            jar.write_bytes(original)

        with self.assertRaises(HTTPError) as caught:
            urlopen(self.base + "/downloads/not-a-native-asset.jar")
        self.assertEqual(caught.exception.code, 404)


if __name__ == "__main__":
    unittest.main()
