# Changelog

26.1 support, for Fabric, NeoForge, and Forge. The 1.20.1, 1.21.1, 1.21.4, 1.21.8, and 1.21.11 builds remain supported.

### Added
- Minecraft 26.1 builds: Fabric (Loader 0.19.5+, Fabric API 0.145.1+), NeoForge 26.1.0.19-beta+, and Forge 62.0.9+. GeckoLib 5.5+ remains optional on Fabric and NeoForge.
- Eyes on a sheep's rear move in to the bare body when it is sheared and back out when the wool regrows.

### Changed
- Java 25 is now required.
- On Forge, GeckoLib-based mobs get no eyes: GeckoLib has no Forge build for Minecraft 26.1.
- Baby mobs whose models Minecraft 26.1 redesigned get eye placements fitted to the new models, scaled from each mob's adult placement. Baby chickens, which no longer have a separate head, carry their eyes on the body above the beak.

### Compatibility
- Worlds saved with the 1.21.11 release load with their eyed mobs, eye items, heads, and settings intact. Worlds from earlier versions must first be opened with the 1.21.11 release.
- Bundled Minecraft eye definitions for mobs whose babies Minecraft 26.1 redesigned select 26.1 and later, with baby placements fitted to the 26.1 baby models; definitions for mobs added after 1.21.4 select the release that introduced the mob and later; the other bundled Minecraft definitions select 1.21.4 and apply through the nearest-version fallback. The mod's data pack declares formats 84.0 through 101.1. Optional-mod definitions keep their earlier version selectors and have not been verified against 26.1 releases of those mods.
