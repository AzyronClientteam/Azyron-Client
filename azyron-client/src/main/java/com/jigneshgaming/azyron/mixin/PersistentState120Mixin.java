package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.util.PersistentStateGuard;
import java.io.File;
import java.io.IOException;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Minecraft 1.20.1 atomic PersistentState save hook.
 * Redirects NbtIo.writeCompressed(NbtCompound, File) inside PersistentState.save(File) (method_17919)
 * to PersistentStateGuard.saveCompressedAtomically so sudden crashes never leave half-written .dat files.
 */
@Mixin(PersistentState.class)
public abstract class PersistentState120Mixin {
    @Redirect(
            method = "method_17919(Ljava/io/File;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/class_2507;method_10630(Lnet/minecraft/class_2487;Ljava/io/File;)V"
            ),
            remap = false,
            require = 0
    )
    private void azyron$atomicSave120(NbtCompound nbt, File targetFile) throws IOException {
        if (targetFile != null && nbt != null) {
            PersistentStateGuard.saveCompressedAtomically(targetFile.toPath(), nbt);
        }
    }
}
