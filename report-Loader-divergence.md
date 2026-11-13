# Report: Loader-divergence (26.2)

Applied 2026-11-12 by taking 26.1.1's hunks for this topic, diffed file by file, while other topics edited 26.2 concurrently. Nothing was compiled, built, or tested. Fix lines in review-Loader-divergence.md filled in. Line endings kept (new Java files LF like 26.1.1's; ModContent and SomeGooglyCommon CRLF as they were; platform-notes.md LF).

## 1. Each loader keeps its own copy of the payload table
- common network/NetworkProtocol.java: `CLIENTBOUND`/`SERVERBOUND` lists and `Clientbound`/`Serverbound` records; class javadoc as 26.1.1's but keeping the link to `platform.ClientNetworking` (the client-package move is Organization 18's). Networking bumped the version to "19" and Magic-numbers removed `id` during this run.
- fabric network/fabric/FabricNetworkTransport.java: per-packet imports, registrations, and receivers replaced by loops over the lists and generic `registerType` (one per direction) / `registerReceiver`.
- fabric network/fabric/FabricClientNetworkTransport.java: registers receivers from `CLIENTBOUND` via a generic `registerReceiver`; stays in `network.fabric` (Organization 18 moves it).
- forge network/forge/ForgeNetworkTransport.java: `addClientbound`/`addServerbound` helpers and loops; channel id left to Magic-numbers (now `SomeGooglyCommon.id("network")`).
- neoforge network/neoforge/NeoForgeNetworkTransport.java: now identical to 26.1.1's.
- orientation-code.md: "`NetworkProtocol` declares version N and the payload table all loaders register." and "Sends go through the platform bridges; clientbound handlers link the client handler only inside lambdas."

## 3, 10
No change.

## 4. Two mechanisms for the loader boundary
- New common platform/Registration.java and fabric/forge/neoforge platform/<loader>/RegistrationImpl.java, identical to 26.1.1's (they call `SomeGooglyCommon.id`, which Magic-numbers had already added to 26.2).
- registry/ModContent.java: `Supplier` fields registered in field initializers in registration order, with the ordering comment; `init()` empty; `register(ContentRegistrar)` gone. Now equals 26.1.1's.
- Entry points SomeGooglyFabric, SomeGooglyForge, SomeGooglyNeoForge: identical to 26.1.1's (`ModContent.init()`; Forge/NeoForge `RegistrationImpl.attach(modBusGroup / modBus)`). 26.2's Forge GeckoLib wiring is not in the entry point, so nothing needed keeping there.
- SomeGooglyCommon.java: removed `init(ContentRegistrar)` and its two imports. Deviation: that is Dead-code 9's hunk, taken because deleting ContentRegistrar would otherwise leave the file uncompilable. The file now equals 26.1.1's.
- Deleted registry/ContentRegistrar.java, the Fabric/Forge/NeoForge `<Loader>ContentRegistrar.java`, and the empty `registry/<loader>` directories. Only generated docs/javadoc still mention them.
- orientation-code.md: "`ModContent` registers content through the `@ExpectPlatform` `Registration`."

## 5. Layer installation
common client/ClientRenderLayers.java appends with `layers.add(eyes)`; the `addLayer` line removed from common somegoogly.accesswidener (now equals 26.1.1's; no build-env or access-transformer copy); fabric client/fabric/FabricClientEvents.java gets the comment on why it bypasses `helper.register`.

## 8. Misnamed Fabric mixin
fabric mixin/fabric/LivingEntityReactionMixin.java renamed HealReactionMixin.java (class name only); somegoogly-fabric.mixins.json lists it alphabetically. Only review files mention the old name.

## 9. Same config list means different things
- common config/ConfigValue.java `Parsed.set` and fabric config/fabric/TomlConfig.java `assign(ConfigValue.Parsed<?>, ...)`: both now equal 26.1.1's.
- New test `listSettingWithNoValidEntryUsesDefault` before `reloadRejectsFileWithMalformedEntry`: common gametest ConfigGameTestsLogic and the Fabric/Forge/NeoForge ConfigGameTests wrappers (all now equal to 26.1.1's); registration line in Forge and NeoForge SomeGooglyGameTestMod; new forge and neoforge `test_instance/configgametests/list_setting_with_no_valid_entry_uses_default.json` copied from 26.1.1.

## 11. Fabric START_TRACKING timing
No code change. Added after the tracking-broadcast line of platform-notes.md and build-env/platform-notes.md a 26.2 [source] line worded as 26.1.1's, naming Fabric API 0.161.0+26.2 (`ServerEntityMixin`, START_TRACKING at `addPairing` `@At("TAIL")`, STOP_TRACKING at `removePairing` HEAD). The source was read during the 26.2 assessment.

## Recalled API facts (first build settles)
Architectury maps `platform.Registration` to `platform.<loader>.RegistrationImpl`; on Fabric ModContent registers at class load, so field order matters; Forge 65 `DeferredRegister.register(BusGroup)` and NeoForge `DeferredRegister.Items.registerItem` exist (both were already used by the deleted registrars); Forge `addMain` and NeoForge `playToClient`/`playToServer` accept the generic type, codec, and handler; `renderer.layers` is mutable and widened.

## In-game checks
On each loader: items, components, creative tab, and eye-modifier recipe register; all payloads travel and a version mismatch is refused; slime eyes draw in front of the outer jelly; on Fabric, healing triggers the heal reaction; with `ambientBehaviorPool = ["not a behavior"]` the default pool is used and Fabric logs the fallback.

## GameTests
+1 per loader (`listSettingWithNoValidEntryUsesDefault`) on Fabric, Forge, and NeoForge.

## Other topics' hunks seen / possible clashes
- Dead-code 9: `SomeGooglyCommon.init` removal taken here (see 4).
- Organization 18: FabricClientNetworkTransport package move; NetworkProtocol javadoc link to `client.platform.ClientNetworking`.
- Organization 6: HealReactionMixin body.
- ClientRenderLayers' `GeckoCompat.clearCaches()` was added by another topic during this run.
