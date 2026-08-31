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
