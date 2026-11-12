# Report: Expected-way (26.1)

Applied 2026-11-11 by hand-porting 1.21.11's changes. Nothing was compiled or tested; everything here comes from reading the source. The Fix lines in review-Expected-way.md are filled in.

## 1. Optometrist can show up on random looted or fished gear
- EyeItemServiceGameTestsLogic gains `optometristRollsOnlyOntoShearsAndBooks` and private `rollsOptometrist` (imports `TagKey`, `EnchantmentHelper`, `EnchantmentInstance`, `Stream`), copied from 1.21.11. 1.21.11's other javadoc rewrites in that file belong to other topics and were not taken.
- Fabric `@GameTest` wrapper; Forge and NeoForge static wrappers, `functions.register` lines, and `test_instance/eyeitemservicegametests/optometrist_rolls_only_onto_shears_and_books.json` (`somegoogly:empty`, 40 ticks).
- No production change; `max_cost` stays open.
- Recalled: `EnchantmentHelper.getAvailableEnchantmentResults(int, ItemStack, Stream<Holder<Enchantment>>)` unchanged in 26.1; `EnchantmentInstance.enchantment()` returns `Holder<Enchantment>`; `lookupOrThrow(...).getOrThrow(TagKey)` streams; `Holder.is(ResourceKey)`.

## 3. Fabric config files are written as hand-built TOML text
`TomlConfig.render` builds a `CommentedConfig` (`TomlFormat.newConfig(LinkedHashMap::new)`) written by `TomlWriter`; `stringList` and `table` deleted. The `assign` hunk is left for Loader-divergence 9. Recalled NightConfig behavior: `set(List<String>, value)` creates intermediate tables, `setComment` writes one `#` line per comment line, `TomlWriter` quotes non-bare table names. Only newly created Fabric files change layout.

## 4, 8
No change.

## 7. The sneak-inspect hint uses its own 16-block ray
- EyeInspector reads `minecraft.hitResult`: an entity hit on a `LivingEntity` or a `BoatEyeLayout.supports` entity gets its verdict, otherwise a block hit on a `SkullBlockEntity` gets the head's. `REACH` and the `LookTarget` call gone; javadoc updated. Deviation: keeps 26.1's `player.sendOverlayMessage(...)`.
- LookTarget: 1.21.11's class javadoc; the predicate-taking `hitInCrosshair` folded into `livingHitInCrosshair`. Inlining into `livingInCrosshair` left to Dead-code 9 (keep `@Nullable`).
- orientation-player.md:86: "The check answers for the crosshair target within reach." orientation-code.md:87 now says the inspector reads the game's crosshair target (edited by the parent after the parallel agent finished).
- Recalled: `Minecraft.hitResult` is the client's crosshair pick, limited to interaction range.
- In-game: the hint appears only within interaction reach, for mobs, boats, and placed heads.

## 9. `/sg mob move` and `/sg mob rot` don't broadcast to operators
`PickerGate.tellSuccess(source, broadcast, key, args)` with 1.21.11's javadoc; mob move and rot pass `true`, the spawnall dropped header `false`; no other callers. Organization 10 changes `announce`. In-game: a second operator and the console see move and rot feedback.

## Clash points
LookTarget (Dead-code 9); PickerGate, PickerSpawnService (Organization 10); TomlConfig (Loader-divergence 9, Organization 14); both SomeGooglyGameTestMod files and the EyeItemServiceGameTests wrappers; orientation-player.md.

## GameTests
Added on each loader: `optometristRollsOnlyOntoShearsAndBooks` (Fabric +1, Forge +1, NeoForge +1). None removed.

## Files modified
common main: client/EyeInspector.java, util/LookTarget.java, picker/PickerGate.java, picker/PickerSpawnService.java, command/GooglyServerCommands.java. Gametest: EyeItemServiceGameTestsLogic; Fabric, Forge, NeoForge EyeItemServiceGameTests; Forge and NeoForge SomeGooglyGameTestMod and the new test_instance JSONs. Fabric: config/fabric/TomlConfig.java. Docs: orientation-player.md, orientation-code.md, review-Expected-way.md.
