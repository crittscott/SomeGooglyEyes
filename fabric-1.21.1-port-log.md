# Fabric 1.21.1 Port Log

This is the append-only audit history for `fabric-1.21.1-port-plan.md`. During normal execution,
write reduced verification results here but do not read the file back. Raw compiler and GameTest
output does not belong here.

## 2026-08-29

- **Gradle/IDE sync:** IntelliJ **Reload All Gradle Projects** completed successfully in 1 minute
  36 seconds; 18 actionable tasks (17 executed, 1 up-to-date), zero configuration failures.
  `common`, `fabric`, `forge`, and `neoforge` all completed their sync work. Nonfatal cache-lock,
  duplicate Fabric class, remapping signature, and Gradle deprecation warnings were reported.
- **Stage 0 initial launch:** `.\gradlew.bat :common:compileJava --console=plain` exited 1 before
  Gradle started because the sandbox could not access the existing wrapper distribution lock. The
  identical authorized retry was used; this infrastructure launch did not count as a post-edit
  attempt.
- **Stage 0 compiler baseline:** The authorized retry reached `:common:compileJava` and exited 1
  after 11 seconds with 87 errors and 10 warnings; one actionable task executed. Errors classify as
  Stage 1: 36, Stage 2: 8, Stage 3: 0, and Stage 4: 43. All warnings are deprecated networking
  receiver registrations assigned to Stage 2.
- **Stage 0 complete:** Every common production failure was classified, no production source was
  edited, and the first bounded Stage 1 work unit is non-rendering `ResourceLocation` migration.

## 2026-08-29 — Stage 1 complete

- Ported common identifiers/tooltips/dye access, eye-item appearance storage, crafting contracts,
  and Optometrist access to the 1.21.1 APIs.
- Replaced raw eye-item NBT with the registered `somegoogly:eye_properties` data component and
  replaced the constructed Optometrist class with a data-driven resource key plus active holders.
- Stage diagnostic: `.\gradlew.bat :common:compileJava --console=plain` reached the compiler and
  failed as expected in 9 seconds with 51 errors and 10 warnings.
- Classification: Stage 1: 0 errors; Stage 2: 8 errors and 10 warnings; Stage 4: 43 errors. The
  generated problems report confirmed all 51 errors; no Stage 1 correction attempt was needed.
- Next bounded unit: Stage 2A.1, the two `EyeItemService` durability callbacks.

## 2026-08-29 — Stage 2 verification

- `.\gradlew.bat :common:compileJava --console=plain` exited 1 before Gradle because the sandbox
  could not access the existing wrapper distribution lock. This was failed post-edit attempt 1.
- The identical authorized retry reached `:common:compileJava`, exited 1 in 9 seconds, and reported
  44 errors with no warnings: one bounded-NBT return-type error in Stage 2 and the unchanged 43
  rendering errors assigned to Stage 4. This was failed post-edit attempt 2.
- After explicitly accepting only a `CompoundTag`, the same command reached `:common:compileJava`,
  exited 1 in 9 seconds, and reported exactly the 43 Stage 4 rendering errors with no warnings. This
  satisfied the Stage 2 diagnostic completion condition.
- Stage 2 completed. Durability, bounded picker NBT, and Architectury 13 typed payload migration are
  complete; the changed `NetworkTracking.send` platform signature is assigned to Fabric Stage 3.

## Stage 3 verification — Fabric compile blocked by Stage 4 common errors

- Command: `.\\gradlew.bat :fabric:compileJava --console=plain`
- Result: exited 1 after 9 seconds while executing the prerequisite `:common:compileJava`; `:fabric:compileJava` was not reached.
- Diagnostics: the prerequisite reported the same 43 common rendering errors already assigned to Stage 4, with no warnings.
- Stage 3 implementation completed in scope: Fabric tracking now sends the shared `CustomPacketPayload`, and the reload-listener identifier uses the 1.21.1 `ResourceLocation` factory. Server lifecycle/event hooks, TOML paths, persistent-data hooks, trade/reaction Mixins, and Mixin registration were inspected against the 1.21.1 APIs and required no further edits.
- Verification state: blocked at attempt 1 of 3 by the planned Stage 4 common-client work. Skipping `:common:compileJava` would not be a valid substitute because required current common classes were not emitted by the failed compilation.

## Stage 4 pre-edit common rendering diagnostic

- Command: `.\\gradlew.bat :common:compileJava --console=plain`
- Result: exited 1 after 9 seconds with 43 errors and no warnings.
- Classification: four source API groups (`ResourceLocation` construction, vertex submission, camera rotation access, and private `RenderType.create`) plus the expected renderer-layer and model-member access failures. All diagnostics remain inside the bounded Stage 4 shared-client work unit.
- Attempt count: pre-edit baseline; 0 of 3 post-edit verification attempts used.

## Stage 4 shared-client verification attempt 1

- Hypothesis/edit: the 1.21.1 vertex builder uses renamed fluent methods, camera rotation replaces the removed render-system inverse matrix, and the formerly accessible renderer/model members require a minimal current Access Widener.
- Command: `.\\gradlew.bat :common:compileJava --console=plain`
- Result: exited 1 after 13 seconds with 4 errors and no warnings, down from 43 errors.
- Diagnosis: `setNormal` now accepts `PoseStack.Pose` rather than `Matrix3f`; the `RenderType.create` Access Widener descriptor does not match the current private overload. All 39 renderer/model-member and other source errors are removed.
- Attempt count: 1 of 3.

## Stage 4 shared-client verification passed

- Hypothesis/edit: pass `PoseStack.Pose` to the 1.21.1 normal writer and widen the private render-type factory with its actual `CompositeRenderType` return descriptor.
- Command: `.\\gradlew.bat :common:compileJava --console=plain`
- Result: passed after 15 seconds; one task executed, no compiler warnings.
- Stage result: all 43 common rendering errors are removed. The shared rendering, resolver, picker, and minimal Access Widener work unit passed on post-edit attempt 2.

## Stage 4 Fabric-client pre-edit diagnostic — infrastructure failure

- Command: `.\\gradlew.bat :fabric:compileJava --console=plain`
- Result: exited 1 after 8 seconds before Fabric compilation; `:common:compileJava` was up-to-date and `:common:processResources` failed to clean stale outputs.
- Classification: pre-edit infrastructure failure with no Java diagnostics. No files, outputs, processes, caches, or environment settings were changed in response.
- Attempt count: pre-edit diagnostic; 0 of 3 post-edit verification attempts used. One identical retry is permitted.

## Stage 4 Fabric-client pre-edit diagnostic — retry reached compiler

- Command: `.\\gradlew.bat :fabric:compileJava --console=plain` (identical retry)
- Result: exited 1 after 11 seconds with 7 Fabric Java errors and no warnings; common compilation and resources completed.
- Classification: three missing current renderer-access declarations and four diagnostics caused by GeckoLib's moved `GeoAnimatable` package. All belong to the bounded Stage 4 Fabric-client/GeckoLib unit.
- Attempt count: pre-edit baseline; 0 of 3 post-edit verification attempts used.

## Stage 4 Fabric-client verification attempt 1 — infrastructure failure

- Hypothesis/edit: widen the three renderer members demonstrated by the compiler and update GeckoLib's moved `GeoAnimatable` imports.
- Command: `.\\gradlew.bat :fabric:compileJava --console=plain`
- Result: exited 1 after 14 seconds before Fabric compilation; common Java compiled, then `:common:processResources` could not clean stale outputs.
- Diagnosis: infrastructure failure, with no Java diagnostic against the edit. No output, cache, process, or environment cleanup was attempted.
- Attempt count: 1 of 3. One identical retry is permitted.

## Stage 4 Fabric-client verification attempt 2

- Hypothesis/edit: identical retry after attempt 1's resource-cleanup interruption would exercise the current Fabric sources.
- Command: `.\\gradlew.bat :fabric:compileJava --console=plain`
- Result: exited 1 after 10 seconds with 1 error and 1 warning.
- Diagnosis: the 1.21.1 player-renderer map is keyed by `PlayerSkin.Model`, not `String`; GeckoLib 4.7.4 deprecates `GeoModel.getModelResource(animatable)` for removal in favor of the render-state overload. The other six baseline Fabric errors are removed.
- Attempt count: 2 of 3.

## Stage 4 Fabric-client verification passed and stage completed

- Hypothesis/edit: make the player-renderer map key loader-neutral because shared code consumes only values, and localize suppression for GeckoLib 4.7.4's supported animatable lookup used outside an active render-state call path.
- Command: `.\\gradlew.bat :fabric:compileJava --console=plain`
- Result: passed after 17 seconds; common Java, common resources/JAR, and Fabric Java completed with no compiler warnings.
- Stage result: both required Stage 4 production compile gates pass. Access Widener header and Fabric Mixin JSON were validated deterministically. The Fabric dispatcher Mixin remains client-only.
- Verification budget: the Fabric-client unit passed on post-edit attempt 3 of 3.
- Manual checks remain required for ordinary/baby/player models, slime ordering, special attachment families, pupil and expression animation, item rendering/tint, picker HUD/gizmo/workflow, renderer reloads, and available GeckoLib entities.

## 2026-08-29 — Stage 5 verification

- `.\gradlew.bat :common:compileJava --console=plain` — sandbox launch exited 1 before Gradle because the existing user Gradle distribution lock was inaccessible; infrastructure failure, attempt 1 of 3.
- Identical authorized retry of `.\gradlew.bat :common:compileJava --console=plain` — exited 0 in 7 seconds; `:common:compileJava` was up to date and the build passed.
- `.\gradlew.bat :fabric:compileJava --console=plain` — exited 1 in 7 seconds before Fabric compilation because `:common:processResources` could not clean stale generated outputs after the resource-directory migration; infrastructure failure, attempt 2 of 3.
- Identical retry of `.\gradlew.bat :fabric:compileJava --console=plain` — exited 0 in 11 seconds; common resources rebuilt and `:fabric:compileJava` was up to date.
- Deterministic source-resource validation — passed: 259 JSON/metadata files parsed, 74 vanilla definitions selected 1.21.1, Optometrist tag/item invariants held, and singular paths were complete.
- `.\gradlew.bat :fabric:processResources --console=plain` — exited 0 in 16 seconds; one actionable task executed. Gradle emitted its existing Gradle 10 deprecation summary.
- Direct processed-resource inspection — passed: singular recipe, structure, enchantment, item-tag, and enchantment-tag paths were present; plural predecessors were absent; processed Fabric metadata targeted Minecraft 1.21.1; 74 vanilla definitions were present.

### Stage 5 complete

Stage 5 completed with common and Fabric production compiles and Fabric resource processing passing. The bounded work unit used 2 of 3 failed attempts, both infrastructure/output-cleanup failures whose identical retries passed. Stage 6 is the next session.

## 2026-08-29 — Stage 6 Fabric GameTest port

- Pre-edit diagnostic: `.\gradlew.bat :fabric:compileGametestJava --console=plain`. The sandboxed
  launch could not access the existing Gradle distribution lock; the identical authorized retry
  reached `:fabric:compileGametestJava` and failed in 9 seconds with 45 errors. The failures were
  confined to Stage 6 identifier factories, crafting inputs and components, data-driven enchantment
  lookup, removed mock-player helpers, and packet fixtures. This diagnostic did not count as a
  post-edit correction attempt.
- Passing gate: `.\gradlew.bat :fabric:compileGametestJava --console=plain` completed in 9 seconds
  with no warnings. One task executed and five production prerequisites were up to date. Failed
  post-edit verification attempts: 0 of 3.
- Stage 6 complete: retained 77 shared assertions, supplied 78 Fabric wrapper tests and 12 discovery
  entrypoints, added the Fabric entity save/load persistence assertion, and stopped at the stage
  boundary.

## 2026-08-29 — Stage 7 Fabric GameTest runtime

- User-run gate: `.\gradlew.bat :fabric:runGameTestServer --console=plain` discovered 78 tests,
  passed all 78 required tests in 1.202 seconds, shut the server down cleanly, and completed the
  Gradle invocation successfully in 16 seconds. Twelve tasks were actionable: four executed and
  eight were up to date. Failed correction attempts: 0 of 3.
- The run emitted a non-fatal Fabric convention warning for an untranslated item tag. Repository
  inspection identified the sole project-owned item tag as `somegoogly:enchantable/shears` and the
  missing display key as `tag.item.somegoogly.enchantable.shears`; no Stage 7 behavior failed.
- Stage 7 complete; stopped at the stage boundary.

## 2026-08-29 — Stage 8 resource correction

- `.\gradlew.bat :fabric:processResources --console=plain` passed in 7 seconds; its single task was
  up to date. The source language JSON parses and contains
  `tag.item.somegoogly.enchantable.shears=Enchantable Shears`. Failed post-edit verification
  attempts: 0 of 3.

## 2026-08-29 — Stage 8 packaging attempt 1

- `.\gradlew.bat :fabric:build --console=plain` failed in 8 seconds at
  `:common:processResources`; Gradle reported that it could not clean stale outputs. Compilation did
  not fail. This is failed post-edit verification attempt 1 of 3 and qualifies for one identical
  infrastructure retry with no cleanup or environment change.

## 2026-08-29 — Stage 8 packaging completion

- The identical retry of `.\gradlew.bat :fabric:build --console=plain` passed in 14 seconds. Thirteen
  tasks were actionable: six executed and seven were up to date. The prior stale-output failure
  remains failed attempt 1 of 3.
- Processed common resources contain
  `tag.item.somegoogly.enchantable.shears=Enchantable Shears`. The remapped release artifact exists
  at `fabric/build/libs/somegoogly-fabric-0.8.1.jar` with a size of 477,665 bytes; it was not opened
  or unpacked.
- Reconciled `player-view.md`, `as-built.md`, and `build-env.md` with the completed 1.21.1 Fabric
  runtime, deferred Forge/NeoForge ports, and outstanding manual client and optional-mod checks.
- Stage 8 and the Fabric 1.21.1 port are complete. All six completion gates pass. No Git, GitHub,
  publishing, or release action occurred.
