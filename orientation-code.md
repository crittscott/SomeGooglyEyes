<!-- FIXED HEADER: not content. Do not edit, trim, or count toward the size limit. -->
> **Orientation snapshot, not a specification.** Describes the code as it currently is; the code wins on any disagreement. See CLAUDE.md § Orientation files.
<!-- END FIXED HEADER -->

# Some Googly Eyes — code orientation

## Project shape

`somegoogly`, package `com.github.crittscott.somegoogly`, version `0.8.3`, Java 21, Minecraft 1.21.1.

`common` ships in every loader artifact.

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

Server owns gameplay/authorization; client owns rendering/picker UI.

`ModContent` registers two items, the appearance and head-eyes components, creative tab, and recipe serializers through the `@ExpectPlatform` `Registration`. Optometrist and `self_shear` are data-driven enchantment and damage-type definitions.

## Configuration and eye definitions

`ServerConfig`/`ClientConfig` expose validated `ConfigValue<T>`s. Forge/NeoForge use native CLIENT/SERVER specs and reset SERVER values on unload. Each `ConfigValue` carries its section, key, default, and comment; `settings()` drives every loader's file. Fabric uses NightConfig TOML at startup and `/reload`, resets on stop, preserves raw values/comments when completing defaults, backs up rewrites, and retains loaded values on malformed input. `FabricMigration.quoteReleasedHeader` quotes released spaced headers under its own backup; sections retain 0.8.2's names. `ServerServices.onServerConfigApplied` and `ClientConfig.onApplied` log the deciding settings when they change.

Datapack definitions at `data/<namespace>/eyes/*.json` use `EyeConfigModel`. Reload resolves versions, validates, encodes to NBT, and swaps `ServerEyeConfigs` only when the encoding changes, keeping the old set on failure. Reload, export, and network decode share geometry limits.

## Entity and item state

`EyeState` stores living-entity and supported-boat state; its `Snapshot` is the transition/sync unit. NBT access goes through `EntityPersistentData`: native persistent compounds on NeoForge and Forge, a persistent attachment on Fabric, where `find` reads without creating one. On load, `FabricMigration` moves the `somegoogly:persistentData` compound the 1.20.1 and 1.21.1 releases saved into it; a non-compound value is set aside there and logged.

Persistent entity keys: `somegoogly:hasGooglyEyes`, `somegoogly:eyeVariantRoll`, `somegoogly:eyeOverrides`.

Mutations flush one full snapshot. Rolls read clamped and are stored only while eyes are on; eye state initializes only when absent. `EyeState.initialize` and `sendTo` skip absent snapshots; mid-life mutations send. Conversions call `ServerServices.onConverted` (eyes and picker freeze); player replacement calls `onPlayerCloned`, copying eyes only on End exit. Joining players receive their own snapshot.

Eye items carry only `AppearanceOverride` in the synced `somegoogly:eye_properties` component. Harvesting copies the first eye's effective appearance; modifier recipes change that component. The Slimy Eye recipe copies every component.

Placed heads save and sync their eye snapshot under the Mixin-owned `somegoogly:data` key. Items use the `somegoogly:head_eyes` component. Optional NeoForge mixins carry eyes through Twilight Forest skull candle conversion and give trophies the same storage, item, and render lifecycle as skulls.

For 1.20.1 saves, `EyeItemMigration` converts `EyeProperties` custom data during `verifyComponentsAfterLoad`: current components win; undecodable data stays and is logged.

`EyeItemService` owns authorization, mutation, drops, and durability for Slimy Eye use and both harvests, on mobs and heads. Loader adapters run interactions and self-shearing after default protection listeners (Forge/NeoForge `LOWEST`, Fabric a late callback phase); head use honors native item-use denial and checks `mayInteract`/`mayUseItemAt`. `markHeadDrop` stamps an eyed mob's own head among its death drops (Forge/NeoForge at `LOWEST`); head loot copies the state to the item with `SkullEyeState.headLootCopy` (Forge/NeoForge `HeadEyesLootModifier`, data in each native loader; Fabric `LootTableEvents.MODIFY`). `EyeItemService` refuses spectators at every use entry and answers `verdictFor`, the eligibility the inspect hint also shows. Another player's eyes need `canHarmPlayer` (`mayAffect`). Item use counts in statistics, and application and harvesting are predicted on the client. Shears are `#somegoogly:shears` (vanilla plus optional `#c:tools/shear`). The shears-kill drop honors `doMobLoot`. Plain-shears self-damage uses the attacker-less `self_shear` type.

Applications emit `ENTITY_INTERACT` (`BLOCK_CHANGE` on heads); harvests emit `SHEAR`; refusals emit neither.

## Eligibility and behaviors

`ServerServices.onLivingEntityLoaded` owns natural eye initialization; later configuration changes never revisit an initialized entity.

`EyeBehavior` catalogs expressions; `OngoingBehavior` ongoing states. `ServerBehaviorScheduler` handles watched mobs, each player counting as their own watcher, and gameplay triggers; sleepers start nothing. `GooglyTracker` applies enabled ongoing states. The scheduler also sends held-eye hurt/heal reactions per hand (any damage). Presentations cache seeded ambient slots. Placed heads seed by position. `BoatEyeLayout` restricts boat support to vanilla registry types and woods, selects weighted variants with the matching `hull`/`raft` attachment, and filters other attachment groups. Boats receive eyes only through items; tracking sends their complete state without living behavior scheduling. `ClientEyeRuntime` owns mob, boat, head, frame, hand, and GUI simulations, retains mob and hand reports separately, and runs every cache through `sweep`: unshown simulations evict, recently shown ones step. New simulations settle on the rim toward gravity at first capture. Head caches use position and block-entity data identity, frame caches entity identity; contents and level changes invalidate them. Its one nested `DrawContext` carries GUI key, item animation, and head; Mixins push and pop it. The master switch pauses mob scheduling while retaining watchers and expiring effects. Authoring triggers remain available. Behaviors start inactive until sync.

## Rendering and attachment

`ClientRenderLayers` installs eye layers per renderer rebuild, before slime's outer layer; GeckoLib uses `CompileRenderLayers`. Both use `EyeRenderData` for previews. `LayerGooglyEyes` insets sheared sheep `body` eyes by wool depth.  `GooglyEyeRenderer` draws static and simulated eyes for every path through `renderHead`; it holds the one `ModelGooglyEye`. `EyePlacement.orientation` orients rendering and pupils.

Resolvers cache by model identity, clear on renderer/runtime reset, and replay `ThirdPartyModelWraps` whole-model and group (baby) transforms; `Resolvers` selects the model family, with `ChildMapResolver` as its non-null fallback.

Common client Mixins bracket a placed head's draw (`SkullBlockRendererMixin`) and a head item's (`HeadItemRendererMixin`); `SkullEyeRenderer` draws after `renderSkull`'s model draw on the widened `SkullModel`/`PiglinHeadModel` head part or a Heads model's reflected `head`, mapping its mob part's largest cube onto the Heads cube (`Placement`). Worn heads get no eyes.

The common `somegoogly.accesswidener` serves common compilation and every loader; Forge and NeoForge convert it to an access transformer at remap.

GeckoLib is optional: common compiles against compile-only `geckolib-common`, touching `client.compat.gecko` only once `GeckoCompat` finds it; a failed attach can't block load.

`PresentationEyes` supplies frame, GUI, hand, and boat layouts. `BoatRendererMixin` draws through `GooglyEyeRenderer.renderBoat` before the hull pose is popped, with its rocking/bubble transforms. `EyeInfo.capturePose` reads gravity and motion axes from a world-aligned pose, once per tick; the view rotation lives in the model-view matrix. `PresentationEyes` samples boat/holder motion and re-primes after gaps. Hands key by holder/hand, shared by first and third person; GUI entries by view key on a UI clock that runs while paused. GUI icons keep GUI scale; loose framed eyes use 3x diameter. No packets/saved fields.

## Networking

Seven `CustomPacketPayload` classes: eye definitions, settings, entity eye state, behavior triggers, anger, picker freeze, and picker export. `NetworkProtocol` holds wire version `19` and the payload table (type, codec, handler per direction) every loader registers from. Forge/NeoForge require the channel version; on Fabric a configuration task asks the client's version and refuses a mismatch or a client without the mod.

`EyeSettingsPacket` carries `googlyEyesEnabled` and `BehaviorSettings`. Joins receive both; the installed definitions packet is built once; after each datapack reload and Forge/NeoForge server-config apply, `ServerServices.broadcastEyeSyncIfChanged` sends each only if it changed. Only new definitions reset client trackers. Behavior triggers name behaviors by `wireId`.

Clientbound lambdas isolate `ClientNetworkHandler`. Serverbound handlers recheck authorization; client export enforces vanilla's payload cap. Eye state follows entity spawn by ID.

## Picker and commands

Client picker drafts/previews are separate from server freezing/spawning/movement/export. `PickerState.DraftSession` keeps species/age drafts; export-all replaces adult and baby separately. `ModelPartVocabulary` shares attachment grammar between editing and export.

`PickerFreezeService` restores prior `NoAI` on release, load, logout, or stop and permits one editor. `PickerGate` owns creative/operator authorization, throttles, and picker feedback. `PickerSpawnService` finalizes through `MobSpawning`, checks individual insertion results, reverses partial additions on failure, and applies terrain only after full acceptance. Its `refusal` also filters suggestions. Exports write a world datapack or game-directory files and log disk failures. Both use `ConfigFile.exportVersion`/`overlay`, replacing authored ages, preserving untouched/disabled definitions, and canonicalizing copied tokens.

`/sg` editing is client-side; admin/spawn/mob-pose are server-side. Fabric forwards `GooglyServerCommands.SERVER_BRANCHES`.

## Loader integration

Fabric Mixins (`somegoogly-fabric.mixins.json`, `mixin.fabric`) cover data migration, hurt and heal, trades, lightning conversion, shears-kill and head drops, tracking start after the spawn packet, and client level replacement; common Mixins (`somegoogly-common.mixins.json`, `mixin.common`) report milking, sheep shearing, and entity events and hold head eyes. The optional Twilight config is not required. All target the pinned Minecraft version; Fabric and common write fixed refmaps.

NeoForge and Forge keep client bootstrap apart from the dedicated server's.

## Automated verification

GameTest scenarios: Fabric 201, Forge 202, NeoForge 207, including thirteen shared boat scenarios per loader. Item coverage: twelve frame, seven GUI, seven hand scenarios. Manual checklists: `docs/*-testing.md`. Visuals, multiplayer, protection ordering, commands, and real saves need manual checks.

## Operational boundaries

- Integration failures log once.
- `build-env/` is not a build input but a snapshot (see `orientation-build.md`); mirror every edit to a file it covers.
