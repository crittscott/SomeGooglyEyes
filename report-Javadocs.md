# Report: Javadocs (1.21.11)

Applied 2026-11-11 by hand-porting 1.21.8's changes. Nothing was compiled, built, or tested; everything here comes from reading the source. Each file was diffed against 1.21.8 and only this topic's hunks taken. Taken whole (code already matched 1.21.8; only docs differed): GuiItemEyeGameTestsLogic, HandItemEyeGameTestsLogic, BoatEyeGameTestsLogic (keeps 1.21.11's `vehicle.boat.AbstractBoat` import), FramedEyeGameTestsLogic (one clause then restored, see 2).

## 1. Stale in-game steps
None.

## 2. Boat, framed, GUI, and hand-item test docs
Every boat, GUI, and hand test doc is 1.21.8's, including `nestedDrawContextsRestoreOuterSelection`; 12 FramedEyeGameTestsLogic docs are 1.21.8's. Deviation: `framedHeadUsesConfiguredLayout` keeps the Heads-mod clause in 1.21.1's/1.21.4's wording. `exportOverlayCombinesDraftAges` takes 1.21.8's text.

## 4. Mixin javadocs
The eight common client mixins take 1.21.8's class javadocs adapted to 1.21.11's hooks: BoatRendererMixin "submits them in `submit` before vanilla restores the composed hull pose"; GuiItemEyesMixin "the item's render state keeps the selection until it is submitted"; ItemFrameRendererMixin "whose own `submit` then scopes it" (Forge/NeoForge events fire "before the item is submitted"); ContainerItemEyesMixin and HotbarItemEyesMixin keep "draw" (their hooked methods still render); HotbarItemEyesMixin uses 1.21.8's one-Mixin-for-every-loader reason. SkullBlockRenderStateMixin gains: "Vanilla render states have no slot for another mod's data, and no loader offers one, so the eyes ride the state as mixin fields."

## 5. Counts and numbers
ClientNetworkHandler "five" and EyeBehavior "eight" removed; ServerBehaviorScheduler "5%" → `{@link ServerConfig#GLOBAL_PERCENT}`; PickerExporter → `{@link ...PickerGate#EXPORT_COOLDOWN_TICKS}`; PickerExportPacket "the export cooldown"; ServerConfig's `ambientBehaviorPool` comment uses 1.21.8's private `availableBehaviors()` over `EyeBehavior.values()` (the only code change; the generated config comment now lists every expression). ContentRegistrar already deleted.

## 6. Fabric docs vouching for Forge
FabricClientConfig and FabricServerConfig take 1.21.8's text. Both HeadEyesLootModifier docs already had the added sentence (NeoForge keeps its Heads clause).

## 7. History and hypotheticals
ThirdPartyModelWraps drops "(those models already worked for adults)"; the scheduler list already reads "hurt/milked/sheared → grow"; GooglyGeoLayer unchanged.

## 8. Undocumented members and unreproducible steps
`BoatEyeLayout.isEligible` and `BehaviorSettings.write` take 1.21.8's javadocs; GuiItemEyes/HandItemEyes deleted by Organization 2. Four test docs take 1.21.8's "No in-game form" text: `refusedBulkSpawnLeavesTerrain`, `deniedItemUseLeavesHeadsUntouched`, `longSegmentsCompareAsNumbers`, `loaderNamesDoNotChangeAVersion`. Deviation: `selfRemoveWithShearsDropsAnEyeAndCostsDurability` has no refusal case in either checkout, so its doc points to `LoaderInteractionGameTestsLogic#useHooksHonorResults` for refusal and keeps 1.21.8's callback-ordering sentence. BehaviorSchedulerGameTestsLogic: duplicate stacked javadocs on `playerHolding` and `receivedExpressionsOutliveRenderTrackers` removed; the file now matches 1.21.8 apart from port renames.

## 9. ChildMapResolver
None.

## Recalled API facts (unverified)
No loader has a render event for non-living entities exposing the boat hull pose, a per-slot/carried-item event, an event around a single GUI item, or a render-state-extraction event; no loader offers a data slot on vanilla render states; Forge/NeoForge frame-item render events fire before the item is submitted with no matching after-event (Fabric has none); `ServerConfig`'s static initializer may call the later-declared static `availableBehaviors()`; `EyeBehavior.id().getPath()` exists.

## In-game checks
A fresh `somegoogly-server.toml` lists every expression id after "Available:" in the `ambientBehaviorPool` comment, on all three loaders.

## Files modified
Gametest logic: BoatEye, FramedEye, GuiItemEye, HandItemEye, PickerExport, PickerSpawnService, SkullEye, VersionRange, EyeItemService, BehaviorScheduler. Mixins: BoatRenderer, ContainerItemEyes, GuiItemEyes, HotbarItemEyes, ItemFrameRenderer, BoatRenderState, LivingEntityRenderer, LivingEntityRenderState, SkullBlockRenderState. Main: ServerConfig, ServerBehaviorScheduler, EyeBehavior, BehaviorSettings, BoatEyeLayout, ClientNetworkHandler, PickerExporter, ThirdPartyModelWraps, PickerExportPacket. Fabric: FabricClientConfig, FabricServerConfig. Docs: review-Javadocs.md.
