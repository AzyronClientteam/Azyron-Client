#!/usr/bin/env python3
"""
AZYRON CLIENT v1.0.3 — version-specific standard Fabric downloads (draft portal).
Each Minecraft target has its own native JAR and matching Modrinth pack. The portal
never rewrites one target's manifest to make a cross-version artifact.
"""

from datetime import datetime, timezone
import hashlib
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import io
import json
import os
from urllib.parse import urlparse
import zipfile

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RELEASES_DIR = os.path.join(BASE_DIR, "releases")
PORT = int(os.environ.get("PORT", "8080"))

ASSET_DEFINITIONS = [
    {
        "slug": "Azyron-Client-1.0.3-mc1.20.1.jar",
        "display_name": "Azyron-Client-1.0.3-mc1.20.1.jar",
        "download_filename": "Azyron-Client-1.0.3-mc1.20.1.jar",
        "disk_filename": "Azyron-Client-1.0.3-mc1.20.1.jar",
        "content_type": "application/java-archive",
        "icon": "package",
        "show_sha256": True,
        "badge": "Native • Minecraft 1.20.1 Fabric",
        "minecraft_target": "1.20.1",
        "artifact_kind": "jar",
    },
    {
        "slug": "Azyron-Modpack-1.0.3-mc1.20.1.mrpack",
        "display_name": "Azyron-Modpack-1.0.3-mc1.20.1.mrpack",
        "download_filename": "Azyron-Modpack-1.0.3-mc1.20.1.mrpack",
        "disk_filename": "Azyron-Modpack-1.0.3-mc1.20.1.mrpack",
        "content_type": "application/octet-stream",
        "icon": "package",
        "show_sha256": True,
        "badge": "Native • Minecraft 1.20.1 Fabric",
        "minecraft_target": "1.20.1",
        "artifact_kind": "mrpack",
    },
    {
        "slug": "Azyron-Client-1.0.3-mc1.21.11.jar",
        "display_name": "Azyron-Client-1.0.3-mc1.21.11.jar",
        "download_filename": "Azyron-Client-1.0.3-mc1.21.11.jar",
        "disk_filename": "Azyron-Client-1.0.3-mc1.21.11.jar",
        "content_type": "application/java-archive",
        "icon": "package",
        "show_sha256": True,
        "badge": "Native • Minecraft 1.21.11 Fabric",
        "minecraft_target": "1.21.11",
        "artifact_kind": "jar",
    },
    {
        "slug": "Azyron-Modpack-1.0.3-mc1.21.11.mrpack",
        "display_name": "Azyron-Modpack-1.0.3-mc1.21.11.mrpack",
        "download_filename": "Azyron-Modpack-1.0.3-mc1.21.11.mrpack",
        "disk_filename": "Azyron-Modpack-1.0.3-mc1.21.11.mrpack",
        "content_type": "application/octet-stream",
        "icon": "package",
        "show_sha256": True,
        "badge": "Native • Minecraft 1.21.11 Fabric",
        "minecraft_target": "1.21.11",
        "artifact_kind": "mrpack",
    },
]



def format_github_size(num_bytes: int) -> str:
    if num_bytes < 1024:
        return f"{num_bytes} Bytes"
    kb = num_bytes / 1024.0
    if kb < 1024:
        return f"{kb:.0f} KB" if kb >= 100 else f"{kb:.1f} KB"
    mb = kb / 1024.0
    return f"{mb:.2f} MB"



def load_live_assets():
    enriched = []
    for item in ASSET_DEFINITIONS:
        disk_path = os.path.join(RELEASES_DIR, item["disk_filename"])
        if not os.path.isfile(disk_path):
            raise FileNotFoundError(f"Missing required build artifact: {disk_path}")
        stat = os.stat(disk_path)
        with open(disk_path, "rb") as fh:
            data = fh.read()
        target = item.get("minecraft_target")
        if target:
            with zipfile.ZipFile(io.BytesIO(data), "r") as archive:
                if item["artifact_kind"] == "jar":
                    metadata = json.loads(archive.read("fabric.mod.json"))
                    if metadata.get("depends", {}).get("minecraft") != [target]:
                        raise ValueError(f"Native JAR target mismatch: {item['display_name']}")
                else:
                    index = json.loads(archive.read("modrinth.index.json"))
                    if index.get("dependencies", {}).get("minecraft") != target:
                        raise ValueError(f"Native MRPack target mismatch: {item['display_name']}")
                    embedded = archive.read("overrides/mods/Azyron-Client-1.0.3.jar")
                    with zipfile.ZipFile(io.BytesIO(embedded), "r") as client_jar:
                        metadata = json.loads(client_jar.read("fabric.mod.json"))
                        if metadata.get("depends", {}).get("minecraft") != [target]:
                            raise ValueError(f"Embedded JAR target mismatch: {item['display_name']}")
        digest = hashlib.sha256(data).hexdigest()
        dt = datetime.fromtimestamp(stat.st_mtime, tz=timezone.utc)
        enriched.append({
            **item,
            "disk_path": disk_path,
            "size_bytes": stat.st_size,
            "size_human": format_github_size(stat.st_size),
            "sha256": digest,
            "uploaded_iso": dt.strftime("%Y-%m-%dT%H:%M:%SZ"),
            "uploaded_short": dt.strftime("%b %-d, %Y"),
            "uploaded_full": dt.strftime("%b %-d, %Y, %H:%M UTC"),
        })
    return enriched


OCTICONS = {
    "mark-github": (
        '<svg aria-hidden="true" height="24" viewBox="0 0 16 16" width="24" fill="currentColor">'
        '<path d="M8 0c4.42 0 8 3.58 8 8a8.013 8.013 0 0 1-5.45 7.59c-.4.08-.55-.17-.55-.38 '
        '0-.27.01-1.13.01-2.2 0-.75-.25-1.23-.54-1.48 1.78-.2 3.65-.88 3.65-3.95 '
        '0-.88-.31-1.59-.82-2.15.08-.2.36-1.02-.08-2.12 0 0-.67-.22-2.2.82-.64-.18-1.32-.27-2-.27'
        '-.68 0-1.36.09-2 .27-1.53-1.03-2.2-.82-2.2-.82-.44 1.1-.16 1.92-.08 2.12-.51.56-.82 '
        '1.28-.82 2.15 0 3.06 1.86 3.75 3.64 3.95-.23.2-.44.55-.51 1.07-.46.21-1.61.55-2.33-.66'
        '-.15-.24-.6-.83-1.23-.82-.67.01-.27.38.01.53.34.19.73.9.82 1.13.16.45.68 1.31 2.69.94 '
        '0 .67.01 1.3.01 1.49 0 .21-.15.45-.55.38A7.995 7.995 0 0 1 0 8c0-4.42 3.58-8 8-8Z"></path></svg>'
    ),
    "lock": (
        '<svg class="octicon" aria-hidden="true" height="16" viewBox="0 0 16 16" width="16" fill="currentColor">'
        '<path d="M4 4a4 4 0 0 1 8 0v2h.25c.966 0 1.75.784 1.75 1.75v5.5A1.75 1.75 0 0 1 12.25 15h-8.5'
        'A1.75 1.75 0 0 1 2 13.25v-5.5C2 6.784 2.784 6 3.75 6H4Zm8.25 3.5h-8.5a.25.25 0 0 0-.25.25v5.5'
        'c0 .138.112.25.25.25h8.5a.25.25 0 0 0 .25-.25v-5.5a.25.25 0 0 0-.25-.25ZM10.5 6V4a2.5 2.5 0 1 0-5 0v2Z"></path></svg>'
    ),
    "tag": (
        '<svg class="octicon" aria-hidden="true" height="16" viewBox="0 0 16 16" width="16" fill="currentColor">'
        '<path d="M1 7.775V2.75C1 1.784 1.784 1 2.75 1h5.025c.464 0 .91.184 1.238.513l6.25 6.25a1.75 1.75 0 0 1 0 2.474'
        'l-5.026 5.026a1.75 1.75 0 0 1-2.474 0l-6.25-6.25A1.752 1.752 0 0 1 1 7.775Zm1.5 0c0 .066.026.13.073.177l6.25 6.25'
        'a.25.25 0 0 0 .354 0l5.025-5.025a.25.25 0 0 0 0-.354l-6.25-6.25a.25.25 0 0 0-.177-.073H2.75a.25.25 0 0 0-.25.25ZM6 5'
        'a1 1 0 1 1 0 2 1 1 0 0 1 0-2Z"></path></svg>'
    ),
    "commit": (
        '<svg class="octicon" aria-hidden="true" height="16" viewBox="0 0 16 16" width="16" fill="currentColor">'
        '<path d="M11.93 8.5a4.002 4.002 0 0 1-7.86 0H.75a.75.75 0 0 1 0-1.5h3.32a4.002 4.002 0 0 1 7.86 0h3.32'
        'a.75.75 0 0 1 0 1.5Zm-1.43-.75a2.5 2.5 0 1 0-5 0 2.5 2.5 0 0 0 5 0Z"></path></svg>'
    ),
    "package": (
        '<svg class="octicon asset-icon" aria-hidden="true" height="16" viewBox="0 0 16 16" width="16" fill="currentColor">'
        '<path d="m8.878.392 5.25 3.045c.54.314.872.89.872 1.514v6.098a1.75 1.75 0 0 1-.872 1.514l-5.25 3.045a1.75 1.75 0 0 1-1.756 0'
        'l-5.25-3.045A1.75 1.75 0 0 1 1 11.049V4.951c0-.624.332-1.201.872-1.514L7.122.392a1.75 1.75 0 0 1 1.756 0ZM7.875 1.69l-4.63 2.685'
        'L8 7.133l4.755-2.758-4.63-2.685a.248.248 0 0 0-.25 0ZM2.5 5.677v5.372c0 .09.047.171.125.216l4.625 2.683V8.432Zm6.25 8.271 '
        '4.625-2.683a.25.25 0 0 0 .125-.216V5.677L8.75 8.432Z"></path></svg>'
    ),
    "zip": (
        '<svg class="octicon asset-icon" aria-hidden="true" height="16" viewBox="0 0 16 16" width="16" fill="currentColor">'
        '<path d="M3.5 1.75v11.5c0 .09.048.173.126.217a.75.75 0 0 1-.752 1.298A1.748 1.748 0 0 1 2 13.25V1.75C2 .784 2.784 0 3.75 0h5.586'
        'c.464 0 .909.185 1.237.513l2.914 2.914c.329.328.513.773.513 1.237v8.586A1.75 1.75 0 0 1 12.25 15h-.5a.75.75 0 0 1 0-1.5h.5'
        'a.25.25 0 0 0 .25-.25V4.664a.25.25 0 0 0-.073-.177L9.513 1.573a.25.25 0 0 0-.177-.073H7.25v1.5h1.5V4.5h-1.5V6h1.5v1.5h-1.5'
        'V9h1.5v2.25a.75.75 0 0 1-.75.75h-2a.75.75 0 0 1-.75-.75V9h1.5V7.5h-1.5V6h1.5V4.5h-1.5V3h1.5V1.5H3.75a.25.25 0 0 0-.25.25Zm2.75 '
        '8.75h1v-1h-1Z"></path></svg>'
    ),
    "copy": (
        '<svg class="octicon" aria-hidden="true" height="16" viewBox="0 0 16 16" width="16" fill="currentColor">'
        '<path d="M0 6.75C0 5.784.784 5 1.75 5h1.5a.75.75 0 0 1 0 1.5h-1.5a.25.25 0 0 0-.25.25v7.5c0 .138.112.25.25.25h7.5'
        'a.25.25 0 0 0 .25-.25v-1.5a.75.75 0 0 1 1.5 0v1.5A1.75 1.75 0 0 1 9.25 16h-7.5A1.75 1.75 0 0 1 0 14.25Z"></path>'
        '<path d="M5 1.75C5 .784 5.784 0 6.75 0h7.5C15.216 0 16 .784 16 1.75v7.5A1.75 1.75 0 0 1 14.25 11h-7.5A1.75 1.75 0 0 1 5 9.25Zm1.75'
        '-.25a.25.25 0 0 0-.25.25v7.5c0 .138.112.25.25.25h7.5a.25.25 0 0 0 .25-.25v-7.5a.25.25 0 0 0-.25-.25Z"></path></svg>'
    ),
    "check": (
        '<svg class="octicon" aria-hidden="true" height="16" viewBox="0 0 16 16" width="16" fill="#3fb950">'
        '<path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.751.751 0 0 1 .018-1.042'
        '.751.751 0 0 1 1.042-.018L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"></path></svg>'
    ),
    "caret": (
        '<svg class="octicon details-caret" aria-hidden="true" height="16" viewBox="0 0 16 16" width="16" fill="currentColor">'
        '<path d="M6.427 4.427l3.396 3.396a.25.25 0 0 1 0 .354l-3.396 3.396A.25.25 0 0 1 6 11.396V4.604a.25.25 0 0 1 .427-.177Z"></path></svg>'
    ),
    "code": (
        '<svg class="octicon" aria-hidden="true" height="16" viewBox="0 0 16 16" width="16" fill="currentColor">'
        '<path d="m11.28 3.22 4.25 4.25a.75.75 0 0 1 0 1.06l-4.25 4.25a.749.749 0 0 1-1.275-.326.749.749 0 0 1 .215-.734L13.94 8'
        'l-3.72-3.72a.749.749 0 0 1 .326-1.275.749.749 0 0 1 .734.215Zm-6.56 0a.751.751 0 0 1 1.042.018.751.751 0 0 1 .018 1.042L2.06 8'
        'l3.72 3.72a.749.749 0 0 1-.326 1.275.749.749 0 0 1-.734-.215L.47 8.53a.75.75 0 0 1 0-1.06Z"></path></svg>'
    ),
}


def render_assets_box(assets):
    rows_html = []
    for a in assets:
        icon_svg = OCTICONS["package"] if a["icon"] == "package" else OCTICONS["zip"]
        href = f"/releases/download/v1.0.3/{a['slug']}"
        badge_html = f'<span class="asset-badge">{a["badge"]}</span>' if a.get("badge") else ""
        if a["show_sha256"]:
            full_sha = f"sha256:{a['sha256']}"
            short_sha = f"sha256:{a['sha256'][:8]}…"
            sha_html = f"""
              <div class="asset-sha" title="{full_sha}">
                <span class="sha-pill">{short_sha}</span>
                <button type="button" class="copy-btn" data-copy="{full_sha}" aria-label="Copy SHA-256 checksum to clipboard" title="Copy full SHA-256 checksum">
                  {OCTICONS['copy']}
                </button>
              </div>
            """
        else:
            sha_html = '<div class="asset-sha asset-sha--empty"></div>'

        rows_html.append(f"""
          <li class="Box-row asset-row">
            <div class="asset-name-cell">
              {icon_svg}
              <a href="{href}" class="asset-link">{a['display_name']}</a>
              {badge_html}
            </div>
            {sha_html}
            <div class="asset-meta-cell">
              <span class="asset-size">{a['size_human']}</span>
              <relative-time datetime="{a['uploaded_iso']}" title="{a['uploaded_full']}" class="asset-date">{a['uploaded_short']}</relative-time>
            </div>
          </li>
        """)

    return f"""
      <div class="release-assets-footer" id="release-assets">
        <details class="assets-details" open>
          <summary class="assets-summary">
            {OCTICONS['caret']}
            <span class="assets-heading">Assets</span>
            <span class="Counter">{len(assets)}</span>
          </summary>
          <div class="Box Box--condensed mt-2">
            <ul class="asset-list">
              {''.join(rows_html)}
            </ul>
          </div>
        </details>
      </div>
    """


def render_version_selector_widget():
    return """
      <section class="mc-selector-panel" aria-label="Minecraft compatibility and native downloads">
        <div class="mc-selector-header">
          <div>
            <div class="mc-selector-eyebrow">VERSION-SPECIFIC STANDARD LAUNCHER FILES • v1.0.3</div>
            <h3 class="mc-selector-title">Choose the Matching Minecraft Version (Fabric)</h3>
          </div>
          <div class="mc-selector-tabs" role="tablist">
            <button type="button" class="mc-tab-btn is-active" data-mc-tab="1.21.11" role="tab" aria-selected="true">
              Minecraft 1.21.11 <span class="mc-tab-tag">Fabric 0.20.1</span>
            </button>
            <button type="button" class="mc-tab-btn" data-mc-tab="1.20.1" role="tab" aria-selected="false">
              Minecraft 1.20.1 <span class="mc-tab-tag">Fabric 0.14.22+</span>
            </button>
          </div>
        </div>

        <div class="mc-profile-pane is-active" id="mc-pane-1.21.11">
          <div class="mc-specs-grid">
            <div class="mc-spec-box"><span class="mc-spec-label">Client</span><strong>AZYRON CLIENT v1.0.3</strong></div>
            <div class="mc-spec-box"><span class="mc-spec-label">Compatibility</span><strong>1.21.11 • Fabric 0.20.1</strong></div>
            <div class="mc-spec-box"><span class="mc-spec-label">Java Runtime</span><strong>OpenJDK 21+</strong></div>
            <div class="mc-spec-box"><span class="mc-spec-label">Download type</span><strong>Native JAR + matching MRPack</strong></div>
          </div>
          <p class="mc-stack-line">
            <strong>Pinned 1.21.11 dependencies:</strong>
            <code>Fabric API 0.141.6+1.21.11</code> · <code>Sodium 0.8.14</code> · <code>Lithium 0.21.4</code> ·
            <code>FerriteCore 8.2.0</code> · <code>EntityCulling 1.11.3</code> · <code>MoreCulling 1.6.2</code> ·
            <code>Cloth Config 21.11.153</code> · <code>Dynamic FPS 3.11.6</code>
          </p>
          <div class="mc-cta-row">
            <a class="mc-download-btn mc-download-btn--primary" href="/releases/download/v1.0.3/Azyron-Client-1.0.3-mc1.21.11.jar">
              Download Azyron-Client-1.0.3-mc1.21.11.jar
            </a>
            <a class="mc-download-btn mc-download-btn--secondary" href="/releases/download/v1.0.3/Azyron-Modpack-1.0.3-mc1.21.11.mrpack">
              Download Azyron-Modpack-1.0.3-mc1.21.11.mrpack
            </a>
            <code class="mc-cli-pill">./releases/download-dependencies.sh --mc 1.21.11</code>
          </div>
          <p class="mc-stack-line">Use these two files only with Minecraft 1.21.11. The game version is selected by your launcher.</p>
        </div>

        <div class="mc-profile-pane" id="mc-pane-1.20.1">
          <div class="mc-specs-grid">
            <div class="mc-spec-box"><span class="mc-spec-label">Client</span><strong>AZYRON CLIENT v1.0.3</strong></div>
            <div class="mc-spec-box"><span class="mc-spec-label">Compatibility</span><strong>1.20.1 • Fabric 0.14.22+</strong></div>
            <div class="mc-spec-box"><span class="mc-spec-label">Java Runtime</span><strong>OpenJDK 17+ (or 21)</strong></div>
            <div class="mc-spec-box"><span class="mc-spec-label">Download type</span><strong>Native JAR + matching MRPack</strong></div>
          </div>
          <p class="mc-stack-line">
            <strong>Pinned 1.20.1 dependencies:</strong>
            <code>Fabric API 0.88.1+1.20.1</code> · <code>Sodium 0.5.3</code> · <code>Lithium 0.11.2</code> ·
            <code>FerriteCore 6.0.0</code> · <code>EntityCulling 1.6.2</code> · <code>MoreCulling 0.19.0</code> ·
            <code>Cloth Config 11.1.106</code> · <code>Dynamic FPS 3.2.1</code>
          </p>
          <div class="mc-cta-row">
            <a class="mc-download-btn mc-download-btn--primary" href="/releases/download/v1.0.3/Azyron-Client-1.0.3-mc1.20.1.jar">
              Download Azyron-Client-1.0.3-mc1.20.1.jar
            </a>
            <a class="mc-download-btn mc-download-btn--secondary" href="/releases/download/v1.0.3/Azyron-Modpack-1.0.3-mc1.20.1.mrpack">
              Download Azyron-Modpack-1.0.3-mc1.20.1.mrpack
            </a>
            <code class="mc-cli-pill">./releases/download-dependencies.sh --mc 1.20.1</code>
          </div>
          <p class="mc-stack-line">Use these two files only with Minecraft 1.20.1. The game version is selected by your launcher.</p>
        </div>
      </section>
    """



def render_release_body_html(assets):
    sha_rows = "".join(
        f'<tr><td><code>{a["display_name"]}</code></td><td><code>{a["size_bytes"]:,} B</code> (<code>{a["size_human"]}</code>)</td><td><code>{a["sha256"]}</code></td></tr>'
        for a in assets
        if a["show_sha256"]
    )
    selector_html = render_version_selector_widget()
    return f"""
      <div class="markdown-body">
        <blockquote>
          <p><strong>AZYRON CLIENT v1.0.3</strong> by <strong>JIGNESH GAMING</strong>. Standard Fabric launcher downloads are provided as two separate native JAR/MRPack pairs: Minecraft <code>1.20.1</code> and <code>1.21.11</code>. Select the game in your launcher and use only its matching pair; there is no universal JAR or multi-version installer.</p>
        </blockquote>

        {selector_html}

        <table>
          <thead><tr><th>Specification</th><th>Details</th></tr></thead>
          <tbody>
            <tr><td><strong>Client</strong></td><td><strong>AZYRON CLIENT v1.0.3</strong> by <strong>JIGNESH GAMING</strong></td></tr>
            <tr><td><strong>Native targets</strong></td><td>Minecraft <code>1.20.1</code> Fabric and Minecraft <code>1.21.11</code> Fabric; each has its own JAR and MRPack.</td></tr>
            <tr><td><strong>Java runtime</strong></td><td><code>1.20.1</code>: Java 17+; <code>1.21.11</code>: Java 21+.</td></tr>
            <tr><td><strong>Release status</strong></td><td><code>Draft</code>. Do not publish without owner approval.</td></tr>
          </tbody>
        </table>

        <hr>
        <h2>Build and compatibility notes</h2>
        <ul>
          <li>Native builds compile against their selected Minecraft, Yarn, Loader and Fabric API dependencies. The pack builder verifies that the embedded client JAR and Modrinth index target the same Minecraft version.</li>
          <li>The shared client retains its existing Mods Menu, HUD, Armour Hub, cosmetics and performance profiles. The game version is selected in the launcher, not by an in-game profile selector.</li>
          <li>See <code>COMPATIBILITY_1201_BUILD_REPORT.md</code> for native build/audit history and exact package hashes. Historical 26/26 suites used generated Minecraft/Fabric stubs; they are not game-launch tests.</li>
        </ul>

        <hr>
        <h2>Performance profile configuration</h2>
        <p>The low-memory profile configures up to <code>6</code> view / <code>5</code> simulation chunks and a <code>60 FPS</code> cap for the 3GB tier. These are configuration targets, not measured framerates. No 60+ FPS, RAM reduction, crash-free or long-session stability claim is made. Device startup, Mixin application, multiplayer, renderer compatibility and FPS/RAM benchmarks remain untested.</p>

        <hr>
        <h2>Artifact SHA-256 checksums</h2>
        <table>
          <thead><tr><th>Asset filename</th><th>Size</th><th>SHA-256</th></tr></thead>
          <tbody>{sha_rows}</tbody>
        </table>
      </div>
    """



def render_page(view_mode: str, assets):
    upload_short = assets[0]["uploaded_short"]
    upload_full = assets[0]["uploaded_full"]
    upload_iso = assets[0]["uploaded_iso"]
    assets_section = render_assets_box(assets)
    body_html = render_release_body_html(assets)

    if view_mode == "index":
        breadcrumb = """
          <div class="subnav-row">
            <div class="subnav-tabs">
              <span class="subnav-pill is-active">Releases</span>
              <span class="subnav-pill">Tags</span>
            </div>
            <span class="private-draft-note">Private draft • v1.0.3 native builds for 1.20.1 and 1.21.11</span>
          </div>
        """
        main_content = f"""
          <div class="releases-index-grid">
            <div class="releases-rail">
              <div class="rail-date"><relative-time datetime="{upload_iso}" title="{upload_full}">{upload_short}</relative-time></div>
              <div class="rail-item">{OCTICONS['lock']} <span>JIGNESH GAMING</span></div>
              <div class="rail-item">{OCTICONS['tag']} <span class="mono">v1.0.3</span></div>
            </div>
            <div class="Box release-card">
              <div class="release-card-body">
                <div class="release-title-row">
                  <h1 class="release-title"><a href="/releases/tag/v1.0.3" class="release-title-link">AZYRON CLIENT v1.0.3</a></h1>
                  <span class="Label Label--draft" title="Draft Pre-release — awaiting approval">Draft Pre-release</span>
                </div>
                {body_html}
                {assets_section}
              </div>
            </div>
          </div>
        """
    else:
        breadcrumb = """
          <nav class="release-breadcrumb" aria-label="Breadcrumb">
            <a href="/releases" class="breadcrumb-link">Releases</a>
            <span class="breadcrumb-sep">/</span>
            <span class="breadcrumb-current">v1.0.3</span>
          </nav>
        """
        main_content = f"""
          <div class="Box release-card">
            <div class="release-card-body">
              <div class="release-title-row">
                <h1 class="release-title">AZYRON CLIENT v1.0.3</h1>
                <span class="Label Label--draft" title="Draft Pre-release — awaiting approval">Draft Pre-release</span>
              </div>
              <div class="release-meta-bar">
                <span class="avatar-circle">JG</span>
                <strong class="author-name">JIGNESH GAMING</strong>
                <span>staged version-specific native builds <relative-time datetime="{upload_iso}" title="{upload_full}">{upload_short}</relative-time></span>
                <span class="meta-dot">·</span>
                <span class="meta-inline">{OCTICONS['tag']} <span class="mono">v1.0.3</span></span>
                <span class="meta-dot">·</span>
                <span class="meta-inline">Modpack <span class="mono">v1.0.3</span></span>
              </div>
              <div class="release-divider"></div>
              {body_html}
              {assets_section}
            </div>
          </div>
        """

    return f"""<!DOCTYPE html>
<html lang="en" data-color-mode="dark">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>AZYRON CLIENT v1.0.3 · AzyronClientteam/Azyron-Client</title>
  <style>
    :root {{
      --bg-canvas: #0d1117;
      --bg-header: #010409;
      --bg-subtle: #161b22;
      --bg-row-hover: #161b22;
      --border-default: #30363d;
      --border-muted: #21262d;
      --fg-default: #e6edf3;
      --fg-muted: #8b949e;
      --fg-accent: #4493f8;
      --draft-fg: #e3b341;
      --draft-border: rgba(187, 128, 9, 0.4);
      --draft-bg: rgba(187, 128, 9, 0.15);
      --counter-bg: rgba(110, 118, 129, 0.4);
      --azyron-purple: #9333ea;
      --azyron-cyan: #00d4ff;
    }}
    [data-color-mode="light"] {{
      --bg-canvas: #ffffff;
      --bg-header: #f6f8fa;
      --bg-subtle: #f6f8fa;
      --bg-row-hover: #f6f8fa;
      --border-default: #d0d7de;
      --border-muted: #d8dee4;
      --fg-default: #1f2328;
      --fg-muted: #656d76;
      --fg-accent: #0969da;
      --draft-fg: #9a6700;
      --draft-border: rgba(154, 103, 0, 0.4);
      --draft-bg: #fff8c5;
      --counter-bg: rgba(175, 184, 193, 0.2);
    }}
    * {{ box-sizing: border-box; }}
    body {{
      margin: 0;
      background-color: var(--bg-canvas);
      color: var(--fg-default);
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Noto Sans", Helvetica, Arial, sans-serif;
      font-size: 14px;
      line-height: 1.5;
      -webkit-font-smoothing: antialiased;
    }}
    a {{ color: var(--fg-accent); text-decoration: none; }}
    a:hover {{ text-decoration: underline; }}
    .mono, code {{
      font-family: ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, "Liberation Mono", monospace;
    }}
    .gh-header {{
      background: var(--bg-header);
      border-bottom: 1px solid var(--border-default);
      padding: 12px 24px 0 24px;
    }}
    .gh-header-top {{
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 12px;
    }}
    .repo-context {{
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
      color: var(--fg-default);
      font-size: 14px;
    }}
    .repo-owner {{ color: var(--fg-default); font-weight: 400; }}
    .repo-slash {{ color: var(--fg-muted); }}
    .repo-name {{ color: var(--fg-default); font-weight: 600; }}
    .Label {{
      display: inline-flex;
      align-items: center;
      padding: 0 8px;
      font-size: 12px;
      font-weight: 500;
      line-height: 20px;
      border-radius: 2em;
      border: 1px solid var(--border-default);
      color: var(--fg-muted);
    }}
    .Label--draft {{
      color: var(--draft-fg);
      background: var(--draft-bg);
      border-color: var(--draft-border);
      padding: 0 10px;
    }}
    .Label--mc {{
      color: #38bdf8;
      background: rgba(0, 212, 255, 0.12);
      border-color: rgba(0, 212, 255, 0.4);
      font-weight: 600;
      padding: 0 10px;
    }}
    .theme-toggle {{
      background: transparent;
      color: var(--fg-muted);
      border: 1px solid var(--border-default);
      border-radius: 6px;
      padding: 4px 10px;
      font-size: 12px;
      cursor: pointer;
    }}
    .theme-toggle:hover {{ color: var(--fg-default); border-color: var(--fg-muted); }}
    .repo-nav {{ display: flex; gap: 4px; overflow-x: auto; }}
    .repo-nav-item {{
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 8px 12px;
      color: var(--fg-default);
      font-size: 14px;
      border-bottom: 2px solid transparent;
      white-space: nowrap;
    }}
    .repo-nav-item.is-selected {{ font-weight: 600; border-bottom-color: #a855f7; }}
    .container-xl {{ max-width: 1012px; margin: 0 auto; padding: 24px 16px 56px 16px; }}
    .release-breadcrumb {{ display: flex; align-items: center; gap: 6px; margin-bottom: 16px; font-size: 14px; }}
    .breadcrumb-link {{ font-weight: 500; }}
    .breadcrumb-sep {{ color: var(--fg-muted); }}
    .breadcrumb-current {{ color: var(--fg-muted); font-weight: 600; }}
    .subnav-row {{
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 20px;
      gap: 12px;
      flex-wrap: wrap;
    }}
    .subnav-tabs {{
      display: inline-flex;
      border: 1px solid var(--border-default);
      border-radius: 6px;
      overflow: hidden;
    }}
    .subnav-pill {{
      padding: 5px 14px;
      font-size: 14px;
      font-weight: 500;
      color: var(--fg-default);
      background: var(--bg-canvas);
      border-right: 1px solid var(--border-default);
    }}
    .subnav-pill:last-child {{ border-right: none; }}
    .subnav-pill.is-active {{ background: #1f6feb; color: #ffffff; }}
    .private-draft-note {{ color: var(--fg-muted); font-size: 12px; }}
    .releases-index-grid {{ display: grid; grid-template-columns: 170px 1fr; gap: 24px; align-items: start; }}
    @media (max-width: 768px) {{ .releases-index-grid {{ grid-template-columns: 1fr; }} }}
    .releases-rail {{ padding-top: 8px; color: var(--fg-muted); font-size: 13px; display: flex; flex-direction: column; gap: 6px; }}
    .rail-item {{ display: inline-flex; align-items: center; gap: 6px; }}
    .Box {{ background-color: var(--bg-canvas); border: 1px solid var(--border-default); border-radius: 6px; }}
    .release-card-body {{ padding: 24px 32px; }}
    @media (max-width: 640px) {{ .release-card-body {{ padding: 16px; }} }}
    .release-title-row {{ display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 8px; }}
    .release-title {{ margin: 0; font-size: 32px; font-weight: 500; line-height: 1.25; color: var(--fg-default); }}
    .release-title-link {{ color: var(--fg-default); }}
    .release-meta-bar {{ display: flex; align-items: center; flex-wrap: wrap; gap: 6px; color: var(--fg-muted); font-size: 14px; margin-bottom: 16px; }}
    .avatar-circle {{
      display: inline-flex; align-items: center; justify-content: center;
      width: 20px; height: 20px; border-radius: 50%;
      background: linear-gradient(135deg, #9333ea, #00d4ff);
      color: #ffffff; font-size: 10px; font-weight: 700;
    }}
    .author-name {{ color: var(--fg-muted); font-weight: 600; }}
    .meta-inline {{ display: inline-flex; align-items: center; gap: 4px; color: var(--fg-muted); }}
    .release-divider {{ border-top: 1px solid var(--border-muted); margin: 16px 0; }}

    /* Interactive Dual-Version Selector */
    .mc-selector-panel {{
      margin: 18px 0 22px 0;
      padding: 18px 20px;
      border-radius: 8px;
      background: linear-gradient(145deg, rgba(147, 51, 234, 0.10), rgba(0, 212, 255, 0.06));
      border: 1px solid rgba(147, 51, 234, 0.45);
    }}
    .mc-selector-header {{
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 14px;
      flex-wrap: wrap;
      margin-bottom: 14px;
    }}
    .mc-selector-eyebrow {{
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.06em;
      color: #38bdf8;
      margin-bottom: 2px;
    }}
    .mc-selector-title {{
      margin: 0 !important;
      font-size: 17px !important;
      font-weight: 600;
      color: var(--fg-default);
    }}
    .mc-selector-tabs {{
      display: inline-flex;
      gap: 8px;
      flex-wrap: wrap;
    }}
    .mc-tab-btn {{
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 7px 13px;
      border-radius: 6px;
      border: 1px solid var(--border-default);
      background: var(--bg-subtle);
      color: var(--fg-default);
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.14s ease;
    }}
    .mc-tab-btn:hover {{ border-color: #a855f7; }}
    .mc-tab-btn.is-active {{
      background: linear-gradient(135deg, #7e22ce, #0284c7);
      border-color: #38bdf8;
      color: #ffffff;
    }}
    .mc-tab-tag {{
      font-size: 11px;
      font-weight: 500;
      padding: 1px 6px;
      border-radius: 10px;
      background: rgba(0, 0, 0, 0.28);
    }}
    .mc-profile-pane {{ display: none; }}
    .mc-profile-pane.is-active {{ display: block; }}
    .mc-specs-grid {{
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 10px;
      margin-bottom: 12px;
    }}
    .mc-spec-box {{
      padding: 9px 12px;
      border-radius: 6px;
      background: var(--bg-canvas);
      border: 1px solid var(--border-default);
      display: flex;
      flex-direction: column;
      gap: 2px;
    }}
    .mc-spec-label {{ font-size: 11px; color: var(--fg-muted); }}
    .mc-stack-line {{ font-size: 13px; margin: 0 0 14px 0 !important; }}
    .mc-cta-row {{
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
    }}
    .mc-download-btn {{
      display: inline-flex;
      align-items: center;
      padding: 8px 14px;
      border-radius: 6px;
      font-size: 13px;
      font-weight: 600;
      text-decoration: none !important;
    }}
    .mc-download-btn--primary {{
      background: linear-gradient(135deg, #9333ea, #0284c7);
      color: #ffffff !important;
      border: 1px solid rgba(56, 189, 248, 0.6);
    }}
    .mc-download-btn--secondary {{
      background: var(--bg-subtle);
      color: var(--fg-default) !important;
      border: 1px solid var(--border-default);
    }}
    .mc-download-btn--secondary:hover {{ border-color: #38bdf8; }}
    .mc-cli-pill {{ font-size: 12px !important; }}

    .markdown-body {{ font-size: 14px; line-height: 1.6; color: var(--fg-default); }}
    .markdown-body p {{ margin: 0 0 12px 0; }}
    .markdown-body blockquote {{
      margin: 0 0 16px 0;
      padding: 4px 16px;
      color: var(--fg-muted);
      border-left: 0.25em solid #9333ea;
    }}
    .markdown-body hr {{ height: 1px; margin: 20px 0; background-color: var(--border-muted); border: 0; }}
    .markdown-body h2 {{
      font-size: 20px; font-weight: 600; margin: 22px 0 10px 0;
      padding-bottom: 6px; border-bottom: 1px solid var(--border-muted);
    }}
    .markdown-body ul, .markdown-body ol {{ margin: 0 0 12px 0; padding-left: 22px; }}
    .markdown-body li {{ margin-bottom: 5px; }}
    .markdown-body table {{ width: 100%; border-collapse: collapse; margin: 10px 0 16px 0; display: block; overflow-x: auto; }}
    .markdown-body th, .markdown-body td {{ padding: 7px 12px; border: 1px solid var(--border-default); text-align: left; font-size: 13px; }}
    .markdown-body th {{ background-color: var(--bg-subtle); font-weight: 600; }}
    .markdown-body code {{ padding: 0.2em 0.4em; font-size: 85%; background-color: rgba(110, 118, 129, 0.25); border-radius: 6px; }}
    .markdown-body kbd {{
      display: inline-block; padding: 2px 5px;
      font: 11px ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
      color: var(--fg-default); background-color: var(--bg-subtle);
      border: 1px solid var(--border-default); border-radius: 4px;
    }}
    .release-assets-footer {{ margin-top: 24px; padding-top: 16px; border-top: 1px solid var(--border-muted); }}
    .assets-details summary {{ list-style: none; cursor: pointer; display: inline-flex; align-items: center; gap: 6px; padding: 4px 0; }}
    .assets-details summary::-webkit-details-marker {{ display: none; }}
    .details-caret {{ color: var(--fg-muted); transition: transform 0.12s ease-in-out; }}
    .assets-details[open] .details-caret {{ transform: rotate(90deg); }}
    .assets-heading {{ font-size: 16px; font-weight: 600; color: var(--fg-default); }}
    .Counter {{
      display: inline-block; min-width: 20px; padding: 0 6px; font-size: 12px;
      font-weight: 500; line-height: 18px; text-align: center;
      color: var(--fg-default); background-color: var(--counter-bg); border-radius: 2em;
    }}
    .mt-2 {{ margin-top: 10px; }}
    .asset-list {{ list-style: none; margin: 0; padding: 0; }}
    .Box-row {{
      display: grid;
      grid-template-columns: minmax(220px, 1.5fr) minmax(150px, 1fr) auto;
      align-items: center;
      gap: 12px;
      padding: 8px 16px;
      border-top: 1px solid var(--border-muted);
      font-size: 14px;
    }}
    .Box-row:first-child {{ border-top: none; }}
    .Box-row:hover {{ background-color: var(--bg-row-hover); }}
    .asset-name-cell {{ display: flex; align-items: center; gap: 8px; min-width: 0; flex-wrap: wrap; }}
    .asset-icon {{ color: var(--fg-muted); flex-shrink: 0; }}
    .asset-link {{ font-weight: 600; }}
    .asset-badge {{
      font-size: 11px;
      padding: 1px 7px;
      border-radius: 10px;
      background: rgba(147, 51, 234, 0.18);
      border: 1px solid rgba(147, 51, 234, 0.45);
      color: #d8b4fe;
    }}
    .asset-sha {{ display: inline-flex; align-items: center; gap: 4px; color: var(--fg-muted); font-size: 12px; justify-self: end; }}
    .sha-pill {{ font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; color: var(--fg-muted); }}
    .copy-btn {{
      display: inline-flex; align-items: center; justify-content: center;
      background: transparent; border: none; color: var(--fg-muted);
      padding: 2px 4px; border-radius: 4px; cursor: pointer;
    }}
    .copy-btn:hover {{ color: var(--fg-default); background: rgba(110, 118, 129, 0.2); }}
    .asset-meta-cell {{
      display: inline-flex; align-items: center; justify-content: flex-end;
      gap: 16px; color: var(--fg-muted); font-size: 12px; white-space: nowrap; min-width: 150px;
    }}
    .asset-size {{ min-width: 58px; text-align: right; }}
    .asset-date {{ min-width: 76px; text-align: right; }}
    .octicon {{ vertical-align: text-bottom; }}
  </style>
</head>
<body>
  <header class="gh-header">
    <div class="gh-header-top">
      <div class="repo-context">
        <a href="/releases" style="color: var(--fg-default); display: inline-flex; align-items: center;">{OCTICONS['mark-github']}</a>
        {OCTICONS['lock']}
        <span class="repo-owner">AzyronClientteam</span>
        <span class="repo-slash">/</span>
        <a href="/releases/tag/v1.0.3" class="repo-name">Azyron-Client</a>
        <span class="Label">Private</span>
      </div>
      <button type="button" class="theme-toggle" id="themeToggle">Light / Dark</button>
    </div>
    <nav class="repo-nav" aria-label="Repository">
      <a href="/releases/tag/v1.0.3" class="repo-nav-item is-selected">{OCTICONS['code']} <span>Code &amp; Releases (v1.0.3)</span></a>
    </nav>
  </header>

  <main class="container-xl">
    {breadcrumb}
    {main_content}
  </main>

  <script>
    (function () {{
      var copyIcon = {json.dumps(OCTICONS['copy'])};
      var checkIcon = {json.dumps(OCTICONS['check'])};
      document.querySelectorAll('.copy-btn').forEach(function (btn) {{
        btn.addEventListener('click', function () {{
          var text = btn.getAttribute('data-copy') || '';
          if (navigator.clipboard && navigator.clipboard.writeText) {{
            navigator.clipboard.writeText(text);
          }}
          btn.innerHTML = checkIcon;
          setTimeout(function () {{ btn.innerHTML = copyIcon; }}, 1500);
        }});
      }});

      var mcTabs = document.querySelectorAll('[data-mc-tab]');
      mcTabs.forEach(function (tab) {{
        tab.addEventListener('click', function () {{
          var targetVer = tab.getAttribute('data-mc-tab');
          mcTabs.forEach(function (t) {{
            var active = t.getAttribute('data-mc-tab') === targetVer;
            t.classList.toggle('is-active', active);
            t.setAttribute('aria-selected', active ? 'true' : 'false');
          }});
          document.querySelectorAll('.mc-profile-pane').forEach(function (pane) {{
            pane.classList.toggle('is-active', pane.id === 'mc-pane-' + targetVer);
          }});
        }});
      }});

      var toggle = document.getElementById('themeToggle');
      if (toggle) {{
        toggle.addEventListener('click', function () {{
          var root = document.documentElement;
          var next = root.getAttribute('data-color-mode') === 'dark' ? 'light' : 'dark';
          root.setAttribute('data-color-mode', next);
        }});
      }}
    }})();
  </script>
</body>
</html>
"""


class GitHubReleasesHandler(BaseHTTPRequestHandler):
    server_version = "AzyronGitHubReleases/1.0.3"

    def log_message(self, fmt, *args):
        print(f"[Releases] {self.address_string()} - {fmt % args}", flush=True)

    def _send_common_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Cache-Control", "no-cache")

    def do_HEAD(self):
        self._dispatch(head_only=True)

    def do_GET(self):
        self._dispatch(head_only=False)

    def _dispatch(self, head_only: bool):
        parsed = urlparse(self.path)
        path = parsed.path.rstrip("/") or "/"

        assets = load_live_assets()
        by_slug = {a["slug"]: a for a in assets}

        if (
            path in ("/", "/releases/tag/v0.1.0-pre", "/releases/tag/v1.0.2", "/releases/tag/v1.0.3")
            or path.startswith("/releases/tag/untagged-")
            or path.startswith("/releases/edit/untagged-")
        ):
            html = render_page("tag", assets).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(html)))
            self._send_common_headers()
            self.end_headers()
            if not head_only:
                self.wfile.write(html)
            return

        if path == "/releases":
            html = render_page("index", assets).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(html)))
            self._send_common_headers()
            self.end_headers()
            if not head_only:
                self.wfile.write(html)
            return

        if path in ("/api/release", "/api/releases/v0.1.0-pre", "/api/releases/v1.0.3"):
            payload = json.dumps({
                "id": 437365942,
                "tag_name": "v1.0.3",
                "version": "1.0.3",
                "modpack_version": "1.0.3",
                "name": "AZYRON CLIENT v1.0.3",
                "supported_minecraft_versions": ["1.21.11", "1.20.1"],
                "draft": True,
                "verification_environment": "historical generated-stub suites; native build report is separate; game/device launch and benchmarks pending",
                "historical_stub_suites": {
                    "scope": "generated Minecraft/Fabric stubs; historical, not native-build or gameplay acceptance",
                    "1.21.11": {"suite": "AzyronClientTestSuite12111", "passed": 26, "failed": 0},
                    "1.20.1": {"suite": "AzyronClientTestSuite1201", "passed": 26, "failed": 0},
                },
                "gameplay_and_device_benchmarks": "NOT RUN",
                "assets": [
                    {
                        "name": a["display_name"],
                        "download_filename": a["download_filename"],
                        "size": a["size_bytes"],
                        "size_human": a["size_human"],
                        "digest": f"sha256:{a['sha256']}" if a["show_sha256"] else None,
                        "uploaded_at": a["uploaded_iso"],
                        "browser_download_url": f"/releases/download/v1.0.3/{a['slug']}",
                    }
                    for a in assets
                ],
            }, indent=2).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(payload)))
            self._send_common_headers()
            self.end_headers()
            if not head_only:
                self.wfile.write(payload)
            return

        for prefix in ("/releases/download/v0.1.0-pre/", "/releases/download/v1.0.3/", "/downloads/"):
            if path.startswith(prefix):
                slug = path[len(prefix):]
                if slug == "SHA256SUMS.txt":
                    sums_path = os.path.join(RELEASES_DIR, slug)
                    if os.path.isfile(sums_path):
                        with open(sums_path, "rb") as fh:
                            raw_sums = fh.read()
                        self.send_response(200)
                        self.send_header("Content-Type", "text/plain; charset=utf-8")
                        self.send_header("Content-Length", str(len(raw_sums)))
                        self.send_header("Content-Disposition", 'attachment; filename="SHA256SUMS.txt"')
                        self._send_common_headers()
                        self.end_headers()
                        if not head_only:
                            self.wfile.write(raw_sums)
                        return
                asset = by_slug.get(slug)
                if asset and os.path.isfile(asset["disk_path"]):
                    self.send_response(200)
                    self.send_header("Content-Type", asset["content_type"])
                    self.send_header("Content-Length", str(asset["size_bytes"]))
                    self.send_header(
                        "Content-Disposition",
                        f'attachment; filename="{asset["download_filename"]}"',
                    )
                    self.send_header("ETag", f'"sha256:{asset["sha256"]}"')
                    self._send_common_headers()
                    self.end_headers()
                    if not head_only:
                        with open(asset["disk_path"], "rb") as fh:
                            while True:
                                chunk = fh.read(65536)
                                if not chunk:
                                    break
                                self.wfile.write(chunk)
                    return

        self.send_response(404)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self._send_common_headers()
        self.end_headers()
        if not head_only:
            self.wfile.write(b"404 Not Found\n")


def main():
    assets = load_live_assets()
    print(f"Loaded {len(assets)} verified v1.0.3 build assets from {RELEASES_DIR}:", flush=True)
    for a in assets:
        print(f"  - {a['display_name']} ({a['size_human']}, sha256:{a['sha256'][:12]}...)", flush=True)
    server = ThreadingHTTPServer(("0.0.0.0", PORT), GitHubReleasesHandler)
    print(f"AZYRON CLIENT v1.0.3 Releases Portal listening on http://0.0.0.0:{PORT}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
