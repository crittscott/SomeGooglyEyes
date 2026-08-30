# NeoForge 1.21.1 Port Status

This is the bounded execution snapshot. Overwrite it in place; never append. Reduced history belongs
in `neoforge-1.21.1-port-log.md`, which is not read during normal execution.

## Current position

| Field | Value |
| --- | --- |
| Overall state | **READY** — Stage 4 complete; Stage 5 is next |
| Current stage | Stage 4 — Resources and production verification — complete |
| Current work unit | None — hard stop after passing Stage 4 gates |
| Work-unit state | Complete |
| Failed verification attempts used | 0 of 3 |
| Stable documents read this session | Yes — `CLAUDE.md`, plan, process, and status reread for Stage 4 |
| Common compile state | Passing — explicit Stage 4 cumulative gate |
| Fabric regression state | Passing at Fabric completion |
| NeoForge compile state | Passing — explicit Stage 4 cumulative gate |
| NeoForge GameTest state | Not configured |
| Forge state | Waiting on the completed NeoForge handoff |
| Last command | `.\gradlew.bat :common:compileJava :neoforge:compileJava :neoforge:processResources --console=plain` — `BUILD SUCCESSFUL` in 8 seconds |

## Current work-unit definition

### Scope and invariant

Stage 4 is complete. NeoForge metadata now declares the fixed loader/platform ranges, Architectury
as the sole required temporary runtime library beyond NeoForge, and GeckoLib 4.7.4 as an optional
client dependency. Source and processed resources have exact path parity without cross-module
duplicates; the Access Transformer is present and byte-identical at its processed standard path.
All 74 vanilla definitions and 171 optional-mod selectors remain unchanged.

### Intended files

- None until Stage 5 is explicitly delegated.

### Verification command

Stage 4 final gate passed:
`.\gradlew.bat :common:compileJava :neoforge:compileJava :neoforge:processResources --console=plain`.

### Completion condition

- Met. Processed metadata contains no unresolved placeholders and has the expected dependency
  ranges/sides. Common has 262 source and 262 processed files; NeoForge has two source and two
  processed files; neither set has missing/unexpected paths and they have zero cross-module path
  duplicates. The processed AT hash matches source. Production compiles and resource processing
  passed on the first attempt.

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
  `neoforge/src/gametest/java` does not yet exist.
- Versions, repositories, module layout, and production source were not changed in Stage 0.

## Last reduced result

Stage 4 passed its combined common compile, NeoForge compile, and NeoForge resource-processing gate
in 8 seconds on its first attempt. Deterministic inspection found exact source/output path parity,
zero duplicated cross-module resource paths, valid JSON throughout, the expected 74 vanilla plus
171 optional definitions, and an unchanged processed Access Transformer.

## Known later-stage failures

Stage 5 NeoForge GameTest configuration and wrappers have not been executed.

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

## Known evidence

- Fabric common, production, resources, 78 GameTests, runtime server, and artifact gates pass.
- `neoforge/build.gradle` targets NeoForge 21.1.248 and Architectury NeoForge 13.0.8.
- `neoforge/src/main/java` contains 14 compiling production files; GameTests remain unconfigured.
- NeoForge metadata exists; its Access Transformer contains the 36 demonstrated renderer/model
  access rules translated from the canonical Access Widener.
- Processed metadata resolves to NeoForge `[21.1.248,22)`, Minecraft `[1.21.1]`, required
  Architectury `[13.0.8,)`, and optional client GeckoLib `[4.7.4,)`, with no placeholders.
- Common resource source/output counts are 262/262 and NeoForge counts are 2/2, with no missing,
  unexpected, or cross-module duplicate paths. All JSON resources parse successfully; pack format
  remains 48.
- Common resources and renderer code already target Minecraft 1.21.1.
- The old Forge source is behavioral reference only and must not be copied mechanically.

## Cumulative gates

| Gate | Status |
| --- | --- |
| Architecture matrix frozen | Yes — 24 imports in 17 files classified |
| `:common:compileJava` | Stage 4 cumulative gate passed |
| `:neoforge:compileJava` | Stage 4 cumulative gate passed with 14 production files |
| `:neoforge:processResources` | Stage 4 passed; output paths and metadata inspected |
| `:neoforge:compileGametestJava` | Not configured/run |
| `:neoforge:runGameTestServer` | Not configured/run |
| Invalidated Fabric regressions | None yet |
| `:neoforge:build` | Not run |

## Blockers

None.

## Exact next action

When explicitly delegated, execute Stage 5: add the NeoForge GameTest source set, expose the 77
shared assertions plus one NeoForge persistence wrapper, verify deterministic discovery metadata,
and require `:neoforge:compileGametestJava` to pass before stopping short of server execution.
