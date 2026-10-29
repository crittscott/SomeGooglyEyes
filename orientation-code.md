<!-- FIXED HEADER: not content. Do not edit, trim, or count toward the size limit. -->
> **Orientation snapshot, not a specification.** Describes the code as it currently is; the code wins on any disagreement. See CLAUDE.md § Orientation files.
<!-- END FIXED HEADER -->

# Some Googly Eyes — code orientation

Structure, ownership, state, loader seams, and invariants; `orientation-player.md` covers behavior.

## Project shape

Identity: mod id `somegoogly`, package `com.github.crittscott.somegoogly`, version `0.8.3`, Java 21, Minecraft 1.21.4.

The Gradle project has four modules; `common` is transformed into all three loader artifacts.

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

Common imports no loader types; adapters and `@ExpectPlatform` isolate differences. Disjoint loader packages avoid Forge split packages.

The server owns gameplay, definitions, authorization, and world mutation; the client owns rendering, pupil motion, inspection, and picker UI.

`ModContent` declares registered content, bound via each loader's `ContentRegistrar`: two items, a `DataComponentType`, a creative tab, and the modifier recipe serializer; the Slimy Eye recipe is vanilla `crafting_transmute`. Optometrist is a data-driven enchantment and `self_shear` a data-driven damage type, both from the common data pack.

## Configuration and eye definitions

`ServerConfig` and `ClientConfig` hold keys, defaults, ranges, validators, and comments, exposing validated `ConfigValue<T>`s; `ConfigValue.Parsed` lists rebuild a parsed view on assignment. Forge and NeoForge use native CLIENT and SERVER specs copied in on load and reload; SERVER unload restores defaults so values cannot escape their world. Fabric's own `TomlConfig` reads both files, the server file at start and each `/reload`, resetting on stop.

Server config keys have the same order across loaders.

Eye definitions are datapack resources at `data/<namespace>/eyes/*.json`, modeled by `EyeConfigModel`. Reload resolves and validates one version per entity type, encodes the set to NBT, and atomically swaps `ServerEyeConfigs` only if that encoding differs; failure keeps the previous set. The stored encoding is the sync payload. The resolved set is pushed to clients, so `ClientEyeConfigs` never selects a version itself. Size and geometry limits are enforced at three points that must stay aligned: datapack reload, picker export, and network decode; reload also refuses a set whose sync payload exceeds the byte limit. `EyeColor` codecs reject out-of-range channels on decode. `EyeConfigLimits.Issue` carries translated feedback and a plain diagnostic for logs and packet failures.

## Entity and item state

`EyeState.Snapshot` is the unit for entity-state transitions, tracking sync, and packet application. NBT access goes through `EntityPersistentData`: native persistent compounds on NeoForge and Forge, a persistent attachment on Fabric. On load, `FabricEntityDataMigration` moves the 1.21.1 release's `somegoogly:persistentData` compound into it; a non-compound value is set aside there and logged.

Persistent entity keys: `somegoogly:hasGooglyEyes` (has eyes), `somegoogly:eyeVariantRoll` (stable variant roll), `somegoogly:eyeOverrides` (optional appearance overrides).

Mutations send one full snapshot; stored rolls read clamped to 0..1. Eye state and roll initialize together once. Initialization/tracking omit default eyeless state; mid-life mutations always send. `EyeState.copy` handles mob conversion and non-death player respawn. Players joining a level receive their own snapshot.

Eye item stacks carry `AppearanceOverride` in the registered `somegoogly:eye_properties` component (synced by `AppearanceOverride.STREAM_CODEC`); harvesting copies the first configured eye's effective appearance; the modifier recipe and Slimy Eye application change only that component, and the Slimy Eye transmute copies every component. Item stacks never carry placement geometry.

Only Fabric entity data needs migration from the 1.21.1 release.

`EyeItemService` owns authorization, mutation, drops, and durability for Slimy Eye use and both harvests. Loader adapters run entity interaction and self-shearing after ordinary protection listeners (Forge/NeoForge `LOWEST`, Fabric a late callback phase). Fabric's adapter refuses spectators itself. Shears are `#somegoogly:shears` (vanilla plus optional `#c:tools/shear`). Applying a Slimy Eye to another player also requires server PvP and `canHarmPlayer`. Plain-shears self-damage uses the attacker-less `self_shear` type, so PvP rules cannot cancel it.

Successful Slimy Eye applications emit `GameEvent.ENTITY_INTERACT`, successful harvests `GameEvent.SHEAR`; refusals emit neither.

## Eligibility and behaviors

`ServerServices.onLivingEntityLoaded` owns natural eye initialization; later configuration changes never revisit an initialized entity.

The `EyeBehavior` enum is the catalog. A `BehaviorInstance` (id, duration, seed, elapsed) is the only per-behavior state sent, and clients derive the animation. `ServerBehaviorScheduler` schedules ambient play for watched eyed mobs and takes damage, heal, trade, milking, shearing, and entity-event triggers; it starts nothing on a sleeper. `GooglyTracker` derives sleeping villagers' eyelids (`ModelGooglyEye.renderLids`), the sleep, goat-ram, and one-eyed aiming squints, creeper rattle, red pupils, and enderman pulse from synced state; only sleep blocks behaviors. Server-only anger (`NeutralMob`, piglin `ANGRY_AT`) is checked per tick and sent on change and to new viewers of eyed mobs. `ClientEyeRuntime` holds rendered entities' transient pupil and behavior state. Entity-unload and client-level replacement events remove trackers and anger, while renderer resets retain anger for loaded entities.

## Rendering and attachment

`ClientRenderLayers` installs `LayerGooglyEyes` on every living renderer, duplicate-safe, after each renderer rebuild through each loader's renderer hook; GeckoLib renderers get theirs from GeckoLib's entity `CompileRenderLayers` event. `LayerGooglyEyes` must precede the slime outer layer; both layers draw the picker preview for the picker's target. Layers see only render states, so common Mixins (`somegoogly-common.mixins.json`, all loaders) store each entity's `EyeRenderData` decision on `LivingEntityRenderState` during extraction; the GeckoLib layer takes it in `preRender`. `ClientEyeConfigs` caches the resolved view per age and variant. `GooglyEyeRenderer.drawEye` draws every eye (mobs, previews, item); the held item's pupil is one local-player simulation ticked by `ClientLifecycle` and drawn in first person only; `EyePlacement.orientation` is the one eye rotation, for rendering and pupil-plane projection.

Attachment resolvers cache by model identity, clear on renderer or runtime reset, and replay `ThirdPartyModelWraps` transforms first. `RootModelResolver` walks `EntityModel.root()` and follows the reflected Citadel, Uranus, and LLibrary resolvers. Baby models are separate instances scaled in their part poses; the picker picks an `AgeableMobRenderer`'s model by entity age.

The common `somegoogly.accesswidener` serves common compilation and every loader; Forge and NeoForge convert it to an access transformer at remap.

GeckoLib is optional: common compiles against compile-only `geckolib-common`; nothing touches `client.compat.gecko` unless `GeckoCompat` finds it, and a failed attach must not block load.

Item definitions in `assets/somegoogly/items/` select special model renderer `GooglyEyeItemRenderer` and tint source `SlimyEyeIrisTint` (third in the Slimy Eye's `tints`, matching `layer2`). NeoForge registers both by event; Forge and Fabric put them into vanilla's `ID_MAPPER`s at client init.

## Networking

Six `CustomPacketPayload` classes: eye definitions, entity eye state, behavior triggers, anger, picker freeze, and picker export. Their ids, in `NetworkHandler`, embed network version `15`; any incompatible wire change requires bumping it. Forge and NeoForge register a required native channel version; Fabric disconnects, during configuration, a client that cannot receive its `Handshake` payload.

The eye-definition packet also carries `googlyEyesEnabled`. Joins always receive it; after each datapack reload and Forge/NeoForge server-config apply, `ServerServices.broadcastEyeConfigsIfChanged` resends it only if the set or switch changed, since it resets client trackers.

Sends go through `@ExpectPlatform` bridges `Networking` and client-only `ClientNetworking`; clientbound handlers reach `ClientNetworkHandler` only inside lambdas, so servers never link it. Serverbound handlers reauthorize the sender; the client rejects exports over vanilla's payload cap. Entity packets reach trackers and self after spawn; unknown entity ids are ignored.

## Picker and commands

The client owns picker drafts and previews; the server owns freezing, spawning, movement, and export. `PickerState.DraftSession` retains separate drafts keyed by species and age. Both export paths use `ConfigFile.overlay` to replace authored ages while retaining untouched age buckets, including disabled ones. `ModelPartVocabulary` shares attachment naming between editing and export.

`PickerFreezeService` preserves prior `NoAI`, reconciles locks at load/logout/stop, and allows one editor. `PickerGate` owns authorization (creative plus permission level 2) and throttles; spawn-all also needs server enablement. `PickerSpawnService` uses `MobSpawning` for loader spawn finalization, honors cancellation, then sets `NoAI`, persistence, and rotation. Its shared `refusal` filters commands and suggestions for dragons, unsummonable types, and configured exclusions. World export stays in the generated datapack; export-all under the game-directory export tree. Both use `ConfigFile.exportVersion`.

Client and server own disjoint `/sg` branches: editing is client-side; admin, spawn, spawn-all, and mob-pose are server-side. Fabric forwards those server branches explicitly, since its client root would capture them.

## Loader integration

Fabric Mixins cover persistent-data migration, heal reactions, trades, and shears-kill drops (no Fabric API events). Common Mixins (`somegoogly-common-server.mixins.json`) report milking, sheep shearing, and entity events. Mixins target the pinned Minecraft version; Fabric and common write fixed refmaps.

NeoForge: common registration runs once from the `@Mod` constructor, and client services must be attached to the correct bus (mod versus game).

Forge's required `PayloadChannel` marks payloads handled. NeoForge and Forge isolate physical-client bootstrap from dedicated-server bootstrap.

## Automated verification

`common/src/gametest/java` holds 108 shared tests, with persistence; each loader wraps them, and Fabric adds 4 migration and TOML tests. Root `generateDocs` syncs all production and GameTest Javadoc into `docs/javadoc/`. Required-client rejection, server commands, picker operator gating, plain-shears self-damage, protection callback ordering, actual anger packet/unload delivery, and real-save migration are manual. Shared regressions cover draft age retention, export age overlays, and anger-cache lifetime.

## Operational boundaries

- Optional renderer integrations log failures once.
- `build-env/` mirrors build inputs as described in `orientation-build.md`; keep covered files synchronized.
