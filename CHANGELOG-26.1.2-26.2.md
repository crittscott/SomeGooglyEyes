# Changelog

26.2 support, for Fabric, NeoForge, and Forge. The 1.20.1, 1.21.1, 1.21.4, 1.21.8, 1.21.11, 26.1, 26.1.1, and 26.1.2 builds remain supported.

### Added
- Minecraft 26.2 builds: Fabric (Loader 0.19.5+, Fabric API 0.161.0+), NeoForge 26.2.0.57+, and Forge 65.1.5+. GeckoLib 5.5.5+ remains optional.
- Eye definitions for the sulfur cube, adult and baby.
- Eyes on a sheep's rear move in to the bare body when it is sheared and back out when the wool regrows.

### Fixed
- Use the clicked head for Adventure-mode item permissions. Applying and harvesting eyes count toward item-use statistics.
- Apply PvP and team rules to eye harvesting. Honor spectator and mob-drop rules, and harvest armor stands before their equipment interaction on Forge and NeoForge.
- Settle animated pupils using the submitted eye pose. Keep eye motion across settings-only updates and identical state packets.
- Complete missing Fabric config keys before applying the settings, and check the definition sync limit before writing picker exports.

### Compatibility
- Multiplayer uses protocol 18 across Fabric, Forge, and NeoForge. Servers and clients need matching versions.
- Worlds saved with the 26.1.2 release load with their eyed mobs, eye items, heads, and settings intact. Worlds from earlier versions must first be opened with the 26.1.2 release.
- Bundled Minecraft eye definitions for mobs whose babies Minecraft 26.1 redesigned select 26.1 and later, with baby placements fitted to the 26.1 baby models; definitions for mobs added after 1.21.4 select the release that introduced the mob and later; the other bundled Minecraft definitions select 1.21.4 and apply through the nearest-version fallback. The mod's data pack declares formats 88.0 through 107.1. Optional-mod definitions have not been verified against 26.2 releases of those mods.
