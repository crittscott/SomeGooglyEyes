<!-- FIXED HEADER: not content. Do not edit, trim, or count toward the size limit. -->
> **Orientation snapshot, not a specification.** Describes the code as it currently is; the code wins on any disagreement. See CLAUDE.md § Orientation files.
<!-- END FIXED HEADER -->

# Some Googly Eyes — code orientation

Structure, ownership, state, loader seams, and invariants; `orientation-player.md` covers behavior.

## Project shape

Identity: mod id `somegoogly`, package `com.github.crittscott.somegoogly`, version `0.8.3`, Java 17, Minecraft 1.20.1.

The Gradle project has three modules; `common` is transformed into both loader artifacts.

| Source tree | Responsibility |
| --- | --- |
| `common/src/main/java` | Shared gameplay, state, codecs, services, networking, rendering, picker, GeckoLib gate |
| `gecko/src/main/java` | GeckoLib-typed layer and bone code, compiled by each loader |
| `common/src/main/resources` | Assets, recipes, eye definitions, language |
| `common/src/gametest` | Shared GameTest assertions and the structure fixture |
| `fabric/src/main` | Fabric entry points, callbacks, configuration, adapters, Mixins, metadata |
| `fabric/src/gametest` | Fabric wrappers and discovery metadata |
| `forge/src/main` | Forge bootstrap, events, native config, adapters, client integration, metadata |
| `forge/src/gametest` | Forge wrappers, persistence proof, dev-mod entry point, discovery metadata |

Common main imports no loader type; differences pass through adapters or `@ExpectPlatform` methods. Loader packages stay disjoint from common packages so Forge sees no split package.

The server owns eligibility, eye state, item actions, behaviors, definitions, picker authorization, and world mutation; the client owns rendering, attachment, pupil motion, inspection, and picker UI.

`ModContent` declares registered content, bound via each loader's `ContentRegistrar`: two items, the Optometrist enchantment, a creative tab, and the modifier and Slimy Eye recipe serializers; `SlimyEyeRecipe` is a shapeless recipe that copies all of the eye's NBT onto its result. `self_shear` is a data-driven damage type from the common data pack.

## Configuration and eye definitions

`ServerConfig` and `ClientConfig` hold keys, defaults, ranges, validators, and comments, exposing validated `ConfigValue<T>`s; `ConfigValue.Parsed` lists rebuild a parsed view on assignment. Forge uses native CLIENT and SERVER specs copied in on load and reload; SERVER unload restores defaults so values cannot escape their world. Fabric's own `TomlConfig` reads both files, the server file at start and each `/reload`, resetting on stop.

Server key order must match across the two loaders' server configs.

Eye definitions are datapack resources at `data/<namespace>/eyes/*.json`, modeled by `EyeConfigModel`. Reload resolves and validates one version per entity type, encodes the set to NBT, and atomically swaps `ServerEyeConfigs` only if that encoding differs; failure keeps the previous set. The stored encoding is the sync payload. The resolved set is pushed to clients, so `ClientEyeConfigs` never selects a version itself. Size and geometry limits are enforced at three points that must stay aligned: datapack reload, picker export, and network decode; reload also refuses a set whose sync payload exceeds the byte limit. `EyeColor` codecs reject out-of-range channels on decode.

## Entity and item state

`EyeState` is the entity-eye-state boundary; its `Snapshot` is the whole unit for server transitions, tracking sync, and client packet application, and partial updates are not a supported path. NBT access goes through `EntityPersistentData`: Forge's native persistent compound, a persistent attachment on Fabric. On load, `FabricEntityDataMigration` moves the `somegoogly:persistentData` compound the 0.8.2 Fabric release saved into it; a non-compound value is set aside there and logged.

Persistent entity keys:

- `somegoogly:hasGooglyEyes` — whether the entity has eyes;
- `somegoogly:eyeVariantRoll` — stable placement-variant roll;
- `somegoogly:eyeOverrides` — optional shared appearance overrides.

Related mutations flush as one snapshot sync. A stored roll outside 0..1 reads clamped. The eye-state key and variant roll are initialized together only when absent, preserving the natural-eyes decision across persistence and transfer. `EyeState.initialize` and `EyeState.sendTo` skip an absent snapshot; mid-life mutations always send. Loader clone and conversion hooks call `EyeState.copy` on mob conversion and non-death player respawn. A player joining a level gets their own snapshot.

Eye item stacks carry `AppearanceOverride` as the `EyeProperties` NBT compound (`EyeItemProperties`); harvesting copies the first configured eye's effective appearance; the modifier recipe and Slimy Eye application change only that compound, and the Slimy Eye recipe copies all of the eye's NBT. Item stacks never carry placement geometry.

`EyeItemService` owns authorization, mutation, drops, and durability for Slimy Eye use and both harvests. Loader adapters run entity interaction after protection listeners (Forge `LOWEST`, Fabric a late callback phase). Fabric's adapter refuses spectators itself. Shears are `#somegoogly:shears` (vanilla plus optional `#c:shears` and `#forge:shears`). Applying a Slimy Eye to another player also requires server PvP and `canHarmPlayer`. Plain-shears self-damage uses the attacker-less `self_shear` type, so PvP rules cannot cancel it.

Successful Slimy Eye applications emit `GameEvent.ENTITY_INTERACT`, successful harvests `GameEvent.SHEAR`; refusals emit neither.

## Eligibility and behaviors

`ServerServices.onLivingEntityLoaded` owns natural eye initialization; later configuration changes never revisit an initialized entity.

The `EyeBehavior` enum is the behavior catalog. A `BehaviorInstance` (id, duration, seed, elapsed) is the only per-behavior state sent, and clients derive the animation; no per-tick packets exist. `ServerBehaviorScheduler` schedules ambient behaviors for watched eyed mobs and takes damage, heal, trade, milking, and entity-event triggers; it starts nothing on a sleeping entity. `GooglyTracker` derives the sleep and goat-ram squint, creeper rattle, aggressive red pupils, and creepy enderman pulse from synced state; only sleep blocks behaviors. Server-only anger (`NeutralMob`, piglin `ANGRY_AT`) is checked per tick over the active set and sent on change. `ClientEyeRuntime` holds transient pupil and behavior state for rendered entities.

## Rendering and attachment

`ClientRenderLayers` installs `LayerGooglyEyes` on every living renderer, duplicate-safe, after each rebuild via each loader's renderer hook; GeckoLib renderers get theirs from GeckoLib's entity `CompileRenderLayers` event. `LayerGooglyEyes` must precede the slime outer layer; both layers draw the picker preview for the picker's target. Each layer takes the entity's `EyeRenderData` decision when it renders the entity. `ClientEyeConfigs` caches the resolved view per age and variant. `GooglyEyeRenderer.drawEye` draws every eye (mobs, previews, item); the held item's pupil is one local-player simulation ticked by `ClientLifecycle` and drawn in first person only; `EyePlacement.orientation` is the one eye rotation, for rendering and pupil-plane projection.

Attachment resolvers cache by model identity, clear on renderer or runtime reset, and replay `ThirdPartyModelWraps` transforms first, choosing the age branch from `model.young`. `Resolvers` picks a family per model: `HierarchicalResolver`, `AgeableListResolver` (replaying its baby head and body transforms), `RabbitLlamaResolver`, `ChildMapResolver`, `TwilightForestResolver`, and the reflected `AdvancedModelBoxResolver` and `LLibraryResolver`.

The common `somegoogly.accesswidener` serves common compilation and both loaders; Forge converts it to an access transformer at remap.

GeckoLib is optional and has no common artifact for 1.20.1: the typed code in `gecko/src/main/java` is `srcDir`-ed into each loader, which compiles it against its own compile-only GeckoLib; common reaches bone enumeration through `GeckoCompat`'s `@ExpectPlatform` `enumerateBones`, only after `GeckoCompat` finds GeckoLib, and a failed attach must not block load.

`GooglyEyeItemRenderer` draws the 3D Googly Eye; `SLIMY_EYE_IRIS_TINT_INDEX` must match the Slimy Eye model's `layer2`.

## Networking

Six `Payload` classes (id plus `write`/`read` body): eye definitions, entity eye state, behavior triggers, anger, picker freeze, and picker export. Their ids, in `NetworkHandler`, embed network version `15`; any incompatible wire change requires bumping it. Forge registers one typed message per payload on a `SimpleChannel` whose required version is the network version; Fabric sends a login query on its `HANDSHAKE` channel and disconnects a client that does not understand it. Fabric's receivers decode off-thread, then hand off.

The eye-definition packet also carries `googlyEyesEnabled`. Joins always receive it; after each datapack reload and Forge server-config apply, `ServerServices.broadcastEyeConfigsIfChanged` resends it only if the set or switch changed, since it resets client trackers.

Sends go through `@ExpectPlatform` bridges `Networking` and client-only `ClientNetworking`; clientbound handlers reach `ClientNetworkHandler` only inside lambdas, so servers never link it. Serverbound handlers re-check the authenticated `ServerPlayer`'s authorization; the client refuses a picker export over vanilla's serverbound cap. Eye-state, behavior, and anger packets reach trackers and the entity itself; eye state follows the entity's spawn, so it carries only the id and an unknown id is ignored.

## Picker and commands

The client owns picker drafts and previews; the server owns freezing, spawning, movement, export. `ModelPartVocabulary` supplies one attachment grammar to live editing and bulk export.

`PickerFreezeService` preserves prior `NoAI`, reconciles locks on mob load, logout, and server stop, and allows one editor. `PickerGate.mayUsePicker` (creative plus permission level 2, like `/summon`) is the one picker rule, server and client. `PickerGate` owns every picker throttle; spawn-all also requires server enablement. `PickerSpawnService` finalizes command-spawned mobs through `MobSpawning` (the loader's finalize-spawn event on Forge; a cancelled spawn is not added) before setting `NoAI`, persistence, and display rotation; its `refusal` (ender dragon, unsummonable, `ServerConfig.isSpawnExcluded`) is the one spawnability rule for both commands and spawn suggestions. World export stays in the generated datapack; export-all under the game-directory export tree. Both use `ConfigFile.exportVersion`.

Client and server own disjoint `/sg` branches: editing is client-side; admin, spawn, spawn-all, and mob-pose are server-side. Fabric forwards those server branches explicitly, since its client root would capture them.

## Loader integration

Fabric Mixins cover data migration, hurt and heal, trades, shears-kill drops, milking, and server-broadcast entity events; the mooshroom shear (no conversion event on either loader), milking, and entity events have Forge Mixins too, since Forge has no event for them. Mixins target the pinned Minecraft version; each loader writes its own fixed refmap.

Forge: common registration runs once from the `@Mod` constructor, client services must be attached to the correct bus (mod versus game), and physical-client bootstrap stays isolated from dedicated-server bootstrap. Forge GameTests use `@PrefixGameTestTemplate(false)` with the `empty` template.

## Automated verification

`common/src/gametest/java` holds 104 shared tests, including save/load persistence; each loader wraps them, and Fabric adds 4 migration and TOML tests. Root `generateDocs` syncs all production and GameTest Javadoc into `docs/javadoc/`. Required-client rejection, server commands, picker operator gating, plain-shears self-damage, and real-save migration are manual.

## Operational boundaries

- Optional renderer integrations log failures once.
- `build-env/` is not a build input; it is a snapshot of the build environment described in `orientation-build.md`, and every edit to a file it covers must be mirrored there.
