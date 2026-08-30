# NeoForge 1.21.1 Port Status

This is the bounded execution snapshot. Overwrite it in place; never append. Reduced history belongs
in `neoforge-1.21.1-port-log.md`, which is not read during normal execution.

## Current position

| Field | Value |
| --- | --- |
| Overall state | **READY** — Stage 1 complete; Stage 2 is next |
| Current stage | Stage 1 — Bootstrap, registration, configuration, and platform services — **COMPLETE** |
| Current work unit | Stage 1 cumulative loader foundation |
| Work-unit state | Complete |
| Failed verification attempts used | 0 of 3 |
| Stable documents read this session | Yes — `CLAUDE.md`, plan, process, and status reread for Stage 1 |
| Common compile state | Up to date during the passing Stage 1 diagnostic |
| Fabric regression state | Passing at Fabric completion |
| NeoForge compile state | Passing with seven Stage 1 production files |
| NeoForge GameTest state | Not configured |
| Forge state | Waiting on the completed NeoForge handoff |
| Last command | `.\gradlew.bat :neoforge:compileJava --console=plain` — `BUILD SUCCESSFUL` in 12 seconds |

## Current work-unit definition

### Scope and invariant

Provide the complete Stage 1 NeoForge loader foundation: one physical-server-safe bootstrap, common
registration exactly once, server datapack reload registration, native client and per-world server
configuration, and the four foundational loader-neutral platform implementations. Stage 2 gameplay
events and runtime persistence/network proof and Stage 3 client rendering remain out of scope.

### Intended files

- `neoforge/src/main/java/com/github/crittscott/somegoogly/neoforge/SomeGooglyNeoForge.java`
- `neoforge/src/main/java/com/github/crittscott/somegoogly/config/neoforge/NeoForgeClientConfig.java`
- `neoforge/src/main/java/com/github/crittscott/somegoogly/config/neoforge/NeoForgeServerConfig.java`
- `neoforge/src/main/java/com/github/crittscott/somegoogly/config/neoforge/ModVersionLookupImpl.java`
- `neoforge/src/main/java/com/github/crittscott/somegoogly/item/neoforge/GooglyEyeItemFactoryImpl.java`
- `neoforge/src/main/java/com/github/crittscott/somegoogly/platform/neoforge/EntityPersistentDataImpl.java`
- `neoforge/src/main/java/com/github/crittscott/somegoogly/platform/neoforge/NetworkTrackingImpl.java`
- `neoforge-1.21.1-port-status.md`
- `neoforge-1.21.1-port-log.md` after verification
- `neoforge-1.21.1-port-status.md`
- `neoforge-1.21.1-port-log.md` after Stage 1 verification

### Verification command

Targeted source inspection followed by `.\gradlew.bat :neoforge:compileJava --console=plain`.

### Completion condition

- All seven Stage 1 production files compile with no current-stage diagnostic.
- Bootstrap, common registration, reload registration, native configuration, and all four platform
  implementations are present without client rendering or Stage 2 gameplay event code.
- Remaining implementation work is classified only into later stages.

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
  optional GeckoLib metadata remains a Stage 4 resource concern.
- `META-INF/accesstransformer.cfg` is intentionally empty pending demonstrated Stage 3 renderer needs.
- `neoforge/src/main/java` and `neoforge/src/gametest/java` do not exist; the module currently has no
  Java implementation or tests.
- Versions, repositories, module layout, and production source were not changed in Stage 0.

## Last reduced result

The first post-edit Stage 1 diagnostic succeeded in 12 seconds. Common compile, resources, classes,
and JAR were up to date; `:neoforge:compileJava` executed and passed. No compiler diagnostics remain,
so failed verification attempts remain 0 of 3.

## Known later-stage failures

No later-stage compiler failure was exposed. Stage 2 still owns server lifecycle/gameplay event
adapters, runtime persistence proof, payload lifecycle, and tracking synchronization. Stage 3 still
owns all client initialization, item rendering, renderer access/layers, picker UI, Access
Transformer rules, and optional GeckoLib integration.

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
- The item factory deliberately creates the shared item without a client renderer until Stage 3.
- Native entity persistent data and packet tracking fanout compile; their runtime behavior remains a
  Stage 2 verification responsibility.
- No common, Fabric, build, metadata, dependency, version, repository, protocol, persistence-key,
  item-component, or data-format change was made in Stage 1.

## Known evidence

- Fabric common, production, resources, 78 GameTests, runtime server, and artifact gates pass.
- `neoforge/build.gradle` targets NeoForge 21.1.248 and Architectury NeoForge 13.0.8.
- `neoforge/src/main/java` and `neoforge/src/gametest/java` contain no implementation.
- NeoForge metadata exists; its Access Transformer is empty.
- Common resources and renderer code already target Minecraft 1.21.1.
- The old Forge source is behavioral reference only and must not be copied mechanically.

## Cumulative gates

| Gate | Status |
| --- | --- |
| Architecture matrix frozen | Yes — 24 imports in 17 files classified |
| `:common:compileJava` | Up to date as a dependency of the Stage 0 diagnostic |
| `:neoforge:compileJava` | Stage 1 passed with seven production files |
| `:neoforge:processResources` | Not run |
| `:neoforge:compileGametestJava` | Not configured/run |
| `:neoforge:runGameTestServer` | Not configured/run |
| Invalidated Fabric regressions | None yet |
| `:neoforge:build` | Not run |

## Blockers

None.

## Exact next action

In a new execution session, reread the controlling documents and begin Stage 2 — server runtime and
networking — by bounding its first coherent work unit.
