# In-Game Test Plan

Run these in a disposable test world first, with screenshots, the latest log, and exact repro steps captured for every failure. Use a dedicated-server pass separately; it is not covered by an integrated-server test.

## Smoke and lifecycle

- [ ] Launch a client with only this mod installed; reach the title screen and create or load a world.
- [ ] Launch a dedicated server with the mod, then connect a matching client. Confirm the server starts without client-class errors.
- [ ] Join, leave, then join a different server or world. Confirm no prior server's configs, eyes, or picker state leak into the next session.
- [ ] Reload the world and restart the game. Confirm persistent eye state, variants, and appearance overrides survive as intended.
- [ ] Test with the mod absent on one side and present on the other; record the actual handshake or error behavior.

## Spawn decisions and datapack selection

Use a test datapack with one unmistakable configured entity.

- [ ] Set the global spawn chance high; newly spawned eligible mobs should receive eyes.
- [ ] Set it low or disabled; newly spawned eligible mobs should not.
- [ ] Confirm existing mobs do not reroll after changing the chance, `/reload`, dimension travel, growth, save/load, or restart.
- [ ] Add an exact per-entity override; verify it beats the global value.
- [ ] Add overlapping wildcard overrides; verify the first matching wildcard wins.
- [ ] Test `enabled:false`; the entity must never visibly receive eyes.
- [ ] Test `any`, `adult`, and `baby` entries, including a mob that grows up.
- [ ] Test several weighted variants. Verify the selected placement stays stable for an individual mob through save/load and age changes.
- [ ] Temporarily give a config a head with no eyes. It should be rejected or visibly fail safely—never create harvestable invisible eyes.
- [ ] Add one malformed JSON file alongside valid files; `/reload` should retain valid configs and log the bad file.

## Rendering and eye physics

- [ ] Check a humanoid, a quadruped, and a hierarchical vanilla model.
- [ ] Check first-person, third-person, spectator, and another player's viewpoint.
- [ ] Walk, sprint, jump, fall, turn sharply, look sharply, ride, swim, and fly. Iris wobble should remain bounded, settle naturally, and not flip at yaw wraparound.
- [ ] Check a baby and adult of the same configured species.
- [ ] Check invisible entities with `affectedByInvisibility` both enabled and disabled.
- [ ] Check glow in darkness and ordinary lighting.
- [ ] Look for clipping, z-fighting, reversed normals, wrong size, or iris/cornea separation at close range.
- [ ] Check the held, inventory, and dropped googly-eye item renderer, including its wobble and appearance.

## Client overrides

- [ ] Toggle the global client disable. No eyes should render; re-enable should restore them without reconnecting.
- [ ] Disable one entity ID and then a whole mod namespace.
- [ ] Put malformed values in `disabledEntities`; the client should log each bad value and continue rendering everything else.
- [ ] Change the client config while in game. Confirm it takes effect without restart.
- [ ] Verify a client cannot force eyes onto an entity the server has not approved.

## Items, harvesting, crafting, and brewing

- [ ] Confirm the googly-eye item appears in its creative tab.
- [ ] Verify unenchanted shears do not steal vanilla sheep or mooshroom interaction.
- [ ] With Optometrist shears, right-click an eyed configured mob: one eye-item drop, eye state removed, shears damaged, mob unharmed.
- [ ] Harvest a mob with distinct configured appearance overrides; verify the drop preserves the effective iris, cornea, and glow.
- [ ] Kill an eyed mob with direct shears melee. Verify the configured chance, drop count, and durability loss.
- [ ] Kill with a projectile, indirect damage, or non-shears while holding shears. Confirm no custom shears harvest occurs.
- [ ] Craft an eye with each dye, glowstone dust, redstone dust, and cobweb. Check the output appearance and that unrelated NBT on the eye survives.
- [ ] Brew an awkward splash potion plus a styled eye. Confirm the brewed splash retains that eye's appearance.
- [ ] Check the creative googly-eyes splash potion's tint and behavior.
- [ ] Throw at one eligible target, several eligible targets, only already-eyed targets, and only ineligible targets. Exactly one eligible target should change when candidates exist.
- [ ] Test a configured player deliberately. Decide whether the potion should affect players; the current code does not hard-exclude them if they are configured.

## Behaviors

Use short test-only ambient intervals.

- [ ] Confirm every ambient behavior in the configured pool can start, render, end, and return to idle.
- [ ] Remove a behavior from the pool; it must no longer occur ambiently.
- [ ] Trigger grow by player damage; test the enabled flag and chance extremes.
- [ ] Trigger swirl through villager trade and healing; verify the heal cooldown.
- [ ] Trigger each behavior through `/sg admin behavior <id>` and `random`.
- [ ] Attempt a second behavior during an active one; it must be dropped rather than interrupting.
- [ ] Have a second player start tracking during an active behavior; they should see a correctly progressed animation.
- [ ] Move all players out of tracking range, then return. No stale or busy animation should survive incorrectly.
- [ ] Stand near an initially eyeless eligible mob, give it eyes with a potion, then test ambient, hurt, heal, and trade reactions. This specifically exercises the scheduler registration gap identified in review.

## `/sg admin`

- [ ] Confirm non-operators cannot use `/sg admin`.
- [ ] Test `eyes`, both tint types, clear, all glow modes, each behavior, and `random`.
- [ ] Confirm mutations synchronize to a second client immediately and persist through restart.
- [ ] Target an entity without a usable config. The command should not report a misleading visible success.
- [ ] Try targeting through a solid wall; decide whether the command should require line of sight.

## Picker and export

Use a creative single-player world.

- [ ] Confirm picker controls are visible and rebindable in Controls: toggle, lock, previous part, next part.
- [ ] Confirm picker activation and every `/sg` authoring command fail outside creative mode.
- [ ] Toggle the picker, lock a mob, cycle parts, unchoose, and toggle off. Check HUD, gizmo, preview, and release of the mob.
- [ ] Lock a mob already set to `NoAI`, then immediately unchoose or toggle off. Verify its original `NoAI` value is restored.
- [ ] While frozen, exit to title, restart, and shut down the integrated server. Verify no unintended permanent freeze.
- [ ] Exercise `list parts`, named and numbered `part`, `part none`, invalid part names, create, move, rotate, posrot, save, select, delete, and invalid indices.
- [ ] Exercise variants: create, select, delete, weights, empty variants, and switching after edits.
- [ ] Test every editable property: eye scale, iris scale, both colors, glow, and invisibility behavior.
- [ ] Export a new config. Confirm the datapack is enabled after reload, the JSON is valid, the eye appears after restart, and a second export overwrites only the expected entity file.
- [ ] Run `/sg spawnall` in a disposable flat world. Check representative vanilla and modded mobs, aquatic water placement, AI freezing, and failure handling for entities that cannot be constructed.

## Compatibility matrix

- [ ] No GeckoLib installed: normal startup and vanilla rendering.
- [ ] GeckoLib installed: a configured GeckoLib entity, picker bone enumeration, normal eyes, overrides, glow, invisibility, and behavior rendering.
- [ ] Citadel-based entity: attachment names, parent transforms, picker enumeration, movement animation, and rendering.
- [ ] A modded entity with an unsupported or odd model: no client crash; failure should be isolated to that entity.
- [ ] Two clients with different client disable lists: each should apply its own local veto without affecting server state or the other client.

## Priority order

Run the dedicated-server, post-potion behavior, picker `NoAI`, export/reload, and second-client synchronization cases first.
