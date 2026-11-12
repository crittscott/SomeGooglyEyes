# Report: Multiloader-organization (26.1)

Applied 2026-11-11 by hand-porting 1.21.11's changes. Nothing was compiled or tested; everything here comes from reading the source. The Fix line in review-Multiloader-organization.md is filled in.

## 1. The place a head item keeps its eyes is spelled out separately on Fabric
- eye/state/SkullEyeState.java: `headLootCopy()` added, a `LootItemFunction.Builder` copying only `ModContent.HEAD_EYES` from the block entity (`CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY).include(...)`). `copyToLoot` deleted with the imports only it used (`LootContext`, `Collection`). `stampItem` kept (`EyeItemService.markHeadDrop` and the Framed, GuiItem, and HandItem gametest logic use it).
- FabricServerEvents: the `LootTableEvents.MODIFY` hook applies `SkullEyeState.headLootCopy()`; unused imports (`ModContent`, `CopyComponentsFunction`, `LootContextParams`) dropped.
- Forge and NeoForge HeadEyesLootModifier: memoized `COPY_EYES` (`Suppliers.memoize(() -> SkullEyeState.headLootCopy().build())`). `doApply` requires the context's `BLOCK_ENTITY` to be a `SkullBlockEntity` with eyes, then applies `COPY_EYES` to each stack with `SkullEyeState.entityFor(stack) != null`. Both class javadocs gain "It copies only a head's eyes onto that head's item." (Javadocs 6). CRLF kept; Forge's file matches 1.21.11's after normalizing line endings.
- SkullEyeGameTestsLogic: `brokenHeadKeepsItsEyes` then discards the first drop, clears the eyes (`Snapshot.NONE`), breaks the head again, and asserts exactly one plain skull drops with no `HEAD_EYES`; javadoc gains the in-game step.
- orientation-code.md: head loot copies the state with `SkullEyeState.headLootCopy` on all three loaders (body 11,878 characters).

## Deviations from 1.21.11
NeoForge's loot-modifier javadoc and the orientation-code.md sentence leave out 1.21.11's Heads-mod clauses (26.1 has no Heads integration). No Heads-mod code brought in.

## Recalled API facts (unverified)
`copyComponentsFromBlockEntity(...).include(...)` returns a `LootItemFunction.Builder`; `LootItemFunction` is a `BiFunction<ItemStack, LootContext, ItemStack>` whose `apply` returns the stack to keep; Guava's `Suppliers.memoize` is on the Forge and NeoForge classpaths. The copy reads the block entity's implicit components; `SkullBlockEntityDataMixin` sets `HEAD_EYES` in `collectImplicitComponents` only when the snapshot has eyes (the old `copyToLoot` read the snapshot directly).

## In-game checks
On each loader: break an eyed head; the drop carries its eyes and placing it restores them. Break a plain head; the drop stacks with ordinary skulls.

## Left to other topics
Dead-code's "Head-loot follow-up" is settled by this change; Dead-code should mark it done.

## Clash points
FabricServerEvents, SkullEyeState, SkullEyeGameTestsLogic, both HeadEyesLootModifier files, orientation-code.md.

## GameTests
None added or removed; `brokenHeadKeepsItsEyes` extended.

## Files modified
common main: eye/state/SkullEyeState.java. Gametest: SkullEyeGameTestsLogic. Loaders: FabricServerEvents, Forge HeadEyesLootModifier, NeoForge HeadEyesLootModifier. Docs: orientation-code.md, review-Multiloader-organization.md.
