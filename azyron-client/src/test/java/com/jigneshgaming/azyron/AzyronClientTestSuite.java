package com.jigneshgaming.azyron;

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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.particle.ParticlesMode;

/**
 * Comprehensive Automated Verification Suite for AZYRON CLIENT (Minecraft 1.21.11 Fabric).
 */
public final class AzyronClientTestSuite {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=================================================================");
        System.out.println(" AZYRON CLIENT v1.0.0 (MC 1.21.11) - Automated Verification Suite");
        System.out.println("=================================================================");

        Path jarPath = args.length > 0
                ? Paths.get(args[0])
                : Paths.get("build/libs/azyron-client-1.0.0+mc1.21.11.jar");

        runTest("1. Production JAR Manifest, Metadata, Mixins & PNG Branding Integrity", () -> testJarIntegrity(jarPath));
        runTest("2. Client Bootstrap & All 11 Modules Registration", AzyronClientTestSuite::testBootstrapAndModules);
        runTest("3. Settings System (Boolean, Number Clamping/Stepping, Mode Cycling)", AzyronClientTestSuite::testSettingsSystem);
        runTest("4. JSON Config Save & Lossless Round-Trip Reload", AzyronClientTestSuite::testConfigRoundTrip);
        runTest("5. Performance Profiles (Potato, Balanced, Smooth FPS) & Particle Culler", AzyronClientTestSuite::testPerformanceProfiles);
        runTest("6. Frame-Time 1% Low Tracker & Memory Optimizer Telemetry", AzyronClientTestSuite::testTelemetryAndMemory);
        runTest("7. CPS Tracker, Compass Facing Math, Smooth Zoom & Toggle Sprint", AzyronClientTestSuite::testGameplayMechanics);
        runTest("8. HUD Rendering, Loading Screen, Main Menu, Right-Shift Menu, HUD Editor & Cosmetics UI", AzyronClientTestSuite::testUiAndHudRendering);

        System.out.println("-----------------------------------------------------------------");
        System.out.printf(" TEST SUMMARY: %d PASSED, %d FAILED%n", passed, failed);
        System.out.println("-----------------------------------------------------------------");

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

    private static void testJarIntegrity(Path jarPath) throws Exception {
        assertTrue(Files.exists(jarPath), "Production JAR must exist at " + jarPath);
        assertTrue(Files.size(jarPath) > 50_000L, "Production JAR size must be non-trivial");

        try (JarFile jf = new JarFile(jarPath.toFile())) {
            Manifest mf = jf.getManifest();
            assertNotNull(mf, "MANIFEST.MF must be present");
            Attributes attrs = mf.getMainAttributes();
            assertEquals("mixin", attrs.getValue("Fabric-Loom-Mixin-Remap-Type"), "Mixin remap type");
            assertEquals("intermediary", attrs.getValue("Fabric-Mapping-Namespace"), "Mapping namespace");

            // Verify fabric.mod.json
            JsonObject fmj = readJsonEntry(jf, "fabric.mod.json");
            assertEquals("azyron-client", fmj.get("id").getAsString(), "Mod ID");
            assertEquals("AZYRON CLIENT", fmj.get("name").getAsString(), "Client Name");
            JsonObject deps = fmj.getAsJsonObject("depends");
            assertEquals("1.21.11", deps.get("minecraft").getAsString(), "Strict Minecraft 1.21.11 dependency");
            assertEquals(">=21", deps.get("java").getAsString(), "Java 21 dependency");

            // Verify entrypoint class exists in JAR
            assertNotNull(jf.getJarEntry("com/jigneshgaming/azyron/AzyronClient.class"), "Entrypoint class in JAR");

            // Verify mixins & refmap
            JsonObject mixinsJson = readJsonEntry(jf, "azyron-client.mixins.json");
            JsonObject refmapJson = readJsonEntry(jf, "azyron-client.refmap.json");
            JsonObject mappings = refmapJson.getAsJsonObject("mappings");
            var mixinArray = mixinsJson.getAsJsonArray("client");
            assertEquals(7, mixinArray.size(), "All 7 client Mixins registered");

            for (int i = 0; i < mixinArray.size(); i++) {
                String mSimple = mixinArray.get(i).getAsString();
                String internal = "com/jigneshgaming/azyron/mixin/" + mSimple;
                assertNotNull(jf.getJarEntry(internal + ".class"), "Mixin class present: " + internal);
                assertTrue(mappings.has(internal), "Refmap entry present for " + internal);
            }

            // Verify PNG icon & logo headers (128x128 RGBA)
            verifyPng128(jf, "assets/azyron/icon.png");
            verifyPng128(jf, "assets/azyron/textures/gui/logo.png");
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
        num.setFromNormalized(0.25f); // 2.0 + 0.25 * 10 = 4.5
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

        // Reload into fresh managers
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

        // Test Potato Mode
        engine.applyProfile(PerformanceProfile.POTATO, mc, true);
        assertEquals(GraphicsMode.FAST, mc.options.getPreset().getValue(), "Potato FAST graphics");
        assertEquals(ParticlesMode.MINIMAL, mc.options.getParticles().getValue(), "Potato MINIMAL particles");
        assertEquals(CloudRenderMode.OFF, mc.options.getCloudRenderMode().getValue(), "Potato OFF clouds");
        assertEquals(6, mc.options.getViewDistance().getValue(), "Potato view distance 6");
        assertEquals(5, mc.options.getSimulationDistance().getValue(), "Potato sim distance 5");

        // Verify Particle Throttler culls distant particles (>18 blocks) AND enforces 40/tick nearby cap
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

        // Test Smooth FPS Mode
        engine.applyProfile(PerformanceProfile.SMOOTH_FPS, mc, true);
        assertEquals(120, mc.options.getMaxFps().getValue(), "Smooth FPS targets stable 120 FPS");

        // Test Balanced Mode
        engine.applyProfile(PerformanceProfile.BALANCED, mc, true);
        assertEquals(GraphicsMode.FANCY, mc.options.getPreset().getValue(), "Balanced FANCY graphics");
        assertEquals(10, mc.options.getViewDistance().getValue(), "Balanced view distance 10");

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

    private static void testUiAndHudRendering() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-ui-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();
        DrawContext ctx = new DrawContext(854, 480);

        // Verify explicit ARGB alpha channel helper
        assertEquals(0xFF00D4FF, AzyronTheme.ACCENT_ELECTRIC_BLUE, "Electric blue has 0xFF opaque alpha");

        // 1. Loading Screen ("Welcome to AZYRON CLIENT")
        AzyronLoadingRenderer.render(ctx, mc, 0.65f, 1.0f);
        assertTrue(ctx.drawnStrings.contains("Welcome to AZYRON CLIENT"), "Loading screen renders 'Welcome to AZYRON CLIENT'");

        // 2. HUD Overlay + Custom Crosshair (all 5 styles)
        CrosshairModule ch = client.getModuleManager().getCrosshairModule();
        ch.setEnabled(true);
        Setting.ModeSetting styleSetting = (Setting.ModeSetting) ch.getSettings().get(0);
        for (String style : styleSetting.getModes()) {
            styleSetting.setValue(style);
            ch.renderCrosshair(ctx, mc);
        }
        client.getModuleManager().renderHudOverlay(ctx, mc);

        // 3. Main Menu Screen
        ctx.drawnStrings.clear();
        AzyronMainMenuScreen mainMenu = new AzyronMainMenuScreen();
        mainMenu.init(mc, 854, 480);
        mainMenu.render(ctx, 427, 240, 0.016f);
        for (String expectedBtn : new String[]{"AZYRON CLIENT", "Singleplayer", "Multiplayer", "Cosmetics", "Mods", "Settings", "Quit Game", "Discord  •  Join JIGNESH GAMING Community"}) {
            assertTrue(ctx.drawnStrings.contains(expectedBtn), "Main Menu renders '" + expectedBtn + "'");
        }
        mainMenu.handleAction("mods");
        assertTrue(mc.currentScreen instanceof AzyronModsScreen, "Main Menu 'Mods' opens AzyronModsScreen");

        // 4. Right-Shift Mods Screen + Search + Mod Settings + Profiles Tab
        AzyronModsScreen modsScreen = (AzyronModsScreen) mc.currentScreen;
        modsScreen.init(mc, 854, 480);
        modsScreen.render(ctx, 400, 220, 0.016f);
        modsScreen.openModuleSettings(client.getModuleManager().getFpsModule());
        modsScreen.render(ctx, 400, 220, 0.016f);
        modsScreen.openProfilesTab();
        modsScreen.render(ctx, 400, 220, 0.016f);
        modsScreen.keyPressed(new KeyInput(256, 0, 0)); // Escape closes back to parent

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
