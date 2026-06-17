# New Features — Difficulty Analysis & Plan

Assessment of the ideas in [new-features.md](new-features.md), read against the current
architecture (datapack-driven config → resolvers → `GooglyTracker` physics → `LayerGooglyEyes`).

## The two keystones

Most of these features collapse into two pieces of new plumbing. Build those, and a dozen
ideas get cheap. Skip them, and you'll reinvent them feature by feature.

### Keystone A — Mutable, mid-life, synced per-mob state

Today the mod is one-directional: the datapack config decides eye geometry, the "does this
mob have eyes" coin is flipped **once at spawn**, stored in NBT, and synced **once** on
start-tracking (`ServerEventHandler.java:54-61` — the comment says "the decision never
changes after spawn, no mid-life sync is needed").

Almost all of Section 2 (harvesting, potions, dye color, redstone glow) **breaks that
assumption**: it mutates a mob's eye state while it's alive and needs to push that to clients.
That requires a new packet + a per-entity override store that the renderer consults on top of
the config. Highest-leverage thing to build.

### Keystone B — Per-eye animation/expression state

Today `GooglyTracker` only does the wobble physics. Section 3 (anger, swirl, stare,
blink/wink) plus eyebrows all want extra per-eye state (a blink timer, a stare flag, a color
tint, a hurt-scale). The tracker is the natural home; it already runs per-eye each tick.

## Difficulty read

| Idea | Difficulty | Why |
|---|---|---|
| **Babies vs Adults** (§0) | **Mostly done** | Schema already has `age: adult/baby/any`, `RuntimeConfigSet` resolves by `isBaby()`, geometry swaps live as the mob grows. What's left is only the *deferred* question: should the spawn-chance/enable differ by age? Small. |
| Staring (§3) | **Easy** | Tracker already produces `deltaX/deltaY`; staring = clamp both to 0 and stop updating. Design Q is the *trigger*, not the code. |
| Villager hurt-grow (§3) | **Easy** | `entity.hurtTime` is readable client-side; multiply `eyeScale` by a decaying factor. Nice quick win. |
| Spider 8-eye layout (§2) | **Easy (data)** | Pure datapack — 8 eyes with positions/azimuth on the head part. Resolvers attach by named part + offset, so "side/back" eyes are just offsets. No code. |
| Redstone glow toggle (§2) | **Medium** | `glows` field + `RENDER_TYPE_EYES` path already exist — but per-mob toggle needs **Keystone A**. |
| Dye / eye color (§2) | **Medium** | `irisColors` exists in config, but recoloring *one mob* = per-entity override + sync = **Keystone A**. |
| Add/remove eyes via shears/potion (§2) | **Medium** | Logic is just flipping the has-eyes flag, but mid-life add/remove needs **Keystone A**'s sync path. Plus the items themselves (see below). |
| Blink / wink (§3) | **Medium** | Model is procedural slabs, so blink = squash Y-scale to ~0 over a few ticks via **Keystone B**. Self-contained, fun. |
| Anger color, villager swirl (§3) | **Medium–Hard** | No uniform "angry" signal — must read per-mob-type state (`NeutralMob`, `EnderMan.isCreepy`, villager level-up event). Per-type wiring, not one hook. |
| **Items as a category** (§2) | **Hard (new surface)** | The mod currently registers **zero items**. Eye items, the eye potion, recipes, textures, data gen — a whole new subsystem before any single item-feature works. |
| Eye-bearing heads + Heads mod (§2) | **Hard** | Separate render path (head block/item), custom NBT, soft-dep on another mod. |
| Light emission (§2) | **Hard** | Per-entity dynamic light is notoriously painful in MC; vanilla doesn't really do it. Likely needs a Dynamic-Lights-style hack. |
| Armor slot eyes (§4) | **Hard / orthogonal** | Custom slot or repurposed equipment + GUI; cuts against the data-driven design. |
| Eyebrows (§4) | **Hard (art + state)** | New model geometry beyond cornea/iris **and** an expression state machine (ties into §3). Art-heavy. |

## Suggested sequence

1. **Keystone A** (mutable synced per-mob override store) — unlocks dye, redstone glow,
   shears/potion in one stroke.
2. **Keystone B** (per-eye animation state on the tracker) — then staring, blink/wink,
   hurt-grow, anger fall out cheaply.
3. The pure-data wins (spider layout, baby/adult tuning) can happen anytime, no code.
4. Defer items-as-objects, Heads integration, light emission, armor slot, eyebrows — each is
   its own project.
