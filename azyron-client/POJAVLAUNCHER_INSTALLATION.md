# AZYRON CLIENT — PojavLauncher & Desktop Installation Guide

**Client:** AZYRON CLIENT `v1.0.0` (by **JIGNESH GAMING**)  
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

### Method A: One-Click Modpack Import (`.mrpack`) — *Fastest*
1. Copy `releases/Azyron-Modpack-1.0.mrpack` to your phone's `Downloads` folder.
2. Open **PojavLauncher** → tap the **Profile Selector** → **Create New Profile** → **Modpack** (or **Import Modrinth `.mrpack`**).
3. Select `Azyron-Modpack-1.0.mrpack`.
4. PojavLauncher will automatically install:
   - Minecraft **`1.21.11`** + **Fabric Loader `0.20.1`**
   - **`Azyron-Client-1.0.jar`**
   - All 9 verified `1.21.11` optimization mods (**Fabric API**, **Sodium**, **Lithium**, **FerriteCore**, **EntityCulling**, **MoreCulling**, **Cloth Config**, **Dynamic FPS**, **ImmediatelyFast**)
   - Pre-tuned `config/` files for maximum mobile FPS.
5. Tap **Play** — you will see **"Welcome to AZYRON CLIENT"** on the custom loading screen followed by the dark purple & electric-blue **AZYRON CLIENT** main menu!

---

### Method B: Manual Installation on PojavLauncher
1. **Install Vanilla 1.21.11 Once**:
   - In PojavLauncher, create a profile for **Minecraft `1.21.11`** (Release) and launch once to download game assets, or go straight to step 2.
2. **Install Fabric Loader for 1.21.11**:
   - Download the official **Fabric Installer (Universal `.jar`)** from `https://fabricmc.net/use/installer/`.
   - In PojavLauncher, tap **Install `.jar`**, select `fabric-installer.jar`, pick Minecraft Version **`1.21.11`** and Loader Version **`0.20.1`**, and tap **Install**.
3. **Extract the AZYRON PojavLauncher Pack**:
   - Locate your PojavLauncher `.minecraft` directory (or custom profile game directory):
     `Android/data/net.kdt.pojavlaunch/files/.minecraft/`
   - Extract `releases/AZYRON-CLIENT-PojavLauncher-1.21.11-Pack.zip` directly into `.minecraft/` so that:
     - `.minecraft/mods/azyron-client-1.0.0+mc1.21.11.jar` is present
     - `.minecraft/config/azyron-client.json` (and companion optimization configs) are present.
4. **Add Required `1.21.11` Dependencies into `.minecraft/mods/`**:
   - Download **Fabric API `0.141.6+1.21.11`** (required) and the recommended `1.21.11` optimization mods listed in [`DEPENDENCIES.md`](DEPENDENCIES.md) (or run `bash download-dependencies.sh` in Termux / PC and copy the `mods/` folder over).
5. **Select Profile & Launch**:
   - Select **`fabric-loader-0.20.1-1.21.11`** in PojavLauncher and tap **Play**!

---

### Touch Controls & Keybindings on PojavLauncher
- **Zero-Keybind Touch Support Built In**: Even without adding custom on-screen keys, you can open AZYRON CLIENT anytime:
  - On the **Main Menu**: Tap **`Mods`** or **`Cosmetics`** directly, or tap the top-right **`Profile`** pill to switch between **Potato Mode**, **Balanced Mode**, and **Smooth FPS Mode**.
  - **In-Game**: Tap **Pause (`ESC`)** and tap **`✦ AZYRON Client`** or **`✦ HUD Editor`** in the top-left corner!
- **Optional Custom Touch Buttons** (PojavLauncher Custom Controls):
  - Add a custom button mapped to **`Right Shift`** (`KEY_RSHIFT`) labeled **`AZYRON`** for instant 1-tap menu access.
  - Add a custom button mapped to **`C`** (Toggleable = ON or Hold) labeled **`ZOOM`** for optical zoom.

---

## Part 2: Desktop Installation (Prism Launcher / Modrinth App / Official Launcher)

### Prism Launcher / Modrinth App / ATLauncher
1. Click **Add Instance → Import from zip / Modpack**.
2. Select `releases/AZYRON-CLIENT-1.0.0+mc1.21.11.mrpack`.
3. Click **Launch**.

### Official Minecraft Launcher
1. Install **Fabric Loader `0.20.1`** for **Minecraft `1.21.11`** using the official Fabric Installer.
2. Extract `releases/AZYRON-CLIENT-PojavLauncher-1.21.11-Pack.zip` into your `.minecraft` folder (`%APPDATA%\.minecraft` on Windows, `~/.minecraft` on Linux, `~/Library/Application Support/minecraft` on macOS).
3. Run `./download-dependencies.sh` (Linux/macOS) or `.\download-dependencies.ps1` (Windows PowerShell) inside `.minecraft` to download and SHA-512 verify **Fabric API** + all `1.21.11` optimization mods into `.minecraft/mods/`.
4. Launch **`fabric-loader-0.20.1-1.21.11`** with **Java 21**.
