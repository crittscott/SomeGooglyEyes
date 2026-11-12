# Report: Test-completeness (1.21.11)

Applied 2026-11-11 by hand-porting 1.21.8's changes. Nothing was compiled, built, or tested; everything here comes from reading the source. Each file was diffed against 1.21.8 and only this topic's hunks taken. Copied whole (only this topic differed): the three loader `SomeGooglyGameTests`, `EyeStateOverrideGameTests`, `EligibilityGameTests` wrappers and the new test_instance JSONs. Copied whole with mechanical port edits (`Identifier`, 1.21.11's `animal.cow.Cow` package, jspecify `@Nullable`, the TestPlayers call): LoaderInteractionGameTestsLogic, EyeStateOverrideGameTestsLogic, EligibilityGameTestsLogic.

## 2. Heal, trade, hit, and milking tests call the scheduler
BehaviorSchedulerGameTestsLogic: `disabledEyesSuspendAutomaticBehaviors` uses `milk(player, cow)` (held item restored; duplicated javadoc removed); `eyelessTrackedMobIgnoresGameEvents` real `cow.heal(1F)` and a real trade with an eyeless villager; `gameEventTriggersRespectConfig` eyed cow and villager, `heal`, `trade`, `cow.hurtServer(level, playerAttack(player), 1F)`; heal-cooldown, full-health, sleeping-villager, completed-trade tests heal and trade for real; private `trade(ServerPlayer, Villager)` helper. No direct `on*` scheduler calls remain. The file differs from 1.21.8 only by port renames and two javadocs 1.21.8 lacks.

## 3. Item use tested below the loader event
- New LoaderInteractionGameTestsLogic (1.21.8's; jspecify; `TestPlayers.create(template, false, TestPlayers.NO_PERMISSIONS)` — an explicit non-operator so spawn-protection bypass can't mask anything).
- `nativeUseHooksHonorResults` on all three loaders: Fabric invokes `UseBlockCallback`/`UseEntityCallback`/`UseItemCallback` (no refusal case); Forge per-event `BUS.post` refused by a temporary `Priority.HIGHEST` predicate listener; NeoForge `NeoForge.EVENT_BUS` with a temporary HIGHEST listener and `unregister`. Forge/NeoForge registrations and `test_instance/somegooglygametests/native_use_hooks_honor_results.json` (max_ticks 40).
- EyeItemServiceGameTestsLogic class javadoc points to LoaderInteractionGameTestsLogic.

## 4. End exit / death copies
`nativePlayerCopiesKeepEyesOnlyWhileAlive` on all loaders (Fabric `COPY_FROM` invoker; Forge `PlayerEvent.Clone.BUS.post(new PlayerEvent.Clone(replacement, original, !alive))`; NeoForge `NeoForge.EVENT_BUS.post`); registrations and JSON (max_ticks 40). Relies on Correctness 2's `ServerServices.onPlayerCloned`.

## 5. Death-harvest tests
- `deathHarvestRejectsNonqualifyingKills` and `realShearsKillsHarvestAnEye` are 1.21.8's real kills (fresh cow per case, `hurtServer(..., Float.MAX_VALUE)`, `eyeDrops(helper, victim)`). Rejections: sword, eyeless, mob, arrow, thorns (Correctness 5 kept), chance, definition, disabled, loot. Real kill: adult and calf, one eye each in its iris color, one durability per harvest.
- 1.21.11 adaptations: `level.getGameRules()` with `get/set(GameRules.MOB_DROPS, …)`; `EntitySpawnReason.TRIGGERED` for the arrow; javadoc says `mob_drops`.
- Removed `deathHarvestUsesTheSuppliedDropSink` (three wrappers, two registrations, two JSONs).

## 6. `/sg admin` dispatcher tests
- EyeStateOverrideGameTestsLogic is 1.21.8's three tests (`adminAppearanceCommands`, `adminCommandsRefuseUnauthorizedOrInvalidUse`, `adminEyesAndBehaviorCommands`); the six mutator tests removed with wrappers, 12 registrations, 12 JSONs; Dead-code's `hasOverrideCompound` went with them.
- Deviation (permissions): 1.21.11 checks `player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)` (PickerGate.mayUsePicker via `GooglyServerCommands.requireTarget`). TestPlayers gains `create(template, creative, PermissionSet permissions)` (anonymous `ServerPlayer` overriding `permissions()`), keeps the two-argument `create` (HeldEyeGameTestsLogic), shared private `setUp`, and constants `GAMEMASTER = Permissions.COMMANDS_GAMEMASTER::equals`, `NO_PERMISSIONS = permission -> false`. The command source uses `.withPermission(TestPlayers.GAMEMASTER)` (as 1.21.8's `withPermission(2)` and PickerAuthGameTestsLogic).

## 7. Eligibility through spawns
EligibilityGameTestsLogic is 1.21.8's: `canEverWearEyesSpansBothAgeBuckets` creates a NATURAL cow, sets age, `snapTo`, `addFreshEntity` under a 100% override, checks aging keeps the roll and a missing/disabled definition never wins; `slimyEyeAppliesOnlyToEligibleTargets` gains the disabled-definition case. Removed `usablePredicateEndpoints` and `eligibilityFollowsTheSharedPredicate` (wrappers, registrations, JSONs).

## Counts and docs
Fabric 187 `@GameTest`, Forge 191, NeoForge 193 (registrations match JSONs one-to-one); was 191/195/197 (9 removed, 5 added). orientation-build.md and orientation-code.md updated; no build-env file carries counts.

## Recalled API facts (unverified)
1.21.11 `ServerPlayer.permissions()` returns `PermissionSet`, public and overridable, read by `PickerGate.mayUsePicker`; `PermissionSet` is a functional interface; `Commands.hasPermission(LEVEL_GAMEMASTERS)` reads the source's permission set; `LivingEntity.heal` reaches loader heal hooks; `Villager.notifyTrade` reaches the trade hook; `hurtServer(level, source, amount)` fires each loader's damage hook on mocks; `GameRules.get/set(MOB_DROPS, …)`; `Entity.snapTo(BlockPos, float, float)`; Forge 61 `PlayerEvent.Clone(Player, Player, boolean)`, interaction-event constructors, `CancellableEventBus.addListener(Priority, Predicate)`/`removeListener`, `getCancellationResult()`; NeoForge `EVENT_BUS.addListener(EventPriority, boolean, Class, Consumer)`/`unregister`; Fabric `COPY_FROM.copyFromPlayer(old, new, alive)` and use-callback invokers; `DamageSources.thorns(player)` is not `PLAYER_ATTACK`.

## Left to other topics
Javadocs: `selfRemoveWithShears…` javadoc's refusal/ordering wording; two extra BehaviorScheduler test javadocs. HeldEyeGameTestsLogic still uses the two-argument `TestPlayers.create` (Organization's choice).

## Clash points
BehaviorSchedulerGameTestsLogic, SomeGooglyGameTestsLogic, EyeStateOverrideGameTestsLogic, EligibilityGameTestsLogic, EyeItemServiceGameTestsLogic, TestPlayers, SomeGooglyGameTestMod files, loader wrappers, orientation-build.md, orientation-code.md.

## In-game checks
On all loaders: eyed cows swirl on heal, villagers on trade, cows grow on milking, switches stop each; Slimy Eye and Optometrist shears on cow, boat, armor stand, placed head, and yourself; inside a refusing claim nothing changes (Fabric claim ordering manual); applied eyes survive leaving the End and are gone after death; a shears kill at `harvestOnKillPercent = 100` drops one eye in the iris color and costs one durability, while sword, arrow, thorns, and `mob_drops` off drop none; `/sg admin tint/glow/eyes/behavior` work for a creative operator and are refused for a survival operator, creative non-operator, bad arguments, no target; a cow with only a baby or only an adult definition still rolls natural eyes. The gametest suite must run; the refusal test depends on the `permissions()` override.

## Files modified
Common gametest: BehaviorSchedulerGameTestsLogic, SomeGooglyGameTestsLogic, EyeStateOverrideGameTestsLogic, EligibilityGameTestsLogic, EyeItemServiceGameTestsLogic (class javadoc), TestPlayers, LoaderInteractionGameTestsLogic (new). Loader gametests: SomeGooglyGameTests, EyeStateOverrideGameTests, EligibilityGameTests on all three; Forge/NeoForge SomeGooglyGameTestMod. test_instance: added 5 JSONs per Forge/NeoForge (two somegooglygametests, three eyestateoverridegametests); deleted 9 per loader (drop-sink, two eligibility, six mutators). Docs: orientation-build.md, orientation-code.md, review-Test-completeness.md.
