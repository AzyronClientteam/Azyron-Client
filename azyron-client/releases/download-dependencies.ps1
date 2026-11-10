# Azyron v1.0.3; separate Minecraft profiles, no automatic cross-version mixing.
param(
 [ValidateSet('1.20.1','1.21.11')][string]$McVersion='1.21.11',
 [switch]$DesktopImmediatelyFast,
 [switch]$ModernFix,
 [string]$FabricLoaderVersion=''
)
$ErrorActionPreference='Stop'
if ($ModernFix) {
 if ($McVersion -ne '1.20.1') { throw 'No approved ModernFix Fabric 1.21.11 artifact.' }
 if ($FabricLoaderVersion -notmatch '^\d+\.\d+\.\d+$' -or [version]$FabricLoaderVersion -lt [version]'0.16.10') { throw 'ModernFix requires -FabricLoaderVersion >=0.16.10.' }
}
New-Item -ItemType Directory -Force mods | Out-Null
$marker='mods/.azyron-minecraft-version'
if ((Test-Path $marker) -and (Get-Content $marker -Raw).Trim() -ne $McVersion) { throw 'Use separate launcher instances/mods directories for each Minecraft version.' }
Set-Content -Path $marker -Value $McVersion -Encoding ascii
function Get-VerifiedMod($Url,$Name,$Algorithm,$Expected) {
 $tmp=Join-Path (Resolve-Path mods) ('.azyron-download.'+[guid]::NewGuid())
 try {
  Invoke-WebRequest -Uri $Url -OutFile $tmp
  if ((Get-FileHash $tmp -Algorithm $Algorithm).Hash.ToLowerInvariant() -ne $Expected) { throw "Hash mismatch: $Name" }
  Move-Item -Force $tmp (Join-Path mods $Name)
 } finally { if (Test-Path $tmp) { Remove-Item $tmp } }
}
if ($McVersion -eq '1.20.1') {
 Get-VerifiedMod 'https://cdn.modrinth.com/data/P7dR8mSH/versions/pYtlDijL/fabric-api-0.88.1%2B1.20.1.jar' 'fabric-api-0.88.1+1.20.1.jar' SHA512 '636f1cf9ab6e0266d29964c6405e0901434ce41ed939337068f680b513817cf5785b03315aefc5c987c3a8d066105c5c0fdd2bc73080f632dbbc9016250954a5'
 Get-VerifiedMod 'https://cdn.modrinth.com/data/AANobbMI/versions/4OZL6q9h/sodium-fabric-mc1.20.1-0.5.3.jar' 'sodium-fabric-mc1.20.1-0.5.3.jar' SHA512 '989dffbf494030a09d69004ff761aadd130ac1fbdd26d640c1dccb5dae6a48db24de812bdf75ae7ba373e8bbdd668300884f35f9ad85b25f168619e33218279b'
 Get-VerifiedMod 'https://cdn.modrinth.com/data/gvQqBUqZ/versions/ZSNsJrPI/lithium-fabric-mc1.20.1-0.11.2.jar' 'lithium-fabric-mc1.20.1-0.11.2.jar' SHA512 'd1b5c90ba8b4879814df7fbf6e67412febbb2870e8131858c211130e9b5546e86b213b768b912fc7a2efa37831ad91caf28d6d71ba972274618ffd59937e5d0d'
 Get-VerifiedMod 'https://cdn.modrinth.com/data/uXXizFIs/versions/FCnCG6PS/ferritecore-6.0.0-fabric.jar' 'ferritecore-6.0.0-fabric.jar' SHA512 '9f563d8ceb2ec838e5da23257d369a26be0323e2f7777df8bb6e2ac4610dd0020827411f7e82e1f151d0fd9236014a8f02801ab4c1a5857ad3fd90345f6cfa9c'
 Get-VerifiedMod 'https://cdn.modrinth.com/data/NNAgCjsB/versions/BDwHAdWc/entityculling-fabric-1.6.2-mc1.20.1.jar' 'entityculling-fabric-1.6.2-mc1.20.1.jar' SHA512 '0080d3733f77e6b2e80e40d5d7bfe63dd125d94984bf92568d3434ff281f68123ec382ee53eba021278c01059299c12492dd518e61de1247f5cf7e8040bc98f1'
 Get-VerifiedMod 'https://cdn.modrinth.com/data/51shyZVL/versions/Bu5pCvCn/moreculling-1.20.1-0.19.0.jar' 'moreculling-1.20.1-0.19.0.jar' SHA512 'de1e00d14d0f682341863028fa970f929ced6b6cdc6f6105b99c0adb0b4be07c24f89ce95f256999acd4469590d40ddedeb41d6466d723d314ad16b6e456ebed'
 Get-VerifiedMod 'https://cdn.modrinth.com/data/9s6osm5g/versions/s7VTKfLA/cloth-config-11.1.106-fabric.jar' 'cloth-config-11.1.106-fabric.jar' SHA512 'bce3d04905f49a331ade089332f0bb5f2329a81451f2acdd4d95590cb090dbc88f0192a85de342e705f4aea9f4cdf2add8b5ef842d0014e29f0a2a9c281984a2'
 Get-VerifiedMod 'https://cdn.modrinth.com/data/LQ3K71Q1/versions/MJxmQ042/dynamic-fps-3.2.1%2Bminecraft-1.20.0.jar' 'dynamic-fps-3.2.1+minecraft-1.20.0.jar' SHA512 '6ca9280ee22153552cdb4a75454850c2b75914d661afdea97c11a7c70370631f8f1574233c52ab46e36e97b9caed3f3574eee95169b2382d1b7a1e96973bdb77'
 if ($DesktopImmediatelyFast) { Get-VerifiedMod 'https://cdn.modrinth.com/data/5ZwdcRci/versions/NJ17fqEK/ImmediatelyFast-1.2.4%2B1.20.1.jar' 'ImmediatelyFast-1.2.4+1.20.1.jar' SHA512 '07c8b0bfe2c032985a664109abe5d55e6eae2faed8b72b38e19af824a38cf08086c31e198fa1890e9871baacdc2421a8bb0fa0c67a08057235b64f3d946319b5' }
}
elseif ($McVersion -eq '1.21.11') {
 Get-VerifiedMod 'https://cdn.modrinth.com/data/P7dR8mSH/versions/6qAuTtLR/fabric-api-0.141.6%2B1.21.11.jar' 'fabric-api-0.141.6+1.21.11.jar' SHA512 '852d3682c4f353fd0ca86546af8578513e8bcf628377fa9d49c556bc59c5024c69ab56c92fc6dfba11fed19e4316a3f5636e7e07fd3d96f7043b531aef59b637'
 Get-VerifiedMod 'https://cdn.modrinth.com/data/AANobbMI/versions/rkdTcxoT/sodium-fabric-0.8.14%2Bmc1.21.11.jar' 'sodium-fabric-0.8.14+mc1.21.11.jar' SHA512 '04c43f9e8534b87a52c42ffd51b0e344d4ef92dc9cc52da33d13af6a18bf74b05380d2027f1e0db982698d0aff274c42741c1e21b313b0cd75ed3af005fc98d9'
 Get-VerifiedMod 'https://cdn.modrinth.com/data/gvQqBUqZ/versions/Ow7wA0kG/lithium-fabric-0.21.4%2Bmc1.21.11.jar' 'lithium-fabric-0.21.4+mc1.21.11.jar' SHA512 'f14a5c3d2fad786347ca25083f902139694f618b7c103947f2fd067a7c5ee88a63e1ef8926f7d693ea79ed7d00f57317bae77ef9c2d630bf5ed01ac97a752b94'
 Get-VerifiedMod 'https://cdn.modrinth.com/data/uXXizFIs/versions/Ii0gP3D8/ferritecore-8.2.0-fabric.jar' 'ferritecore-8.2.0-fabric.jar' SHA512 '3210926a82eb32efd9bcebabe2f6c053daf5c4337eebc6d5bacba96d283510afbde646e7e195751de795ec70a2ea44fef77cb54bf22c8e57bb832d6217418869'
 Get-VerifiedMod 'https://cdn.modrinth.com/data/NNAgCjsB/versions/CTabrwB1/entityculling-fabric-1.11.3-mc1.21.11.jar' 'entityculling-fabric-1.11.3-mc1.21.11.jar' SHA512 '4b5516bf8718d74db0e0368ad2fd2f5ce3e986c3abbf65426418ce3ec162651d29579b9be8aa0d34b88f47e310f6b7bab56bde7ff1223c35998bff0bcbed58c5'
 Get-VerifiedMod 'https://cdn.modrinth.com/data/51shyZVL/versions/wOzykoLV/moreculling-fabric-1.21.11-1.6.2.jar' 'moreculling-fabric-1.21.11-1.6.2.jar' SHA512 '58cec88e84835c543d8604411f89a0984abe51f0332860354355ede290d2442fc33a86ecda1766e0b1a82ba4edfab27f3c9f6855c0af3234e200218b892756f2'
 Get-VerifiedMod 'https://cdn.modrinth.com/data/9s6osm5g/versions/xuX40TN5/cloth-config-21.11.153-fabric.jar' 'cloth-config-21.11.153-fabric.jar' SHA512 '8f455489d4b71069e998568cf4e1450116f4360a4eb481cd89117f629c6883164886cf63ca08ac4fc929dd13d1112152755a6216d4a1498ee6406ef102093e51'
 Get-VerifiedMod 'https://cdn.modrinth.com/data/LQ3K71Q1/versions/Fab7e5Th/dynamic-fps-3.11.6%2Bminecraft-1.21.11-fabric.jar' 'dynamic-fps-3.11.6+minecraft-1.21.11-fabric.jar' SHA512 'c45652252e2176cb8e2c44dfd9032c617135a100dcf161b9e6e6717803c14d1359d0f940fa10dc1aa6ad80669983f4b9c5636c0c2953d3314e06f893f0cdc736'
 if ($DesktopImmediatelyFast) { Get-VerifiedMod 'https://cdn.modrinth.com/data/5ZwdcRci/versions/4EwhsTu7/ImmediatelyFast-Fabric-1.14.3%2B1.21.11.jar' 'ImmediatelyFast-Fabric-1.14.3+1.21.11.jar' SHA512 '2564cb28259352c3aa5759aceca62667de08a7860a40910dad40a043af851a074b2d75cef50b9fa6f1971febb3a5ecebe4067cbd4b60e63ef711b85f4bc21407' }
}
if ($ModernFix) { Get-VerifiedMod 'https://github.com/embeddedt/ModernFix/releases/download/5.25.2%2B1.20.1/modernfix-fabric-5.25.2%2Bmc1.20.1.jar' 'modernfix-fabric-5.25.2+mc1.20.1.jar' SHA256 '5c1f852046e62a17056bb6b571164e61644076c5eb9029c92b31e337cc62c0d9' }
Write-Host 'Pinned files hash-verified. Hardware compatibility/FPS is NOT certified.'
