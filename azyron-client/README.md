# AZYRON CLIENT v1.0.3

> **Verification scope (draft):** The two suites passed 26/26 checks each in isolated JVMs with generated Minecraft/Fabric stubs, **not actual Minecraft/Fabric installations**. Stress/soak checks simulate work, not elapsed gameplay. The 74-class mapping audit checks class/member-name presence; it does not validate full member owner/descriptors or Mixin application. Real Desktop and Android/PojavLauncher launch, rendering, world-load and multiplayer testing remain pending. These results do not establish crash-free runtime compatibility.


> **AZYRON CLIENT – PERFORMANCE. SMOOTHNESS. STYLE.**
> **Client:** **AZYRON CLIENT v1.0.3** • **Developer:** **JIGNESH GAMING**
> **Native downloads:** separate Minecraft `1.20.1` and `1.21.11` Fabric JAR/MRPack pairs.
> **Compatibility:** **Minecraft `1.21.11` (Fabric)** & **Minecraft `1.20.1` (Fabric)** — use the matching native downloads.
> **Supported platforms:** Desktop (Windows / Linux / macOS) and Android through PojavLauncher.

---

## Overview

**AZYRON CLIENT v1.0.3** by **JIGNESH GAMING** is a Fabric client distributed as separate native JAR and Modrinth `.mrpack` pairs for Minecraft `1.20.1` and `1.21.11`. The standard launcher selects the running game version; install only its matching pair. Version-specific builds use their target game APIs. The configured low-memory profile is a target, not a measured FPS guarantee.

---

## Standard Launcher Downloads

Use the JAR and Modrinth pack for the same Minecraft version. Each native JAR and `.mrpack` targets exactly one game version; there is no universal client binary.

| Minecraft target | Native client JAR | Matching Modrinth pack |
| :--- | :--- | :--- |
| **1.20.1 Fabric** | `Azyron-Client-1.0.3-mc1.20.1.jar` | `Azyron-Modpack-1.0.3-mc1.20.1.mrpack` |
| **1.21.11 Fabric** | `Azyron-Client-1.0.3-mc1.21.11.jar` | `Azyron-Modpack-1.0.3-mc1.21.11.mrpack` |

The draft still contains native pairs from the prior verified build. Current source changes have now passed both native builds and package checks in [CI run 42948465997](https://github.com/AzyronClientteam/Azyron-Client/actions/runs/42948465997); its candidates have not yet replaced the attached assets. See [the native build report](COMPATIBILITY_1201_BUILD_REPORT.md).

---


## Supported Minecraft Versions

| Minecraft Version | Mod Loader | Java Runtime | Companion Optimization Stack |
| :--- | :--- | :--- | :--- |
| **Minecraft `1.21.11` (Fabric)** | Fabric Loader `0.20.1` (`>=0.17.3`) | **Java 21+** (`Internal-21` on Pojav) | Fabric API `0.141.6+1.21.11` • Sodium `0.8.14` • Lithium `0.21.4` • FerriteCore `8.2.0` • EntityCulling `1.11.3` • MoreCulling `1.6.2` • DynamicFPS `3.11.6` |
| **Minecraft `1.20.1` (Fabric)** | Fabric Loader `0.14.22+` | **Java 17+** (`Internal-17` or `Internal-21`) | Fabric API `0.88.1+1.20.1` • Sodium `0.5.3` • Lithium `0.11.2` • FerriteCore `6.0.0` • EntityCulling `1.6.2` • MoreCulling `0.19.0` • DynamicFPS `3.2.1` |

---

## Core Client Features

### 1. Client Menu (`Right Shift` + Pause Menu Button) & HUD Editor (`H`)
- Press **`Right Shift`** (or tap **`AZYRON CLIENT v1.0.3`** in the Escape Pause Menu) to open the existing AZYRON client menu.
- Filter modules by category (**All Mods**, **HUD Widgets**, **Gameplay**, **Visuals**, **FPS Profiles**, **Client Options**) or search in real time.
- Click **`HUD Editor`** (or press **`H`**) to drag and reposition any HUD widget with magnetic center snap guides, scroll-wheel scaling (`0.50x – 2.00x`), grid snap toggle, and one-click layout reset.

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
- **Potato Mode**: low-overhead settings — `FAST` graphics, `MINIMAL` particles, clouds `OFF`, up to `6` view / `5` simulation chunks, `50%` entity distance, shadows/AO/vignette `OFF`, and a `40` particles/tick limit.
- **Balanced Mode**: `FANCY` graphics, `DECREASED` particles, `FAST` clouds, up to `10/8` view/simulation chunks, and a `180` particles/tick limit.
- **Smooth FPS Mode**: `FAST` graphics, up to `8/6` view/simulation chunks, a configured `120 FPS` cap, and dynamic particle throttling when frame times rise.

On the 3GB tier, the client targets at most `6/5` chunks and a `60 FPS` cap. These are configuration targets, not measured or guaranteed frame rates; no 3GB device benchmark is available.

---

## Default Keybindings

| Key | Action |
| :--- | :--- |
| **`Right Shift`** | Open **AZYRON CLIENT v1.0.3** menu (also accessible from the Escape Pause Menu) |
| **`H`** | Open **Interactive HUD Editor** (also accessible from the Pause Menu) |
| **`C`** *(Hold)* | **Smooth Optic Zoom** (scroll wheel adjusts magnification while held) |
| **`F8`** | Cycle **Performance Profile** (`Potato Mode` → `Balanced Mode` → `Smooth FPS Mode`) |
| **`I`** | Toggle **Toggle Sprint** ON / OFF |

---

## Building Separate Native Pairs

Run each target independently from `azyron-client/`. Preserve/copy the first JAR before `clean` removes it for the second build:

```bash
./gradlew clean build -Pminecraft_version=1.20.1
python3 scripts/build-modpack.py --mc 1.20.1
cp build/libs/Azyron-Client-1.0.3-mc1.20.1.jar releases/

./gradlew clean build -Pminecraft_version=1.21.11
python3 scripts/build-modpack.py --mc 1.21.11
cp build/libs/Azyron-Client-1.0.3-mc1.21.11.jar releases/
```

These are build/package commands, not gameplay tests. Run separate Fabric launcher instances and install only the JAR and `.mrpack` pair matching that instance. For dependency download instructions, see [DEPENDENCIES.md](DEPENDENCIES.md).

---

## Documentation Index

- **[PojavLauncher & Desktop Installation Guide (`1.21.11` & `1.20.1`)](POJAVLAUNCHER_INSTALLATION.md)**
- **[Dependencies & Optimization Stacks (`1.21.11` & `1.20.1`)](DEPENDENCIES.md)**
- **[Release Notes (`v1.0.3`)](RELEASE_NOTES_v1.0.3.md)**
- **[Validation Status & Testing Scope](TESTING_REPORT.md)**
