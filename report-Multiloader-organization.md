# Report: Multiloader-organization (26.3)

Applied 2026-11-12 from 26.2's post-fix hunks (26.2 has written report-Multiloader-organization.md). Nothing was compiled, built, or tested. The Fix line in review-Multiloader-organization.md is filled in.

## 1. The place a head item keeps its eyes is spelled out separately on Fabric
- common/.../eye/state/SkullEyeState.java: `headLootCopy()` added after `hasEyes` (a `LootItemFunction.Builder` copying only `ModContent.HEAD_EYES` from the block entity); `copyToLoot` deleted with its now-unused imports (`LootContext`, `java.util.Collection`); `CopyComponentsFunction` and `LootItemFunction` imports added; `LootContextParams` kept (used by `headLootCopy`); `stampItem` kept.
- fabric/.../server/fabric/FabricServerEvents.java: the `LootTableEvents.MODIFY` hook applies `SkullEyeState.headLootCopy()`; `ModContent`, `CopyComponentsFunction`, `LootContextParams` imports dropped (`ModContent` had no other use in the file).
- forge/.../server/forge/HeadEyesLootModifier.java and neoforge/.../server/neoforge/HeadEyesLootModifier.java: memoized `COPY_EYES` (Guava `Suppliers.memoize`); `doApply` checks for a `SkullBlockEntity` with eyes and replaces each drop whose `entityFor(stack) != null` with `COPY_EYES.get().apply(stack, context)`; class javadoc gains "It copies only a head's eyes onto that head's item." CRLF kept.
- common/src/gametest/.../SkullEyeGameTestsLogic.java `brokenHeadKeepsItsEyes`: after the restore check, discards the first drop, clears the placed head's eyes, breaks it again, and expects exactly one plain skeleton skull without `HEAD_EYES`; javadoc gains the in-game step.
- orientation-code.md: head loot "copies the state with `SkullEyeState.headLootCopy`" (Forge/NeoForge `HeadEyesLootModifier`, Fabric `LootTableEvents.MODIFY`). Body is 11,883 characters, under the limit.

## Adaptations to 26.3 API
- Both `HeadEyesLootModifier`s read the block entity with `context.getOptional(LootContextParams.BLOCK_ENTITY)` where 26.2 has `getOptionalParameter`.
- NeoForge keeps 26.3's `(Optional<Holder<LootItemCondition>> condition, int priority)` constructor and its `Holder`/`Optional` imports.
- After these edits both loader files differ from 26.2's only in those lines.

## Not touched
- docs/javadoc/ still names `copyToLoot`; it is generated output and is refreshed by the javadoc build.
- Other topics' hunks in the same files (SkullEyeState `isHeadPart`/`set()` null-level, FabricServerEvents `SomeGooglyCommon.id`/`onConverted`/`onPlayerCloned`/`onHurt` and `EyeState`/`Player` imports, SkullEyeGameTestsLogic `EyeAppearance` import, PASS/FAIL assertions, denied-item-use javadoc) were left alone.

## Recalled API facts (unverified)
`CopyComponentsFunction.copyComponentsFromBlockEntity(...).include(...)` returns a `LootItemFunction.Builder`; `LootItemFunction.apply(stack, context)` returns the stack to keep; Guava `Suppliers` is on the Forge/NeoForge classpaths; 26.3's `LootContext.getOptional` exists on Forge as well as NeoForge (same vanilla class); the copy reads the block entity's implicit components, which `SkullBlockEntityDataMixin` fills only when the head has eyes.

## First build and in-game check
The first build settles whether the new imports and the `headLootCopy()` builder type compile on all three loaders, and whether `brokenHeadKeepsItsEyes` passes with its plain-head step on each loader. In game on each loader: break an eyed head (drop keeps eyes, placing restores them); break a plain head (drop stacks with ordinary skulls).

## GameTests
None added or removed on any loader (deltas 0/0/0); `brokenHeadKeepsItsEyes` extended.
