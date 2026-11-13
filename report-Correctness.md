# Report: Correctness (26.2)

Applied 2026-11-12 by taking 26.1.1's Correctness hunks into 26.2 one finding at a time, while other topics were editing the same files. Nothing was compiled, built or tested. The Fix lines in `review-Correctness.md` are filled in.

## 1 (and 4). Slimy Eye blocks mob clicks; client predictions ignore the server
- item/EyeItemService.java: the entity `applySlimyEye(ItemStack, Player, Entity)` runs on both sides (non-`CAN_HAVE` target → `PASS`, protected player → `FAIL`, client `SUCCESS`, server applies and spends the eye with `consumeOne`); its own spectator check is gone (`interact` and `SlimyEyeItem.use` keep theirs). `interact` and `interactBoat` call it on both sides. `useOnSkull` tests `instanceof SkullBlockEntity` first; for a Slimy Eye it checks the verdict (anything but `CAN_HAVE` gives `PASS`), for shears `isHarvestTool`, `enabled(level)`, a head that maps to a mob, and eyes. The head `applySlimyEye` and `harvestSkull` are private and no longer recheck; only `mayAlter` remains. `isHarvestTool` added. Javadocs match 26.1.1.
- item/SlimyEyeItem.java: `use` returns `applySlimyEye(stack, player, player)`; the client branch and `ServerPlayer` import are gone; the spectator check stays.
- Tests: `slimyEyeRefusesAnAlreadyEyedTarget` drives `interact` and expects `PASS` (javadoc adds the horse step); `boatsRefuseSpectatorMutation` drops its `applySlimyEye` assertion; SkullEyeGameTestsLogic's three vanilla-head `FAIL` assertions (eyed head, dragon head, switch off) expect `PASS`.
- Kept from 26.2: the `net.minecraft.advancements.triggers.CriteriaTriggers` import. `playShearSound` was Dead-code 9's to inline.
- Deviation: the SkullEyeGameTestsLogic change was a scripted exact-string replace, not the Edit tool.

## 2. A player's own eyes never play expressions unless someone else is watching
- ServerBehaviorScheduler: `MobState.self`; `onPlayerJoined` and `onPlayerLeft` after `onMilked`; private `removeWatcher`, used by `onStopTracking`; a class-javadoc paragraph.
- ServerServices: `onLivingEntityLoaded` calls `onPlayerJoined` (javadoc updated); `onPlayerLeft` also calls the scheduler; new `onPlayerCloned(original, replacement, alive)`.
- Loaders: Forge and NeoForge `PlayerEvent.Clone` call `onPlayerCloned` with `ServerPlayer` casts and `!isWasDeath()`; Fabric `COPY_FROM.register(ServerServices::onPlayerCloned)`.
- orientation-code.md: "(players watch their own)".

## 3. Frogs grown from tadpoles never get natural eyes (Forge/NeoForge)
`EyeState.copy` skips a default-state source that `ServerEyeConfigs.canEverWearEyes` rejects; import and javadoc added.

## 5. A thorns kill counts as a shears harvest
`onDeath` requires `source.is(DamageTypes.PLAYER_ATTACK)` and a `Player` causing entity (`DamageTypes` import). A thorns case was added to the then-current `deathHarvestRejectsNonqualifyingKills` (javadoc names Thorns); Test-completeness 5's rewrite carries its own thorns case.

## 6. Broken shears don't send Forge's broken-item event
Both Slimy Eye paths use `consumeOne(Player, ItemStack)`; one `wearShears(Player, InteractionHand)` at the end of the class replaces the three-argument version; the explaining comment is at `onDeath`.

## 8 and 16. Picker-frozen mob conversions; a refused freeze releases your mob
- `ServerServices.onConverted` copies the eyes and calls the new `PickerFreezeService.onConverted`; called from Forge/NeoForge `LivingConversionEvent.Post` and Fabric `MOB_CONVERSION`; the three loader event classes no longer import `EyeState`.
- PickerFreezeService: `FrozenRecord(mobId, prevNoAi)` (`ResourceKey`/`Level` imports gone, `LivingEntity` added); `restore` searches `getAllLevels()`; `freeze` checks other players' records before releasing the player's current mob; `onConverted` with javadoc, plus a class-javadoc sentence.
- orientation-code.md: "hands a lock to a converted mob".
- `onPlayerLoggedOut` was kept here; Dead-code 9 removed it concurrently.

## 10. Self-shearing deals the weapon's damage
`getAttributeBaseValue`, javadoc updated. orientation-player.md says "a bare-handed hit of health".

## 12. Locale-dependent pack.mcmeta
`String.format(Locale.ROOT, ...)` with the `Locale` import.

## 13. Deleting a picker eye scrambles cross-eye pairings
`PickerState.crossTargetAfterDelete` added; `delete` remaps the remaining eyes and the draft; javadoc updated.

## 14. GeckoLib bone cache
`ModelMemo` uses `IdentityHashMap` (javadoc updated, weak-key comment removed); new `GeoBones.clear`, `GeckoIntegration.clearCaches`, `GeckoCompat.clearCaches` (guarded by `LOADED`); `ClientRenderLayers.clearCaches` calls `GeckoCompat.clearCaches()`. Forge's `ForgeGeckoLayers` uses the same common cache.

## 18. Forge/NeoForge config reload runs off the server thread
Both server configs get `applyValues`; a reload hands the copy, the applied log, and the broadcast to `server.execute` together; `Loading` applies in place. `define` is Organization 14's.

## 19. Taming plays no love
`EntityEvent.TAMING_SUCCEEDED` added to the affection triggers, with the javadoc; orientation-player.md lists taming.

## 22. Sheared-sheep inset missing from the picker preview
`renderPreview` calls `insetShearedSheepBody` before the saved-eye and gizmo draws.

## 7, 11, 15, 23, 24
No change. Finding 11 is done with Organization 1.

## Docs
orientation-code.md went over 12,000 characters (concurrent edits), so wording in the Eligibility paragraph was shortened ("config changes", "whichever is longer", "Anger syncs on change; only angry entities get an initial packet", `ClientEyeRuntime` "keeps mob reports apart from simulations", master switch "keeping watchers and expiring effects; authoring triggers still work"). orientation-player.md 14,807 characters. Changelog not edited, matching 26.1.1.

## GameTests
No tests added or removed on Fabric, Forge or NeoForge.

## First build settles
`ServerBehaviorScheduler.stateFor(player)` with a `ServerPlayer`; the `DamageTypes` and `HolderLookup` imports in EyeItemService; the remaining `applySlimyEye(stack, (ServerPlayer) player, cow)` calls in EligibilityGameTestsLogic (refused targets now get `PASS`; the assertions use `!consumesAction()`).

## Recalled API facts (unverified)
`DamageSources.thorns(Entity)` has the player as causing entity and is not `PLAYER_ATTACK`; vanilla never calls `Item.use` for a spectator; `PlayerEvent.Clone` fires server-side only on Forge 65 and NeoForge 26.2; Fabric API 0.161.0's `COPY_FROM` is `(ServerPlayer, ServerPlayer, boolean)`; `MinecraftServer.getAllLevels()` exists; `ItemStack.consume(int, LivingEntity)` skips consumption in creative; GeckoLib baked models are value-comparing records; `EntityEvent.TAMING_SUCCEEDED` exists in 26.2; tadpole growth converts through `Mob.convertTo`.

## In-game checks
With a Slimy Eye in hand, an eyed horse can be mounted and an eyed villager traded with. Alone in third person, your eyes blink and react, also after respawn and after leaving the End. Forge/NeoForge with frog chance 100%: frogs grown from tadpoles get eyes. Freeze a pig in the picker and strike it with lightning; releasing the lock restores the piglin's AI. Taming a wolf plays love. In the picker preview, a sheared sheep's eyes sit on the bare body. After a resource reload, a GeckoLib mob's eyes stay on the right bones.
