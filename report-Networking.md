# Report: Networking (1.21.11)

Applied 2026-11-11 by hand-porting 1.21.8's changes. Nothing was compiled, built, or tested; everything here comes from reading the source. No whole files copied. Organization 1's `Target`/hand reactions and Reinvention 4's server clock were deliberately left to those topics.

## 1. Fabric uses the old untyped channel API
Does not apply.

## 2. The trigger packet carries a duration shipped code never varies
- EyeBehaviorTriggerPacket: record `(int entityId, EyeBehavior behavior, long seed, int elapsed)`; `duration`, `MAX_DURATION_TICKS`, `validTiming` gone; decode rejects `elapsed < 0 || elapsed > behavior.defaultDuration()` ("Invalid eye behavior timing"); class javadoc as 1.21.8's without the hand target.
- NetworkProtocol: `NETWORK_VERSION = "19"`.
- ServerBehaviorScheduler: `MobState.activeDuration` removed; `start(mob, state, behavior, seed)` sets `busyUntil = now + behavior.defaultDuration()`; `trigger(mob, behavior, seed)`; `catchUpPacket`, `onHealed`, `react`, `rollAmbient` updated. `now`, `onPlayerHurt` untouched.
- GooglyServerCommands: `trigger(target, behavior, target.getRandom().nextLong())`.
- ClientEyeRuntime: `ReceivedBehavior(behavior, seed, startedAt)`; `setBehavior(living, behavior, seed, elapsed)`; `synchronizeBehavior`/`tickBehaviors` expire on `behavior().defaultDuration()`. ClientNetworkHandler passes no duration.
- GooglyTracker: `install(behavior, seed, age)` passes `behavior.defaultDuration()` to `BehaviorInstance.start`. Deviation: 1.21.8 still has a `duration` parameter there. `BehaviorInstance.start` keeps its duration (local held/framed/skull/boat eyes use it).
- Tests: `behaviorTriggerPacketRoundTrips` round-trips every behavior at elapsed 0, half, and full default duration and rejects -1 and default+1 (new `rejectsTrigger` helper; unknown-behavior case kept). BehaviorSchedulerGameTestsLogic: 30 `trigger` calls lose the duration; five `advance(3)` after a blink and one `advance(1)` become `advance(EyeBehavior.BLINK.defaultDuration())`; `oneBehaviorAtATimeUntilItElapses` asserts a trigger on the stare's last active tick is dropped, then one a tick later starts; `receivedExpressionsOutliveRenderTrackers` runs `SWIRL.defaultDuration() - 5` ticks. Tests keep synchronous `advance(n)` (Reinvention 4 brings `after(...)`).
- Deviation: BehaviorDeterminismGameTestsLogic's class javadoc now says "(id, seed, elapsed ticks)" crosses the wire (1.21.8 left it stale).
- orientation-code.md: "version 19"; "Payloads own stable IDs; triggers carry behavior, seed, and age."; placed-head/frame sentence shortened to stay under the limit (body 11,996 characters).

## 3. Second hand-numbered wire identity
No change; `Target` wire ids left to Organization 1.

## 4. Picker export checks the age after decoding
- PickerExportPacket: after `readUtf(MAX_AGE_LENGTH)` throws `DecoderException("Invalid picker export age")` unless adult/baby; javadoc as 1.21.8 (keeps "10-second cooldown" wording for Javadocs 5).
- PickerExportService: age check and `AGE_BABY` import removed (`AGE_ADULT` still used in `withAge`).
- `pickerExportPacketRoundTrips` round-trips both ages, rejects `""`, `"any"`, `"Adult"`, `"unknown"`, keeps the no-config case.

## Recalled API facts
None new.

## Notes for later topics
- Organization 1: add `Target` between `entityId` and `behavior` (`(entityId, target, behavior, seed, elapsed)`), held-eye reaction checks, hand reports, `rejectsTrigger`-based target tests; do not bump the version again; orientation-code.md has ~4 characters of headroom.
- Reinvention 4: `start`/`trigger` take no duration; tests use synchronous `advance(n)` with `defaultDuration()` counts, ready to become `after(helper, ...)`.

## Clash points
ServerBehaviorScheduler, ClientEyeRuntime, GooglyTracker, EyeBehaviorTriggerPacket, GooglyServerCommands, BehaviorSchedulerGameTestsLogic, SerializationGameTestsLogic, orientation-code.md.

## In-game checks
Mob expressions play and end at their usual lengths; a second client tracking mid-expression joins in sync; `/sg admin behavior <id>` plays and drops correctly; `/sg export` for adult and baby; every loader refuses a version-18 client.

## Files modified
common main: network/EyeBehaviorTriggerPacket, network/NetworkProtocol, network/PickerExportPacket, picker/PickerExportService, eye/behavior/ServerBehaviorScheduler, command/GooglyServerCommands, client/ClientEyeRuntime, client/ClientNetworkHandler, client/GooglyTracker. Gametest: SerializationGameTestsLogic, BehaviorSchedulerGameTestsLogic, BehaviorDeterminismGameTestsLogic. Docs: orientation-code.md, review-Networking.md.
