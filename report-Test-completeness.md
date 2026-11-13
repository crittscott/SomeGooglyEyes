# Report: Test-completeness (26.2)

Applied 2026-11-12 from 26.1.1's post-fix code and report; 26.1.2 had no report for this topic. Nothing built or run; every claim below is from source inspection. Fix lines in review-Test-completeness.md filled in.

## 2. Heal, trade, hit, and milking config tests call the scheduler
- common gametest BehaviorSchedulerGameTestsLogic.java: `disabledEyesSuspendAutomaticBehaviors` milks with `milk(player, cow)` and restores the held item; `eyelessTrackedMobIgnoresGameEvents` heals the eyeless cow for real and trades with a new eyeless tracked villager; `gameEventTriggersRespectConfig` uses `eyedTrackedMob` for a cow and a villager, `cow.heal(1F)`, `trade(player, villager)`, and `cow.hurtServer(level, playerAttack(player), 1F)`; `healSwirlIsRateLimited`, `healAtFullHealthPlaysNoSwirl`, and `sleepingMobPlaysNoBehaviors` heal for real; `sleepingMobPlaysNoBehaviors` and `completedTradeSwirlsEyes` trade through the new private `trade(ServerPlayer, Villager)`. Duplicated javadocs on `disabledEyesSuspendAutomaticBehaviors`, `playerHolding`, and `receivedExpressionsOutliveRenderTrackers` collapsed.
- Deviation: hunks taken by hand rather than the file copied, because Reinvention was converting the same tests from `advance(...)` to `after(...)` at the same time. After both landed, this topic's lines match 26.1.1's.

## 3. Item use tested below the loader event
- New common gametest LoaderInteractionGameTestsLogic.java (26.1.1's, with `EntityTypes.COW/OAK_BOAT/ARMOR_STAND`).
- `nativeUseHooksHonorResults` added to fabric, forge, and neoforge SomeGooglyGameTests.java. Fabric invokes `UseBlockCallback`/`UseEntityCallback`/`UseItemCallback`, with no refusal case. Forge posts `RightClickBlock`, `EntityInteractSpecific` for every entity, and `RightClickItem`, refused by a temporary `Priority.HIGHEST` predicate listener; 26.2's ForgeServerEvents listens only to `EntityInteractSpecific`.
- Deviation: NeoForge posts `EntityInteract` for every entity target, armor stand included, and drops 26.1.1's `EntityInteractSpecific` branch and its `ArmorStand`/`Vec3` imports: NeoForge 26.2.0.57 has no `EntityInteractSpecific`, and NeoForgeServerEvents handles every entity through `EntityInteract`.
- NeoForge 26.2's `EntityInteract` constructor is `(Player, InteractionHand, Entity, Vec3)` (owner-supplied compile error); the test passes `Vec3.ZERO`, as 26.1.1's `EntityInteractSpecific` call did.
- Forge/NeoForge SomeGooglyGameTestMod registration and `native_use_hooks_honor_results.json` added.
- EyeItemServiceGameTestsLogic's class javadoc points to LoaderInteractionGameTestsLogic. The self-shear javadoc had already been rewritten by Javadocs.

## 4. End exit and death copies
`nativePlayerCopiesKeepEyesOnlyWhileAlive` added to the three SomeGooglyGameTests.java: Fabric `ServerPlayerEvents.COPY_FROM` invoker, Forge `PlayerEvent.Clone.BUS.post`, NeoForge `NeoForge.EVENT_BUS.post`. Forge/NeoForge registrations and `native_player_copies_keep_eyes_only_while_alive.json` added.

## 5. Death-harvest tests
- common gametest SomeGooglyGameTestsLogic.java: `deathHarvestRejectsNonqualifyingKills` and `realShearsKillsHarvestAnEye` taken from 26.1.1 (`EntityTypes` constants), with the `eyeDrops` helper; the thorns case is kept. `deathHarvestUsesTheSuppliedDropSink` removed. Unused `EyeItemService`, `Arrow`, and `ArrayList` imports dropped; `LivingEntity` import added. The file now differs from 26.1.1 only in `EntityType`/`EntityTypes`.
- Removed the 3 loader wrappers, 2 registrations, and 2 JSONs.

## 6. `/sg admin` dispatcher tests
- common gametest EyeStateOverrideGameTestsLogic.java replaced by 26.1.1's (`EntityTypes.COW`): `adminAppearanceCommands`, `adminCommandsRefuseUnauthorizedOrInvalidUse`, `adminEyesAndBehaviorCommands`. The six mutator tests and `hasOverrideCompound` are gone.
- New common gametest TestPlayers.java from 26.1.1, with both `create(template, creative)` and `create(template, creative, PermissionSet)`, plus `GAMEMASTER` and `NO_PERMISSIONS`.
- Loader EyeStateOverrideGameTests.java (fabric, forge, neoforge) taken from 26.1.1, CRLF kept.
- Forge/NeoForge, per loader: 6 registrations and 6 JSONs removed; 3 and 3 added.

## 7. Eligibility through spawns
- common gametest EligibilityGameTestsLogic.java taken from 26.1.1 (`EntityTypes` constants): spawn-based `canEverWearEyesSpansBothAgeBuckets`, the disabled-definition case in `slimyEyeAppliesOnlyToEligibleTargets`, and a new class javadoc. `usablePredicateEndpoints` and `eligibilityFollowsTheSharedPredicate` removed.
- Loader EligibilityGameTests.java taken from 26.1.1 (CRLF). Forge/NeoForge: 2 registrations and 2 JSONs removed per loader.

## GameTest count
Removed 9 per loader (`deathHarvestUsesTheSuppliedDropSink`, `usablePredicateEndpoints`, `eligibilityFollowsTheSharedPredicate`, the six EyeState mutator tests); added 5 per loader (the three admin tests, `nativePlayerCopiesKeepEyesOnlyWhileAlive`, `nativeUseHooksHonorResults`). Net −4 per loader on Fabric, Forge, and NeoForge, not counting other topics' additions.

## Recalled API facts (first build settles)
- Forge 65 keeps the `PlayerEvent.Clone(Player, Player, boolean)`, `EntityInteractSpecific(Player, InteractionHand, Entity, Vec3)`, `RightClickBlock`, and `RightClickItem` constructors; `CancellableEventBus.addListener(Priority, Predicate)`/`removeListener`; `post` returning whether the event was cancelled; `getCancellationResult()`.
- NeoForge 26.2.0.57 has `EVENT_BUS.addListener(EventPriority, boolean, Class, Consumer)`/`unregister`, `PlayerInteractEvent.EntityInteract(Player, InteractionHand, Entity)`, the `RightClickBlock`/`RightClickItem` constructors, `PlayerEvent.Clone(Player, Player, boolean)`, and `getCancellationResult()`.
- Fabric API 0.161.0 provides `ServerPlayerEvents.COPY_FROM.copyFromPlayer` and the use-callback invoker signatures.
- Minecraft 26.2: `ServerPlayer.permissions()` can be overridden; `PermissionSet` and `Permissions.COMMANDS_GAMEMASTER` exist (26.2's PickerGate uses them); `heal`, `notifyTrade`, and `hurtServer` reach each loader's hook on mock and fake players; `Entity.snapTo(BlockPos, float, float)` and `lookAt(EntityAnchorArgument.Anchor, Vec3)` exist; `EntityTypes.OAK_BOAT`, `ARMOR_STAND`, and `ARROW` exist.

## In-game checks
Eyed cows swirl when healed and villagers when traded with; cows grow when milked; each config switch stops its reaction. Shears and a Slimy Eye used inside a claim that refuses the click change nothing. On NeoForge, armor stands still take and give eyes through `EntityInteract`. Eyes survive leaving the End and are gone after death. A shears kill drops one eye in the mob's iris color and costs one durability. `/sg admin tint/glow/eyes/behavior` works for a creative operator and is refused otherwise.

## Clashes
BehaviorSchedulerGameTestsLogic was rewritten by Reinvention at the same time; both topics' edits are in the file. Organization 1 also needs TestPlayers' two-argument `create`. The generated `docs/javadoc` still names the removed tests until regenerated.
