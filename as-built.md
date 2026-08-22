# Some Googly Eyes As-Built Orientation

This selective guide identifies the code boundaries and invariants a maintainer should understand
before changing Some Googly Eyes. `player-view.md` describes observable behavior; the code is
authoritative when either document is wrong.

This is an orientation to the current code, not a history, conversation, or prose rendering of the
implementation.

It describes what exists, not necessarily what should exist, and is not a design specification.

## Project shape

Some Googly Eyes is a Java 17 Minecraft 1.20.1 mod for Forge 47.4.10 and Fabric Loader 0.19.3/Fabric
API 0.92.11, using Architectury API 9.2.14. Its mod id is `somegoogly`, its root package is
`com.github.crittscott.somegoogly`, and its version is `0.8.1`.

The active modules are `common`, `forge`, and `fabric`:

| Source tree | Responsibility |
| --- | --- |
| `common/src/main/java` | Shared gameplay, state, codecs, services, networking, rendering, and picker code |
| `common/src/main/resources` | Assets, recipes, eye definitions, language, and the structure fixture |
| `common/src/gametest/java` | Shared GameTest assertions |
| `forge/src/main` | Forge entry points, events, configuration, platform adapters, and metadata |
| `fabric/src/main` | Fabric entry points, callbacks, configuration, platform adapters, Mixins, and metadata |
| Loader `src/gametest` trees | Thin wrappers around shared GameTest assertions |

Common main does not import Forge, Fabric API, or GeckoLib. Loader differences and optional-library
boundaries pass through adapters or Architectury `@ExpectPlatform` methods. Loader packages remain
separate from common packages to avoid split packages under Forge's module layer.

The server owns eligibility, persistent eye state, item actions, behaviors, datapack definitions,
picker authorization, and world mutation. The client owns rendering, model attachment, pupil motion,
inspection, and picker UI and editing state.

The mod registers two items, one enchantment, one creative tab, and two recipe serializers. It does
not register blocks, entities, menus, particles, or sounds.

## Configuration and eye definitions

`ServerConfig` and `ClientConfig` define the configuration schema and expose validated runtime values
through `ConfigValue<T>`. Forge connects these values to `ForgeConfigSpec`; Fabric reads its client
and per-world server TOML files directly.

Server configuration controls spawn and harvest chances, entity overrides, behaviors, and the
picker's destructive spawn-all gate. Client configuration controls local eye visibility.

Eye definitions are server datapack resources at `data/<namespace>/eyes/*.json`. `EyeConfigModel`
defines their serialized and runtime forms, including version entries, age selectors, placement
variants, heads, eyes, and appearance. Reload processing selects and validates one version for each
entity type, then atomically replaces `ServerEyeConfigs`. The resolved set is sent to clients, so
clients do not select versions independently.

Definition size and geometry limits apply at datapack reload, picker export, and network decoding.
Minecraft definitions target exactly `1.20.1`; definitions for optional mods may cover ranges of
their Minecraft 1.20.1 releases.

Player-visible text uses translatable `Component` values backed by
`assets/somegoogly/lang/en_us.json`. Translation arguments carry names, identifiers, paths, counts,
and diagnostic details across command and packet boundaries. Logs, command tokens, config comments,
and schema identifiers are ordinary strings.

## Entity and item state

`EyeState` is the gameplay boundary for entity eye state. Its `Snapshot` is the complete unit used by
server transitions, tracking synchronization, and client packet application. Entity NBT access goes
through `EntityPersistentData`: Forge uses its persistent compound, while Fabric supplies the same
boundary with `EntityPersistentDataMixin`.

The persistent eye fields are:

- `somegoogly:hasGooglyEyes` — whether the entity has eyes;
- `somegoogly:eyeVariantRoll` — its stable placement-variant roll;
- `somegoogly:eyeOverrides` — optional shared appearance overrides.

Related mutations are batched into one full-snapshot synchronization. Natural eye state and the
variant roll are initialized only when the eye-state key is absent, preserving the decision across
entity persistence and transfers.

Eye item stacks carry a fixed `AppearanceOverride` payload. Harvesting captures the effective
appearance of the first configured eye; crafting and Slimy Eye application preserve that payload.
Items do not carry placement geometry.

`EyeItemService` owns the shared application and harvesting operations. Loader events and callbacks
only adapt platform interactions into that service. Slimy Eye use, Optometrist harvesting, and
shears-kill harvesting therefore share the same authorization, state mutation, drops, and durability
rules on both loaders.

## Eligibility and behaviors

`ServerServices.onLivingEntityLoaded` owns natural eye initialization. It excludes players, observes
global enablement, requires an enabled definition for some life stage, applies exact and wildcard
entity percentages, and records independent spawn and variant rolls. Later configuration changes do
not revisit initialized entities.

`EyeBehaviors` contains blink, cross-eye, side-eye, stare, grow, swirl, and color-change definitions.
A `BehaviorInstance` carries the behavior id, duration, seed, and elapsed time. Clients derive the
animation from that state rather than receiving per-tick animation packets.

`ServerBehaviorScheduler` considers watched entities with eyes, schedules ambient behaviors, and
accepts damage, healing, and trade triggers from loader adapters. `ClientEyeRuntime` maintains the
transient pupil and behavior state for rendered entities.

## Rendering and attachment

`ClientRenderLayers` installs the normal and picker layers on compatible living renderers.
`LayerGooglyEyes` renders resolved eyes, and is ordered before the slime outer layer. Installation is
guarded against duplicate layers and reset when renderer state is replaced.

`ClientEyeConfigs` resolves and caches the eye view for an entity's age and placement variant.
`ServerEyeConfigs` provides uncached resolution for occasional server uses. `EyeRenderTransforms`
owns render rotations; `EyePlacement` owns pupil-plane projection.

Attachment resolvers map definition tokens to model parts or bones. They cover ordinary hierarchical
and ageable models, special vanilla and Twilight Forest shapes, Citadel/LLibrary-family boxes, and
GeckoLib bones. Results are cached by model identity and cleared with renderer or runtime resets.

Shared access to vanilla rendering members is declared in the common access widener. Fabric applies
it directly; Forge supplies matching access through patches or its Access Transformer.

GeckoLib is optional. Common code calls the loader-specific `GeckoCompat` bridge, which checks for
GeckoLib before using typed integration code. Failure to attach a GeckoLib layer does not prevent the
base mod from loading.

`GooglyEyeItemRenderer` renders the Googly Eye as a 3D item. The Slimy Eye uses its ordinary item
model, with tint index 2 derived from its appearance payload.

## Networking

`NetworkHandler` uses Architectury `NetworkManager`. Gameplay payload ids contain protocol version
`9`; stable hello and acknowledgment ids negotiate compatibility during login. A mismatch or timeout
disconnects the client before resolved eye definitions are sent.

Server-to-client payloads carry eye state, resolved definitions, and behavior triggers. Client-to-
server payloads request picker freeze, spawn, pose, and export operations. Receivers are registered for
one direction, and every picker request derives its player from the server packet context and repeats
authorization on the server.

`NetworkTracking` abstracts loader-specific tracking-player lookup. Eye-state packets that arrive
before their client entity are retained in a bounded, expiring pending set and applied when the entity
loads. Pending state is cleared on disconnect.

## Picker and commands

The client `/sg` tree and picker controls maintain selection, model-part choice, drafts, previews,
and export views. `ModelPartVocabulary` supplies consistent attachment enumeration and token handling
for live editing and bulk export. The server owns mob freezing, spawning, movement, and world datapack
export.

`PickerFreezeService` preserves a mob's previous `NoAI` value and reconciles freeze markers when mobs
load, players leave, or the server stops. Only the owning editor may retain a lock.

Server handlers rate-limit picker requests. Spawn-all additionally requires creative mode, explicit
server enablement, and a server-wide cooldown. World export is constrained to the generated datapack
directory and triggers a datapack reload. Client export-all writes only to the game-directory export
tree.

The shared client command code builds one Brigadier tree. Loader adapters register it and reconcile
its picker commands with the server-provided admin branch. Forge also registers the `maybe_float`
argument type used by the command tree.

## Loader integration

Forge adapters primarily use Forge entity, interaction, tracking, reload, lifecycle, and rendering
events. Fabric adapters use Fabric callbacks where available and Mixins for persistent entity data,
hurt/heal reactions, completed trades, and renderer-dispatcher reload behavior. Both sides enter the
same common services after adapting the platform event.

Fabric configuration is loaded at client initialization or server start rather than watched for live
file changes. Its Minecraft-facing Mixins target 1.20.1 exactly.

## Automated verification

Shared GameTest assertions live in 12 `*Logic` classes under `common/src/gametest/java`. Forge and
Fabric provide thin annotated wrappers. The assertions cover configuration selection, eligibility,
variants, persistence and serialization, recipes, picker operations, harvesting, behaviors, and
network protocol identifiers.

Fabric GameTest wrapper classes are listed explicitly in its test metadata. Forge discovers its
annotated wrappers through the GameTest development mod.

## Operational boundaries

- Optional renderer integrations may omit eyes when their model family or attachment cannot be
  resolved.
- Changes to third-party models can invalidate bundled attachment tokens or placement geometry.
- Entity persistence on Fabric is provided by the mod's Mixin rather than a component dependency.
- Network wire incompatibilities require a protocol-version change; display version is not the wire
  compatibility contract.
- The root `src/` tree, `working-build-env/`, root `run/`, and root-level IDE launch files are not active
  build inputs.
- Pre-release data and protocol formats have no compatibility layer.
