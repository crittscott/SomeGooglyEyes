# Report: Expected-way (26.3)

Applied 2026-11-12 from 26.2's post-fix hunks (26.2 has written report-Expected-way.md), taking only this topic's hunks. Nothing was compiled, built, or tested; everything below is source inspection. Fix lines in review-Expected-way.md filled in. None of the touched files carries a 26.3 API difference, so no 26.3 adaptation was needed.

## 1. Optometrist can show up on random looted or fished gear
- Reference: 26.2.
- common gametest EyeItemServiceGameTestsLogic: added `optometristRollsOnlyOntoShearsAndBooks` (before the self-remove test) and private `rollsOptometrist`; imports `TagKey`, `EnchantmentHelper`, `EnchantmentInstance`, `Stream`. 26.2's class-javadoc and protection-test javadoc changes (other topics) not taken.
- Fabric EyeItemServiceGameTests: `@GameTest(structure = TEMPLATE, maxTicks = 40)` wrapper. Forge and NeoForge EyeItemServiceGameTests: static wrappers. Forge and NeoForge SomeGooglyGameTestMod: `functions.register("eyeitemservicegametests/optometrist_rolls_only_onto_shears_and_books", ...)` after `optometrist_interact_passes_when_not_applicable`. Other registration differences from 26.2 not taken.
- New: Forge and NeoForge `src/gametest/resources/data/somegoogly/test_instance/eyeitemservicegametests/optometrist_rolls_only_onto_shears_and_books.json`, copied from 26.2 (the neighboring JSON is identical between the checkouts).
- No production change; the `max_cost` question stays open.
- Recalled, unverified: `EnchantmentHelper.getAvailableEnchantmentResults(int, ItemStack, Stream<Holder<Enchantment>>)`, `EnchantmentInstance.enchantment()` returning `Holder<Enchantment>`, `lookupOrThrow(...).getOrThrow(TagKey).stream()`, `Holder.is(ResourceKey)`. None is among 26.3's known API changes.

## 3. Fabric config files are written as hand-built TOML text
- Reference: 26.2.
- fabric config/fabric/TomlConfig.java: `render` builds a `CommentedConfig` (`TomlFormat.newConfig(LinkedHashMap::new)`), sets each default and comment by `[section, key]` path, and returns `new TomlWriter().writeToString(config)`. `stringList` and `table` deleted; no source references remain. Imports were already present.
- `assign` hunk left to Loader-divergence 9.
- Recalled NightConfig behavior: `set(List<String>, value)` creates intermediate tables; `setComment` writes one `#` line per comment line; `TomlWriter` quotes non-bare table names. Only newly created Fabric config files change layout.

## 4, 8
No change.

## 7. The sneak-inspect hint uses its own 16-block ray
- Reference: 26.2.
- common client/EyeInspector.java: now identical to 26.2's post-fix file (26.3's file differed from it only by this finding). `tick` reads `minecraft.hitResult`: an `EntityHitResult` on a living entity or `BoatEyeLayout.supports` entity gets its verdict; a `BlockHitResult` on a `SkullBlockEntity` gets the head verdict. `REACH`, the `LookTarget` call and import removed; `Entity` import added; `sendOverlayMessage` kept.
- common util/LookTarget.java: class javadoc names only the admin and picker spawn verbs; `hitInCrosshair(player, reach, Predicate)` folded into `livingHitInCrosshair` (javadoc and `@Nullable` kept); `Predicate` and `Entity` imports dropped. `livingInCrosshair` still calls `livingHitInCrosshair`: 26.2 has since also inlined it (Dead-code 9), which was not taken here.
- orientation-player.md:86 "The check answers for the crosshair target within reach."; orientation-code.md:87 "the inspector reads the game's crosshair target." Both files stay under their size limits (14,925 and 11,883 body characters). build-env/ holds no copy of these files.
- Recalled: `Minecraft.hitResult` is a public field holding the client's crosshair pick, limited to interaction range.
- In-game check: the hint appears only within interaction reach, for mobs, boats, and placed heads.

## 9. `/sg mob move` and `/sg mob rot` don't broadcast to operators
- Reference: 26.2.
- common picker/PickerGate.java: `tellSuccess(source, broadcast, key, args)` with 26.2's javadoc. command/GooglyServerCommands.java mob move (:207) and mob rot (:228) pass `true`; picker/PickerSpawnService.java:411 (spawnall dropped header) passes `false`. No other callers.
- `PickerSpawnService.announce` left to Organization 10; that topic must pass the `broadcast` flag (26.2 passes `true`).
- In-game check: a second operator and the console see the move and rot feedback.

## Left for other topics (seen in the diffs)
- LookTarget: inlining `livingHitInCrosshair` into `livingInCrosshair` (Dead-code 9).
- TomlConfig `assign` (Loader-divergence 9).
- `PickerSpawnService.announce` via `tellSuccess` (Organization 10); `ServerEyeConfigs.isExcluded` at PickerSpawnService:122.
- `EyePlacement.AZIMUTH_TO_YAW_OFFSET`, `Identifier.tryBuild`, and the `trigger` call in GooglyServerCommands.
- EyeItemServiceGameTestsLogic class javadoc and protection-test javadoc.
- Other registration differences in the SomeGooglyGameTestMod files.

## GameTests
+1 per loader (Fabric, Forge, NeoForge: `optometristRollsOnlyOntoShearsAndBooks`); none removed.

## First build settles
- The new test's recalled enchantment API signatures.
- The TomlConfig rewrite.
- `minecraft.hitResult` in 26.3.
- The `tellSuccess` signature at all callers.
