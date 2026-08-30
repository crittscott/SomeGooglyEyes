# Forge 1.21.1 Port Process

## Purpose and prerequisite

This process governs `forge-1.21.1-port-plan.md`. `CLAUDE.md` remains controlling. Forge execution is
blocked until the NeoForge status is `COMPLETE` and this status is explicitly set to `READY` by the
NeoForge final handoff.

The user runs builds/tests unless a named command or stage is explicitly delegated. Listed gates do
not grant execution authority.

## Persistent state

- `forge-1.21.1-port-status.md` is the bounded current snapshot and is overwritten whenever state,
  evidence, attempts, blockers, or next action changes.
- `forge-1.21.1-port-log.md` is append-only reduced audit history and is not read during normal
  execution.

At each execution-session start, read `CLAUDE.md`, the Forge plan, this process, and Forge status;
set `Stable documents read this session` to `Yes`; read only the relevant assessment section; do not
read the log.

## One stage per session: mandatory hard stop

Only one numbered stage may execute in a session. At its completion:

1. overwrite status with full current position and next action;
2. append reduced verification and stage completion to the log;
3. report the handoff;
4. stop immediately.

Do not inspect the next stage, bound its first unit, or run another gate in the same session.

## Bounded work units

Before editing, status must name one coherent behavioral unit, invariant, intended files, narrowest
gate, completion condition, and failed-attempt count. One file is not necessarily one unit, and one
compiler run may expose several units. Classify unrelated failures rather than expanding scope.

Central registration and networking replacements are distinct units and distinct stages. They must
not be combined into an unreviewable cross-loader rewrite.

## Standard work loop

1. Orient from stable documents and status.
2. Bound and record the work unit.
3. Diagnose once if evidence is missing.
4. Implement coherently across every affected loader.
5. Re-read files and use targeted searches; do not use Git for review.
6. Request/run only the narrowest authorized gate.
7. Reduce evidence into status and append it to the log.
8. Advance, repair, block, or hard-stop at stage completion.

A diagnostic expected to remain red succeeds only when current-stage failures are gone and every
remaining failure is classified later.

## Three failed attempts: hard stop

Each bounded work unit receives at most three failed post-edit verification runs. Each attempt is a
stated hypothesis, bounded corrective edit, and verification. Newly exposed errors and hangs count.
An infrastructure failure may receive one identical retry without environment changes, but the
failed run still counts. A pre-edit baseline does not count.

A pass closes the unit; only a new separately bounded unit resets the count. After attempt 3 fails,
stop immediately: no fourth edit, relabeling, broader unit, alternate gate, test weakening, or counter
reset. Log all three reduced attempts, mark status `BLOCKED`, and request the smallest needed decision
or external change.

## Verification ladder

1. File inspection and targeted `rg` searches.
2. Deterministic JSON/TOML/Access Transformer/resource inspection.
3. `:common:compileJava`.
4. Prior-loader production compiles.
5. `:forge:compileJava`.
6. `:forge:processResources`.
7. Focused prior-loader tests and runtime regressions.
8. `:forge:compileGametestJava`.
9. `:forge:runGameTestServer`.
10. `:forge:build`.

Use the narrowest sufficient gate. Do not run `clean`, refresh dependencies, clear caches, stop
daemons, alter the environment, inspect prohibited generated/decompiled/remapped sources or bytecode,
or launch an unattended interactive client. Treat a compile/build with no result after 10 minutes or
a GameTest server after 20 minutes as blocked.

## Cross-loader regression discipline

- Fabric and completed NeoForge are mandatory regression targets for shared edits.
- Stage 1 registration changes must pass common, Fabric, and NeoForge production compiles before the
  stage closes.
- Stage 2 networking changes must additionally preserve focused payload/protocol tests.
- Shared runtime changes require both prior-loader GameTest servers before Forge completion.
- A prior-loader regression consumes the current Forge work unit's attempt budget.
- Do not solve a Forge failure by introducing Forge or NeoForge types into common.

## Test discipline

Existing behavior assertions remain specifications. Never delete, disable, ignore, catch, dilute,
or hide a test. Zero discovery, dedicated-server client classloading, or a required unapplied Mixin
is a failure. Preserve 77 shared assertions and target 78 Forge wrappers including persistence unless
a documented current discovery structure provides equivalent coverage.

## Architectural decision rules

- Prefer current vanilla facilities, then native loader facilities at loader boundaries, then an
  existing project seam, then the smallest explicit project-owned interface.
- Remove common Architectury runtime use required to support Forge; do not remove build-time
  injection unless evidence requires it.
- Do not add a replacement cross-loader library or dependency without user approval.
- Preserve content ids, data components, persistence keys, packet ids/bodies/bounds, protocol 9,
  server authority, configuration semantics, and player behavior.
- Some contained loader duplication is preferable to a broad new abstraction.

## Immediate stop conditions

Stop without spending further attempts if NeoForge is not complete; progress requires changing the
version/dependency/plugin/repository/mappings/wrapper/JDK/module layout; adding a library; changing
data/protocol/player behavior without approval; accepting a prior-loader regression; inspecting
prohibited source/bytecode/artifacts; Git/GitHub or publishing; cache/environment manipulation;
destructive cleanup; an unsupported compatibility claim; an unpreservable user conflict;
contradictory API evidence; three failed attempts; or completion of the current stage.

## Scope

In scope: Forge production/tests/resources, required common/Fabric/NeoForge architectural edits and
regressions, Forge Access Transformer, narrowly necessary Forge test/run configuration, both Forge
state documents, and final orientation-document reconciliation.

Out of scope: version upgrades, new content, unrelated refactoring, a different loader framework,
publishing, Git history, and unattended client testing.

## Handoffs

An unfinished or blocked handoff names stage/unit, invariant, changed files, exact gate, reduced
attempts, diagnosis, smallest required decision, compile expectations for all loaders, and exact next
action. Completion lists every passing cross-loader gate, architectural decision, deferred manual
check, and artifact path; then stops without Git or publishing.
