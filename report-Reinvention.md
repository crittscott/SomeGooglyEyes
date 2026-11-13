# Report: Reinvention (26.2)

Applied 2026-11-12 by taking 26.1.1's hunks for findings 3, 4, and 6, while other topic agents edited the same files. Nothing was compiled, built, or tested. Fix lines in review-Reinvention.md filled in.

## 3. Fabric skips the vanilla interaction advancement trigger
No code change. New docs/review-checks/ copied from 26.1.1: README.md, advancement-probe/pack.mcmeta, advancement-probe/data/somegoogly_review/advancement/apply.json and harvest.json. README retitled "Manual 26.2 review checks". Deviation: probe pack.mcmeta min/max format `[107, 1]` (26.2 gradle.properties pack_format 107.1) instead of `[101, 1]`. Probe not run.

## 4. The scheduler keeps its own tick counter
- ServerBehaviorScheduler: `now` field, its reset in `clear`, and `now++` in `serverTick` removed; private `clock(LivingEntity mob)` returns `mob.level().getServer().getTickCount()`, used in `catchUpPacket`, the `onHealed` cooldown check and arming, retirement in `serverTick`, and `start` (local `now = clock(mob)`, drops a trigger only when `busyUntil != 0 && now < busyUntil` or asleep, commented). Field comments say "server tick"; class javadoc ends "Time is the server's tick count." Networking (duration parameter removal) and Organization (heal cooldown in BehaviorSettings) edited the same file concurrently.
- BehaviorSchedulerGameTestsLogic: class javadoc says the server clock runs; `advance(int)` replaced by `after(helper, ticks, restore, step)`. Eight tests (`affectionEventsPlayLove`, `disabledEyesSuspendAutomaticBehaviors`, `gameEventTriggersRespectConfig`, `healSwirlIsRateLimited`, `milkingPlaysGrow`, `oneBehaviorAtATimeUntilItElapses` with the duration-1/+1 boundary, `sleepingMobPlaysNoBehaviors` with ambient off before waking, `healAtFullHealthPlaysNoSwirl`) wait real ticks with a `restore` Runnable. Waits use `EyeBehavior.X.defaultDuration()` because Networking removed explicit trigger durations mid-application. Triggers already present (some real, from other topics) kept as found.
- 8 new common/src/gametest/resources/data/somegoogly/test_environment/<test>.json (empty minecraft:all_of, LF).
- Fabric BehaviorSchedulerGameTests: the eight wrappers `maxTicks = 400, environment = "somegoogly:<test>"`.
- Forge/NeoForge test_instance/behaviorschedulergametests/<test>.json x8 each: environment `somegoogly:<test>`, max_ticks 400, CRLF kept.
- orientation-code.md: "...damage/heal/trade/milk/shear/entity-event triggers on the server clock; sleepers start nothing." 26.2 has no "(players watch their own)" to trim.

## 6. Version-range parser
VersionRangeMatcher class-javadoc paragraph on the shared data-format ordering; file equals 26.1.1's.

## Left to other topics
Organization 1: ninth environment (held_eye_swirls_when_healed) and its wrappers, `clock(holder)` hand timing, heal cooldown in BehaviorSettings. Networking: trigger duration removal. Test-completeness 2: real triggers inside the `after(...)` lambdas.

## Expected clashes
BehaviorSchedulerGameTestsLogic and ServerBehaviorScheduler changed during application; one edit normalized `BLINK,2L` spacing. orientation-code.md went over its limit after concurrent edits (trimmed in the parent's reconciliation).

## Recalled API facts
`Level.getServer().getTickCount()`; `GameTestHelper.runAfterDelay(long, Runnable)`; the scheduler sweep runs after the tick count increments on every loader; an empty `minecraft:all_of` test environment is valid and batches its test alone; Fabric `@GameTest(environment = ...)` in Fabric API 0.161.0+26.2; Forge/NeoForge test_instance environment accepts a mod-namespaced id; 26.2 pack format 107.1.

## First build settles
The eight rewritten tests pass within 400 ticks on all loaders (notably the `oneBehaviorAtATimeUntilItElapses` boundary); the environments load; the probe pack format is accepted.

## In-game checks
Mob expressions start, end, and catch up for a late viewer; heal-swirl cooldown about 10 seconds; the advancement probe and spawn-protection/adventure checks in docs/review-checks/README.md on all loaders.

## GameTests
None added or removed (Fabric, Forge, NeoForge: +0 each).
