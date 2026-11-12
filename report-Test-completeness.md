# Report: Test-completeness (26.1)

Applied 2026-11-11 by hand-porting 1.21.11's changes. Nothing was compiled, built, or tested; everything here comes from reading the source. Each file was diffed against 1.21.11 (CRLF/LF normalized) and only this topic's hunks taken. Copied whole (differed only by this topic): SomeGooglyGameTestsLogic, EligibilityGameTestsLogic, EyeStateOverrideGameTestsLogic, TestPlayers, LoaderInteractionGameTestsLogic (new), the three loaders' EyeStateOverrideGameTests and EligibilityGameTests, Fabric and NeoForge SomeGooglyGameTests, the new test_instance JSONs; Forge SomeGooglyGameTests copied then changed for Forge 62. Fix lines in review-Test-completeness.md are filled in.

**Incident:** a python edit truncated both SomeGooglyGameTestMod files (Forge, NeoForge). Each was rebuilt from 1.21.11's, dropping its Heads registrations (2 Forge, 4 NeoForge) and restoring `configgametests/reload_replaces_configs_only_on_content_change` at 26.1's position. The parent checked afterwards: on each loader the 189 registrations match the 189 test_instance JSONs one to one, and every registered method exists in its wrapper class.

## 2. Heal, trade, hit, and milking tests call the scheduler
BehaviorSchedulerGameTestsLogic: `disabledEyesSuspendAutomaticBehaviors` uses `milk(player, cow)` and restores the held item (its broken javadoc fragment and duplicate collapsed into one); `eyelessTrackedMobIgnoresGameEvents` uses `cow.heal(1F)` and a real trade with an eyeless tracked villager; `gameEventTriggersRespectConfig` uses an eyed cow and villager, `cow.heal`, `trade(player, villager)`, `cow.hurtServer(level, playerAttack(player), 1F)`; heal-cooldown, full-health, sleeping-villager, and completed-trade tests heal and trade for real; new private `trade(ServerPlayer, Villager)`. No direct `onMilked`/`onHealed`/`onTrade`/`onHurt` call remains. Reinvention's `after(...)` structure kept. Remaining differences from 1.21.11: 26.1's `interact(..., Vec3.ZERO)`/`interactOn(..., Vec3.ZERO)`, the `trigger(cow, BLINK, 4L)` seed, two javadocs 1.21.11 lacks.

## 3. Item use tested below the loader event
- New LoaderInteractionGameTestsLogic (1.21.11's unchanged; jspecify `@Nullable`; `TestPlayers.create(template, false, TestPlayers.NO_PERMISSIONS)`).
- `nativeUseHooksHonorResults`: Fabric invokes `UseBlockCallback`, `UseEntityCallback`, `UseItemCallback`. Forge (deviation): every entity target posts `PlayerInteractEvent.EntityInteractSpecific(player, MAIN_HAND, target, Vec3.ZERO)`, since Forge 62 has no plain `EntityInteract` and 26.1's ForgeServerEvents listens only to `EntityInteractSpecific` (commented); `RightClickBlock`/`RightClickItem` as 1.21.11; refusal through a temporary `Priority.HIGHEST` predicate listener. NeoForge: 1.21.11's wrapper (`EVENT_BUS.addListener(EventPriority.HIGHEST, false, type, listener)`/`unregister`).
- Registrations and `test_instance/somegooglygametests/native_use_hooks_honor_results.json`. EyeItemServiceGameTestsLogic class javadoc points to LoaderInteractionGameTestsLogic; `selfRemoveWithShearsDropsAnEyeAndCostsDurability`'s protection paragraph links `LoaderInteractionGameTestsLogic#useHooksHonorResults`.

## 4. End exit / death copies
`nativePlayerCopiesKeepEyesOnlyWhileAlive` on all three loaders (Fabric `COPY_FROM.invoker().copyFromPlayer(...)`, Forge `PlayerEvent.Clone.BUS.post(...)`, NeoForge `NeoForge.EVENT_BUS.post(...)`), registrations and JSONs.

## 5. Death-harvest tests
SomeGooglyGameTestsLogic is 1.21.11's: `deathHarvestRejectsNonqualifyingKills` uses a fresh cow per case (sword, eyeless, mob, arrow, thorns, chance, definition, disabled, loot); `realShearsKillsHarvestAnEye` kills an adult and a calf, one eye each in its iris color, one durability per harvest; new `eyeDrops` helper; `GameRules.MOB_DROPS` get/set as in 26.1. `deathHarvestUsesTheSuppliedDropSink` removed with its wrappers, registrations, JSONs.

## 6. `/sg admin` dispatcher tests
EyeStateOverrideGameTestsLogic: `adminAppearanceCommands`, `adminCommandsRefuseUnauthorizedOrInvalidUse`, `adminEyesAndBehaviorCommands`; the six mutator tests and Dead-code's `hasOverrideCompound` removed with wrappers, registrations, JSONs. TestPlayers gains `create(template, creative, PermissionSet)` overriding `permissions()`, `GAMEMASTER = Permissions.COMMANDS_GAMEMASTER::equals`, `NO_PERMISSIONS`; the two-argument `create` stays. Command source `.withPermission(TestPlayers.GAMEMASTER).withSuppressedOutput()` (26.1 already uses `withPermission(Permissions.COMMANDS_GAMEMASTER::equals)` elsewhere).

## 7. Eligibility through spawns
EligibilityGameTestsLogic is 1.21.11's: spawn-based `canEverWearEyesSpansBothAgeBuckets` (NATURAL cow, `snapTo`, `addFreshEntity` under a 100% override; aging keeps the roll; missing or disabled definition never wins); `slimyEyeAppliesOnlyToEligibleTargets` gains the disabled-definition case; class javadoc. `usablePredicateEndpoints` and `eligibilityFollowsTheSharedPredicate` removed with wrappers, registrations, JSONs.

## GameTests (same names on every loader)
Removed (9): `deathHarvestUsesTheSuppliedDropSink`, `usablePredicateEndpoints`, `eligibilityFollowsTheSharedPredicate`, `clearingTintRemovesOnlyThatField`, `glowOverrideCanBeSetAndDropped`, `settingOneFieldLeavesOthersAbsent`, `clearingEveryFieldRemovesTheCompound`, `clearingTintsPreservesGlow`, `snapshotAppliesAllFieldsTogether`. Added (5): `adminAppearanceCommands`, `adminCommandsRefuseUnauthorizedOrInvalidUse`, `adminEyesAndBehaviorCommands`, `nativePlayerCopiesKeepEyesOnlyWhileAlive`, `nativeUseHooksHonorResults`. Net −4 per loader; after this topic: Fabric 186, Forge 189, NeoForge 189.

## Recalled API facts (unverified)
`ServerPlayer.permissions()` is overridable and read by PickerGate and `Commands.hasPermission`; `LivingEntity.heal`, `Villager.notifyTrade`, and `hurtServer` reach the loader hooks on mock and fake players; `Entity.snapTo(BlockPos, float, float)`; Forge 62 constructors `PlayerEvent.Clone(Player, Player, boolean)`, `EntityInteractSpecific(Player, InteractionHand, Entity, Vec3)`, `RightClickBlock(...)`, `RightClickItem(...)`, `CancellableEventBus.addListener(Priority, Predicate)`/`removeListener`, `post` returning cancellation, `getCancellationResult()`; NeoForge `EVENT_BUS.addListener(EventPriority, boolean, Class, Consumer)`/`unregister`; Fabric API 0.145.1 `COPY_FROM.copyFromPlayer` and the use-callback invokers.

## In-game checks
Eyed cows swirl on heal and villagers on trade; cows grow on milking; each switch stops its reaction. Slimy Eye and Optometrist shears on a cow, boat, armor stand, placed head, and yourself; inside a refusing claim nothing changes. Applied eyes survive leaving the End and are gone after death. With `harvestOnKillPercent = 100`, a shears kill drops one eye in the iris color and costs one durability; sword, arrow, thorns, and `mob_drops` off drop none. `/sg admin tint/glow/eyes/behavior` work for a creative operator and are refused otherwise. On Forge, posting `EntityInteractSpecific` for a cow and a boat reaches `EyeItemService.interact`.

## Left to other topics
Javadocs: two BehaviorScheduler test javadocs 1.21.11 lacks; any rewording of the self-shear javadoc beyond the link. Generated docs/javadoc still names removed tests until regenerated.

## Clash points
BehaviorScheduler, SomeGooglyGameTests, EyeStateOverride, Eligibility, EyeItemService logic; TestPlayers; Forge/NeoForge SomeGooglyGameTestMod; all loader SomeGooglyGameTests/EyeStateOverrideGameTests/EligibilityGameTests.

## Files modified
Common gametest: BehaviorSchedulerGameTestsLogic, SomeGooglyGameTestsLogic, EyeStateOverrideGameTestsLogic, EligibilityGameTestsLogic, EyeItemServiceGameTestsLogic, TestPlayers, LoaderInteractionGameTestsLogic (new). Loader gametests: SomeGooglyGameTests, EyeStateOverrideGameTests, EligibilityGameTests on all three; SomeGooglyGameTestMod on Forge and NeoForge. test_instance: 5 JSONs added and 9 deleted per Forge-family loader. Docs: review-Test-completeness.md.
