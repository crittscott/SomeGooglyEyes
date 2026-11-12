# Report: Dead-code (1.21.11)

Applied 2026-11-11 by hand-porting 1.21.8's changes. Nothing was compiled, built, or tested; everything here comes from reading the source. Each file was diffed against 1.21.8 and only this topic's hunks taken; the only whole file copied is the new test-source `TestBehaviorSettings.java`.

## 1. Mob expressions are timed in two places
- client/GooglyTracker.java: public `startBehavior` replaced by package-private `install(behavior, duration, seed, age)` (asleep → `active = null`; otherwise replaces `active` and sets its age); `update` no longer calls `active.advance()`; class javadoc matches 1.21.8.
- client/ClientEyeRuntime.java: `synchronizeBehavior(living, tracker)` has no `ageOffset`; a new received behavior installs at `elapsed`, an unchanged one has its age set to `elapsed`; the tick no longer passes `-1`; `tickBehaviors` private.
- BehaviorSchedulerGameTestsLogic.java: helpers `play`, `receivedBehaviorPlays`, `stepOnce` (as 1.21.8); four `startBehavior` starts → `play`; two sleeping-villager checks → `!receivedBehaviorPlays`; `stepGoat`, `stepTracker`, `irisScaleRange`, `meanPupilStep` use `stepOnce`; `receivedExpressionsOutliveRenderTrackers` calls `ClientEyeRuntime.tick(helper.getLevel(), true)`.
- Deviations: `install` keeps `duration` and `synchronizeBehavior` passes `received.duration()` (1.21.11's `ReceivedBehavior` still carries it; Networking 2 removes it). `receivedBehaviorPlays` passes `EyeBehavior.BLINK.defaultDuration()` to the 5-argument `setBehavior`. `tick` has two arguments in 1.21.11 (1.21.8 has a third, `activeView`). The expiry loop stays at 7 ticks for 1.21.11's explicit duration 12. 1.21.11's determinant guard and `poseStale` javadoc kept. `BehaviorInstance.advance()` stays (HeldEye uses it).

## 3. Sync eligibility checked in two places
- network/EyeConfigSyncPacket.java: new `@Nullable refusal()` runs `validateSync`, then writes the packet to a scratch buffer and checks `MAX_PAYLOAD_BYTES` (size failure → `somegoogly.validation.sync_size` `Issue`); `wireSize` deleted.
- config/EyeConfigReloadListener.java: builds `new EyeConfigSyncPacket(selected, encoded)` and logs `refusal().diagnostic()` (1.21.8's message), replacing the separate checks.
- picker/PickerExportService.java: `syncRefusal` uses `new EyeConfigSyncPacket(next, ServerEyeConfigs.encode(next)).refusal()`.
- SerializationGameTestsLogic.java: `configSyncPacketRoundTrips` asserts `packet.refusal() == null`; javadoc updated.
- Left for Efficiency 1: the cached body, `ServerEyeConfigs.packetFor`/installed packet, the decoded-bytes assertion; `refusal()` should then use the cached body and both callers `packetFor`.

## 6. Picker key list written several times
- client/picker/PickerInput.java: `KEYS = List.of(TOGGLE, LOCK, PART_PREV, PART_NEXT)` (1.21.8's comment); `consumePendingKeys` loops it; `handle` private with 1.21.8's comment; 1.21.11's `CATEGORY` and `key` helper kept.
- FabricClientEvents `PickerInput.KEYS.forEach(KeyBindingHelper::registerKeyBinding)`; ForgeClientBootstrap and NeoForgeClient `PickerInput.KEYS.forEach(event::register)`; each still registers the category first.

## 7. Test-only surface in shipped classes
- New test source `TestBehaviorSettings.java` (copied from 1.21.8). `BehaviorSettings.DEFAULTS` → `TestBehaviorSettings.DEFAULTS` in BehaviorScheduler, BoatEye, FramedEye, GuiItemEye, HandItemEye, HeldEye logic files; `new PresentationEyes(X)` → `PresentationEyes.create(X, new ItemStack(ModContent.GOOGLY_EYE.get()))` in FramedEye (11) and HandItemEye (2).
- Removed: public `PresentationEyes(UUID)`; `BehaviorSettings.DEFAULTS` (`NONE` reads `ServerConfig` defaults directly); `switchedOn`'s argument; `ServerConfig.defaultBehaviors()`. `tickBehaviors` private (finding 1).

## 8. Leftovers inside GooglyTracker
With finding 1: `motionX/Y/Z` fields are locals in `update`; `EyeInfo.update` private. The orphan comment was already in place.

## 9. Single-use wrappers and unneeded public surface
- LookTarget: `livingHitInCrosshair` inlined into `livingInCrosshair` (jspecify `@Nullable` kept).
- EyeItemService: `playShearSound` inlined into `shearEyes`.
- HeadInfo: `getEyeScale` removed; `GooglyEyeRenderer.renderHead` reads `helper.placementAt(head, eye).eyeScale()`.
- EyeState: `overridesTagOrNull` inlined into `readProperties`. Deviation: EyeStateOverrideGameTestsLogic still used it (1.21.8's rewrite is Test-completeness 6), so a test-local `hasOverrideCompound(Cow)` checks `EntityPersistentData.find(cow)` for `EyeState.EYE_OVERRIDES`.
- ClientEyeRuntime: `clearAnger` inlined into `onLevelChanged`.
- SomeGooglyCommon: `init` removed with its imports; the three entry points call `ModContent.register(registrar)` (Loader-divergence 4 changes this to `ModContent.init()`).
- PickerFreezeService: `onPlayerLoggedOut` removed; `ServerServices.onPlayerLeft` calls `unfreeze(player.level().getServer(), player.getUUID())`; links in PickerFreezeService and PickerState point to `#unfreeze`.
- HeldEye: `holdsGooglyEye` private (Organization 1 deletes HeldEye).
- `SkullEyeState.HEADS_MOBS` stays public (a gametest reads it).

## 10
No change.

## Head-loot follow-up
Done by Multiloader-organization 1; noted in the review file.

## Docs
No orientation text needed changing (orientation-code.md still 11,997 characters).

## Recalled API facts (unverified)
`KeyBindingHelper::registerKeyBinding` and `event::register` work as `Consumer<KeyMapping>`; `CompoundTag.contains(String)` exists in 1.21.11; `FriendlyByteBuf.writerIndex()` after writing equals the body length `wireSize` measured.

## Left to other topics
Efficiency 1 (cached body etc.); Loader-divergence 4 (`ModContent.init()`); Organization 1 (delete HeldEye); Test-completeness 6 (removes `hasOverrideCompound`).

## Clash points
Production: GooglyTracker, ClientEyeRuntime, EyeConfigSyncPacket, EyeConfigReloadListener, PickerExportService, PickerInput and the three loader client classes, BehaviorSettings, ServerConfig, PresentationEyes, LookTarget, EyeItemService, HeadInfo, GooglyEyeRenderer, EyeState, SomeGooglyCommon and entry points, PickerFreezeService, ServerServices, PickerState, HeldEye. Tests: BehaviorSchedulerGameTestsLogic, SerializationGameTestsLogic, EyeStateOverrideGameTestsLogic, and the six files switched to TestBehaviorSettings.

## In-game checks
Mob expressions keep time and finish on schedule (look away and back; approach mid-expression from a second client); picker keys (K, V, `[`/`]`) work on all loaders and not while chat is open; an oversized eye set is still refused on reload and export; logging out with a frozen mob restores its AI; shearing eyes still plays the shear sound.

## Files modified
common main: client/GooglyTracker, client/ClientEyeRuntime, client/PresentationEyes, client/HeldEye, client/picker/PickerInput, client/picker/PickerState, client/render/GooglyEyeRenderer, network/EyeConfigSyncPacket, config/EyeConfigReloadListener, config/ServerConfig, picker/PickerExportService, picker/PickerFreezeService, eye/behavior/BehaviorSettings, eye/HeadInfo, eye/state/EyeState, item/EyeItemService, util/LookTarget, server/ServerServices, SomeGooglyCommon. Gametest: BehaviorScheduler, BoatEye, FramedEye, GuiItemEye, HandItemEye, HeldEye, Serialization, EyeStateOverride logic files; TestBehaviorSettings (new). Loaders: FabricClientEvents, SomeGooglyFabric, ForgeClientBootstrap, SomeGooglyForge, NeoForgeClient, SomeGooglyNeoForge. Docs: review-Dead-code.md.
