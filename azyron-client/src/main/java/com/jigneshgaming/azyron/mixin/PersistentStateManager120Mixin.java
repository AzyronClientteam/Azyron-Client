package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.AzyronClient;
import com.jigneshgaming.azyron.util.PersistentStateGuard;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Minecraft 1.20.1 PersistentStateManager safeguard.
 * Protects user world .dat files (including raids.dat / raids_end.dat) against 0-byte truncation,
 * GZip corruption, and malformed RaidManager NBT tags — always backing up untouched files first.
 */
@Mixin(PersistentStateManager.class)
public abstract class PersistentStateManager120Mixin {
    @Shadow(remap = false)
    @Final
    private File field_17664;

    @Unique
    private Path azyron$resolveFile120(String id) {
        if (this.field_17664 == null || id == null || id.isEmpty()) {
            return null;
        }
        return new File(this.field_17664, id + ".dat").toPath();
    }

    @Inject(
            method = "method_120(Ljava/util/function/Function;Ljava/lang/String;)Lnet/minecraft/class_18;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private <T extends PersistentState> void azyron$guardZeroBytePersistentState120(
            Function<NbtCompound, T> readFunction,
            String id,
            CallbackInfoReturnable<T> cir
    ) {
        try {
            Path dataFile = azyron$resolveFile120(id);
            if (dataFile != null && Files.exists(dataFile) && Files.isRegularFile(dataFile) && Files.size(dataFile) == 0L) {
                PersistentStateGuard.ensureBackedUpOnLoadFailure(dataFile, id, "zero-byte-persistent-state-file");
                AzyronClient.LOGGER.warn(
                        "[AZYRON] Detected 0-byte '{}.dat' on 1.20.1; backed up original file before clean state initialization.",
                        id
                );
                cir.setReturnValue(null);
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(
            method = "method_17923(Ljava/lang/String;I)Lnet/minecraft/class_2487;",
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void azyron$repairRaidsCompound120(
            String id,
            int dataVersion,
            CallbackInfoReturnable<NbtCompound> cir
    ) {
        try {
            NbtCompound root = cir.getReturnValue();
            if (root != null) {
                Path dataFile = azyron$resolveFile120(id);
                NbtCompound repaired = PersistentStateGuard.inspectAndRepairRaidsCompound(dataFile, id, root);
                if (repaired != root) {
                    cir.setReturnValue(repaired);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(
            method = "method_120(Ljava/util/function/Function;Ljava/lang/String;)Lnet/minecraft/class_18;",
            at = @At("RETURN"),
            remap = false,
            require = 0
    )
    private <T extends PersistentState> void azyron$backupOnFailure120(
            Function<NbtCompound, T> readFunction,
            String id,
            CallbackInfoReturnable<T> cir
    ) {
        try {
            if (id != null && cir.getReturnValue() == null) {
                Path dataFile = azyron$resolveFile120(id);
                if (dataFile != null && Files.exists(dataFile) && Files.isRegularFile(dataFile)) {
                    PersistentStateGuard.ensureBackedUpOnLoadFailure(dataFile, id, "1201-read-or-parse-failure");
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
