# AZYRON CLIENT v1.0.4 — Release Notes

**Developer:** JIGNESH GAMING

> **Release state:** Draft only. Do not publish until the developer approves publication.

## Highlights

- Supports Minecraft **1.20.1 Fabric** and **1.21.11 Fabric** from one project. Each target is still compiled against its own Minecraft/Yarn/Fabric APIs and target-specific mixin set.
- Reduces the public download list to **one Client bundle ZIP** and **one Modpack bundle ZIP**. Each contains the two correctly matched native target payloads in separate folders; neither is a universal cross-version binary.
- Keeps the AZYRON CLIENT name and original in-game branding. The Mods Menu, HUD, Armour Hub, cosmetics, and shared FPS/memory profile code remain part of both target builds.
- Keeps equivalent low-memory defaults for both targets, using each companion mod's matching configuration schema. Settings are targets, not measured performance claims.

## Downloads

- `Azyron-Client-v1.0.4.zip` — contains the native 1.20.1 and 1.21.11 client JARs.
- `Azyron-Modpack-v1.0.4.zip` — contains the matching standard Modrinth `.mrpack` profile for each target.

After extracting a bundle, install/import only the folder for the Minecraft version selected in the launcher.

## Compatibility and build validation

The project uses separate real Fabric/Loom builds for Minecraft `1.20.1` (Java 17, Yarn `1.20.1+build.10`, Loader `0.14.22+`, Fabric API `0.88.1+1.20.1`) and `1.21.11` (Java 21, Yarn `1.21.11+build.6`, Loader `0.20.1+`, Fabric API `0.141.6+1.21.11`). Their native JARs and matching Modrinth profiles are validated separately before packaging the two bundles.

Current CI status is recorded in [TESTING_REPORT_v1.0.4.md](TESTING_REPORT_v1.0.4.md). Passing a native build or bundle integrity check does not establish game startup, runtime Mixin application, gameplay, Android/Pojav compatibility, stability, or performance.

## Testing limits

No gameplay, FPS, RAM, thermal, renderer, or device-performance result is claimed unless it is explicitly recorded as measured in the testing report. A configured 60 FPS cap is not evidence that 60 FPS is achieved. Historical generated-stub checks are not native game validation.
