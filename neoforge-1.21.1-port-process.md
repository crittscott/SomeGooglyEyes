# NeoForge 1.21.1 Port Process

## Purpose and controlling authority

This process governs `neoforge-1.21.1-port-plan.md`. `CLAUDE.md` remains controlling. The completed
Fabric implementation is the behavioral reference; Forge is a later target, not a NeoForge gate.

The user runs builds and tests unless a named command or stage is explicitly delegated. A command
listed in the plan is not authorization to run it.

## Persistent execution state

- `neoforge-1.21.1-port-status.md` is the bounded current snapshot. Overwrite it whenever position,
  evidence, attempts, blockers, or the exact next action changes.
- `neoforge-1.21.1-port-log.md` is append-only audit history. Append reduced verification and stage
  completion entries, but do not read it during normal execution.

At the start of every execution session, read `CLAUDE.md`, the NeoForge plan, this process, and the
status, then set `Stable documents read this session` to `Yes`. Read only the relevant assessment
section. Do not read the log.

The status must contain the stage and bounded work unit, state, failed-attempt count, scope and
invariant, intended files, verification command, completion condition, last reduced result, known
later-stage failures, decisions, blockers, exact next action, and cumulative gates.

## One stage per session: mandatory hard stop

Only one numbered stage may execute in a session. When its completion condition is satisfied:

1. finish the status update;
2. append the reduced result and stage completion to the log;
3. state the next stage without inspecting it;
4. stop immediately.

Do not open the next stage's source, define its first work unit, or run its first diagnostic. Context
remaining in the session does not weaken this rule.

## Bounded work units

A work unit is one coherent behavioral group, not one file or compiler error. Before editing, record
its invariant, intended files, narrowest verification, completion condition, and attempt count in
the status. Do not absorb unrelated diagnostics; classify them into later stages.

Examples include NeoForge bootstrap/registration, configuration, entity persistence, the complete
server event bridge, payload registration/tracking, renderer installation/access rules, optional
GeckoLib, or one demonstrated GameTest failure cause.

## Standard work loop

1. Orient from stable documents and current status.
2. Bound the work unit in the status.
3. Diagnose once if existing evidence is insufficient.
4. Implement the coherent change.
5. Re-read edited files and search for stale APIs without Git.
6. Request the narrowest gate, or run it only if explicitly delegated.
7. Overwrite status and append reduced evidence to the log.
8. Advance, repair under the attempt rule, block, or hard-stop at stage completion.

For a diagnostic compile expected to remain red, the unit passes only when no current-stage error
remains and every remaining error is classified later.

## Three failed attempts: hard stop

Each bounded work unit gets no more than three failed post-edit verification runs.

An attempt is a stated hypothesis, a bounded corrective edit, and the unit's verification command.
The first failed post-edit run is attempt 1. A newly exposed error still counts. A hang counts. An
infrastructure failure may receive one identical retry without environment changes, but the failed
run still counts.

The pre-edit baseline diagnostic does not count. A passing verification closes the unit; the count
resets only for the next separately bounded unit.

After attempt 3 fails, stop immediately. Do not make a fourth edit, rename or broaden the unit,
switch gates, weaken tests, or reset the counter. Record all three reduced attempts, mark status
`BLOCKED`, and ask for the smallest required decision or external change.

## Verification ladder

1. File inspection and targeted `rg` searches.
2. Deterministic JSON/TOML/Access Transformer/resource inspection.
3. `:common:compileJava`.
4. `:neoforge:compileJava`.
5. `:neoforge:processResources`.
6. `:neoforge:compileGametestJava`.
7. `:neoforge:runGameTestServer`.
8. Invalidated Fabric regression gates.
9. `:neoforge:build`.

Use the narrowest sufficient gate. Do not run `clean`, refresh dependencies, clear caches, stop
daemons, inspect generated/decompiled/remapped sources, or launch an unattended interactive client.
Monitor an authorized command to completion; treat a compile/build without a result after 10 minutes
or GameTest server after 20 minutes as blocked.

## Test discipline

- Existing assertions remain specifications unless they conflict with current authoritative docs or
  an explicitly accepted NeoForge semantic change.
- Never delete, disable, ignore, catch, dilute, or hide a test to pass.
- Zero discovered tests, a dedicated-server classloading failure, or an unapplied required Mixin is
  a failure.
- Preserve 77 shared assertions and target 78 NeoForge wrappers including persistence, unless a
  documented discovery structure proves equivalent coverage.
- Fix production code when a preserved assertion exposes a production defect.

## Architecture rules

- No NeoForge type enters common production code.
- No new common Architectury runtime dependency is added.
- Existing `@ExpectPlatform` seams remain loader-neutral.
- Runtime Architectury uses are recorded for later Forge replacement and are not spread into new
  common classes.
- Packet ids, bodies, protocol 9, persistence keys, data components, configuration semantics,
  authorization, and bounds remain unchanged unless evidence requires a user-approved change.
- Fabric regressions caused by shared edits are NeoForge-stage failures.

## Immediate stop conditions

Stop without spending further attempts if progress requires a version, dependency, repository,
plugin, mappings, wrapper, JDK, module-layout, data-format, protocol, or player-visible behavior
change; Forge implementation; Git/GitHub; publishing; destructive cleanup; cache manipulation;
prohibited source/bytecode inspection; unsupported optional-mod claims; an unpreservable conflict
with user changes; contradictory API evidence; or completion of the current stage.

## Scope

In scope: NeoForge production/test source and resources, narrowly necessary common/Fabric changes,
NeoForge test/run configuration, required Access Transformer rules, both NeoForge state documents,
and final orientation-document reconciliation.

Out of scope: Forge implementation, new content geometry, unrelated refactors, dependency upgrades,
publishing, Git history, and unattended client testing.

## Handoffs

An unfinished or blocked handoff must name stage/unit, invariant, changed files, exact gate, reduced
attempts, diagnosis, smallest needed decision, compile expectation, and exact next action.

Completion must mark NeoForge complete, list every passing gate and manual check, summarize shared
decisions, and change `forge-1.21.1-port-status.md` from `WAITING` to `READY`. That handoff does not
authorize or begin the Forge port.
