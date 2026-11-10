#!/usr/bin/env python3
"""Prepare native candidates ONLY from a successful dual-version real Fabric CI job.
No release API calls, uploads or publication. Never reads releases/ legacy test files.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import shutil
import struct
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSIONS = ("1.20.1", "1.21.11")


def validate_jar(path, mc):
    with zipfile.ZipFile(path) as jar:
        assert jar.testzip() is None, f"Corrupt JAR: {path}"
        mod = json.loads(jar.read("fabric.mod.json"))
        assert mod["id"] == "azyron-client" and mod["version"] == "1.0.3"
        assert mod["depends"]["minecraft"] == [mc], "Not the exact native target"
        assert mod["depends"]["java"] == (">=17" if mc == "1.20.1" else ">=21")
        assert "JIGNESH GAMING" in mod["authors"]
        entries = jar.namelist()
        own = [n for n in entries if n.startswith("com/jigneshgaming/azyron/") and n.endswith(".class")]
        assert len(own) >= 50, "Incomplete client"
        assert not any(n.startswith("net/minecraft/") for n in entries), "Minecraft/test stubs bundled"
        expected_major = 61 if mc == "1.20.1" else 65
        for name in own:
            magic, minor, major = struct.unpack(">IHH", jar.read(name)[:8])
            assert magic == 0xCAFEBABE and major == expected_major, (name, major)
        config = json.loads(jar.read("azyron-client.mixins.json"))
        for group in ("mixins", "client"):
            for simple in config[group]:
                name = config["package"].replace(".", "/") + "/" + simple + ".class"
                assert name in entries, f"Configured mixin missing: {name}"
                assert mc == "1.20.1" or not simple.endswith("120Mixin")
        assert "com/jigneshgaming/azyron/module/impl/ArmorHudModule.class" in entries
        assert "com/jigneshgaming/azyron/ui/AzyronModsScreen.class" in entries
        return {"minecraft": mc, "classes": len(own), "java_class_major": expected_major}


def prepare(inputs, output, run_id, commit):
    # Validate BOTH inputs before writing a single release candidate.
    jars = {}
    reports = {}
    for mc in VERSIONS:
        matches = list(inputs.rglob(f"Azyron-Client-1.0.3-mc{mc}.jar"))
        assert len(matches) == 1, f"Need exactly one genuine CI JAR for {mc}: {matches}"
        jars[mc] = matches[0]
        reports[mc] = validate_jar(matches[0], mc)
    output.mkdir(parents=True, exist_ok=True)
    spec = importlib.util.spec_from_file_location("modpack_builder", ROOT / "scripts/build-modpack.py")
    builder = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(builder)
    checksums = []
    for mc, source in jars.items():
        jar = output / f"Azyron-Client-1.0.3-mc{mc}.jar"
        shutil.copyfile(source, jar)
        pack = output / f"Azyron-Modpack-1.0.3-mc{mc}.mrpack"
        builder.build_mrpack(str(jar), str(pack), mc)
        with zipfile.ZipFile(pack) as z:
            assert z.testzip() is None
            assert z.read("overrides/mods/Azyron-Client-1.0.3.jar") == jar.read_bytes()
            assert json.loads(z.read("modrinth.index.json"))["dependencies"]["minecraft"] == mc
            cfg = json.loads(z.read("overrides/config/azyron-client.json"))
            assert cfg["selectedMcVersion"] == mc
        reports[mc]["files"] = []
        for path in (jar, pack):
            sha = hashlib.sha256(path.read_bytes()).hexdigest()
            checksums.append(f"{sha}  {path.name}")
            reports[mc]["files"].append({"name": path.name, "bytes": path.stat().st_size, "sha256": sha})
    (output / "SHA256SUMS.txt").write_text("\n".join(checksums) + "\n")
    result = {"developer": "JIGNESH GAMING", "version": "1.0.3", "commit": commit,
              "ci_run": run_id, "source": "real Fabric/Loom remapJar; no generated stubs",
              "publication": "NOT PUBLISHED; owner approval required", "hardware_fps_test": "NOT RUN",
              "targets": reports}
    (output / "BUILD-PROVENANCE.json").write_text(json.dumps(result, indent=2) + "\n")
    (output / "README.txt").write_text(
        "Azyron Client v1.0.3 / Azyron Modpack v1.0.3\nDeveloper: JIGNESH GAMING\n"
        "Native candidates, one JAR + one MRPack per Minecraft version. Do not mix them.\n"
        "Built against real Minecraft/Fabric; structural compatibility checks are not gameplay testing.\n"
        "No 3GB/60+ FPS hardware results. Render 6, conservative simulation 5.\n"
        "Keep release draft. Do not publish without owner approval.\n")
    print(json.dumps(result, indent=2))
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--inputs", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--run-id", required=True)
    parser.add_argument("--commit", required=True)
    args = parser.parse_args()
    prepare(args.inputs, args.out, args.run_id, args.commit)
