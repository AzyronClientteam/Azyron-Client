# AZYRON CLIENT v1.0.4

> **AZYRON CLIENT – PERFORMANCE. SMOOTHNESS. STYLE.**
>
> **Developer:** JIGNESH GAMING
> **Downloads:** one Client bundle and one Modpack bundle; each contains native payloads for both supported Minecraft versions.

## Overview

**AZYRON CLIENT v1.0.4** is a Fabric client for Minecraft **1.20.1** and **1.21.11**. The shared client features include the Mods Menu, HUD editor, Armour Hub, cosmetics, and performance profiles. Version-specific Fabric adapters, mappings, dependencies, and mixin sets are built and checked separately for each game version.

The supplied download is a bundle, not a cross-version binary. Download each bundle once, extract it, then use only the folder for the Minecraft version selected in your launcher.

## Downloads

| Download | File | Contents |
|---|---|---|
| **Client bundle** | `Azyron-Client-v1.0.4.zip` | Native Fabric client JAR for 1.20.1 and native Fabric client JAR for 1.21.11 |
| **Modpack bundle** | `Azyron-Modpack-v1.0.4.zip` | Standard Modrinth `.mrpack` profile for 1.20.1 and standard Modrinth `.mrpack` profile for 1.21.11 |

Extract the relevant target folder and install its JAR or import its `.mrpack`. Do not mix the 1.20.1 and 1.21.11 contents. The GitHub release remains a **draft** until the developer approves publication.

## Supported targets

| Minecraft | Loader | Java | Build dependencies |
|---|---|---:|---|
| **1.20.1** | Fabric Loader 0.14.22+ | 17+ | Yarn `1.20.1+build.10`, Fabric API `0.88.1+1.20.1` |
| **1.21.11** | Fabric Loader 0.20.1+ | 21+ | Yarn `1.21.11+build.6`, Fabric API `0.141.6+1.21.11` |

The two profile folders use equivalent low-memory game settings and version-appropriate companion-mod schemas. The client applies the same shared HUD, Armour Hub, Mods Menu, and FPS optimization code, with target-specific adapters where Minecraft APIs differ.

## Existing features

- **Mods Menu:** `Right Shift` or the in-game pause-menu entry.
- **HUD editor:** `H`, with draggable widgets, scaling, and layout controls.
- **Armour Hub:** armor and main-hand display with durability information.
- **HUD modules:** FPS/frame-time, CPS, ping, coordinates, keystrokes, potion effects, crosshair, memory, sprint, and zoom.
- **Performance profiles:** Potato, Balanced, and Smooth FPS, with hardware-aware memory limits and adaptive safeguards.

The low-memory defaults target up to 6 view / 5 simulation chunks and a 60 FPS cap. Those are configuration targets—not evidence of achieved FPS or reduced RAM use.

## Validation limits

The 1.20.1 and 1.21.11 native Fabric/Loom builds and actual-classpath audits run separately in CI. Their success verifies compilation and the targeted classpath/mixin configuration; it does **not** prove game startup, complete runtime Mixin application, gameplay stability, Android/Pojav compatibility, or FPS performance. See [TESTING_REPORT_v1.0.4.md](TESTING_REPORT_v1.0.4.md) for current results. No gameplay or hardware-performance result is claimed unless recorded there as measured.

## Build from source

Run each native build independently from `azyron-client/`, preserving the first output before running `clean` for the second:

```bash
./gradlew clean build -Pminecraft_version=1.20.1
mkdir -p /tmp/azyron-v1.0.4-native
cp build/libs/Azyron-Client-1.0.4-mc1.20.1.jar /tmp/azyron-v1.0.4-native/

./gradlew clean build -Pminecraft_version=1.21.11
cp build/libs/Azyron-Client-1.0.4-mc1.21.11.jar /tmp/azyron-v1.0.4-native/

python3 scripts/prepare-native-candidates.py \
  --inputs /tmp/azyron-v1.0.4-native \
  --out /tmp/azyron-v1.0.4-download-bundles \
  --run-id local --commit "$(git rev-parse HEAD)"
```

The preparation script expects both real target builds and refuses a target-mismatched JAR. Its bundle/ZIP checks are packaging checks, not game tests.

## Documentation

- [Release notes — v1.0.4](RELEASE_NOTES_v1.0.4.md)
- [Validation report — v1.0.4](TESTING_REPORT_v1.0.4.md)
- [Installation guide](POJAVLAUNCHER_INSTALLATION.md)
- [Dependency and optimization matrix](DEPENDENCIES.md)
- [Modpack bundle manifest](modpack/MODPACK_MANIFEST.md)
