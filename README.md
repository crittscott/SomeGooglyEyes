# Some Googly Eyes

A complete rewrite and expansion of iChun's immortal Googly Eyes. Also look to **Regoogly eyes** by EmeryTheModder if you want a pure port of the original.

Now with multiple behaviors. Mobs blink, stare, go cross-eyed, side-eye you, and more.

Eyes are a collectible resource: harvest them, recolor them, brew them into a potion, and put them on other mobs, or yourself.

Compatible with GeckoLib- and Citadel-based mobs. Datapack-aware. In-game configuration tool for adding and configuring eyes.

<!-- TODO: hero GIF or short mp4 clip here — a mob with wobbling eyes sells the whole mod in 5 seconds.
     GitHub plays small mp4/mov files (~10 MB) inline if you drag them into the README editor. -->

<!-- TODO: optional YouTube how-to link, as a clickable thumbnail:
[![Watch the demo](https://img.youtube.com/vi/VIDEO_ID/maxresdefault.jpg)](https://youtu.be/VIDEO_ID)
-->

## Getting eyes

**Dispatch the mob with a direct shears blow**: a chance to drop a googly eye.

**Find the Optometrist enchantment**: Put it on shears; then right-clicking an eyed mob plucks the eye off without harming the mob.

**Customize it in a crafting grid**: eye + any dye sets the iris color, + glowstone dust makes it emit light with dynamic light mods, + redstone turns glow off, + cobweb strips it back to default.

**Brew it**: awkward potion + googly eye → a drinkable **Googly Eyes** potion. The potion inherits the eye's colors.

**Apply it**: drink it to grow your own googly eyes, or throw the splash to give eyes to one lucky mob nearby.

## Configuration

- **Server settings** (`<world>/serverconfig/somegoogly-server.toml`): global and per-mob spawn chances, harvest chance, and which eye expressions play and how often.
- **Client settings** (`config/somegoogly-client.toml`): turn eye rendering off entirely, or hide it for specific mobs or whole mods; purely visual, per-player.

See [docs/configuration.md](docs/configuration.md) for the full reference.

## Pack and mod authors

Eye placements are ordinary datapack JSON. There's also an in-game authoring tool that lets you place, aim, and scale eyes on a live mob in creative mode and export the result as datapack files.

- [docs/datapack-format.md](docs/datapack-format.md) — the eye definition format
- [docs/picker.md](docs/picker.md) — the in-game authoring workflow and `/sg` commands

## Compatibility

Eye placing system works with vanilla models as well as GeckoLib- and Citadel-based models.


## Alpha status

Currently in **alpha release**. Buyer beware. Found a bug or a mob whose eyes sit wrong? [Open an issue](../../issues).

## Credits and license

A port of iChun's **Googly Eyes**, rebuilt for 1.20.1 with new physics, behaviors, and more.

License: **GPL-3.0** — see [LICENSE](LICENSE).
