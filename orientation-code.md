<!-- FIXED HEADER: not content. Do not edit, trim, or count toward the size limit. -->
> **Orientation snapshot, not a specification.** Describes the code as it currently is; the code wins on any disagreement. See CLAUDE.md § Orientation files.
<!-- END FIXED HEADER -->

# Some Googly Eyes — code orientation

Structure, ownership, state, loader seams, and invariants; `orientation-player.md` covers behavior.

## Project shape

Identity: mod id `somegoogly`, package `com.github.crittscott.somegoogly`, version `0.8.3`, Java 21, Minecraft 1.21.1.

The Gradle project has four modules; `common` is transformed into all three loader artifacts.

| Source tree | Responsibility |
| --- | --- |
| `common/src/main/java` | Shared gameplay, state, codecs, services, networking, rendering, picker, GeckoLib integration |
| `common/src/main/resources` | Assets, recipes, eye definitions, language |
| `common/src/gametest` | Shared GameTest assertions and the structure fixture |
| `fabric/src/main` | Fabric entry points, callbacks, configuration, adapters, Mixins, metadata |
| `fabric/src/gametest` | Fabric wrappers and discovery metadata |
| `forge/src/main` | Forge bootstrap, events, native config, adapters, client integration, metadata |
| `forge/src/gametest` | Forge wrappers, persistence proof, dev-mod entry point, discovery metadata |
| `neoforge/src/main` | NeoForge bootstrap, events, native config, adapters, client integration, metadata |
| `neoforge/src/gametest` | NeoForge wrappers, persistence proof, dev-mod entry point, discovery metadata |

Common main imports no loader type; differences pass through adapters or `@ExpectPlatform` methods. Loader packages stay disjoint from common packages so Forge sees no split package.

The server owns eligibility, eye state, item actions, behaviors, definitions, picker authorization, and world mutation; the client owns rendering, attachment, pupil motion, inspection, and picker UI.

`ModContent` binds two items, the appearance component, creative tab, and recipe serializers through each loader's `ContentRegistrar`. Optometrist and `self_shear` are data-driven enchantment and damage-type definitions.

## Configuration and eye definitions

`ServerConfig` and `ClientConfig` expose validated `ConfigValue<T>`s; `ConfigValue.Parsed` lists rebuild on assignment. Forge/NeoForge use native CLIENT/SERVER specs and reset SERVER values on unload. Fabric's `TomlConfig` reads both files, reloads server values at start and `/reload`, and resets on stop.

Server key order must match across the three loaders' server configs.

Datapack definitions at `data/<namespace>/eyes/*.json` use `EyeConfigModel`. Reload resolves versions, validates, encodes to NBT, and swaps `ServerEyeConfigs` only when encoding changes; failure retains the previous set. Clients receive the resolved set. Reload, export, and network decode share geometry/work limits; reload also checks sync byte size. `EyeConfigLimits.Issue` supplies translated nested context and English log/decode diagnostics. `EyeColor` codecs reject out-of-range channels.

## Entity and item state

`EyeState` is the entity-eye-state boundary; its `Snapshot` is the whole unit for server transitions, tracking sync, and client packet application. NBT access goes through `EntityPersistentData`: native persistent compounds on NeoForge and Forge, a persistent attachment on Fabric. On load, `FabricEntityDataMigration` moves the `somegoogly:persistentData` compound the 1.20.1 and 1.21.1 releases saved into it; a non-compound value is set aside there and logged.

Persistent entity keys: `somegoogly:hasGooglyEyes` (has eyes), `somegoogly:eyeVariantRoll` (stable variant roll), `somegoogly:eyeOverrides` (optional appearance overrides).

Mutations flush one full snapshot. Rolls read clamped to 0..1; eye state and roll initialize together only when absent. `EyeState.initialize` and `sendTo` skip absent snapshots; mid-life mutations send. Conversion and non-death respawn call `EyeState.copy`; joining players receive their own snapshot.

Eye items carry only `AppearanceOverride` in the synced `somegoogly:eye_properties` component, never geometry. Harvesting copies the first eye's effective appearance; modifier recipes and Slimy Eye application change that component. The Slimy Eye recipe copies every component.

For 1.20.1 saves, `EyeItemMigration` converts `EyeProperties` custom data during `verifyComponentsAfterLoad`: current components win; undecodable data stays and is logged.

`EyeItemService` owns authorization, mutation, drops, and durability for Slimy Eye use and both harvests. Loader adapters run entity interaction and self-shearing after default protection listeners (Forge/NeoForge `LOWEST`, Fabric a late callback phase). Fabric's adapter refuses spectators itself. Shears are `#somegoogly:shears` (vanilla plus optional `#c:tools/shear`). Applying a Slimy Eye to another player also requires server PvP and `canHarmPlayer`. Plain-shears self-damage uses the attacker-less `self_shear` type, so PvP rules cannot cancel it.

Successful Slimy Eye applications emit `GameEvent.ENTITY_INTERACT`, successful harvests `GameEvent.SHEAR`; refusals emit neither.

## Eligibility and behaviors

`ServerServices.onLivingEntityLoaded` owns natural eye initialization; later configuration changes never revisit an initialized entity.

The `EyeBehavior` enum is the catalog. A `BehaviorInstance` (id, duration, seed, elapsed) is the only per-behavior state sent, and clients derive the animation. `ServerBehaviorScheduler` schedules ambient behaviors for watched eyed mobs and takes damage, heal, trade, milking, shearing, and entity-event triggers; it starts nothing on a sleeper. `GooglyTracker` derives sleeping villagers' eyelids (`ModelGooglyEye.renderLids`), the sleep, goat-ram, and one-eyed aiming squints, creeper rattle, red pupils, and enderman pulse from synced state; only sleep blocks behaviors. Server-only anger (`NeutralMob`, piglin `ANGRY_AT`) is checked per tick and broadcast on change; tracking an eyed mob sends its current boolean without changing the other viewers' broadcast baseline. `ClientEyeRuntime` holds transient pupil and behavior state for rendered entities. Entity unload removes both tracker and anger; level replacement and disconnect clear both caches. Renderer/resource reload retains anger for loaded entities; tracker-only resets also retain it.

## Rendering and attachment

`ClientRenderLayers` installs `LayerGooglyEyes` once per living renderer rebuild; GeckoLib uses `CompileRenderLayers`. The eye layer precedes slime's outer layer. Both render paths use `EyeRenderData` and picker previews. `ClientEyeConfigs` caches by age/variant. `GooglyEyeRenderer.drawEye` draws mobs, previews, and items; `ClientLifecycle` ticks the first-person held pupil. `EyePlacement.orientation` defines rendering and pupil-plane rotation.

Attachment resolvers cache by model identity, clear on renderer or runtime reset, and replay `ThirdPartyModelWraps` transforms first, choosing the age branch from `model.young`. `Resolvers` picks a family per model: `HierarchicalResolver`, `AgeableListResolver` (replaying baby transforms), `RabbitLlamaResolver`, `TwilightForestResolver`, and the reflected `AdvancedModelBoxResolver` and `LLibraryResolver`, with `ChildMapResolver` as an explicit non-null fallback. Cache clearing covers every specialized resolver and the fallback.

The common `somegoogly.accesswidener` serves common compilation and every loader; Forge and NeoForge convert it to an access transformer at remap.

GeckoLib is optional: common compiles against compile-only `geckolib-common`; nothing touches `client.compat.gecko` unless `GeckoCompat` finds it, and a failed attach must not block load.

`GooglyEyeItemRenderer` draws the 3D Googly Eye; `SLIMY_EYE_IRIS_TINT_INDEX` must match the Slimy Eye model's `layer2`.

## Networking

Six `CustomPacketPayload` classes: eye definitions, entity eye state, behavior triggers, anger, picker freeze, and picker export. Their ids, in `NetworkHandler`, embed network version `15`; any incompatible wire change requires bumping it. Forge and NeoForge register a required native channel version; Fabric disconnects, during configuration, a client that cannot receive its `Handshake` payload.

The eye-definition packet also carries `googlyEyesEnabled`. Joins always receive it; after each datapack reload and Forge/NeoForge server-config apply, `ServerServices.broadcastEyeConfigsIfChanged` resends it only if the set or switch changed, since it resets client trackers.

Sends go through `@ExpectPlatform` bridges `Networking` and client-only `ClientNetworking`; clientbound handlers reach `ClientNetworkHandler` only inside lambdas, so servers never link it. Serverbound handlers re-check the authenticated `ServerPlayer`'s authorization; the client refuses a picker export over vanilla's serverbound cap. Eye-state, behavior, and anger packets reach trackers and the entity itself; eye state follows the entity's spawn, so it carries only the id and an unknown id is ignored.

## Picker and commands

The client owns picker drafts and previews; the server owns freezing, spawning, movement, and export. `PickerState.DraftSession` retains drafts keyed by species and age, keeps selection with its identity, and supplies export-all with separate adult and baby replacements; choosing one age never relabels the other's edits. `ModelPartVocabulary` supplies one attachment grammar to live editing and bulk export.

`PickerFreezeService` restores prior `NoAI` on release, load reconciliation, logout, or stop and permits one editor. `PickerGate` owns creative/permission-level-2 authorization and throttles. `PickerSpawnService` uses loader finalization via `MobSpawning`, honors cancellation, then sets persistence, `NoAI`, and rotation; its `refusal` also filters command suggestions. World export writes a generated datapack; bulk export writes under the game directory. Both use `ConfigFile.exportVersion` and `overlay`, replacing authored age buckets while retaining untouched definitions, including disabled ones. Bulk export canonicalizes copied base tokens; drafts are already canonical.

Client and server own disjoint `/sg` branches: editing is client-side; admin, spawn, spawn-all, and mob-pose are server-side. Fabric forwards those server branches explicitly; its client root would capture them.

## Loader integration

Fabric Mixins cover data migration, hurt and heal, trades, shears-kill drops, and client level replacement (no equivalent events in the pinned Fabric API); common Mixins (`somegoogly-common-server.mixins.json`) report milking, sheep shearing, and entity events. All target the pinned Minecraft version; Fabric and common write fixed refmaps.

NeoForge: common registration runs once from the `@Mod` constructor, and client services must be attached to the correct bus (mod versus game).

Forge's required `PayloadChannel` marks payloads handled. NeoForge and Forge isolate physical-client bootstrap from dedicated-server bootstrap.

## Automated verification

`common/src/gametest/java` holds 108 shared tests, including save/load persistence, plus 9 for the 1.20.1 item migration; each loader wraps them, and Fabric adds 4 migration and TOML tests. Root `generateDocs` syncs all Javadoc into `docs/javadoc/`. Regressions cover draft switching, age-overlay JSON round trips, and runtime anger lifetime. Actual loader protection vetoes, packet delivery/retracking with one or two viewers, client unload/dimension changes, translated export feedback, required-client rejection, server commands, picker operator gating, plain-shears self-damage, and real-save migration remain manual checks.

## Operational boundaries

- Optional renderer integrations log failures once.
- `build-env/` is not a build input; it is a snapshot of the build environment described in `orientation-build.md`, and every edit to a file it covers must be mirrored there.
