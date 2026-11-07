# Changelog

1.21.11 support, for Fabric, NeoForge, and Forge. The 1.20.1, 1.21.1, 1.21.4, and 1.21.8 builds remain supported.

### Added
- Minecraft 1.21.11 builds: Fabric (Loader 0.18.4+, Fabric API 0.141.6+), NeoForge 21.11.48+, and Forge 61.1.5+. GeckoLib 5.4.5+ remains optional.
- Eye definitions for the camel husk, mannequin, and parched, matching the camel, player, and skeleton placements, and for the copper golem, nautilus (adult and baby), and zombie nautilus.
- Eyes on a sheep's rear move in to the bare body when it is sheared and back out when the wool regrows.

### Fixed

- Use the clicked head for Adventure-mode item permissions. Applying and harvesting eyes count toward item-use statistics.
- Apply PvP and team rules to eye harvesting. Honor spectator and mob-drop rules, and harvest armor stands before their equipment interaction on Forge and NeoForge.
- Settle animated pupils using the submitted eye pose. Keep eye motion across settings-only updates and identical state packets.
- Complete missing Fabric config keys before applying the settings, and check the definition sync limit before writing picker exports.

### Compatibility

- Multiplayer uses protocol 18 across Fabric, Forge, and NeoForge. Servers and clients need matching versions.
- Worlds saved with the 1.21.8 release load with their eyed mobs, eye items, heads, and settings intact. Worlds from earlier versions must first be opened with the 1.21.8 release.
- Bundled Minecraft eye definitions for mobs added after 1.21.4 select the release that introduced the mob and later; the others select 1.21.4 and apply through the nearest-version fallback. The mod's data pack declares formats 64 through 94. Optional-mod definitions keep their earlier version selectors and have not been verified against 1.21.11 releases of those mods.
