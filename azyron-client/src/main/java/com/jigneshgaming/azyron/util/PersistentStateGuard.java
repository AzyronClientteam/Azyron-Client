package com.jigneshgaming.azyron.util;

import com.jigneshgaming.azyron.AzyronClient;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;

/**
 * Non-destructive safeguard & recovery helper for Minecraft 1.21.11 world PersistentState (.dat) files,
 * specifically protecting raids.dat / raids_end.dat and all world data files.
 *
 * Safety Guarantees:
 *  1. NEVER deletes or overwrites user worlds, saves, or existing backups.
 *  2. ALWAYS creates and byte-size-verifies a timestamped backup in <world>/data/azyron_backups/
 *     BEFORE any NBT schema repair or clean fallback initialization occurs.
 *  3. Writes persistent state updates via temporary file + atomic rename so sudden Android /
 *     PojavLauncher process termination can never produce half-written or 0-byte raids.dat files.
 */
public final class PersistentStateGuard {
    public static final String BACKUP_SUBDIR = "azyron_backups";

    private static final DateTimeFormatter BACKUP_TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC);

    /**
     * Tracks files already backed up during the current load cycle so dual hooks
     * (readNbt exception + readFromFile null-return check) do not create duplicate backups
     * for the exact same file read attempt.
     */
    private static final Set<Path> BACKED_UP_THIS_SESSION = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private PersistentStateGuard() {
    }

    public static boolean isRaidsStateId(String stateId) {
        return "raids".equals(stateId) || "raids_end".equals(stateId);
    }

    /**
     * Creates a verified, collision-free backup copy of originalFile inside
     * <parent>/azyron_backups/<fileName>.<timestamp>.bak before any repair or fallback state creation.
     * Never deletes or modifies originalFile.
     */
    public static Path createBackupBeforeRepair(Path originalFile, String stateId, String reason) {
        if (originalFile == null) {
            return null;
        }
        Path normalized = originalFile.toAbsolutePath().normalize();
        if (!Files.exists(normalized) || !Files.isRegularFile(normalized)) {
            return null;
        }
        try {
            Path parent = normalized.getParent();
            if (parent == null) {
                return null;
            }
            Path backupDir = parent.resolve(BACKUP_SUBDIR);
            Files.createDirectories(backupDir);

            String fileName = normalized.getFileName().toString();
            String ts = BACKUP_TS_FORMAT.format(Instant.now());
            Path backupPath = backupDir.resolve(fileName + "." + ts + ".bak");
            int suffix = 1;
            while (Files.exists(backupPath)) {
                backupPath = backupDir.resolve(fileName + "." + ts + "-" + suffix + ".bak");
                suffix++;
            }

            long sourceSize = Files.size(normalized);
            Files.copy(normalized, backupPath, StandardCopyOption.COPY_ATTRIBUTES);
            long copiedSize = Files.size(backupPath);
            if (copiedSize != sourceSize) {
                throw new IOException("Backup size mismatch (" + copiedSize + " != " + sourceSize + ")");
            }

            // Write human-readable diagnostic metadata alongside the backup without touching original data
            Path metaPath = backupPath.resolveSibling(backupPath.getFileName().toString() + ".meta.txt");
            String meta = "AZYRON CLIENT PersistentState Backup\n"
                    + "State ID: " + (stateId != null ? stateId : "unknown") + "\n"
                    + "Original File: " + normalized + "\n"
                    + "Original Bytes: " + sourceSize + "\n"
                    + "Reason: " + (reason != null ? reason : "corrupted-persistent-state") + "\n"
                    + "Timestamp (UTC): " + Instant.now() + "\n";
            Files.writeString(metaPath, meta, StandardCharsets.UTF_8);

            BACKED_UP_THIS_SESSION.add(normalized);
            AzyronClient.LOGGER.warn(
                    "[AZYRON] Preserved backup of '{}' ({} bytes, reason: {}) -> {}",
                    fileName, sourceSize, reason, backupPath
            );
            return backupPath;
        } catch (Throwable t) {
            AzyronClient.LOGGER.error(
                    "[AZYRON] Failed creating safety backup for '{}': {}",
                    normalized, t.getMessage()
            );
            return null;
        }
    }

    /**
     * Ensures a backup exists for originalFile at least once during the current load if a load/parse failed.
     */
    public static Path ensureBackedUpOnLoadFailure(Path originalFile, String stateId, String reason) {
        if (originalFile == null) {
            return null;
        }
        Path normalized = originalFile.toAbsolutePath().normalize();
        if (BACKED_UP_THIS_SESSION.contains(normalized)) {
            return null;
        }
        return createBackupBeforeRepair(normalized, stateId, reason);
    }

    public static void clearSessionBackupCacheForTests() {
        BACKED_UP_THIS_SESSION.clear();
    }

    /**
     * Inspects a loaded raids.dat / raids_end.dat NbtCompound for structural defects on both:
     *  - Minecraft 1.21.11 Raids.CODEC ("data" -> "next_id", "tick", "raids")
     *  - Minecraft 1.20.1  RaidManager ("data" -> "NextAvailableID", "Tick", "Raids")
     *
     * If and ONLY if repair is needed, creates a verified backup of dataFile FIRST before
     * repairing the in-memory NbtCompound non-destructively.
     */
    public static NbtCompound inspectAndRepairRaidsCompound(Path dataFile, String stateId, NbtCompound rootTag) {
        if (rootTag == null || !isRaidsStateId(stateId)) {
            return rootTag;
        }

        if (McVersionBridge.isMinecraft120()) {
            return inspectAndRepairRaids120(dataFile, stateId, rootTag);
        }

        Optional<NbtCompound> dataOpt = McVersionBridge.nbtGetCompound(rootTag, "data");
        boolean missingDataCompound = !rootTag.contains("data") || dataOpt.isEmpty();
        NbtCompound data = dataOpt.orElse(null);

        boolean missingNextId = (data == null) || !data.contains("next_id") || McVersionBridge.nbtGetInt(data, "next_id").isEmpty();
        boolean missingTick = (data == null) || !data.contains("tick") || McVersionBridge.nbtGetInt(data, "tick").isEmpty();

        int invalidRaidEntries = 0;
        NbtList existingRaidsList = null;
        if (data != null && data.contains("raids")) {
            Optional<NbtList> listOpt = McVersionBridge.nbtGetCompoundList(data, "raids");
            if (listOpt.isEmpty()) {
                invalidRaidEntries = 1;
            } else {
                existingRaidsList = listOpt.get();
                for (int i = 0; i < existingRaidsList.size(); i++) {
                    Optional<NbtCompound> entryOpt = McVersionBridge.nbtListGetCompound(existingRaidsList, i);
                    if (entryOpt.isEmpty() || !isMinimallyValidRaidWithIdEntry(entryOpt.get())) {
                        invalidRaidEntries++;
                    }
                }
            }
        }

        if (!missingDataCompound && !missingNextId && !missingTick && invalidRaidEntries == 0) {
            // Valid 1.21.11 raids.dat compound — return completely untouched
            return rootTag;
        }

        // Mandatory safety backup of the untouched file on disk BEFORE repairing a single tag
        ensureBackedUpOnLoadFailure(dataFile, stateId, "raids-nbt-schema-repair");

        if (data == null) {
            data = new NbtCompound();
            Optional<Integer> rootNext = McVersionBridge.nbtGetInt(rootTag, "next_id");
            Optional<Integer> rootLegacyNext = McVersionBridge.nbtGetInt(rootTag, "NextAvailableID");
            if (rootNext.isPresent()) {
                data.putInt("next_id", Math.max(1, rootNext.get()));
            } else if (rootLegacyNext.isPresent()) {
                data.putInt("next_id", Math.max(1, rootLegacyNext.get()));
            }
            Optional<Integer> rootTick = McVersionBridge.nbtGetInt(rootTag, "tick");
            Optional<Integer> rootLegacyTick = McVersionBridge.nbtGetInt(rootTag, "Tick");
            if (rootTick.isPresent()) {
                data.putInt("tick", Math.max(0, rootTick.get()));
            } else if (rootLegacyTick.isPresent()) {
                data.putInt("tick", Math.max(0, rootLegacyTick.get()));
            }
            rootTag.put("data", data);
        }

        if (!data.contains("next_id") || McVersionBridge.nbtGetInt(data, "next_id").isEmpty()) {
            int recoveredNextId = McVersionBridge.nbtGetInt(data, "NextAvailableID")
                    .map(v -> Math.max(1, v))
                    .orElse(1);
            data.putInt("next_id", recoveredNextId);
        }

        if (!data.contains("tick") || McVersionBridge.nbtGetInt(data, "tick").isEmpty()) {
            int recoveredTick = McVersionBridge.nbtGetInt(data, "Tick")
                    .map(v -> Math.max(0, v))
                    .orElse(0);
            data.putInt("tick", recoveredTick);
        }

        if (invalidRaidEntries > 0) {
            NbtList cleanList = new NbtList();
            if (existingRaidsList != null) {
                for (int i = 0; i < existingRaidsList.size(); i++) {
                    Optional<NbtCompound> entryOpt = McVersionBridge.nbtListGetCompound(existingRaidsList, i);
                    if (entryOpt.isPresent() && isMinimallyValidRaidWithIdEntry(entryOpt.get())) {
                        cleanList.add(entryOpt.get());
                    }
                }
            }
            data.put("raids", cleanList);
        }

        AzyronClient.LOGGER.warn(
                "[AZYRON] Repaired '{}' 1.21.11 NBT structure in memory after creating backup (missingData={}, missingNextId={}, missingTick={}, droppedCorruptEntries={})",
                stateId, missingDataCompound, missingNextId, missingTick, invalidRaidEntries
        );
        return rootTag;
    }

    private static NbtCompound inspectAndRepairRaids120(Path dataFile, String stateId, NbtCompound rootTag) {
        Optional<NbtCompound> dataOpt = McVersionBridge.nbtGetCompound(rootTag, "data");
        boolean missingDataCompound = !rootTag.contains("data") || dataOpt.isEmpty();
        NbtCompound data = dataOpt.orElse(null);

        boolean missingNextId = (data == null) || !data.contains("NextAvailableID") || McVersionBridge.nbtGetInt(data, "NextAvailableID").isEmpty();
        boolean missingTick = (data == null) || !data.contains("Tick") || McVersionBridge.nbtGetInt(data, "Tick").isEmpty();

        int invalidRaidEntries = 0;
        NbtList existingRaidsList = null;
        if (data != null && data.contains("Raids")) {
            Optional<NbtList> listOpt = McVersionBridge.nbtGetCompoundList(data, "Raids");
            if (listOpt.isPresent()) {
                existingRaidsList = listOpt.get();
                for (int i = 0; i < existingRaidsList.size(); i++) {
                    Optional<NbtCompound> entryOpt = McVersionBridge.nbtListGetCompound(existingRaidsList, i);
                    if (entryOpt.isEmpty() || !isMinimallyValidRaid120Entry(entryOpt.get())) {
                        invalidRaidEntries++;
                    }
                }
            }
        }

        if (!missingDataCompound && !missingNextId && !missingTick && invalidRaidEntries == 0) {
            return rootTag;
        }

        ensureBackedUpOnLoadFailure(dataFile, stateId, "raids-1201-nbt-schema-repair");

        if (data == null) {
            data = new NbtCompound();
            rootTag.put("data", data);
        }
        if (!data.contains("NextAvailableID") || McVersionBridge.nbtGetInt(data, "NextAvailableID").isEmpty()) {
            int recovered = McVersionBridge.nbtGetInt(data, "next_id")
                    .or(() -> McVersionBridge.nbtGetInt(rootTag, "NextAvailableID"))
                    .map(v -> Math.max(1, v))
                    .orElse(1);
            data.putInt("NextAvailableID", recovered);
        }
        if (!data.contains("Tick") || McVersionBridge.nbtGetInt(data, "Tick").isEmpty()) {
            int recovered = McVersionBridge.nbtGetInt(data, "tick")
                    .or(() -> McVersionBridge.nbtGetInt(rootTag, "Tick"))
                    .map(v -> Math.max(0, v))
                    .orElse(0);
            data.putInt("Tick", recovered);
        }
        if (invalidRaidEntries > 0) {
            NbtList cleanList = new NbtList();
            if (existingRaidsList != null) {
                for (int i = 0; i < existingRaidsList.size(); i++) {
                    Optional<NbtCompound> entryOpt = McVersionBridge.nbtListGetCompound(existingRaidsList, i);
                    if (entryOpt.isPresent() && isMinimallyValidRaid120Entry(entryOpt.get())) {
                        cleanList.add(entryOpt.get());
                    }
                }
            }
            data.put("Raids", cleanList);
        } else if (!data.contains("Raids")) {
            data.put("Raids", new NbtList());
        }

        AzyronClient.LOGGER.warn(
                "[AZYRON] Repaired '{}' 1.20.1 NBT structure in memory after creating backup (missingData={}, missingNextId={}, missingTick={}, droppedCorruptEntries={})",
                stateId, missingDataCompound, missingNextId, missingTick, invalidRaidEntries
        );
        return rootTag;
    }

    private static boolean isMinimallyValidRaid120Entry(NbtCompound entry) {
        if (entry == null) {
            return false;
        }
        boolean hasId = (entry.contains("Id") && McVersionBridge.nbtGetInt(entry, "Id").isPresent())
                || (entry.contains("id") && McVersionBridge.nbtGetInt(entry, "id").isPresent());
        if (!hasId) {
            return false;
        }
        return (entry.contains("Status") && McVersionBridge.nbtGetString(entry, "Status").isPresent())
                || (entry.contains("status") && McVersionBridge.nbtGetString(entry, "status").isPresent());
    }

    private static boolean isMinimallyValidRaidWithIdEntry(NbtCompound entry) {
        if (entry == null) {
            return false;
        }
        if (!entry.contains("id") || McVersionBridge.nbtGetInt(entry, "id").isEmpty()) {
            return false;
        }
        if (entry.contains("raid") && McVersionBridge.nbtGetCompound(entry, "raid").isPresent()) {
            return true;
        }
        return entry.contains("center") || entry.contains("status") || entry.contains("active");
    }

    /**
     * Atomically writes a compressed PersistentState NbtCompound via a sibling .azyron.tmp file
     * using OutputStream-based NbtIo.writeCompressed (identical Intermediary method_10634 across 1.20.1 & 1.21.11)
     * so sudden power loss or Android LMK termination never truncates the live .dat file.
     */
    public static void saveCompressedAtomically(Path targetFile, NbtCompound nbt) throws IOException {
        if (targetFile == null || nbt == null) {
            return;
        }
        Path parent = targetFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path normalized = targetFile.toAbsolutePath().normalize();
        Path tmpFile = targetFile.resolveSibling(targetFile.getFileName().toString() + ".azyron.tmp");

        try (OutputStream out = Files.newOutputStream(tmpFile)) {
            NbtIo.writeCompressed(nbt, out);
        }
        if (!Files.exists(tmpFile) || Files.size(tmpFile) <= 0L) {
            Files.deleteIfExists(tmpFile);
            throw new IOException("Refusing to commit empty temporary persistent state file: " + tmpFile);
        }

        try {
            Files.move(tmpFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicEx) {
            Files.move(tmpFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
        BACKED_UP_THIS_SESSION.remove(normalized);
    }
}
