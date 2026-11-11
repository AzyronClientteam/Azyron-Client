#!/usr/bin/env python3
"""Offline regression checks. Fixture JARs are NOT runnable game mods."""
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
release_assets = load("native_release_assets", ROOT / "scripts/prepare-native-candidates.py")
draft_updater = load("draft_release_updater", ROOT / "scripts/update-v104-draft-release.py")
portal = load("release_portal", ROOT / "releases-portal/server.py")


class NativePackaging(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.directory = Path(self.tmp.name)
        self.jars = {}
        self.packs = {}
        self.create_native_profiles()

    def create_native_profiles(self):
        for mc in TARGETS:
            jar = self.directory / release_assets.client_jar_name(mc)
            java = ">=17" if mc == "1.20.1" else ">=21"
            metadata = {
                "id": "azyron-client",
                "version": VERSION,
                "name": f"AZYRON CLIENT v{VERSION}",
                "authors": ["JIGNESH GAMING"],
                "depends": {"minecraft": [mc], "java": java},
            }
            java_major = release_assets.CLASS_TARGETS[mc][1]
            required = [
                "com/jigneshgaming/azyron/module/impl/ArmorHudModule.class",
                "com/jigneshgaming/azyron/ui/AzyronModsScreen.class",
                "com/jigneshgaming/azyron/profile/PerformanceEngine.class",
                "com/jigneshgaming/azyron/profile/MemoryOptimizer.class",
            ]
            fixture_classes = required + [
                f"com/jigneshgaming/azyron/fixture/Fixture{index:03}.class"
                for index in range(50 - len(required))
            ]
            class_header = bytes.fromhex("cafebabe0000") + java_major.to_bytes(2, "big")
            with zipfile.ZipFile(jar, "w") as archive:
                archive.writestr("fabric.mod.json", json.dumps(metadata))
                archive.writestr("azyron-client.mixins.json", json.dumps({
                    "package": "com.jigneshgaming.azyron.mixin", "mixins": [], "client": []
                }))
                for entry in fixture_classes:
                    archive.writestr(entry, class_header)
                archive.writestr("fixture.txt", "Synthetic packaging fixture only; not a compiled mod.")
            self.jars[mc] = jar
            pack = self.directory / release_assets.modpack_name(mc)
            builder.build_mrpack(str(jar), str(pack), mc)
            self.packs[mc] = pack

    def assert_profile(self, pack, mc):
        with zipfile.ZipFile(pack) as archive:
            self.assertIsNone(archive.testzip())
            self.assertEqual(len(archive.namelist()), len(set(archive.namelist())))
            index = json.loads(archive.read("modrinth.index.json"))
            self.assertEqual(index["versionId"], VERSION)
            self.assertEqual(index["dependencies"]["minecraft"], mc)
            self.assertEqual(index["name"], f"Azyron Modpack v{VERSION}")
            paths = [entry["path"] for entry in index["files"]]
            self.assertEqual(len(paths), len(set(paths)))
            config = json.loads(archive.read("overrides/config/azyron-client.json"))
            self.assertEqual(config["configVersion"], VERSION)
            self.assertEqual(config["selectedMcVersion"], mc)
            self.assertEqual(config["activeProfile"], "POTATO")
            self.assertTrue(config["customMainMenuEnabled"])
            self.assertTrue(config["autoMemoryOptimization"])
            options = dict(line.split(":", 1) for line in archive.read("overrides/options.txt").decode().splitlines())
            self.assertEqual(options["renderDistance"], "6")
            self.assertEqual(options["simulationDistance"], "5")
            self.assertEqual(options["maxFps"], "60")
            sodium = json.loads(archive.read("overrides/config/sodium-options.json"))
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
            dynamic = json.loads(archive.read("overrides/config/dynamic_fps.json"))
            for state in dynamic["states"].values():
                self.assertFalse(state["run_garbage_collector"])
                self.assertEqual(state["graphics_state"], "default")
                self.assertIn("show_toasts", state)
                self.assertIn("volume_multiplier" if mc == "1.20.1" else "volume_multipliers", state)
            configs = {name for name in archive.namelist() if name.startswith("overrides/config/")}
            self.assertEqual(configs, {
                "overrides/config/azyron-client.json",
                "overrides/config/sodium-options.json",
                "overrides/config/dynamic_fps.json",
            })
            self.assertEqual([name for name in archive.namelist() if name.startswith("overrides/mods/")],
                             [f"overrides/mods/Azyron-Client-{VERSION}.jar"])

    def test_target_specific_gradle_and_modpack_paths(self):
        for mc in TARGETS:
            with self.subTest(mc=mc):
                self.assertEqual(Path(builder.default_native_jar_path(mc)).name,
                                 release_assets.client_jar_name(mc))
                self.assertEqual(Path(builder.default_modpack_path(mc)).name,
                                 release_assets.modpack_name(mc))

    def test_1201_standard_modrinth_profile(self):
        self.assert_profile(self.packs["1.20.1"], "1.20.1")

    def test_12111_standard_modrinth_profile(self):
        self.assert_profile(self.packs["1.21.11"], "1.21.11")

    def test_four_direct_native_assets_and_no_client_zip(self):
        output = self.directory / "release-assets"
        provenance = release_assets.assemble_release_assets(
            self.jars, self.packs, output, run_id="fixture", commit="fixture"
        )
        expected = {
            release_assets.client_jar_name("1.20.1"),
            release_assets.client_jar_name("1.21.11"),
            release_assets.modpack_name("1.20.1"),
            release_assets.modpack_name("1.21.11"),
        }
        actual = {path.name for path in output.iterdir() if path.suffix in (".jar", ".mrpack")}
        self.assertEqual(actual, expected)
        self.assertFalse(list(output.glob("*.zip")))
        self.assertEqual(set(provenance["assets"]), expected)
        for mc in TARGETS:
            jar_path = output / release_assets.client_jar_name(mc)
            with zipfile.ZipFile(jar_path) as jar:
                metadata = json.loads(jar.read("fabric.mod.json"))
                self.assertEqual(metadata["depends"]["minecraft"], [mc])
            pack_path = output / release_assets.modpack_name(mc)
            with zipfile.ZipFile(pack_path) as pack:
                self.assertEqual(json.loads(pack.read("modrinth.index.json"))["dependencies"]["minecraft"], mc)
                embedded = pack.read(f"overrides/mods/Azyron-Client-{VERSION}.jar")
                self.assertEqual(embedded, self.jars[mc].read_bytes())
        sums = (output / "SHA256SUMS.txt").read_text().splitlines()
        self.assertEqual(len(sums), 4)
        for line in sums:
            expected_hash, filename = line.split("  ", 1)
            self.assertEqual(hashlib.sha256((output / filename).read_bytes()).hexdigest(), expected_hash)

    def test_draft_guard_rechecks_real_target_metadata_and_exact_pairing(self):
        output = self.directory / "draft-candidate"
        target_reports = {
            mc: release_assets.validate_native_jar(self.jars[mc], mc)
            for mc in TARGETS
        }
        release_assets.assemble_release_assets(
            self.jars, self.packs, output, run_id="fixture-run", commit="fixture-commit",
            target_reports=target_reports,
        )
        provenance, hashes = draft_updater.validate_candidate(output, "fixture-run", "fixture-commit")
        self.assertEqual(provenance["version"], VERSION)
        self.assertEqual(set(hashes), set(draft_updater.ASSET_TARGETS))
        self.assertEqual(provenance["targets"]["1.21.11"]["java_class_major"], 65)
        self.assertEqual(provenance["targets"]["1.20.1"]["java_class_major"], 61)

    def test_prepare_script_stages_and_verifies_four_native_files(self):
        inputs = self.directory / "build-artifacts"
        inputs.mkdir()
        for mc in TARGETS:
            (inputs / release_assets.client_jar_name(mc)).write_bytes(self.jars[mc].read_bytes())
        output = self.directory / "prepared-candidates"
        provenance = release_assets.prepare(inputs, output, "fixture-run", "fixture-commit")
        self.assertEqual(set(provenance["assets"]), set(draft_updater.ASSET_TARGETS))
        checked, _hashes = draft_updater.validate_candidate(output, "fixture-run", "fixture-commit")
        self.assertEqual(checked["source_commit"], "fixture-commit")

    def test_draft_guard_rejects_any_client_or_modpack_zip_bundle(self):
        output = self.directory / "draft-candidate-with-zip"
        target_reports = {
            mc: release_assets.validate_native_jar(self.jars[mc], mc)
            for mc in TARGETS
        }
        release_assets.assemble_release_assets(
            self.jars, self.packs, output, run_id="fixture-run", commit="fixture-commit",
            target_reports=target_reports,
        )
        (output / "Azyron-Client-v1.0.4.zip").write_bytes(b"not an allowed release asset")
        with self.assertRaisesRegex(RuntimeError, "exactly four target-native downloads"):
            draft_updater.validate_candidate(output, "fixture-run", "fixture-commit")

    def test_portal_exposes_only_the_four_direct_native_assets(self):
        output = self.directory / "portal-assets"
        release_assets.assemble_release_assets(self.jars, self.packs, output)
        old = portal.RELEASES_DIR
        try:
            portal.RELEASES_DIR = str(output)
            assets = portal.load_live_assets()
            self.assertEqual({asset["display_name"] for asset in assets}, {
                release_assets.client_jar_name("1.20.1"),
                release_assets.client_jar_name("1.21.11"),
                release_assets.modpack_name("1.20.1"),
                release_assets.modpack_name("1.21.11"),
            })
            self.assertEqual(len(assets), 4)
            self.assertEqual({Path(asset["display_name"]).suffix for asset in assets}, {".jar", ".mrpack"})
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
