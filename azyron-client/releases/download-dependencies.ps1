# AZYRON CLIENT (Minecraft 1.21.11 Fabric) - Verified Dependency Downloader (Windows PowerShell)
$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Force -Path 'mods' | Out-Null
Write-Host '=== Downloading AZYRON CLIENT 1.21.11 Optimization Mod Stack ===' -ForegroundColor Cyan
Write-Host ' -> Fabric API (fabric-api-0.141.6+1.21.11.jar)'
Invoke-WebRequest -Uri 'https://cdn.modrinth.com/data/P7dR8mSH/versions/6qAuTtLR/fabric-api-0.141.6%2B1.21.11.jar' -OutFile 'mods/fabric-api-0.141.6+1.21.11.jar'
$hash = (Get-FileHash -Path 'mods/fabric-api-0.141.6+1.21.11.jar' -Algorithm SHA512).Hash.ToLower()
if ($hash -ne '852d3682c4f353fd0ca86546af8578513e8bcf628377fa9d49c556bc59c5024c69ab56c92fc6dfba11fed19e4316a3f5636e7e07fd3d96f7043b531aef59b637') { throw 'SHA-512 mismatch for fabric-api-0.141.6+1.21.11.jar' }
Write-Host ' -> Sodium (sodium-fabric-0.8.14+mc1.21.11.jar)'
Invoke-WebRequest -Uri 'https://cdn.modrinth.com/data/AANobbMI/versions/rkdTcxoT/sodium-fabric-0.8.14%2Bmc1.21.11.jar' -OutFile 'mods/sodium-fabric-0.8.14+mc1.21.11.jar'
$hash = (Get-FileHash -Path 'mods/sodium-fabric-0.8.14+mc1.21.11.jar' -Algorithm SHA512).Hash.ToLower()
if ($hash -ne '04c43f9e8534b87a52c42ffd51b0e344d4ef92dc9cc52da33d13af6a18bf74b05380d2027f1e0db982698d0aff274c42741c1e21b313b0cd75ed3af005fc98d9') { throw 'SHA-512 mismatch for sodium-fabric-0.8.14+mc1.21.11.jar' }
Write-Host ' -> Lithium (lithium-fabric-0.21.4+mc1.21.11.jar)'
Invoke-WebRequest -Uri 'https://cdn.modrinth.com/data/gvQqBUqZ/versions/Ow7wA0kG/lithium-fabric-0.21.4%2Bmc1.21.11.jar' -OutFile 'mods/lithium-fabric-0.21.4+mc1.21.11.jar'
$hash = (Get-FileHash -Path 'mods/lithium-fabric-0.21.4+mc1.21.11.jar' -Algorithm SHA512).Hash.ToLower()
if ($hash -ne 'f14a5c3d2fad786347ca25083f902139694f618b7c103947f2fd067a7c5ee88a63e1ef8926f7d693ea79ed7d00f57317bae77ef9c2d630bf5ed01ac97a752b94') { throw 'SHA-512 mismatch for lithium-fabric-0.21.4+mc1.21.11.jar' }
Write-Host ' -> FerriteCore (ferritecore-8.2.0-fabric.jar)'
Invoke-WebRequest -Uri 'https://cdn.modrinth.com/data/uXXizFIs/versions/Ii0gP3D8/ferritecore-8.2.0-fabric.jar' -OutFile 'mods/ferritecore-8.2.0-fabric.jar'
$hash = (Get-FileHash -Path 'mods/ferritecore-8.2.0-fabric.jar' -Algorithm SHA512).Hash.ToLower()
if ($hash -ne '3210926a82eb32efd9bcebabe2f6c053daf5c4337eebc6d5bacba96d283510afbde646e7e195751de795ec70a2ea44fef77cb54bf22c8e57bb832d6217418869') { throw 'SHA-512 mismatch for ferritecore-8.2.0-fabric.jar' }
Write-Host ' -> EntityCulling (entityculling-fabric-1.11.3-mc1.21.11.jar)'
Invoke-WebRequest -Uri 'https://cdn.modrinth.com/data/NNAgCjsB/versions/CTabrwB1/entityculling-fabric-1.11.3-mc1.21.11.jar' -OutFile 'mods/entityculling-fabric-1.11.3-mc1.21.11.jar'
$hash = (Get-FileHash -Path 'mods/entityculling-fabric-1.11.3-mc1.21.11.jar' -Algorithm SHA512).Hash.ToLower()
if ($hash -ne '4b5516bf8718d74db0e0368ad2fd2f5ce3e986c3abbf65426418ce3ec162651d29579b9be8aa0d34b88f47e310f6b7bab56bde7ff1223c35998bff0bcbed58c5') { throw 'SHA-512 mismatch for entityculling-fabric-1.11.3-mc1.21.11.jar' }
Write-Host ' -> More Culling (moreculling-fabric-1.21.11-1.6.2.jar)'
Invoke-WebRequest -Uri 'https://cdn.modrinth.com/data/51shyZVL/versions/wOzykoLV/moreculling-fabric-1.21.11-1.6.2.jar' -OutFile 'mods/moreculling-fabric-1.21.11-1.6.2.jar'
$hash = (Get-FileHash -Path 'mods/moreculling-fabric-1.21.11-1.6.2.jar' -Algorithm SHA512).Hash.ToLower()
if ($hash -ne '58cec88e84835c543d8604411f89a0984abe51f0332860354355ede290d2442fc33a86ecda1766e0b1a82ba4edfab27f3c9f6855c0af3234e200218b892756f2') { throw 'SHA-512 mismatch for moreculling-fabric-1.21.11-1.6.2.jar' }
Write-Host ' -> Cloth Config API (cloth-config-21.11.153-fabric.jar)'
Invoke-WebRequest -Uri 'https://cdn.modrinth.com/data/9s6osm5g/versions/xuX40TN5/cloth-config-21.11.153-fabric.jar' -OutFile 'mods/cloth-config-21.11.153-fabric.jar'
$hash = (Get-FileHash -Path 'mods/cloth-config-21.11.153-fabric.jar' -Algorithm SHA512).Hash.ToLower()
if ($hash -ne '8f455489d4b71069e998568cf4e1450116f4360a4eb481cd89117f629c6883164886cf63ca08ac4fc929dd13d1112152755a6216d4a1498ee6406ef102093e51') { throw 'SHA-512 mismatch for cloth-config-21.11.153-fabric.jar' }
Write-Host ' -> Dynamic FPS (dynamic-fps-3.11.6+minecraft-1.21.11-fabric.jar)'
Invoke-WebRequest -Uri 'https://cdn.modrinth.com/data/LQ3K71Q1/versions/Fab7e5Th/dynamic-fps-3.11.6%2Bminecraft-1.21.11-fabric.jar' -OutFile 'mods/dynamic-fps-3.11.6+minecraft-1.21.11-fabric.jar'
$hash = (Get-FileHash -Path 'mods/dynamic-fps-3.11.6+minecraft-1.21.11-fabric.jar' -Algorithm SHA512).Hash.ToLower()
if ($hash -ne 'c45652252e2176cb8e2c44dfd9032c617135a100dcf161b9e6e6717803c14d1359d0f940fa10dc1aa6ad80669983f4b9c5636c0c2953d3314e06f893f0cdc736') { throw 'SHA-512 mismatch for dynamic-fps-3.11.6+minecraft-1.21.11-fabric.jar' }
Write-Host ' -> ImmediatelyFast (ImmediatelyFast-Fabric-1.14.3+1.21.11.jar)'
Invoke-WebRequest -Uri 'https://cdn.modrinth.com/data/5ZwdcRci/versions/4EwhsTu7/ImmediatelyFast-Fabric-1.14.3%2B1.21.11.jar' -OutFile 'mods/ImmediatelyFast-Fabric-1.14.3+1.21.11.jar'
$hash = (Get-FileHash -Path 'mods/ImmediatelyFast-Fabric-1.14.3+1.21.11.jar' -Algorithm SHA512).Hash.ToLower()
if ($hash -ne '2564cb28259352c3aa5759aceca62667de08a7860a40910dad40a043af851a074b2d75cef50b9fa6f1971febb3a5ecebe4067cbd4b60e63ef711b85f4bc21407') { throw 'SHA-512 mismatch for ImmediatelyFast-Fabric-1.14.3+1.21.11.jar' }
Write-Host '=== All 1.21.11 dependencies downloaded and SHA-512 verified! ===' -ForegroundColor Green
