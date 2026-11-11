# AZYRON CLIENT v1.0.4 — Validation Status

**Developer:** JIGNESH GAMING
**Selected target mapping:** unsuffixed JAR/MRPack = Minecraft 1.21.11; explicit `-mc1.20.1` files = Minecraft 1.20.1.
**Release state:** unpublished draft; publication is not authorized.

## Corrected direct-file distribution — PASSED

The former Client ZIP and Modpack ZIP were removed from the v1.0.4 draft after the replacement assets were uploaded and verified. The release now contains four target-native downloads, plus `SHA256SUMS.txt` and `BUILD-PROVENANCE.json`:

| Minecraft target | Native Fabric JAR | Size | SHA-256 | Matching Modrinth profile | Size | SHA-256 |
|---|---|---:|---|---|---:|---|
| **1.21.11** | `Azyron-Client-1.0.4.jar` | 233,108 bytes | `8647f22472a1fac330b22985d3c821d6ec289f657c1c0594c214976031b60caa` | `Azyron-Modpack-1.0.4.mrpack` | 227,455 bytes | `b92a409806383ddf5619fb8eaa16e9ca2a9895edb59fae9a15ee4bcfcc85d58f` |
| **1.20.1** | `Azyron-Client-1.0.4-mc1.20.1.jar` | 230,532 bytes | `beb0c809121fac6d00006493d5c7ab47bc3f1ec465f3aa61a15f349cbb0b8107` | `Azyron-Modpack-1.0.4-mc1.20.1.mrpack` | 224,722 bytes | `bf1f94b6833341586f89bcf4bca15cd55978113245d6d1590e410c989b2f3c1d` |

The unsuffixed files are **only for Minecraft 1.21.11**. The suffixed pair is **only for Minecraft 1.20.1**. Each `.mrpack` declares one Minecraft version and embeds the exact byte-identical native JAR from the same row. No client ZIP is supplied.

[Open the v1.0.4 draft](https://github.com/AzyronClientteam/Azyron-Client/releases/tag/untagged-5c42243de2e5c21dc359). It remains `draft=true` with `published_at=null`.

## Native builds and package validation — PASSED

GitHub Actions workflow run [42965497293](https://github.com/AzyronClientteam/Azyron-Client/actions/runs/42965497293), source commit [`3f13d12`](https://github.com/AzyronClientteam/Azyron-Client/commit/3f13d129c2bd91d6c5ecd631d0118b186a77f235), completed successfully. It built each supported target independently with Gradle and Fabric Loom against real Minecraft/Yarn/Fabric dependencies; no generated Minecraft/Fabric stubs were used.

- Minecraft **1.20.1 Fabric / Java 17**: native build and actual-classpath compatibility audit **PASSED**.
- Minecraft **1.21.11 Fabric / Java 21**: native build and actual-classpath compatibility audit **PASSED**.
- Candidate validation confirmed Fabric metadata, exact Minecraft/Java dependencies, compiled client classes, class-file major versions (61 for Java 17 and 65 for Java 21), configured mixin class files, and absence of bundled Minecraft/test stubs.
- Both target-specific Modrinth packs passed archive/index/config validation and exact JAR-to-MRPack byte pairing.
- The four uploaded download assets were verified against GitHub's API-reported sizes and SHA-256 digests. The two known obsolete ZIP assets were then removed; final draft asset verification passed.
- Offline packaging/installer tests: **13/13 passed**. Portal integration tests: **5/5 passed**. These tests use synthetic fixture JARs/files for structural regression checks and are not runtime/game tests.
- The ordinary push workflow also passed the direct-asset build/package pipeline in run [42965286837](https://github.com/AzyronClientteam/Azyron-Client/actions/runs/42965286837); its release-refresh job was correctly skipped.

The separate v1.0.3 draft was not modified.

## What build/package validation establishes

The checks establish that each downloadable JAR is a real target-specific Fabric Loom remap build with the expected metadata and bytecode target, and that each `.mrpack` matches one Minecraft version and embeds the exact corresponding native JAR. They also establish archive integrity and release-asset hashes.

They do **not** demonstrate full runtime Mixin transformation or a successful game launch.

## Gameplay and hardware tests — NOT RUN

- Minecraft startup, world loading, complete runtime Mixin application, menus, HUD, Armour Hub, Mods Menu, and saves: **NOT RUN**.
- Multiplayer joins, chunk generation, long sessions, and crash/stability testing: **NOT RUN**.
- Android/PojavLauncher, renderer compatibility, thermal behavior, OOM/ANR, and physical-device testing: **NOT RUN**.
- Average FPS, frame times/1% lows, heap, process RSS/native memory, GC pauses, and thermal measurements: **NOT MEASURED**.

The 6-view / 5-simulation chunk profile and 60 FPS cap are configuration targets only. They are not achieved FPS, a performance benchmark, or a crash-free guarantee. No synthetic/stub test is presented as native game validation.

## Draft-release gate

Publication is **NOT AUTHORIZED**. Release ID `441244221` remains an unpublished draft until JIGNESH GAMING explicitly approves publication. The manual refresh workflow only updates the existing v1.0.4 draft; it does not create or publish a release.
