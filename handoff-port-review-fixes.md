# Handoff: port the review fixes to 1.20.1 and 1.21.1

## Scope and source of truth

Port the implemented fixes from this 1.21.4 checkout into the 1.20.1 and 1.21.1 checkouts, adapting each to the target's existing implementation. This document includes the subsequent fixes for the reported GameTest failures. It does not establish that every issue exists in either target: inspect each target before editing, and record an issue as not applicable when the corresponding feature or defect is absent.

Read each target's `CLAUDE.md`, orientation files, and code conventions first. Preserve its supported loaders, build tools, Java level, persistence representation, network format, and released-world migration policy unless the particular fix requires a change. Do not copy this checkout's dependency baselines, pack format, protocol version, or rendering architecture wholesale. Do not decompile or inspect cached Minecraft/loader sources. Builds and GameTests remain user-run unless explicitly authorized.

Removed findings remain removed. In particular, this work did not change Fabric's GameTest working directory or consolidate ambient-wait calculations. Neither belongs in this port. Removing the loader sender assertions, restoring alleged historical anger semantics, restricting the held eye to an old ambient pool, changing the ambient upper bound, and adding settings-copy APIs for tests were also not implemented.

## Source navigation

Production Java paths below are relative to `common/src/main/java/com/github/crittscott/somegoogly/`, except loader-specific classes. Shared test classes are under `common/src/gametest/java/com/github/crittscott/somegoogly/gametest/`. Loader adapters and test wrappers are in their respective loader source trees with the same package prefix.

| Area | Main source locations |
| --- | --- |
| Fabric configuration | `fabric/.../config/fabric/TomlConfig.java`, `FabricConfigMigration.java`, `FabricServerConfig.java`, `FabricClientConfig.java`; `fabric/build.gradle`; `gradle.properties` |
| Definition packet budget | `eye/behavior/BehaviorSettings.java`; `network/EyeConfigSyncPacket.java`; caller in `config/EyeConfigReloadListener.java` |
| Client expression lifetime | `client/ClientEyeRuntime.java`, `ClientNetworkHandler.java`, `GooglyTracker.java`; `network/EyeBehaviorTriggerPacket.java` |
| Server scheduling | `eye/behavior/ServerBehaviorScheduler.java` |
| Interaction decisions | Forge/NeoForge `server/<loader>/*ServerEvents.java`; shared `server/EyeItemService.java` remains the gameplay service |
| Authoring spawn lifecycle | `picker/PickerSpawnService.java`; existing loader `platform/<loader>/MobSpawningImpl.java` finalization bridges |
| Target picking | `util/LookTarget.java`; `client/EyeInspector.java` |
| Head loot | `eye/state/SkullEyeState.java`; Forge/NeoForge `server/<loader>/HeadEyesLootModifier.java`; loader resource trees |
| Diagnostics | `server/ServerServices.java`; loader server-config adapters; `client/picker/PickerExporter.java` |

The current source is authoritative. Original review line numbers and report summaries may be stale; locate the symbols named here.

## 1. Fabric configuration parsing and preservation

**Issue.** The handwritten TOML parser rejected valid syntax, including digit separators in integers and quoted keys, and discarded table identity. Missing-key completion regenerated the file from normalized runtime values. That could replace an administrator's intended value with a default and discard comments, unknown keys, and unrecognized list entries.

**Implemented solution.** Fabric bundles NightConfig core and TOML, pinned here to `3.8.3` through `nightconfig_version`. Both modules are development `implementation` dependencies and nested production dependencies through Loom's `include`; Fabric declares Maven Central for resolution. `TomlConfig` parses into an insertion-ordered `CommentedConfig`, and schema entries read from their declared table rather than a flattened key map. Integer conversion accepts parsed `Integer` or in-range `Long`; the existing `ConfigValue` validators still normalize runtime values.

`completeIfMissing` parses the commented defaults and recursively adds only absent entries to the parsed document. Existing raw values are not replaced with their normalized runtime forms. The writer retains parsed comments and unknown content; before reformatting, a sibling `.bak` retains the original text, including formatting or comments the parser/writer does not preserve verbatim. A complete, unmigrated file is not rewritten.

Server loading parses the document and resolves its schema tables before resetting or assigning shared runtime values. Client loading likewise parses and resolves its table first. A syntax failure leaves the file and existing runtime values intact and logs the error. Default rendering uses schema defaults, not values left over from a previous load.

**Released-file migration.** `FabricConfigMigration.quoteReleasedHeader` recognizes the released initial `[Server Settings]` or `[Client Settings]` header, optionally preceded by whitespace and comment lines, and quotes that header before strict parsing. The migrated text is written only after parsing succeeds, with an original-text backup. This is a dedicated migration, not a fallback to the old parser.

**Port considerations.** Use the target's actual schema and parser/dependency conventions. Add this specific migration only if the supported preceding release actually wrote those invalid headers. Preserve that target's real section names and lifecycle hooks. Bundle parser dependencies in production as well as providing them to development runs. Do not add readers for formats that never shipped.

**Coverage.** Fabric `ConfigGameTests`: `serverTomlRoundTrips`, `releasedServerTomlKeepsItsValues`, `serverTomlReadsLiteralStringsAndRejectsWrongTypes`, and `tomlCompletionPreservesAuthoredContent`. Cases cover quoted keys, `1_0`, literal strings, table identity, unknown entries, comments, backups, completion, and malformed-file preservation.

## 2. Configuration tests must restore the loaded state

**Issue.** `releasedServerTomlKeepsItsValues` ended by calling `ServerConfig.resetDefaults()`. Later tests therefore inherited defaults instead of the server configuration loaded before the test.

**Implemented solution.** The Fabric test captures each public shared `ConfigValue` and a restoration action, then restores the captured values in `finally`. Capture uses a generic helper so parsed values are restored through their normal setters. Temporary files, including completion backups, are cleaned up.

**Port considerations.** Snapshot the target's complete affected runtime configuration. Do not modify its native backing file to restore an in-memory test fixture. Keep the capture helper in tests; this fix did not introduce production settings-copy APIs.

## 3. Test actual native configuration correction

**Issue.** `configSectionsMatchReleasedFiles` compared constants with literals but documented a real native-file correction scenario. Its reproduction also described an unsupported direct world upgrade for this checkout.

**Implemented solution.** Its Javadoc now accurately describes a names-only test and the supported source release. Forge and NeoForge each add `releasedConfigSurvivesNativeCorrection`: parse a partial released-style configuration, call the actual native `SPEC.correct`, and assert that a non-default value survives while an absent behavior key is added. The native server configuration classes expose their existing `SPEC` as a documented public constant.

**Port considerations.** Use each target's native spec and supported release fixture. Adapt the reproduction to that target's migration policy; do not copy the 1.21.4 document's supported-world statement into an older branch unchanged. This scenario exercises spec correction, not every part of the loader's disk lifecycle.

## 4. Keep client-controlled behaviors inactive before synchronization

**Issue.** `ClientEyeConfigs` initially used `BehaviorSettings.DEFAULTS`, enabling local held-eye expressions before the server had authorized those settings. Disconnect reset the store to the same enabled defaults.

**Implemented solution.** `BehaviorSettings.NONE` disables ambient play, hit-grow, heal-swirl, and all ongoing behaviors while retaining valid timing fields. The client store uses it initially and after `clear`. `DEFAULTS` remains the server's actual default configuration.

**Follow-up test fix.** `angerFollowsEntityLifetime` previously relied on the client's implicit enabled defaults and consequently failed after this change. It now explicitly installs enabled behavior settings for its scenario and restores the original settings in `finally`. Do not fix that test by re-enabling pre-sync production behavior.

**Port considerations.** Apply the inactive state wherever the target stores received behavior settings. A target without this synchronized settings model needs an equivalent readiness policy only if it has the same defect. Tests exercising enabled client behavior must supply an enabled fixture explicitly.

## 5. Reserve the complete definition-packet budget

**Issue.** `EyeConfigSyncPacket.wireSize` measured `BehaviorSettings.DEFAULTS` although the writer sent current settings. A larger configured pool or timing representation could push an accepted definition set over the packet limit. Measuring only the settings present at datapack reload would still allow a later settings change to invalidate that set.

**Implemented solution.** `BehaviorSettings.WIRE_MAXIMUM` includes every supported expression and ongoing behavior and the largest supported numeric settings. `wireSize` uses the real body writer with this maximum block, reserving space for any supported later configuration. The packet's existing actual-size encoder check remains. The definition reload caller keeps the previous set when the conservative budget exceeds the limit.

**Port considerations.** Derive the maximum from the target's actual serialized fields, registered IDs, bounds, and pool normalization. Do not add newer fields merely to reuse the maximum object. This fix does not change the 1.21.4 wire layout; if a target implementation changes its layout, follow that target's protocol-version rules rather than copying version `17`.

**Coverage.** `SerializationGameTestsLogic.configSyncPacketRoundTrips` checks that encoding the maximum settings block exactly matches the reserved size and that a non-default packet fits it.

## 6. Negative packet tests must reject for the intended reason

**Issue.** Handcrafted definition packets omitted the trailing behavior settings. Assertions accepted any runtime exception, so buffer underflow could satisfy the test even if semantic validation had been removed.

**Implemented solution.** `singleConfigPayload` writes the complete tail, including `BehaviorSettings.DEFAULTS`. A safe control decodes successfully and consumes every byte. `rejectsConfig` requires a `DecoderException` containing the intended count, variant, or position diagnostic and releases the fixture buffer. The excessive-count fixture deliberately tests early rejection before reading entries; its identifying diagnostic distinguishes that rejection from truncation.

**Port considerations.** Finish the target's actual packet shape, not this version's shape. Use an independently valid control and identify the specific rejection for each malformed condition.

## 7. Separate received expressions from disposable render trackers

**Issue.** Expression packets were ignored when no `GooglyTracker` existed. A newly tracking viewer could miss a catch-up packet before first render. Offscreen simulation paused expression age, and tracker eviction or geometry replacement lost the active expression.

**Implemented solution.** `ClientEyeRuntime` owns a map from known living entities to `ReceivedBehavior(behavior, duration, seed, startedAt)`. Receipt stores `startedAt = simulationTicks - elapsed`, even without a render tracker. Unknown entity IDs are still ignored by the packet handler.

The simulation clock advances only on unpaused simulation ticks. `tickBehaviors` expires reports independently of rendering. `get` and the simulation path synchronize a tracker's expression with the retained report; a replacement tracker reruns seeded initialization and catches up to the correct age. The simulation path accounts for the age increment that `GooglyTracker.update` itself performs. A newer authoritative report replaces a stale local expression rather than being blocked by it.

Render-cache `clear` retains reports. Entity removal deletes that entity's report, and world replacement/disconnect clears reports and their clock. Anger retains its existing entity-lifetime ownership. Pupil physics can still pause offscreen and be evicted independently. Packet Javadocs describe this ownership.

**Port considerations.** Put durable received state in the target's existing client runtime; do not backport the 1.21.4 render-state architecture. Preserve seeded initialization and avoid double-incrementing age. Distinguish renderer/cache resets from actual entity/world teardown, and preserve pause semantics.

**Coverage.** `receivedExpressionsOutliveRenderTrackers` covers receipt before rendering, elapsed catch-up, cache replacement, paused ticks, offscreen expiry, entity removal, and world replacement. Connected-client reproduction remains necessary for actual packet/unload delivery and visual agreement.

## 8. Suspend automatic scheduling while the master switch is off

**Issue.** Saved eye flags survive disabling the feature, so watched mobs continued automatic reactions, ambient scheduling, anger checks, and related packets while their gameplay eyes were hidden.

**Implemented solution.** `eventTarget` rejects automatic reactions while disabled. Tracking still maintains watcher counts and active membership but skips behavior/anger catch-up sends. `serverTick` advances its clock, retires expired effects, and removes obsolete entries before skipping anger and ambient work. Explicit admin `trigger` remains available for authoring.

A `suspended` flag detects resumption. On resume, current viewers receive anger state and any still-active expression, including viewers who began tracking while disabled. Without that refresh, a newly added viewer could miss an unchanged anger value already cached for older viewers. Stop clears suspension state with the scheduler's other state.

**Port considerations.** Preserve saved eyes and watcher bookkeeping. Gate automatic work separately from explicit authoring commands. Resume synchronization must cover viewers added during suspension, not just changes from the scheduler's previous cached anger value.

**Coverage.** `disabledEyesSuspendAutomaticBehaviors` checks disabled automatic work, continued explicit triggers, expiry, and resumed reactions through retained tracking. Packet delivery and anger catch-up still need connected-client checks.

## 9. Respect separate native item-use denials

**Issue.** Forge/NeoForge block handlers ran after protection listeners but inspected neither loader's separate item-use decision. An uncanceled event with item use denied could still apply or harvest a placed head's eyes, consume an item, damage shears, or create a drop.

**Implemented solution.** Before calling `EyeItemService.useOnSkull`, Forge returns on `Event.Result.DENY` and NeoForge returns on `TriState.FALSE`. The existing late priority, cancellation handling, and shared vanilla permission checks remain. Returning preserves the earlier listener's result.

**Port considerations.** Use the target loader's actual item-use result type and refusal value. Do not copy NeoForge's result type into another version on assumption. Preserve the native event path and the target's existing player test fixture.

**Coverage.** Native `deniedItemUseLeavesHeadsUntouched` wrappers post real events with item use denied without cancellation. The shared scenario checks refused application and harvesting, unchanged items/durability/drops, and an allowed-event control proving that the adapter actually runs.

## 10. Refused authoring spawns must not alter terrain

**Issue.** Bulk spawning wrote its floor, basin/water, and roof before finalization could refuse the mob. A further problem surfaced in the supplied Forge run: the convenience `tryAddFreshEntityWithPassengers` path reported success despite the native insertion refusal, so deferring terrain until that return value alone was insufficient.

**Implemented solution.** `PickerSpawnService` collects `BlockChange` records through `planPlatform`, `planBasin`, and `planRoof`, or a planned water column. It computes the destination and performs normal loader finalization without applying those edits. Terrain is applied only after insertion succeeds; refused candidates leave their cell unchanged.

The follow-up `addCommandEntity` helper collects the root entity and indirect passengers, checks for UUIDs already present in the level, and calls `level.addFreshEntity` for each member, checking each actual result. If any insertion refuses or throws, it discards the members it already added, in reverse order. Both bulk and single spawning use this helper. Thus a refusal needs no terrain rollback, and an accepted root cannot be left behind after a rider refusal.

**Port considerations.** Use the target's real per-entity insertion result and normal loader hooks. Preserve rider relationships and remove only additions made by this attempt. Do not treat a convenience method's name or boolean as proof that all native insertion decisions succeeded. Keep terrain untouched until acceptance; this avoids destroying block-entity contents and then attempting to reconstruct them.

**Coverage.** Native `refusedFinalizationLeavesTerrain` and `refusedInsertionLeavesTerrain` wrappers install temporary refusing listeners and unregister them in `finally`. The shared `refusedBulkSpawnLeavesTerrain` scenario limits candidates to a land and water mob, snapshots the cell, and checks that blocks and an eyed skull block entity remain unchanged. Wrappers require both candidates to reach the refusing listener, preventing an empty-run false pass.

## 11. Extended entity picking must stop at blocking surfaces

**Issue.** The shared picker performed entity intersections out to full reach without limiting them by a nearer block. Inspection and admin commands could select mobs behind walls. The inspector also compared a skull hit with the entity's feet instead of the ray intersection.

**Implemented solution.** `LookTarget.livingHitInCrosshair` performs the block pick first and caps the entity intersection's maximum squared distance at the blocking hit. `livingInCrosshair` remains the entity-returning convenience method. `EyeInspector` uses the entity hit location when comparing distances.

**Port considerations.** Keep the target's existing extended reach, pickability/spectator filter, and fluid policy. Adapt to its ray-picking API without replacing the whole selection system.

**Coverage.** `entityTargetingStopsAtWalls` first selects an unobstructed cow, then inserts a wall and requires no target. Inspector skull/entity ordering also warrants an in-game check.

## 12. Give head-loot copying one shared implementation

**Issue.** Forge and NeoForge independently implemented the same block-entity lookup, eye-snapshot check, head-item filtering, and stamping loop.

**Implemented solution.** Both native `HeadEyesLootModifier.doApply` methods delegate to `SkullEyeState.copyToLoot(LootContext, Collection<ItemStack>)`. Native superclasses, codec registration, and differing method signatures stay in their loaders. Fabric retains its native loot-table integration.

**Port considerations.** Port only if the target has the corresponding head-eye feature and duplicate policy. Use its existing item and block-entity storage representation; introducing this version's components is not part of sharing the rule.

**Coverage.** Existing `brokenHeadKeepsItsEyes` checks that breaking a head creates an item carrying the snapshot and that applying the item's data to a placed head restores it.

## 13. Loader-owned loot declarations need valid resource packs in development

**Issue.** Native loot declarations lived in common and were bundled into unrelated loader artifacts. Moving them to their owning loaders exposed a development-run requirement: Forge warned that `mod:somegoogly` had no pack metadata and the broken-head preservation test failed because the loader-owned declarations were not loaded as a valid pack.

**Implemented solution.** The Forge global declaration lives at `forge/src/main/resources/data/forge/loot_modifiers/global_loot_modifiers.json`; the NeoForge declaration lives under its corresponding loader tree. Each loader has its own `data/somegoogly/loot_modifiers/head_eyes.json`. Their common copies were removed, and adapter comments name loader ownership.

The follow-up build fix makes each native `processResources` copy the shared `common/src/main/resources/pack.mcmeta` into that loader's processed resources. Each `shadowJar` applies `DuplicatesStrategy.EXCLUDE` specifically to `pack.mcmeta`, retaining one copy when common is bundled. Common keeps the shared metadata source. This makes native data visible in development without giving Fabric the native declarations.

**Port considerations.** Inspect both development resource roots and production composition. Use the target's correct shared pack metadata; never copy the 1.21.4 pack format into an older branch. Limit duplicate handling to the shared metadata rather than suppressing all archive duplicates.

## 14. Source archives must match bundled implementation

**Issue.** Loader production archives bundled common's classes, but loader source archives contained only loader-local sources.

**Implemented solution.** Root `build.gradle` configures each enabled loader's `sourcesJar` to include `project(':common').sourceSets.main.allSource` before the existing source-remapping path.

**Port considerations.** Use the target's enabled loader list and task model. Include common once, preserve its source-remapping setup, and keep the target's build snapshot synchronized. No build-tool upgrade or GameTest-directory change is part of this fix.

## 15. Local maintenance fixes

Each row states an implemented change; apply it only when the same code and its prerequisites exist in the target.

| Issue | Implemented solution | Source / port condition |
| --- | --- | --- |
| Invalid internal eye indexes silently returned default geometry/appearance, zero counts, or a clamped variant. | Use direct selected-list access for head/eye indexes and remove variant clamping; retain the meaningful absent/disabled configuration and `hasConfig` gate. | `eye/HeadInfo.java`. First verify that target callers select from the same validated configuration and bound loops by its counts. |
| Animation playback repaired contracts already established before installation. | Assign duration directly; access the initialized blink mask directly; remove the impossible zero-eye return; retain only the legitimate no-partner sentinel and held-eye helper-null case for cross-eye. | `BehaviorInstance.java`, `EyeBehavior.java`. Preserve input validation, positive-duration checks, seeded `onStart`, and cross-target validation. |
| A weight-overflow validation branch was unreachable under preceding bounds. | Remove that branch and the exclusively used `somegoogly.validation.weight_overflow` translation. | `config/EyeConfigLimits.java`, `assets/somegoogly/lang/en_us.json`. Prove the target's own count/weight bounds first; do not remove a reachable guard. |
| The renderer duplicated vanilla interpolation with a different argument order. | Replace all local helper calls with `Mth.lerp(fraction, start, end)` and delete the helper. | `client/render/GooglyEyeRenderer.java`. Preserve the argument order when adapting each call. |
| The server did not log its effective master switch. | `ServerServices.onServerConfigApplied` logs at startup and effective changes, suppresses unchanged values, and clears its cached diagnostic state on stop. All loader config adapters call it after application. | `ServerServices.java`, loader server-config classes. Do not log each entity, packet, or join. |
| Bulk-export disk failures existed only in chat. | Track the current entity and destination outside the loop and log one error with completed-file count and exception, retaining translated chat feedback. | `client/picker/PickerExporter.java`. No per-file success logging. |
| Export Javadocs incorrectly claimed identifier syntax prevented traversal. | Document actual registry-membership validation and state that `eyeFile` constructs a path without checking containment. | `PickerExportService.java`, `EyeConfigReloadListener.java`. This is a contract correction, not a newly implemented filesystem containment check. |
| Spawn-all cooldown Javadoc said per-player despite one shared timestamp. | Describe the interval as server-wide. | `PickerGate.SPAWN_ALL_COOLDOWN_TICKS`. Do not change cooldown behavior while porting the comment. |
| Client-local anger bypassed the existing switched-level helper. | Route the local ongoing anger calculation through `level(ANGER, settings)` while separately gating the server-reported anger flag. | `client/GooglyTracker.java`. This does not restore alleged historical switch semantics. |
| A whole behavior-settings object was named `ambient`. | Rename its parameters/usages to `behaviors`. | `client/SkullEyes.java`. Timing calculations remain unchanged. |
| The held-eye ambient pool was copied and filtered on every attempt. | Cache the pool without `CROSS_EYE`, rebuilding when the immutable settings object changes; uniformly select from the eligible entries. | `client/HeldEye.java`. Keep other configured expressions eligible; color-change renders through the shared cornea-tint path. |
| Configuration documentation called color-change admin-only. | Explain that it is absent from the default pool but can be configured into the pool or triggered by an admin command. | `docs/configuration.md`. Match the target's actual supported behavior. |

## Port order and completion record

1. Read the target and mark which numbered issues apply. Keep any existing unrelated work intact.
2. Port configuration preservation, packet budgeting/fixtures, client expression ownership, and scheduler suspension with their regression scenarios.
3. Port interaction denials, accurate spawn insertion decisions, obstructed targeting, and head-loot ownership. Include native development pack metadata with the resource move, not as a later optional cleanup.
4. Port applicable maintenance rows and source-archive composition. Adapt test wrappers and player fixtures to the target's existing loaders; the Forge and NeoForge fixtures in this checkout are deliberately different.
5. Update affected target documentation, orientation snapshots, and covered `build-env/` copies. Preserve fixed headers and size limits. Leave generated Javadocs to the user's documentation task.
6. Record implementation and verification separately for each version and loader: issue applicable or absent, source changed, scenario ported, user-run result, and remaining manual checks.

Existing player-visible features should acquire equivalent GameTest scenarios with in-game reproduction Javadocs. Do not weaken assertions to make a port pass or replace a real native-event test with a constants-only check. Production packaging, client visuals, real released files/worlds, and actual connection lifecycle still require their respective checks.

## Verification status of this source checkout

The supplied pre-follow-up run executed 131 Fabric scenarios with one failure, `angerFollowsEntityLifetime`, and 129 Forge scenarios with three failures: `angerFollowsEntityLifetime`, `brokenHeadKeepsItsEyes`, and `refusedInsertionLeavesTerrain`. Those observations exposed the fixture, development pack-metadata, and convenience-insertion issues described above. Do not characterize that run as passing.

The current source includes the subsequent fixes: explicit/restored client behavior settings in the anger test; native processed-resource pack metadata with narrowly scoped archive deduplication; and per-member insertion-result checking with partial-addition cleanup. Those follow-up changes have been inspected but have not been rerun through Gradle or GameTests by the assistant. No post-fix passing run or NeoForge result is established by the supplied output. The 1.20.1 and 1.21.1 checkouts have not been audited or changed for this handoff.
