# Datapack format: eye definitions

Eye placements are ordinary datapack JSON. You can add eyes to modded mobs, reshape the shipped placements, or disable a mob entirely.

## File location

One file per entity type, where the file path encodes the entity id:

```text
data/<entity namespace>/eyes/<entity path>.json
```

`minecraft:zombie` → `data/minecraft/eyes/zombie.json`. Files are loaded on world start and `/reload`, then synced to clients automatically.

The **ender dragon** is hard-excluded: a config for it is refused from any datapack.

### Entries: version and age selection

Each entry declares which **mod version** it applies to and which **age** of the mob:

- `version` — either an exact version (`"1.20.1"`) or a bracket range (`"[1.20.1,1.21)"`), matched against the installed version of the file's *namespace* (for `data/minecraft/...` that's the Minecraft version; for a modded namespace, that mod's version). One file can carry placements for several model revisions of the same mob; the matching entries win. If **no** entry matches the installed version, the file still works: the **nearest generation** is used instead — the newest entry older than the installed version, or the oldest entry when the installed version predates them all — and the mismatch is logged (an error when the datapack is stale and should be re-exported, a warning when the mod was downgraded). Eyes are cosmetic, so this degrades gracefully: a placement authored for a different model revision may sit oddly, and an attach point that no longer exists simply doesn't attach.
- `age` — `adult`, `baby`, or `any`. An age-specific entry takes precedence over `any`. Babies and adults can have completely different placements.
- `enabled` — optional, defaults to `true`. `"enabled": false` is an authoritative off-switch: the mob will never roll eyes at spawn, regardless of the server's spawn-chance config.

### Variants: weighted arrangements

`variants` is a list of complete, alternative eye arrangements. Each mob picks **one** variant at spawn (weighted by `weight`, default 1.0, relative) and keeps it for life. A mob with a single arrangement still uses a one-element `variants` list.

### Heads: where eyes attach

A head groups eyes onto one model part:

- `attachPoint` — the model part (or GeckoLib bone) the eyes ride on, so they inherit its animation. Tokens are slash-joined part paths like `"root/body/head"` and are matched by **suffix**, so a bare `"head"` attaches to `root/body/head`; use a longer path (`"body/head"`) to disambiguate two same-named parts. Matching ignores case and punctuation (`leftHead` ≡ `left_head`). A segment like `"#0"` addresses a nameless part positionally — some models have no better name for their root.
- `eyes` — the list of eyes on that part.

The easiest way to discover a mob's part names is the picker (`/sg list parts` on a chosen mob).

## Eye fields

Every field is optional; defaults shown. All of placement and default appearance lives here — the eye *item* carries appearance overrides only, and reuses this placement when applied.

| Field | Default | Meaning |
| --- | --- | --- |
| `position` | `[-0.13, -0.25, -0.25]` | Offset from the attach part's origin, in the part's local block units. |
| `eyeScale` | `0.75` | Overall size of the eye. `0` hides the eye. |
| `irisScale` | `0.6` | Pupil size relative to the eye. |
| `depth` | `1.0` | Thickness multiplier along the look axis (1 = standard googly proportions). The eye's back face stays at its position and the body extends forward, so a deeper eye protrudes further — enough to envelop a modeled eyeball that sticks out of the head. |
| `inclination` | `90.0` | Aim angle in degrees from the part's +Y axis (90 = horizontal). |
| `azimuth` | `270.0` | Aim angle in degrees from the part's +X axis (270 = facing forward, local −Z). |
| `corneaColors` | `[1.0, 1.0, 1.0]` | Eye-white color, RGB 0–1. |
| `irisColors` | `[0.0, 0.0, 0.0]` | Pupil color, RGB 0–1. |
| `glows` | `false` | Render the eye full-bright (spider-style) regardless of light level. |
| `crossTarget` | `-1` | Index of another eye **in the same head** that this eye rolls toward during the `cross_eye` expression. `-1` = this eye doesn't cross.|

Colors set here are the mob's defaults; players can override cornea/iris/glow per mob via harvested items, dye crafting, and the slimy eye that applies them.

## An example

Horse, with potential butt eyes:

```json
{
  "entries": [
    {
      "version": "[1.20.1,1.21)",
      "age": "any",
      "enabled": true,
      "variants": [
        {
          "weight": 1.0,
          "heads": [
            {
              "attachPoint": "head",
              "eyes": [
                {
                  "position": [ -0.18, -0.6, 0.05 ],
                  "eyeScale": 0.75,
                  "irisScale": 0.6,
                  "depth": 1.0,
                  "inclination": 90.0,
                  "azimuth": 180.0,
                  "crossTarget": 1,
                  "corneaColors": [ 1.0, 1.0, 1.0 ],
                  "irisColors": [ 0.0, 0.0, 0.0 ],
                  "glows": false
                },
                {
                  "position": [ 0.18, -0.6, 0.05 ],
                  "eyeScale": 0.75,
                  "irisScale": 0.6,
                  "depth": 1.0,
                  "inclination": 90.0,
                  "azimuth": 0.0,
                  "crossTarget": 0,
                  "corneaColors": [ 1.0, 1.0, 1.0 ],
                  "irisColors": [ 0.0, 0.0, 0.0 ],
                  "glows": false
                }
              ]
            }
          ]
        },
        {
          "weight": 0.05,
          "heads": [
            {
              "attachPoint": "body",
              "eyes": [
                {
                  "position": [ 0.18, -0.3, 0.32 ],
                  "eyeScale": 0.75,
                  "irisScale": 0.6,
                  "depth": 1.0,
                  "inclination": 90.0,
                  "azimuth": 90.0,
                  "crossTarget": 1,
                  "corneaColors": [ 1.0, 1.0, 1.0 ],
                  "irisColors": [ 0.0, 0.0, 0.0 ],
                  "glows": false
                },
                {
                  "position": [ -0.18, -0.3, 0.32 ],
                  "eyeScale": 0.75,
                  "irisScale": 0.6,
                  "depth": 1.0,
                  "inclination": 90.0,
                  "azimuth": 90.0,
                  "crossTarget": 0,
                  "corneaColors": [ 1.0, 1.0, 1.0 ],
                  "irisColors": [ 0.0, 0.0, 0.0 ],
                  "glows": false
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```
