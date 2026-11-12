# Report: Magic-numbers (26.1)

Applied 2026-11-11 by hand-porting 1.21.11's changes. Nothing was compiled, built, or tested; everything here comes from reading the source. Each file was diffed against 1.21.11 and only this topic's hunks taken; only EyeConfigModel was copied whole. Fix lines in review-Magic-numbers.md are filled in.

## 1, 4, 10
None.

## 2. Part-path grammar as bare `/` and `#` literals
EyeConfigModel (from 1.21.11): private `ATTACHMENT_INDEX_PREFIX`/`ATTACHMENT_SEPARATOR`; public `attachmentIndex(int)`, `attachmentPath(Iterable<String>)`, `attachmentPath(String, String)`, `attachmentSegments(String)`, `lastAttachmentSegment(String)`. EyeAttachmentResolver uses `attachmentSegments`; RootModelResolver (both sites) and GeoBones `attachmentPath(prefix, name)`; ReflectedBoxResolver `attachmentIndex(i)` and `attachmentPath(segments)`; SkullEyeState's `isHeadPart` `lastAttachmentSegment(token)`. Deviations: 26.1's one SkullEyeState site is `isHeadPart` (no Heads code, no `HEAD_PART_TOKENS`); 1.21.11's RootModelResolver `part(...)` from another topic not brought over.

## 3. Picker HUD
26.1's PickerHud rewritten with 1.21.11's structure: rows are `Component`s from `target_row`, `part_row`, `eyes_header`, styled with `ChatFormatting` (labels green, values white, selected eye yellow, others white/gray); `keyName` and `partOrNone` return Components; line height `font.lineHeight + LINE_GAP`; `BACKDROP` javadoc; old color constants, `LINE_HEIGHT`, and `Line` removed. 26.1's GUI API kept: `render(GuiGraphicsExtractor, int)` draws each row with `graphics.text(font, row, left, y, ARGB.opaque(ChatFormatting.WHITE.getColor()))`. en_us.json picker.hud block replaced with 1.21.11's (`target_row`, `part_row`, `eyes_header` added; six fragment keys removed; no other lang files).

## 5. Item eye depth literal
GooglyEyeItemRenderer passes `EyePlacement.DEFAULT_DEPTH`; `IRIS_SCALE` has 1.21.11's comment; the `Frame`-based submit untouched.

## 6. Azimuth-to-yaw offset
`EyePlacement.AZIMUTH_TO_YAW_OFFSET = 90F` used in `orientation`; GooglyServerCommands' private constant deleted.

## 9. Hand-built mod ids
`SomeGooglyCommon.id(String)` returns `Identifier`; `NetworkProtocol.id` deleted. Used in EyeConfigReloadListener, EyeBehavior, OngoingBehavior, ModContent (3), GooglyEyeItemRenderer, SlimyEyeIrisTint, PickerInput (key category), the seven packets, RecipeGameTestsLogic, SerializationGameTestsLogic, FabricClientEvents, FabricNetworkTransport (3), Fabric EntityPersistentDataImpl, Fabric RegistrationImpl (private helper removed), FabricServerEvents, ForgeClientBootstrap, ForgeNetworkTransport (channel), Forge RegistrationImpl, NeoForgeClient. GooglyServerCommands uses `Identifier.tryBuild(SomeGooglyCommon.MOD_ID, id)`. Ids unchanged. Left as 1.21.11 leaves them: `"somegoogly"` literals in Gizmo's pipeline id, FabricClientEvents' HUD layer id, PickerExportGameTestsLogic, GooglyEyeRenderer's TEX.

## Recalled API facts (unverified)
Most uncertain: 26.1 `GuiGraphicsExtractor.text(Font, Component, int, int, int)` exists (26.1 code previously used only the String overload; if absent, use the `FormattedCharSequence` form via `row.getVisualOrderText()`). Also `Font.width(FormattedText)`, `font.lineHeight`, `ARGB.opaque(int)`, `Identifier.tryBuild(String, String)` returning null for an invalid path, `KeyMapping.Category(Identifier)`.

## In-game checks
Picker HUD at several GUI scales on each loader (rows "Target: … Variant … Age: …", "Part: …", "Eyes (n):", fully opaque colors); item eye depth unchanged in GUI, hand, and frame; `/sg mob rot` faces as before; eyes attach on vanilla, GeckoLib, reflected models, and placed heads; `/sg admin behavior <id>` accepts valid and rejects bad ids; handshake, registration, and key category on all loaders.

## Clash points
ReflectedBoxResolver, SkullEyeState, NetworkProtocol, the RegistrationImpls, both network transports, FabricClientEvents, EyeConfigReloadListener, GooglyEyeItemRenderer (Organization 1), GooglyServerCommands, the packets, PickerInput, PickerHud, en_us.json, EyePlacement, ModContent.

## GameTests
None added or removed.

## Files modified
common main: SomeGooglyCommon; config/EyeConfigModel, EyeConfigReloadListener; client/render/resolver/EyeAttachmentResolver, ReflectedBoxResolver, RootModelResolver; client/render/GooglyEyeItemRenderer; client/compat/gecko/GeoBones; client/picker/PickerHud, PickerInput; client/SlimyEyeIrisTint; eye/EyePlacement, behavior/EyeBehavior, behavior/OngoingBehavior, state/SkullEyeState; command/GooglyServerCommands; registry/ModContent; network/NetworkProtocol and the seven packets. Resources: lang/en_us.json. Gametest: RecipeGameTestsLogic, SerializationGameTestsLogic. Fabric: FabricClientEvents, FabricNetworkTransport, EntityPersistentDataImpl, RegistrationImpl, FabricServerEvents. Forge: ForgeClientBootstrap, ForgeNetworkTransport, RegistrationImpl. NeoForge: NeoForgeClient. Docs: review-Magic-numbers.md.
