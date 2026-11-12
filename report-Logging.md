# Report: Logging (1.21.11)

Applied 2026-11-11 by hand-porting 1.21.8's changes. Nothing was compiled, built, or tested; everything here comes from reading the source. Each file was diffed against 1.21.8 and only this topic's hunks taken; no file was copied whole.

## 1. A broken eye-definition file still loads partly
- EyeConfigReloadListener: `DataResult<ConfigFile> parsed = ConfigFile.CODEC.parse(...)`; `parsed.result().orElse(null)`; on null logs "Ignoring invalid eye config {}: {}" with `parsed.error().orElseThrow().message()` and counts `failedParse` (1.21.8's comment and message; builds on Guarding 6).
- Test `ConfigGameTestsLogic.reloadRejectsFileWithMalformedEntry` (`Identifier` in place of `ResourceLocation`), placed before `reloadToleratesBadFilesAndDuplicates`; Fabric, Forge, NeoForge `ConfigGameTests` wrappers; `functions.register("configgametests/reload_rejects_file_with_malformed_entry", ...)` in both loaders; forge and neoforge `test_instance/configgametests/reload_rejects_file_with_malformed_entry.json`.
- orientation-build.md now says Forge 193 and NeoForge 195 test functions, matching current register-line counts (it had said 191/193 while the code already had 192/194).

## 2. Client "optional integrations found" line from a hard-coded mod-id list
- Resolvers: `availableFamilies()`. LLibraryResolver: `familyLabel()` returns "LLibrary". ClientLifecycle: `INTEGRATIONS` removed; `logStartup` lists the available families, then "GeckoLib" if `GeckoCompat.isLoaded()`, then "Heads" if `ModVersionLookup.modVersion(SkullEyeState.HEADS_NAMESPACE)` is present.
- ServerServices: the `onServerConfigApplied` summary adds `behaviorSwitchesOff` (false `ConfigValue.Flag` settings in `ServerConfig.SECTION_BEHAVIORS`); 1.21.8's javadoc.
- orientation-code.md: the server log includes the Behaviors switches that are off.
- Deviations: wording stays "optional integrations found" (1.21.8: "optional model libraries found") because Heads is not a model library; Heads is checked with `modVersion`.
- Side effect, as in 1.21.8: reflected resolver handles load at client startup logging instead of first render.

## 3. Forge/NeoForge log "client setup finished" from the mod constructor
- ForgeClientBootstrap: private `onClientSetup(FMLClientSetupEvent)` calls `ClientLifecycle.logStartup()`, registered with `FMLClientSetupEvent.getBus(context.getModBusGroup())`; the trailing call removed; `BusGroup`/`FMLClientSetupEvent` imports added.
- NeoForgeClient: `onClientSetup` registered with `modBus.addListener`; trailing call removed.
- Note: 1.21.11's Forge client events use `.BUS`; only the config classes use `getBus(modBusGroup)`; this follows the FML-event pattern.

## 4. Fabric logs each refused client twice
FabricNetworkTransport: both "Refusing ..." lines removed with the unused `SomeGooglyCommon` import; payload table left for Loader-divergence 1.

## Recalled API facts
DFU `DataResult.result()` is empty whenever there is an error, even with a partial result (the test fails without the fix only if `resultOrPartial` would yield a partial list); Forge 1.21.11 `FMLClientSetupEvent.getBus(BusGroup)` exists; NeoForge's `FMLClientSetupEvent` is a mod-bus event; vanilla/Fabric already log the disconnect reason.

## Left to other topics
Loader-divergence 1 (payload table; must re-add `SomeGooglyCommon` import if used); Loader-divergence 9 (`listSettingWithNoValidEntryUsesDefault`).

## Clash points
EyeConfigReloadListener, LLibraryResolver, Resolvers, ServerServices, ClientLifecycle, ForgeClientBootstrap, NeoForgeClient, FabricNetworkTransport, ConfigGameTestsLogic and wrappers, both SomeGooglyGameTestMod files, orientation-build.md (test counts), orientation-code.md.

## In-game checks
Client log on each loader shows "client setup finished; optional integrations found: [...]" after setup, correct with Citadel, Uranus, LLibrary, GeckoLib, Heads present or absent; the server's "Server configuration applied" line lists off Behaviors switches; a datapack eye file with one undecodable entry is reported "Ignoring invalid eye config" and nothing from it loads; Fabric server refusals of a vanilla and a wrong-version client each appear once.

## Files modified
common main: config/EyeConfigReloadListener, client/ClientLifecycle, client/render/resolver/Resolvers, client/render/resolver/LLibraryResolver, server/ServerServices. Gametest: ConfigGameTestsLogic; fabric/forge/neoforge ConfigGameTests; forge/neoforge SomeGooglyGameTestMod; two new test_instance JSONs. Loaders: FabricNetworkTransport, ForgeClientBootstrap, NeoForgeClient. Docs: orientation-build.md, orientation-code.md, review-Logging.md.
