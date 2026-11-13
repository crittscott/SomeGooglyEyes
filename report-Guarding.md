# Report: Guarding (26.1.1)

Applied 2026-11-11 by taking 26.1's Guarding hunks by hand with the Edit tool. Nothing was compiled, built, or tested. No file copied whole; touched files keep CRLF. Each touched file re-diffed against 26.1: only other topics' hunks remain. Fix lines and intro in review-Guarding.md filled in.

## 2. Picker setters re-clamp values Brigadier has already bounded
PickerState: `setDepth`, `setEyeScale`, `setIrisScale`, `setVariantWeight` assign as given ("clamped" gone from javadocs); `currentVariant` no longer re-clamps `variantIndex`. GooglyClientCommands already bounds them with `floatArg(0)` (:394, :417-420), as 26.1.

## 3. Color range checks repeated
AppearanceOverride `isValid` deleted (no callers); `EyeColor.channel` returns `Math.round(v * 255F)`. The `EyeAppearance` import in AppearanceOverride is Organization 15's.

## 4. Redundant spectator check
Done with Correctness 1 (verified the entity `applySlimyEye` no longer tests `isSpectator()`).

## 5. Null checks on values that cannot be null
`SkullEyeState.set` calls `skull.getLevel().sendBlockUpdated(...)` directly; unused `Level` import removed (`isHeadPart` hunk left for Magic-numbers). `ConfigValue.filter` tests only `valid.test(value)` (`Parsed.set` fallback left for Loader-divergence 9). ReflectedBoxResolver `fallbackNames` walk stops at `c != Object.class`.

## 6. Catch-all around eye-definition parsing
`EyeConfigReloadListener.apply`'s `try`/`catch (Exception e)` removed, body de-indented; `resultOrPartial` kept so a bad file is still logged and counted. A bug in selection or limit checks now fails the whole reload. Logging 1's `result()`/`DataResult` switch and the `ID`/`isExcluded` hunks left.

## 7. One bad Citadel or LLibrary model disables the whole family
ReflectedBoxResolver: `integrationFailed` → weak `failedModels` set (`Collections.newSetFromMap(new WeakHashMap<>())`, cleared in `clearModelCache`); `integrationFailed()`/`disableIntegration` → private `fail(operation, model, subject, failure)` and protected `failed(model)`; model passed through `parentOf`, `applyTransform`, `pathOf`, `BoxChain` (new `model` field); `BoxChain.apply`, `buildIndex`, `resolve` check `failed(model)`; `applyTransform`'s early return dropped. AdvancedModelBoxResolver and LLibraryResolver: `available()` no longer reads a family flag; `handles` adds `&& !failed(model)`. `attachmentIndex`/`attachmentPath` (Magic-numbers 2) and `LLibraryResolver.familyLabel()` (Logging 2) left.

## 8
None (rejected).

## Deviations from 26.1
None.

## Recalled API facts
A placed `SkullBlockEntity` reached from a player interaction always has a level; Brigadier `floatArg(min)` rejects out-of-range input before the command runs; models as `WeakHashMap` keys match through `equals`/`hashCode`.

## First build and in-game checks
No known compile risk; nothing references `integrationFailed`, `disableIntegration`, or `AppearanceOverride.isValid`. In game: with Citadel or LLibrary, one model's reflection failure hides only that model's eyes; a malformed datapack eye config is reported and skipped; adding or removing eyes on a placed head updates nearby clients.

## Left to other topics
Logging 1 and 2, Magic-numbers 2 (and `SkullEyeState.isHeadPart`), Loader-divergence 9, Organization 15, PickerState's `ClientNetworking` import.

## GameTests
None added or removed.
