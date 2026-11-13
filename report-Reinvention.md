# Report: Reinvention (26.1.1)

Applied 2026-11-11 by taking 26.1's hunks for findings 3, 4, and 6. Nothing was compiled, built, or tested. Fix lines and intro in review-Reinvention.md filled in.

## 3. Fabric skips the vanilla interaction advancement trigger
No code change. docs/review-checks/ copied from 26.1 (README, advancement-probe with `data/somegoogly_review/advancement/apply.json` and `harvest.json`); README retitled "Manual 26.1.1 review checks". It carries the spawn-protection/adventure manual check Protection 2 relies on and the held-eye and rendering checks. Probe pack.mcmeta stays `[101, 1]` (26.1.1's `pack_format` is 101.1). Probe not run.

## 4. The scheduler keeps its own tick counter
- ServerBehaviorScheduler: `now` field, its reset, and `now++` removed; private `clock(LivingEntity mob)` returns `mob.level().getServer().getTickCount()`, used in `catchUpPacket`, the `onHealed` cooldown check and arming, end-of-behavior retirement in `serverTick`, and `start`. `start` uses a local `now = clock(mob)` with Networking's `busyUntil = now + behavior.defaultDuration()`, dropping a trigger only when `busyUntil != 0 && now < busyUntil` or the mob sleeps (commented). Field comments say "server tick"; class javadoc ends "Time is the server's tick count."
- BehaviorSchedulerGameTestsLogic: `advance(int)` replaced by `after(helper, ticks, restore, step)` (`helper.runAfterDelay`; on failure runs `restore` and rethrows). Eight tests wait real ticks with `restore` Runnables: `affectionEventsPlayLove`, `disabledEyesSuspendAutomaticBehaviors`, `gameEventTriggersRespectConfig`, `healSwirlIsRateLimited`, `milkingPlaysGrow`, `oneBehaviorAtATimeUntilItElapses`, `sleepingMobPlaysNoBehaviors`, `healAtFullHealthPlaysNoSwirl`.
- Eight new `common/src/gametest/resources/data/somegoogly/test_environment/<test>.json` (empty `minecraft:all_of`) copied from 26.1. Fabric wrappers `maxTicks = 400, environment = "somegoogly:<test>"`; Forge and NeoForge test_instance JSONs set `environment` and `max_ticks` 400 (edited by hand, CRLF kept).
- orientation-code.md: "...shearing/entity-event triggers on the server clock; sleepers start nothing"; trimmed "(players watch their own)" → "(players self-watch)" and "milking/shearing" → "milk/shear". Body 11,997 characters.

## 6. Version-range parser
VersionRangeMatcher class-javadoc paragraph on the shared data-format ordering; file identical to 26.1's.

## Deviations from 26.1
Tests keep direct `on*` calls inside the `after(...)` lambdas (real triggers are Test-completeness 2's); `disabledEyesSuspendAutomaticBehaviors` has no held-item restore until `milk(...)` arrives. orientation-code.md wording trimmed differently from 26.1.

## Left to other topics
Organization 1: ninth environment (`held_eye_swirls_when_healed`) and its 400-tick wrappers, `clock(holder)` for hand timing, heal cooldown into `BehaviorSettings`, its orientation-code.md text (only 3 characters of headroom). Test-completeness 2: real triggers in the `after(...)` lambdas. Test-completeness 2 / Javadocs: duplicated javadocs in BehaviorSchedulerGameTestsLogic.

## Recalled API facts
`Level.getServer().getTickCount()`; `GameTestHelper.runAfterDelay(long, Runnable)`; the scheduler sweep runs after the tick count increments on every loader; an empty `minecraft:all_of` test environment is valid and batches its test alone; Fabric `@GameTest(environment = ...)` in Fabric API 0.145.x; Forge/NeoForge test_instance `environment` accepts a mod-namespaced id.

## First build settles
The eight rewritten tests pass within 400 ticks on all loaders (notably `oneBehaviorAtATimeUntilItElapses`'s `duration - 1`/`+1` boundary); the environments load.

## In-game checks
Mob expressions start, end, and catch up for a late viewer; heal-swirl cooldown about 10 seconds; the advancement probe and spawn-protection/adventure checks in docs/review-checks/README.md on all loaders.

## GameTests
None added or removed.
