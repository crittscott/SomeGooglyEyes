# Handoff: baby/adult eye-attachment fixes

Self-contained summary of a completed bug-fix pass. You shouldn't need the conversation that produced
this — everything relevant is either here or in the files it points to.

## The bug, in one paragraph

Some vanilla mob models reposition their parts with a `PoseStack` scale/translate that lives *outside*
their normal part tree — applied and popped inside their own `renderToBuffer`, invisible to any
`RenderLayer` (including this mod's eye layer). Our attachment resolvers (`client/render/resolver/`) only
replay each part's own `translateAndRotate`, so wherever this outside-the-tree wrap exists, we silently
reconstruct the wrong (usually adult) transform.

## Fixed

A full audit of all 74 vanilla mobs this mod ships eye definitions for found this bug in five places (see
`vanilla-mobs.md` for the checklist and every individual `<mob>.md`). All five are now fixed in code:

- **`AgeableListModel`** family (cow, pig, zombie, etc.) — `AgeableListResolver` replays the baby-only
  head/body scale+offset (`headWrap`/`bodyWrap`), reading the model's private
  `scaleHead`/`babyHeadScale`/`babyYHeadOffset`/`babyZHeadOffset`/`babyBodyScale`/`bodyYOffset` fields via
  access-transformer entries. `cow.json` was re-authored to a single `"any"`-age entry (see `cow.md`).
- **`AgeableHierarchicalModel`** (sniffer) and **`CamelModel`** (camel) — `HierarchicalResolver.youngWrap`
  recognizes each by `instanceof` and replays its baby-only scale+translate (Sniffer's via its
  `youngScaleFactor`/`bodyYOffset` fields, new AT entries needed; Camel's via inline literal constants, no
  AT needed). See `sniffer.md`/`camel.md`.
- **`RabbitModel`** (rabbit) and **`LlamaModel`** (llama, trader_llama) — neither is `HierarchicalModel` or
  `AgeableListModel`, so both fell to `ChildMapResolver`'s positional fallback, which has no concept of
  either model's wrap. A new `RabbitLlamaResolver` (inserted into `Resolvers.ALL` before
  `ChildMapResolver`) handles both: Rabbit's head-group/body-group/adult three-way split (Rabbit is unusual
  — its *adult* render also lives inside a wrap, not just baby), and Llama's independent
  head/body/legs+chest young-only wraps. Both models' `ModelPart` fields needed new AT entries (private,
  no getters). See `rabbit.md`/`llama.md`/`trader_llama.md`.

The shared gotcha across all of these: `Resolvers.ATTACHMENTS` memoizes `resolve()` per (model instance,
token) and replays it every frame, and the model instance is a singleton per renderer shared by every baby
and adult of that entity type. So the young/not-young branch is decided **inside the replayed closure,
checked fresh every call** — never baked in when the closure is built (which only happens once, on the
first resolve). Every fix above follows this pattern; copy it exactly for any future one.

## Data follow-up (separate from code, not yet done)

Renaming an `attachPoint` token is a mechanical necessity whenever a mob moves from `ChildMapResolver`'s
positional naming (`#0`, `#11`, …) to a named resolver — done for `rabbit.json`, `llama.json`, and
`trader_llama.json` as part of this fix (`#N` → the real field name), since otherwise the stored token
simply stops matching anything and the eye disappears. Re-tuning the eye **position** itself is a separate
question, not done here except where mechanically required:

- **`rabbit.json`** needs a real re-authoring pass through the picker. Rabbit was confirmed broken for
  *both* ages before this fix (there was no correctly-tuned reference position to preserve), and the wrap
  now changes the scale its position is interpreted under for both ages.
- **`llama.json`**, **`camel.json`**, **`sniffer.json`** were left untouched: each mob's *adult* render had
  no wrap even before the fix, so their existing positions should still be correct for adults, and should
  now also be correctly scaled for babies for the first time (previously babies just used the wrong adult
  transform). Worth a quick in-game check on a baby of each, but not expected to need re-authoring.
- **`mooshroom.json`** reuses `CowModel` and almost certainly has the same hand-compensated baby position
  `cow.json` had before its fix (a baby entry hand-tuned against the old broken resolver at one rest pose).
  Needs the same treatment as cow: re-author via the picker (or verify math), not a blind data patch. Other
  `AgeableListModel`-family babies (sheep, goat, wolf, etc.) weren't individually checked for this same
  artifact — see `vanilla-mobs.md`'s summary section.

None of this has been verified in-game — the project's build/test policy means that's for the user to run.

## Finding SRG field ids (for any future AT entries)

1. Find the *official* field name and descriptor in
   `C:\Users\Dad\.gradle\caches\fabric-loom\1.20.1\forge\mojmap.tsrg2` (search the class's obf name block —
   find the class first via its official path, e.g. search for the class name as a comment/string, or
   locate it by searching a known official field/method name nearby).
2. Cross-reference the same obf class/field letters against
   `C:\Users\Dad\.gradle\caches\forge_gradle\minecraft_user_repo\de\oceanlabs\mcp\mcp_config\1.20.1-20230612.114412\obf_to_srg.tsrg2`
   to get the `f_XXXXXX_` SRG id for each field.
3. Add to `accesstransformer.cfg` as `public net.minecraft.client.model.<Class> f_XXXXXX_` (no descriptor
   needed for fields — see existing entries for the exact format), with a comment noting the official name.
4. In Java source, reference the field by its **official** name (e.g. `ageable.scaleHead`), not the SRG id
   — the dev compile classpath is official-mapped; SRG ids are only for the AT config file itself.

This lookup (reading mapping files, not decompiled source) needed no special authorization and was used
for all the AT entries added in this fix.

## Reading decompiled vanilla source (if a future session needs to go further)

The user explicitly authorized decompiled-source reading for the original bug hunt, overriding this
project's normal "never decompile" rule — that authorization was scoped to that investigation, not a
standing grant. Everything that investigation found is already written up in this directory; a fresh
session extending this work (e.g. verifying the data follow-up above) should not need to decompile
anything, since the field lookup above covers new AT entries and the write-ups above cover the wraps
themselves. If something genuinely undocumented comes up, ask again rather than assume.

The decompiled+remapped source lives in a persistent sources jar (survives Gradle cache cleanup, unlike
the ephemeral `build/tmp/.cache/expanded` staging directories):

```
C:\Users\Dad\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.0_mapped_parchment_2023.09.03-1.20.1\forge-1.20.1-47.4.0_mapped_parchment_2023.09.03-1.20.1-sources.jar
```

Extract a single class with (from the Bash tool, not PowerShell):
```
unzip -o -j "<jar path>" "net/minecraft/client/model/<ClassName>.java" -d "<scratchpad dir>"
```
Or list/extract everything under a package at once with a glob pattern instead of one filename.

## Files in this directory

- `vanilla-mobs.md` — the checklist/index, all 74 mobs, links to every other file.
- `rabbit.md`, `llama.md`, `trader_llama.md`, `camel.md`, `sniffer.md` — the five fixed-in-this-pass
  writeups, each with the exact vanilla source, constants, and what the landed fix does.
- `<mob>.md` for the other 69 — one-line "covered by X" verdicts, mostly not needed for this fix work but
  kept for completeness / in case the coverage classification is ever in doubt.
- `cow.md` — the reference "covered, and here's what data follow-up looks like" example.
