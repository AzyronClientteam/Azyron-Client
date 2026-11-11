# AZYRON CLIENT v1.0.4 — Target-Specific Modrinth Profiles

Each Modrinth `.mrpack` is a separate, directly distributed, standard single-version profile. It contains exactly one Minecraft target and embeds the matching native Fabric client JAR. The unsuffixed files target **Minecraft 1.21.11**; the explicit `-mc1.20.1` files target **Minecraft 1.20.1**. No client ZIP bundle is produced.

| Minecraft target | Direct `.mrpack` download | Matching client JAR embedded in the profile | Runtime |
|---|---|---|---|
| 1.20.1 Fabric | `Azyron-Modpack-1.0.4-mc1.20.1.mrpack` | `Azyron-Client-1.0.4-mc1.20.1.jar` | Java 17+ |
| 1.21.11 Fabric | `Azyron-Modpack-1.0.4.mrpack` | `Azyron-Client-1.0.4.jar` | Java 21+ |

A manifest edit cannot convert a native JAR or `.mrpack` to another game version. Download/use the JAR and profile from the same row.

## Target-specific contents

- Each profile pins the exact Minecraft version, Fabric Loader/API, and compatible optimization-mod versions.
- The 1.20.1 and 1.21.11 profiles use the same low-memory options (render 6, simulation 5, 60 FPS cap) and the corresponding config schemas for companion mods.
- Both include the existing AZYRON CLIENT features and shared performance-profile configuration; game-specific adapters are compiled and checked separately.
- `scripts/build-modpack.py` verifies the native client JAR's declared Minecraft version, the active index, and the exact embedded JAR bytes before writing an `.mrpack`.
- Release validation additionally checks both compiled JARs' Fabric metadata, class-file version, required classes, configured mixins, archive integrity, and SHA-256 values.

## Build

From `azyron-client/`, build each native JAR independently and stage the four direct assets:

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

The generated directory contains the two native JARs, their two matching `.mrpack` files, and checksum/provenance/readme sidecars. It contains no client ZIP or outer modpack ZIP. Build/package validation does not establish game-launch, gameplay, device, or performance results.
