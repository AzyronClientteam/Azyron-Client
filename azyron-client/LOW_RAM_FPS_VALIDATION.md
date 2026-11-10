# Azyron v1.0.3 — unpublished low-RAM optimization pass

**Products:** Azyron Client v1.0.3 / Azyron Modpack v1.0.3
**Developer:** JIGNESH GAMING
**Targets:** Minecraft **1.20.1 Fabric** and **1.21.11 Fabric**, separately.
**Status:** local implementation and deterministic checks completed; real-game compatibility and hardware acceptance **NOT COMPLETE**. The owner subsequently approved updating the existing v1.0.3 release. Source/config changes were pushed on the Arena branch for real Fabric CI validation; the generated-stub binaries were not uploaded. Existing public assets remain unchanged until genuine builds pass validation.

## What changed

- Profile switching reapplies the selected device's RAM safety bounds when `autoMemoryOptimization` is enabled. Selecting Balanced or Smooth can no longer silently restore excessive distances/FPS on a detected 3GB device.
- 3GB calibration uses render distance at most 6, simulation at most 5, biome blend 0, entity render scale at most 0.5, mipmaps 0 when outside a world, and a 60 FPS target. Existing lower FPS limits survive initial calibration; explicitly selecting a profile still selects its settings before RAM clamping. The user can disable automatic memory optimization. A 60 FPS cap is a frame-pacing/power target, **not evidence of achieving 60 FPS**; it deliberately does not chase 100+ FPS on a 3GB phone.
- Potato's default FPS cap is now 60 rather than 260. No larger heap, forced-GC cycle, speculative JVM flags, or launcher/runtime/signing change was introduced.
- The integrated singleplayer server receives a 6-chunk view/5-chunk simulation ceiling on 3GB early, before the adaptive MSPT sampling warmup. Adaptive simulation reduction has a conservative floor of 5 instead of 2. Remote multiplayer simulation remains controlled by the remote server.
- Vanilla synchronous chunk-write preferences are preserved by default. Saving durability is not silently exchanged for frame rate. The previous async behavior is available only with explicit `-Dazyron.allowAsyncChunkWrites=true`; `azyron.forceSyncChunkWrites` takes precedence. Neither is enabled by these packs.
- Corrected the exact Sodium 0.5.3 and 0.8.14 config schemas (details below). GL no-error contexts remain disabled in supplied configs for conservative Pojav behavior.
- Added separate Dynamic FPS 3.2.1 and 3.11.6 config generators. Legacy required codec fields are supplied. Both use default graphics state on focus changes, avoiding graphics-setting/chunk-reload churn, and neither forces GC. Modern config disables optional native downloads/battery monitoring. No foreground idle throttling was added by Azyron.
- Runtime companion seeding only happens for installed Sodium / Dynamic FPS, once per engine, and does not overwrite existing user files. Other mods own their upstream defaults instead of receiving guessed cross-version config keys. **Existing bad user configs are not automatically migrated**; back them up before resetting those two files in a test instance.
- Each `.mrpack` gets its own active `options.txt`, Azyron config, Sodium config, and Dynamic FPS config. Both alternatives are embedded for the local portal, but embedding is not an automatic launcher-version selector.
- The local portal now switches active configs as well as the manifest, and copies ZIP metadata rather than mutating source offsets while repacking. A regression test caught the latter ZIP-corruption issue.
- Dependency downloaders refuse a known cross-version instance marker, verify downloads in temporary files, and replace the destination only after successful hashing. Failed/corrupt downloads do not replace an existing JAR. This marker is not a complete scanner for manually installed third-party mods.

No HUD module, Armour Hub, Mods Menu, cosmetic, input, authentication, or UI feature was removed. Existing stub HUD/armour/menu checks still pass; this is not certification of their rendering on a physical device. Minimal particles and smaller entity render distance are visual profile settings, not removal of those systems.

## Render 6 / simulation 4 request

**Render 6 is configured. Simulation 4 is NOT claimed as implemented.** The conservative profile uses 5 rather than serializing an unverified out-of-range vanilla option or bypassing option validation. A source mirror inspected during the audit has an integer-slider minimum of 5, but that mirror does not independently certify both exact game versions. Exact 1.20.1 and 1.21.11 in-game behavior still needs validation. Do not call a stub assignment of `4` proof of support.

Multiplayer servers choose their own simulation distance; client options cannot force them to tick at 4. Singleplayer and LAN gameplay implications must also be checked before offering a below-vanilla override. This acceptance criterion remains open.

## Separate dependency audit / decisions

The eight existing pinned entries per manifest are retained; no new mod is automatically added. This pass did **not** re-download all 16 external JARs. Their existing hashes are retained, not invented or newly certified. Full dependency resolution, nested-JAR checks and actual mixin launch tests remain required.

| Mod | 1.20.1 Fabric | 1.21.11 Fabric | Decision |
|---|---|---|---|
| Fabric API | 0.88.1 | 0.141.6 | Retain existing pins |
| Sodium | 0.5.3 | 0.8.14 | Retain; separately source-audited config schema |
| Lithium | 0.11.2 | 0.21.4 | Retain; do not invent mixin overrides |
| FerriteCore | 6.0.0 | 8.2.0 | Retain upstream defaults |
| EntityCulling | 1.6.2 | 1.11.3 | Retain; fresh packs no longer ship guessed config keys |
| MoreCulling | 0.19.0 | 1.6.2 | Retain; real Sodium/culling interaction checks pending |
| Cloth Config | 11.1.106 | 21.11.153 | Retain existing supporting dependency |
| Dynamic FPS | 3.2.1 | 3.11.6 | Retain; separately source-audited config schema |
| ImmediatelyFast | 1.2.4+1.20.1 | 1.14.3+1.21.11 Fabric | Existing **desktop opt-in only**; excluded from mobile/default packs |
| ModernFix | Fabric 5.25.2+mc1.20.1 release found; needs Loader >=0.16.10 | No exact Fabric artifact verified | Optional downloader only for 1.20.1 with explicit compatible-loader declaration; not in default packs |
| Enhanced Block Entities | 1.20 source branch exists; artifact/dependencies not fully resolved | No exact 1.21.11 artifact verified | Deferred for both; do not substitute a 1.21.4 JAR |

Default loader pins remain 0.14.22 (1.20.1) and 0.20.1 (1.21.11). Consequently ModernFix 5.25.2 **must not** be dropped into the default 1.20.1 instance. Its optional installer flag is guarded by an explicit loader version >=0.16.10; upgrading the actual launcher instance is a separate manual operation and still needs testing with the complete stack.

ModernFix's GitHub release metadata reports SHA-256 `5c1f852046e62a17056bb6b571164e61644076c5eb9029c92b31e337cc62c0d9`. The sandbox download followed a release-asset redirect and failed with EOF; **no successful local byte verification occurred**. The optional installer compares actual downloaded bytes against that digest at install time. It is an untested candidate, not a certified default.

Modrinth API metadata retrieval did not return usable JSON here. That is a verification limitation, **not** proof that no suitable releases exist. The restricted build environment also cannot perform the normal full Fabric/Loom dependency download. No unsupported loader substitutions, fake hashes, speculative mod additions, or claim of a conflict-free launch was made.

### Source-audited config corrections

- Sodium 0.5.3: `performance.always_defer_chunk_updates_v2`, not `always_defer_chunk_updates`.
- Sodium 0.8.14: `performance.chunk_build_defer_mode = "ALWAYS"`; `NEARBY` is not one of its enum values. `cpu_render_ahead_limit` belongs in `advanced`. Modern quality settings differ from 0.5.3.
- Dynamic FPS 3.2.1: each `states` entry requires `frame_rate_target`, `volume_multiplier`, `graphics_state`, `show_toasts`, and `run_garbage_collector`.
- Dynamic FPS 3.11.6: volume maps (`volume_multipliers`) and the modern state/config model; legacy scalar volume settings are not reused.

Sources inspected using GitHub API (source inspection is not binary/runtime validation):

- [Sodium 0.5.3 options](https://github.com/CaffeineMC/sodium/blob/mc1.20.1-0.5.3/src/main/java/me/jellysquid/mods/sodium/client/gui/SodiumGameOptions.java)
- [Sodium 0.8.14 options](https://github.com/CaffeineMC/sodium/blob/mc1.21.11-0.8.14/common/src/main/java/net/caffeinemc/mods/sodium/client/gui/SodiumOptions.java)
- [Sodium 0.8.14 DeferMode](https://github.com/CaffeineMC/sodium/blob/mc1.21.11-0.8.14/common/src/main/java/net/caffeinemc/mods/sodium/client/render/chunk/DeferMode.java)
- [Dynamic FPS 3.2.1 codec](https://github.com/juliand665/Dynamic-FPS/blob/3.2.1/src/main/java/dynamic_fps/impl/config/Config.java)
- [Dynamic FPS 3.11.6 defaults](https://github.com/juliand665/Dynamic-FPS/blob/3.11.6/platforms/common/src/main/resources/assets/dynamic_fps/data/default_config.json)
- [ModernFix Fabric 1.20.1 candidate release](https://github.com/embeddedt/ModernFix/releases/tag/5.25.2%2B1.20.1)
- [ModernFix candidate loader requirement](https://github.com/embeddedt/ModernFix/blob/5.25.2%2B1.20.1/fabric/src/main/resources/fabric.mod.json)
- [Enhanced Block Entities 1.20 source](https://github.com/FoundationGames/EnhancedBlockEntities/tree/1.20)

## Validation actually executed

| Check | 1.20.1 | 1.21.11 | Meaning |
|---|---:|---:|---|
| Existing generated-stub JVM suite | 26 groups PASS | 26 groups PASS | Logic/API test doubles only |
| New low-RAM generated-stub regression suite | 63 assertions PASS | 63 assertions PASS | Profiles, opt-out, caps, SP bounds, save durability, both config schemas |
| Previous Android regression groups | 60 assertions PASS | 60 assertions PASS | Options 48, startup 4, mobile 5, config 3 |
| Python packaging/portal/installer regression | 7 tests PASS, combined | 7 tests PASS, combined | Both active profiles, portal conversion, guards, corrupt-download atomicity |
| Python syntax / shell syntax / `git diff --check` | PASS | PASS | Static checks only |
| Local pack ZIP/embedded bytes/active config comparisons | PASS | PASS | Packaging integrity, not imported-game compatibility |
| Full Fabric/Loom build and actual game launch | NOT RUN | NOT RUN | Generated helper is not a substitute |
| Real 3GB SP / multiplayer / chunk generation | NOT RUN | NOT RUN | Hardware unavailable |
| Real 3GB 60-minute session, FPS/1% lows/RAM/GC/thermal/crash log | NOT RUN | NOT RUN | No invented measurements |

The existing tests include labels such as "30-minute equivalent" and "Zero Crash". These describe synthetic loops/test expectations, **not elapsed real gameplay or proof of crash freedom**. No measured FPS, RAM reduction, leak fix, load-time improvement or renderer compatibility is claimed.

Checks are in `src/test/java/com/jigneshgaming/azyron/AzyronLowRamRegressionSuite.java` and `scripts/test-low-ram-packaging.py`. The latter runs offline with Python's standard library:

```sh
python3 azyron-client/scripts/test-low-ram-packaging.py
bash -n azyron-client/releases/download-dependencies.sh
```

JVM tests ran via the existing temporary generated-stub helper `/tmp/azyron_toolchain/build_and_verify_v103.py`, then independent version JVMs. This helper is sandbox infrastructure, not a checked-in reproducible full Minecraft toolchain. Logs and structurally checked **stub-build test artifacts** are kept in ignored `azyron-client/build/fps-validation/`. They must not be released as game-tested binaries. No source archives or APK were refreshed for this pass.

## Local packaging

Build each target explicitly after obtaining a genuine, approved client build:

```sh
python3 azyron-client/scripts/build-modpack.py /path/to/approved-client.jar /path/to/pack-1.20.1.mrpack --mc 1.20.1
python3 azyron-client/scripts/build-modpack.py /path/to/approved-client.jar /path/to/pack-1.21.11.mrpack --mc 1.21.11
```

The JAR must contain expanded v1.0.3 metadata declaring both supported versions. These checks alone do not prove the JAR can launch. Without an explicit output path, `--mc 1.20.1` now uses the `-mc1.20.1` filename to avoid overwriting the default 1.21.11 output. Product names inside manifests remain unchanged.

Use separate launcher instances. Runtime `selectedMcVersion` is metadata, **not** a way to change the already-running Minecraft engine. Fresh packs supply low-RAM defaults. Back up existing worlds, configs and options before importing; runtime companion seeding intentionally preserves existing files.

## Required hardware acceptance / publication gate

For **each version independently**, use a genuine Fabric build, the intended Java runtime (17 for 1.20.1, 21 for 1.21.11), a compatible 3GB device, and record runtime/renderer/resolution/driver/loader/mod hashes. Start with no shaders or high-resolution resource packs, render 6 and the actually accepted simulation setting. Record whether simulation 4 is rejected/clamped, rather than reporting the requested number as measured.

1. Cold start and repeated menu/world joins; verify HUD, Armour Hub, Mods Menu, cosmetics, controls and options persistence.
2. Repeatable singleplayer route in an existing world for 10 minutes, then new-chunk traversal for 10 minutes. Fixed seed, route, speed, resolution and charging/thermal state for baseline versus candidate.
3. The same timed route on a documented multiplayer server; record server simulation/view settings, ping, TPS/MSPT and crowded/entity/block-entity scenes.
4. At least 60 minutes continuous play, periodic world transitions/focus changes. Record average FPS, p50/p95/p99 frame times/1% lows, heap used/committed, process RSS/native memory, GC pauses, thermal throttling, OOM/ANR and full redacted logs. A flat Java heap alone does not rule out native/GPU leaks.
5. A/B optional mods one at a time, never ModernFix/EBE/ImmediatelyFast simultaneously as the first test. Validate block/entity visibility, animations, chests/signs, save/reload, combat and visual correctness. Do not assume an absent optimization mod caused a crash.
6. Use results to select final caps/worker counts. Until then, 60+ FPS is a **target, not achieved acceptance**.

**Owner approval to update the existing release was subsequently received.** Never upload the generated-stub test artifacts. Real Fabric build failures must block replacement of the public client/modpack downloads; disclose real-game and hardware-validation limits even if builds pass.
