# Changelog

26.1.1 support, for Fabric, NeoForge, and Forge. The 1.20.1, 1.21.1, 1.21.4, 1.21.8, 1.21.11, and 26.1 builds remain supported.

### Added
- Minecraft 26.1.1 builds: Fabric (Loader 0.19.5+, Fabric API 0.145.4+), NeoForge 26.1.1.15-beta+, and Forge 63.0.2+. GeckoLib remains optional on Fabric and NeoForge; GeckoLib has no 26.1.1 release, so its 26.1 build (5.5+) is the one to try.
- Eyes on a sheep's rear move in to the bare body when it is sheared and back out when the wool regrows.

### Compatibility
- Worlds saved with the 26.1 release load with their eyed mobs, eye items, heads, and settings intact. Worlds from earlier versions must first be opened with the 26.1 release.
- On Forge, GeckoLib-based mobs still get no eyes.
- Bundled Minecraft eye definitions for mobs whose babies Minecraft 26.1 redesigned select 26.1 and later, with baby placements fitted to the 26.1 baby models; definitions for mobs added after 1.21.4 select the release that introduced the mob and later; the other bundled Minecraft definitions select 1.21.4 and apply through the nearest-version fallback. The mod's data pack declares formats 84.0 through 101.1. Optional-mod definitions have not been verified against 26.1.1 releases of those mods.
