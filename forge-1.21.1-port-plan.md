# Forge 1.21.1 Port Plan

## Objective and prerequisite

Produce a Forge artifact for Minecraft 1.21.1 using Java 21, Forge 52.1.16, and GeckoLib 4.7.4 while
preserving the completed Fabric and NeoForge behavior. Replace unsupported Architectury runtime
facilities with loader-neutral project boundaries, keep prior loaders passing, port Forge runtime and
tests, and package the artifact.

**Prerequisite:** `neoforge-1.21.1-port-status.md` must say `COMPLETE`, and its final handoff must set
`forge-1.21.1-port-status.md` to `READY`. Until then, this plan is blocked and no Forge stage may
begin.

This plan does not authorize Gradle execution. The user runs builds/tests unless a named command or
stage is explicitly delegated.

## Completion gates

1. `.\gradlew.bat :common:compileJava --console=plain`
2. `.\gradlew.bat :fabric:compileJava --console=plain`
3. `.\gradlew.bat :neoforge:compileJava --console=plain`
4. `.\gradlew.bat :forge:compileJava --console=plain`
5. `.\gradlew.bat :forge:processResources --console=plain`
6. `.\gradlew.bat :forge:compileGametestJava --console=plain`
7. `.\gradlew.bat :fabric:runGameTestServer --console=plain` after shared runtime/test changes
8. `.\gradlew.bat :neoforge:runGameTestServer --console=plain` after shared runtime/test changes
9. `.\gradlew.bat :forge:runGameTestServer --console=plain`
10. `.\gradlew.bat :forge:build --console=plain`

Prior-loader runtime regressions may be deferred to the cumulative stage only while their production
compiles and shared tests remain demonstrably coherent. They are mandatory before completion.

## Session discipline and hard stops

Execute exactly one stage per session. Read `CLAUDE.md`, this plan,
`forge-1.21.1-port-process.md`, and `forge-1.21.1-port-status.md` at session start; consult only the
relevant assessment section. Do not read the Forge log during normal execution.

**Stop immediately when the current stage completion condition is satisfied.** Update status,
append reduced evidence, hand off, and do not inspect or begin the next stage.

## Stage summary

| Stage | Work product | Completion condition |
| --- | --- | --- |
| 0 | NeoForge handoff audit, architecture decision, Forge baseline | Prerequisite proven; replacement boundaries frozen; diagnostic classified |
| 1 | Loader-neutral registration and environment boundary | Common, Fabric, and NeoForge production compiles pass |
| 2 | Loader-neutral networking boundary | Common/Fabric/NeoForge compiles and focused protocol tests pass |
| 3 | Forge bootstrap, config, platform services, server events | Forge server-side source compiles or only client errors remain |
| 4 | Forge client, rendering, access, picker, GeckoLib | All three production loader compiles pass |
| 5 | Forge resources and cumulative prior-loader regression | Resources and all invalidated prior-loader gates pass |
| 6 | Forge GameTest port | Forge GameTest compile passes with intended discovery set |
| 7 | Forge dedicated-server stabilization | Every required Forge test passes and server exits cleanly |
| 8 | Artifact and final documentation | Forge build passes; all three loaders truthfully documented |

## Stage 0 — Handoff audit and baseline

### Work

- Verify the NeoForge status is complete and contains final gates, shared interfaces changed, the
  Architectury ownership matrix, and manual limitations.
- Re-inventory common Architectury imports and compare them with that handoff.
- Decide separately whether build-time `@ExpectPlatform` remains and how runtime registration,
  networking, and environment lookups will be replaced.
- Inspect the old Forge source as behavioral project code, not API evidence.
- Obtain one diagnostic `:forge:compileJava` and classify failures into Stages 1–4.

### Hard stop

Stop when the prerequisite, decisions, and baseline are durable. No production source edit belongs
in Stage 0.

## Stage 1 — Registration and environment decoupling

### Work

- Replace common Architectury runtime registration/creative-tab facilities with the smallest
  loader-neutral boundary supported by current vanilla and each loader lifecycle.
- Preserve ids and stable handles for items, data components, recipe serializers, and the creative
  tab.
- Implement the boundary for Fabric and NeoForge first, then provide the Forge registration side
  needed for later bootstrap.
- Replace `Platform`/`Env` lookup with explicit side-safe initialization or a smaller project-owned
  context.
- Do not alter networking yet except where type separation is required to compile the boundary.

### Verification and hard stop

Require passing common, Fabric, and NeoForge production compiles. Obtain a diagnostic Forge compile
only if needed to classify remaining work. Stop immediately; do not begin networking.

## Stage 2 — Networking decoupling

### Work

- Remove common runtime dependence on Architectury `NetworkManager` while retaining vanilla typed
  payload definitions and stream codecs.
- Define one coherent project-owned transport boundary for codec registration, direction-specific
  receivers, context execution, disconnects, direct sends, and tracking fanout.
- Implement and register the boundary for Fabric, NeoForge, and Forge.
- Preserve protocol 9 and packet bytes unless a demonstrated change requires a deliberate protocol
  update.
- Preserve login mismatch/timeout, server-derived sender identity, authorization, size bounds,
  pending client state, and one-snapshot synchronization.
- Update focused shared/Fabric/NeoForge tests without weakening assertions.

### Verification and hard stop

Require common, Fabric, and NeoForge production compiles plus the narrowest available focused test
gate proving payload ids/codecs. Obtain a diagnostic Forge compile and classify remaining loader
errors. Stop immediately after the networking boundary passes on prior loaders.

## Stage 3 — Forge server runtime

### Work

- Replace the old Forge entry point with current Forge 52 bootstrap and registration wiring.
- Port server/client config registration while preserving documented paths and validation.
- Implement version lookup, item construction, entity persistence, and tracking platform seams.
- Port eye-definition reload, entity load/tracking, connection/datapack lifecycle, commands, ticks,
  stop cleanup, item use/drops, damage, healing, and completed trades.
- Use Forge-native events/facilities when available; justify any Mixin by a missing event boundary.
- Keep client classes unavailable to dedicated-server execution.

### Verification and hard stop

Obtain one diagnostic `:forge:compileJava`. Stop when all server/bootstrap failures are removed and
only classified Stage 4 client failures remain, or when production compilation passes.

## Stage 4 — Forge client, access, picker, and GeckoLib

### Work

- Port layer installation/reset, renderer access, item renderer, tint, client commands, picker
  keys/HUD/input, inspector, disconnect cleanup, and renderer reload.
- Translate only demonstrably required common Access Widener members into Forge Access Transformer
  declarations.
- Port optional GeckoLib integration against its Forge 1.21.1 artifact with strict soft loading.
- Preserve layer order, renderer caches, player variants, age transforms, attachments, and export
  containment.

### Verification and hard stop

Require passing common, Fabric, NeoForge, and Forge production compiles. Inspect Forge client-only
registration and the Access Transformer. Stop when all four compiles pass.

## Stage 5 — Resources and cumulative regression

### Work

- Replace stale Forge metadata with the current Forge 52 form and validate dependency/side ranges.
- Verify transformed common resources, Access Transformer discovery, optional GeckoLib declaration,
  and absence of an Architectury runtime dependency in the Forge artifact metadata.
- Process Forge resources and inspect paths without opening an artifact.
- Run every Fabric and NeoForge compile/resource/test gate invalidated by Stages 1–4.
- Run Fabric and NeoForge GameTest servers because shared registration/networking runtime changed.

### Verification and hard stop

Require Forge resource processing, all production compiles, and all invalidated Fabric/NeoForge gates
to pass. Stop before porting Forge GameTest wrappers.

## Stage 6 — Forge GameTests

### Work

- Port discovery metadata, annotations, wrappers, and test-mod entry point to Forge 52.
- Expose all 77 shared assertions and one Forge entity persistence save/load assertion.
- Update tests only for the established registration/networking boundary or current Forge APIs.
- Deterministically verify a nonempty intended discovery set.

### Verification and hard stop

Require `:forge:compileGametestJava` to pass. Stop before starting the GameTest server.

## Stage 7 — Forge runtime stabilization

Run the complete Forge GameTest server only when explicitly authorized. Repair bounded causes in
order: bootstrap/discovery; registry/resources; persistence/events; networking/handshake;
items/recipes/enchantment; picker/behavior; dedicated-server classloading.

Require a nonzero discovered count, every required test passing, and a clean exit. Apply the
three-attempt rule independently to each demonstrated cause. Stop immediately after the suite passes.

## Stage 8 — Package and reconcile

### Work

- Re-run any gate invalidated by Stage 7 corrections.
- Run `:forge:build` when explicitly authorized and verify the expected artifact by path and file
  metadata only; do not open or unarchive it.
- Reconcile `player-view.md`, `as-built.md`, and `build-env.md` for all three loaders.
- Record manual client/optional-mod checks and any intentional per-loader implementation difference.
- Mark Forge complete only when the cumulative Fabric, NeoForge, and Forge gates pass.

### Hard stop

Stop after final status/log updates. Do not commit, publish, or begin unrelated work.

## Required manual Forge client checks

Use the same matrix as Fabric and NeoForge: ordinary/baby/player/special models; slime layer order;
pupil motion and expressions; both item render paths; harvest-craft-apply; Optometrist; visibility;
picker; renderer reload; and available optional GeckoLib/third-party entities.
