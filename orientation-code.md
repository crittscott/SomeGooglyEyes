<!-- FIXED HEADER: not content. Do not edit, trim, or count toward the size limit. -->
> **Orientation snapshot, not a specification.** Describes the code as it currently is; the code wins on any disagreement. See CLAUDE.md § Orientation files.
<!-- END FIXED HEADER -->

# Some Googly Eyes — code orientation

## Project shape

Identity: mod id `somegoogly`, package `com.github.crittscott.somegoogly`, version `0.8.3`, Java 21, Minecraft 1.21.9.

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

Common uses adapters and `@ExpectPlatform`; no loader imports.

The server owns gameplay and authorization; the client owns rendering and picker UI.

`ModContent` binds content through loader `ContentRegistrar`s. Slimy Eye uses vanilla `crafting_transmute`; Optometrist and `self_shear` are data-driven.

## Configuration and eye definitions

`ServerConfig`/`ClientConfig` expose validated `ConfigValue<T>`s. Forge and NeoForge use native CLIENT/SERVER specs, resetting SERVER values on unload. Fabric uses bundled NightConfig for table-aware TOML parsing at startup and `/reload`, resetting on stop. Completion adds only missing keys and retains original text in `.bak`; parse failures preserve the file and runtime values. The server logs the effective master switch at startup and changes.

Server config keys have the same order across loaders.

Eye definitions are datapack resources at `data/<namespace>/eyes/*.json`, modeled by `EyeConfigModel`. Reload resolves one version per entity type, validates, encodes to NBT, and swaps `ServerEyeConfigs` only if the encoding differs, keeping the old set on failure. That encoding is the sync payload, so `ClientEyeConfigs` never selects versions. Size and geometry limits stay aligned across reload (which also caps payload bytes), picker export, and network decode. `EyeColor` codecs reject out-of-range channels on decode. `EyeConfigLimits.Issue` gives translated feedback and log diagnostics.

## Entity and item state

`EyeState.Snapshot` is the unit for entity-state transitions, tracking sync, and packet application. NBT access goes through `EntityPersistentData`: native persistent compounds on NeoForge and Forge, a persistent attachment on Fabric.

Persistent entity keys: `somegoogly:hasGooglyEyes`, `somegoogly:eyeVariantRoll`, `somegoogly:eyeOverrides`.

Mutations send one full snapshot; stored rolls read clamped to 0..1. Eye state and roll initialize together once. Initialization and tracking skip default state; mid-life mutations always send. `EyeState.copy` handles conversion and non-death respawn; joining players get their own snapshot.

Eye items carry `AppearanceOverride` in the synced `somegoogly:eye_properties` component, never geometry; harvesting copies the first eye's effective appearance, the modifier recipe changes only that component, and the Slimy Eye transmute copies every component.

`SkullEyeState` stores placed-head snapshots in the Mixin-added `somegoogly:data` compound, replaced on load and carried by update tags. Head items use the implicit `somegoogly:head_eyes` component. Heads borrow adult definitions, selecting variants with eyes on the head part; modded heads have no mapping. Mutations send block updates.

Saved data from the 1.21.4 release is already in current form; nothing migrates.

`EyeItemService` owns authorization, mutation, drops, and durability for Slimy Eye use and both harvests, on mobs and heads. Loader adapters run entity interaction, block use, and self-shearing after protection listeners (Forge/NeoForge `LOWEST`, Fabric a late phase); head use also checks `mayInteract`/`mayUseItemAt`, and Forge/NeoForge honor separate item-use denials. `markHeadDrop` stamps an eyed mob's own head among its death drops (Forge/NeoForge at `LOWEST`); head loot copies the state to the item (Forge/NeoForge `HeadEyesLootModifier` delegates to `SkullEyeState.copyToLoot`, with native data in each loader; Fabric `LootTableEvents.MODIFY` adding `copy_components`). Fabric's adapter refuses spectators. Shears are `#somegoogly:shears` (vanilla plus optional `#c:tools/shear`). Applying a Slimy Eye to another player also requires server PvP and `canHarmPlayer`. Plain-shears self-damage uses the attacker-less `self_shear` type.

Successful Slimy Eye applications emit `GameEvent.ENTITY_INTERACT` (`BLOCK_CHANGE` on a head), successful harvests `GameEvent.SHEAR`; refusals emit neither.

## Eligibility and behaviors

`ServerServices.onLivingEntityLoaded` owns natural eye initialization; later configuration changes never revisit an initialized entity.

`EyeBehavior` catalogs one-shot expressions; `OngoingBehavior` the ongoing states, each with its server switch. A `BehaviorInstance` (id, duration, seed, elapsed) is the only per-behavior state sent. `ServerBehaviorScheduler` schedules ambient play for watched eyed mobs and takes damage, heal, trade, milking, shearing, and entity-event triggers; it starts nothing on a sleeper. The master switch suspends automatic reactions, ambient work, anger checks, and catch-up packets while preserving watcher bookkeeping and advancing effect expiry; explicit admin triggers remain available. `GooglyTracker` folds in each switched-on `OngoingBehavior`; only sleep blocks behaviors, switch or not. `HeldEye` plays the held item's idle, hurt, and heal behaviors per synced `BehaviorSettings`, caching its eligible ambient pool. Client behaviors are inactive until the first sync. `SkullEyes` derives a head's ambient behaviors on each client from its position and game time, in slots of the synced ambient mean. Server-only anger is checked per tick and sent on change or tracking start. `ClientEyeRuntime` retains expression timing across tracker replacement. Entity unload/world replacement clear expressions and anger; render-cache resets retain them.

## Rendering and attachment

Each eye part is submitted as custom geometry at its finished pose, values copied into its callback; `ModelGooglyEye` is stateless. `ClientRenderLayers` installs `LayerGooglyEyes` once per renderer rebuild, before slime's outer layer; GeckoLib uses its `CompileRenderLayers` event. Both draw picker previews. `LayerGooglyEyes` poses its parent model before resolving. Common Mixins store `EyeRenderData` on `LivingEntityRenderState`; GeckoLib captures it during render-state extraction and registers per-bone submission tasks with state-owned eye data and copied picker drafts. `GooglyEyeRenderer.drawEye` submits every eye, `drawAnimatedEye` simulated ones. `ClientLifecycle` ticks the first-person `HeldEye`. Rendering and pupil projection share `EyePlacement.orientation`.

Attachment resolvers cache by model identity and replay `ThirdPartyModelWraps`. `RootModelResolver` walks `EntityModel.root()` after reflected Citadel/Uranus/LLibrary resolvers. The picker selects separate baby models by age.

The picker gizmo uses a registered no-depth render pipeline. The picker HUD follows Fabric's subtitles, tops NeoForge's layers, and is appended last to Forge's layered HUD draw. Placed-head eyes are chosen at extraction onto `SkullBlockRenderState`; Mixins bracket placed-head and head-item submission and key head items' GUI identity on eyes. `SkullEyeRenderer` poses the `SkullModel`/`PiglinHeadModel` and submits on its head part. Worn heads get no eyes.

GeckoLib links only after detection; attach failures cannot block loading.

Item definitions select `GooglyEyeItemRenderer` and `SlimyEyeIrisTint` (`layer2`). NeoForge registers them by event; Forge and Fabric use vanilla's `ID_MAPPER`s.

## Networking

Six payloads carry definitions, entity eye state, expressions, anger, picker freeze, and export. `NetworkHandler` ids embed version `17`; bump on wire changes. Native loaders require that channel version; Fabric requires its `Handshake`.

The definition packet carries `googlyEyesEnabled` and `BehaviorSettings`; reload validation reserves the largest supported behavior block. Joins receive it; `ServerServices.broadcastEyeConfigsIfChanged` resends after reload/config apply only when definitions or settings changed, because it resets client trackers.

Sends go through `@ExpectPlatform` `Networking` and client-only `ClientNetworking`; clientbound handlers reach `ClientNetworkHandler` only inside lambdas, so servers never link it. Serverbound handlers reauthorize the sender; the client rejects exports over vanilla's cap; unknown entity ids are ignored.

## Picker and commands

The client owns picker drafts and previews; the server owns freezing, spawning, movement, and export. `PickerState.DraftSession` retains separate drafts keyed by species and age. Both export paths use `ConfigFile.overlay` to replace authored ages while retaining untouched age buckets, including disabled ones. `ModelPartVocabulary` shares attachment naming between editing and export.

`PickerFreezeService` preserves prior `NoAI`, reconciles locks at load/logout/stop, and allows one editor. `PickerGate` owns authorization (creative plus permission level 2) and throttles; spawn-all also needs server enablement. `PickerSpawnService` uses `MobSpawning` for loader spawn finalization, honors cancellation, then sets `NoAI`, persistence, and rotation. Its `refusal` filters commands and suggestions. Bulk terrain edits follow accepted finalization and insertion. Each mob/rider insertion result is checked; partial additions are removed on refusal. World export writes the generated datapack, export-all the game-directory export tree. Both use `ConfigFile.exportVersion`; bulk-export disk failures log the entity, target, completed count, and exception.

`LookTarget` limits entity intersections by the nearest blocking surface; the inspector compares actual hit distances. Client and server own disjoint `/sg` branches: editing is client-side; admin, spawn, spawn-all, and mob-pose are server-side. Fabric forwards those server branches explicitly, since its client root would capture them.

## Loader integration

Fabric Mixins cover heal reactions, trades, and shears-kill and head drops (no Fabric API events). Common Mixins (`somegoogly-common-server.mixins.json`) report milking, sheep shearing, and entity events and hold head eyes. Mixins target the pinned Minecraft version, with fixed refmaps.

NeoForge registers from the `@Mod` constructor, client listeners on the right bus.

Forge's required `PayloadChannel` marks payloads handled. NeoForge and Forge isolate physical-client bootstrap from dedicated-server bootstrap.

## Automated verification

Fabric discovers annotated instance methods through `fabric-gametest`; Forge/NeoForge register functions paired with loader-owned test-instance JSON. Shared tests use component-valued messages and cover serialization, gameplay state, expression lifetime, disabled scheduling, and obstructed targeting. Forge/NeoForge add native config-correction and interaction/spawn-refusal tests; Fabric adds TOML preservation tests. Fabric has 128 registered scenarios; Forge and NeoForge each have 129. Required-client rejection, full command UX, general protection ordering, anger delivery, and loading real 1.21.4 saves still need manual checks.

## Operational boundaries

- Optional renderer integrations log failures once.
- `build-env/` mirrors build inputs as described in `orientation-build.md`; keep covered files synchronized.
