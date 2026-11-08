<!-- FIXED HEADER: not content. Do not edit, trim, or count toward the size limit. -->
> **Orientation snapshot, not a specification.** Describes the code as it currently is; the code wins on any disagreement. See CLAUDE.md § Orientation files.
<!-- END FIXED HEADER -->

# Some Googly Eyes — code orientation

## Project shape

Identity: mod id `somegoogly`, package `com.github.crittscott.somegoogly`, version `0.8.3`, Java 21, Minecraft 1.21.8.

`common` feeds all three loaders.

| Source tree | Responsibility |
| --- | --- |
| `common/src/main/java` | Gameplay, state, codecs, networking, rendering, picker, GeckoLib |
| `common/src/main/resources` | Assets, recipes, eye definitions, language |
| `common/src/gametest` | Shared GameTest assertions and the structure fixture |
| `fabric/src/main` | Fabric entry points, callbacks, configuration, adapters, Mixins, metadata |
| `fabric/src/gametest` | Fabric wrappers and discovery metadata |
| `forge/src/main` | Forge bootstrap, events, native config, adapters, client integration, metadata |
| `forge/src/gametest` | Forge wrappers, persistence proof, dev-mod entry point, discovery metadata |
| `neoforge/src/main` | NeoForge bootstrap, events, native config, adapters, client integration, metadata |
| `neoforge/src/gametest` | NeoForge wrappers, persistence proof, dev-mod entry point, discovery metadata |

Common uses adapters and `@ExpectPlatform`; no loader imports.

The server owns gameplay and authorization; the client owns rendering and picker UI.

`ModContent` binds content through loader `ContentRegistrar`s. Slimy Eye uses vanilla `crafting_transmute`; Optometrist and `self_shear` are data-driven.

## Configuration and eye definitions

`ServerConfig` and `ClientConfig` declare typed settings once for all three loaders. Forge and NeoForge bind them to native CLIENT/SERVER specs. Fabric uses NightConfig at startup and `/reload`, adding only missing keys and keeping the original text in `.bak`. Malformed input leaves effective settings intact. Successful loads log the effective server spawn and client visibility settings.

Eye definitions are datapack resources at `data/<namespace>/eyes/*.json`, modeled by `EyeConfigModel`. Reload resolves one version per entity type, validates, encodes to NBT, and swaps `ServerEyeConfigs` only if the encoding differs, keeping the old set on failure. That encoding is the sync payload, so `ClientEyeConfigs` never selects versions. Size and geometry limits stay aligned across reload, picker export, and network decode, including the total sync size. `EyeColor` codecs reject out-of-range channels on decode. `EyeConfigLimits.Issue` gives translated feedback and log diagnostics.

## Entity and item state

`EyeState` also persists and synchronizes boat eyes without living scheduling or natural rolls. `EyeState.Snapshot` is the unit for entity-state transitions, tracking sync, and packet application. NBT access goes through `EntityPersistentData`: native persistent compounds on NeoForge and Forge, a persistent attachment on Fabric.

Persistent entity keys: `somegoogly:hasGooglyEyes`, `somegoogly:eyeVariantRoll`, `somegoogly:eyeOverrides`.

Mutations send one full snapshot; stored rolls read clamped to 0..1. Eye state and roll initialize together once. Initialization and tracking skip default state; mid-life mutations always send. `EyeState.copy` handles conversion and non-death respawn; joining players get their own snapshot.

Eye items carry `AppearanceOverride` in the synced `somegoogly:eye_properties` component, never geometry; harvesting copies the first eye's effective appearance, the modifier recipe changes only that component, and the Slimy Eye transmute copies every component.

`SkullEyeState` stores placed-head snapshots in the Mixin-added `somegoogly:data` compound, sanitized to the eye snapshot on load and carried by update tags. Head items use the implicit `somegoogly:head_eyes` component. Heads borrow adult definitions, selecting variants with eyes on the head part; modded heads have no mapping. Mutations send block updates.

Saved data from the 1.21.4 release is already in current form; nothing migrates.

`EyeItemService` owns eye application, harvest, and the eligibility verdict shared with the inspector. Loader hooks honor protection listeners and item-use denials. The service excludes spectators, respects PvP and teams for other-player changes, and checks the clicked head for adventure restrictions. Successful use records item statistics and the head-use advancement trigger. Death harvest honors `doMobLoot`. Forge and NeoForge stamp head drops at `LOWEST` and use loot modifiers for placed heads. Fabric stamps drops at `spawnAtLocation` and adds `copy_components` to head loot. Plain-shears self-damage uses the attacker-less `self_shear` type.

Successful Slimy Eye applications emit `GameEvent.ENTITY_INTERACT` (`BLOCK_CHANGE` on a head), successful harvests `GameEvent.SHEAR`; refusals emit neither.

## Eligibility and behaviors

`ServerServices.onLivingEntityLoaded` owns natural eye initialization; later configuration changes never revisit an initialized entity.

`EyeBehavior` catalogs expressions; `OngoingBehavior` catalogs ongoing states. `BehaviorInstance` carries id/duration/seed/elapsed. `ServerBehaviorScheduler` runs watched eyed mobs and damage/heal/trade/milking/shearing/entity-event triggers; sleepers start nothing. `GooglyTracker` applies enabled ongoing states. `HeldEye` runs first-person idle/hurt/heal expressions. `PresentationEyes` caches ambient slots for items, placed heads, and boats. Heads seed by position. Only paired layouts cross-eye. Slots fit the longest eligible expression or the synced mean interval, whichever is greater. Anger syncs on change/new viewer. `ClientEyeRuntime` retains mob reports independently of disposable simulations and ages expressions offscreen. Only recently rendered trackers simulate; idle trackers expire. Heads cache by position and recheck replaced eye data at draw. Frames cache by entity and recheck item contents at extraction. Level changes/disconnect clear all. The master switch pauses mob scheduling while retaining watchers and expiring effects; authoring triggers remain available. Client behaviors start inactive until sync.

## Rendering and attachment

`ClientRenderLayers` installs `LayerGooglyEyes` once per renderer rebuild, before slime's outer layer; GeckoLib uses `CompileRenderLayers`. Both draw picker previews. `LayerGooglyEyes` insets sheared adult sheep `body` eyes by wool depth. Common Mixins store `EyeRenderData` on `LivingEntityRenderState`; GeckoLib captures it during render-state extraction and registers per-bone drawing tasks with state-owned eye data and copied picker drafts. `GooglyEyeRenderer.drawEye` draws every eye, `drawAnimatedEye` simulated ones. `ClientEyeRuntime` owns the first-person `HeldEye` and the shared simulation eviction rule. Rendering and pupil projection share `EyePlacement.orientation`.

Attachment resolvers cache by model identity and replay `ThirdPartyModelWraps`. `RootModelResolver` walks `EntityModel.root()` after reflected Citadel/Uranus/LLibrary resolvers. The picker selects separate baby models by age.

The picker gizmo uses a registered no-depth render pipeline. The picker HUD follows Fabric's subtitles, tops NeoForge's layers, and is appended last to Forge's layered HUD draw. Common Mixins bracket placed-head and head-item rendering and key head items' GUI identity on eyes. `SkullEyeRenderer` draws on the widened `SkullModel`/`PiglinHeadModel` head part after the model. Worn heads get no eyes.

GeckoLib links only after detection; attach failures cannot block loading.

`BoatEyeLayout` selects hull or raft geometry. `ClientEyeRuntime` owns world-clock boat simulations and invalidates them on changed snapshots. Boat Mixins extract a draw and render in the composed hull pose. One `PresentationEyes.Draw` on each item render state carries GUI, hand, or frame animation from extraction to rendering. `GuiItemEyes` owns view keys and a UI clock independent of world pause. `HandItemEyes` keys world-timed state by holder and hand. GUI model identity includes the live simulation. GUI icons keep GUI scale. Loose framed eyes use 3x diameter. World eyes capture gravity in the draw pose and settle when rebuilt.

## Networking

Seven play payloads carry definitions, settings, entity eye state, expressions, anger, picker freeze, and export. Stable IDs live on payload types. Forge and NeoForge require native protocol version `18`. Fabric exchanges the version during configuration.

A joining player receives definitions and settings. After config apply or reload, `ServerServices.broadcastEyeSyncIfChanged` broadcasts each independently when it changes. Only a definition change resets simulations. Behavior triggers carry a compact entity ID and an explicit behavior ID.

Sends go through `@ExpectPlatform` `Networking` and client-only `ClientNetworking`; clientbound handlers reach `ClientNetworkHandler` only inside lambdas, so servers never link it. Serverbound handlers reauthorize the sender; the client rejects exports over vanilla's cap; unknown entity ids are ignored.

## Picker and commands

The client owns picker drafts and previews; the server owns freezing, spawning, movement, and export. `PickerState.DraftSession` retains separate drafts keyed by species and age. Both export paths use `ConfigFile.overlay` to replace authored ages while retaining untouched age buckets, including disabled ones. `ModelPartVocabulary` shares attachment naming between editing and export.

`PickerFreezeService` preserves prior `NoAI`, reconciles locks at load/logout/stop, and allows one editor. `PickerGate` owns authorization (creative plus permission level 2) and throttles; spawn-all also needs server enablement. `PickerSpawnService` uses `MobSpawning` for loader spawn finalization, honors cancellation, then sets `NoAI`, persistence, and rotation. Its `refusal` filters commands and suggestions. Bulk terrain edits follow accepted finalization and insertion. Vanilla inserts the mob and its riders. The group is discarded if any insertion is refused. World export writes the generated datapack, export-all the game-directory export tree. Both use `ConfigFile.exportVersion`; bulk-export disk failures log the entity, target, completed count, and exception.

`LookTarget` limits entity intersections by the nearest blocking surface; the inspector compares actual hit distances. Client and server own disjoint `/sg` branches: editing is client-side; admin, spawn, spawn-all, and mob-pose are server-side. Fabric forwards those server branches explicitly, since its client root would capture them.

## Loader integration

Fabric Mixins cover heal reactions, trades, and shears-kill and head drops (no Fabric API events). Common Mixins (`somegoogly-common.mixins.json`) report milking, sheep shearing, and entity events and hold head eyes. Mixins target the pinned Minecraft version, with fixed refmaps.

NeoForge registers from the `@Mod` constructor, client listeners on the right bus.

Forge's required `PayloadChannel` marks payloads handled. NeoForge and Forge isolate physical-client bootstrap from dedicated-server bootstrap.

## Automated verification

GameTest scenarios: Fabric 186, Forge 189, NeoForge 189. Item coverage: twelve frame, seven GUI, seven hand, and thirteen boat scenarios per loader. Client checklists: `docs/framed-eyes-testing.md`, `docs/inventory-eyes-testing.md`, `docs/third-person-eyes-testing.md`, and `docs/boat-eyes-testing.md`. Visuals, connections, protection ordering, and real saves need manual checks.

## Operational boundaries

- Optional renderer integrations log failures once.
- `build-env/` mirrors build inputs as described in `orientation-build.md`; keep covered files synchronized.
