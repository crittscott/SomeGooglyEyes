# Report: Magic-numbers (26.1.1)

Applied 2026-11-11 by taking 26.1's hunks for these findings only. Nothing was compiled, built, or tested. Fix lines and intro in review-Magic-numbers.md filled in. No deviations from 26.1; line endings matched in every whole-file copy.

## 1, 4, 10
No change.

## 2. Part-path grammar
EyeConfigModel, EyeAttachmentResolver, RootModelResolver, ReflectedBoxResolver, GeoBones, SkullEyeState copied whole (only this finding's diff remained). EyeConfigModel has private `ATTACHMENT_INDEX_PREFIX`/`ATTACHMENT_SEPARATOR` and public `attachmentIndex`, `attachmentPath(Iterable)`, `attachmentPath(String, String)`, `attachmentSegments`, `lastAttachmentSegment`. EyeAttachmentResolver uses `attachmentSegments`; RootModelResolver (both sites) and GeoBones `attachmentPath(prefix, name)`; ReflectedBoxResolver `attachmentIndex(i)` and `attachmentPath(segments)`; `SkullEyeState.isHeadPart` `lastAttachmentSegment`.

## 3. Picker HUD
PickerHud.java and lang/en_us.json copied whole. Rows are `Component`s from `target_row`, `part_row`, `eyes_header`, styled with `ChatFormatting`; line height `font.lineHeight + LINE_GAP`; rows drawn with `graphics.text(font, row, left, y, ARGB.opaque(ChatFormatting.WHITE.getColor()))`. Six fragment keys gone from en_us.json; nothing references them.

## 5. Item eye depth
GooglyEyeItemRenderer passes `EyePlacement.DEFAULT_DEPTH` instead of `1F`; `IRIS_SCALE` has 26.1's comment. Other hunks (`currentDraw`, first-person `EYE_LOCAL_POSE`, HeldEye removal) left.

## 6. Azimuth-to-yaw offset
EyePlacement copied whole (`AZIMUTH_TO_YAW_OFFSET = 90F`, used by `orientation`); GooglyServerCommands' private constant deleted, `/sg mob rot` uses `EyePlacement.AZIMUTH_TO_YAW_OFFSET`.

## 9. Hand-built mod ids
- SomeGooglyCommon copied whole (adds `id(String)`). NetworkProtocol: `id` and its unused imports deleted; javadoc no longer mentions the payload-id namespace; `NETWORK_VERSION` stays "18"; `platform.ClientNetworking` link kept.
- Edited by hand: EyeConfigReloadListener, EyeBehavior, OngoingBehavior, SlimyEyeIrisTint, GooglyEyeItemRenderer, ModContent (three ids, `Identifier` import removed), PickerInput (`CATEGORY` one line), the seven packets, RecipeGameTestsLogic (three ids), SerializationGameTestsLogic (seven ids), FabricServerEvents (`LATE_PHASE`), GooglyServerCommands (`Identifier.tryBuild(SomeGooglyCommon.MOD_ID, id)`).
- Copied whole: FabricClientEvents, FabricNetworkTransport (three handshake ids), Fabric EntityPersistentDataImpl, Fabric and Forge RegistrationImpl (restores 26.1's `SomeGooglyCommon.id(name)` over Loader-divergence's interim form), ForgeClientBootstrap, ForgeNetworkTransport (channel id inlined into `.named(...)`), NeoForgeClient.
- A final search finds no `fromNamespaceAndPath(...MOD_ID...)`, `NetworkProtocol.id`, or `tryParse(MOD_ID + ":")` outside `SomeGooglyCommon.id`. Ids unchanged.

## Left for later topics
NetworkProtocol "19" and the `ClientNetworking` package link; GooglyServerCommands `trigger(...)` signature; GooglyEyeItemRenderer draw/pose changes; FabricServerEvents `onHurt`; EyeConfigReloadListener `isExcluded`; SerializationGameTestsLogic `EyeAppearance` package and other test hunks; remaining OngoingBehavior/EyeBehavior hunks.

## Docs
None changed (26.1 changed none for this topic).

## First build settles (unverified API facts, as 26.1)
`GuiGraphicsExtractor.text(Font, Component, int, int, int)` (else use `row.getVisualOrderText()`); `Font.width(FormattedText)`, `font.lineHeight`, `ARGB.opaque(int)`; `Identifier.tryBuild(String, String)` returns null for an invalid path; `KeyMapping.Category(Identifier)`.

## In-game checks
Picker HUD at several GUI scales on each loader; item eye depth unchanged in GUI, hand, and frame; `/sg mob rot` faces as before; eyes attach on vanilla, GeckoLib, reflected models, and placed heads; `/sg admin behavior <id>` accepts valid and rejects bad ids; handshake, registration, and key category work on all loaders.

## GameTests
None added or removed.
