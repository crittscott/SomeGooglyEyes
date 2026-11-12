# Report: Dead-code (26.1)

Applied 2026-11-11 by hand-porting 1.21.11's changes. Nothing was compiled, built, or tested; everything here comes from reading the source. Each file was diffed against 1.21.11 and only this topic's hunks taken. Copied whole: test-source `TestBehaviorSettings.java` (new) and `util/LookTarget.java` (differed from 1.21.11 only by this topic's hunk). Fix lines in review-Dead-code.md are filled in.

## 1. Mob expressions are timed in two places
- client/GooglyTracker.java: public `startBehavior` replaced by package-private `install(behavior, duration, seed, age)` (asleep → `active = null`; otherwise replaces `active` and sets its age); `update` no longer calls `active.advance()`; class javadoc as 1.21.11.
- client/ClientEyeRuntime.java: `synchronizeBehavior(living, tracker)` has no `ageOffset`; a new received behavior installs at `elapsed`, an unchanged one has its age set to `elapsed`; the tick no longer passes `-1`; `tickBehaviors` private.
- BehaviorSchedulerGameTestsLogic: helpers `play`, `receivedBehaviorPlays`, `stepOnce`; four `startBehavior` starts → `play`; two sleeping-villager checks → `!receivedBehaviorPlays`; `stepGoat`, `stepTracker`, `irisScaleRange`, `meanPupilStep` use `stepOnce`; `receivedExpressionsOutliveRenderTrackers` calls `ClientEyeRuntime.tick(helper.getLevel(), true)`.
- Deviations (as in 1.21.11's own application): `install` keeps `duration` and `synchronizeBehavior` passes `received.duration()` (Networking 2 removes it); `receivedBehaviorPlays` passes `EyeBehavior.BLINK.defaultDuration()` to the 5-argument `setBehavior`; `tick` has two arguments in 26.1; the expiry loop stays at 7 ticks for explicit duration 12. `BehaviorInstance.advance()` stays (HeldEye uses it).

## 3. Sync eligibility checked in two places
- network/EyeConfigSyncPacket.java: new `EyeConfigLimits.@Nullable Issue refusal()` runs `validateSync`, then writes the packet into a scratch buffer and checks `MAX_PAYLOAD_BYTES` (size failure → `somegoogly.validation.sync_size` `Issue`); `wireSize` deleted; `writeBody` folded into `write`.
- config/EyeConfigReloadListener.java: builds `new EyeConfigSyncPacket(selected, encoded)` and logs `refusal().diagnostic()` ("exceeds the limits clients accept; keeping the previous configs").
- picker/PickerExportService.java: `syncRefusal` uses `new EyeConfigSyncPacket(next, ServerEyeConfigs.encode(next)).refusal()`.
- SerializationGameTestsLogic: `configSyncPacketRoundTrips` asserts `packet.refusal() == null`; javadoc updated.
- Left for Efficiency 1: cached body, `ServerEyeConfigs.packetFor` and the installed packet, decoded-bytes assertion; then `refusal()` should use the cached body and both callers `packetFor`.

## 6. Picker key list written several times
- client/picker/PickerInput.java: `KEYS = List.of(TOGGLE, LOCK, PART_PREV, PART_NEXT)`; `consumePendingKeys` loops it; `handle` private with 1.21.11's comment; `CATEGORY` (still `Identifier.fromNamespaceAndPath`) and `key` kept.
- FabricClientEvents: `PickerInput.KEYS.forEach(KeyMappingHelper::registerKeyMapping)`; ForgeClientBootstrap and NeoForgeClient: `PickerInput.KEYS.forEach(event::register)`. Each registers the category first.

## 7. Test-only surface in shipped classes
- New test-source `TestBehaviorSettings.java` from 1.21.11 (members it reads checked in 26.1).
- `BehaviorSettings.DEFAULTS` → `TestBehaviorSettings.DEFAULTS` in BehaviorScheduler, BoatEye, FramedEye, GuiItemEye, HandItemEye, HeldEye logic files; `new PresentationEyes(X)` → `PresentationEyes.create(X, new ItemStack(ModContent.GOOGLY_EYE.get()))` in FramedEye (11) and HandItemEye (2).
- Removed: public `PresentationEyes(UUID)`; `BehaviorSettings.DEFAULTS` (`NONE` reads `ServerConfig` defaults directly); `switchedOn`'s argument; `ServerConfig.defaultBehaviors()`. `tickBehaviors` private.

## 8. Leftovers inside GooglyTracker
`motionX/Y/Z` are locals in `update`; `EyeInfo.update` private.

## 9. Single-use wrappers and unneeded public surface
- LookTarget: `livingHitInCrosshair` inlined into `livingInCrosshair` (`@Nullable` kept).
- EyeItemService: `playShearSound` inlined into `shearEyes`.
- HeadInfo: `getEyeScale` removed; GooglyEyeRenderer reads `helper.placementAt(head, eye).eyeScale()`.
- EyeState: `overridesTagOrNull` inlined into `readProperties`; EyeStateOverrideGameTestsLogic gains test-local `hasOverrideCompound(Cow)` reading `EntityPersistentData.find(cow)` for `EyeState.EYE_OVERRIDES` (Test-completeness 6 later rewrites these tests).
- ClientEyeRuntime: `clearAnger` inlined into `onLevelChanged`.
- SomeGooglyCommon: `init` removed; SomeGooglyFabric, SomeGooglyForge, SomeGooglyNeoForge call `ModContent.register(registrar)`.
- PickerFreezeService: `onPlayerLoggedOut` removed; `ServerServices.onPlayerLeft` calls `PickerFreezeService.unfreeze(player.level().getServer(), player.getUUID())`; PickerFreezeService and PickerState javadoc links point to `#unfreeze`.
- HeldEye: `holdsGooglyEye` private. No `HEADS_MOBS` work (no Heads in 26.1).

## 10
No change.

## Head-loot follow-up
Done by Multiloader-organization 1; marked in the review file.

## Docs
No orientation or platform-notes text named the removed members.

## Recalled API facts (unverified)
`KeyMappingHelper::registerKeyMapping` and Forge/NeoForge `RegisterKeyMappingsEvent::register` work as `Consumer<KeyMapping>`; `CompoundTag.contains(String)` exists in 26.1; `FriendlyByteBuf.writerIndex()` after writing into a fresh buffer equals the body length.

## In-game checks
Mob expressions keep time (look away and back; approach mid-expression from a second client); picker keys work on all loaders and not while chat is open; an oversized eye set is still refused on reload and export; logging out with a frozen mob restores its AI; shearing eyes still plays the shear sound.

## Left to other topics
Networking 2 (drop `install`'s duration); Efficiency 1 (cached body, `packetFor`); Loader-divergence 4 (entry points → `ModContent.init()`); Organization 1 (delete HeldEye); Test-completeness 6 (removes `hasOverrideCompound`).

## Clash points
GooglyTracker, ClientEyeRuntime, EyeConfigSyncPacket, EyeConfigReloadListener, PickerExportService, PickerInput and the three loader client classes, BehaviorSettings, ServerConfig, PresentationEyes, LookTarget, EyeItemService, HeadInfo, GooglyEyeRenderer, EyeState, SomeGooglyCommon and the three entry points, PickerFreezeService, ServerServices, PickerState, HeldEye; BehaviorScheduler, Serialization, EyeStateOverride logic and the six TestBehaviorSettings users.

## GameTests
None added or removed.

## Files modified
common main: client/GooglyTracker, client/ClientEyeRuntime, client/PresentationEyes, client/HeldEye, client/picker/PickerInput, client/picker/PickerState, client/render/GooglyEyeRenderer, network/EyeConfigSyncPacket, config/EyeConfigReloadListener, config/ServerConfig, picker/PickerExportService, picker/PickerFreezeService, eye/behavior/BehaviorSettings, eye/HeadInfo, eye/state/EyeState, item/EyeItemService, util/LookTarget, server/ServerServices, SomeGooglyCommon. Gametest: BehaviorScheduler, BoatEye, FramedEye, GuiItemEye, HandItemEye, HeldEye, Serialization, EyeStateOverride logic files; TestBehaviorSettings (new). Loaders: FabricClientEvents, SomeGooglyFabric, ForgeClientBootstrap, SomeGooglyForge, NeoForgeClient, SomeGooglyNeoForge. Docs: review-Dead-code.md.
