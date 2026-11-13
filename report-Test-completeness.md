# Report: Test-completeness (26.1.1)

Applied all six findings 2026-11-11 from 26.1's code. Nothing was compiled, built, or tested. Each 26.1 file diffed against 26.1.1's (CR stripped); copied where the whole remaining diff was this topic's, else edited by hand. Fix lines and intro in review-Test-completeness.md filled in.

## 2. Heal, trade, hit, and milking tests used the scheduler
BehaviorSchedulerGameTestsLogic copied whole: `disabledEyesSuspendAutomaticBehaviors` milks with `milk(player, cow)` and restores the held item; duplicated javadocs collapsed (that test, `playerHolding`, `receivedExpressionsOutliveRenderTrackers`); real triggers `cow.heal(1F)`, `cow.hurtServer(level, playerAttack(player), 1F)`, the `eyedTrackedMob` villager, private `trade(ServerPlayer, Villager)`; the eyeless test uses a real eyeless villager. No direct `onMilked`/`onHealed`/`onTrade`/`onHurt` call remains; Reinvention's `after(...)` lambdas kept.

## 3. Item use tested below the loader event
New LoaderInteractionGameTestsLogic; `nativeUseHooksHonorResults` in all three loaders' SomeGooglyGameTests (copied whole). Fabric invokes `UseBlockCallback`/`UseEntityCallback`/`UseItemCallback` (no refusal case). Forge posts `RightClickBlock`, `EntityInteractSpecific` for every entity target, and `RightClickItem`, refusing via a temporary `Priority.HIGHEST` predicate listener. NeoForge posts `RightClickBlock`, `EntityInteractSpecific` for the armor stand, `EntityInteract`, `RightClickItem`, with a temporary HIGHEST listener. Registrations and `native_use_hooks_honor_results.json` on Forge/NeoForge. EyeItemServiceGameTestsLogic class and self-shear javadocs point to LoaderInteractionGameTestsLogic.

## 4. End exit and death copies
`nativePlayerCopiesKeepEyesOnlyWhileAlive` in the same three files: Fabric `ServerPlayerEvents.COPY_FROM` invoker, Forge `PlayerEvent.Clone.BUS.post`, NeoForge `NeoForge.EVENT_BUS.post`; registrations and JSONs.

## 5. Death-harvest tests
SomeGooglyGameTestsLogic copied whole: real kills with `hurtServer(..., Float.MAX_VALUE)`, a fresh cow per case, `eyeDrops` helper; **Correctness 5's thorns case kept** in `deathHarvestRejectsNonqualifyingKills`; `realShearsKillsHarvestAnEye` checks iris color and one durability per harvest for adult and calf. `deathHarvestUsesTheSuppliedDropSink` removed with its 3 wrappers, 2 registrations, 2 JSONs.

## 6. `/sg admin` dispatcher tests
EyeStateOverrideGameTestsLogic copied whole (CRLF): `adminAppearanceCommands`, `adminCommandsRefuseUnauthorizedOrInvalidUse`, `adminEyesAndBehaviorCommands`; the six mutator tests and Dead-code's `hasOverrideCompound` gone. TestPlayers copied whole: `create(template, creative, PermissionSet)` overriding `permissions()`, `GAMEMASTER`, `NO_PERMISSIONS`. EyeStateOverrideGameTests copied on all loaders (CRLF). 12 registrations/JSONs removed and 6 added across Forge and NeoForge.

## 7. Eligibility through spawns
EligibilityGameTestsLogic copied whole: `canEverWearEyesSpansBothAgeBuckets` spawn-based; `slimyEyeAppliesOnlyToEligibleTargets` gains the disabled-definition case; new class javadoc. `usablePredicateEndpoints` and `eligibilityFollowsTheSharedPredicate` removed; loader EligibilityGameTests copied (CRLF); 4 registrations and 4 JSONs gone.

## GameTest count
Removed 9 (`deathHarvestUsesTheSuppliedDropSink`, `usablePredicateEndpoints`, `eligibilityFollowsTheSharedPredicate`, six EyeState mutator tests); added 5 (three admin tests, `nativePlayerCopiesKeepEyesOnlyWhileAlive`, `nativeUseHooksHonorResults`). Net −4 per loader: Fabric 190 → 186, Forge 193 → 189, NeoForge 193 → 189. Forge/NeoForge 189 registrations match 189 test_instance JSONs one to one, and every registered method exists.

## Files copied whole
Common gametest BehaviorSchedulerGameTestsLogic, SomeGooglyGameTestsLogic, EyeStateOverrideGameTestsLogic, EligibilityGameTestsLogic, TestPlayers, LoaderInteractionGameTestsLogic (new); loader SomeGooglyGameTests (LF), EligibilityGameTests and EyeStateOverrideGameTests (CRLF); Forge/NeoForge SomeGooglyGameTestMod; 5 new JSONs per loader (CRLF); 9 JSONs deleted per loader.

## Deviations and work left
CRLF kept where 26.1's copies are LF. GUI/hand/boat/framed test-logic javadoc rewrites left for Javadocs (26.1.1's review-Javadocs.md claims them). Generated docs/javadoc still names removed tests until regenerated.

## First build settles
Forge 63: `PlayerEvent.Clone(Player, Player, boolean)`, `EntityInteractSpecific(Player, InteractionHand, Entity, Vec3)`, `RightClickBlock`/`RightClickItem` constructors, `CancellableEventBus.addListener(Priority, Predicate)`/`removeListener`, `post` returning cancellation, `getCancellationResult()`. NeoForge 26.1.1.15-beta: `EVENT_BUS.addListener(EventPriority, boolean, Class, Consumer)`/`unregister` and event constructors. Fabric API 0.145.4: `COPY_FROM.copyFromPlayer` and use-callback invokers. Minecraft: overridable `ServerPlayer.permissions()`; `heal`, `notifyTrade`, `hurtServer` reach loader hooks on mock/fake players; `Entity.snapTo(BlockPos, float, float)`.

## In-game checks
Eyed cows swirl on heal and villagers on trade; cows grow on milking; each config switch stops its reaction; shears and Slimy Eye inside a refusing claim change nothing; eyes survive leaving the End and are gone after death; a shears kill drops one eye in the iris color and costs one durability; `/sg admin tint/glow/eyes/behavior` works for a creative operator and is refused otherwise.
