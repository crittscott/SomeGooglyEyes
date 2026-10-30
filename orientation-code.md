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

The server owns gameplay, definitions, and picker authorization; the client owns rendering, inspection, and picker UI.

`ModContent` binds two items, the appearance and head-eyes components, creative tab, and recipe serializers through each loader's `ContentRegistrar`. Optometrist and `self_shear` are data-driven enchantment and damage-type definitions.

## Configuration and eye definitions

`ServerConfig`/`ClientConfig` expose validated `ConfigValue<T>`s. Forge/NeoForge use native CLIENT/SERVER specs and reset SERVER values on unload. Fabric's `TomlConfig` reads both files, reloads server values at start and `/reload`, and resets on stop.

Server key order must match across the three loaders' server configs.

Datapack definitions at `data/<namespace>/eyes/*.json` use `EyeConfigModel`. Reload resolves versions, validates, encodes to NBT, and swaps `ServerEyeConfigs` only when the encoding changes, keeping the old set on failure. Reload, export, and network decode share geometry limits; reload also checks sync size. `EyeConfigLimits.Issue` gives translated and log diagnostics. `EyeColor` codecs reject out-of-range channels.

## Entity and item state

`EyeState` is the entity-eye-state boundary; its `Snapshot` is the whole unit for transitions, sync, and packet application. NBT access goes through `EntityPersistentData`: native persistent compounds on NeoForge and Forge, a persistent attachment on Fabric. On load, `FabricEntityDataMigration` moves the `somegoogly:persistentData` compound the 1.20.1 and 1.21.1 releases saved into it; a non-compound value is set aside there and logged.

Persistent entity keys: `somegoogly:hasGooglyEyes`, `somegoogly:eyeVariantRoll`, `somegoogly:eyeOverrides`.

Mutations flush one full snapshot. Rolls read clamped; eye state and roll initialize together only when absent. `EyeState.initialize` and `sendTo` skip absent snapshots; mid-life mutations send. Conversion and non-death respawn call `EyeState.copy`; joining players receive their own snapshot.

Eye items carry only `AppearanceOverride` in the synced `somegoogly:eye_properties` component. Harvesting copies the first eye's effective appearance; modifier recipes change that component. The Slimy Eye recipe copies every component.

`SkullEyeState` keeps a placed head's `Snapshot` (one `read`/`write`) in a compound the common `SkullBlockEntityDataMixin` adds, saved as `somegoogly:data` and replaced on every load, so it rides the update tag; a head item carries it as the `somegoogly:head_eyes` component, taken and given as an implicit block-entity component. A head borrows its mob's adult definition, rolling among variants with eyes on the part it copies (`HEAD_PART_TOKENS`, else the head part). Vanilla heads map by type, the Heads mod's by block id alone (`HEADS_MOBS`), a soft dependency; other mods' heads have none. Mutations end in a block update.

For 1.20.1 saves, `EyeItemMigration` converts `EyeProperties` custom data during `verifyComponentsAfterLoad`: current components win; undecodable data stays and is logged.

`EyeItemService` owns authorization, mutation, drops, and durability for Slimy Eye use and both harvests, on mobs and heads. Loader adapters run entity interaction, block use, and self-shearing after default protection listeners (Forge/NeoForge `LOWEST`, Fabric a late callback phase); head use also checks `mayInteract`/`mayUseItemAt`. `markHeadDrop` stamps an eyed mob's own head among its death drops (Forge/NeoForge at `LOWEST`); head loot copies the state to the item (Forge/NeoForge `HeadEyesLootModifier`, data in `common`; Fabric `LootTableEvents.MODIFY` adding `copy_components`). Fabric's adapter refuses spectators. Shears are `#somegoogly:shears` (vanilla plus optional `#c:tools/shear`). Applying a Slimy Eye to another player also requires server PvP and `canHarmPlayer`. Plain-shears self-damage uses the attacker-less `self_shear` type.

Successful Slimy Eye applications emit `GameEvent.ENTITY_INTERACT` (`BLOCK_CHANGE` on a head), successful harvests `GameEvent.SHEAR`; refusals emit neither.

## Eligibility and behaviors

`ServerServices.onLivingEntityLoaded` owns natural eye initialization; later configuration changes never revisit an initialized entity.

The `EyeBehavior` enum is the catalog. A `BehaviorInstance` (id, duration, seed, elapsed) is the only per-behavior state sent. `ServerBehaviorScheduler` schedules ambient behaviors for watched eyed mobs and takes damage, heal, trade, milking, shearing, and entity-event triggers; it starts nothing on a sleeper. `GooglyTracker` derives villager eyelids, the sleep, ram, and aiming squints, creeper rattle, red pupils, and enderman pulse from synced state; only sleep blocks behaviors. `HeldEye` starts the held item's idle, hurt, and heal behaviors; nothing is sent. `SkullEyes` derives a head's ambient behaviors on each client from its position and game time, in slots of the synced `AmbientSettings` mean. Server-only anger (`NeutralMob`, piglin `ANGRY_AT`) is sent on change and to each new viewer. `ClientEyeRuntime` holds transient pupil and behavior state for rendered entities and heads (by position, rebuilt when their snapshot changes). Entity unload, level replacement, and disconnect drop trackers and anger; renderer reload keeps anger.

## Rendering and attachment

`ClientRenderLayers` installs `LayerGooglyEyes` once per living renderer rebuild; GeckoLib uses `CompileRenderLayers`. The eye layer precedes slime's outer layer. Both use `EyeRenderData` and draw picker previews. `ClientEyeConfigs` caches by age/variant. `GooglyEyeRenderer.drawEye` draws every eye, `drawAnimatedEye` every simulated one; `ClientLifecycle` ticks `HeldEye`, drawn live in first person only. `EyePlacement.orientation` defines rendering and pupil-plane rotation.

Resolvers cache by model identity, clear on renderer or runtime reset, and replay `ThirdPartyModelWraps` first. `Resolvers` picks a family per model: `HierarchicalResolver`, `AgeableListResolver` (replaying baby transforms), `RabbitLlamaResolver`, `TwilightForestResolver`, and the reflected `AdvancedModelBoxResolver` and `LLibraryResolver`, with `ChildMapResolver` as an explicit non-null fallback.

Common client Mixins (`somegoogly-common.mixins.json`) bracket a placed head's draw (`SkullBlockRendererMixin`) and a head item's (`HeadItemRendererMixin` on the built-in item renderer); `SkullEyeRenderer` draws after `renderSkull`'s model draw on the widened `SkullModel`/`PiglinHeadModel` head part or a Heads model's reflected `head`, mapping its mob part's largest cube onto the Heads cube (`Placement`). Worn heads get no eyes.

The common `somegoogly.accesswidener` serves common compilation and every loader; Forge and NeoForge convert it to an access transformer at remap.

GeckoLib is optional: common compiles against compile-only `geckolib-common`, touching `client.compat.gecko` only once `GeckoCompat` finds it; a failed attach can't block load.

`GooglyEyeItemRenderer` draws the 3D Googly Eye; `SLIMY_EYE_IRIS_TINT_INDEX` must match the Slimy Eye model's `layer2`.

## Networking

Six `CustomPacketPayload` classes: eye definitions, entity eye state, behavior triggers, anger, picker freeze, and picker export. Their ids, in `NetworkHandler`, embed network version `16`; any incompatible wire change requires bumping it. Forge and NeoForge register a required native channel version; Fabric disconnects, during configuration, a client that cannot receive its `Handshake` payload.

The eye-definition packet also carries `googlyEyesEnabled` and `AmbientSettings`. Joins always receive it; after each datapack reload and Forge/NeoForge server-config apply, `ServerServices.broadcastEyeConfigsIfChanged` resends it only if the set, switch, or ambient settings changed, since it resets client trackers.

Sends go through `@ExpectPlatform` `Networking` and client-only `ClientNetworking`; clientbound handlers reach `ClientNetworkHandler` only inside lambdas, so servers never link it. Serverbound handlers re-check the authenticated `ServerPlayer`'s authorization; the client refuses a picker export over vanilla's serverbound cap. Eye state follows the entity's spawn, carrying only its id; unknown ids are ignored.

## Picker and commands

The client owns picker drafts and previews; the server owns freezing, spawning, movement, and export. `PickerState.DraftSession` keeps drafts by species and age; export-all gets separate adult and baby replacements. `ModelPartVocabulary` supplies one attachment grammar to live editing and bulk export.

`PickerFreezeService` restores prior `NoAI` on release, load, logout, or stop and permits one editor. `PickerGate` owns creative/permission-level-2 authorization and throttles. `PickerSpawnService` uses loader finalization via `MobSpawning`, honors cancellation, then sets persistence, `NoAI`, and rotation; its `refusal` also filters suggestions. World export writes a datapack; bulk export writes under the game directory. Both use `ConfigFile.exportVersion` and `overlay`, replacing authored age buckets while retaining untouched definitions, including disabled ones. Bulk export canonicalizes copied base tokens.

Client and server own disjoint `/sg` branches: editing is client-side; admin, spawn, spawn-all, and mob-pose are server-side. Fabric forwards those server branches explicitly; its client root would capture them.

## Loader integration

Fabric Mixins cover data migration, hurt and heal, trades, shears-kill and head drops, and client level replacement (no Fabric API events); common Mixins (`somegoogly-common-server.mixins.json`) report milking, sheep shearing, and entity events and hold head eyes. All target the pinned Minecraft version; Fabric and common write fixed refmaps.

NeoForge registers from the `@Mod` constructor, client listeners on the right bus.

Forge's required `PayloadChannel` marks payloads handled. NeoForge and Forge keep client bootstrap apart from the dedicated server's.

## Automated verification

`common/src/gametest/java` holds 133 shared tests, with persistence, plus 9 for the 1.20.1 item migration; NeoForge wraps all but one head-drop test, Fabric and Forge also skip the 2 placing Heads heads, and Fabric adds 4 migration and TOML tests. Protection vetoes, packet delivery, client unload, export feedback, required-client rejection, server commands, picker gating, self-shear damage, and real-save migration are manual.

## Operational boundaries

- Optional renderer integrations log failures once.
- `build-env/` is not a build input but a snapshot (see `orientation-build.md`); mirror every edit to a file it covers.
