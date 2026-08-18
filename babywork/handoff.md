# Handoff: baby/adult eye-attachment fixes

Self-contained summary of an in-progress bug hunt. You shouldn't need the conversation that produced this
— everything relevant is either here or in the files it points to.

## The bug, in one paragraph

Some vanilla mob models reposition their parts with a `PoseStack` scale/translate that lives *outside*
their normal part tree — applied and popped inside their own `renderToBuffer`, invisible to any
`RenderLayer` (including this mod's eye layer). Our attachment resolvers (`client/render/resolver/`) only
replay each part's own `translateAndRotate`, so wherever this outside-the-tree wrap exists, we
silently reconstruct the wrong (usually adult) transform. `AgeableListResolver` had exactly this gap for
`AgeableListModel` (cow, pig, zombie, etc.) and has been fixed. The same *shape* of bug exists in four more
places that are NOT yet fixed.

## Already fixed (don't redo)

- `AgeableListResolver` now replays `AgeableListModel`'s baby-only head/body scale+offset
  (`headWrap`/`bodyWrap` in `AgeableListResolver.java`), reading the model's private
  `scaleHead`/`babyHeadScale`/`babyYHeadOffset`/`babyZHeadOffset`/`babyBodyScale`/`bodyYOffset` fields via
  new access-transformer entries in `accesstransformer.cfg`.
- `ModelPartTreeResolver` (`NamedRoot`, `PartChain`) was extended with an optional `preTransform` to carry
  this kind of wrap — the general mechanism, reusable for the fixes below.
- `cow.json` was re-authored: baby and adult are now one `"any"` entry, since cow's `scaleHead=false` means
  the same local position is correct for both ages once the wrap is replayed correctly.
- Full audit of all 74 vanilla mobs this mod ships eye definitions for: see `vanilla-mobs.md` (checklist)
  and the individual `<mob>.md` files. Five mobs came back NOT COVERED — that's the rest of this document.

## The critical gotcha (read before touching any resolver)

`Resolvers.ATTACHMENTS` memoizes `resolve()` per `(model instance, token)` and replays it every frame. The
model instance is a **singleton per renderer**, shared by every baby and adult of that entity type. So the
young/not-young branch must be decided **inside the replayed closure, checked fresh every call** — never
baked in when the closure is built (which only happens once, on the first resolve). `AgeableListResolver`'s
`headWrap`/`bodyWrap` do this correctly (they close over the model reference and re-read `.young` each
call); copy that pattern exactly for any new fix.

## What's still broken

Full technical detail (exact code, constants, symptoms) is in each mob's own file. Summary:

| Mob(s) | Model | Base class | Baby only? | AT needed? |
|---|---|---|---|---|
| `rabbit.md` | `RabbitModel` | `EntityModel` directly | No — adult **and** baby wrong | No (constants are inline literals) |
| `llama.md`, `trader_llama.md` | `LlamaModel` | `EntityModel` directly | Yes — adult is fine | No (inline literals) |
| `camel.md` | `CamelModel` | `HierarchicalModel` directly | Yes — adult is fine | No (inline literals) |
| `sniffer.md` | `SnifferModel` | `AgeableHierarchicalModel` → `HierarchicalModel` | Yes — adult is fine | **Yes** — `youngScaleFactor`/`bodyYOffset` are private fields, not literals |

Rabbit was in-game confirmed broken (both ages). Llama/Camel/Sniffer are read from source, not yet
in-game confirmed — worth a quick check before or after fixing.

## Recommended approach per mob

**Camel and Sniffer** (both `HierarchicalModel`-family): extend `HierarchicalResolver` the same way
`AgeableListResolver` was extended — its `roots()` currently returns one `NamedRoot("root", root())`.
Give that root a `preTransform` when the model needs one:
- `instanceof AgeableHierarchicalModel` → replay `scale(youngScaleFactor) + translate(0, bodyYOffset/16, 0)`
  gated on `.young`. This is the "proper" fix since `AgeableHierarchicalModel` is a real shared vanilla
  base class — covers Sniffer now and any future mod/vanilla mob built on it for free.
- `instanceof CamelModel` → replay the hardcoded `scale(0.45) + translate(0, 1.834375, 0)` gated on
  `.young`. Camel doesn't use `AgeableHierarchicalModel` (it hand-rolled the same pattern), so this needs
  its own `instanceof` branch with the literal constants baked into resolver code, not read from a field.

Sniffer needs two new AT entries (`youngScaleFactor`, `bodyYOffset` on `AgeableHierarchicalModel` — look
up SRG ids the same way as before, see "Finding SRG field ids" below). Camel needs none — the wrap
constants in `CamelModel.renderToBuffer` are literal floats, not fields, so just hardcode them in the
resolver same as vanilla does.

**Rabbit and Llama** (neither model family — currently fall to `ChildMapResolver`'s positional fallback):
these need new small dedicated resolver classes (matching the existing one-class-per-family pattern:
`HierarchicalResolver`, `AgeableListResolver`, `CitadelResolver`, `LLibraryResolver`), inserted into
`Resolvers.ALL` before `ChildMapResolver`. Each would:
1. `handles()` → `instanceof RabbitModel` / `instanceof LlamaModel`.
2. Name each mob's fields as roots (the field lists and exact wrap constants are already written out in
   `rabbit.md`/`llama.md` — no need to re-derive from source).
3. Group each root by which wrap it falls under (Rabbit: head-group vs body-group vs adult-no-wrap;
   Llama: head vs body vs legs+chest, adult has no wrap at all) and attach the matching `preTransform`,
   gated on `.young` at apply time per the gotcha above.
4. Needs new AT entries to read `RabbitModel`'s and `LlamaModel`'s private `ModelPart` fields (they're
   `private final`, no getters) — same lookup method as before.

This is an open design question, not a decision already made: whether Rabbit/Llama deserve their own
resolver classes (cleaner, matches existing architecture) vs. a couple of `instanceof` special cases
folded into `ChildMapResolver` (less code, but muddies what's supposed to be a generic reflection
catch-all). Given the project's stated preference for "deeper classes of coherent content" over one-off
fragmentation, and that this is only two mobs sharing a very similar shape, a single new small resolver
class handling both (or two very small ones) seems like the better fit — but worth a deliberate call
before writing it, not just defaulting.

## Data follow-up (separate from code)

`mooshroom.json` reuses `CowModel` and almost certainly has the same hand-compensated baby position
`cow.json` had before its fix (a baby entry whose position was hand-tuned against the old broken resolver
at one rest pose). Needs the same treatment: re-author via the picker (or verify math) once the resolver
fix is confirmed live, not a blind data patch. Other `AgeableListModel`-family babies (sheep, goat, wolf,
etc.) weren't individually checked for this same artifact — see `vanilla-mobs.md`'s summary section.

## Finding SRG field ids (for any new AT entries)

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

## Reading decompiled vanilla source (if you need to verify anything not already written up)

The user explicitly authorized this for this investigation, overriding this project's normal
"never decompile" rule — that authorization is scoped to this bug-hunt, not a standing grant. If a fresh
session needs to go further than what's already documented here and in the `<mob>.md` files, it's
reasonable to ask again rather than assume.

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
- `rabbit.md`, `llama.md`, `trader_llama.md`, `camel.md`, `sniffer.md` — the five NOT COVERED writeups,
  each with the exact vanilla source and constants involved.
- `<mob>.md` for the other 69 — one-line "covered by X" verdicts, mostly not needed for this fix work but
  kept for completeness / in case the coverage classification is ever in doubt.
- `cow.md` — the reference "covered, and here's what data follow-up looks like" example.
