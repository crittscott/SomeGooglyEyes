# Code Review: Legacy Support and Unnecessary Layering

**Scope:** `src/main/java/com/github/crittscott/somegoogly/**` (the mod's own source; `othersourcecode/` is
a vendored external library and out of scope). Focus: backward-compatibility code for old save/NBT/config
formats, deprecated members kept alongside replacements, compatibility shims, needless pass-through
methods, and save/config fields that exist only to support old data.

## Summary

**No findings.** This codebase does not carry any legacy-support code, deprecated members, or needless
pass-through layering. Every method and class I inspected either does real work, implements a genuine
interface contract, or is a small but meaningful adapter (e.g. bridging an event-bus callback signature,
or guarding a soft dependency). There is not a single `@Deprecated` annotation anywhere in the mod's
source, and no NBT/config reader has an "if old shape, do X, else do Y" branch.

I read essentially every non-generated file under `src/main/java`: `config/*`, `network/*`, `item/*`,
`event/*`, `recipe/*`, `command/*`, `eye/*` and `eye/behavior/*`, `eye/state/*`, `picker/*`,
`client/render/resolver/*`, `client/compat/*`, plus the mod entry point (`SomeGoogly.java`) and the
serialization-focused game tests. Below are the areas that looked like plausible candidates at first
glance, and why each was excluded on closer reading.

## Things considered and excluded

### 1. `VersionRangeMatcher` / `ModVersionLookup` / the `version` field in `HeadInfo.VersionedEntry`

Files: `config/VersionRangeMatcher.java`, `config/ModVersionLookup.java`, `eye/HeadInfo.java` (lines
240–270), `config/EyeConfigReloadListener.java`.

This machinery — bracket version ranges (`[1.2.0,1.3.0)`), "nearest generation" fallback, and per-entry
`version` strings in the datapack JSON — looks like classic backward-compatibility code at a glance. It is
not: it selects, among the *currently declared* datapack entries, the one written for the *currently
installed version of the entity-providing mod* (e.g. a companion mod whose model changed between its own
releases). It is not about SomeGooglyEyes reading data saved by an older SomeGooglyEyes. There is no
"if old SomeGooglyEyes format, migrate" branch anywhere — a datapack file has exactly one schema
(`ConfigFile.CODEC`), and every field is required (the class comment in `HeadInfo` is explicit that this is
deliberate: "nothing reads field absent as different from field equals default"). This is a live feature
for authoring eye placements against mods that themselves change, not legacy-data support. Excluded.

### 2. `NetworkHandler.PROTOCOL_VERSION`

File: `network/NetworkHandler.java`, line 19.

A single current protocol-version string (`"6"`) used as a client/server compatibility gate (refuse to
connect on mismatch). There is only one version in play — no branch reads "if protocol version was X, parse
packets the old way." Bumping this number is documented as the mechanism for breaking wire changes, which
is the correct Forge pattern, not a compatibility shim. Excluded.

### 3. `GeckoCompat` / `GeckoIntegration` split

Files: `client/compat/GeckoCompat.java`, `client/compat/GeckoIntegration.java`.

`GeckoCompat.enumerate`/`tryAddLayer` each wrap the corresponding `GeckoIntegration` call in a `LOADED`
check and a `try/catch`. This reads like a pass-through at first glance, but each wrapper adds real
behavior: it avoids classloading GeckoLib types when the mod isn't present (the whole reason
`GeckoIntegration` is a separate class) and degrades a GeckoLib API mismatch to "no support" instead of a
crash. This is the standard Forge soft-dependency pattern, not needless layering. Excluded.

### 4. `EyeReactionHandler` as a "thin adapter"

File: `event/EyeReactionHandler.java`.

The class's own Javadoc calls itself "a thin adapter" that hands events to `ServerBehaviorScheduler`. Each
of its three listener methods is short, but each also does something the scheduler couldn't do itself: it
is the Forge `@SubscribeEvent` entry point (a required contract, not optional layering) and it applies a
per-event guard (`isClientSide()`, or `source.getEntity() instanceof Player`) before delegating. Not a pure
pass-through — excluded.

### 5. `SomeGoogly.addLayers(EntityRenderersEvent.AddLayers)`

File: `SomeGoogly.java`, lines 99–103.

```java
private void addLayers(EntityRenderersEvent.AddLayers event) {
    if (clientEventHandler != null) {
        clientEventHandler.addLayers();
    }
}
```

This looks like a one-line forward to `ClientEventHandler.addLayers()`. It exists because Forge's mod-event
bus listener must match the event type by parameter, while the actual renderer-layer logic lives on an
instance field (`clientEventHandler`) constructed earlier and reused elsewhere (`peekGooglyTracker`,
`clearTrackers`, etc.). The null check also guards against the listener firing before the client branch of
the constructor has run. This is glue code required by the event-bus contract, not speculative layering.
Excluded.

### 6. Parallel client/server config and store classes

Files: `config/ClientConfig.java` vs `config/ServerConfig.java`; `config/ClientEyeConfigs.java` vs
`config/ServerEyeConfigs.java`.

These look superficially duplicated, but their own Javadocs explain the real reason: in single-player, the
integrated server and the client run in the same JVM, so a shared static store would let server-authoritative
state leak into (or be overwritten by) client state. Keeping them separate is a deliberate, currently-used
design decision, not two generations of the same mechanism. Excluded.

## What I did not find

- No `@Deprecated` annotations, anywhere in `src/main/java`.
- No NBT/config reader with an "old key name" / "old shape" fallback branch.
- No migration or upgrade routines for stored data (entity persistent data, item stack NBT, or
  `ForgeConfigSpec` values).
- No compatibility shim re-exporting an old type/method name alongside a new one.
- No pass-through method whose entire body is "call another method with the same arguments and return the
  result" for no discernible reason. The short one-line methods that do exist (e.g.
  `EyeState.hasEyes`, `ServerEyeConfigs.get(entity, LivingEntity)`, `HeadInfo.getEyeScale`) each either read
  a field/NBT value, forward to a differently-typed overload for caller convenience, or implement one leg of
  an interface contract (`EyeAttachmentResolver`, `EyeBehavior`, `EyeModifier`) — none are unexplained
  aliases.
- No config or save-data field whose purpose is "read but never written going forward" or vice versa.

## Conclusion

Given the project's explicit "no legacy support" policy, there is nothing to remove here: the codebase does
not currently carry any weight from past versions of itself. This is a clean bill on this specific axis; no
changes are recommended.
