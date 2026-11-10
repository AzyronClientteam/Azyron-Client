#!/usr/bin/env python3
"""Attach verified native candidates to the existing v1.0.3 draft only.

This script is deliberately not a release publisher: it refuses a non-draft or
wrong-tag release, verifies the CI provenance and every candidate, uploads the
version-specific pairs plus checksums/provenance, then removes only the two
previously identified stale stub assets. The release is explicitly kept draft.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import zipfile

EXPECTED = {
    "Azyron-Client-1.0.3-mc1.20.1.jar": ("1.20.1", 61),
    "Azyron-Modpack-1.0.3-mc1.20.1.mrpack": ("1.20.1", None),
    "Azyron-Client-1.0.3-mc1.21.11.jar": ("1.21.11", 65),
    "Azyron-Modpack-1.0.3-mc1.21.11.mrpack": ("1.21.11", None),
}
OLD_ASSETS = {
    "Azyron-Client-1.0.3.jar": {
        "id": 729999833,
        "size": 209374,
        "digest": "sha256:ffcf394824b934c3ac44dce736e698c2688f27c66654a0c1fc8c66af855548da",
    },
    "Azyron-Modpack-1.0.3.mrpack": {
        "id": 729999953,
        "size": 208140,
        "digest": "sha256:7621755721089e2f0940004e592aa4205a40078ad2ccfd9f9c5c19fe1b8606ee",
    },
}


def gh(args, *, stdin=None):
    result = subprocess.run(
        ["gh", *args], input=stdin, text=True, stdout=subprocess.PIPE,
        stderr=subprocess.PIPE, check=False,
    )
    if result.returncode:
        raise RuntimeError(f"gh {' '.join(args)} failed ({result.returncode}): {result.stderr.strip()}")
    return result.stdout


def get_release(repo, release_id):
    return json.loads(gh(["api", f"repos/{repo}/releases/{release_id}"]))


def require_draft(release, release_id, tag):
    if release.get("id") != release_id or release.get("tag_name") != tag:
        raise RuntimeError(
            f"Release identity mismatch: id={release.get('id')} tag={release.get('tag_name')}"
        )
    if release.get("draft") is not True:
        raise RuntimeError("Release is not a draft; refusing any asset or metadata changes.")


def file_sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def validate_bundle(root, run_id, commit):
    provenance_path = root / "BUILD-PROVENANCE.json"
    sums_path = root / "SHA256SUMS.txt"
    provenance = json.loads(provenance_path.read_text(encoding="utf-8"))
    if provenance.get("developer") != "JIGNESH GAMING" or provenance.get("version") != "1.0.3":
        raise RuntimeError("Candidate provenance has unexpected product/developer metadata.")
    if str(provenance.get("ci_run")) != str(run_id) or provenance.get("commit") != commit:
        raise RuntimeError("Candidate provenance does not match this successful CI run and commit.")
    if provenance.get("source") != "real Fabric/Loom remapJar; no generated stubs":
        raise RuntimeError("Refusing candidates without real Fabric/Loom provenance.")
    targets = provenance.get("targets", {})
    if set(targets) != {"1.20.1", "1.21.11"}:
        raise RuntimeError(f"Unexpected native targets in provenance: {sorted(targets)}")

    hashes = {}
    for line in sums_path.read_text(encoding="utf-8").splitlines():
        match = re.fullmatch(r"([0-9a-f]{64})\s+\*?(.+)", line)
        if not match:
            raise RuntimeError(f"Malformed SHA256SUMS.txt line: {line!r}")
        digest, name = match.groups()
        if name in hashes:
            raise RuntimeError(f"Duplicate checksum entry: {name}")
        hashes[name] = digest
    if set(hashes) != set(EXPECTED):
        raise RuntimeError(f"Unexpected candidate checksum set: {sorted(hashes)}")

    for name, (version, class_major) in EXPECTED.items():
        path = root / name
        if not path.is_file() or file_sha(path) != hashes[name]:
            raise RuntimeError(f"Missing candidate or SHA-256 mismatch: {name}")
        target = targets[version]
        entry = next((item for item in target.get("files", []) if item.get("name") == name), None)
        if not entry or entry.get("sha256") != hashes[name] or entry.get("bytes") != path.stat().st_size:
            raise RuntimeError(f"Candidate disagrees with provenance: {name}")
        with zipfile.ZipFile(path) as archive:
            if archive.testzip() is not None:
                raise RuntimeError(f"Corrupt candidate ZIP/JAR: {name}")
            if name.endswith(".jar"):
                mod = json.loads(archive.read("fabric.mod.json"))
                if mod.get("depends", {}).get("minecraft") != [version]:
                    raise RuntimeError(f"Wrong native Minecraft target in {name}")
                if mod.get("depends", {}).get("java") != (">=17" if version == "1.20.1" else ">=21"):
                    raise RuntimeError(f"Wrong Java target in {name}")
                if any(item.startswith("net/minecraft/") for item in archive.namelist()):
                    raise RuntimeError(f"Minecraft/test stubs are bundled in {name}")
                classes = [item for item in archive.namelist()
                           if item.startswith("com/jigneshgaming/azyron/") and item.endswith(".class")]
                if len(classes) != target.get("classes"):
                    raise RuntimeError(f"Native client class count mismatch in {name}")
                for class_name in classes:
                    data = archive.read(class_name)
                    if len(data) < 8 or data[:4] != b"\xca\xfe\xba\xbe" or int.from_bytes(data[6:8], "big") != class_major:
                        raise RuntimeError(f"Unexpected class-file target in {name}: {class_name}")
            else:
                index = json.loads(archive.read("modrinth.index.json"))
                if index.get("dependencies", {}).get("minecraft") != version:
                    raise RuntimeError(f"Wrong native Minecraft target in {name}")
                jar_name = f"Azyron-Client-1.0.3-mc{version}.jar"
                if archive.read("overrides/mods/Azyron-Client-1.0.3.jar") != (root / jar_name).read_bytes():
                    raise RuntimeError(f"MRPack does not contain its matching native JAR: {name}")

    if not (root / "README.txt").is_file():
        raise RuntimeError("Candidate artifact is missing its README/provenance context.")
    return provenance, hashes


def validate_prior_assets(release):
    by_name = {}
    for asset in release.get("assets", []):
        by_name.setdefault(asset.get("name"), []).append(asset)
    found = {}
    for name, expected in OLD_ASSETS.items():
        entries = by_name.get(name, [])
        if not entries:
            continue
        if len(entries) != 1:
            raise RuntimeError(f"Ambiguous legacy release asset {name}; refusing cleanup.")
        asset = entries[0]
        actual = {key: asset.get(key) for key in expected}
        if actual != expected:
            raise RuntimeError(f"Legacy asset no longer matches known stale file; refusing cleanup: {name}: {actual}")
        found[name] = asset
    return found


def release_body(provenance, hashes, run_id, commit):
    return f"""# Azyron Client v1.0.3 / Azyron Modpack v1.0.3

**Developer: JIGNESH GAMING**

> **DRAFT — NOT PUBLISHED.** This is the existing v1.0.3 release. The version-specific native files below are attached for review; publication still requires the owner's approval.

## Downloads

Use the JAR and MRPack for the same Minecraft version. The old generic-name stub assets have been removed to avoid confusion.

| Minecraft | Client JAR | Modpack | SHA-256 (JAR) | SHA-256 (MRPack) |
|---|---|---|---|---|
| 1.20.1 Fabric | `Azyron-Client-1.0.3-mc1.20.1.jar` | `Azyron-Modpack-1.0.3-mc1.20.1.mrpack` | `{hashes['Azyron-Client-1.0.3-mc1.20.1.jar']}` | `{hashes['Azyron-Modpack-1.0.3-mc1.20.1.mrpack']}` |
| 1.21.11 Fabric | `Azyron-Client-1.0.3-mc1.21.11.jar` | `Azyron-Modpack-1.0.3-mc1.21.11.mrpack` | `{hashes['Azyron-Client-1.0.3-mc1.21.11.jar']}` | `{hashes['Azyron-Modpack-1.0.3-mc1.21.11.mrpack']}` |

`SHA256SUMS.txt` and `BUILD-PROVENANCE.json` are also attached. CI verifies each uploaded asset against its SHA-256 before removing the previous stale files.

## Build validation

- Minecraft **1.20.1 Fabric**: standard Gradle `build` passed; 105 actual-classpath compatibility checks passed; Java 17 output.
- Minecraft **1.21.11 Fabric**: standard Gradle `build` passed; 110 actual-classpath compatibility checks passed; Java 21 output.
- Eight packaging/installer regression tests passed.
- [Successful CI run {run_id}](https://github.com/AzyronClientteam/Azyron-Client/actions/runs/{run_id}) — validated source commit `{commit}`.
- Builds used real Minecraft/Yarn/Fabric dependencies, not generated stubs. Version-specific adapters preserve native APIs while shared HUD, Armour Hub, Mods Menu, input, cosmetics and FPS code remain in the project.
- [Complete compatibility report](https://github.com/AzyronClientteam/Azyron-Client/blob/arena/a6dbb848-azyron-client/azyron-client/COMPATIBILITY_1201_BUILD_REPORT.md).

## Testing limits

Actual game startup/full Mixin transformation, physical-device gameplay, Android/Pojav compatibility, renderer interoperability, 3GB hardware RAM/thermal behavior and FPS benchmarking are **NOT TESTED**. No 60+ FPS, crash-free or measured performance claim is made. The conservative profile is render distance 6 / simulation distance 5; simulation 4 is not claimed, and multiplayer simulation remains server-controlled.

**This release remains a draft. Do not publish without the owner's approval.**
"""


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--candidates", type=Path, required=True)
    parser.add_argument("--repo", required=True)
    parser.add_argument("--release-id", type=int, required=True)
    parser.add_argument("--tag", required=True)
    parser.add_argument("--run-id", required=True)
    parser.add_argument("--commit", required=True)
    args = parser.parse_args()
    if not os.environ.get("GH_TOKEN"):
        raise RuntimeError("GH_TOKEN is required for the authorized draft update.")

    root = args.candidates.resolve()
    provenance, hashes = validate_bundle(root, args.run_id, args.commit)
    release = get_release(args.repo, args.release_id)
    require_draft(release, args.release_id, args.tag)
    legacy_assets = validate_prior_assets(release)

    upload_names = [*EXPECTED, "SHA256SUMS.txt", "BUILD-PROVENANCE.json"]
    upload_paths = [str(root / name) for name in upload_names]
    gh(["release", "upload", args.tag, *upload_paths, "--clobber", "--repo", args.repo])

    # Only proceed to remove old assets after GitHub's own asset API confirms
    # that every replacement and both verification records have the exact bytes.
    release = get_release(args.repo, args.release_id)
    require_draft(release, args.release_id, args.tag)
    uploaded = {}
    for asset in release.get("assets", []):
        if asset.get("name") in upload_names:
            if asset["name"] in uploaded:
                raise RuntimeError(f"Duplicate uploaded release asset: {asset['name']}")
            uploaded[asset["name"]] = asset
    if set(uploaded) != set(upload_names):
        raise RuntimeError(f"Release asset upload incomplete: {sorted(set(upload_names)-set(uploaded))}")
    local_hashes = {name: file_sha(root / name) for name in upload_names}
    for name, asset in uploaded.items():
        expected_size = (root / name).stat().st_size
        expected_digest = "sha256:" + local_hashes[name]
        if asset.get("size") != expected_size or asset.get("digest") != expected_digest:
            raise RuntimeError(
                f"Uploaded asset failed API size/digest verification: {name}; "
                f"got size={asset.get('size')} digest={asset.get('digest')}, "
                f"expected size={expected_size} digest={expected_digest}"
            )

    for name, asset in legacy_assets.items():
        gh(["api", "--method", "DELETE", f"repos/{args.repo}/releases/assets/{asset['id']}"])

    body = release_body(provenance, hashes, args.run_id, args.commit)
    payload = json.dumps({"name": "Azyron Client v1.0.3", "body": body, "draft": True})
    updated = json.loads(gh([
        "api", "--method", "PATCH", f"repos/{args.repo}/releases/{args.release_id}", "--input", "-"
    ], stdin=payload))
    require_draft(updated, args.release_id, args.tag)

    final = get_release(args.repo, args.release_id)
    require_draft(final, args.release_id, args.tag)
    final_assets = {asset.get("name") for asset in final.get("assets", [])}
    if not set(upload_names).issubset(final_assets):
        raise RuntimeError("Final release metadata is missing one or more verified assets.")
    if set(legacy_assets).intersection(final_assets):
        raise RuntimeError("A stale generic-name asset remains attached to the draft.")
    print(f"Updated existing release {args.release_id} ({args.tag}); draft={final['draft']}.")
    print(f"Attached {len(upload_names)} verified assets; removed {len(legacy_assets)} verified stale assets.")
    print(final.get("html_url", ""))


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(f"ERROR: {error}", file=sys.stderr)
        raise
