# Some Googly Eyes

Some Googly Eyes adds animated googly eyes to living entities. Mobs may appear with eyes, react with
expressions, and yield a collectible Googly Eye. Players can customize an eye, embed it in slime, and
apply it to a mob or themselves.

The mod adds two items, one enchantment, and one creative tab. Eye placement comes from datapacks,
allowing different eye arrangements for vanilla and modded mobs.

Completed artifacts target Minecraft 1.21.1 on Fabric, NeoForge, and Forge. Fabric requires Fabric
Loader 0.19.3 or newer, Fabric API 0.116.15+1.21.1 or newer, and Architectury API 13.0.8 or newer.
NeoForge requires NeoForge 21.1.248 or newer within the 21.1 release line and Architectury API
13.0.8 or newer. Forge requires Forge 52.1.16 or newer within the 52 release line and does not
require Architectury API. All loaders require Java 21; GeckoLib 4.7.4 or newer is an optional client
dependency.

## Mobs with eyes

By default, eligible non-player mobs have a 5 percent chance to receive eyes when they spawn.
Changing the server chance affects only new mobs. Players receive eyes only from a Slimy Eye, and
lose them on death or by shearing them off themselves. The ender dragon cannot have eyes.

A mob must have an enabled eye definition for its type and current life stage. A baby without a baby
definition may gain eyes when it becomes an adult.

Pupils respond to gravity, movement, and head motion. They wobble, bounce, and settle.

Eyed mobs occasionally blink, stare, look aside, or go cross-eyed. They may also react when:

- damaged by a player: 20 percent default chance to bulge;
- completing a villager or wandering-trader trade: swirl;
- healed: swirl, with a 200-tick default cooldown.

Only one expression plays at a time, and expressions are cosmetic.

## Obtaining eyes

### Killing with shears

Kill an eyed mob with a direct melee blow from shears for a default 25 percent chance to add one
Googly Eye to its drops. A projectile or indirect kill does not qualify. A successful harvest costs
one shears durability.

### Optometrist

Optometrist is a one-level, treasure-only shears enchantment. Right-click an eyed mob with
Optometrist shears to remove its eyes without harming it. One Googly Eye drops and the shears lose one
durability. Plain shears retain their usual interactions.

The harvested eye keeps the mob's effective iris color, cornea color, and glow, but not its species
or eye placement.

## Googly Eyes and Slimy Eyes

A Googly Eye is a 3D item whose pupil moves while held. Its tooltip shows stored colors in
hexadecimal and its glow setting.

The eye-modifier recipe is shapeless and accepts one Googly Eye and one modifier:

| Modifier | Result |
| --- | --- |
| Vanilla dye | Sets iris color |
| Glowstone dust | Forces glow on |
| Redstone dust | Forces glow off |
| Cobweb | Clears all appearance overrides |

Modifiers may be applied in successive crafting operations. Googly Eyes have no creation recipe;
they must be harvested or taken from the creative menu.

Craft a Googly Eye with a slimeball to make a Slimy Eye. Right-click an eligible, eyeless mob to
apply it. The mob receives an arrangement from its own definition, with the item's appearance applied
to every eye. The Slimy Eye is consumed unless the player is in creative mode.

An eyed mob or one without a definition for its current life stage refuses the item. Harvest existing
eyes before restyling a mob. Sneak-use a Slimy Eye on air to apply it to yourself; another player may
also apply one to you, but only where server PvP is on and your teams permit it.

To take your own eyes off, sneak and right-click the air with shears. Optometrist shears do it
cleanly; plain shears also cost you a normal melee hit's worth of health. Either way one Googly Eye
drops and the shears lose one durability.

While holding either eye item, sneak and aim at a living entity to see whether it:

- can receive eyes now;
- already has eyes;
- supports eyes only at another life stage; or
- has no configuration.

The check reaches about 16 blocks. Client settings that hide eyes do not affect the result.

## Creative tab

The Some Googly Eyes tab contains a Googly Eye, a Slimy Eye, and an Optometrist enchanted book.
Creative eye items have no appearance overrides.

## Configuration

World settings are stored in:

```text
<world>/serverconfig/somegoogly-server.toml
```

### Spawn and harvest

| Key | Default | Effect |
| --- | ---: | --- |
| `googlyEyesEnabled` | `true` | Enables eyes on new mobs |
| `globalPercent` | `5` | Default spawn chance, 0–100 |
| `harvestOnKillPercent` | `25` | Shears-kill drop chance |
| `entityOverrides` | empty | Entity-specific spawn chances |

Overrides use `"entity-pattern,percent"`. Exact IDs take priority; otherwise the first matching
wildcard is used.

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
| `ambientMinTicks` | `100` | Minimum idle interval |
| `ambientMaxTicks` | `400` | Maximum idle interval |
| `ambientBehaviorPool` | blink, cross-eye, side-eye, stare | Idle expression choices |
| `growOnHitPercent` | `20` | Bulge chance after player damage |
| `swirlOnTrade` | `true` | Swirl after a trade |
| `swirlOnHeal` | `true` | Swirl after healing |
| `swirlHealCooldownTicks` | `200` | Delay between healing swirls |

Behavior IDs use the `somegoogly:` namespace. Available behaviors are `blink`, `cross_eye`,
`side_eye`, `stare`, `grow`, `swirl`, and `color_change`.

`allowSpawnAll` defaults to `false` and must be enabled before `/sg spawnall` can run.

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

These options affect only display. Re-enabling a previously disabled entity or namespace may require
a resource reload or restart. Invisibility also hides eyes.

## Datapacks and compatibility

Eye definitions belong at:

```text
data/<entity namespace>/eyes/<entity path>.json
```

They specify adult and baby arrangements, attachment points, size, position, direction, colors,
glow, and weighted variants. Changes take effect on world start or `/reload` and are synchronized to
clients.

Definitions are included for Minecraft, Alex's Mobs, Ars Nouveau, Autumnity, Exotic Birds, Farming for
Blockheads, Hamsters, Ice and Fire, Immersive Engineering, Mowzie's Mobs, Simply Cats, Sushi Go
Crafting, and Twilight Forest. Optional mods
are not required. The optional-mod definitions retain their earlier compatibility selectors and have
not been verified against Minecraft 1.21.1 releases. Updates to another mod's models may require its
eye definitions to be adjusted.

The 74 bundled Minecraft definitions select 1.21.1. Armadillo, bogged, and breeze do not yet have
bundled eye geometry.

Resource packs can replace eye textures and item models. The mod has no JEI plugin; the Slimy Eye
recipe is normally discoverable, but the dynamic modifier recipe may not display usefully.

## Creative eye picker

The picker is an in-world authoring tool for creative players. Its keys are rebindable under
**Options > Controls > Some Googly Eyes**.

| Key | Action |
| --- | --- |
| `K` | Toggle the picker |
| `V` | Choose or release the mob under the crosshair |
| `[` | Select previous model part |
| `]` | Select next model part |

While the picker is active, configured mobs display eyes regardless of spawn chance. A chosen mob is
frozen, and the HUD and axis gizmo show the current editing state. Releasing it or leaving the server
restores its previous AI state. Only one player may edit a mob at a time.

### Workflow

1. Choose a mob with `V` or `/sg choose`.
2. Select an attachment with `[` and `]`, `/sg part <name|number>`, or `/sg list parts`.
3. Create an eye with `/sg create <x> <y> <z>`.
4. Edit it with `/sg move`, `/sg rot`, `/sg posrot`, and `/sg properties`.
5. Save it with `/sg save`; manage eyes with `/sg select`, `/sg dupe`, `/sg delete`, and
   `/sg list eyes`.
6. Manage arrangements with `/sg variant new`, `/sg variant <n>`, `/sg variant weight <w>`, and
   `/sg variant del <n>`.
7. Export with `/sg export` or `/sg exportall`.

Use `~` to leave a component unchanged in `move`, `rot`, and `posrot`. Switching away from an unsaved
eye discards its edits.

`/sg export` writes the chosen definition into the world's `somegoogly-picker` datapack and reloads
datapacks. It is limited to one successful export per player every 10 seconds.

`/sg exportall` writes all loaded definitions and session edits to:

```text
<game directory>/somegoogly-export/data/<namespace>/eyes/<entity path>.json
```

It does not change the world.

### Test mobs

`/sg spawn <entity type>` creates one persistent, frozen mob at the targeted block.

`/sg spawnall [namespace]` builds an audit grid for all available types, optionally restricted to a
namespace. It overwrites blocks, has no undo, and is disabled by default. Use it only in a disposable
world.

Move or rotate the chosen mob with `/sg mob move <dx> <dy> <dz>` and
`/sg mob rot <azimuth>`.

## Admin commands

`/sg admin` requires operator permission level 2 and creative mode. Aim at a living entity within
about 20 blocks.

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
- Baby scaling, changing model variants, or updated third-party models may cause misplaced or missing
  eyes until their definitions are adjusted.
- Optional-mod definitions and client-side model attachment on 1.21.1 require manual compatibility
  verification.
- Fabric, NeoForge, and Forge pass production builds and all 101 dedicated-server GameTests. Each
  loader still requires physical-client smoke testing of ordinary and baby models, players, special
  resolver families, expression and pupil animation, both item render paths, harvesting and
  application, picker editing/export, renderer reload, and optional GeckoLib entities.
- The dynamic modifier recipe may not display in recipe viewers.
