# AZYRON CLIENT v1.0.3 — Native Modrinth Profiles

This directory is the source for two **separate standard Modrinth packs**. Each pack contains one native client JAR and one matching Minecraft/Fabric profile. Use the pair for the game version selected in the launcher; a manifest edit cannot convert a JAR built for another Minecraft version.

| Minecraft target | Source index | Native JAR | Pack output |
|---|---|---|---|
| **1.20.1 Fabric** | `modrinth.index.1.20.1.json` | `Azyron-Client-1.0.3-mc1.20.1.jar` | `Azyron-Modpack-1.0.3-mc1.20.1.mrpack` |
| **1.21.11 Fabric** | `modrinth.index.json` | `Azyron-Client-1.0.3-mc1.21.11.jar` | `Azyron-Modpack-1.0.3-mc1.21.11.mrpack` |

## Target-matched contents

- Each index pins its own Minecraft version, Fabric Loader, Fabric API, and optimization-mod versions. `DEPENDENCIES.md` lists the pins and checksums.
- `overrides/config/` contains shared Azyron settings. The version-specific profile folders provide matching `options.txt` and companion config schemas.
- `icon.png` is the existing Azyron modpack icon.
- The pack builder verifies that the input JAR declares **exactly** the selected Minecraft target and embeds the unmodified JAR bytes. It rejects cross-version relabeling.
- The 1.20.1 and 1.21.11 profiles are alternatives, not two versions selected from inside one pack.

## Build

Run from `azyron-client/`, with the actual Gradle/Minecraft dependencies available:

```bash
./gradlew clean build -Pminecraft_version=1.20.1
python3 scripts/build-modpack.py --mc 1.20.1
cp build/libs/Azyron-Client-1.0.3-mc1.20.1.jar releases/

./gradlew clean build -Pminecraft_version=1.21.11
python3 scripts/build-modpack.py --mc 1.21.11
cp build/libs/Azyron-Client-1.0.3-mc1.21.11.jar releases/
```

The builder reads the matching native JAR from `build/libs/` and writes the versioned `.mrpack` under `releases/`. These build/package checks do not test a game launch or provide FPS/RAM benchmarks. Physical-device, renderer, multiplayer, and stability validation remain separate acceptance work; the 60+ FPS at six chunks goal is not a measured result.
