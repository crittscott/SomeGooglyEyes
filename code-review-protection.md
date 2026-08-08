# Code Review: Protection and Claims

Scope: how SomeGooglyEyes interacts with land-claim/protection mods (FTB Chunks, Towny, GriefPrevention,
Land Claiming Mod, etc.) and vanilla protections (spawn protection, entity invulnerability, adventure
mode). This covers every place the mod reads a player's right-click, mutates a `LivingEntity`, mutates
block state, or spawns an entity.

## Summary

The mod has two distinct interaction surfaces with very different postures toward protection:

1. **The gameplay eye verbs** (apply eyes, shear-harvest, harvest-on-kill, cosmetic reactions) are
   dispatched entirely through standard, cancelable Forge events and correctly defer to whatever a
   protection mod decides. This part is solid — no changes suggested.
2. **The "picker" authoring toolkit** (`/sg spawn`, `/sg spawnall`, mob freeze/move/rotate) is a
   debug/dev-facing feature that mutates blocks and entities directly, gated only by "sender is in
   creative mode." It does not check land ownership, does not run block placement through
   `BlockEvent`-style protection hooks, and (for mob move/freeze) does not fire any cancelable event at
   all. On a server where creative mode is available to non-operator players (a common setup for trusted
   builders), this is a real bypass of claim protection and of vanilla spawn protection.

## Part 1 — Gameplay eye verbs (respects protection)

### Apply eyes / shear-harvest — `EyeItemInteractions.onEntityInteract`

`src/main/java/com/github/crittscott/somegoogly/event/EyeItemInteractions.java:105-136`

Both the slimy-eye "apply" verb and the enchanted-shears "harvest" verb are dispatched from a single
`@SubscribeEvent` handler on `PlayerInteractEvent.EntityInteract` — the standard vanilla/Forge
right-click-an-entity event that essentially every claim/protection mod already hooks and cancels for
entities inside a claim the acting player doesn't own.

The handler method is annotated with plain `@SubscribeEvent` (no `receiveCanceled = true`), so Forge's
event bus will simply skip this listener if the event was already canceled by a higher-priority listener
— which is exactly what a protection mod does. No explicit `event.isCanceled()` check is needed in the
mod's own code because the event bus enforces it before this listener ever runs. This is "the Forge way"
to respect another mod's veto, and it is done correctly here.

- **Game scenario (protected):** a player right-clicks a cow inside another player's GriefPrevention
  claim with a slimy eye in hand. GriefPrevention's own `PlayerInteractEvent.EntityInteract` listener
  (registered at higher priority) cancels the event; SomeGooglyEyes's listener never runs, so no eyes are
  applied and the shears never harvest. This is the expected/correct outcome.
- The client-side branch (lines 116-121) only plays a swing animation as a UX preview and does not
  authoritatively change anything — the real state mutation happens exclusively in the server branch
  (line 122-125), which is itself gated by the same cancelable event. This is a minor UX quirk (a
  player might swing and see nothing happen if the server denies it) but not a protection bypass.

### Harvest-on-kill — `EyeItemInteractions.onLivingDrops`

`src/main/java/com/github/crittscott/somegoogly/event/EyeItemInteractions.java:143-175`

This only runs after a mob has already been killed by a direct player melee hit. Whatever prevented (or
allowed) that kill — PvE/mob-damage protection in a claim, entity invulnerability, etc. — has already run
by the time `LivingDropsEvent` fires. This handler just adds an extra item to the death loot; it does not
independently decide whether the kill itself was allowed. Not a protection concern.

### Cosmetic reactions — `EyeReactionHandler`

`src/main/java/com/github/crittscott/somegoogly/event/EyeReactionHandler.java`

`onLivingHurt`, `onLivingHeal`, and `onTradeWithVillager` only trigger a cosmetic eye animation (bulge,
swirl) in response to damage/heal/trade events that already happened through their own protectable
pipelines (damage protection, trade permission, etc.). These handlers never themselves gate a
world-changing action, so there's nothing here for a protection mod to intercept — correctly out of
scope.

### At-spawn eye roll — `ServerEventHandler.onEntityJoinLevel`

`src/main/java/com/github/crittscott/somegoogly/event/ServerEventHandler.java:77-102`

Rolls whether a newly spawned mob has eyes. This is not player-triggered and has no relationship to land
ownership — not a protection concern.

### Admin command — `GooglyAdminCommand`

`src/main/java/com/github/crittscott/somegoogly/command/GooglyAdminCommand.java:63-66, 169-187`

`/sg admin ...` (toggle has-eyes, set tint, force glow, trigger a behavior) requires **both** operator
permission level 2 (`.requires(src -> src.hasPermission(2))`) and creative mode. Operators are already
trusted to bypass protection by design on every server; this is purely cosmetic besides. Not a protection
concern.

## Part 2 — Picker authoring toolkit (protection gap)

The picker is an authoring/debug tool (its own class docs call it "an authoring aid" / "a debug command
meant for throwaway test worlds") for spawning test mobs and posing them for the eye-placement editor. Its
only server-side gate, applied uniformly by `PickerPermissions.creative`, is:

`src/main/java/com/github/crittscott/somegoogly/picker/PickerPermissions.java:24-33`

```java
public static boolean creative(@Nullable ServerPlayer sender) {
    if (sender == null) return false;
    if (sender.isCreative()) return true;
    ...
}
```

This is **not** an operator check — it's satisfied by any player whose gamemode happens to be creative.
Many servers hand out creative mode to trusted builders, minigame arenas, or temporarily to a player doing
a task, without granting operator status. On such a server, every verb below is reachable by a non-op
player, and none of them check land ownership, claim state, or run the corresponding vanilla/Forge
protection hook.

### `/sg spawn <type>` — `PickerSpawnPacket` → `SpawnAllCommand.spawnOne`

`src/main/java/com/github/crittscott/somegoogly/network/PickerSpawnPacket.java:38-53`
`src/main/java/com/github/crittscott/somegoogly/command/SpawnAllCommand.java:303-367`

- Entity placement uses `level.addFreshEntity(entity)` (line 362), which does fire the standard,
  cancelable `EntityJoinLevelEvent` — a protection mod that cancels entity spawns inside a claim would
  correctly block this. This part is fine.
- However, when the target mob is aquatic, `spawnOne` first calls `level.setBlockAndUpdate(...)`
  directly (lines 249-251, inside the water-column loop) to place a column of water blocks at the
  targeted location — **before** any spawn-permission check on the entity even matters. `setBlockAndUpdate`
  is a raw world-mutation API; it does not fire `BlockEvent.EntityPlaceEvent` or any other cancelable
  block-placement event, because that event is only fired by the normal "player places a block from an
  item" code path. A protection mod that only hooks block-place/break events (the overwhelming majority
  of them) never sees this write and cannot block it.
- There is no `ALLOW_SPAWN_ALL`-style config gate on this single-spawn path at all — only the creative
  check.
- **Game scenario:** a creative-but-non-op player stands at the edge of another player's claim, aims at a
  target block just inside it, and runs `/sg spawn minecraft:cod`. The mod overwrites whatever block is
  there with water, inside the claim, with no claim-protection mod ever getting a chance to veto the block
  change (only the subsequent entity spawn is checkable, and only if the claim mod happens to also gate
  spawns).

### `/sg spawnall` — `PickerSpawnAllPacket` → `SpawnAllCommand.spawn`

`src/main/java/com/github/crittscott/somegoogly/network/PickerSpawnAllPacket.java:43-63`
`src/main/java/com/github/crittscott/somegoogly/command/SpawnAllCommand.java:150-294, 84-126`

This is the more destructive verb: it lays out a grid of every registered living-entity type around the
player and, in "platform mode" (triggered whenever the player is standing over air), calls
`buildPlatform`/`buildBasin`/`buildRoof` — each a nested loop of `level.setBlockAndUpdate(...)` calls that
lay a 5×5 sandstone tile (plus a roof 8 blocks up, plus water basins for aquatic mobs) under every mob in
the grid, explicitly documented as freely overwriting "whatever blocks it lands on." Like `spawnOne`'s
water column, none of this goes through `BlockEvent.EntityPlaceEvent` or any other event a protection mod
would hook — it's a bulk, direct terrain rewrite.

**Mitigation already present:** this verb is additionally gated by
`ServerConfig.ALLOW_SPAWN_ALL` (`src/main/java/com/github/crittscott/somegoogly/config/ServerConfig.java:116-120`),
which **defaults to `false`** and is explicitly documented as "enable only on throwaway authoring worlds."
So on a default configuration this specific command is unreachable. The gap only manifests if an admin
opts in — but even then, opting in does not add a claim check; it just trusts that the world is a
throwaway one. A server that runs mixed content (a creative "gallery" area used with `allowSpawnAll=true`
plus a separate protected survival area on the same world) would still have no claim boundary respected
by this command.

### `/sg freeze` — `PickerFreezePacket` → `PickerFreezeService.freeze`

`src/main/java/com/github/crittscott/somegoogly/network/PickerFreezePacket.java:55-73`
`src/main/java/com/github/crittscott/somegoogly/picker/PickerFreezeService.java:61-85`

Freezing (forcing `NoAi=true` and zeroing velocity on a `Mob`) is resolved purely from a client-supplied
`mobId` (UUID) against `level.getEntity(mobId)` — **any** loaded mob in the sender's current dimension can
be targeted, with no distance check, no ownership check, and no claim check. The only refusal condition is
"another player already has this exact mob frozen." Creative mode is the sole gate.

- **Game scenario:** a creative, non-op player sniffs or guesses the UUID of another player's tamed wolf
  (UUIDs are visible in NBT via various vanilla/mod tools, or simply by looking at the entity with a
  debug/NBT-viewing item) and freezes it from across the map. The pet stops moving and stays that way
  until that same player unfreezes it, the server restarts, or that player logs out (freeze auto-releases
  on the freezer's logout, not on any timeout). This works regardless of whether the wolf is inside its
  owner's claim.

### `/sg mob move` / `/sg mob rot` — `PickerMobPosePacket`

`src/main/java/com/github/crittscott/somegoogly/network/PickerMobPosePacket.java:77-103`

This is the sharpest gap. The handler resolves the client-supplied `mobId` directly against
`sender.serverLevel().getEntity(packet.mobId)` and, if it's a `LivingEntity`, calls `living.teleportTo(...)`
(reposition) or sets its yaw fields (rotate) — with:

- no check that this `mobId` is the mob the sender actually has frozen/is editing (despite the class doc
  describing it as acting on "the mob being edited," nothing in `handle()` cross-references
  `PickerFreezeService`'s per-player frozen-mob record),
- no distance/reach limit (unlike `spawnOne`, which raytraces the player's own view within 20 blocks,
  this packet just takes raw world-space coordinates/offsets and an arbitrary entity UUID),
- no ownership/claim check, and
- no Forge event fired at all — `Entity#teleportTo` and the yaw setters are plain state mutation with
  nothing for a protection mod to observe or cancel, unlike block breaks/places or entity right-clicks.

- **Game scenario:** the same way a malicious/curious creative player can freeze an arbitrary mob by UUID,
  they can also teleport it — e.g. relocate another player's named/leashed pet out of its pen and off a
  cliff, or walk a villager out of a locked trading room, entirely invisibly to any claim mod, since no
  event fires for the claim mod to see. Because a modified client can send this packet with any UUID at
  any time (the doc for `PickerPermissions` itself notes "the creative checks in the client CLI... are UX
  only and never trusted" — but that scrutiny was only applied to the creative-mode check, not to
  "is this actually my frozen mob"), even a well-behaved stock client's existing picker UI is not the only
  way to reach this: the constraint that it only acts on "the mob you're editing" is a client-side
  convention, not a server-enforced one.

### Vanilla spawn protection

None of the picker's direct block/entity mutations (`setBlockAndUpdate`, `teleportTo`, freeze) route
through `ServerPlayerGameMode`'s normal interaction checks, so vanilla's spawn-protection radius
(`level.getServer().isUnderSpawnProtection(...)`, normally enforced for non-op block breaks/places near
world spawn) is bypassed for the same structural reason as claim mods: the mutation never goes through the
code path spawn protection is wired into. In practice this mostly matters for `spawnOne`'s water-column
placement and `spawnall`'s platform building, since those are the only block-mutating verbs.

### Entity invulnerability / adventure mode

Not separately relevant here: every picker verb requires creative mode server-side, and adventure-mode
players are (by vanilla rule) never in creative mode simultaneously, so the adventure-mode block-break
restriction question doesn't arise for this toolkit. The mod never damages entities anywhere in its own
code (harvest-on-kill only reads an already-completed kill), so entity invulnerability is likewise never a
factor the mod needs to check.

## Recommendations (for discussion, not applied — this was a read-only review)

- `PickerMobPosePacket`'s handler could check that `packet.mobId` matches the mob currently recorded as
  frozen for that sender in `PickerFreezeService` before acting, which would at least tie "move/rotate" to
  "a mob I already successfully froze" (itself still not claim-aware, but closes the "arbitrary UUID from
  anywhere in the world" reach).
- `PickerFreezeService.freeze` and `SpawnAllCommand.spawnOne`'s water-column placement have no
  distance/reach limit from the sender at all; `spawnOne`'s own entity-placement half already raytraces
  the player's view within 20 blocks (`SpawnAllCommand.SPAWN_REACH`) — the same reach limit could bound
  these too.
- Whether any of this actually matters depends entirely on the target server's trust model: a server that
  only grants creative mode to full operators has no gap here at all, since operators already bypass
  protection everywhere by convention. The gap is specific to servers that hand out creative mode to a
  wider trust tier than "can bypass claims."
