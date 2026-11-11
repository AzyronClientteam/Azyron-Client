# AZYRON CLIENT `v1.0.4` — Minecraft `1.21.11` & `1.20.1` Fabric Dependencies & Optimization Stacks

The v1.0.4 release uses four direct, target-specific files: one native Fabric JAR and its matching Modrinth `.mrpack` for each Minecraft version. **There is no client ZIP bundle.** The unsuffixed filenames target Minecraft `1.21.11`; the `-mc1.20.1` filenames target Minecraft `1.20.1`. The release remains an unpublished draft until explicit approval.
- **Minecraft `1.21.11` (Fabric)** (`Java 21+`, Fabric Loader `0.20.1`)
- **Minecraft `1.20.1` (Fabric)** (`Java 17+`, Fabric Loader `0.14.22+`)

Both profiles use version-appropriate companion mods and schemas while keeping the same low-memory targets. The packages target Desktop and Android/PojavLauncher, but actual device, renderer, multiplayer, and long-session stability testing remains pending; no FPS or RAM benchmark is claimed.

---

## 1. Platform Requirements Matrix

| Component | Minecraft `1.21.11` profile | Minecraft `1.20.1` profile |
| :--- | :--- | :--- |
| **Native Client download** | `Azyron-Client-1.0.4.jar` | `Azyron-Client-1.0.4-mc1.20.1.jar` |
| **Matching Modrinth profile** | `Azyron-Modpack-1.0.4.mrpack` | `Azyron-Modpack-1.0.4-mc1.20.1.mrpack` |
| **Java Runtime (JVM)** | **OpenJDK `21+`** (`Internal-21` on Pojav) | **OpenJDK `17+`** (`Internal-17` or `Internal-21`) |
| **Fabric Loader** | **`0.20.1`** (`>=0.17.3`) | **`0.14.22+`** |
| **Fabric API** | **`0.141.6+1.21.11`** | **`0.88.1+1.20.1`** |

---

## 2. Minecraft `1.21.11` (Fabric) Pinned Optimization Stack (`--mc 1.21.11`)

| Mod | Version (`1.21.11`) | Optimization Role | SHA-1 | Direct Modrinth CDN URL |
| :--- | :--- | :--- | :--- | :--- |
| **Fabric API** | `0.141.6+1.21.11` | Required Fabric lifecycle & keybinding API | `c98467cbbaf4d197377266795ae015f4130d65b6` | [Download](https://cdn.modrinth.com/data/P7dR8mSH/versions/6qAuTtLR/fabric-api-0.141.6%2B1.21.11.jar) |
| **Sodium** | `0.8.14+mc1.21.11` | GPU-backed terrain and chunk renderer; gains depend on hardware and settings | `4a8733fe544168068d12fa94b2ab03e6c0c7bf91` | [Download](https://cdn.modrinth.com/data/AANobbMI/versions/rkdTcxoT/sodium-fabric-0.8.14%2Bmc1.21.11.jar) |
| **Lithium** | `0.21.4+mc1.21.11` | Singleplayer/IntegratedServer tick, physics, AI & block-shape optimization | `203bdcb26e97b3217b045e1182651a7d7b6462ec` | [Download](https://cdn.modrinth.com/data/gvQqBUqZ/versions/Ow7wA0kG/lithium-fabric-0.21.4%2Bmc1.21.11.jar) |
| **FerriteCore** | `8.2.0-fabric` | Deduplicates block-state/model data; this pack has no measured RAM-reduction figure | `c2e2f5e008f5bc9f401dfad8ca94909917006b65` | [Download](https://cdn.modrinth.com/data/uXXizFIs/versions/Ii0gP3D8/ferritecore-8.2.0-fabric.jar) |
| **EntityCulling** | `1.11.3-mc1.21.11` | Async raytraced occlusion culling for hidden entities & block-entities | `ce2e1cf7e343a625d184c80ccea582d468686a2e` | [Download](https://cdn.modrinth.com/data/NNAgCjsB/versions/CTabrwB1/entityculling-fabric-1.11.3-mc1.21.11.jar) |
| **More Culling** | `1.21.11-1.6.2` | Culls unseen block faces, leaves, signs, rain, beacon beams & item frames | `11a929de62650cc536244d4c2ba889fa84c1c0f2` | [Download](https://cdn.modrinth.com/data/51shyZVL/versions/wOzykoLV/moreculling-fabric-1.21.11-1.6.2.jar) |
| **Cloth Config API** | `21.11.153-fabric` | Lightweight configuration library required by More Culling | `4c224606a963bce223db5b27edb4959ecf40d4ee` | [Download](https://cdn.modrinth.com/data/9s6osm5g/versions/xuX40TN5/cloth-config-21.11.153-fabric.jar) |
| **Dynamic FPS** | `3.11.6+minecraft-1.21.11` | Throttles background/minimized rendering safely (`invisible: 1 FPS`) | `2e5ab45e69b43812491cfa34f8f7a4ce7623c233` | [Download](https://cdn.modrinth.com/data/LQ3K71Q1/versions/Fab7e5Th/dynamic-fps-3.11.6%2Bminecraft-1.21.11-fabric.jar) |

---

## 3. Minecraft `1.20.1` (Fabric) Pinned Optimization Stack (`--mc 1.20.1`)

| Mod | Version (`1.20.1`) | Optimization Role | SHA-1 | Direct Modrinth CDN URL |
| :--- | :--- | :--- | :--- | :--- |
| **Fabric API** | `0.88.1+1.20.1` | Required Fabric lifecycle & keybinding API (`1.20.1`) | `e85c54a01f173f6cb5e533e66593ea5d65d462a1` | [Download](https://cdn.modrinth.com/data/P7dR8mSH/versions/pYtlDijL/fabric-api-0.88.1%2B1.20.1.jar) |
| **Sodium** | `mc1.20.1-0.5.3` | High-FPS OpenGL chunk renderer for `1.20.1` (`0.5.x` config auto-generated) | `74784e6deb72e8cdb4e3788ac3a3cfd88fc77acf` | [Download](https://cdn.modrinth.com/data/AANobbMI/versions/4OZL6q9h/sodium-fabric-mc1.20.1-0.5.3.jar) |
| **Lithium** | `mc1.20.1-0.11.2` | IntegratedServer physics, AI, hopper & chunk-tick optimization (`1.20.1`) | `8f4642147e60827f728badac7a819d1a9f3b25d5` | [Download](https://cdn.modrinth.com/data/gvQqBUqZ/versions/ZSNsJrPI/lithium-fabric-mc1.20.1-0.11.2.jar) |
| **FerriteCore** | `6.0.0-fabric` | Block-state and multipart-model memory deduplication; no pack-specific RAM reduction is measured | `5a57ea73c3c7c0cc300d69611a9cad93baf8e9ab` | [Download](https://cdn.modrinth.com/data/uXXizFIs/versions/FCnCG6PS/ferritecore-6.0.0-fabric.jar) |
| **EntityCulling** | `1.6.2-mc1.20.1` | Async path-tracing entity & block-entity occlusion culling | `8e4be2bb9703deddb3d3d212c812cdc1f9de072b` | [Download](https://cdn.modrinth.com/data/NNAgCjsB/versions/BDwHAdWc/entityculling-fabric-1.6.2-mc1.20.1.jar) |
| **More Culling** | `1.20.1-0.19.0` | Block-face, leaf, sign & item-frame geometry culling | `2b781b5ec7aa1b13f49a5fcc761a6b3f57c1e05f` | [Download](https://cdn.modrinth.com/data/51shyZVL/versions/Bu5pCvCn/moreculling-1.20.1-0.19.0.jar) |
| **Cloth Config API** | `11.1.106-fabric` | Configuration library for `1.20.1` More Culling | `a05e176326365f2b8e377a35be52a5671491bcfd` | [Download](https://cdn.modrinth.com/data/9s6osm5g/versions/s7VTKfLA/cloth-config-11.1.106-fabric.jar) |
| **Dynamic FPS** | `3.2.1+minecraft-1.20.0` | Unfocused/minimized background frame-rate reducer (`1.20.1` compatible) | `d4edd1828b1595eca7be0d36d1cc45b189a17a6e` | [Download](https://cdn.modrinth.com/data/LQ3K71Q1/versions/MJxmQ042/dynamic-fps-3.2.1%2Bminecraft-1.20.0.jar) |

---

## 4. Desktop-Only Optional Add-On (Excluded on Android / PojavLauncher)

| Mod | `1.21.11` Version | `1.20.1` Version | Platform |
| :--- | :--- | :--- | :--- |
| **ImmediatelyFast** | `1.14.3+1.21.11` | `1.2.4+1.20.1` | **Desktop opt-in only** (`--desktop-immediatelyfast` / `-DesktopImmediatelyFast`) — excluded from default packs because mobile OpenGL translation-layer compatibility is not established here. |
