# Report: Efficiency (1.21.11)

Applied 2026-11-11 by hand-porting 1.21.8's changes. Nothing was compiled, built, or tested; everything here comes from reading the source. Each file was diffed against 1.21.8 and only this topic's changes taken. Two files were taken whole from 1.21.8 after the diff showed they differed only by this topic plus mechanical renames: network/EyeConfigSyncPacket.java (`ResourceLocation` → `Identifier`, `SomeGooglyCommon.id` → `NetworkProtocol.id`, jspecify `@Nullable`) and eye/state/EyeState.java (jspecify `@Nullable` only).

## 1. Eye-definition packet rebuilt for every joining player
- EyeConfigSyncPacket: body serialized once in the constructor; static `read` keeps the received bytes (private three-argument constructor); `write` copies the bytes; Dead-code 3's `refusal()` uses `body.length`; `configs`/`encoded` are `Map.copyOf` snapshots with an `encoded()` accessor; 1.21.8's class javadoc; `NetworkProtocol.id("eye_config")` kept.
- ServerEyeConfigs: the `Installed` record replaced by the installed packet; `encoded()` removed; `replaceIfChanged(packet)`, `packet()`, `packetFor(configs)`; 1.21.8's class javadoc.
- EyeConfigReloadListener: `packetFor(selected)`, `refusal()`, `replaceIfChanged(packet)`; `CompoundTag` import dropped.
- PickerExportService: `syncRefusal` uses `ServerEyeConfigs.packetFor(next).refusal()`.
- ServerServices: `eyeConfigPacket()` removed; join and broadcast send `ServerEyeConfigs.packet()`; the `sentEyeConfigs` identity check still works since `all()` returns the installed packet's stable `configs()`.
- Tests: `TestEyeConfigs` installs via `replaceIfChanged(packetFor(...))`; `PickerExportGameTestsLogic` saves and restores `ServerEyeConfigs.packet()`; `configSyncPacketRoundTrips` asserts a decoded packet writes the same bytes. Deviation: its javadoc names both the byte round trip and the sync-limit check.
- Wire format unchanged; network version unchanged.

## 5. Placement roll stored on every living entity
- EyeState: `initialize` stores the roll only when eyes turn on; `Snapshot.write` writes it with eyes and removes it without; `setHasEyes(true)` draws one (`drawRoll`) when none is stored. Existing saves keep their rolls until next written eyeless.
- Test renamed `spawnAlwaysAssignsAVariantRoll` → `onlyEyedMobsStoreAVariantRoll` (1.21.8's body) in SpawnGatingGameTestsLogic, the three loader wrappers, the Forge/NeoForge `functions.register` lines, and the forge/neoforge `test_instance/spawngatinggametests/only_eyed_mobs_store_a_variant_roll.json` (old JSON deleted).

## 7. Eyeless mobs stay on the per-tick list
- `EyeState.sync` calls `ServerBehaviorScheduler.onEyesLost` for eyeless living entities.
- ServerBehaviorScheduler: new `onEyesLost` (idle mob leaves ACTIVE, and STATES when untracked); the retire branch drops a mob that lost its eyes mid-behavior; the ambient-only eviction removed; javadoc/comment updated. `now`, `isAngry`, `onPlayerHurt`, and durations untouched (other topics). 1.21.8 has no test for this; none added.

## 2, 3, 6, 8, 9
None.

## Recalled API facts (unverified)
`FriendlyByteBuf.readBytes(byte[])`, `getBytes(int, byte[])`, `writeBytes(byte[])`, `readableBytes()` behave as netty's; the codec's decode buffer holds exactly this payload's body; `CompoundTag.contains(String)` exists in 1.21.11.

## Left to other topics
Magic-numbers 9 (`NetworkProtocol.id`); Networking 2 (version bump, durations); the rest of 1.21.8's scheduler (server clock, hand reactions, `serverAngry`, `onHurt`); `ServerEyeConfigs.isExcluded` (Organization 17).

## Clash points
EyeConfigSyncPacket, ServerEyeConfigs, EyeConfigReloadListener, PickerExportService, ServerServices, EyeState, ServerBehaviorScheduler; tests SerializationGameTestsLogic, PickerExportGameTestsLogic, TestEyeConfigs, SpawnGatingGameTestsLogic, Forge/NeoForge SomeGooglyGameTestMod.

## In-game checks
Joining players and a `/reload` with changed definitions still get eyes on all loaders; an oversized eye set is still refused on reload and export; `/data get entity` on a new eyeless mob shows no `somegoogly:eyeVariantRoll`, and `/sg admin eyes true` adds one; with ambient off, turning a mob's eyes off stops its behaviors without errors.

## Files modified
common main: network/EyeConfigSyncPacket, config/ServerEyeConfigs, config/EyeConfigReloadListener, picker/PickerExportService, server/ServerServices, eye/state/EyeState, eye/behavior/ServerBehaviorScheduler. Gametest: TestEyeConfigs, PickerExportGameTestsLogic, SerializationGameTestsLogic, SpawnGatingGameTestsLogic; the three SpawnGatingGameTests wrappers; Forge/NeoForge SomeGooglyGameTestMod; the two renamed test_instance JSONs. Docs: review-Efficiency.md.
