# Phase 0 — Production-Safety Spike

Throwaway spike for [PLAN.md](PLAN.md) Phase 0 / milestone **M0**. It exists to answer one
question before any refactor: **does resolving a model part by its typed string name survive
obfuscation and work in a real (reobf'd) client?**

If yes, the central insight of [HANDOFF.md](HANDOFF.md) is validated and the M1 rewrite is safe to
start. If no, we learn it now.

## What it does

Adds a throwaway render layer to every living-entity renderer. For models in the
`HierarchicalModel` family it:

1. walks the named part tree with the **public** `ModelPart#visit` — which calls
   `translateAndRotate` at every node and yields each cube's **fully accumulated** pose plus a
   **path string** built from the modder's child-map keys (e.g. `/root/body/head`);
2. captures the pose of the first cube whose path ends in `/head` — resolution by the **string**
   `"head"` (a constant obfuscation never rewrites), **no field reflection, no mixin**. Because the
   pose is accumulated, it includes **every ancestor's** this-frame animation, not just the head's
   own;
3. offsets up+forward onto the face and draws a small **red wireframe box**.

It draws on **all** hierarchical mobs unconditionally (it ignores the `hasGooglyEyes` selection) so
the test target is deterministic. It does not touch the existing googly-eye code path.

> Iteration note: an earlier version drew a large box at the head *pivot* via a single
> `translateAndRotate`. That landed at the neck (child offsets are parent-relative, so skipping
> ancestors mislocates) and a pivot-centered symmetric box couldn't show rotation — the "too low /
> doesn't track the nod" symptoms. This version uses the full `visit` walk + a face offset to test
> the real M1 attachment mechanism, still with no mixin.

## Why hierarchical-only (zombie deliberately excluded)

`warden` and `allay` are `HierarchicalModel`s, reachable with public API alone — the cleanest test
of the string-name claim. Humanoid mobs like `zombie` extend `AgeableListModel`, whose parts are
only reachable through a `protected headParts()`/`bodyParts()`, i.e. a vanilla `@Invoker` mixin
(Phase 1 work). Shipping un-built mixin plumbing here would risk a *false negative* from a mixin
config mistake rather than from the hypothesis, so it's left out to keep the signal clean. The
index-based addressing for that family is conceptually low-risk (source order is obfuscation-stable);
its only open question is the standard vanilla-class mixin, addressed in M1.

## How to test (you build — I did not)

1. Build the mod jar (`gradlew build`) and install it in a **real, non-dev Forge 1.20.1 client**.
   The dev `runClient` uses deobfuscated mappings and therefore **cannot** validate this — it must
   be a reobf'd jar in an actual client.
2. In-game, spawn a **warden** (`/summon minecraft:warden`) and/or an **allay**/**villager**.
3. Look for a small **red wireframe box** on the **face**, and watch whether it **swings as the
   head nods/turns** (warden's idle/roar/sniff animations are a good stress test).

## Reading the result

- **Box on the face, swings with the head** → string-name resolution *and* the full ancestor-walk
  attachment both work in production. **M0 passes** and the M1 mechanism is de-risked; proceed to
  M1 (port this `visit` walk into `HeadInfo`, add the list-family invoker mixin, expose offset/scale
  per mob).
- **No box (but mod loads)** → the public string-name/`visit` path failed in prod. Stop and rethink
  before M1 — this is exactly what M0 is meant to catch.
- **Box tracks the head but sits off the face** → mechanism works (the point of M0); the fixed
  face-offset guess just needs per-mob tuning, which is the M2 picker's job. Still an M0 pass.
- **Box at the right place but does *not* swing on nod** → the head rotation is applied somewhere the
  walk isn't capturing; flag it, that's a real finding to chase before M1.

(Cross-check: `warden`/`allay` already render real eyes in dev via the old field-reflection path, so
the box should land in roughly the same area — only the *resolution + accumulation* differs.)

## Result: M0 PASSED — spike removed

Validated in a built client: the box resolved by string name (`/head`) appeared on warden and
villager and **tracked all head motion**, confirming string-name resolution + the full ancestor walk
work in obfuscated production. Residual "nose" placement was just the fixed face-offset guess (the
hand-authored visual offset), not a mechanism problem.

The spike has been deleted (the `spike/` package and its registration line) — its mechanism is now
carried by the real render path in M1 (`render/resolver/HierarchicalResolver` via `ModelPart#visit`).
This file is kept as the record of how M0 was de-risked.
