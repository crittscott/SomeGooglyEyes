<!-- FIXED HEADER: not content. Do not edit, trim, or count toward the size limit. -->
> **Orientation snapshot, not a specification.** Describes the code as it currently is; the code wins on any disagreement. See CLAUDE.md § Orientation files.
<!-- END FIXED HEADER -->

# Some Googly Eyes — player-facing behavior

`orientation-code.md` covers code structure.

Some Googly Eyes adds animated eyes to living entities. Mobs may appear with eyes, react with expressions, and yield a collectible Googly Eye. Players can customize an eye, embed it in slime, and apply it to a mob or themselves.

This checkout targets Minecraft 1.21.5 as an unreleased waypoint toward 1.21.8, on Fabric, NeoForge, and Forge. Fabric requires Fabric Loader 0.16.14 or newer and Fabric API 0.128.2+1.21.5 or newer. NeoForge requires NeoForge 21.5.97 or newer within the 21.5 release line. Forge requires Forge 55.1.18 or newer within the 55 release line. All loaders require Java 21; GeckoLib 5.1.0 or newer is an optional client dependency. Clients and servers need the same network version.

## Mobs with eyes

By default, eligible mobs have a 5 percent chance to receive eyes when they spawn. Players receive eyes only from a Slimy Eye, and lose them on death or by shearing them off themselves. The ender dragon is excluded. Converted mobs and players leaving the End keep their eyes.

A mob must have an enabled eye definition for its type and current life stage. A baby without a baby definition may gain eyes when it becomes an adult.

Pupils respond to gravity, movement, and head motion. They wobble, bounce, and settle.

Eyed mobs occasionally blink, stare, look aside, or go cross-eyed. They may also react when:

- damaged by a player: 20 percent default chance to bulge;
- milked (cow, mooshroom, goat): bulge;
- sheared (sheep): bulge;
- showing affection (love mode, villagers breeding, a golem's flower, an ocelot's trust): pupils swell to twice their size;
- completing a villager or wandering-trader trade: swirl;
- healed: swirl, 200-tick default cooldown.

One expression plays at a time; all are cosmetic. Sleepers play nothing else; villagers lower tan lids, others squint. Under any expression, a ramming goat squints, a bow drawer closes the eye away from the bow arm, a swelling creeper's pupils rattle faster toward the blast, angry mobs' pupils turn red, and a screaming enderman's pupils throb.

## Obtaining eyes

### Killing with shears

Kill an eyed mob with a direct melee blow from shears for a default 25 percent chance to add one Googly Eye to its drops. A projectile or indirect kill does not qualify. Shears means vanilla shears or any `c:tools/shear` item. A successful harvest costs one shears durability.

### Optometrist

Optometrist is a one-level, treasure-only shears enchantment. Right-click an eyed mob with Optometrist shears to remove its eyes without harming it. One Googly Eye drops, the shears lose one durability, and the standard shearing sound plays. Plain shears retain their usual interactions.

The harvested eye keeps the mob's effective iris color, cornea color, and glow, but not its species or eye placement.

## Googly Eyes and Slimy Eyes

A Googly Eye held in first person is live: its pupil moves, and it plays idle, hurt (bulge), and heal (swirl) expressions under the server's expression settings. Every other view shows it at rest. Its tooltip shows stored colors in hexadecimal and its glow setting.

Worlds saved by the 1.21.1 release open with their eyed mobs, eye items, and settings intact. Worlds from 1.20.1 must first be opened with the 1.21.1 release.

The eye-modifier recipe is shapeless and accepts one Googly Eye and one modifier:

| Modifier | Result |
| --- | --- |
| Vanilla dye | Sets iris color |
| Glowstone dust | Forces glow on |
| Redstone dust | Forces glow off |
| Cobweb | Clears all appearance overrides |

Modifiers stack across crafts. Googly Eyes have no creation recipe; they must be harvested or taken from the creative menu.

Craft a Googly Eye with a slimeball to make a Slimy Eye; it keeps the eye's appearance and any name. Right-click an eligible, eyeless mob to apply it. The mob receives an arrangement from its own definition, with the item's appearance applied to every eye. A slime squelch plays, and the Slimy Eye is consumed unless the player is in creative mode.

An eyed mob or one without a definition for its current life stage refuses the item. Harvest existing eyes before restyling a mob. Sneak-use a Slimy Eye on air to apply it to yourself; another player may also apply one to you, but only where server PvP is on and your teams permit it.

To take your own eyes off, sneak and right-click the air with shears. Optometrist shears do it cleanly; plain shears also cost a melee hit's worth of health, even with PvP off. Either way one Googly Eye drops, the shears lose one durability, and the shearing sound plays.

## Mob heads

Placed skeleton, wither skeleton, zombie, creeper, piglin, and player heads can have googly eyes, laid out and colored by their mob's definition. Dragon and other modded heads can't. An eyed mob's own head, dropped at death (a charged creeper's kill, a wither skeleton's drop), keeps its eyes. A Slimy Eye on an eyeless placed head gives it eyes; Optometrist shears pop one Googly Eye out of an eyed one. Both need build permission there.

A broken eyed head's item keeps its eyes, listed in its tooltip, and restores them when placed. Head eyes settle under gravity and play idle expressions all players see alike, but don't otherwise react. Head items show their eyes at rest; worn heads show none.

## Checking a target

While holding either eye item, sneak and aim at a living entity or placed head to see whether it:

- can receive eyes now;
- already has eyes;
- supports eyes only at another life stage; or
- has no configuration.

The check reaches about 16 blocks. Client settings that hide eyes do not affect the result.

## Creative tab

The Some Googly Eyes tab contains a Googly Eye, a Slimy Eye, and an Optometrist enchanted book. Creative eye items have no appearance overrides.

## Configuration

World settings are stored in:

```text
<world>/serverconfig/somegoogly-server.toml
```

NeoForge and Forge apply edits as soon as they reload the file; Fabric applies them at world start and on `/reload`. A changed `googlyEyesEnabled` reaches connected players right away. Fabric preserves malformed TOML files and adds missing keys without replacing existing values, saving original text as `.bak`.

### Spawn and harvest

| Key | Default | Effect |
| --- | ---: | --- |
| `googlyEyesEnabled` | `true` | Master switch for the whole mod |
| `globalPercent` | `5` | Default spawn chance, 0–100 |
| `harvestOnKillPercent` | `25` | Shears-kill drop chance |
| `entityOverrides` | empty | Entity-specific spawn chances |

While `googlyEyesEnabled` is `false`, no mob rolls eyes, no client renders eyes on any mob or head, and eyes can't be applied or harvested. Eye data is kept and reappears when the switch returns. Automatic mob expressions and anger checks also stop while disabled. The creative picker, an authoring tool, is exempt.

Overrides use `"entity-pattern,percent"`. Exact IDs take priority; otherwise the first matching wildcard is used.

```toml
entityOverrides = [
    "minecraft:zombie,100",
    "*:*_horse,50",
    "minecraft:*,5"
]
```

### Behaviors

| Key | Default | Effect |
| --- | --- | --- |
| `ambientBehaviors` | `true` | Enables idle expressions, on mobs and heads |
| `ambientMinTicks` | `200` | Minimum idle interval |
| `ambientMaxTicks` | `800` | Maximum idle interval |
| `ambientBehaviorPool` | blink, cross-eye, side-eye, stare | Idle expression choices |
| `growOnHitPercent` | `20` | Bulge chance after player damage |
| `growOnMilk` | `true` | Bulge after milking |
| `growOnShear` | `true` | Bulge after shearing |
| `loveOnAffection` | `true` | Love after an affection event |
| `angryEyes` | `true` | Red pupils for attacking or angry mobs |
| `squintOnSleep` | `true` | Sleep squint; villager lids |
| `squintOnRam` | `true` | Ramming goat squint |
| `winkOnAim` | `true` | One-eyed bow aim |
| `rattleOnSwell` | `true` | Creeper rattle |
| `pulseWhenCreepy` | `true` | Enderman pulse |
| `swirlOnTrade` | `true` | Swirl after a trade |
| `swirlOnHeal` | `true` | Swirl after healing |
| `swirlHealCooldownTicks` | `200` | Delay between healing swirls |

Expression IDs (`somegoogly:`): `blink`, `cross_eye`, `side_eye`, `stare`, `grow`, `love`, `swirl`, `color_change`.

`allowSpawnAll` (default `false`) gates `/sg spawnall`; use it only on a throwaway world, since force-spawning some mods' mobs (MineColonies, Create) can corrupt one. `spawnExcludedMods` (default `minecolonies`, `create`) and `spawnExcludedEntities` (default empty) list namespaces and ids that `/sg spawn` and `/sg spawnall` skip.

### Client display

Client settings are stored in:

```text
config/somegoogly-client.toml
```

| Key | Default | Effect |
| --- | --- | --- |
| `disableGooglyEyes` | `false` | Hides all eyes |
| `disabledEntities` | empty | Hides listed entity IDs, and their heads |
| `disabledMods` | empty | Hides entities from listed namespaces, and their heads |

These options affect only display. On NeoForge and Forge they apply as soon as the loader reloads the file; on Fabric they apply at game start. Invisibility also hides eyes.

## Datapacks and compatibility

Eye definitions belong at:

```text
data/<entity namespace>/eyes/<entity path>.json
```

They specify adult and baby arrangements, attachment points, size, position, direction, colors, glow, and weighted variants. Changes take effect on world start or `/reload` and are synchronized to clients.

Definitions are included for Minecraft, Ad Astra, Adorable Hamster Pets, AdventureZ, Alex's Mobs, Ars Elemental, Ars Nouveau, Artifacts, Autumnity, Caverns & Chasms, Critters and Companions, Ecologics, Environmental, EvilCraft, Exotic Birds, Farming for Blockheads, Forbidden Arcanus, Friends & Foes, Hamsters Plus Lite, Ice and Fire Community Edition, Illager Invasion, Immersive Engineering, Let's Do Alpine Whispers, Let's Do Brewery, Let's Do Furniture, Let's Do Meadow, Let's Do Vinery, Living Things, MmmMmmMmmMmm, Mowzie's Mobs, Naturalist, Occultism, Oh The Biomes We've Gone, Productive Bees, Regions Unexplored, Rotten Creatures, Shiny, Simply Cats, Supplementaries, Sushi Go Crafting, The Aether, The Bumblezone, Tiny Skeletons, Twilight Forest, Upgrade Aquatic, Variants & Ventures, and WilderNature. Apart from Ice and Fire Community Edition's, they are unverified against 1.21.5 releases of those mods.

The 78 bundled Minecraft definitions all select 1.21.4 and are enabled, including the player and armor stand. Axolotl, endermite, pufferfish, silverfish, tadpole, and tropical fish have no bundled definition. Unmatched versions use the nearest declared generation. The witch definition uses the range `[1.20.1,)`, so it matches every supported version.

Resource packs replace textures and item models.

## Creative eye picker

The picker is an in-world authoring tool for operators (permission level 2) in creative mode; every `/sg` command and the picker keys refuse anyone else. Its keys are rebindable under **Options > Controls > Some Googly Eyes**.

| Key | Action |
| --- | --- |
| `K` | Toggle the picker |
| `V` | Choose or release the mob under the crosshair |
| `[` | Select previous model part |
| `]` | Select next model part |

While the picker is active, configured mobs display eyes regardless of spawn chance. A chosen mob is frozen, and the HUD and axis gizmo show the current editing state. Preview eyes show their colors and glow, with pupils centered. Releasing it or leaving the server restores its previous AI state. Only one player may edit a mob at a time.

### Workflow

1. Choose a mob with `V` or `/sg choose`.
2. Select an attachment with `[` and `]`, `/sg part <name|number>`, or `/sg list parts`.
3. Create an eye with `/sg create <x> <y> <z>`.
4. Edit it with `/sg move`, `/sg rot`, `/sg posrot`, and `/sg properties`.
5. Save it with `/sg save`; manage eyes with `/sg select`, `/sg dupe`, `/sg delete`, and `/sg list eyes`.
6. Manage arrangements with `/sg variant new`, `/sg variant <n>`, `/sg variant weight <w>`, and `/sg variant del <n>`.
7. Export with `/sg export` or `/sg exportall`.

Use `~` to leave a component unchanged in `move`, `rot`, and `posrot`. Switching away from an unsaved eye discards its edits. Saved arrangements are retained separately for each species and life stage, so adult and baby edits can coexist in one session.

`/sg export` writes the chosen definition into the world's `somegoogly-picker` datapack and reloads datapacks, at most once per player every 10 seconds; a definition too large to send is refused.

`/sg exportall` writes all loaded definitions and session edits to:

```text
<game directory>/somegoogly-export/data/<namespace>/eyes/<entity path>.json
```

It does not change the world. Both exports replace authored ages and preserve the other loaded age definitions. Both exports declare Minecraft definitions with the exact game version and other mods' definitions with a range spanning that mod's current minor release.

### Test mobs

`/sg spawn <entity type>` creates one persistent, frozen mob at the targeted block.

`/sg spawnall [namespace]` builds an audit grid for all available types, optionally restricted to a namespace. It overwrites blocks, has no undo, and is disabled by default; use it only in a disposable test world.

Both spawn commands skip the ender dragon, entities that cannot be summoned, and any entity whose id or namespace is listed in the server config's `spawnExcludedEntities` / `spawnExcludedMods`. A spawn another mod's listener cancels is not placed, and a refused bulk spawn does not change its cell's terrain. A namespace-restricted `/sg spawnall` reports each type it skipped and why, and `/sg spawn` suggests only types it would spawn.

Move or rotate the chosen mob with `/sg mob move <dx> <dy> <dz>` and `/sg mob rot <azimuth>`.

## Admin commands

`/sg admin` requires operator permission level 2 and creative mode. Aim at a living entity within about 20 blocks.

| Command | Effect |
| --- | --- |
| `/sg admin eyes <true|false>` | Toggles eyes |
| `/sg admin tint iris <r> <g> <b>` | Sets iris color |
| `/sg admin tint cornea <r> <g> <b>` | Sets cornea color |
| `/sg admin tint clear` | Clears color overrides |
| `/sg admin glow <on|off|config>` | Sets glow or restores the definition's value |
| `/sg admin behavior <id|random>` | Plays an expression |

Color channels range from 0 to 1.

## Limitations

- A harvested eye keeps one appearance, not each eye's.
- Axolotl, endermite, pufferfish, silverfish, tadpole, and tropical fish do not have bundled eye definitions.
- Baby scaling, model variants, or updated third-party models may misplace eyes until their definitions are adjusted.
- Fabric contains 131 dedicated-server GameTests; NeoForge and Forge each contain 129. Physical checks cover production artifacts, rendering, interactions, picker export, reloads, GeckoLib, and real-save migration.
- The dynamic modifier recipe may not display in recipe viewers.
