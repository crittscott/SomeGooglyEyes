# Report: Magic-numbers (26.1.2)

Applied 2026-11-12 by taking 26.1.1's hunks for findings 2, 3, 5, 6, and 9 only (diff of each 26.1.1 file against 26.1.2's; whole-file copies only where the whole remaining diff was this topic's and the file has no 26.1.2 addition). Nothing was compiled, built, or tested. Line endings kept per file (CRLF files stay CRLF, LF files stay LF).

## 1, 4, 10
No change.

## 2. Part-path grammar
- Copied whole from 26.1.1 (only this finding's diff remained): EyeConfigModel (private `ATTACHMENT_INDEX_PREFIX`/`ATTACHMENT_SEPARATOR`; public `attachmentIndex`, `attachmentPath(Iterable)`, `attachmentPath(String, String)`, `attachmentSegments`, `lastAttachmentSegment`), EyeAttachmentResolver (`attachmentSegments`), ReflectedBoxResolver (`attachmentIndex(i)`, `attachmentPath(segments)`), GeoBones (`attachmentPath(parentPath, name)`).
- RootModelResolver: both path-join sites use `EyeConfigModel.attachmentPath(prefix, name)`; 26.1.2's added `part(EntityModel, String)` kept.
- SkullEyeState: the hunk goes into 26.1.2's Heads/Twilight-aware `isShownPart` (`EyeConfigModel.lastAttachmentSegment(token)` replaces `token.substring(token.lastIndexOf('/') + 1)`), as 1.21.11 did. `HEAD_PART_TOKENS` values ("#0", "root/body") are data and stay.
- SkullEyeRenderer checked: no `/` or `#` part-path literal, no `split`/`lastIndexOf`. Its only part-name literals are `CUBE_TOKENS` values "head" and "root" (single-segment tokens, data) and `HEAD_FIELD = "head"` (the Heads model field name, not grammar). Nothing to change.

## 3. Picker HUD
PickerHud.java and lang/en_us.json copied whole from 26.1.1 (whole diff was this finding; en_us.json is the only lang file). Rows are `Component`s from `target_row`, `part_row`, `eyes_header` with `ChatFormatting` styles; line height `font.lineHeight + LINE_GAP`; drawn with `graphics.text(font, row, left, y, ARGB.opaque(ChatFormatting.WHITE.getColor()))`. Six fragment keys removed; a search finds no remaining reference.

## 5. Item eye depth
GooglyEyeItemRenderer imports EyePlacement, passes `EyePlacement.DEFAULT_DEPTH` instead of `1F`, and `IRIS_SCALE` has 26.1.1's comment. Other hunks of that file (`currentDraw`, first-person `EYE_LOCAL_POSE`, HeldEye removal) left for other topics.

## 6. Azimuth-to-yaw offset
EyePlacement copied whole (`AZIMUTH_TO_YAW_OFFSET = 90F`, used by `orientation`). GooglyServerCommands: private constant deleted, `EyePlacement` imported, `/sg mob rot` uses `EyePlacement.AZIMUTH_TO_YAW_OFFSET`.

## 9. Hand-built mod ids
- SomeGooglyCommon copied whole (adds `id(String)`). NetworkProtocol: `id` and its now-unused `SomeGooglyCommon`/`Identifier` imports deleted; 26.1.2's javadoc had no payload-id-namespace sentence to drop; `NETWORK_VERSION` stays "18"; `platform.ClientNetworking` link left (Networking's).
- `SomeGooglyCommon.id(...)` now used at: EyeConfigReloadListener, EyeBehavior, OngoingBehavior, SlimyEyeIrisTint, GooglyEyeItemRenderer, ModContent (3; `Identifier` import removed), PickerInput (`CATEGORY` on one line; `Identifier` import removed), the seven packets (`SomeGooglyCommon` imported), RecipeGameTestsLogic (3), SerializationGameTestsLogic (seven payload ids; `NetworkProtocol` import replaced by `SomeGooglyCommon`), FabricServerEvents (`LATE_PHASE`), FabricClientEvents, FabricNetworkTransport (3 handshake ids; `SomeGooglyCommon` imported), Fabric EntityPersistentDataImpl, Fabric and Forge RegistrationImpl (over Loader-divergence 4's `Identifier.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, ...)`; now identical to 26.1.1's), ForgeClientBootstrap, NeoForgeClient. ForgeNetworkTransport copied whole (channel id inlined into `.named(SomeGooglyCommon.id("network"))`; its whole remaining diff was this finding). GooglyServerCommands uses `Identifier.tryBuild(SomeGooglyCommon.MOD_ID, id)`. Unused `Identifier` imports removed where the last use went (also Fabric/Forge RegistrationImpl, EntityPersistentDataImpl, FabricClientEvents, ForgeClientBootstrap, NeoForgeClient).
- Deviation from 26.1.1: per the carry instruction to convert every hand-built mod-namespaced id, four sites 26.1.1 left are also converted: GooglyEyeRenderer `TEX` (`SomeGooglyCommon.id("textures/model/modelgooglyeye.png")`), Gizmo's pipeline location (`"somegoogly"` literal; `SomeGooglyCommon` imported, `Identifier` import removed), FabricClientEvents' picker HUD layer id (`"somegoogly"` literal), and PickerExportGameTestsLogic's two `"somegoogly"` test ids (`not_a_real_mob`, `filler_*`; `SomeGooglyCommon` imported). So FabricClientEvents and Gizmo differ from 26.1.1 only by these lines.
- 26.1.2-only code (NeoForge Twilight mixins, Forge/NeoForge HeadEyesLootModifier, TwilightHeadGameTests, Heads gametests, SkullEyeState) builds no mod-namespaced `Identifier`; their `twilightforest`/`heads`/`minecraft` ids stay. String constants such as NBT keys (`"somegoogly:data"`), the GeckoLib `DataTicket` name, and Fabric gametest `TEMPLATE` strings are not `Identifier` construction and stay.
- Final search: no `fromNamespaceAndPath(...MOD_ID...)`, `fromNamespaceAndPath("somegoogly", ...)`, `NetworkProtocol.id`, or `tryParse(MOD_ID + ":")` outside `SomeGooglyCommon.id`. Ids unchanged.

## Left for other topics / the parent
NetworkProtocol `NETWORK_VERSION` "19" and the `client.platform.ClientNetworking` link (Networking); GooglyServerCommands `trigger(...)` signature; GooglyEyeItemRenderer draw/pose/HeldEye changes; other remaining 26.1.1 hunks in the touched files. Stale mentions of `NetworkProtocol.id` exist only in earlier report-*.md/review-*.md files (historical records; not edited). No orientation or docs change (26.1.1 changed none for this topic; no orientation file mentions the changed names).

## Recalled API facts and checks only a build or in-game test can settle
As 26.1.1: `GuiGraphicsExtractor.text(Font, Component, int, int, int)`, `Font.width(FormattedText)`, `font.lineHeight`, `ARGB.opaque(int)`, `ChatFormatting.getColor()`; `Identifier.tryBuild(String, String)` returns null for an invalid path; `KeyMapping.Category(Identifier)`. A build also confirms the import edits (packets, gametests, Gizmo, FabricNetworkTransport) and that `SomeGooglyCommon.id` in the `TEX` static initializer compiles (it is a static method call, so no forward-reference issue).

## In-game checks
Picker HUD at several GUI scales on each loader; item eye depth unchanged in GUI, hand, and frame; `/sg mob rot` faces as before; eyes attach on vanilla, GeckoLib, reflected models, and placed heads (including Heads and Twilight trophies, which use `isShownPart`); `/sg admin behavior <id>` accepts valid and rejects bad ids; eye texture and gizmo lines still render; handshake, registration, key category, and picker HUD layer work on all loaders.

## GameTests
None added or removed.

## Fix lines
2: As 26.1.1. EyeConfigModel, EyeAttachmentResolver, ReflectedBoxResolver, and GeoBones copied whole from 26.1.1 (attachment-grammar constants and helpers); RootModelResolver's two joins use `attachmentPath(prefix, name)`, keeping 26.1.2's `part(model, token)`; SkullEyeState's hunk went into 26.1.2's Heads/Twilight-aware `isShownPart` (`lastAttachmentSegment(token)`), as 1.21.11 did; `HEAD_PART_TOKENS` data unchanged; SkullEyeRenderer has no part-path `/` or `#` literal. Unbuilt.
3: As 26.1.1. PickerHud and lang/en_us.json copied whole from 26.1.1: `Component` rows from `target_row`, `part_row`, `eyes_header` with `ChatFormatting` styles, line height `font.lineHeight + LINE_GAP`, drawn with `ARGB.opaque(ChatFormatting.WHITE.getColor())`; six fragment keys removed, no references remain. No deviation. Unbuilt.
5: As 26.1.1. GooglyEyeItemRenderer imports EyePlacement and passes `EyePlacement.DEFAULT_DEPTH`; `IRIS_SCALE` has 26.1.1's comment. Other topics' hunks in the file left. No deviation. Unbuilt.
6: As 26.1.1. EyePlacement copied whole (`AZIMUTH_TO_YAW_OFFSET = 90F` used in `orientation`); GooglyServerCommands' private constant deleted and `/sg mob rot` uses `EyePlacement.AZIMUTH_TO_YAW_OFFSET`. No deviation. Unbuilt.
9: As 26.1.1. `SomeGooglyCommon.id(String)` added (file copied whole); `NetworkProtocol.id` and its imports deleted (`NETWORK_VERSION` left at "18"); every mod-namespaced id in common, loaders, and gametests uses it, including both `RegistrationImpl`s over Loader-divergence 4's interim form; ForgeNetworkTransport copied whole (channel inlined into `.named(...)`); GooglyServerCommands uses `Identifier.tryBuild(SomeGooglyCommon.MOD_ID, id)`; unused `Identifier` imports removed. Deviation from 26.1.1: also converted the four sites 26.1.1 left (GooglyEyeRenderer `TEX`, Gizmo pipeline location, FabricClientEvents picker HUD layer id, PickerExportGameTestsLogic's two test ids); 26.1.2-only Twilight/Heads code has no mod-namespaced ids. Ids unchanged. Unbuilt.
