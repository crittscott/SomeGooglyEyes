# Report: Loader-divergence (26.1.1)

Applied 2026-11-11 by taking 26.1's hunks for this topic, diffed file by file. Nothing was compiled, built, or tested. Fix lines and intro in review-Loader-divergence.md filled in. Line endings kept (Java, JSON, orientation-code.md LF; both platform-notes.md CRLF).

## 1. Each loader keeps its own copy of the payload table
- NetworkProtocol: `CLIENTBOUND`/`SERVERBOUND` and `Clientbound`/`Serverbound` records; kept `NETWORK_VERSION = "18"` (Networking bumps it), `id(String)` (Magic-numbers 9 removes it), and the javadoc link to `platform.ClientNetworking` (a later topic moves it). Rewritten with Write (LF) since only other topics' lines were left out.
- FabricNetworkTransport: generic `registerType` (one per direction) and `registerReceiver`; seven per-packet imports gone; handshake ids still use `NetworkProtocol.id(...)`, so the `SomeGooglyCommon` import was not re-added. Remaining diff is Magic-numbers 9's.
- FabricClientNetworkTransport registers receivers from `CLIENTBOUND` through a generic `registerReceiver`; stays in `network.fabric` (Organization 18 moves it).
- ForgeNetworkTransport: `addClientbound`/`addServerbound` helpers; inline `Identifier channelId` kept (Magic-numbers 9).
- NeoForgeNetworkTransport copied whole from 26.1.
- orientation-code.md: "`NetworkProtocol` declares version 18 and the payload table all loaders register. Fabric checks it during configuration; ..." and "clientbound handlers link the client handler only inside lambdas"; took 26.1's trims (dropped "(which also caps payload bytes)", "all three loaders" → "every loader"). Body 12,005 characters by `wc -m` (26.1's 12,004).

## 3, 10
No change.

## 4. Two mechanisms for the loader boundary
- New common `platform/Registration.java` (copied from 26.1) and Fabric/Forge/NeoForge `platform/<loader>/RegistrationImpl.java` (copied from 26.1). 26.1's Fabric and Forge impls call `SomeGooglyCommon.id`, not yet in 26.1.1 (Magic-numbers 9), so those calls were rewritten as `Identifier.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, name)` with the `Identifier` import. **Parent to check these two files.**
- ModContent: `Supplier` fields registered in their initializers, in registration order, with 26.1's comment; `init()` empty; `register(ContentRegistrar)` gone. Kept the `RecipeSerializer<>(MAP_CODEC, STREAM_CODEC)` and `Identifier.fromNamespaceAndPath` resource keys (Magic-numbers 9).
- Entry points call `ModContent.init()` (replacing Dead-code's `ModContent.register(registrar)`); Forge and NeoForge also call `RegistrationImpl.attach(modBusGroup / modBus)`. All three match 26.1.
- Deleted registry/ContentRegistrar.java, the three loader ContentRegistrars, and their empty `registry/<loader>` packages. Generated docs/javadoc may still mention them.
- orientation-code.md: "`ModContent` registers content through the `@ExpectPlatform` `Registration`."

## 5. Layer installation
ClientRenderLayers appends with `layers.add(eyes)`; the `addLayer` line removed from common somegoogly.accesswidener (now matches 26.1); FabricClientEvents gets the comment explaining why it bypasses `helper.register` (remaining diff Magic-numbers 9's).

## 8. Misnamed Fabric mixin
`LivingEntityReactionMixin.java` renamed `HealReactionMixin.java` (class name only; body simplification is Organization 6's); somegoogly-fabric.mixins.json updated, alphabetical.

## 9. Same config list means different things
ConfigValue and TomlConfig copied whole from 26.1. New test `listSettingWithNoValidEntryUsesDefault` before `reloadRejectsFileWithMalformedEntry`: ConfigGameTestsLogic and the three ConfigGameTests wrappers copied whole (entire diff was this test); registration line added by hand to the Forge and NeoForge SomeGooglyGameTestMod; new `list_setting_with_no_valid_entry_uses_default.json` on Forge and NeoForge.

## 11. Fabric START_TRACKING timing
Read `fabric-networking-api-v1` `ServerEntityMixin` at tag `0.145.4+26.1.1` on GitHub: START_TRACKING at `addPairing` `@At("TAIL")`, STOP_TRACKING at `removePairing` `@At("HEAD")`. Added a 26.1.1 [source] line (worded like 26.1's line 78) after line 77 of platform-notes.md and build-env/platform-notes.md. No code change.

## Left to other topics
Magic-numbers 9 (`NetworkProtocol.id`, Fabric handshake ids, Forge channel `Identifier`, `Identifier`s in the Fabric/Forge RegistrationImpls, ModContent resource keys, FabricClientEvents); Networking (version "19"); Organization 18 (move FabricClientNetworkTransport to `client.network.fabric`); Organization 6 (HealReactionMixin body); the `client.platform.ClientNetworking` javadoc link.

## First build settles (recalled API facts, unverified)
Architectury maps `platform.Registration` to `platform.<loader>.RegistrationImpl`; on Fabric ModContent registers at class load so field order matters; Forge 63 `DeferredRegister.register(BusGroup)` and NeoForge `DeferredRegister.Items.registerItem` exist; Forge `addMain` and NeoForge `playToClient`/`playToServer` accept the generic type, codec, and handler; `renderer.layers` is mutable and widened; vanilla `addPairing` sends the spawn packet before its tail.

## In-game checks
On each loader: items, components, creative tab, and eye-modifier recipe register; all payloads travel and a version mismatch is refused; slime eyes draw in front of the outer jelly; on Fabric, healing triggers the heal reaction; with `ambientBehaviorPool = ["not a behavior"]` the default pool is used and Fabric logs the fallback.

## GameTests
+1 per loader (`listSettingWithNoValidEntryUsesDefault`).
