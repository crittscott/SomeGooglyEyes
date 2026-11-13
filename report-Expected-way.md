# Report: Expected-way (26.1.2)

Applied 2026-11-12, unbuilt (nothing compiled, built, or tested). For each finding the 26.1.1 file was diffed against 26.1.2's and only this topic's hunks taken; other topics' hunks (LookTarget inlining for Dead-code 9, TomlConfig `assign` for Loader-divergence 9, and others in the gametest files, GooglyServerCommands, and PickerSpawnService) remain for them. Line endings kept (LF).

## 1. Optometrist can show up on random looted or fished gear
- common EyeItemServiceGameTestsLogic: `optometristRollsOnlyOntoShearsAndBooks` and private `rollsOptometrist` copied verbatim from 26.1.1, before the self-remove test's javadoc; imports `TagKey`, `EnchantmentHelper`, `EnchantmentInstance`, `Stream` added.
- Fabric `@GameTest` wrapper and Forge/NeoForge static wrappers after `optometristInteractPassesWhenNotApplicable`.
- Both SomeGooglyGameTestMod files: `functions.register` line after `optometrist_interact_passes_when_not_applicable`; 26.1.2's Twilight and Heads registrations untouched.
- Forge and NeoForge `test_instance/eyeitemservicegametests/optometrist_rolls_only_onto_shears_and_books.json` copied byte-for-byte from 26.1.1 (no version labels).
- No production change. No deviation.

## 3. Fabric config files are written as hand-built TOML text
fabric TomlConfig: `render` builds a `CommentedConfig` (`TomlFormat.newConfig(LinkedHashMap::new)`), sets each `[section, key]` default with its comment, and writes it with `new TomlWriter().writeToString`. `stringList` and `table` deleted (no other references); existing imports sufficed. The `assign` hunk (Loader-divergence 9) is now the only diff from 26.1.1. No deviation.

## 4. Shears are recognized only by item tag
None.

## 7. The sneak-inspect hint uses its own 16-block ray
- common client/EyeInspector (by hunks, not whole): `tick` reads `minecraft.hitResult`; an `EntityHitResult` whose entity is living or `BoatEyeLayout.supports` gets its verdict, otherwise a `BlockHitResult` whose block entity is a `SkullEyeState.Holder`. Deviation from 26.1.1: keeps 26.1.2's `BlockEntity head` / `instanceof SkullEyeState.Holder` test (26.1.1 tests `SkullBlockEntity`), so Twilight trophies and skull candles count. `REACH` and the `LookTarget` import removed; `Entity` import added; javadoc matches 26.1.1's ("the target a right-click would use").
- common util/LookTarget: class javadoc names only the `/sg admin` and picker spawn verbs; the predicate-taking `hitInCrosshair` folded into `livingHitInCrosshair` (javadoc and `@Nullable` kept); `Predicate`, `Entity` imports removed. `livingInCrosshair` still calls `livingHitInCrosshair` (inlining is Dead-code 9).
- orientation-code.md:87: "the inspector compares actual hit distances" → "the inspector reads the game's crosshair target" (re-read just before the one-phrase edit; Multiloader-organization text untouched).
- orientation-player.md:86: "The check reaches about 16 blocks." → "The check answers for the crosshair target within reach."
- Both orientation files well under their limits; no other doc mentions the 16-block reach.

## 8. Two reaction hooks fire on the request, not the result
None.

## 9. `/sg mob move` and `/sg mob rot` don't broadcast to operators
- PickerGate: `tellSuccess(CommandSourceStack source, boolean broadcast, String key, Object... args)` with 26.1.1's javadoc.
- GooglyServerCommands: mob move (:207) and mob rot (:228) pass `true`.
- PickerSpawnService: spawnall dropped header (:411) passes `false`.
- No other callers. `announce` (PickerSpawnService:169) left for Organization 10. No deviation.

## Left for other topics / the parent
- LookTarget `livingHitInCrosshair` inlining (Dead-code 9); TomlConfig `assign` (Loader-divergence 9); PickerSpawnService `announce` (Organization 10) and `isExcluded` (Organization 17); GooglyServerCommands `AZIMUTH_TO_YAW_OFFSET`, `Identifier.tryBuild`, `trigger` signature; EyeItemServiceGameTestsLogic class and self-remove javadocs and `setShiftKeyDown(false)`; other SomeGooglyGameTestMod registrations.
- GameTests: +1 per loader (`optometristRollsOnlyOntoShearsAndBooks`), none removed.

## Recalled API facts and checks only a build or in-game test can settle
- Recalled: `EnchantmentHelper.getAvailableEnchantmentResults(int, ItemStack, Stream<Holder<Enchantment>>)`; `EnchantmentInstance.enchantment()` returns `Holder<Enchantment>`; `lookupOrThrow(...).getOrThrow(TagKey).stream()`; `Holder.is(ResourceKey)`.
- NightConfig (recalled): `CommentedConfig.set(List<String>, value)` creates intermediate tables; `setComment` writes one `#` line per comment line; `TomlWriter` quotes non-bare table names. Only newly created Fabric config files change layout.
- `Minecraft.hitResult` is assumed to be the client's crosshair pick, limited to interaction range.
- First build settles the new test, the TomlConfig rewrite, and the three `tellSuccess` callers.

## In-game checks
- The sneak-inspect hint appears only within interaction reach, for mobs, boats, vanilla heads, and (NeoForge with Twilight Forest) trophies and skull candles.
- A second operator and the console see `/sg mob move` and `/sg mob rot` feedback; the spawnall dropped header goes to the sender only.
- A freshly created Fabric config file has a valid layout and comments.
