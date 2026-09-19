# Changelog

1.21.1 support, for Fabric, NeoForge, and Forge.

### Added
- Many mods: Abnormals Caverns & Chasms, Abnormals Environmental, Abnormals Upgrade Aquatic, Ad Astra, Adorable Hamster Pets, AdventureZ, Ars Elemental, Artifacts, Critters and Companions, Ecologics, EvilCraft, Forbidden Arcanus, Friends & Foes, Illager Invasion, Let's Do Alpine Whispers, Let's Do Brewery, Let's Do Furniture, Let's Do Meadow, Let's Do Vinery, Living Things, MmmMmmMmmMmm, Naturalist, Occultism, Oh The Biomes We've Gone, Productive Bees, Regions Unexplored, Rotten Creatures, Shiny, Supplementaries, The Aether, The Bumblezone, Tiny Skeletons, Variants & Ventures, WilderNature

- NeoForge and Forge builds alongside Fabric, produced from one shared codebase.
- Per-world server settings at `<world>/serverconfig/somegoogly-server.toml`, loaded through each loader's native configuration facility and kept isolated between worlds.
- Standard interaction and shearing game events now fire for every successful eye action, so other mods and observers respond as they would to vanilla shearing.
- Eye interactions on other players' entities and in claimed areas are gated by operator, PvP, and protection-mod checks.

### Changed
- Ported from Minecraft 1.20.1 to 1.21.1: item data moves from NBT to data components, and enchantment, loot, and rendering integration follow the 1.21.1 APIs.
- Architectury API is no longer a runtime dependency on any loader.
