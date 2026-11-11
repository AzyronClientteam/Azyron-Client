#!/usr/bin/env python3
"""Small read-only HTTP portal for direct native Azyron release assets."""
from __future__ import annotations

from datetime import datetime, timezone
from email.utils import format_datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import importlib.util
import json
import os
from pathlib import Path
import re
import struct
import zipfile
from urllib.parse import quote, urlsplit

ROOT = Path(__file__).resolve().parent
validator_spec = importlib.util.spec_from_file_location(
    "azyron_portal_native_validator", ROOT.parent / "scripts/prepare-native-candidates.py"
)
if validator_spec is None or validator_spec.loader is None:
    raise RuntimeError("Could not load native release asset validation")
native = importlib.util.module_from_spec(validator_spec)
validator_spec.loader.exec_module(native)
RELEASES_DIR = str(ROOT.parent / "releases")
HOST = os.environ.get("AZYRON_PORTAL_HOST", "0.0.0.0")
PORT = int(os.environ.get("PORT", os.environ.get("AZYRON_PORTAL_PORT", "8000")))
VERSION = "1.0.4"
TARGETS = ("1.20.1", "1.21.11")
ALLOWED_ASSETS = frozenset({
    "Azyron-Client-1.0.4.jar",
    "Azyron-Client-1.0.4-mc1.20.1.jar",
    "Azyron-Modpack-1.0.4.mrpack",
    "Azyron-Modpack-1.0.4-mc1.20.1.mrpack",
})

def sha256(path: Path) -> str:
    import hashlib

    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(128 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def supported_asset(path: Path) -> bool:
    """Serve only target-native JARs and exact-paired, structurally valid MRpacks."""
    if path.name not in ALLOWED_ASSETS or not path.is_file() or path.is_symlink():
        return False
    mc = "1.20.1" if "-mc1.20.1." in path.name else "1.21.11"
    try:
        if path.suffix == ".jar":
            native.validate_native_jar(path, mc)
        else:
            matching_jar = path.parent / native.client_jar_name(mc)
            if not matching_jar.is_file() or matching_jar.is_symlink():
                return False
            native.validate_native_jar(matching_jar, mc)
            native.validate_pack(path, mc, matching_jar)
    except (OSError, ValueError, AssertionError, KeyError, struct.error, zipfile.BadZipFile):
        return False
    return True


def load_live_assets() -> list[dict]:
    directory = Path(RELEASES_DIR)
    assets = []
    if not directory.is_dir():
        return assets
    for path in sorted(directory.iterdir(), key=lambda item: item.name):
        if not supported_asset(path):
            continue
        mc = "1.21.11" if "-mc1.20.1." not in path.name else "1.20.1"
        kind = "native-jar" if path.suffix == ".jar" else "modrinth-pack"
        assets.append({
            "display_name": path.name,
            "kind": kind,
            "minecraft_version": mc,
            "size_bytes": path.stat().st_size,
            "sha256": sha256(path),
            "download_url": "/downloads/" + quote(path.name),
        })
    return assets


def page() -> bytes:
    assets = load_live_assets()
    by_target = {
        mc: {asset["kind"]: asset for asset in assets if asset["minecraft_version"] == mc}
        for mc in TARGETS
    }

    def card(mc: str, java: str, suffix: str) -> str:
        native = by_target[mc].get("native-jar")
        pack = by_target[mc].get("modrinth-pack")
        if native and pack:
            files = (
                f'<a class="download" href="{native["download_url"]}">Download native Fabric JAR</a>'
                f'<a class="download secondary" href="{pack["download_url"]}">Download Modrinth .mrpack</a>'
                f'<p class="checksums">JAR SHA-256 <code>{native["sha256"]}</code><br>'
                f'.mrpack SHA-256 <code>{pack["sha256"]}</code></p>'
            )
        else:
            files = '<p class="missing">Target-specific files are not staged yet.</p>'
        return (
            f'<article class="target"><span class="eyebrow">Fabric · {java}</span>'
            f'<h2>Minecraft {mc}</h2>{files}'
            f'<p class="filename">{suffix}</p></article>'
        )

    cards = card("1.21.11", "Java 21+", "Azyron-Client-1.0.4.jar") + card(
        "1.20.1", "Java 17+", "Azyron-Client-1.0.4-mc1.20.1.jar"
    )
    body = f'''<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="theme-color" content="#0b1020">
<title>AZYRON CLIENT v{VERSION} — Releases</title>
<style>
:root{{color-scheme:dark;--bg:#0b1020;--panel:#131b31;--text:#edf2ff;--muted:#a9b5d3;--cyan:#58e6db;--violet:#a99bff}}
*{{box-sizing:border-box}}body{{margin:0;background:radial-gradient(ellipse at 50% -25%,#28365c 0,transparent 58%),var(--bg);color:var(--text);font:16px/1.55 system-ui,-apple-system,"Segoe UI",sans-serif}}
main{{width:min(980px,calc(100% - 32px));margin:0 auto;padding:54px 0 72px}}.hero{{text-align:center;margin-bottom:38px}}.eyebrow{{color:var(--cyan);font-size:.78rem;letter-spacing:.13em;text-transform:uppercase;font-weight:800}}
h1{{font-size:clamp(2.25rem,7vw,4.6rem);letter-spacing:-.06em;line-height:1.04;margin:14px 0}}.hero p,.note{{color:var(--muted)}}.hero p{{max-width:620px;margin:14px auto 0}}.targets{{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:18px}}
.target{{background:linear-gradient(145deg,#18223c,var(--panel));border:1px solid #2c395c;border-radius:20px;padding:25px;box-shadow:0 16px 50px #02051166}}h2{{font-size:1.55rem;margin:8px 0 20px}}.download{{display:block;text-decoration:none;text-align:center;color:#07121c;background:linear-gradient(110deg,var(--cyan),#90f3c4);font-weight:850;padding:13px 17px;border-radius:11px;margin:10px 0}}.secondary{{background:linear-gradient(110deg,#b5aaff,#dfb2ff);color:#151026}}a:hover{{filter:brightness(1.08)}}.checksums{{font-size:.75rem;color:var(--muted);overflow-wrap:anywhere;margin:18px 0}}code{{color:#d9e8ff}}.filename{{color:var(--muted);font-size:.78rem;margin:18px 0 0;overflow-wrap:anywhere}}.note{{margin:22px 0 0;padding:17px 19px;border:1px solid #303b58;border-radius:12px;background:#10182b}}.note strong{{color:var(--text)}}.footer{{text-align:center;color:var(--muted);margin-top:32px;font-size:.83rem}}.missing{{color:#ffd48a}}
</style>
</head>
<body><main>
<header class="hero"><span class="eyebrow">Fabric Edition · Version {VERSION}</span><h1>AZYRON CLIENT</h1><p>Choose the download that matches your exact Minecraft version. Each JAR and Modrinth pack is built for one target only.</p></header>
<section class="targets" aria-label="Minecraft version downloads">{cards}</section>
<p class="note"><strong>Install:</strong> Use the .mrpack with a Modrinth-compatible launcher, or place the native .jar in the mods folder of a matching Fabric installation. Do not mix Minecraft targets. Java 21+ is required for 1.21.11; Java 17+ for 1.20.1.</p>
<p class="note"><strong>Validation:</strong> Each JAR is a real Fabric Loom remapJar build, not a renamed ZIP. Release asset packaging and metadata are checked separately from game-launch, device, FPS, thermal, and gameplay testing; those tests are not claimed here.</p>
<footer class="footer">Developed by JIGNESH GAMING · Azyron Client {VERSION}</footer>
</main></body></html>'''
    return body.encode("utf-8")


class PortalHandler(BaseHTTPRequestHandler):
    server_version = "AzyronReleasePortal/1.0"

    def log_message(self, fmt: str, *args) -> None:
        print(f"{self.client_address[0]} - {fmt % args}")

    def do_GET(self) -> None:
        parsed = urlsplit(self.path)
        if parsed.path in ("/", "/index.html"):
            body = page()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(body)
            return
        if parsed.path == "/healthz":
            body = json.dumps({"ok": True, "assets": len(load_live_assets())}).encode()
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return
        match = re.fullmatch(r"/downloads/([^/]+)", parsed.path)
        if match:
            name = match.group(1)
            path = Path(RELEASES_DIR) / name
            if not supported_asset(path):
                self.send_error(404, "Native release asset not found")
                return
            try:
                stat = path.stat()
                self.send_response(200)
                self.send_header("Content-Type", "application/java-archive" if path.suffix == ".jar" else "application/zip")
                self.send_header("Content-Disposition", f"attachment; filename=\"{path.name}\"")
                self.send_header("Content-Length", str(stat.st_size))
                self.send_header("Last-Modified", format_datetime(datetime.fromtimestamp(stat.st_mtime, timezone.utc), usegmt=True))
                self.send_header("X-Content-Type-Options", "nosniff")
                self.end_headers()
                with path.open("rb") as stream:
                    for chunk in iter(lambda: stream.read(128 * 1024), b""):
                        self.wfile.write(chunk)
            except BrokenPipeError:
                pass
            return
        self.send_error(404)

    def do_HEAD(self) -> None:
        parsed = urlsplit(self.path)
        if parsed.path in ("/", "/index.html"):
            body = page()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            return
        match = re.fullmatch(r"/downloads/([^/]+)", parsed.path)
        if match:
            path = Path(RELEASES_DIR) / match.group(1)
            if not supported_asset(path):
                self.send_error(404, "Native release asset not found")
                return
            self.send_response(200)
            self.send_header("Content-Type", "application/java-archive" if path.suffix == ".jar" else "application/zip")
            self.send_header("Content-Length", str(path.stat().st_size))
            self.send_header("X-Content-Type-Options", "nosniff")
            self.end_headers()
            return
        self.send_error(404)


class ThreadingHTTPServerV6(ThreadingHTTPServer):
    daemon_threads = True


def main() -> None:
    os.chdir(ROOT)
    server = ThreadingHTTPServerV6((HOST, PORT), PortalHandler)
    print(f"AZYRON CLIENT v{VERSION} release portal listening on http://{HOST}:{PORT}")
    server.serve_forever()


if __name__ == "__main__":
    main()
