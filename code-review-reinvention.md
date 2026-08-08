# Code Review: Reinvention — SomeGooglyEyes

Scope: places where the mod reimplements functionality Minecraft/Forge already provides, bypasses the
standard event/capability system, or hand-rolls serialization where a Codec/Capability facility would be
the 1.20.1 Forge convention. Read-only review; no code changes made.

Overall the codebase is disciplined about using Forge facilities correctly — `SimpleJsonResourceReloadListener`
for datapack loading, `Codec`/`RecordCodecBuilder` throughout, `SimpleChannel` for networking,
`ProjectileUtil.getEntityHitResult` for entity ray-picking, real Forge events (`PlayerInteractEvent`,
`LivingDropsEvent`, `EntityJoinLevelEvent`, `RegisterColorHandlersEvent`, `EntityRenderersEvent.AddLayers`)
for the hooks that matter. The findings below are the exceptions, ranked by confidence.

---

## 1. Hand-rolled weighted-random selection duplicates vanilla `WeightedRandom`

**File:** `src/main/java/com/github/crittscott/somegoogly/eye/HeadInfo.java`, lines 323–344
(`chooseVariantIndex`), with the `Variant.weight`/`weight()` fields at lines 219–237.

```java
public static int chooseVariantIndex(RuntimeConfig config, float roll) {
    ...
    double total = 0;
    for (Variant v : variants) {
        total += v.weight();
    }
    if (total <= 0) return 0;
    double target = roll * total;
    double acc = 0;
    for (int i = 0; i < variants.size(); i++) {
        acc += variants.get(i).weight();
        if (target < acc) return i;
    }
    return variants.size() - 1;
}
```

This is a cumulative-weight threshold walk — exactly the algorithm vanilla's
`net.minecraft.util.random.WeightedRandom` (and the `SimpleWeightedRandomList`/`WeightedRandomList` built
on top of it) already implements: `WeightedRandom.getTotalWeight(List<? extends WeightedEntry>)` plus
`WeightedRandom.getWeightedItem(List<T>, int)`, which walks the list decrementing a pre-rolled value by
each entry's weight. This facility has existed since well before 1.20.1 (used for loot pools, cat
variants, villager trades, etc.), so I'm reasonably confident it's present and behaves this way, though I
have not decompiled it to confirm the exact method signatures.

**Why this one is only medium-confidence as a "should fix":** the mod's requirement is subtly different
from what `WeightedRandom` is shaped for. `chooseVariantIndex` must be *deterministic from a stored
`float` roll in [0,1)* — the roll is persisted in entity NBT and synced over the network so client and
server (and every tracking client) resolve the same variant without the server sending the resolved
index. Vanilla's API expects an `int` weight sum and an `int` pre-rolled value in `[0, totalWeight)`,
whereas this mod's `Variant.weight` is an arbitrary `double` ("relative probability", default `1.0`,
author-editable in datapacks). Adapting to vanilla's shape would mean either scaks->int conversion
(precision loss) or switching the config schema to integer weights. Both are plausible, but neither is
free, so this is a legitimate case where a deliberate decision *not* to reuse the vanilla facility could
have been made — I couldn't confirm from the code whether that tradeoff was considered.

**Downside if left as-is:** none functionally (the custom loop is short and correct), but it's ~20 lines
of vanilla logic re-implemented by hand, and a future contributor unfamiliar with `WeightedRandom` has to
re-verify this loop's correctness (edge cases like `total <= 0`, off-by-one on the last entry) rather than
trusting a shared, already-tested vanilla utility.

---

## 2. Per-entity spawn-chance overrides reinvent (part of) the Tag system

**File:** `src/main/java/com/github/crittscott/somegoogly/config/ServerConfig.java`, lines 126–213
(`Override` record, `globToRegex`, `parse`, `percentFor`).

The `entityOverrides` config list supports lines like `minecraft:zombie,100`, `minecraft:*,5`,
`*:*_horse,50`, matched by a hand-written glob-to-regex translator (`globToRegex`) and a linear scan
(`percentFor`) that prefers an exact id match, then the first matching wildcard, then the global default.

Minecraft/Forge's existing facility for "match a group of entity types" is the registry **Tag** system
(`TagKey<EntityType<?>>`, checked via `entityType.is(tagKey)` or `BuiltInRegistries.ENTITY_TYPE.getTag(...)`),
which already ships with useful vanilla groupings (e.g. `#minecraft:skeletons`) and lets datapacks define
custom ones. This is a **lower-confidence, design-tradeoff observation** rather than a firm "this is
wrong": the mod's wildcard matcher lives entirely in a single `ForgeConfigSpec` string list (no extra
datapack files, admin-editable in one place, works before any datapack loads), whereas tags require
separate JSON files per tag and only group by pre-declared sets, not arbitrary suffix globs like
`*_horse`. A tag-based approach couldn't fully replace the wildcard suffix-matching this config supports.
Still, worth flagging: an admin who already knows Minecraft tags has no way to say "everything in
`#c:zombies`" here — only exact ids or the mod's own glob syntax — so a hybrid (`percentFor` also checking
`entityType.is(tagKey)` for `#`-prefixed override lines) would be more "the Forge way" while keeping the
convenience wildcard for everything else.

**Downside:** minor. This is single-purpose config-parsing code, not a place other mods would hook into,
so there's no "other mods miss an event" cost — just a missed opportunity to reuse a more standard,
already-tested grouping mechanism for the common case.

---

## 3. Minor: `java.util.Random` instead of `RandomSource` in two hot spots

**Files:**
- `src/main/java/com/github/crittscott/somegoogly/eye/behavior/ServerBehaviorScheduler.java`, line 36:
  `private static final Random RANDOM = new Random();`
- `src/main/java/com/github/crittscott/somegoogly/client/GooglyTracker.java`, line 13/22/44:
  `public final Random rand;` seeded from `parent.getUUID().hashCode()`
- `src/main/java/com/github/crittscott/somegoogly/eye/behavior/BehaviorInstance.java`, line 24/38:
  `public final Random rand;` seeded from a network-sent `long seed`

Elsewhere in the codebase (`ServerEventHandler`, `EyeItemInteractions`, `GooglyAdminCommand`) randomness
correctly goes through `entity.getRandom()` / `net.minecraft.util.RandomSource`, which is "the Minecraft
way" of getting a random source (and is what an expert modder would expect for anything touching game
state). These three spots instead use plain JDK `java.util.Random`. This is **not** a case of duplicating
vanilla logic — `java.util.Random` is a JDK class, not a Minecraft/Forge facility — so it's not
"reinvention" in the strict sense, just an idiom inconsistency. I'm flagging it at low confidence/priority
only because the task asked about "the Forge/Minecraft way," and a reviewer familiar with the engine would
likely raise an eyebrow at the mix. No practical downside I can identify (nothing here needs to share a
world seed or be replay-deterministic across restarts), so this is cosmetic.

---

## 4. Considered, not flagged: `LivingEntity.getPersistentData()` for eye state

**File:** `src/main/java/com/github/crittscott/somegoogly/eye/state/EyeState.java` (whole file), and its
use from `ServerEventHandler.java`, `EyeItemInteractions.java`, `PickerFreezeService.java`.

The mod stores its per-mob state (`hasGooglyEyes`, `eyeVariantRoll`, `eyeOverrides`) as raw `CompoundTag`
entries under `entity.getPersistentData()`, and hand-writes a sync packet (`EyeStatePacket`) to push
changes to tracking clients, rather than using Forge's `Capability` system.

I looked at this closely because it's the textbook "raw NBT vs. Capability" pattern the task asked about,
but I don't think it's a real finding here, for two reasons:

- `getPersistentData()` is itself a Forge-provided facility, specifically documented by Forge as the
  lightweight way for a mod to attach simple NBT to an entity **it doesn't own** without registering a
  full `Capability`. Given this mod attaches state to *every* living entity type in the game (vanilla and
  modded), that's exactly the scenario the facility exists for.
- A `Capability` here would still need a hand-written `NetworkHandler` packet for sync — Forge capabilities
  don't auto-sync to clients in 1.20.1 — so the win would only be the (de)serialization boilerplate, which
  is already fully centralized through `AppearanceOverride`'s single shared `Codec` (`toNbt`/`fromNbt`).
  Registering an `AttachCapabilitiesEvent<Entity>` listener for every entity in the game to save what's
  already a clean, cheap, Codec-backed NBT read/write would add per-entity-instance overhead this
  "server-friendly" mod's own stated principles argue against.

No action item; noted for completeness since the task explicitly asked about this tradeoff.

---

## 5. Considered, not flagged: manual `EntityRenderDispatcher` iteration in `addLayers`

**File:** `src/main/java/com/github/crittscott/somegoogly/event/ClientEventHandler.java`, lines 45–108
(`addLayers`, `addEyeLayers`), invoked from `SomeGoogly.java` line 99 via the correct Forge hook
(`EntityRenderersEvent.AddLayers`).

The handler reaches into `EntityRenderDispatcher.renderers` and `.getSkinMap()` directly (widened via the
project's own `accesstransformer.cfg`) rather than using the `AddLayers` event's typed accessors
(`getEntityTypes()`/`getRenderer(EntityType)`/`getSkins()`/`getSkin(String)`, from what I recall of that
event's API — flagging this at low confidence since I have not decompiled it to check the exact method
set). This is a low-confidence, minor stylistic note, not a solid finding: the `accesstransformer.cfg`
comment explains a real, specific reason for at least the `LivingEntityRenderer.layers` widening — Forge's
`addLayer()` only appends, and this mod needs to *insert* the eye layer before `SlimeOuterLayer` so the
slime's translucent shell doesn't depth-discard eye fragments underneath it. That's a genuine gap in the
event's own API, not carelessness. Given the AT file's care in documenting *why* each widened member is
needed, I'd treat this whole area as a deliberate, justified exception rather than something to flag as
unexamined reinvention.

---

## 6. Considered, not flagged: shears-based eye harvest doesn't use `IForgeShearable`

**File:** `src/main/java/com/github/crittscott/somegoogly/event/EyeItemInteractions.java`, `harvest()`
(lines 76–91) and `onLivingDrops` (lines 143–176).

Forge has an `IForgeShearable` capability that vanilla shearable content (sheep, mooshroom, snow golem,
vines, etc.) implements to let shears interact with it via a loot table. This mod's shears-harvest of eyes
doesn't use it. I considered flagging this but concluded it's a poor fit rather than a missed facility:
`IForgeShearable` shearing is unconditional (any shears, no enchantment gate) and loot-table driven,
whereas this mod's non-lethal harvest is deliberately gated behind the custom `Optometrist` enchantment
(plain shears only get the kill-drop path via `LivingDropsEvent`, which is already the correct vanilla
event for that). Forcing this mechanic through `IForgeShearable` would mean either dropping the enchantment
gate or fighting the capability's assumptions. Noting it for completeness at low confidence — a reviewer
who disagrees about the enchantment-gating requirement might reach a different conclusion — but I don't
think this is a real gap in the "expected events" sense: `PlayerInteractEvent.EntityInteract` (which is
used and correctly cancelled/consumed) is the event other mods would already be watching for an
entity-right-click interaction, so nothing here is invisible to other mods that isn't already visible.

---

## Summary

| # | Location | What's reinvented | Confidence | Priority |
|---|----------|-------------------|------------|----------|
| 1 | `HeadInfo.chooseVariantIndex` (HeadInfo.java:323) | Vanilla `WeightedRandom` cumulative-weight selection | Medium-high (facility exists), medium (that it cleanly applies) | Low-medium — works correctly as-is |
| 2 | `ServerConfig` entity override glob matcher (ServerConfig.java:126) | Registry Tag grouping | Low-medium | Low — config-only, no other-mod impact |
| 3 | `java.util.Random` in scheduler/tracker | `RandomSource` idiom | Low | Cosmetic |
| 4 | `EyeState` persistent-data storage | Capability system | N/A — examined, judged correct as-is | None |
| 5 | `ClientEventHandler.addLayers` raw renderer access | `AddLayers` event's typed accessors | Low | None — AT is already justified |
| 6 | Shears eye-harvest | `IForgeShearable` | Low | None — poor mechanical fit |

No findings involve a mutation that bypasses an event other mods would expect to see: every entity/world
mutation site checked (`ServerEventHandler`, `EyeItemInteractions`, `PickerFreezeService`,
`GooglyAdminCommand`) already rides an appropriate Forge event (`EntityJoinLevelEvent`,
`PlayerInteractEvent.EntityInteract`, `LivingDropsEvent`, `PlayerEvent.PlayerLoggedOutEvent`,
`ServerStoppingEvent`) or is inherently mod-internal cosmetic state with no vanilla/Forge event analog.
