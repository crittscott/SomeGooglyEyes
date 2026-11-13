# Report: Correctness (26.1.1)

Applied 2026-11-11 by taking 26.1's Correctness hunks, one finding at a time. Nothing was compiled, built, or tested. Fix lines and intro in review-Correctness.md filled in.

## 1 (and 4). Slimy Eye blocks mob clicks; client predictions ignore the server
- item/EyeItemService.java: entity `applySlimyEye(ItemStack, Player, Entity)` runs on both sides (non-`CAN_HAVE` → `PASS`, protected player → `FAIL`, client `SUCCESS`, server applies with `consumeOne`); its spectator check gone (`interact` and `SlimyEyeItem.use` keep theirs). `interact` and the boat branch call it on both sides. `useOnSkull` checks the block entity first, then the Slimy Eye verdict or, for shears, `isHarvestTool`, `enabled(level)`, a head with a mob, and eyes. Head `applySlimyEye` and `harvestSkull` private. `isHarvestTool` added, so Organization 7 has nothing left.
- item/SlimyEyeItem.java: `use` returns `applySlimyEye(stack, player, player)`; client branch and `ServerPlayer` import gone; spectator check kept.
- Tests: `slimyEyeRefusesAnAlreadyEyedTarget` drives `interact`, expects `PASS` (javadoc adds the horse step); `boatsRefuseSpectatorMutation` drops its `applySlimyEye` assertion; SkullEyeGameTestsLogic expects `PASS` for eyed head, dragon head, switch off.
- Deviations: `onDeath` roll stays `nextInt(PERCENT_MAX) >= ...` (26.1's `ServerConfig.roll` comes from a later topic); `playShearSound` kept for Dead-code 9.

## 2. A player's own eyes never play expressions unless someone else is watching
ServerBehaviorScheduler: `MobState.self`, `onPlayerJoined`, `onPlayerLeft`, private `removeWatcher` (used by `onStopTracking`), class-javadoc paragraph. ServerServices: `onLivingEntityLoaded` calls `onPlayerJoined`; `onPlayerLeft` also calls the scheduler (keeps `PickerFreezeService.onPlayerLoggedOut` for Dead-code 9); new `onPlayerCloned(original, replacement, alive)`, called by Forge/NeoForge `PlayerEvent.Clone` (`ServerPlayer` casts) and Fabric `COPY_FROM`. orientation-code.md "(players watch their own)".

## 3. Frogs grown from tadpoles never get natural eyes (Forge)
`EyeState.copy` skips a default-state source that `ServerEyeConfigs.canEverWearEyes` rejects; import and javadoc updated. EyeState's other 26.1 hunks left for their topics.

## 5. Thorns kill counts as a shears harvest
`onDeath` requires `source.is(DamageTypes.PLAYER_ATTACK)` and a `Player` causing entity. A `damageSources().thorns(player)` case added to the current `deathHarvestRejectsNonqualifyingKills` (javadoc lists Thorns); Test-completeness 5 must carry it.

## 6. Broken shears don't send Forge's broken-item event
`consumeOne` on both Slimy Eye paths; one `wearShears(Player, InteractionHand)` at the end of the class, with 26.1's comment at `onDeath`.

## 8 and 16. Picker-frozen mob conversions; refused freeze releases your mob
`ServerServices.onConverted` copies eyes and calls new `PickerFreezeService.onConverted`, called from Forge/NeoForge `LivingConversionEvent.Post` and Fabric `MOB_CONVERSION` (loader event classes no longer import `EyeState`). PickerFreezeService: `FrozenRecord(mobId, prevNoAi)`; `restore` searches every level; `freeze` checks other players' records first; `onConverted` with javadoc; `onPlayerLoggedOut` and its `{@link}` kept for Dead-code 9. orientation-code.md "hands a lock to a converted mob".

## 10. Self-shear deals the weapon's damage
`getAttributeBaseValue`, javadoc updated; orientation-player.md "a bare-handed hit of health".

## 12. Locale-dependent pack.mcmeta
`String.format(Locale.ROOT, ...)`, `Locale` import. PickerExportService's other hunks left for their topics.

## 13. Picker delete scrambles cross-eye pairings
`PickerState.crossTargetAfterDelete`; `delete` remaps remaining eyes and the draft. PickerState's other hunks (import package, clamps, `currentVariant`, `{@link}` to `unfreeze`) left.

## 14. GeckoLib bone cache
`ModelMemo` uses `IdentityHashMap`; new `GeoBones.clear`, `GeckoIntegration.clearCaches`, `GeckoCompat.clearCaches` (guarded by `LOADED`); `ClientRenderLayers.clearCaches` calls it. GeoBones' `attachmentPath` and ClientRenderLayers' `layers.add` hunks left.

## 18. Forge config reload off the server thread
Forge/NeoForge server configs: `applyValues`; a reload hands copy, applied log, and broadcast to `server.execute` together; `Loading` applies in place. `define` not moved (Organization 14).

## 19. Taming plays no love
`EntityEvent.TAMING_SUCCEEDED` with javadoc; orientation-player.md lists taming.

## 22. Sheared-sheep inset missing from picker preview
`renderPreview` calls `insetShearedSheepBody` before the saved-eye and gizmo draws.

## 7, 11, 15, 23, 24
No change; 11 superseded by Organization 1.

## Files copied whole from 26.1
ModelMemo, LayerGooglyEyes, GeckoIntegration, GeckoCompat (entire diff this topic's). PickerFreezeService copied, then `onPlayerLoggedOut` and its `{@link}` restored (remaining diff only Dead-code 9). **EyeItemService copied, then the `ServerConfig.roll` call reverted and `playShearSound` restored; its remaining diff against 26.1 is only that roll and Dead-code 9, so other topics' EyeItemService hunks are already in.** SlimyEyeItem edited (LF here, CRLF in 26.1).

## Docs
orientation-code.md 11,940 characters after header; orientation-player.md 14,878. Version labels untouched.

## Left to later topics
Dead-code 9 (`onPlayerLoggedOut`, its `{@link}` and call, `playShearSound`, PickerState's `{@link}`); Test-completeness 5 (thorns case); Organization 14 (`define`); Organization 1 (finding 11); hand-reaction and `onHurt` hunks in the loader event classes and scheduler.

## First build settles
`ServerBehaviorScheduler.stateFor(player)` with a `ServerPlayer`; `HolderLookup` and `DamageTypes` imports in EyeItemService. EligibilityGameTestsLogic still calls `applySlimyEye(stack, (ServerPlayer) player, cow)`; an unconfigured target now returns `PASS` not `FAIL`, and those assertions use `!consumesAction()`, so they hold.

## Recalled API facts (unverified)
`DamageSources.thorns(Entity)` has the player as causing entity and is not `PLAYER_ATTACK`; vanilla never calls `Item.use` for a spectator; `PlayerEvent.Clone` is server-side only on Forge 63 and NeoForge; Fabric API 0.145.4 `COPY_FROM` is `(ServerPlayer, ServerPlayer, boolean)`; `MinecraftServer.getAllLevels()` exists; `ItemStack.consume(int, LivingEntity)` skips consumption in creative; GeckoLib baked models are value-comparing records.

## In-game checks
Slimy Eye in hand: mount an eyed horse, trade with an eyed villager. Alone in third person your eyes blink and react, also after respawn and leaving the End. Forge/NeoForge frog chance 100%: frogs from tadpoles get eyes. Freeze a pig, strike with lightning; release restores the piglin's AI. Taming a wolf plays love. Sheared sheep preview eyes sit on the bare body.
