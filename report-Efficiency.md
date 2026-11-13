# Report: Efficiency (26.1.2)

Applied 2026-11-12 from 26.1.1's post-fix code, taking only this topic's hunks from `diff --strip-trailing-cr` of each 26.1.1 file against 26.1.2's. Nothing was compiled, built, or tested. Earlier topics' hunks (Expected-way, Multiloader-organization, Correctness, Dead-code) were left in place; Dead-code 3's `refusal()` and its reload/export callers were moved onto the cached packet.

## 1. The eye-definition packet is rebuilt and recompressed for every joining player
- EyeConfigSyncPacket: copied whole from 26.1.1 except the id line, which keeps `NetworkProtocol.id("eye_config")` (no `SomeGooglyCommon` import; Magic-numbers 9). Body serialized once in the public constructor through a private three-argument constructor; static `read` keeps the received bytes and is `STREAM_CODEC`'s decoder; `write` copies the bytes; `refusal()` measures `body.length` (Dead-code 3's scratch-buffer measure replaced); `configs`/`encoded` are `Map.copyOf` snapshots with an `encoded()` accessor. Only the id line differs from 26.1.1.
- ServerEyeConfigs: `Installed` record replaced by the installed `EyeConfigSyncPacket`; `encoded()` removed; `replaceIfChanged(EyeConfigSyncPacket)`, `packet()`, `packetFor(configs)` added; class javadoc updated. Remaining diffs from 26.1.1: `isExcluded` (Organization) and 26.1.2's `SkullEyeState.variantIndex(entity, ...)` head signature, both kept.
- EyeConfigReloadListener: `packetFor(selected)`, `packet.refusal()`, `replaceIfChanged(packet)`; `CompoundTag` import dropped. PickerExportService `syncRefusal`: `ServerEyeConfigs.packetFor(next).refusal()`; `EyeConfigSyncPacket` import dropped. ServerServices: `eyeConfigPacket()` and its import removed; joins and broadcasts send `ServerEyeConfigs.packet()`.
- Tests: TestEyeConfigs uses `replaceIfChanged(ServerEyeConfigs.packetFor(next))`; PickerExportGameTestsLogic saves/restores `ServerEyeConfigs.packet()` (adds `EyeConfigSyncPacket` import); `configSyncPacketRoundTrips` asserts a decoded packet writes the bytes it was sent (javadoc updated).
- Wire format and protocol (18) unchanged. No deviation from 26.1.1.

## 2. Each eye recomputes its constant aiming rotation every frame
None.

## 3. Head items without a live simulation decode their eye data from NBT every frame
None.

## 5. Every living entity stores a placement roll even when it has no eyes
- EyeState: copied whole from 26.1.1 (its whole diff was findings 5 and 7; no 26.1.2 additions): `initialize` stores the roll only when eyes turn on, `Snapshot.write` writes it with eyes and removes it without, `setHasEyes(true)` draws one with new `drawRoll` when none is stored.
- Test renamed `onlyEyedMobsStoreAVariantRoll`: SpawnGatingGameTestsLogic copied whole (CRLF, as before); three `SpawnGatingGameTests` wrappers renamed (CRLF restored after sed; now identical to 26.1.1); Forge and NeoForge `SomeGooglyGameTestMod` `functions.register` lines renamed (26.1.2's own registrations untouched); `only_eyed_mobs_store_a_variant_roll.json` added and `spawn_always_assigns_a_variant_roll.json` deleted under forge and neoforge `test_instance/spawngatinggametests`. No remaining references to the old name.

## 6. Forge `find()` creates an empty ForgeData compound on eyeless boats
None.

## 7. Mobs that lose their eyes stay on the per-tick list when ambient expressions are off
- EyeState.sync calls new `ServerBehaviorScheduler.onEyesLost` for eyeless living entities (in the EyeState copied for finding 5).
- ServerBehaviorScheduler: `onEyesLost` added after `onHealed`; the retire branch drops a mob that lost its eyes mid-behavior (and its STATES entry when unwatched); the ambient-only eviction is removed; class javadoc and tick comment updated. Keeps the existing `now` counter (`now >= state.busyUntil`), as 26.1.1 did; the server clock is Reinvention 4.

## 8. Every rendered living entity does registry and NBT lookups each frame
None.

## 9. Small per-frame allocations and reflection on item draws
None.

## Left for other topics / the parent
- Magic-numbers 9: `NetworkProtocol.id("eye_config")` in EyeConfigSyncPacket.
- Organization: `ServerEyeConfigs.isExcluded` and PickerExportService's ender-dragon check.
- Reinvention 4 and other topics: the rest of 26.1.1's scheduler diff (server clock, hand reactions, `serverAngry`, `onHurt`, `trigger` signature).
- Other topics' hunks in ServerServices, EyeConfigReloadListener, PickerExportService, PickerExportGameTestsLogic, SerializationGameTestsLogic, and both SomeGooglyGameTestMod files.
- No orientation, platform-notes, docs, or changelog change: 26.1.1 made none for this topic, and orientation-code.md's sync and scheduler text stays true.

## Recalled API facts and checks only a build or in-game test can settle
- `FriendlyByteBuf.readBytes(byte[])`, `getBytes(int, byte[])`, `writeBytes(byte[])`, `readableBytes()` behave as Netty's; the codec's decode buffer holds exactly this payload's body.
- `CompoundTag.contains(String)` and `remove(String)` exist as used in EyeState.
- GameTest counts unchanged; one test renamed on all three loaders.

## In-game checks
Joining players and `/reload` with changed definitions still get eyes on all loaders; an oversized set is refused on reload and export; a new eyeless mob has no `somegoogly:eyeVariantRoll` and `/sg admin eyes true` adds one; with ambient behaviors off, turning a mob's eyes off stops its behaviors without errors.

## Fix lines
1: EyeConfigSyncPacket taken whole from 26.1.1 (id line keeps `NetworkProtocol.id("eye_config")`): body serialized once, static `read` keeps received bytes, `write` copies them, Dead-code 3's `refusal()` measures `body.length`, `Map.copyOf` snapshots with `encoded()`. ServerEyeConfigs holds the installed packet (`Installed`/`encoded()` removed; `replaceIfChanged(EyeConfigSyncPacket)`, `packet()`, `packetFor` added), keeping `isExcluded` and 26.1.2's `variantIndex(entity, ...)`. Reload listener, `PickerExportService.syncRefusal`, and ServerServices joins/broadcasts use them; TestEyeConfigs, PickerExportGameTestsLogic, and `configSyncPacketRoundTrips` updated as in 26.1.1. No deviation from 26.1.1. Unbuilt.
5: EyeState taken whole from 26.1.1 (roll stored only with eyes, removed without, drawn by `drawRoll` on `setHasEyes(true)`). Test renamed `onlyEyedMobsStoreAVariantRoll` (SpawnGatingGameTestsLogic copied whole, CRLF kept) in the three wrappers, both Forge/NeoForge `functions.register` lines, and `only_eyed_mobs_store_a_variant_roll.json` (forge, neoforge; old JSONs deleted); 26.1.2's own registrations kept. No deviation from 26.1.1. Unbuilt.
7: `EyeState.sync` calls new `ServerBehaviorScheduler.onEyesLost` for eyeless living entities; the retire branch drops a mob that lost its eyes mid-behavior; ambient-only eviction removed; class javadoc and tick comment updated. Keeps the `now` counter, as 26.1.1 did. No deviation from 26.1.1. Unbuilt.
