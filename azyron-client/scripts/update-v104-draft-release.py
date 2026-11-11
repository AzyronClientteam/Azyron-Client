#!/usr/bin/env python3
"""Safely replace the v1.0.4 draft's obsolete ZIP bundles with native assets.

This updater is pinned to the existing v1.0.4 draft release ID. It never creates
or publishes a release, and cannot modify the separate v1.0.3 draft. New assets
are uploaded and verified through GitHub's release API before the two known old
ZIP assets are removed.
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import zipfile

VERSION = "1.0.4"
TAG = "v1.0.4"
REPOSITORY = "AzyronClientteam/Azyron-Client"
RELEASE_ID = 441244221
TARGETS = ("1.20.1", "1.21.11")
ROOT = Path(__file__).resolve().parents[1]

spec = importlib.util.spec_from_file_location(
    "azyron_native_candidates", ROOT / "scripts/prepare-native-candidates.py"
)
if spec is None or spec.loader is None:
    raise RuntimeError("Could not load the native candidate validator")
native = importlib.util.module_from_spec(spec)
spec.loader.exec_module(native)

ASSET_TARGETS = {
    native.client_jar_name(mc): (mc, "jar") for mc in TARGETS
}
ASSET_TARGETS.update({
    native.modpack_name(mc): (mc, "mrpack") for mc in TARGETS
})
SIDECARS = {"SHA256SUMS.txt", "BUILD-PROVENANCE.json"}
CANDIDATE_FILES = set(ASSET_TARGETS) | SIDECARS | {"README.txt"}
UPLOAD_FILES = set(ASSET_TARGETS) | SIDECARS
LEGACY_ASSETS = {
    "Azyron-Client-v1.0.4.zip": {
        "id": 734143750,
        "size": 411668,
        "digest": "sha256:1c2c0282703120cc7877328cf0d2a37653d0f82bbb83477d8775a1c99b7e1226",
    },
    "Azyron-Modpack-v1.0.4.zip": {
        "id": 734143749,
        "size": 445810,
        "digest": "sha256:83fe62742128c79b13572fadb496a3fc2d297ace4c11467ffe3647e68309d8d4",
    },
}


def file_sha(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(128 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def parse_checksums(path: Path) -> dict[str, str]:
    hashes = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        match = re.fullmatch(r"([0-9a-f]{64})  ([^/\\]+)", line)
        if not match:
            raise RuntimeError(f"Malformed SHA256SUMS.txt line: {line!r}")
        digest, name = match.groups()
        if name in hashes:
            raise RuntimeError(f"Duplicate checksum entry: {name}")
        hashes[name] = digest
    if set(hashes) != set(ASSET_TARGETS):
        raise RuntimeError(f"Checksum list must cover exactly four native assets: {sorted(hashes)}")
    return hashes


def validate_candidate(root: Path, run_id: str, commit: str) -> tuple[dict, dict[str, str]]:
    """Re-validate the exact downloaded artifact before touching GitHub."""
    root = root.resolve()
    if not root.is_dir():
        raise RuntimeError(f"Candidate directory does not exist: {root}")
    actual_names = {path.name for path in root.iterdir()}
    if actual_names != CANDIDATE_FILES or any(not (root / name).is_file() for name in CANDIDATE_FILES):
        raise RuntimeError(
            "Candidate directory must contain exactly four target-native downloads and three sidecars; "
            f"got {sorted(actual_names)}"
        )

    provenance = json.loads((root / "BUILD-PROVENANCE.json").read_text(encoding="utf-8"))
    if provenance.get("product") != "AZYRON CLIENT" or provenance.get("developer") != "JIGNESH GAMING":
        raise RuntimeError("Unexpected candidate product/developer metadata")
    if provenance.get("version") != VERSION or str(provenance.get("ci_run")) != str(run_id):
        raise RuntimeError("Candidate version or CI run ID mismatch")
    if provenance.get("source_commit") != commit:
        raise RuntimeError("Candidate source commit does not match this workflow run")
    native_builds = provenance.get("native_builds", "")
    if "real Fabric/Loom" not in native_builds or "no generated stubs" not in native_builds:
        raise RuntimeError("Candidate does not record real Fabric/Loom remapJar builds")
    release_format = provenance.get("release_format", "")
    if "no ZIP client bundle" not in release_format:
        raise RuntimeError("Candidate provenance does not explicitly prohibit a client ZIP bundle")
    if set(provenance.get("targets", {})) != set(TARGETS):
        raise RuntimeError("Candidate provenance must cover exactly Minecraft 1.20.1 and 1.21.11")
    if provenance.get("game_launch_test") != "NOT RUN":
        raise RuntimeError("Game launch status must remain explicitly NOT RUN")
    if provenance.get("gameplay_test") != "NOT RUN":
        raise RuntimeError("Gameplay status must remain explicitly NOT RUN")
    if provenance.get("device_performance_test") != "NOT RUN":
        raise RuntimeError("Device-performance status must remain explicitly NOT RUN")

    hashes = parse_checksums(root / "SHA256SUMS.txt")
    if set(provenance.get("assets", {})) != set(ASSET_TARGETS):
        raise RuntimeError("Provenance asset list does not match the four direct downloads")
    for name, (mc, kind) in ASSET_TARGETS.items():
        path = root / name
        digest = file_sha(path)
        if digest != hashes[name]:
            raise RuntimeError(f"SHA-256 mismatch: {name}")
        recorded = provenance["assets"][name]
        if recorded.get("name") != name or recorded.get("bytes") != path.stat().st_size:
            raise RuntimeError(f"Provenance size/name mismatch: {name}")
        if recorded.get("sha256") != digest:
            raise RuntimeError(f"Provenance SHA-256 mismatch: {name}")

        target = provenance["targets"][mc]
        if target.get("minecraft") != mc:
            raise RuntimeError(f"Target build report mismatch for {mc}")
        if kind == "jar":
            audit = native.validate_native_jar(path, mc)
            if target.get("java") != audit["java"] or target.get("java_class_major") != audit["java_class_major"]:
                raise RuntimeError(f"Java bytecode target mismatch in provenance for {mc}")
            if target.get("client_classes") != audit["client_classes"]:
                raise RuntimeError(f"Client class count mismatch in provenance for {mc}")
            if target.get("configured_mixins") != audit["configured_mixins"]:
                raise RuntimeError(f"Mixin count mismatch in provenance for {mc}")
        else:
            jar_path = root / native.client_jar_name(mc)
            native.validate_pack(path, mc, jar_path)

        artifact_record = target.get("native_jar" if kind == "jar" else "mrpack", {})
        if artifact_record.get("name") != name or artifact_record.get("bytes") != path.stat().st_size:
            raise RuntimeError(f"Target artifact record mismatch for {name}")
        if artifact_record.get("sha256") != digest:
            raise RuntimeError(f"Target artifact hash mismatch for {name}")

    readme = (root / "README.txt").read_text(encoding="utf-8")
    if "No client ZIP is supplied." not in readme:
        raise RuntimeError("Candidate README does not clearly state that no client ZIP is supplied")
    if "Azyron-Client-1.0.4.jar" not in readme or "Azyron-Modpack-1.0.4.mrpack" not in readme:
        raise RuntimeError("Candidate README is missing the selected 1.21.11 filenames")
    return provenance, hashes


def gh(args: list[str], *, stdin: str | None = None) -> str:
    result = subprocess.run(
        ["gh", *args], input=stdin, text=True,
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False,
    )
    if result.returncode:
        raise RuntimeError(f"gh {' '.join(args)} failed ({result.returncode}): {result.stderr.strip()}")
    return result.stdout


def get_release() -> dict:
    return json.loads(gh(["api", f"repos/{REPOSITORY}/releases/{RELEASE_ID}"]))


def require_existing_draft(release: dict) -> None:
    if release.get("id") != RELEASE_ID or release.get("tag_name") != TAG:
        raise RuntimeError(
            f"Release identity mismatch; expected ID {RELEASE_ID} / {TAG}, "
            f"got {release.get('id')} / {release.get('tag_name')}"
        )
    if release.get("draft") is not True or release.get("published_at") is not None:
        raise RuntimeError("The existing v1.0.4 release is not an unpublished draft; refusing changes")


def asset_map(release: dict) -> dict[str, dict]:
    assets = release.get("assets", [])
    mapped = {asset.get("name"): asset for asset in assets}
    if len(mapped) != len(assets) or None in mapped:
        raise RuntimeError("Draft contains duplicate or unnamed release assets")
    return mapped


def validate_known_legacy_assets(assets: dict[str, dict]) -> dict[str, dict]:
    allowed = set(UPLOAD_FILES) | set(LEGACY_ASSETS)
    unexpected = set(assets) - allowed
    if unexpected:
        raise RuntimeError(f"Unexpected v1.0.4 draft assets; refusing removal: {sorted(unexpected)}")
    found = {}
    for name, expected in LEGACY_ASSETS.items():
        asset = assets.get(name)
        if asset is None:
            continue
        actual = {key: asset.get(key) for key in expected}
        if actual != expected:
            raise RuntimeError(f"Legacy ZIP asset does not match the known stale file; refusing cleanup: {name}")
        found[name] = asset
    return found


def verify_uploaded_assets(assets: dict[str, dict], root: Path) -> None:
    missing = UPLOAD_FILES - set(assets)
    if missing:
        raise RuntimeError(f"GitHub is missing uploaded native assets: {sorted(missing)}")
    for name in UPLOAD_FILES:
        asset = assets[name]
        path = root / name
        expected_digest = "sha256:" + file_sha(path)
        if asset.get("size") != path.stat().st_size or asset.get("digest") != expected_digest:
            raise RuntimeError(
                f"GitHub asset verification failed for {name}: "
                f"size={asset.get('size')} digest={asset.get('digest')}"
            )


def update_draft(root: Path, notes_path: Path) -> dict:
    release = get_release()
    require_existing_draft(release)
    old_assets = validate_known_legacy_assets(asset_map(release))

    upload_paths = [str(root / name) for name in sorted(UPLOAD_FILES)]
    gh(["release", "upload", TAG, *upload_paths, "--clobber", "--repo", REPOSITORY])

    # Confirm that every new file has GitHub's expected digest before changing
    # the release body or deleting either of the previous ZIP assets.
    release = get_release()
    require_existing_draft(release)
    uploaded_assets = asset_map(release)
    validate_known_legacy_assets(uploaded_assets)
    verify_uploaded_assets(uploaded_assets, root)

    payload = json.dumps({
        "name": "AZYRON CLIENT v1.0.4",
        "tag_name": TAG,
        "body": notes_path.read_text(encoding="utf-8"),
        "draft": True,
    })
    updated = json.loads(gh([
        "api", "--method", "PATCH", f"repos/{REPOSITORY}/releases/{RELEASE_ID}", "--input", "-"
    ], stdin=payload))
    require_existing_draft(updated)

    # Remove only the exact, previously identified ZIP assets after replacement
    # bytes are verified. This does not touch the v1.0.3 draft.
    current_assets = asset_map(get_release())
    for name, original in old_assets.items():
        current = current_assets.get(name)
        if current is None:
            continue
        if any(current.get(key) != original.get(key) for key in LEGACY_ASSETS[name]):
            raise RuntimeError(f"Stale ZIP changed during draft update; refusing to delete: {name}")
        gh(["api", "--method", "DELETE", f"repos/{REPOSITORY}/releases/assets/{current['id']}"])

    final = get_release()
    require_existing_draft(final)
    final_assets = asset_map(final)
    if set(final_assets) != UPLOAD_FILES:
        raise RuntimeError(f"Final v1.0.4 draft asset set is not exact: {sorted(final_assets)}")
    verify_uploaded_assets(final_assets, root)
    return final


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--candidates", type=Path, required=True)
    parser.add_argument("--notes", type=Path, required=True)
    parser.add_argument("--repo", required=True)
    parser.add_argument("--release-id", type=int, default=RELEASE_ID)
    parser.add_argument("--tag", default=TAG)
    parser.add_argument("--run-id", required=True)
    parser.add_argument("--commit", required=True)
    args = parser.parse_args()

    if not os.environ.get("GH_TOKEN"):
        raise RuntimeError("GH_TOKEN is required for the manual, authorized draft refresh")
    if args.repo != REPOSITORY or args.release_id != RELEASE_ID or args.tag != TAG:
        raise RuntimeError("This updater is pinned to the existing v1.0.4 draft; refusing another release")
    root = args.candidates.resolve()
    validate_candidate(root, args.run_id, args.commit)
    final = update_draft(root, args.notes.resolve())

    print(f"Updated existing release {RELEASE_ID} ({TAG}); draft={final['draft']}, published_at={final.get('published_at')}.")
    print(f"Verified exactly {len(UPLOAD_FILES)} direct assets; the obsolete Client/Modpack ZIPs were removed.")
    print("v1.0.3 was not modified. No game-launch, gameplay, or device-performance testing is claimed.")
    print(final.get("html_url", ""))


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(f"ERROR: {error}", file=sys.stderr)
        raise
