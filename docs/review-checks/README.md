# Manual 1.21.8 review checks

Use a disposable MultiMC world on each loader (NeoForge, Forge, Fabric). These checks have not been run by the agent.

## Interaction advancement

Copy `advancement-probe` into the world's `datapacks` directory and run `/reload`. Before each attempt, revoke `somegoogly_review:apply` and `somegoogly_review:harvest`. Check completion with `/execute if entity @s[advancements={somegoogly_review:apply=true}] run say apply awarded` (use `harvest` for shears).

Apply a Slimy Eye to an eligible eyeless cow, including the last eye in a stack; check apply credit. Harvest with Optometrist shears and check harvest credit. Repeat with an armor stand for positioned interaction. Repeat with ineligible or protection-refused use; neither refusal should award credit. Compare all three loaders. Record actual results before adding any compensation trigger.

## Spawn protection and adventure restrictions

Use a dedicated disposable server with spawn protection enabled. As an operator, place eyeless and eyed skulls inside the protected area and equivalent controls outside it. As a nonoperator, try Slimy Eye application and Optometrist removal on all of them. Protected heads, item counts, shears durability, and drops must remain unchanged. Controls outside protection must succeed. In adventure mode, compare items whose `can_place_on` permits the clicked skull with ones that do not, clicking each face. Repeat on all three loaders. Keep the current `mayInteract` check until the upstream guarantee is independently established.

## Held eyes and rendering

Hold a Googly Eye in either or both hands. Change between first and third person while it animates; both views show the same pupil and expression. Have another player watch damage (including nonplayer damage such as a fall) and healing, including a heal during the cooldown; both players see the bulge and the swirl. Join tracking during a reaction, unequip and re-equip, change dimensions or worlds, and toggle the master and client visibility switches. Check paused GUI animation, inventory slots, the cursor, the hotbar, frames, placed heads, head items inside slots and frames, head items in hands, and boats. Reload resources and check baby and adult mobs and the existing whole-model integrations. First-person head items remain at rest. Verify production-jar startup on a client and a dedicated server for each loader after the client package moves.
