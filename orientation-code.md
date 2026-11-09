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

## Configuration and eye definitions

`ServerConfig`/`ClientConfig` declare each setting once as a `ConfigValue`; loader schemas derive from `settings()`. `ForgeConfig` converts the schema to native CLIENT/SERVER specs, with server-thread reload application and defaults reset on unload. Fabric entry points share `TomlConfig.load`, using bundled NightConfig at startup and `/reload`; missing-key completion preserves values/comments and backs up rewrites. Malformed input preserves effective values. `FabricMigration.quoteReleasedHeader` retains released names and a `.0.8.2.bak`. Effective config is logged.

Eye definitions (`data/<namespace>/eyes/*.json`, `EyeConfigModel`) reload through decoding, version selection, validation, and NBT encoding. `ServerEyeConfigs` swaps only on encoding changes, retaining the installed set on failure; that set owns its prebuilt sync packet. GZIP sync caps compressed/decoded size and total eyes before client install. Reload, picker export, and decoding share geometry limits; color codecs reject invalid channels.

## Entity and item state

`EyeState` serves living entities and supported vanilla boats; its `Snapshot` is the whole unit for transitions, sync, and packet application, never partial. NBT access goes through `EntityPersistentData`: Forge's native persistent compound, a persistent attachment on Fabric, where `find` reads without creating it. On load, `FabricMigration` moves the `somegoogly:persistentData` compound the 0.8.2 Fabric release saved into it; a non-compound value is set aside there and logged.

Persistent entity keys:

- `somegoogly:hasGooglyEyes` — whether the entity has eyes;
- `somegoogly:eyeVariantRoll` — stable placement-variant roll, stored only with eyes;
- `somegoogly:eyeOverrides` — optional shared appearance overrides.

The eye-state key and roll are initialized together only when absent, keeping the natural-eyes decision. `initialize` and `sendTo` skip a default snapshot; mid-life mutations always send. Loader hooks call `EyeState.copy` on mob conversion and non-death respawn; a player joining a level gets their own snapshot.

Placed heads save a `Snapshot` under `somegoogly:data` only while eyed; load keeps only its keys and clears it when absent. Block updates sync it, loot copies it to `BlockEntityTag`, and placement restores it. Vanilla heads map by type, Heads and Twilight Forest by ID. Optional Forge mixins add trophy storage and sync, and carry eyes through skull candle conversion; their config is non-required, so a Twilight Forest change costs eyes, not a launch. Skull, candle, and trophy draws share nested render contexts. Trophies fit mob eye layouts to their models, with a trophy-local Hydra layout.

Eye items carry `AppearanceOverride` as the `EyeProperties` compound (`EyeItemProperties`), never geometry; harvesting copies the first eye's effective appearance, and the modifier recipe changes only that compound.

`EyeItemService` owns authorization, mutation, drops, and durability for mob/head application and harvest. Use hooks run after protection listeners (Forge `LOWEST`, Fabric a late phase); head use honors native denial and `mayInteract`/`mayUseItemAt`. Spectators are refused. `markHeadDrop` stamps eyed mobs' own heads; placed-head loot uses `SkullEyeState.headLootCopy` through Forge `HeadEyesLootModifier` or Fabric `LootTableEvents.MODIFY`. Shears use `#somegoogly:shears` (vanilla plus optional `#c:shears`/`#forge:shears`). `verdictFor` serves application, prediction, and inspect hints. Player eye interactions require `canHarmPlayer`. Use awards `ITEM_USED`, heads also `ITEM_USED_ON_BLOCK`. Attacker-less `self_shear` bypasses PvP damage cancellation.

Application emits `ENTITY_INTERACT` (`BLOCK_CHANGE` on a head), harvest `SHEAR`, refusal nothing.

## Eligibility and behaviors

`ServerServices.onLivingEntityLoaded` owns natural eye initialization; later configuration changes never revisit an initialized entity.

`EyeBehavior` catalogs expressions; `OngoingBehavior` owns ongoing classifications, including server anger. `ServerBehaviorScheduler` schedules watched eyed mobs and reacts to game events using server tick time; players watch their own eyes. Separate per-hand reactions work without applied eyes and send scoped triggers to holder/trackers. Sleepers start nothing. `PresentationEyes` selects deterministic ambient slots for items, heads, and boats, fitting the longest eligible expression; cross-eye requires a partner. Anger syncs only on change and to new angry viewers. `ClientEyeRuntime` retains reports independently of evicted simulations, ages them offscreen, simulates recently drawn entries, and clears connection state on level replacement/disconnect. The master switch suppresses automatic starts; authoring triggers remain available.

## Rendering and attachment

`ClientRenderLayers` installs eye layers once per renderer rebuild, before slime's outer layer; GeckoLib uses `CompileRenderLayers`. Both draw picker previews through `EyeRenderData`. `LayerGooglyEyes` insets sheared adult sheep `body` eyes by wool depth. `ClientEyeConfigs` caches resolved views. `GooglyEyeRenderer` draws static and simulated eyes; `ClientLifecycle` advances the shared presentation runtime. `EyePlacement.orientation` owns rendering and pupil-plane rotation.

Resolvers cache by model identity, clear on renderer/runtime reset, and replay `ThirdPartyModelWraps`, which owns both whole-model and named-root-group transform decisions; `Resolvers` selects the model family, with `ChildMapResolver` as its non-null fallback.

`somegoogly.accesswidener` serves all modules; Forge remaps it to an access transformer.

Optional GeckoLib: `client/compat/gecko` holds GeckoLib-typed code, reached through `GeckoCompat` only when GeckoLib is found.

`BoatEyeLayout` selects hull/raft geometry; boats use generic snapshots without server expression scheduling. `ClientEyeRuntime` owns boat, hand, GUI, frame, head and mob maps. `PresentationEyes` supplies layouts, ambient expressions and motion inputs. Holder/hand identity is shared by first-/third-person loose-eye views. One nested draw context inherits GUI keys, animation and block/head-item identity. `sweep` shares eviction and simulation on world time or the separate pause-independent GUI clock.

## Networking

Seven `Payload` classes declare eye definitions/settings/state, behavior, anger, freeze and export messages. Both loaders register from `Payload.CLIENTBOUND`/`SERVERBOUND`; stable indices are explicit. `NetworkProtocol` is `21`. Forge uses a required-version `SimpleChannel`; Fabric uses a versioned login query, typed play adapters and game-thread dispatch. Broadcasts encode once.

`EyeSettingsPacket` carries `googlyEyesEnabled` and `BehaviorSettings`. Joins always get it and the definitions, before the player's eye state. After each datapack reload or Forge server-config apply, `ServerServices.broadcastEyeSyncIfChanged` resends each only on a change; only new definitions reset client trackers.

Sends go through `@ExpectPlatform` `Networking` and client-only `ClientNetworking`; clientbound handlers reach `ClientNetworkHandler` only inside lambdas, so servers never link it. Behavior triggers carry entity/hand scope, id, seed, and elapsed time, and derive duration from the behavior. Held scopes decode only grow/swirl. Export age is checked on decode; server-dependent config validation stays in the service. Freeze/export handlers re-check authorization; releasing one's own mob is exempt; the client refuses a picker export over vanilla's serverbound cap. Eye-state, behavior, and anger packets reach trackers and the entity; eye state follows the entity's spawn packet, carrying only its id.

## Picker and commands

Client owns picker drafts/previews; server owns freezing/spawning/movement/export. `EyeConfigModel` owns raw attachment syntax; resolvers normalize and suffix-match it, and `ModelPartVocabulary` canonicalizes editing and bulk export tokens. The HUD uses translated component rows, styled arguments, and font-derived spacing. `PickerState.DraftSession` keeps drafts by type and age, seeded once; both exports `ConfigFile.overlay` authored ages onto untouched ones (disabled buckets included), canonicalizing only copied base tokens. `EyeConfigLimits.Issue` gives translatable feedback and log diagnostics.

`PickerFreezeService` restores prior `NoAI` on release, load, logout, or stop and permits one editor. `PickerGate` owns creative/operator authorization and throttles. `LookTarget` caps extended entity rays at blocking surfaces; inspection reads vanilla's crosshair target. `PickerSpawnService` finalizes through `MobSpawning`, checks each mob/passenger insertion result and rolls back partial additions on refusal, and applies planned terrain only after acceptance. Its `refusal` also filters suggestions. World export writes a datapack; bulk export writes under the game directory and logs disk failures. Both use `ConfigFile.exportVersion`/`overlay`, replacing authored ages, preserving untouched/disabled definitions, and canonicalizing copied tokens.

`/sg` editing is client-side; admin/spawn/mob-pose are server-side. Fabric forwards `SERVER_BRANCHES`.

## Loader integration

Shared Mixins (`somegoogly-common.mixins.json`) cover milking, sheep shearing, mooshroom shearing (`shear`, and Forge's `shearInternal`), server-broadcast entity events, item-eye contexts, boats, and holding and drawing head eyes. Fabric's own cover data migration, hurt (on `LivingEntity` and `Player`) and heal, lightning conversion, trades, shears-kill and head drops, tracking start (`START_TRACKING` precedes the spawn packet), and level changes.

Forge registers from `@Mod`, keeping client bootstrap apart from the dedicated server's, with client listeners on the right bus. Forge GameTests use the unprefixed `empty` template.

## Automated verification

GameTests: Fabric 191, Forge 195 (three require Twilight Forest). Native use wiring, Forge refusals, clone events, real scheduler triggers/kills/spawns, and admin dispatch have GameTest coverage. Visuals, multiplayer, Fabric protection ordering, full respawn transitions, and real saves need manual checks; see `docs/picker-hud-testing.md` for picker/network scenarios.

## Operational boundaries

- Optional renderers log failures once.
- `build-env/` mirrors build inputs (see `orientation-build.md`); update it with them.
