# Forge 1.21.1 Port Log

This file is append-only reduced audit history. Do not read it during normal execution; use
`forge-1.21.1-port-status.md` for current state.

No verification commands or stage completions have been recorded. Forge is waiting on NeoForge.

## Entry format

### YYYY-MM-DD — Stage N — Work unit

- Command: exact command, or `none` for a documentation-only stage result.
- Result: exit state, duration, and reduced error/test count.
- Delta: what changed from the preceding result.
- Attempts: failed post-edit attempts used for this unit.
- Decision: durable implementation or handoff decision.

### 2026-08-30 — Stage 0 — Handoff audit and Forge baseline

- Command: `.\gradlew.bat :forge:compileJava --console=plain`.
- Result: expected failure after 9 seconds; 17 compile errors and 10 deprecation warnings. An initial
  sandboxed launch was denied wrapper-lock access before Gradle; the identical approved retry reached
  compilation.
- Delta: established the first Forge 1.21.1 compiler baseline; no production source changed.
- Attempts: 0 failed post-edit attempts; this was the required pre-edit diagnostic.
- Decision: NeoForge completion is verified. Retain all six existing build-time `@ExpectPlatform`
  seams. Stage 1 replaces runtime registration and the `Platform`/`Env` lookup; Stage 2 replaces
  Architectury networking. The diagnostic assigns 3 errors to Stage 1, 5 to Stage 2, none to Stage 3,
  and 9 to Stage 4; the 10 warnings divide between Stage 3 server/config/bootstrap and Stage 4 client
  registration. Stage 0 is complete and hard-stops before Stage 1.

### 2026-08-30 — Stage 1 — Registration and environment decoupling

- Command: `.\gradlew.bat :common:compileJava --console=plain`; then
  `.\gradlew.bat :fabric:compileJava :neoforge:compileJava --console=plain`.
- Result: both commands passed in 10 and 11 seconds respectively. Fabric compiled cleanly; NeoForge
  emitted one existing deprecated `initializeClient` warning.
- Delta: common registration now uses a loader-neutral `ContentRegistrar` with final bind-once
  handles. Fabric registers through native registries and `FabricItemGroup`; NeoForge and Forge own
  native deferred registers. Loader bootstraps pass physical-side state explicitly, removing common
  `Platform`/`Env` use and Forge's obsolete `EventBuses` hook. Common Architectury imports fell from
  24 in 17 files to 13 in 13 files, all retained injection or Stage 2 networking.
- Attempts: 0 failed post-edit attempts; every required gate passed on its first run.
- Decision: content ids, factories, creative-tab contents, lazy access, packet ids/bodies, protocol
  9, and player behavior remain unchanged. Stage 1 is complete and hard-stops before Stage 2; no
  extra Forge diagnostic was run after the completion gates passed.

## 2026-08-30 — Stage 2 complete: loader-neutral networking

- Replaced common Architectury networking with `NetworkTransport` while preserving protocol 9,
  all ten payload ids, codecs, packet bodies, bounds, directionality, handshake state, sender
  authority, and bounded pending client state.
- Fabric now uses Fabric Networking API payload registries and play receivers/senders; NeoForge uses
  its native payload registrar and distributors; Forge uses an optional Forge 52 payload channel
  with native direct and entity-tracking distribution.
- Common Architectury imports are now only the six retained `@ExpectPlatform` seams in six files.
- `:common:compileJava` passed; combined Fabric/NeoForge production compilation passed.
- Fabric and NeoForge GameTest servers each discovered and passed all 78 required tests, including
  the typed-payload wire-id assertion, and exited cleanly.
- The Forge diagnostic contains no networking error. Its nine remaining errors are the previously
  classified Stage 4 GeckoLib, HUD overlay, client-command, and renderer skin-map failures; ten
  existing deprecation warnings remain.
- Failed post-edit verification attempts: 0. The initial sandbox-denied Gradle wrapper launch was
  retried with approved cache access and did not reach compilation.

## Stage 3 — Forge server runtime — complete

- Replaced deprecated global Forge loading-context access with the injected
  `FMLJavaModLoadingContext`; native content, networking, configs, display compatibility, server
  events, and physical-client dispatch now originate from that entry point.
- Registered the preserved `somegoogly-server.toml` and `somegoogly-client.toml` names and ignored
  config unload notifications after Forge clears their specs.
- Moved client handler state and registration into a physical-client bootstrap and removed the
  obsolete synchronized registration of the picker-only `MaybeFloatArgumentType`.
- Preserved reload/config sync, entity initialization, login/logout, tracking, commands, ticks,
  shutdown, item application/harvesting, drops, healing, and trade transitions. Damage reactions now
  use Forge 52 `LivingDamageEvent` final damage.
- Verification: the first post-edit launch was denied access to the external Gradle wrapper cache;
  the identical approved retry reached compilation. `:common:compileJava` was up-to-date and
  `:forge:compileJava` reported only the same nine classified Stage 4 client errors: GeckoLib API,
  removed HUD/client-command hooks, and renderer skin-map typing. No Stage 3 error remained.

## 2026-08-31 — Stage 4 complete

- Unit: Forge physical-client lifecycle, rendering/item presentation, access, picker, and optional GeckoLib integration.
- Replaced the split stale client handlers with one side-gated Forge bootstrap covering client commands, picker HUD/keys/input, inspection, ticks, entity arrival, disconnect cleanup, item tint, renderer-layer installation, and client networking.
- Updated renderer-map typing and GeckoLib 4.7.4 animatable imports; installed the established 36-entry 1.21.1 Access Transformer set; retained the soft-dependency gate and 3D item-renderer boundary.
- Removed `ClientEventHandler`, `EyeInspectIndicator`, `SlimyEyeColors`, and `ForgePickerClient` after consolidating their responsibilities.
- Pre-edit diagnostic: nine classified Stage 4 errors. The initial sandboxed launch could not access the Gradle wrapper lock; the approved identical retry reached compilation.
- Verification attempt 1: `.\gradlew.bat :common:compileJava :fabric:compileJava :neoforge:compileJava :forge:compileJava --console=plain` passed in 27 seconds. Forge emitted one GeckoLib removal warning; the other three compiles were up-to-date.
- Result: Stage 4 complete with zero failed post-edit attempts. Stage 5 is next in a new session; mandatory hard stop applied.
