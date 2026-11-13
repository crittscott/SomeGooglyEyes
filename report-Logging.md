# Report: Logging (26.3)

Applied 2026-11-12 by taking 26.2's post-fix hunks for this topic (26.2/report-Logging.md existed). Only this topic's hunks were taken, keeping 26.3's side of every API difference. Nothing was compiled, built, or tested. Fix lines in review-Logging.md filled in.

## 1. A broken eye-definition file still loads partly
- EyeConfigReloadListener: `DataResult` import; parses into `DataResult<ConfigFile> parsed` and takes `parsed.result().orElse(null)`; on null logs "Ignoring invalid eye config {}: {}" with `parsed.error().orElseThrow().message()` and counts `failedParse`. Comment is 26.2's. Replaces the `resultOrPartial` path Guarding left.
- ConfigGameTestsLogic: new `reloadRejectsFileWithMalformedEntry` with in-game javadoc, before `reloadToleratesBadFilesAndDuplicates`.
- Fabric, Forge, and NeoForge ConfigGameTests wrappers before `reloadToleratesBadFilesAndDuplicates` (Fabric `@GameTest(structure = TEMPLATE, maxTicks = 60)`).
- Forge and NeoForge SomeGooglyGameTestMod: `functions.register("configgametests/reload_rejects_file_with_malformed_entry", ...)` before the tolerates line.
- New Forge and NeoForge `test_instance/configgametests/reload_rejects_file_with_malformed_entry.json`, copied from 26.2.
- Reference: 26.2. No 26.3 adaptation needed.

## 2. Client "optional integrations found" line from a hard-coded list
- Resolvers: `ArrayList` import and `availableFamilies()`; matches 26.2's file.
- LLibraryResolver: `familyLabel()` override returns "LLibrary".
- ClientLifecycle: `INTEGRATIONS` and the `ModVersionLookup` import removed; `GeckoCompat`, `Resolvers`, `ArrayList` imports added; `logStartup` lists the available families, then "GeckoLib" if `GeckoCompat.isLoaded()`, with 26.2's javadoc. The `tick()` hunk left for another topic.
- ServerServices: `ConfigValue` and `List` imports; `onServerConfigApplied` adds `behaviorSwitchesOff` (keys of false `ConfigValue.Flag` settings in `ServerConfig.SECTION_BEHAVIORS`); javadoc 26.2's.
- orientation-code.md: "The server logs the effective master switch, spawn percentages, and Behaviors switches off at startup and on change." Body 11,991 characters.
- No NeoForge startup-line change (no Heads or Twilight Forest in 26.3).

## 3. Forge/NeoForge log "client setup finished" from the mod constructor
- ForgeClientBootstrap: `BusGroup` and `FMLClientSetupEvent` imports; private `onClientSetup` calling `ClientLifecycle.logStartup()`, registered via `FMLClientSetupEvent.getBus(context.getModBusGroup())` after `ForgeClientConfig.register`; trailing `logStartup()` removed; `ForgeGeckoLayers.register()` kept.
- NeoForgeClient: `FMLClientSetupEvent` import; `onClientSetup` registered with `modBus.addListener` after the config registration; trailing call removed.
- Fabric's call in SomeGooglyFabricClient unchanged.

## 4. Fabric logs each refused client twice
- FabricNetworkTransport: both "Refusing ..." lines and the now-unused `SomeGooglyCommon` import removed (26.3 already uses `NetworkProtocol.id` there).

## Recalled API facts (unverified)
DFU's `DataResult.result()` is empty on any error even with a partial result; Forge 66 keeps Forge 63's `FMLClientSetupEvent.getBus(BusGroup)` and `FMLJavaModLoadingContext.getModBusGroup()`; NeoForge's `FMLClientSetupEvent` is a mod-bus event; vanilla and Fabric already log the disconnect reason.

## In-game checks
- The client log shows "client setup finished; optional integrations found: [...]" after setup, with the right list on each loader.
- The server log lists the Behaviors switches that are off.
- An eye file with one undecodable entry is logged "Ignoring invalid eye config", and nothing in it loads.
- On a Fabric server, each refused client (vanilla or wrong version) is logged once.

## GameTests
+1 per loader (Fabric, Forge, NeoForge): `reloadRejectsFileWithMalformedEntry`.

## Other topics / expected clashes
- Loader-divergence 1: FabricNetworkTransport's payload table; re-add the `SomeGooglyCommon` import if `SomeGooglyCommon.id` is used there.
- Loader-divergence 9: `listSettingWithNoValidEntryUsesDefault` goes just before `reloadRejectsFileWithMalformedEntry` in the logic class, wrappers, and registration lists.
- Left: `SomeGooglyCommon.id` in EyeConfigReloadListener, ForgeClientBootstrap, NeoForgeClient; `ServerEyeConfigs.isExcluded`; `ServerConfig.roll`; `ClientLifecycle.tick()`; ConfigGameTestsLogic's 26.3 SYNCED server-config path javadoc stays.

## First build settles
`FMLClientSetupEvent.getBus(BusGroup)` on Forge 66.1.0, and the new GameTest on all three loaders.
