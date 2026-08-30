# Forge 1.21.1 Port Status

This is the bounded execution snapshot. Overwrite it in place; never append. Reduced history belongs
in `forge-1.21.1-port-log.md`, which is not read during normal execution.

## Current position

| Field | Value |
| --- | --- |
| Overall state | **READY** — NeoForge prerequisite complete; Forge Stage 0 is next |
| Current stage | Stage 0 — Handoff audit and Forge baseline |
| Current work unit | NeoForge completion audit and Architectury replacement freeze |
| Work-unit state | Ready; not started |
| Failed verification attempts used | 0 of 3 |
| Stable documents read this session | No — required when Forge execution becomes ready |
| Common compile state | Passing at Fabric completion |
| Fabric state | Complete and passing |
| NeoForge state | Complete; final handoff recorded below |
| Forge compile state | Unknown; runtime source remains 1.20.1-era |
| Forge GameTest state | Stale wrappers; not compiled for 1.21.1 |
| Last command | None for this port |

## Prerequisite

Prerequisite met: `neoforge-1.21.1-port-status.md` is `COMPLETE`, and its Stage 7 handoff records:

- every passing NeoForge completion gate;
- invalidated Fabric regression results;
- final common Architectury ownership matrix;
- common/platform interfaces changed during NeoForge;
- retained manual client and optional-mod checks;
- the exact Forge Stage 0 starting action.

## NeoForge completion handoff

### Passing completion and regression gates

- Common production compilation passed.
- NeoForge production compilation, resource processing, GameTest compilation, and build passed.
- The NeoForge dedicated server discovered and passed all 78 required tests and exited cleanly.
- `neoforge/build/libs/somegoogly-neoforge-0.8.1.jar` was verified by path and file metadata only:
  468,547 bytes, last written 2026-08-30 23:44:00 UTC.
- Invalidated Fabric production and GameTest compilation passed; the Fabric dedicated server
  discovered and passed all 78 required tests and exited cleanly.

### Shared Architectury ownership and interface changes

Common production contains 24 Architectury imports in 17 files. Six existing build-time seams remain
loader-neutral: `ModVersionLookup`, `EntityPersistentData`, `NetworkTracking`,
`GooglyEyeItemFactory`, `ClientRendererAccess`, and `GeckoCompat`. NeoForge implements those seams
without changing their common signatures. Common runtime registration remains in `ModItems`,
`ModDataComponents`, `ModCreativeTabs`, and `ModRecipes`; runtime networking remains in
`NetworkHandler`, `ClientNetworkHandler`, and five picker packet handlers; one environment lookup
remains in `NetworkHandler.registerCommon`.

No NeoForge type entered common production, no new common Architectury use was added, and no
`@ExpectPlatform` signature changed. The only shared production/test correction made during NeoForge
stabilization converts an unknown picker-export identifier to a string at the translation-argument
boundary. Protocol 9, packet ids and bodies, persistence keys, item components, and serialized
formats remain unchanged.

NeoForge retains Architectury 13 temporarily for runtime registration, networking, and the
environment lookup. Forge must replace those unsupported runtime facilities and separately prove
whether build-time `@ExpectPlatform` injection can remain.

### Retained manual checks

- Physical NeoForge client rendering for ordinary adult/baby models, players, slime/magma-cube
  ordering, rabbit, llama, sniffer, villager, and special resolver families.
- Pupil motion and every expression; both item render paths; harvest/craft/apply; Optometrist;
  visibility; picker editing/export; and renderer reload.
- Optional GeckoLib and optional-mod entities available for Minecraft 1.21.1.

### Exact Forge Stage 0 start

In a new session, read `CLAUDE.md`, the Forge plan, process, and this status, then consult only the
relevant Forge assessment section. Verify this handoff, re-inventory the 24 common Architectury
imports, freeze retain/replace decisions for build-time injection and all three runtime categories,
inspect old Forge source only as project behavior reference, and run one diagnostic
`.\gradlew.bat :forge:compileJava --console=plain`. Do not edit production source in Stage 0.

## Current work-unit definition

### Scope and invariant

After the prerequisite is met, audit the NeoForge handoff, freeze how common runtime registration,
networking, and environment facilities will be replaced for Forge, decide whether build-time
`@ExpectPlatform` can remain, and obtain one diagnostic Forge compile. No production source is edited
in Stage 0.

### Intended files

- `forge-1.21.1-port-status.md`
- `forge-1.21.1-port-log.md` after verification
- Read-only inspection of the completed common/Fabric/NeoForge surfaces, Forge source, and active
  Gradle/metadata files

### Verification command

`.\gradlew.bat :forge:compileJava --console=plain`

### Completion condition

- The NeoForge prerequisite is proven.
- Every common Architectury use has a recorded retain/replace decision and target stage.
- The diagnostic result is reduced and every error is assigned to Stages 1–4.
- No production source is edited.
- Stage 0 is logged and the session hard-stops before Stage 1.

## Initial architecture expectations

| Category | Expected Forge treatment |
| --- | --- |
| `@ExpectPlatform` injection | Retain only if Forge transformation proves it needs no runtime artifact |
| Registration/creative tab | Replace runtime Architectury use with loader-neutral project boundary |
| Networking | Replace `NetworkManager` runtime use while preserving typed payload semantics and protocol 9 |
| `Platform`/`Env` | Replace with explicit side-safe bootstrap/context |
| Existing platform seams | Supply current Forge implementations and reuse for prior loaders where appropriate |

## Known evidence

- Forge targets 1.21.1-52.1.16 and has no Architectury 13 Forge runtime dependency.
- Nineteen Forge production Java files and thirteen GameTest files remain from the 1.20.1-era port.
- Common currently imports Architectury runtime registration, networking, and environment APIs.
- Forge metadata is old-form content around current build variables; its Access Transformer is empty.
- Fabric is passing; NeoForge will become a mandatory regression gate once complete.

## Cumulative gates

| Gate | Status |
| --- | --- |
| NeoForge completion handoff | Complete; Forge is `READY` |
| Forge architecture decision | Pending |
| `:common:compileJava` | Previously passing; post-decoupling run pending |
| `:fabric:compileJava` | Previously passing; post-decoupling run pending |
| `:neoforge:compileJava` | NeoForge port pending |
| `:forge:compileJava` | Not run |
| `:forge:processResources` | Not run |
| Prior-loader GameTest regressions | Pending after shared runtime changes |
| `:forge:compileGametestJava` | Not run |
| `:forge:runGameTestServer` | Not run |
| `:forge:build` | Not run |

## Blockers

None. Forge Stage 0 has not started.

## Exact next action

In a new session, read the Forge controlling documents, audit the handoff and common Architectury
inventory, freeze the Stage 0 architecture decisions, and obtain one diagnostic
`:forge:compileJava`. Do not edit production source in Stage 0.
