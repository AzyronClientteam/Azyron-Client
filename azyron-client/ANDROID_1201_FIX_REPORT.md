# v1.0.3 Android / Minecraft 1.20.1 investigation

Developer: **JIGNESH GAMING**

## Status and release boundary

These changes are local and experimental. They have NOT been pushed or uploaded to GitHub.
The existing draft assets still contain the earlier v1.0.3 build, not these patches.
The prior permission to push an upload-only workflow is not treated as permission to
push source fixes or replace existing assets. No release was published.

## 1. Options type correction and missing/invalid values

`textBackgroundOpacity` is a unit-interval **double**, default `0.5`.
`backgroundForChatOnly` is a separate **boolean**, default `true`.
They must not share a boolean parser. Correct options.txt examples:

```text
textBackgroundOpacity:0.5
backgroundForChatOnly:true
```

The sanitizer now covers both keys with their respective types. Blank/quoted-empty
boolean values restore the key's default rather than unconditionally becoming false.
Null, malformed JSON tokens, wrong NBT types, non-finite numbers and out-of-range
unit sliders receive typed defaults. Explicit false/off is preserved. Each supported
entry is repaired independently; missing entries retain vanilla initialization and
unknown keys, keybindings and resource-pack selections remain untouched.

This is bounded repair of the known boolean and unit-slider options, not a claim
that every possible Minecraft or third-party option schema is validated. The hook
still repairs only the in-memory compound after GameOptions.update; it does not
rewrite the user's original options.txt. Actual Mixin application on a game runtime
has not been verified here. A codec label alone is insufficient to identify a bad key:
collect the adjacent option key and exception from the log.

An independent JSON-config regression also found that a module's `settings: null`
could load successfully but fail later during capture/save. It now creates the
missing settings map rather than calling clear() on null.

## 2. authlib-injector certificate timeout investigation (not resolved)

The launcher-bundled agent manifest reports:

- authlib-injector **1.2.8**
- source commit `5437f5b465ee3f5be0a1e5e1ae3c50978114f6fb`
- source: https://github.com/yushijinhun/authlib-injector/blob/5437f5b465ee3f5be0a1e5e1ae3c50978114f6fb/src/main/java/moe/yushi/authlibinjector/httpd/ProfileKeyFilter.java

The matching source intercepts POST requests for
`api.minecraftservices.com/player/certificates` and generates a local RSA-2048 keypair
for the response. Thus the visible URL does not prove a live request reached Mojang:
the injector's local HTTP server/agent transformation and key generation may be on
the path. RSA work or entropy availability is only a hypothesis on a low-end device,
not an established cause. Whether this handler is enabled in the user's actual agent
configuration is also unknown. The user's installed launcher may bundle a different
agent than this checkout.

The launcher's `GameRunner.addAuthlibInjectorArgs()` installs the agent for an account
with an injector URL. Its preceding authentication-service ping is a separate request;
a successful ping does not verify player certificate handling. AZYRON does not own
these HTTP calls. There is no evidence here connecting a missing optimization mod to
the certificate failure or to a fatal game crash.

Needed evidence (redact access/refresh tokens, Authorization headers, account IDs and
private keys): complete exception/cause chain, fatal crash section if any, launcher
and Java versions, actual agent version, whether the timed-out endpoint is loopback
or remote, elapsed timeout, and the relevant options.txt entries. An isolated
comparison with the same authorized account/network and launcher-supported official
authentication can distinguish agent-specific behavior; do not bypass login or use
an unsupported account type. Do not disable TLS checks or signed-chat enforcement.
No speculative global timeout, authentication bypass, insecure certificate fallback,
or automatic retry loop was introduced. Endpoint/device reproduction is unavailable
in this restricted sandbox; timeout repair remains pending.

## 3. Android startup / resource-display changes

- Cache Android/Pojav environment/path detection after its first probe, retaining
  explicit overrides for tests. No repeated filesystem probes per splash frame.
- Use a simple Android loading display (background, track, progress and one title),
  omitting orbit/shimmer animation and the long cold-boot pixel-text footer. The
  loading path does not add textures, fonts or synchronous resource reloads.
- Keep the existing shader-ready gate and text fallback; do not change GL context,
  driver extensions, resource-reload order or launcher thread pools.
- Avoid a redundant client-config save during initialization; first-run defaults
  and normal setting-change saves remain in place.
- Make successful companion config seeding idempotent per engine instance. I/O
  failures remain retryable, and existing user configs remain untouched.

These reduce known work; no wall-clock boot-time, GPU performance or memory saving
has been measured on Android hardware. Resource-pack decoding itself is unchanged.
ImmediatelyFast remains excluded by default on mobile; missing recommended mods are
not classified as the crash cause.

## 4. Independent automated checks

The original two suites pass **26/26 each**. The rebuilt JAR contains **75 Java 17
classes**, passing the existing mapping-name audit (not complete owner/descriptor
linkage validation). New `AzyronAndroidRegressionSuite` groups run in separate JVMs
and isolated temp directories for EACH Minecraft version property:

| Group | Assertions per version | Result |
| --- | ---: | --- |
| options: blank/malformed bools, numeric opacity, wrong NBT type, missing and unknown keys, idempotence | 48 | PASS for 1.20.1 and 1.21.11 |
| startup: first seed, repeated-call skip, new-instance seed, user-file preservation | 4 | PASS for both |
| mobile: simulated 3GB/1280MB heap tier, cold/warm quad counts, branding, no flite | 5 | PASS for both |
| config: null settings capture/save, malformed JSON fallback and backup | 3 | PASS for both |

**120 new assertions across 8 isolated group executions; zero failures.**

Independent A/B confirmation also compiled the pre-fix implementations from the
previous source archive, then ran each group twice in separate JVMs: first with all
pre-fix classes, then with ONLY that group's patched class(es) overlaid. On both
versions, the baseline failed as expected and the isolated patch passed:

| Group | Pre-fix failure | Isolated patched result |
| --- | --- | --- |
| options | blank value with true fallback incorrectly becomes false | 48 assertions PASS |
| startup | repeated seeding recreates a deleted file (extra filesystem work) | 4 assertions PASS |
| mobile | Android splash has the same quad workload as desktop | 5 assertions PASS |
| config | null settings throws NullPointerException in Map.clear | 3 assertions PASS |

This validates each regression independently of the other fixes. It does not measure
hardware speedups or independently prove the removed startup-save optimization.

All of these run against generated Minecraft/Fabric/LWJGL test doubles. An Android
flag or RAM override does not emulate Adreno 610, Android 9 or OpenLTW. The normal
Gradle/Loom build remains blocked by restricted dependency-network access. The helper
build/remapper/test-double harness currently lives outside the repository under
`/tmp/azyron_toolchain/build_and_verify_v103.py`; this is not a normal reproducible
Gradle test run on a fresh checkout. No hardware or real multiplayer tests passed
by implication. The existing Mixin integration is not applied by these tests.

## 5. Required Android 9 / OpenLTW / Adreno 610 device matrix — NOT RUN

No matching physical device, Android emulator or GPU is attached to this session.
Compatibility cannot be certified from the sandbox. Use a separate backed-up instance
with Minecraft 1.20.1/Fabric and the launcher's supported Java 17 runtime. Keep all
mods fixed while testing each change; run each comparison at least three times.
Suggested initial heap on a 3GB phone: 1280–1408MB, leaving RAM for Android and the GPU.

| Device check | Evidence required | Status |
| --- | --- | --- |
| Empty opacity/chat-only values, missing entries, valid false preserved | options log and successful later-option load | NOT RUN |
| OpenLTW cold startup with default resources, then user's resource pack | full startup log, shader/GL errors, time to menu | NOT RUN |
| Original vs lightweight splash, with all other settings equal | startup elapsed time, peak RSS/heap, frame times | NOT RUN |
| Same account/network with injector, then supported non-injector auth if available | redacted certificate exception and elapsed timing | NOT RUN |
| Singleplayer load/reload, HUD/input, pause/resume/background/foreground | crash/LMK evidence, frame times and temperature | NOT RUN |
| Multiplayer join/reconnect with secure-chat enforcement unchanged | authentication/join results and complete failure chain | NOT RUN |

Release page requested by the user:
https://github.com/AzyronClientteam/Azyron-Client/releases/tag/v0.1.0-pre

Original uploaded draft (authorized repository access required):
https://github.com/AzyronClientteam/Azyron-Client/releases/tag/untagged-1295a286a3be4735b047

Neither link should be described as containing these local fixes until a replacement
upload has actually been approved, performed and verified.
