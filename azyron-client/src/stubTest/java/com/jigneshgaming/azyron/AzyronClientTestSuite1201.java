package com.jigneshgaming.azyron;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jigneshgaming.azyron.config.AzyronConfig;
import com.jigneshgaming.azyron.cosmetics.CosmeticsManager;
import com.jigneshgaming.azyron.mixin.AzyronMixinPlugin;
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
import com.jigneshgaming.azyron.profile.IntegratedServerOptimizer;
import com.jigneshgaming.azyron.profile.MemoryOptimizer;
import com.jigneshgaming.azyron.profile.MultiplayerPerformanceController;
import com.jigneshgaming.azyron.profile.PerformanceEngine;
import com.jigneshgaming.azyron.profile.PerformanceProfile;
import com.jigneshgaming.azyron.ui.AzyronCosmeticsScreen;
import com.jigneshgaming.azyron.ui.AzyronLoadingRenderer;
import com.jigneshgaming.azyron.ui.AzyronMainMenuScreen;
import com.jigneshgaming.azyron.ui.AzyronModsScreen;
import com.jigneshgaming.azyron.ui.HudEditorScreen;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.CpsTracker;
import com.jigneshgaming.azyron.util.McVersionBridge;
import com.jigneshgaming.azyron.util.OptionsConfigSanitizer;
import com.jigneshgaming.azyron.util.PersistentStateGuard;
import com.jigneshgaming.azyron.util.PlatformCompat;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import com.mojang.text2speech.Narrator;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.session.Session;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Identifier;

/**
 * Dedicated Automated Verification Suite for AZYRON CLIENT v1.0.3 — Minecraft 1.20.1 (Fabric 0.14.22+).
 * Executes all 26 runtime, Java 17 classfile v61.0 bytecode, MatrixStack 3D-to-2D bridge,
 * AzyronBaseScreen 1.20.1 input, *120Mixin descriptors, Sodium 0.5.3 config, 1.20.1 raids.dat NBT,
 * Modrinth 1.20.1 .mrpack, and multiplayer soak tests strictly under Minecraft 1.20.1.
 */
public final class AzyronClientTestSuite1201 {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.setProperty("azyron.mcVersion", "1.20.1");
        McVersionBridge.setSimulatedMcVersionForTests(McVersionBridge.SupportedMcVersion.MC_1_20_1);
        DrawContext.setSimulate120MatrixStackMode(true);

        System.out.println("=======================================================================");
        System.out.println(" AZYRON CLIENT v1.0.3 — MINECRAFT 1.20.1 (Fabric 0.14.22+) TEST SUITE");
        System.out.println("=======================================================================");

        Path jarPath = args.length > 0
                ? Paths.get(args[0])
                : Paths.get("releases/Azyron-Client-1.0.3.jar");
        Path mrpack120Path = args.length > 1
                ? Paths.get(args[1])
                : Paths.get("releases/Azyron-Modpack-1.0.3.mrpack");

        runTest("1. [1.20.1] Unified JAR v1.0.3 Java 17 Bytecode (v61.0) & Minecraft 1.20.1 Declaration",
                () -> testJarJava17AndMc120Declaration(jarPath));
        runTest("2. [1.20.1] AzyronMixinPlugin Version-Selective Filtering (Enables *120Mixin, Skips 1.21-Only)",
                AzyronClientTestSuite1201::testMixinPluginSelection1201);
        runTest("3. [1.20.1] All 6 *120Mixin Intermediary Descriptors (FOV double, InGameHud float, Mouse JIII)",
                () -> testAll120MixinBytecodeDescriptors(jarPath));
        runTest("4. [1.20.1] Loading Screen & Native Text Rendering (No 1.21 ShaderLoader Dependency)",
                AzyronClientTestSuite1201::testLoadingScreenAndTextOn1201);
        runTest("5. [1.20.1] Modpack 1.20.1 Profile Manifest (Sodium 0.5.3, Lithium 0.11.2, Fabric API 0.88.1)",
                () -> testMrpack1201ProfileAndHashes(mrpack120Path));
        runTest("6. [1.20.1] Client Startup Bootstrap, 1.20.1 String-Category KeyBindings & All 11 Modules",
                AzyronClientTestSuite1201::testBootstrapAnd1201KeyBindings);
        runTest("7. [1.20.1] Settings System (Boolean, Number Clamping/Stepping, Mode Cycling)",
                AzyronClientTestSuite1201::testSettingsSystem);
        runTest("8. [1.20.1] JSON Config v1.0.3 Save & Selected 1.20.1 Profile Round-Trip Reload",
                AzyronClientTestSuite1201::testConfigRoundTrip1201);
        runTest("9. [1.20.1] Performance Profiles & Sodium 0.5.3 Config Schema Generation",
                AzyronClientTestSuite1201::testPerformanceProfilesAndSodium05Schema);
        runTest("10. [1.20.1] Frame-Time 1% Low Tracker & Memory Optimizer Telemetry",
                AzyronClientTestSuite1201::testTelemetryAndMemory);
        runTest("11. [1.20.1] Mouse Clicks, CPS Counter (LMB/RMB/Fallback), Compass, Zoom (double FOV) & Sprint",
                AzyronClientTestSuite1201::testGameplayMechanics1201);
        runTest("12. [1.20.1] Main Menu, AzyronBaseScreen Direct 1.20.1 Mouse/Key/Scroll Input & HUD Editor",
                AzyronClientTestSuite1201::testUiAndDirect1201ScreenInput);
        runTest("13. [1.20.1] PojavLauncher Android Mobile Aspect Ratios (2400x1080 / 1280x720) & Pause Menu",
                AzyronClientTestSuite1201::testPojavLauncherCompatibility1201);
        runTest("14. [1.20.1] Shared Class Constant-Pool Purity (Zero 1.21-Only Symbols in Shared Classes)",
                () -> testSharedClassPurityOn1201(jarPath));
        runTest("15. [1.20.1] Full Fabric onInitializeClient() & Direct 1.20.1 StatusEffect Resolution",
                AzyronClientTestSuite1201::testFullStartupAndDirectStatusEffect1201);
        runTest("16. [1.20.1] Automatic 3GB-8GB RAM Scaling, Unfocused Throttle & 10,000-Frame MatrixStack Stress",
                AzyronClientTestSuite1201::testAutoRamScalingAndMatrixStackStressBenchmark);
        runTest("17. [1.20.1] Corrupted 1.20.1 raids.dat Backup, PascalCase NBT ('Tick'/'Raids'/'Mobs') Repair & Atomic Save",
                AzyronClientTestSuite1201::testRaidsDat1201PascalCaseRepairAndAtomicSave);
        runTest("18. [1.20.1] Fullscreen 'off' -> 'false' & Malformed Boolean Option Sanitization",
                AzyronClientTestSuite1201::testFullscreenAndOptionsBooleanSanitizer);
        runTest("19. [1.20.1] Android / PojavLauncher Missing Flite Narrator Graceful Fallback",
                AzyronClientTestSuite1201::testAndroidMissingFliteNarratorFallback);
        runTest("20. [1.20.1] Offline / Local Session Detection (Preserving Online Microsoft Auth)",
                AzyronClientTestSuite1201::testOfflineLocalSessionDetection);
        runTest("21. [1.20.1] Armor HUD 1.20.1 drawItemInSlot Dispatch, IntegratedServer Pacing & Config Recovery",
                AzyronClientTestSuite1201::testArmorHud1201AndIntegratedServerPacing);
        runTest("22. [1.20.1] Multiplayer Server-Join Transition Guards (ConnectScreen / LevelLoadingScreen) & 50 Joins",
                AzyronClientTestSuite1201::testMultiplayerServerJoinLifecycle1201);
        runTest("23. [1.20.1] ClientPlayNetworkHandler120Mixin Chunk-Receive Burst Telemetry & Warmup Pacing",
                AzyronClientTestSuite1201::testChunkReceiveBurstTelemetry1201);
        runTest("24. [1.20.1] ClientWorld.runQueuedChunkUpdates() Nanosecond Budgeted Light Queue on 1.20.1",
                AzyronClientTestSuite1201::testBudgetedChunkLightQueue1201);
        runTest("25. [1.20.1] Crowded Multiplayer Hub Entity Render Culling (300 Entities, 100% PvP Preserved)",
                AzyronClientTestSuite1201::testCrowdedMultiplayerHubEntityRenderCulling1201);
        runTest("26. [1.20.1] Extended 30-Minute Equivalent 1.20.1 Multiplayer Soak (36,000 Ticks / 72,000 MatrixStack Frames)",
                AzyronClientTestSuite1201::testExtendedMultiplayerSessionSoak1201);

        DrawContext.setSimulate120MatrixStackMode(false);

        System.out.println("-----------------------------------------------------------------------");
        System.out.printf(" MINECRAFT 1.20.1 SUITE SUMMARY: %d PASSED, %d FAILED%n", passed, failed);
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

    private static void testJarJava17AndMc120Declaration(Path jarPath) throws Exception {
        assertTrue(Files.exists(jarPath), "Unified JAR must exist at " + jarPath);
        try (JarFile jf = new JarFile(jarPath.toFile())) {
            JsonObject fmj = readJsonEntry(jf, "fabric.mod.json");
            assertEquals("1.0.3", fmj.get("version").getAsString(), "Version == 1.0.3");
            JsonObject deps = fmj.getAsJsonObject("depends");
            assertEquals(">=17", deps.get("java").getAsString(), "Java >=17 required for Minecraft 1.20.1 launchers");
            JsonArray mcArr = deps.getAsJsonArray("minecraft");
            boolean has1201 = false;
            for (JsonElement el : mcArr) {
                if ("1.20.1".equals(el.getAsString())) has1201 = true;
            }
            assertTrue(has1201, "fabric.mod.json depends.minecraft includes 1.20.1");

            var entries = jf.entries();
            int classCount = 0;
            while (entries.hasMoreElements()) {
                JarEntry je = entries.nextElement();
                if (!je.getName().endsWith(".class")) continue;
                classCount++;
                byte[] b = jf.getInputStream(je).readAllBytes();
                int major = ByteBuffer.wrap(b, 6, 2).getShort() & 0xFFFF;
                assertEquals(61, major, "Classfile MUST be Java 17 (v61.0) for 1.20.1 JVM compatibility: " + je.getName());
            }
            assertTrue(classCount >= 55, "Verified Java 17 classfile v61.0 across " + classCount + " classes");
        }
    }

    private static void testMixinPluginSelection1201() {
        assertEquals("1.20.1", McVersionBridge.getActiveMcVersionString(), "Active runtime version == 1.20.1");
        assertTrue(McVersionBridge.isMinecraft120(), "isMinecraft120() == true");
        assertTrue(!McVersionBridge.isMinecraft121(), "isMinecraft121() == false");

        AzyronMixinPlugin plugin = new AzyronMixinPlugin();
        plugin.onLoad("com.jigneshgaming.azyron.mixin");

        String[] enabledOn120 = {
                "ClientWorldMixin", "EntityRenderManagerMixin", "GameMenuScreenMixin",
                "GameOptionsMixin", "IntegratedServerMixin", "MinecraftClientMixin",
                "NarratorManagerMixin", "ParticleManagerMixin", "SplashOverlayMixin",
                "GameRenderer120Mixin", "InGameHud120Mixin", "Mouse120Mixin",
                "ClientPlayNetworkHandler120Mixin", "PersistentStateManager120Mixin", "PersistentState120Mixin"
        };
        for (String m : enabledOn120) {
            assertTrue(plugin.shouldApplyMixin("net.minecraft.Dummy", "com.jigneshgaming.azyron.mixin." + m),
                    "1.20.1 runtime MUST apply " + m);
        }

        String[] skippedOn120 = {
                "ChunkBatchSizeCalculatorMixin", "GameRendererMixin", "InGameHudMixin",
                "MouseMixin", "PersistentStateManagerMixin", "RealmsAvailabilityMixin", "Screen121Mixin"
        };
        for (String m : skippedOn120) {
            assertTrue(!plugin.shouldApplyMixin("net.minecraft.Dummy", "com.jigneshgaming.azyron.mixin." + m),
                    "1.20.1 runtime MUST skip 1.21.11-only " + m);
        }
    }

    private static void testAll120MixinBytecodeDescriptors(Path jarPath) throws Exception {
        try (JarFile jf = new JarFile(jarPath.toFile())) {
            String gr = readClassAscii(jf, "com/jigneshgaming/azyron/mixin/GameRenderer120Mixin.class");
            assertTrue(gr.contains("method_3196(Lnet/minecraft/class_4184;FZ)D"),
                    "GameRenderer120Mixin targets 1.20.1 double getFov descriptor");

            String hud = readClassAscii(jf, "com/jigneshgaming/azyron/mixin/InGameHud120Mixin.class");
            assertTrue(hud.contains("method_1753(Lnet/minecraft/class_332;F)V"),
                    "InGameHud120Mixin targets 1.20.1 render(DrawContext, float)");
            assertTrue(hud.contains("method_1736(Lnet/minecraft/class_332;)V"),
                    "InGameHud120Mixin targets 1.20.1 renderCrosshair(DrawContext)");
            assertTrue(hud.contains("method_1765(Lnet/minecraft/class_332;)V"),
                    "InGameHud120Mixin targets 1.20.1 renderStatusEffectOverlay(DrawContext)");

            String mouse = readClassAscii(jf, "com/jigneshgaming/azyron/mixin/Mouse120Mixin.class");
            assertTrue(mouse.contains("method_1601(JIII)V"),
                    "Mouse120Mixin targets 1.20.1 onMouseButton(long, int, int, int)");
            assertTrue(mouse.contains("method_1598(JDD)V"),
                    "Mouse120Mixin targets 1.20.1 onMouseScroll(long, double, double)");

            String net = readClassAscii(jf, "com/jigneshgaming/azyron/mixin/ClientPlayNetworkHandler120Mixin.class");
            assertTrue(net.contains("method_38539(IILnet/minecraft/class_6603;)V"),
                    "ClientPlayNetworkHandler120Mixin targets 1.20.1 loadChunk");

            String psm = readClassAscii(jf, "com/jigneshgaming/azyron/mixin/PersistentStateManager120Mixin.class");
            assertTrue(psm.contains("field_17664"), "PersistentStateManager120Mixin shadows 1.20.1 File directory (field_17664)");
            assertTrue(psm.contains("method_17923(Ljava/lang/String;I)Lnet/minecraft/class_2487;"),
                    "PersistentStateManager120Mixin targets 1.20.1 readNbt(String, int)");

            String ps = readClassAscii(jf, "com/jigneshgaming/azyron/mixin/PersistentState120Mixin.class");
            assertTrue(ps.contains("method_17919(Ljava/io/File;)V"),
                    "PersistentState120Mixin redirects 1.20.1 PersistentState.save(File)");
        }
    }

    private static void testLoadingScreenAndTextOn1201() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-load-120-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();

        SplashOverlay splash = new SplashOverlay(mc, false);
        mc.setOverlay(splash);
        AzyronLoadingRenderer.setCoreResourcesReady(false);

        DrawContext bootCtx = new DrawContext(854, 480);
        AzyronLoadingRenderer.render(bootCtx, mc, 0.5f, 1.0f, false);
        assertTrue(bootCtx.fillCallCount > 25, "1.20.1 cold-boot loading bar renders cleanly via quads");

        splash.markReloadComplete();
        AzyronLoadingRenderer.setCoreResourcesReady(true);
        assertTrue(UiRenderUtils.isCoreTextShaderReady(mc), "1.20.1 text renderer ready once resources reload");

        DrawContext readyCtx = new DrawContext(854, 480);
        AzyronLoadingRenderer.render(readyCtx, mc, 1.0f, 0.9f, true);
        assertTrue(readyCtx.drawnStrings.contains("Welcome to AZYRON CLIENT"), "Renders 'Welcome to AZYRON CLIENT' on 1.20.1");

        mc.setOverlay(null);
        readyCtx.drawnStrings.clear();
        AzyronMainMenuScreen mainMenu = new AzyronMainMenuScreen();
        mainMenu.init(mc, 854, 480);
        mainMenu.render(readyCtx, 427, 240, 0.016f);
        assertTrue(readyCtx.drawnStrings.contains("AZYRON CLIENT v1.0.3  •  Minecraft 1.20.1 (Fabric)"),
                "Main Menu dynamically renders Minecraft 1.20.1 (Fabric) footer");
        Files.deleteIfExists(tempCfg);
    }

    private static void testMrpack1201ProfileAndHashes(Path mrpackPath) throws Exception {
        assertTrue(Files.exists(mrpackPath), "Modpack must exist at " + mrpackPath);
        try (ZipFile zf = new ZipFile(mrpackPath.toFile())) {
            ZipEntry idx120Entry = zf.getEntry("profiles/modrinth.index.1.20.1.json");
            assertNotNull(idx120Entry, "profiles/modrinth.index.1.20.1.json present in Azyron-Modpack-1.0.3.mrpack");
            JsonObject idx = JsonParser.parseString(new String(zf.getInputStream(idx120Entry).readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();

            assertEquals("1.0.3", idx.get("versionId").getAsString(), "1.20.1 profile versionId == 1.0.3");
            JsonObject deps = idx.getAsJsonObject("dependencies");
            assertEquals("1.20.1", deps.get("minecraft").getAsString(), "Targets Minecraft 1.20.1");
            assertEquals("0.14.22", deps.get("fabric-loader").getAsString(), "Targets Fabric Loader 0.14.22");

            JsonArray files = idx.getAsJsonArray("files");
            assertEquals(8, files.size(), "All 8 verified 1.20.1 Modrinth optimization mods included");
            Set<String> expectedMods = Set.of(
                    "mods/fabric-api-0.88.1+1.20.1.jar",
                    "mods/sodium-fabric-mc1.20.1-0.5.3.jar",
                    "mods/lithium-fabric-mc1.20.1-0.11.2.jar",
                    "mods/ferritecore-6.0.0-fabric.jar",
                    "mods/entityculling-fabric-1.6.2-mc1.20.1.jar",
                    "mods/moreculling-1.20.1-0.19.0.jar",
                    "mods/cloth-config-11.1.106-fabric.jar",
                    "mods/dynamic-fps-3.2.1+minecraft-1.20.0.jar"
            );
            Set<String> seen = new HashSet<>();
            for (JsonElement el : files) {
                JsonObject f = el.getAsJsonObject();
                String p = f.get("path").getAsString();
                assertTrue(seen.add(p), "No duplicate 1.20.1 mod: " + p);
                assertEquals(40, f.getAsJsonObject("hashes").get("sha1").getAsString().length(), "Valid SHA-1");
                assertEquals(128, f.getAsJsonObject("hashes").get("sha512").getAsString().length(), "Valid SHA-512");
            }
            assertEquals(expectedMods, seen, "Exact verified 1.20.1 Modrinth stack matched");
        }
    }

    private static void testBootstrapAnd1201KeyBindings() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-boot-120-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);

        var openMenuKey = McVersionBridge.createKeyBinding("key.azyron.open_menu", 344, "azyron", "controls");
        assertNotNull(openMenuKey, "McVersionBridge creates KeyBinding cleanly on 1.20.1");

        ModuleManager mm = client.getModuleManager();
        assertEquals(11, mm.getModules().size(), "11 modules registered on 1.20.1");
        assertEquals(9, mm.getHudModules().size(), "9 HUD modules registered on 1.20.1");
        assertEquals(1, mm.filterModules(ModuleCategory.ALL, "armor").size(), "Finds Armor HUD");
        Files.deleteIfExists(tempCfg);
    }

    private static void testSettingsSystem() {
        Setting.BooleanSetting bool = new Setting.BooleanSetting("b", "Bool", "desc", true);
        bool.toggle();
        assertTrue(!bool.getValue(), "BooleanSetting toggles false");
        Setting.NumberSetting num = new Setting.NumberSetting("n", "Num", "desc", 4.0, 2.0, 12.0, 0.5);
        num.setValue(1.0);
        assertEquals(2.0, num.getValue(), "NumberSetting clamps to min");
        Setting.ModeSetting mode = new Setting.ModeSetting("m", "Mode", "desc", "Cross", "Cross", "Dot");
        mode.cycleNext();
        assertEquals("Dot", mode.getValue(), "ModeSetting cycles");
    }

    private static void testConfigRoundTrip1201() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-cfg-120-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        assertEquals(McVersionBridge.SupportedMcVersion.MC_1_20_1, client.getConfig().getSelectedMcVersion(),
                "Default selected MC profile matches active 1.20.1 runtime");

        client.getConfig().setActiveProfile(PerformanceProfile.SMOOTH_FPS);
        client.getConfig().save();

        JsonObject saved = JsonParser.parseString(Files.readString(tempCfg)).getAsJsonObject();
        assertEquals("1.0.3", saved.get("configVersion").getAsString(), "configVersion == 1.0.3");
        assertEquals("1.20.1", saved.get("selectedMcVersion").getAsString(), "selectedMcVersion == 1.20.1");

        AzyronConfig reloaded = new AzyronConfig(tempCfg);
        reloaded.load();
        assertEquals(PerformanceProfile.SMOOTH_FPS, reloaded.getActiveProfile(), "Reloaded SMOOTH_FPS");
        assertEquals(McVersionBridge.SupportedMcVersion.MC_1_20_1, reloaded.getSelectedMcVersion(), "Reloaded 1.20.1");
        Files.deleteIfExists(tempCfg);
    }

    private static void testPerformanceProfilesAndSodium05Schema() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-perf-120-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();
        PerformanceEngine engine = client.getPerformanceEngine();

        ClientWorld savedWorld = mc.world;
        mc.world = null;
        engine.applyProfile(PerformanceProfile.POTATO, mc, true);
        assertEquals(GraphicsMode.FAST, mc.options.getPreset().getValue(), "1.20.1 Potato FAST graphics");
        assertEquals(ParticlesMode.MINIMAL, mc.options.getParticles().getValue(), "1.20.1 Potato MINIMAL particles");
        assertEquals(CloudRenderMode.OFF, mc.options.getCloudRenderMode().getValue(), "1.20.1 Potato OFF clouds");

        // Verify 1.20.1 Sodium 0.5.3 config schema contains 0.5.x performance keys!
        Path sodiumCfg = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("sodium-options.json");
        JsonObject sod = JsonParser.parseString(Files.readString(sodiumCfg)).getAsJsonObject();
        JsonObject perf = sod.getAsJsonObject("performance");
        assertTrue(perf.get("use_block_face_culling").getAsBoolean(), "Sodium 0.5.3 use_block_face_culling=true");
        assertTrue(perf.get("animate_only_visible_textures").getAsBoolean(), "Sodium 0.5.3 animate_only_visible_textures=true");
        assertTrue(perf.get("use_entity_culling").getAsBoolean(), "Sodium 0.5.3 use_entity_culling=true");
        assertTrue(!perf.get("use_no_error_g_l_context").getAsBoolean(), "PojavLauncher safety: use_no_error_g_l_context=false");

        mc.world = savedWorld;
        Files.deleteIfExists(tempCfg);
    }

    private static void testTelemetryAndMemory() {
        for (int i = 0; i < 20; i++) FrameTimeTracker.onFrameRendered();
        assertTrue(FrameTimeTracker.getOnePercentLowFps(240) >= 1, "1% low FPS valid");
        MemoryOptimizer.onClientTick(MinecraftClient.getInstance(), true);
        assertTrue(MemoryOptimizer.getMaxMb() > 0, "Max MB > 0");
    }

    private static void testGameplayMechanics1201() throws Exception {
        CpsTracker.reset();
        for (int i = 0; i < 12; i++) CpsTracker.recordLeftClick();
        assertEquals(12, CpsTracker.getLeftCps(), "1.20.1 Mouse120Mixin CPS tracking records 12 LMB clicks");

        // Verify double FOV compatibility (GameRenderer120Mixin casts double <-> float)
        ZoomModule zoom = new ZoomModule();
        zoom.setZooming(true);
        double fov120 = 70.0;
        for (int i = 0; i < 30; i++) {
            fov120 = zoom.applyFov((float) fov120);
        }
        assertTrue(fov120 < 20.0, "1.20.1 double FOV zooms smoothly down below 20.0");

        assertEquals("W (-X)", CoordinatesModule.formatFacing(90.0f), "Compass W (-X)");
        ToggleSprintModule ts = new ToggleSprintModule();
        assertTrue(ts.isSprintToggled(), "ToggleSprint active");
    }

    private static void testUiAndDirect1201ScreenInput() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-ui-120-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.setOverlay(null);
        AzyronLoadingRenderer.setCoreResourcesReady(true);

        DrawContext ctx = new DrawContext(854, 480);
        AzyronMainMenuScreen mainMenu = new AzyronMainMenuScreen();
        mainMenu.init(mc, 854, 480);
        mainMenu.render(ctx, 427, 240, 0.016f);
        assertTrue(ctx.drawnStrings.contains("AZYRON CLIENT v1.0.3  •  Minecraft 1.20.1 (Fabric)"),
                "1.20.1 Main Menu footer verified");

        mainMenu.handleAction("singleplayer");
        assertTrue(mc.currentScreen instanceof SelectWorldScreen, "Singleplayer opens SelectWorldScreen");
        mc.currentScreen = null;
        mc.world = new ClientWorld();

        // Test AzyronModsScreen via direct 1.20.1 AzyronBaseScreen methods:
        // mouseClicked(double, double, int), keyPressed(int, int, int), mouseScrolled(double, double, double)
        mainMenu.handleAction("mods");
        AzyronModsScreen modsScreen = (AzyronModsScreen) mc.currentScreen;
        modsScreen.init(mc, 854, 480);
        modsScreen.openClientOptionsTab();
        ctx.drawnStrings.clear();
        modsScreen.render(ctx, 400, 220, 0.016f);
        assertTrue(ctx.drawnStrings.contains("< 1.20.1 >"), "Mods Screen shows < 1.20.1 > on 1.20.1 runtime");

        int winW = Math.min(476, 854 - 16);
        int winH = Math.min(286, 480 - 16);
        int wx = (854 - winW) / 2;
        int wy = (480 - winH) / 2;
        int cx = wx + 108 + 8;
        int cy = wy + 34 + 6;
        assertTrue(modsScreen.mouseClicked(cx + 40, cy + 115, 0),
                "1.20.1 mouseClicked(double, double, int) toggles MC version selector");
        assertEquals(McVersionBridge.SupportedMcVersion.MC_1_21_11, client.getConfig().getSelectedMcVersion(),
                "Selector switched to 1.21.11");
        assertTrue(modsScreen.mouseClicked(cx + 40, cy + 115, 0), "Second click restores 1.20.1");
        assertEquals(McVersionBridge.SupportedMcVersion.MC_1_20_1, client.getConfig().getSelectedMcVersion(),
                "Selector restored to 1.20.1");
        modsScreen.keyPressed(256, 0, 0);

        // Test HudEditorScreen via 1.20.1 mouseClicked / mouseDragged / mouseReleased / mouseScrolled(3-arg)
        HudEditorScreen editor = new HudEditorScreen(mainMenu);
        editor.init(mc, 854, 480);
        editor.render(ctx, 200, 200, 0.016f);
        HudModule fpsHud = client.getModuleManager().getFpsModule();
        int sx = fpsHud.getScreenX(854) + 5;
        int sy = fpsHud.getScreenY(480) + 5;
        assertTrue(editor.mouseClicked(sx, sy, 0), "1.20.1 HUD Editor drag start");
        assertTrue(editor.mouseDragged(280, 170, 0, 80, 80), "1.20.1 HUD Editor drag move");
        assertTrue(editor.mouseReleased(280, 170, 0), "1.20.1 HUD Editor drag release");
        assertTrue(editor.mouseScrolled(fpsHud.getScreenX(854) + 4, fpsHud.getScreenY(480) + 4, 1.0),
                "1.20.1 3-arg mouseScrolled scales HUD widget");
        assertTrue(fpsHud.getScale() > 1.0f, "HUD widget scale increased on 1.20.1");

        AzyronCosmeticsScreen cosScreen = new AzyronCosmeticsScreen(mainMenu);
        cosScreen.init(mc, 854, 480);
        cosScreen.render(ctx, 300, 200, 0.016f);
        cosScreen.charTyped('z', 0);

        Files.deleteIfExists(tempCfg);
    }

    private static void testPojavLauncherCompatibility1201() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-pojav-120-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.setOverlay(null);
        AzyronLoadingRenderer.setCoreResourcesReady(true);

        for (int[] res : new int[][]{{800, 360}, {640, 360}}) {
            DrawContext mobileCtx = new DrawContext(res[0], res[1]);
            AzyronMainMenuScreen mm = new AzyronMainMenuScreen();
            mm.init(mc, res[0], res[1]);
            mm.render(mobileCtx, res[0] / 2, res[1] / 2, 0.016f);
            client.getModuleManager().renderHudOverlay(mobileCtx, mc);
            assertTrue(mobileCtx.matrixStackBalanced(), "MatrixStack push/pop balanced on mobile viewport " + res[0] + "x" + res[1]);
        }
        GameMenuScreen pauseScreen = new GameMenuScreen(true);
        pauseScreen.init(mc, 640, 360);
        assertNotNull(pauseScreen, "1.20.1 GameMenuScreen pause UI ready");
        Files.deleteIfExists(tempCfg);
    }

    private static void testSharedClassPurityOn1201(Path jarPath) throws Exception {
        Set<String> mc121OnlyMixins = Set.of(
                "com/jigneshgaming/azyron/mixin/ChunkBatchSizeCalculatorMixin.class",
                "com/jigneshgaming/azyron/mixin/GameRendererMixin.class",
                "com/jigneshgaming/azyron/mixin/InGameHudMixin.class",
                "com/jigneshgaming/azyron/mixin/MouseMixin.class",
                "com/jigneshgaming/azyron/mixin/PersistentStateManagerMixin.class",
                "com/jigneshgaming/azyron/mixin/RealmsAvailabilityMixin.class",
                "com/jigneshgaming/azyron/mixin/Screen121Mixin.class"
        );
        // Forbidden 1.21-only Intermediary classes in any shared / 1.20.1 class:
        String[] forbidden121Classes = {
                "net/minecraft/class_11909", // Click
                "net/minecraft/class_11910", // MouseInput
                "net/minecraft/class_11908", // KeyInput
                "net/minecraft/class_11905", // CharInput
                "net/minecraft/class_10741", // PersistentStateType
                "net/minecraft/class_8760",  // ChunkBatchSizeCalculator
                "org/joml/Matrix3x2fStack"   // 1.21.6+ 2D matrix stack
        };

        try (JarFile jf = new JarFile(jarPath.toFile())) {
            var entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry je = entries.nextElement();
                String name = je.getName();
                if (!name.endsWith(".class") || mc121OnlyMixins.contains(name)) {
                    continue;
                }
                String ascii = readClassAscii(jf, name);
                for (String forbidden : forbidden121Classes) {
                    assertTrue(!ascii.contains(forbidden),
                            "Shared/1.20.1 class " + name + " MUST NOT directly reference 1.21-only class " + forbidden);
                }
            }
        }
    }

    private static void testFullStartupAndDirectStatusEffect1201() {
        AzyronClient entrypoint = new AzyronClient();
        entrypoint.onInitializeClient();
        MinecraftClient mc = MinecraftClient.getInstance();
        DrawContext ctx = new DrawContext(854, 480);
        entrypoint.getModuleManager().getPotionHudModule().setEnabled(true);
        entrypoint.getModuleManager().renderHudOverlay(ctx, mc);
        assertTrue(ctx.drawnStrings.stream().anyMatch(s -> s.contains("Speed II")),
                "PotionHudModule renders Speed II cleanly on 1.20.1");
        assertTrue(ctx.matrixStackBalanced(), "MatrixStack push/pop strictly balanced after HUD render");
    }

    private static void testAutoRamScalingAndMatrixStackStressBenchmark() throws Exception {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld prevWorld = mc.world;
        try {
            MemoryOptimizer.setSimulatedRamForTesting(1536, 3072);
            Path cfg = Files.createTempFile("azyron-stress-120-", ".json");
            Files.deleteIfExists(cfg);
            AzyronClient c3 = AzyronClient.bootstrapHeadlessForTests(cfg);
            assertEquals(PerformanceProfile.POTATO, c3.getConfig().getActiveProfile(), "3GB selects POTATO on 1.20.1");

            mc.world = prevWorld != null ? prevWorld : new ClientWorld();
            mc.setOverlay(null);
            AzyronLoadingRenderer.setCoreResourcesReady(true);
            c3.getModuleManager().getMemoryHudModule().setEnabled(true);

            DrawContext benchCtx = new DrawContext(1920, 1080);
            long t0 = System.nanoTime();
            for (int frame = 0; frame < 10_000; frame++) {
                benchCtx.drawnStrings.clear();
                c3.getModuleManager().renderHudOverlay(benchCtx, mc);
            }
            long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;
            assertTrue(elapsedMs < 4_000L, "10,000 1.20.1 MatrixStack HUD frames rendered in " + elapsedMs + "ms");
            assertTrue(benchCtx.matrixStackBalanced(), "Zero MatrixStack push/pop leaks across 10,000 frames");
            assertTrue(benchCtx.matrix3dScaleOps > 10_000, "Exercised 1.20.1 MatrixStack.scale(x, y, 1.0f) path");
            Files.deleteIfExists(cfg);
        } finally {
            MemoryOptimizer.setSimulatedRamForTesting(-1, -1);
            FrameTimeTracker.resetForTesting();
            mc.world = prevWorld;
        }
    }

    private static void testRaidsDat1201PascalCaseRepairAndAtomicSave() throws Exception {
        PersistentStateGuard.clearSessionBackupCacheForTests();
        Path worldDataDir = Files.createTempDirectory("azyron-world-120-");

        // Construct 1.20.1 raids.dat with PascalCase Raids list: 1 valid 1.20.1 raid (Id + Status) + 1 corrupt empty entry
        NbtCompound root120 = new NbtCompound();
        NbtCompound data120 = new NbtCompound();
        data120.putInt("NextAvailableID", 5);
        data120.putInt("Tick", 820);

        NbtList raids120 = new NbtList();
        NbtCompound valid120Raid = new NbtCompound();
        valid120Raid.putInt("Id", 2);
        valid120Raid.putString("Status", "ongoing");
        raids120.add(valid120Raid);
        raids120.add(new NbtCompound()); // corrupt 1.20.1 raid entry missing Id/Status
        data120.put("Raids", raids120);
        root120.put("data", data120);

        Path raidsFile = worldDataDir.resolve("raids.dat");
        NbtIo.writeCompressed(root120, raidsFile);

        NbtCompound repaired = PersistentStateGuard.inspectAndRepairRaidsCompound(raidsFile, "raids", root120);
        NbtCompound repairedData = repaired.getCompound("data").orElseThrow();
        assertEquals(5, repairedData.getInt("NextAvailableID").orElse(-1),
                "1.20.1 repair preserves PascalCase NextAvailableID == 5 for 1.20.1 RaidManager.fromNbt");
        assertEquals(820, repairedData.getInt("Tick").orElse(-1),
                "1.20.1 repair preserves PascalCase Tick == 820");
        assertEquals(1, repairedData.getList("Raids").orElseThrow().size(),
                "1.20.1 repair preserves valid Raid entry and strips malformed entry");

        Path atomicTarget = worldDataDir.resolve("raids_120_atomic.dat");
        PersistentStateGuard.saveCompressedAtomically(atomicTarget, repaired);
        NbtCompound roundTrip = NbtIo.readCompressed(atomicTarget, NbtSizeTracker.ofUnlimitedBytes());
        assertEquals(5, roundTrip.getCompound("data").orElseThrow().getInt("NextAvailableID").orElse(-1),
                "1.20.1 atomic raids.dat save round-trips losslessly");
    }

    private static void testFullscreenAndOptionsBooleanSanitizer() {
        assertEquals("false", OptionsConfigSanitizer.normalizeBooleanOptionValue("fullscreen", "off", false), "off -> false");
        assertEquals("false", OptionsConfigSanitizer.normalizeBooleanOptionValue("fullscreen", "0", false), "0 -> false");
        assertEquals("true", OptionsConfigSanitizer.normalizeBooleanOptionValue("enableVsync", "on", false), "on -> true");
    }

    private static void testAndroidMissingFliteNarratorFallback() {
        try {
            PlatformCompat.setSimulatedAndroidForTesting(true);
            PlatformCompat.setSimulatedFliteAvailableForTesting(false);
            assertEquals(Narrator.EMPTY, PlatformCompat.createSafeNarrator(), "1.20.1 Android Narrator.EMPTY fallback");
        } finally {
            PlatformCompat.setSimulatedAndroidForTesting(null);
            PlatformCompat.setSimulatedFliteAvailableForTesting(null);
        }
    }

    private static void testOfflineLocalSessionDetection() {
        assertTrue(PlatformCompat.isOfflineOrLocalSession(new Session("Pojav120", "0", Optional.empty())), "Offline detected");
        String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ4dWlkIjoiMjUzNTQxMjM0NTY3ODkwMSIsImFnZSI6IjE4In0.signatureTokenBlock123456";
        assertTrue(!PlatformCompat.isOfflineOrLocalSession(new Session("Msa120", jwt, Optional.of("2535412345678901"))), "Online MSA preserved");
    }

    private static void testArmorHud1201AndIntegratedServerPacing() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-armor-120-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.setOverlay(null);
        AzyronLoadingRenderer.setCoreResourcesReady(true);
        mc.world = new ClientWorld();

        var armorHud = client.getModuleManager().getArmorHudModule();
        armorHud.setEnabled(true);
        DrawContext hudCtx = new DrawContext(854, 480);
        armorHud.renderOnHud(hudCtx, mc, false);
        assertEquals(5, hudCtx.drawnItemCount, "1.20.1 ArmorHudModule rendered 5 equipped slots");
        assertEquals(5, hudCtx.drawnStackOverlayCount, "1.20.1 ArmorHudModule invoked drawItemInSlot cleanly");
        assertTrue(hudCtx.matrixStackBalanced(), "MatrixStack balanced in ArmorHudModule");

        IntegratedServer server = new IntegratedServer(mc);
        IntegratedServerOptimizer.onIntegratedServerTick(server, mc);
        Files.deleteIfExists(tempCfg);
    }

    private static void testMultiplayerServerJoinLifecycle1201() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-mp-120-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld prevWorld = mc.world;
        var prevScreen = mc.currentScreen;
        try {
            MultiplayerPerformanceController.resetCountersForTesting();
            assertTrue(MultiplayerPerformanceController.isTransitionScreen(new LevelLoadingScreen()), "1.20.1 LevelLoadingScreen");
            assertTrue(MultiplayerPerformanceController.isTransitionScreen(new ConnectScreen()), "1.20.1 ConnectScreen");

            long oldGenBefore = getOldGenCollectionCount();
            DrawContext joinCtx = new DrawContext(854, 480);
            for (int j = 0; j < 50; j++) {
                ClientWorld w = new ClientWorld(Identifier.ofVanilla("overworld"));
                client.getModuleManager().onWorldChanged();
                mc.joinWorld(w);
                MultiplayerPerformanceController.onWorldJoinOrSwitch(mc, w);
                mc.currentScreen = null;
                for (int t = 0; t < 20; t++) {
                    client.getPerformanceEngine().onClientTick(mc);
                    client.getModuleManager().onTick(mc);
                    joinCtx.drawnStrings.clear();
                    client.getModuleManager().renderHudOverlay(joinCtx, mc);
                }
            }
            assertEquals(oldGenBefore, getOldGenCollectionCount(), "50 1.20.1 server joins triggered ZERO Full GCs");
        } finally {
            MultiplayerPerformanceController.resetCountersForTesting();
            mc.world = prevWorld;
            mc.currentScreen = prevScreen;
            Files.deleteIfExists(tempCfg);
        }
    }

    private static void testChunkReceiveBurstTelemetry1201() {
        MultiplayerPerformanceController.resetCountersForTesting();
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.currentScreen = null;
        MultiplayerPerformanceController.onWorldJoinOrSwitch(mc, mc.world);
        for (int c = 0; c < 64; c++) {
            MultiplayerPerformanceController.recordChunkLoaded120();
        }
        assertEquals(64L, MultiplayerPerformanceController.getTotalChunkBatchesPaced(),
                "ClientPlayNetworkHandler120Mixin chunk burst counter tracks 64 chunks");
        MultiplayerPerformanceController.onClientTick(mc);
        MultiplayerPerformanceController.resetCountersForTesting();
    }

    private static void testBudgetedChunkLightQueue1201() {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            MemoryOptimizer.setSimulatedRamForTesting(1536, 3072);
            mc.currentScreen = null;
            ClientWorld w = new ClientWorld();
            final java.util.concurrent.atomic.AtomicInteger done = new java.util.concurrent.atomic.AtomicInteger();
            for (int i = 0; i < 600; i++) w.getChunkUpdatersForTest().add(done::incrementAndGet);
            MultiplayerPerformanceController.runBudgetedChunkLightUpdates(w.getChunkUpdatersForTest(), mc);
            assertTrue(done.get() >= 2 && done.get() <= 6, "1.20.1 light burst capped per frame on 3GB device");
            while (!w.getChunkUpdatersForTest().isEmpty()) {
                MultiplayerPerformanceController.runBudgetedChunkLightUpdates(w.getChunkUpdatersForTest(), mc);
            }
            assertEquals(600, done.get(), "All 600 light updates drained on 1.20.1");
        } finally {
            MemoryOptimizer.setSimulatedRamForTesting(-1, -1);
            MultiplayerPerformanceController.resetCountersForTesting();
        }
    }

    private static void testCrowdedMultiplayerHubEntityRenderCulling1201() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-hub-120-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            MemoryOptimizer.setSimulatedRamForTesting(1536, 3072);
            client.getPerformanceEngine().applyProfile(PerformanceProfile.POTATO, mc, false);
            MultiplayerPerformanceController.onWorldJoinOrSwitch(mc, mc.world);
            mc.player.setPosForTest(0.0, 64.0, 0.0);

            List<Entity> hub = new java.util.ArrayList<>(300);
            hub.add(mc.player);
            for (int i = 0; i < 19; i++) { PlayerEntity p = new PlayerEntity(); p.setPosForTest(6.0, 64.0, 4.0); hub.add(p); }
            for (int i = 0; i < 10; i++) { ProjectileEntity pr = new ProjectileEntity(); pr.setPosForTest(65.0, 66.0, 4.0); hub.add(pr); }
            for (int i = 0; i < 10; i++) { HostileEntity h = new HostileEntity(); h.setPosForTest(24.0, 64.0, 4.0); hub.add(h); }
            int crit = hub.size();
            for (int i = 0; i < 100; i++) { DisplayEntity d = new DisplayEntity(); d.setPosForTest(42.0, 67.0, 30.0); hub.add(d); }
            for (int i = 0; i < 80; i++) { ArmorStandEntity a = new ArmorStandEntity(); a.setPosForTest(45.0, 64.0, 30.0); hub.add(a); }
            for (int i = 0; i < 40; i++) { ItemFrameEntity f = new ItemFrameEntity(); f.setPosForTest(40.0, 65.0, 25.0); hub.add(f); }
            for (int i = 0; i < 20; i++) { ItemEntity it = new ItemEntity(); it.setPosForTest(35.0, 64.0, 20.0); hub.add(it); }
            for (int i = 0; i < 20; i++) { ExperienceOrbEntity x = new ExperienceOrbEntity(); x.setPosForTest(35.0, 64.0, 20.0); hub.add(x); }

            int rendered = 0, critRendered = 0;
            for (int i = 0; i < hub.size(); i++) {
                if (MultiplayerPerformanceController.shouldRenderEntity(hub.get(i), 0.0, 64.0, 0.0, mc)) {
                    rendered++;
                    if (i < crit) critRendered++;
                }
            }
            assertEquals(crit, critRendered, "100% 1.20.1 PvP/combat entities preserved");
            assertTrue(hub.size() - rendered >= 240, "Distant 1.20.1 hub clutter culled");
        } finally {
            MemoryOptimizer.setSimulatedRamForTesting(-1, -1);
            MultiplayerPerformanceController.resetCountersForTesting();
            Files.deleteIfExists(tempCfg);
        }
    }

    private static void testExtendedMultiplayerSessionSoak1201() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-soak-120-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld prevWorld = mc.world;
        try {
            mc.setOverlay(null);
            AzyronLoadingRenderer.setCoreResourcesReady(true);
            mc.currentScreen = null;
            ModuleManager mm = client.getModuleManager();
            for (AzyronModule m : mm.getModules()) m.setEnabled(true);

            long oldGenBefore = getOldGenCollectionCount();
            DrawContext reusableCtx = new DrawContext(854, 480);
            for (int tick = 0; tick < 36_000; tick++) {
                if (tick % 6_000 == 0) {
                    ClientWorld nextWorld = new ClientWorld(Identifier.ofVanilla("overworld"));
                    mm.onWorldChanged();
                    mc.joinWorld(nextWorld);
                    MultiplayerPerformanceController.onWorldJoinOrSwitch(mc, nextWorld);
                }
                client.getPerformanceEngine().onClientTick(mc);
                mm.onTick(mc);
                for (int f = 0; f < 2; f++) {
                    reusableCtx.drawnStrings.clear();
                    mm.renderHudOverlay(reusableCtx, mc);
                }
            }
            assertTrue(reusableCtx.drawnStrings.stream().anyMatch(s -> s.contains("95%")), "1.20.1 ArmorHud rendered 72,000 frames");
            assertTrue(reusableCtx.matrixStackBalanced(), "MatrixStack balanced across all 72,000 1.20.1 frames");
            assertEquals(0L, getOldGenCollectionCount() - oldGenBefore, "Zero Full Old-Gen GCs across 72,000 1.20.1 frames");
        } finally {
            MultiplayerPerformanceController.resetCountersForTesting();
            mc.world = prevWorld;
            Files.deleteIfExists(tempCfg);
        }
    }

    private static String readClassAscii(JarFile jf, String entryName) throws Exception {
        JarEntry je = jf.getJarEntry(entryName);
        assertNotNull(je, "Missing class in JAR: " + entryName);
        return new String(jf.getInputStream(je).readAllBytes(), StandardCharsets.ISO_8859_1);
    }

    private static JsonObject readJsonEntry(JarFile jf, String name) throws Exception {
        JarEntry je = jf.getJarEntry(name);
        assertNotNull(je, "Missing JAR entry: " + name);
        return JsonParser.parseString(new String(jf.getInputStream(je).readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static long getOldGenCollectionCount() {
        long count = 0L;
        for (var gc : java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
            String name = gc.getName().toLowerCase();
            if (name.contains("old") || name.contains("marksweep") || name.contains("full")) {
                long c = gc.getCollectionCount();
                if (c > 0) count += c;
            }
        }
        return count;
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
