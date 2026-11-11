#!/usr/bin/env bash
# Updates an existing GitHub Pre-release / Draft Release (default tag: v0.1.0-pre)
# to AZYRON CLIENT v1.0.2 PRE-RELEASE without publishing it as a stable release:
#   - Removes any legacy 1.0 / 1.0.1 assets attached to the release
#   - Uploads all 4 verified v1.0.2 release assets:
#       1. Azyron-Client-1.0.2.jar
#       2. Azyron-Modpack-1.0.2.mrpack
#       3. Azyron-Client-Source-v1.0.2.zip
#       4. Azyron-Client-Source-v1.0.2.tar.gz

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLIENT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
RELEASES_DIR="${CLIENT_ROOT}/releases"
NOTES_FILE="${CLIENT_ROOT}/RELEASE_NOTES_v1.0.2.md"
REPO="${GITHUB_REPOSITORY:-AzyronClientteam/Azyron-Client}"
TAG="${1:-v0.1.0-pre}"

JAR_FILE="${RELEASES_DIR}/Azyron-Client-1.0.2.jar"
MRPACK_FILE="${RELEASES_DIR}/Azyron-Modpack-1.0.2.mrpack"
SRC_ZIP="${RELEASES_DIR}/Azyron-Client-Source-v1.0.2.zip"
SRC_TGZ="${RELEASES_DIR}/Azyron-Client-Source-v1.0.2.tar.gz"

for f in "${JAR_FILE}" "${MRPACK_FILE}" "${SRC_ZIP}" "${SRC_TGZ}" "${NOTES_FILE}"; do
  if [[ ! -s "${f}" ]]; then
    echo "Error: Missing or empty required v1.0.2 release file: ${f}" >&2
    exit 1
  fi
done

echo "Checking for obsolete 1.0 / 1.0.1 assets on ${TAG}..."
OLD_ASSETS="$(gh release view "${TAG}" -R "${REPO}" --json assets --jq '.assets[].name' 2>/dev/null || true)"
while IFS= read -r asset_name; do
  [[ -z "${asset_name}" ]] && continue
  if [[ "${asset_name}" == *"1.0.1"* || "${asset_name}" == "Azyron-Client-1.0.jar" || "${asset_name}" == "Azyron-Modpack-1.0.mrpack" ]]; then
    echo "Deleting obsolete asset: ${asset_name}"
    gh release delete-asset "${TAG}" "${asset_name}" --yes -R "${REPO}"
  fi
done <<< "${OLD_ASSETS}"

echo "Updating release metadata and uploading verified v1.0.2 assets to ${TAG}..."
gh release edit "${TAG}" \
  --title "AZYRON CLIENT v1.0.2 PRE-RELEASE" \
  --notes-file "${NOTES_FILE}" \
  --prerelease \
  -R "${REPO}"

gh release upload "${TAG}" \
  "${JAR_FILE}#Azyron-Client-1.0.2.jar" \
  "${MRPACK_FILE}#Azyron-Modpack-1.0.2.mrpack" \
  "${SRC_ZIP}#Azyron-Client-Source-v1.0.2.zip" \
  "${SRC_TGZ}#Azyron-Client-Source-v1.0.2.tar.gz" \
  --clobber \
  -R "${REPO}"

echo "Done. Verifying release ${TAG} on ${REPO}:"
gh release view "${TAG}" -R "${REPO}" --json tagName,name,isDraft,isPrerelease,assets
