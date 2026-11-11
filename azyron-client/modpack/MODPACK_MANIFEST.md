# AZYRON CLIENT v1.0.4 — Modpack Bundle

The public **Modpack bundle** is one ZIP download, `Azyron-Modpack-v1.0.4.zip`. It contains two standard Modrinth profiles so the player can import the profile matching their launcher instance:

| Folder in bundle | Import file | Minecraft target | Runtime |
|---|---|---|---|
| `1.20.1/` | `Azyron-Modpack-1.0.4-mc1.20.1.mrpack` | Minecraft 1.20.1 Fabric | Java 17+ |
| `1.21.11/` | `Azyron-Modpack-1.0.4-mc1.21.11.mrpack` | Minecraft 1.21.11 Fabric | Java 21+ |

Each inner `.mrpack` is a separate native Modrinth profile; it contains exactly one Minecraft version and its matching client JAR. The outer bundle is the single user download. A manifest edit cannot convert one native JAR or `.mrpack` to another game version.

## Target-specific contents

- Each index pins the exact Minecraft version, Fabric Loader/API, and compatible optimization-mod versions.
- The 1.20.1 and 1.21.11 profiles use the same low-memory options (render 6, simulation 5, 60 FPS cap) and the corresponding config schemas for their companion mods.
- Both include the existing AZYRON CLIENT features and shared performance-profile configuration; game-specific adapters are compiled and checked separately.
- `scripts/build-modpack.py` verifies the client JAR's declared Minecraft version, the index, and the embedded JAR bytes before writing an `.mrpack`.

## Build

From `azyron-client/`, build each native JAR separately and use `scripts/prepare-native-candidates.py` to create the two public download bundles:

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

The generated bundles include a short README and keep each native target in its own folder. ZIP integrity and metadata checks do not constitute game-launch, gameplay, or performance tests.
