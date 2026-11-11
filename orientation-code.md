<!-- FIXED HEADER: not content. Do not edit, trim, or count toward the size limit. -->
> **Orientation snapshot, not a specification.** Describes the code as it currently is; the code wins on any disagreement. See CLAUDE.md § Orientation files.
<!-- END FIXED HEADER -->

# Some Googly Eyes — code orientation

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

`ServerConfig`/`ClientConfig` expose validated `ConfigValue<T>`s. Forge and NeoForge use native CLIENT/SERVER specs, resetting SERVER values on unload. Fabric uses bundled NightConfig for table-aware TOML parsing at startup and `/reload`, resetting on stop. `FabricMigration` quotes the released unquoted header. Completion adds only missing keys and retains original text in `.bak`; parse failures preserve the file and runtime values. The server logs the effective master switch at startup and changes.

Server config keys share one order across loaders.

Eye definitions are datapack resources at `data/<namespace>/eyes/*.json`, modeled by `EyeConfigModel`. Reload resolves one version per entity type, validates, encodes to NBT, and swaps `ServerEyeConfigs` only if the encoding differs, keeping the old set on failure. That encoding is the sync payload, so `ClientEyeConfigs` never selects versions. Size, payload, and geometry limits match across reload, picker export, and network decode. `EyeColor` codecs reject out-of-range channels on decode. `EyeConfigLimits.Issue` gives translated feedback and log diagnostics.

## Entity and item state

`EyeState.Snapshot` is the unit for entity-state transitions, tracking sync, and packet application. NBT access goes through `EntityPersistentData`: native persistent compounds on NeoForge and Forge, a persistent attachment on Fabric. On load, `FabricMigration` moves the 1.21.1 release's `somegoogly:persistentData` compound into it; a non-compound value is set aside there and logged. Boat tracking and item actions use the same snapshots, without natural rolls or living scheduling.

Persistent entity keys: `somegoogly:hasGooglyEyes`, `somegoogly:eyeVariantRoll`, `somegoogly:eyeOverrides`.

Mutations send one full snapshot; rolls, kept only while eyes are on, read clamped to 0..1. Eye state initializes once. Initialization and tracking skip default state; mid-life mutations always send. Conversions call `ServerServices.onConverted` (eyes, picker freeze); player replacement calls `onPlayerCloned` (eyes on End exit only). Joining players get their own snapshot.

Eye items carry `AppearanceOverride` in the synced `somegoogly:eye_properties` component, never geometry; harvesting copies the first eye's effective appearance, the modifier recipe changes only that component, and the Slimy Eye transmute copies every component.

`SkullEyeState` stores placed-head snapshots in the Mixin-added `somegoogly:data` compound, replaced on load and carried by update tags. Head items use the implicit `somegoogly:head_eyes` component. Heads borrow adult definitions, selecting variants with eyes on the head part; modded heads have no mapping. Mutations send block updates.

Only Fabric entity data needs migration from the 1.21.1 release.

`EyeItemService` owns authorization, mutation, drops, and durability for Slimy Eye use and both harvests, on mobs and heads. Loader adapters run entity interaction, block use, and self-shearing after protection listeners (Forge/NeoForge `LOWEST`, Fabric a late phase); head use also checks `mayInteract`/`mayUseItemAt`, and Forge/NeoForge honor separate item-use denials. `markHeadDrop` stamps an eyed mob's own head among its death drops (Forge/NeoForge at `LOWEST`); head loot copies the state to the item (Forge/NeoForge `HeadEyesLootModifier` delegates to `SkullEyeState.copyToLoot`, with native data in each loader; Fabric `LootTableEvents.MODIFY` adding `copy_components`). It refuses spectators itself. Shears are `#somegoogly:shears` (vanilla plus optional `#c:tools/shear`). Applying a Slimy Eye to another player also requires server PvP and `canHarmPlayer`. Plain-shears self-damage uses the attacker-less `self_shear` type.

Slimy Eye applications emit `GameEvent.ENTITY_INTERACT` (`BLOCK_CHANGE` on a head), harvests `GameEvent.SHEAR`; refusals emit neither.

## Eligibility and behaviors

`ServerServices.onLivingEntityLoaded` owns natural eye initialization; config changes never revisit an initialized entity.

`EyeBehavior` catalogs expressions; `OngoingBehavior` catalogs ongoing states. `BehaviorInstance` carries id/duration/seed/elapsed. `ServerBehaviorScheduler` runs watched eyed mobs (players watch themselves) and damage/heal/trade/milking/shearing/entity-event triggers; sleepers start nothing. `GooglyTracker` applies enabled ongoing states. `HeldEye` runs first-person idle/hurt/heal expressions. `PresentationEyes` caches ambient slots for items, placed heads, and boats. Heads seed by position. Only paired layouts cross-eye. Slots fit the longest eligible expression or the synced mean interval, whichever is greater. Anger syncs on change/new viewer. `ClientEyeRuntime` retains mob reports apart from disposable simulations and ages expressions offscreen. Only recently rendered trackers simulate; idle trackers expire. Heads cache by position, frames by entity. Level changes/disconnect clear all. The master switch pauses mob scheduling while retaining watchers and expiring effects; authoring triggers remain available. Client behaviors start inactive until sync.

## Rendering and attachment

`ClientRenderLayers` installs `LayerGooglyEyes` once per renderer rebuild, before slime's outer layer; GeckoLib uses `CompileRenderLayers`. Both draw picker previews. `LayerGooglyEyes` insets sheared sheep `body` eyes by wool depth. Common Mixins store `EyeRenderData` on `LivingEntityRenderState`; GeckoLib captures it in `preRender`. A `PresentationEyes.Draw` carries an animation from extraction to draw on `ItemStackRenderState` and `BoatRenderState`. `GooglyEyeRenderer.drawEye` draws every eye, `drawAnimatedEye` simulated ones. `ClientEyeRuntime` owns and ticks the first-person `HeldEye`. `EyePlacement.orientation` serves rendering and pupils.

Attachment resolvers cache by model identity and replay `ThirdPartyModelWraps`. `RootModelResolver` walks `EntityModel.root()` after reflected Citadel/Uranus/LLibrary resolvers. The picker selects separate baby models by age.

Common Mixins bracket placed-head and head-item rendering. `SkullEyeRenderer` draws on the widened `SkullModel`/`PiglinHeadModel` head part after the model. Worn heads get no eyes.

GeckoLib code links only after detection; attach failures cannot block loading.

`BoatEyeLayout` selects geometry by vanilla boat/raft entity type. `ClientEyeRuntime.BoatSimulations` owns world-clock simulations. `BoatRendererMixin` extracts eyes onto the state and `GooglyEyeRenderer.renderBoat` draws them before the hull pose is popped. `PresentationEyes` supplies layouts, ambient expressions, and owner motion. `EyeInfo.capturePose` projects gravity/acceleration from world-aligned poses, once a tick. `HandItemEyes` keys world-timed state by holder/hand; `GuiItemEyes` owns view keys and UI time. `ItemModelResolverMixin` picks an item's draw, frame extraction sets one on `state.item`, and `ItemStackRenderStateMixin` scopes it in `ClientEyeRuntime`. GUI icons keep GUI scale; loose framed eyes use 3x diameter. No packets/saved fields.

## Networking

Seven payloads carry definitions, settings, entity eye state, expressions, anger, picker freeze, and export. `NetworkProtocol` holds version `18`; bump on wire changes. Native loaders require that channel version; Fabric checks it in a configuration task (`Handshake`/`HandshakeReply`).

`EyeSettingsPacket` carries `googlyEyesEnabled`/`BehaviorSettings`. Joins receive both; the definitions packet is built once per set. Reload/config apply calls `broadcastEyeSyncIfChanged`, sending each only when it changed; only new definitions restart client motion.

`@ExpectPlatform` networking bridges send payloads; clientbound lambdas isolate client handlers. Serverbound requests recheck authorization; exports obey the payload cap. Entity eye snapshots follow entity spawn.

## Picker and commands

The client owns picker drafts and previews; the server owns freezing, spawning, movement, and export. `PickerState.DraftSession` retains separate drafts keyed by species and age. Both export paths use `ConfigFile.overlay` to replace authored ages while retaining untouched age buckets, including disabled ones. `ModelPartVocabulary` shares attachment naming between editing and export.

`PickerFreezeService` preserves prior `NoAI`, reconciles locks at load/logout/stop, and allows one editor. `PickerGate` owns authorization (creative plus permission level 2) and throttles; spawn-all also needs server enablement. `PickerSpawnService` uses `MobSpawning` for loader spawn finalization, honors cancellation, then sets `NoAI`, persistence, and rotation. Its `refusal` filters commands and suggestions. Individual insertion results decide acceptance; incomplete groups roll back in reverse order, and terrain follows full acceptance. World export writes the generated datapack, export-all the game-directory export tree. Both use `ConfigFile.exportVersion`; bulk-export disk failures log the entity, target, completed count, and exception.

`LookTarget` limits entity intersections by the nearest blocking surface; the inspector compares actual hit distances. Client and server own disjoint `/sg` branches: editing is client-side; admin, spawn, spawn-all, and mob-pose are server-side. Fabric forwards `SERVER_BRANCHES` explicitly, since its client root would capture them.

## Loader integration

Fabric Mixins cover persistent-data migration, heal reactions, trades, and shears-kill and head drops (no Fabric API events). Common Mixins (`somegoogly-common.mixins.json`) report milking, sheep shearing, and entity events and hold head eyes. Mixins target the pinned Minecraft version, with fixed refmaps.

NeoForge registers from the `@Mod` constructor, client listeners on the right bus.

Forge's required `PayloadChannel` marks payloads handled. NeoForge and Forge isolate physical-client bootstrap from dedicated-server bootstrap.

## Automated verification

GameTest scenarios: Fabric 189, Forge 190, NeoForge 190. Item coverage: twelve frame, seven GUI, seven hand, and thirteen boat scenarios per loader. Client checklists: `docs/framed-eyes-testing.md`, `docs/inventory-eyes-testing.md`, `docs/third-person-eyes-testing.md`, and `docs/boat-eyes-testing.md`. Visuals, connections, protection ordering, command UX, and real saves need manual checks.

## Operational boundaries

- `build-env/` mirrors build inputs as described in `orientation-build.md`; keep covered files synchronized.
