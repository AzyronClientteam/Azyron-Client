#!/usr/bin/env python3
"""Validate both real Fabric builds and make the two v1.0.4 download bundles.

This is a candidate-preparation step only: it does not contact GitHub or publish.
Generated-stub fixtures are never used by the native candidate validator.
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import struct
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = "1.0.4"
VERSIONS = ("1.20.1", "1.21.11")
CLIENT_BUNDLE = f"Azyron-Client-v{VERSION}.zip"
MODPACK_BUNDLE = f"Azyron-Modpack-v{VERSION}.zip"
CLASS_TARGETS = {"1.20.1": (17, 61), "1.21.11": (21, 65)}


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def jar_metadata(path: Path) -> dict:
    with zipfile.ZipFile(path) as jar:
        bad = jar.testzip()
        assert bad is None, f"Corrupt JAR {path}: {bad}"
        return json.loads(jar.read("fabric.mod.json"))


def validate_jar_metadata(path: Path, mc: str) -> dict:
    metadata = jar_metadata(path)
    assert metadata.get("id") == "azyron-client", f"Unexpected mod id in {path}"
    assert metadata.get("version") == VERSION, f"Unexpected mod version in {path}"
    assert metadata.get("name") == f"AZYRON CLIENT v{VERSION}", f"Unexpected display name in {path}"
    assert metadata.get("depends", {}).get("minecraft") == [mc], f"JAR is not native to {mc}: {path}"
    java_level = 17 if mc == "1.20.1" else 21
    assert metadata.get("depends", {}).get("java") == f">={java_level}", f"Wrong Java runtime for {mc}"
    assert "JIGNESH GAMING" in metadata.get("authors", []), f"Missing developer in {path}"
    return metadata


def validate_native_jar(path: Path, mc: str) -> dict:
    metadata = validate_jar_metadata(path, mc)
    java_level, expected_major = CLASS_TARGETS[mc]
    with zipfile.ZipFile(path) as jar:
        entries = jar.namelist()
        own_classes = [
            name for name in entries
            if name.startswith("com/jigneshgaming/azyron/") and name.endswith(".class")
        ]
        assert len(own_classes) >= 50, f"Incomplete native client for {mc}: {len(own_classes)} classes"
        assert not any(name.startswith("net/minecraft/") for name in entries), "Minecraft/test stubs bundled"
        majors = set()
        for name in own_classes:
            magic, _minor, major = struct.unpack(">IHH", jar.read(name)[:8])
            assert magic == 0xCAFEBABE, f"Invalid class file: {name}"
            assert major == expected_major, (name, major, expected_major)
            majors.add(major)

        mixins = json.loads(jar.read("azyron-client.mixins.json"))
        configured = []
        for group in ("mixins", "client"):
            for simple_name in mixins.get(group, []):
                entry = mixins["package"].replace(".", "/") + "/" + simple_name + ".class"
                assert entry in entries, f"Configured mixin missing from {mc} JAR: {entry}"
                if mc == "1.21.11":
                    assert not simple_name.endswith("120Mixin"), f"Legacy mixin leaked into 1.21.11: {simple_name}"
                configured.append(simple_name)

        required = (
            "com/jigneshgaming/azyron/module/impl/ArmorHudModule.class",
            "com/jigneshgaming/azyron/ui/AzyronModsScreen.class",
            "com/jigneshgaming/azyron/profile/PerformanceEngine.class",
            "com/jigneshgaming/azyron/profile/MemoryOptimizer.class",
        )
        for entry in required:
            assert entry in entries, f"Required client feature missing for {mc}: {entry}"

    return {
        "minecraft": mc,
        "java": java_level,
        "java_class_major": expected_major,
        "client_classes": len(own_classes),
        "configured_mixins": len(configured),
        "display_name": metadata["name"],
    }


def validate_pack(path: Path, mc: str, expected_jar: Path) -> dict:
    embedded_name = f"overrides/mods/Azyron-Client-{VERSION}.jar"
    with zipfile.ZipFile(path) as pack:
        bad = pack.testzip()
        assert bad is None, f"Corrupt MRPack {path}: {bad}"
        index = json.loads(pack.read("modrinth.index.json"))
        assert index.get("versionId") == VERSION, f"Wrong MRPack version in {path}"
        assert index.get("dependencies", {}).get("minecraft") == mc, f"MRPack target mismatch: {path}"
        mod_jars = [name for name in pack.namelist() if name.startswith("overrides/mods/") and name.endswith(".jar")]
        assert mod_jars == [embedded_name], f"Unexpected client JARs in {path}: {mod_jars}"
        embedded = pack.read(embedded_name)
        assert embedded == expected_jar.read_bytes(), f"MRPack does not contain its exact native JAR: {path}"
        with zipfile.ZipFile(io.BytesIO(embedded)) as embedded_jar:
            embedded_meta = json.loads(embedded_jar.read("fabric.mod.json"))
        assert embedded_meta.get("version") == VERSION
        assert embedded_meta.get("depends", {}).get("minecraft") == [mc]
        options = pack.read("overrides/options.txt").decode("utf-8")
        for setting in ("renderDistance:6", "simulationDistance:5", "maxFps:60"):
            assert setting in options, f"Low-RAM profile setting missing for {mc}: {setting}"
        config = json.loads(pack.read("overrides/config/azyron-client.json"))
        assert config.get("configVersion") == VERSION
        assert config.get("selectedMcVersion") == mc
        assert config.get("autoMemoryOptimization") is True
    return {"minecraft": mc, "bytes": path.stat().st_size, "sha256": sha256(path)}


def _write_deterministic_zip(path: Path, files: dict[str, bytes]) -> None:
    fixed_time = (2026, 11, 7, 0, 0, 0)
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name, data in sorted(files.items()):
            info = zipfile.ZipInfo(name, date_time=fixed_time)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.create_system = 3
            info.external_attr = 0o100644 << 16
            archive.writestr(info, data, compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)


def assemble_download_bundles(
    native_jars: dict[str, Path],
    modpacks: dict[str, Path],
    output: Path,
    *,
    run_id: str = "local",
    commit: str = "local",
    target_reports: dict | None = None,
) -> dict:
    """Create one Client ZIP and one Modpack ZIP from two exact native pairs.

    Structural checks here are suitable for synthetic fixture tests. Production
    CI calls validate_native_jar first against the genuine Loom remapJar output.
    """
    assert set(native_jars) == set(VERSIONS), f"Need JARs for both targets: {set(native_jars)}"
    assert set(modpacks) == set(VERSIONS), f"Need packs for both targets: {set(modpacks)}"
    output.mkdir(parents=True, exist_ok=True)
    existing = list(output.iterdir())
    assert not existing, f"Refusing to mix stale files into candidate output: {existing}"

    client_files = {
        "README.txt": (
            f"AZYRON CLIENT v{VERSION}\nDeveloper: JIGNESH GAMING\n\n"
            "This one download contains native Fabric client JARs for both supported Minecraft versions.\n"
            "Extract the archive and install only the JAR in the folder matching your launcher instance.\n"
            "The included JARs are separate native builds, not one cross-version binary.\n"
            "Minecraft 1.20.1 uses Java 17+; Minecraft 1.21.11 uses Java 21+.\n"
            "Game launch, gameplay, renderer, device, and FPS testing are not represented by this bundle.\n"
        ).encode("utf-8")
    }
    pack_files = {
        "README.txt": (
            f"AZYRON CLIENT v{VERSION} Modpack Bundle\nDeveloper: JIGNESH GAMING\n\n"
            "This one download contains two standard Modrinth .mrpack profiles.\n"
            "Import only the .mrpack under the folder matching your launcher instance.\n"
            "The profiles use distinct native Minecraft/Fabric dependencies and client JARs.\n"
            "Minecraft 1.20.1 uses Java 17+; Minecraft 1.21.11 uses Java 21+.\n"
            "Game launch, gameplay, renderer, device, and FPS testing are not represented by this bundle.\n"
        ).encode("utf-8")
    }

    target_details = {}
    for mc in VERSIONS:
        jar_path = Path(native_jars[mc])
        pack_path = Path(modpacks[mc])
        validate_jar_metadata(jar_path, mc)
        validate_pack(pack_path, mc, jar_path)
        client_rel = f"{mc}/Azyron-Client-{VERSION}-mc{mc}.jar"
        pack_rel = f"{mc}/Azyron-Modpack-{VERSION}-mc{mc}.mrpack"
        client_files[client_rel] = jar_path.read_bytes()
        pack_files[pack_rel] = pack_path.read_bytes()
        target_details[mc] = {
            **(target_reports or {}).get(mc, {}),
            "native_jar": {
                "bundle_path": client_rel,
                "bytes": jar_path.stat().st_size,
                "sha256": sha256(jar_path),
            },
            "modpack": {
                "bundle_path": pack_rel,
                "bytes": pack_path.stat().st_size,
                "sha256": sha256(pack_path),
            },
        }

    client_bundle = output / CLIENT_BUNDLE
    modpack_bundle = output / MODPACK_BUNDLE
    _write_deterministic_zip(client_bundle, client_files)
    _write_deterministic_zip(modpack_bundle, pack_files)

    # Re-open the two user downloads and prove they contain only the two expected
    # target payloads plus their instructions; the public page will list only these.
    with zipfile.ZipFile(client_bundle) as archive:
        assert archive.testzip() is None
        assert set(archive.namelist()) == set(client_files)
        for mc in VERSIONS:
            assert archive.read(f"{mc}/Azyron-Client-{VERSION}-mc{mc}.jar") == Path(native_jars[mc]).read_bytes()
    with zipfile.ZipFile(modpack_bundle) as archive:
        assert archive.testzip() is None
        assert set(archive.namelist()) == set(pack_files)
        for mc in VERSIONS:
            assert archive.read(f"{mc}/Azyron-Modpack-{VERSION}-mc{mc}.mrpack") == Path(modpacks[mc]).read_bytes()

    sums = []
    for path in (client_bundle, modpack_bundle):
        sums.append(f"{sha256(path)}  {path.name}")
    (output / "SHA256SUMS.txt").write_text("\n".join(sums) + "\n", encoding="utf-8")

    provenance = {
        "product": "AZYRON CLIENT",
        "developer": "JIGNESH GAMING",
        "version": VERSION,
        "source_commit": commit,
        "ci_run": run_id,
        "native_builds": "separate real Fabric/Loom remapJar builds; no generated stubs",
        "bundle_layout": "one client ZIP and one modpack ZIP; each contains 1.20.1 and 1.21.11 native payloads",
        "game_launch_test": "NOT RUN",
        "gameplay_test": "NOT RUN",
        "device_performance_test": "NOT RUN",
        "targets": target_details,
        "downloads": {
            "client": {"name": CLIENT_BUNDLE, "bytes": client_bundle.stat().st_size, "sha256": sha256(client_bundle)},
            "modpack": {"name": MODPACK_BUNDLE, "bytes": modpack_bundle.stat().st_size, "sha256": sha256(modpack_bundle)},
        },
    }
    (output / "BUILD-PROVENANCE.json").write_text(json.dumps(provenance, indent=2) + "\n", encoding="utf-8")
    return provenance


def prepare(inputs: Path, output: Path, run_id: str, commit: str) -> dict:
    # Validate both genuine CI outputs before creating candidate bundles.
    native_jars = {}
    target_reports = {}
    for mc in VERSIONS:
        matches = list(inputs.rglob(f"Azyron-Client-{VERSION}-mc{mc}.jar"))
        assert len(matches) == 1, f"Need exactly one genuine CI JAR for {mc}: {matches}"
        native_jars[mc] = matches[0]
        target_reports[mc] = validate_native_jar(matches[0], mc)

    builder_spec = importlib.util.spec_from_file_location("modpack_builder", ROOT / "scripts/build-modpack.py")
    builder = importlib.util.module_from_spec(builder_spec)
    builder_spec.loader.exec_module(builder)
    with tempfile.TemporaryDirectory(prefix="azyron-v104-packs-") as temp_dir:
        packs = {}
        for mc in VERSIONS:
            pack = Path(temp_dir) / f"Azyron-Modpack-{VERSION}-mc{mc}.mrpack"
            builder.build_mrpack(str(native_jars[mc]), str(pack), mc)
            validate_pack(pack, mc, native_jars[mc])
            packs[mc] = pack
        result = assemble_download_bundles(
            native_jars,
            packs,
            output,
            run_id=run_id,
            commit=commit,
            target_reports=target_reports,
        )

    # The candidate folder contains exactly the two requested downloads and two
    # machine-readable verification sidecars—no extra per-version public assets.
    print(json.dumps(result, indent=2))
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--inputs", type=Path, required=True, help="CI artifacts containing both native remapped JARs")
    parser.add_argument("--out", type=Path, required=True, help="Empty directory for two download bundles + provenance")
    parser.add_argument("--run-id", required=True)
    parser.add_argument("--commit", required=True)
    args = parser.parse_args()
    prepare(args.inputs, args.out, args.run_id, args.commit)
