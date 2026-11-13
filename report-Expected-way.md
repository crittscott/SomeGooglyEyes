# Report: Expected-way (26.2)

Applied 2026-11-12 by taking 26.1.1's hunks for this topic only, using Edit, while other topics edited 26.2 at the same time. Nothing was compiled, built, or tested. Fix lines in review-Expected-way.md filled in.

## 1. Optometrist can show up on random looted or fished gear
- common gametest EyeItemServiceGameTestsLogic: `optometristRollsOnlyOntoShearsAndBooks` (before the self-remove test, as in 26.1.1) and private helper `rollsOptometrist`; imports `TagKey`, `EnchantmentHelper`, `EnchantmentInstance`, `Stream`.
- Fabric EyeItemServiceGameTests: `@GameTest(structure = TEMPLATE, maxTicks = 40)` wrapper. Forge and NeoForge EyeItemServiceGameTests: static wrappers. Both SomeGooglyGameTestMod files: `functions.register("eyeitemservicegametests/optometrist_rolls_only_onto_shears_and_books", ...)` after `optometrist_interact_passes_when_not_applicable`.
- New: Forge and NeoForge `src/gametest/resources/data/somegoogly/test_instance/eyeitemservicegametests/optometrist_rolls_only_onto_shears_and_books.json`, copied from 26.1.1 (the neighboring JSONs are identical between the two checkouts).
- No deviations. No production change; the `max_cost` question stays open.
- Recalled, unverified: `EnchantmentHelper.getAvailableEnchantmentResults(int, ItemStack, Stream<Holder<Enchantment>>)`; `EnchantmentInstance.enchantment()` returns `Holder<Enchantment>`; `lookupOrThrow(...).getOrThrow(TagKey).stream()`; `Holder.is(ResourceKey)`. None of these is among 26.2's known renames.

## 3. Fabric config files are written as hand-built TOML text
fabric config/fabric/TomlConfig.java: `render` builds a `CommentedConfig` (`TomlFormat.newConfig(LinkedHashMap::new)`), calls `set(path, default)` and `setComment(path, " " + comment with "\n" -> "\n ")` for each setting, and returns `new TomlWriter().writeToString(config)`. `stringList` and `table` are deleted. All needed imports were already present. No source references remain; only the generated docs/javadoc/fabric HTML still mentions them. The `assign` hunk is left to Loader-divergence 9. Recalled NightConfig behavior: `set(List<String>, value)` creates intermediate tables; `setComment` writes one `#` line per comment line; `TomlWriter` quotes table names that are not bare keys. Only newly created Fabric config files change layout. No deviations.

## 4, 8
No change.

## 7. The sneak-inspect hint uses its own 16-block ray
- common client/EyeInspector.java (by hunks): `tick` reads `minecraft.hitResult`. An `EntityHitResult` whose entity is living or `BoatEyeLayout.supports` gets its verdict; otherwise a `BlockHitResult` on a `SkullBlockEntity` gets the head verdict. `REACH`, the `LookTarget` call and its import are removed; the `Entity` import is added; the `tick` javadoc matches 26.1.1; `sendOverlayMessage` kept. The order of the `registry.ModContent` and `item.EyeItemService` imports still differs from 26.1.1 (cosmetic, not this finding).
- common util/LookTarget.java: the class javadoc names only the admin and picker spawn verbs. The predicate-taking `hitInCrosshair` is folded into `livingHitInCrosshair` (javadoc and `@Nullable` kept); `Predicate` and `Entity` imports dropped. `livingInCrosshair` still calls `livingHitInCrosshair`; the only remaining difference from 26.1.1 is Dead-code 9's inlining.
- orientation-code.md:87 "the inspector reads the game's crosshair target."; orientation-player.md:86 "The check answers for the crosshair target within reach."
- No deviation: the head test is `SkullBlockEntity`, as in 26.1.1; 26.1.2's `SkullEyeState.Holder` test does not apply.
- Recalled: `Minecraft.hitResult` is still a public field in 26.2 and is the client's crosshair pick, limited to interaction range.
- In-game check: the hint appears only within interaction reach, for mobs, boats, and placed heads.

## 9. `/sg mob move` and `/sg mob rot` don't broadcast to operators
common picker/PickerGate.java: `tellSuccess(source, broadcast, key, args)` with 26.1.1's javadoc. GooglyServerCommands mob move and mob rot pass `true`; PickerSpawnService:411 (spawnall dropped header) passes `false`. There are no other callers. `announce` is left to Organization 10. No deviations.
- In-game check: a second operator and the console see the move and rot feedback.

## Left for other topics (seen in the diffs)
- LookTarget: inlining `livingHitInCrosshair` into `livingInCrosshair` (Dead-code 9).
- TomlConfig `assign` (Loader-divergence 9).
- `PickerSpawnService.announce` using `tellSuccess` (Organization 10).
- `ServerEyeConfigs.isExcluded` at PickerSpawnService:122.
- `EyePlacement.AZIMUTH_TO_YAW_OFFSET` and `Identifier.tryBuild` in GooglyServerCommands.
- EyeItemServiceGameTestsLogic: the class javadoc, the protection-test javadoc, and the `setShiftKeyDown(false)` line.
- Other registration differences in the SomeGooglyGameTestMod files.

## Possible clashes
- The SomeGooglyGameTestMod files, GooglyServerCommands and orientation-code.md changed on disk during this work (other topics). Each new register line is present once and the `tellSuccess` and orientation edits are still in place.
- `tellSuccess` now takes a `broadcast` flag, so any other topic's 3-argument call (e.g. Organization 10's `announce`) must pass the flag.

## GameTests
+1 per loader (Fabric, Forge, NeoForge: `optometristRollsOnlyOntoShearsAndBooks`); none removed.

## First build settles
- The new test's recalled enchantment API signatures.
- The TomlConfig rewrite.
- `minecraft.hitResult` in 26.2.
- The `tellSuccess` signature at all three callers.
