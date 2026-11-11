# AZYRON CLIENT v1.0.4 — Installation

**Developer:** JIGNESH GAMING

AZYRON CLIENT v1.0.4 has one Client bundle download and one Modpack bundle download. Each ZIP contains separate native payloads for **Minecraft 1.20.1 Fabric** and **Minecraft 1.21.11 Fabric**. Extract a bundle and use only the folder matching the version selected in your launcher.

| Target | Client JAR inside the Client bundle | Modrinth profile inside the Modpack bundle | Java |
|---|---|---|---:|
| 1.20.1 Fabric | `1.20.1/Azyron-Client-1.0.4-mc1.20.1.jar` | `1.20.1/Azyron-Modpack-1.0.4-mc1.20.1.mrpack` | 17+ |
| 1.21.11 Fabric | `1.21.11/Azyron-Client-1.0.4-mc1.21.11.jar` | `1.21.11/Azyron-Modpack-1.0.4-mc1.21.11.mrpack` | 21+ |

The `.mrpack` files are standard, single-version Modrinth profiles. The outer ZIPs are the one-download-per-product bundles; they do not merge the two native game builds into a cross-version binary.

## PojavLauncher (Android)

1. Create or select a separate PojavLauncher profile for Minecraft `1.20.1` or `1.21.11`.
2. Install Fabric Loader `0.14.22+` for 1.20.1 or `0.20.1+` for 1.21.11, and select Java 17+ or Java 21+ respectively.
3. For a manual install, extract the Client bundle and copy only the matching JAR into that profile's `mods` folder along with the target's Fabric API. Or import the matching `.mrpack` from the Modpack bundle in a launcher that supports Modrinth import.
4. The low-memory profile configures up to 6 view / 5 simulation chunks and a 60 FPS cap. These are settings, not measured device results or a guarantee of stable operation.
5. Select a renderer supported by your device and launcher. No Android GPU/renderer combination, Pojav game launch, or sustained-play performance test is claimed here.
6. Open the in-game Mods Menu with **Right Shift** or the pause-menu entry; open the HUD editor with **H** or its pause-menu entry.

## Desktop launchers

1. Create an instance for the Minecraft version you want and install its matching Fabric Loader.
2. For a manual JAR install, place only the matching native client JAR and Fabric API in the instance's `mods` folder.
3. Or import the matching `.mrpack` profile from the Modpack bundle. Do not import both profiles into one instance.

See [DEPENDENCIES.md](DEPENDENCIES.md) for version-appropriate dependencies, and [TESTING_REPORT_v1.0.4.md](TESTING_REPORT_v1.0.4.md) for the exact build-versus-gameplay validation boundary.
