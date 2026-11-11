package com.jigneshgaming.azyron.mixin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Version-selective SpongePowered Mixin configuration plugin for AZYRON CLIENT v1.0.3.
 *
 * Dynamically enables:
 *  - Shared Mixins across both Minecraft 1.21.11 and Minecraft 1.20.1
 *  - 1.21.11-specific Mixins (*121Mixin, GameRendererMixin, InGameHudMixin, MouseMixin,
 *    PersistentStateManagerMixin, ChunkBatchSizeCalculatorMixin, RealmsAvailabilityMixin) on 1.21.x
 *  - 1.20.1-specific Mixins (*120Mixin) on 1.20.x
 *
 * Never calls Class.forName on Minecraft classes during Mixin plugin initialization so
 * target classes are never loaded prematurely before Mixin transformation.
 */
public final class AzyronMixinPlugin implements IMixinConfigPlugin {

    private static final Set<String> MC_121_ONLY_MIXINS = Set.of(
            "ChunkBatchSizeCalculatorMixin",
            "GameRendererMixin",
            "InGameHudMixin",
            "MouseMixin",
            "PersistentStateManagerMixin",
            "RealmsAvailabilityMixin",
            "Screen121Mixin"
    );

    private boolean runtimeIsMc120 = false;

    @Override
    public void onLoad(String mixinPackage) {
        this.runtimeIsMc120 = detectMinecraft120WithoutClassLoading();
    }

    public static boolean detectMinecraft120WithoutClassLoading() {
        String prop = System.getProperty("azyron.mcVersion", "").trim();
        if (!prop.isEmpty()) {
            return prop.startsWith("1.20");
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
                        return true;
                    }
                    if (friendly.startsWith("1.21")) {
                        return false;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // Safe bytecode resource check (never triggers JVM classloading)
        ClassLoader cl = AzyronMixinPlugin.class.getClassLoader();
        if (cl != null) {
            boolean has121TickCounter = cl.getResource("net/minecraft/class_9779.class") != null
                    || cl.getResource("net/minecraft/client/render/RenderTickCounter.class") != null;
            return !has121TickCounter;
        }
        return false;
    }

    public static boolean shouldApplyMixinForVersion(String mixinClassName, boolean isMc120) {
        if (mixinClassName == null || mixinClassName.isEmpty()) {
            return false;
        }
        int dot = mixinClassName.lastIndexOf('.');
        String simpleName = dot >= 0 ? mixinClassName.substring(dot + 1) : mixinClassName;

        if (simpleName.endsWith("120Mixin")) {
            return isMc120;
        }
        if (simpleName.endsWith("121Mixin") || MC_121_ONLY_MIXINS.contains(simpleName)) {
            return !isMc120;
        }
        // Shared Mixin compatible with both 1.20.1 and 1.21.11
        return true;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return shouldApplyMixinForVersion(mixinClassName, this.runtimeIsMc120);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
