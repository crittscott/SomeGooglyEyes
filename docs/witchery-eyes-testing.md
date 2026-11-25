# Witchery eye placements

The definitions cover the inspected Just Another Witchery Remake 0.5.40 NeoForge and 0.3.7 Fabric JARs. The configured mobs share their geometry across these builds. NeoForge adds Gulg, Hobgoblin, Hunter, Leonard, and Mog. These are authored placements, not in-game-verified results.

There are 21 enabled definitions for the NeoForge roster: Baba Yaga, Coven Witch, Death, Demon, Elle, Ent, Gulg, Hobgoblin, Horned Huntsman, Hunter, Insanity, Leonard, Lilith, Mandrake, Mog, Nightmare, Owl, Parasitic Louse, Spectral Pig, Vampire, and Werewolf. Mandrake's pair covers the two dark-brown pixels on the small head's -Z face, identified by the owner. Nightmare's socket centers are inferred from its dark face band and require visual confirmation. Insanity uses one common head layout for its Creeper, Zombie, Skeleton, and Enderman forms. Mirror Demon, player shells, projectiles, and props have no definitions.

Ent uses a common trunk layout sized to cover the different original eye locations on Alder, Hawthorn, and Rowan textures. Mog and Gulg use their dedicated eye joints; Parasitic Louse uses its foremost small segment. Nightmare's model keeps no common named root, so its first reflected root (`#0`) selects the head. Other authored paths use stable descendant names. The active Werewolf renderer selects `werewolf.png`, not the differently laid-out grey texture.

Imp has no eye definition because its renderer reflects the model. Banshee, Poltergeist, and Spectre have no definitions because their custom ghost renderer skips normal render layers. This integration changes no rendering code.

## Manual client checks

1. Test separate MultiMC instances with Witchery 0.5.40 on NeoForge and 0.3.7 on Fabric, including each build's required libraries. Confirm the definitions reload without errors. Also launch without Witchery.
2. Spawn fresh mobs with full eye chance, or apply Slimy Eyes. Inspect from the front, sides, and above during idle, head turns, walking, flight, and attacks. Check harvesting and reapplication. Inspect every enabled species present in that loader's Witchery build.
3. Check all three Ent wood textures for covered original eye pixels. Check Mog/Gulg's modeled eyes, the tiny Louse head, Leonard's snout, Lilith's inflated overlay, Death's hood, the Huntsman's separate emissive eye layer, and Nightmare's `#0` attachment.
4. Check Owl turns/flight, Werewolf poses, and any available babies. Spectral Pig reuses the checkout's ordinary pig age layouts. Check its visibility separately.
5. Test client mob/mod hiding, server eye disabling, and invisibility. Mandrake should accept Slimy Eyes and put both eyes on its small head, not the larger body root.

Model listings, geometry summaries, and textures are in the workspace parent's `othersource/witchery-1.21.1-{neoforge,fabric}-inspection/`; `othersource/witchery-eye-review.csv` records the placement decisions. Only the owner-authorized Witchery JARs were inspected. No build, GameTest, or client launch was run. Client texture alignment needs the checks above.

Check Mandrake's dark eye pixels are covered as its head animates. In 1.21.1 the same head is the first positional root (`#0`); in 26.1.2 it is `root/head`. Check Insanity in all four mimic forms, including head turns and its Creeper swelling animation; this common layout is not selected by mimic type and may need a shared size or position adjustment. No rendering code is changed.
