# AZYRON CLIENT Modpack Configuration & Metadata (`1.21.11` Fabric)

This directory contains the source configuration and Modrinth Format `v1` metadata used to build `Azyron-Modpack-1.0.mrpack` for **Minecraft Java Edition `1.21.11`** (`Fabric Loader 0.20.1`, `Java 21`).

## Directory Structure

- `modrinth.index.json` — Modrinth Format `v1` index pinning Minecraft `1.21.11`, Fabric Loader `0.20.1`, and `9` verified `1.21.11` Fabric optimization dependencies with exact `sha1`, `sha512`, byte size, and Modrinth CDN URLs.
- `icon.png` — AZYRON CLIENT modpack icon (`13,867` bytes).
- `overrides/config/` — Pre-tuned client and companion optimization configurations deployed into `.minecraft/config/`:
  - `azyron-client.json` — Default AZYRON CLIENT module states, HUD coordinates, and `BALANCED` profile preset.
  - `sodium-options.json` — Tuned chunk renderer & culling defaults for Sodium `0.8.14+mc1.21.11`.
  - `lithium.properties` — Safe tick/physics optimization flags for Lithium `0.21.4+mc1.21.11`.
  - `entityculling.json` — Async path-tracing entity/block-entity culling config for EntityCulling `1.11.3-mc1.21.11`.
  - `moreculling.toml` — Block-face, leaf, and item-frame culling config for More Culling `1.21.11-1.6.2`.
  - `immediatelyfast.json` — Immediate-mode GUI/HUD/entity batching config for ImmediatelyFast `1.14.3+1.21.11`.
  - `dynamic_fps.json` — Background/unfocused frame-rate throttling config for Dynamic FPS `3.11.6+minecraft-1.21.11-fabric`.

## Rebuilding the `.mrpack` Bundle

```bash
python3 scripts/build-modpack.py
```
