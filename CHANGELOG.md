# Changelog

Notable changes to Some Googly Eyes.

Each Minecraft version is maintained on its own permanent branch; `main` is not the release branch.

## [0.8.2] — 2026-09-07

First public release. Minecraft 1.21.1, for Fabric, NeoForge, and Forge.

### Added
- NeoForge and Forge builds alongside Fabric, produced from one shared codebase.
- Per-world server settings at `<world>/serverconfig/somegoogly-server.toml`, loaded through each
  loader's native configuration facility and kept isolated between worlds.
- Standard interaction and shearing game events now fire for every successful eye action, so other
  mods and observers respond as they would to vanilla shearing.
- Interaction sounds for eye actions.
- Eye interactions on other players' entities and in claimed areas are gated by operator, PvP, and
  protection-mod checks.
- GameTest coverage across the server subsystems.

### Changed
- Ported from Minecraft 1.20.1 to 1.21.1: item data moves from NBT to data components, and
  enchantment, loot, and rendering integration follow the 1.21.1 APIs.
- Networking rebuilt on each loader's native payload system.
- Architectury API is no longer a runtime dependency on any loader; it is used only at build time.
  Fabric API is still required. GeckoLib remains an optional client-side dependency (>= 4.7.4).

### Client
- Client settings are unchanged: `config/somegoogly-client.toml`.

### Notes
- Bundled eye definitions for third-party mods carry over their 1.20.1 version selectors. Where a
  mod changed its models on 1.21.1, placements may need adjustment; report mismatches on the issue
  tracker.

## [0.8.1] — 2026-08-23

Internal pre-release on the 1.20.1 line (Fabric only). Not published.
