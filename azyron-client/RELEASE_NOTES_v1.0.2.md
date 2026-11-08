# AZYRON CLIENT `v1.0.2` (Minecraft `1.21.11` Fabric — Draft Release Notes)

**Target:** Minecraft Java Edition **`1.21.11`** • Fabric Loader **`0.20.1`** • **Java 21** (Desktop & PojavLauncher)

## Highlights & Fixes in `v1.0.2`
- **Critical Crash Fix (`MouseMixin`)**: Updated `MouseMixin` to target Minecraft `1.21.11`'s exact `Mouse.onMouseButton(long, MouseInput, int)` (`Lnet/minecraft/class_312;method_1601(JLnet/minecraft/class_11910;I)V`) signature with `@Surrogate` multi-signature safety, `require = 0` (`"defaultRequire": 0`), and a non-duplicating `CpsTracker` polling fallback.
- **Version Metadata Fix**: Replaced unexpanded `"${version}"` placeholder with `"1.0.2"` across `fabric.mod.json`, build scripts, UI footers, and configuration files.
- **Shader Startup Error Fix (`minecraft:core/rendertype_text`)**: Added cold-boot `ShaderLoader` readiness checks (`UiRenderUtils.isCoreTextShaderReady`) and a `RenderPipelines.GUI` pixel-matrix boot label renderer during initial `SplashOverlay` resource reload so `minecraft:core/rendertype_text` (`VERTEX` / `FRAGMENT`) is never queried before core shaders load.
- **Verified `1.21.11` Performance Stack & PojavLauncher Tuning**: Verified all 9 bundled `1.21.11` Fabric optimization dependencies (**Fabric API**, **Sodium**, **Lithium**, **FerriteCore**, **Entity Culling**, **More Culling**, **Cloth Config**, **Dynamic FPS**, **ImmediatelyFast**) with zero duplicates and mobile GLES-safe configuration defaults.

## Release Artifacts
- `Azyron-Client-1.0.2.jar` — Standalone Fabric `1.21.11` client mod JAR
- `Azyron-Modpack-1.0.2.mrpack` — Complete Modrinth modpack for PojavLauncher, Prism Launcher & Modrinth App
- `Azyron-Client-Source-v1.0.2.zip` / `Azyron-Client-Source-v1.0.2.tar.gz` — Complete updated Java 21 source code & build scripts
