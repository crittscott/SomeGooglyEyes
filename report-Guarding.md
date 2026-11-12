# Report: Guarding (26.1)

Applied 2026-11-11 by hand-porting 1.21.11's changes. Nothing was compiled, built, or tested; everything here comes from reading the source. Each file was diffed against 1.21.11 (CRLF/LF normalized) and only this topic's hunks taken; no file was copied whole. Line endings preserved per file. Fix lines in review-Guarding.md are filled in.

## 2. Picker setters re-clamp values Brigadier has already bounded
client/picker/PickerState.java: `setDepth`, `setEyeScale`, `setIrisScale`, `setVariantWeight` assign plainly, "clamped" dropped from their javadocs; `currentVariant` no longer re-clamps `variantIndex`. GooglyClientCommands bounds `w` (:394), `eyescale`, `irisscale`, `depth` (:417-420) with `floatArg(0)`.

## 3. Color range checks repeated
AppearanceOverride: `isValid` deleted (no callers). EyeColor: `channel` returns `Math.round(v * 255F)`. 1.21.11's `EyeAppearance` import left to Organization 15.

## 4. Redundant spectator check
None here; done by Correctness 1.

## 5. Null checks on values that cannot be null
SkullEyeState.set calls `skull.getLevel().sendBlockUpdated(...)` unguarded; `Level` import removed. ConfigValue.filter tests only `valid.test(value)` (1.21.11's `Parsed.set` fallback left to Loader-divergence 9). ReflectedBoxResolver's `fallbackNames` walk stops at `c != Object.class` alone.

## 6. Catch-all around eye-definition parsing
EyeConfigReloadListener.apply: `try`/`catch (Exception e)` removed, body de-indented. `resultOrPartial` kept; parse failures still count via the `file == null` branch. A defect in selection or limit checks now fails the reload. Logging 1 switches to `result()`.

## 7. One bad Citadel or LLibrary model disables the whole family
- ReflectedBoxResolver: `integrationFailed` replaced by weak `failedModels` (`Collections.newSetFromMap(new WeakHashMap<>())`, cleared in `clearModelCache`); `integrationFailed()`/`disableIntegration` replaced by private `fail(operation, model, subject, failure)` and protected `failed(model)`; the model carried through `parentOf`, `applyTransform`, `pathOf`, and `BoxChain` (new `model` field); `BoxChain.apply`, `buildIndex`, `resolve` check `failed(model)`.
- AdvancedModelBoxResolver and LLibraryResolver: `available()` no longer reads a family flag; `handles` adds `&& !failed(model)`.
- Left: `attachmentIndex`/`attachmentPath` (Magic-numbers 2), `LLibraryResolver.familyLabel()` (Logging 2).

## 8. Lenient `optionalFieldOf`
None (rejected).

## Recalled API facts
A placed `SkullBlockEntity` reached from a player interaction has a non-null `getLevel()`; Brigadier `floatArg(min)` rejects out-of-range input before the command runs; `WeakHashMap` keys (models) use `equals`/`hashCode`.

## In-game checks
Citadel/LLibrary: one model's reflection failure hides only that model's eyes; a malformed datapack eye config is still reported and skipped; adding or removing eyes on a placed head still updates nearby clients.

## Left to other topics
Logging 1, Magic-numbers 2, Logging 2, Loader-divergence 9, Organization 15, Organization 17 / Magic-numbers 9 (EyeConfigReloadListener).

## Clash points
EyeConfigReloadListener, ReflectedBoxResolver, LLibraryResolver, AppearanceOverride, ConfigValue, PickerState, SkullEyeState.

## GameTests
None added or removed.

## Files modified
common main: client/picker/PickerState.java, eye/state/AppearanceOverride.java, eye/state/EyeColor.java, eye/state/SkullEyeState.java, config/ConfigValue.java, config/EyeConfigReloadListener.java, client/render/resolver/ReflectedBoxResolver.java, client/render/resolver/AdvancedModelBoxResolver.java, client/render/resolver/LLibraryResolver.java. Docs: review-Guarding.md.
