# Report: Multiloader-organization (26.1.1)

Applied 2026-11-11 by taking 26.1's hunks for this topic. Nothing was compiled, built, or tested. Fix line and intro in review-Multiloader-organization.md filled in.

## 1. The place a head item keeps its eyes is spelled out separately on Fabric
- SkullEyeState: `headLootCopy()` added (a `LootItemFunction.Builder` copying only `ModContent.HEAD_EYES` from the block entity); `copyToLoot` and its now-unused imports (`LootContext`, `java.util.Collection`) deleted; `CopyComponentsFunction`, `LootItemFunction` imports added; `stampItem` kept. This settles Dead-code's head-loot follow-up.
- FabricServerEvents: the `LootTableEvents.MODIFY` hook applies `SkullEyeState.headLootCopy()`; `ModContent`, `CopyComponentsFunction`, `LootContextParams` imports dropped.
- Forge and NeoForge `HeadEyesLootModifier`: memoized `COPY_EYES` (`Suppliers.memoize`); `doApply` checks for a `SkullBlockEntity` with eyes and copies them onto each drop whose `entityFor(stack) != null`. Class javadocs gain "It copies only a head's eyes onto that head's item." (Javadocs 6). Both files copied whole from 26.1 (entire diff was this finding; CRLF kept).
- SkullEyeGameTestsLogic `brokenHeadKeepsItsEyes`: clears the eyes, breaks the head again, expects exactly one plain skull with no `HEAD_EYES`; javadoc gains the in-game step.
- orientation-code.md: head loot "copies the state with `SkullEyeState.headLootCopy`" on all three loaders.

## Deviations from 26.1
None.

## Left for other topics
- SkullEyeState: unused `Level` import, `set()` null-level simplification, `isHeadPart` → `EyeConfigModel.lastAttachmentSegment`.
- FabricServerEvents: `SomeGooglyCommon.id`, `onConverted`, `onPlayerCloned`, `onHurt`, `EyeState`/`Player` imports.
- SkullEyeGameTestsLogic: `EyeAppearance` import, PASS/FAIL assertions, denied-item-use javadoc.
- orientation-code.md: other topics' lines.

## Recalled API facts (unverified)
`copyComponentsFromBlockEntity(...).include(...)` returns a `LootItemFunction.Builder`; `LootItemFunction.apply(stack, context)` returns the stack to keep; Guava `Suppliers` is on the Forge/NeoForge classpaths; the copy reads the block entity's implicit components, which `SkullBlockEntityDataMixin` fills only when the head has eyes.

## First build and in-game check
New imports and `headLootCopy()` builder type compile on all three loaders; `brokenHeadKeepsItsEyes` passes with its plain-head step. In game on each loader: break an eyed head (drop keeps eyes, placing restores them); break a plain head (drop stacks with ordinary skulls).

## GameTests
None added or removed; `brokenHeadKeepsItsEyes` extended.
