# Report: Correctness (26.3)

Applied 2026-11-12. Nothing was compiled, built, or tested. 26.2 has no report-Correctness.md, so 26.1.1 is the reference. 26.2's working tree has already applied this topic (its review Fix lines all read "Took 26.1.1's change"), so its post-fix files were the hunk source, diffed against 26.3 and taken one finding at a time. No 26.3 API difference touches these hunks. Fix lines and the intro in review-Correctness.md are filled in.

## 1 (and 4). Slimy Eye blocks mob clicks; client predictions ignore the server
- item/EyeItemService.java:
  - Entity `applySlimyEye(ItemStack, Player, Entity)` runs on both sides: non-`CAN_HAVE` → `PASS`, protected player → `FAIL`, client `SUCCESS`, server applies with `consumeOne`. Its spectator check is gone; `interact` and `SlimyEyeItem.use` keep theirs.
  - `interact` and the boat branch call it on both sides.
  - `useOnSkull` checks for a `SkullBlockEntity` first, then the Slimy Eye verdict or, for shears, `isHarvestTool`, `enabled(level)`, a mapped head (`SkullEyeState.entityFor`), and eyes.
  - Head `applySlimyEye` and `harvestSkull` are private and no longer recheck.
  - `isHarvestTool` added.
- The file was taken from 26.2, then two things were put back: the `onDeath` roll is `nextInt(PERCENT_MAX) >= ...` (26.2's `ServerConfig.roll` belongs to a later topic), and `playShearSound` is restored and called from `shearEyes` (Dead-code 9). Its remaining diff against 26.2 is only those two items. 26.3's `net.minecraft.advancements.triggers.CriteriaTriggers` import is unchanged.
- item/SlimyEyeItem.java: `use` returns `applySlimyEye(stack, player, player)`; the client branch and the `ServerPlayer` import are gone; the spectator check is kept. Copied from 26.2; its whole diff was this finding.
- Tests:
  - EligibilityGameTestsLogic `slimyEyeRefusesAnAlreadyEyedTarget` puts the stack in hand, drives `interact`, and expects `PASS`; its javadoc adds the horse step.
  - BoatEyeGameTestsLogic `boatsRefuseSpectatorMutation` drops its `applySlimyEye` assertion.
  - SkullEyeGameTestsLogic expects `PASS` for an eyed head, a dragon head, and the switch off.
  - The other Eligibility calls `applySlimyEye(stack, (ServerPlayer) player, cow)` still compile; their refusal assertions use `!consumesAction()`, so `PASS` in place of `FAIL` holds.

## 2. A player's own eyes never play expressions unless someone else is watching
- ServerBehaviorScheduler: `MobState.self`; `onPlayerJoined` and `onPlayerLeft` after `onPlayerHurt`; private `removeWatcher`, used by `onStopTracking`; a class-javadoc paragraph.
- ServerServices: `onLivingEntityLoaded` calls `onPlayerJoined` (javadoc updated); `onPlayerLeft` also calls the scheduler and keeps `PickerFreezeService.onPlayerLoggedOut` for Dead-code 9; new `onPlayerCloned(original, replacement, alive)`.
- Forge/NeoForge `PlayerEvent.Clone` call `onPlayerCloned` with `ServerPlayer` casts; Fabric `COPY_FROM` registers `ServerServices::onPlayerCloned`.
- orientation-code.md: "(players watch their own)".

## 3. Frogs grown from tadpoles never get natural eyes (Forge/NeoForge)
- `EyeState.copy` skips a default-state source that `ServerEyeConfigs.canEverWearEyes` rejects; `ServerEyeConfigs` import and javadoc added.
- EyeState's other 26.2 hunks (roll removal, `drawRoll`, `onEyesLost`, overrides) are left for their topics.

## 5. Thorns kill counts as a shears harvest
- `onDeath` requires `source.is(DamageTypes.PLAYER_ATTACK)` and a `Player` causing entity.
- SomeGooglyGameTestsLogic `deathHarvestRejectsNonqualifyingKills` gets an `onDeath(cow, damageSources().thorns(player), ...)` call after the arrow case (still a direct `onDeath` call in 26.3); its javadoc lists Thorns. Test-completeness 5 must carry this case.

## 6. Broken shears don't send Forge's broken-item event
- `consumeOne(Player, ItemStack)` on both Slimy Eye paths.
- One `wearShears(Player, InteractionHand)` at the end of the class, with the comment at `onDeath`; the old three-argument `wearShears` is gone.

## 8 and 16. Picker-frozen mob conversions; a refused freeze releases your mob
- `ServerServices.onConverted` copies eyes and calls the new `PickerFreezeService.onConverted`; called from Forge/NeoForge `LivingConversionEvent.Post` and Fabric `MOB_CONVERSION`. The three loader event classes no longer import `EyeState`.
- PickerFreezeService: `FrozenRecord(mobId, prevNoAi)`; `restore` searches every level with `getAllLevels`; `freeze` checks other players' records before releasing the player's current mob (finding 16); `onConverted` added with javadoc. Copied from 26.2, then `onPlayerLoggedOut` and its class-javadoc `{@link}` restored for Dead-code 9.
- orientation-code.md: "hands a lock to a converted mob".

## 10. Self-shear deals the weapon's damage
- `getAttributeBaseValue(Attributes.ATTACK_DAMAGE)`, javadoc updated; orientation-player.md: "a bare-handed hit of health".

## 11
No change; superseded by Organization 1.

## 12. Locale-dependent pack.mcmeta
- `String.format(Locale.ROOT, ...)` with the `Locale` import. PickerExportService's other 26.2 hunks (excluded-type check, age check, `packetFor(...).refusal()`) are left for their topics.

## 13. Picker delete scrambles cross-eye pairings
- `PickerState.crossTargetAfterDelete` added; `delete` remaps the remaining eyes and the draft; javadoc updated. PickerState's other hunks (`client.platform` import, clamps, `currentVariant` clamp, `{@link}` to `unfreeze`) are left.

## 14. GeckoLib bone cache
- `ModelMemo` uses `IdentityHashMap` (copied from 26.2; whole diff was this finding).
- New `GeoBones.clear` (javadoc updated), `GeckoIntegration.clearCaches`, and `GeckoCompat.clearCaches` guarded by `LOADED`.
- `ClientRenderLayers.clearCaches` calls `GeckoCompat.clearCaches()`.
- Left for their topics: GeoBones' `EyeConfigModel.attachmentPath` hunk and import; ClientRenderLayers' `layers.add` hunk. There is no `SkullEyeRenderer.clearCaches()` in 26.3.

## 18. Forge config reload off the server thread
- Forge and NeoForge server configs get `applyValues()` before `onConfigChanged`. A `Reloading` event with a running server hands the copy, applied log, and broadcast to `server.execute` together; any other case applies in place.
- 26.3's NeoForge `ModConfig.Type.SYNCED` registration and its javadoc are kept. The in-place branch also covers a remote NeoForge client receiving the synced values; which event NeoForge fires there is unverified, and either path applies the values.
- `define` is not moved (Organization 14).

## 19. Taming plays no love
- `EntityEvent.TAMING_SUCCEEDED` added to the affection events, javadoc updated; orientation-player.md lists taming.

## 22. Sheared-sheep inset missing from the picker preview
- `renderPreview` calls `insetShearedSheepBody` before the saved-eye and gizmo draws. LayerGooglyEyes copied from 26.2; its whole diff was this finding.

## 7, 15, 23, 24
No change.

## Docs
orientation-code.md is 11,942 characters after the header; orientation-player.md is 14,931. Version labels untouched. build-env/ orientation copies hold only the header and headings. No changelog or platform-notes changes.

## GameTests
No required GameTests added or removed: +0 Fabric, +0 Forge, +0 NeoForge. Assertions changed in four Logic files (findings 1 and 5).

## Left to later topics
Dead-code 9 (`onPlayerLoggedOut`, its `{@link}`s and call, `playShearSound`); `ServerConfig.roll`; Test-completeness 5 (thorns case); Organization 14 (`define`); Organization 1 (finding 11); the hand-reaction and `onHurt` hunks in the scheduler and loader event classes; 26.2's EyeState, PickerState, PickerExportService, GeoBones, and ClientRenderLayers hunks from other topics.

## First build settles
`ServerBehaviorScheduler.stateFor(LivingEntity)` with a `ServerPlayer`; the `HolderLookup` and `DamageTypes` imports in EyeItemService; `DamageSources.thorns(Entity)` in the test; `MinecraftServer.getAllLevels()`.

## Recalled API facts (unverified)
`DamageSources.thorns` has the player as causing entity and is not `PLAYER_ATTACK`; vanilla never calls `Item.use` for a spectator; `PlayerEvent.Clone` is server-side only on Forge and NeoForge; Fabric `COPY_FROM` is `(ServerPlayer, ServerPlayer, boolean)`; `ItemStack.consume(int, LivingEntity)` skips consumption in creative; GeckoLib baked models are value-comparing records; tadpole growth converts through `Mob.convertTo`; which `ModConfigEvent` a NeoForge remote client receives for a SYNCED config.

## In-game checks
- With a Slimy Eye in hand, you can mount an eyed horse and trade with an eyed villager.
- Alone in third person, your eyes blink and react, also after respawning and after leaving the End.
- On Forge/NeoForge with frog chance 100%, frogs raised from tadpoles get eyes.
- Freeze a pig in the picker and strike it with lightning; releasing it restores the piglin's AI.
- Taming a wolf plays love.
- A sheared sheep's preview eyes sit on the bare body.
- Killing an eyed cow through Thorns while holding shears drops no Googly Eye.
- Plain-shears self-shear with a sword in the main hand deals bare-handed damage.
- After a resource reload (F3+T), GeckoLib mobs' eyes still attach.
- Deleting an eye in the picker keeps the other cross-eye pairings.
