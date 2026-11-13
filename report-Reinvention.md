# Report: Reinvention (26.3)

Applied 2026-11-12 from 26.2's post-fix hunks (26.2/report-Reinvention.md exists). Only this topic's hunks were taken, keeping 26.3's API differences. Nothing was compiled, built, or tested. Fix lines in review-Reinvention.md filled in.

## 3. Fabric skips the vanilla interaction advancement trigger
No code change. New docs/review-checks/ copied from 26.2: README.md, advancement-probe/pack.mcmeta, advancement-probe/data/somegoogly_review/advancement/apply.json and harvest.json. README retitled "Manual 26.3 review checks". Probe pack.mcmeta min/max format `[121, 0]` (26.3's gradle.properties `pack_format` 121, `pack_format_minor` 0), not 26.2's `[107, 1]`. The probe has not been run.

## 4. The scheduler keeps its own tick counter
- ServerBehaviorScheduler: `now` field, its reset in `clear`, and `now++` in `serverTick` removed. Private `clock(LivingEntity mob)` returns `mob.level().getServer().getTickCount()`, used in `catchUpPacket`, the `onHealed` cooldown check and arming, retirement in `serverTick`, and `start` (local `now = clock(mob)`; a trigger is dropped only when `busyUntil != 0 && now < busyUntil` or the mob is asleep, with a comment).
- Adaptation in `onHealed`: 26.3 has no `BehaviorSettings.canSwirlOnHeal`/`nextHealReadyAt` (Organization's in 26.2), so the 26.1.1 form is kept: `clock(mob) < state.healSwirlReadyAt` and `clock(mob) + ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS.get()`. `eventTarget` returns null for untracked mobs before `clock` is reached, so a client-side call never reaches `getServer()`.
- Field comments say "server tick"; class javadoc ends "Time is the server's tick count."
- BehaviorSchedulerGameTestsLogic: class javadoc says the server clock runs. `advance(int)` replaced by `after(helper, ticks, restore, step)` using `GameTestHelper.runAfterDelay`, running `restore` on failure. Eight tests wait real ticks, each with a `restore` built from its former finally block (success runs `restore` then `helper.succeed()`; a catch runs `restore` and rethrows): `affectionEventsPlayLove`, `disabledEyesSuspendAutomaticBehaviors`, `gameEventTriggersRespectConfig`, `healSwirlIsRateLimited`, `milkingPlaysGrow`, `oneBehaviorAtATimeUntilItElapses` (keeps the duration-1 / +1 boundary), `sleepingMobPlaysNoBehaviors` (turns ambient off before waking; comment "every tick in between"), `healAtFullHealthPlaysNoSwirl`.
- Kept from 26.3: direct `onMilked`/`onHealed`/`onTrade`/`onPlayerHurt` triggers (real triggers and the `trade` helper belong to Test-completeness and Organization); `Enderman` and the boolean `startSleeping` assertion; two-argument `ClientEyeRuntime.tick`; `ServerBehaviorScheduler.isAngry`.
- Test environments: 8 new common/src/gametest/resources/data/somegoogly/test_environment/<test>.json (empty `minecraft:all_of`, LF), copied from 26.2; `held_eye_swirls_when_healed` not copied (Organization 1).
- Fabric BehaviorSchedulerGameTests: the eight wrappers are `maxTicks = 400, environment = "somegoogly:<test>"` (file copied from 26.2; differed only by these lines).
- Forge and NeoForge test_instance/behaviorschedulergametests/<test>.json, 8 each: environment `somegoogly:<test>`, max_ticks 400, CRLF kept (copied from 26.2 after checking each differed only in those keys).
- orientation-code.md: "damage/heal/trade/milk/shear/entity-event triggers on the server clock"; anger sentence "Anger syncs on change; only angry entities get an initial packet."; master-switch sentence "...pauses mob scheduling, keeping watchers and expiring effects; authoring triggers still work." Body 11,992 characters. build-env/orientation-code.md is a different document and unchanged.

## 6. Version-range parser
VersionRangeMatcher class-javadoc paragraph on the shared data-format ordering added; the file equals 26.2's and 26.1.1's. No behavior change.

## Left to other topics
- Organization 1: the ninth environment (`held_eye_swirls_when_healed`), hand state, and the heal cooldown helpers in BehaviorSettings.
- Test-completeness 2: real triggers inside the `after(...)` lambdas and the eyeless-villager trade case.

## Recalled API facts
`Level.getServer().getTickCount()`; `GameTestHelper.runAfterDelay(long, Runnable)`; the scheduler sweep runs after the tick count increments on every loader; an empty `minecraft:all_of` test environment is valid and batches its test alone; Fabric `@GameTest(environment = ...)` exists in Fabric API 0.163.0+26.3; Forge/NeoForge test_instance `environment` accepts a mod-namespaced id; 26.3's pack format is 121.0.

## First build settles
The eight rewritten tests pass within 400 ticks on all loaders (notably the `oneBehaviorAtATimeUntilItElapses` boundary); the environments load; the probe pack format is accepted.

## In-game checks
- Mob expressions start, end, and catch up for a late viewer.
- The heal-swirl cooldown is about 10 seconds.
- The advancement probe and the spawn-protection/adventure checks in docs/review-checks/README.md, on all loaders.

## GameTests
None added or removed: Fabric +0, Forge +0, NeoForge +0.
