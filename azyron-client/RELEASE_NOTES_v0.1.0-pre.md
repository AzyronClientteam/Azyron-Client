> **AZYRON CLIENT — PERFORMANCE. SMOOTHNESS. STYLE.**  
> Welcome to the official **v0.1.0 Pre-Release** of **AZYRON CLIENT**, crafted by **JIGNESH GAMING** exclusively for **Minecraft Java Edition `1.21.11` (Fabric)**. Thank you for testing our early-access foundation build engineered for ultra-clean visuals, rock-solid frame pacing, and effortless compatibility across desktop launchers and **PojavLauncher**.

| Specification | Details |
| :--- | :--- |
| **Release Version** | `AZYRON CLIENT v0.1.0 PRE-RELEASE` (`v0.1.0-pre`) |
| **Release Channel** | `Pre-release` *(Early Testing & Performance Validation)* |
| **Minecraft Version** | **Java Edition `1.21.11` — Fabric** *(Strictly `1.21.11`)* |
| **Fabric Requirements** | Fabric Loader `0.20.1` *(min `>=0.17.3`)* + Fabric API `0.141.6+1.21.11` |
| **Java Runtime** | **Java 21 LTS** *(64-bit OpenJDK 21)* |
| **Developer** | **JIGNESH GAMING** |

---

## Client Introduction

**AZYRON CLIENT** is a modern, lightweight, fair-play **Fabric client and optimization suite** built from the ground up for **Minecraft Java Edition `1.21.11`**. Designed for competitive multiplayer, survival worlds, and low-memory mobile/ARM64 devices running **PojavLauncher**, AZYRON combines a custom dark-themed client interface, a drag-and-drop HUD editor, and a real-time frame-pacing & particle-culling engine — with **zero** unfair gameplay modifications or bloatware.

---

## FPS Optimization Features

- **Real-Time Performance Engine (`PerformanceEngine` & `FrameTimeTracker`)** — Tracks rolling frame-time percentiles (`1%` lows and micro-stutter spikes) and coordinates render settings without stalling the main render thread.
- **Distance & Budget Particle Throttling (`ParticleManagerMixin`)** — Enforces per-tick particle caps and distance cutoffs during heavy combat, explosions, and crowded server lobbies.
- **Smart Heap & GC Hygiene (`MemoryOptimizer`)** — Low-allocation tick/HUD rendering loop designed to minimize garbage-collection pauses on `1.5 GB – 4 GB` RAM allocations.
- **Companion Optimization Config Seeding** — Automatically seeds tuned defaults for the `1.21.11` Fabric optimization stack (**Sodium `0.8.14`**, **Lithium `0.21.4`**, **FerriteCore `8.2.0`**, **EntityCulling `1.11.3`**, **More Culling `1.6.2`**, **ImmediatelyFast `1.14.3`**, and **Dynamic FPS `3.11.6`**) when installed via the Modpack.
- **11 Built-In Modular HUD & Utility Widgets** — `FPS Counter`, `CPS Counter`, `Ping Display`, `Coordinates & Direction`, `Keystrokes`, `Armor Status HUD`, `Potion Effects HUD`, `Memory Usage HUD`, `Custom Crosshair`, `ToggleSprint`, and `Smooth Cinematic/PvP Zoom`, plus custom `AZYRON` Main Menu, Splash Loading Renderer, Cosmetics Manager, and Pause Menu integration.

---

## Performance Profiles

Switch instantly in-game with **`F8`** (or inside the **`Right Shift`** AZYRON Menu) between three engineered performance presets:

| Profile | Graphics & Clouds | Render / Sim Distance | Particle Budget | Entity Scale & Shadows | Target Cap | Best For |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Potato Mode** (`POTATO`) | `Fast` · Clouds `Off` | `6` / `5` chunks | `Minimal` (`<=40/tick`, `18m`) | `50%` · Shadows `Off` | `260 FPS` | Entry-level laptops, integrated GPUs & **PojavLauncher** (`1.5–2 GB` RAM) |
| **Balanced Mode** (`BALANCED`) | `Fancy` · Clouds `Fast` | `10` / `8` chunks | `Decreased` (`<=180/tick`, `42m`) | `75%` · Shadows `On` | `260 FPS` | Everyday survival & multiplayer with crisp visuals and high FPS |
| **Smooth FPS Mode** (`SMOOTH_FPS`) | `Fast` · Clouds `Fast` | `8` / `6` chunks | `Decreased` (`<=100/tick`, `28m`) | `75%` · Shadows `Off` | `120 FPS` | Competitive PvP, consistent `1%` lows & thermal stability on mobile |

---

## Supported Minecraft Version & Platforms

- **Supported Game Version:** **Minecraft: Java Edition `1.21.11` ONLY** *(will not load on `1.21.4` or older `1.21.x` builds due to `1.21.11` intermediary mappings)*
- **Mod Loader:** **Fabric Loader `0.20.1`** (`>=0.17.3`) with **Fabric API `0.141.6+1.21.11`**
- **Java Runtime:** **Java `21`** (`major class version 65`)
- **Supported Launchers:**
  - **Desktop (Windows / macOS / Linux):** Modrinth App, Prism Launcher, MultiMC, Official Minecraft Launcher
  - **Mobile (Android / iOS ARM64):** **PojavLauncher** *(Java 21 runtime selected)*

---

## Installation Instructions

### Option A — Full Modpack (`Azyron-Modpack-1.0.mrpack`) *(Recommended for Maximum FPS)*
1. Open **Modrinth App** or **Prism Launcher**.
2. Click **Add Instance** -> **Import from file** and select **`Azyron-Modpack-1.0.mrpack`**.
3. The launcher will automatically fetch Minecraft `1.21.11`, Fabric Loader `0.20.1`, all **9 verified optimization dependencies**, and install AZYRON CLIENT with pre-tuned configs.
4. Launch the instance and press **`Right Shift`** in-game to customize modules.

### Option B — Standalone Mod JAR (`Azyron-Client-1.0.1.jar`) *(PojavLauncher & Official Launcher)*
1. Install **Minecraft `1.21.11`** and **Fabric Loader `0.20.1`** in your launcher and select a **Java 21** runtime.
2. Download **`Azyron-Client-1.0.1.jar`** from the **Assets** section below and download **`fabric-api-0.141.6+1.21.11.jar`**.
3. Copy both `.jar` files into your `.minecraft/mods/` directory *(recommended RAM: `1536 MB – 2048 MB` on PojavLauncher, `2048 MB – 4096 MB` on Desktop)*.
4. Launch the `fabric-loader-0.20.1-1.21.11` profile.

### Default In-Game Controls
| Keybind | Action |
| :--- | :--- |
| **`Right Shift`** | Open **AZYRON CLIENT** Modules & Settings Menu |
| **`H`** | Open Drag-and-Drop **HUD Layout Editor** |
| **`C`** *(Hold)* | Smooth **Zoom** |
| **`F8`** | Cycle **Performance Profile** (`Potato` -> `Balanced` -> `Smooth FPS`) |
| **`I`** | Toggle **Auto-Sprint** (`ToggleSprint`) |

---

## Known Issues (`v0.1.0-pre`)

- **Pre-Release Build:** This is an early `v0.1.0-pre` validation release; configuration schema keys in `config/azyron-client.json` may evolve prior to `v0.1.0` stable.
- **PojavLauncher Legacy Renderers:** On older Android GPUs using legacy `GL4ES` renderers with `<1.5 GB` RAM allocated, cycling performance profiles inside densely populated worlds may briefly pause for `1–2s` while chunk meshes reload *(switching profiles on the Main Menu or using ANGLE/Zink/LTW with Java 21 is recommended)*.
- **External Shader Packs:** Heavy third-party ray-traced shader packs are not bundled and may override `Potato Mode` / `Smooth FPS Mode` frame-rate targets.
- **Ultra-Wide Aspect Ratios:** HUD snap-grid guides on `21:9` and `32:9` displays are scheduled for further polish in the next milestone.

---

## Changelog (`v0.1.0-pre`)

- **Added:** Initial `1.21.11` Fabric client core (`AzyronClient`), JSON persistence (`AzyronConfig`), and headless test bootstrap (`8/8` automated test suites passing).
- **Added:** Custom branded **AZYRON Main Menu**, **Splash Loading Overlay**, **Mods & Settings Screen**, **Cosmetics Screen**, and **Draggable HUD Editor**.
- **Added:** **11 client modules** (`FPS`, `CPS`, `Ping`, `Coordinates`, `Keystrokes`, `Armor HUD`, `Potion HUD`, `Memory HUD`, `Crosshair`, `ToggleSprint`, `Zoom`).
- **Added:** **3 one-click Performance Profiles** (`Potato Mode`, `Balanced Mode`, `Smooth FPS Mode`) with particle budget culling (`ParticleManagerMixin`) and companion mod config seeding.
- **Packaged:** Verified `Azyron-Client-1.0.1.jar` (`150.47 KB`) and Modrinth v1 modpack `Azyron-Modpack-1.0.mrpack` (`148.02 KB`) pinning `9` SHA-512-verified `1.21.11` Fabric optimization mods.

---

## Artifact Verification (SHA-256)

| Asset Filename | Size | SHA-256 Checksum |
| :--- | :--- | :--- |
| `Azyron-Client-1.0.1.jar` | `154,079 B` (`150 KB`) | `4f4edc88561e55d27a1f027ddbe9b5558c7b0b0c965e55b89beafe70ba9d0834` |
| `Azyron-Modpack-1.0.mrpack` | `151,573 B` (`148 KB`) | `4cb083e369222c032837bf6eee456ccdd820939c3a5623f17a255db7d679b14a` |

---

## Credits

- **Creator & Lead Developer:** **JIGNESH GAMING** — *AZYRON CLIENT Architecture, UI/UX & Performance Engineering*
- **Platform & Optimization Ecosystem:** Special thanks to **Mojang Studios**, the **FabricMC Team**, **CaffeineMC** (*Sodium & Lithium*), **malte0811** (*FerriteCore*), **tr7zw** (*EntityCulling*), **FX-PR0CESS** (*More Culling*), **RK_01** (*ImmediatelyFast*), **juliand665 & LostLuma** (*Dynamic FPS*), **shedaniel** (*Cloth Config*), and the **PojavLauncher Team**.
