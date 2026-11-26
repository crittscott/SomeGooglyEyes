# The picker: in-game eye authoring

The picker is a creative-mode tool for placing eyes on a live, animated mob and exporting the result as [datapack JSON](datapack-format.md).

Requirements: **creative mode** and **operator permission level 2** (the level `/summon` needs), checked on the server, because picker edits change the world without firing protection events. The whole picker, including exporting, spawning, and the `mob` verbs, works from a remote client on any server running the mod.

## Keys

Rebindable in Options → Controls:

| Key | Action |
| --- | --- |
| `K` | Toggle the picker on/off |
| `V` | Choose/release the mob under your crosshair |
| `[` / `]` | Cycle through the mob's model parts |

While the picker is on, every eye-configured mob shows its eyes (ignoring the spawn roll) so you can see existing placements, and the chosen mob is frozen in place (AI off) while you edit it. A HUD panel in the top-right shows the chosen mob, selected variant, its age (adult/baby), selected part, and saved eyes; a 3-axis gizmo marks the selected part's origin and orientation.

## Workflow

Everything beyond part navigation is done through the `/sg` chat commands:

1. **Choose a mob**: look at it and press `V` (or `/sg choose`).
2. **Pick the attach part**: cycle with `[` `]`, or `/sg part <name|number>` (`/sg list parts` to see them all). The gizmo shows the part's local axes; eye positions relative to this coordinate system.
3. **Create an eye**: `/sg create <x> <y> <z>` places a draft eye at that offset.
4. **Shape it**:
   - `/sg move <x> <y> <z>` — set position (`~` leaves an axis unchanged)
   - `/sg rot <inclination> <azimuth>` — aim it (degrees; `~` supported)
   - `/sg posrot <x> <y> <z> <incl> <azi>` — both at once
   - `/sg properties eyescale <v>` / `irisscale <v>` — sizes
   - `/sg properties depth <v>` — thickness multiplier along the look axis (1 = standard; bigger protrudes further, e.g. to cover a modeled eyeball that sticks out of the head)
   - `/sg properties corneacolor <r> <g> <b>` / `iriscolor <r> <g> <b>` — colors (0–1)
   - `/sg properties glow <true|false>`
   - `/sg properties crosstarget <n>` — cross-eye partner (a saved eye number on the same part; `0` clears)
5. **Save it**: `/sg save` commits the draft to the eye list. Re-edit later with `/sg select <n>`, duplicate with `/sg dupe <n>` (copies eye *n* into a new unsaved draft — move it, then save), remove with `/sg delete <n>`, review with `/sg list eyes`.
6. **Variants** (optional alternative arrangements, weighted at spawn): `/sg variant new`, `/sg variant <n>` to switch, `/sg variant weight <w>`, `/sg variant del <n>`, `/sg list variants`.
7. **Export**:
   - `/sg export` sends the chosen mob's config to the server, which writes it into the world's datapack (`<world>/datapacks/somegoogly-picker/...`) and reloads, so it takes effect immediately and persists with the world. The written entry is tagged with whichever age the mob shows in the HUD (adult or baby) — it replaces only that age's entry for the mob, so an already-configured other age (or an `any` entry) is carried forward unchanged rather than overwritten. Rate-limited to one export per 10 seconds (each one is a full datapack reload).
   - `/sg exportall` dumps *every* known config — the loaded ones plus your session's drafts — to `<game dir>/somegoogly-export/` on the client, ready to copy into a mod or datapack. Drafts replace only their authored ages; untouched age entries, including disabled ones, are preserved.

**If you switch away from an edited eye before saving, your changes will be lost.**

Changes saved but not exported are kept per mob type and age for the current session, so you can hop between several mobs and `exportall` at the end. Choosing a mob that already has a config seeds the draft from it, so you edit existing placements rather than starting blank.

`/sg unchoose` (or `V` again) releases the mob; `K` exits the picker.

## Repositioning the chosen mob

While a mob is chosen you can move it:

- `/sg mob move <dx> <dy> <dz>` — nudge it by world-axis offsets (`0` leaves an axis unchanged; unlike the eye `move` verb, which sets absolutes)
- `/sg mob rot <azimuth>` — turn it in the XZ plane; azimuth uses the same convention as `/sg rot` (270 = facing −Z), so the numbers carry over

Both act on the live server mob (feedback comes back as a server message). Not to be confused with the eye `move`/`rot` verbs above, which shape the draft eye.

## Spawning test mobs

`/sg spawn <type>` spawns a single mob at the block you're targeting.

### The (dangerous) spawn grid

`/sg spawnall [mod]` **spawns one of every summonable living mob in a tidy grid**, grouped by mod, AI off, facing you, so you can audit or author eyes for many mobs at once. **There is no undo.** The optional mod argument narrows it to one namespace and reports any mobs that couldn't be spawned. If you run it standing over the void, it builds sandstone display platforms (with water basins for aquatic mobs). **Only run this on a test or throwaway world.** It freely overwrites blocks, and forcing a mob to spawn outside its mod's normal context — MineColonies' and Create's mobs are known cases — can corrupt or destabilize the world. Never run it on a shared or otherwise valuable server.

Spawnall is **disabled by default**: an admin must consciously set `allowSpawnAll = true` in the server config (`<world>/serverconfig/somegoogly-server.toml`, `Picker` section) to enable it. The check is server-side, so a modified client can't bypass it. Set it back to `false` (or restart without it) once you're done auditing.

To keep junk entities out of the grid, list namespaces in `spawnExcludedMods` or specific ids in `spawnExcludedEntities` (same `Picker` section). Both `/sg spawn` and `/sg spawnall` skip anything they match; a namespace-scoped `spawnall` run reports each skipped id so you can see why it's missing. `spawnExcludedMods` ships with `minecolonies` and `create` excluded by default for the world-corruption reason above — only remove either entry on a world you're prepared to lose.

Each mob and passenger is inserted individually, with the insertion call's result deciding acceptance. A mob refusal and a passenger refusal have distinct diagnostics; if a passenger is refused, previously added members are discarded. Immediate UUID lookup visibility is not used to reject an accepted spawn. Planned platforms, roofs, and basins are written only after all members are accepted. The distant-grid regression scenario on both loaders checks that multiple accepted candidates beyond the nearby chunk area each receive a platform.

## Admin commands

`/sg admin` (operator level 2, creative mode) mutates the *live* eye state of the mob you're looking at (the same state the shears/slimy-eye/dye gameplay drives):

| Command | Effect |
| --- | --- |
| `/sg admin eyes <true\|false>` | Give or remove eyes |
| `/sg admin tint iris <r> <g> <b>` | Set iris color (0–1) |
| `/sg admin tint cornea <r> <g> <b>` | Set cornea color (0–1) |
| `/sg admin tint clear` | Clear both color overrides |
| `/sg admin glow <on\|off\|config>` | Force glow, or revert to the mob's config |
| `/sg admin behavior <id\|random>` | Play an expression now (e.g. `blink`, `swirl`, `color_change`) |

## Spellbooks placement checks

With Iron's Spells 'n Spellbooks 1.20.1-3.16.4 in a creative test world, turn on the picker and inspect `irons_spellbooks:ice_spider`: four eyes should cover the front black patches and four should cover the top patches. Check Scapegoat and Spectral Steed from both sides. Debug Wizard, Cryomancer, Citadel Keeper, Fire Boss, Frozen Humanoid, and Priest should have no eye arrangement.

Use Raise Dead to summon zombies and skeletons with the picker on. Their eyes should remain on their faces throughout the rising animation and after it ends. Choose each summon, adjust a head eye, and release it; the preview should use the same head coordinates before and after rising. Reload resources and repeat to check that nested renderer layers are reinstalled without duplicate eyes. This requires a client; dedicated-server GameTests cannot observe either rendering path.

## Ice and Fire placements

The Myrmex and Dread definitions target the original Ice and Fire 2.1.13 beta 5 for Minecraft 1.20.1. Placement references are its `ModelMyrmex*`, `ModelDread*`, and `RenderDreadHorse` sources and textures under `textures/models/myrmex` and `textures/models/dread`. The horse uses the existing vanilla horse head placement because `RenderDreadHorse` uses the vanilla `HorseModel` and `ModelLayers.HORSE`.

Myrmex eyes attach directly to `EyeR` and `EyeL`, on each box's local negative-Y face. In the worker, soldier, royal, queen, swarmer, and pupa textures, the dark pixel is at UV `(47, 2)` and the white pixel at `(47, 4)`; the center between their pixel centers maps to local model coordinates `(0, -1, -0.5)`. The sentinel's longer eye box and shifted texture map that center to `(0, -1, -1)`. Its baby entry uses the pupa placement: `EntityMyrmexBase.isBaby()` covers both immature growth stages, and `RenderMyrmexBase` renders both with `ModelMyrmexPupa`. All Myrmex corneas have a diameter of three model pixels (`eyeScale = 3 / (2 * 1.865)`).

The scuttler's six face eyes sit on `HeadBase`'s local negative-Y face, which its animated head rotation turns toward the viewer. Each upper triangular group of dim cyan pixels gets one eye; the four brighter lower patches get one each. The remaining two eyes sit on the negative-X and positive-X faces. Dread eyes have white corneas and glowing cyan irises. The thrall's head cube is inset by half a model pixel, so its eye centers account for the smaller cube; the lich eyes extend to the front of its hood.

In a creative test world with the original mod installed, choose each Myrmex caste, including the swarmer, and check both sides. The eyes should cover the dark/white marks and follow head movement. Check an immature sentinel as well as an adult. Choose each living Dread type: beast, ghoul, horse, knight, lich, scuttler, and thrall. The scuttler should show six face eyes and one on each side; check the lich's hood and thrall's headwear for clipping. These placements are derived from source and textures; their appearance still needs an in-game check.

## Unsupported models

The unsupported-model message names the entity type under the crosshair. Ice and Fire trolls turn into a separate `iceandfire:stone_statue` entity in daylight when exposed to the sky; those statues use a custom renderer that the picker does not support. Spawn a fresh troll at night or under a solid roof to edit its eyes. Existing statues remain statues at night. To verify the feedback, choose a statue and check that the rejection names `iceandfire:stone_statue`, then choose a live troll under cover. This requires a client; headless GameTests cannot inspect live renderers.
