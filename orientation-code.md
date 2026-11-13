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

Server owns gameplay; client owns rendering and picker UI.

`ModContent` registers items, Optometrist, a creative tab, and recipe serializers through `Registration`. `SlimyEyeRecipe` copies eye NBT.

## Where decisions live

Call these; do not restate them.

| Decision | Owner |
| --- | --- |
| Entity/head can get eyes now | `EyeItemService.verdictFor` |
| Mob can wear eyes at any age | `ServerEyeConfigs.canEverWearEyes` |
| Renderer can't show eyes | `ServerEyeConfigs.isExcluded` |
| Player may change eyes | `EyeItemService.mayAffect`, `mayAlter` |
| Shears; harvest tool | `ModContent.SHEARS` tag; `EyeItemService.isHarvestTool` |
| Percent chance | `ServerConfig.roll` |
| Picker/admin permission; feedback | `PickerGate.mayUsePicker`; `tellSuccess`/`tellFailure` |
| Entity under crosshair | `LookTarget.livingInCrosshair` |
| Mod resource ids | `SomeGooglyCommon.id` |
| Attachment path syntax | `EyeConfigModel.attachmentPath`/`attachmentIndex` |
| Entity eyes; NBT read/write | `EyeState`; `EntityPersistentData.find`/`get` |
| Head eyes and loot | `SkullEyeState` (`headLootCopy`) |
| Conversion/respawn copy | `EyeState.copy` |
| Start an expression; its clock | `ServerBehaviorScheduler.trigger`, `on*`; `clock` |
| Anger | `OngoingBehavior.serverAngry` |
| Sending packets | `Networking.sendToPlayer`/`sendTrackingAndSelf`/`sendToAll` |
| Broken tool; spawn finalize | `ItemDestruction.report`; `MobSpawning.finalizeSpawn` |
| Picker-spawnable mobs | `PickerSpawnService.refusal` |
| Mod versions; integration failures | `ModVersionLookup.modVersion`; `ClientIntegrationFailures.warnOnce` |
| Eye/pupil rotation | `EyePlacement.orientation` |
| Decimals in text | `MaybeFloatArgumentType.angle`/`compact`/`position` |

## Configuration and eye definitions

`ServerConfig`/`ClientConfig` declare each setting once as a `ConfigValue`; loader schemas derive from `settings()`. `ForgeConfig` builds native CLIENT/SERVER specs, applies reloads on the server thread, and resets defaults on unload. Fabric entry points share `TomlConfig.load`, using bundled NightConfig at startup and `/reload`; missing-key completion preserves values/comments and backs up rewrites. `FabricMigration.quoteReleasedHeader` retains released names and a `.0.8.2.bak`. Effective config is logged.

Eye definitions (`data/<namespace>/eyes/*.json`, `EyeConfigModel`) reload through decoding, version selection, validation, and NBT encoding. `ServerEyeConfigs` swaps only on encoding changes, retaining the installed set on failure; that set owns its prebuilt sync packet. GZIP sync caps compressed/decoded size and total eyes before client install. Reload, picker export, and decoding share geometry limits; color codecs reject invalid channels.

## Entity and item state

`EyeState` serves living entities and supported vanilla boats; its `Snapshot` is the whole unit for transitions, sync, and packet application, never partial. `EntityPersistentData` is Forge's persistent compound or a Fabric persistent attachment. On load, `FabricMigration` moves the `somegoogly:persistentData` compound the 0.8.2 Fabric release saved into it; a non-compound value is set aside there and logged.

Persistent entity keys:

- `somegoogly:hasGooglyEyes` — whether the entity has eyes;
- `somegoogly:eyeVariantRoll` — stable placement-variant roll, stored only with eyes;
- `somegoogly:eyeOverrides` — optional shared appearance overrides.

The eye-state key and roll are initialized together only when absent, keeping the natural-eyes decision. `initialize` and `sendTo` skip a default snapshot; mid-life mutations always send. `EyeState.copy` runs on mob conversion and non-death respawn; a player joining a level gets their own snapshot.

Placed heads save a `Snapshot` under `somegoogly:data` only while eyed; load keeps only its keys and clears it when absent. Block updates sync it, loot copies it to `BlockEntityTag`, and placement restores it. Vanilla heads map by type, Heads and Twilight Forest by ID. Optional, non-required Forge mixins add Twilight Forest trophy storage/sync and skull-candle carry. Trophies fit mob eye layouts to their models.

Eye items carry `AppearanceOverride` as the `EyeProperties` compound (`EyeItemProperties`), never geometry; harvesting copies the first eye's effective appearance, and the modifier recipe changes only that compound.

`EyeItemService` owns authorization, mutation, drops, and durability for mob/head application and harvest. Use hooks run after protection listeners (Forge `LOWEST`, Fabric a late phase); head use honors native denial and `mayAlter`. Spectators are refused. `markHeadDrop` stamps eyed mobs' own heads; placed-head loot goes through Forge `HeadEyesLootModifier` or Fabric `LootTableEvents.MODIFY`. `#somegoogly:shears` holds vanilla plus optional `#c:shears`/`#forge:shears`. `verdictFor` also serves prediction and inspect hints. Use awards `ITEM_USED`, heads also `ITEM_USED_ON_BLOCK`. Attacker-less `self_shear` bypasses PvP damage cancellation.

Application emits `ENTITY_INTERACT` (`BLOCK_CHANGE` on a head), harvest `SHEAR`, refusal nothing.

## Eligibility and behaviors

`ServerServices.onLivingEntityLoaded` owns natural eye initialization; later configuration changes never revisit an initialized entity.

`EyeBehavior` catalogs expressions; `OngoingBehavior` ongoing classifications. `ServerBehaviorScheduler` schedules watched eyed mobs and reacts to game events using server tick time; players watch their own eyes. Per-hand reactions need no applied eyes and send scoped triggers. Sleepers start nothing. `PresentationEyes` selects deterministic ambient slots for items, heads, and boats, fitting the longest eligible expression; cross-eye requires a partner. Anger syncs only on change and to new angry viewers. `ClientEyeRuntime` keeps reports past simulation eviction, ages them offscreen, simulates recently drawn entries, and clears on level replacement/disconnect. The master switch suppresses automatic starts; authoring triggers remain available.

## Rendering and attachment

`ClientRenderLayers` installs eye layers once per renderer rebuild, before slime's outer layer; GeckoLib uses `CompileRenderLayers`. Both draw picker previews through `EyeRenderData`. `LayerGooglyEyes` insets sheared adult sheep `body` eyes by wool depth. `ClientEyeConfigs` caches resolved views. `GooglyEyeRenderer` draws static and simulated eyes; `ClientLifecycle` advances the shared presentation runtime.

Resolvers cache by model identity, clear on renderer/runtime reset, and replay `ThirdPartyModelWraps`, which owns both whole-model and named-root-group transform decisions; `Resolvers` selects the model family, with `ChildMapResolver` as its non-null fallback.

`somegoogly.accesswidener` serves all modules; Forge remaps it to an access transformer.

Optional GeckoLib: `client/compat/gecko` holds GeckoLib-typed code, reached through `GeckoCompat` only when GeckoLib is found.

`BoatEyeLayout` selects hull/raft geometry; boats use generic snapshots without server expression scheduling. `ClientEyeRuntime` owns boat, hand, GUI, frame, head and mob maps. One nested draw context inherits GUI keys, animation and block/head-item identity. `sweep` shares eviction and simulation on world time or the separate pause-independent GUI clock.

## Networking

Seven `Payload` classes declare eye definitions/settings/state, behavior, anger, freeze and export messages. Both loaders register from `Payload.CLIENTBOUND`/`SERVERBOUND`; stable indices are explicit. `NetworkProtocol` is `21`. Forge uses a required-version `SimpleChannel`; Fabric uses a versioned login query, typed play adapters and game-thread dispatch. Broadcasts encode once.

`EyeSettingsPacket` carries `googlyEyesEnabled` and `BehaviorSettings`. Joins always get it and the definitions, before the player's eye state. After a datapack reload or Forge config apply, `ServerServices.broadcastEyeSyncIfChanged` resends only what changed; only new definitions reset client trackers.

Client sends use `ClientNetworking`; clientbound handlers reach `ClientNetworkHandler` only inside lambdas, so servers never link it. Behavior triggers carry entity/hand scope, id, seed, and elapsed time, and derive duration from the behavior. Held scopes decode only grow/swirl. Export age is checked on decode; server-dependent config validation stays in the service. Freeze/export handlers re-check authorization; releasing one's own mob is exempt; the client refuses a picker export over vanilla's serverbound cap. Entity packets reach trackers and the entity; eye state follows the entity's spawn packet, carrying only its id.

## Picker and commands

Client owns picker drafts/previews; server owns freezing/spawning/movement/export. Resolvers normalize and suffix-match `EyeConfigModel` attachment syntax, and `ModelPartVocabulary` canonicalizes editing and bulk export tokens. `PickerState.DraftSession` keeps drafts by type and age, seeded once; both exports `ConfigFile.overlay` authored ages onto untouched ones (disabled buckets included), canonicalizing only copied base tokens. `EyeConfigLimits.Issue` gives translatable feedback and log diagnostics.

`PickerFreezeService` restores prior `NoAI` on release, load, logout, or stop and permits one editor. `PickerGate` also throttles. `LookTarget` caps extended entity rays at blocking surfaces; inspection reads vanilla's crosshair target. `PickerSpawnService` finalizes through `MobSpawning`, checks each mob/passenger insertion result and rolls back partial additions on refusal, and applies planned terrain only after acceptance. Its `refusal` also filters suggestions. World export writes a datapack; bulk export writes under the game directory and logs disk failures; both use `ConfigFile.exportVersion`.

`/sg` editing is client-side; admin/spawn/mob-pose are server-side. Fabric forwards `SERVER_BRANCHES`.

## Loader integration

Shared Mixins (`somegoogly-common.mixins.json`) cover milking, sheep shearing, mooshroom shearing (`shear`, and Forge's `shearInternal`), server-broadcast entity events, item-eye contexts, boats, and holding and drawing head eyes. Fabric's own cover data migration, hurt (on `LivingEntity` and `Player`) and heal, lightning conversion, trades, shears-kill and head drops, tracking start (`START_TRACKING` precedes the spawn packet), and level changes.

Forge registers from `@Mod`, keeping client bootstrap apart from the dedicated server's, with client listeners on the right bus. Forge GameTests use the unprefixed `empty` template.

## Automated verification

GameTests: Fabric 191, Forge 195. Native use wiring, Forge refusals, clone events, real scheduler triggers/kills/spawns, and admin dispatch have GameTest coverage. Visuals, multiplayer, Fabric protection ordering, full respawn transitions, and real saves need manual checks; see `docs/picker-hud-testing.md` for picker/network scenarios.

## Operational boundaries

- `build-env/` mirrors build inputs (see `orientation-build.md`); update it with them.
