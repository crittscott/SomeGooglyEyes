# Report: Multiloader-organization (26.2)

Applied 2026-11-12 by taking 26.1.1's hunks for this topic. Nothing was compiled, built, or tested. The Fix line in review-Multiloader-organization.md is filled in.

## 1. The place a head item keeps its eyes is spelled out separately on Fabric
- common/.../eye/state/SkullEyeState.java: `headLootCopy()` added (a `LootItemFunction.Builder` copying only `ModContent.HEAD_EYES` from the block entity), placed alphabetically after `hasEyes`; `copyToLoot` deleted with its now-unused imports (`LootContext`, `java.util.Collection`); `CopyComponentsFunction` and `LootItemFunction` imports added; `LootContextParams` kept (used by `headLootCopy`); `stampItem` kept (EyeItemService and gametests use it).
- fabric/.../server/fabric/FabricServerEvents.java: the `LootTableEvents.MODIFY` hook applies `SkullEyeState.headLootCopy()`; `ModContent`, `CopyComponentsFunction`, `LootContextParams` imports dropped.
- forge/.../server/forge/HeadEyesLootModifier.java and neoforge/.../server/neoforge/HeadEyesLootModifier.java: memoized `COPY_EYES` (Guava `Suppliers.memoize`); `doApply` checks for a `SkullBlockEntity` with eyes and replaces each drop whose `entityFor(stack) != null` with `COPY_EYES.get().apply(stack, context)`; class javadoc gains "It copies only a head's eyes onto that head's item." CRLF kept. Both files now equal 26.1.1's except NeoForge's 26.2 `(conditions, priority)` constructor.
- common/src/gametest/.../SkullEyeGameTestsLogic.java `brokenHeadKeepsItsEyes`: after the restore check, discards the first drop, clears the placed head's eyes, breaks it again, and expects exactly one plain skeleton skull without `HEAD_EYES`; javadoc gains the in-game step.
- orientation-code.md: head loot "copies the state with `SkullEyeState.headLootCopy`" (Forge/NeoForge `HeadEyesLootModifier`, Fabric `LootTableEvents.MODIFY`).

## Deviations from 26.1.1
- None in substance. Used 26.1.1's `SkullBlockEntity` test, not 26.1.2's `SkullEyeState.Holder` test (26.2 has no Twilight trophies, skull candles, or Heads heads). NeoForge's `priority` constructor parameter is 26.2's API and was kept.

## Recalled API facts (unverified)
`CopyComponentsFunction.copyComponentsFromBlockEntity(...).include(...)` returns a `LootItemFunction.Builder`; `LootItemFunction.apply(stack, context)` returns the stack to keep; Guava `Suppliers` is on the Forge/NeoForge classpaths; the copy reads the block entity's implicit components, which `SkullBlockEntityDataMixin` fills only when the head has eyes.

## First build and in-game check
The first build settles whether the new imports and the `headLootCopy()` builder type compile on all three loaders, and whether `brokenHeadKeepsItsEyes` passes with its plain-head step on each loader. In game on each loader: break an eyed head (drop keeps eyes, placing restores them); break a plain head (drop stacks with ordinary skulls).

## GameTests
None added or removed on any loader (deltas 0/0/0); `brokenHeadKeepsItsEyes` extended.

## Other topics' hunks seen (left alone)
- SkullEyeState: `isHeadPart` -> `EyeConfigModel.lastAttachmentSegment`; the `Level` import and `set()` null-level simplification belong to Guarding.
- FabricServerEvents: `SomeGooglyCommon.id`, `onConverted`, `onPlayerCloned`, `onHurt`, `EyeState`/`Player` imports.
- SkullEyeGameTestsLogic: `EyeAppearance` import location, PASS/FAIL assertions, denied-item-use javadoc.
- Clash points: SkullEyeState, FabricServerEvents (rewritten whole by a script, against the brief; check other topics' hunks survived), SkullEyeGameTestsLogic, orientation-code.md line 51.
