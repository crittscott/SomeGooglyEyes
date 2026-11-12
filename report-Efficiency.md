# Report: Efficiency (26.1)

Applied 2026-11-11 by hand-porting 1.21.11's changes. Nothing was compiled, built, or tested; everything here comes from reading the source. Each file was diffed against 1.21.11 and only this topic's hunks taken. Taken whole after the diff showed only this topic plus mechanical differences: network/EyeConfigSyncPacket.java (1.21.11's `SomeGooglyCommon.id` → 26.1's `NetworkProtocol.id`), eye/state/EyeState.java (`entity.level().random` → `entity.level().getRandom()` in `drawRoll`), gametest SpawnGatingGameTestsLogic.java. Fix lines in review-Efficiency.md are filled in.

## 1. Eye-definition packet rebuilt for every joining player
- EyeConfigSyncPacket: body serialized once in the public constructor (private three-argument constructor); static `read` keeps the received bytes (`STREAM_CODEC` uses `EyeConfigSyncPacket::read`); `write` copies the bytes; Dead-code 3's `refusal()` uses `body.length`; `configs` and `encoded` are `Map.copyOf` snapshots with an `encoded()` accessor; class javadoc 1.21.11's; `NetworkProtocol.id("eye_config")` kept.
- ServerEyeConfigs: the installed `EyeConfigSyncPacket` replaces the `Installed` record; `encoded()` removed; `replaceIfChanged(EyeConfigSyncPacket)`, `packet()`, `packetFor(configs)` added; class javadoc 1.21.11's. 1.21.11's `isExcluded` (Organization 17) and its `SkullEyeState.variantIndex(entity, …)` signature not brought in.
- EyeConfigReloadListener: `packetFor(selected)`, `packet.refusal()`, `replaceIfChanged(packet)`; `CompoundTag` import dropped.
- PickerExportService: `syncRefusal` uses `ServerEyeConfigs.packetFor(next).refusal()`.
- ServerServices: `eyeConfigPacket()` removed; joins and broadcasts send `ServerEyeConfigs.packet()`; the `sentEyeConfigs` identity check still works (`all()` returns the installed packet's stable `configs()`).
- Tests: `TestEyeConfigs.install` uses `replaceIfChanged(packetFor(next))`; PickerExportGameTestsLogic saves and restores `ServerEyeConfigs.packet()`; `configSyncPacketRoundTrips` also asserts a decoded packet writes the bytes it was sent (javadoc 1.21.11's).
- Wire format and network version unchanged.

## 5. Placement roll stored on every living entity
- EyeState: `initialize` stores the roll only when eyes turn on; `Snapshot.write` writes the roll with eyes and removes it without; `setHasEyes(true)` draws one with new `drawRoll(entity)` when none is stored; javadocs updated. Existing saves keep their rolls until the next eyeless write.
- `spawnAlwaysAssignsAVariantRoll` → `onlyEyedMobsStoreAVariantRoll` with 1.21.11's body, across SpawnGatingGameTestsLogic, the three `SpawnGatingGameTests` wrappers, Forge/NeoForge `functions.register` lines, and `test_instance/spawngatinggametests/only_eyed_mobs_store_a_variant_roll.json` (old JSONs deleted). Generated docs/javadoc HTML still names the old test until regenerated.

## 7. Eyeless mobs stay on the per-tick list
`EyeState.sync` calls new `ServerBehaviorScheduler.onEyesLost` for eyeless living entities (idle mobs leave `ACTIVE`, and `STATES` when untracked); the retire branch drops a mob that lost its eyes mid-behavior; the ambient-only eviction is removed (the countdown just calls `rollAmbient`); class javadoc and tick comment updated. Deviation: uses 26.1's `now` counter (Reinvention 4 brings `clock(mob)`). No test added, as in 1.21.11.

## 2, 3, 6, 8, 9
None.

## Recalled API facts (unverified)
`FriendlyByteBuf.readBytes(byte[])`, `getBytes(int, byte[])`, `writeBytes(byte[])`, `readableBytes()` behave as Netty's; the codec's decode buffer holds exactly this payload's body; `CompoundTag.contains/remove(String)` exist in 26.1.

## In-game checks
Joining players and `/reload` with changed definitions still get eyes on all loaders; an oversized eye set is still refused on reload and export; `/data get entity` on a new eyeless mob shows no `somegoogly:eyeVariantRoll` and `/sg admin eyes true` adds one; with ambient behaviors off, turning a mob's eyes off stops its behaviors without errors.

## Left to other topics
Magic-numbers 9 (`NetworkProtocol.id`); Networking 2; the rest of 1.21.11's scheduler (server clock, hand reactions, `serverAngry`, `onHurt`); Organization 17 (`isExcluded`).

## Clash points
EyeConfigSyncPacket, ServerEyeConfigs, EyeConfigReloadListener, PickerExportService, ServerServices, EyeState, ServerBehaviorScheduler; Serialization, PickerExport, SpawnGating logic, TestEyeConfigs, the three SpawnGatingGameTests wrappers, Forge/NeoForge SomeGooglyGameTestMod.

## GameTests
No net change; renamed on all three loaders: `spawnAlwaysAssignsAVariantRoll` → `onlyEyedMobsStoreAVariantRoll`.

## Files modified
common main: network/EyeConfigSyncPacket.java, config/ServerEyeConfigs.java, config/EyeConfigReloadListener.java, picker/PickerExportService.java, server/ServerServices.java, eye/state/EyeState.java, eye/behavior/ServerBehaviorScheduler.java. Gametest: TestEyeConfigs, PickerExportGameTestsLogic, SerializationGameTestsLogic, SpawnGatingGameTestsLogic; Fabric/Forge/NeoForge SpawnGatingGameTests; Forge/NeoForge SomeGooglyGameTestMod; two renamed test_instance JSONs. Docs: review-Efficiency.md.
