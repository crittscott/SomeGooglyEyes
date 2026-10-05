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
| `common/src/main/java` | Shared gameplay, state, codecs, services, networking, rendering, picker |
| `common/src/main/resources` | Assets, recipes, eye definitions, language |
| `common/src/gametest` | Shared GameTest assertions and the structure fixture |
| `fabric/src/main` | Fabric entry points, callbacks, configuration, adapters, Mixins, metadata |
| `fabric/src/gametest` | Fabric wrappers and discovery metadata |
| `forge/src/main` | Forge bootstrap, events, native config, adapters, client integration, GeckoLib bridge, metadata, access transformer |
| `forge/src/gametest` | Forge wrappers, persistence proof, dev-mod entry point, discovery metadata |
| `neoforge/src/main` | NeoForge bootstrap, events, native config, adapters, client integration, GeckoLib bridge, metadata |
| `neoforge/src/gametest` | NeoForge wrappers, persistence proof, dev-mod entry point, discovery metadata |

Common main imports no loader or GeckoLib type; differences pass through project adapters or five `@ExpectPlatform` methods. Loader packages stay disjoint from common packages so Forge sees no split package.

Subsystem ownership: the server owns eligibility, persistent eye state, item actions, behaviors, datapack definitions, picker authorization, and world mutation; the client owns rendering, model attachment, pupil motion, inspection, and picker UI and editing state.

Registered content is declared once in `ContentRegistrar` — two items, one `DataComponentType`, one creative tab, two recipe serializers, and nothing else (no blocks, entities, menus, particles, or sounds). Optometrist is a data-driven enchantment from the common data pack. Fabric binds these handles through native registries; NeoForge and Forge bind the same handles through native deferred registers.

## Configuration and eye definitions

`ServerConfig` and `ClientConfig` hold the schema and expose validated values as `ConfigValue<T>`. Fabric and NeoForge load the world's server TOML through `ServerConfigFile`; Fabric reads client TOML directly, NeoForge uses a native CLIENT spec. Forge carries native CLIENT and SERVER specs copied into `ConfigValue<T>` on load and reload; NeoForge server stop and Forge SERVER unload restore defaults so values cannot escape their world.

Server-config keys, defaults, ranges, list validators, and section names must stay aligned between `ServerConfigFile` and `ForgeServerConfig`.

Eye definitions are server datapack resources at `data/<namespace>/eyes/*.json`, modeled by `EyeConfigModel`. Reload resolves and validates exactly one version per entity type, canonically encodes the resolved set, then atomically swaps `ServerEyeConfigs`; failure at any stage keeps the previous set. The resolved set is pushed to clients, so `ClientEyeConfigs` never selects a version itself. Size and geometry limits are enforced at three points that must stay aligned: datapack reload, picker export, and network decode.

Eligibility knobs (spawn and harvest chances, overrides, behavior toggles, spawn-all gate) live in `ServerConfig`; local visibility in `ClientConfig`.

Player-visible strings are translatable `Component`s in `assets/somegoogly/lang/en_us.json`; logs, command literals, config comments, and schema keys are untranslated.

## Entity and item state

`EyeState` is the entity-eye-state boundary; its `Snapshot` is the whole unit for server transitions, tracking sync, and client packet application, and partial updates are not a supported path. NBT access goes through `EntityPersistentData`: native persistent compounds on NeoForge and Forge, `EntityPersistentDataMixin` on Fabric.

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

`ClientRenderLayers` installs the normal and picker layers on compatible living renderers, guarding against duplicates and reinstalling when renderer state is replaced. `LayerGooglyEyes` must be ordered before the slime outer layer. Layers see only render states, so common Mixins (`somegoogly-common.mixins.json`, all loaders) store each entity's `EyeRenderData` decision on `LivingEntityRenderState` during extraction; the GeckoLib layer takes it in `preRender`. `ClientEyeConfigs` caches the resolved eye view per age and placement variant; `ServerEyeConfigs` is the uncached server-side path. `EyeRenderTransforms` owns render rotations; `EyePlacement` owns pupil-plane projection.

Attachment resolvers (definition token to model part or bone) cache by model identity and clear on renderer or runtime reset. `RootModelResolver` walks `EntityModel.root()` and follows the reflected Citadel, Uranus, and LLibrary resolvers. Baby models are separate instances scaled in their part poses; the picker picks an `AgeableMobRenderer`'s model by entity age.

The common `somegoogly.accesswidener` serves common compilation, Fabric, and NeoForge, which converts it to an access transformer at remap. Forge carries its own `META-INF/accesstransformer.cfg`; `verifyCommonAccessMirror`, wired into `check`, fails when it lacks any widener entry.

GeckoLib is optional: common code goes through the `GeckoCompat` bridge, which probes for GeckoLib before touching typed code, and a failed layer attach must not block mod load. The typed GeckoLib layer and bone code is one shared source tree at `gecko/src/main/java`, `srcDir`-ed into every loader's main sourceSet; only `GeckoCompatImpl` stays per-loader.

Item definitions in `assets/somegoogly/items/` select the special model renderer `GooglyEyeItemRenderer` (3D Googly Eye) and tint source `SlimyEyeIrisTint` (third in the Slimy Eye's `tints`, matching `layer2`). NeoForge registers both by event; Forge and Fabric put them into vanilla's `ID_MAPPER`s at client init.

## Networking

Five packet classes implement Minecraft's typed `CustomPacketPayload` contract directly: eye definitions, entity eye state, behavior triggers, picker freeze, and picker export. Their ids embed network version `12`; any incompatible wire change requires bumping it. Forge and NeoForge register a required native channel/version, while Fabric checks at play join that each endpoint declared the expected versioned payload and disconnects an absent or incompatible peer.

`NetworkTransport` contains only sends and client receive handoff; `NetworkTracking` abstracts loader-specific tracking-player fanout. Serverbound handlers re-check the authenticated `ServerPlayer`'s authorization. Eye-state packets carry entity id and UUID; packets preceding entity creation wait in a bounded UUID-keyed map cleared on disconnect.

## Picker and commands

Client picker code owns drafts and previews; the server owns mob freezing, spawning, movement, and world export. Spawn and mob-pose operations are server Brigadier commands; only freeze selection and client-authored export cross custom payloads. `ModelPartVocabulary` supplies one attachment grammar to live editing and bulk export.

`PickerFreezeService` preserves prior `NoAI`, reconciles locks on mob load, logout, and server stop, and permits one editor. Requests are rate-limited; spawn-all also requires creative mode, server enablement, and a server-wide cooldown. `PickerSpawnService` finalizes command-spawned mobs before setting `NoAI`, persistence, and display rotation; it and spawn suggestions obey `ServerConfig.isSpawnExcluded`. World export is confined to the generated datapack and requires creative plus permission level 2; export-all stays under the game-directory export tree.

The client and server own disjoint branches of one `/sg` Brigadier tree: local editing stays client-side, while admin, spawn, spawn-all, and mob-pose commands are server-side. Fabric explicitly forwards those server branches because its matching client root otherwise captures them.

## Loader integration

Fabric Mixins cover persistent data, reactions, trades, shears-kill drops, and renderer reload where callbacks are absent; Mixins and the access widener target the pinned Minecraft version exactly; Fabric and common write fixed refmaps.

NeoForge: common registration runs once from the `@Mod` constructor, and client services must be attached to the correct bus (mod versus game).

Forge's required `PayloadChannel` uses network version 12 and marks payloads handled.

NeoForge and Forge both isolate physical-client bootstrap from dedicated-server bootstrap.

## Automated verification

`common/src/gametest/java` supplies 107 shared assertions; each loader wraps them and adds one persistence test, totaling 108. Each loader's `gametestJavadoc` documents its full GameTest source set, and root `generateDocs` syncs all production and GameTest Javadoc into `docs/javadoc/`. Required-client rejection, server commands, plain-shears self-damage, and actual-save migration remain manual checks.

## Operational boundaries

- Optional renderer integrations log recoverable failures once per operation and omit eyes lacking attachment geometry; third-party model changes can silently invalidate bundled tokens or geometry.
- Wire compatibility is the protocol-version number, not the display version, and pre-release data and protocol formats have no compatibility layer.
- `build-env/` is a byte-for-byte, non-input snapshot of the build files listed in `build-env.md`; mirror every edit to them there.
