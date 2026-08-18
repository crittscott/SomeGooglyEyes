# Vanilla mob eye-attachment audit

Scope: the 74 vanilla mobs this mod ships eye definitions for (`data/minecraft/eyes/*.json`). For each,
`<mob>.md` in this directory records which resolver family its model belongs to, and whether adult/baby
(where it has a baby) attachment is correct. `rabbit.md` is the detailed reference case for the bug family;
`llama.md`, `camel.md`, and `sniffer.md` are the other detailed write-ups (mobs found NOT covered).

Everything else below is "covered": either the model's family resolver (`HierarchicalResolver` /
`AgeableListResolver`) already handles it correctly, or (rabbit/llama/camel/sniffer) it doesn't yet and
that's written up in its own file.

Audit method: every vanilla client model class was checked for its superclass chain and for whether it
overrides `renderToBuffer` with its own scale/translate wrap outside the part tree (the bug class this
whole audit is hunting). See the four NOT COVERED files for the ones that do.

- [x] allay — allay.md
- [x] armor_stand — armor_stand.md
- [x] bat — bat.md
- [x] bee — bee.md
- [x] blaze — blaze.md
- [x] camel — camel.md — **FIXED**
- [x] cat — cat.md
- [x] cave_spider — cave_spider.md
- [x] chicken — chicken.md
- [x] cod — cod.md
- [x] cow — cow.md
- [x] creeper — creeper.md
- [x] dolphin — dolphin.md
- [x] donkey — donkey.md
- [x] drowned — drowned.md
- [x] elder_guardian — elder_guardian.md
- [x] enderman — enderman.md
- [x] evoker — evoker.md
- [x] fox — fox.md
- [x] frog — frog.md
- [x] ghast — ghast.md
- [x] giant — giant.md
- [x] glow_squid — glow_squid.md
- [x] goat — goat.md
- [x] guardian — guardian.md
- [x] hoglin — hoglin.md
- [x] horse — horse.md
- [x] husk — husk.md
- [x] illusioner — illusioner.md
- [x] iron_golem — iron_golem.md
- [x] llama — llama.md — **FIXED**
- [x] magma_cube — magma_cube.md
- [x] mooshroom — mooshroom.md
- [x] mule — mule.md
- [x] ocelot — ocelot.md
- [x] panda — panda.md
- [x] parrot — parrot.md
- [x] phantom — phantom.md
- [x] pig — pig.md
- [x] piglin — piglin.md
- [x] piglin_brute — piglin_brute.md
- [x] pillager — pillager.md
- [x] player — player.md
- [x] polar_bear — polar_bear.md
- [x] rabbit — rabbit.md — **FIXED (code)**, data pending
- [x] ravager — ravager.md
- [x] salmon — salmon.md
- [x] sheep — sheep.md
- [x] shulker — shulker.md
- [x] skeleton — skeleton.md
- [x] skeleton_horse — skeleton_horse.md
- [x] slime — slime.md
- [x] sniffer — sniffer.md — **FIXED**
- [x] snow_golem — snow_golem.md
- [x] spider — spider.md
- [x] squid — squid.md
- [x] stray — stray.md
- [x] strider — strider.md
- [x] trader_llama — trader_llama.md — **FIXED** (same model as llama)
- [x] turtle — turtle.md (covered, minor unrelated caveat noted)
- [x] vex — vex.md
- [x] villager — villager.md
- [x] vindicator — vindicator.md
- [x] wandering_trader — wandering_trader.md
- [x] warden — warden.md
- [x] witch — witch.md
- [x] wither — wither.md
- [x] wither_skeleton — wither_skeleton.md
- [x] wolf — wolf.md
- [x] zoglin — zoglin.md
- [x] zombie — zombie.md
- [x] zombie_horse — zombie_horse.md
- [x] zombie_villager — zombie_villager.md
- [x] zombified_piglin — zombified_piglin.md

## Summary

All 74 mobs are now covered by resolver code: **camel, llama, trader_llama, sniffer** are fixed and their
existing eye positions are expected to still be correct (worth a quick in-game check, not expected to need
re-authoring). **rabbit** is fixed in code but its shipped position was never correct under the old
resolver for either age, so it needs a fresh pass through the picker — see `rabbit.md`.

Everything else was fine as-is: either `HierarchicalModel` with no baby-only wrap override (most of the
list), or `AgeableListModel`/`QuadrupedModel`/`HumanoidModel` family, covered by the `AgeableListResolver`
head/body wrap fix already landed. `shulker` is a special case: its model (`ShulkerModel` via `ListModel`)
isn't claimed by either named resolver and falls to `ChildMapResolver`, but since it has no baby and
`ListModel`'s default render has no wrap to miss, the positional fallback is exact anyway.

Known data follow-up (separate from resolver coverage): mobs in the `AgeableListModel` family that have a
baby form may still carry a hand-compensated baby eye position in their shipped JSON, authored against the
old broken resolver the same way `cow.json` was (see `cow.md`). `mooshroom` reuses `CowModel` and should be
checked/re-authored the same way cow was. Other family members weren't individually re-verified against
their JSON for this same authoring artifact — worth a pass before considering the data itself done.
