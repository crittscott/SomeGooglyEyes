# Report: Javadocs (26.1.1)

Applied 2026-11-11 with the Edit tool only (no whole-file copies). Nothing was compiled, built, or tested. All 29 files still differing from 26.1 differed only in Javadocs hunks. Fix lines and intro in review-Javadocs.md filled in; line endings kept.

## 1, 9
None.

## 2. Boat, framed, GUI, and hand-item test docs
Every test doc in BoatEyeGameTestsLogic (13), FramedEyeGameTestsLogic (12), GuiItemEyeGameTestsLogic (7), HandItemEyeGameTestsLogic (7) takes 26.1's text (`framedHeadUsesConfiguredLayout` without the Heads mod); `exportOverlayCombinesDraftAges` in PickerExportGameTestsLogic too.

## 4. Mixin javadocs
26.1's class javadocs on BoatRenderStateMixin, BoatRendererMixin, ItemFrameRendererMixin, LivingEntityRenderStateMixin, LivingEntityRendererMixin, SkullBlockRenderStateMixin, ContainerItemEyesMixin, GuiItemEyesMixin, HotbarItemEyesMixin (worded for extraction), and SpecialModelWrapperMixin (says why no loader hook was used).

## 5. Counts and numbers
ClientNetworkHandler drops "five", EyeBehavior "eight"; ServerBehaviorScheduler "5%" → `{@link ServerConfig#GLOBAL_PERCENT}`; PickerExporter links `PickerGate#EXPORT_COOLDOWN_TICKS`; PickerExportPacket says "the export cooldown"; ServerConfig's `ambientBehaviorPool` comment calls a new private `availableBehaviors()` reading `EyeBehavior.values()` (`Arrays`, `Collectors` imports) — the topic's only code change. ContentRegistrar already deleted by Loader-divergence 4.

## 6. Fabric docs vouching for Forge
FabricClientConfig and FabricServerConfig take 26.1's text; both HeadEyesLootModifier docs already had the "copies only a head's eyes" sentence.

## 7. History and an incomplete list
ThirdPartyModelWraps drops "(those models already worked for adults)"; ServerBehaviorScheduler's trigger list reads "hurt/milked/sheared → grow".

## 8. Undocumented members and unreproducible steps
`BoatEyeLayout.isEligible` and `BehaviorSettings.write` get 26.1's javadocs; `refusedBulkSpawnLeavesTerrain`, `deniedItemUseLeavesHeadsUntouched`, `longSegmentsCompareAsNumbers`, `loaderNamesDoNotChangeAVersion` say there is no in-game form; `EyeBehavior.defaultDuration()` gets 26.1's corrected javadoc. GuiItemEyes/HandItemEyes were deleted by Organization 2; the self-shear test doc and BehaviorSchedulerGameTestsLogic already matched.

## Recalled API facts (unverified, as 26.1)
No loader has a non-living render event exposing the boat hull pose, a per-slot or carried-item GUI event, an event around extracting one GUI item, a render-state extraction event, or a hook reaching a special model's extracted argument; Forge/NeoForge frame-item render events fire before submission with none after, Fabric has neither; `SpecialModelRenderer.extractArgument` takes no display context; `ServerConfig`'s static initializer can call the later-declared static `availableBehaviors()`.

## In-game check
A fresh `somegoogly-server.toml` lists every expression id after "Available:" in the `ambientBehaviorPool` comment.

## First build settles
`ServerConfig` compiles with `availableBehaviors()` and its imports; everything else is comment-only.

## Remaining differences from 26.1 (common, fabric, forge, neoforge src)
With `diff -rq --strip-trailing-cr`, only the 20 vanilla boat and raft definitions (`version` selector "26.1.1"), as expected. 46 further files differ only in line endings (mostly CRLF here, LF in 26.1; SlimyEyeItem and Fabric BehaviorSchedulerGameTests the reverse); content identical.

## GameTests
None added or removed.
