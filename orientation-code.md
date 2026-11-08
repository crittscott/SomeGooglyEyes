<!-- FIXED HEADER: not content. Do not edit, trim, or count toward the size limit. -->
> **Orientation snapshot, not a specification.** Describes the code as it currently is; the code wins on any disagreement. See CLAUDE.md § Orientation files.
<!-- END FIXED HEADER -->

# Some Googly Eyes — code orientation

## Project shape

Identity: mod id `somegoogly`, package `com.github.crittscott.somegoogly`, version `0.8.3`, Java 17, Minecraft 1.20.1.

`common` is transformed into both loader artifacts.

| Source tree | Responsibility |
| --- | --- |
| `common/src/main/java` | Shared gameplay, state, codecs, services, networking, rendering, picker, GeckoLib integration, Mixins |
| `common/src/main/resources` | Assets, recipes, eye definitions, language |
| `common/src/gametest` | Shared GameTest assertions and the structure fixture |
| `fabric/src/main` | Fabric entry points, callbacks, configuration, adapters, Mixins, metadata |
| `fabric/src/gametest` | Fabric wrappers and discovery metadata |
| `forge/src/main` | Forge bootstrap, events, native config, adapters, client integration, metadata |
| `forge/src/gametest` | Forge wrappers, persistence proof, dev-mod entry point, discovery metadata |

Server owns gameplay/authorization; client owns rendering/picker UI.

`ModContent` binds items, Optometrist, a creative tab, and recipe serializers via loader registrars. `SlimyEyeRecipe` copies eye NBT.

## Configuration and eye definitions

`ServerConfig`/`ClientConfig` each declare a setting once, as a `ConfigValue` holding its section, key, comment, default, and limits; declaration order is file order, and both loaders build their file and schema from `settings()`. Forge applies native CLIENT/SERVER specs on load/reload and resets SERVER on unload. Fabric uses bundled NightConfig TOML at startup and `/reload`, resets on stop, reads declared tables, preserves raw values and comments during missing-key completion, and backs up rewrites. Malformed input preserves loaded values. `FabricConfigMigration` quotes released spaced headers, keeping the original as `.0.8.2.bak`. Sections retain 0.8.2's names. `ServerServices.onServerConfigApplied` and `ClientConfig.onApplied` log the deciding settings at startup and on change.

Eye definitions (`data/<namespace>/eyes/*.json`, `EyeConfigModel`) reload by resolving one version per entity type, validating, and NBT-encoding the set; `ServerEyeConfigs` swaps only if the encoding differs, keeping the old set on failure. Sync GZIP-compresses that NBT, capped at 900 KiB transmitted, 2 MiB decoded, and 8,192 eyes, so `ClientEyeConfigs` never selects versions. Size and geometry limits stay aligned across reload, picker export, and network decode; `EyeColor` codecs reject out-of-range channels.

## Entity and item state

`EyeState` serves living entities and supported vanilla boats; its `Snapshot` is the whole unit for transitions, sync, and packet application, never partial. NBT access goes through `EntityPersistentData`: Forge's native persistent compound, a persistent attachment on Fabric, where `find` reads without creating it. On load, `FabricMigration` moves the `somegoogly:persistentData` compound the 0.8.2 Fabric release saved into it; a non-compound value is set aside there and logged.

Persistent entity keys:

- `somegoogly:hasGooglyEyes` — whether the entity has eyes;
- `somegoogly:eyeVariantRoll` — stable placement-variant roll;
- `somegoogly:eyeOverrides` — optional shared appearance overrides.

The eye-state key and roll are initialized together only when absent, keeping the natural-eyes decision. `initialize` and `sendTo` skip a default snapshot; mid-life mutations always send. Loader hooks call `EyeState.copy` on mob conversion and non-death respawn; a player joining a level gets their own snapshot.

Placed heads save a `Snapshot` under `somegoogly:data` only while eyed; load keeps only its keys and clears it when absent. Block updates sync it, loot copies it to `BlockEntityTag`, and placement restores it. Vanilla heads map by type, Heads and Twilight Forest by ID. Optional Forge mixins add trophy storage and sync, and carry eyes through skull candle conversion; their config is non-required, so a Twilight Forest change costs eyes, not a launch. Skull, candle, and trophy draws share nested render contexts. Trophies fit mob eye layouts to their models, with a trophy-local Hydra layout.

Eye items carry `AppearanceOverride` as the `EyeProperties` compound (`EyeItemProperties`), never geometry; harvesting copies the first eye's effective appearance, and the modifier recipe changes only that compound.

`EyeItemService` owns authorization, mutation, drops, and durability for Slimy Eye use and both harvests, on mobs and heads. Adapters run use hooks after protection listeners (Forge `LOWEST`, Fabric a late phase); head use honors native item-use denial and checks `mayInteract`/`mayUseItemAt`. `markHeadDrop` stamps an eyed mob's own head among its death drops (Forge at `LOWEST`); head loot copies the state to the item (Forge `HeadEyesLootModifier`, data in Forge; Fabric `LootTableEvents.MODIFY`). `EyeItemService`'s use entry points refuse spectators. Shears are `#somegoogly:shears` (vanilla plus optional `#c:shears` and `#forge:shears`). `verdictFor` answers whether a target can have eyes on either side, for application and the inspect hint. Eyes go on or come off another player only where `canHarmPlayer` holds. Use counts in `ITEM_USED`; heads also fire `ITEM_USED_ON_BLOCK`. Plain-shears self-damage uses attacker-less `self_shear`, which PvP rules can't cancel.

Application emits `GameEvent.ENTITY_INTERACT` (`BLOCK_CHANGE` on a head), harvest `SHEAR`, refusal nothing.

## Eligibility and behaviors

`ServerServices.onLivingEntityLoaded` owns natural eye initialization; later configuration changes never revisit an initialized entity.

`EyeBehavior` catalogs expressions; `OngoingBehavior` catalogs ongoing states. `BehaviorInstance` carries id/duration/seed/elapsed. `ServerBehaviorScheduler` runs watched eyed mobs and damage/heal/trade/milking/shearing/entity-event triggers; sleepers start nothing. `GooglyTracker` applies enabled ongoing states. `HeldEye` runs first-person idle/hurt/heal expressions. `PresentationEyes` caches ambient slots for items, placed heads, and boats. Placed heads seed by position. Only paired layouts cross-eye. Slots fit the longest eligible expression or the synced mean interval, whichever is greater. Anger syncs on change and to a new viewer while angry. `ClientEyeRuntime` retains mob reports independently of disposable simulations and ages expressions offscreen. Only recently rendered trackers simulate; idle trackers expire. Heads cache by position, checked against their loaded compound when drawn; frames by entity, invalidated by changed contents when drawn. Level changes/disconnect clear all. The master switch pauses mob scheduling while retaining watchers and expiring effects; authoring triggers remain available. Client behaviors start inactive until sync.

## Rendering and attachment

`ClientRenderLayers` installs eye layers once per renderer rebuild, before slime's outer layer; GeckoLib uses `CompileRenderLayers`. Both draw picker previews through `EyeRenderData`. `LayerGooglyEyes` insets sheared adult sheep `body` eyes by wool depth. `ClientEyeConfigs` caches resolved views. `GooglyEyeRenderer` draws static and simulated eyes; `ClientLifecycle` ticks the first-person held eye. `EyePlacement.orientation` owns rendering and pupil-plane rotation.

Resolvers cache by model identity, clear on renderer/runtime reset, and replay `ThirdPartyModelWraps`; `Resolvers` selects the model family, with `ChildMapResolver` as its non-null fallback.

The common `somegoogly.accesswidener` serves common and both loaders; Forge remaps it to an access transformer.

Optional GeckoLib: `client/compat/gecko` holds the GeckoLib-typed layer and bone code; `GeckoCompat` reaches it only once GeckoLib is found; a failed attach can't block load.

`BoatEyeLayout` selects hull/raft geometry for supported vanilla boats. Boat item interactions and tracking use generic eye snapshots without living behavior scheduling. `ClientEyeRuntime.BoatSimulations` holds disposable world-clock simulations, replaced when a differing snapshot arrives. `PresentationEyes` supplies presentation layouts, ambient expressions, and owner-motion inputs. `HandItemEyes` keys world-timed state by holder/hand; `GuiItemEyes` owns view keys and UI time. Hand, GUI, and frame draws share `ClientEyeRuntime`'s draw stack. GUI Mixins select slots/cursor. `ClientEyeRuntime.sweep` is the eviction and simulation rule every eye cache shares, each on its own clock.

## Networking

Seven `Payload` classes: eye definitions, eye settings, entity eye state, behavior triggers, anger, picker freeze, and picker export. Each declares its id; `NetworkProtocol` holds network version `19`; bump it on wire changes. Forge registers each on a `SimpleChannel` under a fixed id, requiring that version; Fabric's `HANDSHAKE` login query refuses a client that doesn't understand it or replies with another version; its receivers decode off-thread.

`EyeSettingsPacket` carries `googlyEyesEnabled` and `BehaviorSettings`. Joins always get it and the definitions, before the player's eye state. After each datapack reload or Forge server-config apply, `ServerServices.broadcastEyeSyncIfChanged` resends each only on a change; only new definitions reset client trackers.

Sends go through `@ExpectPlatform` `Networking` and client-only `ClientNetworking`; clientbound handlers reach `ClientNetworkHandler` only inside lambdas, so servers never link it. Serverbound handlers re-check authorization; the client refuses a picker export over vanilla's serverbound cap. Eye-state, behavior, and anger packets reach trackers and the entity; eye state follows the entity's spawn, carrying only its id.

## Picker and commands

Client owns picker drafts/previews; server owns freezing/spawning/movement/export. `ModelPartVocabulary` supplies one attachment grammar to editing and bulk export. `PickerState.DraftSession` keeps drafts by type and age, seeded once; both exports `ConfigFile.overlay` authored ages onto untouched ones (disabled buckets included), canonicalizing only copied base tokens. `EyeConfigLimits.Issue` gives translatable feedback and log diagnostics.

`PickerFreezeService` restores prior `NoAI` on release, load, logout, or stop and permits one editor. `PickerGate` owns creative/operator authorization and throttles. `LookTarget` caps extended entity rays at blocking surfaces; inspection compares actual hit locations. `PickerSpawnService` finalizes through `MobSpawning`, checks each mob/passenger insertion result and rolls back partial additions on refusal, and applies planned terrain only after acceptance. Its `refusal` also filters suggestions. World export writes a datapack; bulk export writes under the game directory and logs disk failures. Both use `ConfigFile.exportVersion`/`overlay`, replacing authored ages, preserving untouched/disabled definitions, and canonicalizing copied tokens.

`/sg` editing is client-side; admin/spawn/mob-pose are server-side. Fabric forwards `SERVER_BRANCHES`.

## Loader integration

Shared Mixins (`somegoogly-common.mixins.json`) cover milking, sheep shearing, mooshroom shearing (`shear`, and Forge's `shearInternal`), server-broadcast entity events, item-eye contexts, boats, and holding and drawing head eyes. Fabric's own cover data migration, hurt (on `LivingEntity` and `Player`) and heal, lightning conversion, trades, shears-kill and head drops, and level changes.

Forge registers from `@Mod`, keeping client bootstrap apart from the dedicated server's, with client listeners on the right bus. Forge GameTests use the unprefixed `empty` template.

## Automated verification

GameTest entry points: Fabric 192, Forge 196, including three that require Twilight Forest. Client checklists: `docs/*-eyes-testing.md`. Visuals, multiplayer, protection, commands, and real saves need manual checks.

## Operational boundaries

- Optional renderers log failures once.
- `build-env/` mirrors build inputs (see `orientation-build.md`); update it with them.
