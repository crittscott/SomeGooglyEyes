# NeoForge 1.21.1 Port Log

This file is append-only reduced audit history. Do not read it during normal execution; use
`neoforge-1.21.1-port-status.md` for current state.

No verification commands or stage completions have been recorded.

## Entry format

### YYYY-MM-DD — Stage N — Work unit

- Command: exact command, or `none` for a documentation-only stage result.
- Result: exit state, duration, and reduced error/test count.
- Delta: what changed from the preceding result.
- Attempts: failed post-edit attempts used for this unit.
- Decision: durable implementation or handoff decision.

## 2026-08-30 — Stage 0 complete

- Work unit: Architectury ownership matrix and initial NeoForge diagnostic.
- Inspection: classified all 24 Architectury import lines in 17 common production files; confirmed
  six existing injection seams, four common registration owners, seven common networking users, and
  the single environment lookup site. The NeoForge scaffold has build/metadata resources but no Java
  production or GameTest source.
- Frozen invariants: no new common Architectury dependency or runtime use, no NeoForge type in common,
  existing loader-neutral seams retained, and no protocol/data/behavior change without evidence and
  approval.
- Verification: `.\gradlew.bat :neoforge:compileJava --console=plain` initially could not access the
  user-profile Gradle distribution lock in the sandbox; the identical approved retry completed with
  `BUILD SUCCESSFUL` in 8 seconds. Common compile/resources/JAR were up to date and NeoForge compile
  was `NO-SOURCE`. No compiler failures required Stage 1–3 classification. Attempts remain 0 of 3.
- Changes: status and this append-only log only; no production source, build, metadata, version,
  repository, protocol, or data-format changes.
- Next stage: Stage 1 — bootstrap, registration, configuration, and platform services. Not begun.

## 2026-08-30 — Stage 1 complete

- Units: NeoForge bootstrap/common registration/reload; native client and per-world server config;
  version lookup, item construction, persistent entity data, and tracking packet fanout adapters.
- Added seven NeoForge production files. The `@Mod` constructor initializes common content once,
  registers the server eye-definition reload listener, registers CLIENT/SERVER specs through the mod
  container, and contains no client rendering or Stage 2 gameplay event imports.
- Registration decision: Architectury 13's NeoForge registrar obtains the mod event bus from the mod
  container, so no legacy Forge-only `EventBuses.registerModEventBus` call exists or is required.
- Configuration inspection: all 3 client and 13 server keys use shared defaults, ranges, and
  validators; filenames remain `somegoogly-client.toml` and `somegoogly-server.toml`.
- Platform inspection: all four implementation signatures match their common `@ExpectPlatform`
  declarations. Item rendering is deferred to Stage 3; persistence proof and tracking lifecycle are
  deferred to Stage 2.
- Verification: `.\gradlew.bat :neoforge:compileJava --console=plain` passed on the first post-edit
  run in 12 seconds; common tasks were up to date and NeoForge compilation executed successfully.
  Failed attempts remain 0 of 3, with no later-stage compiler failures exposed.
- No common, Fabric, build, metadata, dependency, version, repository, protocol, persistence-key,
  item-component, or data-format change was made.
- Next stage: Stage 2 — server runtime and networking. Not begun.

## Stage 2 — server event bridge

- Added and registered the NeoForge server event bridge for lifecycle, tracking, commands, item
  interactions, death drops, damage/healing reactions, and completed trades.
- Static inspection found no stale Forge APIs or new Mixins.
- Attempt 1 was an infrastructure-only wrapper-lock denial before Gradle started.
- The identical authorized retry passed `:neoforge:compileJava` in 11 seconds; no source repair was
  required.

## Stage 2 — persistence and networking proof

- Official NeoForge 1.21.1 API source confirms that the native entity persistent-data compound is
  written to and read from disk and that tracking distributors use the server chunk map.
- Official Architectury 1.21 NeoForge source confirms direction-specific typed payload registration,
  queued handling, and authoritative server-player packet context.
- Project inspection confirmed protocol 9, bounded codecs, context-derived players in all five C2S
  picker handlers, handshake timeout, and unchanged persistence keys.
- No additional implementation edit or compile was required; the cumulative Stage 2 NeoForge
  production compile remains passing.

## Stage 2 complete

The complete NeoForge server-side production surface compiles. Stage 3 is next; no Stage 3 source
was inspected or changed in this session.

## Stage 3 — renderer access and layers

- Added the NeoForge `ClientRendererAccess` implementation and client-sided AddLayers registration.
- Translated all 36 current canonical Access Widener entries into named 1.21.1 Access Transformer
  rules; deterministic counts match and no stale 1.20.1 rule was copied.
- `:neoforge:compileJava` passed in 24 seconds on the first attempt with common compile up to date.
## Stage 3 — ordinary client integration

- Registered clientbound receivers before payload registration; wired client commands, ticks,
  queued entity state, disconnect cleanup, picker keys/HUD/input, inspection, Slimy Eye tint, and
  Googly Eye custom rendering through NeoForge's physical-client registrar.
- Verification: `:neoforge:compileJava` passed on attempt 1 in 9 seconds; common was up to date and
  NeoForge emitted only its expected deprecation warning for `Item#initializeClient`.
## Stage 3 — optional GeckoLib and completion

- Added the NeoForge `GeckoCompat` implementation, soft-loaded mod-presence gate, Gecko bone
  enumeration/path resolution, and per-bone normal/picker rendering layer against the declared
  compile-only GeckoLib 4.7.4 API. The public gate contains no GeckoLib type.
- Verification: the Gecko unit passed `:neoforge:compileJava` on attempt 1 in 10 seconds. The explicit
  Stage 3 final gate, `:common:compileJava :neoforge:compileJava`, then passed in 8 seconds.
- Stage 3 complete. Stage 4 resource and production verification is next; hard stop observed.
## Stage 4 — resources and production verification

- Added GeckoLib 4.7.4 to processed NeoForge metadata as an optional client dependency ordered
  after GeckoLib when present; Architectury remains the sole required temporary runtime library.
- Deterministic inspection preserved 74 vanilla definitions and 171 optional selectors, found valid
  JSON throughout, exact common 262/262 and NeoForge 2/2 source/output path parity, zero cross-module
  duplicates, no unresolved metadata placeholders, and a byte-identical processed AT.
- Verification: `:common:compileJava :neoforge:compileJava :neoforge:processResources` passed on
  attempt 1 in 8 seconds. Stage 4 complete; Stage 5 GameTest setup is next; hard stop observed.
## 2026-08-30 — Stage 5 complete

- Added the NeoForge `gametest` source set, dev-only mod metadata and entry point, dedicated
  GameTest-server run configuration, 12 discovery holders for all 77 shared assertions, and one
  NeoForge-native entity persistence save/load test.
- Deterministic inspection found 77 shared wrappers with zero differences from Fabric, 78 total
  tests, 12 `@GameTestHolder` classes with unprefixed `somegoogly:empty` templates, matching
  `somegoogly_gametest` identity, and the enabled `somegoogly` namespace.
- Verification attempt 1 was an infrastructure failure before Gradle startup because the sandbox
  denied the wrapper lock. Attempt 2 compiled and exposed three stale eligibility wrapper arguments.
  The bounded fake-player correction removed them.
- `.\gradlew.bat :neoforge:compileGametestJava --console=plain` passed in 9 seconds on the final
  allowed attempt. The dedicated GameTest server was not started. Stage 6 is next.
## 2026-08-30 — Stage 6 complete

- The user-supplied baseline discovered and ran all 78 tests. One picker-export assertion failed
  because a `ResourceLocation` was passed directly as a translation argument, and shutdown then
  threw when the server-config listener read values during `ModConfigEvent.Unloading`.
- Picker export and its preserved assertion now cross the translation boundary with the unknown
  entity identifier as text. The next server run passed all 78 required tests, closing that bounded
  cause, while reproducing only the classified unload exception.
- NeoForge client and server config adapters now ignore unloading events before reading config-spec
  values, preserving the existing load/reload synchronization.
- The user verified that the subsequent NeoForge build and complete GameTest run both succeed without
  error. Stage 6 is complete; Stage 7 is next.

## 2026-08-30 — Stage 7 complete

- `:common:compileJava`, `:neoforge:compileJava`, `:neoforge:compileGametestJava`,
  `:neoforge:runGameTestServer`, and `:neoforge:build` passed. NeoForge discovered and passed all 78
  required tests and exited cleanly.
- `neoforge/build/libs/somegoogly-neoforge-0.8.1.jar` was verified by path and filesystem metadata
  only: 468,547 bytes, last written 2026-08-30 23:44:00 UTC. It was not opened or unpacked.
- Invalidated Fabric regressions passed: `:fabric:compileJava`, `:fabric:compileGametestJava`, and
  `:fabric:runGameTestServer`; Fabric discovered and passed all 78 required tests and exited cleanly.
- Updated `player-view.md`, `as-built.md`, and `build-env.md` for the completed NeoForge port. Retained
  all required physical-client and optional-mod checks as manual.
- Final Architectury ownership remains 24 imports in 17 common production files. No platform-seam
  signature changed; Forge must replace common runtime registration, networking, and the single
  environment lookup, then separately prove whether build-time `@ExpectPlatform` can remain.
- NeoForge status is `COMPLETE`; Forge status is `READY`. Begin Forge Stage 0 only in a new session.

## 2026-08-31 — Subsequent cross-loader completion

- Fabric, NeoForge, and Forge release builds and all three 78-test dedicated GameTest suites are now
  user-verified passing without errors.
- Updated the NeoForge status snapshot to preserve its historical Forge handoff while recording the
  completed three-loader state.
