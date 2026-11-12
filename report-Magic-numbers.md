# Report: Magic-numbers (1.21.11)

Applied 2026-11-11 by hand-porting 1.21.8's changes. Nothing was compiled, built, or tested; everything here comes from reading the source. Each file was diffed against 1.21.8 and only this topic's hunks taken; only PickerHud was copied whole (its diff showed nothing but this topic's rewrite).

## 1, 4, 10
None.

## 2. Part-path grammar as bare `/` and `#` literals
- EyeConfigModel: 1.21.8's private `ATTACHMENT_INDEX_PREFIX`/`ATTACHMENT_SEPARATOR` and public `attachmentIndex(int)`, `attachmentPath(Iterable<String>)`, `attachmentPath(String, String)`, `attachmentSegments(String)`, `lastAttachmentSegment(String)`.
- Callers: EyeAttachmentResolver (`attachmentSegments`), RootModelResolver both sites and GeoBones (`attachmentPath(prefix, name)`), ReflectedBoxResolver (`attachmentIndex(i)`, `attachmentPath(segments)`), SkullEyeState's Heads-aware `isShownPart` (`lastAttachmentSegment(token)`).
- Deviation: SkullEyeState's `HEAD_PART_TOKENS` values (`root/body`, `root/right_eye`, `root/cube0`) stay literals: they are authored attachment tokens matched verbatim against eye files, not assembled paths (1.21.8 has no such map).

## 3. Picker HUD
- PickerHud replaced with 1.21.8's (Component rows, `ChatFormatting` styles, `font.lineHeight + LINE_GAP`, base color `ARGB.opaque(ChatFormatting.WHITE.getColor())`); no GUI API differences from 1.21.8's; the copy also brought 1.21.8's BACKDROP javadoc and renames.
- en_us.json: `somegoogly.picker.hud.*` replaced by 1.21.8's block (adds `target_row`, `part_row`, `eyes_header`; removes `target_label`, `variant_label`, `age_label`, `part_label`, `eyes_header_label`, `eyes_header_value`); JSON parses; removed keys unreferenced.

## 5. Item eye depth literal
GooglyEyeItemRenderer: `drawAnimatedEye` passes `EyePlacement.DEFAULT_DEPTH`; `IRIS_SCALE` has 1.21.8's comment; nothing else in the submit-based renderer changed.

## 6. Azimuth-to-yaw offset
EyePlacement: `AZIMUTH_TO_YAW_OFFSET = 90F` used in `orientation`; GooglyServerCommands' private constant deleted, uses `EyePlacement.AZIMUTH_TO_YAW_OFFSET`.

## 9. Hand-built mod ids
- `SomeGooglyCommon.id(String)` returns `Identifier`; `NetworkProtocol.id` deleted (javadoc no longer mentions the payload-id namespace).
- Used at: EyeConfigReloadListener, EyeBehavior, OngoingBehavior, ModContent (3), GooglyEyeItemRenderer, SlimyEyeIrisTint, PickerInput (key category), the seven packets; FabricClientEvents, Fabric EntityPersistentDataImpl, FabricServerEvents, FabricNetworkTransport (3), Fabric RegistrationImpl (private `id` helper removed); ForgeClientBootstrap, ForgeNetworkTransport (channel id inlined into `.named(...)`), Forge RegistrationImpl; NeoForgeClient; RecipeGameTestsLogic, SerializationGameTestsLogic.
- GooglyServerCommands uses `Identifier.tryBuild(SomeGooglyCommon.MOD_ID, id)`.
- Ids unchanged; no network version bump.
- Left as in 1.21.8 (not listed by the finding): `"somegoogly"` literals in Gizmo's pipeline id, FabricClientEvents' HUD layer id, PickerExportGameTestsLogic; GooglyEyeRenderer's TEX `fromNamespaceAndPath(MOD_ID, ...)`.

## Recalled API facts (unverified)
1.21.11 `Identifier.tryBuild(String, String)` and `fromNamespaceAndPath`; `ARGB.opaque(int)`; `GuiGraphics.drawString(Font, Component, int, int, int)` and `Font.width(FormattedText)`, text colors need alpha; `KeyMapping.Category(Identifier)`.

## Clash points
ReflectedBoxResolver, SkullEyeState, NetworkProtocol, ModContent, the RegistrationImpls, the transports, FabricClientEvents, EyeConfigReloadListener, GooglyEyeItemRenderer (Organization 1), GooglyServerCommands, the packets (Networking 2), PickerInput, PickerHud, en_us.json, EyePlacement.

## In-game checks
Picker HUD at several GUI scales on each loader (rows "Target: … Variant … Age: …", "Part: …", "Eyes (n):"; labels green, values white, selected eye yellow, fully opaque); item eye depth unchanged in GUI, hand, frame; `/sg mob rot` faces as before; eyes attach on vanilla, GeckoLib, Citadel/LLibrary, Heads-mod heads; `/sg admin behavior <id>` accepts valid and rejects bad ids; networking, registration, and key category work on all loaders.

## Files modified
common main: SomeGooglyCommon, config/EyeConfigModel, config/EyeConfigReloadListener, client/compat/gecko/GeoBones, client/render/resolver/EyeAttachmentResolver, ReflectedBoxResolver, RootModelResolver, eye/state/SkullEyeState, client/picker/PickerHud, client/picker/PickerInput, client/render/GooglyEyeItemRenderer, client/SlimyEyeIrisTint, eye/EyePlacement, eye/behavior/EyeBehavior, eye/behavior/OngoingBehavior, command/GooglyServerCommands, registry/ModContent, network/NetworkProtocol and the seven packets, lang/en_us.json. Gametest: RecipeGameTestsLogic, SerializationGameTestsLogic. Fabric: FabricClientEvents, FabricNetworkTransport, EntityPersistentDataImpl, RegistrationImpl, FabricServerEvents. Forge: ForgeClientBootstrap, ForgeNetworkTransport, RegistrationImpl. NeoForge: NeoForgeClient. Docs: review-Magic-numbers.md.
