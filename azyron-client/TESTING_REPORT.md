# AZYRON CLIENT v1.0.3 — Validation Status

**Developer:** JIGNESH GAMING
**Downloads:** separate native JAR/MRPack pairs for Minecraft 1.20.1 and 1.21.11.
**Scope:** This report separates actual Fabric/Loom build checks, offline package/portal checks, historical generated-stub tests, and gameplay/device validation.

## Current working tree

**The current edits have not passed a native build or game launch.** The last successful native CI run predates the current working-tree Java/UI, Gradle metadata, packaging, portal, and documentation changes. A passing earlier build must not be treated as validation of these edits.

The local sandbox has no `java` or `javac` executable, so Gradle builds could not be run locally. The required native acceptance check is still a fresh real Fabric/Loom build and actual-classpath audit for both targets, followed by validation of the resulting matching JAR/MRPack pairs.

## Earlier native CI result (baseline only)

[CI run 42785077400](https://github.com/AzyronClientteam/Azyron-Client/actions/runs/42785077400), source commit `f3a566fa65a063334537cf7d2b414164b1b10d24`, successfully ran the real Fabric/Loom matrix and packaging workflow before the present edits:

| Target | Native build | Actual-classpath audit | Java output |
|---|---:|---:|---:|
| Minecraft 1.20.1 / Fabric | PASS | 105 checks passed | Java 17 / class-file 61 |
| Minecraft 1.21.11 / Fabric | PASS | 110 checks passed | Java 21 / class-file 65 |

The run also passed eight offline packaging tests and prepared four target-specific candidate files. Its audit read real named Minecraft/Fabric classpath data; it did not start Minecraft or execute the complete Mixin transformer. The source commit is older than this worktree, so these results are historical baseline evidence only.

## Offline package and portal checks

The checked-in Python packaging tests use a synthetic metadata-only fixture JAR. They can test target guards, Modrinth pack ZIP structure, version-specific default paths, configuration contents, and download-installer safeguards. **The fixture is not a runnable mod and is not a substitute for either native build.**

Portal integration tests similarly create metadata-only JAR/MRPack fixtures. They check static version-specific routes, target matching, ETags/lengths, and rejection of mismatches; they do not validate a real release binary or game compatibility.

**Current post-edit results:** `scripts/test-low-ram-packaging.py` passed **9/9** tests; `python3 -m unittest discover -s releases-portal -v` passed **5/5** integration tests; `py_compile` for the portal, test, and packaging scripts passed; and `bash -n releases/download-dependencies.sh` passed. The Python suites used temporary synthetic fixtures and are not native Fabric/game validation.

Run the offline checks from `azyron-client/`:

```bash
python3 scripts/test-low-ram-packaging.py
python3 -m unittest discover -s releases-portal -v
python3 -m py_compile releases-portal/server.py releases-portal/test_server.py
bash -n releases/download-dependencies.sh
```

Record their results separately from native builds and gameplay testing. A passing Python suite proves only the covered deterministic packaging/server behavior.

## Historical generated-stub checks — not native acceptance

Earlier project notes record 26/26 checks per Minecraft target in isolated JVM harnesses built with generated Minecraft/Fabric stubs, plus a constant-pool name audit. They were not actual game runs, are not being presented as current-source results, and do not prove correct invocation descriptors, Mixin application, renderer interoperability, stability, or performance. Synthetic “soak” loops are not elapsed gameplay.

## Gameplay and device validation — NOT RUN

No Minecraft launch, physical-device test, or fresh FPS/RAM/stability benchmark has been performed for the current edits:

- Desktop startup, world loading, Mixin transformation, HUD/Armour Hub/Mods Menu interaction: **NOT RUN**.
- Android/PojavLauncher renderer, startup, memory pressure, thermal behavior, OOM/ANR, or sustained-play tests: **NOT RUN**.
- Multiplayer joins, chunk loading, entity rendering, server transitions, and long sessions: **NOT RUN**.
- FPS averages/1% lows, frame times, JVM heap, process RSS/native memory, GC pauses: **NOT MEASURED**.

The 3GB profile's six-view/five-simulation-chunk and 60 FPS-cap configuration is a **target/configuration**, not an achieved frame rate. **60+ FPS at six chunks is not verified or promised.** No crash-free, stability, RAM-reduction, or measured-performance claim is made. Multiplayer simulation distance remains controlled by the server.

## Required final validation

1. Run a fresh actual Fabric/Loom build and actual-classpath audit for Minecraft 1.20.1 and 1.21.11 after the current source changes.
2. Build one native JAR and one matching `.mrpack` per target. Check each JAR's exact Minecraft/Java metadata and class version; check each pack's manifest, embedded JAR bytes, config, hashes, and ZIP integrity.
3. Launch each target with its matching Fabric Loader/API and dependencies; verify startup/Mixin application, menus, HUD, Armour Hub, controls, worlds, and multiplayer.
4. Separately test Android/PojavLauncher and measure FPS, frame times, heap/RSS, GC, thermals, and stability on a documented compatible device. Do not report the 60+ FPS goal as achieved without those measurements.
5. Keep the existing release a draft. Do not replace/remove old assets until both native pairs pass build/package validation, and do not publish without owner approval.
