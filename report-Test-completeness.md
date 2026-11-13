# Report: Test-completeness (26.3)

Applied 2026-11-12 from 26.2's post-fix code (26.2/report-Test-completeness.md exists). Nothing was built or run; everything below is source inspection. After this topic, 26.3's Fabric, Forge, and NeoForge `src/gametest` trees are identical to 26.2's; in common gametest, each file this topic touches matches 26.2's except for 26.3's API differences in BehaviorSchedulerGameTestsLogic.

## 2. Heal, trade, hit, and milking config tests call the scheduler
- BehaviorSchedulerGameTestsLogic taken from 26.2 (after the other topics its diff held only this finding plus 26.3's API changes): `disabledEyesSuspendAutomaticBehaviors` milks with `milk(player, cow)` and restores the held item; `eyelessTrackedMobIgnoresGameEvents` heals with `cow.heal(1F)` and trades with a new eyeless tracked villager through `trade(player, villager)`; `gameEventTriggersRespectConfig` uses `eyedTrackedMob` for a cow and a villager, `cow.heal(1F)`, `trade(player, villager)`, and `cow.hurtServer(level, playerAttack(player), 1F)`; the heal-cooldown, full-health, and sleeping-villager tests heal for real; the sleeping-villager and completed-trade tests trade through the new private `trade(ServerPlayer, Villager)`; duplicated javadocs on `disabledEyesSuspendAutomaticBehaviors`, `playerHolding`, and `receivedExpressionsOutliveRenderTrackers` collapsed. No direct `onMilked`/`onHealed`/`onTrade`/`onHurt` call remains; Reinvention's `after(...)` structure kept.
- 26.3 adaptations: `Enderman`/`enderman` kept; the bed-sleep test keeps `helper.assertTrue(sleeper.startSleeping(head), …)` and its "vanilla sleep requires a bed" javadoc.

## 3. Item use tested below the loader event
- New common LoaderInteractionGameTestsLogic (from 26.2) with `playerCopiesKeepEyesOnlyWhileAlive` and `useHooksHonorResults`.
- `nativeUseHooksHonorResults` in each loader's SomeGooglyGameTests: Fabric invokes `UseBlockCallback`, `UseEntityCallback`, `UseItemCallback` (no refusal case); Forge posts `RightClickBlock`, `EntityInteractSpecific` for every entity, and `RightClickItem`, refused by a temporary `Priority.HIGHEST` predicate listener; NeoForge posts `RightClickBlock`, `EntityInteract` for every entity including the armor stand (`(Player, InteractionHand, Entity, Vec3.ZERO)`), and `RightClickItem`, with a temporary HIGHEST listener.
- Forge and NeoForge registrations and `native_use_hooks_honor_results.json` added.
- EyeItemServiceGameTestsLogic copied from 26.2: class javadoc points to LoaderInteractionGameTestsLogic and says Fabric's protection-listener ordering is checked by hand; the self-shear javadoc links `LoaderInteractionGameTestsLogic#useHooksHonorResults`.
- 26.2's code already reflects Forge 66 (`EntityInteractSpecific` only) and NeoForge 26.3 (`EntityInteract` for armor stands); no code adaptation.

## 4. End exit and death copies
`nativePlayerCopiesKeepEyesOnlyWhileAlive` in each loader's SomeGooglyGameTests (Fabric `ServerPlayerEvents.COPY_FROM` invoker, Forge `PlayerEvent.Clone.BUS.post`, NeoForge `NeoForge.EVENT_BUS.post`), shared logic in LoaderInteractionGameTestsLogic; Forge and NeoForge registrations and `native_player_copies_keep_eyes_only_while_alive.json` added.

## 5. Death-harvest tests
- SomeGooglyGameTestsLogic copied from 26.2 (its diff was only this finding): `deathHarvestRejectsNonqualifyingKills` uses real kills (`hurtServer(..., Float.MAX_VALUE)` on a fresh cow per refusal: sword, eyeless, mob, arrow, thorns, chance, definition, disabled, loot), keeping Correctness 5's thorns case, and asserts no durability is spent; `realShearsKillsHarvestAnEye` checks iris color and one durability per harvest for an adult and a calf; new `eyeDrops` helper; `deathHarvestUsesTheSuppliedDropSink` removed; unused imports dropped, `LivingEntity` added.
- The sink test's 3 loader wrappers, 2 registrations, and 2 JSONs removed.

## 6. `/sg admin` dispatcher tests
- EyeStateOverrideGameTestsLogic copied from 26.2: `adminAppearanceCommands`, `adminCommandsRefuseUnauthorizedOrInvalidUse`, `adminEyesAndBehaviorCommands`, private `command` and `target` helpers. The six mutator tests and Dead-code's `hasOverrideCompound` are gone.
- TestPlayers copied from 26.2: keeps `create(template, creative)`, adds `create(template, creative, PermissionSet)` overriding `permissions()`, `GAMEMASTER`, `NO_PERMISSIONS`.
- Loader EyeStateOverrideGameTests copied from 26.2 (Fabric `FakePlayer.get`, Forge `SomeGooglyGameTests.survivalPlayer`, NeoForge `FakePlayerFactory.getMinecraft`). Forge and NeoForge per loader: 6 registrations and JSONs removed, 3 added.

## 7. Eligibility through spawns
- EligibilityGameTestsLogic copied from 26.2: `canEverWearEyesSpansBothAgeBuckets` is spawn-based (NATURAL `create`, `setBaby`, `addFreshEntity` under a 100% override; aging keeps the roll; a missing or disabled definition never wins); `slimyEyeAppliesOnlyToEligibleTargets` gains a disabled-definition case; new class javadoc; `usablePredicateEndpoints` and `eligibilityFollowsTheSharedPredicate` removed.
- Loader EligibilityGameTests copied from 26.2; Forge and NeoForge: 2 registrations and JSONs removed per loader.

## GameTest count
- This topic: 9 removed and 5 added per loader, net −4 on Fabric, Forge, and NeoForge.
- Earlier topics: +1 each for Expected-way, Logging, Loader-divergence, and Organization. Summed from the recorded 185/187/187 baseline that gives 185/187/187.
- The registrations disagree with that sum: they now equal 26.2's and 26.1.1's (Forge and NeoForge 189 `test_instance` JSONs, Fabric 185 `@GameTest` lines), and 26.2's review and 26.3's orientation-code.md expect 186/189/189. Either the baseline or a recorded delta is off; the first run settles it.

## Recalled API facts (first build settles)
- Forge 66 keeps Forge 62/63's `PlayerEvent.Clone(Player, Player, boolean)`, `EntityInteractSpecific(Player, InteractionHand, Entity, Vec3)`, `RightClickBlock(Player, InteractionHand, BlockPos, BlockHitResult)`, `RightClickItem(Player, InteractionHand)`, `CancellableEventBus.addListener(Priority, Predicate)`/`removeListener`, `post` returning whether cancelled, and `getCancellationResult()`.
- NeoForge 26.3.0.76 keeps 26.2.0.57's `EntityInteract(Player, InteractionHand, Entity, Vec3)`, `RightClickBlock`/`RightClickItem` constructors, `PlayerEvent.Clone(Player, Player, boolean)`, `EVENT_BUS.addListener(EventPriority, boolean, Class, Consumer)`/`unregister`, and `getCancellationResult()`.
- Fabric API 0.163.0 keeps `ServerPlayerEvents.COPY_FROM.copyFromPlayer` and the use-callback invoker signatures.
- Minecraft 26.3: `ServerPlayer.permissions()` is overridable; `PermissionSet` and `Permissions.COMMANDS_GAMEMASTER` exist; `heal`, `notifyTrade`, and `hurtServer` reach each loader's hook on mock and fake players; `Entity.snapTo(BlockPos, float, float)` and `lookAt(EntityAnchorArgument.Anchor, Vec3)` exist; `EntityTypes.OAK_BOAT`, `ARMOR_STAND`, `ARROW` exist.

## In-game checks
- Eyed cows swirl when healed and villagers swirl when traded with; cows grow when milked; each config switch stops its reaction.
- Shears and a Slimy Eye used inside a claim that refuses the click change nothing.
- On NeoForge, armor stands still take and give eyes through `EntityInteract`.
- Eyes survive leaving the End and are gone after death.
- A shears kill drops one eye in the mob's iris color and costs one durability.
- `/sg admin tint`, `glow`, `eyes`, and `behavior` work for a creative operator and are refused otherwise.

## Notes
- This topic already rewrote EyeItemServiceGameTestsLogic's class and self-shear javadocs and collapsed the duplicated javadocs in BehaviorSchedulerGameTestsLogic.
- Generated `docs/javadoc` still names the removed tests until regenerated. No orientation or docs text named the changed tests.
