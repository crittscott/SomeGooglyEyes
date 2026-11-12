# Report: Logging (26.1)

Applied 2026-11-11 by hand-porting 1.21.11's changes. Nothing was compiled, built, or tested; everything here comes from reading the source. Each file was diffed against 1.21.11 and only this topic's hunks taken; the two new test_instance JSONs match 1.21.11's byte for byte. Fix lines in review-Logging.md are filled in.

## 1. A broken eye-definition file still loads partly
- EyeConfigReloadListener: `DataResult<ConfigFile> parsed = ConfigFile.CODEC.parse(...)`, `parsed.result().orElse(null)`; on null logs "Ignoring invalid eye config {}: {}" with `parsed.error().orElseThrow().message()` and counts `failedParse`; 1.21.11's comment. Guarding's try/catch removal kept.
- New `ConfigGameTestsLogic.reloadRejectsFileWithMalformedEntry` (before `reloadToleratesBadFilesAndDuplicates`); Fabric `@GameTest`, Forge and NeoForge wrappers; `functions.register("configgametests/reload_rejects_file_with_malformed_entry", ...)` in both SomeGooglyGameTestMod; `test_instance/configgametests/reload_rejects_file_with_malformed_entry.json` in forge and neoforge.

## 2. Client "optional integrations found" line from a hard-coded list
- Resolvers: `availableFamilies()`. LLibraryResolver: `familyLabel()` returns "LLibrary". ClientLifecycle: `INTEGRATIONS` removed; `logStartup` lists available families, then "GeckoLib" if `GeckoCompat.isLoaded()`.
- ServerServices: `onServerConfigApplied` adds `behaviorSwitchesOff` (false `ConfigValue.Flag` settings in `ServerConfig.SECTION_BEHAVIORS`); javadoc as 1.21.11.
- orientation-code.md: the server log line covers the master switch, spawn percentages, and Behaviors switches that are off (body 11,999 characters).
- Deviation: no Heads line (no Heads in 26.1). Side effect, as in 1.21.11: reflected resolver handles load during the startup log instead of first render.

## 3. Forge/NeoForge log "client setup finished" from the mod constructor
26.1's `ForgeClientBootstrap.register(FMLJavaModLoadingContext)` matches 1.21.11's shape. ForgeClientBootstrap: private `onClientSetup(FMLClientSetupEvent)` calls `ClientLifecycle.logStartup()`, registered via `FMLClientSetupEvent.getBus(modBusGroup)`; trailing call removed. NeoForgeClient: `onClientSetup` registered with `modBus.addListener`; trailing call removed.

## 4. Fabric logs each refused client twice
FabricNetworkTransport: both "Refusing ..." lines and the unused `SomeGooglyCommon` import removed.

## Recalled API facts
DFU `DataResult.result()` is empty on any error, even with a partial result; Forge 62 `FMLClientSetupEvent.getBus(BusGroup)` exists (by analogy with 1.21.11 and 26.1's `ModConfigEvent.*.getBus`); NeoForge's `FMLClientSetupEvent` is a mod-bus event; vanilla and Fabric already log the disconnect reason.

## In-game checks
The client log shows "client setup finished; optional integrations found: [...]" after setup with the right list on each loader (never GeckoLib on Forge 26.1); "Server configuration applied" lists the Behaviors switches that are off; a datapack eye file with one undecodable entry is "Ignoring invalid eye config" and loads nothing; on a Fabric server, refusing a vanilla client and a wrong-version client each appears once.

## Left to other topics
Loader-divergence 1 (FabricNetworkTransport payload table; re-add `SomeGooglyCommon` import if it uses `SomeGooglyCommon.id`); Loader-divergence 9 (`listSettingWithNoValidEntryUsesDefault` goes right before `reloadRejectsFileWithMalformedEntry`).

## Clash points
EyeConfigReloadListener, LLibraryResolver, Resolvers, ServerServices, ClientLifecycle, ForgeClientBootstrap, NeoForgeClient, FabricNetworkTransport, ConfigGameTestsLogic and wrappers, both SomeGooglyGameTestMod, orientation-code.md.

## GameTests
Added on each loader: `reloadRejectsFileWithMalformedEntry` (Fabric +1, Forge +1, NeoForge +1).

## Files modified
common main: config/EyeConfigReloadListener, client/ClientLifecycle, client/render/resolver/Resolvers, client/render/resolver/LLibraryResolver, server/ServerServices. Gametest: ConfigGameTestsLogic; Fabric/Forge/NeoForge ConfigGameTests; Forge/NeoForge SomeGooglyGameTestMod; two new test_instance JSONs. Loaders: FabricNetworkTransport, ForgeClientBootstrap, NeoForgeClient. Docs: orientation-code.md, review-Logging.md.
