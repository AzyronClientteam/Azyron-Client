# AZYRON CLIENT v1.0.4 — Validation Status

**Developer:** JIGNESH GAMING
**Selected release mapping:** unsuffixed JAR/MRPack = Minecraft 1.21.11; explicit `-mc1.20.1` files = Minecraft 1.20.1.
**Release state:** draft only; publication is not authorized.

## Corrected direct-file distribution

The former Client ZIP and Modpack ZIP are superseded and must not be distributed. The corrected release consists of four separate target-native files:

| Minecraft target | Native Fabric JAR | Matching Modrinth profile | Java |
|---|---|---|---:|
| 1.21.11 | `Azyron-Client-1.0.4.jar` | `Azyron-Modpack-1.0.4.mrpack` | 21+ |
| 1.20.1 | `Azyron-Client-1.0.4-mc1.20.1.jar` | `Azyron-Modpack-1.0.4-mc1.20.1.mrpack` | 17+ |

Each JAR is intended to be a real Gradle/Fabric Loom remapped mod. Each standard `.mrpack` must declare only its own Minecraft target and embed the exact matching JAR. No client ZIP is produced.

## Historical native build validation

GitHub Actions run [42960726074](https://github.com/AzyronClientteam/Azyron-Client/actions/runs/42960726074), source commit [`9307b45`](https://github.com/AzyronClientteam/Azyron-Client/commit/9307b45b82971fd7bcf57d2fff5e80784561488e), passed separate real Fabric/Loom builds and actual-classpath audits for Minecraft 1.20.1 and 1.21.11. That run validated the earlier release packaging, which used outer ZIP bundles. It is **not** evidence that the corrected direct-filename pipeline, updated portal, or replacement of the draft's assets has passed.

## Current correction status

- Offline packaging and installer regression tests: **12/12 passed locally** using explicitly synthetic fixture JARs; these fixtures are not compiled or runnable mods.
- Release portal integration tests: **5/5 passed locally** with synthetic fixture files.
- The new workflow must still run both actual Gradle/Fabric Loom builds, verify compiled classes and metadata, generate the four direct assets, and confirm each exact JAR-to-MRPack pair before release assets are changed.
- The existing v1.0.4 draft currently has its obsolete two ZIP assets. The guarded workflow refresh is pinned to release ID `441244221`, uploads and verifies the four target-native files plus checksums/provenance first, removes only the two previously identified ZIP assets, and leaves the release unpublished. The separate v1.0.3 draft is not modified.

## What the native build/package checks establish

Once the corrected workflow passes, its checks establish that each target compiles against its real Minecraft/Fabric classpath; the JAR has target-specific Fabric metadata, Java bytecode, feature classes, and configured mixins; each `.mrpack` has one matching Minecraft dependency and contains the byte-identical native JAR; and the four direct assets pass archive and SHA-256 verification.

These checks do **not** demonstrate full runtime Mixin transformation or a successful game launch.

## Gameplay and hardware tests — NOT RUN

- Minecraft startup, world loading, complete Mixin application, menus, HUD, Armour Hub, Mods Menu, and saves: **NOT RUN**.
- Multiplayer joins, chunk generation, long sessions, and crash/stability testing: **NOT RUN**.
- Android/PojavLauncher, renderer compatibility, thermal behavior, OOM/ANR, and physical-device testing: **NOT RUN**.
- Average FPS, frame times/1% lows, heap, process RSS/native memory, GC pauses, and thermal measurements: **NOT MEASURED**.

The 6-view / 5-simulation chunk profile and 60 FPS cap are configuration targets only. They are not achieved FPS, a performance benchmark, or a crash-free guarantee. No synthetic/stub test is presented as native game validation.

## Draft-release gate

Publication is **NOT AUTHORIZED**. Release ID `441244221` must remain a draft until JIGNESH GAMING explicitly approves publication. The v1.0.3 draft is separate and must remain untouched. Ordinary pushes only validate/build candidates; a manual `create_draft=true` dispatch may refresh the existing v1.0.4 draft but never publishes it.
