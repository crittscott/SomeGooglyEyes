# Googley Eyes — Revival Implementation Plan

Companion to [HANDOFF.md](HANDOFF.md). The handoff captures *why* the revival is feasible
(string-named parts survive obfuscation; part-choice is an irreducibly visual, hand-authored
decision). This document is the *how* against the current codebase.

**Goal:** make eye attachment work in obfuscated production for a reasonable number of
third-party mods using vanilla `ModelPart`, GeckoLib, and Citadel-style model frameworks, by
using stable runtime attachment tokens instead of obfuscation-fragile field names.

**Current state (what's in this repo):** the original Tier-1 prototype has been replaced. Runtime
attachment now goes through resolver strategies (`HierarchicalResolver`, `ReflectionResolver`,
`CitadelResolver`, and the GeckoLib layer), geometry config is datapack-driven and synced from
server to client, and the in-world picker can author configs through the same attachment paths used
at render time.

---

## The refinement that shapes everything

"Address parts by string name" is clean — but only for models that **retain a named tree**, i.e.
`HierarchicalModel` subclasses (`root()` + children maps survive; `getAnyDescendantWithName` is
public). Many common mobs — every `HumanoidModel`-derived mob (zombie, skeleton, husk, piglin,
players, most humanoid mod mobs) — extend `AgeableListModel`, which **does not retain the root**.
It keeps individual part fields and exposes them only as *positional, unnamed* iterables via
`headParts()` / `bodyParts()` (`ListModel` uses `parts()`). A `ModelPart` doesn't know its own
name; names live only in a parent's children map that these models discard.

So there are **two obfuscation-safe addressing modes**, and the config + resolver must support
both:

| Model family | How to address a part | Stability |
|---|---|---|
| `HierarchicalModel` | string-name **path** via children maps | dev→prod stable (the handoff's case) |
| `AgeableListModel` / `ListModel` | **(group, index)** into `headParts()`/`bodyParts()`/`parts()` | dev→prod stable (source order doesn't remap) |

Indexing is less human-friendly than `"head"`, but it's just as production-stable and is authored
once, visually. Both modes avoid the obfuscated field that doomed the original.

---

## Phase 0 — De-risk first (gate the whole project)

The highest-risk assumption is "this works in obfuscated production." Dev `runClient` uses
deobfuscated mappings, so it **cannot** validate this.

- Build a throwaway spike: resolve the zombie head via `AgeableListModel.headParts()[0]` and an
  `allay`/`warden` head via `HierarchicalModel.getAnyDescendantWithName("head")`, draw a
  placeholder cube, do the `translateAndRotate` walk.
- **Build a real jar and drop it into an actual Forge client** (not dev). Confirm both render.

If this passes, the rest is engineering. If it fails, we learn now instead of after a refactor.

---

## Phase 1 — Reachability layer

New `ModelRootResolver` that, given any `EntityModel`, yields searchable roots:

- `instanceof HierarchicalModel` → `root()`.
- `instanceof AgeableListModel` → `@Invoker` mixin to call `headParts()` + `bodyParts()`.
- `instanceof ListModel` → `@Invoker` for `parts()`.

These are **read-only accessor/invoker mixins against vanilla Mojang classes** — behavior-neutral,
Forge-remapped against official mappings, non-conflicting. The difference from today is we mixin
vanilla base classes (stable) instead of reflecting mod fields (unstable).

---

## Phase 2 — Positioning (replace `HeadInfo.correctPosition`)

- **Hierarchical:** walk root → … → target accumulating `part.translateAndRotate(poseStack)`.
  `getAnyDescendantWithName` finds the part but not the ancestor chain, so either store the full
  path in config or do a one-time parent-tracking search to recover and cache the path.
- **List-based:** the addressed part is top-level, so a single `translateAndRotate` suffices (this
  is what the current code already does — it keeps working for these).

Resolve the handle/path once per `(entityType, modelClass)` and cache it; per-frame cost stays at
the `translateAndRotate` replay.

---

## Phase 3 — Resolver abstraction

```java
interface EyeAttachmentResolver {
    boolean handles(EntityRenderer<?> renderer);
    AttachmentHandle resolve(LivingEntityRenderer<?,?> r, AttachSpec spec); // cached
    void attach(PoseStack stack, AttachmentHandle h);                       // per-frame
}
```

Implementations: `HierarchicalResolver`, `ListModelResolver`, later `GeckoLibResolver`.
[LayerGooglyEyes](src/main/java/com/github/crittscott/somegoogly/render/LayerGooglyEyes.java)
becomes thin: pick resolver via `handles()`, move the PoseStack into attach space, then draw eyes.
The iris physics
([GooglyTracker](src/main/java/com/github/crittscott/somegoogly/tracker/GooglyTracker.java)) and
geometry ([ModelGooglyEye](src/main/java/com/github/crittscott/somegoogly/model/ModelGooglyEye.java))
are kept as-is — they're orthogonal and already work.

---

## Phase 4 — Config schema v2 (replaces `attachPoint`)

Each attachment entry carries an addressing token plus the existing eye params:

```jsonc
{ "address": { "mode": "named",   "path": ["head"] },            // hierarchical
  "address": { "mode": "indexed", "group": "head", "index": 0 }, // list-based
  "offset": [x,y,z], "scale": …, "rotation": …, "cornea": …, "iris": …,
  "glows": …, "affectedByInvisibility": … }
```

- **Honor the per-head list** — fix the bug where everything collapses to `heads.get(0)` in
  [HeadInfo](src/main/java/com/github/crittscott/somegoogly/head/HeadInfo.java)
  (Gleep-Glorp / wither multi-site).
- Version the schema; keep the bundled `minecraft.json`; add a config/datapack override directory
  so users add mobs without rebuilding.

---

## Phase 5 — Fold in the known fixes

While `HeadInfo`/`LayerGooglyEyes` are open:

- Multi-head fix (above).
- Texture/UV mismatch: `modelgooglyeye.png` is 16×16 but the layer declares 64×32
  ([ModelGooglyEye.createBodyLayer](src/main/java/com/github/crittscott/somegoogly/model/ModelGooglyEye.java)) —
  verify and correct.
- Cosmetic `MOD_NAME` ("Some Googlys") vs gradle `mod_name` ("Some Googly Eyes") disagreement.

---

## Phase 6 — In-world part picker (what makes "many mods" cheap)

Creative tool: freeze a target mob (AI off), cycle parts via the **same resolver enumeration**,
stick a prototype eye on the highlighted part, nudge offset/scale with keys, export the config line
(with the correct `named`/`indexed` token). Because it rides the exact runtime path, "looks right
in the picker" ⇒ "works in prod." This turns adding a mob from a code/build/test cycle into a
~2-minute visual chore — the actual answer to "support a reasonable number of mods." Could live
here or in the Assets mod.

---

## Phase 7 — GeckoLib adapter (soft dep)

Separate optional source set/module; `handles()` checks for a GeckoLib renderer; address by
`GeoBone` name; position with GeckoLib's pose utility. Shares the config schema and eye code; only
the resolver differs.

---

## Milestones & test gates

- **M0** — Phase 0 spike passes in a built jar. *Gate — do not proceed without it.*
- **M1** — Phases 1–5: ships, works in production for hand-authored vanilla-framework mobs (both
  model families), multi-head correct. Acceptance test = **built jar in a real client**, matrix:
  humanoid (`zombie`), hierarchical (`allay`/`warden`), multi-head (`wither`), one third-party
  vanilla-framework mob.
- **M2** — Picker. Authoring a new mob becomes visual.
- **M3** — GeckoLib adapter + one GeckoLib mod in the test matrix.

---

## Risk register

- **Production obfuscation** — retired by M0; don't proceed without it.
- **Non-hierarchical name loss** — handled by indexed addressing (Phase 1/4), not a blocker.
- **Mod-version drift** — config goes stale, fails harmlessly (no eyes); normal compat chore.
- **Out of scope** — fully hand-drawn renderers (no addressable parts); judged effectively
  nonexistent, so this bucket is empty in practice.

---

## Net

The path is real. The one architectural mistake — field-name reflection — is the thing to rip out
in M1 and replace with the two-mode part resolver. The in-world picker is what makes the
"many mods" goal economical rather than heroic.

---

## Status

- **M0 — PASSED.** String-name resolution + full ancestor walk validated in a built client (see
  [PHASE0.md](PHASE0.md)).
- **M1 — implemented, pending build verification.** Field reflection removed; resolver architecture
  in place.

### M1 — what landed

- `render/resolver/` — `EyeAttachmentResolver` + `HierarchicalResolver` (public `ModelPart#visit`
  walk, the validated mechanism) + `ListModelResolver` + `Resolvers` registry.
- **List family via reflection, no mixin** (see "Mixin attempt" below): `ListModelResolver` reaches
  parts by enumerating `ModelPart` fields by *type + declaration order* (superclass-first → head is
  element 0), guarded so it can never crash.
- `HeadInfo` — now pure per-head data (drops all field reflection, `headModelCache`, `multiModel`);
  honours each head independently.
- `LayerGooglyEyes` — picks a resolver from the model, loops **per head**, attaches by the
  configured string token, renders that head's eyes. Fixes the old `heads.get(0)` collapse.
- `GooglyTracker` — eye arrays sized per-head; dead `multiModel` branch removed.
- `SomeGoogly.MOD_NAME` corrected to match the gradle `mod_name`.
- Config JSON is **unchanged**: the existing `attachPoint` string is reused as the resolver token,
  normalised (lowercase, strip non-alphanumerics) so camelCase tokens match snake_case part keys.
  (List-family addressing is currently positional — token influences hierarchical only.)

### Mixin attempt (reverted)

First pass used an `@Invoker` mixin for `AgeableListModel.headParts()` + MixinGradle. The refmap
never made it into the built jar (`Reference map 'somegoogly.refmap.json' ... could not be read`), so
at runtime the invoker matched no obfuscated method and **mixin-apply failed fatally** the moment any
mod loaded `AgeableListModel` — crashing the whole pack before our code ran. Lesson: an unmatched
`@Invoker` is fatal at class-transform time, not catchable at call time. Reflection-by-type was chosen
instead: no refmap, no build plumbing, and genuinely fail-safe.

### Build-verification checklist (I could not build)

1. **List family (`zombie`, `cow`)** now relies on reflection + `setAccessible` on vanilla model
   classes. If Java-module access blocks it in prod, the access is guarded → those mobs get no eyes
   (no crash). Confirm whether they get eyes; "hierarchical works, list doesn't" now points at
   reflection access, not a refmap.
2. **Wither (multi-head)** exercises the per-head loop. Its config tokens (`leftHead`/`rightHead`/
   `centerHead`) rely on normalization matching the real part keys; if a head doesn't appear, the
   token needs correcting to the actual string name (a discovery/M2 task).
3. **Texture/UV** (16×16 png vs 64×32 layer decl in `ModelGooglyEye`) was left as-is — not touched in
   M1; revisit only if eyes render with wrong texturing.

### M2 prerequisite — datapack config system (implemented)

Geometry configs moved from one bundled `minecraft.json` to per-entity datapack files, server-loaded
and synced to clients (server-authoritative, matching the existing "server sets eyes, client only
vetoes" model — datapack over resource pack was the deliberate choice).

- **Layout:** `data/<entity-namespace>/eyes/<entity-path>.json`, one entity per file (path = entity
  id). Per-file schema: `{ "enabled": true, "heads": [...] }`. Bundled vanilla defaults: 79 files
  under `data/minecraft/eyes/` (split out of the old `minecraft.json`, which is removed).
- **Load:** `EyeConfigReloadListener` (`SimpleJsonResourceReloadListener`) registered on
  `AddReloadListenerEvent` → fills `ServerEyeConfigs`. Datapack stacking gives per-entity override
  for free.
- **Sync:** `OnDatapackSyncEvent` (login + `/reload`) sends `EyeConfigSyncPacket` over the existing
  channel → `ClientEyeConfigs`. `HeadInfo` reads the client store; sync/disconnect invalidate the
  helper cache and trackers. Server and client stores are separate (no single-player bleed).
- **`enabled: false`:** server-authoritative hard-off in `ServerEventHandler` (beats the percent
  roll); client respects it via `hasConfig()`. Only configured+enabled entities are eligible now.
- **Client veto** (`ClientConfig.DISABLED_ENTITIES`, global off) unchanged. Malformed
  `disabledEntities` strings are dropped entry-by-entry and logged once, so a bad client config does
  not break layer registration or rendering.

Verification (built jar): vanilla still works; drop a datapack in `world/datapacks/` overriding one
entity → `/reload` re-syncs live to all clients; `enabled:false` removes a mob for everyone.

### M2 — in-world part picker (implemented)

Self-contained, single-player, hierarchical-only authoring tool in package `picker/`. Commit-as-you-go
model: fiddle a draft eye on the selected part → commit (appends to the head for that part; commit on
a different part = another head) → export to the world datapack → `/reload`.

- `PickerState` — target, enumerated parts, committed heads (keyed by part token), draft eye, field/step.
- `EyeAttachmentResolver.enumerateParts` + `HierarchicalResolver` impl (via `visit`).
- `PickerLayer` — renders committed + draft eyes and the gizmo on the locked target only;
  `LayerGooglyEyes` is suppressed for that entity.
- `Gizmo` + `GizmoRenderType` — RGB=XYZ axes (3 blocks), +end cube only, origin marker, depth-off
  (custom no-mixin `RenderType` subclass to reach the protected line shards).
- `PickerKeys` / `PickerInput` (in-world keybinds) + `PickerHud` (overlay).
- `PickerExporter` — writes `world/datapacks/somegoogly-picker/data/<ns>/eyes/<entity>.json` and runs
  `/reload`.

Default keys (rebindable): K toggle · V lock · `[` `]` part · `;` `'` field · `-` `=` adjust · `\` step ·
Enter commit · M mirror-pair · Backspace undo · G glow · I invis · P export.

Build-verification risk (no build on my end): keymap/`InputEvent` wiring, `RegisterGuiOverlaysEvent`
API, the custom no-depth `RenderType`, SP datapack-path API, programmatic `/reload`. No mixins, so
worst case is a dead key/overlay, not a crash.

### Next

Polish the picker from real use (e.g. load an existing config as the draft starting point, color
editing, longer lock reach), then the model-type expansion below.

---

## Other model types (Phase A & B)

Coverage today: hierarchical mobs fully (render + picker); list-family/other vanilla `EntityModel`
models through the reflection resolver; GeckoLib works through its own layer; Citadel-style models
work through reflected `AdvancedModelBox` names.

### Phase A — vanilla list-family + any non-hierarchical EntityModel

Make every vanilla `EntityModel`-based mob fully pickable, not just renderable.

1. Generalize `ListModelResolver` into a **reflection resolver for any `EntityModel`** (collect
   `ModelPart` fields by type, superclass-first). `handles()` becomes a catch-all; `HierarchicalResolver`
   still runs first so named mobs keep the better named path.
2. **Indexed tokens** `#0…#k` (these parts have no names): `enumerateParts` returns them;
   `toAttachmentSpace` parses `#N`. Keep the legacy `"head"→field 0` mapping so the 79 bundled configs
   still resolve.
3. Picker lights up automatically (it's resolver-agnostic); HUD shows `#3` instead of a name — you
   identify the part visually via the gizmo.

Effort: small; reflection is already proven in production. **Status: done** (`ReflectionResolver`).

### Phase B — GeckoLib (soft dependency)

GeckoLib is a separate framework (no vanilla `EntityModel`; `GeoEntityRenderer` + `GeoRenderLayer`).

1. Soft dep: `compileOnly` API, gated by `ModList.isLoaded("geckolib")`, all GeckoLib refs isolated in
   classes loaded only when present.
2. Parallel render layer: `GooglyGeoLayer extends GeoRenderLayer`, injected in the `AddLayers` event
   (`GeoEntityRenderer.addRenderLayer`). Reuses `ModelGooglyEye`, config, tracker.
3. Re-key the resolver seam off the **renderer** (vanilla pulls model from `LivingEntityRenderer`;
   GeckoLib pulls `GeoModel` from `GeoEntityRenderer`). Picker + both layers share `enumerateParts` /
   `toAttachmentSpace`; `PickerState` is already token-based.
4. Bones are **named** (from `.geo.json`) → real names in the picker. Enumerate by walking the baked
   `GeoModel` bone tree; position via GeckoLib's `RenderUtils`/bone matrix (exact 4.7.4 API is the main
   unknown).

No server changes (GeckoLib mobs are `LivingEntity`; decide/sync/`enabled`/`NoAi`-freeze all already
apply). Client render + picker only. Effort: medium–large, research-heavy on the bone-transform API.

**Status: works.** Soft dep wired in `build.gradle` (`compileOnly` GeckoLib 4.7.4);
`compat/GeckoCompat` (gate, no GeckoLib refs) → `compat/GeckoIntegration` + `compat/GeoBones` +
`compat/GooglyGeoLayer` (all GeckoLib-referencing, loaded only when present). Layer injection,
bone enumeration, and bone-space positioning have been exercised successfully. Citadel-framework
mobs are handled separately by `CitadelResolver`.

---

## Sidedness conclusion

This mod should remain **BOTH**, not `clientSideOnly`, if the server-authoritative behavior stays:

- **Server required:** datapack eye configs are loaded server-side, selected by loaded mod version
  and age, used to roll/store `somegoogly:hasGooglyEyes`, recomputed when age changes, and synced to
  tracking clients. Multiplayer needs this on the logical server, including a dedicated server.
- **Client required:** all rendering layers, `ModelGooglyEye`, picker HUD/input/export, GeckoLib
  layer, Citadel/vanilla model resolvers, and client veto config live on the physical client.
- **Common/shared:** network registration and payload DTOs need to exist on both sides. Packet
  encode/decode is common; client application should stay behind client-side guards.

So the conceptual split is: server decides and syncs; client renders and authors. The current open
question is not "client-only vs both" but whether the implementation is insulated enough for a
dedicated server, since some common-loaded classes still refer to client-only Minecraft classes.
That server-load question is intentionally left for an experimental dedicated-server test.

---

## ⚠ Deferred caveats — MUST address before "done" (currently IGNORED on purpose)

1. **Mod-version drift / multiple mod versions.** Part tokens — names (hierarchical/GeckoLib) and
   indices (`#N`, reflection) — are stable *within* a mod version but shift when a mod updates (renamed
   parts, added fields move indices). Configs then mis-place or drop eyes silently. Need a real
   strategy: e.g. a mod-version key per config, validation/warning when a mob's live part set doesn't
   match the config, and/or version-scoped config sets. **This is the headline robustness problem for
   "works across many mods."**
2. **Baby vs adult authoring.** The schema and runtime now support separate adult/baby entries, but
   configs still have to be authored for babies where vanilla/model scaling makes adult offsets look
   wrong. The picker exports the target's current age, so the remaining work is coverage/polish, not
   a schema blocker.
