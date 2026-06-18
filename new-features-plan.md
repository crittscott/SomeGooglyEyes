# New Features — Difficulty Analysis & Plan

Assessment of the ideas in [new-features.md](new-features.md), read against the architecture
(datapack-driven config → resolvers → `GooglyTracker` physics → `LayerGooglyEyes`).

## Progress (branch `item-eyes`)

**Keystone A — DONE.** Mutable, mid-life, server-synced per-mob eye state shipped.
- `state/EyeState.java` — NBT-backed override store (has-eyes + iris/cornea tint + glow
  tri-state) with side-safe reads and a server mutation API that broadcasts on change.
- `network/EyeStatePacket.java` (replaced `GooglyEyePacket`) — carries has-eyes + overrides;
  `NetworkHandler` protocol bumped to `"2"`.
- `render/LayerGooglyEyes.java` reads the overrides and layers them over config.
- `command/GooglyDebugCommand.java` — server `/sgdebug eyes|tint|glow` (verified: tint works).

**Keystone B — DONE.** Client-side transient expressions on `GooglyTracker` (no NBT, no sync).
- Stare (random idle bouts), hurt-grow, blink + wink, anger tint, villager swirl — all reuse
  the existing prev/current + `partialTicks` interpolation; `ModelGooglyEye` unchanged.
- Per-behavior `ClientConfig` toggles (`blink/stare/hurtGrow/anger/swirl`, default on).
- Triggerable for testing via the client command `/sg debug blink|stare|swirl|anger` (targets
  the mob under the crosshair via `Minecraft.crosshairPickEntity`). Verified: blink + stare work.
- Full design notes in [keystone-b-plan.md](keystone-b-plan.md).

**Eyes as Items (foundation + thin slice) — DONE.** The §2 items subsystem is now seeded.
- `state/EyeProperties.java` — the canonical, **codec-backed** appearance model (cornea/iris tint +
  glow, all optional) shared by item NBT, the mob override, and the sync packet (one schema, can't
  drift). Core decision: **property vs. placement** — the item carries appearance only; geometry
  always comes from the mob's datapack config (so reattach needs no coordinates).
- `state/EyeState.java` **refactored** onto `EyeProperties` (same wire tag; `/sgdebug` still works).
- `state/EyeHolder.java` + `state/EntityEyeHolder.java` — attachment seam; harvest/reattach are
  written against the holder, not `EyeState`, so future holders (item frame, item model) slot in.
- `state/EyeBehaviors.java` — javadoc-only design note for the future behavior registry (Keystone B
  stays hardcoded; flagged as first to port).
- `item/GooglyEyeItem.java` + `item/ModItems.java` — one `googly_eye` item storing `EyeProperties`
  in stack NBT; in the Tools & Utilities creative tab; tooltip shows colors/glow.
- `event/EyeItemInteractions.java` — **harvest** (shears on an eyed mob → captures appearance into an
  eye item, drops it, eyes off) and **reattach** (eye item on an eyeless configured mob → applies
  appearance, eyes on). Verified in-game: shears remove, right-click adds.
- `client/GooglyEyeItemRenderer.java` — a **BEWLR** rendering the real `ModelGooglyEye`, tinted by the
  item's properties, with proper **googly wobble when held** (reuses the shared `GooglyTracker.EyeInfo`
  physics, so it settles at rest exactly like mob eyes). Sizing: inventory is code-driven
  (`GUI_SCALE`, since the GUI path ignores a BEWLR's json `gui` transform); held/ground/fixed size via
  the json display block. Known limit: Visual Workbench (`FIXED` context, pre-shrinks ~0.175) renders
  the eye small — accepted rather than special-casing.

**Crafting modifiers (dye / glow / strip) — DONE.** Appearance edits as crafting, over `EyeProperties`.
- `recipe/EyeModifierRecipe.java` — a special `CustomRecipe` (1 `googly_eye` + 1 recognized modifier
  ingredient) that copies the eye stack and folds the modifier's delta onto its `EyeProperties` (any
  unrelated NBT survives). `recipe/ModRecipes.java` registers the serializer; recipe json at
  `data/somegoogly/recipes/eye_modifier.json`.
- `recipe/EyeModifier.java` — the rule list (a new edit = a new entry, not a new recipe class):
  **dye → iris colour**, **glowstone dust → glow on**, **redstone → glow off**, **cobweb → strip all
  overrides** back to a bare config-appearance eye. This is the custom NBT-copying serializer the plan
  flagged as "next" — now built; it subsumes the separate dye-recipe and redstone-glow asks.

**Add eyes via potion (single target) — DONE (verified in-game).** `Splash Potion of Googly Eyes`.
- `potion/ModPotions.java` — an effect-less `Potion` (so vanilla's AoE application is an inert no-op;
  we still get its throw/break/particles/cleanup for free). Brewed from an **awkward splash potion +
  a `googly_eye`**; brewing copies the eye's `EyeProperties` onto the potion. No drinkable form.
- `event/EyePotionInteractions.java` — on `ProjectileImpactEvent` for our splash, picks **one random
  eligible mob** in the splash box (config-eligible + not already eyed) and applies the potion's carried
  appearance, then eyes-on, via the `EntityEyeHolder` seam (same order as right-click reattach).
- `config/ServerEyeConfigs.isEligible(LivingEntity)` — extracted shared eligibility gate, now used by
  both the at-spawn roll and the potion (excludes players / unconfigured mobs).

Everything below this line is the original analysis, updated with status.

## The two keystones (both built)

Most features collapse into two pieces of plumbing — now both in place, so the remaining ideas
are mostly thin consumers.

- **Keystone A — mutable, mid-life, synced per-mob state.** Was: config one-directional,
  has-eyes decided once at spawn. Now: `EyeState` mutates appearance/has-eyes mid-life and
  re-syncs to trackers. Section 2 (dye/glow/shears/potion) now just calls this API.
- **Keystone B — per-eye animation/expression state.** Was: `GooglyTracker` only did wobble
  physics. Now: it also carries blink/stare/hurt/anger/swirl state, consumed by the renderer.

## Difficulty read

| Idea | Status / Difficulty | Notes |
|---|---|---|
| Staring (§3) | **DONE** | Random idle bouts; pupils ease to centre. |
| Blink / wink (§3) | **DONE** | Per-eye Y-squash; ~15% single-eye wink. |
| Hurt-grow (§3) | **DONE** | Driven by `hurtTime`; all mobs. |
| Anger tint (§3) | **DONE (best-effort)** | Tints cornea red; only mobs whose aggression is client-visible (enderman/wolf/aggressive `Mob`). Untested in-game. |
| Villager swirl (§3) | **DONE** | Triggered by synced `VillagerData` level increase. Verified in-game. |
| **Eye item + harvest/reattach** (§2) | **DONE** | `googly_eye` item carries `EyeProperties`; shears harvest, right-click reattaches; rendered as the real 3D eye (BEWLR) with held wobble. |
| Add eyes via potion (§2) | **DONE** | `Splash Potion of Googly Eyes`: one random eligible mob in the splash radius gets eyes, inheriting the brewed eye's appearance. `potion/ModPotions` + `event/EyePotionInteractions`. Verified in-game. |
| Dye / eye color (§2) | **DONE** | Crafting: `googly_eye` + any vanilla dye → iris recoloured (`recipe/EyeModifier.DyeEyeModifier`). |
| Redstone glow toggle (§2) | **DONE** | Crafting: glowstone dust → glow on, redstone → glow off (`recipe/EyeModifier`). |
| Strip / reset eye (§2) | **DONE** | Crafting: cobweb → clear all overrides back to config appearance (`recipe/EyeModifier.StripEyeModifier`). |
| **Babies vs Adults** (§0) | **DONE** | Age-appropriate geometry swaps as a mob grows; has-eyes is fixed once at spawn (not re-rolled per age). Accepted as the intended behavior. |
| Spider 8-eye layout (§2) | **Deferred (user)** | Pure datapack — 8 eyes on the head part. User owns this. |
| Crafting recipes for eyes (§2) | **DONE** | `recipe/EyeModifierRecipe` (`CustomRecipe`) + `EyeModifier` rules edit the `EyeProperties` NBT in place; preserves unrelated NBT. |
| Eye-bearing heads + Heads mod (§2) | **Hard** | Separate render path (head block/item), custom NBT, soft-dep on another mod. |
| Armor slot eyes (§4) | **Hard / orthogonal** | Custom slot or repurposed equipment + GUI; cuts against the data-driven design. |
| Eyebrows (§4) | **Hard (art + state)** | New model geometry beyond cornea/iris **and** an expression state machine. Art-heavy. |

## What's next

1. ~~Keystone A~~ ✅, ~~Keystone B~~ ✅, ~~eye-item foundation + harvest/reattach~~ ✅,
   ~~crafting modifiers (dye/glow/strip)~~ ✅, ~~potion-add (single target)~~ ✅ — done.
2. **In-game pass on the one remaining untested effect** — anger tint (`/sg debug anger true` or
   anger a wolf/enderman). Cornea-red on aggression; only mobs whose aggression is client-visible.
3. **Remaining §2 verbs as thin features** over `EyeState`/`EyeProperties`: eye-bearing heads,
   item-frame holder. (Shears harvest is settled — normal `Items.SHEARS`, no dedicated tool.)
4. **User-owned, no code blocking:** spider 8-eye datapack.
5. **Defer:** armor slot, eyebrows — each its own project. Behavior registry seam is recorded in
   `state/EyeBehaviors.java` for when the event list grows.
