# Report: Reinvention (26.1)

Applied 2026-11-11 by hand-porting 1.21.11's changes. Nothing was compiled, built, or tested; everything here comes from reading the source. Copied whole: docs/review-checks/ (then edited), the eight test_environment JSONs, Fabric `BehaviorSchedulerGameTests.java`, `VersionRangeMatcher.java` (the Java files differed from 1.21.11's only by this topic). Fix lines in review-Reinvention.md are filled in.

## 3. Fabric skips the vanilla interaction advancement trigger
No code change. docs/review-checks/ copied from 1.21.11 (README.md; advancement-probe with `data/somegoogly_review/advancement/apply.json` and `harvest.json`). README retitled "Manual 26.1 review checks"; nothing else in it is 1.21.11- or Heads-specific. Probe pack.mcmeta: `min_format` and `max_format` both `[101, 1]`, no legacy `pack_format`, in the layout of 26.1's gametest pack.mcmeta with literal values. Not run.

## 4. The scheduler keeps its own tick counter
- ServerBehaviorScheduler: `now` field, its reset, and `now++` removed; private `clock(LivingEntity mob)` returns `mob.level().getServer().getTickCount()`, used in `catchUpPacket`, the `onHealed` cooldown check and arming (`clock(mob) + ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS.get()`), end-tick retirement in `serverTick`, and `start`. `start` drops a trigger only when `busyUntil != 0 && now < busyUntil` or the mob sleeps, so a behavior past its end tick counts as over before the sweep retires it (commented). Field comments say "server tick"; class javadoc ends "Time is the server's tick count."
- BehaviorSchedulerGameTestsLogic: `advance(int)` replaced by `after(helper, ticks, restore, step)` (`helper.runAfterDelay`; a failure in `step` runs `restore` and rethrows). The eight waiting tests (`affectionEventsPlayLove`, `disabledEyesSuspendAutomaticBehaviors`, `gameEventTriggersRespectConfig`, `healSwirlIsRateLimited`, `milkingPlaysGrow`, `oneBehaviorAtATimeUntilItElapses`, `sleepingMobPlaysNoBehaviors`, `healAtFullHealthPlaysNoSwirl`) wait real ticks with a `restore` Runnable; `sleepingMobPlaysNoBehaviors` turns ambient off before waking the villager. 26.1's direct `on*` calls, spawn code, and seeds kept.
- Eight `common/src/gametest/resources/data/somegoogly/test_environment/<test>.json` (empty `minecraft:all_of`); Fabric wrappers `maxTicks = 400, environment = "somegoogly:<test>"`; Forge/NeoForge `test_instance/behaviorschedulergametests/<test>.json` with `environment` and `max_ticks` 400 (byte-identical to 1.21.11's after line-ending normalization).
- Not applied: the ninth environment (`held_eye_swirls_when_healed`), which Organization 1 brings.
- orientation-code.md: "...shearing/entity-event triggers on the server clock; sleepers start nothing" (body 11,990 characters).

## 6. Version-range parser
VersionRangeMatcher class javadoc paragraph on the shared data-format ordering (file now identical to 1.21.11's). No behavior change.

## Deviations from 1.21.11
Heal cooldown uses `ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS` with `clock(mob)` (`BehaviorSettings.canSwirlOnHeal`/`nextHealReadyAt` come with Organization 1); test bodies keep 26.1's direct `on*` calls (Test-completeness 2); no hand-reaction code (Organization 1).

## Recalled API facts
`Level.getServer().getTickCount()` on the server; `GameTestHelper.runAfterDelay(ticks, Runnable)`; the scheduler sweep runs after the tick count increments on every loader; a test environment splits a test into its own batch, and an empty `minecraft:all_of` environment is valid under `data/<ns>/test_environment/`; Fabric `@GameTest(environment = ...)` exists on 26.1's Fabric API; Forge/NeoForge test_instance `environment` accepts a mod-namespaced id.

## In-game checks
Mob expressions start, end, and catch up for a viewer joining partway; the heal-swirl cooldown is about 10 seconds; run the advancement probe and spawn-protection/adventure checks in docs/review-checks/README.md on all three loaders.

## Left to other topics
Organization 1: the ninth environment and its 400-tick wrappers; `clock(holder)` for hand timing; the heal cooldown into `BehaviorSettings`; orientation-code.md has about 10 characters of headroom. Test-completeness 2: real triggers inside the `after(...)` lambdas. Pre-existing duplicated javadocs in BehaviorSchedulerGameTestsLogic (`disabledEyesSuspendAutomaticBehaviors`, `playerHolding`) left for Test-completeness 2 and Javadocs 8.

## Clash points
ServerBehaviorScheduler, BehaviorSchedulerGameTestsLogic, Fabric BehaviorSchedulerGameTests, Forge/NeoForge behaviorschedulergametests JSONs, orientation-code.md.

## GameTests
None added or removed; eight tests now run in their own environments with 400-tick limits on all loaders.

## Files modified
common main: eye/behavior/ServerBehaviorScheduler.java, config/VersionRangeMatcher.java. Common gametest: BehaviorSchedulerGameTestsLogic.java; 8 new test_environment JSONs. Fabric gametest: BehaviorSchedulerGameTests.java. Forge and NeoForge: 8 test_instance JSONs each. Docs: docs/review-checks/ (new), orientation-code.md, review-Reinvention.md.
