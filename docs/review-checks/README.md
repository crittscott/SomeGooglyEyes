# Manual 1.20.1 review checks

Use a disposable MultiMC world on each loader. These checks have not been run by the agent.

## Interaction advancement

Copy `advancement-probe` into the world's `datapacks` directory and run `/reload`. Before each attempt, revoke `somegoogly_review:apply` and `somegoogly_review:harvest`. Check completion with `/execute if entity @s[advancements={somegoogly_review:apply=true}] run say apply awarded` (use `harvest` for shears).

Apply a Slimy Eye to an eligible eyeless cow, including the last eye in a stack; check apply credit. Harvest with Optometrist shears and check harvest credit. Repeat with an armor stand for positioned interaction. Repeat with ineligible or protection-refused use; neither refusal should award credit. Compare Forge and Fabric. Record actual results before adding any compensation trigger.

## Spawn protection and adventure restrictions

Use a dedicated disposable server with spawn protection enabled. As an operator, place eyeless and eyed skulls inside the protected area and equivalent controls outside it. As a nonoperator, try Slimy Eye application and Optometrist removal on all of them. Protected heads, item counts, shears durability, and drops must remain unchanged. Controls outside protection must succeed. In adventure mode, compare items whose `CanPlaceOn` permits the clicked skull with ones that do not, clicking each face. Repeat on both loaders. Keep the current `mayInteract` check until the upstream guarantee is independently established.

## Held eyes and rendering

Hold a Googly Eye in either or both hands. Change between first and third person while it animates. Have another player watch damage and healing, including nonplayer damage and a heal during cooldown. Join tracking during a reaction, unequip/re-equip, change dimensions/worlds, and toggle master/client visibility. Check paused GUI animation, inventory slots, cursor, hotbar, frames, placed heads, boats, and optional Twilight trophies/candles. Reload resources and check baby/adult rabbit, llama, ageable/hierarchical, Twilight, and existing whole-model integrations. First-person head items remain at rest. Verify production-jar startup on both distributions after the client package moves.
