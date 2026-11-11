# AZYRON CLIENT v1.0.3 — Validation Status

**Developer:** JIGNESH GAMING
**Downloads:** separate native JAR/MRPack pairs for Minecraft 1.20.1 and 1.21.11.
**Scope:** This report separates actual Fabric/Loom build checks, offline package/portal checks, historical generated-stub tests, and gameplay/device validation.

## Current working tree

The current source passed fresh real Fabric/Loom builds and actual-classpath audits for both targets in [CI run 42953223121](https://github.com/AzyronClientteam/Azyron-Client/actions/runs/42953223121), commit `c93bac240e8876a43d1ff6a347b15550749bcc49`. Both build jobs, candidate preparation, and downloaded-artifact checksum verification succeeded. The guarded attachment job also completed and refreshed release `437365942` with the verified version-specific candidates and checksum/provenance sidecars. A subsequent read-only API check returned `draft=true`; this run did not publish the release. This supersedes run 42948465997's skipped-attachment status.

The local sandbox has no `java` or `javac`, so the native builds ran in GitHub Actions rather than locally. These build and package results still do **not** validate game startup, full Mixin transformation, gameplay, renderer compatibility, stability, or FPS/RAM performance.

## Current native CI result

| Target | Native build | Actual-classpath audit | Output |
|---|---:|---:|---|
| Minecraft 1.20.1 / Fabric | PASS | 105 checks passed | Java 17 / class-file 61; 73 client classes |
| Minecraft 1.21.11 / Fabric | PASS | 110 checks passed | Java 21 / class-file 65; 74 client classes |

CI passed all **9/9** packaging/installer tests and prepared the four version-matched candidates. Artifact: [`Azyron-v1.0.3-native-candidates-NOT-PUBLISHED`](https://github.com/AzyronClientteam/Azyron-Client/actions/runs/42953223121/artifacts/13826640241), 858,141 bytes, expires 2026-11-25. Its SHA-256 manifest and provenance were verified in the CI runner after download. The artifact itself is not a release and contains no gameplay benchmark results.

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

## Remaining validation and release gates

1. Launch each native target with its matching Fabric Loader/API and dependencies; verify startup/Mixin application, menus, HUD, Armour Hub, controls, worlds, and multiplayer.
2. Separately test Android/PojavLauncher and measure FPS, frame times, heap/RSS, GC, thermals, and stability on a documented compatible device. Do not report the 60+ FPS goal as achieved without those measurements.
3. The latest API check shows release `437365942` (`v1.0.3`) remains a draft and now carries the verified pairs from run 42953223121 plus `SHA256SUMS.txt` and `BUILD-PROVENANCE.json`. The release record has a non-null historical `published_at`; do not infer publication history from that field alone. No game-launch, gameplay, or hardware-performance gate has passed. Do not publish or make any further release changes without explicit owner authorization.
