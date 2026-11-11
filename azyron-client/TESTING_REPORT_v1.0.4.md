# AZYRON CLIENT v1.0.4 — Validation Status

**Developer:** JIGNESH GAMING
**Download format:** one Client bundle ZIP and one Modpack bundle ZIP; each contains native payloads for both targets.
**Targets:** Minecraft 1.20.1 Fabric and Minecraft 1.21.11 Fabric.

## Native build and bundle status — PASSED

GitHub Actions run [42960143679](https://github.com/AzyronClientteam/Azyron-Client/actions/runs/42960143679), source commit [`073a99f`](https://github.com/AzyronClientteam/Azyron-Client/commit/073a99ffe7dae42000aba7078207c27597a742e4), completed successfully. Each target ran a separate Gradle Fabric/Loom `build` against real Minecraft/Yarn/Fabric dependencies. The normal `check` task also ran the actual-classpath compatibility audit; both matrix jobs passed. No generated Minecraft/Fabric stubs were used for these builds.

| Target | Native Fabric/Loom build | Actual-classpath audit | Native package |
|---|---|---|---|
| Minecraft 1.20.1 / Fabric | **PASSED** | **PASSED** | Java 17 JAR + matching 1.20.1 `.mrpack` |
| Minecraft 1.21.11 / Fabric | **PASSED** | **PASSED** | Java 21 JAR + matching 1.21.11 `.mrpack` |

The candidate job also passed native JAR/mixin/bytecode checks, exact JAR-to-MRPack pairing checks, bundle ZIP integrity checks, and SHA-256 verification. The generated files were assembled from both real target builds; the workflow's draft step was manually enabled for this run.

## Draft release assets

GitHub release **AZYRON CLIENT v1.0.4** was created as a **draft only** (release ID `441244221`; `draft=true`, `published_at=null`, `prerelease=false`). The draft targets source commit `073a99f` and contains exactly the two requested bundle archives:

| Draft asset | Size | SHA-256 |
|---|---:|---|
| `Azyron-Client-v1.0.4.zip` | 411,668 bytes | `1c2c0282703120cc7877328cf0d2a37653d0f82bbb83477d8775a1c99b7e1226` |
| `Azyron-Modpack-v1.0.4.zip` | 445,810 bytes | `83fe62742128c79b13572fadb496a3fc2d297ace4c11467ffe3647e68309d8d4` |

[Open the v1.0.4 draft](https://github.com/AzyronClientteam/Azyron-Client/releases/tag/untagged-a0d4e8fc6e2c2cb00f3d). The draft is not published. Neither the individual per-version JARs nor the individual `.mrpack` profiles are separate release assets.

The Client ZIP contains `1.20.1/Azyron-Client-1.0.4-mc1.20.1.jar` and `1.21.11/Azyron-Client-1.0.4-mc1.21.11.jar`. The Modpack ZIP contains `1.20.1/Azyron-Modpack-1.0.4-mc1.20.1.mrpack` and `1.21.11/Azyron-Modpack-1.0.4-mc1.21.11.mrpack`. These are distinct native target files, not cross-version binaries.

## Structural regression tests

- Offline packaging/configuration tests: **10/10 passed** in run 42960143679.
- Release portal integration tests: **5/5 passed** in run 42960143679.
- Both suites use explicitly synthetic metadata-only fixture JARs for schema, bundle, route, and checksum regression coverage. They are **not** compiled mods and do not establish Fabric compatibility or gameplay.

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

Publication is **NOT AUTHORIZED**. Release ID `441244221` must remain a draft until JIGNESH GAMING explicitly approves publication. The previous v1.0.3 draft is separate and has not been replaced. The manual validation-workflow option creates or refreshes the v1.0.4 draft only; ordinary pushes do not modify releases, and no workflow step publishes it.
