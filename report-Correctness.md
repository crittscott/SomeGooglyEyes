# Report: Correctness (1.21.11)

Applied 2026-11-11 by hand-porting 1.21.8's changes. Nothing was compiled, built, or tested; everything here comes from reading the source. Each file was diffed against 1.21.8's and only this topic's hunks were taken; no file was copied whole.

## 1 (and 4). Slimy Eye blocks mob clicks; client predictions ignore the server
- item/EyeItemService.java: the entity `applySlimyEye(ItemStack, Player, Entity)` runs on both sides (non-`CAN_HAVE` → `PASS`; protected by `mayAffect` → `FAIL`; client `SUCCESS`; server applies, `consumeOne`, `CONSUME`); its spectator check is gone (covers Guarding 4). `interact` and the boat branch call it on both sides; the boat branch reorganized as in 1.21.8. `useOnSkull` checks `SkullBlockEntity` first; Slimy Eye needs `verdictFor(skull) == CAN_HAVE`; shears need `isHarvestTool`, `enabled(level)`, a head with a mob, and eyes (moved up from `harvestSkull`). Head `applySlimyEye` and `harvestSkull` private, javadocs updated. `isHarvestTool` added and used at the three sites, so **Organization 7 has nothing left to do**.
- item/SlimyEyeItem.java: `use` returns `applySlimyEye(stack, player, player)`; client branch and `ServerPlayer` import gone.
- Tests: `slimyEyeRefusesAnAlreadyEyedTarget` drives `interact` and expects `PASS` (javadoc gains the horse step); `boatsRefuseSpectatorMutation` drops its `applySlimyEye` assertion; SkullEyeGameTestsLogic expects `PASS` for eyed head, dragon head, switch off.
- Deviations: `SlimyEyeItem.use` keeps its `isSpectator()` check (Guarding 4 relies on it). Kept 1.21.11's `headParts(entity, helper)`/`canHaveEyes(entity, …)` (Heads), `isPvpAllowed` in `mayAffect`, `GameRules.MOB_DROPS`, the `nextInt(PERCENT_MAX)` roll, `playShearSound` (Dead-code 9).
- In-game: with a Slimy Eye in hand, mount an eyed horse and trade with an eyed villager; eyed head, dragon head, and switch off leave the click to the block.

## 2. A player's own eyes never play expressions unless someone else is watching
- ServerBehaviorScheduler: `MobState.self`, `onPlayerJoined`, `onPlayerLeft`, private `removeWatcher` (used by `onStopTracking`); 1.21.8's class-javadoc paragraph.
- ServerServices: `onLivingEntityLoaded` calls `onPlayerJoined`; `onPlayerLeft` calls the scheduler (its `PickerFreezeService.onPlayerLoggedOut` call left for Dead-code 9); new `onPlayerCloned`, called by Forge/NeoForge `PlayerEvent.Clone` and Fabric `COPY_FROM`.
- orientation-code.md: players watch their own eyes (body 11,997 characters).
- In-game: alone in third person, your eyes blink and react, also after respawn and leaving the End.

## 3. Frogs grown from tadpoles never get natural eyes (Forge)
`EyeState.copy` skips a default-state source that `ServerEyeConfigs.canEverWearEyes` rejects (1.21.8's javadoc). In-game on Forge and NeoForge: frog chance 100%, raise tadpoles.

## 5. Thorns kill counts as a shears harvest
`onDeath` requires `source.is(DamageTypes.PLAYER_ATTACK)` and a `Player` causing entity. A `damageSources().thorns(player)` case is added to the current `deathHarvestRejectsNonqualifyingKills`; Test-completeness 5 must carry it.

## 6. Broken shears / emptied Slimy Eye stacks not reported
`consumeOne` on both Slimy Eye paths; one `wearShears(Player, InteractionHand)` with 1.21.8's comment at `onDeath`; keeps 1.21.11's `hand.asEquipmentSlot()`.

## 8 and 16. Picker-frozen mob conversions; refused freeze releases your mob
- `ServerServices.onConverted` copies eyes and calls new `PickerFreezeService.onConverted`; called from Forge/NeoForge `LivingConversionEvent.Post` and Fabric `MOB_CONVERSION` (loader event classes no longer import `EyeState`).
- PickerFreezeService: `FrozenRecord(mobId, prevNoAi)`; `restore` searches every level; `freeze` checks other players' records before releasing; `onConverted` plus javadoc. `onPlayerLoggedOut` and its `{@link}` kept for Dead-code 9.
- orientation-code.md: a lock passes to the converted mob.
- In-game: freeze a pig, strike it with lightning; releasing restores the piglin's AI; a frozen mob that goes through a portal is released when you let it go. `conversionKeepsEyeState` on every loader shows conversions reach the hooks.

## 10. Self-shear deals the weapon's damage
`getAttributeBaseValue`, javadoc updated; orientation-player.md says "a bare-handed hit of health".

## 11. Held eye ignores the master switch
No change; superseded by Organization 1, which must carry 1.21.8's visibility gate.

## 12. Locale-dependent pack.mcmeta
`String.format(Locale.ROOT, …)`, keeping 1.21.11's `min_format`/`max_format` fields.

## 13. Picker delete scrambles cross-eye pairings
`PickerState.crossTargetAfterDelete` added; `delete` remaps remaining eyes and the draft.

## 14. GeckoLib bone cache
`ModelMemo` uses `IdentityHashMap` (javadoc rewritten); new `GeoBones.clear`, `GeckoIntegration.clearCaches`, `GeckoCompat.clearCaches` (guarded by `LOADED`); `ClientRenderLayers.clearCaches` calls `GeckoCompat.clearCaches()` after its kept `SkullEyeRenderer.clearCaches()`. Recalled: GeckoLib 5.4 baked models are value-comparing records.

## 18. Forge config reload off the server thread
Forge/NeoForge server configs: new `applyValues`; `Reloading` hands copy, applied log, and broadcast to `server.execute` together; `Loading` applies in place. `define` not moved (Organization 14).

## 19. Taming plays no love
`EntityEvent.TAMING_SUCCEEDED` added (javadoc); orientation-player.md lists taming. In-game: taming a wolf or horse plays love.

## 22. Sheared-sheep inset missing from picker preview
`LayerGooglyEyes.renderPreview` calls `insetShearedSheepBody` before saved-eye and gizmo draws (hand-ported onto the submit-based layer). In-game: a sheared sheep's body eyes and gizmo sit on the bare body.

## 7, 15, 23, 24
No change.

## Recalled API facts (unverified)
`DamageSources.thorns(Entity)` has the player as causing entity and is not `PLAYER_ATTACK`; `playerAttack` is `PLAYER_ATTACK`; vanilla never calls `Item.use` for a spectator; Forge `PlayerEvent.Clone` is server-side only (casts to `ServerPlayer`); Fabric `COPY_FROM` is `(ServerPlayer, ServerPlayer, boolean)`; `MinecraftServer.getAllLevels()` exists; `ItemStack.consume(int, LivingEntity)` skips consumption in creative.

## Left to other topics
Dead-code 9 (`onPlayerLoggedOut`, `playShearSound`); Organization 1 (finding 11); Organization 14 (`define`); Test-completeness 5 (thorns case in the rewritten test).

## Clash points
EyeItemService, SlimyEyeItem, ServerServices, ServerBehaviorScheduler, PickerFreezeService, PickerState, the three loader ServerEvents, Forge/NeoForge ServerConfig, EyeState, GeoBones, ModelMemo, ClientRenderLayers, LayerGooglyEyes, Eligibility/Boat/SkullEye/SomeGooglyGameTestsLogic, orientation-player.md, orientation-code.md (body now 11,997 of 12,000 characters).

## Files modified
common main: item/EyeItemService.java, item/SlimyEyeItem.java, eye/behavior/ServerBehaviorScheduler.java, eye/state/EyeState.java, server/ServerServices.java, picker/PickerFreezeService.java, picker/PickerExportService.java, client/picker/PickerState.java, client/render/resolver/ModelMemo.java, client/compat/gecko/GeoBones.java, client/compat/gecko/GeckoIntegration.java, client/compat/GeckoCompat.java, client/ClientRenderLayers.java, client/render/LayerGooglyEyes.java. Gametest: EligibilityGameTestsLogic, BoatEyeGameTestsLogic, SkullEyeGameTestsLogic, SomeGooglyGameTestsLogic. Loaders: FabricServerEvents, ForgeServerEvents, NeoForgeServerEvents, ForgeServerConfig, NeoForgeServerConfig. Docs: orientation-player.md, orientation-code.md, review-Correctness.md.
