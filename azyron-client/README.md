# AZYRON CLIENT v1.0.4

> **AZYRON CLIENT – PERFORMANCE. SMOOTHNESS. STYLE.**
>
> **Developer:** JIGNESH GAMING
> **Distribution:** native Fabric JARs and matching standard Modrinth `.mrpack` files; no client ZIP bundle.

## Overview

**AZYRON CLIENT v1.0.4** is a Fabric client for Minecraft **1.20.1** and **1.21.11**. The shared client features include the Mods Menu, HUD editor, Armour Hub, cosmetics, and performance profiles. Each target is built and checked independently against its own Minecraft, Yarn, Fabric, Java, and mixin APIs.

A JAR is native to exactly one Minecraft target. The unsuffixed files selected for the release target **Minecraft 1.21.11**; the explicitly suffixed files target **Minecraft 1.20.1**. Do not rename a file, mix targets, or treat either binary as cross-version compatible.

## Downloads

| Minecraft target | Native Fabric client | Matching Modrinth profile | Java |
|---|---|---|---:|
| **1.21.11** | `Azyron-Client-1.0.4.jar` | `Azyron-Modpack-1.0.4.mrpack` | 21+ |
| **1.20.1** | `Azyron-Client-1.0.4-mc1.20.1.jar` | `Azyron-Modpack-1.0.4-mc1.20.1.mrpack` | 17+ |

Install the `.mrpack` in a Modrinth-compatible launcher, or place only the matching native JAR in the `mods` folder of a Fabric instance with Fabric API. The `.mrpack` is itself a standard Modrinth archive; there is **no client ZIP bundle**.

The v1.0.4 GitHub release remains **an unpublished draft** until JIGNESH GAMING explicitly approves publication. Draft assets are for review and are not public release downloads.

## Supported targets

| Minecraft | Loader | Java | Build dependencies |
|---|---|---:|---|
| **1.20.1** | Fabric Loader 0.14.22+ | 17+ | Yarn `1.20.1+build.10`, Fabric API `0.88.1+1.20.1` |
| **1.21.11** | Fabric Loader 0.20.1+ | 21+ | Yarn `1.21.11+build.6`, Fabric API `0.141.6+1.21.11` |

The two profiles use equivalent low-memory game settings and version-appropriate companion-mod schemas. The client applies the same shared HUD, Armour Hub, Mods Menu, and FPS/memory profile code, with target-specific adapters where Minecraft APIs differ.

## Existing features

- **Mods Menu:** `Right Shift` or the in-game pause-menu entry.
- **HUD editor:** `H`, with draggable widgets, scaling, and layout controls.
- **Armour Hub:** armor and main-hand display with durability information.
- **HUD modules:** FPS/frame-time, CPS, ping, coordinates, keystrokes, potion effects, crosshair, memory, sprint, and zoom.
- **Performance profiles:** Potato, Balanced, and Smooth FPS, with hardware-aware memory limits and adaptive safeguards.

The low-memory defaults target up to 6 view / 5 simulation chunks and a 60 FPS cap. Those are configuration targets—not evidence of achieved FPS or reduced RAM use.

## Validation limits

The 1.20.1 and 1.21.11 native Fabric/Loom builds and actual-classpath audits run separately in CI. Their success verifies compilation and targeted classpath/mixin configuration; it does **not** prove game startup, complete runtime Mixin application, gameplay stability, Android/Pojav compatibility, or FPS performance. See [TESTING_REPORT_v1.0.4.md](TESTING_REPORT_v1.0.4.md) for current results. No gameplay or hardware-performance result is claimed unless recorded there as measured.

## Build from source

Run each native build independently from `azyron-client/`, preserving the first output before running `clean` for the second:

```bash
./gradlew clean build -Pminecraft_version=1.20.1
mkdir -p /tmp/azyron-v1.0.4-native
cp build/libs/Azyron-Client-1.0.4-mc1.20.1.jar /tmp/azyron-v1.0.4-native/

./gradlew clean build -Pminecraft_version=1.21.11
cp build/libs/Azyron-Client-1.0.4.jar /tmp/azyron-v1.0.4-native/

python3 scripts/prepare-native-candidates.py \
  --inputs /tmp/azyron-v1.0.4-native \
  --out /tmp/azyron-v1.0.4-direct-assets \
  --run-id local --commit "$(git rev-parse HEAD)"
```

Candidate preparation verifies each real remapped Fabric JAR, Java class-file target, Fabric metadata, mixin classes, target-specific MRPack index, exact JAR-to-MRPack pairing, and SHA-256 values. It emits four direct download assets and sidecar records; it never creates a client or modpack bundle ZIP. These packaging checks are not game-launch or gameplay tests.

## Documentation

- [Release notes — v1.0.4](RELEASE_NOTES_v1.0.4.md)
- [Validation report — v1.0.4](TESTING_REPORT_v1.0.4.md)
- [Installation guide](POJAVLAUNCHER_INSTALLATION.md)
- [Dependency and optimization matrix](DEPENDENCIES.md)
- [Target-specific Modrinth manifest](modpack/MODPACK_MANIFEST.md)
