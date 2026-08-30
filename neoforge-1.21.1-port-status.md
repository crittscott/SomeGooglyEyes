# NeoForge 1.21.1 Port Status

This is the bounded execution snapshot. Overwrite it in place; never append. Reduced history belongs
in `neoforge-1.21.1-port-log.md`, which is not read during normal execution.

## Current position

| Field | Value |
| --- | --- |
| Overall state | **READY** — Stage 6 complete; Stage 7 is next |
| Current stage | Stage 6 — Runtime stabilization — complete |
| Current work unit | None — hard stop after passing Stage 6 gates |
| Work-unit state | Complete |
| Failed verification attempts used | 0 of 3 for the final config-lifecycle unit |
| Stable documents read this session | Yes — `CLAUDE.md`, plan, process, and status reread for Stage 6 |
| Common compile state | Passing — explicit Stage 4 cumulative gate |
| Fabric regression state | Passing at Fabric completion |
| NeoForge compile state | Passing — explicit Stage 4 cumulative gate |
| NeoForge GameTest state | Passing — all 78 required tests and a clean exit, user verified |
| Forge state | Waiting on the completed NeoForge handoff |
| Last command | User-reported NeoForge build and GameTest run — both succeeded without error |

## Current work-unit definition

### Scope and invariant

Stage 6 is complete. Picker export now passes the unknown entity identifier to the translation
component as supported text while preserving the rejection result. NeoForge client and server config
adapters continue synchronizing values on load/reload and ignore `ModConfigEvent.Unloading`, so they
do not read cleared values during shutdown. All 78 required tests pass and the server exits cleanly.

### Intended files

- None until Stage 7 is explicitly delegated.

### Verification command

User-verified Stage 6 gate:
`.\gradlew.bat :neoforge:runGameTestServer --console=plain`.

### Completion condition

- Met. The suite discovers all 78 tests, every required test passes, no config-unload exception
  occurs, and the server exits cleanly. The user also reports the NeoForge build succeeds without
  error.

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

Stage 6 first normalized the unknown picker-export identifier to a supported translation argument.
The next run discovered all 78 tests and all passed, proving that cause closed, but the known config
unload exception left shutdown unclean. Both NeoForge config adapters were then made unload-safe. The
user reports the subsequent NeoForge build and complete GameTest run both succeed without error.

## Known later-stage failures

Stage 7 artifact verification, invalidated Fabric regressions, documentation reconciliation, manual
check recording, and Forge handoff remain.

## Decisions

- The frozen matrix above is the ownership contract for the NeoForge port and later Forge handoff.
- Architectury 13 remains temporary for NeoForge registration, networking, and the single
  environment lookup; it will not spread into additional common classes.
- Existing `@ExpectPlatform` signatures remain loader-neutral and receive NeoForge implementations
  only in their assigned later stages.
- Architectury 13's NeoForge registrar discovers the mod container's event bus; common registration
  therefore runs inside the NeoForge constructor without the legacy Forge-only `EventBuses` hook.
- NeoForge-native CLIENT and SERVER specs use `somegoogly-client.toml` and
  `somegoogly-server.toml`; SERVER type retains NeoForge's per-world `serverconfig` location.
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
- NeoForge client and server config listeners ignore `ModConfigEvent.Unloading` before reading spec
  values, while retaining their existing load/reload synchronization.
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

## Cumulative gates

| Gate | Status |
| --- | --- |
| Architecture matrix frozen | Yes — 24 imports in 17 files classified |
| `:common:compileJava` | Stage 4 cumulative gate passed |
| `:neoforge:compileJava` | Stage 4 cumulative gate passed with 14 production files |
| `:neoforge:processResources` | Stage 4 passed; output paths and metadata inspected |
| `:neoforge:compileGametestJava` | Stage 5 passed; 78 intended tests in 12 holders |
| `:neoforge:runGameTestServer` | Stage 6 passed; all 78 required tests and clean exit, user verified |
| Invalidated Fabric regressions | Common picker source/test edit requires Stage 7 Fabric gates |
| `:neoforge:build` | User reports success; Stage 7 artifact-path verification remains |

## Blockers

None.

## Exact next action

When explicitly delegated, execute Stage 7: reconcile invalidated gates, verify the NeoForge artifact
by path and metadata, run required Fabric regressions, update orientation documents, record manual
checks, and write the Forge handoff. Do not begin Forge Stage 0 in that session.
