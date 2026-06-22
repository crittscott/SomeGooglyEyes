---
name: eye-property-unification
description: In-progress refactor unifying eye appearance/placement onto shared codec records
metadata:
  type: project
---

Started 2026-06-21 on branch `item-eyes`. Refactor to remove duplication between the two parallel eye data models:
- `EyeProperties` (codec, item/entity/network override, was 3 packed-int fields) →
- `HeadInfo.EyeConfig` (Gson, datapack geometry+appearance).

Target structure (all immutable records + codecs):
- `EyeColor(r,g,b)` — list `[r,g,b]` codec, absorbs old `EyeState.packColor/unpackColor`.
- `EyeAppearance(cornea, iris, glow)` — concrete, flat JSON `corneaColors/irisColors/glows`.
- `AppearanceOverride` — rename of `EyeProperties`; sparse `Optional<EyeColor>`; the item/mob override.
- `EyePlacement` — position(Vec3)/scale/angles/affectedByInvisibility. (`sideOffset` removed 2026-06-22 as dead cruft — it was always a no-op duplicate of position.x; the optional codec field means old JSONs with the key still parse.)
- `EyeDefinition = EyePlacement + EyeAppearance` — replaces `EyeConfig`; codec flattens to existing flat JSON.

Decisions: JSON stays flat & unchanged (no data-file rewrite); whole config tree moves Gson→JsonOps; picker uses a mutable `EyeDraft` converted to `EyeDefinition` at save. Renderer collapses per-field fallback to `appearance.overlay(override)`. See [[american-spelling]].

DONE 2026-06-21: all phases implemented, `gradlew compileJava` BUILD SUCCESSFUL. EyeConfig deleted; packColor/unpackColor deleted. Adding a new appearance property now = one field in EyeColor/EyeAppearance/AppearanceOverride. Remaining: in-game smoke test (not yet run) — stock mob render, harvest/reattach NBT round-trip, dye/glowstone/cobweb modifiers, picker export+/reload, GeckoLib mob.
