# Report: Guarding (26.2)

Applied 2026-11-12 by taking 26.1.1's Guarding hunks by hand with the Edit tool. Nothing was compiled, built, or tested. No file copied whole. Each touched file was diffed against 26.1.1 before editing; only this topic's hunks were taken. Fix lines in review-Guarding.md filled in.

## 2. Picker setters re-clamp values Brigadier has already bounded
PickerState: `setDepth`, `setEyeScale`, `setIrisScale`, `setVariantWeight` assign as given ("clamped" gone from javadocs); `currentVariant` no longer re-clamps `variantIndex`. GooglyClientCommands already bounds them with `floatArg(0)` (:394, :417-420). No deviation.

## 3. Color range checks repeated
AppearanceOverride `isValid` deleted (no callers); `EyeColor.channel` returns `Math.round(v * 255F)`. No deviation. The missing `EyeAppearance` import (javadoc link) is Organization 15's.

## 4. Redundant spectator check
Fix was already "None." (covered by Correctness 1); nothing done.

## 5. Null checks on values that cannot be null
`SkullEyeState.set(SkullBlockEntity, Snapshot)` calls `skull.getLevel().sendBlockUpdated(...)` directly; unused `Level` import removed. `ConfigValue.filter` tests only `valid.test(value)`. ReflectedBoxResolver `fallbackNames` walk stops at `c != Object.class`. No deviation.

## 6. Catch-all around eye-definition parsing
`EyeConfigReloadListener.apply`'s `try`/`catch (Exception e)` removed, body de-indented; `resultOrPartial` kept (Logging 1 replaces it). A bug in selection or limit checks now fails the whole reload. No deviation.

## 7. One bad Citadel or LLibrary model disables the whole family
ReflectedBoxResolver: `integrationFailed` replaced by a weak `failedModels` set (`Collections.newSetFromMap(new WeakHashMap<>())`, cleared in `clearModelCache`); `integrationFailed()`/`disableIntegration` replaced by private `fail(operation, model, subject, failure)` and protected `failed(model)`; model passed through `parentOf`, `applyTransform` (early return dropped), `pathOf`, `BoxChain` (new `model` field); `BoxChain.apply`, `buildIndex`, `resolve` check `failed(model)`. AdvancedModelBoxResolver and LLibraryResolver: `available()` no longer reads a flag; `handles` adds `&& !failed(model)`. ReflectedBoxResolver now equals 26.1.1's. No deviation.

## 8
None (rejected).

## Recalled API facts
A placed `SkullBlockEntity` reached from a player interaction always has a level; Brigadier `floatArg(min)` rejects out-of-range input before the command runs; models as `WeakHashMap` keys match through `equals`/`hashCode`.

## First build and in-game checks
No known compile risk; nothing in 26.2 references `integrationFailed`, `disableIntegration`, or `AppearanceOverride.isValid`. In game: with Citadel or LLibrary, one model's reflection failure hides only that model's eyes; a malformed datapack eye config is reported and skipped; adding or removing eyes on a placed head updates nearby clients.

## GameTests
None added or removed (Fabric, Forge, NeoForge: +0).

## Left to other topics / possible clashes
PickerState: Correctness (cross-target on delete), `ClientNetworking` import, `unfreeze` javadoc link. SkullEyeState: Magic-numbers 2 (`isHeadPart`), Multiloader-organization 1. ConfigValue: Loader-divergence 9 (`Parsed.set` fallback). EyeConfigReloadListener: Logging 1 (`result()`/`DataResult`), `ID`, `isExcluded`, packet/refusal hunks; another agent edited the file concurrently. LLibraryResolver `familyLabel()` (Logging 2) already present. AppearanceOverride: Organization 15's `EyeAppearance` import.
