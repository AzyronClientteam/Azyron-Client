package com.jigneshgaming.azyron.util;

import com.jigneshgaming.azyron.AzyronClient;
import com.jigneshgaming.azyron.ui.AzyronLoadingRenderer;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;

/**
 * Unified Minecraft 1.20.1 + 1.21.11 Fabric compatibility bridge for AZYRON CLIENT v1.0.3.
 *
 * Pre-resolves runtime MethodHandles once at startup so a single Azyron-Client-1.0.3.jar
 * (compiled to Java 17 bytecode, classfile v61.0) runs natively on both:
 *  - Minecraft 1.21.11 (Fabric, Java 21+)
 *  - Minecraft 1.20.1  (Fabric, Java 17+)
 * with zero per-frame reflection allocations and zero missing Intermediary symbol references.
 */
public final class McVersionBridge {

    public enum SupportedMcVersion {
        MC_1_21_11(
                "1.21.11",
                21,
                "0.20.1",
                "0.141.6+1.21.11",
                "Sodium 0.8.14 • Lithium 0.21.4 • FerriteCore 8.2.0 • EntityCulling 1.11.3 • MoreCulling 1.6.2 • DynamicFPS 3.11.6"
        ),
        MC_1_20_1(
                "1.20.1",
                17,
                "0.14.22+",
                "0.88.1+1.20.1",
                "Sodium 0.5.3 • Lithium 0.11.2 • FerriteCore 6.0.0 • EntityCulling 1.6.2 • MoreCulling 0.19.0 • DynamicFPS 3.2.1"
        );

        private final String versionId;
        private final int minJavaVersion;
        private final String recommendedLoader;
        private final String fabricApiVersion;
        private final String modStackSummary;

        SupportedMcVersion(
                String versionId,
                int minJavaVersion,
                String recommendedLoader,
                String fabricApiVersion,
                String modStackSummary
        ) {
            this.versionId = versionId;
            this.minJavaVersion = minJavaVersion;
            this.recommendedLoader = recommendedLoader;
            this.fabricApiVersion = fabricApiVersion;
            this.modStackSummary = modStackSummary;
        }

        public String getVersionId() { return versionId; }
        public int getMinJavaVersion() { return minJavaVersion; }
        public String getRecommendedLoader() { return recommendedLoader; }
        public String getFabricApiVersion() { return fabricApiVersion; }
        public String getModStackSummary() { return modStackSummary; }

        public SupportedMcVersion next() {
            return this == MC_1_21_11 ? MC_1_20_1 : MC_1_21_11;
        }

        public static SupportedMcVersion fromString(String raw, SupportedMcVersion fallback) {
            if (raw == null || raw.isBlank()) {
                return fallback;
            }
            String trimmed = raw.trim();
            if (trimmed.startsWith("1.20") || "MC_1_20_1".equalsIgnoreCase(trimmed)) {
                return MC_1_20_1;
            }
            if (trimmed.startsWith("1.21") || "MC_1_21_11".equalsIgnoreCase(trimmed)) {
                return MC_1_21_11;
            }
            return fallback;
        }
    }

    private static volatile SupportedMcVersion simulatedVersionOverride = null;
    private static final SupportedMcVersion DETECTED_RUNTIME_VERSION = detectRuntimeVersionInternal();

    // Cached MethodHandles for zero-overhead per-frame HUD & UI calls
    private static final MethodHandle MH_DRAW_TEXT_SHADOW;
    private static final MethodHandle MH_GET_MATRICES;
    private static volatile MethodHandle mhMatrixPush;
    private static volatile MethodHandle mhMatrixPop;
    private static volatile MethodHandle mhMatrixTranslate2D;
    private static volatile MethodHandle mhMatrixTranslate3D;
    private static volatile MethodHandle mhMatrixScale2D;
    private static volatile MethodHandle mhMatrixScale3D;

    private static final MethodHandle MH_DRAW_LOGO_121;
    private static final Object GUI_TEXTURED_PIPELINE_121;
    private static final MethodHandle MH_DRAW_LOGO_120;

    private static final MethodHandle MH_GET_SHADER_LOADER_121;
    private static volatile MethodHandle mhShaderGetSource121;
    private static final Object SHADER_TYPE_VERTEX_121;
    private static final Object SHADER_TYPE_FRAGMENT_121;
    private static volatile Object cachedVerifiedShaderLoader = null;

    private static final MethodHandle MH_STATUS_EFFECT_TYPE;
    private static volatile MethodHandle mhRegistryEntryHasKeyAndValue;
    private static volatile MethodHandle mhRegistryEntryValue;

    private static final MethodHandle MH_APPLY_GRAPHICS_121;
    private static final MethodHandle MH_GET_GRAPHICS_OPT_120;
    private static final MethodHandle MH_GET_VIGNETTE_121;

    private static final MethodHandle MH_CONFIRM_LINK_121;
    private static final MethodHandle MH_CONFIRM_LINK_120;

    private static final MethodHandle MH_PANORAMA_121;
    private static final MethodHandle MH_RENDER_BG_120;

    private static final MethodHandle MH_NBT_GET_STRING;
    private static final MethodHandle MH_NBT_GET_INT;
    private static final MethodHandle MH_NBT_GET_COMPOUND;
    private static final MethodHandle MH_NBT_GET_LIST_121;
    private static final MethodHandle MH_NBT_GET_LIST_120;
    private static final MethodHandle MH_NBTLIST_GET_COMPOUND;

    private static Object cachedKeyCategory121 = null;

    static {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MH_DRAW_TEXT_SHADOW = findMethodByNames(
                lookup,
                DrawContext.class,
                new String[]{"method_25303", "drawTextWithShadow"},
                TextRenderer.class, String.class, int.class, int.class, int.class
        );
        MH_GET_MATRICES = findMethodByNames(
                lookup,
                DrawContext.class,
                new String[]{"method_51448", "getMatrices"}
        );

        Object pipeline = null;
        MethodHandle logo121 = null;
        try {
            Class<?> pipelinesCls = loadFirstExisting(
                    "net.minecraft.class_10799",
                    "net.minecraft.client.gl.RenderPipelines"
            );
            Class<?> pipelineCls = loadFirstExisting("com.mojang.blaze3d.pipeline.RenderPipeline");
            if (pipelinesCls != null && pipelineCls != null) {
                Field f = findFieldByNames(pipelinesCls, "field_56883", "GUI_TEXTURED");
                if (f != null) {
                    pipeline = f.get(null);
                    logo121 = findMethodByNames(
                            lookup,
                            DrawContext.class,
                            new String[]{"method_25290", "drawTexture"},
                            pipelineCls, Identifier.class,
                            int.class, int.class, float.class, float.class,
                            int.class, int.class, int.class, int.class
                    );
                }
            }
        } catch (Throwable ignored) {
        }
        GUI_TEXTURED_PIPELINE_121 = pipeline;
        MH_DRAW_LOGO_121 = logo121;
        MH_DRAW_LOGO_120 = findMethodByNames(
                lookup,
                DrawContext.class,
                new String[]{"method_25290", "drawTexture"},
                Identifier.class,
                int.class, int.class, float.class, float.class,
                int.class, int.class, int.class, int.class
        );

        MH_GET_SHADER_LOADER_121 = findMethodByNames(
                lookup,
                MinecraftClient.class,
                new String[]{"method_62887", "getShaderLoader"}
        );
        Object vType = null;
        Object fType = null;
        try {
            Class<?> stCls = loadFirstExisting("com.mojang.blaze3d.shaders.ShaderType");
            if (stCls != null && stCls.isEnum()) {
                for (Object c : stCls.getEnumConstants()) {
                    String n = ((Enum<?>) c).name();
                    if ("VERTEX".equals(n)) vType = c;
                    else if ("FRAGMENT".equals(n)) fType = c;
                }
            }
        } catch (Throwable ignored) {
        }
        SHADER_TYPE_VERTEX_121 = vType;
        SHADER_TYPE_FRAGMENT_121 = fType;

        MH_STATUS_EFFECT_TYPE = findMethodByNames(
                lookup,
                StatusEffectInstance.class,
                new String[]{"method_5579", "getEffectType"}
        );

        MH_APPLY_GRAPHICS_121 = findMethodByNames(
                lookup,
                GameOptions.class,
                new String[]{"method_74428", "applyGraphicsMode"},
                GraphicsMode.class
        );
        MH_GET_GRAPHICS_OPT_120 = findMethodByNames(
                lookup,
                GameOptions.class,
                new String[]{"method_42534", "getGraphicsMode"}
        );
        MH_GET_VIGNETTE_121 = findMethodByNames(
                lookup,
                GameOptions.class,
                new String[]{"method_75335", "getVignette"}
        );

        MH_CONFIRM_LINK_121 = findStaticMethodByNames(
                lookup,
                ConfirmLinkScreen.class,
                new String[]{"method_60866", "open"},
                Screen.class, String.class, boolean.class
        );
        MH_CONFIRM_LINK_120 = findStaticMethodByNames(
                lookup,
                ConfirmLinkScreen.class,
                new String[]{"method_49623", "open"},
                String.class, Screen.class, boolean.class
        );

        MH_PANORAMA_121 = findDeclaredMethodHandle(
                Screen.class,
                new String[]{"method_57728", "renderPanoramaBackground"},
                DrawContext.class, float.class
        );
        MH_RENDER_BG_120 = findDeclaredMethodHandle(
                Screen.class,
                new String[]{"method_25420", "renderBackground"},
                DrawContext.class
        );

        MH_NBT_GET_STRING = findMethodByNames(
                lookup, NbtCompound.class, new String[]{"method_10558", "getString"}, String.class
        );
        MH_NBT_GET_INT = findMethodByNames(
                lookup, NbtCompound.class, new String[]{"method_10550", "getInt"}, String.class
        );
        MH_NBT_GET_COMPOUND = findMethodByNames(
                lookup, NbtCompound.class, new String[]{"method_10562", "getCompound"}, String.class
        );
        MH_NBT_GET_LIST_121 = findMethodByNames(
                lookup, NbtCompound.class, new String[]{"method_10554", "getList"}, String.class
        );
        MH_NBT_GET_LIST_120 = findMethodByNames(
                lookup, NbtCompound.class, new String[]{"method_10554", "getList"}, String.class, int.class
        );
        MH_NBTLIST_GET_COMPOUND = findMethodByNames(
                lookup, NbtList.class, new String[]{"method_10602", "getCompound"}, int.class
        );
    }

    private McVersionBridge() {
    }

    private static SupportedMcVersion detectRuntimeVersionInternal() {
        String sysProp = System.getProperty("azyron.mcVersion", "").trim();
        if (!sysProp.isEmpty()) {
            return SupportedMcVersion.fromString(sysProp, SupportedMcVersion.MC_1_21_11);
        }
        try {
            FabricLoader loader = FabricLoader.getInstance();
            if (loader != null) {
                Method getModContainer = FabricLoader.class.getMethod("getModContainer", String.class);
                Object optObj = getModContainer.invoke(loader, "minecraft");
                if (optObj instanceof Optional<?> opt && opt.isPresent()) {
                    Object container = opt.get();
                    Object metadata = container.getClass().getMethod("getMetadata").invoke(container);
                    Object version = metadata.getClass().getMethod("getVersion").invoke(metadata);
                    String friendly = String.valueOf(version.getClass().getMethod("getFriendlyString").invoke(version));
                    if (friendly.startsWith("1.20")) {
                        return SupportedMcVersion.MC_1_20_1;
                    }
                    if (friendly.startsWith("1.21")) {
                        return SupportedMcVersion.MC_1_21_11;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // Class-presence fallback: RenderTickCounter (class_9779) exists in 1.21.x, absent in 1.20.1
        if (loadFirstExisting("net.minecraft.class_9779", "net.minecraft.client.render.RenderTickCounter") == null) {
            return SupportedMcVersion.MC_1_20_1;
        }
        return SupportedMcVersion.MC_1_21_11;
    }

    public static SupportedMcVersion getRuntimeMcVersion() {
        SupportedMcVersion override = simulatedVersionOverride;
        return override != null ? override : DETECTED_RUNTIME_VERSION;
    }

    public static String getActiveMcVersionString() {
        return getRuntimeMcVersion().getVersionId();
    }

    public static boolean isMinecraft120() {
        return getRuntimeMcVersion() == SupportedMcVersion.MC_1_20_1;
    }

    public static boolean isMinecraft121() {
        return getRuntimeMcVersion() == SupportedMcVersion.MC_1_21_11;
    }

    public static void setSimulatedMcVersionForTests(SupportedMcVersion version) {
        simulatedVersionOverride = version;
    }

    public static Identifier createIdentifier(String namespace, String path) {
        Identifier parsed = Identifier.tryParse(namespace + ":" + path);
        if (parsed != null) {
            return parsed;
        }
        throw new IllegalArgumentException("Invalid identifier: " + namespace + ":" + path);
    }

    public static void drawTextWithShadow(DrawContext ctx, TextRenderer font, String text, int x, int y, int color) {
        if (ctx == null || font == null || text == null || text.isEmpty() || MH_DRAW_TEXT_SHADOW == null) {
            return;
        }
        try {
            MH_DRAW_TEXT_SHADOW.invoke(ctx, font, text, x, y, color);
        } catch (Throwable t) {
            AzyronClient.LOGGER.debug("[AZYRON] drawTextWithShadow bridge fallback: {}", t.getMessage());
        }
    }

    public static void drawCenteredTextWithShadow(DrawContext ctx, TextRenderer font, String text, int centerX, int y, int color) {
        if (ctx == null || font == null || text == null || text.isEmpty()) {
            return;
        }
        int width = font.getWidth(text);
        drawTextWithShadow(ctx, font, text, centerX - width / 2, y, color);
    }

    public static boolean pushMatrixTranslateScale(DrawContext ctx, float tx, float ty, float scale) {
        if (ctx == null || MH_GET_MATRICES == null) {
            return false;
        }
        try {
            Object matrices = MH_GET_MATRICES.invoke(ctx);
            if (matrices == null) {
                return false;
            }
            ensureMatrixHandlesBound(matrices.getClass());
            if (mhMatrixPush == null || mhMatrixPop == null) {
                return false;
            }
            mhMatrixPush.invoke(matrices);
            if (mhMatrixTranslate2D != null) {
                mhMatrixTranslate2D.invoke(matrices, tx, ty);
            } else if (mhMatrixTranslate3D != null) {
                mhMatrixTranslate3D.invoke(matrices, tx, ty, 0.0f);
            }
            if (scale != 1.0f) {
                if (mhMatrixScale2D != null) {
                    mhMatrixScale2D.invoke(matrices, scale, scale);
                } else if (mhMatrixScale3D != null) {
                    mhMatrixScale3D.invoke(matrices, scale, scale, 1.0f);
                }
            }
            return true;
        } catch (Throwable t) {
            AzyronClient.LOGGER.debug("[AZYRON] Matrix push/scale bridge warning: {}", t.getMessage());
            return false;
        }
    }

    public static void popMatrix(DrawContext ctx) {
        if (ctx == null || MH_GET_MATRICES == null || mhMatrixPop == null) {
            return;
        }
        try {
            Object matrices = MH_GET_MATRICES.invoke(ctx);
            if (matrices != null) {
                mhMatrixPop.invoke(matrices);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void ensureMatrixHandlesBound(Class<?> matrixCls) {
        if (mhMatrixPush != null) {
            return;
        }
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        // 1.21.11 JOML Matrix3x2fStack vs 1.20.1 net.minecraft.client.util.math.MatrixStack (class_4587)
        mhMatrixPush = findMethodByNames(lookup, matrixCls, new String[]{"pushMatrix", "method_22903", "push"});
        mhMatrixPop = findMethodByNames(lookup, matrixCls, new String[]{"popMatrix", "method_22909", "pop"});
        mhMatrixTranslate2D = findMethodByNames(lookup, matrixCls, new String[]{"translate"}, float.class, float.class);
        mhMatrixTranslate3D = findMethodByNames(lookup, matrixCls, new String[]{"method_46416", "translate"}, float.class, float.class, float.class);
        mhMatrixScale2D = findMethodByNames(lookup, matrixCls, new String[]{"scale"}, float.class, float.class);
        mhMatrixScale3D = findMethodByNames(lookup, matrixCls, new String[]{"method_22905", "scale"}, float.class, float.class, float.class);
    }

    public static void drawGuiLogoTexture(DrawContext ctx, Identifier texture, int x, int y, int size) {
        if (ctx == null || texture == null) {
            return;
        }
        try {
            if (MH_DRAW_LOGO_121 != null && GUI_TEXTURED_PIPELINE_121 != null) {
                MH_DRAW_LOGO_121.invoke(ctx, GUI_TEXTURED_PIPELINE_121, texture, x, y, 0.0f, 0.0f, size, size, size, size);
            } else if (MH_DRAW_LOGO_120 != null) {
                MH_DRAW_LOGO_120.invoke(ctx, texture, x, y, 0.0f, 0.0f, size, size, size, size);
            }
        } catch (Throwable ignored) {
            // Safe fallback if resource manager is mid-reload
        }
    }

    public static boolean isCoreTextShaderReady(MinecraftClient client, Identifier coreTextShaderId) {
        if (client == null || client.textRenderer == null) {
            return false;
        }
        try {
            boolean splashActive = client.getOverlay() instanceof SplashOverlay;
            if (splashActive || !AzyronLoadingRenderer.isCoreResourcesReady()) {
                cachedVerifiedShaderLoader = null;
                if (!AzyronLoadingRenderer.isCoreResourcesReady()) {
                    return false;
                }
            }
            if (MH_GET_SHADER_LOADER_121 != null && SHADER_TYPE_VERTEX_121 != null && SHADER_TYPE_FRAGMENT_121 != null) {
                Object shaderLoader = MH_GET_SHADER_LOADER_121.invoke(client);
                if (shaderLoader != null) {
                    if (!splashActive && shaderLoader == cachedVerifiedShaderLoader) {
                        return true;
                    }
                    if (mhShaderGetSource121 == null) {
                        mhShaderGetSource121 = findMethodByNames(
                                MethodHandles.publicLookup(),
                                shaderLoader.getClass(),
                                new String[]{"method_62947", "getSource"},
                                Identifier.class,
                                SHADER_TYPE_VERTEX_121.getClass()
                        );
                    }
                    if (mhShaderGetSource121 != null) {
                        Object vert = mhShaderGetSource121.invoke(shaderLoader, coreTextShaderId, SHADER_TYPE_VERTEX_121);
                        Object frag = mhShaderGetSource121.invoke(shaderLoader, coreTextShaderId, SHADER_TYPE_FRAGMENT_121);
                        boolean ready = (vert instanceof String vs && !vs.isEmpty())
                                && (frag instanceof String fs && !fs.isEmpty());
                        if (ready && !splashActive) {
                            cachedVerifiedShaderLoader = shaderLoader;
                        }
                        return ready;
                    }
                }
            }
        } catch (Throwable ignored) {
            return AzyronLoadingRenderer.isCoreResourcesReady();
        }
        return AzyronLoadingRenderer.isCoreResourcesReady();
    }

    public static KeyBinding createKeyBinding(String translationKey, int glfwKeyCode, String categoryNs, String categoryPath) {
        try {
            // 1.21.11: KeyBinding(String, int, KeyBinding.Category)
            Class<?> catCls = loadFirstExisting(
                    "net.minecraft.class_304$class_11900",
                    "net.minecraft.client.option.KeyBinding$Category"
            );
            if (catCls != null) {
                if (cachedKeyCategory121 == null) {
                    Method createCat = findRawStaticMethod(catCls, new String[]{"method_74698", "create"}, Identifier.class);
                    if (createCat != null) {
                        cachedKeyCategory121 = createCat.invoke(null, createIdentifier(categoryNs, categoryPath));
                    }
                }
                if (cachedKeyCategory121 != null) {
                    Constructor<KeyBinding> ctor = KeyBinding.class.getConstructor(String.class, int.class, catCls);
                    return ctor.newInstance(translationKey, glfwKeyCode, cachedKeyCategory121);
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            // 1.20.1: KeyBinding(String, int, String)
            Constructor<KeyBinding> ctor120 = KeyBinding.class.getConstructor(String.class, int.class, String.class);
            return ctor120.newInstance(translationKey, glfwKeyCode, "key.categories." + categoryNs);
        } catch (Throwable t) {
            throw new IllegalStateException("Unable to construct KeyBinding for " + translationKey, t);
        }
    }

    public static StatusEffect resolveStatusEffect(StatusEffectInstance inst) {
        if (inst == null || MH_STATUS_EFFECT_TYPE == null) {
            return null;
        }
        try {
            Object raw = MH_STATUS_EFFECT_TYPE.invoke(inst);
            if (raw == null) {
                return null;
            }
            if (raw instanceof StatusEffect direct) {
                // Minecraft 1.20.1: getEffectType() returns StatusEffect directly
                return direct;
            }
            // Minecraft 1.21.11: getEffectType() returns RegistryEntry<StatusEffect>
            if (mhRegistryEntryHasKeyAndValue == null) {
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                mhRegistryEntryHasKeyAndValue = findMethodByNames(
                        lookup, raw.getClass(), new String[]{"method_40230", "hasKeyAndValue"}
                );
                mhRegistryEntryValue = findMethodByNames(
                        lookup, raw.getClass(), new String[]{"comp_349", "value"}
                );
            }
            if (mhRegistryEntryHasKeyAndValue != null) {
                boolean bound = (boolean) mhRegistryEntryHasKeyAndValue.invoke(raw);
                if (!bound) {
                    return null;
                }
            }
            if (mhRegistryEntryValue != null) {
                Object val = mhRegistryEntryValue.invoke(raw);
                if (val instanceof StatusEffect se) {
                    return se;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static void setGraphicsMode(GameOptions opts, GraphicsMode mode) {
        if (opts == null || mode == null) {
            return;
        }
        try {
            if (MH_APPLY_GRAPHICS_121 != null) {
                MH_APPLY_GRAPHICS_121.invoke(opts, mode);
                return;
            }
            if (MH_GET_GRAPHICS_OPT_120 != null) {
                Object optObj = MH_GET_GRAPHICS_OPT_120.invoke(opts);
                if (optObj instanceof SimpleOption<?> simpleOpt) {
                    ((SimpleOption<GraphicsMode>) simpleOpt).setValue(mode);
                }
            }
        } catch (Throwable t) {
            AzyronClient.LOGGER.debug("[AZYRON] setGraphicsMode bridge warning: {}", t.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static void setVignette(GameOptions opts, boolean enabled) {
        if (opts == null || MH_GET_VIGNETTE_121 == null) {
            return;
        }
        try {
            Object optObj = MH_GET_VIGNETTE_121.invoke(opts);
            if (optObj instanceof SimpleOption<?> simpleOpt) {
                ((SimpleOption<Boolean>) simpleOpt).setValue(enabled);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void openConfirmLink(Screen parent, String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            if (MH_CONFIRM_LINK_121 != null) {
                MH_CONFIRM_LINK_121.invoke(parent, url, true);
            } else if (MH_CONFIRM_LINK_120 != null) {
                MH_CONFIRM_LINK_120.invoke(url, parent, true);
            }
        } catch (Throwable t) {
            AzyronClient.LOGGER.warn("[AZYRON] Unable to open ConfirmLinkScreen: {}", t.getMessage());
        }
    }

    public static void renderMainMenuBackground(Screen screen, DrawContext ctx, float delta) {
        if (screen == null || ctx == null) {
            return;
        }
        try {
            if (MH_PANORAMA_121 != null) {
                MH_PANORAMA_121.invoke(screen, ctx, delta);
                return;
            }
            if (MH_RENDER_BG_120 != null) {
                MH_RENDER_BG_120.invoke(screen, ctx);
                return;
            }
        } catch (Throwable ignored) {
        }
        ctx.fillGradient(0, 0, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), 0xFF080A16, 0xFF0D1024);
    }

    // =========================================================================
    // Version-Neutral NBT Accessors (1.21.11 Optional<T> vs 1.20.1 direct T)
    // =========================================================================

    public static Optional<String> nbtGetString(NbtCompound nbt, String key) {
        if (nbt == null || key == null || !nbt.contains(key) || MH_NBT_GET_STRING == null) {
            return Optional.empty();
        }
        try {
            Object res = MH_NBT_GET_STRING.invoke(nbt, key);
            if (res instanceof Optional<?> opt) {
                return opt.map(String::valueOf);
            }
            if (res instanceof String s) {
                return Optional.of(s);
            }
        } catch (Throwable ignored) {
        }
        return Optional.empty();
    }

    public static Optional<Integer> nbtGetInt(NbtCompound nbt, String key) {
        if (nbt == null || key == null || !nbt.contains(key) || MH_NBT_GET_INT == null) {
            return Optional.empty();
        }
        try {
            Object res = MH_NBT_GET_INT.invoke(nbt, key);
            if (res instanceof Optional<?> opt) {
                if (opt.isPresent() && opt.get() instanceof Number n) {
                    return Optional.of(n.intValue());
                }
                return Optional.empty();
            }
            if (res instanceof Number n) {
                return Optional.of(n.intValue());
            }
        } catch (Throwable ignored) {
        }
        return Optional.empty();
    }

    public static Optional<NbtCompound> nbtGetCompound(NbtCompound nbt, String key) {
        if (nbt == null || key == null || !nbt.contains(key) || MH_NBT_GET_COMPOUND == null) {
            return Optional.empty();
        }
        try {
            Object res = MH_NBT_GET_COMPOUND.invoke(nbt, key);
            if (res instanceof Optional<?> opt) {
                if (opt.isPresent() && opt.get() instanceof NbtCompound c) {
                    return Optional.of(c);
                }
                return Optional.empty();
            }
            if (res instanceof NbtCompound c) {
                return Optional.of(c);
            }
        } catch (Throwable ignored) {
        }
        return Optional.empty();
    }

    public static Optional<NbtList> nbtGetCompoundList(NbtCompound nbt, String key) {
        if (nbt == null || key == null || !nbt.contains(key)) {
            return Optional.empty();
        }
        try {
            if (MH_NBT_GET_LIST_121 != null) {
                Object res = MH_NBT_GET_LIST_121.invoke(nbt, key);
                if (res instanceof Optional<?> opt) {
                    if (opt.isPresent() && opt.get() instanceof NbtList list) {
                        return Optional.of(list);
                    }
                    return Optional.empty();
                }
                if (res instanceof NbtList list) {
                    return Optional.of(list);
                }
            }
            if (MH_NBT_GET_LIST_120 != null) {
                // NbtElement.COMPOUND_TYPE == 10 in Minecraft 1.20.1
                Object res = MH_NBT_GET_LIST_120.invoke(nbt, key, 10);
                if (res instanceof NbtList list) {
                    return Optional.of(list);
                }
            }
        } catch (Throwable ignored) {
        }
        return Optional.empty();
    }

    public static Optional<NbtCompound> nbtListGetCompound(NbtList list, int index) {
        if (list == null || index < 0 || index >= list.size() || MH_NBTLIST_GET_COMPOUND == null) {
            return Optional.empty();
        }
        try {
            Object res = MH_NBTLIST_GET_COMPOUND.invoke(list, index);
            if (res instanceof Optional<?> opt) {
                if (opt.isPresent() && opt.get() instanceof NbtCompound c) {
                    return Optional.of(c);
                }
                return Optional.empty();
            }
            if (res instanceof NbtCompound c && !c.isEmpty()) {
                return Optional.of(c);
            }
        } catch (Throwable ignored) {
        }
        return Optional.empty();
    }

    // =========================================================================
    // Reflection Lookup Helpers
    // =========================================================================

    private static Class<?> loadFirstExisting(String... names) {
        ClassLoader cl = McVersionBridge.class.getClassLoader();
        for (String name : names) {
            try {
                return Class.forName(name, false, cl);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Field findFieldByNames(Class<?> owner, String... names) {
        for (String n : names) {
            try {
                Field f = owner.getField(n);
                f.setAccessible(true);
                return f;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Method findRawStaticMethod(Class<?> owner, String[] names, Class<?>... params) {
        for (String n : names) {
            try {
                Method m = owner.getMethod(n, params);
                m.setAccessible(true);
                return m;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static MethodHandle findMethodByNames(
            MethodHandles.Lookup lookup,
            Class<?> owner,
            String[] names,
            Class<?>... params
    ) {
        if (owner == null) {
            return null;
        }
        for (String n : names) {
            try {
                Method m = owner.getMethod(n, params);
                return lookup.unreflect(m);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static MethodHandle findStaticMethodByNames(
            MethodHandles.Lookup lookup,
            Class<?> owner,
            String[] names,
            Class<?>... params
    ) {
        return findMethodByNames(lookup, owner, names, params);
    }

    private static MethodHandle findDeclaredMethodHandle(
            Class<?> owner,
            String[] names,
            Class<?>... params
    ) {
        if (owner == null) {
            return null;
        }
        for (String n : names) {
            Class<?> cur = owner;
            while (cur != null) {
                try {
                    Method m = cur.getDeclaredMethod(n, params);
                    m.setAccessible(true);
                    return MethodHandles.lookup().unreflect(m);
                } catch (Throwable ignored) {
                    cur = cur.getSuperclass();
                }
            }
        }
        return null;
    }
}
