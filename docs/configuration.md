# Configuration reference

 **Server** config controls gameplay (who gets eyes, harvesting, expressions). **Client** config controls only what *you* see.

## Server config

`<world>/serverconfig/somegoogly-server.toml`

### Spawn settings

| Key | Default | Meaning |
| --- | --- | --- |
| `googlyEyesEnabled` | `true` | Master switch for the whole mod. Turning it off stops *new* mobs from being given eyes, hides every eye (old and new) on every client, and refuses hand-applying or harvesting eyes. Existing eye data on entities is left untouched and reappears as soon as this is turned back on. |
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

| Key | Default | Meaning |
| --- | --- | --- |
| `ambientBehaviors` | `true` | Whether idle eyed mobs periodically play a random expression. |
| `ambientMinTicks` / `ambientMaxTicks` | `100` / `400` | Idle time between ambient expressions on a mob. |
| `ambientBehaviorPool` | blink, cross_eye, side_eye, stare | Which expressions the idle timer may pick, by id. Remove a line to drop it from rotation. |
| `growOnHitPercent` | `20` | Percent chance an eyed mob's eyes bulge (`grow`) when a player damages it. `0` disables. |
| `swirlOnTrade` | `true` | Completing a trade with an eyed villager or wandering trader spins its pupils (`swirl`). |
| `swirlOnHeal` | `true` | An eyed mob being healed plays `swirl`. |
| `swirlHealCooldownTicks` | `200` | Minimum ticks between heal-triggered swirls on one mob, so regeneration doesn't loop it. |

Behavior ids all live under the `somegoogly:` namespace: `stare`, `blink`, `side_eye`, `cross_eye`, `grow`, `swirl`, `color_change`. The event-driven ones (`grow`, `swirl`) are controlled by their own settings above and are not in the ambient pool by default; `color_change` ships in neither and is only reachable via the admin command.

A mob plays at most one expression at a time; overlapping triggers are dropped, never queued.

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
be harvested); you just don't render them. Config reload behavior differs by loader; if a file edit
is not reflected immediately, reload resources or restart the client.

The server's `googlyEyesEnabled` is a separate, stronger switch: when it's off, nothing renders for
anyone regardless of these client settings, and eyes can't be applied or harvested either. The
creative eye picker ignores `googlyEyesEnabled`.
