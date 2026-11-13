# Report: Javadocs (26.3)

Applied 2026-11-12 from 26.2's post-fix code (26.2/report-Javadocs.md exists). Nothing was compiled, built, or tested. For each file below, every line differing from 26.2 was a comment line, except in ServerConfig (whose whole diff is finding 5), so each 26.3 file was set to 26.2's text with 26.3's line endings kept. Files that differ from 26.2 by 26.3 API changes were not touched: BehaviorSchedulerGameTestsLogic, ConfigGameTestsLogic, OngoingBehavior, Gizmo, PickerInput, GooglyEyeRenderer, GooglyEyeItemRenderer, NeoForgeServerConfig, and both HeadEyesLootModifier files. Fix lines in review-Javadocs.md filled in.

## 1, 9
None.

## 2. Boat, framed, GUI, and hand-item test docs
BoatEyeGameTestsLogic (13 test docs; `EntityTypes` code untouched), FramedEyeGameTestsLogic (12; `framedHeadUsesConfiguredLayout` has no Heads clause), GuiItemEyeGameTestsLogic (7), HandItemEyeGameTestsLogic (7), PickerExportGameTestsLogic (`exportOverlayCombinesDraftAges` names the `"age": "any"`, `"enabled": false` entry). No deviation from 26.2.

## 4. Mixin javadocs
The ten common client mixins (BoatRenderStateMixin, BoatRendererMixin, ContainerItemEyesMixin, GuiItemEyesMixin, HotbarItemEyesMixin, ItemFrameRendererMixin, LivingEntityRenderStateMixin, LivingEntityRendererMixin, SkullBlockRenderStateMixin, SpecialModelWrapperMixin) take 26.2's class javadocs; only class-doc lines changed. HotbarItemEyesMixin's doc names no class ("HUD-layer hook"), so no Gui-to-Hud adaptation was needed; `@Mixin(Hud.class)` kept. No Twilight mixins in 26.3.

## 5. Counts and numbers
ClientNetworkHandler drops "five"; EyeBehavior drops "eight"; ServerBehaviorScheduler's "5%" is `{@link ServerConfig#GLOBAL_PERCENT}`; PickerExporter links `PickerGate#EXPORT_COOLDOWN_TICKS`; PickerExportPacket says "the export cooldown"; ServerConfig's `ambientBehaviorPool` comment calls a new private static `availableBehaviors()` joining every `EyeBehavior.values()` id path (imports `java.util.Arrays`, `java.util.stream.Collectors`), the topic's only code change. ContentRegistrar is absent from 26.3.

## 6. Fabric docs vouching for Forge
FabricClientConfig and FabricServerConfig take 26.2's text. Both HeadEyesLootModifier docs already carry "It copies only a head's eyes onto that head's item." (Multiloader-organization).

## 7. History and an incomplete list
ThirdPartyModelWraps drops "(those models already worked for adults)"; ServerBehaviorScheduler's trigger list reads "hurt/milked/sheared → grow".

## 8. Undocumented members and unreproducible steps
- `BoatEyeLayout.isEligible` and `BehaviorSettings.write` get 26.2's one-line javadocs.
- "No in-game form" text on `refusedBulkSpawnLeavesTerrain` (PickerSpawnServiceGameTestsLogic), `deniedItemUseLeavesHeadsUntouched` (SkullEyeGameTestsLogic), `longSegmentsCompareAsNumbers` and `loaderNamesDoNotChangeAVersion` (VersionRangeGameTestsLogic).
- EyeItemServiceGameTestsLogic already matched 26.2's post-fix file (Test-completeness); `EyeBehavior.defaultDuration()` already had its javadoc; GuiItemEyes and HandItemEyes no longer exist (Organization); no Twilight class doc in 26.3.

## 26.3 API adaptations
None needed.

## Recalled API facts (unverified, carried from 26.2/26.1.1)
No loader has a render event for non-living entities exposing the boat hull pose; no per-slot or carried-item GUI event or single-GUI-item extraction event; no render-state extraction event or hook reaching a special model's extracted argument; Forge and NeoForge frame-item render events fire before the item is submitted with no matching event after, and Fabric has neither; `SpecialModelRenderer.extractArgument` takes no display context; `ServerConfig`'s static initializer can call the static `availableBehaviors()` declared later in the file.

## In-game check
In a fresh `somegoogly-server.toml`, the `ambientBehaviorPool` comment lists every expression id after "Available:".

## First build settles
Whether `ServerConfig` compiles with `availableBehaviors()` and its imports. Everything else is comment-only.

## GameTests
None added or removed on any loader.
