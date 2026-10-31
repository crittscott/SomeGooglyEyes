<!-- FIXED HEADER: not content. Do not edit, trim, or count toward the size limit. -->
> **Orientation snapshot, not a specification.** Describes the code as it currently is; the code wins on any disagreement. See CLAUDE.md § Orientation files.
<!-- END FIXED HEADER -->

# Some Googly Eyes — code orientation

`orientation-player.md` covers player behavior.

## Project shape

Identity: mod id `somegoogly`, package `com.github.crittscott.somegoogly`, version `0.8.3`, Java 17, Minecraft 1.20.1.

`common` is transformed into both loader artifacts.

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

Common uses loader adapters or `@ExpectPlatform`; loader packages avoid split packages.

The server owns gameplay and authorization; the client owns rendering and picker UI.

`ModContent` binds items, Optometrist, a creative tab, and recipe serializers via loader registrars. `SlimyEyeRecipe` copies eye NBT.

## Configuration and eye definitions

`ServerConfig`/`ClientConfig` expose validated `ConfigValue<T>`s. Forge applies native CLIENT/SERVER specs on load/reload and resets SERVER on unload. Fabric uses bundled NightConfig TOML at startup and `/reload`, resets on stop, reads declared tables, preserves raw values and comments during missing-key completion, and backs up rewrites. Malformed input preserves loaded values. `FabricConfigMigration` quotes released spaced headers. Sections retain 0.8.2's names. `ServerServices.onServerConfigApplied` logs the effective master switch at startup and changes.

Eye definitions (`data/<namespace>/eyes/*.json`, `EyeConfigModel`) reload by resolving one version per entity type, validating, and NBT-encoding the set; `ServerEyeConfigs` swaps only if the encoding differs, keeping the old set on failure. That encoding is the sync payload, so `ClientEyeConfigs` never selects versions. Size and geometry limits stay aligned across reload (which reserves the maximum behavior block in its packet budget), picker export, and network decode; `EyeColor` codecs reject out-of-range channels.

## Entity and item state

`EyeState` is the entity-eye-state boundary; its `Snapshot` is the whole unit for transitions, sync, and packet application, never partial. NBT access goes through `EntityPersistentData`: Forge's native persistent compound, a persistent attachment on Fabric. On load, `FabricEntityDataMigration` moves the `somegoogly:persistentData` compound the 0.8.2 Fabric release saved into it; a non-compound value is set aside there and logged.

Persistent entity keys:

- `somegoogly:hasGooglyEyes` — whether the entity has eyes;
- `somegoogly:eyeVariantRoll` — stable placement-variant roll;
- `somegoogly:eyeOverrides` — optional shared appearance overrides.

The eye-state key and roll are initialized together only when absent, keeping the natural-eyes decision. `initialize` and `sendTo` skip a default snapshot; mid-life mutations always send. Loader hooks call `EyeState.copy` on mob conversion and non-death respawn; a player joining a level gets their own snapshot.

`SkullEyeState` keeps a placed head's `Snapshot` (one `read`/`write`) in a field each loader's `SkullBlockEntityDataMixin` adds, saved as `somegoogly:data` and replaced on every `load` (neither loader's own store is), so it rides the update tag and a head item's `BlockEntityTag`. A head borrows its mob's adult definition, rolling among variants with eyes on the part it copies (`HEAD_PART_TOKENS`, else the head part). Vanilla heads map by type, the Heads mod's by block id alone (`HEADS_MOBS`), a soft dependency; other mods' heads have none. Mutations end in a block update.

Eye items carry `AppearanceOverride` as the `EyeProperties` compound (`EyeItemProperties`), never geometry; harvesting copies the first eye's effective appearance, and the modifier recipe changes only that compound.

`EyeItemService` owns authorization, mutation, drops, and durability for Slimy Eye use and both harvests, on mobs and heads. Adapters run entity interaction, block use, and self-shearing after protection listeners (Forge `LOWEST`, Fabric a late phase); head use honors native item-use denial and checks `mayInteract`/`mayUseItemAt`. `markHeadDrop` stamps an eyed mob's own head among its death drops (Forge at `LOWEST`); head loot copies the state to the item (Forge `HeadEyesLootModifier`, data in Forge; Fabric `LootTableEvents.MODIFY`). Fabric's adapter refuses spectators. Shears are `#somegoogly:shears` (vanilla plus optional `#c:shears` and `#forge:shears`). Applying a Slimy Eye to another player also requires server PvP and `canHarmPlayer`. Plain-shears self-damage uses the attacker-less `self_shear` type, which PvP rules can't cancel.

Application emits `GameEvent.ENTITY_INTERACT` (`BLOCK_CHANGE` on a head), harvest `SHEAR`, refusal nothing.

## Eligibility and behaviors

`ServerServices.onLivingEntityLoaded` owns natural eye initialization; later configuration changes never revisit an initialized entity.

`EyeBehavior` catalogs one-shot expressions; `OngoingBehavior` the ongoing states, each with its server switch and vanilla trigger. A `BehaviorInstance` (id, duration, seed, elapsed) is the only per-behavior state sent; clients derive the animation. `ServerBehaviorScheduler` schedules ambient play for watched eyed mobs and takes damage, heal, trade, milking, shearing, and entity-event triggers; it starts nothing on a sleeper. Its tick sends anger (`NeutralMob`, piglin `ANGRY_AT`) on change. `GooglyTracker` folds in each switched-on `OngoingBehavior`; only sleep blocks behaviors, switch or not. `HeldEye` starts the held item's idle, hurt, and heal behaviors per the synced `BehaviorSettings`. `SkullEyes` derives a head's ambient behaviors on each client from its position and game time, in slots of the synced ambient mean. `ClientEyeRuntime` owns entity-lifetime received expressions and anger independently of disposable pupil trackers; expression age advances on unpaused ticks even offscreen. Render resets retain reports; entity removal drops its reports, and level replacement/disconnect clears all. Head simulations are cached by position. Automatic scheduling pauses under the master switch, preserving watchers and expiring effects; resumption refreshes viewers. Explicit authoring triggers remain available. Client behavior settings start and clear inactive until synchronization.

## Rendering and attachment

`ClientRenderLayers` installs eye layers once per renderer rebuild, before slime's outer layer; GeckoLib uses `CompileRenderLayers`. Both draw picker previews through `EyeRenderData`. `ClientEyeConfigs` caches resolved views. `GooglyEyeRenderer` draws static and simulated eyes; `ClientLifecycle` ticks the first-person held eye. `EyePlacement.orientation` owns rendering and pupil-plane rotation.

Resolvers cache by model identity, clear on renderer/runtime reset, and replay `ThirdPartyModelWraps`; `Resolvers` selects the model family, with `ChildMapResolver` as its non-null fallback.

The common `somegoogly.accesswidener` serves common and both loaders; Forge remaps it to an access transformer.

GeckoLib is optional: `gecko/src/main/java` is `srcDir`-ed into each loader against its compile-only GeckoLib; common enumerates bones through `GeckoCompat`'s `@ExpectPlatform` `enumerateBones` only once GeckoLib is found; a failed attach can't block load.

`GooglyEyeItemRenderer` draws the 3D Googly Eye; the Slimy Eye tint index matches model `layer2`.

## Networking

Six `Payload` classes: eye definitions, entity eye state, behavior triggers, anger, picker freeze, and picker export. Ids in `NetworkHandler` embed network version `17`; bump it on wire changes. Forge registers each on a `SimpleChannel` requiring that version; Fabric's `HANDSHAKE` login query disconnects a client that doesn't understand it; its receivers decode off-thread.

The eye-definition packet also carries `googlyEyesEnabled` and `BehaviorSettings`; joins always get it, and after each datapack reload or Forge server-config apply `ServerServices.broadcastEyeConfigsIfChanged` resends it only on a change, since it resets client trackers.

Sends go through `@ExpectPlatform` `Networking` and client-only `ClientNetworking`; clientbound handlers reach `ClientNetworkHandler` only inside lambdas, so servers never link it. Serverbound handlers re-check authorization; the client refuses a picker export over vanilla's serverbound cap. Eye-state, behavior, and anger packets reach trackers and the entity; eye state follows the entity's spawn, carrying only its id.

## Picker and commands

The client owns picker drafts and previews; the server owns freezing, spawning, movement, export. `ModelPartVocabulary` supplies one attachment grammar to editing and bulk export. `PickerState.DraftSession` keeps drafts by type and age, seeded once; both exports `ConfigFile.overlay` authored ages onto untouched ones (disabled buckets included), canonicalizing only copied base tokens. `EyeConfigLimits.Issue` gives translatable feedback and log diagnostics.

`PickerFreezeService` restores prior `NoAI` on release, load, logout, or stop and permits one editor. `PickerGate` owns creative/operator authorization and throttles. `LookTarget` caps extended entity rays at blocking surfaces; inspection compares actual hit locations. `PickerSpawnService` finalizes through `MobSpawning`, inserts each entity/passenger with checked results, discards partial additions on refusal, and applies planned terrain only after acceptance. Its `refusal` also filters suggestions. World export writes a datapack; bulk export writes under the game directory and logs disk failures. Both use `ConfigFile.exportVersion` and `overlay`, replacing authored ages while retaining untouched definitions, including disabled ones; copied tokens are canonicalized.

Client and server own disjoint `/sg` branches: editing is client-side; admin, spawn, spawn-all, and mob-pose are server-side; Fabric forwards them explicitly, since its client root would capture them.

## Loader integration

Fabric Mixins cover data migration, hurt and heal, trades, shears-kill and head drops, milking, sheep shearing, and server-broadcast entity events; both loaders' Mixins hold and draw head eyes; the mooshroom shear, milking, sheep shearing, and entity events have Forge Mixins too, since Forge has no event for them. Mixins target the pinned Minecraft version; each loader writes its own fixed refmap.

Forge registers from `@Mod`, keeping client bootstrap apart from the dedicated server's, with client listeners on the right bus. Forge GameTests use the unprefixed `empty` template.

## Automated verification

Loader GameTest entry points: Fabric 133, Forge 133. Review regressions have scenarios; visuals, connections, artifacts, and released saves require manual checks.

## Operational boundaries

- Optional renderers log failures once.
- `build-env/` is not a build input but a snapshot (see `orientation-build.md`); mirror every edit to a file it covers.
