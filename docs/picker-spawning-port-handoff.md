# Handoff: port the picker spawning fix to other release versions

## Task and scope

Port only the working 1.20.1 picker spawning fix to the other nine release checkouts. This document is a handoff, not authorization to run builds or change Git state. No target checkout has been modified while preparing it.

**Do not port the new eye configs yet.** Also exclude the asset dumps, eye generator, placement overrides, eye-review CSV, eye-config budgets, GZIP config synchronization, network protocol changes, and serialization tests associated with that synchronization. The spawning fix needs no eye-definition format change, network change, new dependency, or new mod integration. Do not copy an entire mixed commit or replace entire source files from 1.20.1.

Paths below are relative to a version checkout unless prefixed with a version. The version checkouts are siblings beneath `SomeGooglyEyes`. Read each destination's `AGENTS.md`, `CLAUDE.md`, code conventions, platform notes, and orientation files before editing it. The user runs Gradle, Git, and GitHub tasks unless separately authorized. Do not decompile or extract Minecraft/loader code, or inspect remapped/decompiled cache sources.

## Observed failure and confirmed result

Both Minecraft and Animania bulk spawning stopped at the same distance, slightly beyond fourteen chunks. In the Animania log, the first 46 types in alphabetical order spawned and all remaining 341 failed. The insertion call reported success, but an immediate UUID lookup did not return the spawned entity. Our helper treated that lookup as a refusal and discarded the mob and its passengers.

Because the column advances only after acceptance, every subsequent candidate retried the same failed cell. Because terrain edits are applied only after acceptance, those cells received no sandstone platform either. This was a general spawning problem, independent of eye layouts or Animania species.

The user reran the corrected 1.20.1 code and confirmed: “That works.” The new automated regression scenario exists in source, but no post-fix GameTest results were supplied in this chat. An exact explanation of the observed chunk-distance threshold was not established and is not required for this fix. Do not introduce a guessed distance limit, force-loading workaround, or delayed UUID polling.

For historical context in the 1.20.1 history: `9ffd3b68` (October 23) wrote platforms before insertion and did not perform the post-insertion UUID check; `1cccae4b` (October 30) deferred terrain writes; `7db6132a` (November 4) introduced the immediate UUID check and discard path. These hashes identify inspected 1.20.1 revisions, not a standalone fix commit to apply to every branch.

## Destination inventory

The following source inspection was performed on November 8, 2026. Recheck before editing in case another task has changed a target. All nine destinations currently enable Fabric, Forge, and NeoForge. None contains `bulkSpawnContinuesBeyondNearbyChunks` yet.

| Release checkout | Current insertion helper | Loader test discovery |
| --- | --- | --- |
| `1.21.1` | Passenger-wrapper insertion followed by immediate root UUID lookup | Annotation-based wrappers on all three loaders |
| `1.21.4` | Passenger-wrapper insertion followed by immediate root UUID lookup | Annotation-based wrappers on all three loaders |
| `1.21.8` | Passenger-wrapper insertion followed by immediate UUID lookup for every member | Fabric annotations; Forge/NeoForge function registration and test-instance JSON |
| `1.21.11` | Immediate lookup for every member | Fabric annotations; Forge/NeoForge function registration and test-instance JSON |
| `26.1` | Immediate lookup for every member | Fabric annotations; Forge/NeoForge function registration and test-instance JSON |
| `26.1.1` | Immediate lookup for every member | Fabric annotations; Forge/NeoForge function registration and test-instance JSON |
| `26.1.2` | Immediate lookup for every member | Fabric annotations; Forge/NeoForge function registration and test-instance JSON |
| `26.2` | Immediate lookup for every member | Fabric annotations; Forge/NeoForge function registration and test-instance JSON |
| `26.3` | Immediate lookup for every member | Fabric annotations; Forge/NeoForge function registration and test-instance JSON |

Do not port through unreleased waypoint directories such as `1.21.3`, `1.21.5`, `1.21.6`, or `1.21.9` as part of this task. Later-release API and test conventions must be retained; sharing the same buggy helper does not prove every Minecraft API signature is identical.

## Authoritative 1.20.1 references

| File | Relevant content |
| --- | --- |
| `common/src/main/java/com/github/crittscott/somegoogly/picker/PickerSpawnService.java` | `InsertionFailure`, `addCommandEntity`, and its callers in `spawn` and `spawnOne` |
| `common/src/main/resources/assets/somegoogly/lang/en_us.json` | Root and passenger insertion refusal messages |
| `common/src/gametest/java/com/github/crittscott/somegoogly/gametest/PickerSpawnServiceGameTestsLogic.java` | `bulkSpawnContinuesBeyondNearbyChunks`; existing finalization and terrain-refusal checks |
| `fabric/src/gametest/java/com/github/crittscott/somegoogly/gametest/PickerAuthGameTests.java` | Fabric regression wrapper |
| `forge/src/gametest/java/com/github/crittscott/somegoogly/gametest/PickerAuthGameTests.java` | Forge regression wrapper and existing refusal listeners |
| `docs/picker.md` | Current insertion/rollback behavior |
| `platform-notes.md` | Version-scoped observation about immediate UUID lookup after distant insertion |

There is no 1.20.1 NeoForge wrapper to copy. Adapt the destination's existing NeoForge wrapper and discovery mechanism.

## Implementation to carry across

Change the shared service, covering both single and bulk spawning:

1. Snapshot the root and all passengers before insertion: the 1.20.1 implementation uses `entity.getSelfAndPassengers().toList()`.
2. Maintain a separate list of members whose individual insertion returned success.
3. Insert each member directly. The 1.20.1 implementation uses the boolean result of `level.addFreshEntity(member)`. Verify the corresponding target-version API; do not substitute immediate UUID lookup for a real insertion result.
4. If the root is refused, report `InsertionFailure.MOB_REFUSED`. If a passenger is refused, report `InsertionFailure.PASSENGER_REFUSED`.
5. Mark the operation complete only after all members have been accepted. Return `null` on complete acceptance, matching the current source helper's nullable-failure contract.
6. In a `finally` block, discard successfully added members in reverse order if the operation did not complete. This also cleans up partial additions if another mod throws during insertion. Do not discard pre-existing world entities or members that were never accepted.
7. Remove the passenger-wrapper-plus-immediate-lookup acceptance test. In newer destinations, remove the lookup of every member as well as the root-only form.
8. Update both callers to consume the failure result. In bulk spawning, write the planned floor, basin, and roof and advance the column only after full acceptance. Retain the existing spawn finalization, frozen/persistent state, permissions, exclusions, and genuine cancellation handling.

Do not restore early platform writes merely because an older working revision used them. The corrected 1.20.1 path keeps terrain untouched on refused spawns. Do not expose production helpers solely to make a test call them.

## Diagnostics

Preserve these existing keys, with the precise root-insertion wording used in the source:

- `somegoogly.command.spawn.refused`: `%s — the entity insertion call returned false.`
- `somegoogly.command.spawnall.dropped_add_refused`: `%s — the entity insertion call returned false`

Add the passenger-specific keys and wire them through the failure enum:

- `somegoogly.command.spawn.passenger_refused`: `%s — a passenger's insertion call returned false; previously added members were discarded.`
- `somegoogly.command.spawnall.dropped_passenger_refused`: `%s — a passenger's insertion call returned false; previously added members were discarded`

The intermediate `not_found_after_add` diagnostic was only for investigation. Do not add that key or its failure branch to destinations. If another task already introduced it there, remove the obsolete branch, keys, and documentation while porting. Player-facing messages remain in the language file and should state the observed failure, not guess at chunk loading or another mod's intentions.

## Regression coverage and loader wiring

Port `bulkSpawnContinuesBeyondNearbyChunks` into each destination's shared test logic. It temporarily limits candidates to cow, pig, and sheep; moves a fake player reference to a distant airborne origin without a connected player loading that area; calls the real bulk-spawn service; and checks that all three candidates are accepted and receive separate sandstone floors. The source uses a relative origin `(4096, 128, 0)`, yaw zero, and floor centers at forward offsets `3`, `8`, and `13`. It explicitly clears the block below the distant origin to select platform mode; that position is outside the empty test structure and cannot be assumed to be air. It restores that block, the player's position/orientation, and both exclusion settings in `finally`.

Adapt helper names, assertion-message types, fake-player construction, and test declaration syntax to the destination. Preserve the essential trigger: a distant platform-mode spawn, not an already player-tracked nearby entity or a direct call to a test-only insertion helper. Do not assert immediate UUID lookup visibility as a success criterion.

Register the scenario on all three enabled loaders:

- Fabric: follow the destination's `PickerAuthGameTests` annotation and method conventions; retain the existing Fabric test entrypoint discovery.
- Forge/NeoForge on `1.21.1` and `1.21.4`: follow their annotation-based wrappers and template conventions.
- Forge/NeoForge on `1.21.8` and later releases: add the wrapper method, register its function in `SomeGooglyGameTestMod.java`, and add a matching resource under `src/gametest/resources/data/somegoogly/test_instance/pickerauthgametests/`. Use the neighboring `entity_targeting_stops_at_walls.json` and its function registration as the target-version template. A wrapper method alone will not make the scenario run. These test-instance resources are in scope; eye-definition resources are not.

Keep the existing finalization-refusal and insertion-refusal terrain tests, including the Forge/NeoForge event listeners. Also verify rider handling: a successful rider chain survives, while refusal of a later member removes earlier additions and leaves terrain unchanged. The distant-grid scenario alone does not prove passenger rollback.

## Verification and completion

For each destination, statically trace both command entry points through finalization, insertion, failure feedback, and terrain application on every loader. Check the language JSON, method references, and explicit test registrations. Search code, tests, and docs for the removed immediate-lookup acceptance path and stale diagnostic text.

Have the user run that release's builds and GameTests according to its own instructions. Do not mark unrun tests as passing. In a disposable world, verify `/sg spawnall minecraft` over air continues beyond the former cutoff, the distant platforms and mobs remain when approached, and single spawning still works. Exercise cancellation and rider rollback on the loaders that support those cases. No new optional mod is needed for the Minecraft regression check.

Update only the affected spawning descriptions in `docs/picker.md` and the destination's orientation files. Recompute that destination's discovered test counts rather than copying 1.20.1 totals, and honor orientation size limits. If adding the platform observation, identify it as tested on 1.20.1 until that destination is verified. Mirror modified snapshot inputs such as `platform-notes.md` into `build-env/` as required by the checkout; do not copy unrelated contents wholesale.

Before reporting completion, verify the port contains no new `data/<namespace>/eyes/*.json` files or eye-placement edits, no dumped assets or generator, no `EyeConfigSyncPacket`/`EyeConfigLimits`/`NetworkProtocol` changes, and no unrelated serialization-test edits. Report the release/loader matrix, checks actually performed, unrun checks, and remaining in-game uncertainties. Do not commit, push, build, or execute the port merely because this handoff describes those later steps.
