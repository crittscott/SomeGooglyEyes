# Report: Correctness (26.1.2)

Applied 2026-11-12, unbuilt (nothing compiled, built, or tested). Each 26.1.1 file was diffed against 26.1.2's (`diff --strip-trailing-cr`) and only this topic's hunks taken. Four files were copied whole because their entire diff was this topic's; EyeItemService and PickerFreezeService were copied from 26.1.1 and then adapted (deviations below). Line endings kept (LF). Expected-way and Multiloader-organization hunks untouched.

## 1. Holding a Slimy Eye blocks riding, trading, and other mob clicks
- item/EyeItemService.java (copied from 26.1.1, adapted): entity `applySlimyEye(ItemStack, Player, Entity)` on both sides (non-`CAN_HAVE` → `PASS`, protected player → `FAIL`, client `SUCCESS`, server applies with `consumeOne`), spectator check gone; `interact` and the boat branch call it on both sides; `useOnSkull` checks the block entity first, then the Slimy Eye verdict or, for shears, `isHarvestTool`, `enabled(level)`, a head with a mob, and eyes; head `applySlimyEye` and `harvestSkull` private; `isHarvestTool` added.
- Deviations from 26.1.1 (26.1.2 additions kept): head paths take `BlockEntity` (javadoc links `verdictFor(BlockEntity)`, `applySlimyEye(ItemStack, ServerPlayer, BlockEntity, Direction)`); `useOnSkull` passes unless the block entity is a `SkullEyeState.Holder`; `verdictFor(BlockEntity)` keeps its `Holder` test (at the end of the class, as in 26.1.1); `harvestSkull` uses `headParts(entity, helper)`; the percent roll stays `nextInt(PERCENT_MAX)` (Organization 1); `playShearSound` restored and called by `shearEyes` (Dead-code 9). Otherwise the file equals 26.1.1's.
- item/SlimyEyeItem.java: `use` returns `applySlimyEye(stack, player, player)`; client branch and `ServerPlayer` import gone; spectator check kept.
- Tests: EligibilityGameTestsLogic `slimyEyeRefusesAnAlreadyEyedTarget` drives `interact` and expects `PASS` (javadoc adds the horse step); BoatEyeGameTestsLogic `boatsRefuseSpectatorMutation` drops its `applySlimyEye` assertion; SkullEyeGameTestsLogic expects `PASS` for an eyed head, a dragon head, and the switch off. Unchanged and unaffected: the Heads tests' `CONSUME` assertions, the `mayAlter` denial `FAIL`, EyeItemServiceGameTestsLogic's PvP `FAIL`, and NeoForge TwilightHeadGameTests (applies to an eyeless trophy, shears an eyed one, ignores result values).

## 2. A player's own eyes never play expressions unless someone else is watching
ServerBehaviorScheduler: `MobState.self`, `onPlayerJoined`, `onPlayerLeft`, private `removeWatcher` (used by `onStopTracking`), class-javadoc paragraph. ServerServices: `onLivingEntityLoaded` calls `onPlayerJoined` (javadoc updated); `onPlayerLeft` also calls the scheduler (keeps `PickerFreezeService.onPlayerLoggedOut` for Dead-code 9); new `onPlayerCloned`, called by Forge/NeoForge `PlayerEvent.Clone` (`ServerPlayer` casts) and Fabric `COPY_FROM` (method reference). orientation-code.md: "(players watch their own)".

## 3. On Forge, frogs grown from tadpoles can never get natural eyes
`EyeState.copy` skips a default-state source that `ServerEyeConfigs.canEverWearEyes` rejects; `ServerEyeConfigs` import and javadoc sentence. EyeState's other 26.1.1 hunks left for their topics.

## 4. Client predictions ignore what the server will decide
Covered by finding 1.

## 5. A thorns kill while holding shears counts as a shears harvest
`onDeath` requires `source.is(DamageTypes.PLAYER_ATTACK)` and a `Player` causing entity (EyeItemService copy). SomeGooglyGameTestsLogic: `onDeath(cow, damageSources().thorns(player), ...)` case added to 26.1.2's current (pre-Test-completeness) `deathHarvestRejectsNonqualifyingKills`; javadoc lists Thorns.

## 6. Shears that break on a harvest kill don't send Forge's broken-item event
With the EyeItemService copy: `consumeOne` on both Slimy Eye paths; one `wearShears(Player, InteractionHand)` at the end of the class, comment at `onDeath`.

## 7. The GeckoLib eye layer crashes on non-living GeckoLib entities
None; already fixed.

## 8. A picker-frozen mob that converts or changes dimension stays AI-less
`ServerServices.onConverted`, called by Forge/NeoForge `LivingConversionEvent.Post` and Fabric `MOB_CONVERSION`; the loader event classes no longer import `EyeState`. PickerFreezeService copied from 26.1.1 (`FrozenRecord(mobId, prevNoAi)`, `restore` searches every level, `freeze` checks other players' records first, new `onConverted`); as in 26.1.1, `onPlayerLoggedOut` and its class-javadoc `{@link}` restored for Dead-code 9. orientation-code.md: "hands a lock to a converted mob".

## 10. Plain-shears self-shear deals the main-hand weapon's damage
With the EyeItemService copy: `getAttributeBaseValue`, javadoc updated. orientation-player.md: "a bare-handed hit of health".

## 11. The held Googly Eye ignores the master switch
Superseded by Organization 1; no change.

## 12. Picker export can write a broken pack.mcmeta on some locales
PickerExportService: `String.format(Locale.ROOT, ...)` and the `Locale` import; other hunks left.

## 13. Deleting an eye in the picker scrambles cross-eye pairings
PickerState: `crossTargetAfterDelete`; `delete` remaps the remaining eyes and the draft (javadoc updated). Other PickerState hunks left.

## 14. The GeckoLib bone cache can return another model's bones
ModelMemo (`IdentityHashMap`), GeckoIntegration (`clearCaches`), GeckoCompat (`clearCaches` guarded by `LOADED`) copied whole from 26.1.1. GeoBones: `clear()` and new javadoc (`attachmentPath` hunk left). ClientRenderLayers: `clearCaches` calls `GeckoCompat.clearCaches()` after 26.1.2's `SkullEyeRenderer.clearCaches()` (kept); `layers.add` hunk left.

## 15. The item-draw context can leak every frame on Fabric
None; does not apply.

## 16. A refused picker freeze still releases the mob you were editing
Covered by finding 8's PickerFreezeService copy.

## 18. Forge config reload copies values off the server thread
Forge and NeoForge server configs: `applyValues`; a reload hands copy, applied log, and broadcast to `server.execute` together; `Loading` applies in place. `define` not moved (Organization 14).

## 19. Taming plays no love
`EntityEvent.TAMING_SUCCEEDED` with the javadoc; orientation-player.md lists taming.

## 22. The sheared-sheep wool inset is missing from the picker preview
LayerGooglyEyes copied whole from 26.1.1 (only this topic's two lines differed).

## 23, 24
None.

## Docs
orientation-code.md held at 12,000 body characters with two wording trims ("At extraction, placed heads check compound identity and frames check item components."; "whichever is longer."). orientation-player.md 14,804 characters. Version labels untouched. build-env/orientation-*.md already differed from the root copies and were not touched. Changelog unchanged (26.1.1's Correctness did not edit its changelog).

## Left for other topics / the parent
- Dead-code 9: `onPlayerLoggedOut` with its `{@link}` and call; `playShearSound`; PickerState's `{@link PickerFreezeService#onPlayerLoggedOut}`.
- Organization 1: `ServerConfig.roll` in EyeItemService and ServerServices; finding 11.
- Organization 14: moving `define`.
- Test-completeness 5: carry the thorns case into the rewritten `deathHarvestRejectsNonqualifyingKills`.
- Other topics' event hunks: `onHurt`/`onHealed` in the loader event classes and the scheduler; hand reactions.

## Recalled API facts and checks only a build or in-game test can settle
- First build settles: `stateFor(player)` with a `ServerPlayer`; the `HolderLookup` and `DamageTypes` imports in EyeItemService; `BlockEntity` vs `SkullEyeState.Holder` in `useOnSkull` and the private head methods; EligibilityGameTestsLogic still calls `applySlimyEye(stack, (ServerPlayer) player, cow)` (an unconfigured or disabled target now returns `PASS` rather than `FAIL`; those assertions use `!consumesAction()`).
- Recalled, unverified: `DamageSources.thorns(Entity)` has the player as causing entity and is not `PLAYER_ATTACK`; vanilla never calls `Item.use` for a spectator; `PlayerEvent.Clone` is server-side only on Forge 64.1.5 and NeoForge 26.1.2.112; Fabric API 0.155.3 `COPY_FROM` is `(ServerPlayer, ServerPlayer, boolean)`; `MinecraftServer.getAllLevels()` exists; `ItemStack.consume(int, LivingEntity)` skips consumption in creative; GeckoLib 5.5.2 baked models are value-comparing records; tadpole growth converts through `Mob.convertTo`.

## In-game checks
- With a Slimy Eye in hand, mount an eyed horse and trade with an eyed villager; right-clicking an eyed placed skull or a dragon head gives the click to the block.
- Alone in third person, your eyes blink and react, also after respawning and after leaving the End.
- Forge and NeoForge, frog chance 100%: frogs grown from tadpoles get eyes.
- Freeze a pig in the picker and strike it with lightning; releasing it restores the piglin's AI.
- Taming a wolf plays love.
- In the picker preview, a sheared sheep's eyes sit on the bare body.
- After F3+T with GeckoLib mobs (Fabric, NeoForge, Forge), eyes stay on the right bones.
- Editing the Forge or NeoForge server config in game applies cleanly.
- A picker export under a comma-decimal locale writes a valid pack.mcmeta.
- Deleting an eye in the picker keeps the other cross-eye pairings.
