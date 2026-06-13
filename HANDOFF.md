# Googley Eyes — Revival Handoff

This document captures a design discussion about reviving **Googley Eyes** (a Forge mod that
adds funny eyes to mobs, a revival of an unmaintained mod of the same name). It was written
from a conversation held in the **Assets** repo — a separate dev/datamining mod that extracts
entity model data. Assets is *not* Googley Eyes; it's a tool that produces info Googley Eyes
needs. Read this, then we can pick up the Googley Eyes work with full context.

> **How to use this doc:** open the Googley Eyes repo and tell Claude "read HANDOFF.md."
> It's a summary of conclusions, not a verbatim chat log.

---

## The goal

Googley Eyes attaches eye geometry to a chosen part of a mob's model (usually the head) and
makes the eyes **track that part as it animates**. The hard requirement: this must work
**in production (obfuscated runtime)** and for **other people's mods**, including mods that:

- use the standard Minecraft `EntityModel` / `ModelPart` framework (most mods), obfuscated or not;
- use **GeckoLib** (a different model/animation framework with its own bone tree);
- (rarely) use a fully custom renderer with no addressable parts.

The author previously got Googley Eyes working for only a handful of cooperative mods and gave
up due to two blockers (see "What actually blocked the original" below).

---

## The single most important insight

**Reference model parts by their string name, NOT by their (obfuscated) field name.**

When a modder builds a mob the standard way, they write:

```java
partdefinition.addOrReplaceChild("head", ...);
```

That `"head"` is a **string** the author typed. It becomes the **key** in the part's child map
(`ModelPart.getChildren()` → `{"head": ..., "body": ...}`). It is *data*, not a Java symbol.

Obfuscation renames **classes, methods, and fields** (the symbol table) — so the field
`ModelPart head` becomes something like `f123343`. But obfuscation **does not touch string
literals**. So `"head"` stays `"head"` in production, byte-for-byte.

**Consequence:** if you stop reaching for the field (`f123343`) and instead read the child-map
**keys**, the obfuscation problem disappears. The author's string names are identical in the dev
environment and in production. This is the thing that would have unblocked the original project.

### Stability — be precise about two kinds
- **Dev → production, same mod version:** rock solid. String names are identical. This is the
  stability we actually need, and we have it.
- **Across different mod versions:** not guaranteed. If a mod author renames a part in an update,
  the config entry goes stale — but it fails *harmlessly* (eyes just don't appear) and is a normal
  "bump the config when the mod updates" chore, same as any compat layer.

---

## What actually blocked the original (and the resolution)

**(a) "Obfuscated mods — I only knew f123343 was the part, and couldn't trust its stability."**
Root cause: holding the wrong handle. `f123343` is the *field* (unstable across obfuscation).
The *string key* (`"head"`, `"crest"`, whatever the author named it) is stable dev→prod. Fix:
walk `getChildren()` and record the string name, never the field. Even a weird/opaque name like
`"bone7"` is fine — it's stable, and our discovery workflow is visual anyway, so semantic
clarity doesn't matter.

**(b) "Some mods don't expose the normal model part hierarchy."**
This splits into two very different cases:
- **A *different* but still named hierarchy (GeckoLib & friends).** These have a rig; it's just
  not Minecraft's `ModelPart` tree. GeckoLib bones come from a `.geo.json` file and **also have
  string names**, reachable through GeckoLib's own API. Supportable via a *second adapter*. The
  same string-name principle applies.
- **No named structure at all** (a mod that hand-draws geometry with no addressable parts).
  Genuinely unsupportable. Rare. Accept it as out of scope.

So "I couldn't do anything with them" was only true for the second case; GeckoLib (likely most of
the frustration) is reachable — it was just missing an adapter.

---

## Part-choice is a human, visual task (don't try to automate it)

No procedure can know that on mob "Gleep Glorp" the eyes look best on the part the author called
`crest` vs `noggin`. Mobs come in all shapes with all kinds of rigs. **Choosing the part is
irreducibly visual.** The tool's job is *not* to choose — it's to let a human choose by looking,
then freeze that choice into data. The config (which the author already has for a few mods) is the
*output* of discovery, not a substitute for it.

---

## The runtime attachment technique (vanilla framework)

This is the part that makes eyes track animation, and it needs almost no "scary" code:

1. **Attach via the official Forge event, not a mixin.** `EntityRenderersEvent.AddLayers` hands
   you each mob's renderer; `LivingEntityRenderer.addLayer(...)` is **public**. So hanging an eye
   render layer onto the pipeline is supported API — no code injection into anyone's methods.
2. **Timing is the trick.** A `RenderLayer` runs *after* `model.setupAnim(...)`, so by the time
   your layer executes, every `ModelPart` already holds **this frame's animated transform**.
3. **Get into head-space by replaying the hierarchy onto the PoseStack:**
   ```
   poseStack.pushPose();
   // walk root → ... → head, calling part.translateAndRotate(poseStack) on each
   // now the stack is in head space, post-animation
   //   -> render the eyes here
   poseStack.popPose();
   ```
   `ModelPart.translateAndRotate(PoseStack)` is public and applies the part's current animated
   transform. `ModelPart` has no parent pointer, so walk **top-down** from the root (using the
   `getChildren()` string keys) accumulating the path to the target.

   Shortcut for `HierarchicalModel`s: `getAnyDescendantWithName("head")` returns the part publicly
   with no mixin — but it doesn't give you the ancestor chain you need for positioning, so the
   top-down walk is the general tool.

---

## Mixins — what's actually needed, and why it's safe

Three things people conflate:
- **The data-driven config: zero mixins.** It's just JSON (entity id → part name → offset/scale).
- **Attaching the eyes: not a mixin.** It's the public `AddLayers` event + `addLayer`.
- **Walking the part tree to find the named part: one tiny mixin.** `ModelPart.children` is a
  *private* field with no public getter. Reading it needs a read-only **accessor** mixin — exactly
  the `ModelPartAccessor` the Assets mod already has.

**Why this mixin is harmless:** mixins range from invasive (injecting into other people's methods —
fragile, conflict-prone) to gentle **accessors/invokers** that change *no behavior* and merely
expose an existing private field/method. We only use the gentle kind. It's behavior-neutral,
version-stable, and two mods both adding an accessor to `ModelPart.children` won't collide. It's
the standard, boring Forge solution — not a hack.

---

## Proposed architecture

A small **resolver/adapter** abstraction so different frameworks converge on one path:

```
interface EyeAttachmentResolver {
    boolean handles(EntityRenderer<?> renderer);
    // per-frame, per-entity: move `stack` into attachment space and draw the eyes
    void attach(PoseStack stack, MultiBufferSource buf, Entity e, /* frame args */, EyeRenderer eyes);
}
```

Implementations:
- **VanillaResolver** — render-layer + top-down `translateAndRotate` walk (covers the majority).
- **GeckoLibResolver** — **soft dependency** (separate optional module, only loads if GeckoLib is
  present). Uses GeckoLib's own render layer + find-bone-by-name + its util to move the pose onto a
  bone. Note: GeckoLib's `GeoEntityRenderer` is *not* a `LivingEntityRenderer`, so branch on
  renderer type at `AddLayers` time — hence the `handles()` method.

Both frameworks give you **string-named parts** and a way to reach a part's **animated pose**,
which is all the eyes need — so the eye-rendering code and the config schema are **shared**; only
the resolver differs.

Config schema (rough): `entityType -> [ { partPath, offset[xyz], scale, eyeStyle } ]`, with sane
defaults (head-name heuristics: `head`, `centerhead`, `headmain`, `lefthead`, `righthead`, …) and
per-entity overrides loaded from config/datapack.

---

## The ideal discovery tool (the real unlock)

The most useful tool isn't an offline JSON dump (it tells you `crest` *exists*, not *where it is*
on the mob). Build an **in-world part picker**:

- Spawn the target mob with **AI off / frozen** (so it doesn't wander).
- Cycle through its parts with a key; each press **highlights one part** and sticks a **prototype
  eye** on it so you see it live on the model.
- Nudge offset/scale with keys until it looks right.
- Press export → writes the config line: entity id, the **string** part name, your offsets.

Why this is the unlock: the picker uses the **same runtime machinery** (string-named parts + the
render-pose attachment) as the shipped mod. So **anything you can pick in the tool is guaranteed to
work in the shipped mod, in prod.** Discovery and runtime become the same code path — "it looked
right while authoring" and "it works for users" stop being two separate gambles.

---

## Effort tiers (honest)

| Tier | Scope | Effort | Confidence |
|------|-------|--------|------------|
| 1 | Vanilla framework, well-behaved mobs (head named `head`, standard renderer) | Days | High — basically solved |
| 2 | Vanilla + arbitrary part names + config/override/heuristics system | ~1–2 weeks | High |
| 3 | GeckoLib adapter (soft dep, separate module, learn its layer/bone API) | ~1 week + iteration | Medium-high |
| 4 | Mods with no named part structure (hand-rolled rendering) | Case-by-case, usually not solvable | Low — accept as unsupported |

Tier 4 is the only genuinely intractable bucket and it's rare. Everything using *either* vanilla
`ModelPart` *or* GeckoLib bones is reachable with one shared technique.

---

## How the Assets mod fits

Assets stays a **discovery/datamining tool**, not Googley Eyes. It dumps entity model data:
- `/assets skeleton` — part-name hierarchy (tells you the string names to target).
- `/assets geometry` — full cube geometry + transforms.
- `/assets head` — heuristic head-part location.
- `/assets blockbench` — `.bbmodel` per entity (visually confirm orientation/where the face points).

It already uses the obfuscation-safe approach (string keys via `getChildren()`, mixin accessors
compiled against Mojmap and auto-remapped). It feeds Googley Eyes by helping author default
configs. The in-world part picker (above) could live here or in Googley Eyes — but note the picker
is more valuable than the offline dump for the *choosing* step, because it's visual.

(Optional, Assets-only nicety discussed and set aside: capturing `getTextureLocation(entity)` to
auto-pair each dumped model with its texture PNG for the Blockbench workflow. Irrelevant to the
eyes themselves, since eyes use their own texture.)

---

## Suggested next steps

1. Stand up **Tier 1**: an `AddLayers` hook + a render layer that does the top-down
   `translateAndRotate` walk to a head part found by string name, drawing a placeholder eye.
2. Add the **config/override system** + head-name heuristics (Tier 2).
3. Build the **in-world part picker** to make discovery visual and prod-faithful.
4. Add the **GeckoLib adapter** as a soft-dep module (Tier 3).
5. Treat no-named-structure mods (Tier 4) as explicitly unsupported.
