# AZYRON CLIENT — Minecraft `1.21.11` Fabric Dependencies & Optimization Stack

All dependencies below are strictly verified for **Minecraft Java Edition `1.21.11`** on **Fabric** (`Java 21`) across both **Desktop PCs** and **Android / PojavLauncher**. Incompatible mobile GL mods are intentionally excluded from the universal modpack to guarantee stability, smooth startup, and glitch-free rendering.

---

## 1. Platform Requirements

| Component | Exact / Minimum Version | Requirement | Notes |
| :--- | :--- | :--- | :--- |
| **Minecraft: Java Edition** | **`1.21.11`** | **Required** | Strictly locked (`"minecraft": "1.21.11"` in `fabric.mod.json`) |
| **Java Runtime (JVM)** | **`OpenJDK 21+`** | **Required** | Use `Internal-21` on PojavLauncher or Temurin/OpenJDK 21 on PC |
| **Fabric Loader** | **`0.20.1`** (`>=0.17.3`) | **Required** | Loads AZYRON CLIENT & SpongePowered Mixin `0.8.7` |

---

## 2. Required API Dependency

| Mod | Version | Filename | SHA-512 Checksum | Direct Modrinth CDN URL |
| :--- | :--- | :--- | :--- | :--- |
| **Fabric API** | `0.141.6+1.21.11` | `fabric-api-0.141.6+1.21.11.jar` | `852d3682c4f353fd0ca86546af8578513e8bcf628377fa9d49c556bc59c5024c69ab56c92fc6dfba11fed19e4316a3f5636e7e07fd3d96f7043b531aef59b637` | [Download](https://cdn.modrinth.com/data/P7dR8mSH/versions/6qAuTtLR/fabric-api-0.141.6%2B1.21.11.jar) |

---

## 3. Universal PC + Android/PojavLauncher `1.21.11` Optimization Stack (8 Mods)

Included automatically in `releases/Azyron-Modpack-1.0.2.mrpack` and `releases/download-dependencies.sh` / `.ps1`, with pre-tuned PojavLauncher-safe configuration overrides in `config/`:

| Mod | Version (`1.21.11`) | Optimization Role | SHA-1 | Direct Modrinth CDN URL |
| :--- | :--- | :--- | :--- | :--- |
| **Sodium** | `0.8.14+mc1.21.11` | Next-gen chunk & terrain OpenGL rendering engine (2x–4x FPS boost) | `4a8733fe544168068d12fa94b2ab03e6c0c7bf91` | [Download](https://cdn.modrinth.com/data/AANobbMI/versions/rkdTcxoT/sodium-fabric-0.8.14%2Bmc1.21.11.jar) |
| **Lithium** | `0.21.4+mc1.21.11` | Singleplayer/IntegratedServer tick, physics, AI & block-shape optimization | `203bdcb26e97b3217b045e1182651a7d7b6462ec` | [Download](https://cdn.modrinth.com/data/gvQqBUqZ/versions/Ow7wA0kG/lithium-fabric-0.21.4%2Bmc1.21.11.jar) |
| **FerriteCore** | `8.2.0-fabric` | Cuts blockstate/model RAM usage by ~40% (vital for `3GB–4GB` phones) | `c2e2f5e008f5bc9f401dfad8ca94909917006b65` | [Download](https://cdn.modrinth.com/data/uXXizFIs/versions/Ii0gP3D8/ferritecore-8.2.0-fabric.jar) |
| **EntityCulling** | `1.11.3-mc1.21.11` | Async raytraced occlusion culling for hidden entities & block-entities | `ce2e1cf7e343a625d184c80ccea582d468686a2e` | [Download](https://cdn.modrinth.com/data/NNAgCjsB/versions/CTabrwB1/entityculling-fabric-1.11.3-mc1.21.11.jar) |
| **More Culling** | `1.21.11-1.6.2` | Culls unseen block faces, leaves, signs, rain, beacon beams & item frames | `11a929de62650cc536244d4c2ba889fa84c1c0f2` | [Download](https://cdn.modrinth.com/data/51shyZVL/versions/wOzykoLV/moreculling-fabric-1.21.11-1.6.2.jar) |
| **Cloth Config API** | `21.11.153-fabric` | Lightweight configuration library required by More Culling | `4c224606a963bce223db5b27edb4959ecf40d4ee` | [Download](https://cdn.modrinth.com/data/9s6osm5g/versions/xuX40TN5/cloth-config-21.11.153-fabric.jar) |
| **Dynamic FPS** | `3.11.6+minecraft-1.21.11` | Throttles background/minimized rendering safely (`invisible: 1 FPS`) | `2e5ab45e69b43812491cfa34f8f7a4ce7623c233` | [Download](https://cdn.modrinth.com/data/LQ3K71Q1/versions/Fab7e5Th/dynamic-fps-3.11.6%2Bminecraft-1.21.11-fabric.jar) |

---

## 4. Desktop-Only Optional Add-On (Excluded on Android / PojavLauncher)

| Mod | Version (`1.21.11`) | Platform | Reason Excluded from Universal Pack |
| :--- | :--- | :--- | :--- |
| **ImmediatelyFast** | `1.14.3+1.21.11` | **Desktop PC GPUs ONLY** (`--desktop-immediatelyfast` / `-DesktopImmediatelyFast`) | Upstream explicitly marks Android OpenGL translation layers (`GL4ES`, `LTW`, `MobileGlues`, `ANGLE`) unsupported (`RaphiMC/ImmediatelyFast#386`), where low-level framebuffer/streaming hooks can cause corrupted mob textures or native `SIGABRT` crashes (`PojavLauncher#1948`). A fallback `config/immediatelyfast.json` (`enhanced_batching: false`) is still shipped in overrides in case a user manually adds the JAR. |

---

## 5. Compatibility & Safety Notes

- **Zero Mixin Collisions**:
  - `PersistentStateManagerMixin` backs up unreadable/corrupted world `.dat` files (`raids.dat`, `raids_end.dat`, etc.) to `<world>/data/azyron_backups/` before vanilla fallback or repair, and writes saves atomically via `.azyron.tmp` rename.
  - `NarratorManagerMixin` safely bypasses missing native `libflite.so` on Android/PojavLauncher and minimal Linux without throwing `FatalException` or `InactiveNarratorLibraryException`.
  - `GameOptionsMixin` normalizes non-standard boolean strings (such as PojavLauncher's `fullscreen:off`) before `SimpleOption` JSON parsing, preventing `Codec.BOOL` startup errors.
  - `IntegratedServerMixin` enables asynchronous chunk writes and smooth MSPT-aware simulation distance pacing in singleplayer worlds.
