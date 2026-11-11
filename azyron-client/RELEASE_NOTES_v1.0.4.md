# AZYRON CLIENT v1.0.4 — Release Notes

**Developer:** JIGNESH GAMING

> **Release state: DRAFT — NOT PUBLISHED.** Do not publish until JIGNESH GAMING explicitly approves publication.

## Highlights

- Supports Minecraft **1.20.1 Fabric** and **1.21.11 Fabric** with separate native Fabric/Loom builds, target-specific adapters, and mixin sets.
- Distributes the actual compiled Fabric client JAR and matching standard Modrinth `.mrpack` directly for each target. **No client ZIP bundle is provided.**
- Keeps the AZYRON CLIENT name and in-game branding, including the Mods Menu, HUD, Armour Hub, cosmetics, and shared FPS/memory profile code.
- Keeps equivalent low-memory defaults for both targets, using each companion mod's matching configuration schema. Settings are targets, not measured performance claims.

## Direct downloads

| Minecraft target | Native executable Fabric mod | Matching Modrinth profile | Java |
|---|---|---|---:|
| **1.21.11** | `Azyron-Client-1.0.4.jar` | `Azyron-Modpack-1.0.4.mrpack` | 21+ |
| **1.20.1** | `Azyron-Client-1.0.4-mc1.20.1.jar` | `Azyron-Modpack-1.0.4-mc1.20.1.mrpack` | 17+ |

The requested unsuffixed filenames are for **1.21.11 only**. The explicitly suffixed JAR and `.mrpack` are for **1.20.1 only**. Use the JAR and Modrinth profile from the same row; neither JAR is universal across both Minecraft versions. The `.mrpack` embeds the exact matching native client JAR.

Install the `.mrpack` with a Modrinth-compatible launcher, or place only the matching JAR in a matching Fabric instance. The release draft also includes `SHA256SUMS.txt` and `BUILD-PROVENANCE.json` for verification.

## Compatibility and build validation

The project uses separate Fabric/Loom builds for Minecraft `1.20.1` (Java 17 bytecode, Yarn `1.20.1+build.10`, Loader `0.14.22+`, Fabric API `0.88.1+1.20.1`) and `1.21.11` (Java 21 bytecode, Yarn `1.21.11+build.6`, Loader `0.20.1+`, Fabric API `0.141.6+1.21.11`). Release checks verify actual remapped JARs, target metadata, class-file versions, configured mixins and classes, separate Modrinth indices, and exact JAR-to-MRPack pairing.

Current CI status is recorded in [TESTING_REPORT_v1.0.4.md](TESTING_REPORT_v1.0.4.md). Passing a native build or package-integrity check does not establish game startup, runtime Mixin application, gameplay, Android/Pojav compatibility, stability, or performance.

## Testing limits

No gameplay, FPS, RAM, thermal, renderer, or device-performance result is claimed unless explicitly recorded as measured in the testing report. A configured 60 FPS cap is not evidence that 60 FPS is achieved. Historical generated-stub checks are not native game validation.
