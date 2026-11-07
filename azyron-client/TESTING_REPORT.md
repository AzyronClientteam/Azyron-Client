# AZYRON CLIENT — Verification & Testing Report

**Client:** AZYRON CLIENT `v1.0.0+mc1.21.11`  
**Author:** JIGNESH GAMING  
**Target Environment:** Minecraft Java Edition **`1.21.11`** • Fabric Loader `0.20.1` (`>=0.17.3`) • Temurin OpenJDK `21.0.8+9`

---

## 1. Build & Compilation Verification

| Check | Result | Details |
| :--- | :---: | :--- |
| **Java 21 Source Compilation (`-21 -parameters -g`)** | **PASS** | Compiled all 38 Java 21 source files into 48 `.class` files with **0 errors and 0 warnings** |
| **Official `1.21.11.tiny` Intermediary Bytecode Audit** | **PASS** | Every `net/minecraft/*` class, method, field, enum constant, record component (`Click.comp_4798/comp_4799`, `MouseInput.comp_4801`, `RegistryEntry.comp_349`), and `InvokeDynamic` handle verified against official `FabricMC/intermediary` `1.21.11.tiny` (**0 unremapped symbols**) |
| **HotSpot JVM Strict Bytecode Verifier (`-Xverify:all`)** | **PASS** | Loaded and linked all **48/48** `intermediary`-remapped classes in `azyron-client-1.0.0+mc1.21.11.jar` under OpenJDK `21.0.8` with `-Xverify:all` |
| **JAR Manifest & Metadata (`fabric.mod.json`)** | **PASS** | `Fabric-Loom-Mixin-Remap-Type: mixin`, `Fabric-Mapping-Namespace: intermediary`, `"minecraft": "1.21.11"`, `"java": ">=21"` |
| **PNG Branding Assets (`icon.png` & `logo.png`)** | **PASS** | Both verified as valid `128x128` 8-bit RGBA PNG textures (`89 50 4E 47` signature + visual inspection) |

---

## 2. Mixin & `azyron-client.refmap.json` Target Verification (`1.21.11`)

All **7** client Mixin classes and all **10** `@Inject` method descriptors were verified against Minecraft `1.21.11` Intermediary mappings:

| Mixin Class | Target Class (`1.21.11`) | Verified `refmap.json` Target Descriptor | Status |
| :--- | :--- | :--- | :---: |
| `GameMenuScreenMixin` | `GameMenuScreen` (`class_433`) | `Lnet/minecraft/class_433;method_20543()V` (`initWidgets`) | **PASS** |
| `GameRendererMixin` | `GameRenderer` (`class_757`) | `Lnet/minecraft/class_757;method_3196(Lnet/minecraft/class_4184;FZ)F` (`getFov`) | **PASS** |
| `InGameHudMixin` | `InGameHud` (`class_329`) | `method_1736` (`renderCrosshair`), `method_1765` (`renderStatusEffectOverlay`), `method_1753` (`render`) | **PASS** |
| `MinecraftClientMixin` | `MinecraftClient` (`class_310`) | `Lnet/minecraft/class_310;method_1507(Lnet/minecraft/class_437;)V` (`setScreen`), `method_24287()Ljava/lang/String;` (`getWindowTitle`) | **PASS** |
| `MouseMixin` | `Mouse` (`class_312`) | `Lnet/minecraft/class_312;method_1601(JLnet/minecraft/class_11910;I)V` (`onMouseButton`), `method_1598(JDD)V` (`onMouseScroll`) | **PASS** |
| `ParticleManagerMixin` | `ParticleManager` (`class_702`) | `Lnet/minecraft/class_702;method_3056(Lnet/minecraft/class_2394;DDDDDD)Lnet/minecraft/class_703;` (`addParticle`) | **PASS** |
| `SplashOverlayMixin` | `SplashOverlay` (`class_425`) | `Lnet/minecraft/class_425;method_25394(Lnet/minecraft/class_332;IIF)V` (`render` + `@Shadow field_18217, field_17770, field_17771`) | **PASS** |

---

## 3. Automated Functional & UI Test Suite (`AzyronClientTestSuite`)

Executed on OpenJDK `21.0.8+9` (`8/8` test groups passed, `0` failures):

1. **`[PASS]` Production JAR Manifest, Metadata, Mixins & PNG Branding Integrity**
   - Verified JAR structure, `MANIFEST.MF`, `fabric.mod.json` `1.21.11` version lock, all 7 Mixin `.class` files + `refmap` entries, and `128x128` PNG headers.
2. **`[PASS]` Client Bootstrap & All 11 Modules Registration**
   - Verified headless initialization, registration of all 11 modules (`fps_counter`, `cps_counter`, `ping_display`, `coordinates_hud`, `keystrokes_hud`, `armor_hud`, `potion_hud`, `toggle_sprint`, `zoom`, `custom_crosshair`, `memory_hud`), and live search/category filtering.
3. **`[PASS]` Settings System (`BooleanSetting`, `NumberSetting`, `ModeSetting`)**
   - Verified boolean toggling, numeric min/max clamping + step quantization + normalized slider math, and cyclic mode switching.
4. **`[PASS]` JSON Config Save & Lossless Round-Trip Reload**
   - Verified atomic `.tmp` write and reload of `config/azyron-client.json` across HUD anchors, scales, module toggles, performance profiles, and cosmetics.
5. **`[PASS]` Performance Profiles (`Potato Mode`, `Balanced Mode`, `Smooth FPS Mode`) & Particle Culler**
   - Verified `GameOptions` application (`FAST`/`FANCY`, `MINIMAL`/`DECREASED`, chunk view/sim distances `6/5`, `10/8`, `8/6`, `120`/`260` FPS targets), distant particle culling (`>18m` rejected in Potato Mode), and strict `40/tick` particle cap enforcement.
6. **`[PASS]` Frame-Time 1% Low Tracker & Memory Optimizer Telemetry**
   - Verified zero-allocation frame-time ring buffer, 99th-percentile 1% low FPS computation, frame-spike detector, and JVM heap/allocation telemetry.
7. **`[PASS]` CPS Tracker, Compass Facing Math, Smooth Zoom & Toggle Sprint**
   - Verified LMB/RMB rolling 1-second click tracking, 8-octant compass math (`N`, `NE`, `E`, `SE`, `S`, `SW`, `W`, `NW`), smooth FOV interpolation + scroll magnification, and Toggle Sprint state transitions.
8. **`[PASS]` HUD Rendering, Loading Screen, Main Menu, Right-Shift Menu, HUD Editor & Cosmetics UI**
   - Simulated full `DrawContext` rendering and `1.21.11` `Click` / `KeyInput` / `CharInput` interaction across:
     - `AzyronLoadingRenderer` (verified `"Welcome to AZYRON CLIENT"` output)
     - `AzyronMainMenuScreen` (verified `"AZYRON CLIENT"`, `"Singleplayer"`, `"Multiplayer"`, `"Cosmetics"`, `"Mods"`, `"Settings"`, `"Quit Game"`, and `"Discord  •  Join JIGNESH GAMING Community"`, plus navigation into `AzyronModsScreen`)
     - `AzyronModsScreen` (module grid, Mod Settings drawer, FPS Profiles dashboard, Escape navigation)
     - `HudEditorScreen` (drag-and-drop reposition, magnetic center snap, scroll-wheel scaling)
     - `AzyronCosmeticsScreen` & `CrosshairModule` (all 5 crosshair geometries).

---

## 4.Minecraft `1.21.11` & PojavLauncher Specific Safeguards Verified

- **No ProGuard-Obfuscated Vanilla Enum `.values()` Calls**: Verified zero `.values()` calls on Minecraft enums (`EquipmentSlot`, `GraphicsMode`, `ParticlesMode`, `CloudRenderMode`), preventing `NoSuchMethodError` on `1.21.11`.
- **1.21.11 `DrawContext` Explicit ARGB Alpha & `Matrix3x2fStack`**: All colors carry explicit alpha channels (`0xFF...` / `AzyronTheme.withAlpha(...)`), and 2D HUD scaling uses JOML `Matrix3x2fStack` (`pushMatrix()` / `popMatrix()`) + `RenderPipelines.GUI_TEXTURED`.
- **1.21.11 `Click` / `KeyInput` / `CharInput` GUI API**: All custom `Screen` classes implement the exact `1.21.11` record-based GUI input methods.
- **PojavLauncher Touch Ergonomics**: Normalized `[0..1]` HUD coordinates adapt cleanly across phone aspect ratios, `CpsTracker` supports both GLFW mouse events and Pojav touch keybind edges, and `GameMenuScreenMixin` provides touch-accessible `✦ AZYRON Client` and `✦ HUD Editor` buttons directly on the Pause Menu.
