# Fabric Port Handoff

Snapshot of the active worktree through 2026-08-20. This replaces the earlier mid-Phase-3 handoff.

## Current state

The Fabric port is implemented at source level across registration, persistence, configuration,
networking, authoritative server behavior, client rendering, picker tooling, optional GeckoLib
integration, and GameTest wrappers. Forge now uses the same shared implementations for those systems,
with thin Forge adapters where the loader APIs differ.

The current worktree has **not completed** compilation and has not been run after these changes. Repository instructions in
`CLAUDE.md` prohibit build, run, GameTest, decompile, archive, and Gradle-cache inspection commands
unless the user separately requests them. Treat every API signature and Mixin target listed under
"Manual verification" as source-reviewed but runtime-unverified.

The user started the first full build on 2026-08-20. It reached `:common:compileJava` and exposed a
missing `EnchantmentCategory` import in `OptometristEnchantment`; that import is now restored. The
next runs confirmed that common compilation passes, then exposed separate loader errors. The Forge
tracking distributor needed a `Supplier<Entity>`; both loaders exposed two picker command typing
errors; Fabric also showed that renderer enumeration and `addLayer` had relied on Forge-patched
members. Those are now corrected with a `ClientRendererAccess` platform boundary, additional Fabric
Access Widener entries, a typed `PickerFreezePacket` helper, direct Brigadier argument retrieval, and
the required Forge supplier lambdas. Neither loader build has yet been rerun past those corrections.

The old root `src/` tree is still present as a legacy, inactive reference. The root Gradle project only
includes `common`, `forge`, and `fabric`; do not edit `src/` when changing the live mod.

## Implemented architecture

### Shared registration

`SomeGooglyCommon.init()` registers the Architectury networking receivers and all shared registries.
Items, the Optometrist enchantment, recipes, and the creative tab use Architectury
`DeferredRegister`/`CreativeTabRegistry` from `common/src/main`.

`GooglyEyeItemFactory` is an `@ExpectPlatform` boundary because Forge patches
`Item#initializeClient` onto the vanilla item class. Forge returns an anonymous item subclass with the
custom renderer; Fabric registers the shared renderer with `BuiltinItemRendererRegistry`.

Optometrist uses vanilla `EnchantmentCategory.BREAKABLE` and overrides `canEnchant(ItemStack)` to
narrow eligibility to shears. The rejected Forge-only custom-enum factory has been removed.

### Persistent entity state

All active code accesses entity-owned NBT through `platform/EntityPersistentData`.

- Forge delegates to its patched `Entity#getPersistentData()`.
- Fabric mixes a private `CompoundTag` field into `Entity`, writes it from `saveWithoutId`, restores it
  from `load`, and exposes it through `FabricEntityPersistentDataHolder`.

The Fabric key is `somegoogly:persistentData`. The compound contains the existing eye-state and picker
keys unchanged, so the common `EyeState` and picker services do not know which loader owns storage.

### Shared code that needs widened Minecraft access

The renderer/resolver, picker-rendering, client-command, and Gecko bridge classes which require
loader-transformed Minecraft or optional GeckoLib types live in `common/src/loader/java`. Both loader
projects add that directory to their own `main` Java source set, so it is one source of truth but
compiles inside the Forge AT or Fabric AW view rather than during `common:compileJava`.

Forge keeps `META-INF/accesstransformer.cfg`; Fabric uses `somegoogly.accesswidener`. Their widened
members cover `ModelPart.children`, ageable model head/body and baby transforms,
`LivingEntityRenderer.layers`, and the Rabbit/Llama model-part fields. Fabric additionally widens the
renderer-dispatch maps and `LivingEntityRenderer.addLayer`; Forge supplies equivalent public access
through its patches. `ClientRendererAccess` hides that asymmetry from the shared installer.

### Configuration and datapacks

`ServerConfig` and `ClientConfig` contain loader-neutral `ConfigValue<T>` runtime values.

- Forge retains `ForgeConfigSpec` in `ForgeServerConfig` and `ForgeClientConfig`, copying loaded values
  into the shared runtime stores on config events.
- Fabric uses the narrow `FabricToml` parser for exactly this mod's booleans, bounded integers, and
  string arrays. Client config is `config/somegoogly-client.toml`; server config is per-world at
  `serverconfig/somegoogly-server.toml`.
- `ModVersionLookup` is an `@ExpectPlatform` boundary backed by Forge `ModList` or Fabric Loader.
- The shared `EyeConfigReloadListener` is registered directly on Forge and through
  `FabricEyeConfigReloadListener`/`ResourceManagerHelper` on Fabric.

Fabric configuration is restart/world-open scoped; it does not currently watch TOML files for live
reload. Datapack eye definitions still reload through Minecraft's resource reload lifecycle.

### Networking

All packet records, codecs, authorization checks, and handlers live in `common/network`. Transport is
Architectury `NetworkManager`; Forge `SimpleChannel` is gone.

Protocol version is `7`:

- stable negotiation ids: `somegoogly:protocol_hello`, `somegoogly:protocol_ack`;
- gameplay ids: `somegoogly:v7/...`;
- server starts a hello/ack exchange at player join and disconnects a client after 100 ticks without a
  valid acknowledgement;
- the client and server both reject a mismatched version with an explicit message;
- S2C receivers are registered only from the physical-client entry points;
- C2S picker requests are registered by common initialization and still authorize creative mode on
  the server;
- tracking distribution is an `@ExpectPlatform` boundary: Forge uses its tracking packet distributor,
  Fabric uses `PlayerLookup.tracking`.

The wire fields inside the eight gameplay packets are unchanged. A new source-level GameTest checks
that the handshake ids stay stable, every gameplay id carries the protocol prefix, and all ids are
unique.

### Authoritative server behavior

`ServerServices` owns first-load eye rolls, join/leave handling, protocol ticking, config sync,
tracking sync, behavior scheduler hooks, picker cleanup, and shutdown cleanup. `EyeItemService` owns
slimy-eye application plus enchanted-shears and death harvesting.

Forge event subscribers are now thin adapters. Fabric uses API callbacks for entity use, entity load,
death, connections, datapack sync, server ticks/lifecycle, commands, and tracking. Two Fabric Mixins
cover API gaps:

- `LivingEntityReactionMixin`: post-hurt and post-heal behavior triggers;
- `MerchantResultSlotMixin`: completed villager/trader trade behavior trigger.

`EntityPersistentDataMixin` and the client renderer reload Mixin are the other two Fabric Mixins.

### Client and picker

`ClientEyeRuntime`, `EyeInspector`, packet application, render-layer installation, picker state/input/
HUD, and client commands are shared. Loader entry points only register their native callbacks.

Fabric registers key bindings, consumes key presses at end-client-tick, renders the picker HUD,
clears state on disconnect, installs the slimy-eye color provider, and attaches the built-in renderer
for the 3D eye item. `EntityRenderDispatcherMixin` installs eye and picker layers after renderer maps
are rebuilt. Layer insertion preserves the special ordering before `SlimeOuterLayer`.

GeckoLib remains optional. Both loaders compile against version `4.7.4`; runtime use is gated by each
loader's loaded-mod check. Fabric metadata only suggests GeckoLib.

Client `/sg` commands use Architectury's client-command event and source stack. Entity-type arguments
use `ResourceLocationArgument` plus summonable-registry suggestions, avoiding the server-only command
source assumptions in vanilla's resource argument helper.

## GameTests

There are 69 wrappers per loader across 12 classes. Assertion logic lives once under
`common/src/gametest/java`; Forge and Fabric wrappers delegate one method at a time. Fabric lists all
12 wrapper classes under its `fabric-gametest` entrypoint. Forge uses the separate
`somegoogly_gametest` dev mod.

No GameTests have been run against the present worktree.

## Source-level checks completed

- Fabric main metadata, Mixin metadata, and GameTest metadata parse as JSON.
- Forge and Fabric each expose 69 `@GameTest` wrapper methods and the same wrapper class set.
- Common main has no Forge, Fabric, or GeckoLib imports.
- No active common-main class imports a loader-only resolver, picker renderer, Gecko bridge, render
  layer installer, or client-command type.
- Combined common/loader/Forge and common/loader/Fabric source sets contain no duplicate top-level
  fully-qualified class names.
- Active code contains no old `SimpleChannel`, `NetworkHandler.INSTANCE`, temporary Fabric no-op, or
  obsolete enchantment-category factory.
- Direct `Entity#getPersistentData()` use is confined to the Forge platform implementation.
- `git diff --check` reports no whitespace errors.

These checks do not establish compilation or runtime correctness.

## Manual verification, in order

Run these only when separately authorized:

1. `./gradlew :common:compileJava`
2. `./gradlew :forge:compileJava :fabric:compileJava`
3. `./gradlew :forge:build :fabric:build`
4. `./gradlew :forge:runGameTestServer`
5. `./gradlew :fabric:runGameTestServer`

Then launch both clients and verify:

1. Main menu/load on each loader with and without GeckoLib present.
2. Dedicated-server startup on each loader, ensuring no physical-client class loads.
3. Join handshake and config sync; deliberate protocol mismatch gives the explicit disconnect.
4. Natural eye rolls persist across save/reload and dimension travel on Fabric.
5. Existing eyed mobs synchronize when a player begins tracking them.
6. Slimy-eye application wins over a target's ordinary right-click action.
7. Optometrist shears harvest non-lethally; plain shears can harvest only through the configured
   direct-melee death chance; drops and durability are correct.
8. Hurt, heal, and completed trades trigger the same eye reactions as Forge.
9. Vanilla mobs render correctly, including baby `AgeableListModel`, baby Sniffer,
   Rabbit/Llama, slime layer ordering, and resource-reload layer reinstallation.
10. The eye item uses its 3D renderer and the slimy-eye iris tint is correct.
11. Picker keys, HUD, freeze/release, spawn, pose, export, and `/sg` client commands work; non-creative
    C2S requests are refused server-side.
12. GeckoLib entities enumerate bones and receive the eye layer when GeckoLib is loaded.

## Highest-risk unverified points

- Mojang-mapped Fabric Mixin target names/descriptors: `Entity#saveWithoutId`, `Entity#load`,
  `LivingEntity#actuallyHurt`, `LivingEntity#heal`, `MerchantResultSlot#onTake`, its `merchant` field,
  and `EntityRenderDispatcher#onResourceManagerReload`.
- Architectury 9.2.14 callback and buffer behavior in the new handshake/tracking paths.
- Fabric built-in item renderer, HUD, color-provider, and key-binding callback signatures.
- Runtime equivalence of the Fabric death/trade/reaction hook timing.
- Optional GeckoLib class loading when GeckoLib is absent.

If compilation exposes a signature mismatch, fix the narrow adapter or Mixin descriptor; do not move
loader APIs back into common main or duplicate the shared gameplay implementation.

## Remaining task

The source implementation and documentation are complete. The remaining work is verification and any
narrow corrections it reveals. Do not describe the Fabric port as released or behaviorally verified
until both loader builds, both 69-test GameTest runs, dedicated-server starts, and the manual client
matrix pass.
