# Report: Networking (26.1.1)

Applied 2026-11-11 by hand-porting 26.1's Networking hunks with the Edit tool. Nothing was compiled, built, or tested. No file copied whole. Organization 1's `Target` and Reinvention 4's server clock and `after(...)` waiting left out on purpose. Fix lines and intro in review-Networking.md filled in.

## 1, 3
No change.

## 2. The trigger packet carries a duration that shipped code never varies
- EyeBehaviorTriggerPacket is `(entityId, behavior, seed, elapsed)`; `duration`, `MAX_DURATION_TICKS`, `validTiming` gone; decode rejects `elapsed` outside `0..behavior.defaultDuration()` ("Invalid eye behavior timing"); javadoc says every expression runs for `defaultDuration()` (no hand-target wording).
- NetworkProtocol `NETWORK_VERSION = "19"`; CHANGELOG-26.1-26.1.1.md:16 "protocol 19".
- ServerBehaviorScheduler: `MobState.activeDuration` removed; `start(mob, state, behavior, seed)` sets `busyUntil = now + behavior.defaultDuration()`; `trigger(mob, behavior, seed)`; `catchUpPacket`, heal swirl, `react`, `rollAmbient` updated; `now` clock kept.
- GooglyServerCommands `trigger(target, behavior, target.getRandom().nextLong())`. ClientEyeRuntime: `ReceivedBehavior(behavior, seed, startedAt)`, `setBehavior(living, behavior, seed, elapsed)`; expiry uses `defaultDuration()`. ClientNetworkHandler passes no duration. GooglyTracker `install(behavior, seed, age)` passes `defaultDuration()` to `BehaviorInstance.start`.
- Tests: `behaviorTriggerPacketRoundTrips` round-trips every behavior at elapsed 0, half, full and rejects -1 and full+1 (new `rejectsTrigger`); unknown-behavior case kept. BehaviorSchedulerGameTestsLogic: 29 `trigger` calls lose duration; blink waits use `advance(BLINK.defaultDuration())`; `oneBehaviorAtATimeUntilItElapses` advances `STARE.defaultDuration() - 1`, checks a trigger is dropped, advances 1, checks a new one starts (26.1's check with `advance` for `after`); `receivedExpressionsOutliveRenderTrackers` expiry loop `SWIRL.defaultDuration() - 5`. BehaviorDeterminismGameTestsLogic javadoc "(id, seed, elapsed ticks)".
- orientation-code.md: "Payloads own stable IDs; triggers carry behavior, seed, and age. `NetworkProtocol` declares version 19"; placed-head/frame sentence shortened to "Placed heads and frames recheck eye data at extraction." Body 11,991 characters.

## 4. Picker export checks the age after decoding
Not listed as left in the hand-off, but all its hunks were missing, so applied here. PickerExportPacket decoder throws `DecoderException("Invalid picker export age")` unless adult or baby (`AGE_ADULT`/`AGE_BABY` static imports); javadoc "semantic config validation" and second paragraph as 26.1 ("10-second cooldown" left to Javadocs). PickerExportService age check and `AGE_BABY` import removed (`isExcluded` and javadoc hunk left). `pickerExportPacketRoundTrips` round-trips both ages and rejects `""`, `"any"`, `"Adult"`, `"unknown"`.

## Deviations from 26.1
No `Target`, held-eye decode checks, or hand-target tests (Organization 1); no `after(...)` or `clock(mob)` (Reinvention 4); other topics' hunks (imports, `ClientNetworking` package, `OngoingBehavior.ANGER.serverAngry`, `tick(..., null)`, duplicate javadoc removal, `isExcluded`) left.

## Left to other topics
Organization 1: `Target` between `entityId` and `behavior`, held-eye decode checks, target tests, "target (entity or hand)" in orientation-code.md (no further version bump; about 9 characters of headroom). Reinvention 4: server clock and `after(...)`.

## First build settles
All changed call sites compile (grep found no remaining 5-argument callers); scheduler tests assume `advance(D)` retires a behavior started D ticks earlier.

## In-game checks
Expressions play and end at usual lengths; a second client tracking a mob mid-expression joins in sync; `/sg admin behavior <id>` plays and is dropped while busy; `/sg export` works for adult and baby; every loader refuses a version-18 client.

## GameTests
None added or removed; two round-trip tests extended.
