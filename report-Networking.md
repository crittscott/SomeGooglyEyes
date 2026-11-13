# Report: Networking (26.3)

Applied 2026-11-12 from 26.2's post-fix Networking hunks (26.2/report-Networking.md exists). Only this topic's hunks were taken; Organization 1's `Target` (present in 26.2) was left out, and 26.3's API differences were kept. Nothing was compiled, built, or tested. No file was copied whole. Fix lines in review-Networking.md filled in.

## 1, 3
No change.

## 2. The trigger packet carries a duration that shipped code never varies
- EyeBehaviorTriggerPacket: record `(entityId, behavior, seed, elapsed)`; `duration`, `MAX_DURATION_TICKS`, `validTiming` removed; decode throws "Invalid eye behavior timing" unless 0 <= elapsed <= `behavior.defaultDuration()`; write drops the duration varint; javadoc says every expression runs for `defaultDuration()` (entity-only wording until Organization 1 adds `Target`).
- NetworkProtocol: `NETWORK_VERSION = "19"`. CHANGELOG-26.2-26.3.md:16 says "protocol 19".
- ServerBehaviorScheduler: `MobState.activeDuration` removed; `start(mob, state, behavior, seed)` sets `busyUntil = now + behavior.defaultDuration()`; `trigger(mob, behavior, seed)` replaces the duration form; heal-swirl, react, ambient callers and `catchUpPacket` updated. 26.3 keeps the scheduler's own `now` clock (Reinvention 4).
- GooglyServerCommands: `trigger(target, behavior, target.getRandom().nextLong())`.
- ClientEyeRuntime: `ReceivedBehavior(behavior, seed, startedAt)`; `setBehavior(living, behavior, seed, elapsed)`; both expiry checks use `behavior().defaultDuration()`.
- ClientNetworkHandler passes no duration.
- GooglyTracker: `install(behavior, seed, age)` passes `behavior.defaultDuration()` to `BehaviorInstance.start`; javadoc reworded.
- EyeBehavior: `defaultDuration()` javadoc "Length in ticks of one play of this expression, whether ambient or triggered; no play sets its own."
- SerializationGameTestsLogic: `behaviorTriggerPacketRoundTrips` round-trips every behavior at elapsed 0, half, and full, checking behavior, elapsed, and seed; rejects -1 and full+1 through a new `rejectsTrigger` helper; unknown-behavior case kept; javadoc updated. Target cases are Organization 1's.
- BehaviorSchedulerGameTestsLogic: all 29 `trigger` calls lose their duration; the five blink waits and the full-health heal test's wait are `advance(EyeBehavior.BLINK.defaultDuration())`; the stare test waits `STARE.defaultDuration() - 1`, checks a trigger on the last active tick is dropped, waits 1 more, then checks a new trigger starts; `receivedExpressionsOutliveRenderTrackers` uses `setBehavior` without 12/8 durations and its expiry loop runs `SWIRL.defaultDuration() - 5`; `receivedBehaviorPlays` passes no duration. Synchronous `advance(n)` waits kept (26.2 has Reinvention 4's `after(...)`); 26.3's `Enderman` and boolean `startSleeping` kept.
- BehaviorDeterminismGameTestsLogic javadoc: "(id, seed, elapsed ticks)".
- orientation-code.md: "Payload types own stable IDs; triggers carry behavior, seed, and age. `NetworkProtocol` declares version 19"; heads/frames sentence shortened to "Placed heads and frames recheck eye data at extraction." Body back under 12,000.

## 4. Picker export checks the age after decoding
- PickerExportPacket: static imports `AGE_ADULT`, `AGE_BABY`; decoder throws `DecoderException("Invalid picker export age")` unless adult or baby; javadoc "semantic config validation", second paragraph as 26.2 ("10-second cooldown" wording left for Javadocs).
- PickerExportService: age check and `AGE_BABY` import removed; `AGE_ADULT` still used; `export_rejected_malformed_payload` still used by the codec-parse refusal.
- SerializationGameTestsLogic `pickerExportPacketRoundTrips` round-trips both ages and rejects "", "any", "Adult", "unknown" on decode; static import `AGE_BABY`.
- PickerExportGameTestsLogic unchanged (every `export` passes `AGE_ADULT`).

## Left for other topics
- Organization 1 adds `Target` to the packet, the trigger round-trip test (target loop, held-eye and bad-target cases), `catchUpPacket`, and the `start` packet in ServerBehaviorScheduler.
- Reinvention 4: the server clock and `after(...)` waits.

## First build settles
No caller still passes a duration (a grep of all four source trees found none); the stare test's last-tick check holds with 26.3's `advance` clock (`busyUntil = now + 50` retires once `now` reaches it).

## In-game checks
- Expressions play and end at their usual lengths.
- A second client that starts tracking a mob mid-expression joins in sync.
- `/sg admin behavior <id>` plays, and is dropped while the mob is busy.
- `/sg export` works for adult and baby.
- Every loader refuses a version-18 client.

## GameTests
None added or removed (+0 each); two round-trip tests extended.
