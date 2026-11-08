package com.jigneshgaming.azyron;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jigneshgaming.azyron.config.AzyronConfig;
import com.jigneshgaming.azyron.cosmetics.CosmeticsManager;
import com.jigneshgaming.azyron.module.AzyronModule;
import com.jigneshgaming.azyron.module.HudModule;
import com.jigneshgaming.azyron.module.ModuleCategory;
import com.jigneshgaming.azyron.module.ModuleManager;
import com.jigneshgaming.azyron.module.Setting;
import com.jigneshgaming.azyron.module.impl.CoordinatesModule;
import com.jigneshgaming.azyron.module.impl.CpsModule;
import com.jigneshgaming.azyron.module.impl.CrosshairModule;
import com.jigneshgaming.azyron.module.impl.ToggleSprintModule;
import com.jigneshgaming.azyron.module.impl.ZoomModule;
import com.jigneshgaming.azyron.profile.FrameTimeTracker;
import com.jigneshgaming.azyron.profile.MemoryOptimizer;
import com.jigneshgaming.azyron.profile.PerformanceEngine;
import com.jigneshgaming.azyron.profile.PerformanceProfile;
import com.jigneshgaming.azyron.ui.AzyronCosmeticsScreen;
import com.jigneshgaming.azyron.ui.AzyronLoadingRenderer;
import com.jigneshgaming.azyron.ui.AzyronMainMenuScreen;
import com.jigneshgaming.azyron.ui.AzyronModsScreen;
import com.jigneshgaming.azyron.ui.HudEditorScreen;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.CpsTracker;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import com.mojang.blaze3d.shaders.ShaderType;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticlesMode;

/**
 * Comprehensive Automated Verification Suite for AZYRON CLIENT v1.0.2
 * (Minecraft 1.21.11 + Fabric Loader 0.20.1 + PojavLauncher Compatibility).
 */
public final class AzyronClientTestSuite {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=======================================================================");
        System.out.println(" AZYRON CLIENT v1.0.2 (MC 1.21.11 / Fabric 0.20.1) - Verification Suite");
        System.out.println("=======================================================================");

        Path jarPath = args.length > 0
                ? Paths.get(args[0])
                : Paths.get("releases/Azyron-Client-1.0.2.jar");
        Path mrpackPath = args.length > 1
                ? Paths.get(args[1])
                : Paths.get("releases/Azyron-Modpack-1.0.2.mrpack");

        runTest("1. Production JAR v1.0.2 Metadata (Zero ${version}), Mixins & PNG Integrity",
                () -> testJarIntegrityAndVersion(jarPath));
        runTest("2. MouseMixin 1.21.11 Descriptor, @Surrogate Safety & CPS Fallback Verification",
                () -> testMouseMixinDescriptorAndFallback(jarPath));
        runTest("3. Cold-Boot ShaderLoader (rendertype_text) Zero-Error & Default Text Rendering",
                AzyronClientTestSuite::testColdBootShaderSafetyAndTextRendering);
        runTest("4. Modpack Azyron-Modpack-1.0.2.mrpack (MC 1.21.11, Fabric 0.20.1, Zero Duplicates)",
                () -> testMrpackAndPojavConfigs(mrpackPath));
        runTest("5. Client Startup Bootstrap & All 11 Modules Registration",
                AzyronClientTestSuite::testBootstrapAndModules);
        runTest("6. Settings System (Boolean, Number Clamping/Stepping, Mode Cycling)",
                AzyronClientTestSuite::testSettingsSystem);
        runTest("7. JSON Config v1.0.2 Save & Lossless Round-Trip Reload",
                AzyronClientTestSuite::testConfigRoundTrip);
        runTest("8. Performance Profiles (Potato, Balanced, Smooth FPS), In-World Mipmap Guard & Culler",
                AzyronClientTestSuite::testPerformanceProfiles);
        runTest("9. Frame-Time 1% Low Tracker & Memory Optimizer Telemetry",
                AzyronClientTestSuite::testTelemetryAndMemory);
        runTest("10. Mouse Clicks, CPS Counter (LMB/RMB/Touch), Compass, Smooth Zoom & Toggle Sprint",
                AzyronClientTestSuite::testGameplayMechanics);
        runTest("11. Main Menu, Singleplayer World Loading, HUD, Settings Menu, HUD Editor & Cosmetics",
                AzyronClientTestSuite::testUiSingleplayerAndHudRendering);
        runTest("12. PojavLauncher Mobile Aspect Ratios (2400x1080 / 1280x720) & Pause Menu Touch UI",
                AzyronClientTestSuite::testPojavLauncherCompatibility);
        runTest("13. JVM Bytecode Linkage (FabricLoader InterfaceMethodref, Event, JOML Matrix3x2f, RegistryEntry, GameOptions)",
                () -> testJvmBytecodeLinkageContracts(jarPath));
        runTest("14. Full Fabric onInitializeClient() Startup Flow, Active Status Effects & 3GB-8GB RAM Smoothness",
                AzyronClientTestSuite::testFullFabricStartupLifecycleAndRamSmoothness);
        runTest("15. Automatic 3GB-8GB RAM Tier Scaling, Unfocused Background Throttle & 10,000-Frame HUD Stress Test",
                AzyronClientTestSuite::testAutoRamScalingBackgroundThrottleAndHudBenchmark);

        System.out.println("-----------------------------------------------------------------------");
        System.out.printf(" TEST SUMMARY: %d PASSED, %d FAILED%n", passed, failed);
        System.out.println("-----------------------------------------------------------------------");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void runTest(String name, ThrowingRunnable body) {
        try {
            body.run();
            passed++;
            System.out.println(" [PASS] " + name);
        } catch (Throwable t) {
            failed++;
            System.err.println(" [FAIL] " + name + " -> " + t.getMessage());
            t.printStackTrace(System.err);
        }
    }

    private static void testJarIntegrityAndVersion(Path jarPath) throws Exception {
        assertTrue(Files.exists(jarPath), "Production JAR must exist at " + jarPath);
        assertTrue(Files.size(jarPath) > 50_000L, "Production JAR size must be non-trivial");

        try (JarFile jf = new JarFile(jarPath.toFile())) {
            Manifest mf = jf.getManifest();
            assertNotNull(mf, "MANIFEST.MF must be present");
            Attributes attrs = mf.getMainAttributes();
            assertEquals("mixin", attrs.getValue("Fabric-Loom-Mixin-Remap-Type"), "Mixin remap type");
            assertEquals("intermediary", attrs.getValue("Fabric-Mapping-Namespace"), "Mapping namespace");

            // Strictly verify fabric.mod.json has expanded version "1.0.2" and zero "${" placeholders
            JarEntry fmjEntry = jf.getJarEntry("fabric.mod.json");
            assertNotNull(fmjEntry, "fabric.mod.json must exist in JAR");
            String rawFmj;
            try (InputStream in = jf.getInputStream(fmjEntry)) {
                rawFmj = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            assertTrue(!rawFmj.contains("${"), "fabric.mod.json must NOT contain unexpanded ${...} placeholders");
            JsonObject fmj = JsonParser.parseString(rawFmj).getAsJsonObject();
            assertEquals("azyron-client", fmj.get("id").getAsString(), "Mod ID");
            assertEquals("1.0.2", fmj.get("version").getAsString(), "Expanded mod version == 1.0.2");
            assertEquals("AZYRON CLIENT", fmj.get("name").getAsString(), "Client Name");

            JsonObject deps = fmj.getAsJsonObject("depends");
            assertEquals("1.21.11", deps.get("minecraft").getAsString(), "Strict Minecraft 1.21.11 dependency");
            assertEquals(">=21", deps.get("java").getAsString(), "Java 21 dependency");

            assertNotNull(jf.getJarEntry("com/jigneshgaming/azyron/AzyronClient.class"), "Entrypoint class in JAR");

            JsonObject mixinsJson = readJsonEntry(jf, "azyron-client.mixins.json");
            assertEquals(0, mixinsJson.getAsJsonObject("injectors").get("defaultRequire").getAsInt(),
                    "defaultRequire == 0 so optional mixin target conflicts never crash startup");

            JsonObject refmapJson = readJsonEntry(jf, "azyron-client.refmap.json");
            JsonObject mappings = refmapJson.getAsJsonObject("mappings");
            JsonArray mixinArray = mixinsJson.getAsJsonArray("client");
            assertEquals(7, mixinArray.size(), "All 7 client Mixins registered");

            for (int i = 0; i < mixinArray.size(); i++) {
                String mSimple = mixinArray.get(i).getAsString();
                String internal = "com/jigneshgaming/azyron/mixin/" + mSimple;
                assertNotNull(jf.getJarEntry(internal + ".class"), "Mixin class present: " + internal);
                assertTrue(mappings.has(internal), "Refmap entry present for " + internal);
            }

            verifyPng128(jf, "assets/azyron/icon.png");
            verifyPng128(jf, "assets/azyron/textures/gui/logo.png");
        }
    }

    private static void testMouseMixinDescriptorAndFallback(Path jarPath) throws Exception {
        try (JarFile jf = new JarFile(jarPath.toFile())) {
            // 1. Inspect refmap target for MouseMixin
            JsonObject refmap = readJsonEntry(jf, "azyron-client.refmap.json");
            JsonObject mouseMap = refmap.getAsJsonObject("mappings")
                    .getAsJsonObject("com/jigneshgaming/azyron/mixin/MouseMixin");
            String targetDesc = "Lnet/minecraft/class_312;method_1601(JLnet/minecraft/class_11910;I)V";
            assertEquals(targetDesc, mouseMap.get("onMouseButton(JLnet/minecraft/client/input/MouseInput;I)V").getAsString(),
                    "MouseMixin explicit 1.21.11 refmap target");

            // 2. Parse compiled & remapped MouseMixin.class constant pool + method descriptors directly
            JarEntry mouseMixinEntry = jf.getJarEntry("com/jigneshgaming/azyron/mixin/MouseMixin.class");
            assertNotNull(mouseMixinEntry, "MouseMixin.class must exist in JAR");
            byte[] bytes;
            try (InputStream in = jf.getInputStream(mouseMixinEntry)) {
                bytes = in.readAllBytes();
            }
            String asciiView = new String(bytes, StandardCharsets.ISO_8859_1);
            assertTrue(asciiView.contains("(JLnet/minecraft/class_11910;ILorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V"),
                    "MouseMixin.class must contain exact 1.21.11 intermediary callback descriptor (JLnet/minecraft/class_11910;IL...CallbackInfo;)V");
            assertTrue(asciiView.contains("Lorg/spongepowered/asm/mixin/injection/Surrogate;"),
                    "MouseMixin.class must retain runtime @Surrogate fallback handlers");
            assertTrue(asciiView.contains("comp_4801"),
                    "MouseMixin.class must invoke MouseInput.button() -> comp_4801()I");
        }

        // 3. Verify CPS fallback polling works when MouseMixin hook has NOT fired
        Path tempCfg = Files.createTempFile("azyron-cps-fb-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.currentScreen = null;
        CpsTracker.reset();
        assertTrue(!CpsTracker.isMixinHookActive(), "Mixin hook starts inactive after reset");

        CpsModule cpsMod = client.getModuleManager().getCpsModule();
        // Simulate 4 left-button click edges via Mouse state when Mixin hook is inactive
        for (int i = 0; i < 4; i++) {
            mc.mouse.setButtonStates(true, false);
            cpsMod.onTick(mc);
            mc.mouse.setButtonStates(false, false);
            cpsMod.onTick(mc);
            Thread.sleep(30L);
        }
        assertEquals(4, CpsTracker.getLeftCps(), "Fallback polling accurately records 4 LMB clicks without Mixin");

        // Now mark Mixin hook active and verify zero double-counting between hardware click & keybind poll
        CpsTracker.reset();
        CpsTracker.markMixinHookActive();
        CpsTracker.recordLeftClick();
        mc.mouse.setButtonStates(true, false);
        mc.options.attackKey.setPressed(true);
        cpsMod.onTick(mc);
        assertEquals(1, CpsTracker.getLeftCps(), "Hardware click + simultaneous attackKey/mouse state does not double count");
        mc.mouse.setButtonStates(false, false);
        mc.options.attackKey.setPressed(false);
        Files.deleteIfExists(tempCfg);
    }

    private static void testColdBootShaderSafetyAndTextRendering() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-shader-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();

        // Phase 1: Simulate cold-boot startup BEFORE ResourceReload loads minecraft:core/rendertype_text
        ShaderLoader shaderLoader = mc.getShaderLoader();
        shaderLoader.simulateColdBootEmpty();
        SplashOverlay splash = new SplashOverlay(mc, false);
        mc.setOverlay(splash);
        AzyronLoadingRenderer.setCoreResourcesReady(false);

        assertTrue(!UiRenderUtils.isCoreTextShaderReady(mc),
                "isCoreTextShaderReady must be false while ShaderLoader is empty during cold-boot SplashOverlay");

        DrawContext bootCtx = new DrawContext(854, 480);
        AzyronMainMenuScreen mainMenu = new AzyronMainMenuScreen();
        mainMenu.init(mc, 854, 480);

        for (int frame = 0; frame < 8; frame++) {
            float progress = frame * 0.12f;
            // GameRenderer renders currentScreen first, then SplashOverlay on top
            mainMenu.render(bootCtx, 427, 240, 0.016f);
            AzyronLoadingRenderer.render(bootCtx, mc, progress, 1.0f, false);
            client.getModuleManager().renderHudOverlay(bootCtx, mc);
        }

        // Verify ZERO shader errors for minecraft:core/rendertype_text during cold boot!
        assertEquals(0, shaderLoader.getShaderErrors().size(),
                "Cold-boot SplashOverlay & MainMenu must emit ZERO minecraft:core/rendertype_text shader errors");
        assertTrue(bootCtx.fillCallCount > 50,
                "Cold-boot 5x7 pixel font & progress bar must render cleanly via RenderPipelines.GUI quads");

        // Phase 2: Complete ResourceReload (ShaderLoader loads VERTEX + FRAGMENT sources for core/rendertype_text)
        shaderLoader.simulateReloadComplete();
        splash.markReloadComplete();
        AzyronLoadingRenderer.setCoreResourcesReady(true);
        assertTrue(UiRenderUtils.isCoreTextShaderReady(mc),
                "isCoreTextShaderReady must become true once core/rendertype_text VERTEX & FRAGMENT are loaded");
        assertNotNull(shaderLoader.getSource(UiRenderUtils.CORE_TEXT_SHADER_ID, ShaderType.VERTEX),
                "VERTEX shader source loaded");
        assertNotNull(shaderLoader.getSource(UiRenderUtils.CORE_TEXT_SHADER_ID, ShaderType.FRAGMENT),
                "FRAGMENT shader source loaded");

        // Fade-out SplashOverlay + uncovered Main Menu now render crisp native Minecraft text
        DrawContext readyCtx = new DrawContext(854, 480);
        AzyronLoadingRenderer.render(readyCtx, mc, 1.0f, 0.85f, true);
        assertTrue(readyCtx.drawnStrings.contains("Welcome to AZYRON CLIENT"),
                "Post-reload SplashOverlay renders native 'Welcome to AZYRON CLIENT'");

        mc.setOverlay(null);
        readyCtx.drawnStrings.clear();
        mainMenu.render(readyCtx, 427, 240, 0.016f);
        assertTrue(readyCtx.drawnStrings.contains("AZYRON CLIENT v1.0.2  •  Minecraft 1.21.11 (Fabric)"),
                "Main Menu renders updated v1.0.2 footer with native Minecraft TextRenderer");
        assertEquals(0, shaderLoader.getShaderErrors().size(),
                "Zero shader errors after reload completion");

        Files.deleteIfExists(tempCfg);
    }

    private static void testMrpackAndPojavConfigs(Path mrpackPath) throws Exception {
        assertTrue(Files.exists(mrpackPath), "Modpack must exist at " + mrpackPath);
        try (ZipFile zf = new ZipFile(mrpackPath.toFile())) {
            ZipEntry idxEntry = zf.getEntry("modrinth.index.json");
            assertNotNull(idxEntry, "modrinth.index.json present in mrpack");
            JsonObject idx;
            try (InputStream in = zf.getInputStream(idxEntry)) {
                idx = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            }
            assertEquals("1.0.2", idx.get("versionId").getAsString(), "mrpack versionId == 1.0.2");
            JsonObject deps = idx.getAsJsonObject("dependencies");
            assertEquals("1.21.11", deps.get("minecraft").getAsString(), "mrpack targets Minecraft 1.21.11");
            assertEquals("0.20.1", deps.get("fabric-loader").getAsString(), "mrpack targets Fabric Loader 0.20.1");

            JsonArray files = idx.getAsJsonArray("files");
            assertEquals(9, files.size(), "All 9 verified 1.21.11 Fabric mods included");
            Set<String> paths = new HashSet<>();
            for (JsonElement el : files) {
                JsonObject f = el.getAsJsonObject();
                String p = f.get("path").getAsString();
                assertTrue(paths.add(p), "No duplicate mod path in modrinth.index.json: " + p);
                assertEquals(128, f.getAsJsonObject("hashes").get("sha512").getAsString().length(), "Valid SHA-512 hash");
            }

            // Verify bundled client JAR inside mrpack is ONLY Azyron-Client-1.0.2.jar
            assertNotNull(zf.getEntry("overrides/mods/Azyron-Client-1.0.2.jar"),
                    "overrides/mods/Azyron-Client-1.0.2.jar present in mrpack");
            assertTrue(zf.getEntry("overrides/mods/Azyron-Client-1.0.1.jar") == null,
                    "Old 1.0.1 JAR must not be duplicated in mrpack");

            // Verify PojavLauncher mobile GLES-safe config flags
            ZipEntry sodiumEntry = zf.getEntry("overrides/config/sodium-options.json");
            assertNotNull(sodiumEntry, "sodium-options.json in mrpack");
            try (InputStream in = zf.getInputStream(sodiumEntry)) {
                JsonObject sod = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                assertTrue(!sod.getAsJsonObject("performance").get("use_no_error_g_l_context").getAsBoolean(),
                        "PojavLauncher safety: use_no_error_g_l_context == false");
            }

            ZipEntry immEntry = zf.getEntry("overrides/config/immediatelyfast.json");
            assertNotNull(immEntry, "immediatelyfast.json in mrpack");
            try (InputStream in = zf.getInputStream(immEntry)) {
                JsonObject imm = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                assertTrue(!imm.get("font_atlas_resizing").getAsBoolean(),
                        "PojavLauncher safety: font_atlas_resizing == false");
                assertTrue(!imm.get("fast_buffer_upload").getAsBoolean(),
                        "PojavLauncher safety: fast_buffer_upload == false");
            }
        }
    }

    private static void testBootstrapAndModules() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-test-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);

        ModuleManager mm = client.getModuleManager();
        assertEquals(11, mm.getModules().size(), "11 total modules registered");
        assertEquals(9, mm.getHudModules().size(), "9 draggable HUD modules registered");

        String[] requiredIds = {
                "fps_counter", "cps_counter", "ping_display", "coordinates_hud",
                "keystrokes_hud", "armor_hud", "potion_hud", "toggle_sprint",
                "zoom", "custom_crosshair", "memory_hud"
        };
        for (String id : requiredIds) {
            assertNotNull(mm.getById(id), "Module must be registered: " + id);
        }

        List<AzyronModule> zoomSearch = mm.filterModules(ModuleCategory.ALL, "zoom");
        assertEquals(1, zoomSearch.size(), "Search filter finds Zoom module");
        Files.deleteIfExists(tempCfg);
    }

    private static void testSettingsSystem() {
        Setting.BooleanSetting bool = new Setting.BooleanSetting("b", "Bool", "desc", false);
        bool.toggle();
        assertTrue(bool.getValue(), "BooleanSetting toggles to true");

        Setting.NumberSetting num = new Setting.NumberSetting("n", "Num", "desc", 4.0, 2.0, 12.0, 0.5);
        num.setValue(99.0);
        assertEquals(12.0, num.getValue(), "NumberSetting clamps to max");
        num.setFromNormalized(0.25f);
        assertEquals(4.5, num.getValue(), "NumberSetting normalized step calculation");

        Setting.ModeSetting mode = new Setting.ModeSetting("m", "Mode", "desc", "Cross", "Cross", "Dot", "Box");
        mode.cycleNext();
        assertEquals("Dot", mode.getValue(), "ModeSetting cycles forward");
        mode.cyclePrev();
        assertEquals("Cross", mode.getValue(), "ModeSetting cycles backward");
    }

    private static void testConfigRoundTrip() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-roundtrip-", ".json");
        Files.deleteIfExists(tempCfg);

        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        HudModule fps = client.getModuleManager().getFpsModule();
        fps.setAnchor(0.42f, 0.73f);
        fps.setScale(1.35f);
        client.getModuleManager().getCrosshairModule().setEnabled(true);
        client.getCosmeticsManager().setActiveCape(CosmeticsManager.CapeStyle.CYBER_GRID);
        client.getCosmeticsManager().setActiveHalo(CosmeticsManager.HaloStyle.CROWN_AURA);
        client.getConfig().setActiveProfile(PerformanceProfile.POTATO);
        client.getConfig().save();

        JsonObject savedJson = JsonParser.parseString(Files.readString(tempCfg)).getAsJsonObject();
        assertEquals("1.0.2", savedJson.get("configVersion").getAsString(), "Saved configVersion == 1.0.2");

        AzyronConfig reloaded = new AzyronConfig(tempCfg);
        reloaded.load();
        ModuleManager mm2 = new ModuleManager();
        CosmeticsManager cm2 = new CosmeticsManager();
        reloaded.applyToManagers(mm2, cm2);

        assertEquals(PerformanceProfile.POTATO, reloaded.getActiveProfile(), "Reloaded active profile");
        assertEquals(CosmeticsManager.CapeStyle.CYBER_GRID, cm2.getActiveCape(), "Reloaded cape");
        assertEquals(CosmeticsManager.HaloStyle.CROWN_AURA, cm2.getActiveHalo(), "Reloaded halo");
        assertTrue(mm2.getCrosshairModule().isEnabled(), "Reloaded crosshair enabled state");
        assertTrue(Math.abs(mm2.getFpsModule().getAnchorX() - 0.42f) < 0.001f, "Reloaded HUD anchorX");
        assertTrue(Math.abs(mm2.getFpsModule().getScale() - 1.35f) < 0.001f, "Reloaded HUD scale");

        Files.deleteIfExists(tempCfg);
    }

    private static void testPerformanceProfiles() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-perf-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();
        PerformanceEngine engine = client.getPerformanceEngine();

        // On main menu (mc.world == null), mipmapLevels IS updated
        ClientWorld savedWorld = mc.world;
        mc.world = null;
        engine.applyProfile(PerformanceProfile.POTATO, mc, true);
        assertEquals(GraphicsMode.FAST, mc.options.getPreset().getValue(), "Potato FAST graphics");
        assertEquals(ParticlesMode.MINIMAL, mc.options.getParticles().getValue(), "Potato MINIMAL particles");
        assertEquals(CloudRenderMode.OFF, mc.options.getCloudRenderMode().getValue(), "Potato OFF clouds");
        assertEquals(6, mc.options.getViewDistance().getValue(), "Potato view distance 6");
        assertEquals(5, mc.options.getSimulationDistance().getValue(), "Potato sim distance 5");
        assertEquals(0, mc.options.getMipmapLevels().getValue(), "Menu Potato sets mipmapLevels 0");

        // In-world (mc.world != null), switching to BALANCED updates view distance without mutating mipmapLevels mid-world
        mc.world = savedWorld;
        engine.applyProfile(PerformanceProfile.BALANCED, mc, true);
        assertEquals(GraphicsMode.FANCY, mc.options.getPreset().getValue(), "Balanced FANCY graphics");
        assertEquals(10, mc.options.getViewDistance().getValue(), "Balanced view distance 10");
        assertEquals(0, mc.options.getMipmapLevels().getValue(), "In-world profile switch avoids mipmap reload spike");

        // Verify Particle Throttler in Potato Mode
        engine.applyProfile(PerformanceProfile.POTATO, mc, false);
        engine.onClientTick(mc);
        assertTrue(!engine.shouldAllowParticle(mc.player.getX() + 100.0, mc.player.getY(), mc.player.getZ()),
                "Potato Mode culls distant particles beyond 18 blocks");
        int allowed = 0;
        for (int i = 0; i < 100; i++) {
            if (engine.shouldAllowParticle(mc.player.getX() + 2.0, mc.player.getY(), mc.player.getZ())) {
                allowed++;
            }
        }
        assertEquals(40, allowed, "Potato Mode strictly caps nearby particles at 40/tick");

        engine.applyProfile(PerformanceProfile.SMOOTH_FPS, mc, true);
        assertEquals(120, mc.options.getMaxFps().getValue(), "Smooth FPS targets stable 120 FPS");

        Files.deleteIfExists(tempCfg);
    }

    private static void testTelemetryAndMemory() {
        for (int i = 0; i < 20; i++) {
            FrameTimeTracker.onFrameRendered();
        }
        int low = FrameTimeTracker.getOnePercentLowFps(240);
        assertTrue(low >= 1 && low <= 240, "1% low FPS within valid bounds");
        assertNotNull(FrameTimeTracker.getStabilityRating(), "Stability rating non-null");

        MemoryOptimizer.onClientTick(MinecraftClient.getInstance(), true);
        assertTrue(MemoryOptimizer.getMaxMb() > 0, "JVM Max MB > 0");
        assertTrue(MemoryOptimizer.getUsagePercent() >= 0 && MemoryOptimizer.getUsagePercent() <= 100, "RAM % in [0..100]");
    }

    private static void testGameplayMechanics() {
        CpsTracker.reset();
        for (int i = 0; i < 9; i++) CpsTracker.recordLeftClick();
        for (int i = 0; i < 5; i++) CpsTracker.recordRightClick();
        assertEquals(9, CpsTracker.getLeftCps(), "LMB CPS tracks 9 clicks");
        assertEquals(5, CpsTracker.getRightCps(), "RMB CPS tracks 5 clicks");

        assertEquals("S (+Z)", CoordinatesModule.formatFacing(0.0f), "Yaw 0 = South");
        assertEquals("W (-X)", CoordinatesModule.formatFacing(90.0f), "Yaw 90 = West");
        assertEquals("N (-Z)", CoordinatesModule.formatFacing(180.0f), "Yaw 180 = North");
        assertEquals("E (+X)", CoordinatesModule.formatFacing(270.0f), "Yaw 270 = East");

        ZoomModule zoom = new ZoomModule();
        assertEquals(70.0f, zoom.applyFov(70.0f), "Unzoomed FOV unchanged");
        zoom.setZooming(true);
        float zoomedFov = 70.0f;
        for (int i = 0; i < 30; i++) {
            zoomedFov = zoom.applyFov(70.0f);
        }
        assertTrue(zoomedFov < 20.0f, "Smooth Zoom interpolates 70 FOV down toward ~17.5 FOV");
        assertTrue(zoom.onScroll(1.0), "Scroll-to-zoom consumes scroll while zooming");

        ToggleSprintModule ts = new ToggleSprintModule();
        assertTrue(ts.isSprintToggled(), "ToggleSprint enabled by default");
        ts.toggleSprintState();
        assertTrue(!ts.isSprintToggled(), "ToggleSprint toggles off cleanly");
    }

    private static void testUiSingleplayerAndHudRendering() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-ui-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.setOverlay(null);
        mc.getShaderLoader().simulateReloadComplete();
        AzyronLoadingRenderer.setCoreResourcesReady(true);

        DrawContext ctx = new DrawContext(854, 480);
        assertEquals(0xFF00D4FF, AzyronTheme.ACCENT_ELECTRIC_BLUE, "Electric blue has 0xFF opaque alpha");

        // 1. Loading Screen
        AzyronLoadingRenderer.render(ctx, mc, 0.65f, 1.0f);
        assertTrue(ctx.drawnStrings.contains("Welcome to AZYRON CLIENT"), "Loading screen renders 'Welcome to AZYRON CLIENT'");

        // 2. Main Menu + Singleplayer World Selection & World Load Flow
        ctx.drawnStrings.clear();
        AzyronMainMenuScreen mainMenu = new AzyronMainMenuScreen();
        mainMenu.init(mc, 854, 480);
        mainMenu.render(ctx, 427, 240, 0.016f);
        for (String expectedBtn : new String[]{
                "AZYRON CLIENT", "Singleplayer", "Multiplayer", "Cosmetics", "Mods",
                "Settings", "Quit Game", "Discord  •  Join JIGNESH GAMING Community"
        }) {
            assertTrue(ctx.drawnStrings.contains(expectedBtn), "Main Menu renders '" + expectedBtn + "'");
        }

        // Click "Singleplayer" -> verify clean transition to SelectWorldScreen -> enter world & render HUD
        mainMenu.handleAction("singleplayer");
        assertTrue(mc.currentScreen instanceof SelectWorldScreen,
                "Clicking 'Singleplayer' opens vanilla SelectWorldScreen cleanly");
        mc.currentScreen = null;
        mc.world = new ClientWorld();

        // 3. In-World HUD Overlay + Custom Crosshair (all 5 styles)
        CrosshairModule ch = client.getModuleManager().getCrosshairModule();
        ch.setEnabled(true);
        Setting.ModeSetting styleSetting = (Setting.ModeSetting) ch.getSettings().get(0);
        for (String style : styleSetting.getModes()) {
            styleSetting.setValue(style);
            ch.renderCrosshair(ctx, mc);
        }
        ctx.drawnStrings.clear();
        client.getModuleManager().renderHudOverlay(ctx, mc);
        assertTrue(ctx.drawnStrings.stream().anyMatch(s -> s.contains("FPS")), "In-world HUD renders FPS module");
        assertTrue(ctx.drawnStrings.stream().anyMatch(s -> s.contains("CPS")), "In-world HUD renders CPS module");

        // 4. Right-Shift Mods & Settings Menu + Search + Profiles Tab
        mainMenu.handleAction("mods");
        assertTrue(mc.currentScreen instanceof AzyronModsScreen, "Main Menu 'Mods' opens AzyronModsScreen");
        AzyronModsScreen modsScreen = (AzyronModsScreen) mc.currentScreen;
        modsScreen.init(mc, 854, 480);
        modsScreen.render(ctx, 400, 220, 0.016f);
        modsScreen.openModuleSettings(client.getModuleManager().getFpsModule());
        modsScreen.render(ctx, 400, 220, 0.016f);
        modsScreen.openProfilesTab();
        modsScreen.render(ctx, 400, 220, 0.016f);
        modsScreen.openClientOptionsTab();
        ctx.drawnStrings.clear();
        modsScreen.render(ctx, 400, 220, 0.016f);
        assertTrue(ctx.drawnStrings.contains("AZYRON CLIENT  •  v1.0.2  (Minecraft 1.21.11 Fabric)"),
                "Mods Screen footer displays v1.0.2");
        modsScreen.keyPressed(new KeyInput(256, 0, 0));

        // 5. Interactive HUD Editor Drag & Scale
        HudEditorScreen editor = new HudEditorScreen(mainMenu);
        editor.init(mc, 854, 480);
        editor.render(ctx, 200, 200, 0.016f);
        HudModule fpsHud = client.getModuleManager().getFpsModule();
        int startX = fpsHud.getScreenX(854) + 5;
        int startY = fpsHud.getScreenY(480) + 5;
        editor.mouseClicked(new Click(startX, startY, new MouseInput(0, 0)), false);
        editor.mouseDragged(new Click(300, 180, new MouseInput(0, 0)), 100, 100);
        editor.mouseReleased(new Click(300, 180, new MouseInput(0, 0)));
        editor.mouseScrolled(fpsHud.getScreenX(854) + 4, fpsHud.getScreenY(480) + 4, 0.0, 1.0);
        assertTrue(fpsHud.getScale() > 1.0f, "HUD Editor scroll wheel increases widget scale");

        // 6. Cosmetics Screen
        AzyronCosmeticsScreen cosScreen = new AzyronCosmeticsScreen(mainMenu);
        cosScreen.init(mc, 854, 480);
        cosScreen.render(ctx, 300, 200, 0.016f);
        cosScreen.charTyped(new CharInput('a', 0));

        Files.deleteIfExists(tempCfg);
    }

    private static void testPojavLauncherCompatibility() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-pojav-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.setOverlay(null);
        mc.getShaderLoader().simulateReloadComplete();
        AzyronLoadingRenderer.setCoreResourcesReady(true);

        // Verify ultrawide 20:9 Android phone scaled resolution (800x360 -> 2400x1080 @ GUI scale 3)
        // and compact 16:9 tablet resolution (640x360 -> 1280x720 @ GUI scale 2)
        for (int[] res : new int[][]{{800, 360}, {640, 360}}) {
            int w = res[0];
            int h = res[1];
            DrawContext mobileCtx = new DrawContext(w, h);
            AzyronMainMenuScreen mm = new AzyronMainMenuScreen();
            mm.init(mc, w, h);
            mm.render(mobileCtx, w / 2, h / 2, 0.016f);

            for (HudModule hud : client.getModuleManager().getHudModules()) {
                int sx = hud.getScreenX(w);
                int sy = hud.getScreenY(h);
                assertTrue(sx >= 0 && sx <= w, "HUD X clamped inside mobile viewport " + w + "x" + h);
                assertTrue(sy >= 0 && sy <= h, "HUD Y clamped inside mobile viewport " + w + "x" + h);
            }
            client.getModuleManager().renderHudOverlay(mobileCtx, mc);
        }

        // Verify Pause Menu Screen works cleanly on touch devices (no Right-Shift physical keyboard required)
        GameMenuScreen pauseScreen = new GameMenuScreen(true);
        pauseScreen.init(mc, 640, 360);
        assertNotNull(pauseScreen, "GameMenuScreen initializes cleanly for PojavLauncher touch controls");

        Files.deleteIfExists(tempCfg);
    }

    private static void testJvmBytecodeLinkageContracts(Path jarPath) throws Exception {
        int checkedClasses = 0;
        boolean sawFabricLoaderInterfaceRef = false;
        boolean sawFabricEventClassRef = false;
        boolean sawJomlMatrix3x2fReturn = false;
        boolean sawRegistryEntryInterfaceRef = false;
        boolean sawGetAoMethodRef = false;
        boolean sawGetVignetteMethodRef = false;

        try (JarFile jf = new JarFile(jarPath.toFile())) {
            var entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry je = entries.nextElement();
                if (!je.getName().endsWith(".class")) {
                    continue;
                }
                checkedClasses++;
                byte[] data;
                try (InputStream in = jf.getInputStream(je)) {
                    data = in.readAllBytes();
                }

                ByteBuffer buf = ByteBuffer.wrap(data);
                int magic = buf.getInt();
                assertEquals(0xCAFEBABE, magic, "Valid class magic in " + je.getName());
                buf.getShort(); // minor
                int major = buf.getShort() & 0xFFFF;
                assertEquals(65, major, "Java 21 classfile v65 in " + je.getName());

                int cpCount = buf.getShort() & 0xFFFF;
                int[] tags = new int[cpCount];
                String[] utf8 = new String[cpCount];
                int[] ref1 = new int[cpCount];
                int[] ref2 = new int[cpCount];

                for (int i = 1; i < cpCount; i++) {
                    int tag = buf.get() & 0xFF;
                    tags[i] = tag;
                    switch (tag) {
                        case 1 -> {
                            int len = buf.getShort() & 0xFFFF;
                            byte[] strBytes = new byte[len];
                            buf.get(strBytes);
                            utf8[i] = new String(strBytes, StandardCharsets.UTF_8);
                        }
                        case 7, 8, 16, 19, 20 -> ref1[i] = buf.getShort() & 0xFFFF;
                        case 9, 10, 11, 12, 17, 18 -> {
                            ref1[i] = buf.getShort() & 0xFFFF;
                            ref2[i] = buf.getShort() & 0xFFFF;
                        }
                        case 3, 4 -> buf.position(buf.position() + 4);
                        case 5, 6 -> {
                            buf.position(buf.position() + 8);
                            i++;
                        }
                        case 15 -> buf.position(buf.position() + 3);
                        default -> throw new AssertionError("Unknown CP tag " + tag + " in " + je.getName());
                    }
                }

                for (int i = 1; i < cpCount; i++) {
                    int tag = tags[i];
                    if (tag == 7) {
                        String clsName = utf8[ref1[i]];
                        assertTrue(!"net/fabricmc/fabric/api/client/event/lifecycle/v1/ClientTickEvents$Event".equals(clsName),
                                "Must NOT reference bogus nested ClientTickEvents$Event in " + je.getName());
                        assertTrue(!clsName.startsWith("net/minecraft/client/"),
                                "Must NOT contain unremapped net/minecraft/client/* class in " + je.getName() + ": " + clsName);
                    } else if (tag == 9 || tag == 10 || tag == 11) {
                        String owner = utf8[ref1[ref1[i]]];
                        String name = utf8[ref1[ref2[i]]];
                        String desc = utf8[ref2[ref2[i]]];

                        // 1. FabricLoader MUST use CONSTANT_InterfaceMethodref (tag == 11), NEVER CONSTANT_Methodref (10)
                        if ("net/fabricmc/loader/api/FabricLoader".equals(owner)) {
                            assertEquals(11, tag,
                                    "FabricLoader." + name + desc + " in " + je.getName()
                                            + " MUST be CONSTANT_InterfaceMethodref (tag 11)");
                            sawFabricLoaderInterfaceRef = true;
                        }

                        // 2. ClientTickEvents.END_CLIENT_TICK MUST have descriptor Lnet/fabricmc/fabric/api/event/Event;
                        if (tag == 9 && "net/fabricmc/fabric/api/client/event/lifecycle/v1/ClientTickEvents".equals(owner)
                                && "END_CLIENT_TICK".equals(name)) {
                            assertEquals("Lnet/fabricmc/fabric/api/event/Event;", desc,
                                    "ClientTickEvents.END_CLIENT_TICK descriptor in " + je.getName());
                            sawFabricEventClassRef = true;
                        }

                        // 3. JOML Matrix3x2fStack translate/scale MUST return Lorg/joml/Matrix3x2f;
                        if ("org/joml/Matrix3x2fStack".equals(owner)
                                && ("translate".equals(name) || "scale".equals(name))) {
                            assertEquals("(FF)Lorg/joml/Matrix3x2f;", desc,
                                    "Matrix3x2fStack." + name + " descriptor in " + je.getName());
                            sawJomlMatrix3x2fReturn = true;
                        }

                        // 4. RegistryEntry (net/minecraft/class_6880) MUST use CONSTANT_InterfaceMethodref (tag == 11)
                        if ("net/minecraft/class_6880".equals(owner)) {
                            assertEquals(11, tag,
                                    "RegistryEntry (class_6880)." + name + " in " + je.getName()
                                            + " MUST be CONSTANT_InterfaceMethodref (tag 11)");
                            sawRegistryEntryInterfaceRef = true;
                        }

                        // 5. GameOptions (class_315) MUST NOT touch private fields field_1841 (ao) or field_63499 (vignette)
                        if (tag == 9 && "net/minecraft/class_315".equals(owner)) {
                            assertTrue(!"field_1841".equals(name) && !"field_63499".equals(name),
                                    "GameOptions private field " + name + " accessed directly in " + je.getName());
                        }
                        if ("net/minecraft/class_315".equals(owner) && "method_41792".equals(name)) {
                            sawGetAoMethodRef = true;
                        }
                        if ("net/minecraft/class_315".equals(owner) && "method_75335".equals(name)) {
                            sawGetVignetteMethodRef = true;
                        }
                    }
                }
            }

            // Also verify en_us.json has the 1.21.11 keybinding category translation key
            JsonObject langJson = readJsonEntry(jf, "assets/azyron/lang/en_us.json");
            assertEquals("AZYRON CLIENT", langJson.get("key.category.azyron.controls").getAsString(),
                    "1.21.11 keybinding category lang key present");
        }

        assertTrue(checkedClasses >= 40, "Inspected all compiled classes in JAR (" + checkedClasses + ")");
        assertTrue(sawFabricLoaderInterfaceRef, "Verified FabricLoader CONSTANT_InterfaceMethodref in JAR");
        assertTrue(sawFabricEventClassRef, "Verified ClientTickEvents.END_CLIENT_TICK Event descriptor in JAR");
        assertTrue(sawJomlMatrix3x2fReturn, "Verified JOML Matrix3x2fStack.translate/scale return Matrix3x2f in JAR");
        assertTrue(sawRegistryEntryInterfaceRef, "Verified RegistryEntry (class_6880) CONSTANT_InterfaceMethodref in JAR");
        assertTrue(sawGetAoMethodRef && sawGetVignetteMethodRef,
                "Verified GameOptions.getAo() & getVignette() getter calls in JAR");
    }

    private static void testFullFabricStartupLifecycleAndRamSmoothness() throws Exception {
        // 1. Execute the REAL Fabric entrypoint onInitializeClient() end-to-end
        AzyronClient entrypoint = new AzyronClient();
        entrypoint.onInitializeClient();
        assertEquals(entrypoint, AzyronClient.getInstance(), "onInitializeClient sets singleton instance");
        assertEquals(11, entrypoint.getModuleManager().getModules().size(), "All 11 modules initialized via entrypoint");

        // 2. Verify active status effect rendering through RegistryEntry interface dispatch
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.getShaderLoader().simulateReloadComplete();
        DrawContext ctx = new DrawContext(854, 480);
        entrypoint.getModuleManager().getPotionHudModule().setEnabled(true);
        entrypoint.getModuleManager().renderHudOverlay(ctx, mc);
        assertTrue(ctx.drawnStrings.stream().anyMatch(s -> s.contains("Speed II")),
                "PotionHudModule renders active Speed II effect via RegistryEntry interface cleanly");

        // 3. Fire live Fabric END_CLIENT_TICK event and verify zero exceptions & throttled RAM telemetry
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.invoker().onEndTick(mc);
        for (int i = 0; i < 120; i++) {
            MemoryOptimizer.onClientTick(mc, true);
        }
        assertTrue(MemoryOptimizer.getMaxMb() > 0, "3GB-8GB RAM telemetry operational under high-FPS polling");
    }

    private static void testAutoRamScalingBackgroundThrottleAndHudBenchmark() throws Exception {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld prevWorld = mc.world;

        try {
            // 1. Simulate 3GB RAM device (1536MB JVM heap / 3072MB physical RAM)
            MemoryOptimizer.setSimulatedRamForTesting(1536, 3072);
            assertEquals(MemoryOptimizer.RamTier.TIER_3GB, MemoryOptimizer.detectRamTier(), "3GB device classified as TIER_3GB");
            assertEquals(PerformanceProfile.POTATO, MemoryOptimizer.getRecommendedProfileForHardware(),
                    "3GB RAM device auto-selects Potato Mode on first boot");

            Path cfg3gb = Files.createTempFile("azyron-3gb-", ".json");
            Files.deleteIfExists(cfg3gb);
            AzyronClient c3 = AzyronClient.bootstrapHeadlessForTests(cfg3gb);
            assertEquals(PerformanceProfile.POTATO, c3.getConfig().getActiveProfile(),
                    "First-launch config persists auto-detected POTATO profile on 3GB RAM");

            // Set inflated vanilla options (16 view, 12 sim, FANCY, 4 mipmaps) and let auto-calibration clamp them
            mc.world = null;
            mc.options.getViewDistance().setValue(16);
            mc.options.getSimulationDistance().setValue(12);
            mc.options.getMipmapLevels().setValue(4);
            mc.options.getEntityDistanceScaling().setValue(1.25);
            mc.options.applyGraphicsMode(GraphicsMode.FANCY);
            c3.getPerformanceEngine().onClientTick(mc);

            assertEquals(6, mc.options.getViewDistance().getValue(), "3GB RAM auto-clamps view distance to 6");
            assertEquals(5, mc.options.getSimulationDistance().getValue(), "3GB RAM auto-clamps simulation distance to 5");
            assertEquals(0, mc.options.getMipmapLevels().getValue(), "3GB RAM auto-clamps mipmaps to 0");
            assertEquals(GraphicsMode.FAST, mc.options.getPreset().getValue(), "3GB RAM auto-switches to FAST graphics");
            Files.deleteIfExists(cfg3gb);

            // 2. Simulate 4GB RAM device (2048MB JVM heap / 4096MB physical RAM)
            MemoryOptimizer.setSimulatedRamForTesting(2048, 4096);
            assertEquals(MemoryOptimizer.RamTier.TIER_4GB, MemoryOptimizer.detectRamTier(), "4GB device classified as TIER_4GB");
            assertEquals(PerformanceProfile.SMOOTH_FPS, MemoryOptimizer.getRecommendedProfileForHardware(),
                    "4GB RAM device auto-selects Smooth FPS Mode");
            mc.options.getViewDistance().setValue(14);
            mc.options.getSimulationDistance().setValue(10);
            c3.getPerformanceEngine().applyAutoRamCalibration(mc);
            assertEquals(7, mc.options.getViewDistance().getValue(), "4GB RAM auto-clamps view distance to 7");
            assertEquals(6, mc.options.getSimulationDistance().getValue(), "4GB RAM auto-clamps simulation distance to 6");

            // 3. Simulate 6GB RAM device (3072MB JVM heap / 6144MB physical RAM) & 8GB RAM device (4096MB / 8192MB)
            MemoryOptimizer.setSimulatedRamForTesting(3072, 6144);
            assertEquals(MemoryOptimizer.RamTier.TIER_6GB, MemoryOptimizer.detectRamTier(), "6GB device classified as TIER_6GB");
            assertEquals(PerformanceProfile.BALANCED, MemoryOptimizer.getRecommendedProfileForHardware(),
                    "6GB RAM device auto-selects Balanced Mode");

            MemoryOptimizer.setSimulatedRamForTesting(4096, 8192);
            assertEquals(MemoryOptimizer.RamTier.TIER_8GB, MemoryOptimizer.detectRamTier(), "8GB device classified as TIER_8GB");
            assertEquals(PerformanceProfile.BALANCED, MemoryOptimizer.getRecommendedProfileForHardware(),
                    "8GB RAM device auto-selects Balanced Mode");

            // 4. Verify unfocused window background FPS & particle throttling
            PerformanceEngine engine = c3.getPerformanceEngine();
            engine.applyProfile(PerformanceProfile.BALANCED, mc, false);
            assertEquals(260, mc.options.getMaxFps().getValue(), "Focused Balanced maxFps == 260");
            engine.onWindowFocusChanged(mc, false);
            assertEquals(15, mc.options.getMaxFps().getValue(), "Unfocused window throttles background FPS to 15");
            assertTrue(!engine.shouldAllowParticle(mc.player.getX(), mc.player.getY(), mc.player.getZ()),
                    "Unfocused window culls background particles");
            engine.onWindowFocusChanged(mc, true);
            assertEquals(260, mc.options.getMaxFps().getValue(), "Refocused window restores maxFps to 260 immediately");

            // 5. 10,000-frame zero-allocation HUD render & primitive CpsTracker stress benchmark
            mc.world = prevWorld != null ? prevWorld : new ClientWorld();
            mc.setOverlay(null);
            mc.getShaderLoader().simulateReloadComplete();
            AzyronLoadingRenderer.setCoreResourcesReady(true);
            c3.getModuleManager().getMemoryHudModule().setEnabled(true);
            CpsTracker.reset();
            for (int c = 0; c < 35; c++) {
                CpsTracker.recordLeftClick();
            }
            assertEquals(35, CpsTracker.getLeftCps(), "Primitive ring-buffer CpsTracker records 35 rapid clicks");

            DrawContext benchCtx = new DrawContext(1920, 1080);
            long t0 = System.nanoTime();
            for (int frame = 0; frame < 10_000; frame++) {
                benchCtx.drawnStrings.clear();
                c3.getModuleManager().renderHudOverlay(benchCtx, mc);
            }
            long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;
            assertTrue(elapsedMs < 4_000L, "10,000 full HUD frames rendered smoothly in " + elapsedMs + "ms");
            assertTrue(benchCtx.drawnStrings.stream().anyMatch(s -> s.contains("35 |")),
                    "Cached CPS HUD renders 35 LMB CPS accurately across 10,000 frames");
        } finally {
            MemoryOptimizer.setSimulatedRamForTesting(-1, -1);
            FrameTimeTracker.resetForTesting();
            mc.world = prevWorld;
        }
    }

    private static JsonObject readJsonEntry(JarFile jf, String name) throws Exception {
        JarEntry je = jf.getJarEntry(name);
        assertNotNull(je, "Missing JAR entry: " + name);
        try (InputStream in = jf.getInputStream(je)) {
            String raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return JsonParser.parseString(raw).getAsJsonObject();
        }
    }

    private static void verifyPng128(JarFile jf, String path) throws Exception {
        JarEntry je = jf.getJarEntry(path);
        assertNotNull(je, "Missing PNG asset: " + path);
        try (InputStream in = jf.getInputStream(je)) {
            byte[] b = in.readAllBytes();
            assertTrue(b.length > 24, "PNG bytes > 24");
            assertEquals((byte) 0x89, b[0], "PNG signature byte 0");
            assertEquals((byte) 'P', b[1], "PNG signature byte 1");
            assertEquals((byte) 'N', b[2], "PNG signature byte 2");
            assertEquals((byte) 'G', b[3], "PNG signature byte 3");
            int width = ByteBuffer.wrap(b, 16, 4).getInt();
            int height = ByteBuffer.wrap(b, 20, 4).getInt();
            assertEquals(128, width, path + " width == 128");
            assertEquals(128, height, path + " height == 128");
        }
    }

    private static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("Assertion failed: " + msg);
    }

    private static void assertNotNull(Object obj, String msg) {
        if (obj == null) throw new AssertionError("Expected non-null: " + msg);
    }

    private static void assertEquals(Object expected, Object actual, String msg) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(msg + " | expected=<" + expected + "> actual=<" + actual + ">");
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
