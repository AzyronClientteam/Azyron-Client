> **AZYRON CLIENT — PERFORMANCE. SMOOTHNESS. STYLE.**  
> Welcome to **AZYRON CLIENT `v1.0.2` PRE-RELEASE**, crafted by **JIGNESH GAMING** exclusively for **Minecraft Java Edition `1.21.11` with Fabric**. This `v1.0.2` update resolves the `MouseMixin` startup crash on Minecraft `1.21.11`, eliminates cold-boot `rendertype_text` shader warnings, locks all client metadata to `1.0.2`, and ships a verified 9-mod `1.21.11` performance stack tuned for both desktop launchers and **PojavLauncher**.

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
| **Client Mod JAR (`1.0.2`)** | [**`Azyron-Client-1.0.2.jar`**](https://github.com/AzyronClientteam/Azyron-Client/raw/v0.1.0-pre/azyron-client/releases/Azyron-Client-1.0.2.jar) | Standalone Fabric `1.21.11` client mod (`140,175 B` · `137 KB`) |
| **Modpack (`1.0.2`)** | [**`Azyron-Modpack-1.0.2.mrpack`**](https://github.com/AzyronClientteam/Azyron-Client/raw/v0.1.0-pre/azyron-client/releases/Azyron-Modpack-1.0.2.mrpack) | Modrinth `v1` modpack with `Azyron-Client-1.0.2.jar` + 9 verified `1.21.11` Fabric optimization mods (`138,547 B` · `135 KB`) |
| **Source Code Archive (`.zip`)** | [**`Azyron-Client-Source-v1.0.2.zip`**](https://github.com/AzyronClientteam/Azyron-Client/raw/v0.1.0-pre/azyron-client/releases/Azyron-Client-Source-v1.0.2.zip) | Complete `v1.0.2` Java 21 source code, Gradle build scripts & modpack configs |
| **Source Code Archive (`.tar.gz`)** | [**`Azyron-Client-Source-v1.0.2.tar.gz`**](https://github.com/AzyronClientteam/Azyron-Client/raw/v0.1.0-pre/azyron-client/releases/Azyron-Client-Source-v1.0.2.tar.gz) | Complete `v1.0.2` compressed tarball source archive |

---

## What's New & Fixed in Changelog (`v1.0.2`)

- **Fixed Critical Startup Crash (`MouseMixin` on `1.21.11`):** Updated `MouseMixin` to target Minecraft `1.21.11`'s exact `Mouse.onMouseButton(long, MouseInput, int)` (`Lnet/minecraft/class_312;method_1601(JLnet/minecraft/class_11910;I)V`) descriptor with `@Surrogate` fallback overloads, `require = 0` (`"defaultRequire": 0`), and a non-duplicating `CpsTracker` tick-polling fallback so startup never aborts across desktop or PojavLauncher runtimes.
- **Fixed Version Metadata (`1.0.2`):** Replaced unexpanded literal `"${version}"` placeholders with **`"1.0.2"`** across `fabric.mod.json`, Gradle build configuration, UI footers, and Modrinth modpack manifests.
- **Fixed Cold-Boot Shader Error (`minecraft:core/rendertype_text`):** Added `ShaderLoader` readiness gating (`UiRenderUtils.isCoreTextShaderReady`) and a `RenderPipelines.GUI` 5×7 pixel-matrix boot renderer during initial `SplashOverlay` reload so `VERTEX` and `FRAGMENT` core text shaders are never queried before shader compilation finishes.
- **Verified `1.21.11` Fabric Optimization Stack & PojavLauncher Tuning:** Verified all **9** companion Fabric `1.21.11` mods (**Fabric API `0.141.6+1.21.11`**, **Sodium `0.8.14`**, **Lithium `0.21.4`**, **FerriteCore `8.2.0`**, **EntityCulling `1.11.3`**, **More Culling `1.6.2`**, **Cloth Config `21.11.153`**, **Dynamic FPS `3.11.6`**, and **ImmediatelyFast `1.14.3`**) with zero duplicate JARs and mobile OpenGL ES–safe defaults (`use_no_error_g_l_context: false`, `font_atlas_resizing: false`, `fast_buffer_upload: false`).
- **Removed Legacy Builds:** Removed all `1.0` / `1.0.1` client and modpack artifacts in favor of clean `1.0.2` builds.

---

## Key Client Features (`v1.0.2`)

- **Real-Time Performance Engine (`PerformanceEngine` & `FrameTimeTracker`)** — Tracks rolling frame-time percentiles (`1%` lows and frame spikes) with three instant in-game presets (**Potato Mode**, **Balanced Mode**, **Smooth FPS Mode** via **`F8`**).
- **Distance & Budget Particle Throttling (`ParticleManagerMixin`)** — Enforces per-tick particle caps and distance cutoffs during heavy combat and crowded lobbies.
- **Smart Heap & GC Hygiene (`MemoryOptimizer`)** — Low-allocation HUD & tick pipeline engineered for `1.5 GB – 4 GB` RAM allocations on PojavLauncher and desktop systems.
- **11 Modular HUD & Fair-Play Utility Widgets** — `FPS Counter`, `CPS Counter`, `Ping Display`, `Coordinates & Direction`, `Keystrokes`, `Armor Status HUD`, `Potion Effects HUD`, `Memory Usage HUD`, `Custom Crosshair`, `ToggleSprint`, and `Smooth Zoom`, plus custom `AZYRON` Main Menu, Splash Loading Overlay, Pause Menu button (`✦ AZYRON Client`), Cosmetics Manager, and Drag-and-Drop HUD Editor.

---

## Installation Instructions (`v1.0.2`)

### Option A — Full Modpack (`Azyron-Modpack-1.0.2.mrpack`) *(Recommended for PojavLauncher, Prism Launcher & Modrinth App)*
1. Open **PojavLauncher**, **Modrinth App**, or **Prism Launcher**.
2. Click **Add Instance / Create Profile** → **Import Modpack** and select **`Azyron-Modpack-1.0.2.mrpack`**.
3. The launcher will install **Minecraft Java `1.21.11`**, **Fabric Loader `0.20.1`**, all **9 verified `1.21.11` optimization mods**, and **`Azyron-Client-1.0.2.jar`** with pre-tuned configs.
4. Launch the `1.21.11` Fabric instance with **Java 21** (`1536 MB – 2048 MB` RAM recommended on PojavLauncher; `2048 MB – 4096 MB` on Desktop).

### Option B — Standalone Client JAR (`Azyron-Client-1.0.2.jar`) *(PojavLauncher & Official Minecraft Launcher)*
1. Install **Minecraft Java `1.21.11`** with **Fabric Loader `0.20.1`** and select **Java 21**.
2. Download **`Azyron-Client-1.0.2.jar`** and **`fabric-api-0.141.6+1.21.11.jar`**.
3. Place both `.jar` files inside `.minecraft/mods/` *(delete any older `Azyron-Client-*.jar` files first so only `Azyron-Client-1.0.2.jar` is present)*.
4. Launch `fabric-loader-0.20.1-1.21.11` and press **`Right Shift`** (or Pause → **`✦ AZYRON Client`** on touch devices) to configure modules.

### Default In-Game Controls
| Keybind | Action |
| :--- | :--- |
| **`Right Shift`** | Open **AZYRON CLIENT `v1.0.2`** Modules & Settings Menu |
| **`H`** | Open Drag-and-Drop **HUD Layout Editor** |
| **`C`** *(Hold)* | Smooth **Zoom** |
| **`F8`** | Cycle **Performance Profile** (`Potato` → `Balanced` → `Smooth FPS`) |
| **`I`** | Toggle **Auto-Sprint** (`ToggleSprint`) |

---

## Credits

- **Creator & Lead Developer:** **JIGNESH GAMING** — *AZYRON CLIENT Architecture, UI/UX & Performance Engineering*
- **Platform & Optimization Ecosystem:** Special thanks to **Mojang Studios**, the **FabricMC Team**, **CaffeineMC** (*Sodium & Lithium*), **malte0811** (*FerriteCore*), **tr7zw** (*EntityCulling*), **FX-PR0CESS** (*More Culling*), **RK_01** (*ImmediatelyFast*), **juliand665 & LostLuma** (*Dynamic FPS*), **shedaniel** (*Cloth Config*), and the **PojavLauncher Team**.
