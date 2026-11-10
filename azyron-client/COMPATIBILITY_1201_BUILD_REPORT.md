# Minecraft 1.20.1 / 1.21.11 native Fabric build repair

**Azyron Client v1.0.3 / Azyron Modpack v1.0.3**
Developer: **JIGNESH GAMING**

## Scope and release safety

The existing release (numeric ID `437365942`, tag `v1.0.3`) was public when this task began. On the owner's explicit instruction it has been changed to **draft**; the numeric release and tag were retained. A public tag-page URL may stop working while the release is a draft. No old/stub-built binaries are uploaded by this task. Candidate packaging is gated on successful real Fabric builds for **both** versions and requires separate publication approval.

## Complete original compiler inventory

The full untruncated initial CI log was recovered through read-only diagnostic checks on commit `8963c84`. Javac's maximum-error limit was increased to 1,000; the initial build reports **49 errors**, so this is not a truncated first-100-errors list. Inventory below records original file/line positions, before the fixes. Several lines are cascading errors from the same unavailable type. Overrides/signature errors are included as well as all missing symbols.

| Original file and line (under `src/main/java/`) | Diagnostic |
|---|---|
| `com/jigneshgaming/azyron/util/PlatformCompat.java:13` | package net.minecraft.client.session does not exist |
| `com/jigneshgaming/azyron/util/PlatformCompat.java:169` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/PersistentStateManagerMixin.java:12` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/PersistentStateManagerMixin.java:41` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/PersistentStateManagerMixin.java:99` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/PersistentStateManagerMixin.java:119` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/EntityRenderManagerMixin.java:6` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/MouseMixin.java:8` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/MouseMixin.java:34` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/Screen121Mixin.java:5` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/Screen121Mixin.java:7` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/Screen121Mixin.java:8` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/Screen121Mixin.java:25` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/Screen121Mixin.java:33` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/Screen121Mixin.java:41` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/Screen121Mixin.java:49` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/Screen121Mixin.java:57` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/RealmsAvailabilityMixin.java:6` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/RealmsAvailabilityMixin.java:7` | package net.minecraft.client.session does not exist |
| `com/jigneshgaming/azyron/mixin/RealmsAvailabilityMixin.java:23` | package RealmsAvailability does not exist |
| `com/jigneshgaming/azyron/mixin/ChunkBatchSizeCalculatorMixin.java:4` | cannot find symbol |
| `com/jigneshgaming/azyron/profile/PerformanceProfile.java:5` | cannot find symbol |
| `com/jigneshgaming/azyron/profile/PerformanceProfile.java:72` | cannot find symbol |
| `com/jigneshgaming/azyron/profile/PerformanceProfile.java:91` | cannot find symbol |
| `com/jigneshgaming/azyron/profile/PerformanceProfile.java:128` | cannot find symbol |
| `com/jigneshgaming/azyron/profile/PerformanceEngine.java:16` | cannot find symbol |
| `com/jigneshgaming/azyron/profile/MultiplayerPerformanceController.java:9` | cannot find symbol |
| `com/jigneshgaming/azyron/profile/MultiplayerPerformanceController.java:10` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/EntityRenderManagerMixin.java:17` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/RealmsAvailabilityMixin.java:19` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/ChunkBatchSizeCalculatorMixin.java:14` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/PersistentStateManagerMixin.java:77` | method readNbt in class PersistentStateManager cannot be applied to given types; |
| `com/jigneshgaming/azyron/mixin/Screen121Mixin.java:24` | method does not override or implement a method from a supertype |
| `com/jigneshgaming/azyron/mixin/Screen121Mixin.java:32` | method does not override or implement a method from a supertype |
| `com/jigneshgaming/azyron/mixin/Screen121Mixin.java:40` | method does not override or implement a method from a supertype |
| `com/jigneshgaming/azyron/mixin/Screen121Mixin.java:48` | method does not override or implement a method from a supertype |
| `com/jigneshgaming/azyron/mixin/Screen121Mixin.java:56` | method does not override or implement a method from a supertype |
| `com/jigneshgaming/azyron/mixin/RealmsAvailabilityMixin.java:27` | cannot find symbol |
| `com/jigneshgaming/azyron/mixin/RealmsAvailabilityMixin.java:31` | package RealmsAvailability does not exist |
| `com/jigneshgaming/azyron/mixin/RealmsAvailabilityMixin.java:31` | package RealmsAvailability does not exist |
| `com/jigneshgaming/azyron/ui/AzyronBaseScreen.java:52` | method does not override or implement a method from a supertype |
| `com/jigneshgaming/azyron/module/impl/ArmorHudModule.java:144` | cannot find symbol |
| `com/jigneshgaming/azyron/profile/PerformanceProfile.java:15` | cannot find symbol |
| `com/jigneshgaming/azyron/profile/PerformanceProfile.java:34` | cannot find symbol |
| `com/jigneshgaming/azyron/profile/PerformanceProfile.java:53` | cannot find symbol |
| `com/jigneshgaming/azyron/profile/PerformanceEngine.java:125` | cannot find symbol |
| `com/jigneshgaming/azyron/profile/MultiplayerPerformanceController.java:51` | cannot find symbol |
| `com/jigneshgaming/azyron/profile/MultiplayerPerformanceController.java:52` | cannot find symbol |
| `com/jigneshgaming/azyron/profile/MultiplayerPerformanceController.java:185` | cannot find symbol |

## Root causes and fixes

- **Moved types:** 1.20.1 uses `client.util.Session`, `client.option.ParticlesMode`, `gui.screen.ConnectScreen`, and `gui.screen.LevelLoadingScreen`. The 1.21.11 equivalents live in different packages. Two small native `MinecraftVersionApi` adapters now isolate these differences. Both are compiled against actual Minecraft dependencies. No fake Minecraft classes, textual source rewriting, or per-frame reflection is introduced by the adapters.
- **Armour HUD:** 1.20.1 uses public `DrawContext.drawItemInSlot`; 1.21.11 uses public `drawStackOverlay`. Both remain active through the native adapter, preserving durability/stack overlays.
- **Particles:** a shared `ParticleQuality` profile enum maps to each game's actual particle enum in its native adapter. The policy is shared, not copied into diverging FPS engines.
- **Screens/input:** removed an invalid shared `@Override` on the four-argument scroll overload. 1.20.1 retains primitive input callbacks; modern record inputs are compiled only with the modern `Screen121Mixin`.
- **Modern-only types:** `PersistentStateType`, `MouseInput`, `Click`, `KeyInput`, `CharInput`, `RealmsAvailability`, and `ChunkBatchSizeCalculator` are not imported or compiled into the legacy target. Their legacy functionality uses the existing compatible persistent-state, mouse, HUD, zoom, and chunk-load hooks. The absent old chunk-batch protocol is not emulated with incompatible packets. Normal online authentication is not bypassed.
- **Entity rendering:** 1.20.1's `EntityRenderDispatcher` gets a native mixin calling the same shared `MultiplayerPerformanceController` used by modern `EntityRenderManager`. No culling/gameplay logic was removed to compile.
- **Mixins:** source selection and processed mixin JSON agree. Only the selected version's mixins are present in each native JAR. Legacy raw Intermediary selectors/field names were replaced by actual 1.20.1 named mappings and remapped through Loom; essential legacy hooks now require a match. This removes the previous legacy selector remap warnings. The nonexistent explicit legacy-AP refmap was removed; Loom's Mixin extension remaps annotation targets directly.
- **Build dependencies:** pinned the successfully resolved Loom **1.14.10**, Gradle **9.2.1**, actual per-version Yarn, Fabric Loader and Fabric API. 1.20.1 emits Java 17/class 61 bytecode; 1.21.11 emits Java 21/class 65 bytecode. Each built `fabric.mod.json` declares only its actual Minecraft version and correct Java requirement.
- **Packaging:** separate native filenames prevent wrong-version replacement. The builder rejects native JAR/manifest version mismatches; the portal no longer tries to convert a native JAR into another game version by editing the manifest. Old local test binaries under `releases/` are not inputs to candidate CI preparation.

## Shared FPS / features

`PerformanceEngine`, `MemoryOptimizer`, `IntegratedServerOptimizer`, `MultiplayerPerformanceController`, all modules, menus, HUD/Armour Hub, cosmetics and options/startup guards remain shared main sources. The same render/simulation/particle/worker policies and Sodium/Dynamic FPS configuration generators are used on both versions. Only native API calls and genuinely different hooks are separated.

The conservative 3GB profile remains render **6**, simulation **5**, target **60 FPS**, blend 0, entity render scale 0.5, bounded chunk workers, no forced GC and preserved save durability. Simulation 4 is not claimed. A target/cap is not a measured framerate. Remote server simulation remains server-controlled. No unverified ModernFix/EBE dependency was added to make compilation pass.

## Real validation (not generated stubs)

First repaired real build/audit: [Actions run 42775821760](https://github.com/AzyronClientteam/Azyron-Client/actions/runs/42775821760), commit `289773f`:

| Target | Actual dependencies | Build | Real-classpath structural/policy checks |
|---|---|---|---|
| 1.20.1 | Yarn 1.20.1+build.10, Loader 0.14.22, Fabric API 0.88.1+1.20.1 | `remapJar` PASS | 80 PASS |
| 1.21.11 | Yarn 1.21.11+build.6, Loader 0.20.1, Fabric API 0.141.6+1.21.11 | `remapJar` PASS | 84 PASS |

The audit reads actual named Minecraft bytecode from Loom's real classpath; it does not substitute Minecraft/Fabric classes or start the game. It checks active injection/shadow targets and redirect callsites, plus pure shared policy/configuration logic. Callback-argument checking and candidate packaging are additionally gated in the final run.

Local Python packaging/installer tests: **8 PASS**, including native mismatch rejection, native portal selection, both profiles, corrupt-download atomicity and version guards. Fixtures in those Python tests test ZIP structure only and are **not** used as client build artifacts.

The legacy stub-based suites have been retained under `src/stubTest/java` (moved, not deleted) so standard `gradle build` cannot accidentally compile/run synthetic Minecraft constructors as real tests. `check` is wired to the real-classpath audit. These historical suites are not the real-build acceptance gate. Their success is not reported as runtime/gameplay certification.

## Remaining limits

Actual game startup/mixin transformation, physical-device HUD/menus/cosmetics/input behavior, SP/MP play, renderer compatibility, 3GB RAM/thermal/GC behavior and sustained FPS remain **NOT TESTED** in this task. Structural target/argument validation does not execute the full Mixin transformer or prove interoperability with every third-party mod. A modern Loom warning about remapping `modifiers` remains to be reviewed; successful compilation is not proof that every runtime warning is harmless.

**No 60+ FPS, crash-free behavior, memory-leak fix, or measured performance gain is claimed. Do not publish until the owner approves.**

## Reproducing standard real builds

From `azyron-client/`, with the installed Java 21 build toolchain and network access to Mojang/Fabric/Gradle repositories:

```sh
./gradlew clean build -Pminecraft_version=1.20.1
# Copy/store the 1.20.1 native JAR before cleaning for the other version.
./gradlew clean build -Pminecraft_version=1.21.11
```

The Gradle configuration selects the correct Yarn/Loader/Fabric API for the chosen version. Java 17 is the **1.20.1 runtime/output target**; using Java 21 to run Gradle does not change its class-61 output. Separate launcher instances and matching native files are required. The common source folder is not a claim that one remapped binary works on both games.

The CI preparation job depends on the entire matrix succeeding. It downloads the genuine per-version build outputs, validates metadata/class versions/mixin presence and retained HUD/menu classes, builds matching MRPacks, writes SHA-256/provenance, then downloads the candidate artifact again and checks all four file hashes. It does not call the GitHub Releases upload/publish API and has no contents-write permission.

## Initial standard-build acceptance and first prepared candidates

**Final run: [42776467477](https://github.com/AzyronClientteam/Azyron-Client/actions/runs/42776467477) — SUCCESS.** Candidate source commit: `97c231ea61b03d4543f101e8effa53a2994c2a99`.

| Target | Standard real `gradle build` | Actual-classpath checks (including callback arguments) | Output |
|---|---|---|---|
| 1.20.1 Fabric | PASS | 105 PASS | Java 17, class 61, 73 own classes |
| 1.21.11 Fabric | PASS | 110 PASS | Java 21, class 65, 74 own classes |

The class-count difference is from native adapter/mixin selection, not removal of modules. Both builds and all eight packaging tests passed before preparation. All four candidate files were re-downloaded **inside CI** and their SHA-256 checks passed. The sandbox retrieved provenance/logs via the check API; it did not separately download the binary bundle locally.

[Download the prepared native candidate bundle](https://github.com/AzyronClientteam/Azyron-Client/actions/runs/42776467477/artifacts/13743509075) — GitHub Actions artifact, sign-in may be required, retention 14 days. Includes both JAR/MRPack pairs, `SHA256SUMS.txt`, `BUILD-PROVENANCE.json` and safety README. These are not public-release downloads.

| Candidate file | Bytes | SHA-256 |
|---|---:|---|
| `Azyron-Client-1.0.3-mc1.20.1.jar` | 231812 | `5390cb332c85cd6521bf2440467e1b946ef744b71356a5936171e12157d6c7a7` |
| `Azyron-Modpack-1.0.3-mc1.20.1.mrpack` | 226057 | `64be061005e57d08411904cf073e23e2bdc0b6558dcee4d3e7427bb92642b512` |
| `Azyron-Client-1.0.3-mc1.21.11.jar` | 234391 | `00a5743ff994e86b7b2497264fcf06a6bfb33ca9ed4534add50599f876786f40` |
| `Azyron-Modpack-1.0.3-mc1.21.11.mrpack` | 228786 | `c035b8527361a787fa5d9178f0afa4887be2a05807c542582a85ca39ec5622f1` |

## Subsequent owner-requested draft release update

After the user explicitly requested an update to the existing release page, CI reran the standard native builds and packaging checks in [run 42785077400](https://github.com/AzyronClientteam/Azyron-Client/actions/runs/42785077400) from source commit `f3a566fa65a063334537cf7d2b414164b1b10d24`. Minecraft 1.20.1 and 1.21.11 both built successfully again; the 105/110 actual-classpath checks and eight packaging tests passed. The separate upload job downloaded that run's generated bundle inside GitHub Actions, rechecked its hashes and target metadata, and verified GitHub's asset digests before replacing the previous files.

The existing release **437365942** now has four version-specific native files plus `SHA256SUMS.txt` and `BUILD-PROVENANCE.json` attached. The two old generic-name stub assets (IDs `729999833` and `729999953`) were removed after the replacements passed verification. The release remains **draft=true**; it was not published, and the existing Git tag reference was not moved. GitHub currently returns this draft URL: [open draft release](https://github.com/AzyronClientteam/Azyron-Client/releases/tag/untagged-cb657c3dd524580623db). Draft releases may not resolve through the public `/releases/tag/v1.0.3` route until published; the release record remains associated with tag `v1.0.3`.

| Asset attached to the draft | Bytes | SHA-256 |
|---|---:|---|
| `Azyron-Client-1.0.3-mc1.20.1.jar` | 231812 | `5390cb332c85cd6521bf2440467e1b946ef744b71356a5936171e12157d6c7a7` |
| `Azyron-Modpack-1.0.3-mc1.20.1.mrpack` | 226057 | `b4ff111aeb32893ab999aad8de9ee7882dfff829418754d3369e0a593d7720a2` |
| `Azyron-Client-1.0.3-mc1.21.11.jar` | 234391 | `00a5743ff994e86b7b2497264fcf06a6bfb33ca9ed4534add50599f876786f40` |
| `Azyron-Modpack-1.0.3-mc1.21.11.mrpack` | 228786 | `2b5fe608aff5903c1c7243184b6b465f6ecd6c3e5dbfcbb959303d2990e277be` |

The earlier Actions candidate bundle remains an archived validation artifact; use the attached release assets and their current checksums above as the draft's authoritative files. No physical game/Mixin-transformation test or device FPS benchmark was added by the release update. No 60+ FPS claim is made.
