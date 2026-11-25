# Witchery eye placements

The definitions target Just Another Witchery Remake 0.5.40 on NeoForge. Geometry, part hierarchy, active textures, and registry IDs were inspected from the owner-authorized local Witchery JAR. No Minecraft or loader classes were inspected. These are authored placements, not in-game-verified results.

There are 21 enabled definitions: Baba Yaga, Coven Witch, Death, Demon, Elle, Ent, Gulg, Hobgoblin, Horned Huntsman, Hunter, Insanity, Leonard, Lilith, Mandrake, Mog, Nightmare, Owl, Parasitic Louse, Spectral Pig, Vampire, and Werewolf. Mandrake's pair covers the two dark-brown pixels on the small head's -Z face, identified by the owner. Nightmare's socket centers are inferred from its dark face band and especially need visual confirmation. Insanity uses one common head layout for its Creeper, Zombie, Skeleton, and Enderman forms. Mirror Demon, player shells, projectiles, and props have no definitions.

Ent's three wood textures put their natural eyes at different texels. Its common layout uses larger eyes that cover all three pairs, rather than random variants unrelated to the selected wood. Mog and Gulg attach each eye to its dedicated eye joint, past the front of the modeled eyeball. Parasitic Louse uses its small foremost segment. Werewolf's active texture is `werewolf.png`; the differently laid-out grey texture is not selected by the inspected renderer.

Imp has no eye definition because its renderer reflects the model. Banshee, Poltergeist, and Spectre have no definitions because their custom ghost renderer skips normal render layers. This integration changes no rendering code.

## Manual client checks

1. Launch a throwaway MultiMC world with Witchery and its required libraries, then reload the definitions. Confirm the log reports no malformed Witchery definitions. Also launch once without Witchery to check optional loading.
2. Spawn fresh copies of the listed mobs with the eye spawn chance set to full, or apply Slimy Eyes. Check each pair from the front, both sides, and above while the mob turns and animates. Check pupil motion, harvesting, and reapplication.
3. Inspect Alder, Hawthorn, and Rowan Ents separately: both original eye pixels should be covered, and neither googly eye should float off the trunk. Check the Louse's tiny head, Mog/Gulg's protruding eyes, Leonard's snout, Lilith's overlay, Death's hood, and the Huntsman's emissive eye layer for clipping or uncovered original eyes.
4. Check Owl head turns and flight, both Werewolf poses, and any baby forms available in Witchery. Spectral Pig reuses this checkout's ordinary pig age layouts; check its visibility and any age changes.
5. Check client mob/mod hiding, server eye disabling, and entity invisibility. Mandrake should accept Slimy Eyes and put both eyes on its small head, not the larger body root.

Inspection artifacts and texture contact sheets are under the workspace parent's `othersource/witchery-26.1.2-neoforge-inspection/`; the authoring notes are in `othersource/witchery-eye-review.csv`. No build, GameTest, or client launch was run for this change. A server GameTest cannot confirm texture alignment.

Check Mandrake's dark eye pixels are covered as its head animates. In 1.21.1 the same head is the first positional root (`#0`); in 26.1.2 it is `root/head`. Check Insanity in all four mimic forms, including head turns and its Creeper swelling animation; this common layout is not selected by mimic type and may need a shared size or position adjustment. No rendering code is changed.
