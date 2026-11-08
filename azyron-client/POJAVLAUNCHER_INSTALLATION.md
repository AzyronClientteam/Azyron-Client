# AZYRON CLIENT — PojavLauncher & Desktop Installation Guide

**Client:** AZYRON CLIENT `v1.0.2` (by **JIGNESH GAMING**)  
**Minecraft Version:** Java Edition **`1.21.11` ONLY**  
**Mod Loader:** **Fabric Loader** (`0.20.1` recommended, `>=0.17.3` supported)  
**Java Runtime:** **Java 21** (`OpenJDK 21` — mandatory for Minecraft `1.21.11`)

---

## Part 1: PojavLauncher Installation (Android / iOS)

### Prerequisites in PojavLauncher
1. Open **PojavLauncher** and go to **Settings → Java Tweaks**:
   - **Runtime**: Select **Internal-21 (Java 21)** (Minecraft `1.21.11` requires Java 21).
   - **Memory Allocation (`3GB – 8GB` Safe Guide)**:
     - **`3 GB` device RAM** → allocate **`1280 MB – 1408 MB`** *(AZYRON auto-activates Potato Mode + `6/5` chunk clamp)*
     - **`4 GB` device RAM** → allocate **`1600 MB – 2048 MB`** *(AZYRON auto-activates Smooth FPS Mode + `7/6` chunk clamp)*
     - **`6 GB` device RAM** → allocate **`2560 MB – 3072 MB`** *(AZYRON auto-activates Balanced Mode)*
     - **`8 GB+` device RAM** → allocate **`3072 MB – 4096 MB`** *(avoid allocating >50% of total phone RAM so Android's Low Memory Killer keeps background services stable)*.
2. Go to **Settings → Video & Renderer**:
   - Select **LTW (LightThatWay / GL4ES 1.21+)** or **MobileGlues** (recommended for Minecraft `1.21.11` + Sodium `0.8.14`), or **Holy GL4ES** if using Vanilla renderer without Sodium on older Adreno/Mali GPUs.
   - Set **Resolution Scaler** to **`75% – 85%`** on `3GB–4GB` phones for higher FPS with crisp HUD text.

---

### Method A: One-Click Modpack Import (`.mrpack`) — *Recommended*
1. Copy `releases/Azyron-Modpack-1.0.2.mrpack` to your phone's `Downloads` folder.
2. Open **PojavLauncher** → tap the **Profile Selector** → **Create New Profile** → **Modpack** (or **Import Modrinth `.mrpack`**).
3. Select `Azyron-Modpack-1.0.2.mrpack`.
4. PojavLauncher will automatically install:
   - Minecraft **`1.21.11`** + **Fabric Loader `0.20.1`**
   - **`Azyron-Client-1.0.2.jar`**
   - All **8 universal Android/PC verified `1.21.11` mods** (**Fabric API `0.141.6+1.21.11`**, **Sodium `0.8.14+mc1.21.11`**, **Lithium `0.21.4+mc1.21.11`**, **FerriteCore `8.2.0-fabric`**, **EntityCulling `1.11.3-mc1.21.11`**, **More Culling `1.21.11-1.6.2`**, **Cloth Config `21.11.153-fabric`**, **Dynamic FPS `3.11.6+minecraft-1.21.11-fabric`**)
   - Mobile GLES-safe `config/` overrides (`use_no_error_g_l_context: false`, `invisible.frame_rate_target: 1`, `battery_tracker.enabled: false`).
   - *(Note: `ImmediatelyFast` is intentionally omitted on Android/PojavLauncher because mobile GL translation layers do not support its desktop buffer/framebuffer extensions.)*
5. Tap **Play**.

---

### Method B: Manual Installation on PojavLauncher
1. Install **Fabric Loader `0.20.1`** for **Minecraft `1.21.11`** using the official Universal Fabric Installer JAR.
2. Copy `releases/Azyron-Client-1.0.2.jar` into `.minecraft/mods/` *(remove any older `Azyron-Client-1.0*.jar` so there is no duplicate mod ID)*.
3. Add **Fabric API `0.141.6+1.21.11`** (required) and the verified universal `1.21.11` optimization mods listed in [`DEPENDENCIES.md`](DEPENDENCIES.md) into `.minecraft/mods/`.
4. Select **`fabric-loader-0.20.1-1.21.11`** and tap **Play**.

---

### Built-In Android & PojavLauncher Compatibility Protections in `v1.0.2`
- **Missing `libflite.so` Narrator Protection**: Android/PojavLauncher does not ship desktop Linux `libflite.so` TTS libraries. AZYRON `v1.0.2` detects missing Flite libraries automatically and falls back cleanly to `Narrator.EMPTY` with zero native crashes or error dialogs.
- **`fullscreen:off` Option Normalization**: PojavLauncher and legacy launchers sometimes write `fullscreen:off` into `options.txt`, which causes vanilla `1.21.11` to log boolean parse errors. AZYRON automatically normalizes `"off"` → `"false"` and `"on"` → `"true"` before option parsing.
- **Offline / Local Account Realms Silence**: When playing on a PojavLauncher Local/Offline profile (`accessToken: "0"`), AZYRON skips unnecessary background `pc.realms.minecraft.net` checks cleanly without modifying or bypassing online Microsoft authentication.

---

## Part 2: Desktop Installation (Prism Launcher / Modrinth App / Official Launcher)

1. In **Prism Launcher** or **Modrinth App**, click **Add Instance → Import from zip / Modpack** and select `releases/Azyron-Modpack-1.0.2.mrpack`.
2. For the **Official Minecraft Launcher**, install **Fabric Loader `0.20.1`** for **Minecraft `1.21.11`**, place `Azyron-Client-1.0.2.jar` into `.minecraft/mods/`, run `download-dependencies.sh` / `.ps1` *(pass `--desktop-immediatelyfast` or `-DesktopImmediatelyFast` on PC if desired)*, and launch with **Java 21**.
