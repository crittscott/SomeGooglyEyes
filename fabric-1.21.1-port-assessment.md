# Fabric 1.21.1 Port Assessment

## Purpose and scope

This document records the known technical shape of the Some Googly Eyes Fabric port from Minecraft
1.20.1 to Minecraft 1.21.1. It is an assessment, not an execution log or a substitute for the staged
plan in `fabric-1.21.1-port-plan.md`.

The target build environment now syncs with Java 21, Minecraft 1.21.1, Fabric Loader 0.19.3, Fabric
API 0.116.15+1.21.1, Architectury API 13.0.8, and GeckoLib 4.7.4. The Gradle project contains
`common`, `fabric`, `forge`, and a scaffold-only `neoforge` module. Build-environment migration is
complete for this phase; source compilation has not been attempted.

The port is Fabric-first. Common production and GameTest code are in scope because most of the mod
lives in `common`. Forge and NeoForge runtime implementation are later ports and are not regression
gates for Fabric. Changes made for Fabric should remain loader-neutral where the existing project
seams permit, but the Fabric port does not repair either Forge-family loader.

This is an unreleased mod. Preserving 1.20.1 save data, item data, wire data, or binary compatibility
is not required. Player-visible behavior in `player-view.md` remains the target unless the user
explicitly accepts a change.

## Current project surface

The current source inventory is:

| Source set | Java files | Role |
| --- | ---: | --- |
| `common/src/main/java` | 106 | Shared gameplay, state, codecs, networking, commands, picker, rendering, and attachment logic |
| `fabric/src/main/java` | 23 | Fabric entry points, events, configuration, platform implementations, Mixins, and GeckoLib integration |
| `common/src/gametest/java` | 12 | Shared GameTest logic classes |
| `fabric/src/gametest/java` | 12 | Fabric discovery wrappers |

The existing test layout contains 77 shared public assertion methods and 78 Fabric wrapper methods.
Those counts are a coverage baseline, not permission to preserve obsolete signatures mechanically.

The Gradle sync deliberately left the common Access Widener and both Forge-family Access Transformers
empty. They are valid placeholders only. Fabric rendering cannot be considered ported until the
1.21.1 Access Widener has been rebuilt from the members the renderer actually requires.

## Overall port shape

The expected sequence is:

1. Establish a compiler baseline and classify errors by subsystem.
2. Port common item data, content registration, recipes, enchantment data, identifiers, and changed
   vanilla signatures.
3. Port common server services, datapack configuration, commands, picker operations, and networking.
4. Port Fabric bootstrap, server events, configuration, platform implementations, and Mixins.
5. Port shared and Fabric client rendering, attachment resolution, picker UI, item rendering, and
   optional GeckoLib integration; rebuild the Fabric Access Widener.
6. Migrate resources and obtain passing common and Fabric production gates.
7. Port the shared and Fabric GameTest source sets.
8. Stabilize the Fabric GameTest server.
9. Package the Fabric artifact and reconcile documentation.

A compile error is evidence that an API use is stale. It is not permission to change behavior,
delete compatibility, weaken validation, or bypass a test.

## Minecraft changes that directly affect the project

### Item appearance data components

`EyeItemProperties` stores `AppearanceOverride` under `EyeProperties` in raw `ItemStack` NBT through
`getTagElement`, `getTag`, and `getOrCreateTag`. Raw stack tags are no longer the 1.21.1 item-state
boundary.

The port needs one coherent component-backed representation shared by Googly Eyes and Slimy Eyes.
`AppearanceOverride` already has a codec and immutable value semantics, so the expected default is a
registered `somegoogly:eye_properties` data component backed by that codec. The built-in
`minecraft:custom_data` component remains an acceptable fallback only if the registered component
cannot satisfy crafting, tooltip, tint, harvest, and application paths without expanding
loader-specific code.

The following invariants must survive:

- harvested appearance remains attached to the item;
- successive crafting modifiers compose without losing unrelated components;
- crafting a Slimy Eye copies the appearance exactly;
- applying and re-harvesting an eye preserves effective iris, cornea, and glow state;
- creative eye items carry no override;
- geometry remains entity-definition data and is never stored on the item.

No compatibility reader for the 1.20.1 raw tag is required.

### Enchantments are data-driven

`OptometristEnchantment` is a constructed `Enchantment` subclass registered through an Architectury
`DeferredRegister`. Minecraft 1.21.1 represents enchantment definitions through registry data rather
than the 1.20.1 rarity/category constructor model.

The port must replace the subclass and registration path with a `somegoogly:optometrist` enchantment
definition and current holder-based access. `EyeItemService` and the creative-tab book must use the
current enchantment lookup and stack APIs.

The behavior to preserve is unusually specific:

- one level;
- shears only;
- treasure-only rather than enchanting-table generated;
- available through the intended treasure/trade mechanisms;
- right-click harvesting remains non-lethal and costs one durability.

The exact 1.21.1 data definition and acquisition semantics must be proven from permitted
documentation and runtime tests rather than inferred from the removed subclass methods.

### Recipes and crafting inputs

`EyeModifierRecipe` and `SlimyEyeRecipe` use 1.20.1 crafting containers, serializer callbacks,
resource identifiers, and network methods. The 1.21.1 recipe API uses current crafting-input and
codec/stream-codec contracts.

The port must preserve:

- dynamic appearance modification;
- preservation of unrelated item components;
- a recipe-book-visible Slimy Eye recipe;
- the two serializer ids;
- exact one-eye/one-modifier matching;
- deterministic clearing and glow/color override behavior.

The recipe JSON result shape and the resource directory also need the 1.21.1 forms.

### Identifiers and changed signatures

`ResourceLocation` constructors are private in 1.21.1. Construction sites in common, Fabric, and
GameTest code must use the appropriate factories. Known sites span configuration, commands,
behaviors, render textures, entity constants, picker packets, and tests.

Other expected common migrations include item tooltip context, crafting input, registry-holder
access, NBT accounter construction, entity save/load signatures, creative-tab output, and selected
command or rendering signatures. Mechanical changes should be grouped by subsystem rather than
performed opportunistically across the entire tree.

### Resource layout and datapack selection

The shared resources currently use:

- `data/somegoogly/recipes`;
- `data/somegoogly/structures`;
- recipe results with the old `item` field;
- `pack_format` 15.

The vanilla registry directories must be singularized and resource metadata updated for 1.21.1.

Seventy-four bundled Minecraft eye definitions select exactly version `1.20.1`. They will not resolve
for 1.21.1 until updated. Definitions for optional mods use their own release ranges and must be
reviewed separately; blindly changing those ranges would claim compatibility that has not been
tested.

Minecraft 1.21.1 also contains living entity types absent from the 1.20.1 definition set. Adding
their eye geometry is a compatibility/content task requiring visual authoring and is not required to
make the existing Fabric port compile, but final documentation must not overstate vanilla coverage.

## Shared networking

`NetworkHandler` and `ClientNetworkHandler` use Architectury 9-era `NetworkManager` registration and
manual `FriendlyByteBuf` payloads. The network surface includes:

- login hello and acknowledgment;
- eye-state snapshots;
- resolved eye-definition synchronization;
- behavior triggers;
- picker freeze, spawn, spawn-all, pose, and export requests;
- tracking-player fanout;
- bounded pending client state.

Architectury API 13 may require a different payload registration or buffer contract. The exact
change must be established from the current API and compiler diagnostics. The port must preserve:

- server authority and direction-specific receivers;
- player identity from packet context;
- authorization repeated on the server;
- bounded definition and picker payload decoding;
- disconnect on protocol mismatch or timeout;
- pending state for packets arriving before their entity;
- one full eye-state snapshot per mutation.

If the wire representation changes, the protocol identifier must change with it. No compatibility
bridge to protocol 9 is required.

Architectury API 13 supports the Fabric and NeoForge sides of this build but has no matching Forge
platform artifact for Minecraft 1.21.1. Removing or replacing Architectury runtime API usage for the
later Forge port is a separate architectural task. The Fabric port must not increase Architectury
coupling or introduce Fabric API types into common production code.

## Fabric-specific server integration

### Bootstrap and events

`SomeGooglyFabric` and `SomeGooglyFabricClient` retain recognizable Fabric entry-point shapes.
Registration APIs and callback signatures must nevertheless be confirmed for:

- resource reload listeners;
- server entity load and tracking;
- join, disconnect, datapack sync, stopping, and tick callbacks;
- living-entity death and entity-use interactions;
- server and client commands;
- client lifecycle, HUD, render, input, and item-renderer events.

The current debug logging is diagnostic behavior, not a port invariant. It may remain while useful
but must not obscure lifecycle correctness.

### Persistent entity data

`EntityPersistentDataMixin` stores one compound on every entity and injects into `saveWithoutId` and
`load`. The injected target names, arguments, and return callback types must be re-established for
1.21.1.

The persistent keys and snapshot semantics remain authoritative. Item data components do not imply
that entity state should move away from entity NBT.

### Reaction and trade Mixins

`LivingEntityReactionMixin` targets `actuallyHurt` and `heal`; `MerchantResultSlotMixin` targets
`onTake`. These are behaviorally small but mapping-sensitive. Each target must be verified at compile
and runtime. A Mixin that compiles but does not apply is a failed port.

### Fabric configuration

Fabric reads client and per-world server TOML through `FabricToml` and registers a reload listener
for eye definitions. File locations and behavior should remain unchanged. Identifier parsing,
resource-listener identity, lifecycle timing, and any registry-context parameters require porting.

## Client rendering and attachment

Rendering is the largest and highest-risk subsystem. Shared client code contains model and layer
installation, pupil physics, item rendering, picker rendering, HUD/input, model-part vocabulary, and
multiple attachment resolvers. Fabric adds renderer-map access, reload handling, client event
registration, and GeckoLib integration.

Likely change areas include:

- `EntityModel` and render-layer generic signatures;
- model render and vertex-consumer arguments;
- `LivingEntityRenderer` layer access and installation;
- renderer map access after resource reload;
- ageable model families and their baby transforms;
- `RenderType` construction for the picker gizmo;
- item-renderer registration and render method signatures;
- HUD and key-mapping callbacks;
- renderer reload Mixins;
- GeckoLib renderer, bone, baked-model, and layer APIs.

The old Access Widener named `AgeableListModel`, `AgeableHierarchicalModel`, rabbit and llama fields,
renderer maps and layers, and a private `RenderType` factory. It was intentionally cleared during
build setup. The 1.21.1 version must contain only access still required by the ported implementation.
Do not restore declarations merely because they existed in 1.20.1.

Attachment compatibility is behavior, not just compilation. A passing headless suite cannot prove
that eyes remain correctly positioned on every vanilla or optional model. The final handoff must
name manual checks for ordinary hierarchical models, age-dependent models, slime layer ordering,
players, special vanilla shapes, and optional GeckoLib models.

GeckoLib remains optional. Failure to resolve or attach an optional GeckoLib layer must not prevent
the base mod from loading.

## Existing verification assets

The GameTest suite covers:

- configuration selection and version ranges;
- eligibility and spawn gating;
- variant selection;
- eye-state overrides;
- persistence and packet serialization;
- recipes;
- picker export and freeze behavior;
- deterministic behaviors;
- loader-integrated initialization behavior.

The shared logic and Fabric wrappers both use 1.20.1 identifiers, crafting, buffers, entity APIs, and
GameTest discovery conventions. They should be ported after production code so assertions can be
adapted against established 1.21.1 boundaries.

Tests must not be deleted, disabled, undiscovered, or weakened to obtain a passing run. Packet,
item-component, enchantment, persistence, and resource-selection migrations should gain focused
coverage where the existing suite does not establish the new boundary.

## Risk ranking

| Risk | Area | Reason |
| --- | --- | --- |
| Highest | Shared rendering, attachment resolvers, and Access Widener | Broad reliance on Minecraft client internals plus visual correctness outside headless coverage |
| High | Networking and protocol handshake | Many bounded bidirectional payloads, tracking fanout, login compatibility, and pending state |
| High | Item appearance component and recipes | Central harvest-craft-apply round trip and component-preservation requirements |
| High | Data-driven Optometrist enchantment | Registration and acquisition semantics changed, not just method signatures |
| Medium-high | Fabric persistent-data and reaction/trade Mixins | Mapping-sensitive hooks whose runtime application matters |
| Medium | Fabric server/client callback wiring and configuration | Numerous loader callbacks but good existing service boundaries |
| Medium | GeckoLib 1.21.1 integration | Optional but model/API-sensitive |
| Medium | Resource layout and eye-definition version selection | Mechanical paths plus compatibility claims and visual geometry |
| Medium-low | GameTest API and discovery | Existing coverage is strong, but all wrappers and several shared helpers need API migration |
| Low | Simple identifier factories and metadata | Local and mechanical once grouped correctly |

## Non-goals

- Implementing Forge or NeoForge runtime code.
- Making Forge or NeoForge compile as a Fabric-stage regression gate.
- Supporting legacy 1.20.1 item, entity, datapack, or wire formats.
- Changing the established Gradle, loader, mappings, plugin, JDK, or dependency baseline.
- Adding new vanilla or optional-mod eye geometry merely to obtain a passing build.
- Replacing the GameTest framework.
- Redesigning player-visible eye behavior, picker commands, configuration semantics, or network
  authority to accommodate a compiler error.
- Publishing, committing, or performing any Git or GitHub operation.

## Questions deliberately deferred to evidence

Resolve these in their named stage from compiler diagnostics, permitted official documentation, and
focused tests:

- whether a registered `somegoogly:eye_properties` component cleanly replaces the old stack tag or
  whether `minecraft:custom_data` is the smaller correct boundary;
- the exact 1.21.1 Optometrist enchantment definition needed to preserve treasure/trade availability
  while excluding ordinary enchanting-table generation;
- the Architectury 13 payload registration and buffer contract;
- whether the current packet body formats can remain unchanged;
- the current entity save/load and reaction/trade Mixin descriptors;
- which old Access Widener entries remain necessary after the renderer is ported;
- which attachment resolvers survive intact and which model families require replacement logic;
- whether GeckoLib 4.7.4 preserves the required renderer-layer extension points;
- which optional-mod eye-definition ranges can honestly be advanced for 1.21.1.

If resolving one of these requires a new dependency, a build-environment change, a new
player-visible behavior, or a cross-loader architecture decision outside Fabric scope, stop for
user direction.
