# Keystone B — Client-side transient eye expressions

## Context

Keystone A (done, on `item-eyes`) added *persistent, server-synced* per-mob appearance
(has-eyes / iris+cornea tint / glow) via NBT + `EyeStatePacket`. Keystone B is the opposite kind
of state: **transient, client-only animation derived from the entity's live state each tick** —
the eyes *react* to what the mob is doing. No NBT, no packets.

This is a clean extension of the existing wobble physics: `GooglyTracker` already ticks per-eye
each client tick (`update()`), reading entity state locally (`getYHeadRot`, `getXRot`, motion),
and `LayerGooglyEyes` interpolates the result with `partialTicks`. Keystone B adds expression
state to the tracker and consumes it in the renderer the same way.

**Scope (confirmed with user)** — all four behavior groups, no items, no babies, no spider eyes:

1. **Stare** — random idle bouts: pupils glide to center and stop tracking briefly.
2. **Hurt-grow** — eyes briefly enlarge when the mob takes damage (all mobs).
3. **Blink + wink** — periodic vertical squash of the eye; occasionally just one eye.
4. **Anger tint + villager swirl** — color shift toward red when angry (only for mobs whose
   aggression is client-visible); pupils swirl when a villager levels up.

**Key simplifier:** `ModelGooglyEye` needs **no changes**. Blink = Y-squash via `poseStack.scale`;
hurt-grow = uniform scale; stare = drive `moveIris` toward (0,0); swirl = drive `moveIris` with
circular deltas; anger = color lerp in the renderer. All effects reuse existing primitives.

## Architecture

Mirror the existing `prevDeltaX/deltaX` → interpolate-with-`partialTicks` pattern already in
`GooglyTracker.EyeInfo` / `LayerGooglyEyes`. Every new animated scalar stores a `prev` + current
value advanced in `update()` and is interpolated at render time. Because `update()` only runs
while the mob is on-screen (gated by `requireUpdate()` + the 10-tick expiry in
`ClientEventHandler`), expression timers naturally pause for unseen mobs — which is fine.

## State added to `GooglyTracker`

Tracker-level (per mob), with `prev`/current pairs where rendered:

- **Blink scheduler:** `int blinkCooldown`; on reaching 0, start a blink — both eyes, or a
  single random eye (wink) with ~15% probability — over a short duration (~6 ticks), then reset
  `blinkCooldown` to a random `BLINK_MIN..BLINK_MAX` (≈ 40–200 ticks). Drives each affected eye's
  per-eye blink target along a 0→1→0 curve.
- **Stare scheduler:** `int stareCooldown`, `int stareTicks`, `float prevStareAmount, stareAmount`
  (0..1, eased). When `stareCooldown` hits 0, stare for a random duration (~20–60 ticks);
  `stareAmount` eases toward 1 while staring and back to 0 after, so pupils glide rather than snap.
- **Hurt:** `float prevHurt, hurt` from `parent.hurtTime / 10f` (vanilla `hurtDuration` = 10),
  clamped 0..1.
- **Anger:** `float prevAnger, anger` (0..1) eased toward `isAngryClientSide(parent) ? 1 : 0`
  (~0.1/tick) for a smooth fade.
- **Swirl (villager):** `int lastVillagerLevel`, `int swirlTicks`, `float prevSwirlAngle,
  swirlAngle`. On construction, seed `lastVillagerLevel` from current level. Each tick for a
  `Villager`, read `getVillagerData().getLevel()` (synced data, client-visible); if it increased,
  start a swirl (~30 ticks) and advance `swirlAngle` while active.

Per-eye (`EyeInfo`):

- `float prevBlink, blink` (0 = open … 1 = closed) — per-eye so wink can close just one.

## `update()` additions

Extend `GooglyTracker.update()` (and `EyeInfo.update`) to: copy current→prev for all new scalars,
advance the blink/stare/swirl timers, read `hurtTime` and villager level, and ease the
anger/stare blends. Reuse the existing per-tick structure; no new tick hook needed.

`isAngryClientSide(LivingEntity)` helper (best-effort, documented as client-visible-only):

```java
if (e instanceof Wolf w) return w.isAngry();
if (e instanceof EnderMan em) return em.isCreepy();
if (e instanceof Mob m) return m.isAggressive(); // synced mob flag; set by only some mobs
return false;
```

## Render consumption in `LayerGooglyEyes`

Inside the existing per-eye loop, compute interpolated scalars
(`v = prev + (cur - prev) * partialTicks`) and apply, each gated by its `ClientConfig` toggle:

- **Hurt-grow:** `grow = 1 + hurtLerp * HURT_GROW` (≈ 0.5).
- **Blink:** `blinkY = 1 - blinkLerp * 0.95` (down to ~0.05 = closed).
- Replace the current scale line
  `poseStack.scale(eyeScale, eyeScale, eyeScale * 0.4F)` with
  `poseStack.scale(eyeScale * grow, eyeScale * grow * blinkY, eyeScale * 0.4F * grow)`
  so grow/blink wrap both cornea and iris.
- **Stare / swirl** modify the `moveIris` inputs (currently the interpolated `deltaX/deltaY`):
  - swirl active → `dx = cos(swirlAngleLerp) * R`, `dy = sin(swirlAngleLerp) * R` (R ≈ 0.8);
  - else → take physics `dx,dy`, then lerp toward center by stare: `dx *= (1 - stareLerp)`,
    `dy *= (1 - stareLerp)`.
- **Anger tint:** after the Keystone A override resolution of `irisColours`/`corneaColours`,
  lerp each toward `ANGER_COLOR` (red) by `angerLerp`. (Layer order: config → Keystone A
  override → anger tint.)

## Files

- **`tracker/GooglyTracker.java`** — add the tracker- and eye-level expression state, the
  `update()` advancement, and the `isAngryClientSide` helper. (Constants live here too.)
- **`render/LayerGooglyEyes.java`** — consume the interpolated expression scalars as above;
  add a small `lerpColor(float[], float[], float)` helper.
- **`config/ClientConfig.java`** — add an "Expressions" section with `BooleanValue` toggles
  `BLINK`, `STARE`, `HURT_GROW`, `ANGER`, `SWIRL` (all `define(..., true)`), mirroring the
  existing `DISABLE_GOOGLY_EYES` pattern.
- **`model/ModelGooglyEye.java`** — **unchanged** (noted explicitly; no new geometry).

## Reused existing patterns

- Interpolation: the `prevDeltaX/deltaX` + `partialTicks` idiom already in
  `GooglyTracker.EyeInfo` and `LayerGooglyEyes:130-132`.
- Iris positioning: existing `ModelGooglyEye.moveIris(x, y, pupilSize)` for stare + swirl.
- Per-tick driving: existing `GooglyTracker.update()` called from
  `ClientEventHandler.onWorldTick`; no new event subscription.
- Config: `ForgeConfigSpec.Builder` push/define/pop pattern in `ClientConfig`.

## Edge cases

- **Off-screen mobs:** timers pause (tracker only updates while rendered); resumes cleanly.
- **Tracker reset** on config sync (`clearTrackers`) drops expression state — acceptable
  (animations simply restart).
- **Anger coverage is partial by design:** mobs whose anger isn't client-visible (NeutralMob
  persisted anger, e.g. hoglin/zombified piglin) just won't tint. Documented in the helper.
- **`moveIris` guard:** existing zero/negative `pupilSize` guard still applies; stare/swirl only
  change `x,y`, not scale, so no new NaN risk.
- **Blink vs glow:** the glow pass re-renders cornea/iris; it inherits the same squashed/grown
  pose (it's inside the same `pushPose`), so blink/hurt apply to glowing eyes too.

## Verification (end-to-end)

1. `./gradlew compileJava` — compiles.
2. `runClient`, world with eye-bearing mobs (raise spawn chance or picker bypass), then observe:
   - **Stare:** watch any mob — pupils periodically glide to center and hold briefly, then resume.
   - **Hurt-grow:** hit a mob — eyes pop larger for the hurt flash, then settle.
   - **Blink + wink:** watch over ~10 s — eyes blink; occasionally a single-eye wink.
   - **Anger:** attack a wolf (→ `isAngry`) or stare at an enderman (→ `isCreepy`) — eyes tint red,
     fade back when calm.
   - **Swirl:** trade with a villager until it levels up — pupils swirl for ~1.5 s.
3. **Toggles:** set each `ClientConfig` expression flag to `false` → that behavior stops; others
   continue.

## Out of scope (deferred / user-owned)

Babies vs adults, spider 8-eye layout (user-owned), eyebrows + armor slot + light emission (Hard),
and everything requiring the items system (dye/shears/potion/redstone are already covered by the
Keystone A `EyeState` API and will be wired as their own item features later).
