#!/usr/bin/env python3
"""
AZYRON CLIENT — Authentic GitHub Releases Page Server (Private / Draft Mode)
Serves the standard GitHub Releases layout (/releases and /releases/tag/v1.0)
with expandable Assets backed by the actual compiled Minecraft 1.21.11 build files.
"""

from datetime import datetime, timezone
import hashlib
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
from urllib.parse import urlparse

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RELEASES_DIR = os.path.join(BASE_DIR, "releases")
PORT = int(os.environ.get("PORT", "8080"))

ASSET_DEFINITIONS = [
    {
        "slug": "Azyron-Client-1.0.1.jar",
        "display_name": "Azyron-Client-1.0.1.jar",
        "download_filename": "Azyron-Client-1.0.1.jar",
        "disk_filename": "Azyron-Client-1.0.1.jar",
        "content_type": "application/java-archive",
        "icon": "package",
        "show_sha256": True,
    },
    {
        "slug": "Azyron-Modpack-1.0.mrpack",
        "display_name": "Azyron-Modpack-1.0.mrpack",
        "download_filename": "Azyron-Modpack-1.0.mrpack",
        "disk_filename": "Azyron-Modpack-1.0.mrpack",
        "content_type": "application/octet-stream",
        "icon": "package",
        "show_sha256": True,
    },
    {
        "slug": "Azyron-Client-Source-v1.0.1.zip",
        "display_name": "Azyron-Client-Source-v1.0.1.zip",
        "download_filename": "Azyron-Client-Source-v1.0.1.zip",
        "disk_filename": "Azyron-Client-Source-v1.0.1.zip",
        "content_type": "application/zip",
        "icon": "package",
        "show_sha256": True,
    },
    {
        "slug": "Azyron-Client-Source-v1.0.1.tar.gz",
        "display_name": "Azyron-Client-Source-v1.0.1.tar.gz",
        "download_filename": "Azyron-Client-Source-v1.0.1.tar.gz",
        "disk_filename": "Azyron-Client-Source-v1.0.1.tar.gz",
        "content_type": "application/gzip",
        "icon": "package",
        "show_sha256": True,
    },
    {
        "slug": "Source-code.zip",
        "display_name": "Source code (zip)",
        "download_filename": "Azyron-Client-Source-v1.0.1.zip",
        "disk_filename": "Azyron-Client-Source-v1.0.1.zip",
        "content_type": "application/zip",
        "icon": "zip",
        "show_sha256": False,
    },
    {
        "slug": "Source-code.tar.gz",
        "display_name": "Source code (tar.gz)",
        "download_filename": "Azyron-Client-Source-v1.0.1.tar.gz",
        "disk_filename": "Azyron-Client-Source-v1.0.1.tar.gz",
        "content_type": "application/gzip",
        "icon": "zip",
        "show_sha256": False,
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
            digest = hashlib.sha256(fh.read()).hexdigest()
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
        href = f"/releases/download/v0.1.0-pre/{a['slug']}"
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
            </div>
            {sha_html}
            <div class="asset-meta-cell">
              <span class="asset-size">{a['size_human']}</span>
              <relative-time datetime="{a['uploaded_iso']}" title="{a['uploaded_full']}" class="asset-date">{a['uploaded_short']}</relative-time>
            </div>
          </li>
        """)

    return f"""
      <div class="release-assets-footer">
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


def render_release_body_html():
    return """
      <div class="markdown-body">
        <blockquote>
          <p><strong>AZYRON CLIENT — PERFORMANCE. SMOOTHNESS. STYLE.</strong><br>
          Welcome to the official <strong>v0.1.0 Pre-Release</strong> of <strong>AZYRON CLIENT</strong>, crafted by <strong>JIGNESH GAMING</strong> exclusively for <strong>Minecraft Java Edition <code>1.21.11</code> (Fabric)</strong>. Thank you for testing our early-access foundation build engineered for ultra-clean visuals, rock-solid frame pacing, and effortless compatibility across desktop launchers and <strong>PojavLauncher</strong>.</p>
        </blockquote>

        <table>
          <thead>
            <tr><th>Specification</th><th>Details</th></tr>
          </thead>
          <tbody>
            <tr><td><strong>Release Version</strong></td><td><code>AZYRON CLIENT v0.1.0 PRE-RELEASE</code> (<code>v0.1.0-pre</code>)</td></tr>
            <tr><td><strong>Release Channel</strong></td><td><code>Pre-release</code> <em>(Early Testing &amp; Performance Validation)</em></td></tr>
            <tr><td><strong>Minecraft Version</strong></td><td><strong>Java Edition <code>1.21.11</code> — Fabric</strong> <em>(Strictly <code>1.21.11</code>)</em></td></tr>
            <tr><td><strong>Fabric Requirements</strong></td><td>Fabric Loader <code>0.20.1</code> <em>(min <code>&gt;=0.17.3</code>)</em> + Fabric API <code>0.141.6+1.21.11</code></td></tr>
            <tr><td><strong>Java Runtime</strong></td><td><strong>Java 21 LTS</strong> <em>(64-bit OpenJDK 21)</em></td></tr>
            <tr><td><strong>Developer</strong></td><td><strong>JIGNESH GAMING</strong></td></tr>
          </tbody>
        </table>

        <hr>

        <h2>Client Introduction</h2>
        <p><strong>AZYRON CLIENT</strong> is a modern, lightweight, fair-play <strong>Fabric client and optimization suite</strong> built from the ground up for <strong>Minecraft Java Edition <code>1.21.11</code></strong>. Designed for competitive multiplayer, survival worlds, and low-memory mobile/ARM64 devices running <strong>PojavLauncher</strong>, AZYRON combines a custom dark-themed client interface, a drag-and-drop HUD editor, and a real-time frame-pacing &amp; particle-culling engine — with <strong>zero</strong> unfair gameplay modifications or bloatware.</p>

        <hr>

        <h2>FPS Optimization Features</h2>
        <ul>
          <li><strong>Real-Time Performance Engine (<code>PerformanceEngine</code> &amp; <code>FrameTimeTracker</code>)</strong> — Tracks rolling frame-time percentiles (<code>1%</code> lows and micro-stutter spikes) and coordinates render settings without stalling the main render thread.</li>
          <li><strong>Distance &amp; Budget Particle Throttling (<code>ParticleManagerMixin</code>)</strong> — Enforces per-tick particle caps and distance cutoffs during heavy combat, explosions, and crowded server lobbies.</li>
          <li><strong>Smart Heap &amp; GC Hygiene (<code>MemoryOptimizer</code>)</strong> — Low-allocation tick/HUD rendering loop designed to minimize garbage-collection pauses on <code>1.5 GB – 4 GB</code> RAM allocations.</li>
          <li><strong>Companion Optimization Config Seeding</strong> — Automatically seeds tuned defaults for the <code>1.21.11</code> Fabric optimization stack (<strong>Sodium <code>0.8.14</code></strong>, <strong>Lithium <code>0.21.4</code></strong>, <strong>FerriteCore <code>8.2.0</code></strong>, <strong>EntityCulling <code>1.11.3</code></strong>, <strong>More Culling <code>1.6.2</code></strong>, <strong>ImmediatelyFast <code>1.14.3</code></strong>, and <strong>Dynamic FPS <code>3.11.6</code></strong>) when installed via the Modpack.</li>
          <li><strong>11 Built-In Modular HUD &amp; Utility Widgets</strong> — <code>FPS Counter</code>, <code>CPS Counter</code>, <code>Ping Display</code>, <code>Coordinates &amp; Direction</code>, <code>Keystrokes</code>, <code>Armor Status HUD</code>, <code>Potion Effects HUD</code>, <code>Memory Usage HUD</code>, <code>Custom Crosshair</code>, <code>ToggleSprint</code>, and <code>Smooth Cinematic/PvP Zoom</code>, plus custom <code>AZYRON</code> Main Menu, Splash Loading Renderer, Cosmetics Manager, and Pause Menu integration.</li>
        </ul>

        <hr>

        <h2>Performance Profiles</h2>
        <p>Switch instantly in-game with <kbd>F8</kbd> (or inside the <kbd>Right Shift</kbd> AZYRON Menu) between three engineered performance presets:</p>
        <table>
          <thead>
            <tr>
              <th>Profile</th>
              <th>Graphics &amp; Clouds</th>
              <th>Render / Sim</th>
              <th>Particle Budget</th>
              <th>Entity Scale &amp; Shadows</th>
              <th>Target Cap</th>
              <th>Best For</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><strong>Potato Mode</strong> (<code>POTATO</code>)</td>
              <td><code>Fast</code> · Clouds <code>Off</code></td>
              <td><code>6</code> / <code>5</code> chunks</td>
              <td><code>Minimal</code> (<code>&lt;=40/tick</code>, <code>18m</code>)</td>
              <td><code>50%</code> · Shadows <code>Off</code></td>
              <td><code>260 FPS</code></td>
              <td>Entry-level laptops, iGPUs &amp; <strong>PojavLauncher</strong> (<code>1.5–2 GB</code> RAM)</td>
            </tr>
            <tr>
              <td><strong>Balanced Mode</strong> (<code>BALANCED</code>)</td>
              <td><code>Fancy</code> · Clouds <code>Fast</code></td>
              <td><code>10</code> / <code>8</code> chunks</td>
              <td><code>Decreased</code> (<code>&lt;=180/tick</code>, <code>42m</code>)</td>
              <td><code>75%</code> · Shadows <code>On</code></td>
              <td><code>260 FPS</code></td>
              <td>Everyday survival &amp; multiplayer with crisp visuals and high FPS</td>
            </tr>
            <tr>
              <td><strong>Smooth FPS Mode</strong> (<code>SMOOTH_FPS</code>)</td>
              <td><code>Fast</code> · Clouds <code>Fast</code></td>
              <td><code>8</code> / <code>6</code> chunks</td>
              <td><code>Decreased</code> (<code>&lt;=100/tick</code>, <code>28m</code>)</td>
              <td><code>75%</code> · Shadows <code>Off</code></td>
              <td><code>120 FPS</code></td>
              <td>Competitive PvP, consistent <code>1%</code> lows &amp; mobile thermal stability</td>
            </tr>
          </tbody>
        </table>

        <hr>

        <h2>Supported Minecraft Version &amp; Platforms</h2>
        <ul>
          <li><strong>Supported Game Version:</strong> <strong>Minecraft: Java Edition <code>1.21.11</code> ONLY</strong> <em>(strictly pinned to <code>1.21.11</code> intermediary mappings)</em></li>
          <li><strong>Mod Loader:</strong> <strong>Fabric Loader <code>0.20.1</code></strong> (<code>&gt;=0.17.3</code>) with <strong>Fabric API <code>0.141.6+1.21.11</code></strong></li>
          <li><strong>Java Runtime:</strong> <strong>Java <code>21</code></strong> (<code>major class version 65</code>)</li>
          <li><strong>Supported Launchers:</strong> Modrinth App, Prism Launcher, MultiMC, Official Minecraft Launcher, and <strong>PojavLauncher</strong> (Android / iOS ARM64)</li>
        </ul>

        <hr>

        <h2>Installation Instructions</h2>
        <h3>Option A — Full Modpack (<code>Azyron-Modpack-1.0.mrpack</code>) <em>(Recommended for Maximum FPS)</em></h3>
        <ol>
          <li>Open <strong>Modrinth App</strong> or <strong>Prism Launcher</strong>.</li>
          <li>Click <strong>Add Instance</strong> &rarr; <strong>Import from file</strong> and select <strong><code>Azyron-Modpack-1.0.mrpack</code></strong>.</li>
          <li>The launcher will automatically fetch Minecraft <code>1.21.11</code>, Fabric Loader <code>0.20.1</code>, all <strong>9 verified optimization dependencies</strong>, and install AZYRON CLIENT with pre-tuned configs.</li>
          <li>Launch the instance and press <kbd>Right Shift</kbd> in-game to customize modules.</li>
        </ol>

        <h3>Option B — Standalone Mod JAR (<code>Azyron-Client-1.0.1.jar</code>) <em>(PojavLauncher &amp; Official Launcher)</em></h3>
        <ol>
          <li>Install <strong>Minecraft <code>1.21.11</code></strong> and <strong>Fabric Loader <code>0.20.1</code></strong> in your launcher and select a <strong>Java 21</strong> runtime.</li>
          <li>Download <strong><code>Azyron-Client-1.0.1.jar</code></strong> from the <strong>Assets</strong> section below and download <strong><code>fabric-api-0.141.6+1.21.11.jar</code></strong>.</li>
          <li>Copy both <code>.jar</code> files into your <code>.minecraft/mods/</code> directory <em>(recommended RAM: <code>1536 MB – 2048 MB</code> on PojavLauncher, <code>2048 MB – 4096 MB</code> on Desktop)</em>.</li>
          <li>Launch the <code>fabric-loader-0.20.1-1.21.11</code> profile.</li>
        </ol>

        <h3>Default In-Game Controls</h3>
        <table>
          <thead>
            <tr><th>Keybind</th><th>Action</th></tr>
          </thead>
          <tbody>
            <tr><td><kbd>Right Shift</kbd></td><td>Open <strong>AZYRON CLIENT</strong> Modules &amp; Settings Menu</td></tr>
            <tr><td><kbd>H</kbd></td><td>Open Drag-and-Drop <strong>HUD Layout Editor</strong></td></tr>
            <tr><td><kbd>C</kbd> <em>(Hold)</em></td><td>Smooth <strong>Zoom</strong></td></tr>
            <tr><td><kbd>F8</kbd></td><td>Cycle <strong>Performance Profile</strong> (<code>Potato</code> &rarr; <code>Balanced</code> &rarr; <code>Smooth FPS</code>)</td></tr>
            <tr><td><kbd>I</kbd></td><td>Toggle <strong>Auto-Sprint</strong> (<code>ToggleSprint</code>)</td></tr>
          </tbody>
        </table>

        <hr>

        <h2>Known Issues (<code>v0.1.0-pre</code>)</h2>
        <ul>
          <li><strong>Pre-Release Build:</strong> Early <code>v0.1.0-pre</code> validation release; configuration keys in <code>config/azyron-client.json</code> may evolve prior to <code>v0.1.0</code> stable.</li>
          <li><strong>PojavLauncher Legacy Renderers:</strong> On older Android GPUs using legacy <code>GL4ES</code> renderers with <code>&lt;1.5 GB</code> RAM allocated, cycling profiles inside densely populated worlds may briefly pause for <code>1–2s</code> while chunk meshes reload <em>(ANGLE/Zink/LTW with Java 21 is recommended)</em>.</li>
          <li><strong>External Shader Packs:</strong> Heavy third-party ray-traced shader packs are not bundled and may override <code>Potato Mode</code> / <code>Smooth FPS Mode</code> frame-rate targets.</li>
          <li><strong>Ultra-Wide Aspect Ratios:</strong> HUD snap-grid guides on <code>21:9</code> and <code>32:9</code> displays are scheduled for further polish in the next milestone.</li>
        </ul>

        <hr>

        <h2>Changelog (<code>v0.1.0-pre</code>)</h2>
        <ul>
          <li><strong>Added:</strong> Initial <code>1.21.11</code> Fabric client core (<code>AzyronClient</code>), JSON persistence (<code>AzyronConfig</code>), and headless test bootstrap (<code>8/8</code> automated test suites passing).</li>
          <li><strong>Added:</strong> Custom branded <strong>AZYRON Main Menu</strong>, <strong>Splash Loading Overlay</strong>, <strong>Mods &amp; Settings Screen</strong>, <strong>Cosmetics Screen</strong>, and <strong>Draggable HUD Editor</strong>.</li>
          <li><strong>Added:</strong> <strong>11 client modules</strong> (<code>FPS</code>, <code>CPS</code>, <code>Ping</code>, <code>Coordinates</code>, <code>Keystrokes</code>, <code>Armor HUD</code>, <code>Potion HUD</code>, <code>Memory HUD</code>, <code>Crosshair</code>, <code>ToggleSprint</code>, <code>Zoom</code>).</li>
          <li><strong>Added:</strong> <strong>3 one-click Performance Profiles</strong> (<code>Potato Mode</code>, <code>Balanced Mode</code>, <code>Smooth FPS Mode</code>) with particle budget culling (<code>ParticleManagerMixin</code>) and companion mod config seeding.</li>
          <li><strong>Packaged:</strong> Verified <code>Azyron-Client-1.0.1.jar</code> (<code>150.47 KB</code>) and Modrinth v1 modpack <code>Azyron-Modpack-1.0.mrpack</code> (<code>148.02 KB</code>) pinning <code>9</code> SHA-512-verified <code>1.21.11</code> Fabric optimization mods.</li>
        </ul>

        <hr>

        <h2>Artifact Verification (SHA-256)</h2>
        <table>
          <thead>
            <tr><th>Asset Filename</th><th>Size</th><th>SHA-256 Checksum</th></tr>
          </thead>
          <tbody>
            <tr><td><code>Azyron-Client-1.0.1.jar</code></td><td><code>154,079 B</code> (<code>150 KB</code>)</td><td><code>4f4edc88561e55d27a1f027ddbe9b5558c7b0b0c965e55b89beafe70ba9d0834</code></td></tr>
            <tr><td><code>Azyron-Modpack-1.0.mrpack</code></td><td><code>151,573 B</code> (<code>148 KB</code>)</td><td><code>4cb083e369222c032837bf6eee456ccdd820939c3a5623f17a255db7d679b14a</code></td></tr>
          </tbody>
        </table>

        <hr>

        <h2>Credits</h2>
        <ul>
          <li><strong>Creator &amp; Lead Developer:</strong> <strong>JIGNESH GAMING</strong> — <em>AZYRON CLIENT Architecture, UI/UX &amp; Performance Engineering</em></li>
          <li><strong>Platform &amp; Optimization Ecosystem:</strong> Special thanks to <strong>Mojang Studios</strong>, the <strong>FabricMC Team</strong>, <strong>CaffeineMC</strong> (<em>Sodium &amp; Lithium</em>), <strong>malte0811</strong> (<em>FerriteCore</em>), <strong>tr7zw</strong> (<em>EntityCulling</em>), <strong>FX-PR0CESS</strong> (<em>More Culling</em>), <strong>RK_01</strong> (<em>ImmediatelyFast</em>), <strong>juliand665 &amp; LostLuma</strong> (<em>Dynamic FPS</em>), <strong>shedaniel</strong> (<em>Cloth Config</em>), and the <strong>PojavLauncher Team</strong>.</li>
        </ul>
      </div>
    """


def render_page(view_mode: str, assets):
    upload_short = assets[0]["uploaded_short"]
    upload_full = assets[0]["uploaded_full"]
    upload_iso = assets[0]["uploaded_iso"]
    assets_section = render_assets_box(assets)
    body_html = render_release_body_html()

    if view_mode == "index":
        breadcrumb = """
          <div class="subnav-row">
            <div class="subnav-tabs">
              <span class="subnav-pill is-active">Releases</span>
              <span class="subnav-pill">Tags</span>
            </div>
            <span class="private-draft-note">Private staging • Auto-publish disabled</span>
          </div>
        """
        main_content = f"""
          <div class="releases-index-grid">
            <div class="releases-rail">
              <div class="rail-date"><relative-time datetime="{upload_iso}" title="{upload_full}">{upload_short}</relative-time></div>
              <div class="rail-item">{OCTICONS['lock']} <span>JIGNESH GAMING</span></div>
              <div class="rail-item">{OCTICONS['tag']} <span class="mono">v0.1.0-pre</span></div>
              <div class="rail-item">{OCTICONS['commit']} <span class="mono">97dc3e5</span></div>
            </div>
            <div class="Box release-card">
              <div class="release-card-body">
                <div class="release-title-row">
                  <h1 class="release-title"><a href="/releases/tag/v0.1.0-pre" class="release-title-link">AZYRON CLIENT v0.1.0 PRE-RELEASE</a></h1>
                  <span class="Label Label--draft" title="Unpublished draft release — kept private until approved">Draft</span>
                  <span class="Label Label--prerelease" title="This release is labeled as non-production ready">Pre-release</span>
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
            <span class="breadcrumb-current">v0.1.0-pre</span>
          </nav>
        """
        main_content = f"""
          <div class="Box release-card">
            <div class="release-card-body">
              <div class="release-title-row">
                <h1 class="release-title">AZYRON CLIENT v0.1.0 PRE-RELEASE</h1>
                <span class="Label Label--draft" title="Unpublished draft release — kept private until approved">Draft</span>
                <span class="Label Label--prerelease" title="This release is labeled as non-production ready">Pre-release</span>
              </div>
              <div class="release-meta-bar">
                <span class="avatar-circle">JG</span>
                <strong class="author-name">JIGNESH GAMING</strong>
                <span>created this draft pre-release <relative-time datetime="{upload_iso}" title="{upload_full}">{upload_short}</relative-time></span>
                <span class="meta-dot">·</span>
                <span class="meta-inline">{OCTICONS['tag']} <span class="mono">v0.1.0-pre</span></span>
                <span class="meta-dot">·</span>
                <span class="meta-inline">{OCTICONS['commit']} <span class="mono">97dc3e5</span></span>
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
  <title>Release AZYRON CLIENT v0.1.0 PRE-RELEASE · AzyronClientteam/Azyron-Client</title>
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
    a {{
      color: var(--fg-accent);
      text-decoration: none;
    }}
    a:hover {{
      text-decoration: underline;
    }}
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
      padding: 0 7px;
      font-size: 12px;
      font-weight: 500;
      line-height: 18px;
      border-radius: 2em;
      border: 1px solid var(--border-default);
      color: var(--fg-muted);
    }}
    .Label--draft {{
      color: var(--draft-fg);
      background: var(--draft-bg);
      border-color: var(--draft-border);
      font-size: 12px;
      line-height: 20px;
      padding: 0 10px;
    }}
    .Label--prerelease {{
      color: #d29922;
      background: transparent;
      border-color: rgba(210, 153, 34, 0.5);
      font-size: 12px;
      line-height: 20px;
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
    .repo-nav {{
      display: flex;
      gap: 4px;
      overflow-x: auto;
    }}
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
    .repo-nav-item.is-selected {{
      font-weight: 600;
      border-bottom-color: #f78166;
    }}
    .repo-nav-item:hover {{
      text-decoration: none;
      background: var(--bg-subtle);
      border-top-left-radius: 6px;
      border-top-right-radius: 6px;
    }}
    .container-xl {{
      max-width: 1012px;
      margin: 0 auto;
      padding: 24px 16px 56px 16px;
    }}
    .release-breadcrumb {{
      display: flex;
      align-items: center;
      gap: 6px;
      margin-bottom: 16px;
      font-size: 14px;
    }}
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
    .subnav-pill.is-active {{
      background: #1f6feb;
      color: #ffffff;
    }}
    .private-draft-note {{
      color: var(--fg-muted);
      font-size: 12px;
    }}
    .releases-index-grid {{
      display: grid;
      grid-template-columns: 170px 1fr;
      gap: 24px;
      align-items: start;
    }}
    @media (max-width: 768px) {{
      .releases-index-grid {{ grid-template-columns: 1fr; }}
    }}
    .releases-rail {{
      padding-top: 8px;
      color: var(--fg-muted);
      font-size: 13px;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }}
    .rail-date {{ margin-bottom: 4px; }}
    .rail-item {{
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }}
    .Box {{
      background-color: var(--bg-canvas);
      border: 1px solid var(--border-default);
      border-radius: 6px;
    }}
    .release-card-body {{
      padding: 24px 32px;
    }}
    @media (max-width: 640px) {{
      .release-card-body {{ padding: 16px; }}
    }}
    .release-title-row {{
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
      margin-bottom: 8px;
    }}
    .release-title {{
      margin: 0;
      font-size: 32px;
      font-weight: 400;
      line-height: 1.25;
      color: var(--fg-default);
    }}
    .release-title-link {{ color: var(--fg-default); }}
    .release-title-link:hover {{ color: var(--fg-accent); text-decoration: none; }}
    .release-meta-bar {{
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 6px;
      color: var(--fg-muted);
      font-size: 14px;
      margin-bottom: 16px;
    }}
    .avatar-circle {{
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 20px;
      height: 20px;
      border-radius: 50%;
      background: linear-gradient(135deg, #9333ea, #00d4ff);
      color: #ffffff;
      font-size: 10px;
      font-weight: 700;
    }}
    .author-name {{ color: var(--fg-muted); font-weight: 600; }}
    .meta-inline {{
      display: inline-flex;
      align-items: center;
      gap: 4px;
      color: var(--fg-muted);
    }}
    .release-divider {{
      border-top: 1px solid var(--border-muted);
      margin: 16px 0;
    }}
    .markdown-body {{
      font-size: 14px;
      line-height: 1.6;
      color: var(--fg-default);
    }}
    .markdown-body p {{ margin: 0 0 12px 0; }}
    .markdown-body blockquote {{
      margin: 0 0 16px 0;
      padding: 4px 16px;
      color: var(--fg-muted);
      border-left: 0.25em solid var(--border-default);
    }}
    .markdown-body blockquote p:last-child {{ margin-bottom: 0; }}
    .markdown-body hr {{
      height: 1px;
      padding: 0;
      margin: 20px 0;
      background-color: var(--border-muted);
      border: 0;
    }}
    .markdown-body h2 {{
      font-size: 20px;
      font-weight: 600;
      margin: 22px 0 10px 0;
      padding-bottom: 6px;
      border-bottom: 1px solid var(--border-muted);
    }}
    .markdown-body h3 {{
      font-size: 15px;
      font-weight: 600;
      margin: 16px 0 8px 0;
    }}
    .markdown-body ul, .markdown-body ol {{
      margin: 0 0 12px 0;
      padding-left: 22px;
    }}
    .markdown-body li {{ margin-bottom: 5px; }}
    .markdown-body table {{
      width: 100%;
      border-collapse: collapse;
      margin: 10px 0 16px 0;
      display: block;
      overflow-x: auto;
    }}
    .markdown-body th, .markdown-body td {{
      padding: 7px 12px;
      border: 1px solid var(--border-default);
      text-align: left;
      font-size: 13px;
    }}
    .markdown-body th {{
      background-color: var(--bg-subtle);
      font-weight: 600;
    }}
    .markdown-body tr:nth-child(2n) td {{
      background-color: rgba(110, 118, 129, 0.06);
    }}
    .markdown-body code {{
      padding: 0.2em 0.4em;
      font-size: 85%;
      background-color: rgba(110, 118, 129, 0.25);
      border-radius: 6px;
    }}
    .markdown-body kbd {{
      display: inline-block;
      padding: 2px 5px;
      font: 11px ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
      color: var(--fg-default);
      background-color: var(--bg-subtle);
      border: 1px solid var(--border-default);
      border-radius: 4px;
    }}
    .release-assets-footer {{
      margin-top: 24px;
      padding-top: 16px;
      border-top: 1px solid var(--border-muted);
    }}
    .assets-details summary {{
      list-style: none;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      user-select: none;
      padding: 4px 0;
    }}
    .assets-details summary::-webkit-details-marker {{ display: none; }}
    .details-caret {{
      color: var(--fg-muted);
      transition: transform 0.12s ease-in-out;
    }}
    .assets-details[open] .details-caret {{
      transform: rotate(90deg);
    }}
    .assets-heading {{
      font-size: 16px;
      font-weight: 600;
      color: var(--fg-default);
    }}
    .Counter {{
      display: inline-block;
      min-width: 20px;
      padding: 0 6px;
      font-size: 12px;
      font-weight: 500;
      line-height: 18px;
      text-align: center;
      color: var(--fg-default);
      background-color: var(--counter-bg);
      border-radius: 2em;
    }}
    .mt-2 {{ margin-top: 10px; }}
    .asset-list {{
      list-style: none;
      margin: 0;
      padding: 0;
    }}
    .Box-row {{
      display: grid;
      grid-template-columns: minmax(190px, 1.4fr) minmax(150px, 1fr) auto;
      align-items: center;
      gap: 12px;
      padding: 8px 16px;
      border-top: 1px solid var(--border-muted);
      font-size: 14px;
    }}
    .Box-row:first-child {{ border-top: none; }}
    .Box-row:hover {{ background-color: var(--bg-row-hover); }}
    @media (max-width: 680px) {{
      .Box-row {{
        grid-template-columns: 1fr;
        gap: 4px;
      }}
      .asset-sha--empty {{ display: none; }}
    }}
    .asset-name-cell {{
      display: flex;
      align-items: center;
      gap: 8px;
      min-width: 0;
    }}
    .asset-icon {{
      color: var(--fg-muted);
      flex-shrink: 0;
    }}
    .asset-link {{
      font-weight: 600;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }}
    .asset-sha {{
      display: inline-flex;
      align-items: center;
      gap: 4px;
      color: var(--fg-muted);
      font-size: 12px;
      justify-self: end;
    }}
    @media (max-width: 680px) {{
      .asset-sha {{ justify-self: start; padding-left: 24px; }}
    }}
    .sha-pill {{
      font-family: ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, monospace;
      color: var(--fg-muted);
    }}
    .copy-btn {{
      display: inline-flex;
      align-items: center;
      justify-content: center;
      background: transparent;
      border: none;
      color: var(--fg-muted);
      padding: 2px 4px;
      border-radius: 4px;
      cursor: pointer;
    }}
    .copy-btn:hover {{
      color: var(--fg-default);
      background: rgba(110, 118, 129, 0.2);
    }}
    .asset-meta-cell {{
      display: inline-flex;
      align-items: center;
      justify-content: flex-end;
      gap: 16px;
      color: var(--fg-muted);
      font-size: 12px;
      white-space: nowrap;
      min-width: 150px;
    }}
    @media (max-width: 680px) {{
      .asset-meta-cell {{
        justify-content: flex-start;
        padding-left: 24px;
      }}
    }}
    .asset-size {{
      color: var(--fg-muted);
      min-width: 58px;
      text-align: right;
    }}
    .asset-date {{
      color: var(--fg-muted);
      min-width: 76px;
      text-align: right;
    }}
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
        <a href="/releases/tag/v0.1.0-pre" class="repo-name">Azyron-Client</a>
        <span class="Label">Private</span>
      </div>
      <button type="button" class="theme-toggle" id="themeToggle">Light / Dark</button>
    </div>
    <nav class="repo-nav" aria-label="Repository">
      <a href="/releases/tag/v0.1.0-pre" class="repo-nav-item is-selected">{OCTICONS['code']} <span>Code &amp; Releases</span></a>
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
    server_version = "AzyronGitHubReleases/1.0"

    def log_message(self, fmt, *args):
        print(f"[Releases] {self.address_string()} - {fmt % args}", flush=True)

    def _send_common_headers(self):
        # Permit Arena preview iframe & cross-origin preview hosts cleanly
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
        if "Azyron-Client-1.0.1.jar" in by_slug:
            by_slug["Azyron-Client-1.0.jar"] = by_slug["Azyron-Client-1.0.1.jar"]

        if (
            path in ("/", "/releases/tag/v0.1.0-pre", "/releases/tag/v1.0")
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

        if path in ("/api/releases/v0.1.0-pre", "/api/releases/v1.0"):
            payload = json.dumps({
                "id": 437365942,
                "tag_name": "v0.1.0-pre",
                "name": "AZYRON CLIENT v0.1.0 PRE-RELEASE",
                "draft": True,
                "prerelease": True,
                "assets": [
                    {
                        "name": a["display_name"],
                        "download_filename": a["download_filename"],
                        "size": a["size_bytes"],
                        "size_human": a["size_human"],
                        "digest": f"sha256:{a['sha256']}" if a["show_sha256"] else None,
                        "uploaded_at": a["uploaded_iso"],
                        "browser_download_url": f"/releases/download/v0.1.0-pre/{a['slug']}",
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

        for prefix in ("/releases/download/v0.1.0-pre/", "/releases/download/v1.0/"):
            if path.startswith(prefix):
                slug = path[len(prefix):]
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
    print(f"Loaded {len(assets)} verified build assets from {RELEASES_DIR}:", flush=True)
    for a in assets:
        print(f"  - {a['display_name']} ({a['size_human']}, sha256:{a['sha256'][:12]}...)", flush=True)
    server = ThreadingHTTPServer(("0.0.0.0", PORT), GitHubReleasesHandler)
    print(f"AZYRON CLIENT GitHub Releases server listening on http://0.0.0.0:{PORT}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
