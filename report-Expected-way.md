# Report: Expected-way (26.1.1)

Applied 2026-11-11 by taking 26.1's hunks for this topic only. Nothing was compiled, built, or tested. Fix lines and intro in review-Expected-way.md filled in.

## 1. Optometrist can show up on random looted or fished gear
- EyeItemServiceGameTestsLogic: `optometristRollsOnlyOntoShearsAndBooks` and private helper `rollsOptometrist`; imports `TagKey`, `EnchantmentHelper`, `EnchantmentInstance`, `Stream`.
- Fabric `@GameTest` wrapper; Forge and NeoForge static wrappers; `functions.register` line in both SomeGooglyGameTestMod files after `optometrist_interact_passes_when_not_applicable`; Forge and NeoForge `test_instance/eyeitemservicegametests/optometrist_rolls_only_onto_shears_and_books.json` copied from 26.1.
- 26.1's other hunks in these files (Logic class javadoc, other tests, registration renames) left for their topics. No production change; the `max_cost` question stays open.
- Recalled, unverified: `EnchantmentHelper.getAvailableEnchantmentResults(int, ItemStack, Stream<Holder<Enchantment>>)`; `EnchantmentInstance.enchantment()` returns `Holder<Enchantment>`; `lookupOrThrow(...).getOrThrow(TagKey).stream()`; `Holder.is(ResourceKey)`.

## 3. Fabric config files are written as hand-built TOML text
`TomlConfig.render` builds a `CommentedConfig` (`TomlFormat.newConfig(LinkedHashMap::new)`) written with `TomlWriter`; `stringList` and `table` deleted. The `assign` hunk is left for Loader-divergence 9. Recalled NightConfig behavior: `set(List<String>, value)` creates intermediate tables; `setComment` writes one `#` line per comment line; `TomlWriter` quotes non-bare table names. Only newly created Fabric config files change layout.

## 4, 8
No change.

## 7. The sneak-inspect hint uses its own 16-block ray
- client/EyeInspector.java copied whole from 26.1: reads `minecraft.hitResult`; a living or boat-supported entity under the crosshair gets its verdict, otherwise a placed head (`SkullBlockEntity`) does. `REACH` and the `LookTarget` call gone; `sendOverlayMessage` kept.
- util/LookTarget.java: class javadoc names only the admin and picker spawn verbs; the predicate-taking `hitInCrosshair` folded into `livingHitInCrosshair` (javadoc and `@Nullable` kept); `Predicate` and `Entity` imports dropped. `livingInCrosshair` still calls `livingHitInCrosshair`; inlining is Dead-code 9's.
- orientation-player.md:86 "The check answers for the crosshair target within reach."; orientation-code.md:87 "the inspector reads the game's crosshair target."
- Recalled: `Minecraft.hitResult` is the client's crosshair pick, limited to interaction range. In-game: the hint appears only within interaction reach, for mobs, boats, and placed heads.

## 9. `/sg mob move` and `/sg mob rot` don't broadcast to operators
`PickerGate.tellSuccess(source, broadcast, key, args)` with 26.1's javadoc; GooglyServerCommands mob move (:207) and mob rot (:228) pass `true`; PickerSpawnService:411 (spawnall dropped header) passes `false`. `announce` change left for Organization 10. In-game: a second operator and the console see move and rot feedback.

## Left for later topics
LookTarget inlining (Dead-code 9); TomlConfig `assign` (Loader-divergence 9); `PickerSpawnService.announce` (Organization 10); remaining hunks in EyeItemServiceGameTestsLogic and the SomeGooglyGameTestMod files.

## Files copied whole
common client/EyeInspector.java; Forge and NeoForge `optometrist_rolls_only_onto_shears_and_books.json` (new).

## GameTests
+1 per loader (`optometristRollsOnlyOntoShearsAndBooks`); none removed.

## First build settles
The new test's recalled enchantment API signatures; the TomlConfig rewrite; the `tellSuccess` signature at all three callers.
