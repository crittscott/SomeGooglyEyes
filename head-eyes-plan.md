# Plan: googly eyes on placed mob heads (1.20.1)

Status: all phases written; not yet compiled, run, or checked in game. The open verification items below still stand.

## Goal

A placed mob head can carry googly eyes as server-owned state that every player sees alike. Eyes reach a head two ways: an eyed mob's head drop inherits the mob's eyes (a purple-eyed skeleton leaves a purple-eyed head), and a Slimy Eye applied to a placed head. Optometrist shears take them off again. Eyed heads keep their eyes through breaking and re-placing. Ambient expressions play on head eyes; mob-only states (sleep, anger, aim, rattle, pulse) don't apply. Timing and pupil position need only agree roughly between players.

## Decisions taken (change before starting if wrong)

- **Code vocabulary.** Code says "skull" (vanilla's `SkullBlock`, `SkullBlockEntity`); player text and docs say "head".
- **Definitions.** A head uses its mob's existing eye definition (adult), keeping only the heads whose attach token's last segment is `head` (`head`, `root/head`). Mapping in common: skeleton, wither skeleton, zombie, creeper, piglin, player. The dragon head and modded skull types have no mapping and refuse eyes. No new data files, no picker work.
- **State.** A head stores the same `EyeState.Snapshot` a mob does (has-eyes, variant roll, appearance overrides) under the same key names. Re-placing an eyed head keeps its roll, so it keeps its arrangement.
- **Shears-kill double drop.** An eyed mob killed with shears may drop both a harvested Googly Eye and an eyed head; the two rolls stay independent.
- **Behaviors run client-side from shared inputs.** Each client derives a head's ambient schedule from the head's position and the level's game time, so all players see the same behavior within a tick or so, with no server scheduling and no packets. This is the one deliberate departure from mobs: mob triggers happen only on the server; a head has none.
- **Server ambient settings reach clients.** `ambientBehaviors`, `ambientMinTicks`, `ambientMaxTicks`, and `ambientBehaviorPool` ride the eye-definition sync packet so an admin's settings govern heads too. Network version 15 → 16.
- **Out of scope for this plan:** eyes drawn on worn heads and on head items in inventory, look-at-player, note-block reactions. Each can follow later on the same render hook.

## Phase 1 — State and persistence

1. **Shared snapshot codec.** Give `EyeState.Snapshot` `read(CompoundTag)` / `write(CompoundTag)` and route `EyeState`'s entity path through them, so the key names and the overrides codec have one home.
2. **Platform bridge.** `@ExpectPlatform SkullPersistentData.get(SkullBlockEntity)` returns the mod's compound on a head block entity, plus the NBT path of that compound inside the block entity's saved data (needed for item stamping and the loot copy).
   - Forge: `BlockEntity.getPersistentData()`; path `ForgeData`.
   - Fabric: a `SkullBlockEntity` mixin with a duck interface holding a compound, written in `saveAdditional` and read in `load` under `somegoogly:data`.
3. **`SkullEyeState`** (common, server-authoritative, sibling of `EyeState`): `snapshot(be)`, `hasEyes(be)`, `apply(be, snapshot)`, `clear(be)`, `definitionFor(SkullBlock.Type)`, and `stampItem(ItemStack, Snapshot)` (writes the snapshot into the item's `BlockEntityTag` at the bridge path). Every mutation ends with `setChanged()` and `level.sendBlockUpdated(...)`, which resends vanilla's block-entity data packet. No new state packet.
4. **Verify (needs you, since I can't read Forge source):** that Forge's base `BlockEntity` writes `ForgeData` from `saveAdditional` (so it's in the head's update tag and pick-block copy) and reads it in `load` (so client update packets and item placement restore it).

## Phase 2 — Getting eyes onto heads

1. **Eyed mob's head drop.** When an eyed mob dies (and `googlyEyesEnabled` is on), every head item it drops in the death-loot phase is stamped with the mob's snapshot. One common method, `EyeItemService.stampHeadDrops` (or alongside `onDeath`), decides; loaders only route drops to it.
   - Forge: in `onLivingDrops`, walk `event.getDrops()`.
   - Fabric: the existing `dropCustomDeathLoot` TAIL hook runs before `Skeleton`/`Zombie`/`Creeper` spawn their charged-creeper head and misses loot-table heads (wither skeleton). Add a mixin on `Entity.spawnAtLocation(ItemStack, float)` that stamps head stacks while the entity is a dying `LivingEntity`.
2. **Slimy Eye on a placed head.** `EyeItemService.applySlimyEyeToSkull(stack, player, pos)`: refuse unless the switch is on, the head type has a definition, and the head has no eyes; require `level.mayInteract(player, pos)` and `player.mayUseItemAt(pos, face, stack)`; roll a variant, apply the item's appearance, consume outside creative, play the squelch, emit `GameEvent.BLOCK_CHANGE`.
   - Forge: `PlayerInteractEvent.RightClickBlock` at `LOWEST`.
   - Fabric: `UseBlockCallback` in the existing late phase; refuse spectators as the entity adapter does.
3. **Optometrist on a placed head.** `EyeItemService.harvestSkull(...)`, same routing and permission checks: drop one Googly Eye with the first eye's effective appearance (server-side `HeadInfo` resolve by entity id, adult), clear the head's eyes, cost one durability, shear sound, `GameEvent.SHEAR`. Plain shears do nothing to a head.
4. **Breaking keeps eyes.** Head loot copies the mod's compound from the block entity onto the dropped item's `BlockEntityTag` at the bridge path.
   - Forge: a global loot modifier on head blocks, reading `LootContextParams.BLOCK_ENTITY`.
   - Fabric: `LootTableEvents.MODIFY` on the vanilla head loot tables, adding a copy-from-block-entity function to their pool.
   - Placement needs nothing: vanilla loads `BlockEntityTag` into the new block entity, and the bridge's load picks it up.
5. **Item tooltip.** An eyed head item shows that it has googly eyes and its stored colors and glow, using the eye items' existing tooltip lines and lang keys where they fit.

## Phase 3 — Rendering

1. **Render hook** (client mixin, once per loader): `SkullBlockRenderer.render` records the block entity being drawn (a static "current skull", cleared at TAIL); an injection in the static `renderSkull` right after the model draw calls common `SkullEyeRenderer.render(...)` with the pose, buffers, light, and model. With no current skull (worn heads, items), it does nothing.
2. **`SkullEyeRenderer`** (common): applies the model's head part transform (access-widen the `head` field on `SkullModel` and `PiglinHeadModel`), then for each head-attached eye captures gravity and draws through `GooglyEyeRenderer.drawAnimatedEye`, the same path the held eye uses. Honors `googlyEyesEnabled` and the client hide-lists by the mapped entity id; invisibility doesn't apply.
3. **Client tracker `SkullEyes`** (client, the `HeldEye` pattern rather than a `GooglyTracker` refactor): holds the head's `HeadInfo`, an `EyeInfo[][]`, the active `BehaviorInstance` (with `HeadInfo` so cross-eye works), and the snapshot it was built from. `ClientEyeRuntime` keeps them in a map keyed by `BlockPos`, with the same render-stamp eviction and simulate-only-while-rendered rules, cleared on disconnect and level change. The tracker re-reads the snapshot only when the block entity's compound object changes (a new load replaces it), so nothing is parsed from NBT per frame.
4. **Physics.** Each tick: overlays via `EyeInfo.applyOverlays`, physics via `EyeInfo.update` with zero motion and the head's fixed yaw; gravity comes from the render pose, so wall and rotated floor heads settle correctly.

## Phase 4 — Head behaviors

1. **Sync the ambient settings.** Add the four ambient values to `EyeConfigSyncPacket` and to `ServerServices.broadcastEyeConfigsIfChanged`'s change test; mirror them in `ClientEyeConfigs`. Bump `NETWORK_VERSION` to 16.
2. **Deterministic schedule** (in `SkullEyes`): time is cut into slots of `(ambientMinTicks + ambientMaxTicks) / 2` ticks. For slot `k`, a random source seeded from the head's position and `k` picks one behavior from the pool and a start offset that leaves room for its default duration. Each tick the tracker starts the slot's behavior when game time reaches its start, or, for a tracker created mid-behavior, starts it with `elapsed` set to the ticks already past (the existing catch-up path). Off when `ambientBehaviors` is off or the switch is off.

## Phase 5 — Inspector

Extend the sneak-and-aim check (`EyeInspector`) to a targeted head block: can receive eyes, already has eyes, or has no configuration. New lang keys as needed.

## Phase 6 — GameTests

Shared logic in `common/src/gametest`, thin wrappers per loader, javadoc telling how to reproduce each in game:

- a Slimy Eye on a placed skeleton head gives it eyes with the item's appearance and consumes the item;
- refusals: a head that already has eyes, a dragon head, `googlyEyesEnabled = false`;
- Optometrist on an eyed head drops a matching Googly Eye, clears the head, costs durability;
- breaking an eyed head drops an item carrying the snapshot; placing that item restores it;
- an eyed, tinted skeleton killed by a charged creeper's explosion drops a head with its snapshot; an eyeless one drops a plain head;
- head eye state survives save and load.

Rendering, the render hook on both loaders, and the client-derived behaviors stay manual.

## Phase 7 — Documentation

Update `orientation-player.md` (heads section, limitations), `orientation-code.md` (state, bridge, render hook, schedule, network version 16, test counts), and the `build-env/` snapshot only if a build file changes. Add a README line under "Getting eyes".

## Open verification items

- Forge `ForgeData` save/load behavior on block entities (Phase 1.4).
- Exact `SkullModel` / `PiglinHeadModel` head-part field names for the access widener, and the `renderSkull` injection point (signature and where the model draw sits).
- Fabric API 0.92.11 `LootTableEvents.MODIFY` supports adding a function to an existing pool (`modifyPools`).
- Which zombie variants drop a zombie head on a charged-creeper kill; the stamp is type-agnostic either way.
