#!/usr/bin/env python3
"""Offline packaging/schema tests. Fixture JARs are NOT runnable game mods."""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = "1.0.4"
TARGETS = ("1.20.1", "1.21.11")


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


builder = load("pack_builder", ROOT / "scripts/build-modpack.py")
bundles = load("candidate_bundles", ROOT / "scripts/prepare-native-candidates.py")
portal = load("release_portal", ROOT / "releases-portal/server.py")


class LowRamPackaging(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.directory = Path(self.tmp.name)
        self.jars = {}
        self.packs = {}
        self.create_native_profiles()

    def create_native_profiles(self):
        for mc in TARGETS:
            jar = self.directory / f"Azyron-Client-{VERSION}-mc{mc}.jar"
            metadata = {
                "id": "azyron-client",
                "version": VERSION,
                "name": f"AZYRON CLIENT v{VERSION}",
                "authors": ["JIGNESH GAMING"],
                "depends": {
                    "minecraft": [mc],
                    "java": ">=17" if mc == "1.20.1" else ">=21",
                },
            }
            with zipfile.ZipFile(jar, "w") as archive:
                archive.writestr("fabric.mod.json", json.dumps(metadata))
                archive.writestr("fixture.txt", "Synthetic packaging fixture only; not a compiled mod.\n")
            self.jars[mc] = jar
            pack = self.directory / f"Azyron-Modpack-{VERSION}-mc{mc}.mrpack"
            builder.build_mrpack(str(jar), str(pack), mc)
            self.packs[mc] = pack

    def assert_profile(self, pack, mc):
        with zipfile.ZipFile(pack) as z:
            self.assertIsNone(z.testzip())
            self.assertEqual(len(z.namelist()), len(set(z.namelist())))
            index = json.loads(z.read("modrinth.index.json"))
            self.assertEqual(index["versionId"], VERSION)
            self.assertEqual(index["dependencies"]["minecraft"], mc)
            self.assertEqual(index["name"], f"Azyron Modpack v{VERSION}")
            paths = [f["path"] for f in index["files"]]
            self.assertEqual(len(paths), len(set(paths)))
            config = json.loads(z.read("overrides/config/azyron-client.json"))
            self.assertEqual(config["configVersion"], VERSION)
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
            configs = {n for n in z.namelist() if n.startswith("overrides/config/")}
            self.assertEqual(configs, {
                "overrides/config/azyron-client.json",
                "overrides/config/sodium-options.json",
                "overrides/config/dynamic_fps.json",
            })
            self.assertEqual([n for n in z.namelist() if n.startswith("overrides/mods/")],
                             [f"overrides/mods/Azyron-Client-{VERSION}.jar"])

    def test_native_default_paths(self):
        for mc in TARGETS:
            with self.subTest(mc=mc):
                self.assertEqual(Path(builder.default_native_jar_path(mc)).name,
                                 f"Azyron-Client-{VERSION}-mc{mc}.jar")
                self.assertEqual(Path(builder.default_modpack_path(mc)).name,
                                 f"Azyron-Modpack-{VERSION}-mc{mc}.mrpack")

    def test_1201_profile_schema(self):
        self.assert_profile(self.packs["1.20.1"], "1.20.1")

    def test_12111_profile_schema(self):
        self.assert_profile(self.packs["1.21.11"], "1.21.11")

    def test_bundle_builds_exactly_two_user_download_archives(self):
        output = self.directory / "bundles"
        provenance = bundles.assemble_download_bundles(
            self.jars, self.packs, output, run_id="fixture", commit="fixture"
        )
        client_name = f"Azyron-Client-v{VERSION}.zip"
        pack_name = f"Azyron-Modpack-v{VERSION}.zip"
        self.assertEqual({p.name for p in output.glob("*.zip")}, {client_name, pack_name})
        self.assertTrue((output / "SHA256SUMS.txt").is_file())
        self.assertTrue((output / "BUILD-PROVENANCE.json").is_file())
        for path in (output / client_name, output / pack_name):
            with zipfile.ZipFile(path) as archive:
                self.assertIsNone(archive.testzip())
                self.assertIn("README.txt", archive.namelist())
                self.assertEqual(len([n for n in archive.namelist() if n.startswith("1.20.1/")]), 1)
                self.assertEqual(len([n for n in archive.namelist() if n.startswith("1.21.11/")]), 1)
        with zipfile.ZipFile(output / client_name) as archive:
            self.assertEqual(archive.read(f"1.20.1/Azyron-Client-{VERSION}-mc1.20.1.jar"), self.jars["1.20.1"].read_bytes())
            self.assertEqual(archive.read(f"1.21.11/Azyron-Client-{VERSION}-mc1.21.11.jar"), self.jars["1.21.11"].read_bytes())
        with zipfile.ZipFile(output / pack_name) as archive:
            for mc in TARGETS:
                internal = f"{mc}/Azyron-Modpack-{VERSION}-mc{mc}.mrpack"
                self.assertEqual(archive.read(internal), self.packs[mc].read_bytes())
        self.assertEqual(set(provenance["downloads"]), {"client", "modpack"})
        sums = (output / "SHA256SUMS.txt").read_text()
        self.assertEqual(len(sums.splitlines()), 2)
        for line in sums.splitlines():
            expected, filename = line.split("  ", 1)
            self.assertEqual(hashlib.sha256((output / filename).read_bytes()).hexdigest(), expected)

    def test_portal_serves_only_the_two_bundle_assets(self):
        output = self.directory / "bundles"
        bundles.assemble_download_bundles(self.jars, self.packs, output)
        old = portal.RELEASES_DIR
        try:
            portal.RELEASES_DIR = str(output)
            assets = portal.load_live_assets()
            self.assertEqual({asset["display_name"] for asset in assets}, {
                f"Azyron-Client-v{VERSION}.zip",
                f"Azyron-Modpack-v{VERSION}.zip",
            })
            self.assertEqual(len(assets), 2)
            for asset in assets:
                self.assertEqual(Path(asset["display_name"]).suffix, ".zip")
        finally:
            portal.RELEASES_DIR = old

    def test_native_jar_cannot_be_relabelled(self):
        wrong = self.directory / "wrong-target.jar"
        with zipfile.ZipFile(wrong, "w") as archive:
            archive.writestr("fabric.mod.json", json.dumps({
                "id": "azyron-client", "version": VERSION,
                "depends": {"minecraft": ["1.21.11"]},
            }))
        with self.assertRaisesRegex(ValueError, "Native client JAR"):
            builder.build_mrpack(str(wrong), str(self.directory / "wrong.mrpack"), "1.20.1")

    def test_reject_unknown_target(self):
        with self.assertRaises(ValueError):
            builder.build_mrpack(str(self.jars["1.21.11"]), str(self.directory / "unknown.mrpack"), "1.21.4")

    def test_installer_guards_before_download(self):
        script = str(ROOT / "releases/download-dependencies.sh")
        for arguments in [["--mc", "1.21.4"], ["--mc", "1.21.11", "--modernfix"],
                          ["--mc", "1.20.1", "--modernfix"],
                          ["--mc", "1.20.1", "--modernfix", "--fabric-loader", "0.14.22"]]:
            result = subprocess.run(["bash", script, *arguments], cwd=self.directory, capture_output=True)
            self.assertEqual(result.returncode, 2, result.stdout + result.stderr)
            self.assertFalse((self.directory / "mods").exists())

    def test_installer_rejects_corrupt_bytes_atomically(self):
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
