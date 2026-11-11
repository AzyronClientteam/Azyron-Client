# AZYRON CLIENT v1.0.3 — Standard Fabric Launcher Installation

**Developer:** **JIGNESH GAMING**

AZYRON CLIENT v1.0.3 is distributed as separate native JAR and Modrinth pack pairs. Choose the Minecraft version in your launcher and install only the matching pair.

| Minecraft target | Native client JAR | Matching Modrinth pack | Java |
|---|---|---|---|
| **1.20.1 Fabric** | `Azyron-Client-1.0.3-mc1.20.1.jar` | `Azyron-Modpack-1.0.3-mc1.20.1.mrpack` | Java 17 or 21 |
| **1.21.11 Fabric** | `Azyron-Client-1.0.3-mc1.21.11.jar` | `Azyron-Modpack-1.0.3-mc1.21.11.mrpack` | Java 21+ |

The game version is selected by the launcher—not by an in-game client setting. Never install a 1.20.1 JAR/profile in a 1.21.11 instance, or vice versa.

## PojavLauncher (Android)

1. Create or select a PojavLauncher profile for the target Minecraft version above.
2. Install Fabric Loader for that exact version (`0.14.22+` for 1.20.1; `0.20.1` for 1.21.11) and select Java 17/21 or Java 21 respectively.
3. Copy the matching native JAR into that profile's `mods` folder and install the matching Fabric API. The matching `.mrpack` contains the version-specific optimization dependencies and config files, but use it only with a launcher that supports importing Modrinth packs.
4. For an approximately 3GB device, the built-in profile targets up to 6 view / 5 simulation chunks and a 60 FPS cap. As a starting JVM allocation, use about 1280–1408 MB and leave headroom for Android and graphics-driver memory. These are configuration suggestions, not a guarantee of stable operation or 60 FPS.
5. Choose a renderer supported by your device and launcher. No specific Android GPU/renderer combination has been physically validated in this task.
6. In game, open the Mods Menu with **Right Shift** or from the pause screen; open the HUD Editor with **H** or its pause-screen button. HUD, Armour Hub, and the other existing client modules remain part of the client.

## Desktop launchers

### Standard Fabric JAR

1. Create a separate game profile for Minecraft `1.20.1` or `1.21.11` and install the matching Fabric Loader.
2. Place the matching native client JAR and Fabric API in that instance's `mods` folder.
3. Add only the companion mods intended for that Minecraft target. See [DEPENDENCIES.md](DEPENDENCIES.md) for the pinned versions and hashes.

### Modrinth `.mrpack`

In a launcher that supports Modrinth pack import, import the `.mrpack` whose filename matches the target Minecraft version in the table above. Each pack contains one game version and its matching native client build; it is not a multi-version installer.

## Building both native pairs from source

Run from `azyron-client/` with Gradle/Minecraft dependencies available:

```bash
./gradlew clean build -Pminecraft_version=1.20.1
python3 scripts/build-modpack.py --mc 1.20.1

./gradlew clean build -Pminecraft_version=1.21.11
python3 scripts/build-modpack.py --mc 1.21.11
```

The builder checks the JAR's declared Minecraft target before creating the matching pack. Build/package checks do not exercise an actual game launch. Desktop/mobile gameplay, multiplayer, renderer stability, and device FPS/RAM measurements remain pending; 60+ FPS at six chunks is a target, not a measured result. See [COMPATIBILITY_1201_BUILD_REPORT.md](COMPATIBILITY_1201_BUILD_REPORT.md) and [TESTING_REPORT.md](TESTING_REPORT.md) for validation scope.
