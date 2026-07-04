# Some Googly Eyes

Googly eyes for (nearly) every mob in Minecraft. Eyes wobble with real physics — pupils swing, bounce off the rim, and settle toward true down no matter how the mob's head tilts. Mobs blink, stare, go cross-eyed, and side-eye you when you least expect it.

A Forge mod for **Minecraft 1.20.1**.

<!-- TODO: hero GIF or short mp4 clip here — a mob with wobbling eyes sells the whole mod in 5 seconds.
     GitHub plays small mp4/mov files (~10 MB) inline if you drag them into the README editor. -->

<!-- TODO: optional YouTube how-to link, as a clickable thumbnail:
[![Watch the demo](https://img.youtube.com/vi/VIDEO_ID/maxresdefault.jpg)](https://youtu.be/VIDEO_ID)
-->

## What it does

- A small percentage of mobs (configurable, default 2%) spawn with googly eyes, placed per-species by data-driven configs — over 80 vanilla mobs are covered out of the box, including multi-eyed arrangements and weighted placement variants.
- Eyes are simulated, not animated: each pupil is a tiny physics object that reacts to the mob's movement and head turns, and rests at the bottom of the eye like the real toy.
- Eyed mobs play little expressions: ambient blinks, stares, side-eyes, and crossed eyes; they *grow* wide when you hit them and *swirl* when traded with or healed.
- Eyes are a collectible resource with a full gameplay loop: harvest them, recolor them, brew them into a potion, and put them on other mobs — or yourself.

## The gameplay loop

1. **Find an eyed mob.** They're rare by default. Look closely.
2. **Harvest the eye.**
   - Kill the mob with a direct **shears** blow: a chance (default 25%) to drop a googly eye.
   - Or find the **Optometrist** enchantment (treasure-only: loot chests, fishing, librarian trades) and put it on shears — right-clicking an eyed mob then plucks the eye off *without harming it*.
3. **Customize it** in a crafting grid: eye + any **dye** sets the iris color, + **glowstone dust** makes it glow, + **redstone** turns glow off, + **cobweb** strips it back to default.
4. **Brew it**: awkward potion + googly eye → a drinkable *Googly Eyes* potion; awkward splash potion + eye → the splash form. The potion inherits the eye's colors.
5. **Apply it**: drink it to grow your own googly eyes, or throw the splash to give eyes to one lucky mob nearby.

## Installation

1. Install [Minecraft Forge](https://files.minecraftforge.net/) **47.4.0 or newer** for Minecraft **1.20.1**.
2. Download the mod jar from the [Releases](../../releases) page.
3. Drop it into your `mods` folder.

Works in single-player out of the box. The mod is built to run on servers as well (all gameplay decisions are server-authoritative), but dedicated-server use hasn't been broadly tested in this alpha — reports welcome.

## Configuration

- **Server settings** (`<world>/serverconfig/somegoogly-server.toml`): global and per-mob spawn chances, harvest chance, and which eye expressions play and how often.
- **Client settings** (`config/somegoogly-client.toml`): turn eye rendering off entirely, or hide it for specific mobs or whole mods — purely visual, per-player.

See [docs/configuration.md](docs/configuration.md) for the full reference.

## For pack and mod authors

Eye placements are ordinary datapack JSON — you can add eyes to modded mobs or reshape the vanilla ones without touching code. There's also an in-game authoring tool (the *picker*) that lets you place, aim, and scale eyes on a live mob in creative mode and export the result as datapack files.

- [docs/datapack-format.md](docs/datapack-format.md) — the eye definition format
- [docs/picker.md](docs/picker.md) — the in-game authoring workflow and `/sg` commands

## Compatibility

- **GeckoLib** mobs get eyes via a dedicated integration (optional dependency).
- **Citadel**-based mobs (Alex's Mobs, Ice and Fire, …) are supported through their own model resolver (optional).
- **JEI** shows the potion brewing recipes (optional).
- The ender dragon is the one mob that can never have eyes — its renderer can't host them.

## Alpha status

This is an **alpha release** (0.8.0). Things that may change without migration support: the datapack format, config keys, and network protocol between versions. Found a bug or a mob whose eyes sit wrong? [Open an issue](../../issues).

## Credits and license

A port of iChun's *Googly Eyes* (which was itself a port), rebuilt for 1.20.1 with new physics, behaviors, and the gameplay loop.

License: **All Rights Reserved**. You may not redistribute the mod or its source without permission.
