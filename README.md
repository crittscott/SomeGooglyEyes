# Some Googly Eyes

A rewrite of iChun's immortal Googly Eyes.

Now with multiple behaviors. Mobs blink, stare, go cross-eyed, and side-eye you when you least expect it.

Eyes are a collectible resource: harvest them, recolor them, brew them into a potion, and put them on other mobs — or yourself.

<!-- TODO: hero GIF or short mp4 clip here — a mob with wobbling eyes sells the whole mod in 5 seconds.
     GitHub plays small mp4/mov files (~10 MB) inline if you drag them into the README editor. -->

<!-- TODO: optional YouTube how-to link, as a clickable thumbnail:
[![Watch the demo](https://img.youtube.com/vi/VIDEO_ID/maxresdefault.jpg)](https://youtu.be/VIDEO_ID)
-->

## Getting eyes

Kill the mob with a direct **shears** blow: a chance (default 25%) to drop a googly eye or find the **Optometrist** enchantment (treasure-only: loot chests, fishing, librarian trades) and put it on shears — right-clicking an eyed mob then plucks the eye off *without harming it*.

Customize it in a crafting grid: eye + any **dye** sets the iris color, + **glowstone dust** makes it glow, + **redstone** turns glow off, + **cobweb** strips it back to default.

Brew it: awkward potion + googly eye → a drinkable *Googly Eyes* potion; awkward splash potion + eye → the splash form. The potion inherits the eye's colors.

Apply it: drink it to grow your own googly eyes, or throw the splash to give eyes to one lucky mob nearby.

## Configuration

- **Server settings** (`<world>/serverconfig/somegoogly-server.toml`): global and per-mob spawn chances, harvest chance, and which eye expressions play and how often.
- **Client settings** (`config/somegoogly-client.toml`): turn eye rendering off entirely, or hide it for specific mobs or whole mods — purely visual, per-player.

See [docs/configuration.md](docs/configuration.md) for the full reference.

## For pack and mod authors

Eye placements are ordinary datapack JSON — you can add eyes to modded mobs or reshape the vanilla ones. There's also an in-game authoring tool (the *picker*) that lets you place, aim, and scale eyes on a live mob in creative mode and export the result as datapack files.

- [docs/datapack-format.md](docs/datapack-format.md) — the eye definition format
- [docs/picker.md](docs/picker.md) — the in-game authoring workflow and `/sg` commands

## Compatibility

- **GeckoLib** mobs get eyes via a dedicated integration (optional dependency).
- **Citadel**-based mobs (Alex's Mobs, Ice and Fire, …) are supported through their own model resolver (optional).
- **JEI** shows the potion brewing recipes (optional).


## Alpha status

This is an **alpha release** (0.8.0). Things that may change without migration support: the datapack format, config keys, and network protocol between versions. Found a bug or a mob whose eyes sit wrong? [Open an issue](../../issues).

## Credits and license

A port of iChun's *Googly Eyes* (which was itself a port), rebuilt for 1.20.1 with new physics, behaviors, and the gameplay loop.

License: **GPL-3.0** — see [LICENSE](LICENSE).
