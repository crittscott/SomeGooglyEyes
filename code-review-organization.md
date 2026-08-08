# Code Review: Organization

Scope: how functionality is grouped into classes/packages across
`src/main/java/com/github/crittscott/somegoogly`. This review does not assess correctness,
performance, or style — only whether related code lives together and whether the package
structure is internally consistent. No code was changed.

## Summary

The codebase is generally well-organized: registration code follows Forge's `DeferredRegister`
convention cleanly (`item/ModItems.java`, `enchant/ModEnchantments.java`, `recipe/ModRecipes.java`),
networking is centralized in one place (`network/NetworkHandler.java`), and several subsystems show
real discipline about having a single source of truth (e.g. `HeadInfo.RuntimeConfig#isUsable` is the
one eligibility predicate, reused by `ServerEyeConfigs#isEligible` and `EyeInspectIndicator`, and a
dedicated GameTest (`gametest/EligibilityGameTests.java`) asserts those call sites can't drift apart).

The main organizational weakness is in the **client rendering code**: the "draw a googly eye on a
model" concern is split across four packages (`client`, `client.render`, `client.picker`,
`client.compat`) with no principled boundary between the first two. There's also one clear, minor
constant duplication, and one packaging inconsistency in how "this class is server-only" is signaled
across the codebase.

## Findings

### 1. The eye-rendering pipeline is split between `client` and `client.render` with no real boundary

`client/render/` contains `GooglyEyeRenderer.java`, `LayerGooglyEyes.java`, and
`EyeRenderGating.java` — the "how/whether to draw a googly eye" logic. But two classes that are just
as central to that same concern sit one level up in the parent `client/` package instead:

- `client/ModelGooglyEye.java` — the actual 3D model/mesh the renderer draws.
- `client/GooglyTracker.java` — the per-entity simulated eye state (pupil physics, blink/grow/color
  overlays) that `GooglyEyeRenderer.renderEye` reads every frame.

`client/render/GooglyEyeRenderer.java` imports both of these directly (lines 3-4), and its own class
doc even cross-references `ModelGooglyEye#moveIris` by fully-qualified name. `client/render/LayerGooglyEyes.java`
does the same (imports `client.GooglyTracker` and `client.ModelGooglyEye`, lines 4-5). There's no
apparent reason `ModelGooglyEye` and `GooglyTracker` are one directory level "out" from the renderer
code that is their only real reason to exist — they aren't used outside the render pipeline except
via `SomeGoogly.clientEventHandler.getGooglyTracker(...)`, which is itself render-layer plumbing
(`ClientEventHandler.addLayers`/`addEyeLayers`, in `event/ClientEventHandler.java`, wires the eye
layers into every entity renderer).

The remaining two files in the top-level `client/` package are a different concern each:
`EyeInspectIndicator.java` (the sneak-and-look action-bar hint) and `SlimyEyeColors.java` (an
`ItemColor` handler for tinting the slimy-eye item icon), plus `GooglyEyeItemRenderer.java` (renders
the eye item's 3D icon in hand/inventory — arguably also part of the same "how do we draw an eye"
family as `ModelGooglyEye`/`GooglyEyeRenderer`).

Suggestion: either move `ModelGooglyEye.java` and `GooglyTracker.java` into `client/render/`
alongside `GooglyEyeRenderer`/`LayerGooglyEyes` (and consider `GooglyEyeItemRenderer.java` too), or
flatten `client/render/` back into `client/` if the split isn't meant to carry meaning. As it stands,
a new contributor has no way to predict which package a new eye-drawing class belongs in.

### 2. The eye-drawing "engine" itself is spread across four packages

Beyond finding 1, the actual pixel-drawing code for a single googly eye is triangulated across three
independent call sites, each in a different package, each explicitly documented as needing to stay in
sync with the others:

- `client/render/GooglyEyeRenderer.java` — normal (behavior-animated) draw, used by both
  `client/render/LayerGooglyEyes.java` (vanilla models) and `client/compat/GooglyGeoLayer.java`
  (GeckoLib models). Its own class doc says as much (lines 18-22).
- `client/picker/PickerLayer.java` — the picker's static (no-physics) preview draw
  (`renderPreviewEye`, lines 86-108), whose doc comment says it's "shared with the GeckoLib preview
  path (`GooglyGeoLayer`) so the two can't drift" — and indeed `client/compat/GooglyGeoLayer.java`
  calls `PickerLayer.renderPreviewEye` directly (line 172, 179).
- `client/render/EyeRenderGating.java` — the "should this mob show eyes at all" gate, again shared
  by both `LayerGooglyEyes` and `GooglyGeoLayer`, with its doc explicitly noting the picker-preview
  branch is deliberately *not* included here because the two layers diverge on it.

So the four pieces of "draw a googly eye" — geometry (`client.ModelGooglyEye`), normal per-frame draw
(`client.render.GooglyEyeRenderer`), static preview draw (`client.picker.PickerLayer`), and the
GeckoLib adapter that orchestrates both draw paths (`client.compat.GooglyGeoLayer`) — live in four
different packages. Given how carefully the code already documents these cross-package dependencies
("shared with the GeckoLib preview path", "see GooglyGeoLayer", "see LayerGooglyEyes"), this looks
like an organic split (vanilla-first, then picker added, then GeckoLib compat added) rather than a
deliberate design. I'd suggest a single `client/render/` package (or a `client/render/eyes/` +
`client/render/gecko/` split) rather than scattering the shared pieces by which *feature*
(picker vs. compat) happened to need them, since all four already know about and depend on each
other. That said, keeping GeckoLib-specific code physically separate under `client/compat` is
defensible on its own (see finding 4) — it's specifically the picker/render split that lacks a clear
rationale.

### 3. Server-only code is signaled two different, inconsistent ways

The codebase has two competing conventions for marking a class as server-side-only (there's no
client-side ambiguity — `client/` and `client.picker/` etc. consistently mean client-only):

- **Dedicated package**: `picker/` (top-level, no `client.` prefix) holds
  `PickerExportService.java`, `PickerFreezeService.java`, and `PickerPermissions.java` — all
  server-only, and named to parallel `client/picker/`'s `PickerExporter`, `PickerState`, etc.
- **Filename prefix within a shared package**: `event/ServerEventHandler.java` and
  `event/ClientEventHandler.java` sit in the same `event/` package; `config/ServerConfig.java` /
  `config/ServerEyeConfigs.java` and `config/ClientConfig.java` / `config/ClientEyeConfigs.java`
  likewise share `config/`; `command/GooglyAdminCommand.java` (server-registered) and
  `command/GooglyClientCommands.java` (client-registered) share `command/`.

Both conventions are reasonable in isolation, but the codebase uses whichever one a given feature
happened to start with rather than one consistently. A reader who has learned "server-only code goes
in its own package, mirroring `client.*`" (from `picker/` vs `client.picker/`) will not find
`ServerConfig`/`ServerEventHandler`/`GooglyAdminCommand` where that pattern predicts. This is a minor
finding — not misleading once you know the codebase, but worth picking one convention if the package
layout gets revisited.

### 4. Duplicated reach constant: `GooglyAdminCommand.REACH` and `SpawnAllCommand.SPAWN_REACH`

`command/GooglyAdminCommand.java` line 57:
```java
private static final double REACH = 20.0;
```
`command/SpawnAllCommand.java` line 68:
```java
private static final double SPAWN_REACH = 20.0;
```

Both are the same value (20 blocks) for a conceptually related purpose (how far the `/sg admin` and
`/sg spawn` target raytraces reach), and `SpawnAllCommand`'s field javadoc even says so explicitly:
"Reach of the `/sg spawn` placement raytrace (**matches the admin command's target reach**)." The
relationship is documented but not enforced — nothing stops the two from drifting apart if one is
tuned later. Since `command/` already has no shared constants file, either constant could reference
the other (`SpawnAllCommand.SPAWN_REACH = GooglyAdminCommand.REACH`), or, if a third such constant
ever appears, a small shared holder in `command/` would be worth it. Not urgent given there are only
two call sites, but flagged since the code's own comment acknowledges the coupling.

(For contrast, `client/EyeInspectIndicator.java`'s `REACH = 16.0D` is a third, deliberately
*different* reach value with its own documented rationale — that one is not a duplication issue.)

### 5. `ClientEventHandler` mixes two unrelated responsibilities

`event/ClientEventHandler.java` combines:
- Render-layer registration (`addLayers`/`addEyeLayers`, lines 44-108) — wiring `LayerGooglyEyes` and
  `PickerLayer` into every entity renderer on client setup / resource reload.
- `GooglyTracker` lifecycle management (the `trackers` map, `getGooglyTracker`, `peekGooglyTracker`,
  `clearTrackers`, and the tick-based eviction/update loop in `onWorldTick`, lines 42, 116-167).

These aren't obviously the same concern — one is a one-time (well, per-resource-reload) setup
routine, the other is an ongoing per-tick registry with its own eviction policy. They're bundled here
mostly because both need a place to live that survives across ticks and is reachable from
`client/render/LayerGooglyEyes.java` via the static `SomeGoogly.clientEventHandler` field. This isn't
clearly wrong — Forge event-handler classes often accumulate a bit of state — but if `GooglyTracker`
and `ModelGooglyEye` do get moved into `client/render/` (finding 1), the tracker registry
(`trackers`, `getGooglyTracker`, `peekGooglyTracker`, `clearTrackers`, the eviction tick) would fit
more naturally as a small class in `client/render/` too (e.g. `GooglyTrackers`), leaving
`ClientEventHandler` focused on the layer-registration/event-glue role its name suggests. Flagging
this as a secondary observation, not a strong recommendation on its own.

### 6. `SpawnAllCommand` is not actually a command

`command/SpawnAllCommand.java` lives in the `command/` package and is named like a Brigadier command,
but it registers nothing — the actual `/sg spawn` / `/sg spawnall` command tree is registered in
`command/GooglyClientCommands.java`, which sends `PickerSpawnPacket`/`PickerSpawnAllPacket` to the
server; the server-side handlers of those packets (`network/PickerSpawnPacket.java`,
`network/PickerSpawnAllPacket.java`) call `SpawnAllCommand.spawn`/`spawnOne` directly. So
`SpawnAllCommand` is really "the server-side execution logic for the spawn verbs invoked via network
packet," parallel in role to `picker/PickerExportService.java` (which sits in `picker/`, not
`command/`, despite also being the server-side half of a client command). This is a naming/placement
inconsistency rather than a functional problem — the class doc is honest about the split ("This is
the server-side half: the commands themselves are client commands...") — but a reader scanning
`command/` for "things registered as commands" will be misled by this file, and a reader scanning
`picker/` for "picker server logic" won't find the spawn logic there even though `PickerSpawnPacket`
is a picker packet. Consider whether `SpawnAllCommand` belongs in `picker/` alongside
`PickerExportService`/`PickerFreezeService` (all three are "server-side executors for picker network
requests"), which would make `command/` mean "classes that register a Brigadier command" consistently
(it would then hold `GooglyAdminCommand`, `GooglyClientCommands`, and the small
`MaybeFloatArgumentType` helper).

## Areas checked that were *not* problems

For completeness, these were specifically examined given the review's focus and found to be
well-organized, not scattered:

- **Registration classes** (`ModItems`, `ModCreativeTabs`, `ModEnchantments`, `ModRecipes`) — one per
  owning package, each a thin `DeferredRegister`, wired centrally from `SomeGoogly.java`. This is the
  standard Forge pattern.
- **Networking** (`network/NetworkHandler.java`) — single registration point, packet classes
  colocated in one package regardless of direction, direction declared per packet.
- **`eye.behavior`** — deterministic, non-sided behavior/physics code (`EyeBehavior`,
  `EyeBehaviors`, `Curves`, the individual `*Behavior` classes) lives in one package and is reused
  identically by server scheduling (`ServerBehaviorScheduler`) and client simulation
  (`GooglyTracker`), consistent with the project's pattern of keeping shared logic unsided.
- **Eligibility/usability predicate** — `HeadInfo.RuntimeConfig#isUsable` is the single source of
  truth, delegated to by `ServerEyeConfigs#isEligible` and read directly by
  `EyeInspectIndicator`, with a dedicated GameTest (`EligibilityGameTests`) guarding against drift.
- **`GeckoCompat` / `GeckoIntegration` split** (`client/compat/`) — this looks like a naming
  collision at first glance but is a deliberate, well-documented soft-dependency firewall:
  `GeckoCompat` contains zero GeckoLib references so it always class-loads safely, and
  `GeckoIntegration` (which does touch GeckoLib types) is only reached behind a `LOADED` check. Not
  an organizational problem.
- **`util/LookTarget.java`** — a single-file package, but genuinely a single, narrow, shared utility
  (entity-in-crosshair raytrace) used by both a server command (`GooglyAdminCommand`) and client code
  (`EyeInspectIndicator`); not a dumping ground, and not artificially isolated either.
- **`item/EyeItemProperties.java`** — correctly depends on `eye.state.AppearanceOverride` (not the
  reverse), keeping the layering direction consistent between the "item" and "eye" packages.

## Notes on scope

This review only covers `src/main/java`. It does not touch build files, resources/datapacks, or the
`gametest` package's internal organization (which is a parallel concern — test layout — not covered
here per the review's focus on production code organization). I did not flag any finding I was
unsure about without saying so; everything above is backed by the specific file/line references
included.
