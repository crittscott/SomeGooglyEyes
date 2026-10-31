<!-- FIXED HEADER: not content. Do not edit, trim, or count toward the size limit. -->
> **Orientation snapshot, not a specification.** Describes the code as it currently is; the code wins on any disagreement. See CLAUDE.md § Orientation files.
<!-- END FIXED HEADER -->

# Some Googly Eyes — code orientation

Structure, ownership, state, loader seams, and invariants; `orientation-player.md` covers behavior.

## Project shape

Identity: mod id `somegoogly`, package `com.github.crittscott.somegoogly`, version `0.8.3`, Java 21, Minecraft 1.21.1.

`common` is transformed into all three loader artifacts.

| Source tree | Responsibility |
| --- | --- |
| `common/src/main/java` | Shared gameplay, state, services, networking, rendering, picker, GeckoLib, Mixins |
| `common/src/main/resources` | Assets, recipes, eye definitions, language |
| `common/src/gametest` | Shared GameTest assertions and the structure fixture |
| `fabric/src/main` | Fabric entry points, callbacks, configuration, adapters, Mixins, metadata |
| `fabric/src/gametest` | Fabric wrappers and discovery metadata |
| `forge/src/main` | Forge bootstrap, events, native config, adapters, client integration, metadata |
| `forge/src/gametest` | Forge wrappers, persistence proof, dev-mod entry point, discovery metadata |
| `neoforge/src/main` | NeoForge bootstrap, events, native config, adapters, client integration, metadata |
| `neoforge/src/gametest` | NeoForge wrappers, persistence proof, dev-mod entry point, discovery metadata |

Common imports no loader type (adapters or `@ExpectPlatform`); loader packages avoid split packages.

The server owns gameplay and authorization; the client owns rendering and picker UI.

`ModContent` binds two items, the appearance and head-eyes components, creative tab, and recipe serializers through each loader's `ContentRegistrar`. Optometrist and `self_shear` are data-driven enchantment and damage-type definitions.

## Configuration and eye definitions

`ServerConfig`/`ClientConfig` expose validated `ConfigValue<T>`s. Forge/NeoForge use native CLIENT/SERVER specs and reset SERVER values on unload. Fabric uses bundled NightConfig TOML at startup and `/reload`, resets on stop, reads declared tables, preserves authored raw values and comments during missing-key completion, and backs up rewrites. Malformed input preserves loaded values. `FabricConfigMigration` quotes released spaced headers. Sections retain 0.8.2's names. `ServerServices.onServerConfigApplied` logs the effective master switch at startup and changes.

Server key order must match across the three loaders' server configs.

Datapack definitions at `data/<namespace>/eyes/*.json` use `EyeConfigModel`. Reload resolves versions, validates, encodes to NBT, and swaps `ServerEyeConfigs` only when the encoding changes, keeping the old set on failure. Reload, export, and network decode share geometry limits; reload reserves the maximum supported behavior block in its sync budget. `EyeConfigLimits.Issue` gives translated and log diagnostics. `EyeColor` codecs reject out-of-range channels.

## Entity and item state

`EyeState` is the entity-eye-state boundary; its `Snapshot` is the whole unit for transitions, sync, and packet application. NBT access goes through `EntityPersistentData`: native persistent compounds on NeoForge and Forge, a persistent attachment on Fabric. On load, `FabricEntityDataMigration` moves the `somegoogly:persistentData` compound the 1.20.1 and 1.21.1 releases saved into it; a non-compound value is set aside there and logged.

Persistent entity keys: `somegoogly:hasGooglyEyes`, `somegoogly:eyeVariantRoll`, `somegoogly:eyeOverrides`.

Mutations flush one full snapshot. Rolls read clamped; eye state and roll initialize together only when absent. `EyeState.initialize` and `sendTo` skip absent snapshots; mid-life mutations send. Conversion and non-death respawn call `EyeState.copy`; joining players receive their own snapshot.

Eye items carry only `AppearanceOverride` in the synced `somegoogly:eye_properties` component. Harvesting copies the first eye's effective appearance; modifier recipes change that component. The Slimy Eye recipe copies every component.

`SkullEyeState` stores a placed head's snapshot in Mixin-owned `somegoogly:data`, replaced on load and included in update tags. Head items use the `somegoogly:head_eyes` component, exchanged implicitly by the block entity. Heads borrow adult variants with eyes on their copied part (`HEAD_PART_TOKENS`, else head). Vanilla types and Heads block IDs (`HEADS_MOBS`) map to mobs without linking that optional mod. Mutations send a block update. Native loot adapters delegate to `copyToLoot`.

For 1.20.1 saves, `EyeItemMigration` converts `EyeProperties` custom data during `verifyComponentsAfterLoad`: current components win; undecodable data stays and is logged.

`EyeItemService` owns authorization, mutation, drops, and durability for Slimy Eye use and both harvests, on mobs and heads. Loader adapters run entity interaction, block use, and self-shearing after default protection listeners (Forge/NeoForge `LOWEST`, Fabric a late callback phase); head use honors native item-use denial and checks `mayInteract`/`mayUseItemAt`. `markHeadDrop` stamps an eyed mob's own head among its death drops (Forge/NeoForge at `LOWEST`); head loot copies the state to the item (Forge/NeoForge `HeadEyesLootModifier`, data in each native loader; Fabric `LootTableEvents.MODIFY` adding `copy_components`). Fabric's adapter refuses spectators. Shears are `#somegoogly:shears` (vanilla plus optional `#c:tools/shear`). Applying a Slimy Eye to another player also requires server PvP and `canHarmPlayer`. Plain-shears self-damage uses the attacker-less `self_shear` type.

Successful Slimy Eye applications emit `GameEvent.ENTITY_INTERACT` (`BLOCK_CHANGE` on a head), successful harvests `GameEvent.SHEAR`; refusals emit neither.

## Eligibility and behaviors

`ServerServices.onLivingEntityLoaded` owns natural eye initialization; later configuration changes never revisit an initialized entity.

`EyeBehavior` catalogs one-shot expressions; `OngoingBehavior` the ongoing states, each with its server switch. A `BehaviorInstance` (id, duration, seed, elapsed) is the only per-behavior state sent. `ServerBehaviorScheduler` schedules ambient behaviors for watched eyed mobs and takes damage, heal, trade, milking, shearing, and entity-event triggers; it starts nothing on a sleeper. `GooglyTracker` folds in each switched-on `OngoingBehavior`; only sleep blocks behaviors, switch or not. `HeldEye` starts the held item's idle, hurt, and heal behaviors per the synced `BehaviorSettings`. `SkullEyes` derives a head's ambient behaviors on each client from its position and game time, in slots of the synced ambient mean. Server-only anger (`NeutralMob`, piglin `ANGRY_AT`) is sent on change and to each new viewer. `ClientEyeRuntime` owns entity-lifetime received expressions and anger independently of disposable pupil trackers; expression age advances on unpaused ticks even offscreen. Render resets retain reports; entity removal drops its reports, and level replacement/disconnect clears all. Head simulations are cached by position. Automatic scheduling pauses under the master switch, preserving watchers and expiring effects; resumption refreshes viewers. Explicit authoring triggers remain available. Client behavior settings start and clear inactive until synchronization.

## Rendering and attachment

`ClientRenderLayers` installs eye layers once per renderer rebuild, before slime's outer layer; GeckoLib uses `CompileRenderLayers`. Both draw picker previews through `EyeRenderData`. `ClientEyeConfigs` caches resolved views. `GooglyEyeRenderer` draws static and simulated eyes; `ClientLifecycle` ticks the first-person held eye. `EyePlacement.orientation` owns rendering and pupil-plane rotation.

Resolvers cache by model identity, clear on renderer/runtime reset, and replay `ThirdPartyModelWraps`; `Resolvers` selects the model family, with `ChildMapResolver` as its non-null fallback.

Common client Mixins (`somegoogly-common.mixins.json`) bracket a placed head's draw (`SkullBlockRendererMixin`) and a head item's (`HeadItemRendererMixin` on the built-in item renderer); `SkullEyeRenderer` draws after `renderSkull`'s model draw on the widened `SkullModel`/`PiglinHeadModel` head part or a Heads model's reflected `head`, mapping its mob part's largest cube onto the Heads cube (`Placement`). Worn heads get no eyes.

The common `somegoogly.accesswidener` serves common compilation and every loader; Forge and NeoForge convert it to an access transformer at remap.

GeckoLib is optional: common compiles against compile-only `geckolib-common`, touching `client.compat.gecko` only once `GeckoCompat` finds it; a failed attach can't block load.

`GooglyEyeItemRenderer` draws the 3D Googly Eye; `SLIMY_EYE_IRIS_TINT_INDEX` must match the Slimy Eye model's `layer2`.

## Networking

Six `CustomPacketPayload` classes: eye definitions, entity eye state, behavior triggers, anger, picker freeze, and picker export. Their ids, in `NetworkHandler`, embed network version `17`; any incompatible wire change requires bumping it. Forge and NeoForge register a required native channel version; Fabric disconnects, during configuration, a client that cannot receive its `Handshake` payload.

The eye-definition packet also carries `googlyEyesEnabled` and `BehaviorSettings`. Joins always receive it; after each datapack reload and Forge/NeoForge server-config apply, `ServerServices.broadcastEyeConfigsIfChanged` resends it only if the set, switch, or behavior settings changed, since it resets client trackers.

`Networking`/`ClientNetworking` use `@ExpectPlatform`; clientbound handlers link `ClientNetworkHandler` only in lambdas. Serverbound handlers recheck authorization; client export enforces vanilla's payload cap. Eye state follows entity spawn by ID.

## Picker and commands

The client owns picker drafts and previews; the server owns freezing, spawning, movement, and export. `PickerState.DraftSession` keeps drafts by species and age; export-all gets separate adult and baby replacements. `ModelPartVocabulary` supplies one attachment grammar to live editing and bulk export.

`PickerFreezeService` restores prior `NoAI` on release, load, logout, or stop and permits one editor. `PickerGate` owns creative/operator authorization and throttles. `LookTarget` caps extended entity rays at blocking surfaces; inspection compares actual hit locations. `PickerSpawnService` finalizes through `MobSpawning`, inserts each entity/passenger with checked results, discards partial additions on refusal, and applies planned terrain only after acceptance. Its `refusal` also filters suggestions. World export writes a datapack; bulk export writes under the game directory and logs disk failures. Both use `ConfigFile.exportVersion` and `overlay`, replacing authored ages while retaining untouched definitions, including disabled ones; copied tokens are canonicalized.

Client and server own disjoint `/sg` branches: editing is client-side; admin, spawn, spawn-all, and mob-pose are server-side. Fabric forwards those server branches explicitly; its client root would capture them.

## Loader integration

Fabric Mixins cover data migration, hurt and heal, trades, shears-kill and head drops, and client level replacement (no Fabric API events); common Mixins (`somegoogly-common-server.mixins.json`) report milking, sheep shearing, and entity events and hold head eyes. All target the pinned Minecraft version; Fabric and common write fixed refmaps.

NeoForge registers from the `@Mod` constructor, client listeners on the right bus.

Forge's required `PayloadChannel` marks payloads handled. NeoForge and Forge keep client bootstrap apart from the dedicated server's.

## Automated verification

Loader GameTest entry points: Fabric 142, Forge 140, Neoforge 142. Review regressions have scenarios; visuals, connections, artifacts, and released saves require manual checks.

## Operational boundaries

- Optional renderer integrations log failures once.
- `build-env/` is not a build input but a snapshot (see `orientation-build.md`); mirror every edit to a file it covers.
