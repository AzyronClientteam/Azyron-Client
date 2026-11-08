#!/usr/bin/env python3
"""
AZYRON CLIENT — Complete Source Archive Builder & Verifier (v1.0.2)
Generates:
  1. releases/Azyron-Client-Source-v1.0.2.zip
  2. releases/Azyron-Client-Source-v1.0.2.tar.gz

Includes:
  - Complete Java 21 client & test source code (src/main/java, src/test/java)
  - Fabric configuration, Mixins, intermediary refmap, and GUI/lang resources
  - Gradle build configuration & Gradle Wrapper (build.gradle, settings.gradle, gradle.properties, gradlew, gradle/)
  - Modpack configuration, Modrinth v1 metadata, icon & tuned overrides (modpack/)
  - Build, dependency download, and draft-release staging scripts (scripts/, releases/download-dependencies.*)
  - Documentation & MIT License

Performs:
  - Strict secret/credential scan across every included file before packaging
  - Deterministic archive creation under root prefix Azyron-Client-Source-v1.0.2/
  - Full extraction + byte-for-byte verification of both .zip and .tar.gz archives
  - SHA-256 checksum calculation and SHA256SUMS.txt update
"""

import gzip
import hashlib
import io
import os
import re
import shutil
import stat
import tarfile
import tempfile
import zipfile

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
CLIENT_ROOT = os.path.dirname(SCRIPT_DIR)
RELEASES_DIR = os.path.join(CLIENT_ROOT, "releases")
ARCHIVE_PREFIX = "Azyron-Client-Source-v1.0.2"

OUT_ZIP = os.path.join(RELEASES_DIR, f"{ARCHIVE_PREFIX}.zip")
OUT_TGZ = os.path.join(RELEASES_DIR, f"{ARCHIVE_PREFIX}.tar.gz")

EXCLUDED_DIRS = {
    ".git",
    ".gradle",
    ".idea",
    ".vscode",
    "__pycache__",
    "build",
    "out",
    "bin",
    "run",
}

EXCLUDED_FILENAMES = {
    ".DS_Store",
    "Thumbs.db",
    "local.properties",
    ".netrc",
    ".git-credentials",
    "SHA256SUMS.txt",
}

EXCLUDED_BINARY_BUNDLES_IN_RELEASES = {
    ".jar",
    ".mrpack",
    ".zip",
    ".gz",
    ".tgz",
}

SECRET_PATTERNS = [
    ("GitHub Personal Access Token", re.compile(r"\bgh[pousr]_[A-Za-z0-9_]{20,}\b")),
    ("GitHub Fine-Grained PAT", re.compile(r"\bgithub_pat_[A-Za-z0-9_]{20,}\b")),
    ("AWS Access Key ID", re.compile(r"\bAKIA[0-9A-Z]{16}\b")),
    ("Google API Key", re.compile(r"\bAIza[0-9A-Za-z\-_]{35}\b")),
    ("OpenAI / Generic sk- Token", re.compile(r"\bsk-[A-Za-z0-9]{20,}\b")),
    ("Slack Token", re.compile(r"\bxox[baprs]-[A-Za-z0-9\-]{10,}\b")),
    ("PEM Private Key", re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----")),
    ("Hardcoded Bearer Token", re.compile(r"Authorization:\s*Bearer\s+[A-Za-z0-9\-\._~\+\/]{16,}", re.I)),
    ("Credential URL", re.compile(r"https?://[^/\s:@]+:[^/\s:@]+@github\.com", re.I)),
]


def collect_source_files() -> list[tuple[str, str]]:
    collected = []
    for root, dirs, files in os.walk(CLIENT_ROOT):
        dirs[:] = sorted(d for d in dirs if d not in EXCLUDED_DIRS and not d.startswith(".env"))
        rel_dir = os.path.relpath(root, CLIENT_ROOT)
        for fname in sorted(files):
            if fname in EXCLUDED_FILENAMES or fname.startswith(".env"):
                continue
            if fname.endswith((".pem", ".key", ".p12", ".jks", ".keystore")):
                raise RuntimeError(f"Refusing to package key/keystore file: {fname}")
            full_path = os.path.join(root, fname)
            rel_path = os.path.normpath(os.path.join(rel_dir, fname)).lstrip("./")
            if rel_dir == ".":
                rel_path = fname
            rel_posix = rel_path.replace(os.sep, "/")

            # Inside releases/, include helper scripts only — exclude binary build artifacts/archives
            if rel_posix.startswith("releases/"):
                _, ext = os.path.splitext(fname)
                if ext.lower() in EXCLUDED_BINARY_BUNDLES_IN_RELEASES:
                    continue

            collected.append((rel_posix, full_path))
    collected.sort(key=lambda item: item[0])
    return collected


def scan_for_secrets(files: list[tuple[str, str]]) -> None:
    for rel_posix, full_path in files:
        with open(full_path, "rb") as fh:
            raw = fh.read()
        try:
            text = raw.decode("utf-8")
        except UnicodeDecodeError:
            continue
        for label, pattern in SECRET_PATTERNS:
            if pattern.search(text):
                raise RuntimeError(f"SECURITY ABORT: Detected potential {label} in {rel_posix}")


def verify_required_categories(files: list[tuple[str, str]]) -> None:
    paths = {rel for rel, _ in files}
    required_exact = [
        "build.gradle",
        "settings.gradle",
        "gradle.properties",
        "gradlew",
        "gradlew.bat",
        "gradle/wrapper/gradle-wrapper.properties",
        "gradle/wrapper/gradle-wrapper.jar",
        "src/main/resources/fabric.mod.json",
        "src/main/resources/azyron-client.mixins.json",
        "mappings/azyron-client.refmap.json",
        "modpack/modrinth.index.json",
        "modpack/icon.png",
        "modpack/overrides/config/azyron-client.json",
        "modpack/overrides/config/sodium-options.json",
        "scripts/build-modpack.py",
        "scripts/stage-draft-release.sh",
        "src/main/java/com/jigneshgaming/azyron/AzyronClient.java",
        "src/test/java/com/jigneshgaming/azyron/AzyronClientTestSuite.java",
        "LICENSE",
        "README.md",
    ]
    missing = [req for req in required_exact if req not in paths]
    if missing:
        raise RuntimeError(f"Missing required source files: {missing}")

    java_count = sum(1 for p in paths if p.startswith("src/main/java/") and p.endswith(".java"))
    if java_count < 38:
        raise RuntimeError(f"Expected >=38 main Java source files, found {java_count}")


def build_zip(files: list[tuple[str, str]], out_zip: str) -> None:
    fixed_dt = (2026, 11, 7, 0, 0, 0)
    with zipfile.ZipFile(out_zip, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
        for rel_posix, full_path in files:
            st = os.stat(full_path)
            is_exec = bool(st.st_mode & stat.S_IXUSR)
            perm = 0o755 if is_exec else 0o644
            info = zipfile.ZipInfo(f"{ARCHIVE_PREFIX}/{rel_posix}", date_time=fixed_dt)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.create_system = 3  # Unix
            info.external_attr = (stat.S_IFREG | perm) << 16
            with open(full_path, "rb") as fh:
                zf.writestr(info, fh.read(), compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)


def build_targz(files: list[tuple[str, str]], out_tgz: str) -> None:
    fixed_mtime = 1793990400  # 2026-11-06T18:40:00Z deterministic timestamp
    with open(out_tgz, "wb") as raw_out:
        with gzip.GzipFile(filename=f"{ARCHIVE_PREFIX}.tar", mode="wb", fileobj=raw_out, compresslevel=9, mtime=fixed_mtime) as gz_out:
            with tarfile.open(mode="w", fileobj=gz_out, format=tarfile.PAX_FORMAT) as tf:
                for rel_posix, full_path in files:
                    st = os.stat(full_path)
                    is_exec = bool(st.st_mode & stat.S_IXUSR)
                    with open(full_path, "rb") as fh:
                        data = fh.read()
                    ti = tarfile.TarInfo(name=f"{ARCHIVE_PREFIX}/{rel_posix}")
                    ti.size = len(data)
                    ti.mtime = fixed_mtime
                    ti.mode = 0o755 if is_exec else 0o644
                    ti.uid = 0
                    ti.gid = 0
                    ti.uname = "azyron"
                    ti.gname = "azyron"
                    tf.addfile(ti, io.BytesIO(data))


def verify_extraction(files: list[tuple[str, str]], out_zip: str, out_tgz: str) -> None:
    expected = {}
    for rel_posix, full_path in files:
        with open(full_path, "rb") as fh:
            expected[f"{ARCHIVE_PREFIX}/{rel_posix}"] = hashlib.sha256(fh.read()).hexdigest()

    # 1. Verify ZIP integrity & full extraction
    with tempfile.TemporaryDirectory(prefix="azyron_verify_zip_") as tmp_zip:
        with zipfile.ZipFile(out_zip, "r") as zf:
            bad = zf.testzip()
            if bad is not None:
                raise RuntimeError(f"ZIP CRC error on {bad}")
            zf.extractall(tmp_zip)
        for arc_rel, exp_sha in expected.items():
            ext_p = os.path.join(tmp_zip, arc_rel)
            if not os.path.isfile(ext_p):
                raise RuntimeError(f"Missing extracted ZIP file: {arc_rel}")
            with open(ext_p, "rb") as fh:
                got_sha = hashlib.sha256(fh.read()).hexdigest()
            if got_sha != exp_sha:
                raise RuntimeError(f"SHA mismatch on extracted ZIP file: {arc_rel}")

    # 2. Verify TAR.GZ integrity & full extraction
    with tempfile.TemporaryDirectory(prefix="azyron_verify_tgz_") as tmp_tgz:
        with tarfile.open(out_tgz, "r:gz") as tf:
            if hasattr(tarfile, "data_filter"):
                tf.extractall(tmp_tgz, filter="data")
            else:
                tf.extractall(tmp_tgz)
        for arc_rel, exp_sha in expected.items():
            ext_p = os.path.join(tmp_tgz, arc_rel)
            if not os.path.isfile(ext_p):
                raise RuntimeError(f"Missing extracted TAR.GZ file: {arc_rel}")
            with open(ext_p, "rb") as fh:
                got_sha = hashlib.sha256(fh.read()).hexdigest()
            if got_sha != exp_sha:
                raise RuntimeError(f"SHA mismatch on extracted TAR.GZ file: {arc_rel}")


def sha256_file(path: str) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as fh:
        for chunk in iter(lambda: fh.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def main() -> None:
    os.makedirs(RELEASES_DIR, exist_ok=True)
    files = collect_source_files()
    scan_for_secrets(files)
    verify_required_categories(files)

    build_zip(files, OUT_ZIP)
    build_targz(files, OUT_TGZ)
    verify_extraction(files, OUT_ZIP, OUT_TGZ)

    # Update SHA256SUMS.txt for all release assets
    release_assets = [
        "Azyron-Client-1.0.2.jar",
        "Azyron-Modpack-1.0.2.mrpack",
        "Azyron-Client-Source-v1.0.2.zip",
        "Azyron-Client-Source-v1.0.2.tar.gz",
    ]
    sums_lines = []
    for fname in release_assets:
        fpath = os.path.join(RELEASES_DIR, fname)
        if os.path.isfile(fpath):
            sums_lines.append(f"{sha256_file(fpath)}  {fname}\n")
    with open(os.path.join(RELEASES_DIR, "SHA256SUMS.txt"), "w", encoding="utf-8") as fh:
        fh.writelines(sums_lines)

    print(f"Packaged & verified {len(files)} source files (0 secrets detected):")
    for fname in release_assets:
        fpath = os.path.join(RELEASES_DIR, fname)
        if os.path.isfile(fpath):
            print(f"  {fname}: {os.path.getsize(fpath):,} bytes | sha256:{sha256_file(fpath)}")


if __name__ == "__main__":
    main()
