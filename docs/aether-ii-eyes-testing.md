# The Aether II eye placement checks

The bundled `data/aether_ii/eyes/` definitions cover all 35 living entity types registered by The Aether II 1.0-alpha.4.2 for Minecraft 26.1.2, with eyes disabled for Glitterwing, Mimic, Skyroot Lizard, and Slider. They are authored from the Java model parts and texture UVs in `othersource/The-Aether-II-26.1-develop` in the parent project. The version declaration is `[1.0-alpha.4.2,1.1)`; the original Aether's `aether` definitions are independent.

Source inspection checked entity coverage and unambiguous attachment paths for enabled models and ages. JSON structure, numeric bounds, and cross-eye indices were checked separately. No compilation, automated GameTests, or agent-run in-game verification has been performed.

Use a creative MultiMC NeoForge 26.1.2 instance with Some Googly Eyes and The Aether II, plus the latter's dependencies. Give each target eyes with a Slimy Eye so the checks do not depend on the natural spawn roll. Spawn eggs or `/summon aether_ii:<entity path>` can create the targets. Check both ages of breedable animals, and repeat the three biome families for Highfields, Magnetic, and Arctic.

| Targets | Expected layout and checks |
| --- | --- |
| Aerbunny | A small pair on the front of the head. Check sitting, hopping, puffing, and babies. |
| Aerwhale | One eye on each side of the upper head, above the jaw. Check both sides while turning. |
| Flying Cow, Phyg, Sheepuff | A front-facing pair at the texture eyes. Check walking, flight, babies, and Sheepuff wool. |
| Highfields, Magnetic, and Arctic Taegores | A front-facing adult pair and a separate baby pair. Check head motion and ramming. |
| Highfields, Magnetic, and Arctic Burrukais | Adult eyes attach to the sides of `head_rear`; baby eyes attach to the smaller `head`. Check ramming without eyes entering the head plates. |
| Highfields, Magnetic, and Arctic Kirrids | Adult eyes attach to the tilted `head_main_r1`; babies use side-facing eyes on `head`. Check wool, sheared animals, intact/broken plates, eating, jumping, and ramming. |
| Moa | Adult eyes attach to two faces of the rotated `head_front`; babies use their own head layout. Check both sides, head movement, feather/color variants, and the Raptor appearance. |
| Prismallard | A small front pair above the bill. Check swimming and bill movement. |
| Glitterwing, Mimic, Skyroot Lizard, Slider | Eyes are disabled. They should not acquire googly eyes naturally or from a Slimy Eye. |
| Shroudwing | A tiny front pair below the antenna. Check walking, flight, and texture variants. |
| Aechor Plant, Carrion Sprout | Added pairs on the front rim of the body/flower disk; these textures have no existing eyes. Check growth and attack motion. |
| Cockatrice | One eye on each side of the head. Check attack motion and feather clearance. |
| Blue Swet, Golden Swet | One centered front eye on the inner head, at scale 1.7. Check visibility through the gel, jumping, squashing, and size changes. |
| Skephid | Eight eyes: a large lower pair, four smaller upper eyes on the two forward sides, and two on top. Check all eight during movement and attacks. |
| Arkenium Taluton | A front pair with local Y set to -0.15. Check attacks and body movement. |
| Gravitite Taluton | Three eyes at the asymmetric emissive markings on the rotated core. Check visibility around the floating debris and during attacks. |
| Detonation Sentry | One front eye outside the raised shell, at scale 2.0. Check activation and charging. |
| Sentry Golem | A front pair on the small head above the torso. Check melee/ranged forms and attacks. |
| Bladeshroom Hunter | One eye on each side of the narrow head. Check emergence, walking, and attacks. |
| Edward | A front pair below the hat brim. Check head turning and animation. |

Harvest an eyed target with Optometrist shears and reapply its eye: the layout should disappear and return. Reload the datapacks and reconnect to check that definitions still arrive. Watch for missing-attachment or definition-load warnings. Final eye scale, overlap with surrounding geometry, and animated fit require these visual checks.
