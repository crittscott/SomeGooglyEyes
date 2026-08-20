# Some Googly Eyes As-Built Orientation

This document describes the active source architecture. `player-view.md` is the player-visible
behavior contract; `build-env.md` describes Gradle and packaging; `fabric-port-handoff.md` records the
current verification state. Code is authoritative when documentation disagrees.

## Project at a glance

Some Googly Eyes is an unreleased Minecraft 1.20.1 mod targeting Forge 47.4.10 and Fabric Loader
0.19.3/Fabric API 0.92.11 under Architectury API 9.2.14. It uses Java 17, mod id `somegoogly`, root
package `com.github.crittscott.somegoogly`, and version `0.8.1`.

The active project has three modules: `common`, `forge`, and `fabric`. The old root `src/` tree remains
as an inactive legacy reference and is not compiled by the current Gradle project.

The server owns eligibility, spawn rolls, persistent state, item verbs, behaviors, picker authority,
and datapack definitions. The client owns model attachment, rendering, wobble simulation, inspection,
and picker authoring UI. The mod registers two items, one enchantment, one creative tab, two recipe
serializers, Forge's `maybe_float` command argument, server/client commands, and cross-loader custom
network payloads. It does not register blocks, entities, menus, particles, or sounds.

## Source boundaries

| Source tree | Responsibility |
| --- | --- |
| `common/src/main/java` | Loader-neutral gameplay, state, codecs, registration, server services, packet logic, and client code that needs only vanilla/Architectury |
| `common/src/loader/java` | Single-source client code compiled inside each loader's transformed Minecraft view; resolvers, render layers, picker rendering/input/commands, Gecko bridge |
| `common/src/main/resources` | Shared assets, recipes, eye datapacks, language, structure fixture |
| `common/src/gametest/java` | Loader-neutral GameTest assertion logic |
| `forge/src/main` | Forge entry point, events, config storage, platform implementations, metadata, AT |
| `fabric/src/main` | Fabric entry points, callbacks, config storage, platform implementations, metadata, AW, Mixins |
| `forge/src/gametest` / `fabric/src/gametest` | Thin loader-specific GameTest wrappers and dev-mod metadata |

Common main must not import Forge, Fabric API, or GeckoLib and must not call Forge-patched or
AT/AW-only vanilla members. Genuine loader differences use thin adapters or Architectury
`@ExpectPlatform` stubs. Code shared in meaning but dependent on widened/optional types goes under
`common/src/loader/java`, which each loader compiles directly.

## Initialization and registration

Each loader calls `SomeGooglyCommon.init()` once. It registers:

- C2S network receivers and protocol negotiation;
- `googly_eye` and `slimy_eye`;
- `optometrist`;
- the `googly` creative tab;
- `eye_modifier` and `slimy_eye` recipe serializers.

These registries use Architectury `DeferredRegister`; the creative tab uses
`CreativeTabRegistry`. Forge separately registers the `maybe_float` argument type because the picker
command tree historically uses that Forge registry object.

`GooglyEyeItemFactory` is the one item-construction platform boundary. Forge creates an anonymous
`GooglyEyeItem` subclass whose patched `initializeClient` method supplies the 3D renderer. Fabric uses
the plain shared item and registers the renderer from its client entry point.

Optometrist is rare, treasure-only, and constructed with vanilla `EnchantmentCategory.BREAKABLE`.
Its `canEnchant` override restricts actual applicability to `ShearsItem` on both loaders.

## Config and eye definitions

`ServerConfig` and `ClientConfig` expose immutable, validated runtime values through `ConfigValue<T>`.
Loader storage is separate:

- Forge `ForgeConfigSpec` adapters update those runtime values on config load/reload events.
- Fabric reads a narrow TOML subset. Client values come from `somegoogly-client.toml`; server values
  come from each world's `serverconfig/somegoogly-server.toml` at server start.

Server config owns global enablement, global and per-entity eye percentages, death-harvest chance,
behavior timing/pools, reaction switches/chances, and the destructive picker spawn-all gate. Client
config owns global display disablement and disabled entity/mod lists.

Eye placement files are server datapack resources under `data/<namespace>/eyes/*.json`. The shared
`EyeConfigReloadListener` parses all candidate versions, uses `ModVersionLookup` to select the matching
entry for each namespace, validates them, and atomically replaces `ServerEyeConfigs`. Forge and Fabric
provide their loader-specific mod-version lookup. The server serializes the resolved runtime config set
to clients; clients do not independently select datapack versions.

## Persistent and portable state

`EyeState` is the sole gameplay API for entity eye state. All entity NBT goes through
`EntityPersistentData`:

- Forge returns its patched persistent compound.
- Fabric's `EntityPersistentDataMixin` owns a compound on every `Entity`, persists it beneath
  `somegoogly:persistentData`, and restores it on load.

The eye keys are:

- `somegoogly:hasGooglyEyes` — authoritative on/off state;
- `somegoogly:variantRoll` — stable weighted placement-variant roll;
- `somegoogly:eyeOverrides` — optional appearance override compound.

`EyeState` setters immediately synchronize the full current state to tracking clients. First entity
load initializes the on/off decision and variant roll only if the state lacks the eye key, preserving
the result across saves, chunk reloads, and dimension changes.

Eye items carry portable `AppearanceOverride` data on the stack. Harvest captures the effective
appearance from head 0/eye 0 after layering config appearance and entity override. Crafting and slimy
application preserve that payload.

Picker freeze markers use the same entity-persistence boundary but are transiently reconciled by
`PickerFreezeService` when a mob loads, a player leaves, or the server stops, so a crash or disconnect
does not permanently strand `NoAI` state.

## Eligibility and spawn decisions

`ServerServices.onLivingEntityLoaded` makes the one-time decision:

1. Players never receive naturally rolled eyes.
2. Global server enablement must be on.
3. `ServerEyeConfigs.canEverWearEyes` must find at least one enabled config independent of age.
4. An exact entity percentage overrides wildcard entries; the first matching wildcard overrides the
   global percentage.
5. The entity's random source supplies the percentage roll and an independent variant roll.

This decision is lifetime state. Config changes affect new entities, not already initialized ones.
Mid-life changes go through slimy eyes, shears, picker/admin operations, or recipes.

Client render gating additionally respects client-local disabled entity/mod lists, the client global
disable switch, config age constraints, spectator/invisibility conditions, and attachment resolution.

## Applying and harvesting eyes

`EyeItemService` owns the loader-neutral verbs.

- A slimy eye owns a living-entity right-click before the target's ordinary interaction. If the target
  is eligible and currently eyeless, the server applies the stack appearance, rolls a fresh placement
  variant, enables eyes, and consumes one item unless the player is creative.
- Optometrist shears right-click an eyed configured entity to drop one portable eye, clear the entity
  appearance, turn eyes off, and damage the shears.
- A direct player melee kill made with shears has the configured chance to drop one portable eye and
  damage the shears. Optometrist is not required for the death path.

Forge event objects and Fabric callbacks only adapt these operations. Authorization and mutation stay
in common code.

## Behaviors

`EyeBehaviors` registers deterministic behavior definitions: blink, cross-eye, side-eye, stare, grow,
swirl, and the dormant color-change behavior. A `BehaviorInstance` combines id, duration, seed, and
elapsed time; clients derive animation continuously from those values rather than receiving per-tick
motion packets.

`ServerBehaviorScheduler` tracks only watched, eyed entities, schedules ambient behaviors, and emits
event-driven reactions. Player damage may trigger grow. Healing and completed villager/trader trades
may trigger swirl under server config and cooldown rules.

Forge uses living/trade events. Fabric uses `LivingEntityReactionMixin` for post-hurt/post-heal and
`MerchantResultSlotMixin` for completed trades. Both call the same scheduler methods.

The client `ClientEyeRuntime` owns weak per-entity `GooglyTracker` instances and advances physical eye
wobble plus active behavior influences. Disconnect and resource/config replacement clear transient
client state.

## Rendering and model attachment

`ClientRenderLayers.install` walks the entity renderer map after renderers are created or rebuilt. It
adds `LayerGooglyEyes` and the picker layer to compatible living renderers, guards against duplicate
installation with weak identity state, and inserts before `SlimeOuterLayer` so eyes are not hidden by
the translucent outer cube.

Forge invokes installation from `EntityRenderersEvent.AddLayers`. Fabric injects after
`EntityRenderDispatcher#onResourceManagerReload`, when the rebuilt renderer map is available.

Resolvers map datapack part tokens onto runtime model parts:

- hierarchical and ageable model families;
- child maps and generic model-part trees;
- Rabbit/Llama hand-rolled transforms;
- Twilight Forest special cases;
- reflected Citadel/LLibrary-family boxes;
- GeckoLib bones.

The Forge AT and Fabric AW expose the private/protected vanilla members needed by each loader.
`ClientRendererAccess` normalizes Forge's patched renderer access against Fabric's widened renderer
maps and `addLayer` method. Resolver outputs are memoized by model identity and invalidated with
renderer/runtime resets.

GeckoLib is a soft dependency. Loader gates first check whether `geckolib` is loaded, then invoke the
shared Gecko bridge defensively. Absence or an integration failure falls back to no Gecko layer instead
of preventing base-mod startup.

The 3D `googly_eye` item uses `GooglyEyeItemRenderer`. `slimy_eye` uses its ordinary item model with
iris tint index 2 derived from the stack appearance.

## Networking

`NetworkHandler` uses Architectury `NetworkManager`. Protocol version `7` appears in every gameplay
payload id (`somegoogly:v7/...`). Two stable ids negotiate compatibility:
`somegoogly:protocol_hello` and `somegoogly:protocol_ack`.

At join, the server sends its version and starts a 100-tick timeout. A matching client acknowledges,
the server marks it ready, and then sends resolved eye configs. A mismatch on either side, or no
acknowledgement, disconnects with a clear protocol message. This network protocol—not the display mod
version—is the compatibility contract.

S2C packets:

- eye state;
- resolved eye configs;
- behavior trigger.

C2S picker requests:

- freeze/release;
- spawn one;
- spawn all;
- set mob pose;
- export.

Architectury registration locks each receiver to C2S or S2C. Every picker handler obtains the sender
from the server packet context and performs creative/feature authorization server-side. Client checks
are UX only.

`NetworkTracking` hides distribution differences: Forge uses its tracking distributors, Fabric uses
`PlayerLookup.tracking`. Direct player state/config sends use Architectury directly.

## Picker and commands

The client `/sg` tree and picker keyboard UI are authoring tools. Picker client state owns the selected
entity, model part, eye draft, preview layers, lock state, and export view. The server owns all world
mutation and file export.

Freeze preserves the mob's previous `NoAI` value and releases on unlock, disconnect, load recovery, or
server stop. Spawn-all requires both creative mode and the server `allowSpawnAll` opt-in. Export is
rate-limited, path-constrained to the world datapack area, and writes loader-neutral JSON through the
shared service.

Client commands register through Architectury's client-command event on both loaders. The Forge-only
`maybe_float` argument registry remains for existing server-visible command serialization needs; shared
client command code uses the Architectury client source stack and resource-location suggestions.

## Loader event map

| Semantic event | Forge | Fabric |
| --- | --- | --- |
| Entity first load | `EntityJoinLevelEvent` | `ServerEntityEvents.ENTITY_LOAD` |
| Living entity use | `PlayerInteractEvent.EntityInteract` adapter | `UseEntityCallback` |
| Death harvest | `LivingDropsEvent` adapter | `ServerLivingEntityEvents.AFTER_DEATH` |
| Hurt/heal reaction | Forge living events | `LivingEntityReactionMixin` |
| Trade reaction | `TradeWithVillagerEvent` | `MerchantResultSlotMixin` |
| Start/stop tracking | Forge player tracking events | Fabric `EntityTrackingEvents` |
| Datapack reload | `AddReloadListenerEvent` | `ResourceManagerHelper` |
| Config sync | `OnDatapackSyncEvent` | `SYNC_DATA_PACK_CONTENTS` |
| Join/leave | Forge player events | Fabric play-connection events |
| Server tick/stop | Forge server events | Fabric lifecycle events |
| Render layer install | Forge AddLayers event | renderer-dispatcher Mixin |
| Client tick/HUD/input/color | Forge client events | Fabric client APIs |

## Automated verification

Shared assertion bodies live in 12 `*Logic` classes under `common/src/gametest/java`. Each loader has
thin wrappers exposing 69 `@GameTest` methods. Coverage includes config parsing/selection, eligibility,
variant selection, state and appearance serialization, recipes, picker freeze/export, spawn gating,
behavior determinism, and network protocol id invariants.

Fabric wrappers implement `FabricGameTest` and are explicitly listed in the GameTest dev-mod metadata.
Forge wrappers retain the holder annotations and are discovered through its GameTest dev mod.

The present source tree has been structurally audited but not compiled or executed after the port.
See `fabric-port-handoff.md` for the verification matrix and highest-risk unverified hooks.

## Known boundaries

- Fabric TOML values reload at client initialization or server start, not through a live file watcher.
- GeckoLib compatibility is optional and defensive; unsupported custom renderers simply receive no
  Gecko eye layer.
- Fabric entity persistence intentionally mirrors Forge's private persistent compound instead of
  adding a third-party component dependency.
- Several Fabric parity hooks are Mixins against Minecraft 1.20.1 internals, which is why the Minecraft
  version is exact.
- The legacy root `src/`, `working-build-env/`, old root `run/`, and old IDE launch files are references,
  not active build inputs.
- No compatibility layer preserves pre-release data/schema versions; edit the format and bundled data
  together before release.

## Maintenance checklist

- Put loader-neutral logic in common main; keep loader callbacks thin.
- Put AT/AW- or Gecko-dependent shared source in `common/src/loader/java`.
- Keep Forge AT and Fabric AW member lists aligned.
- Route every entity persistent-data access through `EntityPersistentData`.
- Bump `NetworkHandler.PROTOCOL_VERSION` for any incompatible packet field/meaning change; keep hello
  and ack ids stable and gameplay ids versioned.
- Register S2C receivers only from physical-client initialization.
- Keep C2S picker authorization on the server.
- Add new GameTest logic once in common and a wrapper method on each loader; also list any new Fabric
  wrapper class in its metadata.
- Test dedicated servers so client classes cannot leak through common initialization.
- Re-run persistence, tracking, baby-model, slime-order, resource-reload, picker cleanup, and optional
  GeckoLib checks after touching their respective adapters.
- Never make active changes in the legacy root `src/` tree.
