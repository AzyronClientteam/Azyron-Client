#!/usr/bin/env python3
"""Integration tests for the direct native-asset release portal."""
from __future__ import annotations

import importlib.util
import json
from pathlib import Path
import tempfile
import threading
import unittest
import urllib.error
import urllib.request
import zipfile

SERVER_PATH = Path(__file__).with_name("server.py")
CLIENT_ROOT = SERVER_PATH.parent.parent
spec = importlib.util.spec_from_file_location("azyron_release_portal", SERVER_PATH)
server = importlib.util.module_from_spec(spec)
spec.loader.exec_module(server)
builder_spec = importlib.util.spec_from_file_location(
    "azyron_portal_pack_builder", CLIENT_ROOT / "scripts/build-modpack.py"
)
builder = importlib.util.module_from_spec(builder_spec)
builder_spec.loader.exec_module(builder)


class ReleasePortalTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.original = server.RELEASES_DIR
        server.RELEASES_DIR = self.temp.name
        self.addCleanup(lambda: setattr(server, "RELEASES_DIR", self.original))
        self.files = {
            "Azyron-Client-1.0.4.jar": ("jar", "1.21.11"),
            "Azyron-Modpack-1.0.4.mrpack": ("mrpack", "1.21.11"),
            "Azyron-Client-1.0.4-mc1.20.1.jar": ("jar", "1.20.1"),
            "Azyron-Modpack-1.0.4-mc1.20.1.mrpack": ("mrpack", "1.20.1"),
        }
        jar_paths = {}
        required_classes = [
            "com/jigneshgaming/azyron/module/impl/ArmorHudModule.class",
            "com/jigneshgaming/azyron/ui/AzyronModsScreen.class",
            "com/jigneshgaming/azyron/profile/PerformanceEngine.class",
            "com/jigneshgaming/azyron/profile/MemoryOptimizer.class",
        ]
        for name, (kind, mc) in self.files.items():
            path = Path(self.temp.name) / name
            if kind != "jar":
                continue
            java_major = server.native.CLASS_TARGETS[mc][1]
            metadata = {
                "id": "azyron-client",
                "version": "1.0.4",
                "name": "AZYRON CLIENT v1.0.4",
                "authors": ["JIGNESH GAMING"],
                "depends": {"minecraft": [mc], "java": ">=17" if mc == "1.20.1" else ">=21"},
            }
            fixture_classes = required_classes + [
                f"com/jigneshgaming/azyron/fixture/Fixture{index:03}.class"
                for index in range(50 - len(required_classes))
            ]
            header = bytes.fromhex("cafebabe0000") + java_major.to_bytes(2, "big")
            with zipfile.ZipFile(path, "w") as archive:
                archive.writestr("fabric.mod.json", json.dumps(metadata))
                archive.writestr("azyron-client.mixins.json", json.dumps({
                    "package": "com.jigneshgaming.azyron.mixin", "mixins": [], "client": []
                }))
                for entry in fixture_classes:
                    archive.writestr(entry, header)
            jar_paths[mc] = path

        for name, (kind, mc) in self.files.items():
            if kind == "mrpack":
                builder.build_mrpack(str(jar_paths[mc]), str(Path(self.temp.name) / name), mc)

        self.httpd = server.ThreadingHTTPServerV6(("127.0.0.1", 0), server.PortalHandler)
        self.thread = threading.Thread(target=self.httpd.serve_forever, daemon=True)
        self.thread.start()
        self.addCleanup(self.stop_server)
        self.base = f"http://127.0.0.1:{self.httpd.server_port}"

    def stop_server(self):
        self.httpd.shutdown()
        self.httpd.server_close()
        self.thread.join(timeout=2)

    def get(self, path):
        with urllib.request.urlopen(self.base + path, timeout=5) as response:
            return response.status, response.headers, response.read()

    def test_home_page_has_four_target_specific_files_and_no_client_zip(self):
        status, headers, body = self.get("/")
        self.assertEqual(status, 200)
        self.assertIn("text/html", headers["Content-Type"])
        text = body.decode()
        for name in self.files:
            self.assertIn(name, text)
        self.assertIn("Minecraft 1.21.11", text)
        self.assertIn("Minecraft 1.20.1", text)
        self.assertIn("not a renamed ZIP", text)
        self.assertNotIn("Azyron-Client-v1.0.4.zip", text)
        self.assertNotIn("Download client bundle ZIP", text)

    def test_health_reports_only_approved_assets(self):
        status, headers, body = self.get("/healthz")
        self.assertEqual(status, 200)
        self.assertEqual(json.loads(body), {"ok": True, "assets": 4})

    def test_downloads_are_the_original_native_files(self):
        for name in self.files:
            with self.subTest(name=name):
                status, headers, body = self.get("/downloads/" + name)
                expected = (Path(self.temp.name) / name).read_bytes()
                self.assertEqual(status, 200)
                self.assertEqual(body, expected)
                self.assertEqual(headers["Content-Disposition"], f'attachment; filename="{name}"')
                self.assertEqual(headers["X-Content-Type-Options"], "nosniff")
                if name.endswith(".jar"):
                    self.assertEqual(headers["Content-Type"], "application/java-archive")
                else:
                    self.assertEqual(headers["Content-Type"], "application/zip")

    def test_zip_bundles_stale_files_and_unknown_paths_are_hidden(self):
        (Path(self.temp.name) / "Azyron-Client-v1.0.4.zip").write_bytes(b"old bundle")
        (Path(self.temp.name) / "README.txt").write_text("not a downloadable asset")
        self.assertEqual(len(server.load_live_assets()), 4)
        for path in ("/downloads/Azyron-Client-v1.0.4.zip", "/downloads/README.txt", "/robots.txt"):
            with self.subTest(path=path), self.assertRaises(urllib.error.HTTPError) as context:
                urllib.request.urlopen(self.base + path, timeout=5)
            self.assertEqual(context.exception.code, 404)

    def test_symlink_is_never_served(self):
        path = Path(self.temp.name) / "Azyron-Client-1.0.4.jar"
        content = path.read_bytes()
        path.unlink()
        target = Path(self.temp.name) / "outside.jar"
        target.write_bytes(content)
        path.symlink_to(target)
        self.assertNotIn("Azyron-Client-1.0.4.jar", {item["display_name"] for item in server.load_live_assets()})
        with self.assertRaises(urllib.error.HTTPError) as context:
            urllib.request.urlopen(self.base + "/downloads/Azyron-Client-1.0.4.jar", timeout=5)
        self.assertEqual(context.exception.code, 404)


if __name__ == "__main__":
    unittest.main(verbosity=2)
