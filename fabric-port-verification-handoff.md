# Fabric Port Verification Handoff

This is the authoritative continuation document for the Some Googly Eyes Forge/Fabric port. Work on
one verification phase per conversation unless the user explicitly combines phases.

## Read first

At the start of each continuation conversation, read:

1. `CLAUDE.md` — repository restrictions and authorization rules.
2. `player-view.md` — player-visible acceptance contract.
3. `as-built.md` — active architecture and runtime invariants.
4. `build-env.md` — Gradle layout, versions, and environment traps.
5. This handoff — current checkpoint, risks, and remaining plan.

The active Gradle modules are `common`, `forge`, and `fabric`. The root `src/` tree is an inactive
legacy reference and must not receive live changes.

## Current checkpoint

- Branch: `1.20.1`.
- Implementation checkpoint: `4039ada Add Fabric support and complete the dual-loader port`.
- The worktree was clean immediately after that commit.
- User-confirmed successful commands on 2026-08-20:
  - `.\gradlew :forge:build`
  - `.\gradlew :fabric:build`
- Common, Forge, and Fabric compile and package successfully.
- Neither 69-test GameTest suite has been run against this checkpoint.
- Systematic client, dedicated-server, and behavior parity testing remains.
- The port is not yet behaviorally verified or release-ready.

`CLAUDE.md` prohibits build and run commands unless the user explicitly asks. For each phase, either
guide the user through its commands and analyze the pasted results, or run them only when that phase's
prompt clearly authorizes it.

## Current architecture

### Source ownership

- `common/src/main/java` contains loader-neutral registration, state, config values, packet logic,
  server services, shared rendering/resolvers, picker UI/input/commands, and other vanilla/Architectury
  client code. Its canonical AW supplies the vanilla member access required at compile time; Fabric
  resource processing packages that file beside Fabric's metadata.
- `forge/src/main` and `fabric/src/main` contain entrypoints, native event/config adapters, platform
  implementations, loader-local GeckoLib adapters, and metadata. Their packages are disjoint from
  common packages.
- `common/src/gametest/java` contains shared test logic. Each loader has thin wrappers in its own
  `src/gametest/java` tree.

### Registration and configuration

`SomeGooglyCommon.init()` registers common networking, items, Optometrist, the creative tab, and recipe
serializers through Architectury. `GooglyEyeItemFactory` is an `@ExpectPlatform` boundary because Forge
attaches its renderer through patched item API while Fabric registers it during client initialization.

Optometrist uses vanilla `EnchantmentCategory.BREAKABLE` and overrides `canEnchant(ItemStack)` to
accept only shears.

`ServerConfig` and `ClientConfig` expose common `ConfigValue<T>` values. Forge uses `ForgeConfigSpec`.
Fabric uses the narrow `FabricToml` reader:

- client: `config/somegoogly-client.toml`;
- server: `<world>/serverconfig/somegoogly-server.toml`.

Fabric TOML loads at client initialization or world start; it is not live-watched. Datapack eye
definitions still use Minecraft's resource reload lifecycle.

### Persistent entity state

All active entity-owned NBT access goes through `EntityPersistentData`.

- Forge delegates to its patched persistent compound.
- Fabric's `EntityPersistentDataMixin` adds a compound to every entity and saves/loads it beneath
  `somegoogly:persistentData`.

The common eye and picker services must remain unaware of the loader-specific storage mechanism.

### Networking and server authority

Networking uses Architectury protocol version `7`.

- Stable handshake ids: `somegoogly:protocol_hello` and `somegoogly:protocol_ack`.
- Gameplay ids: `somegoogly:v7/...`.
- Join starts a hello/ack exchange with a 100-tick timeout.
- Matching acknowledgement marks the player ready and triggers config sync.
- Mismatch or timeout disconnects with an explicit protocol message.
- S2C handlers register only during physical-client initialization.
- Forge tracking uses its packet distributor; Fabric uses `PlayerLookup.tracking`.

The server owns eye eligibility, persistent state, behavior scheduling, item application/harvesting,
picker world mutation, and C2S authorization. Client checks are UX only.

`ServerServices` and `EyeItemService` contain the common behavior. Forge subscribers and Fabric API
callbacks are thin adapters. Fabric Mixins cover persistent data, hurt/heal reactions, completed
merchant trades, and client renderer reload.

### Rendering and picker

The common Access Widener and Forge's patches or Access Transformer expose the members needed by
shared render code. Fabric additionally widens renderer-dispatch maps, `addLayer`, and the full
`RenderType.create` factory;
`ClientRendererAccess` hides that loader asymmetry.

Shared code owns eye trackers, model attachment, layer installation, inspection, picker state/HUD/
input, export, and client `/sg` commands. Loader modules only register their native callbacks.

GeckoLib 4.7.4 is optional and compile-only behind `GeckoCompat`. Loader implementations must prevent
Gecko-typed classes from loading when GeckoLib is absent, particularly on dedicated servers.

### GameTests

There are 69 tests per loader across 12 wrapper classes. Assertion logic lives once in common. Forge
uses a separate `somegoogly_gametest` dev mod; Fabric lists every wrapper class explicitly under its
`fabric-gametest` entrypoint. A run which discovers zero tests is a failure, not a pass.

## Invariants to preserve

- Common main must not import Forge, Fabric API, GeckoLib, or Forge-only patched members.
- Shared vanilla rendering may use members declared by the common AW when Forge supplies equivalent
  access through a patch or AT.
- Loader source packages must remain disjoint from common packages.
- GeckoLib-typed code belongs behind the loader-specific `GeckoCompat` implementations.
- Genuine API differences use thin adapters or `@ExpectPlatform`; gameplay logic remains common.
- Keep the Forge AT and common AW aligned wherever both loaders require the same access.
- Route all entity persistent data through `EntityPersistentData`.
- Keep S2C receiver registration on the physical client.
- Keep all C2S picker authorization on the server.
- Bump the gameplay protocol version after an incompatible wire change; keep hello/ack ids stable.
- Add test logic once in common and expose it through both loaders. Add any new Fabric wrapper class to
  Fabric GameTest metadata.
- Preserve `player-view.md`; do not change behavior merely to simplify a loader adapter.

## Unverified risks

1. Fabric Mixin targets and descriptors at runtime:
   - `Entity#saveWithoutId` and `Entity#load`;
   - `LivingEntity#actuallyHurt` and `LivingEntity#heal`;
   - `MerchantResultSlot#onTake` and its `merchant` field;
   - `EntityRenderDispatcher#onResourceManagerReload`.
2. Architectury join, handshake, and buffer behavior on both loaders.
3. Fabric death, hurt, heal, and trade callback timing relative to Forge behavior.
4. Fabric persistence across save/reload, chunk unload, and dimension travel.
5. Renderer installation at initial load and after resource reload without duplicate layers.
6. Baby transforms, Rabbit/Llama transforms, slime layer ordering, and Gecko bone attachment.
7. Absence of accidental GeckoLib loading when the dependency is missing.
8. Fabric key polling behavior while screens are open and under key repeat.
9. Picker freeze cleanup after unlock, disconnect, chunk reload, and shutdown.
10. Forge emits FML-context deprecation warnings. They are not failures and are outside parity work
    unless the user requests cleanup.

## Remaining plan

Each phase is intended to occupy one conversation. Begin with a running task list, complete and record
the phase, then stop unless the user explicitly asks to continue.

### Phase 1 — Forge GameTests

Goal: establish the Forge regression baseline for shared/server logic.

1. Run or have the user run `.\gradlew :forge:runGameTestServer`.
2. Confirm that 69 tests are discovered.
3. Record pass/fail counts and every failing test name.
4. Fix only defects revealed by the run and rerun until clean or concretely blocked.

Exit: 69 tests discovered and passing on Forge.

### Phase 2 — Fabric GameTests

Goal: prove the same shared test suite passes under Fabric.

1. Run or have the user run `.\gradlew :fabric:runGameTestServer`.
2. Confirm that 69 tests are discovered through Fabric's explicit entrypoints.
3. Compare discovery and failures with Forge.
4. Fix the narrowest correct loader/shared layer and rerun until clean or blocked.

Exit: 69 tests discovered and passing on Fabric.

### Phase 3 — client and dedicated-server smoke tests

Goal: catch entrypoint, metadata, side-loading, Mixin, and optional-dependency startup failures.

Matrix:

- Forge client reaches the main menu and opens a world.
- Fabric client reaches the main menu and opens a world.
- Forge dedicated server reaches ready state and stops cleanly.
- Fabric dedicated server reaches ready state and stops cleanly.
- Fabric starts without GeckoLib; perform the equivalent Forge check if the development runtime
  normally supplies GeckoLib.

Exit: no startup crash, Mixin application error, client-class leak, or mandatory GeckoLib loading.

### Phase 4 — networking and config synchronization

Goal: verify protocol-v7 negotiation and server-authoritative definitions.

On Forge-to-Forge and Fabric-to-Fabric connections, verify:

1. Matching builds complete hello/ack and join normally.
2. Resolved eye config reaches the client on join and datapack reload.
3. Existing entity state arrives when tracking begins.
4. Disconnect/reconnect clears old client config and tracker state.
5. A missing, unresponsive, or deliberately incompatible client receives the intended disconnect
   rather than a decoder crash or partial connection.

Cross-loader client/server interoperability is out of scope unless explicitly requested.

Exit: same-loader join, sync, reload, reconnect, and incompatibility behavior are stable.

### Phase 5 — persistence, eligibility, and tracking

Goal: validate the lifetime eye decision and Fabric persistent storage.

Verify:

1. Eligible non-player entities roll once; players do not roll naturally.
2. Eye state, variant roll, and overrides survive save/reload.
3. State survives chunk unload/reload and dimension travel without rerolling.
4. Config changes affect new entities, not initialized ones.
5. Newly tracking players receive full current state.
6. Mid-life changes synchronize to all required recipients.

Exit: no rerolls, lost state, duplicate initialization, or tracking desynchronization.

### Phase 6 — items, enchantment, recipes, and harvesting

Goal: verify the player-facing eye acquisition/application loop.

Verify:

1. Items, recipes, creative tab, and Optometrist book exist on both loaders.
2. The 3D Googly Eye renderer and Slimy Eye iris tint are correct.
3. Slimy Eye application owns the target click and preserves appearance.
4. Invalid targets refuse without consuming the item.
5. Optometrist applies only to shears and enables nonlethal harvesting.
6. Direct shears melee death harvesting obeys chance, drop, and durability rules.
7. Appearance survives harvest, modifier recipes, Slimy Eye crafting, and reapplication.

Exit: the complete item loop matches `player-view.md` on both loaders.

### Phase 7 — behavior event parity

Goal: verify scheduler operation and Fabric reaction hooks.

Verify:

1. Ambient behaviors run only for watched, eligible eyed mobs.
2. Player damage can trigger grow under configured probability.
3. Healing triggers swirl and respects cooldown.
4. Completed villager and wandering-trader trades trigger swirl once.
5. Untracked mobs do not retain unnecessary active scheduling.
6. Newly tracking players receive catch-up for an active behavior.
7. Server stop/world change clears transient scheduler state.

Exit: Forge and Fabric triggers and observable timing are equivalent.

### Phase 8 — vanilla rendering and resource reload

Goal: exercise AT/AW-sensitive model attachment and renderer installation.

Minimum matrix:

- ordinary hierarchical vanilla mob;
- adult and baby `AgeableListModel` mob;
- baby Sniffer;
- adult/baby Rabbit and Llama;
- Slime, including outer-layer ordering;
- player skin renderer.

Trigger a resource reload and confirm layers are reinstalled once, caches clear, and rendering
continues. Also verify client disabled-entity/mod settings and the inspection indicator.

Exit: correct attachment, scale, ordering, and reload behavior without duplicate layers or crashes.

### Phase 9 — picker, commands, and authorization

Goal: verify the authoring workflow without weakening server authority.

Verify:

1. Picker keys, HUD, gizmo, part cycling, lock/unlock, and preview.
2. Client `/sg` commands, suggestions, edits, spawn, spawnall, and export.
3. Freeze restores prior `NoAI` after unlock, disconnect, chunk reload, and stop.
4. Non-creative C2S requests are rejected server-side.
5. `spawnall` additionally requires server opt-in.
6. Export remains cooldown-, size-, and path-constrained and writes expected datapack JSON.

Exit: picker workflow functions on both loaders and authority remains server-side.

### Phase 10 — optional compatibility and release audit

Goal: close compatibility and documentation gaps.

1. Confirm base client and dedicated server startup without GeckoLib.
2. With GeckoLib present, verify supported Geo renderers enumerate bones and receive layers.
3. When available, check representative Citadel/LLibrary, Alex's Mobs, Exotic Birds, and Twilight
   Forest definitions without making those mods mandatory.
4. Re-run both builds and both 69-test suites after runtime fixes.
5. Update `player-view.md`, `as-built.md`, `build-env.md`, and this handoff to final reality.
6. Separately decide whether to remove inactive `src/`, old `run/`, stale IDE runs, old handoff, and
   `working-build-env/`; deletion is not implied by verification.
7. Produce an evidence-backed release-readiness report with verified combinations and limitations.

Exit: current documentation and an explicit release-readiness decision with no unstated gaps.

## Progress recording

After each phase, append one entry below containing:

- date and phase;
- exact commands or manual matrix used;
- automated discovery/pass/fail counts;
- defects and fixing commits;
- remaining unverified points;
- next phase.

## Progress log

- 2026-08-20 — Checkpoint `4039ada`: Forge and Fabric builds pass. Next: Phase 1, Forge GameTests.
