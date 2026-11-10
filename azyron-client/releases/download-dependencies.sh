#!/usr/bin/env bash
# Azyron Client v1.0.3 / Azyron Modpack v1.0.3 — separate Fabric profiles.
# Optional ModernFix is 1.20.1 ONLY and requires --fabric-loader >=0.16.10.
# ImmediatelyFast is explicit desktop opt-in; do NOT use it on Android by default.
set -euo pipefail
MC_VERSION=1.21.11
DESKTOP_IF=false
MODERNFIX=false
LOADER=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --mc) [[ $# -ge 2 ]] || { echo 'Missing --mc value' >&2; exit 2; }; MC_VERSION="$2"; shift 2;;
    --mc=*) MC_VERSION="${1#*=}"; shift;;
    1.20.1|1.21.11) MC_VERSION="$1"; shift;;
    --desktop-immediatelyfast) DESKTOP_IF=true; shift;;
    --modernfix) MODERNFIX=true; shift;;
    --fabric-loader) [[ $# -ge 2 ]] || exit 2; LOADER="$2"; shift 2;;
    *) echo "Unknown option: $1" >&2; exit 2;;
  esac
done
[[ "$MC_VERSION" == 1.20.1 || "$MC_VERSION" == 1.21.11 ]] || { echo 'Unsupported Minecraft version' >&2; exit 2; }
if [[ "$MODERNFIX" == true ]]; then
  [[ "$MC_VERSION" == 1.20.1 ]] || { echo 'No approved ModernFix Fabric 1.21.11 artifact; refusing cross-version installation.' >&2; exit 2; }
  [[ "$LOADER" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || { echo 'ModernFix requires explicit --fabric-loader MAJOR.MINOR.PATCH (>=0.16.10).' >&2; exit 2; }
  [[ "$(printf '%s\n' 0.16.10 "$LOADER" | sort -V | head -1)" == 0.16.10 ]] || { echo 'ModernFix needs Fabric Loader >=0.16.10.' >&2; exit 2; }
fi
mkdir -p mods
MARKER=mods/.azyron-minecraft-version
if [[ -f "$MARKER" && "$(cat "$MARKER")" != "$MC_VERSION" ]]; then
  echo 'Do not mix Minecraft versions. Use separate launcher instances/mods directories.' >&2; exit 2
fi
printf '%s\n' "$MC_VERSION" > "$MARKER"
download() {
  local url="$1" name="$2" algorithm="$3" expected="$4" tmp
  tmp="$(mktemp "mods/.azyron-download.XXXXXX")"
  if ! curl --fail --location --retry 3 --connect-timeout 15 --max-time 180 "$url" -o "$tmp"; then rm -f "$tmp"; return 1; fi
  if ! printf '%s  %s\n' "$expected" "$tmp" | "${algorithm}sum" -c -; then rm -f "$tmp"; return 1; fi
  mv -f "$tmp" "mods/$name"
}
if [[ "$MC_VERSION" == "1.20.1" ]]; then
  download 'https://cdn.modrinth.com/data/P7dR8mSH/versions/pYtlDijL/fabric-api-0.88.1%2B1.20.1.jar' 'fabric-api-0.88.1+1.20.1.jar' sha512 '636f1cf9ab6e0266d29964c6405e0901434ce41ed939337068f680b513817cf5785b03315aefc5c987c3a8d066105c5c0fdd2bc73080f632dbbc9016250954a5'
  download 'https://cdn.modrinth.com/data/AANobbMI/versions/4OZL6q9h/sodium-fabric-mc1.20.1-0.5.3.jar' 'sodium-fabric-mc1.20.1-0.5.3.jar' sha512 '989dffbf494030a09d69004ff761aadd130ac1fbdd26d640c1dccb5dae6a48db24de812bdf75ae7ba373e8bbdd668300884f35f9ad85b25f168619e33218279b'
  download 'https://cdn.modrinth.com/data/gvQqBUqZ/versions/ZSNsJrPI/lithium-fabric-mc1.20.1-0.11.2.jar' 'lithium-fabric-mc1.20.1-0.11.2.jar' sha512 'd1b5c90ba8b4879814df7fbf6e67412febbb2870e8131858c211130e9b5546e86b213b768b912fc7a2efa37831ad91caf28d6d71ba972274618ffd59937e5d0d'
  download 'https://cdn.modrinth.com/data/uXXizFIs/versions/FCnCG6PS/ferritecore-6.0.0-fabric.jar' 'ferritecore-6.0.0-fabric.jar' sha512 '9f563d8ceb2ec838e5da23257d369a26be0323e2f7777df8bb6e2ac4610dd0020827411f7e82e1f151d0fd9236014a8f02801ab4c1a5857ad3fd90345f6cfa9c'
  download 'https://cdn.modrinth.com/data/NNAgCjsB/versions/BDwHAdWc/entityculling-fabric-1.6.2-mc1.20.1.jar' 'entityculling-fabric-1.6.2-mc1.20.1.jar' sha512 '0080d3733f77e6b2e80e40d5d7bfe63dd125d94984bf92568d3434ff281f68123ec382ee53eba021278c01059299c12492dd518e61de1247f5cf7e8040bc98f1'
  download 'https://cdn.modrinth.com/data/51shyZVL/versions/Bu5pCvCn/moreculling-1.20.1-0.19.0.jar' 'moreculling-1.20.1-0.19.0.jar' sha512 'de1e00d14d0f682341863028fa970f929ced6b6cdc6f6105b99c0adb0b4be07c24f89ce95f256999acd4469590d40ddedeb41d6466d723d314ad16b6e456ebed'
  download 'https://cdn.modrinth.com/data/9s6osm5g/versions/s7VTKfLA/cloth-config-11.1.106-fabric.jar' 'cloth-config-11.1.106-fabric.jar' sha512 'bce3d04905f49a331ade089332f0bb5f2329a81451f2acdd4d95590cb090dbc88f0192a85de342e705f4aea9f4cdf2add8b5ef842d0014e29f0a2a9c281984a2'
  download 'https://cdn.modrinth.com/data/LQ3K71Q1/versions/MJxmQ042/dynamic-fps-3.2.1%2Bminecraft-1.20.0.jar' 'dynamic-fps-3.2.1+minecraft-1.20.0.jar' sha512 '6ca9280ee22153552cdb4a75454850c2b75914d661afdea97c11a7c70370631f8f1574233c52ab46e36e97b9caed3f3574eee95169b2382d1b7a1e96973bdb77'
  if [[ "$DESKTOP_IF" == true ]]; then download 'https://cdn.modrinth.com/data/5ZwdcRci/versions/NJ17fqEK/ImmediatelyFast-1.2.4%2B1.20.1.jar' 'ImmediatelyFast-1.2.4+1.20.1.jar' sha512 '07c8b0bfe2c032985a664109abe5d55e6eae2faed8b72b38e19af824a38cf08086c31e198fa1890e9871baacdc2421a8bb0fa0c67a08057235b64f3d946319b5'; fi
elif [[ "$MC_VERSION" == "1.21.11" ]]; then
  download 'https://cdn.modrinth.com/data/P7dR8mSH/versions/6qAuTtLR/fabric-api-0.141.6%2B1.21.11.jar' 'fabric-api-0.141.6+1.21.11.jar' sha512 '852d3682c4f353fd0ca86546af8578513e8bcf628377fa9d49c556bc59c5024c69ab56c92fc6dfba11fed19e4316a3f5636e7e07fd3d96f7043b531aef59b637'
  download 'https://cdn.modrinth.com/data/AANobbMI/versions/rkdTcxoT/sodium-fabric-0.8.14%2Bmc1.21.11.jar' 'sodium-fabric-0.8.14+mc1.21.11.jar' sha512 '04c43f9e8534b87a52c42ffd51b0e344d4ef92dc9cc52da33d13af6a18bf74b05380d2027f1e0db982698d0aff274c42741c1e21b313b0cd75ed3af005fc98d9'
  download 'https://cdn.modrinth.com/data/gvQqBUqZ/versions/Ow7wA0kG/lithium-fabric-0.21.4%2Bmc1.21.11.jar' 'lithium-fabric-0.21.4+mc1.21.11.jar' sha512 'f14a5c3d2fad786347ca25083f902139694f618b7c103947f2fd067a7c5ee88a63e1ef8926f7d693ea79ed7d00f57317bae77ef9c2d630bf5ed01ac97a752b94'
  download 'https://cdn.modrinth.com/data/uXXizFIs/versions/Ii0gP3D8/ferritecore-8.2.0-fabric.jar' 'ferritecore-8.2.0-fabric.jar' sha512 '3210926a82eb32efd9bcebabe2f6c053daf5c4337eebc6d5bacba96d283510afbde646e7e195751de795ec70a2ea44fef77cb54bf22c8e57bb832d6217418869'
  download 'https://cdn.modrinth.com/data/NNAgCjsB/versions/CTabrwB1/entityculling-fabric-1.11.3-mc1.21.11.jar' 'entityculling-fabric-1.11.3-mc1.21.11.jar' sha512 '4b5516bf8718d74db0e0368ad2fd2f5ce3e986c3abbf65426418ce3ec162651d29579b9be8aa0d34b88f47e310f6b7bab56bde7ff1223c35998bff0bcbed58c5'
  download 'https://cdn.modrinth.com/data/51shyZVL/versions/wOzykoLV/moreculling-fabric-1.21.11-1.6.2.jar' 'moreculling-fabric-1.21.11-1.6.2.jar' sha512 '58cec88e84835c543d8604411f89a0984abe51f0332860354355ede290d2442fc33a86ecda1766e0b1a82ba4edfab27f3c9f6855c0af3234e200218b892756f2'
  download 'https://cdn.modrinth.com/data/9s6osm5g/versions/xuX40TN5/cloth-config-21.11.153-fabric.jar' 'cloth-config-21.11.153-fabric.jar' sha512 '8f455489d4b71069e998568cf4e1450116f4360a4eb481cd89117f629c6883164886cf63ca08ac4fc929dd13d1112152755a6216d4a1498ee6406ef102093e51'
  download 'https://cdn.modrinth.com/data/LQ3K71Q1/versions/Fab7e5Th/dynamic-fps-3.11.6%2Bminecraft-1.21.11-fabric.jar' 'dynamic-fps-3.11.6+minecraft-1.21.11-fabric.jar' sha512 'c45652252e2176cb8e2c44dfd9032c617135a100dcf161b9e6e6717803c14d1359d0f940fa10dc1aa6ad80669983f4b9c5636c0c2953d3314e06f893f0cdc736'
  if [[ "$DESKTOP_IF" == true ]]; then download 'https://cdn.modrinth.com/data/5ZwdcRci/versions/4EwhsTu7/ImmediatelyFast-Fabric-1.14.3%2B1.21.11.jar' 'ImmediatelyFast-Fabric-1.14.3+1.21.11.jar' sha512 '2564cb28259352c3aa5759aceca62667de08a7860a40910dad40a043af851a074b2d75cef50b9fa6f1971febb3a5ecebe4067cbd4b60e63ef711b85f4bc21407'; fi
fi
if [[ "$MODERNFIX" == true ]]; then download 'https://github.com/embeddedt/ModernFix/releases/download/5.25.2%2B1.20.1/modernfix-fabric-5.25.2%2Bmc1.20.1.jar' 'modernfix-fabric-5.25.2+mc1.20.1.jar' sha256 '5c1f852046e62a17056bb6b571164e61644076c5eb9029c92b31e337cc62c0d9'; fi
echo "Pinned $MC_VERSION files downloaded with hash verification. Hardware compatibility/FPS is NOT certified."
