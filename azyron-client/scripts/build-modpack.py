#!/usr/bin/env python3
"""
AZYRON CLIENT — Reproducible Modrinth (.mrpack) Builder for Minecraft 1.21.11 (Fabric 0.20.1)
Packages modpack/modrinth.index.json, modpack/icon.png, modpack/overrides/config/*,
and the compiled AZYRON CLIENT 1.0.2 JAR into releases/Azyron-Modpack-1.0.2.mrpack.
"""

import json
import os
import sys
import zipfile

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
CLIENT_ROOT = os.path.dirname(SCRIPT_DIR)
MODPACK_SRC = os.path.join(CLIENT_ROOT, "modpack")
RELEASES_DIR = os.path.join(CLIENT_ROOT, "releases")


def build_mrpack(jar_path: str, out_path: str) -> None:
    index_path = os.path.join(MODPACK_SRC, "modrinth.index.json")
    icon_path = os.path.join(MODPACK_SRC, "icon.png")
    overrides_dir = os.path.join(MODPACK_SRC, "overrides")

    for req in (index_path, icon_path, overrides_dir, jar_path):
        if not os.path.exists(req):
            raise FileNotFoundError(f"Missing required modpack input: {req}")

    with open(index_path, "r", encoding="utf-8") as fh:
        idx = json.load(fh)
    deps = idx.get("dependencies", {})
    if deps.get("minecraft") != "1.21.11":
        raise ValueError("modrinth.index.json must target Minecraft 1.21.11 strictly")
    if deps.get("fabric-loader") != "0.20.1":
        raise ValueError("modrinth.index.json must target Fabric Loader 0.20.1")

    seen_paths = set()
    for f in idx.get("files", []):
        p = f.get("path", "")
        if p in seen_paths:
            raise ValueError(f"Duplicate dependency entry in modrinth.index.json: {p}")
        seen_paths.add(p)

    # Verify bundled client JAR has expanded version 1.0.2 (no literal ${version})
    with zipfile.ZipFile(jar_path, "r") as jf:
        fmj = json.loads(jf.read("fabric.mod.json").decode("utf-8"))
        if "${" in json.dumps(fmj) or fmj.get("version") != "1.0.2":
            raise ValueError(f"Invalid fabric.mod.json version inside {jar_path}: {fmj.get('version')}")

    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with zipfile.ZipFile(out_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
        zf.write(index_path, "modrinth.index.json")
        zf.write(jar_path, "overrides/mods/Azyron-Client-1.0.2.jar")
        zf.write(icon_path, "icon.png")
        for root, _, files in sorted(os.walk(overrides_dir)):
            for fname in sorted(files):
                full_p = os.path.join(root, fname)
                rel_p = os.path.relpath(full_p, MODPACK_SRC).replace(os.sep, "/")
                if rel_p.startswith("overrides/mods/"):
                    continue
                zf.write(full_p, rel_p)

    with zipfile.ZipFile(out_path, "r") as zf:
        bad = zf.testzip()
        if bad is not None:
            raise RuntimeError(f"Corrupt entry in generated mrpack: {bad}")
        mod_jars = [n for n in zf.namelist() if n.startswith("overrides/mods/") and n.endswith(".jar")]
        if mod_jars != ["overrides/mods/Azyron-Client-1.0.2.jar"]:
            raise RuntimeError(f"Unexpected overrides/mods contents: {mod_jars}")
    print(f"Built verified mrpack: {out_path} ({os.path.getsize(out_path)} bytes)")


if __name__ == "__main__":
    default_jar = os.path.join(RELEASES_DIR, "Azyron-Client-1.0.2.jar")
    default_out = os.path.join(RELEASES_DIR, "Azyron-Modpack-1.0.2.mrpack")
    jar_arg = sys.argv[1] if len(sys.argv) > 1 else default_jar
    out_arg = sys.argv[2] if len(sys.argv) > 2 else default_out
    build_mrpack(jar_arg, out_arg)
