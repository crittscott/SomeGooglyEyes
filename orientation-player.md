<!-- FIXED HEADER: not content. Do not edit, trim, or count toward the size limit. -->
> **Orientation snapshot, not a specification.** Describes the code as it currently is; the code wins on any disagreement. See CLAUDE.md § Orientation files.
<!-- END FIXED HEADER -->

# Some Googly Eyes — player-facing behavior

What a player currently observes. `orientation-code.md` covers the code structure.

Some Googly Eyes adds animated googly eyes to living entities. Mobs may appear with eyes, react with expressions, and yield a collectible Googly Eye. Players can customize an eye, embed it in slime, and apply it to a mob or themselves.

The mod adds two items, one enchantment, and one creative tab. Eye placement comes from datapacks, allowing different eye arrangements for vanilla and modded mobs.

Artifacts target Minecraft 1.21.4 on Fabric, NeoForge, and Forge. Fabric requires Fabric Loader 0.16.14 or newer and Fabric API 0.119.4+1.21.4 or newer. NeoForge requires NeoForge 21.4.158 or newer within the 21.4 release line. Forge requires Forge 54.1.18 or newer within the 54 release line. All loaders require Java 21; GeckoLib 4.8.5 or newer is an optional client dependency. Multiplayer clients and servers must both have Some Googly Eyes with the same network version.

## Mobs with eyes

By default, eligible non-player mobs have a 5 percent chance to receive eyes when they spawn. Changing the server chance affects only new mobs. Players receive eyes only from a Slimy Eye, and lose them on death or by shearing them off themselves. The ender dragon cannot have eyes.

A mob must have an enabled eye definition for its type and current life stage. A baby without a baby definition may gain eyes when it becomes an adult.

Pupils respond to gravity, movement, and head motion. They wobble, bounce, and settle.

Eyed mobs occasionally blink, stare, look aside, or go cross-eyed. They may also react when:

- damaged by a player: 20 percent default chance to bulge;
- completing a villager or wandering-trader trade: swirl;
- healed: swirl, with a 200-tick default cooldown.

Only one expression plays at a time, and expressions are cosmetic.

## Obtaining eyes

### Killing with shears

Kill an eyed mob with a direct melee blow from shears for a default 25 percent chance to add one Googly Eye to its drops. A projectile or indirect kill does not qualify. A successful harvest costs one shears durability.

### Optometrist

Optometrist is a one-level, treasure-only shears enchantment. Right-click an eyed mob with Optometrist shears to remove its eyes without harming it. One Googly Eye drops, the shears lose one durability, and the standard shearing sound plays. Plain shears retain their usual interactions.

The harvested eye keeps the mob's effective iris color, cornea color, and glow, but not its species or eye placement.

## Googly Eyes and Slimy Eyes

A Googly Eye is a 3D item whose pupil moves while you hold it in first person; other views show it at rest. Its tooltip shows stored colors in hexadecimal and its glow setting.

Worlds saved by the 1.21.1 release open with their eyed mobs, eye items, and settings intact. Worlds from 1.20.1 must first be opened with the 1.21.1 release.

The eye-modifier recipe is shapeless and accepts one Googly Eye and one modifier:

| Modifier | Result |
| --- | --- |
| Vanilla dye | Sets iris color |
| Glowstone dust | Forces glow on |
| Redstone dust | Forces glow off |
| Cobweb | Clears all appearance overrides |

Modifiers may be applied in successive crafting operations. Googly Eyes have no creation recipe; they must be harvested or taken from the creative menu.

Craft a Googly Eye with a slimeball to make a Slimy Eye; it keeps the eye's appearance and any name. Right-click an eligible, eyeless mob to apply it. The mob receives an arrangement from its own definition, with the item's appearance applied to every eye. A slime squelch plays, and the Slimy Eye is consumed unless the player is in creative mode.

An eyed mob or one without a definition for its current life stage refuses the item. Harvest existing eyes before restyling a mob. Sneak-use a Slimy Eye on air to apply it to yourself; another player may also apply one to you, but only where server PvP is on and your teams permit it.

To take your own eyes off, sneak and right-click the air with shears. Optometrist shears do it cleanly; plain shears also cost you a normal melee hit's worth of health, even where PvP is off. Either way one Googly Eye drops, the shears lose one durability, and the shearing sound plays.

While holding either eye item, sneak and aim at a living entity to see whether it:

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

NeoForge and Forge apply edits as soon as they reload the file; Fabric applies them at world start and on `/reload`. A changed `googlyEyesEnabled` reaches connected players right away. Fabric's reader accepts basic and literal strings, logging any value it cannot read.

### Spawn and harvest

| Key | Default | Effect |
| --- | ---: | --- |
| `googlyEyesEnabled` | `true` | Master switch for the whole mod |
| `globalPercent` | `5` | Default spawn chance, 0–100 |
| `harvestOnKillPercent` | `25` | Shears-kill drop chance |
| `entityOverrides` | empty | Entity-specific spawn chances |

While `googlyEyesEnabled` is `false`, no new mob rolls eyes, no client renders any eyes on any mob (old or new), and eyes can't be hand-applied or harvested. Existing eye data is kept, not erased, and reappears once the switch is turned back on. The creative eye picker is exempt, since it is an authoring tool rather than gameplay.

Overrides use `"entity-pattern,percent"`. Exact IDs take priority; otherwise the first matching wildcard is used.

```toml
entityOverrides = [
    "minecraft:zombie,100",
    "*:*_horse,50",
    "minecraft:*,5"
]
```

### Expressions

| Key | Default | Effect |
| --- | --- | --- |
| `ambientBehaviors` | `true` | Enables idle expressions |
| `ambientMinTicks` | `200` | Minimum idle interval |
| `ambientMaxTicks` | `800` | Maximum idle interval |
| `ambientBehaviorPool` | blink, cross-eye, side-eye, stare | Idle expression choices |
| `growOnHitPercent` | `20` | Bulge chance after player damage |
| `swirlOnTrade` | `true` | Swirl after a trade |
| `swirlOnHeal` | `true` | Swirl after healing |
| `swirlHealCooldownTicks` | `200` | Delay between healing swirls |

Behavior IDs use the `somegoogly:` namespace. Available behaviors are `blink`, `cross_eye`, `side_eye`, `stare`, `grow`, `swirl`, and `color_change`.

`allowSpawnAll` defaults to `false` and must be enabled before `/sg spawnall` can run. Enable it only on a test or throwaway world: `/sg spawnall` force-spawns every summonable mob, including ones, like MineColonies' and Create's, that can corrupt or destabilize a world when spawned outside their mod's normal context. `spawnExcludedMods` and `spawnExcludedEntities` list namespaces and entity ids that `/sg spawn` and `/sg spawnall` skip. `spawnExcludedMods` defaults to `minecolonies` and `create` for exactly this reason — removing either from the list on a world you care about risks that world; `spawnExcludedEntities` defaults to empty. Both affect only those authoring commands.

### Client display

Client settings are stored in:

```text
config/somegoogly-client.toml
```

| Key | Default | Effect |
| --- | --- | --- |
| `disableGooglyEyes` | `false` | Hides all eyes |
| `disabledEntities` | empty | Hides listed entity IDs |
| `disabledMods` | empty | Hides entities from listed namespaces |

These options affect only display. On NeoForge and Forge they apply as soon as the loader reloads the file; on Fabric they apply at game start. Invisibility also hides eyes.

## Datapacks and compatibility

Eye definitions belong at:

```text
data/<entity namespace>/eyes/<entity path>.json
```

They specify adult and baby arrangements, attachment points, size, position, direction, colors, glow, and weighted variants. Changes take effect on world start or `/reload` and are synchronized to clients.

Definitions are included for Minecraft, Ad Astra, Adorable Hamster Pets, AdventureZ, Alex's Mobs, Ars Elemental, Ars Nouveau, Artifacts, Autumnity, Critters and Companions, EvilCraft, Exotic Birds, Farming for Blockheads, Forbidden Arcanus, Friends & Foes, Hamsters Plus Lite, Ice and Fire Community Edition, Illager Invasion, Immersive Engineering, Let's Do Alpine Whispers, Let's Do Brewery, Let's Do Furniture, Let's Do Meadow, Let's Do Vinery, Living Things, MmmMmmMmmMmm, Mowzie's Mobs, Naturalist, Occultism, Oh The Biomes We've Gone, Productive Bees, Regions Unexplored, Rotten Creatures, Shiny, Simply Cats, Supplementaries, Sushi Go Crafting, The Aether, The Bumblezone, Tiny Skeletons, Twilight Forest, Variants & Ventures, and WilderNature. Optional mods are not required. Except for Ice and Fire Community Edition, whose definitions select that mod's 1.21.1 releases, the optional-mod definitions retain their earlier compatibility selectors and have not been verified against Minecraft 1.21.4 releases. Updates to another mod's models may require its eye definitions to be adjusted.

The 77 bundled Minecraft definitions select 1.21.4. Armadillo, bogged, and breeze do not yet have bundled eye geometry.

Resource packs can replace eye textures and item models. The mod has no JEI plugin; the Slimy Eye recipe is normally discoverable, but the dynamic modifier recipe may not display usefully.

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

Use `~` to leave a component unchanged in `move`, `rot`, and `posrot`. Switching away from an unsaved eye discards its edits.

`/sg export` writes the chosen definition into the world's `somegoogly-picker` datapack and reloads datapacks. It is limited to one successful export per player every 10 seconds, and a definition too large to send is refused with a message.

`/sg exportall` writes all loaded definitions and session edits to:

```text
<game directory>/somegoogly-export/data/<namespace>/eyes/<entity path>.json
```

It does not change the world. Both exports declare Minecraft definitions with the exact game version and other mods' definitions with a range spanning that mod's current minor release.

### Test mobs

`/sg spawn <entity type>` creates one persistent, frozen mob at the targeted block.

`/sg spawnall [namespace]` builds an audit grid for all available types, optionally restricted to a namespace. It overwrites blocks, has no undo, and is disabled by default. Use it only in a disposable test world: force-spawning mobs like MineColonies' or Create's outside their mod's normal context can corrupt or destabilize the world, which is why both are excluded by default (see `spawnExcludedMods` above).

Both spawn commands skip the ender dragon, entities that cannot be summoned, and any entity whose id or namespace is listed in the server config's `spawnExcludedEntities` / `spawnExcludedMods`. A spawn another mod's listener cancels is not placed. A namespace-restricted `/sg spawnall` reports each type it skipped and why, and `/sg spawn` suggests only types it would spawn.

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

- Applied player eyes are lost on death.
- Harvested items keep one appearance shared by all eyes, not each eye's separate appearance.
- The ender dragon cannot receive eyes.
- Armadillo, bogged, and breeze do not have bundled eye definitions.
- Baby scaling, changing model variants, or updated third-party models may cause misplaced or missing eyes until their definitions are adjusted.
- Optional-mod definitions and client-side model attachment on 1.21.4 require manual compatibility verification.
- Fabric contains 103 dedicated-server GameTests; NeoForge and Forge each contain 99. Each loader requires production-build verification and physical-client smoke testing of ordinary and baby models, players, special resolver families, expression and pupil animation, both item render paths, harvesting and application, picker editing/export, renderer reload, optional GeckoLib entities, and that `googlyEyesEnabled=false` actually stops rendering on an already-connected client while still letting the picker preview. Migration from an actual 1.21.1 save also requires physical verification.
- The dynamic modifier recipe may not display in recipe viewers.
