# Eye drafts from the asset dump

The 991 entity geometry files in `othersource/myassets` each have a corresponding bundled definition at `common/src/main/resources/data/<namespace>/eyes/<entity>.json`. These are geometry-based starting points for in-game editing. They include effects, traps, and other unusual models present in the dump; an exported model alone does not establish that its entity is a living, supported eye target.

Every definition has one enabled, age-independent arrangement. The default draft has four eyes, in this order:

1. Face eye toward the negative-X side.
2. Face eye toward the positive-X side.
3. Side eye facing negative X.
4. Side eye facing positive X.

Animania's bees, bulls/calves/cows, foxes, dogs (male, female, and puppies), hogs, sows, kittens, queens, toms, hamster, and hedgehogs keep only the face pair. Bucks, does, rabbit kits, chicks, hens, ferrets, ewes, axolotls, lambs, peachicks, peacocks, peahens, piglets, rams, roosters, kids, and mobs whose names end in `goat` keep only the side pair. Each doe matches its corresponding buck. These 381 definitions each contain two eyes (200 face-only, 181 side-only); the other six Animania definitions retain four eyes. The four mobs whose names end in `goat` preserve their X positions and use Y = -0.45 and Z = -0.64. The 14 goat-breed bucks and does (Alpine, Angora, Fainting, Kiko, Kinder, Nigerian Dwarf, and Pygmy) preserve X and use Y = -0.05 and Z = -0.35; kids retain their default placements. Angora bucks and does retain X offsets of -0.22 and +0.22. All nine male wolves use positions (±0.11, -0.05, -0.3). All three bees use positions (±0.1, 0.03, -0.31). The review CSV's `eye_layout` column records each arrangement.

Directions here refer to the surface part's local axes. Face eyes assume local negative Z. Child rotations are applied when a surface is beneath the attachment. All eyes have `crossTarget: -1`, so deleting either pair does not leave dangling eye indices. Eyes use the picker's default scale (`eyeScale: 0.75`), white corneas, black pupils, and no glow, except Animania calves use the user-specified scale 0.6, chicks use 0.35, and roosters use 0.5. Both calf eyes have Y = 0.01 and Z = -0.13; their X positions remain unchanged. Head dimensions change placement offsets, never eye size.

Precise Animania placements also include these user-specified adjustments; unspecified coordinates and properties retain their existing values:

| Mobs | Adjustment |
| --- | --- |
| `bull_umbra`, `cow_umbra` | Both eyes: Z = 0 |
| `bull_wooly`, `cow_wooly` | Negative-X eye only: Z = 0 |
| `chick_*` | X = ±0.06, Y = -0.03; scale 0.35 |
| `*_fox` | Y = -0.05 |
| `ewe_*` | Y = 0.05, Z = -0.16 |

## Choosing attachments and surfaces

Selection searches the geometry in preorder, ignoring case: `head`, then `root`, then `0`/`#0`/the dump's positional `part0`. Only after these choices does it guess from compound names such as `head2` or `HeadBase`, or finally take the first root. An entity with several named heads receives eyes on the first selected head only. Animania's rabbit bucks, does, and kits use `0/Neck1/HeadFront`; bucks and does use X = ±0.16, Y = 0.02, Z = 0.05. Bucks and does without the rabbit HeadFront path retain their existing head attachments. Ferrets use the user-confirmed runtime token `0/Head`, the hamster uses `0/hamsterHead`, and hedgehogs use `0/head`; the dump labels all three families' exported head geometry `hamsterHead`. Foxes, dogs/puppies, and cats use their exported `0/body/neck1/head_base` path. Placements are expressed in the selected head's local coordinates. No Animania definition remains attached to bare `0`/`#0`; the audit of other dumped namespaces found no additional root-only configs with an immediate child named `head`.

The selected part remains the attachment even if it is an empty pivot. In that case, a substantial descendant supplies a surface, preferring head-like names. The main solid box supplies placement bounds, including inflation; horns, ears, and decorative planes do not expand those bounds. For the vanilla goat model, the small boxes on the head pivot are ears, so its larger nose child supplies the skull surface. Descendant translations, rotations, and scales are carried into attachment-local placement. Eyes sit at box mid-height, two on its negative-Z face and one on either X side. Long snouts, translucent planes, and unusual local axes can make these guesses poor.

Each model is checked independently against the resolver's normalized suffix matching. A unique leaf name is normally used directly: all seven Realm RPG Demons definitions attach to `head`. Parent paths are used for explicit user choices or to distinguish duplicate names. Five models have duplicate selected names that cannot be uniquely addressed by their dumped paths (Caniac, three Animania bees, and MobZ's wasp); these use the first matching leaf and are flagged.

The dump's positional `partN` roots use `#N` attachment tokens. The dump does not identify the runtime resolver family, so this mapping and other standalone root labels still require picker confirmation. Source-tree agreement establishes what the dump contains, not what `/sg list parts` exposes in a running game.

The dump has no installed mod-version metadata, so entries use the unbounded version range `(,)`. It also lacks separate adult/baby samples, so entries use `age: any`. Baby transformations, alternate models, and animated descendants require in-game checks.

## Review inventory

[dumped-eyes-review.csv](dumped-eyes-review.csv) lists every entity, selection rule, original part path, attachment token, surface part and box coordinates, model class, and review notes. Each row records a comparison of every part in that entity's geometry, skeleton, and Blockbench trees. All 991 trees agree across the three exports. Filter `attachment_check` for positional roots and duplicate names, `selection` for guesses, and `review` for root-token, animation, or flat-geometry concerns. Every face direction remains a guess until inspected in game.

| Namespace | Definitions |
| --- | ---: |
| alexscaves | 43 |
| animania | 387 |
| cnb | 9 |
| enemyexpansion | 27 |
| graveyard | 13 |
| guardvillagers | 1 |
| legendary_monsters | 34 |
| macabre | 132 |
| mobz | 59 |
| mutantszombies | 8 |
| myths_of_the_sea | 6 |
| realmrpg_demons | 7 |
| realmrpg_wyrms | 2 |
| species | 19 |
| spore | 86 |
| threateningly_mobs | 85 |
| undead_revamp2 | 45 |
| undermod | 2 |
| zombie_extreme | 26 |

The dumped-model definitions contain 3,202 eyes. Together with the bundled Minecraft 1.20.1 entries, the set contains 3,398 eyes. The shared sync limits allow 8,192 eyes and 2 MiB of decoded data. Eye definitions are GZIP-compressed before transmission, with a separate 900 KiB wire cap retained for both loaders. Reload and picker export check both size budgets, and the client bounds decompression before parsing. Network protocol 19 requires matching clients and servers. Configurations exceeding a count or byte cap cause reload to retain the previous set.

## Editing and regeneration

Use `/sg list parts` and the creative picker to verify attachments and orientation, delete the unwanted pair, and adjust placement. `/sg export` writes a world datapack override; `/sg exportall` writes editable definitions under the game directory. Neither command edits these source resource files.

`othersource/generate_dumped_eyes.py` implements the geometry conversion using only Python's standard library. It reads the loose exports, not archives or decompiled code. All source trees and outputs are prepared before any config is written. Normally it refuses to overwrite existing definitions. `--refresh` explicitly replaces drafts listed in the existing review CSV, including any manual edits to those files; other existing definitions are still protected. The CSV is written alongside the definitions. No game, Gradle build, or GameTests were run for these drafts.
