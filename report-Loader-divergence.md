# Report: Loader-divergence (1.21.11)

Applied 2026-11-11 by hand-porting 1.21.8's changes. Nothing was compiled, built, or tested; everything here comes from reading the source. Each file was diffed against 1.21.8 and only this topic's hunks taken. Copied whole after the diff showed only this topic's difference: NeoForgeNetworkTransport, platform/Registration, the NeoForge RegistrationImpl, and the three ConfigGameTests wrappers.

## 1. Each loader keeps its own copy of the payload table
- NetworkProtocol holds 1.21.8's `CLIENTBOUND`/`SERVERBOUND` lists and `Clientbound`/`Serverbound` records; keeps `NETWORK_VERSION = "18"` and `id(String)` (Magic-numbers 9 replaces it); javadoc keeps the `platform.ClientNetworking` link.
- FabricNetworkTransport registers types and serverbound receivers from the lists through 1.21.8's generic `registerType`/`registerReceiver`; FabricClientNetworkTransport registers receivers from `CLIENTBOUND` (stays in `network.fabric` for Organization 18).
- ForgeNetworkTransport from 1.21.8 (`addClientbound`/`addServerbound`), keeping 1.21.11's `Identifier.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "network")` channel id. NeoForgeNetworkTransport copied whole (now LF).
- Wire unchanged. orientation-code.md: `NetworkProtocol` holds the payload table.

## 3, 10
No change.

## 4. Two mechanisms for the loader boundary
- New: common `platform/Registration.java` (`@ExpectPlatform` creativeTab, dataComponent, item, recipeSerializer); `platform/fabric/RegistrationImpl`; `platform/forge/RegistrationImpl` with `attach(BusGroup)`; `platform/neoforge/RegistrationImpl` with `attach(IEventBus)`. `SomeGooglyCommon.id(name)` written as `Identifier.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, name)` (Fabric via a private `id` helper).
- registry/ModContent.java from 1.21.8 with the same `Identifier` substitution: `Supplier` fields registered in field initializers, empty `init()`. 1.21.11 has no content 1.21.8 lacks.
- Entry points: Fabric `ModContent.init()`; Forge `ModContent.init(); RegistrationImpl.attach(modBusGroup);`; NeoForge `ModContent.init(); RegistrationImpl.attach(modBus);`.
- Deleted: registry/ContentRegistrar.java, Fabric/Forge/NeoForge ContentRegistrars, and their now-empty directories.
- orientation-code.md: `ModContent` registers through the `@ExpectPlatform` `Registration`.

## 5. Layer installation
ClientRenderLayers appends with `layers.add(eyes)`; the `LivingEntityRenderer.addLayer` line removed from somegoogly.accesswidener; FabricClientEvents has 1.21.8's comment on not using `helper.register`. No access-transformer or build-env copy mentions `addLayer`.

## 8. Misnamed Fabric mixin
LivingEntityReactionMixin renamed HealReactionMixin (body unchanged; Organization 6 edits it); somegoogly-fabric.mixins.json alphabetical. No other references.

## 9. Same config list means different things
- `ConfigValue.Parsed.set` falls back to the default when a non-empty list has no valid entry (1.21.8's javadoc); `TomlConfig.assign(ConfigValue.Parsed<?>, ...)` logs each rejected entry, then "has no valid entries; using the default". Both match 1.21.8.
- Test `listSettingWithNoValidEntryUsesDefault` in ConfigGameTestsLogic; three ConfigGameTests wrappers copied from 1.21.8; `functions.register("configgametests/list_setting_with_no_valid_entry_uses_default", ...)` in both loaders; forge and neoforge `test_instance/configgametests/list_setting_with_no_valid_entry_uses_default.json`.
- Counts: orientation-build.md Forge 194, NeoForge 196; orientation-code.md "GameTest scenarios" was stale (188/191/193), now Fabric 190, Forge 194, NeoForge 196.

## 11. Fabric START_TRACKING timing
On Fabric API's 1.21.11 branch, `fabric-networking-api-v1/.../mixin/networking/ServerEntityMixin.java` (`@Mixin(ServerEntity.class)`) fires START_TRACKING from `@Inject(method = "addPairing", at = @At("TAIL"))` and STOP_TRACKING at `removePairing` HEAD. Recorded in platform-notes.md as a 1.21.11 fact [source]. No code change.

## orientation-code.md size
It was about 12,050 characters before this topic; trimmed to 11,998 ("`common` feeds every loader."; dropped "(which also caps payload bytes)"; shorter Fabric-version, schema, definitions/settings, sends sentences; dropped "Successful" in the Slimy Eye/harvest game-event sentence).

## Recalled API facts (unverified)
Architectury resolves `platform.Registration` to `platform.<loader>.RegistrationImpl`; Forge 1.21.11 `DeferredRegister.register(BusGroup)` and NeoForge `DeferredRegister.Items.registerItem(name, Function<Item.Properties, T>)` exist (both used by the deleted registrars); Fabric registers on class load, so ModContent's field order matters; `renderer.layers` is mutable and widened; vanilla `ServerEntity.addPairing` sends the spawn packet before its tail.

## Left to other topics
Magic-numbers 9 (`NetworkProtocol.id`, inline `Identifier`s in ModContent, RegistrationImpls, Forge channel id); Organization 18 (move FabricClientNetworkTransport); Organization 6 (HealReactionMixin body); Organization 14 (`define`).

## Clash points
NetworkProtocol; the three transports; ModContent; entry points; ClientRenderLayers; somegoogly.accesswidener; FabricClientEvents; HealReactionMixin and mixins JSON; ConfigValue; TomlConfig; ConfigGameTestsLogic and wrappers; SomeGooglyGameTestMod files; orientation-code.md; orientation-build.md; platform-notes.md.

## In-game checks
Each loader: items, components, creative tab, eye-modifier recipe register; eyes work on mobs and heads; eye state, config sync, settings, behaviors, anger, picker freeze, export still travel; version refusal works; slime eyes draw in front of the outer jelly; healing triggers the heal reaction on Fabric; `ambientBehaviorPool = ["not a behavior"]` gives the default pool (Fabric logs the fallback); entities sent on Fabric START_TRACKING show state on first sight.

## Files modified
common: network/NetworkProtocol, registry/ModContent, platform/Registration (new), client/ClientRenderLayers, config/ConfigValue, somegoogly.accesswidener, ContentRegistrar (deleted); gametest ConfigGameTestsLogic. fabric: SomeGooglyFabric, FabricNetworkTransport, FabricClientNetworkTransport, platform/fabric/RegistrationImpl (new), FabricClientEvents, HealReactionMixin (renamed), TomlConfig, somegoogly-fabric.mixins.json, ConfigGameTests, FabricContentRegistrar (deleted). forge: SomeGooglyForge, ForgeNetworkTransport, platform/forge/RegistrationImpl (new), ConfigGameTests, SomeGooglyGameTestMod, new test_instance JSON, ForgeContentRegistrar (deleted). neoforge: same set. Docs: orientation-code.md, orientation-build.md, platform-notes.md, review-Loader-divergence.md.
