# Changelog

1.21.8 support, for Fabric, NeoForge, and Forge. The 1.20.1, 1.21.1, and 1.21.4 builds remain supported.

### Added
- Minecraft 1.21.8 builds: Fabric (Loader 0.16.14+, Fabric API 0.136.1+), NeoForge 21.8.57+, and Forge 58.1.26+. GeckoLib 5.2.2+ remains optional.
- Eye definition for the happy ghast, matching the ghast's placement.
- Eyes on a sheep's rear move in to the bare body when it is sheared and back out when the wool regrows.

### Changed
- On Forge, the picker HUD is registered as a Forge HUD layer.

### Fixed
- Optometrist follows PvP and team rules. Spectators cannot change placed heads or shear themselves. Adventure restrictions use the clicked head.
- Shears-kill harvesting honors `doMobLoot`. Optometrist handles an armor stand before its equipment interaction on Forge and NeoForge. Successful Slimy Eye use records item statistics.
- On NeoForge, shears that break during an eye harvest fire the item-destroyed event.
- Display pupils follow world gravity independently of the camera and settle when rebuilt. Settings sync preserves existing eye motion.
- The Slimy Eye recipe unlocks when a player obtains a Googly Eye. Villager trades trigger the eye reaction on Fabric, and healing at full health leaves eyes alone.
- Placed heads discard unrelated data from their eye-state compound. Picker exports check the full definition set against the sync limit.

### Compatibility
- Network protocol 18 is required on both sides. Fabric reports missing or mismatched clients during configuration.
- Worlds saved with the 1.21.4 release load with their eyed mobs, eye items, heads, and settings intact. Worlds from earlier versions must first be opened with the 1.21.4 release.
- Bundled Minecraft eye definitions select 1.21.4, except the happy ghast's, which selects 1.21.6 and later. The mod's data pack uses pack format 81. Optional-mod definitions keep their earlier version selectors and have not been verified against 1.21.8 releases of those mods.
