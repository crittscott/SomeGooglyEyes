<!-- FIXED HEADER: not content. Do not edit, trim, or count toward the size limit. -->
> **Orientation snapshot, not a specification.** Describes the code as it currently is; the code wins on any disagreement. See CLAUDE.md § Orientation files.
<!-- END FIXED HEADER -->

# Some Googly Eyes — player-facing behavior

`orientation-code.md` covers code structure.

Some Googly Eyes adds animated eyes to mobs, players, heads, and vanilla boats. Harvest eyes, customize their appearance, and apply them with slime.

This checkout targets Minecraft 26.1, on Fabric, NeoForge, and Forge. Fabric requires Fabric Loader 0.19.5 or newer and Fabric API 0.145.1+26.1 or newer. NeoForge requires NeoForge 26.1.0.19-beta or newer below 26.1.1. Forge requires Forge 62.0.9 or newer within the 62 release line. All loaders require Java 25. GeckoLib 5.5+ is optional on Fabric and NeoForge. Clients and servers need the same network version.

## Mobs with eyes

By default, eligible mobs have a 5 percent chance to receive eyes when they spawn. Players receive eyes only from a Slimy Eye, and lose them on death or by shearing them off themselves. The ender dragon is excluded. Converted mobs and players leaving the End keep their eyes.

A mob must have an enabled eye definition for its type and current life stage. A baby without a baby definition may gain eyes when it becomes an adult.

Pupils respond to gravity, movement, and head motion.

A sheared sheep's body eyes sit on its bare body until the wool regrows.

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

Harvested eyes keep effective iris/cornea colors and glow, not species or placement.

## Googly Eyes and Slimy Eyes

First-person loose eyes react to movement/hurt/heal; third-person eyes use world gravity and holder motion. Frames, GUI, and third-person hands animate independently; paired heads may cross-eye. GUI runs while paused. Frame eyes are 3x. Ambient-off keeps physics; global switches gate animation.

Worlds saved by the 1.21.11 release open with their eyed mobs, eye items, and settings intact. Worlds from earlier versions must first be opened with the 1.21.11 release.

The eye-modifier recipe is shapeless and accepts one Googly Eye and one modifier:

| Modifier | Result |
| --- | --- |
| Vanilla dye | Sets iris color |
| Glowstone dust | Forces glow on |
| Redstone dust | Forces glow off |
| Cobweb | Clears all appearance overrides |

Modifiers stack. Googly Eyes come from harvesting or creative.

Picking up a Googly Eye unlocks the Slimy Eye recipe. Craft an eye with a slimeball to make a Slimy Eye retaining its appearance and name. Apply it to an eligible eyeless mob: its configured eyes receive the item's appearance, a squelch plays, and one item is consumed outside creative.

Harvest before restyling. Sneak-use on air applies to yourself. Applying or taking another player’s eyes requires level PvP and team permission.

To take your own eyes off, sneak and right-click the air with shears. Optometrist shears do it cleanly; plain shears also cost a melee hit's worth of health, even with PvP off. Either way one Googly Eye drops, the shears lose one durability, and the shearing sound plays.

## Mob heads

Placed skeleton, wither skeleton, zombie, creeper, piglin, and player heads can have googly eyes, laid out and colored by their mob's definition. Dragon and other modded heads can't. An eyed mob's own head, dropped at death (a charged creeper's kill, a wither skeleton's drop), keeps its eyes. A Slimy Eye on an eyeless placed head gives it eyes; Optometrist shears pop one Googly Eye out of an eyed one. Both need use permission at the clicked head, including the item's Adventure-mode block rule.

A broken eyed head's item keeps its eyes, listed in its tooltip, and restores them when placed. Head eyes settle under gravity and play synchronized idle expressions. Cross-eye requires a paired layout. GUI/framed/third-person held heads animate; ground/first-person heads rest; worn heads show none.

Vanilla boats and rafts start eyeless. Slimy Eyes add two outward-facing eyes. Optometrist shears remove them and drop one eye for one durability. Eyes survive saves. Using eye items takes priority over boarding or opening cargo. Boat pupils respond to gravity and movement.

## Checking a target

While holding either eye item, sneak and aim at a living entity, vanilla boat, or placed head to see whether it:

- can receive eyes now;
- already has eyes;
- supports eyes only at another life stage; or
- has no configuration.

The check reaches about 16 blocks. Client settings that hide eyes do not affect the result.

## Creative tab

The Some Googly Eyes tab contains both eye items and an Optometrist book.

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

While `googlyEyesEnabled` is `false`, natural rolls, rendering, application, harvest, automatic expressions, and anger checks stop. Saved eyes return when enabled. The creative picker is exempt.

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

Definitions are included for Minecraft, Ad Astra, Adorable Hamster Pets, AdventureZ, Alex's Mobs, Ars Elemental, Ars Nouveau, Artifacts, Autumnity, Caverns & Chasms, Critters and Companions, Ecologics, Environmental, EvilCraft, Exotic Birds, Farming for Blockheads, Forbidden Arcanus, Friends & Foes, Hamsters Plus Lite, Ice and Fire Community Edition, Illager Invasion, Immersive Engineering, Let's Do Alpine Whispers, Let's Do Brewery, Let's Do Furniture, Let's Do Meadow, Let's Do Vinery, Living Things, MmmMmmMmmMmm, Mowzie's Mobs, Naturalist, Occultism, Oh The Biomes We've Gone, Productive Bees, Regions Unexplored, Rotten Creatures, Shiny, Simply Cats, Supplementaries, Sushi Go Crafting, The Aether, The Bumblezone, Tiny Skeletons, Twilight Forest, Upgrade Aquatic, Variants & Ventures, and WilderNature. They are unverified against 26.1 releases.

The 85 bundled Minecraft definitions are all enabled, including the player and armor stand. The 38 for mobs whose babies 26.1 redesigned select `[26.1,)` and add baby placements, mobs added after 1.21.4 select a range starting at the release that introduced them, the witch's selects `[1.20.1,)`, and the rest select 1.21.4 through the nearest-version fallback. Axolotl, endermite, pufferfish, silverfish, tadpole, and tropical fish have no bundled definition.

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
- GameTest totals are in `orientation-code.md`. Rendering, interactions, boats, protection, exports, retracking, and GeckoLib require client checks.
- The modifier recipe may be absent from recipe viewers.
