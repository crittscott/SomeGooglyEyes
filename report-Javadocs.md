# Report: Javadocs (26.1)

Applied 2026-11-11 by hand-porting 1.21.11's changes onto the current 26.1 code, after all other topics. Nothing was compiled, built, or tested; everything here comes from reading the source. Taken whole from 1.21.11: GuiItemEyeGameTestsLogic, HandItemEyeGameTestsLogic, BoatEyeGameTestsLogic, FramedEyeGameTestsLogic (their code already matched 1.21.11's exactly; only docs differed), with one Framed doc changed back. Every other file was edited in place. Fix lines in review-Javadocs.md are filled in.

## 1
None.

## 2. Boat, framed, GUI, and hand-item test docs
Every boat, framed, GUI, and hand test doc is 1.21.11's, matched by test name. Deviation: `framedHeadUsesConfiguredLayout` uses 1.21.8's Heads-free wording. `exportOverlayCombinesDraftAges` takes 1.21.11's text.

## 4. Mixin javadocs
The nine common client mixins take 1.21.11's class javadocs, checked against 26.1's hooks: BoatRendererMixin "submits them in `submit` before vanilla restores the composed hull pose"; ItemFrameRendererMixin "whose own `submit` then scopes it"; the three render-state mixins the "no slot for another mod's data" sentence; LivingEntityRendererMixin the "last point that sees the entity itself" sentence. GUI deviations for 26.1's extraction: ContainerItemEyesMixin brackets each slot's and the carried item's extraction (`extractSlot`, `extractFloatingItem`); HotbarItemEyesMixin brackets the hotbar's extraction (`extractItemHotbar`); GuiItemEyesMixin selects for every GUI item as it is extracted (`GuiGraphicsExtractor.item`). New: SpecialModelWrapperMixin says a special model's `extractArgument` sees only the stack, its `submit` gets no display context, and no loader hook reaches the extracted argument (recalled, not checked).

## 5. Counts and numbers
ClientNetworkHandler drops "five"; EyeBehavior drops "eight"; ServerBehaviorScheduler links `ServerConfig#GLOBAL_PERCENT`; PickerExporter links `PickerGate#EXPORT_COOLDOWN_TICKS`; PickerExportPacket says "the export cooldown"; ServerConfig's `ambientBehaviorPool` comment uses a new private `availableBehaviors()` over `EyeBehavior.values()` (the only code change: the generated config comment lists every expression id). ContentRegistrar was deleted by Loader-divergence 4.

## 6. Fabric docs vouching for Forge
FabricClientConfig and FabricServerConfig take 1.21.11's text. Both HeadEyesLootModifier docs already carry the sentence (Multiloader-organization 1).

## 7. History and hypotheticals
ThirdPartyModelWraps drops "(those models already worked for adults)"; ServerBehaviorScheduler's trigger list reads "hurt/milked/sheared → grow".

## 8. Undocumented members and unreproducible steps
`BoatEyeLayout.isEligible` and `BehaviorSettings.write` take 1.21.11's javadocs. GuiItemEyes and HandItemEyes are deleted. "No in-game form" text on `refusedBulkSpawnLeavesTerrain`, `deniedItemUseLeavesHeadsUntouched`, `longSegmentsCompareAsNumbers`, `loaderNamesDoNotChangeAVersion`. `selfRemoveWithShearsDropsAnEyeAndCostsDurability` already links `LoaderInteractionGameTestsLogic#useHooksHonorResults`. Duplicate stacked javadocs removed on `playerHolding` and `receivedExpressionsOutliveRenderTrackers`; no others in 26.1.

## Extra
`EyeBehavior.defaultDuration()`'s javadoc ("when triggered without an explicit duration") was untrue after Networking 2; it now reads "Length in ticks of one play of this expression, whether ambient or triggered; no play sets its own."

## 9
None.

## Recalled API facts (unverified)
No loader has a non-living render event exposing the boat hull pose, a per-slot or carried-item GUI event, an event around a single GUI item's extraction, a render-state-extraction event, a data slot on vanilla render states, or a hook reaching a special model's extracted argument; Forge and NeoForge frame-item render events fire before the item is submitted with no after-event, Fabric has none; 26.1's `SpecialModelRenderer.extractArgument(ItemStack)` takes no display context; `ServerConfig`'s static initializer may call the later-declared static `availableBehaviors()`.

## In-game checks
A fresh `somegoogly-server.toml` lists every expression id after "Available:" in the `ambientBehaviorPool` comment.

## Clash points
ServerConfig, ServerBehaviorScheduler, EyeBehavior, BehaviorSchedulerGameTestsLogic, the four test files copied whole, the GUI mixins.

## GameTests
None added or removed.

## Files modified
Gametest logic: BoatEye, FramedEye, GuiItemEye, HandItemEye, PickerExport, PickerSpawnService, SkullEye, VersionRange, BehaviorScheduler. Mixins: BoatRenderState, BoatRenderer, ContainerItemEyes, GuiItemEyes, HotbarItemEyes, ItemFrameRenderer, LivingEntityRenderState, LivingEntityRenderer, SkullBlockRenderState, SpecialModelWrapper. Main: ServerConfig, ServerBehaviorScheduler, EyeBehavior, BehaviorSettings, BoatEyeLayout, ClientNetworkHandler, PickerExporter, PickerExportPacket, ThirdPartyModelWraps. Fabric: FabricClientConfig, FabricServerConfig. Docs: review-Javadocs.md.
