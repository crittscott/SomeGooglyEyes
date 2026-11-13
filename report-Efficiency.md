# Report: Efficiency (26.3)

Applied 2026-11-12 from 26.2's post-fix code (26.2/report-Efficiency.md), taking only this topic's hunks. Nothing was compiled, built, or tested. Fix lines in review-Efficiency.md filled in.

## 1. Eye-definition packet rebuilt for every joining player
Reference: 26.2.
- EyeConfigSyncPacket: equals 26.2's file except the id line, which keeps 26.3's `NetworkProtocol.id("eye_config")` (no `SomeGooglyCommon` import). The public constructor serializes the body once through a private three-argument constructor; `STREAM_CODEC` decodes with a static `read` that keeps the received bytes; `write` copies the stored bytes; `configs`/`encoded` are `Map.copyOf` snapshots with an `encoded()` accessor; class javadoc updated.
- Dead-code 3 overlap: `refusal()` now measures `body.length`, replacing the scratch `Unpooled` buffer measurement Dead-code 3 left.
- ServerEyeConfigs: the `Installed` record is replaced by the installed `EyeConfigSyncPacket`; `encoded()` removed; `replaceIfChanged(EyeConfigSyncPacket)`, `packet()`, and `packetFor(configs)` added; class javadoc updated; keeps `EntityTypes.ENDER_DRAGON`. 26.2's `isExcluded` (Organization) not taken.
- EyeConfigReloadListener: uses `ServerEyeConfigs.packetFor(selected)`, `packet.refusal()`, and `replaceIfChanged(packet)`; `CompoundTag` import dropped. Other topics' differences (`SomeGooglyCommon.id`, `isExcluded`, decode-error handling) left.
- PickerExportService: `syncRefusal` uses `ServerEyeConfigs.packetFor(next).refusal()`; `EyeConfigSyncPacket` import dropped.
- ServerServices: `eyeConfigPacket()` and its import removed; broadcast and join send `ServerEyeConfigs.packet()`.
- Tests: `TestEyeConfigs.install` uses `replaceIfChanged(packetFor(next))`; PickerExportGameTestsLogic saves and restores `ServerEyeConfigs.packet()`; `SerializationGameTestsLogic.configSyncPacketRoundTrips` also asserts a decoded packet writes the bytes it was sent (javadoc updated).
- Wire format and network version unchanged (protocol 18).

## 5. Placement roll stored on every living entity
Reference: 26.2.
- EyeState: copied whole from 26.2 (its only differences from 26.3 were this finding and finding 7). `Snapshot.write` writes the roll only with eyes and removes it otherwise; `initialize` stores the roll only when eyes turn on; new `drawRoll` used by `enableWithProperties` and by `setHasEyes(true)` when no roll is stored; javadocs updated.
- Test: `spawnAlwaysAssignsAVariantRoll` renamed `onlyEyedMobsStoreAVariantRoll`. SpawnGatingGameTestsLogic equals 26.2's (CRLF kept, `EntityPersistentData` import added); the name changed in the Fabric/Forge/NeoForge `SpawnGatingGameTests` wrappers and both Forge/NeoForge `SomeGooglyGameTestMod` registrations; the Forge/NeoForge test_instance JSON renamed to `only_eyed_mobs_store_a_variant_roll.json` with its function id updated.
- Generated `docs/javadoc` HTML still names the old test; not edited.

## 7. Eyeless mobs stay on the per-tick list
Reference: 26.2.
- `EyeState.sync` calls the new `ServerBehaviorScheduler.onEyesLost` for eyeless living entities.
- Scheduler: `onEyesLost` added after `onHealed`; the retire branch drops a mob that lost its eyes mid-behavior; the ambient-only eviction removed; class javadoc line and tick comment updated.
- Keeps 26.3's `now` counter; 26.2's `clock(mob)`, hand reactions, and other differences belong to other topics.

## 2, 3, 6, 8, 9
None.

## Left to other topics / possible clashes
- ServerBehaviorScheduler: `clock(mob)`, hand reactions, `OngoingBehavior.ANGER.serverAngry`, the `GLOBAL_PERCENT` javadoc link.
- ServerEyeConfigs / EyeConfigReloadListener / PickerExportService: `isExcluded`, `SomeGooglyCommon.id`, decode-error handling, the age check, javadoc wording.
- ServerServices: `ServerConfig.roll` and behavior-switch logging.
- SerializationGameTestsLogic: trigger-packet and export-age hunks.

## Docs
No orientation or doc text described the changed members; nothing edited (as 26.2).

## Recalled API facts (unverified)
`FriendlyByteBuf.readBytes(byte[])`, `getBytes(int, byte[])`, `writeBytes(byte[])`, and `readableBytes()` behave as Netty's; the codec's decode buffer holds exactly this payload's body; `CompoundTag.contains(String)` and `remove(String)` exist in 26.3.

## In-game checks
- Joining players, and `/reload` with changed definitions, still get eyes on all loaders.
- An oversized definition set is refused on reload and on export.
- A new eyeless mob has no `somegoogly:eyeVariantRoll`; `/sg admin eyes true` adds one.
- With ambient behaviors off, turning a mob's eyes off stops its behaviors without errors.

## GameTests
Counts unchanged on all loaders (+0 each); one test renamed on each loader.
