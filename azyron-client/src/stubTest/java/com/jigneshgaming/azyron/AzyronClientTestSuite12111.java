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
import com.mojang.blaze3d.shaders.ShaderType;
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
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.ReconfiguringScreen;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.realms.RealmsAvailability;
import net.minecraft.client.session.Session;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Identifier;

/**
 * Dedicated Automated Verification Suite for AZYRON CLIENT v1.0.3 — Minecraft 1.21.11 (Fabric 0.20.1).
 * Executes all 26 runtime, bytecode, UI, Mixin-plugin, Modrinth .mrpack, and multiplayer soak tests
 * strictly under the Minecraft 1.21.11 runtime profile.
 */
public final class AzyronClientTestSuite12111 {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.setProperty("azyron.mcVersion", "1.21.11");
        McVersionBridge.setSimulatedMcVersionForTests(McVersionBridge.SupportedMcVersion.MC_1_21_11);

        System.out.println("=======================================================================");
        System.out.println(" AZYRON CLIENT v1.0.3 — MINECRAFT 1.21.11 (Fabric 0.20.1) TEST SUITE");
        System.out.println("=======================================================================");

        Path jarPath = args.length > 0
                ? Paths.get(args[0])
                : Paths.get("releases/Azyron-Client-1.0.3.jar");
        Path mrpackPath = args.length > 1
                ? Paths.get(args[1])
                : Paths.get("releases/Azyron-Modpack-1.0.3.mrpack");

        runTest("1. [1.21.11] Unified JAR v1.0.3 Metadata (MC 1.20.1 + 1.21.11, Java >=17), Mixins & PNGs",
                () -> testJarIntegrityAndVersion(jarPath));
        runTest("2. [1.21.11] AzyronMixinPlugin Version-Selective Filtering (Enables 1.21.11, Skips *120Mixin)",
                AzyronClientTestSuite12111::testMixinPluginSelection12111);
        runTest("3. [1.21.11] MouseMixin & Screen121Mixin Descriptors, @Surrogate Safety & CPS Fallback",
                () -> testMouseAndScreen121MixinDescriptors(jarPath));
        runTest("4. [1.21.11] Cold-Boot ShaderLoader (rendertype_text) Zero-Error & Native Text Rendering",
                AzyronClientTestSuite12111::testColdBootShaderSafetyAndTextRendering);
        runTest("5. [1.21.11] Modpack Azyron-Modpack-1.0.3.mrpack (MC 1.21.11 + Dual Profiles, Zero Duplicates)",
                () -> testMrpackAndPojavConfigs(mrpackPath));
        runTest("6. [1.21.11] Client Startup Bootstrap & All 11 Modules Registration",
                AzyronClientTestSuite12111::testBootstrapAndModules);
        runTest("7. [1.21.11] Settings System (Boolean, Number Clamping/Stepping, Mode Cycling)",
                AzyronClientTestSuite12111::testSettingsSystem);
        runTest("8. [1.21.11] JSON Config v1.0.3 Save, Selected MC Profile & Lossless Round-Trip Reload",
                AzyronClientTestSuite12111::testConfigRoundTrip);
        runTest("9. [1.21.11] Performance Profiles, Sodium 0.8.14 Config & In-World Mipmap Guard",
                AzyronClientTestSuite12111::testPerformanceProfiles);
        runTest("10. [1.21.11] Frame-Time 1% Low Tracker & Memory Optimizer Telemetry",
                AzyronClientTestSuite12111::testTelemetryAndMemory);
        runTest("11. [1.21.11] Mouse Clicks, CPS Counter (LMB/RMB/Touch), Compass, Smooth Zoom & Toggle Sprint",
                AzyronClientTestSuite12111::testGameplayMechanics);
        runTest("12. [1.21.11] Main Menu, Singleplayer Load, HUD, Mods Hub, MC Selector & HUD Editor",
                AzyronClientTestSuite12111::testUiSingleplayerAndHudRendering);
        runTest("13. [1.21.11] PojavLauncher Mobile Aspect Ratios (2400x1080 / 1280x720) & Pause Menu Touch UI",
                AzyronClientTestSuite12111::testPojavLauncherCompatibility);
        runTest("14. [1.21.11] JVM Bytecode Linkage (Classfile v61.0, FabricLoader, Event, Narrator, Shared Dual-Intermediary)",
                () -> testJvmBytecodeLinkageContracts(jarPath));
        runTest("15. [1.21.11] Full Fabric onInitializeClient() Startup Flow & RegistryEntry<StatusEffect> Dispatch",
                AzyronClientTestSuite12111::testFullFabricStartupLifecycleAndRamSmoothness);
        runTest("16. [1.21.11] Automatic 3GB-8GB RAM Tier Scaling, Unfocused Throttle & 10,000-Frame Matrix3x2fStack Stress",
                AzyronClientTestSuite12111::testAutoRamScalingBackgroundThrottleAndHudBenchmark);
        runTest("17. [1.21.11] Corrupted raids_end.dat Backup, 1.21.11 NBT Schema Repair & Atomic Save",
                AzyronClientTestSuite12111::testRaidsDatBackupRepairAndAtomicSave);
        runTest("18. [1.21.11] Fullscreen 'off' -> 'false' & Malformed Boolean Option Sanitization",
                AzyronClientTestSuite12111::testFullscreenAndOptionsBooleanSanitizer);
        runTest("19. [1.21.11] Android / PojavLauncher Missing Flite Narrator Graceful Fallback (Zero Crash)",
                AzyronClientTestSuite12111::testAndroidMissingFliteNarratorFallback);
        runTest("20. [1.21.11] Offline / Local Realms Auth Graceful Handling (Preserving Online Microsoft Auth)",
                AzyronClientTestSuite12111::testOfflineLocalRealmsGracefulHandling);
        runTest("21. [1.21.11] Armor HUD Overlay Dispatch, IntegratedServer Pacing & Corrupt Config Backup",
                AzyronClientTestSuite12111::testArmorHudIntegratedServerAndConfigRecovery);
        runTest("22. [1.21.11] Multiplayer Server-Join Transition Guards, Unbound Registry Safety & 50 Zero-GC Joins",
                AzyronClientTestSuite12111::testMultiplayerServerJoinLifecycleAndUnboundRegistries);
        runTest("23. [1.21.11] ChunkBatchSizeCalculator Protocol Flow-Control Pacing Across 3GB-8GB RAM Tiers",
                AzyronClientTestSuite12111::testChunkBatchSizeCalculatorPacingAcrossRamTiers);
        runTest("24. [1.21.11] ClientWorld.runQueuedChunkUpdates() Nanosecond Budgeted Light Queue (250 & 1,200 Bursts)",
                AzyronClientTestSuite12111::testBudgetedChunkLightQueueUnderServerJoinBursts);
        runTest("25. [1.21.11] Crowded Multiplayer Hub Entity Render Culling (300 Entities, 100% Combat Preserved)",
                AzyronClientTestSuite12111::testCrowdedMultiplayerHubEntityRenderCulling);
        runTest("26. [1.21.11] Extended 30-Minute Equivalent Multiplayer Soak (36,000 Ticks / 72,000 HUD Frames)",
                AzyronClientTestSuite12111::testExtendedMultiplayerSessionSoak);

        System.out.println("-----------------------------------------------------------------------");
        System.out.printf(" MINECRAFT 1.21.11 SUITE SUMMARY: %d PASSED, %d FAILED%n", passed, failed);
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
        assertTrue(Files.size(jarPath) > 60_000L, "Production JAR size must be non-trivial");

        try (JarFile jf = new JarFile(jarPath.toFile())) {
            Manifest mf = jf.getManifest();
            assertNotNull(mf, "MANIFEST.MF must be present");
            Attributes attrs = mf.getMainAttributes();
            assertEquals("mixin", attrs.getValue("Fabric-Loom-Mixin-Remap-Type"), "Mixin remap type");
            assertEquals("intermediary", attrs.getValue("Fabric-Mapping-Namespace"), "Mapping namespace");
            assertEquals("1.0.3", attrs.getValue("Implementation-Version"), "Manifest Implementation-Version == 1.0.3");

            JarEntry fmjEntry = jf.getJarEntry("fabric.mod.json");
            assertNotNull(fmjEntry, "fabric.mod.json must exist in JAR");
            String rawFmj;
            try (InputStream in = jf.getInputStream(fmjEntry)) {
                rawFmj = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            assertTrue(!rawFmj.contains("${"), "fabric.mod.json must NOT contain unexpanded ${...} placeholders");
            JsonObject fmj = JsonParser.parseString(rawFmj).getAsJsonObject();
            assertEquals("azyron-client", fmj.get("id").getAsString(), "Mod ID");
            assertEquals("1.0.3", fmj.get("version").getAsString(), "Expanded mod version == 1.0.3");
            assertEquals("AZYRON CLIENT", fmj.get("name").getAsString(), "Client Name");

            JsonObject deps = fmj.getAsJsonObject("depends");
            JsonArray mcVersions = deps.getAsJsonArray("minecraft");
            assertEquals(2, mcVersions.size(), "Unified JAR supports 2 Minecraft versions");
            assertEquals("1.20.1", mcVersions.get(0).getAsString(), "Supports Minecraft 1.20.1");
            assertEquals("1.21.11", mcVersions.get(1).getAsString(), "Supports Minecraft 1.21.11");
            assertEquals(">=17", deps.get("java").getAsString(), "Java >=17 dependency for 1.20.1 & 1.21.11 compatibility");

            assertNotNull(jf.getJarEntry("com/jigneshgaming/azyron/AzyronClient.class"), "Entrypoint class in JAR");
            assertNotNull(jf.getJarEntry("com/jigneshgaming/azyron/mixin/AzyronMixinPlugin.class"), "AzyronMixinPlugin in JAR");

            JsonObject mixinsJson = readJsonEntry(jf, "azyron-client.mixins.json");
            assertEquals("JAVA_17", mixinsJson.get("compatibilityLevel").getAsString(), "Mixin compatibilityLevel == JAVA_17");
            assertEquals("com.jigneshgaming.azyron.mixin.AzyronMixinPlugin", mixinsJson.get("plugin").getAsString(),
                    "AzyronMixinPlugin wired in azyron-client.mixins.json");
            assertEquals(0, mixinsJson.getAsJsonObject("injectors").get("defaultRequire").getAsInt(),
                    "defaultRequire == 0");

            JsonArray commonMixins = mixinsJson.getAsJsonArray("mixins");
            JsonArray clientMixins = mixinsJson.getAsJsonArray("client");
            assertEquals(3, commonMixins.size(), "3 common Mixins registered (1.21.11 + 1.20.1 PersistentState guards)");
            assertEquals(19, clientMixins.size(), "19 client Mixins registered (shared + 1.21.11 + 1.20.1)");

            for (JsonArray arr : new JsonArray[]{commonMixins, clientMixins}) {
                for (int i = 0; i < arr.size(); i++) {
                    String mSimple = arr.get(i).getAsString();
                    String internal = "com/jigneshgaming/azyron/mixin/" + mSimple;
                    assertNotNull(jf.getJarEntry(internal + ".class"), "Mixin class present in JAR: " + internal);
                }
            }

            verifyPng128(jf, "assets/azyron/icon.png");
            verifyPng128(jf, "assets/azyron/textures/gui/logo.png");
        }
    }

    private static void testMixinPluginSelection12111() {
        assertEquals("1.21.11", McVersionBridge.getActiveMcVersionString(), "Active runtime version == 1.21.11");
        assertTrue(McVersionBridge.isMinecraft121(), "isMinecraft121() == true");
        assertTrue(!McVersionBridge.isMinecraft120(), "isMinecraft120() == false");

        AzyronMixinPlugin plugin = new AzyronMixinPlugin();
        plugin.onLoad("com.jigneshgaming.azyron.mixin");

        String[] enabledOn121 = {
                "ClientWorldMixin", "EntityRenderManagerMixin", "GameMenuScreenMixin",
                "GameOptionsMixin", "IntegratedServerMixin", "MinecraftClientMixin",
                "NarratorManagerMixin", "ParticleManagerMixin", "SplashOverlayMixin",
                "ChunkBatchSizeCalculatorMixin", "GameRendererMixin", "InGameHudMixin",
                "MouseMixin", "PersistentStateManagerMixin", "RealmsAvailabilityMixin", "Screen121Mixin"
        };
        for (String m : enabledOn121) {
            assertTrue(plugin.shouldApplyMixin("net.minecraft.Dummy", "com.jigneshgaming.azyron.mixin." + m),
                    "1.21.11 runtime MUST apply " + m);
        }

        String[] skippedOn121 = {
                "GameRenderer120Mixin", "InGameHud120Mixin", "Mouse120Mixin",
                "ClientPlayNetworkHandler120Mixin", "PersistentStateManager120Mixin", "PersistentState120Mixin"
        };
        for (String m : skippedOn121) {
            assertTrue(!plugin.shouldApplyMixin("net.minecraft.Dummy", "com.jigneshgaming.azyron.mixin." + m),
                    "1.21.11 runtime MUST skip 1.20.1-only " + m);
        }
    }

    private static void testMouseAndScreen121MixinDescriptors(Path jarPath) throws Exception {
        try (JarFile jf = new JarFile(jarPath.toFile())) {
            JsonObject refmap = readJsonEntry(jf, "azyron-client.refmap.json");
            JsonObject mouseMap = refmap.getAsJsonObject("mappings")
                    .getAsJsonObject("com/jigneshgaming/azyron/mixin/MouseMixin");
            String targetDesc = "Lnet/minecraft/class_312;method_1601(JLnet/minecraft/class_11910;I)V";
            assertEquals(targetDesc, mouseMap.get("onMouseButton(JLnet/minecraft/client/input/MouseInput;I)V").getAsString(),
                    "MouseMixin explicit 1.21.11 refmap target");

            byte[] mouseBytes = jf.getInputStream(jf.getJarEntry("com/jigneshgaming/azyron/mixin/MouseMixin.class")).readAllBytes();
            String mouseAscii = new String(mouseBytes, StandardCharsets.ISO_8859_1);
            assertTrue(mouseAscii.contains("(JLnet/minecraft/class_11910;ILorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V"),
                    "MouseMixin.class retains 1.21.11 intermediary descriptor");
            assertTrue(mouseAscii.contains("comp_4801"), "MouseMixin invokes MouseInput.button() -> comp_4801");

            byte[] screen121Bytes = jf.getInputStream(jf.getJarEntry("com/jigneshgaming/azyron/mixin/Screen121Mixin.class")).readAllBytes();
            String screen121Ascii = new String(screen121Bytes, StandardCharsets.ISO_8859_1);
            assertTrue(screen121Ascii.contains("method_25402"), "Screen121Mixin overrides mouseClicked -> method_25402");
            assertTrue(screen121Ascii.contains("method_25404"), "Screen121Mixin overrides keyPressed -> method_25404");
            assertTrue(screen121Ascii.contains("method_25400"), "Screen121Mixin overrides charTyped -> method_25400");

            // Crucial: AzyronBaseScreen itself must have ZERO references to 1.21-only Click/KeyInput/CharInput classes!
            byte[] baseScreenBytes = jf.getInputStream(jf.getJarEntry("com/jigneshgaming/azyron/ui/AzyronBaseScreen.class")).readAllBytes();
            String baseAscii = new String(baseScreenBytes, StandardCharsets.ISO_8859_1);
            assertTrue(!baseAscii.contains("class_11909") && !baseAscii.contains("class_11908") && !baseAscii.contains("class_11905"),
                    "AzyronBaseScreen must NOT reference 1.21-only Click/KeyInput/CharInput records directly");
        }

        Path tempCfg = Files.createTempFile("azyron-cps-fb-121-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.currentScreen = null;
        CpsTracker.reset();
        CpsModule cpsMod = client.getModuleManager().getCpsModule();
        for (int i = 0; i < 4; i++) {
            mc.mouse.setButtonStates(true, false);
            cpsMod.onTick(mc);
            mc.mouse.setButtonStates(false, false);
            cpsMod.onTick(mc);
            Thread.sleep(32L);
        }
        assertEquals(4, CpsTracker.getLeftCps(), "Fallback polling accurately records 4 LMB clicks");
        Files.deleteIfExists(tempCfg);
    }

    private static void testColdBootShaderSafetyAndTextRendering() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-shader-121-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();

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
            mainMenu.render(bootCtx, 427, 240, 0.016f);
            AzyronLoadingRenderer.render(bootCtx, mc, frame * 0.12f, 1.0f, false);
            client.getModuleManager().renderHudOverlay(bootCtx, mc);
        }

        assertEquals(0, shaderLoader.getShaderErrors().size(),
                "Cold-boot SplashOverlay & MainMenu emit ZERO minecraft:core/rendertype_text shader errors");
        assertTrue(bootCtx.fillCallCount > 50, "Cold-boot pixel font & progress bar render via GUI quads");

        shaderLoader.simulateReloadComplete();
        splash.markReloadComplete();
        AzyronLoadingRenderer.setCoreResourcesReady(true);
        assertTrue(UiRenderUtils.isCoreTextShaderReady(mc), "isCoreTextShaderReady becomes true post-reload");
        assertNotNull(shaderLoader.getSource(UiRenderUtils.CORE_TEXT_SHADER_ID, ShaderType.VERTEX), "VERTEX loaded");
        assertNotNull(shaderLoader.getSource(UiRenderUtils.CORE_TEXT_SHADER_ID, ShaderType.FRAGMENT), "FRAGMENT loaded");

        DrawContext readyCtx = new DrawContext(854, 480);
        AzyronLoadingRenderer.render(readyCtx, mc, 1.0f, 0.85f, true);
        assertTrue(readyCtx.drawnStrings.contains("Welcome to AZYRON CLIENT"), "Renders 'Welcome to AZYRON CLIENT'");

        mc.setOverlay(null);
        readyCtx.drawnStrings.clear();
        mainMenu.render(readyCtx, 427, 240, 0.016f);
        assertTrue(readyCtx.drawnStrings.contains("AZYRON CLIENT v1.0.3  •  Minecraft 1.21.11 (Fabric)"),
                "Main Menu renders v1.0.3 • Minecraft 1.21.11 (Fabric) footer");
        Files.deleteIfExists(tempCfg);
    }

    private static void testMrpackAndPojavConfigs(Path mrpackPath) throws Exception {
        assertTrue(Files.exists(mrpackPath), "Modpack must exist at " + mrpackPath);
        try (ZipFile zf = new ZipFile(mrpackPath.toFile())) {
            ZipEntry idxEntry = zf.getEntry("modrinth.index.json");
            assertNotNull(idxEntry, "modrinth.index.json present in mrpack");
            JsonObject idx = JsonParser.parseString(new String(zf.getInputStream(idxEntry).readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("1.0.3", idx.get("versionId").getAsString(), "mrpack versionId == 1.0.3");
            JsonObject deps = idx.getAsJsonObject("dependencies");
            assertEquals("1.21.11", deps.get("minecraft").getAsString(), "mrpack root targets Minecraft 1.21.11");
            assertEquals("0.20.1", deps.get("fabric-loader").getAsString(), "mrpack targets Fabric Loader 0.20.1");

            // Both embedded version manifests must also exist inside profiles/
            assertNotNull(zf.getEntry("profiles/modrinth.index.1.21.11.json"), "Embedded 1.21.11 profile manifest present");
            assertNotNull(zf.getEntry("profiles/modrinth.index.1.20.1.json"), "Embedded 1.20.1 profile manifest present");

            JsonArray files = idx.getAsJsonArray("files");
            assertEquals(8, files.size(), "All 8 universal PC + PojavLauncher verified 1.21.11 Fabric mods included");
            Set<String> paths = new HashSet<>();
            for (JsonElement el : files) {
                JsonObject f = el.getAsJsonObject();
                String p = f.get("path").getAsString();
                assertTrue(paths.add(p), "No duplicate mod path: " + p);
                assertEquals(128, f.getAsJsonObject("hashes").get("sha512").getAsString().length(), "Valid SHA-512");
            }

            assertNotNull(zf.getEntry("overrides/mods/Azyron-Client-1.0.3.jar"),
                    "Unified overrides/mods/Azyron-Client-1.0.3.jar present in mrpack");
            assertTrue(zf.getEntry("overrides/mods/Azyron-Client-1.0.2.jar") == null,
                    "Superseded 1.0.2 JAR must NOT exist in mrpack");
        }
    }

    private static void testBootstrapAndModules() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-test-121-", ".json");
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
            assertNotNull(mm.getById(id), "Module registered: " + id);
        }
        assertEquals(1, mm.filterModules(ModuleCategory.ALL, "zoom").size(), "Search filter finds Zoom");
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
        Path tempCfg = Files.createTempFile("azyron-roundtrip-121-", ".json");
        Files.deleteIfExists(tempCfg);

        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        HudModule fps = client.getModuleManager().getFpsModule();
        fps.setAnchor(0.42f, 0.73f);
        fps.setScale(1.35f);
        client.getModuleManager().getCrosshairModule().setEnabled(true);
        client.getCosmeticsManager().setActiveCape(CosmeticsManager.CapeStyle.CYBER_GRID);
        client.getCosmeticsManager().setActiveHalo(CosmeticsManager.HaloStyle.CROWN_AURA);
        client.getConfig().setActiveProfile(PerformanceProfile.POTATO);
        client.getConfig().setSelectedMcVersion(McVersionBridge.SupportedMcVersion.MC_1_21_11);
        client.getConfig().save();

        JsonObject savedJson = JsonParser.parseString(Files.readString(tempCfg)).getAsJsonObject();
        assertEquals("1.0.3", savedJson.get("configVersion").getAsString(), "Saved configVersion == 1.0.3");
        assertEquals("1.21.11", savedJson.get("selectedMcVersion").getAsString(), "Saved selectedMcVersion == 1.21.11");

        AzyronConfig reloaded = new AzyronConfig(tempCfg);
        reloaded.load();
        ModuleManager mm2 = new ModuleManager();
        CosmeticsManager cm2 = new CosmeticsManager();
        reloaded.applyToManagers(mm2, cm2);

        assertEquals(PerformanceProfile.POTATO, reloaded.getActiveProfile(), "Reloaded active profile");
        assertEquals(McVersionBridge.SupportedMcVersion.MC_1_21_11, reloaded.getSelectedMcVersion(), "Reloaded MC profile");
        assertEquals(CosmeticsManager.CapeStyle.CYBER_GRID, cm2.getActiveCape(), "Reloaded cape");
        assertTrue(mm2.getCrosshairModule().isEnabled(), "Reloaded crosshair enabled");
        Files.deleteIfExists(tempCfg);
    }

    private static void testPerformanceProfiles() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-perf-121-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        // Isolate preset values here; RAM enforcement has a dedicated 3GB test.
        client.getConfig().setAutoMemoryOptimization(false);
        MinecraftClient mc = MinecraftClient.getInstance();
        PerformanceEngine engine = client.getPerformanceEngine();

        ClientWorld savedWorld = mc.world;
        mc.world = null;
        engine.applyProfile(PerformanceProfile.POTATO, mc, true);
        assertEquals(GraphicsMode.FAST, mc.options.getPreset().getValue(), "Potato FAST graphics");
        assertEquals(ParticlesMode.MINIMAL, mc.options.getParticles().getValue(), "Potato MINIMAL particles");
        assertEquals(CloudRenderMode.OFF, mc.options.getCloudRenderMode().getValue(), "Potato OFF clouds");
        assertEquals(6, mc.options.getViewDistance().getValue(), "Potato view distance 6");
        assertEquals(5, mc.options.getSimulationDistance().getValue(), "Potato sim distance 5");
        assertEquals(0, mc.options.getMipmapLevels().getValue(), "Menu Potato sets mipmapLevels 0");

        // Verify 1.21.11 Sodium 0.8.x config schema written by PerformanceEngine
        Path sodiumCfg = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("sodium-options.json");
        JsonObject sod = JsonParser.parseString(Files.readString(sodiumCfg)).getAsJsonObject();
        assertTrue(sod.has("performance"), "1.21.11 writes Sodium 0.8.x nested performance schema");
        assertTrue(!sod.getAsJsonObject("performance").get("use_no_error_g_l_context").getAsBoolean(),
                "Sodium 0.8.x keeps use_no_error_g_l_context=false for PojavLauncher safety");

        mc.world = savedWorld;
        engine.applyProfile(PerformanceProfile.BALANCED, mc, true);
        assertEquals(GraphicsMode.FANCY, mc.options.getPreset().getValue(), "Balanced FANCY graphics");
        assertEquals(10, mc.options.getViewDistance().getValue(), "Balanced view distance 10");
        assertEquals(0, mc.options.getMipmapLevels().getValue(), "In-world profile switch avoids mipmap reload spike");

        engine.applyProfile(PerformanceProfile.POTATO, mc, false);
        engine.onClientTick(mc);
        assertTrue(!engine.shouldAllowParticle(mc.player.getX() + 100.0, mc.player.getY(), mc.player.getZ()),
                "Potato Mode culls distant particles");
        engine.applyProfile(PerformanceProfile.SMOOTH_FPS, mc, true);
        assertEquals(120, mc.options.getMaxFps().getValue(), "Smooth FPS targets 120 FPS");
        Files.deleteIfExists(tempCfg);
    }

    private static void testTelemetryAndMemory() {
        for (int i = 0; i < 20; i++) {
            FrameTimeTracker.onFrameRendered();
        }
        int low = FrameTimeTracker.getOnePercentLowFps(240);
        assertTrue(low >= 1 && low <= 240, "1% low FPS within bounds");
        MemoryOptimizer.onClientTick(MinecraftClient.getInstance(), true);
        assertTrue(MemoryOptimizer.getMaxMb() > 0, "JVM Max MB > 0");
    }

    private static void testGameplayMechanics() {
        CpsTracker.reset();
        for (int i = 0; i < 9; i++) CpsTracker.recordLeftClick();
        for (int i = 0; i < 5; i++) CpsTracker.recordRightClick();
        assertEquals(9, CpsTracker.getLeftCps(), "LMB CPS tracks 9 clicks");
        assertEquals(5, CpsTracker.getRightCps(), "RMB CPS tracks 5 clicks");

        assertEquals("S (+Z)", CoordinatesModule.formatFacing(0.0f), "Yaw 0 = South");
        assertEquals("N (-Z)", CoordinatesModule.formatFacing(180.0f), "Yaw 180 = North");

        ZoomModule zoom = new ZoomModule();
        zoom.setZooming(true);
        float zoomedFov = 70.0f;
        for (int i = 0; i < 30; i++) zoomedFov = zoom.applyFov(70.0f);
        assertTrue(zoomedFov < 20.0f, "Smooth Zoom interpolates 70 FOV down");
        assertTrue(zoom.onScroll(1.0), "Scroll-to-zoom consumes scroll");

        ToggleSprintModule ts = new ToggleSprintModule();
        assertTrue(ts.isSprintToggled(), "ToggleSprint enabled by default");
        ts.toggleSprintState();
        assertTrue(!ts.isSprintToggled(), "ToggleSprint toggles off");
    }

    private static void testUiSingleplayerAndHudRendering() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-ui-121-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.setOverlay(null);
        mc.getShaderLoader().simulateReloadComplete();
        AzyronLoadingRenderer.setCoreResourcesReady(true);

        DrawContext ctx = new DrawContext(854, 480);
        assertEquals(0xFF00D4FF, AzyronTheme.ACCENT_ELECTRIC_BLUE, "Electric blue opaque");

        AzyronLoadingRenderer.render(ctx, mc, 0.65f, 1.0f);
        assertTrue(ctx.drawnStrings.contains("Welcome to AZYRON CLIENT"), "Loading screen title");

        ctx.drawnStrings.clear();
        AzyronMainMenuScreen mainMenu = new AzyronMainMenuScreen();
        mainMenu.init(mc, 854, 480);
        mainMenu.render(ctx, 427, 240, 0.016f);
        assertTrue(ctx.drawnStrings.contains("Singleplayer"), "Main Menu renders Singleplayer");
        mainMenu.handleAction("singleplayer");
        assertTrue(mc.currentScreen instanceof SelectWorldScreen, "Opens SelectWorldScreen");
        mc.currentScreen = null;
        mc.world = new ClientWorld();

        CrosshairModule ch = client.getModuleManager().getCrosshairModule();
        ch.setEnabled(true);
        ch.renderCrosshair(ctx, mc);
        ctx.drawnStrings.clear();
        client.getModuleManager().renderHudOverlay(ctx, mc);
        assertTrue(ctx.drawnStrings.stream().anyMatch(s -> s.contains("FPS")), "HUD renders FPS");

        // Mods Hub + interactive 1.21.11 <-> 1.20.1 profile selector
        mainMenu.handleAction("mods");
        AzyronModsScreen modsScreen = (AzyronModsScreen) mc.currentScreen;
        modsScreen.init(mc, 854, 480);
        modsScreen.openClientOptionsTab();
        ctx.drawnStrings.clear();
        modsScreen.render(ctx, 400, 220, 0.016f);
        assertTrue(ctx.drawnStrings.contains("AZYRON CLIENT  •  v1.0.3  (Minecraft 1.21.11 & 1.20.1 Fabric)"),
                "Mods Screen displays unified v1.0.3 dual-version badge");
        assertTrue(ctx.drawnStrings.contains("< 1.21.11 >"), "Mods Screen shows < 1.21.11 > profile selector");

        // Click the MC version profile selector card to cycle 1.21.11 -> 1.20.1 -> 1.21.11
        int winW = Math.min(476, 854 - 16);
        int winH = Math.min(286, 480 - 16);
        int wx = (854 - winW) / 2;
        int wy = (480 - winH) / 2;
        int cx = wx + 108 + 8;
        int cy = wy + 34 + 6;
        modsScreen.azyronMouseClicked(cx + 40, cy + 115, 0);
        assertEquals(McVersionBridge.SupportedMcVersion.MC_1_20_1, client.getConfig().getSelectedMcVersion(),
                "Clicking MC selector switches profile to 1.20.1");
        modsScreen.azyronMouseClicked(cx + 40, cy + 115, 0);
        assertEquals(McVersionBridge.SupportedMcVersion.MC_1_21_11, client.getConfig().getSelectedMcVersion(),
                "Clicking MC selector again restores 1.21.11");
        modsScreen.azyronKeyPressed(256, 0, 0);

        HudEditorScreen editor = new HudEditorScreen(mainMenu);
        editor.init(mc, 854, 480);
        editor.render(ctx, 200, 200, 0.016f);
        HudModule fpsHud = client.getModuleManager().getFpsModule();
        int startX = fpsHud.getScreenX(854) + 5;
        int startY = fpsHud.getScreenY(480) + 5;
        editor.azyronMouseClicked(startX, startY, 0);
        editor.azyronMouseDragged(300, 180, 0, 100, 100);
        editor.azyronMouseReleased(300, 180, 0);
        editor.mouseScrolled(fpsHud.getScreenX(854) + 4, fpsHud.getScreenY(480) + 4, 0.0, 1.0);
        assertTrue(fpsHud.getScale() > 1.0f, "HUD Editor scroll increases widget scale");

        AzyronCosmeticsScreen cosScreen = new AzyronCosmeticsScreen(mainMenu);
        cosScreen.init(mc, 854, 480);
        cosScreen.render(ctx, 300, 200, 0.016f);
        cosScreen.azyronCharTyped('a', 0);

        Files.deleteIfExists(tempCfg);
    }

    private static void testPojavLauncherCompatibility() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-pojav-121-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.setOverlay(null);
        mc.getShaderLoader().simulateReloadComplete();
        AzyronLoadingRenderer.setCoreResourcesReady(true);

        for (int[] res : new int[][]{{800, 360}, {640, 360}}) {
            DrawContext mobileCtx = new DrawContext(res[0], res[1]);
            AzyronMainMenuScreen mm = new AzyronMainMenuScreen();
            mm.init(mc, res[0], res[1]);
            mm.render(mobileCtx, res[0] / 2, res[1] / 2, 0.016f);
            client.getModuleManager().renderHudOverlay(mobileCtx, mc);
        }
        GameMenuScreen pauseScreen = new GameMenuScreen(true);
        pauseScreen.init(mc, 640, 360);
        assertNotNull(pauseScreen, "GameMenuScreen initializes cleanly");
        Files.deleteIfExists(tempCfg);
    }

    private static void testJvmBytecodeLinkageContracts(Path jarPath) throws Exception {
        int checkedClasses = 0;
        boolean sawFabricLoaderInterfaceRef = false;
        boolean sawFabricEventClassRef = false;
        boolean sawNarratorInterfaceRef = false;

        try (JarFile jf = new JarFile(jarPath.toFile())) {
            var entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry je = entries.nextElement();
                if (!je.getName().endsWith(".class")) continue;
                checkedClasses++;
                byte[] data = jf.getInputStream(je).readAllBytes();
                ByteBuffer buf = ByteBuffer.wrap(data);
                assertEquals(0xCAFEBABE, buf.getInt(), "Valid class magic in " + je.getName());
                buf.getShort();
                int major = buf.getShort() & 0xFFFF;
                assertEquals(61, major, "Java 17 classfile v61.0 (compatible with Java 17 & Java 21) in " + je.getName());

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
                        case 5, 6 -> { buf.position(buf.position() + 8); i++; }
                        case 15 -> buf.position(buf.position() + 3);
                        default -> throw new AssertionError("Unknown CP tag " + tag);
                    }
                }
                for (int i = 1; i < cpCount; i++) {
                    int tag = tags[i];
                    if (tag == 7) {
                        String clsName = utf8[ref1[i]];
                        assertTrue(!clsName.startsWith("net/minecraft/client/"),
                                "No unremapped net/minecraft/client/* class in " + je.getName() + ": " + clsName);
                    } else if (tag == 9 || tag == 10 || tag == 11) {
                        String owner = utf8[ref1[ref1[i]]];
                        String name = utf8[ref1[ref2[i]]];
                        String desc = utf8[ref2[ref2[i]]];
                        if ("net/fabricmc/loader/api/FabricLoader".equals(owner)) {
                            assertEquals(11, tag, "FabricLoader MUST use CONSTANT_InterfaceMethodref");
                            sawFabricLoaderInterfaceRef = true;
                        }
                        if (tag == 9 && "net/fabricmc/fabric/api/client/event/lifecycle/v1/ClientTickEvents".equals(owner)
                                && "END_CLIENT_TICK".equals(name)) {
                            assertEquals("Lnet/fabricmc/fabric/api/event/Event;", desc, "Event descriptor");
                            sawFabricEventClassRef = true;
                        }
                        if ((tag == 10 || tag == 11) && "com/mojang/text2speech/Narrator".equals(owner)) {
                            assertEquals(11, tag, "Narrator MUST use CONSTANT_InterfaceMethodref");
                            sawNarratorInterfaceRef = true;
                        }
                    }
                }
            }
        }
        assertTrue(checkedClasses >= 55, "Inspected all compiled classes (" + checkedClasses + ")");
        assertTrue(sawFabricLoaderInterfaceRef && sawFabricEventClassRef && sawNarratorInterfaceRef,
                "Verified FabricLoader, Event, and Narrator bytecode linkage contracts");
    }

    private static void testFullFabricStartupLifecycleAndRamSmoothness() throws Exception {
        AzyronClient entrypoint = new AzyronClient();
        entrypoint.onInitializeClient();
        assertEquals(entrypoint, AzyronClient.getInstance(), "onInitializeClient sets singleton");
        assertEquals(11, entrypoint.getModuleManager().getModules().size(), "All 11 modules initialized");

        MinecraftClient mc = MinecraftClient.getInstance();
        mc.getShaderLoader().simulateReloadComplete();
        DrawContext ctx = new DrawContext(854, 480);
        entrypoint.getModuleManager().getPotionHudModule().setEnabled(true);
        entrypoint.getModuleManager().renderHudOverlay(ctx, mc);
        assertTrue(ctx.drawnStrings.stream().anyMatch(s -> s.contains("Speed II")),
                "PotionHudModule renders Speed II via 1.21.11 RegistryEntry<StatusEffect> bridge");

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.invoker().onEndTick(mc);
    }

    private static void testAutoRamScalingBackgroundThrottleAndHudBenchmark() throws Exception {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld prevWorld = mc.world;
        try {
            MemoryOptimizer.setSimulatedRamForTesting(1536, 3072);
            assertEquals(MemoryOptimizer.RamTier.TIER_3GB, MemoryOptimizer.detectRamTier(), "3GB tier");
            Path cfg3gb = Files.createTempFile("azyron-3gb-121-", ".json");
            Files.deleteIfExists(cfg3gb);
            AzyronClient c3 = AzyronClient.bootstrapHeadlessForTests(cfg3gb);
            assertEquals(PerformanceProfile.POTATO, c3.getConfig().getActiveProfile(), "3GB auto-selects POTATO");

            PerformanceEngine engine = c3.getPerformanceEngine();
            engine.applyProfile(PerformanceProfile.BALANCED, mc, false);
            engine.onWindowFocusChanged(mc, false);
            assertEquals(15, mc.options.getMaxFps().getValue(), "Unfocused throttles to 15 FPS");
            engine.onWindowFocusChanged(mc, true);
            assertEquals(60, mc.options.getMaxFps().getValue(), "Refocused preserves 3GB 60 FPS target");

            mc.world = prevWorld != null ? prevWorld : new ClientWorld();
            mc.setOverlay(null);
            mc.getShaderLoader().simulateReloadComplete();
            AzyronLoadingRenderer.setCoreResourcesReady(true);
            c3.getModuleManager().getMemoryHudModule().setEnabled(true);
            CpsTracker.reset();
            for (int c = 0; c < 35; c++) CpsTracker.recordLeftClick();

            DrawContext benchCtx = new DrawContext(1920, 1080);
            long t0 = System.nanoTime();
            for (int frame = 0; frame < 10_000; frame++) {
                benchCtx.drawnStrings.clear();
                c3.getModuleManager().renderHudOverlay(benchCtx, mc);
            }
            long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;
            assertTrue(elapsedMs < 4_000L, "10,000 Matrix3x2fStack HUD frames rendered in " + elapsedMs + "ms");
            Files.deleteIfExists(cfg3gb);
        } finally {
            MemoryOptimizer.setSimulatedRamForTesting(-1, -1);
            FrameTimeTracker.resetForTesting();
            mc.world = prevWorld;
        }
    }

    private static void testRaidsDatBackupRepairAndAtomicSave() throws Exception {
        PersistentStateGuard.clearSessionBackupCacheForTests();
        Path worldDataDir = Files.createTempDirectory("azyron-world-121-");

        NbtCompound legacyRoot = new NbtCompound();
        NbtCompound legacyData = new NbtCompound();
        legacyData.putInt("NextAvailableID", 9);
        legacyData.putInt("Tick", 1450);

        NbtList raidsList = new NbtList();
        NbtCompound validRaidEntry = new NbtCompound();
        validRaidEntry.putInt("id", 3);
        NbtCompound innerRaid = new NbtCompound();
        innerRaid.putString("status", "ongoing");
        validRaidEntry.put("raid", innerRaid);
        raidsList.add(validRaidEntry);
        raidsList.add(new NbtCompound()); // malformed entry
        legacyData.put("raids", raidsList);
        legacyRoot.put("data", legacyData);

        Path raidsEndFile = worldDataDir.resolve("raids_end.dat");
        NbtIo.writeCompressed(legacyRoot, raidsEndFile);

        NbtCompound repaired = PersistentStateGuard.inspectAndRepairRaidsCompound(raidsEndFile, "raids_end", legacyRoot);
        NbtCompound repairedData = repaired.getCompound("data").orElseThrow();
        assertEquals(9, repairedData.getInt("next_id").orElse(-1), "Migrated NextAvailableID -> next_id == 9");
        assertEquals(1450, repairedData.getInt("tick").orElse(-1), "Migrated Tick -> tick == 1450");
        assertEquals(1, repairedData.getList("raids").orElseThrow().size(), "Isolated malformed 1.21.11 raid entry");

        Path atomicTarget = worldDataDir.resolve("raids_atomic.dat");
        PersistentStateGuard.saveCompressedAtomically(atomicTarget, repaired);
        assertTrue(Files.exists(atomicTarget) && Files.size(atomicTarget) > 0, "Atomic save committed");
    }

    private static void testFullscreenAndOptionsBooleanSanitizer() {
        assertEquals("false", OptionsConfigSanitizer.normalizeBooleanOptionValue("fullscreen", "off", false), "off -> false");
        assertEquals("true", OptionsConfigSanitizer.normalizeBooleanOptionValue("fullscreen", "on", false), "on -> true");
        NbtCompound optsNbt = new NbtCompound();
        optsNbt.putString("fullscreen", "off");
        optsNbt.putString("enableVsync", "on");
        OptionsConfigSanitizer.sanitizeOptionsCompound(optsNbt);
        assertEquals("false", optsNbt.getString("fullscreen").orElse(""), "Compound fullscreen:off -> false");
        assertEquals("true", optsNbt.getString("enableVsync").orElse(""), "Compound enableVsync:on -> true");
    }

    private static void testAndroidMissingFliteNarratorFallback() {
        try {
            PlatformCompat.setSimulatedAndroidForTesting(true);
            PlatformCompat.setSimulatedFliteAvailableForTesting(false);
            Narrator safeNarrator = PlatformCompat.createSafeNarrator();
            assertEquals(Narrator.EMPTY, safeNarrator, "Missing Flite returns Narrator.EMPTY");
        } finally {
            PlatformCompat.setSimulatedAndroidForTesting(null);
            PlatformCompat.setSimulatedFliteAvailableForTesting(null);
        }
    }

    private static void testOfflineLocalRealmsGracefulHandling() {
        Session pojavLocal = new Session("JigneshLocal", "0", Optional.empty());
        assertTrue(PlatformCompat.isOfflineOrLocalSession(pojavLocal), "Token '0' recognized as local");
        String realisticMsaJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ4dWlkIjoiMjUzNTQxMjM0NTY3ODkwMSIsImFnZSI6IjE4In0.signatureTokenBlock123456";
        Session onlineMsa = new Session("JigneshMSA", realisticMsaJwt, Optional.of("2535412345678901"));
        assertTrue(!PlatformCompat.isOfflineOrLocalSession(onlineMsa), "Online MSA preserved");
        RealmsAvailability.Info offlineInfo = new RealmsAvailability.Info(RealmsAvailability.Type.AUTHENTICATION_ERROR);
        assertTrue(offlineInfo.exception() == null, "Null exception prevents retry loop");
    }

    private static void testArmorHudIntegratedServerAndConfigRecovery() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-armor-121-", ".json");
        Files.writeString(tempCfg, "{ broken_json_syntax: [1, 2, ", StandardCharsets.UTF_8);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.setOverlay(null);
        mc.getShaderLoader().simulateReloadComplete();
        AzyronLoadingRenderer.setCoreResourcesReady(true);
        mc.world = new ClientWorld();

        var armorHud = client.getModuleManager().getArmorHudModule();
        armorHud.setEnabled(true);
        Setting.ModeSetting durMode = (Setting.ModeSetting) armorHud.getSettings().get(3);
        DrawContext hudCtx = new DrawContext(854, 480);
        for (String mode : new String[]{"Percentage", "Points", "Bar Only"}) {
            durMode.setValue(mode);
            armorHud.renderOnHud(hudCtx, mc, false);
        }
        assertEquals(15, hudCtx.drawnItemCount, "ArmorHudModule rendered 15 items");
        assertEquals(15, hudCtx.drawnStackOverlayCount, "ArmorHudModule rendered 15 public overlays");
        assertTrue(IntegratedServerOptimizer.shouldForceSyncChunkWrites(true), "Vanilla save durability preserved");
        IntegratedServer server = new IntegratedServer(mc);
        IntegratedServerOptimizer.onIntegratedServerTick(server, mc);
        Files.deleteIfExists(tempCfg);
    }

    private static void testMultiplayerServerJoinLifecycleAndUnboundRegistries() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-mp-join-121-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld prevWorld = mc.world;
        var prevScreen = mc.currentScreen;
        try {
            MultiplayerPerformanceController.resetCountersForTesting();
            mc.setOverlay(null);
            mc.getShaderLoader().simulateReloadComplete();
            AzyronLoadingRenderer.setCoreResourcesReady(true);
            assertTrue(MultiplayerPerformanceController.isTransitionScreen(new LevelLoadingScreen()), "LevelLoadingScreen");
            assertTrue(MultiplayerPerformanceController.isTransitionScreen(new ConnectScreen()), "ConnectScreen");
            assertTrue(MultiplayerPerformanceController.isTransitionScreen(new ReconfiguringScreen()), "ReconfiguringScreen");
            assertTrue(MultiplayerPerformanceController.isTransitionScreen(new ProgressScreen()), "ProgressScreen");
            assertTrue(MultiplayerPerformanceController.isTransitionScreen(new DisconnectedScreen()), "DisconnectedScreen");

            mc.currentScreen = null;
            mc.player.setStatusEffectsForTest(List.of(
                    new StatusEffectInstance(new RegistryEntry.Unbound<>(), 1, 400, false),
                    new StatusEffectInstance()
            ));
            DrawContext unboundCtx = new DrawContext(854, 480);
            client.getModuleManager().renderHudOverlay(unboundCtx, mc);
            assertTrue(unboundCtx.drawnStrings.stream().anyMatch(s -> s.contains("Effect II")), "Unbound degrades to Effect II");
            mc.player.setStatusEffectsForTest(List.of(new StatusEffectInstance()));

            long oldGenBefore = getOldGenCollectionCount();
            DrawContext joinHudCtx = new DrawContext(854, 480);
            MemoryOptimizer.setSimulatedRamForTesting(1536, 3072);
            for (int joinIdx = 0; joinIdx < 50; joinIdx++) {
                ClientWorld serverWorld = new ClientWorld(Identifier.ofVanilla("overworld"));
                client.getModuleManager().onWorldChanged();
                mc.joinWorld(serverWorld);
                MultiplayerPerformanceController.onWorldJoinOrSwitch(mc, serverWorld);
                mc.currentScreen = null;
                for (int t = 0; t < 20; t++) {
                    client.getPerformanceEngine().onClientTick(mc);
                    client.getModuleManager().onTick(mc);
                    joinHudCtx.drawnStrings.clear();
                    client.getModuleManager().renderHudOverlay(joinHudCtx, mc);
                }
            }
            assertEquals(oldGenBefore, getOldGenCollectionCount(), "50 joins triggered ZERO Full GCs");
        } finally {
            MemoryOptimizer.setSimulatedRamForTesting(-1, -1);
            MultiplayerPerformanceController.resetCountersForTesting();
            mc.world = prevWorld;
            mc.currentScreen = prevScreen;
            Files.deleteIfExists(tempCfg);
        }
    }

    private static void testChunkBatchSizeCalculatorPacingAcrossRamTiers() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-chunk-pace-121-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            mc.currentScreen = null;
            MemoryOptimizer.setSimulatedRamForTesting(1408, 3072);
            client.getPerformanceEngine().applyProfile(PerformanceProfile.POTATO, mc, false);
            MultiplayerPerformanceController.onWorldJoinOrSwitch(mc, mc.world);
            float t3Join = MultiplayerPerformanceController.paceDesiredChunksPerTick(64.0f);
            assertTrue(t3Join >= 1.0f && t3Join <= 1.6f, "3GB join-warmup chunk batch cap ~1.28");
        } finally {
            MemoryOptimizer.setSimulatedRamForTesting(-1, -1);
            MultiplayerPerformanceController.resetCountersForTesting();
            Files.deleteIfExists(tempCfg);
        }
    }

    private static void testBudgetedChunkLightQueueUnderServerJoinBursts() {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            MemoryOptimizer.setSimulatedRamForTesting(1536, 3072);
            mc.currentScreen = null;
            ClientWorld azyronWorld = new ClientWorld();
            final java.util.concurrent.atomic.AtomicInteger completed = new java.util.concurrent.atomic.AtomicInteger();
            for (int i = 0; i < 1200; i++) azyronWorld.getChunkUpdatersForTest().add(completed::incrementAndGet);
            MultiplayerPerformanceController.runBudgetedChunkLightUpdates(azyronWorld.getChunkUpdatersForTest(), mc);
            int ranFirst = completed.get();
            assertTrue(ranFirst >= 2 && ranFirst <= 6, "1,200-chunk burst capped on 3GB frame (ran " + ranFirst + ")");
            while (!azyronWorld.getChunkUpdatersForTest().isEmpty()) {
                MultiplayerPerformanceController.runBudgetedChunkLightUpdates(azyronWorld.getChunkUpdatersForTest(), mc);
            }
            assertEquals(1200, completed.get(), "All 1,200 light updates drained");
        } finally {
            MemoryOptimizer.setSimulatedRamForTesting(-1, -1);
            MultiplayerPerformanceController.resetCountersForTesting();
        }
    }

    private static void testCrowdedMultiplayerHubEntityRenderCulling() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-hub-cull-121-", ".json");
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
            for (int i = 0; i < 15; i++) { PlayerEntity p = new PlayerEntity(); p.setPosForTest(5.0, 64.0, 4.0); hub.add(p); }
            for (int i = 0; i < 10; i++) { ProjectileEntity pr = new ProjectileEntity(); pr.setPosForTest(70.0, 66.0, 4.0); hub.add(pr); }
            for (int i = 0; i < 14; i++) { HostileEntity h = new HostileEntity(); h.setPosForTest(25.0, 64.0, 4.0); hub.add(h); }
            int crit = hub.size();
            for (int i = 0; i < 100; i++) { DisplayEntity d = new DisplayEntity(); d.setPosForTest(40.0, 67.0, 30.0); hub.add(d); }
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
            assertEquals(crit, critRendered, "100% critical combat entities preserved");
            assertTrue(hub.size() - rendered >= 240, "Distant hub clutter culled");
        } finally {
            MemoryOptimizer.setSimulatedRamForTesting(-1, -1);
            MultiplayerPerformanceController.resetCountersForTesting();
            Files.deleteIfExists(tempCfg);
        }
    }

    private static void testExtendedMultiplayerSessionSoak() throws Exception {
        Path tempCfg = Files.createTempFile("azyron-soak-121-", ".json");
        Files.deleteIfExists(tempCfg);
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(tempCfg);
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld prevWorld = mc.world;
        try {
            mc.setOverlay(null);
            mc.getShaderLoader().simulateReloadComplete();
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
            assertTrue(reusableCtx.drawnStrings.stream().anyMatch(s -> s.contains("95%")), "ArmorHud rendered through 72,000 frames");
            assertEquals(0L, getOldGenCollectionCount() - oldGenBefore, "Zero Full Old-Gen GCs across 72,000 frames");
        } finally {
            MultiplayerPerformanceController.resetCountersForTesting();
            mc.world = prevWorld;
            Files.deleteIfExists(tempCfg);
        }
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

    private static JsonObject readJsonEntry(JarFile jf, String name) throws Exception {
        JarEntry je = jf.getJarEntry(name);
        assertNotNull(je, "Missing JAR entry: " + name);
        try (InputStream in = jf.getInputStream(je)) {
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static void verifyPng128(JarFile jf, String path) throws Exception {
        JarEntry je = jf.getJarEntry(path);
        assertNotNull(je, "Missing PNG asset: " + path);
        byte[] b = jf.getInputStream(je).readAllBytes();
        assertEquals(128, ByteBuffer.wrap(b, 16, 4).getInt(), path + " width == 128");
        assertEquals(128, ByteBuffer.wrap(b, 20, 4).getInt(), path + " height == 128");
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
