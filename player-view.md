# Some Googly Eyes - as-built player-facing description

This describes the mod's current observable behavior. It is not a design specification; where it
disagrees with the code, the code is authoritative.

Some Googly Eyes adds animated googly eyes to living entities. Mobs may spawn with eyes, react with
expressions, and drop a collectible Googly Eye. Players can change an eye's color or glow, embed it in
slime, and apply it to another mob or themselves. Eye placement is supplied by datapacks, so supported
vanilla and modded mobs can have different eye counts and arrangements.

The mod adds two items, one enchantment, and one creative tab. It adds no blocks or creatures of its
own.

## Which mobs get eyes

By default, each eligible non-player living entity has a 5 percent chance to receive eyes when it is
first initialized. The decision is made once and remains attached to that entity through save/load,
dimension changes, and aging. Changing the server chance affects new entities, not existing ones.

A mob is eligible only when the server has a usable eye definition for its type. Definitions can be
specific to adults or babies. A baby with only an adult definition may already have a positive stored
eye roll but shows no eyes until it grows up.

Players never receive eyes automatically. A Slimy Eye can give them eyes. Player eye state is not
copied to the new player entity after death, so those eyes are lost on respawn.

The ender dragon is always excluded, even if a datapack tries to define it.

## How the eyes move

Each pupil has local googly physics. It falls toward real world-down, wobbles in response to the
entity's movement and head motion, bounces against the eye's edge, and settles when motion stops.
Off-screen eyes are not simulated.

Eyed mobs also play short expressions. By default, an idle mob occasionally blinks, stares, looks to
the side, or goes cross-eyed. Cross-eyed movement appears only where the mob's eye definition pairs
the relevant eyes.

Other reactions are event-driven:

- A player damaging an eyed mob has a default 20 percent chance to make its eyes bulge.
- Completing a trade with an eyed villager or wandering trader makes its pupils swirl.
- Healing an eyed mob makes its pupils swirl, with a default 200-tick cooldown.

Only one expression plays at a time. A new trigger is ignored while one is active. These animations
are cosmetic; they do not affect AI, combat, trading, or other game mechanics.

## Getting a Googly Eye

### Killing with shears

Kill an eyed living entity with a direct melee blow from shears. By default there is a 25 percent
chance to add one Googly Eye to its normal drops. A projectile, indirect kill, or switching away from
the shears does not qualify. The shears take one durability on a successful harvest.

The result is always one item, no matter how many eyes appeared on the entity.

### Optometrist harvesting

Optometrist is a rare, one-level, treasure-only shears enchantment. It does not appear in the
enchanting table, but it can occur through treasure-enchantment sources such as enchanted books in
loot, fishing, and librarian trades.

Right-click an eyed living entity with Optometrist shears to pluck its eyes off without damaging it.
The entity loses its visible eyes, one Googly Eye drops beside it, and the shears take one durability.
Plain shears do not perform this right-click harvest and retain their ordinary interactions with
sheep, mooshrooms, and other shearable mobs.

The harvested item captures the entity's effective first-eye appearance: iris color, cornea color,
and glow after any entity-wide override. It does not retain the mob type or eye placement.

## The Googly Eye item

The Googly Eye is a 3D item using the same model as eyes on mobs. Its pupil reacts to the holder's
movement and look direction while held. It is centered in inventories, item frames, and dropped-item
rendering.

Its tooltip lists any stored appearance fields as hexadecimal iris/cornea colors and a glow value.
The item can stack only with other eyes whose normal item data is compatible.

A plain Googly Eye is an ingredient and inspection tool. Right-clicking a mob with it does not apply
eyes.

## Customizing an eye

The eye-modifier recipe is shapeless and accepts exactly one Googly Eye plus exactly one modifier.

| Modifier | Result |
| --- | --- |
| Any vanilla dye | Sets the iris to that dye's color |
| Glowstone dust | Forces glow on |
| Redstone dust | Forces glow off |
| Cobweb | Clears iris, cornea, and glow overrides back to target-config defaults |

Modifiers can be applied over several crafting operations. A dye changes only the iris, and
glowstone or redstone changes only glow. The cobweb is the full reset.

There is no normal crafting recipe that creates a new Googly Eye. It must be harvested, obtained from
a treasure source only if another mod or pack adds one, or taken from the creative menu.

## Crafting and using a Slimy Eye

Craft one Googly Eye with one slimeball, shapelessly, to make a Slimy Eye. The result carries the
Googly Eye's stored appearance.

Right-click an eligible, eyeless living entity with a Slimy Eye to apply it. The item:

- consumes one Slimy Eye in non-creative play;
- gives the target eyes immediately;
- applies the item's iris, cornea, and glow overrides to all of that target's eyes;
- rolls a fresh weighted eye-placement variant from the target's datapack definition.

The item carries appearance, not geometry. A dyed eye therefore keeps its color when moved between
mobs, but each target uses its own configured number, size, position, direction, and arrangement.

An already-eyed target or a target with no usable definition at its current age refuses the item and
consumes nothing. To restyle an eyed mob, first harvest its eye, modify the item, craft another Slimy
Eye, and apply it again.

The Slimy Eye owns the entity right-click while held. It applies before a horse, villager, pet, or
other mob can perform its usual right-click response.

Sneak-use the Slimy Eye on air to apply it to yourself. This is the normal self-application path for
player eyes. Another player can also right-click you with one.

## Checking eye compatibility

Hold either a Googly Eye or Slimy Eye, sneak, and aim at a living entity. The action bar reports one
of four outcomes:

- the target can have eyes now;
- the target already has eyes;
- the type supports eyes only at the other life stage;
- the target is not configured.

The check reaches about 16 blocks, so it can be used before approaching a dangerous mob. It reports
server-synced eligibility and current eye state. Local client settings that hide eyes do not change
the verdict or prevent application.

## Creative menu

The Some Googly Eyes tab contains:

- Googly Eye
- Slimy Eye
- an enchanted book with Optometrist

Fresh creative-menu eye items have no appearance override: they use white corneas, black irises, and
no forced glow unless a target's datapack defaults say otherwise.

## Server configuration

Each world uses:

```text
<world>/serverconfig/somegoogly-server.toml
```

### Spawn and harvest settings

| Key | Default | Effect |
| --- | ---: | --- |
| `googlyEyesEnabled` | `true` | Stops new spawn decisions when false; existing eyes remain |
| `globalPercent` | `5` | Default chance from 0 to 100 |
| `harvestOnKillPercent` | `25` | Successful-eye-drop chance for a direct shears kill |
| `entityOverrides` | empty | Exact or wildcard entity-specific chances |

Each override is written as `"entity-pattern,percent"`. Exact ids always beat wildcard rules. If no
exact id matches, the first matching wildcard line wins; otherwise the global percentage is used.

```toml
entityOverrides = [
    "minecraft:zombie,100",
    "*:*_horse,50",
    "minecraft:*,5"
]
```

### Behavior settings

| Key | Default | Effect |
| --- | --- | --- |
| `ambientBehaviors` | `true` | Enables idle expressions |
| `ambientMinTicks` | `100` | Minimum idle interval |
| `ambientMaxTicks` | `400` | Maximum idle interval |
| `ambientBehaviorPool` | blink, cross-eye, side-eye, stare | Eligible idle expressions |
| `growOnHitPercent` | `20` | Bulge chance when damaged by a player |
| `swirlOnTrade` | `true` | Swirl after a completed trade |
| `swirlOnHeal` | `true` | Swirl when healed |
| `swirlHealCooldownTicks` | `200` | Per-mob delay between heal-triggered swirls |

Behavior ids use the `somegoogly:` namespace. The complete built-in set is `blink`, `cross_eye`,
`side_eye`, `stare`, `grow`, `swirl`, and `color_change`. The last three are not in the default
ambient pool; `grow` and `swirl` have their event triggers, while `color_change` is normally reachable
only through the admin command.

### Picker safety setting

`allowSpawnAll` defaults to `false`. It must be explicitly enabled before creative users can run the
destructive `/sg spawnall` audit grid.

## Client configuration

Each player has:

```text
config/somegoogly-client.toml
```

| Key | Default | Effect |
| --- | --- | --- |
| `disableGooglyEyes` | `false` | Hides all eyes on this client |
| `disabledEntities` | empty | Hides exact entity ids |
| `disabledMods` | empty | Hides every entity in listed namespaces |

These options are visual only. Hidden mobs still have server-side eyes, react, can be harvested, and
remain visible to other players. Invisibility also hides the added eyes.

Turning rendering off is observed by the live render gate. If an entity or namespace was disabled
when render layers were installed, re-enabling it may require a client resource reload or restart to
install the omitted layer.

## Datapacks and compatibility

Eye definitions live at:

```text
data/<entity namespace>/eyes/<entity path>.json
```

They determine eligibility, adult/baby placement, weighted variants, attachment part, eye count,
position, size, depth, aim, cross-eye partners, default colors, and glow. The server loads them on
world start and `/reload`, selects a definition compatible with the installed version of the owning
mod, and syncs the selected data to clients.

If no exact version entry matches, the nearest authored generation is used and the mismatch is
logged. A stale definition may therefore remain visible but sit incorrectly after an upstream model
change. Setting `enabled` to false prevents the entity type from receiving or accepting eyes.

The mod ships 243 definitions for:

- Minecraft
- Alex's Mobs
- Ars Nouveau
- Autumnity
- Exotic Birds
- Hamsters
- Ice and Fire
- Immersive Engineering
- Mowzie's Mobs
- Simply Cats
- Twilight Forest

It supports ordinary vanilla model families plus compatibility paths for GeckoLib, Citadel, and the
legacy LLibrary model toolkit used by some Mowzie's Mobs entities. These integrations do not make the
other mods mandatory. Model names, bones, and third-party render APIs can change, so a definition may
need to be re-authored after updating another mod.

Resource packs can replace the item and eye textures or item models. The mod ships no JEI plugin.
The Slimy Eye recipe has declared ingredients and can be discovered normally; the dynamic eye
modifier recipe may not be listed usefully by recipe viewers.

## The creative eye picker

The picker is an in-world authoring tool, not a survival mechanic. Every picker action requires
creative mode, checked again by the server for actions that affect the world. It works from a remote
client when the server also runs the mod.

### Keyboard controls

The default keys are rebindable under Options -> Controls -> Some Googly Eyes.

| Key | Action |
| --- | --- |
| `K` | Turn the picker on or off |
| `V` | Choose or release the living entity under the crosshair |
| `[` | Previous model part |
| `]` | Next model part |

While active, configured mobs show their eyes regardless of their spawn roll. The selected mob is
frozen with AI disabled, a HUD shows the current mob/variant/part/eyes, and a colored axis gizmo marks
the selected part. Releasing the mob, leaving the server, or stopping the server restores its prior
AI state. A second player cannot edit the same frozen mob at once.

### Authoring workflow

Eye edits use `/sg` commands:

1. Choose a target with `V` or `/sg choose`.
2. Select an attachment using `[` and `]`, `/sg part <name|number>`, or inspect names with
   `/sg list parts`.
3. Create a draft with `/sg create <x> <y> <z>`.
4. Adjust it with `/sg move`, `/sg rot`, `/sg posrot`, and `/sg properties ...`.
5. Commit with `/sg save`; use `/sg select`, `/sg dupe`, `/sg delete`, and `/sg list eyes` to manage
   saved eyes.
6. Use `/sg variant new`, `/sg variant <n>`, `/sg variant weight <w>`, and
   `/sg variant del <n>` for alternative weighted arrangements.
7. Export with `/sg export` or `/sg exportall`.

`~` leaves a component unchanged in eye `move`, `rot`, and `posrot`. The property commands edit eye
scale, iris scale, depth, cornea color, iris color, glow, and cross-eye target. A cross-eye target must
be another saved eye attached to the same part.

Switching away from an unsaved draft discards those current edits. Saved drafts persist per entity
type for the current server connection and begin from the loaded definition when one exists.

### Export behavior

`/sg export` sends the chosen config to the server. The server writes it to:

```text
<world>/datapacks/somegoogly-picker/data/<namespace>/eyes/<entity path>.json
```

It then reloads datapacks so the change becomes active and persistent. Successful exports are limited
to one per player every 10 seconds because each performs a full reload.

`/sg exportall` is client-only. It dumps every loaded definition plus current session drafts to:

```text
<game directory>/somegoogly-export/data/<namespace>/eyes/<entity path>.json
```

It does not modify the world or reload datapacks.

### Test-mob tools

`/sg spawn <entity type>` creates one persistent, AI-disabled living entity at the targeted block when
it fits.

`/sg spawnall [namespace]` creates an audit grid containing one living entity of every available type,
or every type in the optional namespace. It builds sandstone platforms and water basins as needed,
overwrites blocks freely, and has no undo. It is disabled by default; use it only in a disposable
authoring world.

`/sg mob move <dx> <dy> <dz>` and `/sg mob rot <azimuth>` reposition the chosen live entity. Mob move
uses world-coordinate offsets; eye move sets local eye coordinates.

## Admin commands

The server-side `/sg admin` tree requires operator permission level 2, creative mode, and a living
entity under the player's crosshair within about 20 blocks.

| Command | Effect |
| --- | --- |
| `/sg admin eyes <true|false>` | Toggle the target's stored eye flag |
| `/sg admin tint iris <r> <g> <b>` | Set entity-wide iris override |
| `/sg admin tint cornea <r> <g> <b>` | Set entity-wide cornea override |
| `/sg admin tint clear` | Clear both color overrides |
| `/sg admin glow <on|off|config>` | Force glow or return to datapack defaults |
| `/sg admin behavior <id|random>` | Trigger one expression immediately |

Color channels range from 0 to 1. Behavior triggering still obeys the one-at-a-time rule.

## Visible limitations

- Players lose applied eyes on death and respawn.
- A harvested item captures one effective appearance, not separate colors for every displayed eye.
- Per-entity appearance overrides apply uniformly to all eyes on an entity.
- The ender dragon cannot have eyes.
- Some baby models may place eyes slightly differently because their special baby-scale transform is
  outside the model-part tree used for attachment.
- Third-party model updates can rename or reorganize attach points; the eye may disappear or move until
  its datapack definition is updated.
- A mob type that swaps among different models can show an eye only when the active model contains the
  configured part or bone.
- The dynamic eye-modifier recipe may not appear in JEI or similar recipe viewers.
- The Mods screen metadata still lacks a configured homepage, issue tracker, update URL, and logo.
