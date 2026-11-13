# Report: Javadocs (26.1.2)

Applied 2026-11-12, unbuilt. Method: each 26.1.1 file was diffed against 26.1.2 with `diff --strip-trailing-cr`. Where the whole diff was this topic's and the file had no 26.1.2 addition, the 26.1.1 file was copied whole with matching line endings. PickerExporter was copied with its CRLF converted to 26.1.2's LF. Where a file also held other changes, only this topic's hunks were taken with the Edit tool. The seven Twilight mixin javadocs and the `TwilightHeadGameTests` class javadoc were written new for 26.1.2's hooks. Nothing was compiled, built, or tested. After the edits, the only 26.1.1 comment lines missing from 26.1.2 belong to 26.1.2's Heads and Twilight code or to other topics, plus the intended `framedHeadUsesConfiguredLayout` deviation.

## 1. Some GameTest in-game steps are wrong or stale
None.

## 2. Boat, framed, GUI, and hand-item test docs
- BoatEyeGameTestsLogic, GuiItemEyeGameTestsLogic, HandItemEyeGameTestsLogic, and FramedEyeGameTestsLogic: copied whole from 26.1.1. They had doc-only diffs and no 26.1.2 additions, and both sides are LF.
- `framedHeadUsesConfiguredLayout`: then given 1.21.11's wording with the Heads clause ("frame an eyed skeleton skull, and with the Heads mod installed an eyed Heads-mod head; each shows …"). This deviates from 26.1.1 because 26.1.2 has Heads.
- PickerExportGameTestsLogic: only the `exportOverlayCombinesDraftAges` doc hunk was taken. The file's `SomeGooglyCommon.id` lines come from another topic and were kept.

## 4. Mixin javadocs
- Common client mixins: copied whole from 26.1.1, since their diffs were doc-only and line endings match. The ten files are BoatRenderStateMixin, BoatRendererMixin, ContainerItemEyesMixin, GuiItemEyesMixin, HotbarItemEyesMixin, ItemFrameRendererMixin, LivingEntityRenderStateMixin, LivingEntityRendererMixin, SkullBlockRenderStateMixin, and SpecialModelWrapperMixin.
- New class javadocs on the seven NeoForge Twilight mixins, modeled on 1.21.1's and written for 26.1.2's hooks:
  - `client/TwilightTrophyRendererMixin` chooses eyes at `extractRenderState` and brackets `submit` with a `ClientEyeRuntime` draw context, inside which the trophy model mixins draw, as `SkullBlockRendererMixin` does.
  - `client/TwilightSkullCandleRendererMixin` does the same for skull candles, so the skull model submitted inside gets the eyes as a vanilla head does under `SkullBlockRendererMixin`.
  - `client/TwilightTrophyModelMixin` draws eyes on the first part `renderTrophy` submits, at that call, within the current draw context (a placed trophy's or a trophy item's).
  - `client/TwilightKnightTrophyModelMixin` does the same for the part submitted by the six-argument `submitModelPart`.
  - Both model mixins note that their `@ModifyArgs` only reads arguments.
  - `client/TwilightTrophyRenderStateMixin` explains why the eyes ride the state: no slot exists in the render state, and neither Twilight Forest nor any loader offers one. This follows SkullBlockRenderStateMixin.
  - `TwilightSkullConversionMixin` (CRLF kept) covers hand and dispenser candle conversion, which Twilight does with no API or event, replacing the block entity directly. Each redirect still makes the original call.
  - `TwilightTrophyDataMixin` (CRLF kept) gives trophies the saved, synced, and item-carried eye state of `SkullBlockEntityDataMixin` as a `SkullEyeState.Holder`.
  - Every one of these docs says Twilight Forest is optional with no API, and that its renderers and block entities bypass the vanilla head paths.
- Deviations from 1.21.1:
  - There is no item-renderer mixin in 26.1.2, so trophy items are described as drawing within the item draw context.
  - The skull-candle doc does not claim the mixin draws eyes itself; in 26.1.2 it only brackets.

## 5. Counts and numbers
Copied whole from 26.1.1, since the diffs were doc-only plus the topic's ServerConfig code:
- ClientNetworkHandler and EyeBehavior: drop "five" and "eight".
- ServerBehaviorScheduler: `{@link ServerConfig#GLOBAL_PERCENT}`.
- PickerExporter: links `PickerGate#EXPORT_COOLDOWN_TICKS`. CRLF was converted to 26.1.2's LF.
- PickerExportPacket: "the export cooldown".
- ServerConfig: new `availableBehaviors()` plus the `Arrays` and `Collectors` imports.
- ContentRegistrar: already deleted.

No deviation.

## 6. Fabric docs vouching for Forge
- FabricClientConfig and FabricServerConfig: copied whole from 26.1.1.
- Both HeadEyesLootModifier docs already have "It copies only a head's eyes onto that head's item." This was verified only, and NeoForge's Heads clause was kept.

## 7. History and an incomplete list
- ThirdPartyModelWraps (copied whole, CRLF both sides): drops "(those models already worked for adults)".
- ServerBehaviorScheduler: "hurt/milked/sheared → grow", taken in the finding 5 copy.

## 8. Undocumented members and unreproducible steps
- BoatEyeLayout (CRLF) and BehaviorSettings: copied whole, which gives `isEligible` and `write` their javadocs.
- EyeBehavior's copy brings the `defaultDuration()` javadoc.
- These docs now say there is no in-game form: `refusedBulkSpawnLeavesTerrain`, `deniedItemUseLeavesHeadsUntouched`, `longSegmentsCompareAsNumbers`, and `loaderNamesDoNotChangeAVersion`. These were taken as hunks, because SkullEyeGameTestsLogic also has Heads tests.
- Already deleted: GuiItemEyes, HandItemEyes, and HeldEye.
- Unchanged: the self-shear test doc already matches 26.1.1, and BehaviorSchedulerGameTestsLogic has no duplicate javadoc above `receivedExpressionsOutliveRenderTrackers`.
- NeoForge `TwilightHeadGameTests` (CRLF kept): new class javadoc covering the Twilight Forest-only scenarios. These are hand and dispenser candle conversion keeping a head's eyes, and trophies taking, keeping, and giving up eyes and saving only their eye state. The tests need Twilight Forest loaded and fail without it.

## 9. ChildMapResolver
None.

## Left for other topics / the parent
- The Fix lines in review-Javadocs.md.
- The remaining 26.1.1-vs-26.1.2 differences are 26.1.2's Heads and Twilight additions and other topics' hunks: SkullEyeRenderer, SkullEyeState, EyeItemService, the loot modifiers, and so on.
- The empty `common/.../client/mixin` and `common/.../server/mixin` directories exist in 26.1.2 only. They were not touched.

## Recalled API facts and checks only a build or in-game test can settle
Inherited from 26.1.1 and unverified:
- No loader has any of these:
  - a non-living render event that exposes the boat hull pose
  - a per-slot or carried-item GUI event
  - an event around a single GUI item
  - a render-state extraction event
  - a hook that reaches a special model's extracted argument
- Forge and NeoForge frame-item events fire only before submission.
- `extractArgument` takes no display context.
- `ServerConfig`'s static initializer can call the later-declared `availableBehaviors()`.

The Twilight docs rest on these assumptions:
- Twilight Forest's `SkullCandleRenderer.submit` submits its skull through vanilla `SkullBlockRenderer.submitSkull`, where `SkullBlockRendererMixin` draws the eyes. The candle mixin itself only brackets.
- The Knight Phantom's six-argument `submitModelPart` call is the part the eyes sit on.
- Trophy items' `renderTrophy` runs inside the `ItemStackRenderStateMixin` bracket.

First build: only ServerConfig's `availableBehaviors()` and its imports are code. Everything else is comment-only.

## In-game checks
- A fresh `somegoogly-server.toml` lists every expression id after "Available:" in the `ambientBehaviorPool` comment.

## Fix lines
2: Every boat, framed, GUI, and hand test doc takes 26.1.1's text. BoatEyeGameTestsLogic, FramedEyeGameTestsLogic, GuiItemEyeGameTestsLogic, and HandItemEyeGameTestsLogic were copied whole, since only docs differed. `exportOverlayCombinesDraftAges` (PickerExportGameTestsLogic) takes 26.1.1's text as a hunk. Deviation: `framedHeadUsesConfiguredLayout` takes 1.21.11's wording with the Heads clause. Unbuilt.
4: The ten common client mixins' class javadocs were copied whole from 26.1.1. All seven NeoForge Twilight mixins have new class javadocs in the manner of 1.21.1's, written for 26.1.2's hooks. Each says what it hooks, that Twilight Forest is optional with no API, and that its renderers and block entities bypass vanilla's head paths. The trophy and skull-candle renderer mixins bracket `submit` with a `ClientEyeRuntime` draw context. The two trophy-model mixins draw on the trophy part within the current draw context, placed or item, and their `@ModifyArgs` only reads. TwilightTrophyRenderStateMixin says why the eyes ride the state. Deviation from 1.21.1: there is no item-renderer mixin, and the candle mixin only brackets. Unbuilt.
5: ClientNetworkHandler drops "five" and EyeBehavior drops "eight". ServerBehaviorScheduler's "5%" is `{@link ServerConfig#GLOBAL_PERCENT}`. PickerExporter links `PickerGate#EXPORT_COOLDOWN_TICKS`, and PickerExportPacket says "the export cooldown". ServerConfig's `ambientBehaviorPool` comment uses a new private `availableBehaviors()` over `EyeBehavior.values()`. All were copied whole from 26.1.1; ContentRegistrar was already deleted by Loader-divergence 4. No deviation. Unbuilt.
6: FabricClientConfig and FabricServerConfig take 26.1.1's text, with no Forge or NeoForge vouching. Both HeadEyesLootModifier docs already carry "It copies only a head's eyes onto that head's item." (Multiloader-organization); this was verified only. Unbuilt.
7: ThirdPartyModelWraps drops "(those models already worked for adults)". ServerBehaviorScheduler's trigger list reads "hurt/milked/sheared → grow". Unbuilt.
8: `BoatEyeLayout.isEligible` and `BehaviorSettings.write` gain 26.1.1's javadocs, and `EyeBehavior.defaultDuration()` gets 26.1.1's corrected javadoc. GuiItemEyes, HandItemEyes, and HeldEye were already deleted by Organization. Four test docs gain "No in-game form" text: `refusedBulkSpawnLeavesTerrain`, `deniedItemUseLeavesHeadsUntouched`, `longSegmentsCompareAsNumbers`, and `loaderNamesDoNotChangeAVersion`. The self-shear doc was unchanged. BehaviorSchedulerGameTestsLogic had no duplicate javadoc. As in 1.21.1, NeoForge `TwilightHeadGameTests` gains a class javadoc, adjusted to 26.1.2's four tests: candle conversion by hand and dispenser, and trophy eyes and saved state. The tests need Twilight Forest loaded. Unbuilt.
