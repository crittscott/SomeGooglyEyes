# Some Googly Eyes: Living Specification

> **Status:** implementation-grounded, living document.  
> **Scope:** current Java implementation and the shipped data format, reviewed 2026-06-23.  
> **Validation status:** this document records source-level behavior; it is not evidence that every path has been tested in-game or on a dedicated server.

## 1. Purpose

Some Googly Eyes adds configurable, animated googly eyes to living Minecraft entities. It is designed around a clean separation:

- Datapacks describe *where* eyes belong on a particular entity model and what their default appearance is.
- The server decides *whether* an individual entity has eyes, persists that decision, and synchronizes it.
- The client draws the eyes, simulates their wobble, applies client preferences, and provides an in-world authoring tool for new placements.
- Eye items carry a portable *appearance*, not a portable placement. When that appearance is applied to a mob (only via the splash potion), the mob's datapack placement is used.

The mod id is `somegoogly`.

## 2. Status vocabulary

| Status | Meaning |
| --- | --- |
| **Implemented** | Present in the Java source and part of normal runtime registration. |
| **Partial** | Present, but intentionally limited or missing an important intended extension. |
| **Experimental** | Implemented code exists, but compatibility has not been established by runtime testing. |
| **Deferred** | Named architectural seam or intended feature with no complete gameplay implementation. |

## 3. Side ownership

| Concern | Owner | Status |
| --- | --- | --- |
| Read datapack eye definitions | Server | Implemented |
| Select version and age appropriate definitions | Server | Implemented |
| Decide whether a newly seen entity has eyes | Server | Implemented |
| Persist per-entity state and appearance overrides | Server | Implemented |
| Synchronize definitions, state, and expression triggers | Server to client | Implemented |
| Draw eyes and simulate idle wobble | Client | Implemented |
| Respect local disable preferences | Client | Implemented |
| Run the placement picker and `/sg` authoring commands | Client, single-player export only | Implemented / Partial |

Conceptually this is a **both-sides mod**. A server without the client merely owns data and state; a client without the server can render only after it receives the server-selected configuration and state. Client registrations are guarded by distribution checks, but dedicated-server loadability is still **experimental** and must not be treated as verified.

The package layout mirrors this split: client-only code (rendering, the wobble tracker, the placement picker, GeckoLib compatibility, the shared eye model, client config, and the `/sg` picker commands) lives under `com.github.crittscott.somegoogly.client`, while shared and server-only code stays directly under `com.github.crittscott.somegoogly`. The boundary is structural only — the runtime ownership above is unchanged.

## 4. Runtime model

### 4.1 Entity state

Each living entity may have these values in persistent Forge data:

| Key | Meaning |
| --- | --- |
| `somegoogly:hasGooglyEyes` | Whether the entity currently displays eyes. |
| `somegoogly:eyeVariantRoll` | A stable random roll used to choose one weighted placement variant. |
| `somegoogly:eyeOverrides` | Optional appearance override: cornea color, iris color, and glow. |

`hasGooglyEyes` and the variant roll are assigned the first time the entity joins the server level. The eye flag is a once-at-spawn decision, but it can later be changed by gameplay actions. It is retained through saves, dimension changes, and aging. The variant roll is likewise retained, so an entity keeps the same visual arrangement for life.

An entity is eligible for the initial roll only when the server has a usable enabled definition for it at either its current or alternate age. This prevents a baby that only has an adult definition from being permanently excluded before it grows.

**Players are a deliberate exception.** A player has an eye definition (`data/minecraft/eyes/player.json`, a humanoid head arrangement) and renders eyes when it has them, but the server never includes players in the at-spawn roll: a player always starts with no eyes and can only gain them mid-life from the splash potion (§9.4). Because the flag lives in ordinary persistent data, which Forge clears on respawn, a player's eyes are lost on death.

### 4.2 Lifecycle

1. The server reload listener reads `data/<namespace>/eyes/*.json` from active datapacks.
2. It selects the definitions matching the namespace's loaded mod version and each definition's age selector.
3. At first entity join, the server determines an eye flag using the global/per-entity spawn chance (players are always excluded and start with no eyes) and stores a placement-variant roll.
4. On datapack sync, the selected definitions are sent to every client. On entity tracking, its current eye state is sent to the new observer.
5. State mutations broadcast the full state to every tracking client immediately.
6. Client render layers read the synced definition plus the entity's synced persistent state and draw the result.

## 5. Datapack definition format

### 5.1 Location

An eye-definition file belongs at:

```text
data/<entity namespace>/eyes/<entity path>.json
```

For example, the entity id `minecraft:axolotl` maps to `data/minecraft/eyes/axolotl.json`.

### 5.2 Top-level schema

```json
{
  "entries": [
    {
      "version": "[1.20.1,1.21)",
      "age": "any",
      "enabled": true,
      "variants": [ ... ]
    }
  ]
}
```

Each entry is selected independently by `version` and `age`.

| Field | Meaning |
| --- | --- |
| `version` | Exact version or a bracket range such as `[lower,upper)`. It is matched against the loaded version of the file's namespace. |
| `age` | `adult`, `baby`, or `any`. An age-specific match takes precedence over `any`. |
| `enabled` | Defaults to `true`. `false` is a server-authoritative hard disable for this configuration. |
| `variants` | One or more weighted complete arrangements; the entity rolls one for life. This is the only placement shape — even a single arrangement is one weight-one variant. |

Entry-level `heads` is no longer a valid shape; a bare `heads` list is silently ignored. There is no backward compatibility — all shipped files were migrated to `variants` by `scripts/migrate_eyes_to_variants.py`.

If multiple matching entries target the same entity and age slot, the first is kept and later matching entries are ignored with a warning. Invalid files are logged and skipped without aborting the complete reload.

### 5.3 Variants and heads

```json
{
  "variants": [
    {
      "weight": 1.0,
      "heads": [
        {
          "attachPoint": "head",
          "eyes": [ ... ]
        }
      ]
    }
  ]
}
```

- A variant is a complete visual arrangement, not merely an alternate individual eye.
- Weights are relative; omitted weights default to one and negative weights behave as zero.
- The entity's stored variant roll is mapped deterministically onto the applicable age configuration, so client and server agree without sending a resolved index.
- A head is an attachment group. `attachPoint` is passed to the active model resolver; it defaults to `head`.

### 5.4 Eye definition

Each object in `eyes` is flat rather than nested:

| Field | Default | Meaning |
| --- | --- | --- |
| `position` | `[-0.13, -0.25, -0.25]` | Local offset from the attachment point. |
| `eyeScale` | `0.75` | Base eye size. Non-positive eyes are skipped. |
| `irisScale` | `0.6` | Iris/pupil size. |
| `inclination` | `90.0` | Aim angle from local positive Y, in degrees. |
| `azimuth` | `270.0` | Aim angle from local positive X, in degrees. |
| `corneaColors` | `[1, 1, 1]` | RGB cornea color, stored as three floats. |
| `irisColors` | `[0, 0, 0]` | RGB iris color, stored as three floats. |
| `glows` | `false` | Whether the eye receives the glowing-eye render pass. |
| `affectedByInvisibility` | `true` | Whether entity invisibility hides this eye. |

The sampled shipped definitions for axolotl, bee, and warden each have one enabled `any` entry for `[1.20.1,1.21)` holding a single weight-one variant with two symmetric black-on-white eyes. Axolotl and warden attach to `head`; bee attaches to `bone`. Pig ships two variants, so different pigs roll different arrangements.

## 6. Configuration

### 6.1 Server configuration

| Setting | Default | Behavior |
| --- | ---: | --- |
| `googlyEyesEnabled` | `true` | Global master switch for new spawn decisions. |
| `globalPercent` | `2` | Default percentage chance for an eligible entity to get eyes at first spawn. |
| `entityOverrides` | empty | Lines of `entity-or-glob,percent`. Exact ids beat wildcard entries; otherwise the first matching wildcard wins. |
| `harvestOnKillPercent` | `25` | Chance for an eyed mob killed directly with a shears item (any `ShearsItem`, no enchantment needed) to drop eye items. |
| `ambientBehaviors` | `true` | Enables idle cosmetic expressions. |
| `ambientMinTicks` / `ambientMaxTicks` | configured range | Random delay range between ambient expression attempts. |
| `ambientBehaviorPool` | `blink, side_eye, stare, cross_eye` | Expression ids the idle timer may play. Unknown ids are ignored. Event-driven behaviors are not in this pool. |
| `growOnHitPercent` | `20` | Chance an eyed mob plays `grow` when a player damages it. `0` disables. |
| `swirlOnTrade` | `true` | Completing a trade with an eyed villager or wandering trader plays `swirl`. |
| `swirlOnHeal` | `true` | An eyed mob being healed plays `swirl` (rate-limited per mob). |
| `swirlHealCooldownTicks` | `200` | Minimum ticks between heal-triggered swirls on one mob. |

Spawn percentage controls how often a *new* eligible entity receives eyes. Changing it does not reroll an entity that already has stored state.

### 6.2 Client configuration

| Setting | Behavior |
| --- | --- |
| `disableGooglyEyes` | Local master render veto. |
| `disabledEntities` | Local list of entity ids (e.g. `minecraft:zombie`) whose eyes are not rendered. |
| `disabledMods` | Local list of mod namespaces (e.g. `minecraft`) whose entities' eyes are not rendered. |

Malformed `disabledEntities` values are dropped and logged once rather than crashing setup or rendering. The parsed views of both lists are cached and rebuilt only when the client config (re)loads, rather than on every render call.

## 7. Rendering and attachment

### 7.1 Normal rendering

The client adds a googly-eye layer to every vanilla `LivingEntityRenderer`, including both player skin models (default and slim). The layer returns without drawing when any of the following applies:

- the picker is actively previewing that entity;
- the client global or per-entity veto is active;
- the entity has no synced `hasGooglyEyes` flag, outside picker mode;
- no enabled client-synced geometry exists;
- the model has no usable attachment resolver;
- the eye is hidden by invisibility or has a non-positive scale.

For each surviving eye, rendering combines:

1. its datapack placement;
2. datapack appearance overlaid by the entity's optional appearance override;
3. local, client-only wobble physics;
4. at most one active behavior contribution;
5. normal and, when configured, glowing render passes.

The eye model is shared between mob eyes, picker previews, and eye items. Its cornea and iris are separate, closed 16-sided shallow cylinders; the iris is moved independently in front of the cornea, giving the object its wobble. The iris position maps linearly onto the full circular interior of the cornea, so the pupil can travel anywhere up to the rim.

### 7.4 Iris wobble physics

The wobble is a client-only, per-eye physical simulation, ticked at the client tick rate and interpolated at render time. The same simulation drives mob eyes and a held eye item, so they behave identically.

Each eye is modeled as a point-mass pupil moving in the eye's local plane, constrained to a unit disk that maps onto the cornea's full circular interior:

- **Gravity** pulls the pupil toward the bottom of the eye (local down).
- **Pseudo-forces** push the pupil opposite the eye socket's acceleration: the holder's linear acceleration (projected so sideways movement and jumping/falling register) and the holder's head yaw/pitch angular acceleration. Because the forcing is acceleration-based, steady motion at constant speed produces no movement; starts, stops, turns, and jumps are what throw the pupil around.
- **Collision** with the circular rim reflects the pupil radially with a coefficient of restitution below one, plus tangential friction, so it bounces a number of times and then settles. Rest and slide cutoffs zero out residual motion so it parks cleanly at the bottom instead of jittering forever.
- A small per-eye noise term keeps multiple eyes on one entity from moving in perfect lockstep.

The simulation state is transient and client-only: it is never persisted or synchronized. Each eye is an independent simulation, so the number of eyes per head is unconstrained.

At most one active behavior (see §8) is blended over this baseline: a behavior that drives the pupil pulls it off the physics position toward the behavior's target by a weight, while behaviors that leave the pupil alone let the wobble show through unchanged.

### 7.2 Attachment resolvers

| Resolver | Target models | Token form | Status |
| --- | --- | --- | --- |
| Hierarchical | `HierarchicalModel` | Normalized part names | Implemented |
| Citadel | Citadel/LLibrary-style advanced models | Unique box names or `#index` | Implemented / compatibility-sensitive |
| Reflection fallback | Other vanilla-style entity models | `#index`; legacy non-index token means the first part | Implemented / brittle by design |
| GeckoLib | GeckoLib `GeoEntityRenderer` models | Bone names | Implemented / version-sensitive |

The hierarchical resolver replays animated model transforms, but it discovers parts through cube visitation. A pivot/group part containing no cube cannot currently be selected or used as an attachment point; the result is a silent skipped head. This is a known **Partial** limitation.

The reflection fallback depends on class field order. It exists to broaden model coverage, but may shift when an upstream model changes. Ageable list-model scaling is also outside its part-tree transform and can misplace baby eyes.

### 7.3 GeckoLib compatibility

If GeckoLib is installed, non-vanilla Geo entity renderers receive `GooglyGeoLayer`. It mirrors the normal layer's state checks, override composition, wobble, behavior display, invisibility handling, and picker preview. Bone traversal uses the GeckoLib API directly, so changes to that API can break attachment until this compatibility layer is updated.

## 8. Cosmetic behaviors

The server owns the schedule; clients own the visual playback. A behavior trigger contains an entity id, behavior id, duration, seed, and elapsed time. The seed lets every observer reconstruct random choices consistently, while a newly tracking observer can start partway through an existing effect.

Built-in behaviors and their trigger sources are:

| Id | Visual effect | Trigger |
| --- | --- | --- |
| `stare` | Pupils ease to center, hold, then release to wobble. | Ambient |
| `blink` | A seeded random subset of eyes squashes shut and opens. | Ambient |
| `side_eye` | Pupils center, then slide to one seeded side. | Ambient |
| `cross_eye` | Pupils center and move inward according to their configured horizontal position. | Ambient |
| `grow` | Eyes bulge and settle. | A player damages the mob (`growOnHitPercent`) |
| `swirl` | Pupils spiral toward center. | A trade completes with the villager/wandering trader, or the mob is healed (`swirlOnTrade` / `swirlOnHeal`) |
| `color_change` | Corneas blend toward a seeded hue and back. | None — registered and debug-triggerable only |

Triggers fall into two tracks. **Ambient** behaviors are chosen at random from the configured pool (§6.1) by the idle timer. **Event-driven** behaviors (`grow`, `swirl`) are started from server game events — `LivingHurtEvent`, `LivingHealEvent`, and Forge's `TradeWithVillagerEvent` — and are not in the ambient pool. `color_change` ships in neither track but stays registered so `/sg admin` can play it.

Only one behavior may run on an entity at a time, and the rule is non-interruptable for every trigger source: an event reaction that arrives while a behavior is already playing is dropped, not queued. Behaviors do not persist to NBT and are cosmetic; after a reload they simply start fresh.

Every event trigger is gated to entities that both have eyes and are currently tracked by a player — the same condition ambient scheduling uses, checked by reusing the per-mob schedule state that only exists for tracked, eyed mobs. Reactions therefore never fire for off-screen or eyeless mobs, and the client independently drops a trigger for any mob it is not actively rendering. The heal trigger additionally enforces a per-mob cooldown (`swirlHealCooldownTicks`), armed only when a swirl actually starts, so a regenerating mob does not swirl back-to-back.

**Known behavioral gap — Partial:** the scheduler adds an entity on `StartTracking` only if it already has eyes. If an eyeless entity gains eyes through the potion while it is already being watched, its visual state syncs and renders, but it is registered for neither ambient nor event-driven behaviors until tracking restarts. This conclusion is source-derived and awaits runtime confirmation.

## 9. Gameplay systems

### 9.1 Eye item and appearance

`somegoogly:googly_eye` is a 3D item. Its `EyeProperties` NBT stores a sparse `AppearanceOverride`:

- optional cornea RGB;
- optional iris RGB;
- optional glow state.

An absent property falls back to the recipient mob's per-eye datapack appearance. The item deliberately contains no position, scale, rotation, or attachment data.

When rendered in a hand, the item runs the same wobble step as a mob eye. In inventories, frames, and ground contexts its iris is centered.

### 9.2 Harvest

| Action | Result | Status |
| --- | --- | --- |
| Right-click an eyed mob with Optometrist-enchanted shears (any `ShearsItem`, vanilla or modded) | Removes its eyes without damage, drops eye items carrying the mob's effective appearance. | Implemented |
| Kill an eyed mob with a direct shears attack (any `ShearsItem`, no enchantment) | May drop the same eye items, using the server harvest chance. | Implemented |

Both harvest paths also apply to a player who currently has eyes (a player is a living entity with a definition). Shears-killing an eyed player therefore drops eye items as for any mob.

Right-clicking a mob or player with a googly-eye item does **nothing**: the item is purely a brewing/crafting ingredient, and the splash potion (§9.4) is the only way to give an entity eyes. Right-clicking a googly-eye item into an item frame remains ordinary vanilla behavior and is unaffected.

Harvested stacks use the mob's total configured eye count, but sample the effective appearance from the first configured eye. The current override model is per-mob rather than per-eye, so asymmetric eye colors are not preserved as separate item properties.

### 9.3 Crafting

The special `eye_modifier` recipe accepts exactly one googly-eye item and one modifier:

| Ingredient | Effect |
| --- | --- |
| Any vanilla dye | Set iris color. |
| Glowstone dust | Force glow on. |
| Redstone | Force glow off. |
| Cobweb | Remove all overrides and fall back to a recipient's config appearance. |

The output preserves unrelated NBT from the source eye item.

### 9.4 Potion

An awkward splash potion brewed with a googly-eye item becomes the `somegoogly:googly_eyes` splash potion and inherits the eye item's appearance.

On impact, the server chooses exactly one random nearby eligible, currently eyeless living entity—players included, since they have a definition but never spawn with eyes—and applies the potion's appearance before turning eyes on. This is the only way to give an entity eyes. It does not cancel vanilla impact handling; the custom potion carries no normal mob effects.

### 9.5 Enchantment

`somegoogly:optometrist` is a treasure-only enchantment restricted to shears (any `ShearsItem`, vanilla or modded). Its purpose is non-lethal harvesting.

## 10. Placement picker and authoring workflow

The picker is an in-world, creative-mode authoring system. It is client-state driven, but writing output is intentionally restricted to a single-player integrated server.

### 10.1 Workflow

1. Enable the picker with the configurable toggle key (default `K`).
2. Look at a living entity and lock it (default `V`). The integrated server freezes a mob target by temporarily setting `NoAi`.
3. Select an attachment part with the bracket keys or `/sg part`.
4. Create or edit a draft eye through `/sg` commands, save it, and inspect the HUD preview.
5. Export with `/sg export`.

The exporter writes:

```text
<world>/datapacks/somegoogly-picker/data/<namespace>/eyes/<entity>.json
```

It creates `pack.mcmeta` when needed, writes a pretty-printed file, and requests `/reload`. The exported file contains one exact-version, age-specific entry whose `variants` list holds every authored arrangement with its weight.

### 10.2 `/sg` command surface

The client command tree uses full verb names only (no short aliases) for choosing a target, selecting a part, creating/moving/rotating an eye (with `/sg posrot` setting position and rotation together for the common move-and-aim case), changing scale/color/glow/invisibility, saving/selecting/deleting/listing eyes, managing variants, exporting, and spawning an authoring grid of living entity types in single-player.

Variants are authored explicitly: `/sg variant new` appends and switches to a fresh arrangement, `/sg variant <n>` switches to one, `/sg variant del <n>` removes one (the last variant cannot be deleted), and `/sg variant weight <w>` sets the current variant's relative weight. All other eye-editing verbs act on the variant currently being edited; the HUD shows the active variant and weight.

`~` in movement/rotation leaves that component unchanged.

`/sg` is a single user-facing command name, but it is registered from two sides: these picker verbs are client commands, while the operator-only `admin` subtree (see §11) is a server command grafted under the same root. The two occupy disjoint paths, so command fall-through routes each input to the side that owns it — confirmed in single-player; the dedicated-server client→server hop is unverified (see §13's dedicated-server caveat). There is no client-to-server command packet; the picker verbs mutate client `PickerState`, and the `admin` verbs run server-side with normal command permissions and context.

The picker previews saved and current eyes with a centered iris and a local RGB transform gizmo. While previewing its target, ordinary eye rendering is suppressed to prevent duplicate eyes.

### 10.3 Picker limitations

- **Partial:** it authors fresh from the live model and never reads back a mob's committed config, so editing an existing placement means re-authoring it.
- **Partial:** it cannot author an empty pivot joint if the relevant resolver cannot enumerate it.
- **Partial:** freezing is restored on ordinary unlock/logout and synchronously at integrated-server stop. An autosave followed by a hard crash can still persist temporary `NoAi`.
- **Deferred:** remote/multiplayer export is intentionally unsupported.

## 11. Debug and administrative commands

`/sg admin` is an operator-only (permission level 2) subtree of `/sg`, intended for development and verification. It targets the living entity under the player's crosshair and can toggle eyes, adjust iris/cornea tint, change glow behavior, and trigger a named or random cosmetic behavior.

It is a server command registered under the shared `/sg` root (see §10.2), and is gated by `requires(hasPermission(2))`, so the whole subtree is hidden from non-operators in suggestions. It is registered in normal server event handling despite being intended as development tooling, so it should be treated as an active, non-player-facing interface rather than dead code.

## 12. Network protocol

The mod uses one SimpleChannel with protocol version `3` and three server-to-client packet types:

| Packet | Purpose |
| --- | --- |
| `EyeStatePacket` | Entity id, eye flag, variant roll, and optional override NBT. |
| `EyeConfigSyncPacket` | The server-selected runtime geometry definitions, serialized one entity at a time. |
| `EyeBehaviorTriggerPacket` | Transient behavior id, timing, seed, and elapsed time. |

Malformed individual entries in a config-sync payload are logged and skipped so later entries can still be applied. The client clears its synced definitions and render trackers on disconnect.

## 13. Non-goals and deferred seams

| Item | Status | Notes |
| --- | --- | --- |
| Per-eye mutable appearance overrides | Deferred | Current overrides apply uniformly to a mob. |
| Game-event behavior triggers | Implemented | `grow` on player hit and `swirl` on trade/heal are wired; ambient scheduling and `/sg admin` remain the other trigger sources. |
| Additional `EyeHolder` implementations | Deferred | The interface anticipates item frames, item stacks, or head blocks, but entities are the only concrete holder. |
| Dedicated-server compatibility certification | Experimental | Code has side guards, but no runtime result is recorded here. |
| Robust generic model attachment | Partial | Reflection and external-framework integrations intentionally trade completeness for broad coverage. |

## 14. Maintenance rules for this document

When changing the mod, update this document in the same change when any of the following changes:

- datapack schema, default values, selection precedence, or migration expectations;
- server/client ownership or packet contents;
- persisted NBT keys or eye-item NBT schema;
- user-visible item, recipe, potion, enchantment, command, or picker behavior;
- resolver support or known model-framework limitations;
- an item moves between Implemented, Partial, Experimental, and Deferred.

Do not promote a compatibility claim—especially dedicated-server support or external model-framework behavior—from Experimental without a recorded runtime check.
