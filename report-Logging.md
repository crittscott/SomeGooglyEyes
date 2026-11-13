# Report: Logging (26.1.1)

Applied 2026-11-11 by taking 26.1's hunks for this topic with the Edit tool. Nothing was compiled, built, or tested. Each touched file re-diffed against 26.1: remaining differences are other topics' (`SomeGooglyCommon.id` in EyeConfigReloadListener/ForgeClientBootstrap/NeoForgeClient, `ServerEyeConfigs.isExcluded`, `ServerConfig.roll` in ServerServices). Fix lines and intro in review-Logging.md filled in; line endings unchanged.

## 1. A broken eye-definition file still loads partly
EyeConfigReloadListener parses into `DataResult<ConfigFile> parsed`, takes `parsed.result().orElse(null)`, and on null logs "Ignoring invalid eye config {}: {}" with `parsed.error().orElseThrow().message()` and counts `failedParse` (26.1's comment; Guarding's try/catch removal kept). New test `ConfigGameTestsLogic.reloadRejectsFileWithMalformedEntry` before `reloadToleratesBadFilesAndDuplicates`: Fabric `@GameTest` wrapper, Forge/NeoForge static wrappers, `functions.register("configgametests/reload_rejects_file_with_malformed_entry", ...)` in both SomeGooglyGameTestMod files, and the two new test_instance JSONs (copied from 26.1).

## 2. Client "optional integrations found" line from a hard-coded list
`Resolvers.availableFamilies()` (file copied whole, CRLF); `LLibraryResolver.familyLabel()` returns "LLibrary"; ClientLifecycle drops `INTEGRATIONS` and `ModVersionLookup`, and `logStartup` lists available families plus "GeckoLib" if `GeckoCompat.isLoaded()` (its `tick()` hunk left for another topic). `ServerServices.onServerConfigApplied` adds `behaviorSwitchesOff` (false `ConfigValue.Flag` keys in `ServerConfig.SECTION_BEHAVIORS`), with 26.1's javadoc. orientation-code.md: "...logs the effective master switch, spawn percentages, and Behaviors switches that are off at startup and on change." orientation-code.md is now at about the 12,000-character body limit.

## 3. Forge/NeoForge log "client setup finished" from the mod constructor
ForgeClientBootstrap: private `onClientSetup(FMLClientSetupEvent)` calls `ClientLifecycle.logStartup()`, registered via `FMLClientSetupEvent.getBus(modBusGroup)`; trailing call removed. NeoForgeClient: `onClientSetup` registered with `modBus.addListener` after the config registration; trailing call removed.

## 4. Fabric logs each refused client twice
FabricNetworkTransport: both "Refusing ..." lines and the unused `SomeGooglyCommon` import removed. Loader-divergence 1 re-adds the import with `SomeGooglyCommon.id`.

## Recalled API facts (unverified)
DFU `DataResult.result()` is empty on any error even with a partial result; Forge 63 has `FMLClientSetupEvent.getBus(BusGroup)` (inferred from 26.1 and 1.21.11); NeoForge `FMLClientSetupEvent` is a mod-bus event; vanilla and Fabric already log the disconnect reason.

## In-game checks
Client log "client setup finished; optional integrations found: [...]" after setup with the right list per loader; server log lists Behaviors switches that are off; an eye file with one undecodable entry is logged "Ignoring invalid eye config" and nothing in it loads; on a Fabric server each refused client is logged once.

## Left to other topics
Loader-divergence 1 (FabricNetworkTransport payload table and `SomeGooglyCommon` import); Loader-divergence 9 (`listSettingWithNoValidEntryUsesDefault` goes right before `reloadRejectsFileWithMalformedEntry` in the logic class, wrappers, and registration lists).

## GameTests
+1 per loader.

## First build settles
`FMLClientSetupEvent.getBus(BusGroup)` on Forge 63.0.2; the new GameTest on all three loaders.
