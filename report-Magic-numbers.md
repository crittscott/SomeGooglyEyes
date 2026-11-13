# Report: Magic-numbers (26.3)

Applied 2026-11-12 from 26.2's post-fix code (26.2/report-Magic-numbers.md exists), taking only this topic's hunks per file (`diff --strip-trailing-cr` against 26.2) and keeping 26.3's side of every API difference and each file's line endings. Nothing was compiled, built, or tested. Fix lines in review-Magic-numbers.md filled in.

## 1, 4, 10
No change: 1 and 4 not pursued; 26.3 has no Twilight Forest code for 10.

## 2. Part-path grammar
- config/EyeConfigModel: private `ATTACHMENT_INDEX_PREFIX` and `ATTACHMENT_SEPARATOR`; public `attachmentIndex(int)`, `attachmentPath(Iterable<String>)`, `attachmentPath(String, String)`, `attachmentSegments(String)`, `lastAttachmentSegment(String)`.
- Callers: EyeAttachmentResolver `attachmentSegments`; RootModelResolver `attachmentPath(prefix, name)` at both sites; GeoBones `attachmentPath(parentPath, name)`; ReflectedBoxResolver `attachmentIndex(i)` and `attachmentPath(segments)` (completes what Guarding left); SkullEyeState `isHeadPart` uses `lastAttachmentSegment`.
- EyeConfigModel, the resolvers, and GeoBones now equal 26.2's. No 26.3 adaptation needed.

## 3. Picker HUD
- client/picker/PickerHud.java equals 26.2's post-fix file: rows are `Component`s from `target_row`, `part_row`, `eyes_header`, `eye_line1`/`eye_line2` with `ChatFormatting` styles; line height `font.lineHeight + LINE_GAP`; drawn with `graphics.text(font, row, left, y, CommonColors.WHITE)`; color constants, `LINE_HEIGHT`, `Line`, and `I18n` gone.
- lang/en_us.json equals 26.2's: removed `age_label`, `eyes_header_label`, `eyes_header_value`, `part_label`, `target_label`, `variant_label`; added `eyes_header`, `part_row`, `target_row`. No references to removed keys remain.
- Deviation inherited from 26.2: base color `CommonColors.WHITE` instead of 26.1.1's `ARGB.opaque(ChatFormatting.WHITE.getColor())` (26.2 has no `ChatFormatting.getColor()`). Recalled API; first build settles it.

## 5. Item eye depth
- GooglyEyeItemRenderer imports EyePlacement and passes `EyePlacement.DEFAULT_DEPTH` instead of `1F`; `IRIS_SCALE` comment "Fixed item appearance, independent of the mob placement defaults." 26.3's `pose.rotateDegrees` kept. Other 26.2 hunks (HeldEye removal, `currentDraw`, first-person `EYE_LOCAL_POSE`) left for Organization.

## 6. Azimuth-to-yaw offset
- EyePlacement declares `AZIMUTH_TO_YAW_OFFSET = 90F` (javadoc "Azimuth starts at +X; vanilla yaw starts at +Z.") and uses it in `orientation`; GooglyServerCommands' private constant deleted, `/sg mob rot` uses `EyePlacement.AZIMUTH_TO_YAW_OFFSET`.

## 9. Hand-built mod ids
- SomeGooglyCommon: `id(String)` and `Identifier` import.
- NetworkProtocol: `id` deleted with its imports; class javadoc drops "the namespace of its payload ids"; keeps 26.3's `platform.ClientNetworking` link and `NETWORK_VERSION` "18".
- `SomeGooglyCommon.id` now used in: EyeConfigReloadListener, EyeBehavior, OngoingBehavior, ModContent (3), GooglyEyeItemRenderer, SlimyEyeIrisTint, PickerInput `CATEGORY`; the seven packets; RecipeGameTestsLogic (3), SerializationGameTestsLogic (7); Fabric FabricClientEvents, FabricNetworkTransport (3 handshake ids), EntityPersistentDataImpl, RegistrationImpl (4), FabricServerEvents; Forge ForgeClientBootstrap, ForgeNetworkTransport (`channelId` local inlined), RegistrationImpl; NeoForge NeoForgeClient.
- GooglyServerCommands uses `Identifier.tryBuild(SomeGooglyCommon.MOD_ID, id)` instead of `tryParse(MOD_ID + ":" + id)`.
- Unused `Identifier` imports removed from ModContent, PickerInput, Fabric EntityPersistentDataImpl, Fabric RegistrationImpl, ForgeClientBootstrap, ForgeNetworkTransport, Forge RegistrationImpl, NeoForgeClient.
- Left as 26.2 left them: `"somegoogly"` literals in Gizmo and FabricClientEvents (`picker` HUD layer); GooglyEyeRenderer's two-line `TEX`.
- Ids unchanged; no protocol bump.

## Docs
None changed; orientation-code.md stays at 12,000 characters.

## Recalled API facts (unverified until first build)
`CommonColors.WHITE`; `GuiGraphicsExtractor.text(Font, Component, int, int, int)`; `Font.width(FormattedText)` and `font.lineHeight`; `Identifier.tryBuild(String, String)` returns null for an invalid path; `KeyMapping.Category(Identifier)`.

## In-game checks
- Picker HUD layout and colors at several GUI scales on each loader.
- Item eye depth unchanged in the GUI, the hand, and an item frame.
- `/sg mob rot` facing unchanged.
- Eyes attach on vanilla, GeckoLib, and reflected models, and on placed heads.
- `/sg admin behavior <id>` accepts valid ids and rejects bad ones.
- Handshake, registration, persistent data, and key category work on all loaders.

## GameTests
None added or removed (+0 Fabric, +0 Forge, +0 NeoForge).

## Other topics' hunks seen and left
GooglyEyeItemRenderer HeldEye/`currentDraw`/first-person pose; EyeBehaviorTriggerPacket `Target`; PickerExportPacket age validation and javadoc; SerializationGameTestsLogic trigger and age tests; OngoingBehavior `serverAngry`; EyeConfigReloadListener `isExcluded`; EyeBehavior javadocs; FabricServerEvents `onHurt`; GooglyServerCommands trigger duration argument.
