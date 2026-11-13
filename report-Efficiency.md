# Report: Efficiency (26.2)

Applied 2026-11-12 from 26.1.1's post-fix code, taking only this topic's hunks. Nothing was compiled, built, or tested. Fix lines in review-Efficiency.md filled in.

## 1. Eye-definition packet rebuilt for every joining player
- EyeConfigSyncPacket: public constructor serializes the body once through a private three-argument constructor; `STREAM_CODEC` decodes with a static `read` that keeps the received bytes; `write` copies the stored bytes; `refusal()` measures `body.length` (overlaps Dead-code 3); `configs`/`encoded` are `Map.copyOf` snapshots with an `encoded()` accessor; `wireSize` and `writeBody` removed. The file now equals 26.1.1's exactly (its `SomeGooglyCommon.id` line came from the Magic-numbers agent during this run).
- ServerEyeConfigs: `Installed` record replaced by the installed `EyeConfigSyncPacket`; `encoded()` removed; `replaceIfChanged(EyeConfigSyncPacket)`, `packet()`, `packetFor(configs)` added; class javadoc updated. Deviation: keeps 26.2's `EntityTypes.ENDER_DRAGON`.
- EyeConfigReloadListener: `packetFor(selected)`, `packet.refusal()`, `replaceIfChanged(packet)`; separate wire-size check and `CompoundTag` import removed.
- PickerExportService: `syncRefusal` uses `ServerEyeConfigs.packetFor(next).refusal()`; `EyeConfigSyncPacket` import dropped.
- ServerServices: `eyeConfigPacket()` and its import removed; broadcast and join send `ServerEyeConfigs.packet()`.
- Tests: `TestEyeConfigs.install` uses `replaceIfChanged(packetFor(next))`; PickerExportGameTestsLogic saves/restores `ServerEyeConfigs.packet()` (import added); `configSyncPacketRoundTrips` asserts a decoded packet writes the bytes it was sent and that a small set has no `refusal()` (replacing the `wireSize` assertion), javadoc updated.
- Wire format unchanged.

## 5. Placement roll stored on every living entity
- EyeState: `Snapshot.write` writes the roll only with eyes and removes it otherwise; `initialize` stores the roll only when eyes turn on; new `drawRoll` used by `enableWithProperties` and by `setHasEyes(true)` when no roll is stored; javadocs updated. 26.1.1's other EyeState differences (`copy` gated on `canEverWearEyes`, `overridesTagOrNull` removal) belong to other topics and were left.
- Test: `spawnAlwaysAssignsAVariantRoll` became `onlyEyedMobsStoreAVariantRoll` in SpawnGatingGameTestsLogic (26.1.1's body with 26.2's `EntityTypes.COW`; `EntityPersistentData` import added), the fabric/forge/neoforge `SpawnGatingGameTests` wrappers (CRLF restored after sed), both `SomeGooglyGameTestMod` registrations, and the forge/neoforge test_instance JSON (renamed to `only_eyed_mobs_store_a_variant_roll.json`, function id updated, old file deleted).

## 7. Eyeless mobs stay on the per-tick list
`EyeState.sync` calls new `ServerBehaviorScheduler.onEyesLost` for eyeless living entities. Scheduler: `onEyesLost` added; the retire branch drops a mob that lost its eyes mid-behavior; ambient-only eviction removed; class javadoc line and tick comment updated. Keeps 26.2's `clock(mob)`.

## 2, 3, 6, 8, 9
None.

## Left to other topics / possible clashes
- ServerBehaviorScheduler: 26.1.1's self-watcher code (`onPlayerJoined`/`onPlayerLeft`/`removeWatcher`), the sheared/taming javadoc, and the `GLOBAL_PERCENT` link.
- ServerServices: `onConverted`, `onPlayerCloned`, `ServerConfig.roll`.
- EyeConfigReloadListener: `isExcluded`, `SomeGooglyCommon.id`, and the decode-error hunks.
- PickerExportService: `Locale.ROOT` and `isExcluded`.
- SerializationGameTestsLogic: the trigger-packet and export-age hunks.

Other agents edited ServerServices, EyeConfigReloadListener, ServerBehaviorScheduler, EyeState, EyeConfigSyncPacket, PickerExportGameTestsLogic, SerializationGameTestsLogic, and both SomeGooglyGameTestMod files during this run; these hunks were still in place at the end.

## Docs
orientation-code.md's "Eye state and roll initialize together once" was left as 26.1.1 left it.

## Recalled API facts (unverified)
`FriendlyByteBuf.readBytes(byte[])`, `getBytes(int, byte[])`, `writeBytes(byte[])`, `readableBytes()` behave as Netty's; the codec's decode buffer holds exactly this payload's body; `CompoundTag.contains(String)` and `remove(String)` exist in 26.2.

## In-game checks
- Joining players, and `/reload` with changed definitions, still get eyes on all loaders.
- An oversized definition set is refused on reload and on export.
- A new eyeless mob has no `somegoogly:eyeVariantRoll`, and `/sg admin eyes true` adds one.
- With ambient behaviors off, turning a mob's eyes off stops its behaviors without errors.

## GameTests
Counts unchanged on all loaders; one test renamed on each.
