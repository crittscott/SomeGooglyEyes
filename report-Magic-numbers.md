# Report: Magic-numbers (26.2)

Applied 2026-11-12 by taking 26.1.1's hunks for findings 2, 3, 5, 6, and 9, each by Edit (no whole-file copies, because other topics were editing the same files concurrently). Nothing was compiled, built, or tested; this is source inspection only. Fix lines in review-Magic-numbers.md filled in. 26.1.2 had no report for this topic.

## 1, 4, 10
No change (not pursued, or no Twilight Forest code in 26.2).

## 2. Part-path grammar
- config/EyeConfigModel: private `ATTACHMENT_INDEX_PREFIX` / `ATTACHMENT_SEPARATOR`; public `attachmentIndex(int)`, `attachmentPath(Iterable)`, `attachmentPath(String, String)`, `attachmentSegments(String)`, `lastAttachmentSegment(String)`.
- EyeAttachmentResolver (`attachmentSegments`, import added), RootModelResolver (both sites `attachmentPath(prefix, name)`), GeoBones (`attachmentPath(parentPath, name)`, import added), ReflectedBoxResolver (`attachmentIndex(i)`, `attachmentPath(segments)`, import added), SkullEyeState `isHeadPart` (`lastAttachmentSegment`).
- No deviation.

## 3. Picker HUD
- client/picker/PickerHud.java now matches 26.1.1's exactly (verified by diff): `Component` rows from `target_row`, `part_row`, `eyes_header` with `ChatFormatting` styles; `LINE_GAP` and `font.lineHeight`; drawn with `ARGB.opaque(ChatFormatting.WHITE.getColor())`; color constants, `LINE_HEIGHT`, `Line` record, and `I18n` gone.
- lang/en_us.json: `age_label`, `eyes_header_label`, `eyes_header_value`, `part_label`, `target_label`, `variant_label` removed; `eyes_header`, `part_row`, `target_row` added. File now equals 26.1.1's. No references to the removed keys remain.

## 5. Item eye depth
GooglyEyeItemRenderer imports EyePlacement, passes `EyePlacement.DEFAULT_DEPTH` instead of `1F`, and comments `IRIS_SCALE` ("Fixed item appearance, independent of the mob placement defaults."). Its other 26.1.1 hunks (HeldEye removal, `currentDraw`, first-person `EYE_LOCAL_POSE`) belong to Organization.

## 6. Azimuth-to-yaw offset
EyePlacement declares `AZIMUTH_TO_YAW_OFFSET = 90F`, used by `orientation`; GooglyServerCommands' private constant deleted, `/sg mob rot` uses `EyePlacement.AZIMUTH_TO_YAW_OFFSET` (import added).

## 9. Hand-built mod ids
- SomeGooglyCommon: `id(String)` and `Identifier` import added.
- NetworkProtocol: `id` deleted with its `SomeGooglyCommon` and `Identifier` imports; class javadoc no longer mentions the payload-id namespace. `NETWORK_VERSION` untouched; 26.2's `platform.ClientNetworking` link left as it was.
- `SomeGooglyCommon.id` at: EyeConfigReloadListener, EyeBehavior, OngoingBehavior, ModContent (3), GooglyEyeItemRenderer, SlimyEyeIrisTint, PickerInput, the seven packets (`SomeGooglyCommon` import added to each), RecipeGameTestsLogic (3), SerializationGameTestsLogic (7; `NetworkProtocol` import swapped for `SomeGooglyCommon`), FabricClientEvents (`attachment_caches`), FabricNetworkTransport (3 handshake ids; import added), Fabric EntityPersistentDataImpl, FabricServerEvents (`after_protection`), ForgeClientBootstrap, ForgeNetworkTransport (`channelId` local removed, inlined into `.named(...)`), NeoForgeClient. Both `RegistrationImpl`s already used `SomeGooglyCommon.id` when reached.
- GooglyServerCommands: `Identifier.tryBuild(SomeGooglyCommon.MOD_ID, id)`.
- Now-unused `net.minecraft.resources.Identifier` imports removed from PickerInput, ModContent, Fabric EntityPersistentDataImpl, ForgeClientBootstrap, ForgeNetworkTransport, NeoForgeClient (as 26.1.1).
- Final search: no `fromNamespaceAndPath(...MOD_ID...)` outside `SomeGooglyCommon.id`, no `NetworkProtocol.id`, no `tryParse(MOD_ID + ":")`. Ids unchanged.
- Left as 26.1.1 leaves them: `Identifier.fromNamespaceAndPath("somegoogly", ...)` literals in Gizmo (`pipeline/gizmo_lines`) and FabricClientEvents (`picker` HUD layer), and GooglyEyeRenderer's `TEX`, which uses `fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, ...)` split over two lines. These are the same pattern the finding targets; a later pass could convert them.

## Deviations from 26.1.1
Hunks were applied by Edit rather than whole-file copies. PickerHud's base text color is `CommonColors.WHITE` instead of 26.1.1's `ARGB.opaque(ChatFormatting.WHITE.getColor())`: the first 26.2 build failed because `ChatFormatting` has no `getColor()` in 26.2 (owner-supplied compile result). `CommonColors.WHITE` is recalled, not checked; the next build settles it.

## Docs
None changed (no doc mentions `NetworkProtocol.id`, the HUD keys, or the offset constant).

## Recalled API facts (unverified until first build)
`GuiGraphicsExtractor.text(Font, Component, int, int, int)`; `Font.width(FormattedText)`, `font.lineHeight`; `ARGB.opaque(int)`; `ChatFormatting.WHITE.getColor()` non-null; `Identifier.tryBuild(String, String)` returns null for an invalid path; `KeyMapping.Category(Identifier)`.

## In-game checks
Picker HUD layout and colors at several GUI scales on each loader; item eye depth unchanged in GUI, hand, and frame; `/sg mob rot` facing unchanged; eyes attach on vanilla, GeckoLib, reflected models, and placed heads; `/sg admin behavior <id>` accepts valid and rejects bad ids; handshake, registration, persistent data, and key category work on all loaders.

## GameTests
None added or removed on any loader.

## Clashes and other topics' hunks seen
- SomeGooglyCommon, NetworkProtocol, GooglyEyeItemRenderer, GeoBones, ReflectedBoxResolver, SerializationGameTestsLogic, EyeBehavior, EyeConfigReloadListener, PickerExportPacket, FabricServerEvents changed on disk during the run (other topics). The last search found no remaining hand-built `MOD_ID` ids and no `NetworkProtocol.id`.
- Remaining 26.1.1 differences in touched files belong to other topics: GeoBones `clear`; SkullEyeState loot / null-level hunks; PickerExportPacket cooldown javadoc.
