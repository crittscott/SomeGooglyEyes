# Forge 1.21.1 Port Assessment

## Purpose and scope

This document records the known technical shape of the Some Googly Eyes Forge port for Minecraft
1.21.1. It follows the NeoForge port and is governed by `forge-1.21.1-port-plan.md` and
`forge-1.21.1-port-process.md`.

Fabric is the completed 1.21.1 reference. NeoForge must also be complete before Forge execution
begins. Forge is not merely a package rename: the common module currently relies on Architectury API
13 runtime facilities, and Architectury publishes no Forge 1.21.1 runtime artifact for that line.

The established Forge target is Java 21, Minecraft 1.21.1, Forge 52.1.16, Architectury Loom
1.17.491, and GeckoLib 4.7.4. Versions and module layout are frozen unless the user explicitly
reopens build-environment work.

## Current project surface

| Source set | Current state |
| --- | --- |
| `common/src/main/java` | Passing 1.21.1 code with Architectury runtime registration/networking/environment use |
| `fabric/src/main/java` | Completed runtime and mandatory regression target |
| `neoforge/src/main/java` | Must be completed before Forge begins; mandatory regression target afterward |
| `forge/src/main/java` | 19 Java files from the 1.20.1-era Forge implementation |
| `common/src/gametest/java` | 77 shared assertions |
| `forge/src/gametest/java` | 13 stale Forge files, including wrappers and a test mod entry point |

The Forge module has 1.21.1 build metadata, an empty Access Transformer, and no Architectury runtime
dependency. Its Java source still imports 1.20.1 Forge APIs and removed Architectury Forge glue.

## Central architectural blocker

Common runtime dependencies fall into these categories:

- Registry and creative-tab facilities: `DeferredRegister`, `RegistrySupplier`, and
  `CreativeTabRegistry`.
- Networking: `NetworkManager` registration, send/context operations, and payload helpers.
- Environment lookup: `Platform` and `Env`.
- Build-time injection: `@ExpectPlatform` seams.

Forge cannot load code that retains the first three categories without a supported Forge runtime
implementation. The Forge port must replace them with vanilla or project-owned loader-neutral
boundaries and implement those boundaries for Fabric, completed NeoForge, and Forge.

`@ExpectPlatform` is a separate question. It may remain if the Forge transformation produces an
artifact with no missing runtime class and all implementations resolve. Stage 0 must prove or reject
that option; it must not remove injection merely because the runtime library is unavailable.

Shared architectural edits make both Fabric and NeoForge regression gates. Forge is incomplete if
either prior loader breaks.

## Value and limits of the old Forge source

The old source describes behavior and loader responsibilities: bootstrap, config, entity
persistence, tracking, server events, item interactions, reactions, client events, picker input,
renderer access, item colors, and GeckoLib layers. It may guide the new implementation.

It is not API evidence. Package names, event types, mod-bus access, registries, networking,
configuration, rendering, GameTest discovery, and client extensions must be established for Forge
52.1.16 from compiler diagnostics and permitted official documentation/source. Stale code should be
replaced coherently rather than patched until it compiles.

## Implementation surfaces

### Registration and environment replacement

Common code must retain stable, loader-neutral access to registered items, data components, recipe
serializers, and the creative tab. The replacement should follow current vanilla/loader registration
lifecycles and avoid a new loader-specific global registry singleton in common.

Client receiver registration must be initiated explicitly from each loader's client bootstrap rather
than discovered through a common Architectury environment lookup if that lookup is removed.

### Networking replacement

The current protocol uses typed `CustomPacketPayload` wrappers with protocol version 9. The
replacement must preserve ids, codecs, byte representation, directionality, bounds, login
hello/acknowledgment and timeout, server-derived sender identity, tracking fanout, and bounded pending
client state.

The correct abstraction is the smallest project-owned boundary that lets common define payload
semantics while loaders register receivers and send payloads through their native facilities.
Changing the carrier is not permission to change packet bodies. If bytes change, update the protocol
contract and tests deliberately; no legacy bridge is required.

### Forge bootstrap, config, and server events

Forge needs a current `@Mod` entry point, registration lifecycle, client-side isolation, server and
client config, reload listener, entity load/tracking, connection/datapack lifecycle, commands, ticks,
stop cleanup, item interactions, drops, damage, healing, and trade events.

Entity persistence remains behind `EntityPersistentData`. Use a Forge-native facility if it
preserves the existing compound keys and save/load behavior. Do not migrate entity state merely
because item state uses data components.

### Client rendering and access

The shared renderer is already 1.21.1 code. Forge must install and reset layers, supply renderer-map
access, register the 3D item renderer and tint, connect picker keys/HUD/input and client commands,
and translate only demonstrably required common Access Widener members into the Forge Access
Transformer.

GeckoLib remains optional. The Forge bridge must not load GeckoLib classes when absent and must
isolate unsupported renderer failures.

### Resources, metadata, and tests

Forge must consume the existing common 1.21.1 resources and use current Forge 52 metadata. The
Optometrist data, tags, recipes, 74 vanilla eye definitions, and translation keys are shared and
should not be forked.

Forge GameTests should expose all 77 shared assertions plus a Forge persistence round trip. The old
13-file test surface is a discovery reference, not a reason to retain stale annotations. A nonzero
discovered count and clean dedicated-server exit are mandatory.

## Risk ranking

| Risk | Area | Reason |
| --- | --- | --- |
| Highest | Common networking replacement | Cross-loader protocol, login, context, and tracking behavior |
| Highest | Common registration replacement | Central content handles and lifecycle across three loaders |
| High | Prior-loader regressions | Shared changes can invalidate completed Fabric and NeoForge artifacts |
| High | Forge client access/rendering | Access Transformer and event APIs are mapping-sensitive and visually unproven |
| High | Dedicated-server classloading | Client separation must survive new explicit bootstrap boundaries |
| Medium-high | Persistent state and server events | Silent lifecycle omissions can alter gameplay or saved state |
| Medium | GameTest configuration/discovery | Existing wrappers target the old loader API |
| Medium | Optional GeckoLib | Loader and renderer API sensitivity |
| Low | Common gameplay/resources | Already verified on 1.21.1 and should remain unchanged |

## Non-goals

- Starting before the NeoForge completion handoff.
- Reverting common code to Minecraft 1.20.1 or preserving unreleased legacy formats.
- Keeping Architectury runtime calls merely to minimize edits when Forge cannot supply them.
- Removing useful loader-neutral seams or redesigning gameplay.
- Changing versions, dependencies, repositories, plugins, mappings, wrapper, or module layout.
- Weakening tests or accepting Fabric/NeoForge regressions.
- Adding content geometry, claiming optional-mod compatibility, publishing, or using Git/GitHub.

## Questions deferred to evidence

- Whether transformed `@ExpectPlatform` seams can remain without an Architectury Forge runtime.
- The smallest registration abstraction compatible with Fabric, NeoForge, and Forge lifecycles.
- The Forge 52 custom-payload registration and login/play boundaries.
- Whether protocol 9 bodies can remain byte-identical after transport replacement.
- Current Forge config, entity persistence, tracking, trade, and client event facilities.
- Which old Forge Mixins/events can be deleted, retained, or replaced.
- Exact Access Transformer rules and item-renderer/client-command hooks.
- Forge GameTest discovery and dedicated-server configuration under the active Loom version.

Resolve these only from local source, compiler evidence, official published API documentation/source,
and focused tests. Never inspect decompiled, remapped, cached Minecraft or Forge sources, bytecode,
or unpacked artifacts.
