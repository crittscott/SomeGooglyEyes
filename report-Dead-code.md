# Report: Dead-code (26.3)

Applied 2026-11-12 to 26.3. Nothing was compiled, built, or tested. Fix lines in review-Dead-code.md filled in. 26.2 had written report-Dead-code.md, so its post-fix code was the reference. Where 26.2's hunks for a finding also carry another topic's changes that 26.3 has not applied yet (Networking 2, Efficiency 1), 26.1.1's Dead-code-only shape was used instead, as the review describes.

New file: `common/src/gametest/.../gametest/TestBehaviorSettings.java`, copied unchanged from 26.1.1 (identical to 26.2's).

## 1. Mob expressions are timed in two places
Reference: 26.1.1's Dead-code step (26.2's code also includes Networking 2's duration removal, not taken).
- GooglyTracker: public `startBehavior` replaced by package-private `install(EyeBehavior behavior, int duration, long seed, int age)`; asleep sets `active = null`, otherwise replaces `active` and sets its age. `update` no longer calls `active.advance()`. Class javadoc as 26.2/26.1.1.
- ClientEyeRuntime: `synchronizeBehavior(living, tracker)` no longer takes `ageOffset`; a newly received behavior is installed at `elapsed` with its received duration, an unchanged one has its age set. The two `get`/`setBehavior` calls drop `0`; the tracker sweep drops `-1` and passes the map key `living`. `tickBehaviors` is private.
- BehaviorSchedulerGameTestsLogic: new `BehaviorInstance` import and helpers `play`, `receivedBehaviorPlays`, `stepOnce` (`receivedBehaviorPlays` calls the 5-argument `setBehavior(living, BLINK, BLINK.defaultDuration(), 1L, 0)`); the four `startBehavior` asserts use `play`, the two sleeping checks `!receivedBehaviorPlays`; `stepGoat`, `stepTracker`, `irisScaleRange`, `meanPupilStep` use `stepOnce`; `receivedExpressionsOutliveRenderTrackers` replaces three `tickBehaviors()` calls with `ClientEyeRuntime.tick(helper.getLevel(), true)` (26.3's two-argument `tick`; 26.2 and 26.1.1 have a third `activeView` argument). The expiry loop stays at 7 ticks for the explicit duration 12.
- Left for Networking 2: removing `duration` from `install`, `ReceivedBehavior`, `setBehavior`, and the test calls.

## 3. Sync eligibility checked in two places
Reference: 26.1.1's Dead-code shape, without Efficiency 1 (26.2's cached `body` and `ServerEyeConfigs.packetFor` not taken).
- EyeConfigSyncPacket: `refusal()` runs `EyeConfigLimits.validateSync(configs)`, then writes the packet to a scratch `Unpooled` buffer; over `MAX_PAYLOAD_BYTES` it returns `Issue("somegoogly.validation.sync_size", List.of(size, MAX_PAYLOAD_BYTES), diagnostic)`. `wireSize` and `writeBody` folded into `write`. New imports `java.util.List` and jspecify `@Nullable`.
- EyeConfigReloadListener: encodes, then logs `new EyeConfigSyncPacket(selected, encoded).refusal().diagnostic()` when refused; still installs with `replaceIfChanged(selected, encoded)`. Validation now runs after encoding; same result.
- PickerExportService.syncRefusal: `new EyeConfigSyncPacket(next, ServerEyeConfigs.encode(next)).refusal()`, translated with `refusal.message()`.
- SerializationGameTestsLogic.configSyncPacketRoundTrips: the `wireSize` assertion becomes `packet.refusal() == null`, javadoc updated; `bytes` still used by `roundTrip`.

## 6. Picker key list written several times
Reference: 26.2.
- PickerInput: `java.util.List` import, `KEYS = List.of(TOGGLE, LOCK, PART_PREV, PART_NEXT)`; `consumePendingKeys` loops over it; `handle` private with the corrected queued-press comment. 26.3's `InputConstants.KEY_*` defaults and `CATEGORY` construction kept.
- Fabric: `PickerInput.KEYS.forEach(KeyMappingHelper::registerKeyMapping)`; Forge and NeoForge: `PickerInput.KEYS.forEach(event::register)`. Each category still registered first.

## 7. Test-only surface in shipped classes
Reference: 26.2/26.1.1.
- Removed: public `PresentationEyes(UUID)`; `BehaviorSettings.DEFAULTS` (`NONE` reads the `ServerConfig` defaults directly); `switchedOn`'s argument (always reads current values); `ServerConfig.defaultBehaviors()` (`parseBehaviorPool` stays).
- Gametests: 13 references in six files use `TestBehaviorSettings.DEFAULTS` (BehaviorScheduler 1, BoatEye 4, FramedEye 1, GuiItemEye 3, HandItemEye 3, HeldEye 1). FramedEye (11 places) and HandItemEye (2) use `PresentationEyes.create(X, new ItemStack(ModContent.GOOGLY_EYE.get()))`.

## 8. Leftovers inside GooglyTracker
`motionX/Y/Z` are locals in `update`; `EyeInfo.update` is private. No outside callers.

## 9. Single-use wrappers and unneeded public surface
Reference: 26.2's hunks, except where noted.
- LookTarget: `livingHitInCrosshair` inlined into `livingInCrosshair`; the file now equals 26.2's.
- EyeItemService: `playShearSound` inlined into `shearEyes`.
- HeadInfo: `getEyeScale` removed; GooglyEyeRenderer reads `helper.placementAt(head, eye).eyeScale()`. HeadInfo's `EyeAppearance` import left for the package-move topic.
- EyeState: `overridesTagOrNull` inlined into `readProperties`. EyeStateOverrideGameTestsLogic gets a private `hasOverrideCompound(Cow)` reading `EntityPersistentData.find(cow)` and checking `getCompound(EyeState.EYE_OVERRIDES).isPresent()` (written here: 26.2 and 26.1.1 later replaced those tests in Test-completeness 6); imports `EntityPersistentData`, `CompoundTag`.
- ClientEyeRuntime: `clearAnger` inlined into `onLevelChanged` as `ANGRY.clear()`.
- SomeGooglyCommon: `init` and its two imports removed; the Fabric, Forge, and NeoForge entry points import `ModContent` and call `ModContent.register(registrar)` (26.1.1's Dead-code step; Loader-divergence 4 later changes it to `ModContent.init()`).
- Logout: `PickerFreezeService.onPlayerLoggedOut` removed, class javadoc links `#unfreeze` (file now equals 26.2's); `ServerServices.onPlayerLeft` calls `PickerFreezeService.unfreeze(player.level().getServer(), player.getUUID())`; PickerState's javadoc links `PickerFreezeService#unfreeze`.
- HeldEye: `holdsGooglyEye` is private (Organization 1 deletes HeldEye later).

## 10
No change.

## Docs
No orientation file, platform-notes, docs Markdown, changelog, or build-env copy names a removed member. Generated `docs/javadoc` HTML not edited.

## Recalled API facts
`KeyMappingHelper::registerKeyMapping` and Forge/NeoForge `event::register` work as `Consumer<KeyMapping>`; `CompoundTag.getCompound` returns an `Optional` in 26.3 (as EyeState's own use shows); tests use the `@Nullable` result of `PresentationEyes.create` directly, as 26.1.1 does.

## In-game checks
- Mob expressions keep time: look away and back, and approach mid-expression from a second client.
- Picker keys work on all loaders and do nothing while chat is open.
- Logging out with a frozen mob restores its AI.
- Shearing eyes plays the shear sound.
- Eyes with zero scale stay hidden.
- A datapack reload over the sync limit is still refused with a log line.

## GameTests
None added or removed on any loader.

## Clashes and follow-up
- Networking 2 must remove `duration` from `GooglyTracker.install`, `ClientEyeRuntime.ReceivedBehavior`, `setBehavior`, and the BehaviorSchedulerGameTestsLogic calls (`receivedBehaviorPlays` and the explicit 12/8 durations).
- Efficiency 1 must replace `refusal()`'s scratch-buffer measurement with the cached body and `ServerEyeConfigs.packetFor`, and change the reload listener and PickerExportService to match.
- Loader-divergence 4 (entry points), Organization 1 (HeldEye), and Test-completeness 6 (EyeStateOverrideGameTestsLogic) touch files changed here.
- BehaviorSchedulerGameTestsLogic's duplicated javadoc above `receivedExpressionsOutliveRenderTrackers` is left for Javadocs.
