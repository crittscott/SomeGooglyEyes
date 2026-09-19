# Some Googly Eyes

A complete rewrite and expansion of [iChun's Googly Eyes](https://www.curseforge.com/minecraft/mc-mods/googlyeyes). (Also look to [Regoogly eyes](https://www.curseforge.com/minecraft/mc-mods/regoogly-eyes) by EmeryTheModder if you want a pure port of the original.)

Now with multiple behaviors: mobs blink, stare, go cross-eyed, side-eye you, and more.

Eyes are a collectible resource: harvest them, recolor them, set them in a slimeball and stick them on other mobs, or yourself.

Includes attachment support for GeckoLib-, Citadel-, and Uranus-based mobs, as well as those using LLibrary. Datapack-aware: In-game configuration tool for adding and configuring eyes and exporting as a datapack.

![Googly Eyes splash](docs/googlyeyes-splash.png)

![Loaders: Fabric + NeoForge + Forge](https://img.shields.io/badge/Loaders-Fabric%20%2B%20NeoForge%20%2B%20Forge-5C7C8A?style=for-the-badge) ![Minecraft: 1.20.1, 1.21.1](https://img.shields.io/badge/Minecraft-1.20.1%20and%201.21.1-8A5A9B?style=for-the-badge)

## Getting eyes

**Dispatch the mob with a direct shears blow**: there's a chance to drop a googly eye.

**Find the Optometrist enchantment**: Put it on shears; then right-clicking an eyed mob plucks the eye off without harming the mob.

**Customize it in a crafting grid**: eye + any dye sets the iris color, + glowstone dust makes it visible in darkness, + redstone turns glow off, + cobweb strips it back to default.

**Set it in slime**: googly eye + slimeball → a **Slimy Eye**, which inherits the eye's colors.

**Apply it**: right-click a mob with the slimy eye to give it eyes, or sneak and use it to grow your own. Each application picks the mob's eye arrangement anew. A mob that already has eyes refuses it. Pluck them off first to restyle.

## Eye potential
Don't waste your slimy eye: hold an eye and sneak while targeting a mob to see whether that mob can have eyes at all.

## Configuration

- **Server settings** (`<world>/serverconfig/somegoogly-server.toml`): global and per-mob spawn chances, harvest chance, and which eye expressions play and how often.
- **Client settings** (`config/somegoogly-client.toml`): turn eye rendering off entirely, or hide it for specific mobs or whole mods; purely visual, per-player.

See [docs/configuration.md](docs/configuration.md) for the full reference.

## Pack and mod authors

Eye placements are ordinary datapack JSON. There's also an in-game authoring tool that lets you place, aim, and scale eyes on a live mob in creative mode and export the result as datapack files.

- [docs/datapack-format.md](docs/datapack-format.md) — the eye definition format
- [docs/picker.md](docs/picker.md) — the in-game authoring workflow and `/sg` commands

## Compatibility

The eye placing system supports vanilla models as well as GeckoLib-, Citadel-, and Uranus-based models, including those that use legacy LLibrary code.

Ships with predefined eye configs for:

- Minecraft
- Abnormals Autumnity
- Abnormals Caverns & Chasms
- Abnormals Environmental
- Abnormals Upgrade Aquatic
- Ad Astra
- Adorable Hamster Pets
- AdventureZ
- Alex's Mobs
- Ars Elemental
- Ars Nouveau
- Artifacts
- Critters and Companions
- Ecologics
- EvilCraft
- Exotic Birds
- Farming for Blockheads
- Forbidden Arcanus
- Friends & Foes
- Hamsters Plus Lite
- Ice and Fire Community Edition
- Illager Invasion
- Immersive Engineering
- Let's Do Alpine Whispers
- Let's Do Brewery
- Let's Do Furniture
- Let's Do Meadow
- Let's Do Vinery
- Living Things
- MmmMmmMmmMmm
- Mowzie's Mobs
- Naturalist
- Occultism
- Oh The Biomes We've Gone
- Productive Bees
- Regions Unexplored
- Rotten Creatures
- Shiny
- Simply Cats
- Supplementaries
- Sushi Go Crafting
- The Aether
- The Bumblezone
- Tiny Skeletons
- Twilight Forest
- Variants & Ventures
- WilderNature

Use the in-game eye config system to generate custom datapacks. Submissions for future releases appreciated! Find a mob that doesn't behave? [Open an issue](../../issues).

Available for Fabric, NeoForge, and Forge on Minecraft 1.21.1 and 1.20.1.

## Credits and license

A port of iChun's **Googly Eyes**, rebuilt for 1.21.1 and 1.20.1 with new physics, behaviors, and more.

License: [**GPL-3.0**](LICENSE).
