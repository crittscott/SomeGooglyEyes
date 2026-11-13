# Report: Networking (26.1.2)
Applied 2026-11-12 by hand-porting 26.1.1's Networking hunks (diffed with `diff --strip-trailing-cr`, edits by Edit and small Python replace scripts that assert match counts). Nothing was compiled, built, or tested. No file copied whole: 26.1.1's copies of every touched file also carry Organization 1 (`Target`, hand reactions) and Reinvention 4 (server clock, `after(...)`), which are left out here. Line endings kept (LF).

## 1, 3
No change.

## 2. The trigger packet carries a duration that shipped code never varies
- network/EyeBehaviorTriggerPacket.java: record is `(entityId, behavior, seed, elapsed)`; `duration` field and wire field, `MAX_DURATION_TICKS`, `validTiming` removed; decode throws "Invalid eye behavior timing" unless `0 <= elapsed <= behavior.defaultDuration()`. Javadoc rewritten as 26.1.1's but without the hand-target wording ("play behavior on the eyes of entity entityId ... Every expression runs for its behavior's defaultDuration()"); the second paragraph keeps 26.1.2's "trigger ... render tracker" wording (26.1.1's "report for a known entity or hand" is Organization 1).
- network/NetworkProtocol.java: `NETWORK_VERSION = "19"`. CHANGELOG-26.1.1-26.1.2.md:18 now says protocol 19.
- eye/behavior/ServerBehaviorScheduler.java: `MobState.activeDuration` removed; `start(mob, state, behavior, seed)` sets `busyUntil = now + behavior.defaultDuration()`; `trigger(mob, behavior, seed)`; `catchUpPacket`, heal swirl, `react`, `rollAmbient`, and the start broadcast updated. The `now` scheduler clock kept (Reinvention 4).
- command/GooglyServerCommands.java: `trigger(target, behavior, target.getRandom().nextLong())`.
- client/ClientEyeRuntime.java: `ReceivedBehavior(behavior, seed, startedAt)`; `setBehavior(living, behavior, seed, elapsed)`; both expiry checks use `behavior().defaultDuration()` (the leftovers Dead-code 1 handed over).
- client/ClientNetworkHandler.java: passes no duration.
- client/GooglyTracker.java: `install(behavior, seed, age)` passes `behavior.defaultDuration()` to `BehaviorInstance.start` (`BehaviorInstance.duration` itself stays).
- Gametests: SerializationGameTestsLogic `behaviorTriggerPacketRoundTrips` round-trips every behavior at elapsed 0, half, and full (checking behavior, elapsed, seed) and rejects -1 and full+1 through a new `rejectsTrigger` helper; unknown-behavior case kept unchanged (no target varint, since there is no `Target` yet); javadoc updated without target wording. BehaviorSchedulerGameTestsLogic: 26 `trigger` calls lose their duration; the five `advance(3)` blink waits and the `advance(1)` in `healAtFullHealthPlaysNoSwirl` become `advance(EyeBehavior.BLINK.defaultDuration())`; `oneBehaviorAtATimeUntilItElapses` advances `STARE.defaultDuration() - 1`, checks a trigger is dropped on the last active tick, advances 1, checks a new one starts (26.1.1's check, with synchronous `advance` instead of `after`); `receivedExpressionsOutliveRenderTrackers` uses the 4-argument `setBehavior` and an expiry loop of `SWIRL.defaultDuration() - 5` ticks (was 7 for a 12-tick swirl); `receivedBehaviorPlays` passes no duration. BehaviorDeterminismGameTestsLogic javadoc says "(id, seed, elapsed ticks)".
- orientation-code.md: "Payloads own stable IDs; triggers carry behavior, seed, and age. `NetworkProtocol` declares version 19"; "At extraction, placed heads check compound identity and frames check item components." shortened to "Placed heads and frames recheck eye data at extraction." as in 26.1.1, to fit. Body 11,993 characters.
- Deviation from 26.1.1: no `Target`, no held-eye decode check, no target round-trips or bad-target case (Organization 1); synchronous `advance(n)` instead of `after(...)` (Reinvention 4).

## 4. Picker export checks the age after decoding
- network/PickerExportPacket.java: decoder throws `DecoderException("Invalid picker export age")` unless `AGE_ADULT`/`AGE_BABY` (new static imports); javadoc "all validation" became "semantic config validation", and the second paragraph is 26.1.1's ("The decoder requires an adult or baby age and a config compound ..."). "10-second cooldown" left for Javadocs.
- picker/PickerExportService.java: age check and `AGE_BABY` import removed (`AGE_ADULT` still used at :167). The `isExcluded`/javadoc hunk in 26.1.1's diff belongs to another topic and was left.
- SerializationGameTestsLogic `pickerExportPacketRoundTrips`: shared `cow` id, round-trips both ages, rejects `""`, `"any"`, `"Adult"`, `"unknown"` on decode; javadoc as 26.1.1; `AGE_BABY` static import added.
- No deviation from 26.1.1.

## Left for other topics / the parent
- Organization 1: `EyeBehaviorTriggerPacket.Target` between `entityId` and `behavior`, held-eye decode check, target round-trip and bad-target tests, "target (entity or hand)" in orientation-code.md (about 7 characters of headroom remain, so that edit needs a trim), and the "report for a known entity or hand" javadoc wording. No further protocol bump needed for it within this batch.
- Reinvention 4: server clock (`clock(mob)`) and `after(...)` waits in BehaviorSchedulerGameTestsLogic; the synchronous `advance(...)` calls written here will be replaced then.
- Javadocs: "10-second cooldown" wording in PickerExportPacket.
- BehaviorSchedulerGameTestsLogic has a duplicated javadoc line before `playerHolding` (another topic's; untouched).
- Parent: Fix lines in review-Networking.md.

## Recalled API facts and checks only a build or in-game test can settle
- All changed call sites should compile; a grep found no remaining 5-argument `setBehavior`, 4-argument `trigger`, or `duration`-taking `install` callers in any module.
- Scheduler tests assume `advance(D)` (D calls of `serverTick()`) retires a behavior started D ticks earlier and that D-1 does not.
- `receivedExpressionsOutliveRenderTrackers` assumes a swirl received at elapsed 3, aged to 5, expires after `SWIRL.defaultDuration() - 5` more unpaused ticks.

## In-game checks
Expressions play and end at their usual lengths; a second client tracking a mob mid-expression joins in sync; `/sg admin behavior <id>` plays, and is dropped while busy; `/sg export` works for adult and baby; every loader refuses a protocol-18 client.

## Fix lines
2: `EyeBehaviorTriggerPacket` is `(entityId, behavior, seed, elapsed)`; `duration`, `MAX_DURATION_TICKS`, `validTiming` gone; decode rejects `elapsed` outside `0..behavior.defaultDuration()`; javadoc updated. Duration parameters removed from `ServerBehaviorScheduler.start`/`trigger` and `MobState.activeDuration` (expiry `now + behavior.defaultDuration()`), `GooglyServerCommands`, `ClientEyeRuntime.ReceivedBehavior`/`setBehavior` (expiry on `behavior().defaultDuration()`), `ClientNetworkHandler`, `GooglyTracker.install` (passes `behavior.defaultDuration()` to `BehaviorInstance.start`). `NETWORK_VERSION` "19"; CHANGELOG-26.1.1-26.1.2.md:18 protocol 19. `behaviorTriggerPacketRoundTrips` round-trips every behavior at elapsed 0/half/full and rejects -1 and full+1 (new `rejectsTrigger` helper). BehaviorSchedulerGameTestsLogic callers updated with synchronous `advance(n)` kept (blink waits use `BLINK.defaultDuration()`; the stare test checks the last active tick drops a trigger; the received-expression expiry loop runs `SWIRL.defaultDuration() - 5` ticks). BehaviorDeterminismGameTestsLogic javadoc says "(id, seed, elapsed ticks)". orientation-code.md updated by hand with 26.1.2's labels (body 11,993 characters). Same as 26.1.1; `Target` and the server clock left to Organization 1 and Reinvention 4. Unbuilt.
4: `PickerExportPacket`'s decoder throws `DecoderException("Invalid picker export age")` unless adult/baby; javadoc updated ("10-second cooldown" wording left for Javadocs); `PickerExportService` age check and `AGE_BABY` import removed; `pickerExportPacketRoundTrips` round-trips both ages and rejects `""`, `"any"`, `"Adult"`, `"unknown"`. Same as 26.1.1. Unbuilt.
