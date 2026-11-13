# Report: Dead-code (26.1.2)

Applied 2026-11-12, unbuilt (nothing compiled, built, or tested). Each 26.1.1 file was diffed against 26.1.2's and only this topic's hunks taken, as 26.1.1's Dead-code report describes them; where 26.1.1's files already carry later topics (Efficiency 1, Networking 2, Organization, Loader-divergence 4), those spots were written by hand to 26.1.1's post-Dead-code form. Copied whole (entire diff this topic's, no 26.1.2 addition): test-source `TestBehaviorSettings.java` (new), `util/LookTarget.java`, `picker/PickerFreezeService.java`, `eye/HeadInfo.java`. No head code touched; `BlockEntity`/`Holder` signatures, Heads, Twilight, and GeckoLib code unchanged.

## 1. Mob expressions are timed in two places
- GooglyTracker: public `startBehavior` replaced by package-private `install(behavior, duration, seed, age)` (asleep → `active = null`, else replaces `active` and sets its age); `update` no longer calls `active.advance()`; class and `update` javadocs match 26.1.1.
- ClientEyeRuntime: `synchronizeBehavior(living, tracker)` without `ageOffset` (new received behavior installed at `elapsed`, unchanged one has its age set); `get`/`setBehavior` no longer pass `0`; the tracker sweep calls `synchronizeBehavior(living, tracker)`; `tickBehaviors` private.
- BehaviorSchedulerGameTestsLogic: `BehaviorInstance` import; helpers `play`, `receivedBehaviorPlays` (5-argument `setBehavior` with `EyeBehavior.BLINK.defaultDuration()`), `stepOnce`; four starts use `play`; two sleeping checks use `!receivedBehaviorPlays`; `stepGoat`, `stepTracker`, `irisScaleRange`, `meanPupilStep` use `stepOnce`; `receivedExpressionsOutliveRenderTrackers` calls `ClientEyeRuntime.tick(helper.getLevel(), true)` (expiry loop stays 7 ticks for duration 12).
- No deviation; `install` keeps `duration` for Networking 2.

## 3. Sync eligibility checked in two places
- EyeConfigSyncPacket: `EyeConfigLimits.@Nullable Issue refusal()` runs `validateSync`, then writes the packet to a scratch `Unpooled` buffer and compares `writerIndex()` with `MAX_PAYLOAD_BYTES` (failure → `somegoogly.validation.sync_size` `Issue` with size and limit); `wireSize`/`writeBody` folded into `write`; `List` and jspecify `Nullable` imports.
- EyeConfigReloadListener builds `new EyeConfigSyncPacket(selected, encoded)` and logs `refusal().diagnostic()`, still installing with `replaceIfChanged(selected, encoded)`. `PickerExportService.syncRefusal` uses `new EyeConfigSyncPacket(next, ServerEyeConfigs.encode(next)).refusal()`.
- SerializationGameTestsLogic `configSyncPacketRoundTrips` asserts `packet.refusal() == null`; javadoc updated.
- Deviation: written by hand because 26.1.1's packet now carries Efficiency 1's cached body; the result is 26.1.1's pre-Efficiency form.

## 6. Picker key list written several times
PickerInput `KEYS = List.of(TOGGLE, LOCK, PART_PREV, PART_NEXT)` drained by `consumePendingKeys`; `handle` private with the corrected comment; `CATEGORY` unchanged. Fabric `PickerInput.KEYS.forEach(KeyMappingHelper::registerKeyMapping)`; ForgeClientBootstrap and NeoForgeClient `PickerInput.KEYS.forEach(event::register)`, categories first. ForgeClientBootstrap's GeckoLib lines kept.

## 7. Test-only surface in shipped classes
`TestBehaviorSettings.java` copied from 26.1.1; six gametest files use `TestBehaviorSettings.DEFAULTS` (13 references: BehaviorScheduler, BoatEye, FramedEye, GuiItemEye, HandItemEye, HeldEye); `new PresentationEyes(X)` → `PresentationEyes.create(X, new ItemStack(ModContent.GOOGLY_EYE.get()))` in FramedEye (11) and HandItemEye (2). Removed public `PresentationEyes(UUID)`, `BehaviorSettings.DEFAULTS` (`NONE` reads the `ServerConfig` defaults), `switchedOn`'s argument, `ServerConfig.defaultBehaviors()`. No deviation.

## 8. Leftovers inside GooglyTracker
`motionX/Y/Z` locals in `update`; `EyeInfo.update` private. No deviation.

## 9. Single-use wrappers and unneeded public surface
- LookTarget (copied): `livingHitInCrosshair` inlined into `livingInCrosshair`.
- EyeItemService: `playShearSound` inlined into `shearEyes`.
- HeadInfo (copied): `getEyeScale` and an unused `EyeAppearance` import removed; GooglyEyeRenderer reads `helper.placementAt(head, eye).eyeScale()`.
- EyeState: `overridesTagOrNull` inlined into `readProperties`; EyeStateOverrideGameTestsLogic gets a local `hasOverrideCompound(Cow)` reading `EntityPersistentData.find(cow)` for `EyeState.EYE_OVERRIDES` (imports added).
- ClientEyeRuntime: `clearAnger` inlined into `onLevelChanged`.
- SomeGooglyCommon: `init` and its two imports removed; SomeGooglyFabric, SomeGooglyForge, SomeGooglyNeoForge call `ModContent.register(registrar)` (ModContent import), keeping registrar construction and bus attach.
- Logout: PickerFreezeService (copied) drops `onPlayerLoggedOut`, class-javadoc link to `#unfreeze`; `ServerServices.onPlayerLeft` calls `PickerFreezeService.unfreeze(player.level().getServer(), player.getUUID())`; PickerState's link points to `#unfreeze`.
- HeldEye: `holdsGooglyEye` private.
- `SkullEyeState.HEADS_MOBS` stays public (SkullEyeGameTestsLogic reads it). The entry points have no Heads, Twilight, or GeckoLib lines beside the `init` call, so nothing else there changed.

## 10. EyeColor NBT duplicates its codec
None.

## Docs
No orientation file, platform notes, docs, or mixin JSON names a removed or renamed member.

## Left for other topics / the parent
- Networking 2: drop the duration from `install`, `ReceivedBehavior`, the 5-argument `setBehavior`, and `receivedBehaviorPlays`.
- Efficiency 1: cached body, `ServerEyeConfigs.packetFor`, `replaceIfChanged(packet)`, decoded-bytes assertion.
- Loader-divergence 4: `ModContent.init()` / `RegistrationImpl.attach`.
- Organization: deletes HeldEye, GuiItemEyes, HandItemEyes (HeldEye still uses `BehaviorInstance.advance()` until then).
- Test-completeness 6: removes `hasOverrideCompound`.
- Others: `SomeGooglyCommon.id`; EyeItemService's `ServerConfig.roll`.
- Javadocs: a duplicated javadoc above `receivedExpressionsOutliveRenderTrackers` (pre-existing).

## Recalled API facts and checks only a build or in-game test can settle
- `KeyMappingHelper::registerKeyMapping` and `event::register` work as `Consumer<KeyMapping>` on each loader; `CompoundTag.contains(String)` exists; after writing to a fresh buffer `FriendlyByteBuf.writerIndex()` equals the payload length; tests can assign the `@Nullable` result of `PresentationEyes.create` directly; `ClientEyeRuntime.tick(ServerLevel, true)` runs inside a gametest (as in 26.1.1).

## In-game checks
- Mob expressions keep time: look away and back; approach mid-expression from a second client.
- Picker keys work on all loaders, and not while chat is open.
- An oversized eye set is refused on reload and on picker export.
- Logging out with a frozen mob restores its AI.
- Shearing eyes plays the shear sound.

## GameTests
None added or removed.
