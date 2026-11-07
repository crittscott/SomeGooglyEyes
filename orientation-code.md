<!-- FIXED HEADER: not content. Do not edit, trim, or count toward the size limit. -->
> **Orientation snapshot, not a specification.** Describes the code as it currently is; the code wins on any disagreement. See CLAUDE.md § Orientation files.
<!-- END FIXED HEADER -->

# Some Googly Eyes — code orientation

## Project shape

Identity: mod id `somegoogly`, package `com.github.crittscott.somegoogly`, version `0.8.3`, Java 25, Minecraft 26.1.2.

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

`@ExpectPlatform` supplies loader adapters.

`ModContent` binds content through loader `ContentRegistrar`s. Slimy Eye uses vanilla `crafting_transmute`; Optometrist and `self_shear` are data-driven.

## Configuration and eye definitions

`ServerConfig`/`ClientConfig` expose validated `ConfigValue<T>`s. Forge and NeoForge use native CLIENT/SERVER specs, resetting SERVER values on unload. Fabric uses bundled NightConfig for table-aware TOML parsing at startup and `/reload`, resetting on stop. Completion adds only missing keys and retains original text in `.bak`; parse failures preserve the file and runtime values. The server logs the effective master switch at startup and changes.

A single typed schema defines the released keys, table order, defaults, and bounds for both native specs and Fabric TOML. Apply logs summarize spawn and visibility settings.

Eye definitions are datapack resources at `data/<namespace>/eyes/*.json`, modeled by `EyeConfigModel`. Reload resolves one version per entity type, validates, encodes to NBT, and swaps `ServerEyeConfigs` only if the encoding differs, keeping the old set on failure. That encoding is the sync payload, so `ClientEyeConfigs` never selects versions. Size and geometry limits stay aligned across reload (which also caps payload bytes), picker export, and network decode. `EyeColor` codecs reject out-of-range channels on decode. `EyeConfigLimits.Issue` gives translated feedback and log diagnostics.

## Entity and item state

`EyeState` also persists and synchronizes boat eyes without living scheduling or natural rolls. `EyeState.Snapshot` is the unit for entity-state transitions, tracking sync, and packet application. Reads through `EntityPersistentData` leave absent data absent: native persistent compounds on NeoForge and Forge, a persistent attachment on Fabric.

Persistent entity keys: `somegoogly:hasGooglyEyes`, `somegoogly:eyeVariantRoll`, `somegoogly:eyeOverrides`.

Mutations send one full snapshot; stored rolls read clamped to 0..1. Eye state and roll initialize together once. Initialization and tracking skip default state; mid-life mutations always send. `EyeState.copy` handles conversion and non-death respawn; joining players get their own snapshot.

Eye items carry `AppearanceOverride` in the synced `somegoogly:eye_properties` component, never geometry; harvesting copies the first eye's effective appearance, the modifier recipe changes only that component, and the Slimy Eye transmute copies every component.

Placed heads save and sync their eye snapshot under the Mixin-owned `somegoogly:data` key, normalized to an eye-state snapshot on load. Items use the `somegoogly:head_eyes` component. Optional NeoForge mixins carry eyes through Twilight Forest skull candle conversion and give trophies the same storage, item, and render lifecycle as skulls. Hydra uses its own trophy layout.

Saved data from the 26.1.1 release is already in current form; nothing migrates.

`EyeItemService` owns authorization, mutation, drops, and durability for Slimy Eye use and both harvests, on mobs and heads. Loader adapters run entity interaction, block use, and self-shearing after protection listeners (Forge/NeoForge `LOWEST`, Fabric a late phase); head use also checks `mayInteract`/`mayUseItemAt`, and Forge/NeoForge honor separate item-use denials. `markHeadDrop` stamps an eyed mob's own head among its death drops (Forge/NeoForge at `LOWEST`); head loot copies the state to the item (Forge/NeoForge `HeadEyesLootModifier` delegates to `SkullEyeState.copyToLoot`, with native data in each loader; Fabric `LootTableEvents.MODIFY` adding `copy_components`). The shared use entry points refuse spectators. Shears are `#somegoogly:shears` (vanilla plus optional `#c:tools/shear`). Applying or harvesting another player’s eyes requires level PvP and `canHarmPlayer`. Head use checks the clicked block in Adventure mode and records its use criterion and statistic before spending the item. Harvest kills honor the mob-drops rule. NeoForge reports shears breaks explicitly. Plain-shears self-damage uses the attacker-less `self_shear` type.

Successful Slimy Eye applications emit `GameEvent.ENTITY_INTERACT` (`BLOCK_CHANGE` on a head), successful harvests `GameEvent.SHEAR`; refusals emit neither.

## Eligibility and behaviors

`ServerServices.onLivingEntityLoaded` owns natural eye initialization; later configuration changes never revisit an initialized entity.

`EyeBehavior` catalogs expressions; `OngoingBehavior` catalogs ongoing states. `BehaviorInstance` carries id/duration/seed/elapsed. `ServerBehaviorScheduler` runs watched eyed mobs and damage/heal/trade/milking/shearing/entity-event triggers; sleepers start nothing. `GooglyTracker` applies enabled ongoing states. `HeldEye` runs first-person idle/hurt/heal expressions. `PresentationEyes` caches ambient slots for items, placed heads, and boats. Heads seed by position. Only paired layouts cross-eye. Slots fit the longest eligible expression or the synced mean interval, whichever is greater. Anger syncs on changes, with an initial packet only for angry entities. `ClientEyeRuntime` retains mob reports independently of disposable simulations and ages expressions offscreen. Only recently rendered trackers simulate; idle trackers expire. Placed heads check compound identity at extraction and frames check item components at extraction. Boat snapshot changes evict the affected simulation. Level changes/disconnect clear all. The master switch pauses mob scheduling while retaining watchers and expiring effects; authoring triggers remain available. Client behaviors start inactive until sync.

## Rendering and attachment

Each eye part is submitted as custom geometry at its finished pose, values copied into its callback; `ModelGooglyEye` is stateless. `ClientRenderLayers` installs `LayerGooglyEyes` once per renderer rebuild, before slime's outer layer; GeckoLib uses `CompileRenderLayers` on every loader. Both draw picker previews. `LayerGooglyEyes` poses its parent model before resolving and insets sheared sheep `body` eyes by wool depth. Common Mixins store `EyeRenderData` on `LivingEntityRenderState`; GeckoLib captures it at extraction and registers per-bone `RenderPassInfo` tasks with state-owned eye data and copied picker drafts; bone tasks deduplicate by identity. `GooglyEyeRenderer` shares the head and preview submission loops. `ClientEyeRuntime` scopes a presentation through item submission and owns the first-person `HeldEye`. `SpecialModelWrapperMixin` captures the display context in the item renderer's `Frame` argument, because `submit` receives none. Rendering and pupils share `EyePlacement.orientation`.

Attachment resolvers cache by model identity and replay `ThirdPartyModelWraps`. `RootModelResolver` walks `EntityModel.root()` after reflected Citadel/Uranus/LLibrary resolvers.

The picker gizmo uses a no-depth pipeline with per-vertex width. The HUD follows subtitles on Fabric and is added last on Forge and NeoForge. Placed-head eyes are chosen at extraction onto `SkullBlockRenderState`; Mixins bracket placed-head and head-item submission and key animated GUI items on their simulation and head items on their snapshot. The skull hook shares the submitted model state and pose. `SkullEyeRenderer` poses vanilla and Heads models using the submitted state. Heads placements use cached cube fits; item eyes submit once across face/wool passes. Fits clear on renderer rebuild. Worn heads get no eyes.

GeckoLib links only after detection; attach failures cannot block loading.

`BoatEyeLayout` selects hull and raft geometry by entity type. Boat Mixins extract a draw and submit eyes in the composed hull pose. Item model extraction chooses a GUI or third-person hand draw. Frame extraction puts its draw on the contained item state. GUI pupils follow UI time, and world pupils settle toward gravity at their first valid pose.

## Networking

Seven play payloads carry definitions, settings, entity eye state, expressions, anger, picker freeze, and export. Payload types own stable IDs. `NetworkProtocol` declares version 18. Fabric negotiates a version during configuration, and the native loaders require it on their channels.

The definition and settings packets sync separately. Joining players receive both with datapack sync. Reloads and config applies broadcast only changed data. Definitions reset the simulations, while settings update the switches and behavior rules those simulations use.

Sends go through the platform bridges. Clientbound registrations link the client handler from lambda bodies. Serverbound picker handlers authorize the sender. Decoders and receivers validate input before installation or use.

## Picker and commands

`PickerState.DraftSession` retains separate drafts keyed by species and age. Both export paths use `ConfigFile.overlay` to replace authored ages while retaining untouched age buckets, including disabled ones. `ModelPartVocabulary` shares attachment naming between editing and export.

`PickerFreezeService` preserves prior `NoAI`, reconciles locks at load/logout/stop, and allows one editor. `PickerGate` owns authorization (creative plus `Permissions.COMMANDS_GAMEMASTER`) and throttles; spawn-all also needs server enablement. `PickerSpawnService` uses `MobSpawning` for loader spawn finalization, honors cancellation, then sets `NoAI`, persistence, and rotation. Its `refusal` filters commands and suggestions. Vanilla inserts each passenger group. The spawn service removes partial groups on refusal and edits terrain only after success. World export validates the merged definition sync budget, writes the generated datapack, and invokes vanilla reload. Export-all writes the game-directory export tree. The world pack declares the running game’s major and minor data-pack format.

`LookTarget` limits entity intersections by the nearest blocking surface; the inspector compares actual hit distances. Client editing and server admin, spawn, and pose commands share `/sg`. Fabric forwards the server branches.

## Loader integration

Fabric Mixins cover heal reactions, trades, and shears-kill and head drops (no Fabric API events). Common Mixins in `somegoogly-common.mixins.json` report milking, sheep shearing, and entity events and hold head eyes. Client entries share that config under `mixin.common.client`. Mixins target the pinned, unobfuscated Minecraft version without refmaps.

Forge’s required `PayloadChannel` marks payloads handled. Both native loaders isolate physical-client bootstrap.

## Automated verification

GameTest scenarios: Fabric 187, Forge 189, NeoForge 195. Client checklists under `docs/` cover frames, inventory, hands, and boats.

## Operational boundaries

- Optional renderer integrations log failures once.
- `build-env/` mirrors build inputs as described in `orientation-build.md`; keep covered files synchronized.
