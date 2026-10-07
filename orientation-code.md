<!-- FIXED HEADER: not content. Do not edit, trim, or count toward the size limit. -->
> **Orientation snapshot, not a specification.** Describes the code as it currently is; the code wins on any disagreement. See CLAUDE.md § Orientation files.
<!-- END FIXED HEADER -->

# Some Googly Eyes — code orientation

Structure, ownership, state, loader seams, and invariants; `orientation-player.md` covers behavior.

## Project shape

Identity: mod id `somegoogly`, package `com.github.crittscott.somegoogly`, version `0.8.2`, Java 21, Minecraft 1.21.4.

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

Common main imports no loader type; only `client.compat.gecko` uses GeckoLib; differences pass through adapters or six `@ExpectPlatform` methods. Loader packages stay disjoint from common packages so Forge sees no split package.

The server owns eligibility, eye state, item actions, behaviors, definitions, picker authorization, and world mutation; the client owns rendering, attachment, pupil motion, inspection, and picker UI.

Registered content is declared once in `ModContent` and bound through the loader's `ContentRegistrar`: two items, a `DataComponentType`, a creative tab, and the modifier recipe serializer; the Slimy Eye recipe is vanilla `crafting_transmute`. Optometrist is a data-driven enchantment from the common data pack.

## Configuration and eye definitions

`ServerConfig` and `ClientConfig` hold keys, defaults, ranges, validators, and comments, exposing validated `ConfigValue<T>`s; `ConfigValue.Parsed` lists rebuild a parsed view on assignment. Forge and NeoForge use native CLIENT and SERVER specs copied in on load and reload; SERVER unload restores defaults so values cannot escape their world. Fabric's own `TomlConfig` reads both files, the server file at start and each `/reload`, resetting on stop.

File names, section names, and comments are `ServerConfig`/`ClientConfig` constants; server key order must match across the three loaders' server configs.

Eye definitions are datapack resources at `data/<namespace>/eyes/*.json`, modeled by `EyeConfigModel`. Reload resolves and validates one version per entity type, encodes the set to NBT, and atomically swaps `ServerEyeConfigs` only if that encoding differs; failure keeps the previous set. The stored encoding is the sync payload. The resolved set is pushed to clients, so `ClientEyeConfigs` never selects a version itself. Size and geometry limits are enforced at three points that must stay aligned: datapack reload, picker export, and network decode; reload also refuses a set whose sync payload exceeds the byte limit. `EyeColor` codecs reject out-of-range channels on decode.

Player-visible strings are `Component`s in `en_us.json`; logs, command literals, config comments, and schema keys are not.

## Entity and item state

`EyeState` is the entity-eye-state boundary; its `Snapshot` is the whole unit for server transitions, tracking sync, and client packet application, and partial updates are not a supported path. NBT access goes through `EntityPersistentData`: native persistent compounds on NeoForge and Forge, a persistent attachment on Fabric. On load, `FabricEntityDataMigration` moves the 1.21.1 release's `somegoogly:persistentData` compound into it; a non-compound value is set aside there and logged.

Persistent entity keys:

- `somegoogly:hasGooglyEyes` — whether the entity has eyes;
- `somegoogly:eyeVariantRoll` — stable placement-variant roll;
- `somegoogly:eyeOverrides` — optional shared appearance overrides.

Related mutations flush as one full-snapshot sync. A stored roll outside 0..1 reads clamped. The eye-state key and variant roll are initialized together only when absent, preserving the natural-eyes decision across persistence and transfer. `EyeState.initialize` and `EyeState.sendTo` skip an absent snapshot; mid-life mutations always send.

Eye item stacks carry `AppearanceOverride` in the registered `somegoogly:eye_properties` component (synced by `AppearanceOverride.STREAM_CODEC`); harvesting copies the first configured eye's effective appearance; the modifier recipe and Slimy Eye application change only that component, and the Slimy Eye transmute copies every component. Item stacks never carry placement geometry.

The item component, entity keys, and server-config keys are unchanged since the 1.21.1 release, so only Fabric entity data needs migration.

`EyeItemService` owns authorization, mutation, drops, and durability for Slimy Eye use and both harvests. Loader adapters run entity interaction after protection listeners (Forge/NeoForge `LOWEST`, Fabric a late callback phase). Applying a Slimy Eye to another player also requires server PvP and `canHarmPlayer`.

Every successful Slimy Eye application emits `GameEvent.ENTITY_INTERACT`; every successful interactive, self, or kill harvest emits `GameEvent.SHEAR`. Refused operations emit neither.

## Eligibility and behaviors

`ServerServices.onLivingEntityLoaded` owns natural eye initialization; once an entity is initialized, later configuration changes never revisit it.

The `EyeBehavior` enum is the behavior catalog. A `BehaviorInstance` (id, duration, seed, elapsed) is the only per-behavior state sent, and clients derive the animation from it, so no per-tick animation packets exist. `ServerBehaviorScheduler` schedules ambient behaviors for watched eyed entities and takes damage, heal, and trade triggers from adapters. `ClientEyeRuntime` holds transient pupil and behavior state for rendered entities.

## Rendering and attachment

`ClientRenderLayers` installs `LayerGooglyEyes` on every living renderer, duplicate-safe, after each renderer rebuild (NeoForge and Forge on `AddLayers`, Forge by registry entity type, Fabric by its living-renderer callback, clearing caches from a client reload listener); GeckoLib renderers get theirs from GeckoLib's entity `CompileRenderLayers` event. `LayerGooglyEyes` must precede the slime outer layer; both layers draw the picker preview for the picker's target. Layers see only render states, so common Mixins (`somegoogly-common.mixins.json`, all loaders) store each entity's `EyeRenderData` decision on `LivingEntityRenderState` during extraction; the GeckoLib layer takes it in `preRender`. `ClientEyeConfigs` caches the resolved view per age and variant. `GooglyEyeRenderer.drawEye` draws every eye (mobs, previews, item); the held item's pupil is one local-player simulation ticked by `ClientLifecycle` and drawn in first person only; `EyePlacement.orientation` is the one eye rotation, for rendering and pupil-plane projection.

Attachment resolvers cache by model identity, clear on renderer or runtime reset, and replay `ThirdPartyModelWraps` transforms first. `RootModelResolver` walks `EntityModel.root()` and follows the reflected Citadel, Uranus, and LLibrary resolvers. Baby models are separate instances scaled in their part poses; the picker picks an `AgeableMobRenderer`'s model by entity age.

The common `somegoogly.accesswidener` serves common compilation and every loader; Forge and NeoForge convert it to an access transformer at remap.

GeckoLib is optional: common compiles against compile-only `geckolib-common`, each loader against its own artifact; nothing touches `client.compat.gecko` or registers a layer listener unless `GeckoCompat` finds it. A failed attach must not block load.

Item definitions in `assets/somegoogly/items/` select special model renderer `GooglyEyeItemRenderer` and tint source `SlimyEyeIrisTint` (third in the Slimy Eye's `tints`, matching `layer2`). NeoForge registers both by event; Forge and Fabric put them into vanilla's `ID_MAPPER`s at client init.

## Networking

Five `CustomPacketPayload` classes: eye definitions, entity eye state, behavior triggers, picker freeze, and picker export. Their ids, in `NetworkHandler`, embed network version `14`; any incompatible wire change requires bumping it. Forge and NeoForge register a required native channel version; Fabric disconnects, during configuration, a client that cannot receive its never-sent `Handshake` payload.

The eye-definition packet also carries `googlyEyesEnabled`. Joins always receive it; after each datapack reload and Forge/NeoForge server-config apply, `ServerServices.broadcastEyeConfigsIfChanged` resends it only if the set or switch changed, since it resets client trackers.

Sends go through `@ExpectPlatform` bridges `Networking` and client-only `ClientNetworking`; clientbound handlers reach `ClientNetworkHandler` only inside lambdas, so servers never link it. Serverbound handlers re-check the authenticated `ServerPlayer`'s authorization; the client refuses a picker export over vanilla's serverbound cap. Eye-state and behavior packets reach trackers and the entity itself; eye state follows the entity's spawn, so it carries only the id and an unknown id is ignored.

## Picker and commands

The client owns picker drafts and previews; the server owns freezing, spawning, movement, and world export; only freeze and export cross payloads. `ModelPartVocabulary` supplies one attachment grammar to live editing and bulk export.

`PickerFreezeService` preserves prior `NoAI`, reconciles locks on mob load, logout, and server stop, and allows one editor. `PickerGate` owns every picker throttle (per-tick requests, export cooldowns, spawn-all cooldown); spawn-all also requires creative and server enablement. `PickerSpawnService` finalizes command-spawned mobs through `MobSpawning` (the loader's finalize-spawn event on NeoForge and Forge; a cancelled spawn is not added) before setting `NoAI`, persistence, and display rotation; its `refusal` (ender dragon, unsummonable, `ServerConfig.isSpawnExcluded`) is the one spawnability rule for both commands and spawn suggestions. World export is confined to the generated datapack and requires creative plus permission level 2; export-all stays under the game-directory export tree. Both use `ConfigFile.exportVersion`.

Client and server own disjoint `/sg` branches: editing is client-side; admin, spawn, spawn-all, and mob-pose are server-side. Fabric explicitly forwards those server branches because its matching client root otherwise captures them.

## Loader integration

Fabric Mixins cover persistent-data migration, heal reactions, trades, and shears-kill drops (no Fabric API events) and target the pinned Minecraft version; Fabric and common write fixed refmaps.

NeoForge: common registration runs once from the `@Mod` constructor, and client services must be attached to the correct bus (mod versus game).

Forge's required `PayloadChannel` marks payloads handled. NeoForge and Forge isolate physical-client bootstrap from dedicated-server bootstrap.

## Automated verification

`common/src/gametest/java` holds 99 shared tests, including save/load persistence; each loader wraps them, and Fabric adds 4 migration and TOML tests. Root `generateDocs` syncs all production and GameTest Javadoc into `docs/javadoc/`. Required-client rejection, server commands, plain-shears self-damage, and real-save migration are manual.

## Operational boundaries

- Optional renderer integrations log recoverable failures once and omit eyes lacking geometry.
- `build-env/` is a byte-for-byte, non-input snapshot of the build files listed in `build-env.md`; mirror edits there.
