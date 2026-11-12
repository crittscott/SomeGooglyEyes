# Report: Multiloader-organization (1.21.11)

Applied 2026-11-11 by hand-porting 1.21.8's changes. Nothing was compiled or tested; everything here comes from reading the source.

## Finding 1. The place a head item keeps its eyes is spelled out separately on Fabric

**What changed**
- `SkullEyeState.java`: added `headLootCopy()`, a `LootItemFunction.Builder` copying only `ModContent.HEAD_EYES` from the block entity; deleted `copyToLoot` and its now-unused imports (`LootContext`, `Collection`); kept `stampItem` (used by `EyeItemService.markHeadDrop` and three gametest logic classes).
- `FabricServerEvents.java`: the `LootTableEvents.MODIFY` hook applies `SkullEyeState.headLootCopy()`; unused imports (`ModContent`, `CopyComponentsFunction`, `LootContextParams`) dropped.
- Forge and NeoForge `HeadEyesLootModifier.java`: a memoized `COPY_EYES` (`Suppliers.memoize`) built from `headLootCopy()`; `doApply` checks that the context's `BLOCK_ENTITY` is a `SkullBlockEntity` with `SkullEyeState.hasEyes(skull)`, then applies `COPY_EYES` to each generated stack with `SkullEyeState.entityFor(stack) != null`. Forge now matches 1.21.8's exactly; NeoForge differs only by keeping its javadoc clause about other mods' heads such as the Heads mod's. Both files were CRLF in the working tree and are now LF.
- `SkullEyeGameTestsLogic.java`: `brokenHeadKeepsItsEyes` also clears the eyes, breaks the head again, and asserts the plain drop has no `HEAD_EYES`; its javadoc gains the matching in-game step.
- `orientation-code.md`: the head-loot sentence names `SkullEyeState.headLootCopy` for all three loaders (still under 12,000 characters).

**Deviations from 1.21.8**
- `headLootCopy()` uses `CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)`, the form 1.21.11's Fabric hook already used (1.21.8: `copyComponents(Source.BLOCK_ENTITY)`).
- The head-item test stays `entityFor(stack) != null`, which in 1.21.11 includes Heads-mod head items.
- NeoForge javadoc keeps its Heads-mod sentence.

**Recalled API facts (unverified)**
- `copyComponentsFromBlockEntity(...).include(...)` yields a `LootItemFunction.Builder`.
- `LootItemFunction` is a `BiFunction<ItemStack, LootContext, ItemStack>`; `apply(stack, context)` returns the stack to keep.
- The copy reads the block entity's implicit components; `SkullBlockEntityDataMixin` sets `HEAD_EYES` in `collectImplicitComponents` only when the snapshot has eyes. New dependency: Heads-mod block entities are `SkullBlockEntity`s and run that injection (the old `copyToLoot` read `SkullEyeState.snapshot` directly).

**Left undone:** nothing. review-Dead-code.md's "Head-loot follow-up" points here; left for that topic's file.

**Clash points:** FabricServerEvents.java, SkullEyeState.java, SkullEyeGameTestsLogic.java, orientation-code.md.

**In-game checks**
- Each loader: break an eyed head (drops with eyes; placing restores them) and a plain head (drop stacks with ordinary skulls).
- NeoForge with Heads: an eyed Heads-mod head drops with its eyes (`headsModHeadTakesKeepsAndGivesUpEyes` covers it when Heads is present).
