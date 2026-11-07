# AZYRON CLIENT — Minecraft `1.21.11` Fabric Dependencies & Optimization Stack

All dependencies below are strictly verified for **Minecraft Java Edition `1.21.11`** on **Fabric** (`Java 21`). Unnecessary, bloated, or conflicting mods are intentionally excluded to maximize stability, startup speed, and PojavLauncher compatibility.

---

## 1. Platform Requirements

| Component | Exact / Minimum Version | Requirement | Notes |
| :--- | :--- | :--- | :--- |
| **Minecraft: Java Edition** | **`1.21.11`** | **Required** | Strictly locked (`"minecraft": "1.21.11"` in `fabric.mod.json`) |
| **Java Runtime (JVM)** | **`OpenJDK 21+`** | **Required** | Use `Internal-21` on PojavLauncher or Temurin 21 on PC |
| **Fabric Loader** | **`0.20.1`** (`>=0.17.3`) | **Required** | Loads AZYRON CLIENT & SpongePowered Mixin `0.8.7` |

---

## 2. Required API Dependency

| Mod | Version | Filename | SHA-512 Checksum | Direct Modrinth CDN URL |
| :--- | :--- | :--- | :--- | :--- |
| **Fabric API** | `0.141.6+1.21.11` | `fabric-api-0.141.6+1.21.11.jar` | `852d3682c4f353fd0ca86546af8578513e8bcf628377fa9d49c556bc59c5024c69ab56c92fc6dfba11fed19e4316a3f5636e7e07fd3d96f7043b531aef59b637` | [Download](https://cdn.modrinth.com/data/P7dR8mSH/versions/6qAuTtLR/fabric-api-0.141.6%2B1.21.11.jar) |

---

## 3. Integrated `1.21.11` Optimization Mod Stack

Included automatically in `releases/AZYRON-CLIENT-1.0.0+mc1.21.11.mrpack` and `releases/download-dependencies.sh` / `.ps1`, with pre-tuned configuration overrides in `config/`:

| Mod | Version (`1.21.11`) | Optimization Role | SHA-1 | Direct Modrinth CDN URL |
| :--- | :--- | :--- | :--- | :--- |
| **Sodium** | `0.8.14+mc1.21.11` | Next-gen chunk & terrain OpenGL rendering engine (2x–4x FPS boost) | `4a8733fe544168068d12fa94b2ab03e6c0c7bf91` | [Download](https://cdn.modrinth.com/data/AANobbMI/versions/rkdTcxoT/sodium-fabric-0.8.14%2Bmc1.21.11.jar) |
| **Lithium** | `0.21.4+mc1.21.11` | Server/singleplayer tick, physics, AI & block-shape optimization | `203bdcb26e97b3217b045e1182651a7d7b6462ec` | [Download](https://cdn.modrinth.com/data/gvQqBUqZ/versions/Ow7wA0kG/lithium-fabric-0.21.4%2Bmc1.21.11.jar) |
| **FerriteCore** | `8.2.0-fabric` | Cuts blockstate/model RAM usage by ~40% (vital for PojavLauncher phones) | `c2e2f5e008f5bc9f401dfad8ca94909917006b65` | [Download](https://cdn.modrinth.com/data/uXXizFIs/versions/Ii0gP3D8/ferritecore-8.2.0-fabric.jar) |
| **EntityCulling** | `1.11.3-mc1.21.11` | Async raytraced occlusion culling for hidden entities & block-entities | `ce2e1cf7e343a625d184c80ccea582d468686a2e` | [Download](https://cdn.modrinth.com/data/NNAgCjsB/versions/CTabrwB1/entityculling-fabric-1.11.3-mc1.21.11.jar) |
| **More Culling** | `1.21.11-1.6.2` | Culls unseen block faces, leaves, signs, rain, beacon beams & item frames | `11a929de62650cc536244d4c2ba889fa84c1c0f2` | [Download](https://cdn.modrinth.com/data/51shyZVL/versions/wOzykoLV/moreculling-fabric-1.21.11-1.6.2.jar) |
| **Cloth Config API** | `21.11.153-fabric` | Lightweight configuration library used by More Culling | `4c224606a963bce223db5b27edb4959ecf40d4ee` | [Download](https://cdn.modrinth.com/data/9s6osm5g/versions/xuX40TN5/cloth-config-21.11.153-fabric.jar) |
| **Dynamic FPS** | `3.11.6+minecraft-1.21.11` | Throttles background/minimized rendering to save battery, thermals & CPU | `2e5ab45e69b43812491cfa34f8f7a4ce7623c233` | [Download](https://cdn.modrinth.com/data/LQ3K71Q1/versions/Fab7e5Th/dynamic-fps-3.11.6%2Bminecraft-1.21.11-fabric.jar) |
| **ImmediatelyFast** | `1.14.3+1.21.11` | Batches immediate-mode GUI, HUD, text & entity draw calls | `43e451464f979bdf9ff4ac288b358a2f91b79ed8` | [Download](https://cdn.modrinth.com/data/5ZwdcRci/versions/4EwhsTu7/ImmediatelyFast-Fabric-1.14.3%2B1.21.11.jar) |

---

## 4. Compatibility Notes

- **Zero Mixin Collisions**:
  - `SplashOverlayMixin` injects non-destructively at `@At("TAIL")` on `SplashOverlay.render`, preserving 100% compatibility with **Dynamic FPS**'s `SplashOverlay` reload listener and vanilla resource reload completion.
  - `ParticleManagerMixin` gates particle creation before allocation without conflicting with **Sodium**'s particle vertex writer.
  - `InGameHudMixin` renders all HUD overlays using Minecraft `1.21.11`'s `DrawContext` (`Matrix3x2fStack` + `RenderPipelines.GUI_TEXTURED` + explicit `0xAARRGGBB` alpha colors), batching cleanly with **ImmediatelyFast**.
