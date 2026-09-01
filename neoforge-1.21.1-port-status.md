# NeoForge 1.21.1 Port Status

This is the bounded execution snapshot. Overwrite it in place; never append. Reduced history belongs
in `neoforge-1.21.1-port-log.md`, which is not read during normal execution.

## Current position

| Field | Value |
| --- | --- |
| Overall state | **COMPLETE** — all NeoForge stages and automated gates passed |
| Current stage | Stage 7 — Artifact, documentation, and Forge handoff — complete |
| Current work unit | None — hard stop after completed NeoForge handoff |
| Work-unit state | Complete |
| Failed verification attempts used | 0 of 3 |
| Stable documents read this session | Yes — `CLAUDE.md`, plan, process, and status read before Stage 7 |
| Common compile state | Passing — Stage 7 rerun |
| Fabric regression state | Passing — compile, GameTest compile, and all 78 runtime tests |
| NeoForge compile state | Passing — Stage 7 rerun |
| NeoForge GameTest state | Passing — all 78 required tests and a clean exit, Stage 7 rerun |
| Forge state | **COMPLETE** — build and all 78 GameTests pass |
| Last command | User verification: all three loader builds and GameTest servers pass without errors |

The sections below retain the bounded NeoForge-port record and its historical Forge handoff. The
current cross-loader state is that Fabric, NeoForge, and Forge are complete.

## Historical NeoForge completion work unit

### Scope and invariant

Stage 7 is complete. Fabric and NeoForge retain the same common behavior and protocol. All automated
completion and regression gates pass, the NeoForge release artifact is verified by path and file
metadata, orientation documents describe both completed loaders, and Forge is ready without its
Stage 0 having begun.

### Intended files

- None. The NeoForge port is complete.

### Verification command

All Stage 7 commands listed in the final handoff completed successfully.

### Completion condition

- Met. All listed gates pass, the expected NeoForge artifact exists and was verified by path and
  file metadata only, orientation documents describe the completed NeoForge port, outstanding
  manual checks are explicit, and both port statuses contain the Forge handoff with Forge `READY`.

## Frozen architecture matrix

Targeted inspection found 24 Architectury import lines in 17 common production files. Every import is
owned by one of the following rows; there are no unclassified common Architectury imports.

| Category | Current owner and exact common surface | NeoForge disposition | Forge replacement boundary |
| --- | --- | --- | --- |
| Build-time injection — version lookup | Common declares `ModVersionLookup.versionForNamespace`; Fabric supplies `config.fabric.ModVersionLookupImpl` | Stage 1 supplies the matching NeoForge implementation through the existing seam | Retain the loader-neutral common signature; prove Forge transformation support or replace only the loader implementation mechanism |
| Build-time injection — entity persistence | Common declares `EntityPersistentData.get`; Fabric supplies a Mixin-backed `platform.fabric.EntityPersistentDataImpl` | Stage 1 establishes the NeoForge implementation; Stage 2 proves native save/load semantics and unchanged keys | Retain `EntityPersistentData` as the project-owned persistence boundary and replace only its loader implementation |
| Build-time injection — tracking fanout | Common declares `NetworkTracking.send`; Fabric supplies `platform.fabric.NetworkTrackingImpl` and Fabric's tracking lookup | Stage 1 establishes the counterpart signature; Stage 2 connects NeoForge's authoritative tracking recipient set | Retain `NetworkTracking` and replace its Architectury send/runtime use during Forge networking work |
| Build-time injection — eye item construction | Common declares `GooglyEyeItemFactory.create`; Fabric returns the ordinary item and attaches rendering during client init | Stage 1 supplies construction; any NeoForge item-renderer attachment remains Stage 3 | Retain the loader-neutral factory signature; Forge supplies its own construction/render attachment implementation |
| Build-time injection — renderer access | Common declares the three `ClientRendererAccess` methods; Fabric implements them through Access Widener-exposed members | Stage 3 implements access and layer insertion with only demonstrated Access Transformer rules | Retain `ClientRendererAccess`; Forge translates only its required renderer access surface |
| Build-time injection — optional GeckoLib | Common declares `GeckoCompat.enumerate` and `tryAddLayer`; Fabric gates typed integration behind mod presence | Stage 3 supplies a soft-loaded NeoForge bridge against the declared compile-only artifact | Retain `GeckoCompat`; Forge supplies its own optional typed bridge without exposing GeckoLib to common callers |
| Runtime registration | Common owns `ModItems`, `ModDataComponents`, `ModCreativeTabs`, and `ModRecipes` through `DeferredRegister`, `RegistrySupplier`, and `CreativeTabRegistry`; `SomeGooglyCommon.init` invokes them | Keep Architectury 13 registration temporarily and attach it once from the Stage 1 NeoForge bootstrap | Replace runtime registration in Forge Stages 1–2 without changing the four common registry identities or spreading Architectury calls |
| Runtime networking | Common owns `NetworkHandler`, `ClientNetworkHandler`, and five picker packet handlers (`PickerFreezePacket`, `PickerSpawnPacket`, `PickerSpawnAllPacket`, `PickerMobPosePacket`, `PickerExportPacket`) through `NetworkManager` | Keep Architectury 13 typed transport for NeoForge; Stage 2 supplies lifecycle/tracking and Stage 3 physically registers client receivers | Replace transport behind a project-owned Forge boundary while preserving payload ids, bodies, bounds, directionality, context-derived player identity, and protocol 9 |
| Environment lookup | `NetworkHandler.registerCommon` alone uses `Platform.getEnvironment()` and `Env.SERVER` to register dedicated-server clientbound codecs without client receivers | Keep this lookup temporarily; Stage 1 keeps client bootstrap physically isolated and Stage 2 verifies dedicated-server registration | Replace with explicit loader bootstrap/context during Forge networking work; do not spread `Platform` or `Env` |

Frozen rules: add no common Architectury dependency or new common runtime use; add no NeoForge type to
common production code; retain the existing loader-neutral seams; make no packet-id, packet-body,
protocol, persistence-key, item-component, data-format, or player-visible behavior change without
demonstrated evidence and user approval.

## Scaffold inspection

- `neoforge/build.gradle` keeps the established Loom NeoForge platform, transformed common artifact,
  Architectury NeoForge 13.0.8 runtime, compile-only GeckoLib 4.7.4, and Java 21 root toolchain.
- `META-INF/neoforge.mods.toml` declares NeoForge, exact Minecraft range, and Architectury as required;
  GeckoLib 4.7.4 is optional and client-sided.
- `META-INF/accesstransformer.cfg` contains the 36 demonstrated renderer/model access rules required
  by Stage 3.
- `neoforge/src/main/java` contains 14 production files through Stage 3;
  `neoforge/src/gametest/java` contains 13 files: 12 holders and one dev-mod entry point.
- Versions, repositories, module layout, and production source were not changed in Stage 0.

## Last reduced result

Stage 7 reran common and NeoForge production compilation, NeoForge GameTest compilation, the full
NeoForge dedicated server, and the NeoForge build successfully. The server discovered all 78 tests,
all passed, and it exited cleanly. The release artifact exists at
`neoforge/build/libs/somegoogly-neoforge-0.8.1.jar` with size 468,547 bytes and last-write timestamp
2026-08-30 23:44:00 UTC; no artifact was opened or unpacked. Fabric production and GameTest
compilation also pass, and its dedicated server discovered and passed all 78 tests before exiting
cleanly. `player-view.md`, `as-built.md`, and `build-env.md` now describe the completed NeoForge port.

## Known later-stage failures

No automated NeoForge work remains. The physical-client and optional-mod checks below remain manual.

## Decisions

- The frozen matrix above is the ownership contract for the NeoForge port and later Forge handoff.
- Architectury 13 remains temporary for NeoForge registration, networking, and the single
  environment lookup; it will not spread into additional common classes.
- Existing `@ExpectPlatform` signatures remain loader-neutral and receive NeoForge implementations
  only in their assigned later stages.
- Architectury 13's NeoForge registrar discovers the mod container's event bus; common registration
  therefore runs inside the NeoForge constructor without the legacy Forge-only `EventBuses` hook.
- The NeoForge-native CLIENT spec uses `somegoogly-client.toml`. Server settings use the shared
  direct loader so every active world creates and reads `serverconfig/somegoogly-server.toml` rather
  than falling back to NeoForge's instance-wide config directory.
- The item factory supplies NeoForge's lazy custom client renderer while retaining the shared item
  behavior and component payload.
- NeoForge's native entity compound satisfies the project persistence boundary and retains the
  existing keys; the loader-specific persistence GameTest remains Stage 5/6 runtime proof.
- NeoForge events cover the complete Stage 2 server surface, so no NeoForge Mixin was added.
- Damage reactions use `LivingDamageEvent.Post`; harvest drops join `LivingDropsEvent`'s collection;
  healing and completed trades use their native events.
- Architectury 13 remains the temporary typed transport. Its NeoForge adapter supplies the
  authoritative C2S player context; native `PacketDistributor` supplies tracking fanout.
- No common, Fabric, build, metadata, dependency, version, repository, protocol, persistence-key,
  item-component, data-format, or player-visible behavior change was made in Stage 2.
- NeoForge client services are registered once from a physical-client branch, split correctly
  between the mod and game buses, with clientbound receivers installed before payload registration
  freezes.
- The 36 canonical Access Widener entries have a one-for-one named Access Transformer translation;
  renderer and player maps plus layer insertion remain behind `ClientRendererAccess`.
- GeckoLib 4.7.4 stays compile-only and optional. The always-loadable platform gate contains no
  GeckoLib type; typed bone/layer code is reached only after NeoForge reports GeckoLib loaded.
- Stage 3 changed no common, Fabric, build, metadata, dependency, version, repository, protocol,
  persistence-key, item-component, or data-format surface.
- GeckoLib is metadata-optional on the physical client with minimum version 4.7.4 and `AFTER`
  ordering; Architectury 13.0.8 remains the only required temporary runtime library beyond
  NeoForge and Minecraft.
- Common resources remain canonical. NeoForge packages them through `transformProductionNeoForge`
  and contributes only its expanded mod metadata and Access Transformer; source and processed paths
  have no duplicates.
- Stage 4 changed only NeoForge resource expansion and metadata. The 74 vanilla definitions, 171
  optional selectors, common/Fabric sources, protocol, persistence, components, and data formats
  remain unchanged.
- NeoForge GameTests use a separate `somegoogly_gametest` dev mod while holder annotations and the
  run property select the production `somegoogly` template namespace.
- NeoForge's native persistence proof mirrors Fabric's save/load boundary and keeps the same eye-state
  keys and appearance payload. Loader-specific fake players adapt the five shared player-dependent
  assertions without changing common test logic.
- Stage 5 changed only NeoForge build/test source and discovery metadata plus the NeoForge status/log;
  no common, Fabric, or production source changed.
- Picker export converts its unknown `ResourceLocation` to text at the translation-component boundary;
  the preserved shared assertion expects the same visible identifier content.
- The NeoForge client config listener ignores `ModConfigEvent.Unloading` before reading spec values.
  Server settings are reset and loaded directly when each world is about to start.
- The shared picker production/test edit invalidates the corresponding Fabric compile and runtime
  regression gates for Stage 7. Protocol, persistence, components, data formats, and player-visible
  wording remain unchanged.

## Known evidence

- Fabric common, production, resources, 78 GameTests, runtime server, and artifact gates pass.
- `neoforge/build.gradle` targets NeoForge 21.1.248 and Architectury NeoForge 13.0.8.
- `neoforge/src/main/java` contains 14 compiling production files; NeoForge GameTest compilation now
  passes with 13 loader test files and an intended 78-test discovery surface.
- NeoForge metadata exists; its Access Transformer contains the 36 demonstrated renderer/model
  access rules translated from the canonical Access Widener.
- Processed metadata resolves to NeoForge `[21.1.248,22)`, Minecraft `[1.21.1]`, required
  Architectury `[13.0.8,)`, and optional client GeckoLib `[4.7.4,)`, with no placeholders.
- Common resource source/output counts are 262/262 and NeoForge counts are 2/2, with no missing,
  unexpected, or cross-module duplicate paths. All JSON resources parse successfully; pack format
  remains 48.
- Common resources and renderer code already target Minecraft 1.21.1.
- The old Forge source is behavioral reference only and must not be copied mechanically.
- The user verified that the NeoForge build succeeds and all 78 NeoForge GameTests pass without an
  unload exception or shutdown error after the Stage 6 fixes.
- Stage 7 independently reran the common, NeoForge, and invalidated Fabric gates successfully and
  verified the NeoForge release artifact by path, size, and timestamp only.

## Stage 7 Forge handoff

### Final automated gates

- `:common:compileJava` passed.
- `:neoforge:compileJava`, `:neoforge:processResources`, and
  `:neoforge:compileGametestJava` passed.
- `:neoforge:runGameTestServer` discovered and passed all 78 required tests and exited cleanly.
- `:neoforge:build` passed; the release artifact is
  `neoforge/build/libs/somegoogly-neoforge-0.8.1.jar` (468,547 bytes, last written
  2026-08-30 23:44:00 UTC).
- Invalidated Fabric regressions passed: `:fabric:compileJava`,
  `:fabric:compileGametestJava`, and `:fabric:runGameTestServer`; the server discovered and passed all
  78 required tests and exited cleanly.

### Final shared Architectury ownership

The authoritative matrix above remains current: 24 Architectury imports in 17 common production
files. Six build-time seams remain loader-neutral: `ModVersionLookup`, `EntityPersistentData`,
`NetworkTracking`, `GooglyEyeItemFactory`, `ClientRendererAccess`, and `GeckoCompat`. Common runtime
registration remains in `ModItems`, `ModDataComponents`, `ModCreativeTabs`, and `ModRecipes`;
runtime networking remains in `NetworkHandler`, `ClientNetworkHandler`, and the five picker packet
handlers; the single environment lookup remains in `NetworkHandler.registerCommon`.

NeoForge temporarily uses Architectury 13 for runtime registration, networking, and the environment
lookup. Forge has no Architectury 13 platform artifact for Minecraft 1.21.1, so Forge Stage 0 must
freeze replacements for those three runtime categories and separately prove whether build-time
`@ExpectPlatform` injection can remain.

### Common and platform interface changes

- No common `@ExpectPlatform` signature changed, no NeoForge type entered common production code,
  and no new common Architectury dependency or runtime use was added.
- NeoForge supplied implementations for the six existing build-time seams; it added native config,
  event, persistence, and tracking adapters without changing their common contracts.
- The only shared production/test correction made during NeoForge stabilization converts an unknown
  picker-export `ResourceLocation` to its string at the translation-argument boundary and preserves
  the existing rejection text and result. Protocol 9, payload ids and bodies, persistence keys, item
  components, and serialized formats are unchanged.

### Retained manual checks

- Physical NeoForge client: ordinary adult/baby models, players, slime/magma-cube ordering, rabbit,
  llama, sniffer, villager, and special resolver families.
- Pupil motion and every expression; Googly Eye 3D rendering; Slimy Eye tint; harvest, craft, and
  apply round trips; Optometrist books and harvesting; visibility settings; picker editing/export;
  and renderer reload.
- Optional GeckoLib and optional-mod entities available for Minecraft 1.21.1. Optional-mod selectors
  and the 74 vanilla definitions were not changed during the port.

### Exact Forge Stage 0 starting action

In a new session, read `CLAUDE.md`, `forge-1.21.1-port-plan.md`,
`forge-1.21.1-port-process.md`, and `forge-1.21.1-port-status.md`; consult only the relevant Forge
assessment section. Verify this handoff, re-inventory the 24 common Architectury imports, freeze the
Forge retain/replace decisions, inspect the old Forge source only as project behavior reference, and
run one diagnostic `.\gradlew.bat :forge:compileJava --console=plain`. Do not edit production source
in Forge Stage 0.

## Cumulative gates

| Gate | Status |
| --- | --- |
| Architecture matrix frozen | Yes — 24 imports in 17 files classified |
| `:common:compileJava` | Stage 7 rerun passed |
| `:neoforge:compileJava` | Stage 7 rerun passed with 14 production files |
| `:neoforge:processResources` | Stage 7 build passed; Stage 4 paths and metadata inspection remains valid |
| `:neoforge:compileGametestJava` | Stage 7 rerun passed; 78 intended tests in 12 holders |
| `:neoforge:runGameTestServer` | Stage 7 rerun passed; all 78 required tests and clean exit |
| Invalidated Fabric regressions | Stage 7 passed compile, GameTest compile, and 78-test runtime gates |
| `:neoforge:build` | Stage 7 passed; release artifact path and metadata verified without opening it |

## Blockers

None. Retained client and optional-mod checks are manual completion qualifications, not automated
gate blockers.

## Exact next action

All three Minecraft 1.21.1 loader ports are complete and their automated build and GameTest gates
pass. No automated port stage remains. Perform the retained physical-client and optional-mod checks
before claiming visual or third-party compatibility.
