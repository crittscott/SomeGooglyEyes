# Report: Loader-divergence (26.1.2)
Applied 2026-11-12, unbuilt: nothing was compiled, built, or tested. Method: each 26.1.1 file was diffed against 26.1.2's with `diff --strip-trailing-cr`, and only this topic's hunks were taken. A file was copied whole only where the whole diff belonged to this topic. Files keep their line endings: Java and JSON LF, ModContent.java and the three entry points CRLF, orientation-code.md LF, both platform-notes.md files CRLF.

## 1. Each loader keeps its own copy of the payload table
- NetworkProtocol: added `CLIENTBOUND`/`SERVERBOUND` and the `Clientbound`/`Serverbound` records. Kept `NETWORK_VERSION = "18"` (Networking bumps it), `id(String)` (Magic-numbers 9 removes it), and the javadoc link to `platform.ClientNetworking`, which is where the class still lives in 26.1.2. Rewritten with Write, LF.
- FabricNetworkTransport: added generic `registerType` (one per direction) and `registerReceiver`, and removed the seven per-packet imports. The handshake ids still call `NetworkProtocol.id`. The only remaining diff against 26.1.1 is Magic-numbers 9's (`SomeGooglyCommon.id`). The Fabric API names (`clientboundPlay`/`serverboundPlay`, `context.packetListener()`) were already 0.155.3's.
- FabricClientNetworkTransport now registers from `CLIENTBOUND` through a generic `registerReceiver`. It stays in `network.fabric` for Organization 18.
- ForgeNetworkTransport: added `addClientbound`/`addServerbound` and kept the inline `Identifier channelId` for Magic-numbers 9.
- NeoForgeNetworkTransport copied whole from 26.1.1, since its whole diff was this fix.
- orientation-code.md, by hand: "`common` feeds every loader"; dropped "(which also caps payload bytes)"; "`NetworkProtocol` declares version 18 and the payload table all loaders register. Fabric checks it during configuration; the native loaders require it on their channels."; "Sends go through the platform bridges; clientbound handlers link the client handler only inside lambdas." The body (after the fixed header) is 11,935 characters by `wc -m`.
- No deviation from 26.1.1.

## 3. None
No change.

## 4. Two mechanisms for the loader boundary
- New files: common `platform/Registration.java` (copied from 26.1.1) and `platform/<loader>/RegistrationImpl.java` for Fabric, Forge, and NeoForge. 26.1.1's Fabric and Forge impls now call `SomeGooglyCommon.id`, which comes from Magic-numbers 9 and is not yet in 26.1.2. Those calls are written as `Identifier.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, name)`, with the `Identifier` import and the lines wrapped. This matches what 26.1.1 did at this step. The NeoForge impl is copied whole.
- ModContent: 26.1.1's version with `SomeGooglyCommon.id("...")` written out as `Identifier.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "...")` (Magic-numbers 9). It has `Supplier` fields registered in their initializers, in registration order, an empty `init()`, and the `RecipeSerializer<>(MAP_CODEC, STREAM_CODEC)` is kept. Converted back to CRLF.
- Entry points, edited by hand: Fabric calls `ModContent.init()`. Forge calls `ModContent.init(); RegistrationImpl.attach(modBusGroup);` and keeps `HeadEyesLootModifier.register(modBusGroup)` and its GeckoLib path. NeoForge calls `ModContent.init(); RegistrationImpl.attach(modBus);` and keeps `HeadEyesLootModifier.register(modBus)`, the head-integrations log line, and `headIntegrations()`. The Twilight mixin config is declared outside the entry point and was not touched. One small difference: Fabric's `ModContent` import stays in its existing position.
- Deleted registry/ContentRegistrar.java, Fabric/Forge/NeoForgeContentRegistrar.java, and the empty `registry/<loader>` directories.
- orientation-code.md: "`ModContent` registers content through the `@ExpectPlatform` `Registration`."
- Generated docs/javadoc still lists the deleted classes. Regenerating it is left to the build.

## 5. Layer installation
- ClientRenderLayers now appends with `layers.add(eyes)`.
- Removed the `LivingEntityRenderer addLayer` line from common somegoogly.accesswidener. It now equals 26.1.1's apart from 26.1.2's Heads `ModelPart.cubes` entry. There is no build-env or access-transformer copy.
- FabricClientEvents gains the comment explaining why it bypasses `helper.register`. The remaining diff is Magic-numbers 9's.

## 8. Misnamed Fabric mixin
LivingEntityReactionMixin.java renamed HealReactionMixin.java (class name only; the body simplification is Organization 6's). somegoogly-fabric.mixins.json is copied from 26.1.1 and is alphabetical. No other source references; review-*.md files mention the old name and were left alone.

## 9. The same config list means different things
- ConfigValue.java and TomlConfig.java copied whole from 26.1.1, since each diff was only this fix. `TomlConfig.assign` now takes `ConfigValue.Parsed<?>`; its one caller already passes a `Parsed`.
- New test `listSettingWithNoValidEntryUsesDefault`, placed before `reloadRejectsFileWithMalformedEntry`. ConfigGameTestsLogic and the Fabric, Forge, and NeoForge ConfigGameTests wrappers are copied whole, since each diff was only this test. The test uses `EyeBehavior.BLINK.id()` and `ConfigValue.defaultValue()`, both present in 26.1.2.
- Added the `functions.register("configgametests/list_setting_with_no_valid_entry_uses_default", ...)` line to the Forge and NeoForge SomeGooglyGameTestMod, before the malformed-entry line.
- Added `list_setting_with_no_valid_entry_uses_default.json` on Forge and NeoForge, copied from 26.1.1. The neighboring JSON is identical between the checkouts.

## 10. None
No change.

## 11. Fabric START_TRACKING timing
Read fabric-networking-api-v1 `ServerEntityMixin` at GitHub FabricMC/fabric tag `0.155.3+26.1.2`. START_TRACKING fires at `addPairing` `@At("TAIL")` and STOP_TRACKING at `removePairing` `@At("HEAD")`, the same as 0.145.4+26.1.1. Added a "26.1.2: ... Fabric API 0.155.3+26.1.2 ... [source]" line, worded like 26.1.1's, after the tracking-broadcast line (line 78) of platform-notes.md and build-env/platform-notes.md. The two copies are identical. No code change.

## Left for other topics / the parent
- Magic-numbers 9: `NetworkProtocol.id`, the Fabric handshake ids, the Forge channel `Identifier`, the `Identifier`s in the Fabric/Forge RegistrationImpls and ModContent, and FabricClientEvents.
- Networking: version "19".
- Organization 18: move FabricClientNetworkTransport to `client.network.fabric` and ClientNetworking to `client.platform`, updating the NetworkProtocol javadoc link.
- Organization 6: HealReactionMixin body.
- Parent: the GameTest counts in orientation-code.md ("Fabric 188, Forge 191, NeoForge 197") were not changed; this topic adds 1 per loader.
- Parent: check the Fabric/Forge RegistrationImpl rewrites and ModContent.
- Generated docs/javadoc is stale.

## Recalled API facts and checks only a build or in-game test can settle
- Architectury maps `platform.Registration` to `platform.<loader>.RegistrationImpl`.
- On Fabric, ModContent registers at class load, so field order matters (data components before items, items before the tab).
- Forge 64.1.5 `DeferredRegister.register(BusGroup)` and NeoForge 26.1.2.112 `DeferredRegister.Items.registerItem` exist as used.
- Forge `addMain` and NeoForge `playToClient`/`playToServer` accept the generic type, codec, and handler.
- `LivingEntityRenderer.layers` is mutable and widened; `addLayer` is no longer used anywhere.
- Vanilla `addPairing` sends the spawn packet before its tail. Only the Fabric side was checked from source.

## In-game checks
On each loader: items, components, creative tab, and eye-modifier recipe register; all payloads travel, and a version mismatch is refused; slime eyes draw in front of the outer jelly. On Fabric, healing triggers the heal reaction. With `ambientBehaviorPool = ["not a behavior"]`, the default pool is used and Fabric logs the fallback. GameTests: +1 per loader (`listSettingWithNoValidEntryUsesDefault`).

## Fix lines
1: NetworkProtocol holds `CLIENTBOUND`/`SERVERBOUND` and the `Clientbound`/`Serverbound` records beside `NETWORK_VERSION` "18" and `id` (kept for Magic-numbers 9; the Fabric handshake ids still call `NetworkProtocol.id`); the javadoc link stays on `platform.ClientNetworking`. FabricNetworkTransport registers types and serverbound receivers from the lists through generic `registerType`/`registerReceiver` (Fabric API 0.155.3+26.1.2 already used `clientboundPlay`/`serverboundPlay`/`context.packetListener()`). FabricClientNetworkTransport registers from `CLIENTBOUND` and stays in network/fabric. ForgeNetworkTransport uses `addClientbound`/`addServerbound`, keeping its `Identifier.fromNamespaceAndPath` channel id. NeoForgeNetworkTransport taken whole from 26.1.1. Wire unchanged. orientation-code.md: `NetworkProtocol` declares version 18 and the payload table; clientbound handlers link the client handler only inside lambdas; trimmed as 26.1.1. No deviation from 26.1.1. Unbuilt.
4: New common platform/Registration (`@ExpectPlatform`, taken from 26.1.1) with Fabric, Forge (`attach(BusGroup)`), and NeoForge (`attach(IEventBus)`) `RegistrationImpl`s. Ids use `Identifier.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, ...)` because 26.1.1's current Fabric/Forge impls and ModContent call Magic-numbers 9's `SomeGooglyCommon.id`, which was written out here. ModContent holds `Supplier` fields registered in field initializers, in registration order, with an empty `init()`. Entry points call `ModContent.init()` and, on Forge/NeoForge, `RegistrationImpl.attach`, keeping 26.1.2's `HeadEyesLootModifier.register` on both, NeoForge's head-integrations log, and Forge GeckoLib. ContentRegistrar, the three loader registrars, and their empty registry packages deleted. orientation-code.md updated. Unbuilt.
5: ClientRenderLayers appends with `layers.add(eyes)`; the `addLayer` line removed from somegoogly.accesswidener (26.1.2's Heads `ModelPart.cubes` entry kept; no build-env or access-transformer copy); FabricClientEvents explains why it bypasses `helper.register`. No deviation from 26.1.1. Unbuilt.
8: LivingEntityReactionMixin renamed HealReactionMixin (body unchanged; Organization 6 simplifies it); somegoogly-fabric.mixins.json alphabetical. No other references. No deviation from 26.1.1. Unbuilt.
9: `ConfigValue.Parsed.set` falls back to the default for a non-empty list with no valid entry; `TomlConfig.assign` takes a `Parsed` and logs each invalid entry and the fallback (both files taken whole from 26.1.1). Test `listSettingWithNoValidEntryUsesDefault` added before `reloadRejectsFileWithMalformedEntry` in ConfigGameTestsLogic and the three wrappers (taken whole from 26.1.1), with Forge/NeoForge SomeGooglyGameTestMod registrations and test_instance JSON on Forge and NeoForge. +1 test per loader; counts left for the parent. No deviation from 26.1.1. Unbuilt.
11: No code change. Fabric API's `0.155.3+26.1.2` tag, fabric-networking-api-v1 `ServerEntityMixin`, fires START_TRACKING at `addPairing` `@At("TAIL")` and STOP_TRACKING at `removePairing` HEAD; recorded as a 26.1.2 [source] fact in platform-notes.md and build-env/platform-notes.md, worded as 26.1.1's. Unbuilt.
