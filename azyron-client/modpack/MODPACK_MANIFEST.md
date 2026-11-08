# AZYRON CLIENT Modpack Configuration & Metadata (`1.21.11` Fabric)

This directory contains the source configuration and Modrinth Format `v1` metadata used to build `Azyron-Modpack-1.0.2.mrpack` for **Minecraft Java Edition `1.21.11`** (`Fabric Loader 0.20.1`, `Java 21`).

## Verified Compatibility Matrix (`1.21.11` Fabric + Desktop & Android/PojavLauncher)

| Mod | Version (`1.21.11` Fabric) | Desktop PC | Android / PojavLauncher | Modpack Status |
|---|---|---|---|---|
| **Fabric API** | `0.141.6+1.21.11` | Verified | Verified | **Included (`required`)** |
| **Sodium** | `0.8.14+mc1.21.11` | Verified | Verified (`use_no_error_g_l_context: false`) | **Included (`required`)** |
| **Lithium** | `0.21.4+mc1.21.11` | Verified | Verified (Pure Java tick/chunk/AI engine) | **Included (`required`)** |
| **FerriteCore** | `8.2.0-fabric` | Verified | Verified (Pure Java 3GB–8GB RAM deduplication) | **Included (`required`)** |
| **EntityCulling** | `1.11.3-mc1.21.11` | Verified | Verified (CPU raycast occlusion culling) | **Included (`required`)** |
| **MoreCulling** (+ Cloth Config) | `1.21.11-1.6.2` | Verified | Verified (`entityModelCulling = false`) | **Included (`required`)** |
| **Dynamic FPS** | `3.11.6+minecraft-1.21.11-fabric` | Verified | Verified (`invisible` target `1 FPS`, `battery_tracker: false`) | **Included (`required`)** |
| **ImmediatelyFast** | `1.14.3+1.21.11` | Verified | **Incompatible** (`RaphiMC/ImmediatelyFast#386`: unsupported on Pojav/mobile OpenGL translation layers; causes mob texture corruption & `SIGABRT`) | **Excluded from universal `.mrpack`** (opt-in `--desktop-immediatelyfast` in downloader) |

## Directory Structure

- `modrinth.index.json` — Modrinth Format `v1` index pinning Minecraft `1.21.11`, Fabric Loader `0.20.1`, and `8` universal PC + Android/PojavLauncher verified `1.21.11` Fabric dependencies with exact `sha1`, `sha512`, byte size, and Modrinth CDN URLs.
- `icon.png` — AZYRON CLIENT modpack icon (`13,867` bytes).
- `overrides/config/` — Pre-tuned client and companion optimization configurations deployed into `.minecraft/config/`:
  - `azyron-client.json` — Default AZYRON CLIENT `v1.0.2` module states, HUD coordinates, and auto-calibrated 3GB–8GB RAM settings.
  - `sodium-options.json` — Tuned chunk renderer & mobile-safe GL context defaults for Sodium `0.8.14+mc1.21.11`.
  - `lithium.properties` — Safe Integrated Server tick, chunk, AI, and physics optimization flags for Lithium `0.21.4+mc1.21.11`.
  - `entityculling.json` — Async path-tracing entity/block-entity culling config for EntityCulling `1.11.3-mc1.21.11`.
  - `moreculling.toml` — Block-face, leaf, and item-frame culling config for More Culling `1.21.11-1.6.2`.
  - `dynamic_fps.json` — Android/desktop-safe background frame-rate throttling config for Dynamic FPS `3.11.6+minecraft-1.21.11-fabric`.
  - `immediatelyfast.json` — Safe fallback `1.14.3` config (`enhanced_batching: false`, `avoid_redundant_framebuffer_switching: false`, `map_atlas_size: 1024`) in case a user manually installs ImmediatelyFast.

## Rebuilding the `.mrpack` Bundle

```bash
python3 scripts/build-modpack.py
```
