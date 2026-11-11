#!/usr/bin/env python3
"""
AZYRON CLIENT — Reproducible Modrinth (.mrpack) Builder for v1.0.4
Builds separate standard Modrinth packs for native Minecraft targets:
  - Minecraft 1.21.11 (Fabric 0.20.1)
  - Minecraft 1.20.1  (Fabric 0.14.22+) [--mc 1.20.1]
Each pack embeds the matching native client JAR and profile. A manifest edit cannot
convert a native JAR to a different Minecraft version.
"""

import argparse
import json
import os
import zipfile

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
CLIENT_ROOT = os.path.dirname(SCRIPT_DIR)
MODPACK_SRC = os.path.join(CLIENT_ROOT, "modpack")


def validate_index(index_path: str, expected_mc: str) -> None:
    with open(index_path, "r", encoding="utf-8") as fh:
        idx = json.load(fh)
    if idx.get("versionId") != "1.0.4":
        raise ValueError(f"{index_path} must declare versionId 1.0.4 (got {idx.get('versionId')})")
    deps = idx.get("dependencies", {})
    if deps.get("minecraft") != expected_mc:
        raise ValueError(f"{index_path} must target Minecraft {expected_mc} (got {deps.get('minecraft')})")
    seen_paths = set()
    for f in idx.get("files", []):
        p = f.get("path", "")
        if p in seen_paths:
            raise ValueError(f"Duplicate dependency entry in {index_path}: {p}")
        seen_paths.add(p)
        hashes = f.get("hashes", {})
        if len(hashes.get("sha1", "")) != 40 or len(hashes.get("sha512", "")) != 128:
            raise ValueError(f"Missing valid sha1/sha512 in {index_path} for {p}")


def default_native_jar_path(mc_version: str) -> str:
    if mc_version not in ("1.20.1", "1.21.11"):
        raise ValueError("Unsupported Minecraft version")
    filename = "Azyron-Client-1.0.4.jar" if mc_version == "1.21.11" else "Azyron-Client-1.0.4-mc1.20.1.jar"
    return os.path.join(CLIENT_ROOT, "build", "libs", filename)


def default_modpack_path(mc_version: str) -> str:
    if mc_version not in ("1.20.1", "1.21.11"):
        raise ValueError("Unsupported Minecraft version")
    filename = "Azyron-Modpack-1.0.4.mrpack" if mc_version == "1.21.11" else "Azyron-Modpack-1.0.4-mc1.20.1.mrpack"
    return os.path.join(CLIENT_ROOT, "build", "modpack-profiles", filename)


def build_mrpack(jar_path: str, out_path: str, mc_version: str = "1.21.11") -> None:
    index_121 = os.path.join(MODPACK_SRC, "modrinth.index.json")
    index_120 = os.path.join(MODPACK_SRC, "modrinth.index.1.20.1.json")
    icon_path = os.path.join(MODPACK_SRC, "icon.png")
    overrides_dir = os.path.join(MODPACK_SRC, "overrides")

    for req in (index_121, index_120, icon_path, overrides_dir, jar_path):
        if not os.path.exists(req):
            raise FileNotFoundError(f"Missing required modpack input: {req}")

    validate_index(index_121, "1.21.11")
    validate_index(index_120, "1.20.1")

    if mc_version not in ("1.20.1", "1.21.11"):
        raise ValueError("Unsupported Minecraft version")
    active_index = index_120 if mc_version == "1.20.1" else index_121
    profile_dirs = {v: os.path.join(MODPACK_SRC, "profiles", v, "overrides") for v in ("1.20.1", "1.21.11")}
    for version, directory in profile_dirs.items():
        if not os.path.isfile(os.path.join(directory, "options.txt")):
            raise FileNotFoundError(f"Missing low-RAM profile: {version}")

    # Require the native target: never pair a 1.21.11 JAR with a 1.20.1 manifest.
    with zipfile.ZipFile(jar_path, "r") as jf:
        fmj = json.loads(jf.read("fabric.mod.json").decode("utf-8"))
        if "${" in json.dumps(fmj) or fmj.get("version") != "1.0.4":
            raise ValueError(f"Invalid fabric.mod.json version inside {jar_path}: {fmj.get('version')}")
        mc_dep = fmj.get("depends", {}).get("minecraft")
        if mc_dep != [mc_version]:
            raise ValueError(f"Native client JAR must declare only [{mc_version!r}], got {mc_dep}")

    os.makedirs(os.path.dirname(os.path.abspath(out_path)), exist_ok=True)
    with zipfile.ZipFile(out_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
        zf.write(active_index, "modrinth.index.json")
        zf.write(active_index, f"profiles/modrinth.index.{mc_version}.json")
        zf.write(jar_path, "overrides/mods/Azyron-Client-1.0.4.jar")
        zf.write(icon_path, "icon.png")
        # Only the client config is shared. Companion schemas must not leak from
        # one MC version to the other; unsupported optimizers use upstream defaults.
        for root, _, files in sorted(os.walk(overrides_dir)):
            for fname in sorted(files):
                full_p = os.path.join(root, fname)
                rel_p = os.path.relpath(full_p, MODPACK_SRC).replace(os.sep, "/")
                if rel_p.startswith("overrides/config/") or rel_p.startswith("overrides/mods/"):
                    continue
                if rel_p == "overrides/options.txt":
                    continue
                zf.write(full_p, rel_p)
        for version, directory in profile_dirs.items():
            if version != mc_version:
                continue
            for root, _, files in sorted(os.walk(directory)):
                for fname in sorted(files):
                    full_p = os.path.join(root, fname)
                    rel_p = os.path.relpath(full_p, directory).replace(os.sep, "/")
                    zf.write(full_p, f"profiles/{version}/overrides/{rel_p}")
                    if version == mc_version:
                        zf.write(full_p, f"overrides/{rel_p}")

    with zipfile.ZipFile(out_path, "r") as zf:
        bad = zf.testzip()
        if bad is not None:
            raise RuntimeError(f"Corrupt entry in generated mrpack: {bad}")
        mod_jars = [n for n in zf.namelist() if n.startswith("overrides/mods/") and n.endswith(".jar")]
        if mod_jars != ["overrides/mods/Azyron-Client-1.0.4.jar"]:
            raise RuntimeError(f"Unexpected overrides/mods contents: {mod_jars}")
    print(f"Built structurally checked mrpack (MC {mc_version}): {out_path} ({os.path.getsize(out_path)} bytes)")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Build a version-specific Azyron Modrinth pack")
    parser.add_argument("jar", nargs="?", default=None,
                        help="Native client JAR (default: matching build/libs JAR)")
    parser.add_argument("out", nargs="?", default=None,
                        help="Output path (default: target-specific file under build/modpack-profiles/)")
    parser.add_argument("--mc", choices=["1.21.11", "1.20.1"], default="1.21.11",
                        help="Minecraft target for both the native JAR and Modrinth index")
    args = parser.parse_args()
    jar = args.jar or default_native_jar_path(args.mc)
    out = args.out or default_modpack_path(args.mc)
    build_mrpack(jar, out, args.mc)
