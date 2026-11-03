<!-- FIXED HEADER: not content. Do not edit, trim, or count toward the size limit. -->
> **Orientation snapshot, not a specification.** Describes the code as it currently is; the code wins on any disagreement. See CLAUDE.md § Orientation files.
<!-- END FIXED HEADER -->

# Some Googly Eyes — code orientation

## Project shape

Identity: mod id `somegoogly`, package `com.github.crittscott.somegoogly`, version `0.8.3`, Java 17, Minecraft 1.20.1.

`common` is transformed into both loader artifacts.

| Source tree | Responsibility |
| --- | --- |
| `common/src/main/java` | Shared gameplay, state, codecs, services, networking, rendering, picker, GeckoLib gate |
| `gecko/src/main/java` | GeckoLib-typed layer and bone code, compiled by each loader |
| `common/src/mixin/java` | Shared Mixins, compiled by each loader |
| `common/src/main/resources` | Assets, recipes, eye definitions, language |
| `common/src/gametest` | Shared GameTest assertions and the structure fixture |
| `fabric/src/main` | Fabric entry points, callbacks, configuration, adapters, Mixins, metadata |
| `fabric/src/gametest` | Fabric wrappers and discovery metadata |
| `forge/src/main` | Forge bootstrap, events, native config, adapters, client integration, metadata |
| `forge/src/gametest` | Forge wrappers, persistence proof, dev-mod entry point, discovery metadata |

Server owns gameplay/authorization; client owns rendering/picker UI.

`ModContent` binds items, Optometrist, a creative tab, and recipe serializers via loader registrars. `SlimyEyeRecipe` copies eye NBT.

## Configuration and eye definitions

`ServerConfig`/`ClientConfig` expose validated `ConfigValue<T>`s. Forge applies native CLIENT/SERVER specs on load/reload and resets SERVER on unload. Fabric uses bundled NightConfig TOML at startup and `/reload`, resets on stop, reads declared tables, preserves raw values and comments during missing-key completion, and backs up rewrites. Malformed input preserves loaded values. `FabricConfigMigration` quotes released spaced headers. Sections retain 0.8.2's names. `ServerServices.onServerConfigApplied` logs the effective master switch at startup and changes.

Eye definitions (`data/<namespace>/eyes/*.json`, `EyeConfigModel`) reload by resolving one version per entity type, validating, and NBT-encoding the set; `ServerEyeConfigs` swaps only if the encoding differs, keeping the old set on failure. That encoding is the sync payload, so `ClientEyeConfigs` never selects versions. Size and geometry limits stay aligned across reload (which reserves the maximum behavior block in its packet budget), picker export, and network decode; `EyeColor` codecs reject out-of-range channels.

## Entity and item state

`EyeState` serves living entities and supported vanilla boats; its `Snapshot` is the whole unit for transitions, sync, and packet application, never partial. NBT access goes through `EntityPersistentData`: Forge's native persistent compound, a persistent attachment on Fabric. On load, `FabricEntityDataMigration` moves the `somegoogly:persistentData` compound the 0.8.2 Fabric release saved into it; a non-compound value is set aside there and logged.

Persistent entity keys:

- `somegoogly:hasGooglyEyes` — whether the entity has eyes;
- `somegoogly:eyeVariantRoll` — stable placement-variant roll;
- `somegoogly:eyeOverrides` — optional shared appearance overrides.

The eye-state key and roll are initialized together only when absent, keeping the natural-eyes decision. `initialize` and `sendTo` skip a default snapshot; mid-life mutations always send. Loader hooks call `EyeState.copy` on mob conversion and non-death respawn; a player joining a level gets their own snapshot.

`SkullEyeState` keeps a placed head's `Snapshot` (one `read`/`write`) in a field the shared `SkullBlockEntityDataMixin` adds, saved as `somegoogly:data` and replaced on every `load` (neither loader's own store is), so it rides the update tag and a head item's `BlockEntityTag`. A head borrows its mob's adult definition, rolling among variants with eyes on the part it copies (`HEAD_PART_TOKENS`, else the head part). Vanilla heads map by type, the Heads mod's by block id alone (`HEADS_MOBS`), a soft dependency; other mods' heads have none. Mutations end in a block update.

Eye items carry `AppearanceOverride` as the `EyeProperties` compound (`EyeItemProperties`), never geometry; harvesting copies the first eye's effective appearance, and the modifier recipe changes only that compound.

`EyeItemService` owns authorization, mutation, drops, and durability for Slimy Eye use and both harvests, on mobs and heads. Adapters run entity interaction, block use, and self-shearing after protection listeners (Forge `LOWEST`, Fabric a late phase); head use honors native item-use denial and checks `mayInteract`/`mayUseItemAt`. `markHeadDrop` stamps an eyed mob's own head among its death drops (Forge at `LOWEST`); head loot copies the state to the item (Forge `HeadEyesLootModifier`, data in Forge; Fabric `LootTableEvents.MODIFY`). Fabric's adapter refuses spectators. Shears are `#somegoogly:shears` (vanilla plus optional `#c:shears` and `#forge:shears`). Applying a Slimy Eye to another player also requires server PvP and `canHarmPlayer`. Plain-shears self-damage uses the attacker-less `self_shear` type, which PvP rules can't cancel.

Application emits `GameEvent.ENTITY_INTERACT` (`BLOCK_CHANGE` on a head), harvest `SHEAR`, refusal nothing.

## Eligibility and behaviors

`ServerServices.onLivingEntityLoaded` owns natural eye initialization; later configuration changes never revisit an initialized entity.

`EyeBehavior` catalogs expressions; `OngoingBehavior` catalogs ongoing states. `BehaviorInstance` carries id/duration/seed/elapsed. `ServerBehaviorScheduler` runs watched eyed mobs and damage/heal/trade/milking/shearing/entity-event triggers; sleepers start nothing. `GooglyTracker` applies enabled ongoing states. `HeldEye` runs first-person idle/hurt/heal expressions. `PresentationEyes` caches ambient slots for items, placed heads, and boats. Placed heads seed by position. Only paired layouts cross-eye. Slots fit the longest eligible expression or the synced mean interval, whichever is greater. Anger syncs on change/new viewer. `ClientEyeRuntime` retains mob reports independently of disposable simulations and ages expressions offscreen. Only recently rendered trackers simulate; idle trackers expire. Heads cache by position, frames by entity; removed/changed frame contents invalidate them. Level changes/disconnect clear all. The master switch pauses mob scheduling while retaining watchers and expiring effects; authoring triggers remain available. Client behaviors start inactive until sync.

## Rendering and attachment

`ClientRenderLayers` installs eye layers once per renderer rebuild, before slime's outer layer; GeckoLib uses `CompileRenderLayers`. Both draw picker previews through `EyeRenderData`. `LayerGooglyEyes` insets sheared adult sheep `body` eyes by wool depth. `ClientEyeConfigs` caches resolved views. `GooglyEyeRenderer` draws static and simulated eyes; `ClientLifecycle` ticks the first-person held eye. `EyePlacement.orientation` owns rendering and pupil-plane rotation.

Resolvers cache by model identity, clear on renderer/runtime reset, and replay `ThirdPartyModelWraps`; `Resolvers` selects the model family, with `ChildMapResolver` as its non-null fallback.

The common `somegoogly.accesswidener` serves common and both loaders; Forge remaps it to an access transformer.

Optional GeckoLib: `gecko/src/main/java` is `srcDir`-ed into each loader against its compile-only GeckoLib; common enumerates bones through `GeckoCompat`'s `@ExpectPlatform` `enumerateBones` only once GeckoLib is found; a failed attach can't block load.

`BoatEyeLayout` selects hull/raft geometry for supported vanilla boats. Boat item interactions and tracking use generic eye snapshots without living behavior scheduling. `BoatEyeAnimations` owns disposable world-clock simulations. The boat Mixin draws before the hull pose is popped. `PresentationEyes` supplies presentation layouts, ambient expressions, and owner-motion inputs. `EyeInfo.capturePose` projects world gravity/acceleration independently of the camera; frames/GUI remain stationary. `HandEyeAnimations` keys world-timed state by holder/hand; `HandItemEyes` scopes draws and item states retain it. `GuiEyeAnimations` owns view keys and UI time; `GuiItemEyes` scopes extraction/draw context. GUI Mixins select slots/cursor; item states retain context and animated cache identity. GUI icons keep GUI scale; loose framed eyes use 3x diameter. No packets/saved fields.

## Networking

Six `Payload` classes: eye definitions, entity eye state, behavior triggers, anger, picker freeze, and picker export. Ids in `NetworkHandler` embed network version `17`; bump it on wire changes. Forge registers each on a `SimpleChannel` requiring that version; Fabric's `HANDSHAKE` login query disconnects a client that doesn't understand it; its receivers decode off-thread.

The eye-definition packet also carries `googlyEyesEnabled` and `BehaviorSettings`; joins always get it, and after each datapack reload or Forge server-config apply `ServerServices.broadcastEyeConfigsIfChanged` resends it only on a change, since it resets client trackers.

Sends go through `@ExpectPlatform` `Networking` and client-only `ClientNetworking`; clientbound handlers reach `ClientNetworkHandler` only inside lambdas, so servers never link it. Serverbound handlers re-check authorization; the client refuses a picker export over vanilla's serverbound cap. Eye-state, behavior, and anger packets reach trackers and the entity; eye state follows the entity's spawn, carrying only its id.

## Picker and commands

Client owns picker drafts/previews; server owns freezing/spawning/movement/export. `ModelPartVocabulary` supplies one attachment grammar to editing and bulk export. `PickerState.DraftSession` keeps drafts by type and age, seeded once; both exports `ConfigFile.overlay` authored ages onto untouched ones (disabled buckets included), canonicalizing only copied base tokens. `EyeConfigLimits.Issue` gives translatable feedback and log diagnostics.

`PickerFreezeService` restores prior `NoAI` on release, load, logout, or stop and permits one editor. `PickerGate` owns creative/operator authorization and throttles. `LookTarget` caps extended entity rays at blocking surfaces; inspection compares actual hit locations. `PickerSpawnService` finalizes through `MobSpawning`, inserts each entity/passenger with checked results, discards partial additions on refusal, and applies planned terrain only after acceptance. Its `refusal` also filters suggestions. World export writes a datapack; bulk export writes under the game directory and logs disk failures. Both use `ConfigFile.exportVersion`/`overlay`, replacing authored ages, preserving untouched/disabled definitions, and canonicalizing copied tokens.

`/sg` editing is client-side; admin/spawn/mob-pose are server-side. Fabric forwards server branches explicitly.

## Loader integration

Shared Mixins (`somegoogly-common*.mixins.json`) cover milking, sheep shearing, server-broadcast entity events, item-eye contexts, boats, and holding and drawing head eyes; each loader compiles them into its own refmap. Fabric's own cover data migration, hurt and heal, trades, shears-kill and head drops, and level changes; each loader has a mooshroom-shear Mixin, Forge's also targeting `shearInternal`.

Forge registers from `@Mod`, keeping client bootstrap apart from the dedicated server's, with client listeners on the right bus. Forge GameTests use the unprefixed `empty` template.

## Automated verification

GameTest scenarios: Fabric 172, Forge 172. Item coverage: twelve frame, seven GUI, seven hand scenarios; thirteen boat scenarios per loader. Client checklists: `docs/framed-eyes-testing.md`, `docs/inventory-eyes-testing.md`, `docs/third-person-eyes-testing.md`, and `docs/boat-eyes-testing.md`. Visuals, multiplayer, protection ordering, commands, and real saves need manual checks.

## Operational boundaries

- Optional renderers log failures once.
- `build-env/` mirrors build inputs (see `orientation-build.md`); update it with them.
