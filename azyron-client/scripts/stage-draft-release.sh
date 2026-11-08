#!/usr/bin/env bash
# Stages all 4 verified AZYRON CLIENT v1.0.2 release assets on GitHub
# while keeping the release strictly in DRAFT mode (unpublished):
#   1. Azyron-Client-1.0.2.jar
#   2. Azyron-Modpack-1.0.2.mrpack
#   3. Azyron-Client-Source-v1.0.2.zip
#   4. Azyron-Client-Source-v1.0.2.tar.gz

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLIENT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
RELEASES_DIR="${CLIENT_ROOT}/releases"
NOTES_FILE="${CLIENT_ROOT}/RELEASE_NOTES_v1.0.2.md"
REPO="${GITHUB_REPOSITORY:-AzyronClientteam/Azyron-Client}"
TAG="v1.0.2"

JAR_FILE="${RELEASES_DIR}/Azyron-Client-1.0.2.jar"
MRPACK_FILE="${RELEASES_DIR}/Azyron-Modpack-1.0.2.mrpack"
SRC_ZIP="${RELEASES_DIR}/Azyron-Client-Source-v1.0.2.zip"
SRC_TGZ="${RELEASES_DIR}/Azyron-Client-Source-v1.0.2.tar.gz"

for f in "${JAR_FILE}" "${MRPACK_FILE}" "${SRC_ZIP}" "${SRC_TGZ}" "${NOTES_FILE}"; do
  if [[ ! -s "${f}" ]]; then
    echo "Error: Missing or empty required release file: ${f}" >&2
    exit 1
  fi
done

if gh release view "${TAG}" -R "${REPO}" >/dev/null 2>&1; then
  echo "Uploading assets to existing Draft Release ${TAG} on ${REPO}..."
  gh release upload "${TAG}" \
    "${JAR_FILE}#Azyron-Client-1.0.2.jar" \
    "${MRPACK_FILE}#Azyron-Modpack-1.0.2.mrpack" \
    "${SRC_ZIP}#Azyron-Client-Source-v1.0.2.zip" \
    "${SRC_TGZ}#Azyron-Client-Source-v1.0.2.tar.gz" \
    --clobber \
    -R "${REPO}"
else
  echo "Creating UNPUBLISHED Draft Release ${TAG} on ${REPO} (--draft)..."
  gh release create "${TAG}" \
    "${JAR_FILE}#Azyron-Client-1.0.2.jar" \
    "${MRPACK_FILE}#Azyron-Modpack-1.0.2.mrpack" \
    "${SRC_ZIP}#Azyron-Client-Source-v1.0.2.zip" \
    "${SRC_TGZ}#Azyron-Client-Source-v1.0.2.tar.gz" \
    --draft \
    --title "AZYRON CLIENT v1.0.2 (Minecraft 1.21.11 Fabric)" \
    --notes-file "${NOTES_FILE}" \
    -R "${REPO}"
fi

echo "Done. Verifying release remains DRAFT (unpublished):"
gh release view "${TAG}" -R "${REPO}" --json tagName,name,isDraft,isPrerelease,assets
