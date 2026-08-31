# Fabric 1.21.1 Port Status

This is the compact execution snapshot. Overwrite it in place whenever the position changes; never
append to it. Reduced verification history belongs in the append-only
`fabric-1.21.1-port-log.md`, which is not read during normal execution.

`fabric-1.21.1-port-process.md` governs this snapshot.

## Current position

| Field | Value |
| --- | --- |
| Overall state | **COMPLETE** — all Fabric 1.21.1 port gates pass |
| Current stage | Stage 8 — Final package and documentation reconciliation |
| Current work unit | Tag translation, final Fabric artifact, and current-state documentation |
| Work-unit state | Complete |
| Failed verification attempts used | 1 of 3 |
| Stable documents read this session | Yes |
| Cross-loader state | **COMPLETE** — Fabric, NeoForge, and Forge builds and 78-test suites pass |
| Common compile state | Passing on 2026-08-29 after Stage 5 resources; task up to date |
| Fabric compile state | Passing on 2026-08-29 after Stage 5 resources; task up to date |
| Forge state | Complete — build and all 78 GameTests pass |
| NeoForge state | Complete — build and all 78 GameTests pass |
| Last updated | 2026-08-31 cross-loader documentation reconciliation |
| Last command | User verification: all three loader builds and GameTest servers pass without errors |

The sections below retain the bounded Fabric-port record. Statements describing NeoForge and Forge
as future work record the state when the Fabric port closed; the cross-loader fields above and the
subsequent-completion section below are current.

## Historical Fabric completion work unit

### Scope and invariant

Add the missing display translation for the Optometrist shears-support item tag, produce and locate
the Fabric release artifact without opening it, and reconcile the player, implementation, and build
orientation documents with the completed Minecraft 1.21.1 Fabric baseline. Preserve player-visible
behavior and represent Forge and NeoForge as subsequent, incomplete ports.

### Intended files

- `common/src/main/resources/assets/somegoogly/lang/en_us.json`
- `player-view.md`
- `as-built.md`
- `build-env.md`
- Stage 8 state in `fabric-1.21.1-port-status.md` and reduced audit entries in
  `fabric-1.21.1-port-log.md`

### Verification command

1. `.\gradlew.bat :fabric:processResources --console=plain`
2. `.\gradlew.bat :fabric:build --console=plain`

### Completion condition

- the item-tag translation key exists in source and processed resources;
- the invalidated Fabric resource gate and final Fabric build pass;
- the expected release JAR exists under `fabric/build/libs` and is not opened or unpacked;
- `player-view.md`, `as-built.md`, and `build-env.md` describe the current 1.21.1 Fabric state,
  subsequent Forge/NeoForge work, and remaining manual compatibility checks;
- every completion gate is passing and the failed-attempt count remains explicit.

## Stage 0 compiler baseline

`.\gradlew.bat :common:compileJava --console=plain` reached `:common:compileJava` and failed in 11
seconds with 87 errors and 10 warnings. One actionable task executed. The preliminary sandbox launch
could not access the existing Gradle distribution lock; the identical authorized retry reached the
compiler. Neither run counts against a post-edit work-unit attempt.

| Assigned stage | Errors | Failure groups |
| --- | ---: | --- |
| Stage 1 | 36 | Six non-rendering identifier constructors; eye-item raw NBT and tooltip signatures; dye color API; both recipe contracts and serializers; data-driven enchantment and holder access |
| Stage 2 | 8 | Five networking buffer/send signatures; `NbtAccounter` construction; two durability callback signatures |
| Stage 3 | 0 | No Fabric implementation error was exposed by the common production compile |
| Stage 4 | 43 | Client rendering identifiers and signatures; vertex and render-system APIs; renderer-layer access; model-part, ageable-model, rabbit, llama, and Twilight Forest attachment access |

The 10 warnings are deprecated Architectury receiver registrations in `ClientNetworkHandler` and
`NetworkHandler`; they are assigned to Stage 2 networking. All 87 compiler errors are classified.

## Stage 1 diagnostic result

The single Stage 1 diagnostic reached `:common:compileJava` and failed in 9 seconds with 51 errors
and 10 warnings. All 36 Stage 1 baseline errors are removed. The generated problems report confirms
that the remaining errors are exactly 8 assigned to Stage 2 and 43 assigned to Stage 4. No Stage 1
correction attempt was required.

## Stage 2 diagnostic result

The sandboxed launch could not access the existing Gradle distribution lock. The identical
authorized retry reached `:common:compileJava` and failed in 9 seconds with 44 errors and no warnings.
All original eight Stage 2 errors and ten networking warnings were removed; the one newly exposed
bounded-NBT return-type error was corrected. The final diagnostic failed in 9 seconds with exactly
the 43 errors assigned to Stage 4 and no warnings, satisfying Stage 2's completion condition.

## Stage 3 diagnostic result

Fabric tracking was updated to send the shared typed payload, and the Fabric reload-listener id was
ported to the 1.21.1 identifier factory. Bootstrap, callback, configuration, persistent-data, item,
version, reaction, trade, and Mixin-registration sources were inspected; no other Stage 3 source
change was indicated. The Mixin JSON parses successfully and keeps the renderer Mixin client-only.

The delegated `.\gradlew.bat :fabric:compileJava --console=plain` diagnostic exited in 9 seconds at
its prerequisite `:common:compileJava`, which reported exactly the known 43 Stage 4 rendering errors
and no warnings. `:fabric:compileJava` did not execute. Because the failed common compile did not emit
all classes required on Fabric's compile classpath, excluding the prerequisite would not be valid
verification. This is failed verification attempt 1 of 3 and a stage-ordering blocker, not evidence
of a Stage 3 source failure.

The passing Stage 4 Fabric production compile subsequently executed the full Fabric compiler and
certified the Stage 3 implementation together with the current client code.

## Stage 4 diagnostic result

The shared client migration replaced 1.20.1 identifier, vertex-consumer, normal-transform, and
camera-rotation calls; restored only the renderer, model-tree, age-transform, rabbit, llama, and
render-type access used by current source; and retained the existing resolver and picker behavior.
The common compile passed on correction attempt 2 with no warnings.

Fabric client integration restored renderer-map/layer access, made the player-renderer map key
loader-neutral, moved GeckoLib's animatable import to its 4.7.4 package, and retained GeckoLib's
supported animatable model lookup for the out-of-render enumeration path. The Fabric compile passed
on attempt 3 with no warnings. Access Widener and Mixin JSON inspection passed; the dispatcher reload
Mixin remains client-only. Headless verification does not establish visual attachment or animation
correctness, so the planned manual client checks remain outstanding.

## Stage 5 result

The two custom recipes and GameTest structure moved to the singular 1.21.1 registry directories;
the Slimy Eye result uses the current `id` field; and the common pack metadata targets data pack
format 48. Optometrist now has a data-driven definition, a shears-only supported-item tag, and the
treasure/trade/random-loot tag membership needed to preserve its acquisition boundary without
enchanting-table generation.

All 74 existing Minecraft definitions select exactly 1.21.1. Optional-mod version selectors were
outside the bounded replacement and remain unchanged. Armadillo, bogged, and breeze have no bundled
geometry and remain a manual content task. Deterministic validation parsed 259 production JSON and
metadata files. The common and Fabric compiles and Fabric resource processing passed; direct
inspection of both processed-resource trees found all singular paths, no plural predecessors, and
74 processed vanilla definitions.

## Stage 6 result

The 12 shared GameTest logic classes now use the established 1.21.1 identifier, crafting-input,
item-component, registry-holder, payload, and entity boundaries. Player-dependent shared assertions
remain loader-neutral by accepting a vanilla `Player`; the Fabric wrappers supply Fabric API fake
players. The recipe tests now verify preservation of an unrelated component, and Optometrist is
resolved from the data-driven enchantment registry and checked for shears-only support and treasure
membership.

Fabric retains all 12 discovery entrypoints and now exposes 78 wrapper tests over the 77 shared
assertions. The additional Fabric-only assertion exercises the entity persistent-data Mixin through
an actual save/load round trip. Discovery JSON parses, targeted stale API searches are empty, and
`:fabric:compileGametestJava` passed on the first post-edit verification with no warnings.

## Stage 7 result

The user-run Fabric GameTest server started successfully, discovered all 78 tests, passed all 78
required tests in 1.202 seconds, shut down cleanly, and completed the Gradle invocation in 16
seconds. No runtime correction attempt was required. The run emitted one non-fatal Fabric convention
warning because the project-owned `somegoogly:enchantable/shears` item tag has no
`tag.item.somegoogly.enchantable.shears` entry in `en_us.json`; this does not affect tag loading,
Optometrist behavior, or the passing suite.

## Stage 8 result

The missing `tag.item.somegoogly.enchantable.shears` translation now resolves to `Enchantable
Shears` in source and processed common resources. The invalidated Fabric resource gate passed in 7
seconds. The first Fabric build attempt failed before compilation because Gradle could not clean
stale `:common:processResources` outputs; the single permitted identical retry passed in 14 seconds
with 6 tasks executed and 7 up to date.

The remapped release artifact exists at `fabric/build/libs/somegoogly-fabric-0.8.1.jar` and is
477,665 bytes. It was verified by path and file metadata only and was not opened or unpacked.
`player-view.md`, `as-built.md`, and `build-env.md` now describe the completed Minecraft 1.21.1
Fabric runtime, the incomplete Forge and NeoForge ports, and the remaining manual compatibility
checks.

## Established decisions and evidence

- Fabric is the first runtime port and establishes the 1.21.1 common baseline.
- Forge and NeoForge are not regression gates and are not edited during this plan.
- The successful Java 21/Minecraft 1.21.1 Gradle baseline is frozen unless the user explicitly
  reopens build-environment work.
- No compatibility layer for unreleased 1.20.1 item, entity, datapack, or wire data is required.
- Player-visible behavior and server authority remain as documented in `player-view.md` and
  `as-built.md`.
- Gradle gates are user-run unless explicitly delegated.
- The common Access Widener remains an empty placeholder until Stage 4 establishes required 1.21.1
  members.
- The Fabric port must not add Fabric API types to common production code or expand Architectury
  runtime coupling.
- Gradle's generated problems report preserved all 87 compiler diagnostics after direct console
  capture truncation; it was inspected without rerunning the compile.
- Architectury 13 networking uses one typed payload wrapper per existing channel; packet ids and
  bodies remain unchanged, so protocol version 9 remains the wire contract.
- Dedicated servers register clientbound payload codecs without client receivers; physical clients
  register the corresponding receivers from `ClientNetworkHandler`.
- `NetworkTracking.send` now accepts a `CustomPacketPayload`; Fabric must port its implementation in
  Stage 3.
- Successful eye harvesting now passes the actual hand equipment slot to the 1.21.1 durability API;
  both harvest paths still apply exactly one durability point.
- Picker export decoding uses `NbtAccounter.create(MAX_CONFIG_BYTES)` and accepts only a compound
  root; oversized or wrong-root payloads reach the existing null-payload rejection path.
- Fabric tracking now passes one typed `CustomPacketPayload` to the unchanged authoritative
  tracking recipient set.
- Fabric configuration retains the per-world server and game-config client paths; the reload
  listener id remains `somegoogly:eye_configs` through the 1.21.1 identifier factory.
- Vertex emission uses the 1.21.1 fluent `addVertex`/`set*` contract and transforms normals through
  the full `PoseStack.Pose`.
- Pupil gravity converts the local pose back to world orientation with the active camera quaternion.
- The Access Widener contains only members referenced by current shared/Fabric rendering source.
- Shared renderer-map consumers treat the player-skin key as opaque; Fabric 1.21.1 uses
  `PlayerSkin.Model`, while Forge's later port must update its implementer to the common wildcard key.
- GeckoLib remains soft-gated; the typed integration catches unsupported renderers and is loaded only
  when GeckoLib is present.
- The combined common resource pack uses 1.21.1 data pack format 48; Fabric's built-in mod-resource
  loading supplies both the shared assets and data.
- Optometrist is defined as a one-level, rare-weight, main-hand, shears-only enchantment. It extends
  the treasure, tradeable, random-loot, and double-trade-price tags and is deliberately absent from
  non-treasure and enchanting-table tags.
- The 74 existing Minecraft eye definitions now select exactly 1.21.1. Optional-mod selectors were
  outside the mechanical update and remain unchanged.
- Minecraft 1.21.1 adds armadillo, bogged, and breeze relative to the 1.20.1 definition baseline;
  they have no authored eye geometry in this port and remain a manual content task.
- Stage 6 crafting tests use `CraftingInput` and verify preservation of an unrelated data component
  alongside the eye appearance component.
- Shared player-dependent assertions accept a vanilla `Player`; Fabric wrappers supply Fabric API
  `FakePlayer` instances without introducing Fabric types into shared test logic.
- The Optometrist test resolves its data-driven registry holder, verifies shears-only support, and
  verifies treasure-tag membership.
- Stage 6 retains 77 shared assertion methods and supplies 78 Fabric wrapper tests; the additional
  Fabric-only test exercises entity eye-state persistence through the save/load Mixin boundary.
- Stage 7 discovered and passed all 78 required tests on the dedicated Fabric GameTest server. The
  missing display translation for `somegoogly:enchantable/shears` is a non-fatal resource-quality
  warning, not a runtime or test failure.
- Stage 8 supplies `tag.item.somegoogly.enchantable.shears` as `Enchantable Shears` and documents
  Fabric as the sole completed 1.21.1 runtime target. Forge remains unported 1.20.1-era source around
  1.21.1 build metadata; NeoForge remains a metadata/access scaffold with no runtime Java.
- The final Fabric build produces `somegoogly-fabric-0.8.1.jar`; the artifact was confirmed without
  inspection or extraction.

## Cumulative gate record

| Gate | Status | Evidence |
| --- | --- | --- |
| Gradle/IDE sync | Passing | Four-module reload completed 2026-08-29 |
| `:common:compileJava` | Passing | Stage 4 shared-client gate passed in 15 seconds with no warnings on 2026-08-29 |
| `:fabric:compileJava` | Passing | Stage 4 gate passed in 17 seconds with no warnings on 2026-08-29 |
| `:fabric:processResources` | Passing | Stage 8 rerun passed in 7 seconds; task up to date on 2026-08-29 |
| `:fabric:compileGametestJava` | Passing | Stage 6 gate passed in 9 seconds with no warnings on 2026-08-29 |
| `:fabric:runGameTestServer` | Passing | User-run Stage 7 gate passed 78 of 78 required tests and exited cleanly in 16 seconds on 2026-08-29 |
| `:fabric:build` | Passing | Stage 8 gate passed on the identical retry in 14 seconds on 2026-08-29 |

## Subsequent loader completion

- NeoForge and Forge ports are complete. Each produces a Minecraft 1.21.1 release artifact and its
  dedicated server discovers and passes all 78 required GameTests with a clean exit.
- Shared runtime registration and networking now use project-owned loader-neutral boundaries.
  Fabric and NeoForge retain Architectury API 13 at runtime; Forge uses native runtime facilities
  and only Architectury's build-time `@ExpectPlatform` transformation.

## Manual verification still required

- Physical-client rendering for ordinary adult/baby models, players, slime/magma-cube layer order,
  rabbit, llama, sniffer, villager, and other specialized attachment families.
- Pupil motion and every expression; Googly Eye 3D rendering; Slimy Eye tint; harvest/craft/apply
  appearance flow; Optometrist books and harvesting; client visibility settings; picker editing and
  export; renderer reloads.
- Optional GeckoLib entities and all optional-mod eye definitions against actual Minecraft 1.21.1
  mod releases. The retained optional-mod version selectors are not compatibility claims.
- Armadillo, bogged, and breeze still require authored eye geometry.

## Blockers

None.

## Exact next action

All three Minecraft 1.21.1 loader ports are complete and their automated build and GameTest gates
pass. Perform the documented physical-client and optional-mod checks before claiming visual or
third-party compatibility. The historical Fabric Stage 8 used 1 of 3 failed attempts: a stale-output
cleanup failure whose identical retry passed.
