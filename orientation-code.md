<!-- FIXED HEADER: not content. Do not edit, trim, or count toward the size limit. -->
> **Orientation snapshot, not a specification.** Describes the code as it currently is; the code wins on any disagreement. See CLAUDE.md § Orientation files.
<!-- END FIXED HEADER -->

# Some Googly Eyes — code orientation

Structure, ownership, state, loader seams, and invariants; `orientation-player.md` covers behavior.

## Project shape

Identity: mod id `somegoogly`, package `com.github.crittscott.somegoogly`, version `0.8.3`, Java 21, Minecraft 1.21.4.

`common` is transformed into all three loader artifacts.

| Source tree | Responsibility |
| --- | --- |
| `common/src/main/java` | Shared gameplay, state, codecs, services, networking, rendering, picker, GeckoLib integration |
| `common/src/main/resources` | Assets, recipes, eye definitions, language |
| `common/src/gametest` | Shared GameTest assertions and the structure fixture |
| `fabric/src/main` | Fabric entry points, callbacks, configuration, adapters, Mixins, metadata |
| `fabric/src/gametest` | Fabric wrappers and discovery metadata |
| `forge/src/main` | Forge bootstrap, events, native config, adapters, client integration, metadata |
| `forge/src/gametest` | Forge wrappers, persistence proof, dev-mod entry point, discovery metadata |
| `neoforge/src/main` | NeoForge bootstrap, events, native config, adapters, client integration, metadata |
| `neoforge/src/gametest` | NeoForge wrappers, persistence proof, dev-mod entry point, discovery metadata |

Common imports no loader types (adapters or `@ExpectPlatform`); loader packages avoid split packages.

The server owns gameplay, definitions, and authorization; the client owns rendering, inspection, and picker UI.

`ModContent` binds content through loader `ContentRegistrar`s. Slimy Eye uses vanilla `crafting_transmute`; Optometrist and `self_shear` are data-driven.

## Configuration and eye definitions

`ServerConfig`/`ClientConfig` expose validated `ConfigValue<T>`s. Forge and NeoForge use native CLIENT/SERVER specs, resetting SERVER values on unload. Fabric uses bundled NightConfig for table-aware TOML parsing at startup and `/reload`, resetting on stop. `FabricConfigMigration` quotes the released unquoted header. Completion adds only missing keys and retains original text in `.bak`; parse failures preserve the file and runtime values. The server logs the effective master switch at startup and changes.

Server config keys have the same order across loaders.

Eye definitions are datapack resources at `data/<namespace>/eyes/*.json`, modeled by `EyeConfigModel`. Reload resolves one version per entity type, validates, encodes to NBT, and swaps `ServerEyeConfigs` only if the encoding differs, keeping the old set on failure. That encoding is the sync payload, so `ClientEyeConfigs` never selects versions. Size and geometry limits stay aligned across reload (which also caps payload bytes), picker export, and network decode. `EyeColor` codecs reject out-of-range channels on decode. `EyeConfigLimits.Issue` gives translated feedback and log diagnostics.

## Entity and item state

`EyeState.Snapshot` is the unit for entity-state transitions, tracking sync, and packet application. NBT access goes through `EntityPersistentData`: native persistent compounds on NeoForge and Forge, a persistent attachment on Fabric. On load, `FabricEntityDataMigration` moves the 1.21.1 release's `somegoogly:persistentData` compound into it; a non-compound value is set aside there and logged.

Persistent entity keys: `somegoogly:hasGooglyEyes`, `somegoogly:eyeVariantRoll`, `somegoogly:eyeOverrides`.

Mutations send one full snapshot; stored rolls read clamped to 0..1. Eye state and roll initialize together once. Initialization and tracking skip default state; mid-life mutations always send. `EyeState.copy` handles conversion and non-death respawn; joining players get their own snapshot.

Eye items carry `AppearanceOverride` in the synced `somegoogly:eye_properties` component, never geometry; harvesting copies the first eye's effective appearance, the modifier recipe changes only that component, and the Slimy Eye transmute copies every component.

`SkullEyeState` stores placed-head snapshots in the Mixin-added `somegoogly:data` compound, replaced on load and carried by update tags. Head items use the implicit `somegoogly:head_eyes` component. Heads borrow adult definitions, selecting variants with eyes on the head part; modded heads have no mapping. Mutations send block updates.

Only Fabric entity data needs migration from the 1.21.1 release.

`EyeItemService` owns authorization, mutation, drops, and durability for Slimy Eye use and both harvests, on mobs and heads. Loader adapters run entity interaction, block use, and self-shearing after protection listeners (Forge/NeoForge `LOWEST`, Fabric a late phase); head use also checks `mayInteract`/`mayUseItemAt`, and Forge/NeoForge honor separate item-use denials. `markHeadDrop` stamps an eyed mob's own head among its death drops (Forge/NeoForge at `LOWEST`); head loot copies the state to the item (Forge/NeoForge `HeadEyesLootModifier` delegates to `SkullEyeState.copyToLoot`, with native data in each loader; Fabric `LootTableEvents.MODIFY` adding `copy_components`). Fabric's adapter refuses spectators. Shears are `#somegoogly:shears` (vanilla plus optional `#c:tools/shear`). Applying a Slimy Eye to another player also requires server PvP and `canHarmPlayer`. Plain-shears self-damage uses the attacker-less `self_shear` type.

Successful Slimy Eye applications emit `GameEvent.ENTITY_INTERACT` (`BLOCK_CHANGE` on a head), successful harvests `GameEvent.SHEAR`; refusals emit neither.

## Eligibility and behaviors

`ServerServices.onLivingEntityLoaded` owns natural eye initialization; later configuration changes never revisit an initialized entity.

`EyeBehavior` catalogs expressions; `OngoingBehavior` catalogs ongoing states. `BehaviorInstance` carries id/duration/seed/elapsed. `ServerBehaviorScheduler` runs watched eyed mobs and damage/heal/trade/milking/shearing/entity-event triggers; sleepers start nothing. `GooglyTracker` applies enabled ongoing states. `HeldEye` runs first-person idle/hurt/heal expressions. `SkullEyes` seeds ambient slots by position; `ItemEyes` uses seeded time slots for loose eyes or head layouts; only paired layouts cross-eye. Frame slots last the synced mean interval or longest eligible expression, whichever is greater. Anger syncs on change/new viewer. `ClientEyeRuntime` retains mob reports independently of disposable simulations and ages expressions offscreen. Only recently rendered trackers simulate; idle trackers expire. Heads cache by position, frames by entity; removed/changed frame contents invalidate them. Level changes/disconnect clear all. The master switch pauses mob scheduling while retaining watchers and expiring effects; authoring triggers remain available. Client behaviors start inactive until sync.

## Rendering and attachment

`ClientRenderLayers` installs `LayerGooglyEyes` once per renderer rebuild, before slime's outer layer; GeckoLib uses `CompileRenderLayers`. Both draw picker previews. `LayerGooglyEyes` insets sheared sheep `body` eyes by wool depth. Common Mixins store `EyeRenderData` on `LivingEntityRenderState`; GeckoLib captures it in `preRender`. `GooglyEyeRenderer.drawEye` draws every eye, `drawAnimatedEye` simulated ones. `ClientLifecycle` ticks the first-person `HeldEye`. `EyePlacement.orientation` serves rendering and pupils.

Attachment resolvers cache by model identity and replay `ThirdPartyModelWraps`. `RootModelResolver` walks `EntityModel.root()` after reflected Citadel/Uranus/LLibrary resolvers. The picker selects separate baby models by age.

Common Mixins bracket placed-head and head-item rendering. `SkullEyeRenderer` draws on the widened `SkullModel`/`PiglinHeadModel` head part after the model. Worn heads get no eyes.

Optional GeckoLib code is linked only after detection; attach failures cannot block loading.

`ItemEyes` supplies frame/GUI/hand layouts and ambient expressions. `HandEyeAnimations` keys world-timed state by holder/hand; `HandItemEyes` scopes draws and item states retain it. `GuiEyeAnimations` owns view keys and UI time; `GuiItemEyes` scopes extraction/draw context. GUI Mixins select slots/cursor; item states retain context and animated cache identity. GUI icons keep GUI scale; loose framed eyes use 3x diameter. No packets/saved fields.

## Networking

Six payloads carry definitions, entity eye state, expressions, anger, picker freeze, and export. `NetworkHandler` ids embed version `17`; bump on wire changes. Native loaders require that channel version; Fabric requires its `Handshake`.

The eye-definition packet also carries `googlyEyesEnabled` and `BehaviorSettings`; reload size validation reserves the largest supported behavior block, including settings changed later. Joins always receive it; after each datapack reload and Forge/NeoForge server-config apply, `ServerServices.broadcastEyeConfigsIfChanged` resends it only if the set, switch, or behavior settings changed, since it resets client trackers.

Sends go through `@ExpectPlatform` `Networking` and client-only `ClientNetworking`; clientbound handlers reach `ClientNetworkHandler` only inside lambdas, so servers never link it. Serverbound handlers reauthorize the sender; the client rejects exports over vanilla's cap; unknown entity ids are ignored.

## Picker and commands

The client owns picker drafts and previews; the server owns freezing, spawning, movement, and export. `PickerState.DraftSession` retains separate drafts keyed by species and age. Both export paths use `ConfigFile.overlay` to replace authored ages while retaining untouched age buckets, including disabled ones. `ModelPartVocabulary` shares attachment naming between editing and export.

`PickerFreezeService` preserves prior `NoAI`, reconciles locks at load/logout/stop, and allows one editor. `PickerGate` owns authorization (creative plus permission level 2) and throttles; spawn-all also needs server enablement. `PickerSpawnService` uses `MobSpawning` for loader spawn finalization, honors cancellation, then sets `NoAI`, persistence, and rotation. Its `refusal` filters commands and suggestions. Bulk terrain edits follow accepted finalization and insertion. Each mob/rider insertion result is checked; partial additions are removed on refusal. World export writes the generated datapack, export-all the game-directory export tree. Both use `ConfigFile.exportVersion`; bulk-export disk failures log the entity, target, completed count, and exception.

`LookTarget` limits entity intersections by the nearest blocking surface; the inspector compares actual hit distances. Client and server own disjoint `/sg` branches: editing is client-side; admin, spawn, spawn-all, and mob-pose are server-side. Fabric forwards those server branches explicitly, since its client root would capture them.

## Loader integration

Fabric Mixins cover persistent-data migration, heal reactions, trades, and shears-kill and head drops (no Fabric API events). Common Mixins (`somegoogly-common-server.mixins.json`) report milking, sheep shearing, and entity events and hold head eyes. Mixins target the pinned Minecraft version, with fixed refmaps.

NeoForge registers from the `@Mod` constructor, client listeners on the right bus.

Forge's required `PayloadChannel` marks payloads handled. NeoForge and Forge isolate physical-client bootstrap from dedicated-server bootstrap.

## Automated verification

GameTest scenarios: Fabric 156, Forge 154, NeoForge 154. Shared item coverage includes twelve frame, seven GUI, and five hand scenarios. Client checklists: `docs/framed-eyes-testing.md`, `docs/inventory-eyes-testing.md`, and `docs/third-person-eyes-testing.md`. Visuals, connections, protection ordering, command UX, and real saves need manual checks.

## Operational boundaries

- Optional renderer integrations log failures once.
- `build-env/` mirrors build inputs as described in `orientation-build.md`; keep covered files synchronized.
