> **AZYRON CLIENT — PERFORMANCE. SMOOTHNESS. STYLE.**  
> Welcome to **AZYRON CLIENT `v1.0.2` PRE-RELEASE**, crafted by **JIGNESH GAMING** exclusively for **Minecraft Java Edition `1.21.11` with Fabric**. This comprehensive `v1.0.2` stability, compatibility, and performance release resolves `raids.dat` world data corruption/loss risks, normalizes invalid `fullscreen:off` option values, adds zero-crash Android/PojavLauncher missing-`libflite` narrator handling, silences offline Realms error spam without touching Microsoft auth, removes Android-incompatible `ImmediatelyFast` from the universal modpack, fixes `1.21.11` `ArmorHudModule` durability overlay linkage, and adds IntegratedServer tick/chunk I/O pacing across `3GB–8GB` RAM systems.

| Specification | Details |
| :--- | :--- |
| **Release Title** | `AZYRON CLIENT v1.0.2 PRE-RELEASE` |
| **Client Version** | **`1.0.2`** *(Pre-release)* |
| **Minecraft Compatibility** | **Java Edition `1.21.11` with Fabric** *(Strictly `1.21.11`)* |
| **Fabric Requirements** | Fabric Loader `0.20.1` *(min `>=0.17.3`)* + Fabric API `0.141.6+1.21.11` |
| **Java Runtime** | **Java 21 LTS** *(64-bit OpenJDK 21)* |
| **Developer** | **JIGNESH GAMING** |

---

## Direct `v1.0.2` Downloads

| Package | Filename & Direct Download | Description |
| :--- | :--- | :--- |
| **Client Mod JAR (`1.0.2`)** | [**`Azyron-Client-1.0.2.jar`**](releases/Azyron-Client-1.0.2.jar) | Standalone Fabric `1.21.11` client mod (`62` Java 21 classes, `15` Mixins) |
| **Universal Modpack (`1.0.2`)** | [**`Azyron-Modpack-1.0.2.mrpack`**](releases/Azyron-Modpack-1.0.2.mrpack) | Modrinth `v1` universal PC + Android/PojavLauncher modpack with `Azyron-Client-1.0.2.jar` + 8 verified `1.21.11` Fabric optimization mods |
| **Source Code Archive (`.zip`)** | [**`Azyron-Client-Source-v1.0.2.zip`**](releases/Azyron-Client-Source-v1.0.2.zip) | Complete `v1.0.2` Java 21 source code, Gradle build scripts, 25-suite verifier & modpack configs |
| **Source Code Archive (`.tar.gz`)** | [**`Azyron-Client-Source-v1.0.2.tar.gz`**](releases/Azyron-Client-Source-v1.0.2.tar.gz) | Complete `v1.0.2` compressed tarball source archive |

---

## What's New & Fixed in Changelog (`v1.0.2`)

- **Multiplayer Server-Join Lag & Crash Fix (`MultiplayerPerformanceController`, `ChunkBatchSizeCalculatorMixin`, `ClientWorldMixin`, `EntityRenderManagerMixin`, `MemoryOptimizer`, `InGameHudMixin`, `PotionHudModule`, `PingModule`):**
  - Removed automatic `System.gc()` calls during server login, `LevelLoadingScreen` / `ConnectScreen` / `ReconfiguringScreen`, and BungeeCord/Velocity dimension transfers to eliminate multi-second Stop-The-World GC freezes and KeepAlive disconnects on `3GB–8GB` devices.
  - Added protocol-compliant `ChunkBatchSizeCalculator` (`class_8760.method_53485()F`) chunk-batch pacing across `3GB–8GB` RAM tiers and an 80-tick (`4.0s`) server-join warmup ramp (`1.28–6.00` chunks/tick on join -> `2.98–14.00` steady state).
  - Added nanosecond frame-budgeted draining of `ClientWorld.runQueuedChunkUpdates()` (`class_638.method_38534()V` / `field_34804`) so `250–1,200+` chunk light bursts spread smoothly across frames (`<= 1.85 ms` max frame time on `3GB` tier) with zero dropped updates.
  - Added gameplay-safe distant hub entity culling (`EntityRenderManager.shouldRender`, `class_898.method_3950`) that preserves 100% of nearby entities (`<=16m`), players, combat hostiles, projectiles, mounts, and glowing targets while culling distant lobby hologram/item clutter.
  - Guarded HUD overlays during `LevelLoadingScreen` / `ConnectScreen` / `ReconfiguringScreen`, verified `RegistryEntry.hasKeyAndValue()` (`method_40227()Z`) before `.value()` in `PotionHudModule`, throttled `PingModule` latency polling to every 10 ticks, released old-world HUD caches on `MinecraftClient.joinWorld`, preserved **Armour Hub (`ArmorHudModule`)** in full, and logged diagnostics instead of hiding errors.

- **World Data Protection & `raids.dat` Recovery (`PersistentStateGuard` + `PersistentStateManagerMixin`):** Automatically backs up truncated, corrupted, or legacy `raids.dat` / `raids_end.dat` (and any unreadable world `.dat` state) into `<world>/data/azyron_backups/<file>.<timestamp>.bak` (+ diagnostic `.meta.txt`) **before** any repair or vanilla fallback occurs, repairs legacy/partial `1.21.11` raid NBT tags (`next_id`, `tick`, `raids`) in memory without deleting world files, and writes saves atomically via `.azyron.tmp` rename.
- **`fullscreen:off` & Boolean Option Sanitizer (`OptionsConfigSanitizer` + `GameOptionsMixin`):** Automatically normalizes non-standard boolean tokens (`"off"` → `"false"`, `"on"` → `"true"`, malformed strings → safe boolean defaults) during `GameOptions.load()` before `SimpleOption` parsing.
- **Android / PojavLauncher Missing `libflite` Narrator Fallback (`PlatformCompat` + `NarratorManagerMixin`):** Detects Android/PojavLauncher and missing Linux `libflite.so` native TTS libraries, catching JNA `UnsatisfiedLinkError` / Mojang `Narrator.FatalException` and falling back cleanly to `Narrator.EMPTY` without desktop TinyFileDialogs popups or crashes.
- **Graceful Offline / Local Realms Check (`RealmsAvailabilityMixin`):** Detects local/offline session tokens (`"0"`, `""`, `"offline"`) and skips unnecessary `RealmsClient` HTTP calls cleanly (`Type.AUTHENTICATION_ERROR` with zero log spam) while keeping online Microsoft authentication 100% untouched.
- **Android-Safe `1.21.11` Modpack Audit:** Verified all 8 universal PC + Android/PojavLauncher mods (**Fabric API `0.141.6`**, **Sodium `0.8.14`**, **Lithium `0.21.4`**, **FerriteCore `8.2.0`**, **EntityCulling `1.11.3`**, **More Culling `1.6.2`**, **Cloth Config `21.11.153`**, **Dynamic FPS `3.11.6`**) and moved desktop-only **ImmediatelyFast `1.14.3`** to opt-in PC flags (`--desktop-immediatelyfast` / `-DesktopImmediatelyFast`).
- **Singleplayer IntegratedServer Lag Fix (`IntegratedServerOptimizer` + `IntegratedServerMixin`):** Disables blocking synchronous chunk writes (`syncChunkWrites = false`), tracks a rolling 40-tick MSPT window, and smoothly paces simulation distance with hysteresis when server ticks exceed `46 ms` or RAM enters critical pressure.
- **`3GB – 8GB` RAM & CPU Thread Calibration (`MemoryOptimizer` + `PerformanceEngine`):** Adds hardware-aware `3GB`, `4GB`, `6GB`, and `8GB+` RAM tier calibration, bounded `1..4` chunk builder thread budgeting, emergency critical-RAM distance clamping, and automatic `.bak` backup before resetting malformed `azyron-client.json` configs.
- **HUD Rendering & `1.21.11` Bytecode Safety (`ArmorHudModule` + `HudModule` + `UiRenderUtils`):** Replaced inaccessible private `DrawContext.drawItemBar` (`method_64860`) with public `DrawContext.drawStackOverlay` (`method_51431`), wrapped every HUD `pushMatrix()`/`popMatrix()` in `try/finally`, guarded `NaN`/`Infinity` anchors & scales, and preserved `MouseMixin` `1.21.11` + cold-boot `rendertype_text` shader fixes.
