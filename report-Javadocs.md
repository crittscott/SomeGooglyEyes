# Report: Javadocs (26.2)

Applied 2026-11-12 with the Edit tool only, hunk by hunk, while other topics edited the same files. Nothing was compiled, built, or tested. Each hunk was found by diffing 26.1.1 against 26.2 and taking only comment hunks this topic's 26.1.1 report attributes to it; 26.2's API names and code were kept. Fix lines in review-Javadocs.md filled in.

## 1, 9
None.

## 2. Boat, framed, GUI, and hand-item test docs
- BoatEyeGameTestsLogic: 13 test docs take 26.1.1's text (`EntityTypes` names and other code untouched).
- FramedEyeGameTestsLogic: 12 test docs; `framedHeadUsesConfiguredLayout` is Heads-free.
- GuiItemEyeGameTestsLogic: 7 test docs.
- HandItemEyeGameTestsLogic: 7 test docs.
- PickerExportGameTestsLogic: `exportOverlayCombinesDraftAges` names the `"age": "any"`, `"enabled": false` entry.
No deviation.

## 4. Mixin javadocs
Class javadocs from 26.1.1 on BoatRenderStateMixin, BoatRendererMixin, ItemFrameRendererMixin, LivingEntityRenderStateMixin, LivingEntityRendererMixin, SkullBlockRenderStateMixin, ContainerItemEyesMixin, GuiItemEyesMixin, HotbarItemEyesMixin, SpecialModelWrapperMixin. 26.1.1's HotbarItemEyesMixin doc names no class ("HUD-layer hook"), so it applies as written; `@Mixin(Hud.class)` kept.

## 5. Counts and numbers
ClientNetworkHandler drops "five"; EyeBehavior drops "eight"; ServerBehaviorScheduler's "5%" is `{@link ServerConfig#GLOBAL_PERCENT}`; PickerExporter links `PickerGate#EXPORT_COOLDOWN_TICKS`; PickerExportPacket says "the export cooldown"; ServerConfig's `ambientBehaviorPool` comment calls a new private `availableBehaviors()` over `EyeBehavior.values()` (imports `java.util.Arrays`, `java.util.stream.Collectors`), the topic's only code change. ContentRegistrar was already absent from 26.2 when applied.

## 6. Fabric docs vouching for Forge
FabricClientConfig and FabricServerConfig take 26.1.1's text. Both HeadEyesLootModifier docs (forge, neoforge) already had "It copies only a head's eyes onto that head's item." (Multiloader-organization), so they were left; no diff against 26.1.1.

## 7. History and an incomplete list
ThirdPartyModelWraps drops "(those models already worked for adults)"; ServerBehaviorScheduler's trigger list reads "hurt/milked/sheared → grow".

## 8. Undocumented members and unreproducible steps
- `BoatEyeLayout.isEligible` and `BehaviorSettings.write` get 26.1.1's one-line javadocs.
- "No in-game form" text on `refusedBulkSpawnLeavesTerrain` (PickerSpawnServiceGameTestsLogic), `deniedItemUseLeavesHeadsUntouched` (SkullEyeGameTestsLogic), `longSegmentsCompareAsNumbers` and `loaderNamesDoNotChangeAVersion` (VersionRangeGameTestsLogic).
- `selfRemoveWithShearsDropsAnEyeAndCostsDurability` (EyeItemServiceGameTestsLogic): its protection-mod paragraph now links `LoaderInteractionGameTestsLogic#useHooksHonorResults`. Deviation: 26.1.1 got this hunk from another topic, but 26.2's verdict lists the site under this finding and no other 26.2 review claims it.
- `EyeBehavior.defaultDuration()` already had 26.1.1's javadoc when applied (Networking); not touched.
- GuiItemEyes and HandItemEyes were already deleted by Organization, so their `clear`/`endPresentation`/`remove`/`tick` members were skipped.

## Recalled API facts (unverified, as 26.1.1)
- No loader has a render event for non-living entities that exposes the boat hull pose.
- No loader has a per-slot or carried-item GUI event, or an event around extracting one GUI item.
- No loader has a render-state extraction event, or a hook that reaches a special model's extracted argument.
- Forge and NeoForge frame-item render events fire before submission, with no matching event after; Fabric has neither.
- `SpecialModelRenderer.extractArgument` takes no display context.
- `ServerConfig`'s static initializer can call the later-declared static `availableBehaviors()`.

## In-game check
A fresh `somegoogly-server.toml` lists every expression id after "Available:" in the `ambientBehaviorPool` comment.

## First build settles
`ServerConfig` compiles with `availableBehaviors()` and its imports; everything else is comment-only.

## GameTests
None added or removed on any loader.

## Other topics' hunks seen (left alone)
- GuiItemEyeGameTestsLogic: 26.1.1's `nestedDrawContextsRestoreOuterSelection` test and its move off `GuiItemEyes` (Organization).
- FramedEyeGameTestsLogic and BoatEyeGameTestsLogic: code hunks for `PresentationEyes.create`, `TestBehaviorSettings`, `TestEyeRuntime.BoatCache` and `ClientEyeRuntime.onBoatState`.
- EyeItemServiceGameTestsLogic: 26.2 has the shared-fake-player `setShiftKeyDown(false)` reset, which 26.1.1 lacks.
- ContainerItemEyesMixin, GuiItemEyesMixin and HotbarItemEyesMixin: Organization's code changes arrived while this was applied; only class-doc lines were touched here.
