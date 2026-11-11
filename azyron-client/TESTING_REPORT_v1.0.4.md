# AZYRON CLIENT v1.0.4 — Validation Status

**Developer:** JIGNESH GAMING
**Download format:** one Client bundle ZIP and one Modpack bundle ZIP; each contains native payloads for both targets.
**Targets:** Minecraft 1.20.1 Fabric and Minecraft 1.21.11 Fabric.

## Validation status

The v1.0.4 real Fabric/Loom build matrix is **PENDING CI**. Each game target must build separately against its own Minecraft, Yarn, Fabric Loader, and Fabric API dependencies. The CI candidate step must validate each native JAR and matching Modrinth profile before assembling the two user download bundles.

| Target | Native Fabric/Loom build | Actual-classpath audit | Bundle payload |
|---|---|---|---|
| Minecraft 1.20.1 / Fabric | PENDING | PENDING | Native Java 17 JAR + matching 1.20.1 `.mrpack` |
| Minecraft 1.21.11 / Fabric | PENDING | PENDING | Native Java 21 JAR + matching 1.21.11 `.mrpack` |

The client bundle is intended to contain `1.20.1/Azyron-Client-1.0.4-mc1.20.1.jar` and `1.21.11/Azyron-Client-1.0.4-mc1.21.11.jar`. The modpack bundle is intended to contain `1.20.1/Azyron-Modpack-1.0.4-mc1.20.1.mrpack` and `1.21.11/Azyron-Modpack-1.0.4-mc1.21.11.mrpack`. Those are internal target-specific payloads, not additional public download links.

## What build/package checks can establish

- Each target's source compiles against real Minecraft/Fabric dependencies, rather than generated stubs.
- The actual-classpath audit checks configured injections and target references for that build.
- The target's JAR metadata, Java class-file version, mixin config, and required feature classes are internally consistent.
- The corresponding `.mrpack` declares the same Minecraft version and embeds the exact matching client JAR.
- The outer bundle ZIPs contain the two expected target payloads with valid checksums and archive integrity.

These checks do **not** demonstrate full runtime Mixin transformation or a successful game launch.

## Gameplay and hardware tests — NOT RUN

- Minecraft startup, world loading, complete Mixin application, menus, HUD, Armour Hub, Mods Menu, and saves: **NOT RUN**.
- Multiplayer joins, chunk generation, long sessions, and crash/stability testing: **NOT RUN**.
- Android/PojavLauncher, renderer compatibility, thermal behavior, OOM/ANR, and physical-device testing: **NOT RUN**.
- Average FPS, frame times/1% lows, heap, process RSS/native memory, GC pauses, and thermal measurements: **NOT MEASURED**.

The 6-view / 5-simulation chunk profile and 60 FPS cap are configuration targets only. They are not achieved FPS, a performance benchmark, or a crash-free guarantee. No synthetic/stub test is presented as native game validation.

## Draft-release gate

The v1.0.4 release must remain a GitHub draft until JIGNESH GAMING approves publication. The previous v1.0.3 draft is a separate release and must not be replaced by this update. The v1.0.4 draft should contain exactly the two requested download bundles, with no individual per-version downloads presented as separate public assets.
