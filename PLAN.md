# Googley Eyes — Revival Implementation Plan

Companion to [HANDOFF.md](HANDOFF.md). The handoff captures *why* the revival is feasible
(string-named parts survive obfuscation; part-choice is an irreducibly visual, hand-authored
decision). This document is the *how* against the current codebase.

**Goal:** make eye attachment work in obfuscated production for a reasonable number of
third-party mods using the vanilla `ModelPart` framework (and later GeckoLib), by replacing the
current obfuscation-fragile field-reflection with stable, production-safe part resolution.

**Current state (what's in this repo):** a Tier-1 prototype that resolves head parts via
`Class.getDeclaredField(name)` reflection on the renderer's model
([HeadInfo.findHeadModelPart](src/main/java/com/github/crittscott/somegoogly/head/HeadInfo.java)).
This works in the dev environment (Parchment mappings → fields really are named `head`) but
returns `null` in obfuscated production (the field is `f_xxxxx_`), so the shipped mod would
render nothing. The layer/event/config/physics skeleton is otherwise sound and reusable.

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
- **Client veto** (`ClientConfig.DISABLED_ENTITIES`, global off) unchanged.

Verification (built jar): vanilla still works; drop a datapack in `world/datapacks/` overriding one
entity → `/reload` re-syncs live to all clients; `enabled:false` removes a mob for everyone.

### Next

M2 (in-world part picker) to author the per-mob pivot→face offsets visually — the residual "nose"
placement from M0 is exactly what it solves, and the picker now exports straight into a datapack
(`world/datapacks/…/eyes/`) with `/reload` applying it live. Then M3 (GeckoLib adapter).
