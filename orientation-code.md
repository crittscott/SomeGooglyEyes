<!-- FIXED HEADER: not content. Do not edit, trim, or count toward the size limit. -->
> **Orientation snapshot, not a specification.** Describes the code as it currently is; the code wins on any disagreement. See CLAUDE.md § Orientation files.
<!-- END FIXED HEADER -->

# Some Googly Eyes — code orientation

Build structure, ownership, persisted state, loader seams, and invariants. `orientation-player.md` covers observable behavior.

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

Common main imports no loader type, and only the `client.compat.gecko` package imports GeckoLib types; differences pass through project adapters or five `@ExpectPlatform` methods. Loader packages stay disjoint from common packages so Forge sees no split package.

The server owns eligibility, eye state, item actions, behaviors, datapack definitions, picker authorization, and world mutation; the client owns rendering, model attachment, pupil motion, inspection, and picker UI and editing.

Registered content is declared once in `ContentRegistrar`: two items, one `DataComponentType`, one creative tab, and two recipe serializers. Optometrist is a data-driven enchantment from the common data pack. Fabric binds these handles through native registries; NeoForge and Forge bind the same handles through native deferred registers.

## Configuration and eye definitions

`ServerConfig` and `ClientConfig` hold keys, defaults, ranges, validators, and server comments, exposing validated `ConfigValue<T>`s. Forge and NeoForge use native CLIENT and SERVER specs copied in on load and reload; SERVER unload restores defaults so values cannot escape their world. Fabric has no config system: its own `TomlConfig` reads both files, the server file at server start.

Server-config section names and key order must stay aligned across `FabricServerConfig`, `ForgeServerConfig`, and `NeoForgeServerConfig`.

Eye definitions are server datapack resources at `data/<namespace>/eyes/*.json`, modeled by `EyeConfigModel`. Reload resolves and validates exactly one version per entity type, canonically encodes the resolved set, then atomically swaps `ServerEyeConfigs`; failure at any stage keeps the previous set. The resolved set is pushed to clients, so `ClientEyeConfigs` never selects a version itself. Size and geometry limits are enforced at three points that must stay aligned: datapack reload, picker export, and network decode.

Eligibility knobs (spawn and harvest chances, overrides, behavior toggles, spawn-all gate) live in `ServerConfig`; local visibility in `ClientConfig`.

Player-visible strings are translatable `Component`s in `assets/somegoogly/lang/en_us.json`; logs, command literals, config comments, and schema keys are untranslated.

## Entity and item state

`EyeState` is the entity-eye-state boundary; its `Snapshot` is the whole unit for server transitions, tracking sync, and client packet application, and partial updates are not a supported path. NBT access goes through `EntityPersistentData`: native persistent compounds on NeoForge and Forge, a persistent attachment on Fabric, into which `EntityPersistentDataMigrationMixin` moves the released `somegoogly:persistentData` key on load.

Persistent entity keys:

- `somegoogly:hasGooglyEyes` — whether the entity has eyes;
- `somegoogly:eyeVariantRoll` — stable placement-variant roll;
- `somegoogly:eyeOverrides` — optional shared appearance overrides.

Related mutations flush as one full-snapshot sync. The eye-state key and variant roll are initialized together only when absent, preserving the natural-eyes decision across persistence and transfer. `EyeState.initialize` and `EyeStateSync.sendTo` skip an absent snapshot; mid-life mutations always send.

Eye item stacks carry `AppearanceOverride` in the registered `somegoogly:eye_properties` component; harvesting copies the first configured eye's effective appearance, and crafting and Slimy Eye application preserve that component while leaving other stack components untouched. Item stacks never carry placement geometry.

`EyeItemProperties` converts released 1.20.1 `minecraft:custom_data.EyeProperties` as `GooglyEyeItem` and `SlimyEyeItem` decode via `verifyComponentsAfterLoad`. A current component wins even when empty; converted or superseded old data is removed, malformed data and unrelated custom data are retained.

`EyeItemService` owns authorization, mutation, drops, and durability for Slimy Eye use and both harvest paths. Loader adapters run entity interaction after protection listeners (Forge/NeoForge `LOWEST`, Fabric a late callback phase). Applying a Slimy Eye to another player also requires server PvP and `canHarmPlayer`.

Every successful Slimy Eye application emits `GameEvent.ENTITY_INTERACT`; every successful interactive, self, or kill harvest emits `GameEvent.SHEAR`. Refused operations emit neither.

## Eligibility and behaviors

`ServerServices.onLivingEntityLoaded` owns natural eye initialization; once an entity is initialized, later configuration changes never revisit it.

`EyeBehaviors` is the ambient-behavior catalog. A `BehaviorInstance` (id, duration, seed, elapsed) is the only per-behavior state sent, and clients derive the animation from it, so no per-tick animation packets exist. `ServerBehaviorScheduler` schedules ambient behaviors for watched eyed entities and takes damage, heal, and trade triggers from loader adapters. `ClientEyeRuntime` holds transient pupil and behavior state for rendered entities and is never persisted.

## Rendering and attachment

`ClientRenderLayers` installs the normal and picker layers on living renderers and the GeckoLib layer on others, guarding against duplicates and reinstalling after renderer rebuilds: NeoForge from `AddLayers`' renderers, Forge by a dispatcher walk on `AddLayers`, Fabric by its living-renderer callback plus a post-reload dispatcher walk. `LayerGooglyEyes` must be ordered before the slime outer layer. Layers see only render states, so common Mixins (`somegoogly-common.mixins.json`, all loaders) store each entity's `EyeRenderData` decision on `LivingEntityRenderState` during extraction; the GeckoLib layer takes it in `preRender`. `ClientEyeConfigs` caches the resolved eye view per age and placement variant; `ServerEyeConfigs` is the uncached server-side path. `EyeRenderTransforms` owns render rotations; `EyePlacement` owns pupil-plane projection.

Attachment resolvers (definition token to model part or bone) cache by model identity and clear on renderer or runtime reset. `RootModelResolver` walks `EntityModel.root()` and follows the reflected Citadel, Uranus, and LLibrary resolvers. Baby models are separate instances scaled in their part poses; the picker picks an `AgeableMobRenderer`'s model by entity age.

The common `somegoogly.accesswidener` serves common compilation and every loader; Forge and NeoForge convert it to an access transformer at remap.

GeckoLib is optional: common compiles against compile-only `geckolib-common`, and `GeckoCompat` probes for it via `ModVersionLookup` before touching the typed code in `client.compat.gecko`; a failed layer attach must not block mod load.

Item definitions in `assets/somegoogly/items/` select the special model renderer `GooglyEyeItemRenderer` (3D Googly Eye) and tint source `SlimyEyeIrisTint` (third in the Slimy Eye's `tints`, matching `layer2`). NeoForge registers both by event; Forge and Fabric put them into vanilla's `ID_MAPPER`s at client init.

## Networking

Five packet classes implement Minecraft's typed `CustomPacketPayload` contract directly: eye definitions, entity eye state, behavior triggers, picker freeze, and picker export. Their ids embed network version `12`; any incompatible wire change requires bumping it. Forge and NeoForge register a required native channel/version, while Fabric checks at play join that each endpoint declared the expected versioned payload and disconnects an absent or incompatible peer.

Sends go through `@ExpectPlatform` bridges `Networking` (player, entity trackers) and client-only `ClientNetworking` (server). `NetworkTransport` is the client receive handoff, keeping client classes out of payload registration. Serverbound handlers re-check the authenticated `ServerPlayer`'s authorization. Eye-state packets carry entity id and UUID; packets preceding entity creation wait in a bounded UUID-keyed map cleared on disconnect.

## Picker and commands

Client picker code owns drafts and previews; the server owns mob freezing, spawning, movement, and world export. Spawn and mob-pose operations are server Brigadier commands; only freeze selection and client-authored export cross custom payloads. `ModelPartVocabulary` supplies one attachment grammar to live editing and bulk export.

`PickerFreezeService` preserves prior `NoAI`, reconciles locks on mob load, logout, and server stop, and permits one editor. Requests are rate-limited; spawn-all also requires creative mode, server enablement, and a server-wide cooldown. `PickerSpawnService` finalizes command-spawned mobs before setting `NoAI`, persistence, and display rotation; it and spawn suggestions obey `ServerConfig.isSpawnExcluded`. World export is confined to the generated datapack and requires creative plus permission level 2; export-all stays under the game-directory export tree.

The client and server own disjoint branches of one `/sg` Brigadier tree: local editing stays client-side, while admin, spawn, spawn-all, and mob-pose commands are server-side. Fabric explicitly forwards those server branches because its matching client root otherwise captures them.

## Loader integration

Fabric Mixins cover persistent-data migration, heal reactions, trades, shears-kill drops, and renderer reload, which lack Fabric API events; Mixins and the access widener target the pinned Minecraft version exactly; Fabric and common write fixed refmaps.

NeoForge: common registration runs once from the `@Mod` constructor, and client services must be attached to the correct bus (mod versus game).

Forge's required `PayloadChannel` marks payloads handled. NeoForge and Forge isolate physical-client bootstrap from dedicated-server bootstrap.

## Automated verification

`common/src/gametest/java` supplies 106 shared assertions; each loader wraps them and adds a persistence test (Fabric also migration and TOML tests): 109 on Fabric, 107 elsewhere. Each loader's `gametestJavadoc` documents its full GameTest source set, and root `generateDocs` syncs all production and GameTest Javadoc into `docs/javadoc/`. Required-client rejection, server commands, plain-shears self-damage, and actual-save migration remain manual checks.

## Operational boundaries

- Optional renderer integrations log recoverable failures once per operation and omit eyes lacking attachment geometry; third-party model changes can silently invalidate bundled tokens or geometry.
- Wire compatibility is the protocol-version number, not the display version, and pre-release data and protocol formats have no compatibility layer.
- `build-env/` is a byte-for-byte, non-input snapshot of the build files listed in `build-env.md`; mirror every edit to them there.
