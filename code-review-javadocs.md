# Javadoc Review — SomeGooglyEyes

Scope: all Java source under `src/main/java` (106 files). Excludes `othersourcecode/` (third-party reference sources) and build output.

## Overall assessment

The codebase's javadoc is unusually good and unusually consistent. A full sweep of every file's class-level doc comment (and close reading of ~35 representative files spanning every package — eye state/behavior, rendering, resolvers, network, picker, config, item, recipe, event, enchant, gametest) turned up almost no violations of the project's stated philosophy. In particular:

- No history references were found (no "used to", "previously", "originally", "no longer", "legacy", "refactor", version-number call-outs, etc., anywhere in a doc comment).
- No speculative/future-hypothetical documentation or TODO-as-doc was found.
- No stale/inaccurate javadoc was found relative to current code behavior.
- Class-level docs consistently describe *contract and purpose* ("what this is for, and how it relates to its neighbors") rather than internals, even for genuinely complex machinery (the reflection-based `ReflectedBoxResolver`, the server-owned `PickerFreezeService` crash-recovery layering, `ServerBehaviorScheduler`'s tracking-based scheduling).

The findings below are the exceptions to that pattern — real but narrow: a few files with no javadoc at all, a few asymmetries where a "sibling" class/method has documentation and its counterpart doesn't, one genuinely malformed `@param` set, and two borderline cases of algorithm-detail leaking into an otherwise contract-focused doc comment.

---

## Missing Javadoc

### Files with no javadoc anywhere

1. **`src/main/java/com/github/crittscott/somegoogly/event/ServerEventHandler.java`** — no class-level javadoc. This class bundles eight unrelated `@SubscribeEvent` handlers (datapack reload registration, config sync, spawn-decision rolling, command registration, tracking start/stop sync, server-stop cleanup). Each method has excellent inline commentary, but there is nothing at the class level orienting a reader to what the class as a whole is responsible for or why these particular events are grouped together. Worth a short class summary given the grab-bag nature.

2. **`src/main/java/com/github/crittscott/somegoogly/event/ClientEventHandler.java`** — same gap, client side: no class-level javadoc, and it bundles renderer-layer injection (`addLayers`), the per-tick tracker sweep (`onWorldTick`), disconnect cleanup (`onLoggingOut`), and tracker lookup (`getGooglyTracker`/`peekGooglyTracker`). Two of its public methods are also undocumented despite non-obvious behavior:
   - `addLayers()` (lines 45–83): iterates every registered entity renderer and GeckoLib-compat-attaches or vanilla-attaches the eye/picker layers; ~30 lines with no top-level summary of what it does or when it's expected to run (it's called both at client setup and on every resource reload — that's stated only in an inline comment, not in a doc comment a caller would see from the call site in `SomeGoogly`).
   - `getGooglyTracker(LivingEntity, HeadInfo)` (lines 120–127): silently replaces the cached tracker when `!tracker.matches(helper)`, i.e. it has cache-invalidation semantics a caller should know about, undocumented.

3. **`src/main/java/com/github/crittscott/somegoogly/client/render/LayerGooglyEyes.java`** — no class-level javadoc. This is notable because its GeckoLib counterpart, `GooglyGeoLayer`, *does* have a thorough class doc that explicitly describes itself as "the GeckoLib counterpart of `LayerGooglyEyes` + `PickerLayer`." The relationship is documented from one side only; a reader who opens `LayerGooglyEyes` first has no pointer to its Geckolib sibling or to `GooglyEyeRenderer`/`EyeRenderGating`, both of which it depends on and both of which are documented as being shared between the two layers.

4. **`src/main/java/com/github/crittscott/somegoogly/SomeGoogly.java`** — no javadoc, but low priority: this is the `@Mod` entry point, its structure is standard Forge boilerplate, and its constructor body is already well commented inline line-by-line.

5. **`src/main/java/com/github/crittscott/somegoogly/network/NetworkHandler.java`** — no javadoc, but low priority: standard `SimpleChannel` registration boilerplate, already well commented inline (including a useful "version contract" note on `PROTOCOL_VERSION`).

6. **`src/main/java/com/github/crittscott/somegoogly/gametest/SomeGooglyGameTests.java`** — no javadoc, but low priority: a `GameTest` class whose four test methods are self-explanatory from their names and bodies, consistent with the instruction not to demand javadoc on obviously self-explanatory code. (Nearly every other gametest class in the package *does* carry a class-level javadoc summarizing what it covers — see e.g. `ConfigGameTests`, `EligibilityGameTests`, `BehaviorDeterminismGameTests` — so this is a minor outlier in an otherwise consistent sub-package, but not a real comprehension cost.)

### Asymmetric coverage — one side of a pair is documented, the other isn't

7. **`src/main/java/com/github/crittscott/somegoogly/config/ClientConfig.java`** — no class-level javadoc at all, while its server-side counterpart `ServerConfig.java` has a substantial one explaining the split of responsibility between the two config classes and the datapack `enabled` flag. A reader landing on `ClientConfig` gets no equivalent orientation (what it's for, why `disabledEntities`/`disabledMods` exist as two separate lists, why the caches are invalidated on config reload — that last point is only in an inline comment).

8. **`src/main/java/com/github/crittscott/somegoogly/client/picker/PickerState.java`** — several setters lack javadoc while structurally identical sibling setters have it:
   - `setEyeScale(float)` (line 436), `setCorneaColor(float,float,float)` (line 432), `setGlow(boolean)` (line 471), `setIrisColor(float,float,float)` (line 475), and `setIrisScale(float)` (line 479) have no javadoc.
   - By contrast `setDepth(float)` (line 441) — doing the identical `Math.max(0, v)` clamp — is documented as "Thickness multiplier along the look axis (1 = standard; clamped >= 0)", and `setVariantWeight`, `setPosition`, `setRotation`, `setCrossTarget`, `setPartByName`, `setPartByNumber` all carry a one-line javadoc.
   - Since `setEyeScale` and `setIrisScale` clamp to `>= 0` just like the documented `setDepth`, the clamping behavior is silently undocumented for two of the three float scale setters — a real (if minor) gap, not just a style nit.
   - `cyclePart(int dir)` (line 198) is also undocumented; the sign convention of `dir` (which direction cycles which way through `parts`) isn't otherwise obvious from the call site.

9. **`src/main/java/com/github/crittscott/somegoogly/eye/state/AppearanceOverride.java`** — `withCorneaColor` (line 57) documents "`{@code null}` clears the field", but the two structurally identical sibling methods, `withGlow` (line 61) and `withIrisColor` (line 65), do not, despite having exactly the same `Optional.ofNullable(...)` null-clears-the-override semantics.

---

## Problematic Javadoc Content

### Malformed Javadoc

**`src/main/java/com/github/crittscott/somegoogly/client/GooglyTracker.java`, lines 138–144** (javadoc on `EyeInfo.update`, method signature at lines 145–146):

```java
 * @param rand      randomness source (per-tracker / per-held-eye)
 * @param headYaw   the holder's head yaw this tick ({@code getYHeadRot})
 * @param headPitch the holder's pitch this tick ({@code getXRot})
 * @param motionX/Y/Z the holder's position delta this tick
 * @param anchorX/Y the behavior spring's target in the unit disk (ignored when stiffness is 0)
 * @param stiffness the behavior spring's stiffness (0 = no behavior force on the pupil)
 */
public void update(Random rand, float headYaw, float headPitch, double motionX, double motionY, double motionZ,
                   float anchorX, float anchorY, float stiffness) {
```

The method has nine separate parameters (`rand, headYaw, headPitch, motionX, motionY, motionZ, anchorX, anchorY, stiffness`), but the javadoc collapses three of them into `@param motionX/Y/Z` and two into `@param anchorX/Y`. Neither tag name matches an actual parameter (`motionX/Y/Z` and `anchorX/Y` are not valid identifiers), so a javadoc linter/generator would flag both as unmatched tags and would report `motionX`, `motionY`, `motionZ`, `anchorX`, and `anchorY` as having no `@param` documentation at all. This is the one clear-cut malformed-tag instance found in the codebase; every other `@param`/`@return` usage found (in `SpawnAllCommand`, `Resolvers`, `ModelMemo`, `EyeAttachmentResolver`, `Attachment`) is correctly formed, one tag per parameter.

### Algorithm internals vs. contract (borderline)

Two cases came close to describing "how" instead of "what," though both are defensible given the code is inherently mathematical/physical — for a physics or pure-geometry method, the exact algorithm often *is* the observable contract. Flagging them as borderline rather than clear violations:

- **`GooglyTracker.java`, lines 118–122** (javadoc on `EyeInfo.update`): opens with "Semi-implicit Euler: accumulate forces -> integrate velocity -> integrate position -> resolve the circular wall." This is a description of internal integration steps rather than caller-facing behavior. The rest of the same doc comment (forcing terms, spring behavior, damping) is written in contract terms (what the pupil visibly does), so this is a partial lapse within an otherwise well-written comment rather than a systemic issue.

- **`src/main/java/com/github/crittscott/somegoogly/client/render/GooglyEyeRenderer.java`, lines 44–56** (javadoc on `captureGravity`): walks through the exact matrix composition used to map world-down into the pupil plane ("`localToWorld = (view → world) · (local → view)`", inverting it, etc.). This is implementation math rather than a statement of the method's effect, but since the method's entire job *is* that specific computation, documenting the derivation arguably serves future maintainers who need to modify it correctly (e.g. if the camera/pose convention ever changes). Included here for completeness rather than as a strong recommendation to rewrite.

### Stale/inaccurate javadoc

None found.

### Speculative/future/TODO-as-documentation

None found.

### History references

None found. A codebase-wide search for common history-reference language ("used to", "previously", "originally", "no longer", "legacy", "deprecated", "refactor", "bug fix", "TODO", "FIXME", "prior to", etc.) inside `src/` turned up only two incidental, non-offending hits, neither of which is a javadoc comment or a reference to code history:
- `HeadInfo.java:411` — `/** The selected variant's head list (identity used to invalidate per-mob trackers). */` — "used to" here means "used for the purpose of," not "formerly did"; it's a correct, present-tense description of the field's role.
- `PickerState.java:315` — `unfreeze(); // release a previously frozen mob, if any` — a plain inline comment (not javadoc) describing runtime state ("a mob that was frozen earlier in this session"), not code history.

---

## Summary

| Category | Count | Severity |
|---|---|---|
| Files with zero javadoc, real cost | `ServerEventHandler`, `ClientEventHandler` (+2 undocumented public methods), `LayerGooglyEyes` | Moderate |
| Files with zero javadoc, low cost (boilerplate/self-explanatory) | `SomeGoogly`, `NetworkHandler`, `SomeGooglyGameTests` | Low |
| Asymmetric sibling documentation | `ClientConfig` vs `ServerConfig`; `PickerState` setters; `AppearanceOverride.withGlow`/`withIrisColor` | Low–Moderate |
| Malformed `@param` tags | `GooglyTracker.EyeInfo#update` | Moderate (would fail javadoc lint) |
| Algorithm-detail-in-contract-doc (borderline) | `GooglyTracker.EyeInfo#update`, `GooglyEyeRenderer#captureGravity` | Low, defensible |
| Stale/inaccurate javadoc | 0 | — |
| Speculative/history-referencing javadoc | 0 | — |

The codebase is, on the whole, an example of the project's documentation philosophy being followed well; the fixes above are narrow and localized rather than indicative of a systemic problem.
