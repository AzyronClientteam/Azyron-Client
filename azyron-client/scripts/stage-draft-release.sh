#!/usr/bin/env bash
# Stages an UNPUBLISHED Draft Release (v1.0) on GitHub with the exact build artifacts:
#   1. Azyron-Client-1.0.jar
#   2. Azyron-Modpack-1.0.mrpack
#   (+ GitHub's native "Source code (zip)" and "Source code (tar.gz)")
#
# NOTE: Uses --draft strictly so nothing is publicly published until explicitly approved.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLIENT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
RELEASES_DIR="${CLIENT_ROOT}/releases"

JAR_FILE="${RELEASES_DIR}/Azyron-Client-1.0.jar"
MRPACK_FILE="${RELEASES_DIR}/Azyron-Modpack-1.0.mrpack"

if [[ ! -f "${JAR_FILE}" || ! -f "${MRPACK_FILE}" ]]; then
  echo "Error: Missing build artifacts in ${RELEASES_DIR}" >&2
  exit 1
fi

NOTES_FILE="$(mktemp)"
trap 'rm -f "${NOTES_FILE}"' EXIT

cat << 'EOF' > "${NOTES_FILE}"
**AZYRON CLIENT – PERFORMANCE. SMOOTHNESS. STYLE.**
High-FPS Minecraft Java Edition **1.21.11** Fabric client developed by **JIGNESH GAMING**, engineered for desktop launchers and **PojavLauncher**.

### Highlights
- **Target Platform:** Minecraft Java Edition `1.21.11` (Fabric Loader `>=0.17.3`, Java 21).
- **Performance Profiles:** *Potato Mode*, *Balanced Mode*, and *Smooth FPS Mode* with particle throttling, frame-spike stabilization, and safe memory trimming.
- **Client Menu & HUD Editor:** Open with `Right Shift` (or via the Pause Menu on PojavLauncher) to configure modules and drag/scale widgets.
- **Included Modules:** FPS Counter (1% Lows), CPS Counter, Ping Display, Coordinates HUD, Keystrokes HUD, Armor HUD, Potion Effects HUD, Memory HUD, Toggle Sprint, Smooth Zoom, and Custom Crosshair.
EOF

echo "Staging UNPUBLISHED Draft Release v1.0 on GitHub (--draft)..."
gh release create "v1.0" \
  "${JAR_FILE}#Azyron-Client-1.0.jar" \
  "${MRPACK_FILE}#Azyron-Modpack-1.0.mrpack" \
  --draft \
  --title "AZYRON CLIENT 1.0" \
  --notes-file "${NOTES_FILE}"

echo "Draft release v1.0 created (unpublished). Review on GitHub Releases before approving public release."
