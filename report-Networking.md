# Report: Networking (26.1)

Applied 2026-11-11 by hand-porting 1.21.11's changes. Nothing was compiled, built, or tested; everything here comes from reading the source. No file copied whole. Organization 1's `Target` and Reinvention 4's server clock were deliberately left out. Fix lines in review-Networking.md are filled in.

## 1, 3
No change.

## 2. The trigger packet carries a duration shipped code never varies
- EyeBehaviorTriggerPacket is `(int entityId, EyeBehavior behavior, long seed, int elapsed)`; `duration`, `MAX_DURATION_TICKS`, `validTiming` gone; decode rejects `elapsed < 0 || elapsed > behavior.defaultDuration()` ("Invalid eye behavior timing"); class javadoc says every expression runs for `EyeBehavior#defaultDuration()`.
- NetworkProtocol: `NETWORK_VERSION = "19"`.
- ServerBehaviorScheduler: `MobState.activeDuration` removed; `start(mob, state, behavior, seed)` sets `busyUntil = now + behavior.defaultDuration()`; `trigger(mob, behavior, seed)`; `catchUpPacket`, heal swirl, react/ambient starts updated.
- GooglyServerCommands: `trigger(target, behavior, target.getRandom().nextLong())`.
- ClientEyeRuntime: `ReceivedBehavior(behavior, seed, startedAt)`; `setBehavior(living, behavior, seed, elapsed)`; expiry on `behavior().defaultDuration()`. ClientNetworkHandler passes no duration.
- GooglyTracker: `install(behavior, seed, age)` passes `behavior.defaultDuration()` to `BehaviorInstance.start` (which keeps its duration parameter for local held, framed, skull, and boat eyes).
- SerializationGameTestsLogic `behaviorTriggerPacketRoundTrips`: every behavior at elapsed 0, half, and full; rejects -1 and full+1 via new `rejectsTrigger`; unknown-behavior case kept.
- BehaviorSchedulerGameTestsLogic: 26 `trigger` calls lose the duration; blink waits use `BLINK.defaultDuration()`; `oneBehaviorAtATimeUntilItElapses` checks the last active tick still drops a trigger and the next tick starts one; `receivedExpressionsOutliveRenderTrackers` updated. Tests keep synchronous `advance(n)`.
- BehaviorDeterminismGameTestsLogic: class javadoc says "(id, seed, elapsed ticks)" crosses the wire.
- orientation-code.md: "triggers carry behavior, seed, and age", "version 19"; placed-head/frame sentence shortened (body 11,970 characters).
- CHANGELOG-21.11-26.1.md: "Multiplayer uses protocol 19".

## 4. Picker export checks the age after decoding
PickerExportPacket throws `DecoderException("Invalid picker export age")` unless adult or baby; javadoc updated (the "10-second cooldown" wording is left for Javadocs 5). PickerExportService: age check and `AGE_BABY` import removed. `pickerExportPacketRoundTrips` round-trips both ages and rejects `""`, `"any"`, `"Adult"`, `"unknown"`.

## Deviations from 1.21.11
No `Target`, held-eye decode checks, or hand-target tests (Organization 1); no `after(...)` real-tick waiting (Reinvention 4); test bodies keep their direct `onHealed`/`onTrade` calls (Test-completeness 2). `EyeBehavior.defaultDuration()`'s javadoc ("when triggered without an explicit duration") left as 1.21.11 left it; a Javadocs candidate.

## In-game checks
Mob expressions play and end at usual lengths; a second client tracking a mob mid-expression joins in sync; `/sg admin behavior <id>` plays and drops while busy; `/sg export` works for adult and baby; every loader refuses a version-18 client.

## Left to other topics
Organization 1: `Target` between `entityId` and `behavior`, held-eye decode checks, target tests; no second version bump; orientation-code.md has about 30 characters of headroom. Reinvention 4: server clock.

## Clash points
ServerBehaviorScheduler, ClientEyeRuntime, GooglyTracker, EyeBehaviorTriggerPacket, GooglyServerCommands, ClientNetworkHandler, PickerExportPacket, PickerExportService, BehaviorScheduler/Serialization/BehaviorDeterminism logic, orientation-code.md, CHANGELOG-21.11-26.1.md.

## GameTests
None added or removed; `behaviorTriggerPacketRoundTrips` and `pickerExportPacketRoundTrips` extended.

## Files modified
common main: network/EyeBehaviorTriggerPacket, network/NetworkProtocol, network/PickerExportPacket, picker/PickerExportService, eye/behavior/ServerBehaviorScheduler, command/GooglyServerCommands, client/ClientEyeRuntime, client/ClientNetworkHandler, client/GooglyTracker. Gametest: SerializationGameTestsLogic, BehaviorSchedulerGameTestsLogic, BehaviorDeterminismGameTestsLogic. Docs: orientation-code.md, CHANGELOG-21.11-26.1.md, review-Networking.md.
