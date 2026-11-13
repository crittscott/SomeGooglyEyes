# Report: Dead-code (26.1.1)

Applied 2026-11-11 by taking 26.1's Dead-code hunks. Nothing was compiled, built, or tested. Fix lines and intro in review-Dead-code.md filled in; head-loot follow-up marked done by Multiloader-organization 1.

Copied whole from 26.1: new test-source TestBehaviorSettings.java; util/LookTarget.java and picker/PickerFreezeService.java (each's only remaining diff was this topic's; both now identical to 26.1's).

## 1. Mob expressions are timed in two places
- GooglyTracker: public `startBehavior` replaced by package-private `install(behavior, duration, seed, age)` (asleep → `active = null`, else replace `active` and set its age); `update` no longer calls `active.advance()`; class javadoc as 26.1.
- ClientEyeRuntime: `synchronizeBehavior(living, tracker)` without `ageOffset` (new behavior installed at `elapsed`, unchanged one has its age set); `setBehavior` calls and tracker sweep no longer pass `0`/`-1`; `tickBehaviors` private.
- BehaviorSchedulerGameTestsLogic: helpers `play`, `receivedBehaviorPlays`, `stepOnce` (+ `BehaviorInstance` import); four starts use `play`; sleeping-villager checks use `!receivedBehaviorPlays`; `stepGoat`, `stepTracker`, `irisScaleRange`, `meanPupilStep` use `stepOnce`; `receivedExpressionsOutliveRenderTrackers` calls `ClientEyeRuntime.tick(helper.getLevel(), true)`.
- Deviations (as in 26.1's own Dead-code pass): `install` keeps `duration` and `synchronizeBehavior` passes `received.duration()`; `receivedBehaviorPlays` uses the 5-argument `setBehavior` with `BLINK.defaultDuration()`; expiry loop stays 7 ticks for duration 12. GooglyTracker now differs from 26.1 only by Networking 2's `install` duration.

## 3. Sync eligibility checked in two places (without Efficiency 1)
- EyeConfigSyncPacket: `EyeConfigLimits.@Nullable Issue refusal()` (runs `validateSync`, writes into a scratch `Unpooled` buffer, checks `writerIndex()` against `MAX_PAYLOAD_BYTES`, size failure → `somegoogly.validation.sync_size`); `wireSize`/`writeBody` folded into `write`; `List` and jspecify `Nullable` imports.
- EyeConfigReloadListener builds `new EyeConfigSyncPacket(selected, encoded)` and logs `refusal().diagnostic()`; still installs with `replaceIfChanged(selected, encoded)`.
- PickerExportService `syncRefusal` uses `new EyeConfigSyncPacket(next, ServerEyeConfigs.encode(next)).refusal()`.
- SerializationGameTestsLogic `configSyncPacketRoundTrips` asserts `packet.refusal() == null`.

## 6. Picker key list written several times
PickerInput: `KEYS = List.of(TOGGLE, LOCK, PART_PREV, PART_NEXT)`, `consumePendingKeys` loops over it; `handle` private with corrected comment; `CATEGORY` still uses `Identifier.fromNamespaceAndPath` (`SomeGooglyCommon.id` is another topic's). FabricClientEvents `PickerInput.KEYS.forEach(KeyMappingHelper::registerKeyMapping)`; ForgeClientBootstrap and NeoForgeClient `PickerInput.KEYS.forEach(event::register)`; categories still registered first.

## 7. Test-only surface in shipped classes
Six gametest files use `TestBehaviorSettings.DEFAULTS` (BehaviorScheduler, BoatEye, FramedEye, GuiItemEye, HandItemEye, HeldEye). `new PresentationEyes(X)` → `PresentationEyes.create(X, new ItemStack(ModContent.GOOGLY_EYE.get()))` in FramedEye (11) and HandItemEye (2). Removed: public `PresentationEyes(UUID)`, `BehaviorSettings.DEFAULTS` (`NONE` reads `ServerConfig` defaults directly), `switchedOn`'s argument, `ServerConfig.defaultBehaviors()`.

## 8. Leftovers inside GooglyTracker
`motionX/Y/Z` locals in `update`; `EyeInfo.update` private.

## 9. Single-use wrappers and unneeded public surface
- LookTarget: `livingHitInCrosshair` inlined into `livingInCrosshair`.
- EyeItemService: `playShearSound` inlined into `shearEyes` (`ServerConfig.roll` hunk left for its topic).
- HeadInfo: `getEyeScale` removed; GooglyEyeRenderer reads `helper.placementAt(head, eye).eyeScale()`.
- EyeState: `overridesTagOrNull` inlined into `readProperties`; EyeStateOverrideGameTestsLogic gets a test-local `hasOverrideCompound(Cow)` (checks `EntityPersistentData.find(cow)` for `EYE_OVERRIDES`), written from 26.1's report since 26.1's file lost it to Test-completeness 6.
- ClientEyeRuntime: `clearAnger` inlined into `onLevelChanged`.
- SomeGooglyCommon: `init` and two imports removed; SomeGooglyFabric/Forge/NeoForge call `ModContent.register(registrar)`.
- Logout: `PickerFreezeService.onPlayerLoggedOut` removed; `ServerServices.onPlayerLeft` calls `PickerFreezeService.unfreeze(player.level().getServer(), player.getUUID())`; PickerState link points to `#unfreeze`.
- HeldEye: `holdsGooglyEye` private.

## 10
No change.

## Docs
No orientation, platform-notes, or docs text named the removed members; a grep found no remaining references.

## Left to later topics
Networking 2 (drop `install`'s and `ReceivedBehavior`'s duration and the 5-argument `setBehavior`); Efficiency 1 (cached body, `ServerEyeConfigs.packetFor`, `replaceIfChanged(packet)`, decoded-bytes assertion); Loader-divergence 4 (`ModContent.init()` / `RegistrationImpl.attach`); Organization 1 (delete HeldEye); Test-completeness 6 (removes `hasOverrideCompound`); `SomeGooglyCommon.id` uses, EyeItemService's `ServerConfig.roll`, BehaviorScheduler's runAfterDelay / `trigger` changes.

## First build settles
`KeyMappingHelper::registerKeyMapping` and `event::register` as `Consumer<KeyMapping>`; `CompoundTag.contains(String)`; `writerIndex()` equals body length after writing to a fresh buffer; `@Nullable` `PresentationEyes.create` results assigned directly in tests.

## In-game checks
Mob expressions keep time (look away and back; approach mid-expression from a second client). Picker keys work on all loaders, not while chat is open. An oversized eye set is refused on reload and export. Logging out with a frozen mob restores its AI. Shearing eyes plays the shear sound.

## GameTests
None added or removed.
