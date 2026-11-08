package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.AzyronClient;
import com.jigneshgaming.azyron.util.PersistentStateGuard;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Protects user world PersistentState (.dat) files — especially raids.dat / raids_end.dat —
 * against truncation, GZip corruption, 1.21.11 Codec schema mismatches, and mid-save crashes.
 * Always creates a verified timestamped backup before any repair or clean fallback initialization,
 * and never deletes or overwrites user worlds or saved data.
 */
@Mixin(PersistentStateManager.class)
public abstract class PersistentStateManagerMixin {
    @Shadow
    @Final
    private Path directory;

    @Shadow
    private Path getFile(String id) {
        return this.directory.resolve(id + ".dat");
    }

    @Inject(method = "readFromFile", at = @At("HEAD"), cancellable = true, require = 0)
    private <T extends PersistentState> void azyron$guardZeroBytePersistentState(
            PersistentStateType<T> type,
            CallbackInfoReturnable<T> cir
    ) {
        try {
            if (type == null) {
                return;
            }
            Path dataFile = this.getFile(type.id());
            if (dataFile != null && Files.exists(dataFile) && Files.isRegularFile(dataFile) && Files.size(dataFile) == 0L) {
                PersistentStateGuard.ensureBackedUpOnLoadFailure(dataFile, type.id(), "zero-byte-persistent-state-file");
                AzyronClient.LOGGER.warn(
                        "[AZYRON] Detected 0-byte '{}.dat'; original file backed up safely before clean state initialization.",
                        type.id()
                );
                cir.setReturnValue(null);
            }
        } catch (Throwable ignored) {
        }
    }

    @Redirect(
            method = "readFromFile",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/PersistentStateManager;readNbt(Ljava/lang/String;Lnet/minecraft/datafixer/DataFixTypes;I)Lnet/minecraft/nbt/NbtCompound;"
            ),
            require = 0
    )
    private NbtCompound azyron$readAndRepairPersistentStateNbt(
            PersistentStateManager manager,
            String id,
            DataFixTypes dataFixTypes,
            int currentSaveVersion
    ) throws IOException {
        Path dataFile = this.getFile(id);
        try {
            NbtCompound root = manager.readNbt(id, dataFixTypes, currentSaveVersion);
            return PersistentStateGuard.inspectAndRepairRaidsCompound(dataFile, id, root);
        } catch (Throwable t) {
            PersistentStateGuard.ensureBackedUpOnLoadFailure(
                    dataFile,
                    id,
                    "corrupted-nbt-" + t.getClass().getSimpleName()
            );
            if (t instanceof IOException ioEx) {
                throw ioEx;
            }
            if (t instanceof RuntimeException rtEx) {
                throw rtEx;
            }
            // Convert fatal Error / LinkageError / OutOfMemoryError from corrupt NBT length prefixes
            // into IOException so PersistentStateManager.readFromFile catches it cleanly instead of crashing the server.
            throw new IOException("Guarded unrecoverable error reading persistent state '" + id + ".dat'", t);
        }
    }

    @Inject(method = "readFromFile", at = @At("RETURN"), require = 0)
    private <T extends PersistentState> void azyron$backupOnCodecFailureBeforeFallback(
            PersistentStateType<T> type,
            CallbackInfoReturnable<T> cir
    ) {
        try {
            if (type != null && cir.getReturnValue() == null) {
                Path dataFile = this.getFile(type.id());
                if (dataFile != null && Files.exists(dataFile) && Files.isRegularFile(dataFile)) {
                    PersistentStateGuard.ensureBackedUpOnLoadFailure(dataFile, type.id(), "codec-parse-failure");
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(
            method = "save(Lnet/minecraft/world/PersistentStateType;Lnet/minecraft/nbt/NbtCompound;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void azyron$atomicSavePersistentState(PersistentStateType<?> type, NbtCompound nbt, CallbackInfo ci) {
        if (type == null || nbt == null) {
            return;
        }
        Path target = this.getFile(type.id());
        try {
            PersistentStateGuard.saveCompressedAtomically(target, nbt);
            ci.cancel();
        } catch (Throwable t) {
            AzyronClient.LOGGER.error(
                    "[AZYRON] Atomic save failed for '{}.dat' (existing file left untouched): {}",
                    type.id(), t.getMessage()
            );
            ci.cancel();
        }
    }
}
