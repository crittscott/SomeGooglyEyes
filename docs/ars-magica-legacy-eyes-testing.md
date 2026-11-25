# Ars Magica: Legacy eye placements

Definitions target the supplied Ars Magica: Legacy 1.5.0 source for Minecraft 1.20.1. Placements were derived from its entity textures, geometry JSON, models, and renderer registrations. They have not been checked in-game.

The seven enabled guardian definitions attach to `head`. Their authored GeckoLib 4 placements use model coordinates with X negated, Y up, and coordinates divided by 16; they do not subtract the bone pivot. The dryad uses the humanoid `head` attachment and the mana creeper uses `root/head`, both in vanilla part coordinates. These source placements and the Ars Magica version label are preserved in this checkout; they do not establish compatibility with later models or GeckoLib versions.

| Mob | Placement basis |
| --- | --- |
| Air guardian | Two blue marks on the front of its seven-pixel head. |
| Arcane guardian | Magenta eyes on the front of the skull inside the hood; the skull is offset from the model center. Texture artwork is four times the geometry UV resolution. |
| Earth guardian | Two green marks across the front of the six-pixel-high head. |
| Ender guardian | Purple eyes above the teeth. Texture artwork is four times the geometry UV resolution. |
| Fire guardian | Red eye patches on opposite sides of the main head cube, behind the snout. Texture artwork is four times the geometry UV resolution. |
| Ice guardian | Two eyes on the upper front head block, above the modeled mouth opening; the texture has no painted eye marks. |
| Nature guardian | Green patches on opposite sides of the head, below the branches. Texture artwork is twice the geometry UV resolution. |
| Dryad | Small eyes low on the face beneath the hair. |
| Mana creeper | Dark eye patches on the front of the vanilla creeper head. |

Life, lightning, and water guardians have disabled definitions. The mage is also disabled because `ClientInit` registers it with `EmptyRenderer`.

## Source-version client checks

These checks apply to the source baseline: use a Forge 1.20.1 MultiMC instance with Ars Magica: Legacy, its dependencies, and the 1.20.1 build of Some Googly Eyes. The supplied Ars Magica source is Forge-only. Runtime availability and placement compatibility on this checkout's Minecraft version are unverified; these bundled definitions do not add a mod dependency. If a compatible Ars Magica release is available, repeat the checks on that release and verify its model attachments and coordinate frames.

1. In a creative world with operator permission, summon each enabled mob with `/summon arsmagicalegacy:<mob_id>` using the underscored names from the definition filenames.
2. Enable the picker with `K` to display configured eyes regardless of spawn chance. Inspect each mob from the front and both sides. Fire and nature guardian eyes should face outward from their side patches; the dryad eyes should sit beneath the hair; the ice guardian eyes should sit above its mouth.
3. Release any picker selection and watch head turns and idle/combat animations. Eyes should remain attached to the head. Check the fire guardian's nearby horns and the nature guardian's branches for occlusion.
4. Summon life, lightning, and water guardians and confirm they remain eyeless. The mage should remain invisible, matching its empty renderer.
5. Run `/reload` and confirm the enabled placements and disabled guardians remain the same.
