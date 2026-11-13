# Report: Loader-divergence (26.3)

Applied 2026-11-12 from 26.2's post-fix code (26.2 has a complete report for this topic), taking only this topic's hunks from a `diff --strip-trailing-cr` of each 26.2 file against 26.3's and keeping 26.3's side of every API difference and every other topic's state. Nothing was compiled, built, or tested. Fix lines in review-Loader-divergence.md filled in. Line endings kept (Java files LF, ModContent CRLF).

## 1. Each loader keeps its own copy of the payload table
- common network/NetworkProtocol.java: `CLIENTBOUND`/`SERVERBOUND` lists and `Clientbound`/`Serverbound` records, as 26.2. Keeps 26.3's `NETWORK_VERSION` "18" (26.2's "19" belongs to Networking) and `id(String)` (Magic-numbers 9 not yet run). Class javadoc combines 26.2's payload-table text with the payload-id namespace and keeps the link to `platform.ClientNetworking`.
- fabric FabricNetworkTransport: per-packet imports, type registrations, and play receivers replaced by loops over the lists through generic `registerType` (one per direction) and `registerReceiver`; handshake ids still use `NetworkProtocol.id`.
- fabric FabricClientNetworkTransport: registers receivers from `CLIENTBOUND` through a generic `registerReceiver`; stays in `network.fabric` (Organization 18 moves it).
- forge ForgeNetworkTransport: `addClientbound`/`addServerbound` helpers and loops; keeps its `Identifier.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "network")` channel id.
- neoforge NeoForgeNetworkTransport: copied from 26.2 (differed only by this finding).
- orientation-code.md: "`NetworkProtocol` declares version 18 and the payload table all loaders register." and "Sends go through the platform bridges; clientbound handlers link the client handler only inside lambdas." Two other Networking sentences shortened to stay at the 12,000-character limit.

## 3, 10
No change.

## 4. Two mechanisms for the loader boundary
- New common platform/Registration.java (`@ExpectPlatform`) and fabric/forge/neoforge platform/<loader>/RegistrationImpl.java, taken from 26.2. 26.2's Fabric and Forge impls call `SomeGooglyCommon.id`, which 26.3 lacks yet; written as `Identifier.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, name)`. NeoForge impl unchanged from 26.2.
- registry/ModContent.java: `Supplier` fields registered in field initializers, in registration order, with the ordering comment; `init()` empty; `register(ContentRegistrar)` gone. Equals 26.2's except the resource keys keep `Identifier.fromNamespaceAndPath`.
- Entry points SomeGooglyFabric, SomeGooglyForge, SomeGooglyNeoForge copied from 26.2 (differed only by this finding): each calls `ModContent.init()`; Forge/NeoForge also call `RegistrationImpl.attach(modBusGroup / modBus)`. Forge GeckoLib wiring (`ForgeGeckoLayers`, from ForgeClientBootstrap) untouched.
- Deleted: registry/ContentRegistrar.java, FabricContentRegistrar, ForgeContentRegistrar, NeoForgeContentRegistrar, their `registry/<loader>` directories, and the then-empty loader `registry` directories. Only generated docs/javadoc still mention them.
- orientation-code.md: "`ModContent` registers content through the `@ExpectPlatform` `Registration`."

## 5. Layer installation
- ClientRenderLayers appends with `layers.add(eyes)`.
- somegoogly.accesswidener: only the `LivingEntityRenderer.addLayer` line removed; the `RenderPipelines` lines keep 26.3's `com/mojang/renderpearl/api/pipeline` descriptors. No build-env or access-transformer copy.
- FabricClientEvents gains the comment on why it bypasses `helper.register`.

## 8. Misnamed Fabric mixin
- LivingEntityReactionMixin renamed HealReactionMixin (class name only; 26.3's body is Organization 6's). somegoogly-fabric.mixins.json lists it alphabetically.

## 9. Same config list means different things
- `ConfigValue.Parsed.set` and Fabric `TomlConfig.assign(ConfigValue.Parsed<?>, ...)` copied from 26.2 (both differed only by this finding).
- New test `listSettingWithNoValidEntryUsesDefault`, just before `reloadRejectsFileWithMalformedEntry`: ConfigGameTestsLogic (with `ConfigValue` and `EyeBehavior` imports; 26.3's NeoForge server-config path javadoc kept); Fabric, Forge, and NeoForge ConfigGameTests wrappers; Forge and NeoForge SomeGooglyGameTestMod registrations; new Forge and NeoForge `test_instance/configgametests/list_setting_with_no_valid_entry_uses_default.json` copied from 26.2.

## 11. Fabric START_TRACKING timing
No code change. A 26.3 [source] line added to platform-notes.md and build-env/platform-notes.md (kept identical), after the tracking-broadcast line, worded as 26.2's: Fabric API 0.163.0+26.3 fabric-networking-api-v1 `ServerEntityMixin` fires START_TRACKING at `addPairing` `@At("TAIL")` and STOP_TRACKING at `removePairing` HEAD.

## Recalled API facts (first build settles)
Architectury maps `platform.Registration` to `platform.<loader>.RegistrationImpl`; on Fabric, ModContent registers at class load, so field order matters; Forge 66 `DeferredRegister.register(BusGroup)` and NeoForge `DeferredRegister.Items.registerItem` exist (already used by the deleted registrars); Forge `addMain`, NeoForge `playToClient`/`playToServer`, and Fabric `PayloadTypeRegistry.clientboundPlay`/`serverboundPlay` and `registerGlobalReceiver` accept the generic type, codec, and handler; `renderer.layers` is mutable and widened.

## In-game checks
On each loader: items, components, the creative tab, and the eye-modifier recipe register; all payloads travel and a version mismatch is refused; slime eyes draw in front of the outer jelly; on Fabric, healing triggers the heal reaction; with `ambientBehaviorPool = ["not a behavior"]` the default pool is used and Fabric logs the fallback.

## GameTests
+1 required test per loader (`listSettingWithNoValidEntryUsesDefault`) on Fabric, Forge, and NeoForge.

## Other topics' hunks / possible clashes
- Magic-numbers 9: `NetworkProtocol.id` kept (Fabric handshake ids call it); the ModContent resource keys, Fabric and Forge `RegistrationImpl`s, and the Forge channel id can switch to `SomeGooglyCommon.id`.
- Networking: 26.2's version "19" not taken.
- Organization 18: FabricClientNetworkTransport package move and the `client.platform.ClientNetworking` javadoc link.
- Organization 6: HealReactionMixin body (26.3 still has the `isClientSide` check).
- orientation-code.md sits exactly at the 12,000-character limit; any later addition must trim.
