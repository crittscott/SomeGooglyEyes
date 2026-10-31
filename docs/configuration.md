# Configuration reference

 **Server** config controls gameplay (who gets eyes, harvesting, expressions). **Client** config controls only what *you* see.

## Server config

`<world>/serverconfig/somegoogly-server.toml`

Forge applies edits as soon as they reload the file; Fabric applies them at world start and on `/reload`. Every key is written with an explanatory comment. Fabric uses NightConfig TOML, preserves existing raw values, unknown keys, and parsed comments when adding absent defaults, and saves the original text in a sibling `.bak` before rewriting. Invalid syntax leaves the file and loaded settings intact.

### Spawn settings

| Key | Default | Meaning |
| --- | --- | --- |
| `googlyEyesEnabled` | `true` | Master switch for the whole mod. Turning it off stops *new* mobs from being given eyes, hides every eye (old and new) on every client, and refuses hand-applying or harvesting eyes. Existing eye data stays intact and reappears when enabled. Automatic reactions, anger checks, and ambient scheduling pause while disabled; explicit admin expressions remain available. Current viewers receive a state refresh on resumption. |
| `globalPercent` | `5` | Default percent chance (0–100) for an eligible mob to spawn with eyes. |
| `harvestOnKillPercent` | `25` | Percent chance that an eyed mob killed by a player's direct shears blow drops its eye. The Optometrist enchantment bypasses this with a guaranteed, non-lethal right-click harvest. |
| `entityOverrides` | empty | Per-mob spawn chances, one entry per line as `"entity,percent"`. |

`entityOverrides` supports `*` wildcards in the entity id:

```toml
entityOverrides = [
    "minecraft:zombie,100",   # every zombie
    "*:*_horse,50",           # any horse variant from any mod
    "minecraft:*,5"           # all other vanilla mobs
]
```

Resolution order: an exact (wildcard-free) id always wins; otherwise the **first matching** wildcard line wins (list specific patterns above broad ones); otherwise `globalPercent`.

A mob's eyes-or-not decision is made **once, at first spawn**, and stored on the mob for life. Changing these settings affects newly spawned mobs only. Existing mobs never reroll.

A percent of `0` is how you turn eyes off for a specific entity or, with a wildcard, an entire mod's
namespace (e.g. `"alexsmobs:*,0"`). This only stops *new* spawns from rolling eyes. It doesn't strip
eyes from mobs that already have them, and a player can still give one of these mobs eyes by hand with
a Slimy Eye.

### Behavior settings

Eyes have two kinds of behavior. **Expressions** are one-shot animations (`stare`, `blink`, `side_eye`, `cross_eye`, `grow`, `love`, `swirl`, `color_change`, all under the `somegoogly:` namespace); a mob plays at most one at a time, and overlapping triggers are dropped, never queued. **Ongoing behaviors** last as long as the mob stays in some state, and sit on top of whatever expression plays.

#### Expressions

| Key | Default | Meaning |
| --- | --- | --- |
| `ambientBehaviors` | `true` | Whether idle eyed mobs, placed heads, and a held Googly Eye periodically play a random expression. |
| `ambientMinTicks` / `ambientMaxTicks` | `200` / `800` | Idle time between ambient expressions. |
| `ambientBehaviorPool` | blink, cross_eye, side_eye, stare | Which expressions the idle timer may pick, by id. Remove a line to drop it from rotation. A held Googly Eye skips `cross_eye`. |
| `growOnHitPercent` | `20` | Percent chance an eyed mob's eyes bulge (`grow`) when a player damages it, and that a held Googly Eye bulges when its holder is hurt. `0` disables. |
| `growOnMilk` | `true` | Milking an eyed cow, mooshroom, or goat with a bucket makes its eyes bulge (`grow`). |
| `growOnShear` | `true` | Shearing an eyed sheep makes its eyes bulge (`grow`). |
| `loveOnAffection` | `true` | An eyed mob's pupils swell (`love`) when it enters love mode, when villagers breed, when an iron golem offers a flower, or when an ocelot comes to trust a player. |
| `swirlOnTrade` | `true` | Completing a trade with an eyed villager or wandering trader spins its pupils (`swirl`). |
| `swirlOnHeal` | `true` | An eyed mob being healed, or the holder of a Googly Eye healing, plays `swirl`. |
| `swirlHealCooldownTicks` | `200` | Minimum ticks between heal-triggered swirls on one mob or held eye, so regeneration doesn't loop it. |

The event-driven expressions (`grow`, `love`, `swirl`) are controlled by their own settings above and are not in the ambient pool by default; `color_change` is absent from the default pool but can be added to `ambientBehaviorPool` or triggered by an admin command.

#### Ongoing behaviors

| Key | Default | Meaning |
| --- | --- | --- |
| `squintOnSleep` | `true` | A sleeping eyed mob squints; a sleeping villager closes tan eyelids to a slit instead. A sleeping mob plays no expressions either way. |
| `squintOnRam` | `true` | An eyed goat squints while it lowers its head to ram. |
| `winkOnAim` | `true` | An eyed skeleton drawing its bow, or an eyed player drawing a bow, closes the eye away from the arm holding it. |
| `rattleOnSwell` | `true` | A swelling eyed creeper's pupils rattle, harder the closer it is to exploding. |
| `pulseWhenCreepy` | `true` | A creepy eyed enderman's pupils pulse while it stares down whoever looked at it. |
| `angryEyes` | `true` | An eyed mob that is attacking, or angry without attacking (a provoked iron golem, piglin, or zombified piglin), shows red pupils. For the second kind the server checks each watched eyed mob once per tick and tells clients only when its anger starts or ends; with this off it checks nothing. |

Each client works these out from state the game already shares, so they cost the server nothing beyond `angryEyes`' check. Connected clients see a change to any of these settings as soon as the server applies it.

### Picker settings

| Key | Default | Meaning |
| --- | --- | --- |
| `allowSpawnAll` | `false` | Must be set `true` before `/sg spawnall` will run. **Only enable this on a test world** — `/sg spawnall` force-spawns every summonable mob, and mobs from mods like MineColonies can corrupt a world when spawned outside their mod's normal context. See [docs/picker.md](picker.md). |
| `spawnExcludedMods` | `minecolonies`, `create` | Namespaces that `/sg spawn` and `/sg spawnall` skip, e.g. `"mekanism"`. Authoring commands only. No effect on eye eligibility or natural spawning. Only remove either entry on a world you're prepared to lose. |
| `spawnExcludedEntities` | empty | Entity ids that `/sg spawn` and `/sg spawnall` skip, e.g. `"minecraft:armor_stand"`. Same authoring-only scope. |

## Client config

Location: `config/somegoogly-client.toml` (per player, affects rendering only).

| Key | Default | Meaning |
| --- | --- | --- |
| `disableGooglyEyes` | `false` | Hide all googly eyes on this client. |
| `disabledEntities` | empty | Entity ids that should not display eyes, e.g. `"minecraft:zombie"`. |
| `disabledMods` | empty | Mod namespaces whose entities should not display eyes, e.g. `"minecraft"`. |

These are personal preferences: the mob still *has* eyes (other players see them, and they can still
be harvested); you just don't render them. Forge applies edits as soon as they reload the
file; on Fabric, restart the client.

The server's `googlyEyesEnabled` is a separate, stronger switch: when it's off, nothing renders for
anyone regardless of these client settings, and eyes can't be applied or harvested either. The
creative eye picker ignores `googlyEyesEnabled`.
