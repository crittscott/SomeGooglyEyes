# Report: Guarding (26.3)

Applied 2026-11-12 from 26.2's post-fix hunks (26.2/report-Guarding.md exists; 26.2 made no changes beyond 26.1.1's code). Each touched file was diffed against 26.2 and only this topic's hunks were taken. None of the touched files differ between 26.2 and 26.3 by any of 26.3's API changes, so no adaptation was needed. Nothing was compiled, built, or tested. Fix lines in review-Guarding.md filled in.

## 2. Picker setters re-clamp values Brigadier has already bounded
PickerState: `setDepth`, `setEyeScale`, `setIrisScale`, `setVariantWeight` assign the value as given, and "clamped" is gone from their javadocs. `currentVariant` returns `variants.get(variantIndex)` without re-clamping. GooglyClientCommands already bounds these values with `floatArg(0)`, so it was not changed.

## 3. Color range checks repeated after every color is already guaranteed in range
`AppearanceOverride.isValid` deleted (no callers). `EyeColor.channel` returns `Math.round(v * 255F)`. `EyeColor.isValid` stays (the codec uses it). Organization 15 owns any `EyeAppearance` import once that class moves.

## 4. Redundant spectator check
None separately (Correctness 1 covers it).

## 5. Null checks on values that cannot be null
`SkullEyeState.set(SkullBlockEntity, Snapshot)` calls `skull.getLevel().sendBlockUpdated(...)` directly; the unused `Level` import removed. `ConfigValue.filter` tests only `valid.test(value)`. ReflectedBoxResolver's `fallbackNames` superclass walk stops at `c != Object.class`. Magic-numbers 2's `isHeadPart` hunk and Loader-divergence 9's `Parsed.set` fallback were not taken.

## 6. A catch-all around eye-definition parsing
`EyeConfigReloadListener.apply`: the per-file `try`/`catch (Exception e)` removed and the body de-indented. `resultOrPartial` and its comment kept for Logging 1. A bug in selection or limit checks now fails the whole reload instead of being reported as a data error.

## 7. One bad Citadel or LLibrary model disables the whole family
- ReflectedBoxResolver: a weak `failedModels` set (`Collections.newSetFromMap(new WeakHashMap<>())`, cleared in `clearModelCache`) replaces `integrationFailed`. Private `fail(operation, model, subject, failure)` and protected `failed(model)` replace `integrationFailed()` and `disableIntegration`. The model is passed through `parentOf`, `applyTransform` (early return dropped), `pathOf`, and `BoxChain` (new `model` field); `BoxChain.apply`, `buildIndex`, and `resolve` check `failed(model)`.
- Built from 26.2's file with Magic-numbers 2's three hunks reverted (the `EyeConfigModel` import, `attachmentIndex(i)`, `attachmentPath(segments)`); it keeps `"#" + i` and `String.join("/", segments)`. The file now has 26.2's LF line endings.
- AdvancedModelBoxResolver and LLibraryResolver: `available()` no longer reads a flag; `handles` adds `&& !failed(model)`. LLibraryResolver's `familyLabel()` override (Logging 2) not taken.

## 8
None (rejected).

## Recalled API facts
A placed `SkullBlockEntity` reached from a player interaction always has a level; Brigadier `floatArg(min)` rejects out-of-range input before the command runs; models used as `WeakHashMap` keys match through `equals`/`hashCode`.

## First build and in-game checks
No known compile risk; nothing in 26.3 references `integrationFailed`, `disableIntegration`, or `AppearanceOverride.isValid`. In game: with Citadel or LLibrary, one model's reflection failure hides only that model's eyes; a malformed datapack eye config is reported and skipped; adding or removing eyes on a placed head updates nearby clients.

## GameTests
None added or removed (Fabric, Forge, NeoForge: +0).

## Left to other topics
Magic-numbers 2 (SkullEyeState `isHeadPart`; ReflectedBoxResolver `attachmentIndex`/`attachmentPath` and import); Loader-divergence 9 (`Parsed.set` fallback); Logging 1 (EyeConfigReloadListener `result()`/`DataResult`); Logging 2 (LLibraryResolver `familyLabel()`); Organization 15 (`EyeAppearance` move); PickerState's `ClientNetworking` import package (Organization 18).
