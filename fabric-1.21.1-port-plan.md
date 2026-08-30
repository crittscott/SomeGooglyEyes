# Fabric 1.21.1 Port Plan

## Objective

Produce a Fabric artifact for Minecraft 1.21.1 using Java 21, Fabric Loader 0.19.3, Fabric API
0.116.15+1.21.1, Architectury API 13.0.8, and GeckoLib 4.7.4. It must compile, process its resources,
start under the Fabric GameTest server, pass the ported automated suite, and preserve the behavior and
invariants documented in `player-view.md` and `as-built.md`.

The port is Fabric-first. Common code is in scope because it owns most gameplay and rendering.
Forge and NeoForge runtime implementation are later projects; neither loader is a regression gate
for this plan. Do not add Fabric API types to common production code or enlarge Architectury runtime
coupling solely because Forge is temporarily out of scope.

This plan does not itself authorize Gradle execution. Under `CLAUDE.md`, verification commands are
run by the user unless the user explicitly delegates a named command or stage.

## Completion gates

The Fabric port is complete only when all of these succeed in the current build environment:

1. `.\gradlew.bat :common:compileJava --console=plain`
2. `.\gradlew.bat :fabric:compileJava --console=plain`
3. `.\gradlew.bat :fabric:processResources --console=plain`
4. `.\gradlew.bat :fabric:compileGametestJava --console=plain`
5. `.\gradlew.bat :fabric:runGameTestServer --console=plain`
6. `.\gradlew.bat :fabric:build --console=plain`

The final handoff must identify client presentation and optional-mod compatibility that still require
manual testing. A manual Fabric client smoke test is required before calling visual compatibility
confirmed, although it is not an unattended Gradle gate.

## Session discipline

Execute exactly one stage per session. Begin each session by reading `CLAUDE.md`, this plan,
`fabric-1.21.1-port-process.md`, and `fabric-1.21.1-port-status.md`. Consult the relevant portion of
`fabric-1.21.1-port-assessment.md` only when the current stage needs it. Do not read
`fabric-1.21.1-port-log.md` during execution.

End the session when the stage's primary gate or diagnostic completion condition is satisfied.
`fabric-1.21.1-port-process.md` governs bounded work units, verification attempts, and persistent
state.

## Stage summary

| Stage | Work product | Primary gate or completion condition |
| --- | --- | --- |
| 0 | Compiler baseline and initialized execution state | One diagnostic common compile, with errors classified |
| 1 | Common item data, content, recipes, enchantment, identifiers | Diagnostic common compile; remaining errors assigned later |
| 2 | Common services, configuration, commands, picker, networking | Diagnostic common compile; only client-stage errors remain |
| 3 | Fabric bootstrap, server events, config, platform code, Mixins | Diagnostic Fabric compile; only client-stage errors remain |
| 4 | Shared and Fabric client rendering and attachment | Passing common and Fabric production compiles |
| 5 | 1.21.1 resources and cumulative production verification | Passing resource processing and production compiles |
| 6 | Shared and Fabric GameTest port | Passing Fabric GameTest compile |
| 7 | Runtime stabilization | Passing Fabric GameTest server |
| 8 | Artifact and documentation reconciliation | Passing Fabric build |

Until Stage 4, a diagnostic compile may remain red in a known later-stage subsystem. A stage finishes
only when its own failures are removed and every remaining compiler failure is classified against a
later stage. Run one diagnostic compile after the stage's edits, not one per file or error cluster.

## Stage 0 — Baseline diagnostics

### Scope

- Confirm the successful four-module Gradle sync remains the build-environment baseline.
- Run one diagnostic `:common:compileJava`.
- Count and classify failures into Stages 1, 2, and 4.
- Record errors rooted in Fabric implementers separately for Stage 3.
- Record the first bounded Stage 1 work unit in the status snapshot.

### Constraints

- The diagnostic is discovery, not permission for opportunistic edits.
- Do not run `clean`, refresh dependencies, inspect caches, or change the build environment.
- Do not edit Forge or NeoForge.
- Do not infer the correct replacement API solely from an error message.

### Completion criteria

- The status contains a reproducible compiler baseline and finite Stage 1 work unit.
- The log contains one reduced baseline entry: command, exit status, error count, and major groups.
- No production source has been changed in response to unclassified errors.

## Stage 1 — Common data and content foundation

### Stage 1A: identifiers and simple signatures

- Replace common `ResourceLocation` constructors with `fromNamespaceAndPath`, `parse`, or
  `withDefaultNamespace` according to intent.
- Port item tooltip and other mechanical item signatures.
- Keep identifier parsing failures and translatable diagnostics behaviorally unchanged.

### Stage 1B: eye-item appearance component

- Replace raw stack-tag ownership in `EyeItemProperties` with one component-backed boundary.
- Expected default: register a `somegoogly:eye_properties` data component backed by
  `AppearanceOverride.CODEC`.
- If a registered component cannot preserve the required crafting and item-renderer semantics
  without loader-specific common code, stop before choosing `minecraft:custom_data` instead.
- Update harvest, crafting, tooltip, tint, creation, application, and creative-stack call sites.
- Preserve unrelated components when a recipe modifies an eye.
- No 1.20.1 compatibility reader is required.

### Stage 1C: recipes

- Port `EyeModifierRecipe` and `SlimyEyeRecipe` to the 1.21.1 crafting-input and serializer
  contracts.
- Preserve dynamic appearance output and ordinary recipe-book discovery for the Slimy Eye.
- Keep serializer ids stable unless the serialized format changes materially.

### Stage 1D: Optometrist enchantment and creative tab

- Replace the constructed enchantment subclass with the current data-driven definition and
  holder-based access.
- Port shears-level checks and creative enchanted-book construction.
- Preserve level, supported item, treasure-only acquisition, trading/treasure availability, and
  non-lethal harvesting semantics.
- If current vanilla data facilities cannot express those acquisition rules without a behavior
  choice, stop for the user.

### Verification

- Inspect all changed files and search for the old raw item-tag, recipe serializer, enchantment
  subclass, and identifier-constructor patterns in the Stage 1 scope.
- Run `:common:compileJava` diagnostically once after all Stage 1 work units are complete.
- Remaining errors must belong to common services/networking or client rendering.

## Stage 2 — Common services, configuration, picker, and networking

### Stage 2A: server services and entity APIs

- Port `EyeItemService`, `ServerServices`, `EyeState`, behavior scheduling, eligibility, spawning,
  durability, drops, and entity interactions to changed vanilla signatures.
- Preserve server authority, spawn initialization, stable variant roll, harvest probabilities,
  cooldowns, and expression exclusivity.
- Keep entity persistent state in the existing loader-neutral compound boundary.

### Stage 2B: configuration and datapack reload

- Port resource listener construction, registry access, identifier parsing, and codec calls used by
  `EyeConfigReloadListener`, `ServerEyeConfigs`, and `ClientEyeConfigs`.
- Preserve atomic server replacement, server-side version selection, validation limits, and resolved
  synchronization to clients.
- Do not revise optional-mod version ranges in Java code.

### Stage 2C: commands and picker services

- Port admin and client command construction, entity lookup, spawning, pose mutation, export,
  filesystem-safe world datapack output, freeze ownership, and rate limiting.
- Preserve authorization, creative-mode requirements, target ranges, cooldowns, and export
  directory containment.

### Stage 2D: networking

- Establish the Architectury API 13 payload registration and buffer contract from permitted
  documentation and compiler evidence.
- Port the hello/ack exchange, state/config/behavior payloads, all picker requests, tracking fanout,
  and pending client state.
- Preserve directionality, server-derived player identity, bounded decoding, login mismatch and
  timeout disconnects, and one-snapshot synchronization.
- Change the protocol identifier if and only if the wire representation changes; no legacy bridge is
  required.

### Verification

- Run `:common:compileJava` diagnostically once after Stage 2.
- Only shared client/rendering failures and Fabric implementation failures may remain.
- Record any changed platform-interface signature for Stage 3.

## Stage 3 — Fabric server and platform integration

### Stage 3A: bootstrap, events, and commands

- Port `SomeGooglyFabric`, `FabricServerEvents`, resource reload registration, server lifecycle,
  entity load/tracking, connection, datapack synchronization, death, entity-use, and command
  callbacks.
- Preserve registration order only where it carries behavior.
- Confirm dedicated-server safety.

### Stage 3B: configuration

- Port `FabricToml`, `FabricServerConfig`, `FabricClientConfig`, and
  `FabricEyeConfigReloadListener`.
- Preserve client and per-world server paths, defaults, validation, and Fabric's load-at-start
  behavior.

### Stage 3C: platform implementations

- Port Fabric implementations of entity persistent data, tracking-player lookup, version lookup, and
  item construction to the Stage 1 and Stage 2 common signatures.
- Do not copy Forge mechanisms into Fabric when Fabric provides an established facility.

### Stage 3D: server Mixins

- Re-establish the 1.21.1 entity save/load, hurt, heal, and completed-trade targets and descriptors.
- Keep the persistent compound, player-damage trigger, healing cooldown path, and trade swirl
  behavior unchanged.
- Treat an unapplied or incorrectly targeted Mixin as a failure even when Java compilation passes.

### Verification

- Inspect Mixin config and changed JSON deterministically.
- Run `:fabric:compileJava` diagnostically once.
- Only shared or Fabric client errors assigned to Stage 4 may remain.

## Stage 4 — Shared and Fabric client rendering

### Stage 4A: rendering primitives and layers

- Port `ModelGooglyEye`, `GooglyEyeRenderer`, `LayerGooglyEyes`, pupil transforms, behavior
  animation, invisibility/display gating, and slime-layer ordering.
- Port generic and render-method signatures without moving client classes into server paths.

### Stage 4B: attachment resolvers and Access Widener

- Port the model-part tree, hierarchical, age-dependent, rabbit/llama, Citadel, LLibrary, Twilight
  Forest, and reflected-box resolvers that still correspond to 1.21.1 model families.
- Restore only the Access Widener entries the ported code demonstrably requires.
- Keep attachment caching by model identity and clear it on renderer/runtime resets.
- If required private member identities cannot be established from permitted sources, stop rather
  than inspect prohibited Minecraft or Forge sources.

### Stage 4C: picker client

- Port picker keys, input, HUD, preview layer, axis gizmo, model-part vocabulary, client commands,
  and export-all filesystem output.
- Preserve chosen-entity state, draft-discard rules, unchanged-component marker semantics, and
  client-only export containment.

### Stage 4D: Fabric client integration

- Port `SomeGooglyFabricClient`, `FabricClientEvents`, `FabricClientCommands`,
  `ClientRendererAccessImpl`, renderer reload handling, item colors, HUD/input callbacks, and the
  Googly Eye item renderer registration.
- Verify physical-side safety.

### Stage 4E: GeckoLib

- Port the Fabric GeckoLib bridge, bones, renderer layer, and integration classes against the
  declared 1.21.1 artifact.
- Preserve optional loading: GeckoLib absence or one unsupported renderer must not prevent the base
  mod from loading.

### Verification

- `:common:compileJava` must pass.
- `:fabric:compileJava` must pass.
- Inspect the Access Widener and Mixin configuration for stale 1.20.1 members and targets.
- Record manual visual checks that headless verification cannot cover.

## Stage 5 — Resources and cumulative production verification

### Stage 5A: resource layout and metadata

- Move `data/somegoogly/recipes` to `data/somegoogly/recipe`.
- Move `data/somegoogly/structures` to `data/somegoogly/structure`.
- Port recipe JSON result and ingredient forms.
- Add the data-driven Optometrist enchantment resources and any required item tags.
- Set the appropriate 1.21.1 pack metadata.
- Validate Fabric production metadata and Mixin JSON.

### Stage 5B: eye-definition selection

- Update the 74 Minecraft definitions that select exactly `1.20.1` so they can resolve for
  `1.21.1`.
- Do not advance optional-mod version ranges without evidence for the corresponding 1.21.1 mod
  release.
- Record vanilla entities newly lacking definitions and geometry needing manual adjustment.
- Validate every changed JSON file deterministically.

### Verification

- `:common:compileJava` must pass.
- `:fabric:compileJava` must pass.
- `:fabric:processResources` must pass.
- Inspect the processed-resource paths without unarchiving an artifact.

## Stage 6 — Fabric GameTest port

### Stage 6A: shared logic

- Port the 12 shared `*Logic` classes to current identifiers, item components, crafting, enchantment,
  buffers/payloads, entity state, registry context, and GameTest APIs.
- Preserve the 77 shared public assertion methods unless an intentional consolidation is documented
  and retains equivalent coverage.

### Stage 6B: Fabric wrappers and discovery

- Port the 12 Fabric wrapper classes and Fabric GameTest entrypoint metadata.
- Preserve the intended 78 wrapper methods and ensure every shared logic class remains discoverable.
- Update the structure template location and annotation shape only as required by 1.21.1/Fabric API.

### Stage 6C: new-boundary coverage

- Add focused assertions for the eye appearance component, data-driven Optometrist lookup,
  packet/protocol changes, and Fabric entity persistence where existing tests do not establish them.
- Do not introduce another test framework.

### Verification

- `:fabric:compileGametestJava` must pass.
- Inspect discovery metadata so a later passing server cannot be caused by zero discovered tests.

## Stage 7 — Runtime GameTest stabilization

Run the complete Fabric GameTest server and address failures in bounded demonstrated-cause groups:

1. bootstrap, registry, resource, or discovery failures;
2. eye item component, recipe, enchantment, and serialization failures;
3. entity persistence, eligibility, spawn, and variant failures;
4. networking, tracking, protocol, and pending-state failures;
5. picker export/freeze and behavior-scheduler failures;
6. dedicated-server classloading or Mixin application failures.

### Verification

- Run `:fabric:runGameTestServer --console=plain`.
- Require a reported nonzero test count, a fully passing suite, and a clean server exit.
- Apply the three-attempt rule in `fabric-1.21.1-port-process.md` to each bounded failure unit.

### Test discipline

- Fix production code when a preserved assertion exposes a production defect.
- Update a test only for a demonstrated 1.21.1 or Fabric API change.
- Do not add delays, broaden tolerances, swallow exceptions, or remove assertions to pass.
- Treat hangs, crashes before discovery, missing Mixins, and zero discovered tests as failures.

## Stage 8 — Final package and reconciliation

### Work units

1. Re-run any cumulative gate invalidated by Stage 7 fixes.
2. Run `:fabric:build --console=plain`.
3. Verify the expected Fabric artifact exists without inspecting or unarchiving it.
4. Reconcile `as-built.md`, `player-view.md`, and `build-env.md` with the completed Fabric state.
5. Record Forge and NeoForge as subsequent ports and identify the Architectury API constraint for
   Forge.
6. Record unsupported or unverified optional-mod definitions and required manual client checks.

### Completion criteria

- Every completion gate passes.
- The status says `complete` and the log contains each final passing result.
- Documentation describes the current 1.21.1 Fabric implementation, not the port history.
- Forge and NeoForge are not represented as complete.
- No Git, GitHub, publishing, or release action has occurred.

## Required manual Fabric client checks

A human client smoke test should cover:

- ordinary adult and baby vanilla models;
- player eyes;
- slime/magma-cube layer ordering;
- rabbit, llama, sniffer, villager, and other special resolver families;
- pupil motion, settling, blinking, grow, swirl, stare, side-eye, and cross-eye expressions;
- Googly Eye 3D item rendering and Slimy Eye tint;
- harvest-craft-apply appearance round trips;
- Optometrist enchanted books and shears-only harvesting;
- client visibility configuration;
- picker selection, gizmo, editing, save/export, and export-all;
- renderer reloads and optional GeckoLib entities where available.

Any failure opens a new bounded client work unit and must be reflected in `player-view.md` if it
remains an accepted limitation.
