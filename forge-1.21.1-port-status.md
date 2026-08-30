# Forge 1.21.1 Port Status

This is the bounded execution snapshot. Overwrite it in place; never append. Reduced history belongs
in `forge-1.21.1-port-log.md`, which is not read during normal execution.

## Current position

| Field | Value |
| --- | --- |
| Overall state | **WAITING** — NeoForge must complete first |
| Current stage | Stage 0 — Handoff audit and Forge baseline |
| Current work unit | NeoForge completion audit and Architectury replacement freeze |
| Work-unit state | Blocked by prerequisite; not started |
| Failed verification attempts used | 0 of 3 |
| Stable documents read this session | No — required when Forge execution becomes ready |
| Common compile state | Passing at Fabric completion |
| Fabric state | Complete and passing |
| NeoForge state | Planned; not yet complete |
| Forge compile state | Unknown; runtime source remains 1.20.1-era |
| Forge GameTest state | Stale wrappers; not compiled for 1.21.1 |
| Last command | None for this port |

## Prerequisite

Forge may become `READY` only when `neoforge-1.21.1-port-status.md` is `COMPLETE` and its Stage 7
handoff records:

- every passing NeoForge completion gate;
- invalidated Fabric regression results;
- final common Architectury ownership matrix;
- common/platform interfaces changed during NeoForge;
- retained manual client and optional-mod checks;
- the exact Forge Stage 0 starting action.

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
| NeoForge completion handoff | Pending |
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

NeoForge port completion and its explicit Forge handoff.

## Exact next action

Do nothing in Forge. Execute the NeoForge plan through Stage 7. When that final handoff changes this
status to `READY`, begin a new session by reading the Forge controlling documents and auditing the
recorded NeoForge evidence. Do not carry Forge implementation into the NeoForge completion session.
