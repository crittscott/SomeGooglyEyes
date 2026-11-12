# Report: Reinvention (1.21.11)

Applied 2026-11-11 by hand-porting 1.21.8's changes. Nothing was compiled, built, or tested; everything here comes from reading the source. No whole Java files copied; only 1.21.8's docs/review-checks tree and test_environment JSONs were copied whole.

## 3. Fabric skips the vanilla interaction advancement trigger
No code change. docs/review-checks/ added from 1.21.8 (README.md; advancement-probe with `data/somegoogly_review/advancement/apply.json` and `harvest.json`). README retitled "Manual 1.21.11 review checks"; the rest, including the held-eye section, is 1.21.8's. The probe's pack.mcmeta follows 1.21.11's own GameTest data pack form (`min_format` and `max_format` both `[94, 1]`, no legacy `pack_format`; 1.21.8's uses legacy `pack_format` 81). Not run.

## 4. The scheduler keeps its own tick counter
- ServerBehaviorScheduler: `now` field, its reset, and `now++` removed; private `clock(mob)` returns `mob.level().getServer().getTickCount()`, used for catch-up, heal cooldown check and arming, end-tick retirement, and `start`; `start` drops a trigger only when `busyUntil != 0 && now < busyUntil`, so a behavior past its end tick counts as over before the sweep (1.21.8's comment); class javadoc ends "Time is the server's tick count." Deviation: the heal cooldown uses `clock(mob)` with `ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS` directly (1.21.8's `BehaviorSettings.canSwirlOnHeal`/`nextHealReadyAt` belong to Organization 1).
- BehaviorSchedulerGameTestsLogic: `advance(int)` replaced by 1.21.8's `after(helper, ticks, restore, step)` (`runAfterDelay`, restore on failure). Eight tests wait real ticks with a `restore` Runnable: `affectionEventsPlayLove`, `disabledEyesSuspendAutomaticBehaviors`, `gameEventTriggersRespectConfig`, `healSwirlIsRateLimited`, `milkingPlaysGrow`, `oneBehaviorAtATimeUntilItElapses`, `sleepingMobPlaysNoBehaviors`, `healAtFullHealthPlaysNoSwirl`. Direct `on*` calls kept (Test-completeness 2 swaps them for real triggers).
- Eight `common/src/gametest/resources/data/somegoogly/test_environment/<test>.json` (empty `minecraft:all_of`) copied from 1.21.8; Fabric `BehaviorSchedulerGameTests` now matches 1.21.8 (`maxTicks = 400, environment = "somegoogly:<test>"`); Forge/NeoForge `test_instance/behaviorschedulergametests/<test>.json` have `environment` and `max_ticks` 400. No tests added/removed.
- Not applied: the ninth environment, `held_eye_swirls_when_healed`. 1.21.11's `heldEyeSwirlsWhenHealed` is the old client-side test stepping its own ticks; Organization 1's rewrite brings the waiting version, its environment JSON, and 400-tick wrappers.
- orientation-code.md: scheduler sentence ends "...entity-event triggers on the server clock"; small trims elsewhere; body 11,998 characters.

## 6. Version-range parser
1.21.8's class javadoc paragraph added to VersionRangeMatcher. No behavior change.

## Recalled API facts
`Level.getServer().getTickCount()` server-side (as PickerGate); `GameTestHelper.runAfterDelay`; a test environment splits a test into its own batch; `minecraft:all_of` environment JSON shape; Fabric `@GameTest(environment = ...)`; `min_format`/`max_format` `[major, minor]` for a 94.1 data pack (from 1.21.11's own pack.mcmeta); `runAfterDelay` tick ordering matches 1.21.8's assumption.

## Notes for later topics
- Organization 1: add `test_environment/held_eye_swirls_when_healed.json`, Fabric `maxTicks = 400, environment = ...`, and Forge/NeoForge `environment`/`max_ticks` 400 when the held-eye test becomes a real-tick test; use `clock(holder)` in `HandState` timing; orientation-code.md has ~2 characters of headroom.
- Test-completeness: the scheduler tests already use `after(...)`/`restore`; swap the direct `on*` calls inside those lambdas.
- Pre-existing: `disabledEyesSuspendAutomaticBehaviors` has its javadoc block duplicated (1.21.8 has one).

## Clash points
ServerBehaviorScheduler (Organization 1), BehaviorSchedulerGameTestsLogic (Test-completeness), HeldEye test files and JSONs (Organization 1), orientation-code.md.

## In-game checks
Mob expressions start, end, and catch up for a viewer joining partway; heal-swirl cooldown about 10 seconds; run the advancement probe and spawn-protection checks in docs/review-checks/README.md on all three loaders; the eight waiting scheduler gametests pass on all loaders.

## Files modified
common main: eye/behavior/ServerBehaviorScheduler, config/VersionRangeMatcher. Gametest: BehaviorSchedulerGameTestsLogic; 8 new test_environment JSONs; fabric BehaviorSchedulerGameTests; 8 forge and 8 neoforge behaviorschedulergametests test_instance JSONs. Docs: docs/review-checks/ (new), orientation-code.md, review-Reinvention.md.
