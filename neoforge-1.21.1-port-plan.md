# NeoForge 1.21.1 Port Plan

## Objective

Produce a NeoForge artifact for Minecraft 1.21.1 using Java 21, NeoForge 21.1.248, Architectury API
13.0.8, and GeckoLib 4.7.4. It must preserve the completed Fabric behavior, compile, process
resources, discover and pass the ported GameTests on a dedicated server, and package successfully.

This plan does not authorize Gradle execution. Under `CLAUDE.md`, the user runs builds and tests
unless a named command or stage is explicitly delegated.

## Completion gates

1. `.\gradlew.bat :common:compileJava --console=plain`
2. `.\gradlew.bat :neoforge:compileJava --console=plain`
3. `.\gradlew.bat :neoforge:processResources --console=plain`
4. `.\gradlew.bat :neoforge:compileGametestJava --console=plain`
5. `.\gradlew.bat :neoforge:runGameTestServer --console=plain`
6. `.\gradlew.bat :neoforge:build --console=plain`
7. Fabric regression gates invalidated by shared edits, at minimum `:fabric:compileJava` and
   `:fabric:compileGametestJava`, plus `:fabric:runGameTestServer` when shared runtime behavior or
   tests changed.

A physical NeoForge client smoke test remains required before visual compatibility is confirmed.

## Session discipline and hard stops

Execute exactly one stage per session. Begin by reading `CLAUDE.md`, this plan,
`neoforge-1.21.1-port-process.md`, and `neoforge-1.21.1-port-status.md`. Consult the relevant
assessment section only as needed. Do not read `neoforge-1.21.1-port-log.md` during normal execution.

**When a stage completion condition is met, stop immediately.** Update the status, append the reduced
result to the log, hand off, and do not inspect or begin the next stage in that session.

## Stage summary

| Stage | Work product | Completion condition |
| --- | --- | --- |
| 0 | Cross-loader architecture freeze and NeoForge compiler baseline | Dependency matrix recorded; one diagnostic compile classified |
| 1 | Bootstrap, registration, configuration, and platform services | Diagnostic NeoForge compile; later-stage failures classified |
| 2 | Server events, persistence, item interactions, and networking | Server-side production surface compiles or only client failures remain |
| 3 | Client rendering, picker, access, and optional GeckoLib | Common and NeoForge production compiles pass |
| 4 | Resources, metadata, and cumulative production verification | Resource processing and production compiles pass |
| 5 | NeoForge GameTest source set and wrappers | NeoForge GameTest compile passes with intended discovery set |
| 6 | Dedicated-server runtime stabilization | All discovered required tests pass and server exits cleanly |
| 7 | Artifact, documentation, and Forge handoff | NeoForge build and invalidated Fabric regressions pass; Forge handoff recorded |

Until Stage 3, a diagnostic compile may remain red only in a classified later-stage subsystem.

## Stage 0 — Architecture freeze and baseline

### Work

- Inventory every common Architectury import and classify it as build-time injection, runtime
  registration, runtime networking, or environment lookup.
- Record the present owner, the NeoForge implementation path, and the planned Forge replacement
  boundary for each category in the status.
- Freeze these rules: no new common Architectury dependency; no NeoForge API in common; no packet or
  data-format change without explicit evidence.
- Inspect the NeoForge Gradle and metadata scaffold without changing versions or repositories.
- Obtain one diagnostic `:neoforge:compileJava` and classify every failure into Stages 1–3.

### Hard stop

Stop when the matrix and diagnostic classification are durable. Do not edit production source in
Stage 0.

## Stage 1 — Bootstrap, registration, configuration, and platform services

### Work

- Create the NeoForge `@Mod` entry point and attach Architectury registration to the correct mod bus.
- Initialize common items, data components, recipes, creative tab, networking, and services exactly
  once.
- Implement NeoForge version lookup, item construction, persistent-data boundary, and tracking
  boundary as far as their signatures can be established independently of event wiring.
- Implement client and per-world server configuration with documented paths, defaults, and
  validation.
- Register the eye-definition reload listener at the correct server lifecycle boundary.
- Keep client classes physically isolated from dedicated-server bootstrap.

### Verification and hard stop

Inspect changed sources and metadata, then obtain one diagnostic `:neoforge:compileJava`. Stop when
all Stage 1 failures are removed and every remaining failure belongs to Stage 2 or 3.

## Stage 2 — Server runtime and networking

### Work

- Adapt entity load/tracking, join/disconnect, datapack sync, server tick/stop, and command events.
- Adapt death, direct entity use, damage, healing, and completed-trade events into the existing
  common services.
- Preserve natural initialization, stable variant rolls, full-snapshot synchronization, harvesting
  qualification, durability cost, behavior cooldowns, and picker authorization.
- Complete native entity persistence and prove save/load behavior.
- Register Architectury 13 payloads on NeoForge, implement tracking fanout, preserve protocol 9,
  bounded decoding, direction-specific receivers, handshake timeout, and pending client state.
- Prefer NeoForge events over new Mixins. Any necessary Mixin must have a documented missing-event
  cause and a runtime application test.

### Verification and hard stop

Obtain one diagnostic `:neoforge:compileJava`. Stop when the server-side production surface compiles
and only classified Stage 3 client errors remain, or when the full production compile passes.

## Stage 3 — Client, access, picker, and GeckoLib

### Work

- Install shared eye and picker layers on all compatible living renderers without duplicates and
  before the slime outer layer.
- Implement `ClientRendererAccess`, renderer reset/reload handling, item renderer, tint, client
  commands, key mappings, HUD/input, inspection, and disconnect cleanup.
- Translate only the required common Access Widener members into NeoForge Access Transformer rules.
- Port the optional GeckoLib bridge against the declared NeoForge artifact and retain soft loading.
- Preserve renderer caches, age-dependent attachments, player skin variants, and export containment.

### Verification and hard stop

Require passing `:common:compileJava` and `:neoforge:compileJava`. Inspect the Access Transformer and
all client-only registrations. Stop immediately after both compiles pass.

## Stage 4 — Resources and production verification

### Work

- Validate `neoforge.mods.toml`, dependency ranges, side declarations, resource inclusion, and the
  Access Transformer declaration/path.
- Confirm common assets/data are included through the transformed common artifact without stale
  plural registry paths or duplicated resources.
- Confirm GeckoLib remains optional and Architectury remains the only required temporary runtime
  library beyond NeoForge.
- Do not change the 74 vanilla definitions or optional-mod selectors without new evidence.

### Verification and hard stop

Require passing common and NeoForge production compiles and `:neoforge:processResources`. Inspect
processed resource paths without opening or unarchiving an artifact. Stop when the gates pass.

## Stage 5 — NeoForge GameTests

### Work

- Add a NeoForge `gametest` source set and dedicated GameTest server configuration without changing
  the established module architecture beyond what this test surface requires.
- Create loader wrappers for all 77 shared assertions.
- Add one NeoForge-specific entity persistence save/load test.
- Verify discovery metadata and namespace configuration; zero discovery is a failure.
- Preserve assertions for components, recipes, enchantments, packets, picker operations, behaviors,
  configuration, eligibility, variants, and persistence.

### Verification and hard stop

Require `:neoforge:compileGametestJava` to pass and deterministically inspect the intended wrapper
count and discovery configuration. Stop before starting the server.

## Stage 6 — Runtime stabilization

Run the complete NeoForge GameTest server only when explicitly authorized. Repair failures in
bounded demonstrated-cause groups: bootstrap/discovery; registries/resources; persistence/events;
networking; recipes/items/enchantments; picker/behavior; dedicated-server classloading.

Require a nonzero test count, every required test passing, and a clean server exit. Apply the
three-attempt rule independently to each bounded cause. Stop immediately when the suite passes.

## Stage 7 — Package, reconcile, and hand off to Forge

### Work

- Re-run any production or test gate invalidated by Stage 6 fixes.
- Run `:neoforge:build` when explicitly authorized and verify the expected artifact by path and file
  metadata only; do not open or unarchive it.
- Run the Fabric regression gates required by shared changes.
- Update `player-view.md`, `as-built.md`, and `build-env.md` to describe NeoForge truthfully.
- Record remaining manual client and optional-mod checks.
- Write the Forge handoff into both port statuses: final NeoForge gates, shared Architectury
  dependency matrix, common interfaces changed, NeoForge-specific decisions, and exact Forge Stage
  0 starting action.

### Hard stop

Mark NeoForge complete only when every required gate passes. Set Forge from `WAITING` to `READY` only
in this stage. Do not begin Forge Stage 0 in the same session.

## Required manual NeoForge client checks

- Ordinary adult/baby models, players, slime/magma-cube ordering, rabbit, llama, sniffer, villager,
  and special resolver families.
- Pupil motion and every expression.
- Googly Eye 3D rendering, Slimy Eye tint, and harvest-craft-apply round trips.
- Optometrist books and harvesting, visibility settings, picker editing/export, and renderer reload.
- Optional GeckoLib and optional-mod entities available for Minecraft 1.21.1.
