# Fabric 1.21.1 Port Process

## Purpose

This process governs execution of `fabric-1.21.1-port-plan.md`. It supports sustained code, resource,
test, and repair work while preventing uncontrolled iteration, unjustified redesign, build
environment rewrites, and loss of state across turns or context compaction. Each session is bounded
to one stage.

`CLAUDE.md` remains controlling. This process grants no permission to ignore its restrictions on
source inspection, Git, caches, build execution, or project scope.

## Build and verification authority

The user runs builds and tests unless the user explicitly delegates a named command or stage.
Listing a command as a gate does not itself authorize its execution.

When the user runs a gate and supplies its result, treat that result exactly as a locally executed
verification:

- inspect the supplied output once;
- reduce it to command, exit status, error or test count, and delta from the preceding run;
- update the status snapshot;
- append the reduced result to the log.

When the user explicitly authorizes a Gradle command, run only that command or the commands in the
delegated stage. Do not infer permission to run later gates.

## Authorized work

Within the current Fabric port, execution may:

- edit common and Fabric production source required by the current stage;
- edit shared and Fabric GameTests while preserving their coverage;
- edit checked-in common and Fabric resources and metadata required for Minecraft 1.21.1;
- edit the common Access Widener when the current rendering work establishes a required member;
- make narrowly necessary changes to existing Fabric source sets or test/run configuration, but only
  with explicit user direction if the change alters the established build architecture;
- use read-only searches and inspect project-generated text logs and reports;
- consult official published Fabric, Architectury, GeckoLib, Parchment, and Minecraft API
  documentation when local code and compiler diagnostics are insufficient.

This authorization does not include Git operations, publishing, network service changes, IDE
automation, operating-system changes, cache manipulation, dependency upgrades, Forge or NeoForge
runtime work, decompilation, unarchiving, bytecode inspection, or prohibited cached/remapped sources.

## Fabric establishes the 1.21.1 common baseline

Forge is not already ported. NeoForge contains no runtime Java. Neither loader can serve as a
regression gate during the Fabric port.

Common changes should:

- use vanilla or loader-neutral types where practical;
- retain existing `@ExpectPlatform` seams until a current stage deliberately replaces one;
- avoid new Fabric API types in common production code;
- avoid increasing Architectury runtime coupling;
- record changed platform-interface signatures needed by later Forge and NeoForge ports;
- preserve server authority, persistence ownership, packet limits, and player-visible behavior.

Do not edit Forge or NeoForge production or test code to make a Fabric gate pass. Expected
Forge/NeoForge breakage is recorded in the status but is not counted as a Fabric failure.

Architectury API 13 has Fabric and NeoForge platform artifacts but no Forge 1.21.1 platform artifact.
Removing or replacing its common runtime facilities for Forge is a later architectural task unless
Fabric itself cannot compile without resolving that boundary. If the Fabric port reaches such a
conflict, stop for user direction rather than expanding scope.

## Persistent execution state

Execution state lives in two files with opposite disciplines:

- `fabric-1.21.1-port-status.md` is a small bounded snapshot. Overwrite it in place whenever state
  changes. It is the only state file read during the normal work loop.
- `fabric-1.21.1-port-log.md` is append-only history. Write to it but do not read it during normal
  execution. It exists for human audit.

The status snapshot contains only:

- current stage and work unit;
- work-unit state: not started, in progress, passed, blocked, or complete;
- failed verification attempt count for the current work unit;
- whether stable controlling documents have been read this session;
- work-unit scope, intended files, verification command, and completion condition;
- last command and one-line result;
- known failure classes assigned to later stages;
- established technical decisions and evidence;
- blockers;
- exact next action;
- cumulative gate record.

Overwrite the snapshot:

- when a work unit is bounded, passes, or blocks;
- after a material implementation decision;
- after receiving a verification result;
- before ending an unfinished session.

Append one log entry:

- after every verification command, whether run by the user or explicitly delegated;
- when a stage completes;
- when a work unit reaches the attempt limit.

Never copy raw Gradle output into either file. Record only reduced evidence.

A resumed agent reads `CLAUDE.md`, the plan, this process, and the status before acting, then sets
`Stable documents read this session` to `Yes`. It does not read the log. It consults only the
relevant section of the assessment and continues from the recorded next action.

## Definition of a bounded work unit

A work unit is one coherent behavioral group implemented and checked together. It is not one file or
one compiler error. Mechanical changes across a subsystem may form one unit; central state,
networking, persistence, rendering, or data-format decisions deserve their own units.

Examples:

- all common identifier factory replacements outside client rendering;
- the complete eye-item appearance component migration;
- both recipe serializer ports;
- the Optometrist data and holder-access migration;
- the complete Architectury networking registration boundary;
- Fabric entity persistent data and its save/load Mixin;
- the common rendering layer and its minimal Access Widener entries;
- one demonstrated cause shared by several GameTest failures.

Before editing, the status must name:

- scope and invariant;
- intended files;
- verification command;
- completion condition;
- current failed-attempt count.

Do not expand a work unit because a diagnostic exposes an unrelated later-stage error. Assign that
error to its planned stage.

## Standard work loop

For each work unit:

1. **Orient.** Read the status and the current stage's relevant source, tests, and assessment section.
2. **Bound.** Write the work-unit definition into the status before editing.
3. **Diagnose.** Use existing evidence or obtain at most one pre-edit diagnostic when evidence is
   missing.
4. **Implement.** Make the complete coherent change using current vanilla/Fabric facilities and the
   project's existing subsystem boundaries.
5. **Inspect.** Re-read edited files and search for stale API use. Do not use Git for review.
6. **Verify.** Request the narrowest plan gate from the user, or run it if explicitly authorized.
7. **Record.** Overwrite the status and append one reduced log entry.
8. **Advance or repair.** Close a passing unit or perform a bounded correction under the
   three-attempt rule.

Passing a narrow gate does not remove later cumulative gates.

When the work unit satisfying the current stage's primary condition passes, end the session. Do not
start the next stage in the same session.

## Three-attempt rule

Each bounded work unit may have no more than three failed post-edit verification runs.

An attempt consists of:

1. a stated failure hypothesis;
2. a bounded edit intended to correct it;
3. the work unit's verification command.

The first failed verification after an edit is attempt 1. After attempt 3 fails, stop. Do not make a
fourth correction, broaden the work unit, weaken a test, switch gates, or reset the counter under a
new label.

Counting rules:

- The single pre-edit baseline diagnostic is not an attempt.
- Every failed post-edit verification counts, including a new error exposed by the preceding fix.
- A successful verification closes the work unit; the counter resets only for the next separately
  bounded unit.
- A dependency or infrastructure failure may be retried once with the identical command and no
  environment changes; the failed run still counts.
- A hang without a decisive result counts as a failed attempt.
- Forge or NeoForge compilation is not run and cannot consume an attempt during this Fabric plan.

At the limit, the log records all three hypotheses, edits, commands, and reduced results. The status
records the blocked state and the smallest decision or information needed from the user.

## Verification ladder

Use the narrowest sufficient level:

1. focused file inspection and `rg` searches;
2. deterministic JSON, TOML, Access Widener, and resource-path inspection;
3. `:common:compileJava` for common production code;
4. `:fabric:compileJava` for Fabric production code;
5. `:fabric:processResources` for production resources and metadata;
6. `:fabric:compileGametestJava` for test source and discovery wrappers;
7. `:fabric:runGameTestServer` for server runtime behavior and Mixin application;
8. `:fabric:build` for final packaging.

Do not repeatedly run a broad gate when a narrower one proves the current correction. Do not use
`clean` routinely. Do not refresh dependencies, delete run directories, alter Gradle user settings,
stop daemons, or touch caches to address a code failure.

Verification output is transient. Read it in the turn it is supplied, reduce it, and do not copy the
error list into the status or log. If exact detail is needed in a later session, request or run the
gate again only when later edits could have invalidated it or the status explicitly requires it.

For a diagnostic compile expected to remain red until a later stage, current-stage success means:

- no errors remain in the current stage's completed work units;
- every remaining error is assigned to a later stage;
- the current work did not introduce an unclassified failure.

Passing gates explicitly named in the plan remain mandatory.

## Test discipline

- Existing behavior assertions are specifications unless they conflict with `player-view.md`,
  current documented invariants, or an explicitly accepted 1.21.1 semantic change.
- Never delete, disable, ignore, or weaken a test merely to make a gate pass.
- Never replace an assertion with logging or catch an exception that should fail the test.
- Update tests for current identifiers, item components, registry context, payloads, resources,
  fixtures, and APIs only after the production boundary is established.
- Group several failures only when evidence identifies one shared production cause.
- A GameTest run with zero discovered tests is a failure.
- A dedicated-server classloading or Mixin-application failure is a production failure.
- Preserve the baseline 77 shared assertion methods and 78 Fabric wrapper methods unless an
  explicitly documented consolidation retains equivalent coverage.

Add focused tests when a port changes a central boundary not already covered, especially:

- the eye appearance component through harvest, crafting, application, and re-harvest;
- preservation of unrelated item components;
- Optometrist lookup and shears-only behavior;
- packet/protocol round trips and bounds;
- Fabric entity persistence across save/load;
- datapack definition selection for 1.21.1.

Use the existing GameTest framework.

## Decision rules

Execution may choose between implementation details when all of the following are true:

- player-visible behavior remains unchanged;
- the choice remains inside the current work unit and subsystem owner;
- it uses a current vanilla, Fabric, Architectury, or GeckoLib facility already in the build;
- it adds no dependency, plugin, repository, or build-system change;
- it introduces no Fabric API type into common production code;
- it does not require Forge or NeoForge implementation;
- it is reversible without migration or broad redesign;
- one option is clearly smaller or more coherent with project principles.

When several approaches remain plausible, prefer:

1. current vanilla facility;
2. current Fabric facility at the Fabric boundary;
3. existing Some Googly Eyes abstraction seam;
4. smallest explicit context plumbing;
5. contained duplication over a new cross-cutting abstraction;
6. a documented stop for user choice.

Compiler errors establish that an API use is wrong. They do not establish the correct behavioral
replacement.

## Immediate stop conditions

Stop without spending the remaining attempt budget when progress would require:

- changing Minecraft, Fabric Loader, Fabric API, Gradle, Loom, Architectury, Shadow, GeckoLib,
  mappings, the wrapper, the JDK, or the established module layout;
- adding or replacing a dependency, repository, plugin, loader, or test framework;
- changing the OS, IDE, Java installation, environment variables, network configuration, Gradle user
  home, daemon state, or caches;
- decompiling, unarchiving, inspecting bytecode, or reading prohibited cached/remapped Minecraft or
  Forge sources;
- a Git or GitHub operation;
- destructive deletion or broad movement outside a named resource migration in Stage 5;
- changing documented player behavior, probabilities, cooldowns, eligibility, harvesting,
  appearance semantics, picker authorization, export containment, or network authority;
- choosing a materially different item-component schema after the Stage 1 boundary is established;
- changing network wire semantics without updating the protocol contract;
- introducing Fabric API or a loader-specific global registry singleton into common production code;
- implementing Forge or NeoForge to make a Fabric gate pass;
- deleting optional compatibility or a test because its API is inconvenient;
- claiming an optional-mod definition range without evidence;
- a conflict with unrelated user changes that cannot be preserved;
- contradictory API evidence that cannot be resolved from permitted sources;
- three failed verification attempts for the current work unit;
- completion of the current stage's primary condition.

At an immediate stop, make no speculative workaround. Record the state and ask for the smallest
necessary decision.

## Runtime command handling

These rules apply only when the user explicitly delegates command execution:

- use plain console output;
- run one bounded command and monitor it to completion;
- do not poll more often than once per minute;
- treat a normal compile or build without a terminal result after 10 minutes as blocked;
- treat a GameTest server without a decisive suite result after 20 minutes as blocked;
- do not kill unrelated processes, stop global daemons, or clear caches;
- record a still-running command in the status and do not start another;
- do not launch an interactive client.

When the user runs commands, ask only for the result needed by the current gate and use the supplied
output as evidence.

## Scope fences

### In scope

- `common/src/main` and `common/src/gametest` as required by the Fabric port;
- `fabric/src/main` and `fabric/src/gametest`;
- shared resources consumed by Fabric;
- Fabric metadata, Mixins, and the common Access Widener;
- narrowly necessary existing Fabric test/run configuration, subject to user direction;
- the five Fabric port documents;
- final reconciliation of `player-view.md`, `as-built.md`, and `build-env.md`.

### Out of scope

- Forge and NeoForge production, resources, metadata, tests, runtime, and packaging;
- release publishing or Git history;
- unrelated refactoring, formatting, or documentation cleanup;
- new eye geometry for newly added or unverified entities;
- optional-mod compatibility claims without a tested 1.21.1 target;
- performance redesign without a demonstrated regression;
- unattended interactive-client testing.

## Session and turn boundaries

Run exactly one stage per session. When the current stage's primary gate passes, or its diagnostic
completion condition is satisfied with all remaining failures classified, stop immediately and hand
off. Do not begin the next stage, inspect its first work unit, or run another gate in the same
session.

Before ending an unfinished session:

1. finish the current safe atomic edit if possible;
2. do not request or start a verification that cannot be monitored;
3. overwrite the status with the complete current position;
4. name the exact next file or command;
5. state whether common/Fabric are expected to compile;
6. leave the failed-attempt count explicit.

The next session resumes from that action. It does not repeat a passing gate unless later edits could
have invalidated it.

## Blocked handoff

When blocked, report:

- stage and bounded work unit;
- intended invariant;
- files changed;
- exact verification command;
- concise result of each failed attempt;
- best current diagnosis;
- smallest required user decision, API information, or external-state change;
- whether the workspace is compile-ready, intentionally incomplete, or has a running command.

The status and log must already contain the durable form of this information.

## Completion handoff

When every gate passes:

- mark the status `complete`;
- record every final passing command in the log;
- summarize behavior-affecting decisions in the status;
- list deferred Forge and NeoForge work;
- list optional-mod definitions and client presentation still requiring manual verification;
- do not commit, publish, or modify Git.
