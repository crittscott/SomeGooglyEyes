# Report: Multiloader-organization (26.1.2)

Applied 2026-11-12, unbuilt. This finding's hunks were taken from 26.1.1 by diffing each 26.1.1 file against 26.1.2's (`diff --strip-trailing-cr`); no file was copied whole. The two `HeadEyesLootModifier`s were edited hunk by hunk so they keep 26.1.2's `SkullEyeState.Holder`/`BlockEntity` head type, NeoForge's `priority` constructor, and NeoForge's javadoc clause about other mods' heads. Nothing was compiled, built, or tested.

## 1. The place a head item keeps its eyes is spelled out separately on Fabric
- common `SkullEyeState`: added `headLootCopy()` (same javadoc and body as 26.1.1: a `LootItemFunction.Builder` copying `ModContent.HEAD_EYES` from `LootContextParams.BLOCK_ENTITY`). Deleted `copyToLoot` and its now-unused imports (`LootContext`, `java.util.Collection`); added `CopyComponentsFunction`, `LootItemFunction` imports. `stampItem` kept; `LootContextParams` still used.
- fabric `FabricServerEvents`: `LootTableEvents.MODIFY` hook applies `SkullEyeState.headLootCopy()`; dropped `ModContent`, `CopyComponentsFunction`, `LootContextParams` imports (no other use).
- forge and neoforge `HeadEyesLootModifier` (CRLF kept): memoized `COPY_EYES` (`Suppliers.memoize` from Guava, plus `java.util.function.Supplier`); class javadoc gains "It copies only a head's eyes onto that head's item." (Javadocs 6); `doApply` reads `BlockEntity head = context.getOptionalParameter(LootContextParams.BLOCK_ENTITY)` and, when `head instanceof SkullEyeState.Holder && SkullEyeState.hasEyes(head)`, applies `COPY_EYES` to each stack with `entityFor(stack) != null`. Deviation from 26.1.1: `Holder` + `hasEyes(BlockEntity)` instead of `SkullBlockEntity`, so Twilight trophies, skull candles, and Heads heads keep their eyes. NeoForge keeps "so it reaches other mods' heads, such as the Heads mod's" and its `(conditions, priority)` constructor.
- common gametest `SkullEyeGameTestsLogic.brokenHeadKeepsItsEyes`: after the restore, discards the first drop, clears the eyes (`Snapshot.NONE`), breaks again, and expects exactly one skeleton skull without `HEAD_EYES`; javadoc gains the in-game step. Text matches 26.1.1; imports already present.
- orientation-code.md (one targeted edit): head loot "copies the state with `SkullEyeState.headLootCopy` (Forge/NeoForge `HeadEyesLootModifier`, with native data in each loader; Fabric `LootTableEvents.MODIFY`)". Body 11,955 characters.
- Verified by reading: NeoForge `TwilightTrophyDataMixin.collectImplicitComponents` sets `ModContent.HEAD_EYES` (:72-77) and `applyImplicitComponents` reads it, so the `copy_components` copy covers trophies. Fabric `headLootTables()` still gathers every block with `SkullEyeState.entityFor(block) != null`, the same recognizer.
- Reference search: no `copyToLoot` remains in Java, mixin JSON, or Markdown other than review/report files. The generated docs/javadoc HTML for SkullEyeState still lists `copyToLoot` (left for the next javadoc build).

## Left for other topics / the parent
- Fix line in review-Multiloader-organization.md.
- Regenerate docs/javadoc so `copyToLoot` drops out.
- No gametest added or removed; no registrations or JSON changed (`brokenHeadKeepsItsEyes` only extended).

## Recalled API facts and checks only a build or in-game test can settle
- `copyComponentsFromBlockEntity(...).include(...)` returns a `LootItemFunction.Builder`; `LootItemFunction.apply(stack, context)` (a `BiFunction`) returns the stack to keep; Guava `Suppliers` is on the Forge and NeoForge classpaths; `LootContext.getOptionalParameter(LootContextParams.BLOCK_ENTITY)` is typed `BlockEntity`.
- The copy reads the block entity's implicit components; `SkullBlockEntityDataMixin` and `TwilightTrophyDataMixin` fill them only when the head has eyes. Whether Heads-mod block entities collect `HEAD_EYES` through the skull mixin is an in-game check.
- First build settles the new imports and `headLootCopy()` on all three loaders, and whether `brokenHeadKeepsItsEyes` passes with its plain-head step.

## In-game checks
- All loaders: break an eyed skull; the drop keeps its eyes and placing restores them. Break a plain skull; its drop stacks with ordinary skulls.
- NeoForge with Twilight Forest: an eyed trophy (and skull candle) broken and re-placed keeps its eyes.
- NeoForge with Heads: an eyed Heads head broken and re-placed keeps its eyes.

## Files changed
- common/src/main/java/com/github/crittscott/somegoogly/eye/state/SkullEyeState.java
- fabric/src/main/java/com/github/crittscott/somegoogly/server/fabric/FabricServerEvents.java
- forge/src/main/java/com/github/crittscott/somegoogly/server/forge/HeadEyesLootModifier.java
- neoforge/src/main/java/com/github/crittscott/somegoogly/server/neoforge/HeadEyesLootModifier.java
- common/src/gametest/java/com/github/crittscott/somegoogly/gametest/SkullEyeGameTestsLogic.java
- orientation-code.md
