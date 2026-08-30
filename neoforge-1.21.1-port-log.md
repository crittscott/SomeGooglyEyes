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
