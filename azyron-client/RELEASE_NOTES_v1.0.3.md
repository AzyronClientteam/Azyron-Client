# AZYRON CLIENT v1.0.3 — Release Notes & Changelog

> **Current-source validation:** The latest source snapshot passed separate real Fabric/Loom builds and actual-classpath audits for Minecraft `1.20.1` and `1.21.11` in [CI run 42953223121](https://github.com/AzyronClientteam/Azyron-Client/actions/runs/42953223121). Candidate packaging and checksum verification passed, and the guarded job attached the verified version-specific pairs to the existing draft; the release remains a draft. No game launch or device benchmark has been performed. Historical generated-stub checks below are not native-build or gameplay acceptance tests.

> **AZYRON CLIENT – PERFORMANCE. SMOOTHNESS. STYLE.**
> **Developer:** **JIGNESH GAMING**
> **Native downloads:** separate matching Fabric JAR/MRPack pairs for Minecraft `1.20.1` and `1.21.11`.

---

## Highlights in `v1.0.3`

### 1. Separate Native Fabric Builds (`Minecraft 1.20.1` and `1.21.11`)
- **Version-matched client JARs:** `Azyron-Client-1.0.3-mc1.20.1.jar` is compiled against the `1.20.1` APIs and Java 17; `Azyron-Client-1.0.3-mc1.21.11.jar` is compiled against `1.21.11` APIs and Java 21. Their manifests and mixin sets target one Minecraft version each. Use the matching JAR/MRPack pair; neither file is universal.
- **Native compatibility bridge:** shared client logic uses version-specific adapters for HUD rendering, input, keybinding registration, options, world data, and other API differences.
- **Version-specific Mixin sets:** each native build includes only the selected target's mixins and API adapters, and is compiled against that game's actual Fabric/Minecraft dependencies.
  - The build configuration selects a target-specific mixin set and version adapter; a JAR is not intended to contain the other target's incompatible mixins. Confirm the processed mixin list in each fresh native build.

### 2. Separate Standard-Launcher Profiles
- **Native Modrinth packs:** `Azyron-Modpack-1.0.3-mc1.20.1.mrpack` and `Azyron-Modpack-1.0.3-mc1.21.11.mrpack` each contain one matching game manifest, native client JAR, and optimization profile. The launcher—not a client settings row—selects the Minecraft runtime.
- **Version-Aware Companion Config & World Data Guards**:
  - Automatically generates **Sodium `0.8.x`** config on `1.21.11` and **Sodium `0.5.x`** config on `1.20.1` (both keeping `use_no_error_g_l_context: false` for PojavLauncher safety).
  - Protects and repairs both **`1.21.11` snake_case `raids.dat` / `raids_end.dat`** (`next_id`, `tick`, `raids`) and **`1.20.1` PascalCase `raids.dat`** (`NextAvailableID`, `Tick`, `Raids`) with mandatory pre-repair backups and atomic `.azyron.tmp` saves.

### 3. Historical Generated-Stub Checks (Not Native Acceptance)
- Earlier project notes record **26/26** test cases in each isolated generated Minecraft/Fabric stub harness. These are historical and do not describe the current working-tree source.
- A prior constant-pool audit checked symbol names against both Intermediary tables. It did not validate full method descriptors, Mixin application, game startup, or gameplay.
- For actual native build status, see [COMPATIBILITY_1201_BUILD_REPORT.md](COMPATIBILITY_1201_BUILD_REPORT.md); for the current validation boundary, see [TESTING_REPORT.md](TESTING_REPORT.md).

## Local Android / Minecraft 1.20.1 follow-up (not uploaded)

- Distinguish double `textBackgroundOpacity` from boolean `backgroundForChatOnly`.
- Use typed defaults for empty/invalid boolean and unit-slider options, preserving
  absent/unknown entries and isolating repairs by key.
- Recover null module-settings maps on capture/save.
- Cache Android detection, use a reduced Android splash, avoid a redundant startup
  save and skip repeated successful companion-config seeding.
- Independent pre-fix versus isolated-patch regression groups pass; this does not
  substitute for an actual Android 9 / OpenLTW / Adreno 610 test.
- Certificate timeout investigation is inconclusive without a redacted stack trace;
  no authentication, TLS or secure-chat bypass was added.

See [the detailed report](ANDROID_1201_FIX_REPORT.md). CI run 42953223121 contains verified native candidates for the current source snapshot; the existing release remains a draft and its attached pairs match those candidates. Native builds do not constitute game-launch or device-performance tests. Keep the release a draft and do not publish.
