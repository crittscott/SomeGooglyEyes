# Some Buckets As-Built Orientation

Orientation to the repository's build structure, subsystem ownership, persistent data, cross-loader boundaries, and maintenance invariants. `player-view.md` covers observable behavior. Not a spec, not a prose restatement of the code; the code wins when they disagree. No history.

Length budget: 150 lines / 12k characters. If an edit pushes past that, cut something — don't append.

Per-sentence test: every sentence either (a) names the file/class to open to change a behavior, or (b) names an invariant not visible from any single file — a cross-module contract, an ordering requirement, a "keep these in sync". Sentences that only restate what the code does get deleted, as do enumerations of a method's branches or steps. Update in place.

This is an orientation to the current code, not a history, conversation, or prose rendering of the implementation. It describes what exists, not necessarily what should exist, and is not a design specification.

## Project shape

Identity: mod id `somegoogly`, root package `com.github.crittscott.somegoogly`, version `0.8.1`, Java 21, Minecraft 1.21.1. Runtime target versions are pinned and must track the Gradle scripts: Fabric Loader 0.19.3 with Fabric API 0.116.15+1.21.1, NeoForge 21.1.248, Forge 52.1.16. Fabric and NeoForge depend on Architectury API 13.0.8; Forge has no Architectury API runtime dependency but keeps the build-time `@ExpectPlatform` transform.

The Gradle project has four modules; `common` is transformed into all three loader artifacts.

| Source tree | Responsibility |
| --- | --- |
| `common/src/main/java` | Shared gameplay, state, codecs, services, networking, rendering, picker |
| `common/src/main/resources` | Assets, recipes, eye definitions, language, structure fixture |
| `common/src/gametest/java` | Shared GameTest assertions |
| `fabric/src/main` | Fabric entry points, callbacks, configuration, adapters, Mixins, metadata |
| `fabric/src/gametest` | Fabric wrappers and discovery metadata |
| `forge/src/main` | Forge bootstrap, events, native config, adapters, client integration, GeckoLib bridge, metadata, Access Transformer |
| `forge/src/gametest` | Forge wrappers, persistence proof, dev-mod entry point, discovery metadata |
| `neoforge/src/main` | NeoForge bootstrap, events, native config, adapters, client integration, GeckoLib bridge, metadata, Access Transformer |
| `neoforge/src/gametest` | NeoForge wrappers, persistence proof, dev-mod entry point, discovery metadata |

Common main imports no Forge, NeoForge, Fabric API, or GeckoLib type; every loader or optional-library difference passes through a project-owned adapter or one of six Architectury `@ExpectPlatform` methods. Loader packages stay disjoint from common packages so Forge's module layer sees no split package.

Subsystem ownership: the server owns eligibility, persistent eye state, item actions, behaviors, datapack definitions, picker authorization, and world mutation; the client owns rendering, model attachment, pupil motion, inspection, and picker UI and editing state.

Registered content is declared once in `ContentRegistrar` — two items, one `DataComponentType`, one creative tab, two recipe serializers, and nothing else (no blocks, entities, menus, particles, or sounds). Optometrist is a data-driven enchantment from the common data pack. Fabric binds these handles through native registries; NeoForge and Forge bind the same handles through native deferred registers.

## Configuration and eye definitions

`ServerConfig` and `ClientConfig` hold the schema and expose validated values as `ConfigValue<T>`. Fabric reads its client TOML directly; Fabric and NeoForge share one direct loader for the active world's server TOML; NeoForge adds a native CLIENT spec; Forge uses native CLIENT and SERVER specs and must copy the shared values into `ConfigValue<T>` on load and reload or the two representations diverge.

Eye definitions are server datapack resources at `data/<namespace>/eyes/*.json`, modeled by `EyeConfigModel`. Reload resolves and validates exactly one version per entity type, then atomically swaps `ServerEyeConfigs`; the resolved set is pushed to clients, so `ClientEyeConfigs` never selects a version itself. Size and geometry limits are enforced at three points that must stay aligned: datapack reload, picker export, and network decode.

Change eligibility knobs (spawn and harvest chances, entity overrides, behavior toggles, the spawn-all gate) in `ServerConfig`; change local eye visibility in `ClientConfig`.

The 74 bundled Minecraft definitions target exactly `1.21.1`. Armadillo, bogged, and breeze have no bundled definition. Optional-mod definitions keep their pre-1.21.1 release ranges and are unverified against 1.21.1 mod builds.

Player-visible strings are translatable `Component`s keyed in `assets/somegoogly/lang/en_us.json`, with identifiers, counts, and paths passed as translation arguments. Logs, command literals, config comments, and schema keys are plain strings and are not translated.

## Entity and item state

`EyeState` is the entity-eye-state boundary; its `Snapshot` is the whole unit for server transitions, tracking sync, and client packet application, and partial updates are not a supported path. NBT access goes through `EntityPersistentData`: native persistent compounds on NeoForge and Forge, `EntityPersistentDataMixin` on Fabric.

Persistent entity keys:

- `somegoogly:hasGooglyEyes` — whether the entity has eyes;
- `somegoogly:eyeVariantRoll` — stable placement-variant roll;
- `somegoogly:eyeOverrides` — optional shared appearance overrides.

Related mutations are flushed as one full-snapshot sync. The eye-state key and variant roll are written only when the eye-state key is absent, so the natural-eyes decision survives persistence and transfer.

Eye item stacks carry `AppearanceOverride` in the registered `somegoogly:eye_properties` component; harvesting copies the first configured eye's effective appearance, and crafting and Slimy Eye application preserve that component while leaving other stack components untouched. Item stacks never carry placement geometry.

`EyeItemService` is the single authorization, mutation, drop, and durability path; Fabric callbacks and Mixins, NeoForge events, and Forge events feed Slimy Eye use, Optometrist harvesting, and shears-kill harvesting into it.

## Eligibility and behaviors

`ServerServices.onLivingEntityLoaded` owns natural eye initialization; once an entity is initialized, later configuration changes never revisit it.

`EyeBehaviors` is the ambient-behavior catalog. A `BehaviorInstance` (id, duration, seed, elapsed) is the only per-behavior state sent, and clients derive the animation from it, so no per-tick animation packets exist. `ServerBehaviorScheduler` schedules ambient behaviors for watched eyed entities and takes damage, heal, and trade triggers from loader adapters. `ClientEyeRuntime` holds transient pupil and behavior state for rendered entities and is never persisted.

## Rendering and attachment

`ClientRenderLayers` installs the normal and picker layers on compatible living renderers, guarding against duplicates and reinstalling when renderer state is replaced. `LayerGooglyEyes` must be ordered before the slime outer layer. `ClientEyeConfigs` caches the resolved eye view per age and placement variant; `ServerEyeConfigs` is the uncached path for occasional server use. `EyeRenderTransforms` owns render rotations; `EyePlacement` owns pupil-plane projection.

Attachment resolvers (definition token to model part or bone) live with the render code and cache by model identity, cleared on renderer or runtime reset.

Vanilla rendering access is declared once in the common 1.21.1 Access Widener, which Fabric applies directly; NeoForge and Forge each carry the same 36 entries transcribed one-for-one into an Access Transformer, and the three lists must match.

GeckoLib is optional: common code goes through the loader's `GeckoCompat` bridge, which probes for GeckoLib before touching typed integration code, and a failed GeckoLib layer attach must not block mod load.

`GooglyEyeItemRenderer` draws the Googly Eye as a 3D item; the Slimy Eye uses its normal item model with tint index 2 fed from its appearance payload, and that index must stay in sync with the model JSON.

## Networking

`NetworkHandler` defines the typed custom payloads and codecs behind the project-owned `NetworkTransport` boundary; loader adapters only register and send. Gameplay payload ids embed protocol version `9` and the bodies are byte-identical across loaders, so any wire-format change requires bumping that number. Stable hello and acknowledgment ids run a login handshake, and a mismatch or timeout disconnects the client before resolved eye definitions are sent.

Each receiver is registered for one direction only. Every picker request re-derives its player from the server packet context and re-checks authorization server-side. `NetworkTracking` abstracts loader-specific tracking-player lookup for fanout. Eye-state packets that arrive before their entity wait in a bounded, expiring pending set and are cleared on disconnect.

## Picker and commands

Client picker and command code owns selection, drafts, previews, and export views; the server owns mob freezing, spawning, movement, and world datapack export. `ModelPartVocabulary` is the shared attachment enumeration and token grammar for both live editing and bulk export, and the two must not drift.

`PickerFreezeService` saves and restores each mob's prior `NoAI` value and reconciles freeze markers on mob load, player logout, and server stop; only the owning editor may hold a lock. Picker requests are rate-limited; spawn-all additionally requires creative mode, explicit server enablement, and a server-wide cooldown. World export is confined to the generated datapack directory and triggers a reload; client export-all writes only under the game-directory export tree.

The shared client command code builds one Brigadier tree; each loader registers it and reconciles its picker branch with the server-supplied admin branch.

## Loader integration

Fabric: Mixins carry what has no callback — persistent entity data, hurt and heal reactions, completed trades, renderer-dispatcher reload — and the Mixins plus Access Widener target 1.21.1 exactly; configuration is read once at client init or server start with no file watching.

NeoForge: common registration runs once from the `@Mod` constructor, and client services must be attached to the correct bus (mod versus game).

Forge: native CLIENT and SERVER config specs with shared values synced on load and reload; the Architectury `@ExpectPlatform` transform is build-time only.

NeoForge and Forge both isolate physical-client bootstrap from dedicated-server bootstrap.

## Automated verification

77 shared assertions live in 12 `*Logic` classes under `common/src/gametest/java`. Each loader exposes 78 annotated tests — thin wrappers over the shared logic plus one loader-specific entity-persistence save/load test — so the per-loader count must stay at shared + 1. Visual behavior is covered only by the manual physical-client smoke-test matrix: rendering, item presentation, picker UI, renderer reloads, and optional GeckoLib models.

## Operational boundaries

- Optional renderer integrations may render no eyes when a model family or attachment token cannot be resolved, and third-party model changes can silently invalidate bundled tokens or placement geometry.
- Entity persistence on Fabric depends on the mod's own Mixin, not a component dependency.
- Wire compatibility is the protocol-version number, not the display version, and pre-release data and protocol formats have no compatibility layer.
- `build-env/` is not a build input; it copies the root and module build scripts verbatim, and every edit to a build script must be mirrored there.
