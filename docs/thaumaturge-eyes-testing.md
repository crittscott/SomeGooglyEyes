# Thaumaturge eye placement checks

The bundled `data/thaumaturge/eyes/` definitions cover the 43 living entity types registered in the inspected Thaumaturge sources: 31 have eyes and 12 are disabled. Placements target Thaumaturge 1.1.x (`[1.1,1.2)`), using `othersource/Thaumaturge-1.21.1` (1.1.1) and `othersource/Thaumaturge-26.1.2` (1.1.0) in the parent project. Java geometry, texture UVs, and the actual PNGs supplied the placements. The ordinary crossbow turret has different geometry and attachment names in the two Minecraft versions; each checkout carries its own placement.

Inhabited zombies, cultist clerics, eldritch guardians, eldritch wardens, taint seeds, and prime taint seeds are explicitly disabled. The small construct golem, advanced crossbow turret, wisps, taint swarms, and both cultist portals are also disabled: their renderers bypass the existing living-model eye layer. The legacy tainted animals use vanilla renderers and retain this checkout's vanilla layouts, including baby layouts where present. Spell bats use the modern bat layout.

The model-part resolver honors visibility along the entire attachment chain. The eldritch golem therefore shows its head eye while headed and its exposed core eye while headless. `skipDraw` is deliberately ignored: Thaumaturge supplies its glowing core geometry through a separate material layer.

JSON fields, numeric bounds, cross-eye indices, living-entity coverage, and custom model attachment paths were checked by source inspection and a Python data check. Compilation, GameTests, and in-game verification have not been run. Vanilla bat and silverfish placements depend on the existing bat definition and the expected vanilla silverfish `segment0` geometry respectively; their animated fit still needs visual confirmation.

Use a creative MultiMC instance for this checkout with Some Googly Eyes, Thaumaturge, and its dependencies. Give targets eyes with a Slimy Eye so the checks do not depend on natural spawn rolls. Use spawn eggs or `/summon thaumaturge:<entity path>`, then inspect the placements while the target moves and attacks.

| Targets | Expected layout and checks |
| --- | --- |
| Brainy zombie, brainy drowned, brainy husk, giant brainy zombie | A front pair. Check babies, giant scaling, and the drowned's outer layer. |
| Cultist knight, leader, ranger, ranger captain | A pair in front of the helmet faceplate or visor. Check turning, attacks, and spawned armor; eyes must remain visible through the armor geometry. |
| Pech | A pair below the brow beside the nose. Check all three skins, head turns, and jaw motion. |
| Eldritch crab | One central turquoise texture eye covered by a googly eye. Check armored and bare forms and head motion. |
| Eldritch golem | One eye over the low head socket; after the head is lost, one larger eye on the rotated exposed core. The hidden head must leave no floating eye, and the exposed core eye must be absent before that phase. |
| Eldritch hierophant | One large eye on the modeled eye inside the hood. Check casting, movement, and death. |
| Firebat and spell bat | A small pair on the head. Check flight and hanging/resting poses. |
| Mind spider | The vanilla spider arrangement. Check that its existing viewer restriction and translucency still apply. |
| Thaumic slime | The vanilla slime pair inside the translucent shell. Check several sizes and squishing. |
| Taint crawler | A small pair on the front segment. Check crawling and segment motion. |
| Taintacle, small taintacle, giant taintacle | One eye on each of the four textured sides of the tip core. Check the openings between the lobes during swaying, strikes, emergence, and death. |
| Taint spore and taint spore swarmer | A pair on each pod's front membrane. Check growth, breathing, release, and the swarmer's separate glowing core layer. |
| Arcane bore | One eye on the forward tip. Check aiming, recoil, spin, and digging effects. |
| Ordinary crossbow turret | A front pair on the aiming assembly. Check tracking, firing, reloading, and minecart mounting in each version. |
| Legacy taint spider, creeper, cow, pig, chicken, sheep, villager | Vanilla layouts. These registry entries are not normally summonable; inspect any retained legacy entities, including babies and sheep wool changes, where available. |
| Disabled mobs | A Slimy Eye must refuse application; no natural eyes appear. |

Harvest an eyed target with Optometrist shears and reapply its eye. Reload the datapacks and reconnect to check definition delivery. Watch for missing-attachment or definition-load warnings. Final scale, armor overlap, animated fit, and the eldritch golem's phase transition require these in-game checks.
