# Report: Logging (26.2)

Applied 2026-11-12 by taking 26.1.1's hunks for this topic with the Edit tool, while other topics' agents edited the same checkout. Nothing compiled, built, or tested. Fix lines in review-Logging.md filled in.

## 1. A broken eye-definition file still loads partly
- EyeConfigReloadListener: adds the `DataResult` import and parses into `DataResult<ConfigFile> parsed`. It takes `parsed.result().orElse(null)`; on null it logs "Ignoring invalid eye config {}: {}" with `parsed.error().orElseThrow().message()` and counts `failedParse`. Uses 26.1.1's comment. Guarding's try/catch removal was already present, so the hunk went in unwrapped.
- ConfigGameTestsLogic: new `reloadRejectsFileWithMalformedEntry` with in-game javadoc, placed right before `reloadToleratesBadFilesAndDuplicates`.
- Fabric, Forge and NeoForge ConfigGameTests: wrappers placed before `reloadToleratesBadFilesAndDuplicates`.
- Forge and NeoForge SomeGooglyGameTestMod: `functions.register("configgametests/reload_rejects_file_with_malformed_entry", ...)` before the tolerates line.
- New forge and neoforge `test_instance/configgametests/reload_rejects_file_with_malformed_entry.json`, copied from 26.1.1, LF like the neighboring file.

## 2. Client "optional integrations found" line from a hard-coded list
- Resolvers: adds the `ArrayList` import and `availableFamilies()`, by Edit rather than copying the file whole; the result matches 26.1.1's hunk.
- LLibraryResolver: `familyLabel()` returns "LLibrary".
- ClientLifecycle: removed `INTEGRATIONS` and the `ModVersionLookup` import; added the `GeckoCompat`, `Resolvers` and `ArrayList` imports; `logStartup` lists the available families, then "GeckoLib" if `GeckoCompat.isLoaded()`, with 26.1.1's javadoc. The `tick()` hunk was left for another topic.
- ServerServices: adds the `ConfigValue` and `List` imports. `onServerConfigApplied` adds `behaviorSwitchesOff`: the keys of `ConfigValue.Flag` settings in `ServerConfig.SECTION_BEHAVIORS` that are false. Uses 26.1.1's javadoc.
- orientation-code.md: "The server logs the effective master switch, spawn percentages, and Behaviors switches off at startup and on change." Body was 11,976 characters at the time.
- No NeoForge startup-line change, since 26.2 has no Heads or Twilight Forest.

## 3. Forge/NeoForge log "client setup finished" from the mod constructor
- ForgeClientBootstrap: adds the `BusGroup` and `FMLClientSetupEvent` imports; adds private `onClientSetup`, which calls `ClientLifecycle.logStartup()`, registered via `FMLClientSetupEvent.getBus(context.getModBusGroup())` right after `ForgeClientConfig.register`; the trailing `logStartup()` is removed; `ForgeGeckoLayers.register()` is kept.
- NeoForgeClient: adds the `FMLClientSetupEvent` import; `onClientSetup` is registered with `modBus.addListener` after the config registration; the trailing call is removed.

## 4. Fabric logs each refused client twice
- FabricNetworkTransport: both "Refusing ..." lines and the now-unused `SomeGooglyCommon` import removed.

## Recalled API facts (unverified)
- DFU `DataResult.result()` is empty on any error, even when a partial result exists.
- Forge 65 keeps Forge 63's `FMLClientSetupEvent.getBus(BusGroup)` and `FMLJavaModLoadingContext.getModBusGroup()`.
- NeoForge's `FMLClientSetupEvent` is a mod-bus event.
- Vanilla and Fabric already log the disconnect reason.

## In-game checks
- Client log shows "client setup finished; optional integrations found: [...]" after setup, with the right list on each loader.
- Server log lists the Behaviors switches that are off.
- An eye file with one undecodable entry is logged "Ignoring invalid eye config" and nothing in it loads.
- On a Fabric server, each refused client (vanilla or wrong version) is logged once.

## GameTests
+1 per loader: `reloadRejectsFileWithMalformedEntry`.

## Other topics / expected clashes
- Loader-divergence 1: FabricNetworkTransport's payload table, and adding the `SomeGooglyCommon` import back if `SomeGooglyCommon.id` is used there.
- Loader-divergence 9: `listSettingWithNoValidEntryUsesDefault` goes right before `reloadRejectsFileWithMalformedEntry` in the logic class, the wrappers and the registration lists.
- Hunks seen and left for other topics: `SomeGooglyCommon.id` in EyeConfigReloadListener, ForgeClientBootstrap and NeoForgeClient; `ServerEyeConfigs.isExcluded` and `packetFor`/`refusal` in EyeConfigReloadListener; `ServerConfig.roll`, `onConverted`, `onPlayerCloned` and `ServerEyeConfigs.packet()` in ServerServices; `PickerInput.KEYS` in both client bootstraps; `ClientLifecycle.tick()`; LLibraryResolver's `failed(model)`.

## First build settles
`FMLClientSetupEvent.getBus(BusGroup)` on Forge 65.1.5, and the new GameTest on all three loaders.
