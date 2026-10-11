# Changelog

1.21.4 support, for Fabric, NeoForge, and Forge. The 1.20.1 and 1.21.1 builds remain supported.

### Added
- Minecraft 1.21.4 builds: Fabric (Loader 0.16.14+, Fabric API 0.119.4+), NeoForge 21.4.158+, and Forge 54.1.18+. GeckoLib 4.8.5+ remains optional.
- Eye definition for the creaking.
- Eyes now survive mob conversion (a cured zombie villager, a sheared mooshroom) and a player leaving the End. They are still lost on player death.
- Players now see their own eyes react to expressions.
- Shears from other mods in the `c:tools/shear` tag work wherever vanilla shears do, through the new `#somegoogly:shears` tag.
- Every server config key is written with an explanatory comment.
- Picker hints and HUD show your current key bindings instead of the default keys.

### Changed
- The creative eye picker and all its `/sg` commands now require operator permission level 2 as well as creative mode, because picker edits don't fire protection events.
- Removing your own eyes with plain shears now deals damage even where PvP or team rules would block it.
- The Slimy Eye recipe is a vanilla transmute recipe, so the Slimy Eye keeps every property of the eye, including its name.
- `/sg spawn` and `/sg spawnall` now fire the loader's normal spawn events, so a spawn another mod cancels is not placed. The ender dragon is no longer offered as a spawn suggestion.
- On NeoForge, server config edits now take effect as soon as the file reloads, as on Forge. On Fabric, the server config is re-read on `/reload`.
- On NeoForge and Forge, turning `googlyEyesEnabled` on or off now reaches connected players immediately.
- Client config edits that hide eyes apply without a resource reload.
- On NeoForge and Forge, the client config section is now named `client`, matching Fabric.
- Fabric servers now refuse a client without the mod before it joins.
- Fabric eye items can no longer be used by spectators.
- Picker preview eyes glow like real eyes.
- Living Things eye definitions attach to that mod's renamed `root` model part.
- Problems with the config files, picker spawns, and eye definitions are now logged at warning level.
- Network version is now 14; clients and servers must both run this version.

### Compatibility
- Worlds saved with the 1.21.1 release load with their eyed mobs, eye items, and settings intact. Worlds from 1.20.1 must first be opened with the 1.21.1 release.
- Bundled Minecraft eye definitions select 1.21.4, and the mod's data pack uses pack format 61. Optional-mod definitions keep their earlier version selectors and, apart from the Living Things attach points, have not been verified against 1.21.4 releases of those mods.
