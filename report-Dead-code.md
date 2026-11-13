# Report: Dead-code (26.2)

Applied 2026-11-12 by taking 26.1.1's Dead-code hunks while other topics were applied to 26.2 at the same time. Nothing was compiled, built, or tested. Fix lines in review-Dead-code.md filled in.

New file: `common/src/gametest/.../gametest/TestBehaviorSettings.java`, copied from 26.1.1 (LF, like its neighbors).

## 1. Mob expressions are timed in two places
- GooglyTracker: public `startBehavior` replaced by package-private `install`; asleep sets `active = null`, otherwise replaces `active` and sets its age. `update` no longer calls `active.advance()`. Class javadoc as 26.1.1.
- ClientEyeRuntime: `synchronizeBehavior(living, tracker)` has no `ageOffset`; a newly received behavior is installed at `elapsed`, an unchanged one has its age set. The two `get`-path calls drop `0`; the tracker sweep drops `-1` and passes `living`. `tickBehaviors` is private.
- BehaviorSchedulerGameTestsLogic: `BehaviorInstance` import and helpers `play`, `receivedBehaviorPlays`, `stepOnce`; four `startBehavior` asserts use `play`; the two sleeping checks use `!receivedBehaviorPlays`; `stepGoat`, `stepTracker`, `irisScaleRange`, `meanPupilStep` use `stepOnce`; `receivedExpressionsOutliveRenderTrackers` calls `ClientEyeRuntime.tick(helper.getLevel(), true)` in three places.
- Deviation: Networking 2, applied concurrently, dropped `install`'s duration. The final code is 26.1.1's `install(behavior, seed, age)`, and `receivedBehaviorPlays` uses `setBehavior(living, BLINK, 1L, 0)`. The expiry loop's tick count is Networking 2's.

## 3. Sync eligibility checked in two places
No change by this pass. The concurrent Efficiency 1 pass already put 26.1.1's code in place: `EyeConfigSyncPacket.refusal()` measures the cached body; EyeConfigReloadListener and `PickerExportService.syncRefusal` use `packetFor(...).refusal()`; `wireSize` is removed; `configSyncPacketRoundTrips` asserts `refusal() == null`. No `wireSize` or `writeBody` remains.

## 6. Picker key list written several times
- PickerInput: `java.util.List` import and `KEYS = List.of(TOGGLE, LOCK, PART_PREV, PART_NEXT)`; `consumePendingKeys` loops over it. `handle` is private with the corrected comment and keeps 26.2's `Minecraft.getInstance().gui.screen()`.
- Loaders: Fabric `PickerInput.KEYS.forEach(KeyMappingHelper::registerKeyMapping)`; Forge and NeoForge `PickerInput.KEYS.forEach(event::register)`. Categories are still registered first.

## 7. Test-only surface in shipped classes
- Removed from shipped code: public `PresentationEyes(UUID)`; `BehaviorSettings.DEFAULTS` (`NONE` reads `ServerConfig` defaults directly); `switchedOn`'s argument; `ServerConfig.defaultBehaviors()` (`parseBehaviorPool` stays).
- Gametests: 13 references in six files use `TestBehaviorSettings.DEFAULTS` (BehaviorScheduler 1, BoatEye 4, FramedEye 1, GuiItemEye 3, HandItemEye 3, HeldEye 1). FramedEye (11 places) and HandItemEye (2) use `PresentationEyes.create(X, new ItemStack(ModContent.GOOGLY_EYE.get()))`.

## 8. Leftovers inside GooglyTracker
`motionX/Y/Z` are locals in `update`; `EyeInfo.update` is private.

## 9. Single-use wrappers and unneeded public surface
- LookTarget: `livingHitInCrosshair` inlined into `livingInCrosshair`; the file now equals 26.1.1's.
- EyeItemService: `playShearSound` inlined into `shearEyes`.
- HeadInfo: `getEyeScale` removed; GooglyEyeRenderer reads `helper.placementAt(head, eye).eyeScale()`.
- EyeState: `overridesTagOrNull` inlined into `readProperties`. The test-local `hasOverrideCompound(Cow)` was added to EyeStateOverrideGameTestsLogic, then Test-completeness 6 replaced that file's tests; neither name is referenced now.
- ClientEyeRuntime: `clearAnger` inlined into `onLevelChanged`.
- Logout: `PickerFreezeService.onPlayerLoggedOut` removed and the class javadoc links `#unfreeze` (file equals 26.1.1's); `ServerServices.onPlayerLeft` calls `PickerFreezeService.unfreeze(player.level().getServer(), player.getUUID())`; PickerState's javadoc link points to `#unfreeze`.
- Already in place, not edited here: `SomeGooglyCommon.init` removal and the three entry points (taken by Loader-divergence 4); `holdsGooglyEye`, removed when Organization 1 deleted HeldEye.

## 10
No change.

## Docs
No orientation, platform-notes, changelog or docs Markdown names a removed member. The generated `docs/javadoc` HTML still lists them; it is regenerated output and was not edited.

## Recalled API facts
`KeyMappingHelper::registerKeyMapping` and Forge/NeoForge `event::register` work as `Consumer<KeyMapping>`; tests use the `@Nullable` result of `PresentationEyes.create` directly, as 26.1.1 does.

## In-game checks
Mob expressions keep time (look away and back; approach mid-expression from a second client). Picker keys work on all loaders and do nothing while chat is open. Logging out with a frozen mob restores its AI. Shearing eyes plays the shear sound. Eyes with zero scale stay hidden.

## GameTests
None added or removed on any loader.

## Clashes
- Shared with concurrent passes: GooglyTracker, ClientEyeRuntime and BehaviorSchedulerGameTestsLogic (Networking 2); HeldEyeGameTestsLogic (Organization 1); EyeStateOverrideGameTestsLogic (Test-completeness 6).
- BehaviorSchedulerGameTestsLogic's hunks went in through one scripted read-modify-write rather than Edit, so a concurrent edit landing in that instant could have been lost; checked in the parent's reconciliation.
