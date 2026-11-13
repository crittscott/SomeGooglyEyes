# Report: Networking (26.2)

Applied 2026-11-12 by taking 26.1.1's Networking hunks while other topics' agents edited 26.2 at the same time. Nothing was compiled, built, or tested. No file copied whole. Fix lines in review-Networking.md filled in.

## 1, 3
No change.

## 2. The trigger packet carries a duration that shipped code never varies
- EyeBehaviorTriggerPacket: `duration`, `MAX_DURATION_TICKS`, `validTiming` removed; decode throws "Invalid eye behavior timing" unless 0 <= elapsed <= behavior.defaultDuration(); write drops the duration varint; javadoc says every behavior runs for `defaultDuration()`. (Organization 1 then added `Target` here.)
- NetworkProtocol `NETWORK_VERSION = "19"`; CHANGELOG-26.1.2-26.2.md:17 "protocol 19".
- ServerBehaviorScheduler: `MobState.activeDuration` removed; `start(mob, state, behavior, seed)` sets `busyUntil = now + behavior.defaultDuration()`; `trigger(mob, behavior, seed)`; catch-up, heal swirl, react, rollAmbient callers updated.
- GooglyServerCommands `trigger(target, behavior, target.getRandom().nextLong())`. ClientEyeRuntime `ReceivedBehavior(behavior, seed, startedAt)`, `setBehavior(living, behavior, seed, elapsed)`, expiry on `behavior().defaultDuration()`. ClientNetworkHandler passes no duration. GooglyTracker `install(behavior, seed, age)` passes `defaultDuration()` to `BehaviorInstance.start`.
- EyeBehavior `defaultDuration()` javadoc reworded as 26.1.1 ("no play sets its own"); the old text described explicit durations (26.1.1 made this edit under Javadocs 8).
- Tests: `behaviorTriggerPacketRoundTrips` round-trips every behavior at elapsed 0/half/full (checks behavior, elapsed, seed) and rejects -1 and full+1 via new `rejectsTrigger`; unknown-behavior case kept. BehaviorSchedulerGameTestsLogic: all `trigger`/`setBehavior` calls lose duration; blink waits use `BLINK.defaultDuration()`; the stare test waits `STARE.defaultDuration() - 1`, checks a trigger is dropped, waits 1, checks a new one starts; received-expression expiry loop `SWIRL.defaultDuration() - 5`. BehaviorDeterminismGameTestsLogic javadoc "(id, seed, elapsed ticks)".
- orientation-code.md: "Payload types own stable IDs; triggers carry behavior, seed, and age. `NetworkProtocol` declares version 19"; heads/frames sentence shortened to "Placed heads and frames recheck eye data at extraction." (as 26.1.1). Body 11,973 characters at the time.

## 4. Picker export checks the age after decoding
- PickerExportPacket: `AGE_ADULT`/`AGE_BABY` static imports; decoder throws `DecoderException("Invalid picker export age")` unless adult or baby; javadoc "semantic config validation" and second paragraph as 26.1.1 ("10-second cooldown" left to Javadocs).
- PickerExportService: age check and `AGE_BABY` import removed (`AGE_ADULT` still used; `export_rejected_malformed_payload` still used by the codec-parse refusal).
- `pickerExportPacketRoundTrips` round-trips both ages and rejects "", "any", "Adult", "unknown"; `AGE_BABY` import added.

## Deviations from 26.1.1
None in substance. Scheduler-test waits were written with `advance(...)`; Reinvention 4's agent converted them to `after(...)` keeping the tick counts. Early edits to NetworkProtocol, PickerExportPacket, PickerExportService and ServerBehaviorScheduler were made with short exact-string replacements (each checked unique), not the Edit tool.

## Recalled API facts
None new.

## Clashes and other topics' hunks
- Organization 1 added `Target` to the packet while this ran. SerializationGameTestsLogic's trigger round-trip constructed the 4-argument packet; it needs `Target.ENTITY` and 26.1.1's target loop, held-eye and bad-target cases (Organization 1's hunks). Settled in the parent's reconciliation.
- GooglyServerCommands, ClientEyeRuntime, ClientNetworkHandler, ServerBehaviorScheduler and EyeBehavior were edited by other agents during this session; these hunks were applied to the current text.

## First build settles
That no caller still passes a duration (grep found none) and that test callers match the final `Target` constructor.

## In-game checks
Expressions play and end at their usual lengths; a second client tracking a mob mid-expression joins in sync; `/sg admin behavior <id>` plays and is dropped while busy; `/sg export` works for adult and baby; every loader refuses a version-18 client.

## GameTests
None added or removed on Fabric, Forge, or NeoForge (+0 each); two round-trip tests extended.
