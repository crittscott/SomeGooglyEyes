# Forge 1.21.1 Port Status

This is the bounded execution snapshot. Overwrite it in place; never append. Reduced history belongs
in `forge-1.21.1-port-log.md`, which is not read during normal execution.

## Current position

| Field | Value |
| --- | --- |
| Overall state | **COMPLETE** — all Forge stages and cross-loader automated gates pass |
| Current stage | Stage 8 — package and documentation reconciliation — complete |
| Current work unit | None — final Forge handoff recorded |
| Work-unit state | Complete |
| Failed verification attempts used | 0 of 3 |
| Stable documents read this session | Yes |
| Common compile state | Passing — cumulative production gate |
| Fabric state | Complete — build and all 78 GameTests pass |
| NeoForge state | Complete — build and all 78 GameTests pass |
| Forge compile state | Passing — completed build |
| Forge GameTest state | Passing — all 78 required tests discovered and passed; server exited cleanly |
| Last command | User verification: all three loader builds and GameTest servers pass without errors |

## Prerequisite

Prerequisite met: `neoforge-1.21.1-port-status.md` is `COMPLETE`, and its Stage 7 handoff records:

- every passing NeoForge completion gate;
- invalidated Fabric regression results;
- final common Architectury ownership matrix;
- common/platform interfaces changed during NeoForge;
- retained manual client and optional-mod checks;
- the exact Forge Stage 0 starting action.

## NeoForge completion handoff

### Passing completion and regression gates

- Common production compilation passed.
- NeoForge production compilation, resource processing, GameTest compilation, and build passed.
- The NeoForge dedicated server discovered and passed all 78 required tests and exited cleanly.
- `neoforge/build/libs/somegoogly-neoforge-0.8.1.jar` was verified by path and file metadata only:
  468,547 bytes, last written 2026-08-30 23:44:00 UTC.
- Invalidated Fabric production and GameTest compilation passed; the Fabric dedicated server
  discovered and passed all 78 required tests and exited cleanly.

### Shared Architectury ownership and interface changes

Common production contains 24 Architectury imports in 17 files. Six existing build-time seams remain
loader-neutral: `ModVersionLookup`, `EntityPersistentData`, `NetworkTracking`,
`GooglyEyeItemFactory`, `ClientRendererAccess`, and `GeckoCompat`. NeoForge implements those seams
without changing their common signatures. Common runtime registration remains in `ModItems`,
`ModDataComponents`, `ModCreativeTabs`, and `ModRecipes`; runtime networking remains in
`NetworkHandler`, `ClientNetworkHandler`, and five picker packet handlers; one environment lookup
remains in `NetworkHandler.registerCommon`.

No NeoForge type entered common production, no new common Architectury use was added, and no
`@ExpectPlatform` signature changed. The only shared production/test correction made during NeoForge
stabilization converts an unknown picker-export identifier to a string at the translation-argument
boundary. Protocol 9, packet ids and bodies, persistence keys, item components, and serialized
formats remain unchanged.

NeoForge retains Architectury 13 temporarily for runtime registration, networking, and the
environment lookup. Forge must replace those unsupported runtime facilities and separately prove
whether build-time `@ExpectPlatform` injection can remain.

### Retained manual checks

- Physical NeoForge client rendering for ordinary adult/baby models, players, slime/magma-cube
  ordering, rabbit, llama, sniffer, villager, and special resolver families.
- Pupil motion and every expression; both item render paths; harvest/craft/apply; Optometrist;
  visibility; picker editing/export; and renderer reload.
- Optional GeckoLib and optional-mod entities available for Minecraft 1.21.1.

### Exact Forge Stage 0 start

In a new session, read `CLAUDE.md`, the Forge plan, process, and this status, then consult only the
relevant Forge assessment section. Verify this handoff, re-inventory the 24 common Architectury
imports, freeze retain/replace decisions for build-time injection and all three runtime categories,
inspect old Forge source only as project behavior reference, and run one diagnostic
`.\gradlew.bat :forge:compileJava --console=plain`. Do not edit production source in Stage 0.

## Completed Stage 7 work unit

### Scope and invariant

The Stage 7 unit addressed only Forge bootstrap module duplication. Forge no longer extends its raw
runtime classpath from `common`; `developmentForge` continues to supply Architectury's transformed
common development module. This is the only loader-specific classpath exception: compile, shadow,
source-set, and development wiring remain aligned with Fabric and NeoForge. No production source or
test assertion changed.

### Intended files

- `forge/build.gradle`
- `forge-1.21.1-port-status.md`
- `forge-1.21.1-port-log.md`

### Verification commands

- User-supplied pre-edit `.\gradlew.bat :forge:runGameTestServer --console=plain` baseline failed
  before discovery with a duplicate-package `ResolutionException`; this baseline does not consume a
  post-edit attempt.
- Post-edit attempt 1: `.\gradlew.bat :forge:runGameTestServer --console=plain` passed in 25s.

### Completion condition

- Met: Forge passed module bootstrap, discovered exactly 78 tests, passed all 78 required tests,
  shut down cleanly, and returned a successful Gradle exit. No newly exposed cause remained.

## Stage 8 completion handoff

- The user verified that Fabric, NeoForge, and Forge all build and run their complete GameTest suites
  without errors. Each dedicated server discovers and passes all 78 required tests.
- Release artifacts were verified by path and file metadata only; none was opened or unpacked:
  - `fabric/build/libs/somegoogly-fabric-0.8.1.jar` — 493,712 bytes, last written
    2026-08-31 23:18:48 UTC;
  - `neoforge/build/libs/somegoogly-neoforge-0.8.1.jar` — 483,318 bytes, last written
    2026-08-31 23:19:19 UTC;
  - `forge/build/libs/somegoogly-forge-0.8.1.jar` — 485,525 bytes, last written
    2026-08-31 23:25:03 UTC.
- `player-view.md`, `as-built.md`, `build-env.md`, `README.md`, and the CurseForge and Modrinth
  descriptions now describe all three completed Minecraft 1.21.1 loaders. The earlier loader status
  documents retain their historical port records with current cross-loader completion notes.
- The intentional Forge-only build difference is documented: Forge omits the raw common runtime
  classpath edge because its transformed development module already supplies common code and the raw
  edge creates duplicate module packages. Compile, source-set, shadow, and development
  transformation wiring remains aligned across loaders.
- Automated port work is complete. Physical-client rendering and optional-mod compatibility checks
  remain manual qualifications and are not automated gate failures.

## Frozen Forge architecture decisions

The independent inventory found the same 24 Architectury imports in the same 17 common production
files recorded by the NeoForge handoff: six build-time injection imports, nine runtime-registration
imports, seven runtime-networking imports, and two environment imports. There are no unclassified
common Architectury imports.

| Category | Exact common surface | Frozen treatment and target stage |
| --- | --- | --- |
| Build-time injection — version lookup | `ModVersionLookup` | Retain the common signature and existing `@ExpectPlatform` mechanism; supply the current Forge implementation in Stage 3. |
| Build-time injection — entity persistence | `EntityPersistentData` | Retain the common boundary and keys; implement with Forge-native persistent entity data in Stage 3. |
| Build-time injection — tracking fanout | `NetworkTracking` | Retain its `CustomPacketPayload` signature; replace its old Architectury carrier with Forge-native tracking fanout in Stage 2. |
| Build-time injection — eye item construction | `GooglyEyeItemFactory` | Retain the common factory signature; port Forge's client renderer attachment in Stage 4. |
| Build-time injection — renderer access | `ClientRendererAccess` | Retain the three common methods; supply only demonstrated Forge access and current map types in Stage 4. |
| Build-time injection — optional GeckoLib | `GeckoCompat` | Retain the soft-loaded common boundary; port the typed Forge 4.7.4 bridge in Stage 4. |
| Runtime registration | `ModItems`, `ModDataComponents`, `ModCreativeTabs`, and `ModRecipes` formerly used `DeferredRegister`, `RegistrySupplier`, and `CreativeTabRegistry` | Stage 1 complete: common uses `ContentRegistrar` and final bind-once handles; Fabric registers immediately through native registries and `FabricItemGroup`; NeoForge and Forge use native deferred registers. |
| Runtime networking | `NetworkHandler`, `ClientNetworkHandler`, and the five picker packet handlers formerly used `NetworkManager` | Stage 2 complete: common uses `NetworkTransport` with vanilla payload types/codecs; Fabric uses Fabric Networking API, NeoForge uses its payload registrar/distributors, and Forge uses a Forge 52 payload channel. Native adapters own direction registration, queued context, server-derived player identity, capability checks, direct sends, and fanout. |
| Environment lookup | `NetworkHandler.registerCommon` formerly used `Platform` and `Env` | Stage 1 complete: each loader supplies physical-side state explicitly; client receiver registration remains reachable only from loader client initialization. |

The build-time seams are retained because the Forge module still uses Architectury Loom's Forge
transformation and `transformProductionForge`, has no Architectury API runtime dependency, and the
diagnostic compiled common and reached the Forge implementation sources without a missing
`@ExpectPlatform` annotation or unresolved stub. Every missing Architectury class in the diagnostic
came from an explicit runtime API import or an exposed runtime-registration type. Final transformed
artifact resolution remains a later packaging gate.

Frozen invariants: introduce no loader type into common; add no replacement cross-loader library;
preserve all content ids, registry identities, persistence keys, data components, packet ids and
bodies, protocol 9, bounds, directions, server authority, configuration semantics, and
player-visible behavior.

## Stage 0 diagnostic baseline

The authorized command reached Java compilation and failed after 9 seconds with 17 errors and 10
deprecation warnings. An initial sandboxed launch could not access the existing Gradle wrapper lock;
the identical approved retry reached the compiler. This pre-edit baseline does not consume a failed
post-edit attempt.

| Stage | Diagnostic classification |
| --- | --- |
| Stage 1 | 3 errors: missing Architectury `EventBuses` import/use in bootstrap and exposed `RegistrySupplier` from `ModItems.SLIMY_EYE`. |
| Stage 2 | 5 errors: old `NetworkTrackingImpl` imports/uses `NetworkManager` and uses stale tracking-target call shapes. |
| Stage 3 | No compiler errors. Six deprecation warnings cover config/mod-context/bootstrap calls; server runtime completeness still requires the planned native lifecycle audit. |
| Stage 4 | 9 errors: stale GeckoLib animatable API, removed HUD overlay event, missing Architectury client-command event, renderer skin-map type mismatch, and their dependent symbols. Four additional deprecation warnings are client listener registrations. |

The legacy Forge files establish responsibilities only: bootstrap and config; persistence and
tracking; reload, connection, tracking, commands, ticks, shutdown, item interactions, drops, damage,
healing, and trades; client lifecycle, item presentation, layers, commands, inspection, picker
input/HUD, and reload cleanup; and optional GeckoLib. Their API names are not evidence for Forge
52.1.16.

## Known evidence

- Forge targets 1.21.1-52.1.16 and has no Architectury 13 Forge runtime dependency.
- Nineteen Forge production Java files and thirteen GameTest files remain from the 1.20.1-era port.
- Common has exactly 24 Architectury imports in 17 production files, matching the handoff inventory.
- The six existing `@ExpectPlatform` seams remain the build-time loader boundary; the three runtime
  categories are assigned to Stages 1 and 2 above.
- Forge metadata is old-form content around current build variables; its Access Transformer is empty.
- Fabric and NeoForge are complete and passing mandatory regression targets.
- The Stage 0 Forge diagnostic produced 17 classified errors and 10 classified warnings; it made no
  production edit and consumed no failed post-edit attempt.
- Stage 1 reduced common Architectury imports from 24 in 17 files to 13 in 13 files: six retained
  build-time injection imports and seven networking imports reserved for Stage 2.
- Common, Fabric, and NeoForge production compiles pass after the registration/environment change.
  The NeoForge compile emitted only its existing deprecated `initializeClient` warning.
- Stage 2 reduced common Architectury imports to the six retained `@ExpectPlatform` seams in six
  files. No Architectury networking import remains in any production source.
- Fabric and NeoForge native transports compile and their dedicated servers each discovered and
  passed all 78 required tests, including the strengthened typed-payload ID assertion.
- The Forge 52 payload channel, sends, capability check, and tracking fanout compile. Its diagnostic
  now reports only nine previously classified Stage 4 client/Gecko/rendering errors.
- Stage 3 replaces deprecated global loading-context lookup with the injected Forge 52 context,
  registers explicit `somegoogly-server.toml` and `somegoogly-client.toml` filenames, and ignores
  config unload events after their specs are cleared.
- Dedicated-server bootstrap no longer owns client handler state or the client-only command argument
  registration. Physical-client setup is contained in `ForgeClientBootstrap`; Forge local client
  commands need no synchronized command-argument registry, matching Fabric and NeoForge.
- Forge server events retain every common service transition. Damage reactions use
  `LivingDamageEvent`, whose amount is the final post-mitigation value in Forge 52.
- Stage 4 consolidates Forge physical-client lifecycle wiring in `ForgeClientBootstrap`, registered
  only through the existing `DistExecutor` guard. Forge-native client commands use
  `RegisterClientCommandsEvent`; the picker HUD uses the 1.21.1-backported
  `AddGuiOverlayLayersEvent`; key mappings are consumed during the end client tick.
- The Forge Access Transformer now matches the 36 established NeoForge rendering declarations
  one-for-one. Forge renderer-map access satisfies the common wildcard skin-map contract.
- Forge GeckoLib integration now targets the 4.7.4 `software.bernie.geckolib.animatable` package and
  remains reachable only through the `ModList` soft-dependency gate with failure isolation.
- No common, Fabric, or NeoForge production source changed during Stage 4. Their production compiles
  and Forge production compilation passed together on the first post-edit verification.
- Stage 5 replaces Forge's template-era metadata prose with its actual declarations and adds
  GeckoLib 4.7.4+ as an optional client dependency. The Forge Gradle dependency remains
  `compileOnly`, and neither source nor processed metadata declares an Architectury runtime
  dependency.
- Forge resource processing passed. Processed `mods.toml` contains resolved Forge `[52.1.16,53)`,
  Minecraft `[1.21.1]`, and optional client GeckoLib `[4.7.4,)` ranges with no unresolved
  placeholders. The processed Access Transformer is present and matches all 36 source declarations.
- The common processed resource tree contains 262 files, including 74 Minecraft eye definitions and
  the expanded pack metadata. Forge's established `shadowBundle` consumes
  `:common:transformProductionForge`, matching the completed loader packaging boundary without
  adding the common Access Widener to Forge runtime resources.
- The Stage 5 cumulative production gate passed for common, Fabric, NeoForge, and Forge. Fabric and
  NeoForge then each discovered and passed all 78 required GameTests and exited cleanly.
- Stage 6 exposes 78 Forge tests through 12 `@GameTestHolder` classes: all 77 shared assertions plus
  one Forge entity-persistence save/load proof. Explicit namespaced templates replace Forge's removed
  prefix annotation, and player-dependent wrappers use survival mock players from `GameTestHelper`.
- Forge GameTest compilation passed on attempt 3. Attempts 1 and 2 were bounded API migrations from
  removed Forge helpers to compiler-proven Forge 52/vanilla equivalents; no tests were weakened.
- Stage 7 removed only Forge's raw `runtimeClasspath.extendsFrom common` edge. The retained
  `developmentForge.extendsFrom common` transformation supplies common code without presenting two
  modules that export the same package; Fabric and NeoForge build files were not changed.
- The first post-edit Forge dedicated-server run discovered exactly 78 tests, passed all 78 required
  tests in 1.017s, shut down cleanly, and completed the Gradle build successfully in 25s.

## Cumulative gates

| Gate | Status |
| --- | --- |
| NeoForge completion handoff | Complete; independently verified |
| Forge architecture decision | Complete; frozen above |
| `:common:compileJava` | Stage 5 cumulative gate passed |
| `:fabric:compileJava` | Stage 5 cumulative gate passed |
| `:neoforge:compileJava` | Stage 5 cumulative gate passed |
| `:forge:compileJava` | Stage 5 cumulative gate passed |
| `:forge:processResources` | Stage 5 passed; metadata and Access Transformer inspected |
| Prior-loader GameTest regressions | Stage 5 Fabric and NeoForge: all 78 passed with clean exits |
| `:forge:compileGametestJava` | Stage 6 passed with 78 intended wrappers |
| `:forge:runGameTestServer` | Stage 7 passed; all 78 required tests passed with a clean exit |
| All loader builds and GameTests | User verified passing without errors after Stage 7 |
| `:forge:build` | Passed per user verification; release artifact metadata verified |

## Blockers

None. The Forge port is complete.

## Exact next action

No automated port stage remains. Perform the documented physical-client and optional-mod checks
before claiming visual or third-party compatibility. Do not commit or publish without explicit
authorization.
