# The picker: in-game eye authoring

The picker is a creative-mode tool for placing eyes on a live, animated mob and exporting the result as [datapack JSON](datapack-format.md).

Requirements: **creative mode**, checked on the server. The whole picker — including exporting, spawning, and the `mob` verbs — works from a remote client on any server running the mod; server admins should know that handing out creative also hands out these tools.

## Keys

Rebindable in Options → Controls:

| Key | Action |
| --- | --- |
| `K` | Toggle the picker on/off |
| `V` | Choose/release the mob under your crosshair |
| `[` / `]` | Cycle through the mob's model parts |

While the picker is on, every eye-configured mob shows its eyes (ignoring the spawn roll) so you can see existing placements, and the chosen mob is frozen in place (AI off) while you edit it. A HUD panel in the top-right shows the chosen mob, selected part, and saved eyes; a 3-axis gizmo marks the selected part's origin and orientation.

## Workflow

Everything beyond navigation is done through the `/sg` chat commands:

1. **Choose a mob**: look at it and press `V` (or `/sg choose`).
2. **Pick the attach part**: cycle with `[` `]`, or `/sg part <name|number>` (`/sg list parts` to see them all). The gizmo shows the part's local axes — eye positions are in this frame.
3. **Start an eye**: `/sg create <x> <y> <z>` places a draft eye at that offset.
4. **Shape it**:
   - `/sg move <x> <y> <z>` — set position (`~` leaves an axis unchanged)
   - `/sg rot <inclination> <azimuth>` — aim it (degrees; `~` supported)
   - `/sg posrot <x> <y> <z> <incl> <azi>` — both at once
   - `/sg properties eyescale <v>` / `irisscale <v>` — sizes
   - `/sg properties corneacolor <r> <g> <b>` / `iriscolor <r> <g> <b>` — colors (0–1)
   - `/sg properties glow <true|false>` / `invis <true|false>`
   - `/sg properties crosstarget <n>` — cross-eye partner (a saved eye number on the same part; `0` clears)
5. **Save it**: `/sg save` commits the draft to the eye list. Re-edit later with `/sg select <n>`, remove with `/sg delete <n>`, review with `/sg list eyes`.
6. **Variants** (optional alternative arrangements, weighted at spawn): `/sg variant new`, `/sg variant <n>` to switch, `/sg variant weight <w>`, `/sg variant del <n>`, `/sg list variants`.
7. **Export**:
   - `/sg export` sends the chosen mob's config to the server, which writes it into the world's datapack (`<world>/datapacks/somegoogly-picker/...`) and reloads, so it takes effect immediately and persists with the world. Rate-limited to one export per 10 seconds (each one is a full datapack reload).
   - `/sg exportall` dumps *every* known config — the loaded ones plus your session's drafts — to `<game dir>/somegoogly-export/`, ready to copy into a mod or datapack.

Drafts are kept per mob type for the whole session, so you can hop between several mobs and `exportall` at the end. Choosing a mob that already has a config seeds the draft from it, so you edit existing placements rather than starting blank.

`/sg unchoose` (or `V` again) releases the mob; `K` exits the picker.

## Repositioning the chosen mob

While a mob is chosen you can move it into better light or a clearer pose:

- `/sg mob move <x> <y> <z>` — teleport it to absolute world coordinates (`~` leaves an axis unchanged, as in the eye `move` verb)
- `/sg mob rot <azimuth>` — turn it in the XZ plane; azimuth uses the same convention as `/sg rot` (270 = facing −Z), so the numbers carry over

Both act on the live server mob (feedback comes back as a server message). Not to be confused with the eye `move`/`rot` verbs above, which shape the draft eye.

## Spawning test mobs

`/sg spawn <type>` (creative) spawns a single mob — AI off, persistent, facing you — at the block you're targeting (within 20 blocks). It refuses if no block is targeted or the mob's bounding box doesn't fit there. Nothing is terraformed, so aim into water when spawning an aquatic mob.

### The (dangerous) spawn grid

`/sg spawnall [mod]` (creative) **spawns one of every living mob in a tidy grid**, grouped by mod, AI off, facing you, so you can audit or author eyes for many mobs at once. **There is no undo.** The optional mod argument narrows it to one namespace and reports any mobs that couldn't be spawned. If you run it standing over the void, it builds sandstone display platforms (with water basins for aquatic mobs). Use a throwaway world: it freely overwrites blocks — think twice before running it on a shared server.

## Admin commands

Separate from the picker, `/sg admin` (operator level 2, works on any server) mutates the *live* eye state of the mob you're looking at — the same state the shears/potion/dye gameplay drives:

| Command | Effect |
| --- | --- |
| `/sg admin eyes <true\|false>` | Give or remove eyes |
| `/sg admin tint iris <r> <g> <b>` | Set iris color (0–255) |
| `/sg admin tint cornea <r> <g> <b>` | Set cornea color (0–255) |
| `/sg admin tint clear` | Clear both color overrides |
| `/sg admin glow <on\|off\|config>` | Force glow, or revert to the mob's config |
| `/sg admin behavior <id\|random>` | Play an expression now (e.g. `blink`, `swirl`, `color_change`) |
