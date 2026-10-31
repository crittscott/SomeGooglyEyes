# Code Review Findings (merged)

13 findings, most severe first, merged from two reviews. "Found by" says which review reported each one: review A (the original `claude-code-review.md`), review B (`claude-code-review-2.md`), or both. When the reviews gave different line numbers, both are listed.

## 1. `wireSize()` underestimates the sync packet

`common/src/main/java/com/github/crittscott/somegoogly/network/EyeConfigSyncPacket.java:90` (A: 1851)

Found by: both

`wireSize()` measures the payload using `BehaviorSettings.DEFAULTS` rather than the settings that will actually be sent. The reload-time size check therefore undercounts the behavior block, which is now larger: the pool (each entry a full ResourceLocation string), the grow/swirl values and the ongoing-behavior ids.

**Failure scenario:** An admin sets `ambientBehaviorPool` to all 8 ids, and a datapack's encoded eye configs come within a few hundred bytes of `MAX_PAYLOAD_BYTES`. `EyeConfigReloadListener` passes `wireSize()`, so the configs are installed. `write()` then encodes `BehaviorSettings.fromConfig()`, goes over the limit, and throws `EncoderException` on every sync, so joining or reloading clients get disconnected. The check should measure with `BehaviorSettings.fromConfig()`, or with a worst-case bound.

## 2. Fabric config rewrite discards user content

`fabric/src/main/java/com/github/crittscott/somegoogly/config/fabric/TomlConfig.java:41` (A: 2376)

Found by: both

`completeIfMissing` rewrites the whole file from `render()` whenever a key is missing, and it counts a key whose value could not be parsed as missing. The rewrite replaces values that failed to parse with defaults and drops all user comments, unknown keys, and list entries the validator filtered out. This contradicts the javadoc's promise that the file keeps its values.

**Failure scenario:** A Fabric admin writes `globalPercent = 17%` or `globalPercent = "17"`, or an upgraded file lacks a new key such as `squintOnSleep`. The file is silently rewritten with the default `globalPercent`, and the only log lines are a parse warning and/or "Added missing keys". The admin's value, comments, and any unrecognized `ambientBehaviorPool` ids are lost permanently. The same happens to the client file through `FabricClientConfig`.

## 3. `angryEyes` now also disables red pupils

`common/src/main/java/com/github/crittscott/somegoogly/client/GooglyTracker.java:914`

Found by: A

`angryEyes` now also turns off red pupils for attacking mobs, so a value kept from a released config now means something different.

**Failure scenario:** An admin on the 1.21.1 release set `angryEyes = false` to skip the server's per-tick anger check, and attacking mobs still showed red pupils. After the upgrade the same file is kept, but `isAngry` now returns false whenever ANGER is disabled, so attacking zombies lose their red pupils without any change to the file. The commit says it keeps released config values, but this keeps the value while changing its effect.

## 4. Gametest resets the server config to defaults instead of restoring it

`fabric/src/gametest/java/com/github/crittscott/somegoogly/gametest/ConfigGameTests.java:180`

Found by: B

`releasedServerTomlKeepsItsValues` calls `ServerConfig.resetDefaults()` in its `finally` block. That replaces the gametest server's loaded config with defaults instead of restoring what the server had loaded.

**Failure scenario:** If the gametest world's `somegoogly-server.toml` sets non-default values, every later test in the run sees defaults. Scheduler and spawn tests that depend on the world's config then pass or fail depending on test order. The test should snapshot the loaded values and restore them, or reload the world's own file.

## 5. Client runs built-in defaults before the first sync

`common/src/main/java/com/github/crittscott/somegoogly/config/ClientEyeConfigs.java:511`

Found by: B

Before the first sync, and after disconnect, the client now holds `BehaviorSettings.DEFAULTS` instead of the old do-nothing `NONE`. Until the server's settings arrive, the client runs the built-in defaults rather than the server's.

**Failure scenario:** On a server with `ambientBehaviors=false` and `swirlOnHeal=false`, a player who joins holding a Googly Eye, or who sees placed heads before `EyeConfigSyncPacket` is processed, can see ambient winks or swirls the server disabled. Ongoing behaviors the server turned off, such as squints and anger, also show briefly. A settings object with everything off would avoid showing behavior the server never allowed.

## 6. Held eye draws from the full server ambient pool

`common/src/main/java/com/github/crittscott/somegoogly/client/HeldEye.java:1045`

Found by: A

The held eye now draws from the server's full ambient pool, so admins can add expressions the old fixed pool never allowed, and some of them may not render on a lone held eye.

**Failure scenario:** Setting `ambientBehaviorPool` to include `color_change`, `love` or `grow` makes the held eye play these as idle expressions. `color_change` is documented as admin-command-only, and if `HeldEye` doesn't apply iris tint, the countdown is spent on an invisible expression. The removed `AMBIENT_POOL` limited it to blink, side-eye and stare. Only cross-eye is filtered now.

## 7. `configSectionsMatchReleasedFiles` checks constants only, and its javadoc names an unsupported world

`common/src/gametest/java/com/github/crittscott/somegoogly/gametest/ConfigGameTestsLogic.java:67` (A: 358)

Found by: both (the unsupported 1.20.1 world in the reproduction steps); B only (the test checking constants only)

`configSectionsMatchReleasedFiles` only compares the section constants with string literals, so it never exercises the Forge/NeoForge file correction its javadoc describes. The javadoc's in-game steps also name a "0.8.2 for 1.20.1" world, but this version migrates only from the 1.21.1 release.

**Failure scenario:** If Forge's spec push or the quoting of a section name changed so an old file's values were dropped, this test would still pass. A tester following the javadoc would open a 1.20.1 world directly, which is unsupported (orientation-player says such worlds must first be opened with the 1.21.1 release). The reproduction should name the 1.21.1 release.

## 8. Ambient-wait formula lives in two places

`common/src/main/java/com/github/crittscott/somegoogly/eye/behavior/BehaviorSettings.java:891`

Found by: B (related to #9)

`nextAmbientTicks` and `meanTicks` copy the ambient-wait formula from `ServerBehaviorScheduler.nextCooldown` (`min + nextInt(max - min)`, or `min` when `max <= min`). The same rule now lives in two places.

**Failure scenario:** CLAUDE.md says "Each concept has one home. A behavior lives in exactly one place." If someone changes the server's draw, for example to make max inclusive, the held eye and placed heads silently stop matching mobs. `ServerBehaviorScheduler` should draw through `BehaviorSettings` (or a shared helper) instead of its own copy.

## 9. `nextAmbientTicks` upper bound is now exclusive

`common/src/main/java/com/github/crittscott/somegoogly/eye/behavior/BehaviorSettings.java:1553`

Found by: A (related to #8)

`nextAmbientTicks` never returns `maxTicks`, unlike the `HeldEye.nextAmbientDelay` it replaced, which included the upper bound.

**Failure scenario:** It matches the server scheduler, but the held eye's longest wait changed from 800 to 799. `heldEyePlaysIdleBehaviors` loops exactly `maxTicks` times, so the test now depends on that exclusive bound. A later change to an inclusive draw would make the test fail now and then.

## 10. `isAngry` bypasses the `level()` helper

`common/src/main/java/com/github/crittscott/somegoogly/client/GooglyTracker.java:566`

Found by: B (same code as #3)

`isAngry` repeats the switch check by hand (`settings.enabled(ANGER) && ... ANGER.level(parent)`) instead of using the new `level()` helper that every other ongoing behavior goes through.

**Failure scenario:** The gating logic is duplicated, and two code paths now apply a switch to a behavior. If the gating changes, for example to a per-entity override, ANGER could drift from the others. `level(OngoingBehavior.ANGER, settings) > 0F || angry && settings.enabled(ANGER)` would keep one path.

## 11. Misleading parameter name `ambient` in `SkullEyes`

`common/src/main/java/com/github/crittscott/somegoogly/client/SkullEyes.java:463` (A: 1115)

Found by: both

The parameter is still named `ambient` even though its type is now `BehaviorSettings`, a record with a boolean `ambient()` component, so a call would read `ambient.ambient()`.

**Failure scenario:** Anyone reading the code would easily mistake the whole settings object for the ambient switch. The name should be `behaviors`, as `ClientEyeRuntime` uses.

## 12. `startAmbient` copies the pool on every fire

`common/src/main/java/com/github/crittscott/somegoogly/client/HeldEye.java:384` (A: 1045)

Found by: both

Every time the ambient timer fires, `startAmbient` makes a new `ArrayList` copy of the pool just to take `CROSS_EYE` out, although the filtered pool changes only when settings sync.

**Failure scenario:** This is a small amount of throwaway allocation on the client tick. Filtering cross-eye once per settings object, or picking a random index and re-rolling or skipping when it lands on `CROSS_EYE`, would avoid the copy.

## 13. Duplicated `BehaviorSettings` copy helpers in tests

`common/src/gametest/java/com/github/crittscott/somegoogly/gametest/BehaviorSchedulerGameTestsLogic.java:336`

Found by: A

Two test files each build an 8-argument `BehaviorSettings` by copying every field from DEFAULTS (`without` here and `settings` in HeldEyeGameTestsLogic).

**Failure scenario:** Adding a field to `BehaviorSettings` means fixing every hand-written copy (two helpers plus the SerializationGameTestsLogic literal). Adding `with`-style copy methods on the record, or one shared test helper, would leave a single place to update.


## Coverage by the topic reviews

Compared against all 16 completed topic reviews after removing duplicate findings across their report files. “Caught” means a retained finding identifies the same problem and corrective action; “Partly caught” means only part of the finding, or an open question rather than a finding, appears there. “Not caught” describes coverage only: it does not establish that the original finding is valid or that its historical behavior should be restored. The topic reviews assess the current code without inspecting history.

| Original finding | Coverage | Retained report and explanation |
| --- | --- | --- |
| 1. `wireSize()` underestimates the sync packet | Not caught | No topic report identifies measuring default behavior settings instead of the actual or worst-case settings. |
| 2. Fabric config rewrite discards user content | Partly caught | [Expected-way, finding 1](review-Expected-way.md#1-fabric-interprets-toml-with-a-partial-handwritten-parser) catches rejected valid TOML values becoming defaults and being overwritten, on client and server, and calls for preserving the file on parse failure. It does not separately identify loss of user comments, unknown keys, or filtered entries when completing an otherwise readable file. |
| 3. `angryEyes` also disables red pupils | Not caught | No report identifies this change in the meaning of a released setting. The topic reviews do not compare historical behavior. |
| 4. GameTest resets the server config instead of restoring it | Not caught | Test-completeness identifies a different fixture problem, not the configuration cleanup in `releasedServerTomlKeepsItsValues`. |
| 5. Client runs built-in defaults before first sync | Not caught | No report identifies behavior enabled before the first server settings arrive. |
| 6. Held eye draws from the full server ambient pool | Not caught | No report identifies unwanted or invisible held-eye expressions selected from that pool. |
| 7. Config-section test checks constants only and names an unsupported world | Partly caught | [Legacy, open question 1](review-Legacy.md#open-questions) asks whether the reproduction should name the supported 1.21.1 upgrade rather than 1.20.1. No retained finding identifies the constants-only test as failing to exercise loader file correction, and the reproduction issue remains an owner question. |
| 8. Ambient-wait formula lives in two places | Caught | [Dead-code, finding 1](review-Dead-code.md#1-ambient-expression-timing-has-two-implementations) identifies the duplicated random-wait calculation in `BehaviorSettings` and `ServerBehaviorScheduler` and prescribes one shared implementation. |
| 9. `nextAmbientTicks` upper bound is exclusive | Not caught | Dead-code finding 1 explicitly preserves the current exclusive bound; it does not flag the historical bound change or the held-eye test's dependence on it. |
| 10. `isAngry` bypasses the `level()` helper | Not caught | No report identifies the duplicated ongoing-behavior switch check in `isAngry`. |
| 11. Misleading `ambient` parameter in `SkullEyes` | Not caught | No report flags this parameter name. |
| 12. `startAmbient` copies the pool on every fire | Not caught | Efficiency addresses server work while disabled, not the held-eye pool allocation. |
| 13. Duplicated `BehaviorSettings` copy helpers in tests | Not caught | No report identifies the repeated test-record reconstruction helpers. |

Coverage totals: 1 caught, 2 partly caught, and 10 not caught. Duplicate removal preserves the findings cited here once each; it does not turn a previously identified issue into a coverage gap.
