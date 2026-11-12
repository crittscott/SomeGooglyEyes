# Report: Loader-divergence (26.1)

Applied 2026-11-11 by hand-porting 1.21.11's changes. Nothing was compiled, built, or tested; everything here comes from reading the source. Each file was diffed against 1.21.11 and only this topic's hunks taken. Copied whole (differed only by this topic): NeoForgeNetworkTransport, ConfigValue, TomlConfig, ConfigGameTestsLogic, the three ConfigGameTests wrappers, platform/Registration, NeoForge RegistrationImpl. Fix lines in review-Loader-divergence.md are filled in.

## 1. Each loader keeps its own copy of the payload table
- NetworkProtocol holds `CLIENTBOUND`/`SERVERBOUND` and the `Clientbound`/`Serverbound` records, keeping `NETWORK_VERSION = "18"` and `id(String)` (Magic-numbers 9 removes it); javadoc links stay on 26.1's `platform.ClientNetworking`.
- FabricNetworkTransport registers types and serverbound receivers from the lists through generic `registerType`/`registerReceiver`, keeping Fabric API 0.145.1's `PayloadTypeRegistry.clientboundPlay()`/`serverboundPlay()`, `clientboundConfiguration`/`serverboundConfiguration`, `createClientboundPacket`, `context.packetListener()`. FabricClientNetworkTransport registers from `CLIENTBOUND` (still in `network.fabric`; Organization 18 moves it). ForgeNetworkTransport uses `addClientbound`/`addServerbound` helpers, keeping its inline channel `Identifier`. NeoForgeNetworkTransport from 1.21.11. Wire unchanged.
- orientation-code.md: `NetworkProtocol` declares version 18 and the payload table all loaders register; clientbound handlers link the client handler only inside lambdas.

## 3, 10
No change.

## 4. Two mechanisms for the loader boundary
- New `platform/Registration.java` (`@ExpectPlatform` creativeTab, dataComponent, item, recipeSerializer) with Fabric, Forge (`attach(BusGroup)`), and NeoForge (`attach(IEventBus)`) `RegistrationImpl`s; ids via `Identifier.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, ...)`.
- ModContent: `Supplier` fields registered in field initializers in registration order (1.21.11's comment) and an empty `init()`.
- Deviations: 26.1's recipe serializer kept (`new RecipeSerializer<>(EyeModifierRecipe.MAP_CODEC, EyeModifierRecipe.STREAM_CODEC)`, where 1.21.11 uses `CustomRecipe.Serializer`); Fabric creative tab keeps `FabricCreativeModeTab.builder()`; Fabric RegistrationImpl builds the value then calls `Registry.register`.
- Entry points: Fabric `ModContent.init();`; Forge `ModContent.init(); RegistrationImpl.attach(modBusGroup);`; NeoForge `ModContent.init(); RegistrationImpl.attach(modBus);`.
- Deleted: registry/ContentRegistrar.java (its "four" javadoc count with it) and the three loader ContentRegistrars with their empty packages. Generated docs/javadoc HTML and .idea/workspace.xml still mention them.
- orientation-code.md: "`ModContent` registers content through the `@ExpectPlatform` `Registration`."

## 5. Layer installation
ClientRenderLayers appends with `layers.add(eyes)`; `LivingEntityRenderer.addLayer` removed from somegoogly.accesswidener (no build-env copy); FabricClientEvents carries 1.21.11's comment on bypassing `helper.register`.

## 8. Misnamed Fabric mixin
LivingEntityReactionMixin → HealReactionMixin (body unchanged); somegoogly-fabric.mixins.json alphabetical.

## 9. Same config list means different things
`ConfigValue.Parsed.set` falls back to the default when a non-empty list has no valid entry; `TomlConfig.assign(ConfigValue.Parsed<?>, ...)` logs each rejected entry, then the fallback. Test `listSettingWithNoValidEntryUsesDefault` before `reloadRejectsFileWithMalformedEntry` in the logic, three wrappers, Forge/NeoForge registrations, and new `test_instance/configgametests/list_setting_with_no_valid_entry_uses_default.json`.

## 11. Fabric START_TRACKING timing
Fabric API's `26.1` branch, fabric-networking-api-v1 `ServerEntityMixin` fires START_TRACKING at `addPairing` `@At("TAIL")` and STOP_TRACKING at `removePairing` HEAD. Recorded as a 26.1 [source] line in platform-notes.md and build-env/platform-notes.md. No code change.

## orientation-code.md size
11,999 → 12,009 after the updates → trimmed to 11,978 following 1.21.11's wording.

## Recalled API facts (unverified)
Architectury resolves `platform.Registration` to `platform.<loader>.RegistrationImpl`; on Fabric, ModContent registers at class load, so field order matters (components, items, creative tab); Forge 62's `DeferredRegister.register(BusGroup)` and NeoForge's `DeferredRegister.Items.registerItem(name, Function<Item.Properties, T>)` exist (the deleted registrars used both); `renderer.layers` is mutable and widened; vanilla `ServerEntity.addPairing` sends the spawn packet before its tail; Forge `PayloadConnection...addMain` accepts generic type/codec/handler.

## In-game checks
On each loader: items, components, creative tab, and the eye-modifier recipe register; eye state, config sync, settings, behaviors, anger, picker freeze and export travel; version refusal works; slime eyes draw in front of the outer jelly; healing triggers the heal reaction on Fabric; `ambientBehaviorPool = ["not a behavior"]` gives the default pool and Fabric logs the fallback; entities sent on Fabric START_TRACKING show their state on first sight.

## Left to other topics
Magic-numbers 9 (`NetworkProtocol.id`, inline `Identifier`s in ModContent, RegistrationImpls, Forge channel); Organization 18; Organization 6 (HealReactionMixin body).

## GameTests
Added `listSettingWithNoValidEntryUsesDefault` on Fabric, Forge, NeoForge (+1 each).

## Files modified
common: network/NetworkProtocol, registry/ModContent, platform/Registration (new), client/ClientRenderLayers, config/ConfigValue, somegoogly.accesswidener, registry/ContentRegistrar (deleted); gametest ConfigGameTestsLogic. fabric: SomeGooglyFabric, FabricNetworkTransport, FabricClientNetworkTransport, platform/fabric/RegistrationImpl (new), FabricClientEvents, HealReactionMixin (renamed), TomlConfig, somegoogly-fabric.mixins.json, ConfigGameTests, FabricContentRegistrar (deleted). forge and neoforge: entry point, network transport, RegistrationImpl (new), ConfigGameTests, SomeGooglyGameTestMod, new test_instance JSON, ContentRegistrar (deleted). Docs: orientation-code.md, platform-notes.md, build-env/platform-notes.md, review-Loader-divergence.md.
