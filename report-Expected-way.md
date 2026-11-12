# Report: Expected-way (1.21.11)

Applied 2026-11-11 by hand-porting 1.21.8's changes. Nothing was compiled or tested; every statement below comes from reading the source only. 1.21.8's review file was not on disk, so its fixes were taken from the "1.21.8 fix" lines in 1.21.11's review file and from diffs against 1.21.8's code.

## 1. Optometrist can show up on random looted or fished gear
- **Changed:** EyeItemServiceGameTestsLogic.java gains `optometristRollsOnlyOntoShearsAndBooks` and private `rollsOptometrist` (imports `TagKey`, `EnchantmentHelper`, `EnchantmentInstance`, `Stream`); Fabric `@GameTest` wrapper and Forge/NeoForge static wrappers in EyeItemServiceGameTests.java; Forge/NeoForge `functions.register` lines; Forge/NeoForge `test_instance/eyeitemservicegametests/optometrist_rolls_only_onto_shears_and_books.json` (copied from 1.21.8).
- **Deviation:** `Identifier.withDefaultNamespace` in place of `ResourceLocation`.
- **Recalled API facts:** `EnchantmentHelper.getAvailableEnchantmentResults(int, ItemStack, Stream<Holder<Enchantment>>)` keeps this shape in 1.21.11; `EnchantmentInstance` is a record whose `enchantment()` returns `Holder<Enchantment>`; `lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(TagKey)` returns a holder set with `stream()`; `Holder.is(ResourceKey)` matches `ModContent.OPTOMETRIST`.
- No production change. The `max_cost` question stays open.

## 3. Fabric config files are written as hand-built TOML text
- **Changed:** TomlConfig.java: `render` builds a `CommentedConfig` (`TomlFormat.newConfig(LinkedHashMap::new)`) written by `TomlWriter`; `stringList` and `table` deleted (no other references).
- **Not taken:** 1.21.8's `assign` hunk (another topic).
- **Recalled NightConfig 3.8 facts:** `set(List<String>, value)` creates intermediate tables; `setComment` writes one `#` line per comment line; `TomlWriter` quotes non-bare table names.
- Only newly created Fabric config files change layout.

## 4 and 8
No change; proposed fix None.

## 7. The sneak-inspect hint uses its own 16-block ray
- **Changed:** EyeInspector.java taken whole from 1.21.8 (differed only by this fix and import order); LookTarget.java class javadoc updated and the predicate-taking `hitInCrosshair` merged into `livingHitInCrosshair`, its last caller (`Predicate`/`Entity` imports dropped); orientation-player.md:86 reads "The check answers for the crosshair target within reach."; orientation-code.md:85's inspector clause now says it reads the game's crosshair target.
- **Deviation:** Dead-code 9's inlining of `livingHitInCrosshair` left to that topic; it now only has to inline it into `livingInCrosshair`, keeping 1.21.11's jspecify `@Nullable`.
- **Recalled API fact:** `Minecraft.hitResult` is the client's crosshair pick, limited to interaction range, current when the client tick reads it.
- **In-game check:** the hint appears only within interaction reach, for mobs and placed heads.

## 9. `/sg mob move` and `/sg mob rot` don't broadcast to operators
- **Changed:** PickerGate.java `tellSuccess(source, broadcast, key, args)` with 1.21.8's javadoc; GooglyServerCommands.java mob move and rot pass `true`; PickerSpawnService.java's spawnall dropped header passes `false`.
- **Not done:** Organization 10's `announce` change.
- **In-game check:** a second operator and the server console see the move and rot feedback.

## Line endings
EyeInspector, LookTarget, TomlConfig, the three EyeItemServiceGameTests.java files, and both SomeGooglyGameTestMod.java files were written LF (the working tree is CRLF; git's `text=auto` normalizes either).

## Clash points
LookTarget.java (Dead-code 9); PickerGate.java and PickerSpawnService.java (Organization 10); TomlConfig.java (Loader-divergence 9, Organization 14); Forge/NeoForge SomeGooglyGameTestMod.java and loader gametest wrappers (any topic adding/removing gametests); orientation-player.md and orientation-code.md.
