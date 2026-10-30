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

`ModContent` binds, through each loader's `ContentRegistrar`, two items, the appearance and head-eyes components, a creative tab, and the modifier recipe serializer; the Slimy Eye recipe is vanilla `crafting_transmute`. Optometrist and `self_shear` are data-driven.

## Configuration and eye definitions

`ServerConfig`/`ClientConfig` expose validated `ConfigValue<T>`s. Forge and NeoForge use native CLIENT/SERVER specs, resetting SERVER values on unload. Fabric's own `TomlConfig` reads both files, the server file at start and each `/reload`, resetting on stop. Fabric, like the native specs, fills in missing keys; sections keep 0.8.2's names.

Server config keys have the same order across loaders.

Eye definitions are datapack resources at `data/<namespace>/eyes/*.json`, modeled by `EyeConfigModel`. Reload resolves one version per entity type, validates, encodes to NBT, and swaps `ServerEyeConfigs` only if the encoding differs, keeping the old set on failure. That encoding is the sync payload, so `ClientEyeConfigs` never selects versions. Size and geometry limits stay aligned across reload (which also caps payload bytes), picker export, and network decode. `EyeColor` codecs reject out-of-range channels on decode. `EyeConfigLimits.Issue` gives translated feedback and log diagnostics.

## Entity and item state

`EyeState.Snapshot` is the unit for entity-state transitions, tracking sync, and packet application. NBT access goes through `EntityPersistentData`: native persistent compounds on NeoForge and Forge, a persistent attachment on Fabric. On load, `FabricEntityDataMigration` moves the 1.21.1 release's `somegoogly:persistentData` compound into it; a non-compound value is set aside there and logged.

Persistent entity keys: `somegoogly:hasGooglyEyes`, `somegoogly:eyeVariantRoll`, `somegoogly:eyeOverrides`.

Mutations send one full snapshot; stored rolls read clamped to 0..1. Eye state and roll initialize together once. Initialization and tracking skip default state; mid-life mutations always send. `EyeState.copy` handles conversion and non-death respawn; joining players get their own snapshot.

Eye items carry `AppearanceOverride` in the synced `somegoogly:eye_properties` component, never geometry; harvesting copies the first eye's effective appearance, the modifier recipe changes only that component, and the Slimy Eye transmute copies every component.

`SkullEyeState` keeps a placed head's `Snapshot` (one `read`/`write`) in a compound the common `SkullBlockEntityDataMixin` adds, saved as `somegoogly:data` and replaced on every load, so it rides the update tag; a head item carries it as the `somegoogly:head_eyes` component, taken and given as an implicit block-entity component. A head borrows its mob's adult definition, rolling among variants with eyes on its head part. Vanilla heads map by type; other mods' heads have none. Mutations end in a block update.

Only Fabric entity data needs migration from the 1.21.1 release.

`EyeItemService` owns authorization, mutation, drops, and durability for Slimy Eye use and both harvests, on mobs and heads. Loader adapters run entity interaction, block use, and self-shearing after protection listeners (Forge/NeoForge `LOWEST`, Fabric a late phase); head use also checks `mayInteract`/`mayUseItemAt`. `markHeadDrop` stamps an eyed mob's own head among its death drops (Forge/NeoForge at `LOWEST`); head loot copies the state to the item (Forge/NeoForge `HeadEyesLootModifier`, data in `common`; Fabric `LootTableEvents.MODIFY` adding `copy_components`). Fabric's adapter refuses spectators. Shears are `#somegoogly:shears` (vanilla plus optional `#c:tools/shear`). Applying a Slimy Eye to another player also requires server PvP and `canHarmPlayer`. Plain-shears self-damage uses the attacker-less `self_shear` type.

Successful Slimy Eye applications emit `GameEvent.ENTITY_INTERACT` (`BLOCK_CHANGE` on a head), successful harvests `GameEvent.SHEAR`; refusals emit neither.

## Eligibility and behaviors

`ServerServices.onLivingEntityLoaded` owns natural eye initialization; later configuration changes never revisit an initialized entity.

`EyeBehavior` catalogs one-shot expressions; `OngoingBehavior` the ongoing states, each with its server switch. A `BehaviorInstance` (id, duration, seed, elapsed) is the only per-behavior state sent. `ServerBehaviorScheduler` schedules ambient play for watched eyed mobs and takes damage, heal, trade, milking, shearing, and entity-event triggers; it starts nothing on a sleeper. `GooglyTracker` folds in each switched-on `OngoingBehavior`; only sleep blocks behaviors, switch or not. `HeldEye` plays the held item's idle, hurt, and heal behaviors per the synced `BehaviorSettings`. `SkullEyes` derives a head's ambient behaviors on each client from its position and game time, in slots of the synced ambient mean. Server-only anger (`NeutralMob`, piglin `ANGRY_AT`) is checked per tick and sent on change and to new viewers of eyed mobs. `ClientEyeRuntime` holds transient pupil and behavior state for rendered entities and heads (by position, rebuilt when their snapshot changes). Entity unload and level replacement remove trackers and anger; renderer resets keep it.

## Rendering and attachment

`ClientRenderLayers` installs `LayerGooglyEyes` on every living renderer, duplicate-safe, after each rebuild; GeckoLib renderers get theirs from its `CompileRenderLayers` event. `LayerGooglyEyes` must precede the slime outer layer; both draw the picker preview. Layers see only render states, so common Mixins (`somegoogly-common.mixins.json`, all loaders) store each entity's `EyeRenderData` decision on `LivingEntityRenderState` during extraction; the GeckoLib layer takes it in `preRender`. `ClientEyeConfigs` caches views. `GooglyEyeRenderer.drawEye` draws every eye, `drawAnimatedEye` every simulated one; `ClientLifecycle` ticks `HeldEye`, drawn live in first person only; `EyePlacement.orientation` is the one eye rotation, for rendering and pupil-plane projection.

Attachment resolvers cache by model identity, clear on renderer or runtime reset, and replay `ThirdPartyModelWraps` transforms first. `RootModelResolver` walks `EntityModel.root()` and follows the reflected Citadel, Uranus, and LLibrary resolvers. Baby models are separate scaled instances; the picker picks an `AgeableMobRenderer`'s by age.

Common client Mixins bracket a placed head's draw (`SkullBlockRendererMixin`) and a head item's (`ItemStackRenderStateMixin`, holding the stack `ItemModelResolverMixin` records); `SkullEyeRenderer` draws after `renderSkull`'s model draw on the widened `SkullModel`/`PiglinHeadModel` head part. Worn heads get no eyes.

The common `somegoogly.accesswidener` serves common compilation and every loader; Forge and NeoForge convert it to an access transformer at remap.

GeckoLib is optional: common compiles against compile-only `geckolib-common`, touching `client.compat.gecko` only once `GeckoCompat` finds it; a failed attach can't block load.

Item definitions in `assets/somegoogly/items/` select special model renderer `GooglyEyeItemRenderer` and tint source `SlimyEyeIrisTint` (third in the Slimy Eye's `tints`, matching `layer2`). NeoForge registers both by event; Forge and Fabric put them into vanilla's `ID_MAPPER`s at client init.

## Networking

Six `CustomPacketPayload` classes: eye definitions, entity eye state, behavior triggers, anger, picker freeze, and picker export. Their ids, in `NetworkHandler`, embed network version `17`; bump it on wire changes. Forge and NeoForge require a native channel version; Fabric disconnects a client that can't receive its `Handshake`.

The eye-definition packet also carries `googlyEyesEnabled` and `BehaviorSettings`. Joins always receive it; after each datapack reload and Forge/NeoForge server-config apply, `ServerServices.broadcastEyeConfigsIfChanged` resends it only if the set, switch, or behavior settings changed, since it resets client trackers.

Sends go through `@ExpectPlatform` `Networking` and client-only `ClientNetworking`; clientbound handlers reach `ClientNetworkHandler` only inside lambdas, so servers never link it. Serverbound handlers reauthorize the sender; the client rejects exports over vanilla's cap; unknown entity ids are ignored.

## Picker and commands

The client owns picker drafts and previews; the server owns freezing, spawning, movement, and export. `PickerState.DraftSession` retains separate drafts keyed by species and age. Both export paths use `ConfigFile.overlay` to replace authored ages while retaining untouched age buckets, including disabled ones. `ModelPartVocabulary` shares attachment naming between editing and export.

`PickerFreezeService` preserves prior `NoAI`, reconciles locks at load/logout/stop, and allows one editor. `PickerGate` owns authorization (creative plus permission level 2) and throttles; spawn-all also needs server enablement. `PickerSpawnService` uses `MobSpawning` for loader spawn finalization, honors cancellation, then sets `NoAI`, persistence, and rotation. Its `refusal` filters commands and suggestions. World export writes the generated datapack, export-all the game-directory export tree. Both use `ConfigFile.exportVersion`.

Client and server own disjoint `/sg` branches: editing is client-side; admin, spawn, spawn-all, and mob-pose are server-side. Fabric forwards those server branches explicitly, since its client root would capture them.

## Loader integration

Fabric Mixins cover persistent-data migration, heal reactions, trades, and shears-kill and head drops (no Fabric API events). Common Mixins (`somegoogly-common-server.mixins.json`) report milking, sheep shearing, and entity events and hold head eyes. Mixins target the pinned Minecraft version, with fixed refmaps.

NeoForge registers from the `@Mod` constructor, client listeners on the right bus.

Forge's required `PayloadChannel` marks payloads handled. NeoForge and Forge isolate physical-client bootstrap from dedicated-server bootstrap.

## Automated verification

`common/src/gametest/java` holds 123 shared tests, with persistence; each loader wraps all but one head-drop test, and Fabric adds 5 migration and TOML tests. Required-client rejection, server commands, picker gating, self-shear damage, protection ordering, anger delivery, and real-save migration are manual.

## Operational boundaries

- Optional renderer integrations log failures once.
- `build-env/` mirrors build inputs as described in `orientation-build.md`; keep covered files synchronized.
