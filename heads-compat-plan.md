# Plan: googly eyes on Heads mod heads (1.20.1)

Status: all phases written; rendering not yet checked in game.

## Goal

When Mrbysco's Heads mod (1.1.3, Forge 1.20.1, source in `othersource/Heads-1.20`) is installed, its placed heads and head items take googly eyes exactly as vanilla heads do: an eyed mob's own Heads drop keeps its eyes, a Slimy Eye gives an eyeless Heads head eyes, Optometrist shears take them back, breaking and re-placing keeps them, the inspector and tooltip know them, and placed heads play ambient expressions. When Heads is absent, nothing changes and nothing references it.

## What already works (no change)

Heads' `HeadBlock extends SkullBlock` and `HeadBlockEntity extends SkullBlockEntity` (no `saveAdditional`/`load` override), its block entity uses the vanilla `SkullBlockRenderer`, its models come in through Forge's `CreateSkullModels`, its items are `StandingAndWallBlockItem`s drawn by the vanilla skull item path, and its block loot is a plain item drop. So `SkullBlockEntityDataMixin`, both render Mixins, `HeadEyesLootModifier`, `useOnSkull`, and the tooltip and inspector already reach Heads' blocks and items. Each of them only waits for `SkullEyeState.entityFor` to name a mob.

## Decisions taken (change before starting if wrong)

- **Soft dependency by registry id only.** No compile, runtime, or class reference to Heads anywhere in shipped code. A Heads head is recognized by its block's registry id (namespace `heads`). Without Heads those ids never appear, so every lookup misses and behavior is identical to today. `mods.toml` gets an optional `heads` dependency (`mandatory=false`, `ordering="NONE"`, `side="BOTH"`) so the pairing is declared the Forge way. Fabric is untouched beyond common code, since Heads is Forge-only.
- **The mapping is a code table in common, not a data file.** Both sides need it (the server for drops, loot, and use; the client for rendering), the client never receives server data files, and it describes another mod's fixed content, not something an admin tunes. It lives in `SkullEyeState` beside the vanilla `MOBS` map, keyed by Heads' type name (the block id path with `_wall` and the trailing `_head`/`_skull` removed, so floor and wall blocks share an entry). 1.20.1's `SkullBlock.Type` has no name, so the block id is the only key.
- **Eye placement is derived, not hand-tuned.** Vanilla skull models share their mob's head geometry; Heads' models don't, because each re-centers its mob's head cube on the block. Each Heads model may also rescale it (the ghast's 16-cube head is 8 here) or tilt it (the horse's head part has a fixed `xRot` of 0.45). For each Heads entry, the client bakes the mob's vanilla model layer once and reads the first cube of the part the eyes attach to. It then reads the first cube of the Heads model's head part and maps one onto the other: the mob cube's front-face center onto the Heads cube's front-face center, scaled by the smaller of the width and height ratios. The eyes keep the mob definition's positions, sizes, and orientations through that transform. A Heads entry can override the cube index or add an offset where the first-cube rule is wrong. This avoids hand-copying vanilla mob geometry, which I can't read, and it holds as long as Heads' model is a copy of the mob's.
- **Finding the Heads model's head part uses cached reflection.** Heads' models keep it in a protected `head` field (`HeadModelBase`, `SheepSkullModel`, `HorseSkullModel`, and their subclasses). Blaze and husk use vanilla `SkullModel`, which the widened field already covers. This follows the existing reflected resolvers for third-party models. A model with no such field gets no eyes, and the failure is logged once, as optional renderer integrations already do.
- **Out of scope:** worn heads (unchanged), and any mob whose eye definition doesn't attach to a single part that has a cube. Those are the Heads entries listed under "Left eyeless".

## The table

Each entry: Heads type name → entity id, the vanilla model layer to bake, and the attach token whose part the Heads head reproduces (default: tokens whose last segment is `head`, as now). The Heads types collapse to these entities:

- Variants of one mob: `axolotl_*` → axolotl, `cat_*` → cat, `*_sheep`, `sheep`, `sheep_shaven` → sheep, `horse_*` (except below) → horse, `fox`, `fox_snow` → fox, `brown_mooshroom`, `red_mooshroom` → mooshroom.
- Renamed: `horse_skeleton` → skeleton_horse, `horse_undead` → zombie_horse.
- One-to-one: bat, bee, blaze, cave_spider, chicken, cod, cow, dolphin, donkey, drowned, enderman, endermite, evoker, ghast, glow_squid, goat, hoglin, husk, illusioner, iron_golem, magma_cube, mule, ocelot, pig, piglin, piglin_brute, pillager, salmon, silverfish, slime, snow_golem, spider, squid, stray, turtle, villager, vindicator, wandering_trader, witch, wolf, zoglin, zombie_villager, zombified_piglin.

Entries whose definition attaches elsewhere name their part: bee `body`, ghast `root/body`, chicken `head1` (check what part this token resolves to on the baked layer).

**Left eyeless (no entry, refused like the dragon):**
- Axolotl, endermite, silverfish: we have no eye definitions for them.
- Squid and glow squid: eyes attach to `root`, which has no cube.
- Slime and magma cube: eyes attach to eye parts (`root/right_eye`, `root/cube0`) that the Heads model doesn't reproduce.

Any of these can get an entry later with a hand offset.

## Phase 1 — Mapping (common)

Done as below, except the part rule is keyed by mob (`HEAD_PART_TOKENS`, used by `headParts(entity, helper)`) rather than by Heads entry, since the part a head reproduces belongs to its mob. Also done: the optional `heads` entry in `mods.toml`, and the Phase 4 GameTests that need no rendering (two mapping tests on both loaders; two Forge-only tests placing Heads blocks).

1. Add the Heads table to `SkullEyeState` and route `entityFor(Block)` through it after the vanilla `MOBS` map. Add a `partsFor(block, helper)` that returns the head's attach-part indexes using the entry's token (or the default `head` rule), replacing today's `headParts(helper)` at its callers.
2. Forge `onLivingDrops`: register at `EventPriority.LOWEST`. Heads adds its drop from a NORMAL-priority `LivingDropsEvent` listener, so ours must run after it. Fabric's drop hooks are unaffected.

That makes stamping, Slimy Eye, Optometrist, loot copy, tooltip, and inspector work for every mapped Heads head. Rendering still shows nothing until Phase 2, because `headPart` refuses Heads' models.

## Phase 2 — Rendering (common client)

Done, with these departures: the mob's model comes from a throwaway sample entity's renderer (`ModelPartVocabulary.sampleFor`) rather than a baked layer, so no layer table is needed; the mob part comes from the existing resolvers (`Attachment.part()`); each side uses its largest cube in the part's subtree, not the first, with the child-part chain to it replayed (Heads side) or undone (mob side); the chicken fits by its `head` cube while its eyes stay on `head1`, which shares that pivot. Placements are cached per Heads model and token and cleared with the resolver caches; a failed fit skips that part's eyes and logs once.

1. `SkullEyeRenderer.headPart`: after the vanilla models, look up a cached `head` field on the model's class by reflection.
2. A client cache of per-entry transforms: on first use, bake the entry's vanilla layer from `Minecraft.getInstance().getEntityModels()`, walk the token to the part, read both first cubes (access-widen `ModelPart.cubes`), and compute translation and scale. The cache is cleared with the other resolver caches on renderer reload.
3. `renderEyes` applies the head part's `translateAndRotate` as now, then the entry's transform, then draws.

## Phase 3 — Dev setup

Done: Forge loads Heads 1.1.3 in development runs and GameTests through `modLocalRuntime "maven.modrinth:heads:$heads_version"` (`heads_version = X7aPmbm0`, the Modrinth version id), from Modrinth Maven declared exclusively for the `maven.modrinth` group. It never enters the compile classpath or the published artifact. Mirrored in `build-env/` and `orientation-build.md`.

## Phase 4 — GameTests

- Shared, run with or without Heads (pure lookups on registry ids): each Heads type name maps to its entity, floor and wall ids share an entry, eyeless entries refuse, and a non-Heads id with the same path does not match.
- Forge-only, registered only when `ModList.get().isLoaded("heads")`: a Slimy Eye on a placed `heads:cow_head` gives it eyes; Optometrist takes them off; breaking an eyed Heads head drops an item carrying the snapshot; an eyed cow killed so Heads drops its head gets a stamped head (drop rate forced through Heads' config, or the head stack handed to `markHeadDrop` directly if forcing it is awkward).

Rendering placement is checked in game, family by family: humanoid-like, quadrupeds, horses, fish, spiders, bee, ghast, chicken.

## Phase 5 — Documentation

Done: README, `orientation-player.md`, `orientation-code.md`, `orientation-build.md`.

Update `orientation-player.md` (Heads support, which heads stay eyeless), `orientation-code.md` (the table, derived placement, the optional dependency, test counts), `orientation-build.md` and `build-env/` for the dev dependency, and the README's "Getting eyes".

## Open verification items (in game, once rendering works)

- Eye placement on every Heads head, family by family: humanoid-like, quadrupeds, horses (tilted, cube under a child part), fish, spiders, bee (body), ghast (half scale), chicken (`head1` fitted by `head`).
- Whether the largest cube is the main head cube on both sides for every mapped mob (a hat, mane, or snout larger than the head would throw it off).
- That `ModelPart.Cube`'s `minX`…`maxZ` are public, as assumed (the build will say).
