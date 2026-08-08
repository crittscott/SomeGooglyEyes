# Code Review: Dead Code and Duplication

Scope: `src/main/java/com/github/crittscott/somegoogly/**` (~100 files, ~11,800 lines) at the current `main` HEAD (`3dd8c78`). Review was read-only: no build/run, no decompilation, no git actions.

Methodology: cross-referenced every class name across the tree (grep census), scripted checks for unused imports and low-reference private methods/fields, a normalized-body scan for duplicated method bodies across files, and manual reading of the packages most likely to accumulate cruft (event handlers, render resolvers, GeckoLib/Citadel/LLibrary compat, the picker CLI, behaviors, config stores).

## Summary

This codebase is unusually clean for something described as having "grown organically." There are no commented-out code blocks, no `TODO`/`FIXME`/`XXX` markers, no unused imports anywhere in the tree, and no obviously superseded/orphaned code paths. Only one genuinely dead symbol was found. The rest of the findings below are minor, small-scale duplication — a few identical or near-identical short methods across two or three files — most of which is either already justified in the surrounding comments or too small to be worth extracting given the project's stated bias against speculative abstraction.

## Confirmed dead code

### `ClientEventHandler.MARK` — unused Log4j marker field

**File:** `src/main/java/com/github/crittscott/somegoogly/event/ClientEventHandler.java:37`

```java
private static final Marker MARK = MarkerManager.getMarker(ClientEventHandler.class.getSimpleName());
```

No logger exists anywhere in this class (grepped for `LOGGER`/`log.`/`logger` in the file — no matches), and `MARK` itself is referenced nowhere else in the whole source tree (grepped `\bMARK\b` across `src/main/java` — the declaration is the only hit). This looks like a leftover from a version of the class that logged with this marker; the logging was removed at some point but the field was not. Safe to delete along with its now-unused imports (`org.apache.logging.log4j.Marker`, `org.apache.logging.log4j.MarkerManager`).

## Minor / low-confidence duplication

These are small (3–6 line) identical or near-identical method bodies. None of them are large enough to be clear-cut "should be a shared method" cases under the project's simplicity-first guidance, but they're exactly the copy-paste pattern the review was looking for, so I'm listing them for you to judge.

### `create()` factory + `appendHoverText()` — `GooglyEyeItem` vs `SlimyEyeItem`

**Files:**
- `src/main/java/com/github/crittscott/somegoogly/item/GooglyEyeItem.java:34-43`
- `src/main/java/com/github/crittscott/somegoogly/item/SlimyEyeItem.java:46-55`

Both classes have an identical-shape static factory:

```java
public static ItemStack create(AppearanceOverride properties, int count) {
    ItemStack stack = new ItemStack(ModItems.XXX_EYE.get(), count);
    EyeItemProperties.set(stack, properties);
    return stack;
}
```

(differing only in which `ModItems` constant is used), and an identical `appendHoverText` override body (`EyeItemProperties.appendTooltip(stack, tooltip);`). This is plausibly just "two items with the same NBT-appearance shape" rather than copy-paste drift, and at 2 call sites / 4 lines it's below the threshold where extraction clearly pays for itself — flagging for your judgment rather than asserting it should change.

### `handles()` — `CitadelResolver` vs `LLibraryResolver`

**Files:**
- `src/main/java/com/github/crittscott/somegoogly/client/render/resolver/CitadelResolver.java:64-67`
- `src/main/java/com/github/crittscott/somegoogly/client/render/resolver/LLibraryResolver.java:67-70`

Both implement `handles()` identically:
```java
public boolean handles(EntityModel<?> model) {
    return HANDLES.available() && HANDLES.modelClass().isInstance(model);
}
```
Both classes already share their real common machinery via the `ReflectedBoxResolver` abstract base (token matching, path building, box indexing/caching — see `ReflectedBoxResolver.java`), so this is the one leftover spot where the two families' `handles()` bodies happen to coincide. Could be pulled into `ReflectedBoxResolver` as a `final` method using the existing `available()`/`boxClass()`... except `boxClass()` isn't the *model* class, so it would need a new `protected abstract Class<?> modelClass()` hook. Minor enough that I'd call this optional.

### `get(ResourceLocation, boolean)` — `ClientEyeConfigs` vs `ServerEyeConfigs`

**Files:**
- `src/main/java/com/github/crittscott/somegoogly/config/ClientEyeConfigs.java:36-39`
- `src/main/java/com/github/crittscott/somegoogly/config/ServerEyeConfigs.java:51-54`

```java
public static RuntimeConfig get(ResourceLocation entity, boolean baby) {
    RuntimeConfigSet set = configs.get(entity);
    return set == null ? null : set.get(baby);
}
```
Identical body. However, this one is **explicitly and deliberately duplicated** — both classes' javadoc says the two stores are kept as separate static maps specifically to avoid single-player state bleed between the integrated server and the client (`ClientEyeConfigs`: "Separate from ServerEyeConfigs to avoid single-player static-state bleed"). Merging the lookup would mean introducing a shared non-static helper or a common base, which the existing design intentionally avoids. I'd leave this one alone — noting it only because it technically matches "duplicated logic," not because I think it should change.

## Patterns that look like duplication but are not (checked and ruled out)

To save you from re-checking these yourself:

- **`ModelPartTreeResolver` / `ReflectedBoxResolver` / `GeoBones`** (`client/render/resolver/` and `client/compat/GeoBones.java`) each implement their own depth-first, suffix-matching tree search (`search()`/`find()`/`pathOf()`). This *looks* like the same algorithm implemented three times, and structurally it is — but the three walk fundamentally different node types (vanilla `ModelPart`, a reflected `Object` box, and GeckoLib's `GeoBone`) with no common Java supertype, and the resolver hierarchy already factors out everything that *can* be shared within a family (`ModelPartTreeResolver` is the shared base for the three vanilla-tree resolvers; `ReflectedBoxResolver` is the shared base for the two reflection-based ones). `GeoBones` is the odd one out because GeckoLib bones can't share either base. This is a plausible target for a generic "named tree" interface, but it's a nontrivial abstraction, not a copy-paste bug — flagging for awareness, not urging action.
- **`GooglyGeoLayer` / `LayerGooglyEyes` / `PickerLayer`** all have an identical two-line constructor (`super(renderer); this.modelGooglyEye = new ModelGooglyEye();`) — trivial, not worth extracting.
- **`SideEyeBehavior` / `CrossEyeBehavior`** (`eye/behavior/`) share the same constant names (`AMOUNT`, `CENTER_FRAC`, `HOLD_FRAC`, `STIFFNESS`) and both use `Curves.slide`/`Curves.trapezoid`, but the actual per-eye math differs (single fixed direction vs. per-partner-eye projection), and both already delegate their shared curve math to `Curves`. Not flagging as duplication.
- **`EligibilityGameTests` / `SpawnGatingGameTests`** (`gametest/`) share a "force a config value, run the assertion, restore in `finally`" shape. This is standard GameTest setup/teardown boilerplate, and the class javadoc in `EligibilityGameTests` explicitly calls out that it reuses "the same force-and-restore pattern as `SpawnGatingGameTests`" — an intentional, acknowledged pattern, not organic drift.
- **The `EyeItemInteractions` apply/harvest verbs**, described in the class javadoc as deliberately *not* implemented via `Item#interactLivingEntity`, are consistent with the actual code — no leftover `interactLivingEntity` override was found on `SlimyEyeItem` or `GooglyEyeItem`. The commit history (`bfef94d "Rename 'slimey' to 'slimy'; apply eyes via the entity-interact event"`) suggests this was a recent refactor; I specifically checked for an orphaned old interaction path and found none.

## Not investigated further (out of scope / needs runtime info)

- **GameTest classes** (`gametest/*.java`, 11 files) each show up in only their own file when grepped by class name. This is expected, not dead code: Forge discovers them via the `@GameTestHolder(SomeGoogly.MOD_ID)` annotation (confirmed present on all 11), not by direct reference — the standard Minecraft/Forge GameTest registration mechanism.
- **Network packet classes** (`network/*.java`, 8 files) share the expected Forge `SimpleChannel` encode/decode/handle boilerplate shape. This is "the Forge way" of doing packets, not copy-paste drift worth flagging.
- I did not attempt to determine whether every `EyeBehavior`/`EyeModifier`/resolver implementation is reachable from actual gameplay data (e.g., whether some datapack-only path never gets exercised) — that would require running the game or the datapacks, which is out of scope here.

## What I checked and found nothing

- **Unused imports:** none, anywhere in the tree (scripted check).
- **Unused private methods/fields:** scripted a whole-corpus reference count for every `private` method/field declaration; only `ClientEventHandler.MARK` (above) came back as truly unreferenced. Everything else that looked suspiciously low-count turned out to have exactly one legitimate call site (normal for small single-purpose helpers) — e.g., `PickerState.clearDraft()` (used 8× internally, just never from outside the class), `EyeModifier`/`EyeBehaviors` registries (all entries referenced from their `List.of(...)` registration), all `GooglyClientCommands` `prop*`/`list*`/`variant*` handlers (registered once each from `register()`).
- **Commented-out code:** none found (checked via pattern search for comment lines containing code-shaped syntax — assignments, semicolons, control-flow keywords).
- **`TODO`/`FIXME`/`XXX`/`@Deprecated`:** none found anywhere in `src/main/java`.
- **Resolver registry completeness:** `Resolvers.ALL` (`client/render/resolver/Resolvers.java:23-29`) lists 5 concrete resolvers; the two abstract bases (`ModelPartTreeResolver`, `ReflectedBoxResolver`) are correctly *not* in that list — they're superclasses, not resolvers themselves, and all their subclasses are present in `ALL`.
- **Behavior registry completeness:** `EyeBehaviors.ALL` (`eye/behavior/EyeBehaviors.java:23-31`) lists all 7 concrete behavior classes found in the package; none are orphaned.

## Recommendation

Only one change is clearly warranted: delete the unused `MARK` field (and its two now-unused imports) in `ClientEventHandler.java`. Everything else above is either intentional (and documented as such) or small enough that extracting it would trade a few duplicated lines for a new abstraction — which cuts against this project's stated simplicity-first bias. I'd leave the rest as-is unless you specifically want the `CitadelResolver`/`LLibraryResolver` `handles()` duplication cleaned up, in which case it's a small, low-risk change (add a `modelClass()` hook to `ReflectedBoxResolver`).
