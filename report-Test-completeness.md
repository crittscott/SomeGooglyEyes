# Report: Test-completeness (26.1.2)
Applied findings 2-7 on 2026-11-12, not built. Each 26.1.1 gametest file was diffed against 26.1.2's with `diff --strip-trailing-cr`. A file was copied whole only where the whole remaining diff belonged to this topic. Everywhere else the edits were made by hand. Gradle, git, and gh were not run.

## 2. Heal, trade, hit, and milking tests used the scheduler
The whole BehaviorSchedulerGameTestsLogic diff belonged to this finding, so the file was copied whole from 26.1.1 (LF on both sides). The test now milks with `milk(player, cow)` and restores the held item. It uses `cow.heal(1F)`, `cow.hurtServer(level, playerAttack(player), 1F)`, the `eyedTrackedMob` villager, a real eyeless villager, and the private `trade(ServerPlayer, Villager)` helper. Three duplicated javadocs were collapsed. Reinvention's `after(...)` structure was already identical in both files. No deviation from 26.1.1.

## 3. Item use tested below the loader event
- New LoaderInteractionGameTestsLogic, copied from 26.1.1. It uses `SkullBlockEntity` locally and passes it to 26.1.2's `SkullEyeState.set(BlockEntity, …)` and `SkullEyeState.snapshot(BlockEntity)`, so no change was needed.
- `nativeUseHooksHonorResults` added to the three loaders' SomeGooglyGameTests. Each file was copied whole, because the whole diff was findings 3-5.
- EyeItemServiceGameTestsLogic: only the two javadoc hunks were taken (the class javadoc and the self-shear javadoc). 26.1.2's `player.setShiftKeyDown(false)` line, which comes from another topic, was kept.
- Forge and NeoForge got their registrations and `native_use_hooks_honor_results.json` (copied CRLF).
- 26.1.2's ForgeServerEvents listens only to `EntityInteractSpecific`, matching the Forge wrapper.

## 4. End exit and death copies
`nativePlayerCopiesKeepEyesOnlyWhileAlive` was added in the same three wrapper files:
- Fabric: `ServerPlayerEvents.COPY_FROM` invoker.
- Forge: `PlayerEvent.Clone.BUS.post`.
- NeoForge: `NeoForge.EVENT_BUS.post`.

Registrations and `native_player_copies_keep_eyes_only_while_alive.json` were added on Forge and NeoForge.

## 5. Death-harvest tests
The whole SomeGooglyGameTestsLogic diff belonged to this finding, so the file was copied whole from 26.1.1. The tests now make real kills with `hurtServer(..., Float.MAX_VALUE)`, use a fresh cow per case, and share an `eyeDrops` helper. Correctness 5's thorns case is kept: 26.1.1's copy has it as the `"thorns"` refusal, which uses `sources.thorns(player)`. `realShearsKillsHarvestAnEye` checks iris color and one durability per harvest. `deathHarvestUsesTheSuppliedDropSink` was removed along with its three wrappers, two registrations, and two JSONs.

## 6. `/sg admin` dispatcher tests
- EyeStateOverrideGameTestsLogic copied whole (CRLF on both sides). It now holds `adminAppearanceCommands`, `adminCommandsRefuseUnauthorizedOrInvalidUse`, and `adminEyesAndBehaviorCommands`. The six mutator tests and Dead-code's test-local `hasOverrideCompound` are gone.
- TestPlayers copied whole: `create(template, creative, PermissionSet)`, `GAMEMASTER`, `NO_PERMISSIONS`.
- EyeStateOverrideGameTests copied on all three loaders (CRLF). Forge uses `SomeGooglyGameTests.survivalPlayer`, which exists in 26.1.2.
- On each of Forge and NeoForge: 6 registrations and 6 JSONs removed, 3 and 3 added.

## 7. Eligibility through spawns
- EligibilityGameTestsLogic copied whole. `canEverWearEyesSpansBothAgeBuckets` is now spawn-based, and `slimyEyeAppliesOnlyToEligibleTargets` gains the disabled-definition case.
- `usablePredicateEndpoints` and `eligibilityFollowsTheSharedPredicate` were removed.
- EligibilityGameTests copied on all three loaders (CRLF).
- On each of Forge and NeoForge: 2 registrations and 2 JSONs removed.

## GameTest counts
- **Fabric:** 188 `@GameTest` methods.
- **Forge:** 191 `functions.register("…")` lines and 191 test_instance JSONs.
- **NeoForge:** 197 `functions.register("…")` lines and 197 test_instance JSONs.

Registrations and JSON paths match one to one on both loaders, and every registered `Class::method` exists as a `static void` in its wrapper. 26.1.2's own registrations were kept: the 4 NeoForge `TwilightHeadGameTests`, the 4 NeoForge Heads tests, and the 2 Forge Heads tests. orientation-code.md ("Fabric 188, Forge 191, NeoForge 197") and orientation-build.md ("191 … 197") already stated these counts, so they were not edited. orientation-code.md stays at 11,995 characters. build-env/orientation-build.md and build-env/orientation-code.md state no counts.

## Left for other topics / the parent
- Javadocs: the remaining doc-only hunks in BoatEye, FramedEye, GuiItemEye, HandItemEye, PickerExport, PickerSpawnService, and VersionRange GameTestsLogic.
- PickerExport's `SomeGooglyCommon.id` hunk is not this topic's.
- The generated docs/javadoc still names the removed tests until it is regenerated.
- report-Dead-code.md:48 already notes that this topic removes `hasOverrideCompound`.

## Recalled API facts and checks only a build or in-game test can settle
- **Forge 64:** `PlayerEvent.Clone(Player, Player, boolean)`, `EntityInteractSpecific(Player, InteractionHand, Entity, Vec3)`, and the `RightClickBlock`/`RightClickItem` constructors and `.BUS` fields; `CancellableEventBus.addListener(Priority, Predicate)`/`removeListener`; `post` returning cancellation; `getCancellationResult()`. All assumed unchanged from Forge 62/63.
- **NeoForge 26.1.2.112:** `EVENT_BUS.addListener(EventPriority, boolean, Class, Consumer)`/`unregister` and the event constructors.
- **Fabric API 0.155.3+26.1.2:** `COPY_FROM.copyFromPlayer` and the use-callback invokers.
- **Minecraft:** `ServerPlayer.permissions()` can be overridden; `heal`, `notifyTrade`, and `hurtServer` reach the loader hooks on mock and fake players; `Entity.snapTo(BlockPos, float, float)` exists; a `SkullBlockEntity` passes where `SkullEyeState` takes a `BlockEntity`.

## In-game checks
- Eyed cows swirl on heal, and villagers on trade.
- Cows grow on milking.
- Each config switch stops its reaction.
- Shears and a Slimy Eye inside a refusing claim change nothing.
- Eyes survive leaving the End and are gone after death.
- A shears kill drops one eye in the iris color and costs one durability.
- `/sg admin tint/glow/eyes/behavior` works for a creative operator and is refused for anyone else.

## Fix lines
2: BehaviorSchedulerGameTestsLogic copied whole from 26.1.1 (its whole diff was this finding): real milking with the held item restored, `cow.heal(1F)`, real trades through `trade(ServerPlayer, Villager)` with eyed and eyeless villagers, `cow.hurtServer(level, playerAttack(player), 1F)`, duplicated javadocs collapsed; no direct `onMilked`/`onHealed`/`onTrade`/`onHurt` call remains. Unbuilt.
3: New LoaderInteractionGameTestsLogic, copied from 26.1.1 (its `SkullBlockEntity` passes to 26.1.2's `BlockEntity` `SkullEyeState` API unchanged); `nativeUseHooksHonorResults` in the three loaders' SomeGooglyGameTests (copied whole); Forge/NeoForge registrations and `native_use_hooks_honor_results.json`; EyeItemServiceGameTestsLogic's class and self-shear javadocs point to LoaderInteractionGameTestsLogic (26.1.2's `setShiftKeyDown(false)` line kept). Forge 64 is assumed to keep Forge 62/63's event constructors. Unbuilt.
4: `nativePlayerCopiesKeepEyesOnlyWhileAlive` in the same three SomeGooglyGameTests (Fabric `COPY_FROM` invoker, Forge `PlayerEvent.Clone.BUS.post`, NeoForge `NeoForge.EVENT_BUS.post`); Forge/NeoForge registrations and `native_player_copies_keep_eyes_only_while_alive.json`. Unbuilt.
5: SomeGooglyGameTestsLogic copied whole from 26.1.1: real kills with `hurtServer(..., Float.MAX_VALUE)` on a fresh cow per case, `eyeDrops` helper, Correctness 5's thorns case kept in `deathHarvestRejectsNonqualifyingKills`, iris and durability checked per harvest in `realShearsKillsHarvestAnEye`; `deathHarvestUsesTheSuppliedDropSink` removed with its three wrappers, two registrations, and two JSONs. Unbuilt.
6: EyeStateOverrideGameTestsLogic (CRLF) and TestPlayers copied whole from 26.1.1: three `/sg admin` dispatcher tests replace the six mutator tests, Dead-code's `hasOverrideCompound` removed; EyeStateOverrideGameTests copied on all three loaders; 12 registrations and 12 JSONs removed, 6 and 6 added. Unbuilt.
7: EligibilityGameTestsLogic copied whole from 26.1.1: spawn-based `canEverWearEyesSpansBothAgeBuckets`, disabled-definition case in `slimyEyeAppliesOnlyToEligibleTargets`, new class javadoc; `usablePredicateEndpoints` and `eligibilityFollowsTheSharedPredicate` removed with EligibilityGameTests copied on all three loaders, 4 registrations, and 4 JSONs. Counts now Fabric 188, Forge 191, NeoForge 197, registrations matching JSONs. Unbuilt.
