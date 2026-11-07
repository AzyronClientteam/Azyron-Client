# AZYRON CLIENT

> **AZYRON CLIENT – PERFORMANCE. SMOOTHNESS. STYLE.**  
> **Developer:** JIGNESH GAMING  
> **Target Version:** Minecraft Java Edition **1.21.11 ONLY** (Fabric Mod Loader • Java 21)  
> **Supported Platforms:** Desktop (Windows / Linux / macOS) & **PojavLauncher** (Android / iOS)

---

## Overview

**AZYRON CLIENT** is a lightweight, high-FPS Minecraft Java Edition **1.21.11** Fabric client inspired by **FastClient** and **Spunky Boost**, built by **JIGNESH GAMING**. Every subsystem is engineered for maximum FPS, low frame-time variance (high 1% lows), reduced RAM pressure, fast startup/resource reloads, and crisp purple & electric-blue visual polish on both desktop PCs and **PojavLauncher** mobile devices.

---

## Pre-Built Release Assets (`azyron-client/releases/`)

| Asset | Description |
| :--- | :--- |
| `releases/Azyron-Client-1.0.jar` | Production Minecraft `1.21.11` Fabric client JAR (`intermediary` remapped + Mixin refmap) |
| `releases/Azyron-Modpack-1.0.mrpack` | Modrinth Modpack containing `Azyron-Client-1.0.jar` + all 9 verified `1.21.11` optimization mods + pre-tuned configs |
| `releases/Azyron-Client-1.0.zip` | Source code archive (`.zip`) |
| `releases/Azyron-Client-1.0.tar.gz` | Source code archive (`.tar.gz`) |
| `releases/SHA256SUMS.txt` | Verified SHA-256 checksums for all release assets |

---

## Core Client Features

### 1. Client Menu (`Right Shift` + Pause Menu Button) & Interactive HUD Editor (`H`)
- Press **`Right Shift`** (or tap **`✦ AZYRON Client`** in the Escape Pause Menu on PojavLauncher) to open the sleek dark-obsidian, purple & electric-blue client menu.
- Filter modules by category (**All Mods**, **HUD Widgets**, **Gameplay**, **Visuals**, **FPS Profiles**, **Client Options**) or search in real time.
- Click **`⚙ Settings`** (or right-click any module card) to open **Mod Settings** with live sliders, mode selectors, toggles, and HUD widget scale controls.
- Click **`✦ HUD Editor`** (or press **`H`**) to drag and reposition any HUD widget with magnetic center snap guides, scroll-wheel scaling (`0.50x – 2.00x`), grid snap toggle, and one-click layout reset.

### 2. Built-In Functional Modules (11 Modules)
| Module | Category | Features |
| :--- | :--- | :--- |
| **FPS Counter** | HUD | Live FPS, **1% Low FPS**, and optional frame-time (`ms`) readout |
| **CPS Counter** | HUD | Real-time Left & Right Clicks Per Second (`LMB | RMB`) + animated burst bar (mouse & Pojav touch compatible) |
| **Ping Display** | HUD | Live server latency (`ms`) with green / amber / crimson connection quality color-coding |
| **Coordinates HUD** | HUD | `X / Y / Z`, 8-way compass + axis indicator (`N (-Z)`, `E (+X)`, etc.), Chunk `XY`, and optional Nether/Overworld `x8` ratio |
| **Keystrokes HUD** | HUD | Smooth press-glow interpolated `W A S D`, `LMB / RMB` with live CPS, and `Spacebar` tiles |
| **Armor HUD** | HUD | Equipped Helmet, Chestplate, Leggings, Boots & Mainhand item icons with durability `%` or points and low-durability alerts |
| **Potion Effects HUD** | HUD | Active status effects sorted by duration, Roman numeral amplifiers, `MM:SS` timers, `<10s` expiring blink, and optional vanilla icon suppression |
| **Toggle Sprint** | HUD / Gameplay | Hands-free automatic sprinting (`I` to toggle) with `[Sprinting (Toggled)]` HUD status badge |
| **Zoom** | Gameplay | Smooth interpolated optical zoom (Hold **`C`**) with mouse scroll-wheel dynamic magnification (`1.5x – 25x`) |
| **Custom Crosshair** | Visual | 5 competitive styles (`Cross`, `Circle-Cross`, `Dot`, `Chevron`, `Box`), dynamic sprint expansion, dark outline & crimson entity-target highlight |
| **Memory HUD** | HUD | Live JVM heap usage (`%` & `MB`) and real-time allocation rate (`MB/s`) |

### 3. Engineered Performance Profiles (`F8` or FPS Profiles Tab)
- **Potato Mode**: Maximum FPS boost — `FAST` graphics, `MINIMAL` particles, clouds `OFF`, `6` chunk view / `5` simulation distance, `50%` entity distance, shadows/AO/vignette `OFF`, strict `40/tick` & `18m` particle culler.
- **Balanced Mode**: Crisp visuals + high FPS — `FANCY` graphics, `DECREASED` particles, `FAST` clouds, `10` chunk view / `8` simulation distance, `75%` entity distance, `180/tick` particle cap.
- **Smooth FPS Mode**: Ultra-consistent frame pacing & 1% lows — `FAST` graphics, `8/6` chunk distances, `120 FPS` frame-pacing target, plus dynamic frame-spike particle throttling whenever frame times exceed `16.5ms`.

### 4. Custom Main Menu, Loading Screen & Cosmetics
- **AZYRON Main Menu**: Renders over Minecraft `1.21.11`'s live 3D Panorama with dark purple & electric-blue glassmorphic styling, **AZYRON CLIENT** crest, **Singleplayer**, **Multiplayer**, **Cosmetics**, **Mods**, **Settings**, **Quit Game**, **Discord Community** card, live FPS/RAM bar, and quick Performance Profile switcher.
- **AZYRON Loading Screen**: Displays **"Welcome to AZYRON CLIENT"** with an animated orbital crest, dual-tone purple-cyan progress bar, loading stage readout, and accelerated fade-out.
- **Cosmetics Locker**: Equip custom cloaks (`Azyron Nebula`, `Electric Cyber`, `Obsidian Crown`, `Crimson Pulse`), animated halos (`Azyron Halo`, `Starlight Orbit`, `Champion Crown`), and client badges (`[AZYRON]`, `[FPS+]`, `[JG VIP]`) with a live animated preview stage.

---

## Default Keybindings

| Key | Action |
| :--- | :--- |
| **`Right Shift`** | Open **AZYRON CLIENT Menu** (also accessible via **`✦ AZYRON Client`** in Escape Pause Menu) |
| **`H`** | Open **Interactive HUD Editor** (also accessible via **`✦ HUD Editor`** in Pause Menu) |
| **`C`** *(Hold)* | **Smooth Optic Zoom** (scroll wheel adjusts magnification while held) |
| **`F8`** | Cycle **Performance Profile** (`Potato Mode` → `Balanced Mode` → `Smooth FPS Mode`) |
| **`I`** | Toggle **Toggle Sprint** ON / OFF |

---

## Building From Source

Requires **JDK 21** (Temurin / OpenJDK 21):

```bash
cd azyron-client
./gradlew clean build
```

Compiled artifacts are output to `build/libs/azyron-client-1.0.0+mc1.21.11.jar`.

---

## Documentation Index

- **[PojavLauncher & Desktop Installation Guide](POJAVLAUNCHER_INSTALLATION.md)**
- **[Minecraft 1.21.11 Dependencies & Optimization Stack](DEPENDENCIES.md)**
- **[Verification & Testing Report](TESTING_REPORT.md)**
