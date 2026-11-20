# Client tests: implementation plan for Fabric 1.21.8

Implements `docs/client-test-plan.md` as Fabric client gametests that only set up scenes and take screenshots, reviewed in the `screenshot-review` app (`../screenshot-review/PRD.md`). Shipped code does not change; everything lives in test source, test resources, and the Fabric build script.

The owner runs every build and test run; each phase ends with a run the owner makes and reviews.

**Status:** Phases 1 to 5 are written; Phase 0 was skipped at the owner's direction, so the API facts below are recalled, not checked, and the first build settles them. Nothing in the mod has been built or run. The review app compiles. Section 15 is not applicable on Fabric 1.21.8, the 2.4 frame rows run as six scenarios, and 3.8's pause-screen GUI check is omitted because no pausing screen shows eye icons.

## Phase 0: Confirm the API

Read Fabric API's own sources and Javadoc for `fabric-client-gametest-api-v1` at this checkout's Fabric API version (0.136.1+1.21.8), and record what is confirmed in `platform-notes.md`. The plan below rests on these recalled facts, each to be confirmed or corrected:

- Tests implement `FabricClientGameTest` and are listed under a `fabric-client-gametest` entrypoint; each entrypoint class is one test with one `runTest(ClientGameTestContext)`.
- The client run enables them with the `fabric.client.gametest` system property, runs every registered test in turn, and quits.
- `ClientGameTestContext` offers `worldBuilder()` (singleplayer world with adjustable creation settings, and a dedicated server the client can connect to), `waitTicks`, `waitFor`, `runOnClient`/`computeOnClient`, `getInput()` (keys, mouse, typing), `setScreen`, `clickScreenButton`, and `takeScreenshot` returning the saved file's path.
- The singleplayer context exposes the integrated server for `runOnServer`, and a way to wait for chunks to render.
- Options are reset and the window size fixed for every run.
- Opening an existing save (needed for 10.11) is possible, through the API or by driving the world-select screen.

Anything not confirmed changes the matching part of Phase 2 before code is written.

## Phase 1: Build wiring

In `fabric/build.gradle`, following the existing `gametest` source set's pattern:

- A `clientGametest` source set compiled against `main` (and `common`), with its own resources.
- A `somegoogly_clientgametest` entry in `loom.mods`.
- A `clientGameTest` run: `client()`, the `fabric.client.gametest` property, source `clientGametest`, and its own run directory (`run/clientgametest`) so the scripted client never touches the development client's worlds, options, or configs.
- A second run, `clientGameTestHidden`, with run directory `run/clientgametest-hidden` and a property naming the launch group, for the [Launch] tests (9.4, 12.2.x). Its client config is copied into place by a `Copy` task the run depends on.
- `fabric/src/clientGametest/resources/fabric.mod.json` for `somegoogly_clientgametest`, client-only, listing the test classes under `fabric-client-gametest`.

Update `orientation-build.md` and the `build-env/` snapshot.

**Owner run:** `runClientGameTest` with a single smoke test that creates a world, waits for chunks, and takes one screenshot.

## Phase 2: Test harness

All in `fabric/src/clientGametest/java/.../clientgametest/`, as a few coherent classes rather than many small ones.

**Shots and manifest.** One method takes a screenshot with its plan number, title, and expectation, copies the image into a timestamped run folder (`run/clientgametest/review/<time>/`), and records the entry. The manifest (`manifest.json`, format defined in the review app's README) is written after every shot, so a crashed run still leaves a reviewable folder. The run header carries Minecraft version, loader, mod version, and start time.

**Scene control.**
- Create the standard world: superflat, creative, cheats on, fixed time and weather, mob spawning off.
- Give each scenario its own area and place the camera there with `/tp`, at an exact position and rotation.
- Run commands as the player through the client's normal command path, so `/sg` and vanilla permission checks apply. Run server-only setup on the integrated server.
- Freeze and step ticks; force expressions with `/sg admin behavior` or a one-expression pool.
- Write `somegoogly-server.toml` in the world's `serverconfig`, `/reload`, and restore the original afterward.
- Toggle the HUD, F5 mode, subtitles, and GUI scale; open and close screens; press keys and click slots.
- Switch game mode, give and equip items, and summon mobs with fixed rotation and `NoAI` as needed.

**Test datapack.** Registered by the test mod as a built-in datapack, off by default and enabled per test with `/datapack enable`. It holds the paired-eye and one-eye boat layouts, two alternate cow layouts, a definition-disabling override, and a stick in `c:tools/shear`. Accessor mixins are added in the test source set only if a scenario cannot be staged with commands; none is expected.

**Launch groups.** A property selects which tests run, so the hidden-config run executes only the [Launch] tests and the normal run skips them.

**Owner run:** the smoke test now produces a run folder with a manifest.

## Phase 3: Review app

Build the Rust app from its PRD against the manifest format fixed in Phase 2. It is independent of the mod code and can be done in its own session once the format exists.

**Owner check:** open the smoke-test run folder, judge the screenshot, and export a report.

## Phase 4: Tests, in batches

One test class per plan section, since each class is one world: per-scenario worlds would cost a world creation each. A class runs its scenarios in order, each in its own area. Each scenario's Javadoc says how to reproduce it in-game, and its plan number appears in its shots.

| Batch | Sections | Notes |
| --- | --- | --- |
| A | 1 Carriers, 2 Physics | Establishes camera placement and freezing patterns |
| B | 3 Ambient, 4 Triggered | Relies on forced expressions; 4.4, 4.7, 4.8 need AI and may need rework |
| C | 7 Crafting, 8 Display | Heaviest GUI driving |
| D | 5 Getting, 6 Applying, 9 Target check | Survival and adventure modes, both hands |
| E | 10 Lifecycle, 12.1 Server config, 13 Reload | Save/reopen and datapack swapping |
| F | 11 Multiplayer | Dedicated server [DS] |
| G | 14 Picker and commands | Key bindings and chat |
| H | 9.4, 12.2.x | The hidden-config run [Launch] |
| I | 10.11 | Needs a world saved by the 1.21.4 release, created by the owner and kept under the test resources |
| J | 1.1.8, 15.x | [Mod]; only if the owner adds those mods to the test run |

[File] tests (12.1.7, 14.9, 14.10) take their in-game shots only.

After each batch the owner runs and reviews. A flagged shot is either a test problem, fixed in the test, or a mod bug, fixed the usual way with its own server gametest where one can reach it.

## Phase 5: Documentation

- `orientation-code.md`: the client tests, their sections, and how to run them.
- `platform-notes.md`: client-gametest behavior learned along the way, marked with how it is known.
- The four manual checklists: decide, once their items are covered and reviewed, whether to delete them or keep them for Forge and NeoForge, which these tests do not reach.

## Later versions

The tests drive the game through commands, input, and screenshots, not through the mod's rendering code, so the 1.21.9 rendering rewrite does not touch them. Carrying them to 1.21.11 and 26.x should need only Fabric API changes, Minecraft command or name changes, and fresh judgments of the screenshots.
