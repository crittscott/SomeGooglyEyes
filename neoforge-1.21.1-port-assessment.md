# NeoForge 1.21.1 Port Assessment

## Purpose and scope

This document records the known technical shape of the Some Googly Eyes NeoForge port for Minecraft
1.21.1. It is an assessment, not an execution log or a substitute for
`neoforge-1.21.1-port-plan.md`.

Fabric is the completed 1.21.1 reference implementation. NeoForge is the next runtime target. Forge
follows NeoForge and is governed by its own document set. Player-visible behavior in
`player-view.md` and the implementation invariants in `as-built.md` remain authoritative.

The established environment is Java 21, Minecraft 1.21.1, NeoForge 21.1.248, Architectury API
13.0.8, Architectury Loom 1.17.491, and GeckoLib 4.7.4. The NeoForge module already has build and
metadata scaffolding but no Java implementation or GameTest source set.

## Current project surface

| Source set | Java files | Role |
| --- | ---: | --- |
| `common/src/main/java` | 106 | Passing shared 1.21.1 gameplay, networking, rendering, and picker code |
| `fabric/src/main/java` | 23 | Completed loader reference and regression target |
| `neoforge/src/main/java` | 0 | Runtime implementation to create |
| `common/src/gametest/java` | 12 | 77 passing shared assertions |
| `fabric/src/gametest/java` | 12 | 78 passing Fabric wrappers |
| `neoforge/src/gametest/java` | 0 | Discovery wrappers and loader-specific persistence test to create |

`neoforge/build.gradle`, `META-INF/neoforge.mods.toml`, and an empty Access Transformer are present.
The module already depends on `architectury-neoforge:13.0.8` and compile-only GeckoLib 4.7.4.

## Cross-loader architecture assessment

The common module uses Architectury in three distinct ways:

1. `@ExpectPlatform` seams for persistent entity data, tracking, version lookup, item construction,
   renderer access, and GeckoLib compatibility.
2. Runtime registration facilities in `ModItems`, `ModDataComponents`, `ModCreativeTabs`, and
   `ModRecipes`.
3. Runtime networking and environment facilities in `NetworkHandler`, `ClientNetworkHandler`, and
   the picker payload classes.

Architectury 13 supports NeoForge, so none of these facilities blocks this port. Architectury 13 has
no Forge 1.21.1 runtime artifact, however. The NeoForge port therefore must not add new common
Architectury dependencies or spread existing runtime calls into additional subsystems. Stage 0 must
record an ownership matrix for every use: keep temporarily for NeoForge, isolate behind an existing
seam, or reserve for replacement during the later Forge port.

The Forge port will decide whether build-time `@ExpectPlatform` injection can remain. NeoForge must
not prematurely replace a working injection seam, but it must leave the common API loader-neutral.

## Implementation surfaces

### Bootstrap and registration

NeoForge needs an `@Mod` entry point that attaches Architectury registration to the NeoForge mod bus,
initializes common content exactly once, registers loader events, and keeps client initialization off
dedicated servers. Registration order is behavior only where registries or listeners require it.

The existing Fabric entry points demonstrate the required common initialization. The old Forge entry
point is a behavioral reference only; its 1.20.1 APIs and Architectury Forge glue are not valid
NeoForge implementations.

### Configuration and reloads

Server and client settings must retain their documented locations, defaults, validation, and
load-at-start behavior. NeoForge-native configuration is preferred when it can preserve those
semantics. Eye definitions remain server datapack resources selected by the server and synchronized
to clients. Reload registration must preserve atomic replacement and bounded validation.

### Server events and persistent entity state

Loader adapters are required for entity load, tracking, player join/disconnect, datapack sync,
server stop and tick, deaths, entity use, damage, healing, completed trades, and command
registration. Each adapter should call the existing common service that owns the behavior.

Entity eye state remains in the existing persistent compound boundary. A NeoForge-native persistent
entity facility may implement that boundary if it preserves the same keys and save/load behavior.
Item data remains in the registered `somegoogly:eye_properties` component.

### Networking

The common typed payload implementation and protocol version 9 passed Fabric verification.
NeoForge's Architectury runtime should carry the same ids, codecs, directionality, bounds, and
server-derived player identity. NeoForge must implement tracking recipient lookup and confirm that
dedicated servers register clientbound codecs without loading client receivers.

A loader transport difference does not by itself authorize a packet-body or protocol change.

### Client integration and access

The shared renderer is already ported to Minecraft 1.21.1. NeoForge must install its layers, expose
renderer maps and layers through the existing platform seam, register the 3D item renderer, Slimy
Eye tint, picker keys/HUD/input, client commands, disconnect/reset handling, and renderer reload
handling.

The common Access Widener identifies the exact named 1.21.1 members used by shared and Fabric code.
NeoForge's empty Access Transformer must be rebuilt only for members the NeoForge artifact actually
requires. It is a translation target, not permission to copy stale 1.20.1 declarations.

GeckoLib remains optional. Typed NeoForge integration must load only when GeckoLib is present, and a
failure on one unsupported renderer must not prevent the base mod from loading.

### Resources and metadata

Common resources are already in the 1.21.1 singular registry directories with pack format 48. The
NeoForge port should reuse them, validate `neoforge.mods.toml`, declare Architectury as required and
GeckoLib as optional/suggested as appropriate, and avoid duplicating common data.

### GameTests

The port should expose all 77 shared assertion methods through NeoForge discovery wrappers and add
one NeoForge persistence round-trip assertion, matching the Fabric coverage boundary of 78 wrapper
tests unless NeoForge discovery requires a documented equivalent organization. Zero discovered
tests is a failure.

## Risk ranking

| Risk | Area | Reason |
| --- | --- | --- |
| Highest | Client layer installation and Access Transformer | Headless compilation cannot prove access or visual attachment |
| High | Loader networking and login lifecycle | Protocol authority, directionality, and dedicated-server safety must survive |
| High | Persistent state and server lifecycle events | A missed event can silently change saved or initialized entity state |
| Medium-high | GameTest discovery and runtime | No NeoForge test source set currently exists |
| Medium | Configuration and datapack reload | File ownership and reload timing differ by loader |
| Medium | Optional GeckoLib integration | Optional classloading and renderer APIs are loader-sensitive |
| Low | Common gameplay and resources | Already passing on Minecraft 1.21.1 under Fabric |

## Non-goals

- Porting Forge in the NeoForge stages.
- Removing all Architectury runtime use before NeoForge can run.
- Changing the frozen build versions or module layout.
- Changing player-visible behavior, probabilities, persistence, packet bodies, or protocol ids
  without demonstrated necessity and user approval.
- Adding eye geometry or claiming untested optional-mod compatibility.
- Weakening, deleting, disabling, or hiding tests.
- Publishing, committing, or performing Git or GitHub operations.

## Questions deferred to evidence

- The exact NeoForge event and configuration APIs required by 21.1.248.
- Whether native entity persistent data directly satisfies `EntityPersistentData`.
- The exact tracking-player and completed-trade event boundaries.
- Which Fabric Mixins can become NeoForge events and whether any narrow Mixin remains necessary.
- The exact Access Transformer syntax for every member used by the shared renderer.
- The NeoForge item-renderer and renderer-reload hooks for the 3D eye and layer reinjection.
- The NeoForge GameTest discovery/run configuration required by the established Loom version.
- Whether `@ExpectPlatform` remains suitable for the later Forge artifact after runtime Architectury
  facilities are removed.

Resolve these from compiler diagnostics, official published API documentation/source, and focused
tests. Do not inspect decompiled, remapped, cached Minecraft, Forge, or NeoForge sources.
