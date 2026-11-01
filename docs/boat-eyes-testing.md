# Boat eyes: physical-client checks

Run on Minecraft 1.20.1 with Fabric and Forge. Run the multiplayer checks with a dedicated server and two clients. Shared GameTests cover state, appearance transfer, native save/load, payload serialization, geometry selection, simulation replacement, pause/idle eviction, spectator refusal, and ambient pairing; they do not establish visual placement or event ordering.

## Application and normal use

- Place every vanilla wood's boat and chest boat, including bamboo rafts and chest rafts. All start eyeless, even with the mob spawn chance set to its maximum. Existing unmodified boats also remain eyeless.
- Apply a Slimy Eye in survival. The boat gains two eyes, one on each side, one item is consumed, and the application plays its sound without boarding or opening cargo. Try ordinary, dyed, glowing, and glow-disabled eyes.
- Apply in creative; the stack remains. Try another Slimy Eye on an eyed boat; it spends nothing and retains normal boat interaction.
- Remove the eyes using Optometrist shears. Exactly one Googly Eye drops with the effective first eye's appearance, the boat loses its eyes, the shears lose one durability, and the shearing sound plays without boarding or opening cargo. Plain shears retain normal boat interaction.
- Test both hands. For offhand use, sneak with an empty main hand so ordinary main-hand boarding does not consume the click first. Check that the actual used hand supplies the item or loses durability. Rejected actions retain vanilla fallback; when sneaking, also check the eye/shears item's normal self-use.
- Try from spectator mode, out of interaction range, and under a protection mod that cancels entity interaction. No eye state, item count, tool durability, drops, or game events should change for denied use. Repeat for both hands and both cargo variants.
- Hold either eye item and sneak while aiming at a boat: the inspector reports eligibility or existing eyes, and does not inspect through walls. Disable the matching definition and verify the verdict changes.

## Placement and rendering

- Inspect both sides near the front of every hull and cargo variant, both occupied and empty. Check pupil direction, clipping, gaps between the eyes and hull, the separate raft placement, paddles, passengers, chest visibility, and water masking.
- Row forward and backward, turn, hit the hull to make it rock, and use bubble columns in both directions. Eyes must stay attached to the composed hull pose; world gravity must remain correct while rocking. With ambient expressions off, both side pupils settle downward and lag when the boat accelerates or turns. Changing camera pitch or F5 view alone must not move the gravity direction.
- Inspect from above, below, either side, and several camera angles, in water and on land, in bright light and darkness. Glow must follow the eye's appearance; eyes must not appear detached as camera orientation changes.
- Compare ordinary render distance and edge-of-screen culling with an eyeless boat. Check that the configured side placement stays inside the visible hull's rendering envelope.

## Expressions and lifecycle

- Shorten ambient intervals and try each enabled ambient expression. The bundled opposite-side eyes have no cross-eye partners; a datapack can supply paired eyes that cross; a one-eye layout or eyes with no cross-target must not cross. No hurt, heal, anger, or sleep reactions are added to boats.
- Turn ambient behavior off, then use an empty ambient pool: expressions stop while pupil physics remains. Pause and resume a single-player world; boat animation pauses while inventory animation continues.
- Put two boats together. They must have independent pupil state. Look away long enough for render-cache eviction, then return; expressions follow current world time without replaying a backlog.
- Change a layout or disable it with a datapack and reload. Old geometry and simulations must disappear. Re-enable it; stored eye state is retained. Also test a resource reload, client display switches, and disabled-entity entries for `minecraft:boat` and `minecraft:chest_boat`.
- Save and reload the world, unload and reload the chunk, and disconnect/reconnect. Layout roll and appearance persist; disposable pupil state can restart. Change worlds and confirm no previous boat simulation remains.

## Multiplayer and item boundary

- Have one player apply, harvest, and reapply while the other watches. Then join with a second client after application and enter tracking range after unloading the boat. Both clients must receive the same saved appearance and ambient timing without animation packets.
- Turn the server master switch off and back on while both clients watch. Eyes hide and interactions stop while off; saved eyes return when enabled.
- Break an eyed boat and place its dropped item. The resulting boat starts eyeless; there is no boat-item eye component or additional eye drop. Repeat with a chest boat, creative destruction, and a bamboo raft, checking ordinary boat and cargo drops.

## Shared-path regressions

Retest eyed mobs and placed heads, head-item persistence, first-person loose-eye movement/hurt/heal, framed eye diameter and animation, inventory/hotbar/cursor animation, and both third-person hands. Paired head items must retain cross-eye. Existing checklists: `framed-eyes-testing.md`, `inventory-eyes-testing.md`, and `third-person-eyes-testing.md`.
