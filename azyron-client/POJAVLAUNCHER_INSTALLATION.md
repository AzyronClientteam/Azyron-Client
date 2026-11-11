# AZYRON CLIENT v1.0.4 — Installation

**Developer:** JIGNESH GAMING

AZYRON CLIENT v1.0.4 provides separate native files for **Minecraft 1.20.1 Fabric** and **Minecraft 1.21.11 Fabric**. The unsuffixed files target 1.21.11; the `-mc1.20.1` files target 1.20.1. Download/install only the pair matching your launcher instance. There is no client ZIP bundle.

| Target | Native Fabric JAR | Matching Modrinth `.mrpack` | Java |
|---|---|---|---:|
| 1.20.1 Fabric | `Azyron-Client-1.0.4-mc1.20.1.jar` | `Azyron-Modpack-1.0.4-mc1.20.1.mrpack` | 17+ |
| 1.21.11 Fabric | `Azyron-Client-1.0.4.jar` | `Azyron-Modpack-1.0.4.mrpack` | 21+ |

Each `.mrpack` is a standard, single-version Modrinth profile with the exact matching native client JAR embedded. Do not rename files or mix the two targets.

## PojavLauncher (Android)

1. Create or select a separate PojavLauncher profile for Minecraft `1.20.1` or `1.21.11`.
2. Install Fabric Loader `0.14.22+` for 1.20.1 or `0.20.1+` for 1.21.11, and select Java 17+ or Java 21+ respectively.
3. For a manual install, copy only the matching native JAR into that profile's `mods` folder along with the target's Fabric API. Alternatively, import the matching `.mrpack` in a launcher that supports Modrinth import.
4. The low-memory profile configures up to 6 view / 5 simulation chunks and a 60 FPS cap. These are settings, not measured device results or a guarantee of stable operation.
5. Select a renderer supported by your device and launcher. No Android GPU/renderer combination, Pojav game launch, or sustained-play performance test is claimed here.
6. Open the in-game Mods Menu with **Right Shift** or the pause-menu entry; open the HUD editor with **H** or its pause-menu entry.

## Desktop launchers

1. Create an instance for the Minecraft version you want and install its matching Fabric Loader.
2. For a manual JAR install, place only the matching native client JAR and Fabric API in the instance's `mods` folder.
3. Or import the matching `.mrpack` profile. Do not import the two profiles into one instance.

The v1.0.4 GitHub release is currently an unpublished draft. Its attached files are not public downloads until publication is explicitly approved by JIGNESH GAMING.

See [DEPENDENCIES.md](DEPENDENCIES.md) for version-appropriate dependencies, and [TESTING_REPORT_v1.0.4.md](TESTING_REPORT_v1.0.4.md) for the exact build-versus-gameplay validation boundary.
