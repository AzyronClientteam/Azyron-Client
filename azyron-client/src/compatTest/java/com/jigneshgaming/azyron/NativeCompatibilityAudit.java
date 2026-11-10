package com.jigneshgaming.azyron;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jigneshgaming.azyron.profile.IntegratedServerOptimizer;
import com.jigneshgaming.azyron.profile.MemoryOptimizer;
import com.jigneshgaming.azyron.profile.PerformanceEngine;
import com.jigneshgaming.azyron.util.McVersionBridge;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/** Structural checks against REAL named Minecraft bytecode on Loom's classpath.
 * Does not initialize the game or substitute Minecraft/Fabric classes. Not an FPS test.
 */
public final class NativeCompatibilityAudit {
    private static final Map<String, ClassNode> classes = new HashMap<>();
    private static int checks;
    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    private static ClassNode node(String name) throws Exception {
        if (classes.containsKey(name)) return classes.get(name);
        try (InputStream in = NativeCompatibilityAudit.class.getClassLoader().getResourceAsStream(name + ".class")) {
            if (in == null) throw new AssertionError("Missing real class: " + name);
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, 0);
            classes.put(name, node);
            return node;
        }
    }
    private static List<AnnotationNode> annotations(List<AnnotationNode> a, List<AnnotationNode> b) {
        List<AnnotationNode> all = new ArrayList<>();
        if (a != null) all.addAll(a);
        if (b != null) all.addAll(b);
        return all;
    }
    private static Object value(AnnotationNode annotation, String key) {
        if (annotation.values != null) for (int i = 0; i < annotation.values.size(); i += 2)
            if (key.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
        return null;
    }
    private static boolean hasField(ClassNode cls, String name, String desc) throws Exception {
        for (FieldNode f : cls.fields) if (f.name.equals(name) && f.desc.equals(desc)) return true;
        return cls.superName != null && !cls.superName.equals("java/lang/Object") && hasField(node(cls.superName), name, desc);
    }
    private static List<MethodNode> methods(ClassNode cls, String selector) throws Exception {
        int start = selector.indexOf('(');
        String name = start < 0 ? selector : selector.substring(0, start);
        String desc = start < 0 ? null : selector.substring(start);
        List<MethodNode> matches = new ArrayList<>();
        for (MethodNode m : cls.methods)
            if (m.name.equals(name) && (desc == null || m.desc.equals(desc))) matches.add(m);
        if (matches.isEmpty() && cls.superName != null && !cls.superName.equals("java/lang/Object") && !name.equals("<init>"))
            matches.addAll(methods(node(cls.superName), selector));
        return matches;
    }
    private static void verifyMixin(String name) throws Exception {
        ClassNode mixin = node(name.replace('.', '/'));
        ClassNode target = null;
        for (AnnotationNode annotation : annotations(mixin.visibleAnnotations, mixin.invisibleAnnotations)) {
            if (!annotation.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")) continue;
            Object types = value(annotation, "value");
            if (types instanceof List<?> list && !list.isEmpty()) target = node(((Type) list.get(0)).getInternalName());
        }
        check(target != null, name + " has a real @Mixin target");
        for (FieldNode field : mixin.fields) {
            for (AnnotationNode a : annotations(field.visibleAnnotations, field.invisibleAnnotations))
                if (a.desc.endsWith("/Shadow;"))
                    check(hasField(target, field.name, field.desc), name + " missing shadow field " + field.name + field.desc);
        }
        for (MethodNode method : mixin.methods) {
            for (AnnotationNode a : annotations(method.visibleAnnotations, method.invisibleAnnotations)) {
                if (a.desc.endsWith("/Shadow;"))
                    check(!methods(target, method.name + method.desc).isEmpty(), name + " missing shadow method " + method.name + method.desc);
                if (!a.desc.endsWith("/Inject;") && !a.desc.endsWith("/Redirect;")) continue;
                Object selectors = value(a, "method");
                if (!(selectors instanceof List<?> list)) throw new AssertionError("No injection selectors: " + name);
                List<MethodNode> found = new ArrayList<>();
                for (Object selector : list) found.addAll(methods(target, selector.toString()));
                check(!found.isEmpty(), name + "." + method.name + " missing injection target " + list);
                if (a.desc.endsWith("/Inject;")) {
                    Type[] handler = Type.getArgumentTypes(method.desc);
                    boolean compatible = false;
                    for (MethodNode candidate : found) {
                        Type[] expected = Type.getArgumentTypes(candidate.desc);
                        boolean captures = handler.length == expected.length + 1;
                        if (captures) for (int i = 0; i < expected.length; i++) captures &= expected[i].equals(handler[i]);
                        compatible |= handler.length == 1 || captures;
                    }
                    check(compatible, name + "." + method.name + " incompatible callback arguments " + method.desc
                            + " versus " + found.stream().map(m -> m.name + m.desc).toList());
                }
                if (a.desc.endsWith("/Redirect;")) {
                    Object atValue = value(a, "at");
                    List<?> ats = atValue instanceof List<?> atList ? atList : List.of(atValue);
                    for (Object at : ats) {
                        String invoke = (String) value((AnnotationNode) at, "target");
                        if (invoke == null) continue;
                        boolean called = false;
                        for (MethodNode m : found) for (AbstractInsnNode instruction : m.instructions) {
                            if (instruction instanceof MethodInsnNode call)
                                called |= invoke.equals("L" + call.owner + ";" + call.name + call.desc);
                        }
                        check(called, name + " missing redirected invocation " + invoke);
                    }
                }
            }
        }
    }
    public static void main(String[] args) throws Exception {
        String mc = args[0];
        try (InputStream in = NativeCompatibilityAudit.class.getResourceAsStream("/azyron-client.mixins.json")) {
            check(in != null, "Processed mixin configuration exists");
            JsonObject config = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            for (String group : List.of("mixins", "client")) for (var item : config.getAsJsonArray(group)) {
                String simple = item.getAsString();
                check(mc.equals("1.20.1") || !simple.endsWith("120Mixin"), "Legacy mixin leaked into modern artifact");
                verifyMixin(config.get("package").getAsString() + "." + simple);
            }
        }
        MemoryOptimizer.setSimulatedRamForTesting(1408, 3072);
        check(MemoryOptimizer.detectRamTier() == MemoryOptimizer.RamTier.TIER_3GB, "3GB tier detection");
        check(MemoryOptimizer.detectRamTier().getMaxSafeViewDistance() == 6, "Shared render distance cap");
        check(IntegratedServerOptimizer.computeDesiredSimStepDown(80, true, true, 5, 0) == 0, "Safe simulation floor");
        check(IntegratedServerOptimizer.computeDesiredSimStepDown(80, true, true, 6, 0) == 1, "Shared overload pacing");
        var version = mc.equals("1.20.1") ? McVersionBridge.SupportedMcVersion.MC_1_20_1 : McVersionBridge.SupportedMcVersion.MC_1_21_11;
        JsonObject sodium = JsonParser.parseString(PerformanceEngine.buildSodiumOptionsJson(version, 1, 2)).getAsJsonObject();
        check(sodium.getAsJsonObject("performance").get("chunk_builder_threads").getAsInt() == 1, "Shared bounded chunk workers");
        JsonObject dynamic = JsonParser.parseString(PerformanceEngine.buildDynamicFpsOptionsJson(version)).getAsJsonObject();
        for (var state : dynamic.getAsJsonObject("states").entrySet())
            check(!state.getValue().getAsJsonObject().get("run_garbage_collector").getAsBoolean(), "No forced GC");
        System.out.println("PASS real-classpath compatibility audit MC " + mc + ": " + checks + " checks; NOT a game/FPS benchmark");
    }
}
