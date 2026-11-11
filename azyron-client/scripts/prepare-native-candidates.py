#!/usr/bin/env python3
"""Validate real Fabric builds and stage four direct v1.0.4 native assets.

Minecraft 1.21.11 is the default target and uses the unqualified filenames.
Minecraft 1.20.1 retains an explicit target suffix. Each MRPack is a standard,
single-version Modrinth profile. This script emits no client or modpack bundle ZIP.
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import shutil
import struct
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = "1.0.4"
VERSIONS = ("1.20.1", "1.21.11")
CLASS_TARGETS = {"1.20.1": (17, 61), "1.21.11": (21, 65)}


def client_jar_name(mc: str) -> str:
    if mc == "1.21.11":
        return f"Azyron-Client-{VERSION}.jar"
    if mc == "1.20.1":
        return f"Azyron-Client-{VERSION}-mc1.20.1.jar"
    raise ValueError(f"Unsupported Minecraft target: {mc}")


def modpack_name(mc: str) -> str:
    if mc == "1.21.11":
        return f"Azyron-Modpack-{VERSION}.mrpack"
    if mc == "1.20.1":
        return f"Azyron-Modpack-{VERSION}-mc1.20.1.mrpack"
    raise ValueError(f"Unsupported Minecraft target: {mc}")


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
        for name in own_classes:
            magic, _minor, major = struct.unpack(">IHH", jar.read(name)[:8])
            assert magic == 0xCAFEBABE, f"Invalid class file: {name}"
            assert major == expected_major, (name, major, expected_major)

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


def assemble_release_assets(
    native_jars: dict[str, Path],
    modpacks: dict[str, Path],
    output: Path,
    *,
    run_id: str = "local",
    commit: str = "local",
    target_reports: dict | None = None,
) -> dict:
    """Stage two target-specific compiled JARs and two standard .mrpack files.

    The 1.21.11 files use the requested unqualified names; the 1.20.1 files
    carry an explicit target suffix. The function never renames a ZIP to .jar.
    """
    assert set(native_jars) == set(VERSIONS), f"Need native JARs for both targets: {set(native_jars)}"
    assert set(modpacks) == set(VERSIONS), f"Need MRpacks for both targets: {set(modpacks)}"
    output.mkdir(parents=True, exist_ok=True)
    existing = list(output.iterdir())
    assert not existing, f"Refusing to mix stale files into candidate output: {existing}"

    target_details = {}
    release_files = []
    for mc in VERSIONS:
        source_jar = Path(native_jars[mc])
        source_pack = Path(modpacks[mc])
        validate_jar_metadata(source_jar, mc)
        validate_pack(source_pack, mc, source_jar)
        jar_path = output / client_jar_name(mc)
        pack_path = output / modpack_name(mc)
        shutil.copyfile(source_jar, jar_path)
        shutil.copyfile(source_pack, pack_path)
        # Validate the actual files at their final download paths, not only inputs.
        validate_jar_metadata(jar_path, mc)
        validate_pack(pack_path, mc, jar_path)
        release_files.extend((jar_path, pack_path))
        target_details[mc] = {
            **(target_reports or {}).get(mc, {}),
            "native_jar": {
                "name": jar_path.name,
                "bytes": jar_path.stat().st_size,
                "sha256": sha256(jar_path),
            },
            "mrpack": {
                "name": pack_path.name,
                "bytes": pack_path.stat().st_size,
                "sha256": sha256(pack_path),
            },
        }

    names = [path.name for path in release_files]
    assert len(names) == 4 and len(set(names)) == 4
    assert not any(name.endswith(".zip") for name in names), f"ZIP release assets are prohibited: {names}"
    sums = [f"{sha256(path)}  {path.name}" for path in sorted(release_files, key=lambda p: p.name)]
    (output / "SHA256SUMS.txt").write_text("\n".join(sums) + "\n", encoding="utf-8")
    (output / "README.txt").write_text(
        f"AZYRON CLIENT v{VERSION} — direct native release assets\n"
        "Developer: JIGNESH GAMING\n\n"
        "1.21.11 Fabric (Java 21+): Azyron-Client-1.0.4.jar and Azyron-Modpack-1.0.4.mrpack\n"
        "1.20.1 Fabric (Java 17+): Azyron-Client-1.0.4-mc1.20.1.jar and "
        "Azyron-Modpack-1.0.4-mc1.20.1.mrpack\n\n"
        "Each .mrpack is a standard single-Minecraft-version Modrinth profile.\n"
        "No client ZIP is supplied. Builds are not gameplay, device, or FPS tests.\n",
        encoding="utf-8",
    )
    result = {
        "product": "AZYRON CLIENT",
        "developer": "JIGNESH GAMING",
        "version": VERSION,
        "source_commit": commit,
        "ci_run": str(run_id),
        "native_builds": "separate real Fabric/Loom remapJar builds; no generated stubs",
        "release_format": "four direct assets: one native JAR and one single-version MRPack per target; no ZIP client bundle",
        "game_launch_test": "NOT RUN",
        "gameplay_test": "NOT RUN",
        "device_performance_test": "NOT RUN",
        "targets": target_details,
        "assets": {
            path.name: {"name": path.name, "bytes": path.stat().st_size, "sha256": sha256(path)}
            for path in release_files
        },
    }
    (output / "BUILD-PROVENANCE.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    return result


def prepare(inputs: Path, output: Path, run_id: str, commit: str) -> dict:
    native_jars = {}
    target_reports = {}
    for mc in VERSIONS:
        expected = client_jar_name(mc)
        matches = list(inputs.rglob(expected))
        assert len(matches) == 1, f"Need exactly one Gradle/Loom JAR for {mc} ({expected}): {matches}"
        native_jars[mc] = matches[0]
        target_reports[mc] = validate_native_jar(matches[0], mc)

    builder_spec = importlib.util.spec_from_file_location("modpack_builder", ROOT / "scripts/build-modpack.py")
    builder = importlib.util.module_from_spec(builder_spec)
    builder_spec.loader.exec_module(builder)
    with tempfile.TemporaryDirectory(prefix="azyron-v104-mrpacks-") as temp_dir:
        packs = {}
        for mc in VERSIONS:
            pack = Path(temp_dir) / modpack_name(mc)
            builder.build_mrpack(str(native_jars[mc]), str(pack), mc)
            validate_pack(pack, mc, native_jars[mc])
            packs[mc] = pack
        result = assemble_release_assets(
            native_jars,
            packs,
            output,
            run_id=run_id,
            commit=commit,
            target_reports=target_reports,
        )

    print(json.dumps(result, indent=2))
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--inputs", type=Path, required=True, help="CI artifacts with both native remapped JARs")
    parser.add_argument("--out", type=Path, required=True, help="Empty directory for four direct native assets and sidecars")
    parser.add_argument("--run-id", required=True)
    parser.add_argument("--commit", required=True)
    args = parser.parse_args()
    prepare(args.inputs, args.out, args.run_id, args.commit)
