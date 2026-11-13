# Report: Efficiency (26.1.1)

Applied 2026-11-11 from 26.1's changes, taking only this topic's hunks. Nothing was compiled, built, or tested. Fix lines and intro in review-Efficiency.md filled in.

## 1. Eye-definition packet rebuilt for every joining player
- EyeConfigSyncPacket: copied whole from 26.1, then `NetworkProtocol.id("eye_config")` restored and the `SomeGooglyCommon` import dropped (`SomeGooglyCommon.id` is Magic-numbers 9's). The public constructor serializes the body once through a private three-argument constructor; `STREAM_CODEC` decodes with a static `read` that keeps the received bytes; `write` copies the stored bytes; `refusal()` measures `body.length`; `configs` and `encoded` are `Map.copyOf` snapshots with an `encoded()` accessor. Only the id line still differs from 26.1.
- ServerEyeConfigs: `Installed` record replaced by the installed `EyeConfigSyncPacket`; `encoded()` removed; `replaceIfChanged(EyeConfigSyncPacket)`, `packet()`, `packetFor(configs)` added; class javadoc updated. `isExcluded` (Organization 17) is the only remaining diff.
- EyeConfigReloadListener uses `packetFor(selected)`, `packet.refusal()`, `replaceIfChanged(packet)`; `CompoundTag` import dropped. PickerExportService `syncRefusal` uses `ServerEyeConfigs.packetFor(next).refusal()`. ServerServices: `eyeConfigPacket()` removed; joins and broadcasts send `ServerEyeConfigs.packet()`.
- Tests: `TestEyeConfigs.install` uses `replaceIfChanged(packetFor(next))` (file matches 26.1); PickerExportGameTestsLogic saves and restores `ServerEyeConfigs.packet()`; `configSyncPacketRoundTrips` also asserts a decoded packet writes the bytes it was sent.
- Wire format and protocol (18) unchanged.

## 5. Placement roll stored on every living entity
- EyeState copied whole from 26.1 (entire diff was findings 5 and 7): `initialize` stores the roll only when eyes turn on; `Snapshot.write` writes the roll with eyes and removes it without; `setHasEyes(true)` draws one with new `drawRoll` when none is stored.
- `spawnAlwaysAssignsAVariantRoll` renamed `onlyEyedMobsStoreAVariantRoll`: SpawnGatingGameTestsLogic copied whole (CRLF restored); three `SpawnGatingGameTests` wrappers renamed (CRLF kept); Forge/NeoForge `SomeGooglyGameTestMod` registrations renamed; `only_eyed_mobs_store_a_variant_roll.json` added and `spawn_always_assigns_a_variant_roll.json` deleted on Forge and NeoForge.

## 7. Eyeless mobs stay on the per-tick list
`EyeState.sync` calls new `ServerBehaviorScheduler.onEyesLost` for eyeless living entities. Scheduler: `onEyesLost` added; the retire branch drops a mob that lost its eyes mid-behavior; ambient-only eviction removed; class javadoc and tick comment updated. Deviation: scheduler still uses 26.1.1's `now` counter (26.1's `clock(mob)` is Reinvention 4).

## 2, 3, 6, 8, 9
None.

## Left to other topics
Magic-numbers 9 (`SomeGooglyCommon.id` in EyeConfigSyncPacket and SerializationGameTestsLogic); Organization 17 (`isExcluded`); 26.1's other scheduler changes (server clock, hand reactions, `serverAngry`, `onHurt`, `trigger` signature); other topics' hunks in ServerServices, EyeConfigReloadListener, PickerExportService, PickerExportGameTestsLogic, SerializationGameTestsLogic, and both SomeGooglyGameTestMod files.

## Line endings
Git Bash `sed -i` turned the three CRLF `SpawnGatingGameTests.java` wrappers LF; CRLF restored. Other sed-touched files are LF in 26.1 and 26.1.2, so most likely were LF here.

## Recalled API facts (unverified)
`FriendlyByteBuf.readBytes(byte[])`, `getBytes(int, byte[])`, `writeBytes(byte[])`, `readableBytes()` behave as Netty's; the codec's decode buffer holds exactly this payload's body; `CompoundTag.contains(String)` and `remove(String)` exist.

## In-game checks
Joining players and `/reload` with changed definitions still get eyes on all loaders; an oversized set is refused on reload and export; a new eyeless mob has no `somegoogly:eyeVariantRoll` and `/sg admin eyes true` adds one; with ambient behaviors off, turning a mob's eyes off stops its behaviors without errors.

## GameTests
Counts unchanged; one test renamed on all three loaders.
