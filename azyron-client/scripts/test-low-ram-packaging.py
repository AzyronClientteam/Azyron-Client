#!/usr/bin/env python3
"""Offline packaging/schema tests. Fixture JAR is NOT a runnable game mod."""
import importlib.util
import io
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


builder = load("pack_builder", ROOT / "scripts/build-modpack.py")
portal = load("release_portal", ROOT / "releases-portal/server.py")


class LowRamPackaging(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.directory = Path(self.tmp.name)
        self.jar = self.directory / "fixture.jar"
        with zipfile.ZipFile(self.jar, "w") as z:
            z.writestr("fabric.mod.json", json.dumps({
                "version": "1.0.3", "depends": {"minecraft": ["1.20.1", "1.21.11"]}
            }))

    def build(self, mc):
        path = self.directory / f"pack-{mc}.mrpack"
        with zipfile.ZipFile(self.jar, "w") as z:
            z.writestr("fabric.mod.json", json.dumps({
                "version": "1.0.3", "depends": {"minecraft": [mc]}
            }))
        builder.build_mrpack(str(self.jar), str(path), mc)
        return path

    def assert_profile(self, pack, mc):
        with zipfile.ZipFile(pack) as z:
            self.assertIsNone(z.testzip())
            self.assertEqual(len(z.namelist()), len(set(z.namelist())))
            index = json.loads(z.read("modrinth.index.json"))
            self.assertEqual(index["dependencies"]["minecraft"], mc)
            self.assertEqual(index["name"], "Azyron Modpack v1.0.3")
            paths = [f["path"] for f in index["files"]]
            self.assertEqual(len(paths), len(set(paths)))
            config = json.loads(z.read("overrides/config/azyron-client.json"))
            self.assertEqual(config["selectedMcVersion"], mc)
            self.assertEqual(config["activeProfile"], "POTATO")
            self.assertTrue(config["customMainMenuEnabled"])
            self.assertTrue(config["autoMemoryOptimization"])
            options = dict(line.split(":", 1) for line in z.read("overrides/options.txt").decode().splitlines())
            self.assertEqual(options["renderDistance"], "6")
            self.assertEqual(options["simulationDistance"], "5")
            self.assertEqual(options["maxFps"], "60")
            sodium = json.loads(z.read("overrides/config/sodium-options.json"))
            perf = sodium["performance"]
            self.assertEqual(perf["chunk_builder_threads"], 1)
            self.assertEqual(sodium["advanced"]["cpu_render_ahead_limit"], 2)
            self.assertNotIn("cpu_render_ahead_limit", perf)
            self.assertFalse(perf["use_no_error_g_l_context"])
            if mc == "1.20.1":
                self.assertTrue(perf["always_defer_chunk_updates_v2"])
                self.assertNotIn("chunk_build_defer_mode", perf)
            else:
                self.assertEqual(perf["chunk_build_defer_mode"], "ALWAYS")
                self.assertNotIn("always_defer_chunk_updates_v2", perf)
            dynamic = json.loads(z.read("overrides/config/dynamic_fps.json"))
            for state in dynamic["states"].values():
                self.assertFalse(state["run_garbage_collector"])
                self.assertEqual(state["graphics_state"], "default")
                self.assertIn("show_toasts", state)
                self.assertIn("volume_multiplier" if mc == "1.20.1" else "volume_multipliers", state)
            # No guessed schemas for unrelated mods; let installed versions own defaults.
            configs = {n for n in z.namelist() if n.startswith("overrides/config/")}
            self.assertEqual(configs, {"overrides/config/azyron-client.json",
                                      "overrides/config/sodium-options.json", "overrides/config/dynamic_fps.json"})
            self.assertEqual([n for n in z.namelist() if n.startswith("overrides/mods/")],
                             ["overrides/mods/Azyron-Client-1.0.3.jar"])

    def test_1201(self):
        self.assert_profile(self.build("1.20.1"), "1.20.1")

    def test_12111(self):
        self.assert_profile(self.build("1.21.11"), "1.21.11")

    def test_portal_selects_native_artifact(self):
        for mc in ("1.20.1", "1.21.11"):
            source = self.build(mc)
            (self.directory / f"Azyron-Modpack-1.0.3-mc{mc}.mrpack").write_bytes(source.read_bytes())
        old = portal.RELEASES_DIR
        try:
            portal.RELEASES_DIR = str(self.directory)
            for mc in ("1.20.1", "1.21.11"):
                self.assert_profile(io.BytesIO(portal.build_mrpack_bytes_for_mc(mc)), mc)
        finally:
            portal.RELEASES_DIR = old

    def test_native_jar_cannot_be_relabelled(self):
        source = self.build("1.21.11")
        (self.directory / "Azyron-Modpack-1.0.3.mrpack").write_bytes(source.read_bytes())
        with self.assertRaisesRegex(ValueError, "Native client JAR"):
            builder.build_mrpack(str(self.jar), str(self.directory / "wrong.mrpack"), "1.20.1")
        old = portal.RELEASES_DIR
        try:
            portal.RELEASES_DIR = str(self.directory)
            with self.assertRaisesRegex(ValueError, "unsafe"):
                portal.build_mrpack_bytes_for_mc("1.20.1")
        finally:
            portal.RELEASES_DIR = old

    def test_reject_unknown_target(self):
        with self.assertRaises(ValueError):
            self.build("1.21.4")

    def test_installer_guards_before_download(self):
        script = str(ROOT / "releases/download-dependencies.sh")
        for arguments in [["--mc", "1.21.4"], ["--mc", "1.21.11", "--modernfix"],
                          ["--mc", "1.20.1", "--modernfix"],
                          ["--mc", "1.20.1", "--modernfix", "--fabric-loader", "0.14.22"]]:
            result = subprocess.run(["bash", script, *arguments], cwd=self.directory, capture_output=True)
            self.assertEqual(result.returncode, 2, result.stdout + result.stderr)
            self.assertFalse((self.directory / "mods").exists())

    def test_installer_rejects_corrupt_bytes_atomically(self):
        # Fake transport exercises installer hashing, without contacting any host.
        bin_dir = self.directory / "bin"
        bin_dir.mkdir()
        curl = bin_dir / "curl"
        curl.write_text('#!/bin/bash\nwhile [[ $# -gt 0 ]]; do if [[ "$1" == -o ]]; then printf bad > "$2"; exit 0; fi; shift; done\nexit 1\n')
        curl.chmod(0o755)
        mods = self.directory / "mods"
        mods.mkdir()
        existing = mods / "fabric-api-0.88.1+1.20.1.jar"
        existing.write_bytes(b"existing user file")
        result = subprocess.run(["bash", str(ROOT / "releases/download-dependencies.sh"), "--mc", "1.20.1"],
                                cwd=self.directory, capture_output=True,
                                env={**os.environ, "PATH": str(bin_dir) + os.pathsep + os.environ["PATH"]})
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(existing.read_bytes(), b"existing user file")
        self.assertFalse(list(mods.glob(".azyron-download.*")))
        self.assertIn(b"FAILED", result.stdout)

    def test_installer_refuses_version_mixing(self):
        mods = self.directory / "mods"
        mods.mkdir()
        (mods / ".azyron-minecraft-version").write_text("1.20.1\n")
        result = subprocess.run(["bash", str(ROOT / "releases/download-dependencies.sh"), "--mc", "1.21.11"],
                                cwd=self.directory, capture_output=True)
        self.assertEqual(result.returncode, 2)
        self.assertIn(b"Do not mix", result.stderr)


if __name__ == "__main__":
    unittest.main(verbosity=2)
