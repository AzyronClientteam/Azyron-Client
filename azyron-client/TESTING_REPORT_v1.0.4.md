# AZYRON CLIENT v1.0.4 — Validation Status

**Developer:** JIGNESH GAMING
**Download format:** one Client bundle ZIP and one Modpack bundle ZIP; each contains native payloads for both targets.
**Targets:** Minecraft 1.20.1 Fabric and Minecraft 1.21.11 Fabric.

## Native build and packaging status — PASSED

GitHub Actions run [42959203065](https://github.com/AzyronClientteam/Azyron-Client/actions/runs/42959203065), commit [`4accb54`](https://github.com/AzyronClientteam/Azyron-Client/commit/4accb54fdabee903c901d1f529b0bd26f0dad66d), completed successfully. Each target ran a separate Gradle Fabric/Loom `build` against real Minecraft/Yarn/Fabric dependencies. The normal `check` task also ran the actual-classpath compatibility audit; both matrix jobs passed. No generated Minecraft/Fabric stubs were used for these builds.

| Target | Native Fabric/Loom build | Actual-classpath audit | Native package |
|---|---|---|---|
| Minecraft 1.20.1 / Fabric | **PASSED** | **PASSED** | Java 17 JAR + matching 1.20.1 `.mrpack` |
| Minecraft 1.21.11 / Fabric | **PASSED** | **PASSED** | Java 21 JAR + matching 1.21.11 `.mrpack` |

The candidate job also passed native JAR/mixin/bytecode checks, exact JAR-to-MRPack pairing checks, bundle ZIP integrity checks, and SHA-256 verification. It uploaded the Actions artifact `Azyron-v1.0.4-native-bundles-NOT-PUBLISHED`; this is a validation artifact, not a release or a public download. The workflow does not create, edit, or publish a GitHub release.

The bundle layout is one Client ZIP with `1.20.1/Azyron-Client-1.0.4-mc1.20.1.jar` and `1.21.11/Azyron-Client-1.0.4-mc1.21.11.jar`; and one Modpack ZIP with `1.20.1/Azyron-Modpack-1.0.4-mc1.20.1.mrpack` and `1.21.11/Azyron-Modpack-1.0.4-mc1.21.11.mrpack`. These are separate target-specific files, not additional public download links or cross-version binaries.

## Structural regression tests

- Offline packaging/configuration tests: **10/10 passed** in CI run 42959203065.
- Release portal integration tests: **5/5 passed** locally; they are also included in the current CI workflow for subsequent runs.
- These suites use explicitly synthetic metadata-only fixture JARs for schema, bundle, route, and checksum regression coverage. They are **not** compiled mods and do not establish Fabric compatibility or gameplay.

## What build/package checks establish

- Each target's source compiles against its real Minecraft/Fabric classpath.
- The actual-classpath audit checks configured injections and target references for that build.
- Each JAR's target metadata, Java class-file version, mixin config, and required feature classes are internally consistent.
- Each `.mrpack` declares one matching Minecraft version and embeds the exact corresponding native client JAR.
- The two outer bundle ZIPs contain the expected target payloads and pass archive/checksum verification.

These checks do **not** demonstrate full runtime Mixin transformation or a successful game launch.

## Gameplay and hardware tests — NOT RUN

- Minecraft startup, world loading, complete Mixin application, menus, HUD, Armour Hub, Mods Menu, and saves: **NOT RUN**.
- Multiplayer joins, chunk generation, long sessions, and crash/stability testing: **NOT RUN**.
- Android/PojavLauncher, renderer compatibility, thermal behavior, OOM/ANR, and physical-device testing: **NOT RUN**.
- Average FPS, frame times/1% lows, heap, process RSS/native memory, GC pauses, and thermal measurements: **NOT MEASURED**.

The 6-view / 5-simulation chunk profile and 60 FPS cap are configuration targets only. They are not achieved FPS, a performance benchmark, or a crash-free guarantee. No synthetic/stub test is presented as native game validation.

## Draft-release gate

Publication is **NOT AUTHORIZED**. Any v1.0.4 GitHub release must remain a draft until JIGNESH GAMING explicitly approves publication. The previous v1.0.3 draft is separate and must not be replaced. The validation workflow does not change GitHub releases on ordinary pushes; its manual `create_draft=true` option only creates or refreshes a guarded draft after both native builds pass. It attaches exactly the two requested download bundles, with no individual per-version files presented as separate public assets.
