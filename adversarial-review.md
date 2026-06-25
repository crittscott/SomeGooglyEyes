# Adversarial Code Review

Static adversarial review; no files were changed during the review and no build was run.

## Findings

### High — a server can freeze a client through an unchecked behavior packet

`duration` and `elapsed` are read unchecked, then the client replays `elapsed` ticks in a tight loop. A malicious or compromised server can send huge values and lock the render thread.

Relevant code:

- `src/main/java/com/github/crittscott/somegoogly/network/EyeBehaviorTriggerPacket.java:52`
- `src/main/java/com/github/crittscott/somegoogly/client/GooglyTracker.java:253`

Declare this packet `PLAY_TO_CLIENT` and reject out-of-range values before constructing or replaying it.

### High — mobs given eyes while already watched never enter behavior scheduling

`StartTracking` only registers mobs that already have eyes. The potion later calls `setHasEyes(true)` but does not notify the scheduler, so ambient, hurt, heal, and trade reactions silently never occur until the mob is untracked and tracked again.

Relevant code:

- `src/main/java/com/github/crittscott/somegoogly/event/ServerEventHandler.java:75`
- `src/main/java/com/github/crittscott/somegoogly/eye/behavior/ServerBehaviorScheduler.java:60`
- `src/main/java/com/github/crittscott/somegoogly/event/EyePotionInteractions.java:70`

Track watched living entities regardless of current eye state, then gate actual behavior execution on `hasEyes`.

### Medium — config-sync input has no aggregate bound

Each entry allows up to 1 MiB of JSON, but the sender-controlled entry count is uncapped and every entry is parsed. This is a client DoS surface against an untrusted server, and the no-direction packet registrations leave the protocol boundary unnecessarily loose.

Relevant code:

- `src/main/java/com/github/crittscott/somegoogly/network/NetworkHandler.java:21`
- `src/main/java/com/github/crittscott/somegoogly/network/EyeConfigSyncPacket.java:51`

Use client-bound message directions plus sensible caps for entry count, total bytes, variants, heads, and eyes.

### Medium — picker can corrupt an already-frozen mob's `NoAI` state

`freeze()` records `frozenPrevNoAi` asynchronously on the server thread, while `unfreeze()` snapshots that field immediately on the client thread. Choose and immediately unchoose a mob that already has `NoAI=true`; the queued unfreeze can restore the stale default `false`.

Relevant code:

- `src/main/java/com/github/crittscott/somegoogly/client/picker/PickerState.java:147`
- `src/main/java/com/github/crittscott/somegoogly/client/picker/PickerState.java:190`

Put capture, freeze, and restore behind one server-thread state object or token.

### Medium — malformed-but-decodable datapacks can mint invisible “eyes”

A variant is considered usable if it has a head, even if every head has zero eyes. Such mobs can roll as eyed and be harvested; harvesting falls back to the default appearance and forces a minimum drop count of one.

Relevant code:

- `src/main/java/com/github/crittscott/somegoogly/config/EyeConfigReloadListener.java:101`
- `src/main/java/com/github/crittscott/somegoogly/config/ServerEyeConfigs.java:62`
- `src/main/java/com/github/crittscott/somegoogly/event/EyeItemInteractions.java:135`

Require at least one actual eye before accepting a variant or config.
