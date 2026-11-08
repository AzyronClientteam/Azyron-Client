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
   - **Memory Allocation**:
     - `4 GB` device RAM → allocate **`1400 MB – 1600 MB`**
     - `6 GB` device RAM → allocate **`2048 MB`**
     - `8 GB+` device RAM → allocate **`2560 MB – 3072 MB`** *(avoid allocating >50% of total phone RAM so Android does not kill the background process)*.
2. Go to **Settings → Video & Renderer**:
   - Select **LTW (LightThatWay / GL4ES 1.21+)** or **MobileGlues** (recommended for Minecraft `1.21.11` + Sodium `0.8.14`), or **Holy GL4ES** if using Vanilla renderer without Sodium on older Adreno/Mali GPUs.
   - Set **Resolution Scaler** to **`75% – 85%`** on mid-range phones for a huge extra FPS boost with crisp HUD text.

---

### Method A: One-Click Modpack Import (`.mrpack`) — *Recommended*
1. Copy `releases/Azyron-Modpack-1.0.2.mrpack` to your phone's `Downloads` folder.
2. Open **PojavLauncher** → tap the **Profile Selector** → **Create New Profile** → **Modpack** (or **Import Modrinth `.mrpack`**).
3. Select `Azyron-Modpack-1.0.2.mrpack`.
4. PojavLauncher will automatically install:
   - Minecraft **`1.21.11`** + **Fabric Loader `0.20.1`**
   - **`Azyron-Client-1.0.2.jar`**
   - All 9 verified `1.21.11` optimization mods (**Fabric API `0.141.6+1.21.11`**, **Sodium `0.8.14+mc1.21.11`**, **Lithium `0.21.4+mc1.21.11`**, **FerriteCore `8.2.0-fabric`**, **EntityCulling `1.11.3-mc1.21.11`**, **More Culling `1.21.11-1.6.2`**, **Cloth Config `21.11.153+fabric`**, **Dynamic FPS `3.11.6+minecraft-1.21.11-fabric`**, **ImmediatelyFast `1.14.3+1.21.11`**)
   - Mobile GLES-safe `config/` overrides (`use_no_error_g_l_context: false`, `font_atlas_resizing: false`, `fast_buffer_upload: false`).
5. Tap **Play**.

---

### Method B: Manual Installation on PojavLauncher
1. Install **Fabric Loader `0.20.1`** for **Minecraft `1.21.11`** using the official Universal Fabric Installer JAR.
2. Copy `releases/Azyron-Client-1.0.2.jar` into `.minecraft/mods/` *(remove any older `Azyron-Client-1.0*.jar` so there is no duplicate mod ID)*.
3. Add **Fabric API `0.141.6+1.21.11`** (required) and the verified `1.21.11` optimization mods listed in [`DEPENDENCIES.md`](DEPENDENCIES.md) into `.minecraft/mods/`.
4. Select **`fabric-loader-0.20.1-1.21.11`** and tap **Play**.

---

### Note on Realms Authentication Logs in PojavLauncher
If you launch Minecraft `1.21.11` using a **PojavLauncher Local / Offline Account** (or an expired Microsoft session), vanilla Minecraft's background `RealmsClient` startup check queries `pc.realms.minecraft.net` expecting a signed 3-part Microsoft/Xbox Minecraft Services JWT (`header.payload.signature`). Because offline accounts supply a placeholder token (`"0"`), vanilla Minecraft logs a non-fatal Realms JWT authentication error and disables Realms notifications. Singleplayer and compatible servers continue to work normally; logging in with a genuine Microsoft account that owns Minecraft: Java Edition provides a valid JWT automatically.

---

## Part 2: Desktop Installation (Prism Launcher / Modrinth App / Official Launcher)

1. In **Prism Launcher** or **Modrinth App**, click **Add Instance → Import from zip / Modpack** and select `releases/Azyron-Modpack-1.0.2.mrpack`.
2. For the **Official Minecraft Launcher**, install **Fabric Loader `0.20.1`** for **Minecraft `1.21.11`**, place `Azyron-Client-1.0.2.jar` into `.minecraft/mods/`, run `download-dependencies.sh` / `.ps1`, and launch with **Java 21**.
