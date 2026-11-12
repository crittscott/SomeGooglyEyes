# Report: Guarding (1.21.11)

Applied 2026-11-11 by hand-porting 1.21.8's changes. Nothing was compiled, built, or tested; everything here comes from reading the source. Each file was diffed against 1.21.8 and only this topic's hunks taken; no file was copied whole.

## 2. Picker setters re-clamp values Brigadier has already bounded
client/picker/PickerState.java: `setDepth`, `setEyeScale`, `setIrisScale`, `setVariantWeight` assign plainly, "clamped" dropped from their javadocs; `currentVariant` no longer re-clamps. GooglyClientCommands bounds `w`, `eyescale`, `irisscale`, `depth` with `floatArg(0)`; no other callers. PickerState now differs from 1.21.8 only by port renames.

## 3. Color range checks repeated
eye/state/AppearanceOverride.java: `isValid` deleted (no callers). eye/state/EyeColor.java: `channel` returns `Math.round(v * 255F)`. Relies on both EyeColor codecs rejecting out-of-range channels and `floatArg(0, 1)` color arguments. Not taken: 1.21.8's `EyeAppearance` import in AppearanceOverride (belongs with Organization 15's package move).

## 4. Redundant spectator check
None needed; Correctness 1 removed it.

## 5. Null checks on values that cannot be null
- SkullEyeState.set calls `skull.getLevel().sendBlockUpdated(...)` unguarded; the `Level` import removed (nothing else, including the Heads code, uses it). Production callers pass placed skulls from EyeItemService.
- ConfigValue.filter tests only `valid.test(value)`. 1.21.8's `Parsed.set` fallback hunk not taken (Loader-divergence 9).
- ReflectedBoxResolver's superclass walk in `fallbackNames` stops at `c != Object.class` alone.

## 6. Catch-all around eye-definition parsing
EyeConfigReloadListener: `try`/`catch (Exception e)` removed and body de-indented. Parse failures are reported by the codec's `resultOrPartial` log and the `file == null` branch (`failedParse`). A defect in selection or limit checks now fails the reload. Left for Logging 1: switch to `result()` and its log message.

## 7. One bad Citadel or LLibrary model disables the whole family
- ReflectedBoxResolver: `integrationFailed` replaced by weak `failedModels` (`Collections.newSetFromMap(new WeakHashMap<>())`, cleared in `clearModelCache`); private `fail(operation, model, subject, failure)` and protected `failed(model)`; model carried through `parentOf`, `applyTransform`, `pathOf`, `BoxChain` (new `model` field); `BoxChain.apply` checks `failed(model)`; `buildIndex` and `resolve` check it.
- AdvancedModelBoxResolver and LLibraryResolver: `available()` no longer reads a family flag; `handles` adds `&& !failed(model)`. Both match 1.21.8 except LLibraryResolver's `familyLabel()` (Logging 2).
- Deviations: `BoxChain` fields ordered `chain`, `model`. The `"#" + i` (~:166) and `String.join("/", segments)` (~:219) literals left for Magic-numbers 2. Generated javadoc HTML under docs/javadoc still lists the old members (build output, left).

## 8. Lenient `optionalFieldOf`
None (rejected).

## Recalled API facts
A placed `SkullBlockEntity` reached from a player interaction has a non-null `getLevel()`; Brigadier `floatArg(min[, max])` rejects out-of-range input before the command runs; `WeakHashMap` keys use `equals`/`hashCode`, as in 1.21.8.

## Left to other topics
Logging 1 (`result()` in EyeConfigReloadListener); Magic-numbers 2 (`attachmentIndex`/`attachmentPath` in ReflectedBoxResolver); Logging 2 (`LLibraryResolver.familyLabel()`); Loader-divergence 9 (`ConfigValue.Parsed.set`); Organization 15 (`EyeAppearance` import); Organization 17 and Magic-numbers 9 (`isExcluded`, `SomeGooglyCommon.id` in EyeConfigReloadListener).

## Clash points
EyeConfigReloadListener, ReflectedBoxResolver, LLibraryResolver, AppearanceOverride, ConfigValue, PickerState, SkullEyeState.

## In-game checks
Citadel/LLibrary: one model's reflection failure hides only that model's eyes; a malformed datapack eye config is still reported and skipped; adding or removing eyes on a placed head still updates nearby clients.

## Files modified
common main: client/picker/PickerState, eye/state/AppearanceOverride, eye/state/EyeColor, eye/state/SkullEyeState, config/ConfigValue, config/EyeConfigReloadListener, client/render/resolver/ReflectedBoxResolver, client/render/resolver/AdvancedModelBoxResolver, client/render/resolver/LLibraryResolver. Docs: review-Guarding.md.
