# TODO

## 1. Uranus model support (Ice and Fire Community Edition)

Goal: attach eyes to, and author eyes for, mobs whose models come from the Uranus library
(`com.iafenvoy.uranus`), the same way we already do for Citadel, LLibrary, and GeckoLib.

Status: steps 1, 2, and 5 are done. Step 3 (in-game verification) needs a build. Step 4 (coverage
for the 11 undefined mobs) needs picker authoring in game.

### Why this is small

Uranus is a 1.21.1 port of the same gegy1000/pau101 model toolkit that Citadel shades. Every handle
`CitadelResolver` reflects exists in Uranus under the same name and signature:

| Handle | Citadel | Uranus |
| --- | --- | --- |
| all-boxes list | `AdvancedEntityModel#getAllParts()` | same, `Iterable<AdvancedModelBox>` |
| parent pointer | `AdvancedModelBox#getParent()` | same, set by `addChild` |
| pose transform | `AdvancedModelBox#translateAndRotate(PoseStack)` | same body: translate, Z/Y/X rotate, scale |
| intrinsic name | `public String boxName` | `public final String boxName` (may be null) |

Only the two fully qualified class names differ:

- `com.github.alexthe666.citadel.client.model.AdvancedEntityModel` / `.AdvancedModelBox`
- `com.iafenvoy.uranus.client.model.AdvancedEntityModel` / `.AdvancedModelBox`

All of the token, path, field-name-fallback, `#N`, chain, and cache logic already lives in
`ReflectedBoxResolver`; `CitadelResolver` is only the reflective handles over it.

Rendering and the picker share one strategy interface, so a single new resolver covers both. The only
consumers of `Resolvers.forModel` are `LayerGooglyEyes`, `PickerLayer`, and `ModelPartVocabulary`
(which feeds `[`/`]` cycling, `/sg list parts`, `/sg part`, `/sg export`, `/sg exportall`). Nothing
else is model-family aware. Nothing server-side changes at all.

### Environment facts (from `othersourcecode/`)

- Ice and Fire CE is **NeoForge only** on 1.21.1: `neoforge.mods.toml` is the sole metadata, built
  with ModDevGradle against `neo_version=21.1.248`. There is no `fabric.mod.json`.
- `mod_id=iceandfire` (unchanged from the original mod), `mod_version=2.1.2`.
- Requires `uranus >= 2.3.2` and `jupiter >= 2.3`.
- Entity ids are unchanged from the original mod (`fire_dragon`, `cockatrice`, `gorgon`, ...).
- CE does **not** use GeckoLib for entities (GeckoLib appears only as a `compileOnly` integration).
- Every mob renderer is a `MobRenderer`, so `ClientRenderLayers` installs our layers with no change.
  `StoneStatueEntityRenderer` is a plain `EntityRenderer` and is out of reach (statues, not mobs).
- No model applies a whole-model transform inside `renderToBuffer`, so `ThirdPartyModelWraps` needs
  no new entries. The renderers' whole-body `scale()` overrides run before layers, so eyes inherit
  them already.
- Models are created once per renderer at registration; no runtime model swapping.
- Every living model derives from `AdvancedEntityModel`, directly or through `DragonBaseModel`,
  `BipedBaseModel`, or `DreadBaseModel`. The one plain `BasicEntityModel` is `ChainTieModel`, a
  non-living entity. Dragons and the sea serpent are `TabulaModel`s (tabula-named cubes);
  hand-written models use the nameless box constructor and rely on our Java-field-name fallback.
  `BipedBaseModel` holds its boxes in `HideableModelRenderer` fields, a subclass of
  `AdvancedModelBox`, which the fallback scan already accepts (`isAssignableFrom`).

### Step 1 - Resolver (the only code change) - DONE

Make the Citadel resolver family-parameterized rather than cloning it; the two families are
structurally identical and should not be free to drift.

1. Rename `CitadelResolver` to `AdvancedModelBoxResolver` (same package, still extends
   `ReflectedBoxResolver`).
2. Move `HANDLES` from a static field to a final instance field, loaded from constructor arguments:
   the family label, the model class name, and the box class name. `Handles.load(...)` keeps its
   current shape - `ClassNotFoundException` means "mod absent", any other throwable warns once and
   disables the family.
3. Add two static factories (or constants): `citadel()` and `uranus()`, each supplying its pair of
   class names and a label.
4. Replace the literal `"CitadelResolver"` strings in the `warnOnce` calls with the family label, and
   override the label used by `ReflectedBoxResolver.disableIntegration` / `fallbackNames`, which
   currently pass `getClass().getSimpleName()`. With one class serving two families those log lines
   would otherwise be ambiguous. Add a `protected String familyLabel()` to `ReflectedBoxResolver`
   defaulting to `getClass().getSimpleName()` and use it in both warn sites.
5. Register the Uranus instance in `Resolvers.ALL`, immediately after the Citadel one. Order is
   otherwise unchanged; `ChildMapResolver` stays last. The two instances hold independent per-model
   caches and independent `integrationFailed` flags, which is what we want - a Citadel failure must
   not disable Uranus.
6. Update the `{@link CitadelResolver}` references in the `EyeAttachmentResolver` and
   `ReflectedBoxResolver` class javadoc, and rewrite the resolver's class javadoc to describe both
   families (Citadel: Alex's Mobs and pre-CE Ice and Fire; Uranus: Ice and Fire CE).

No new source tree, no loader-specific code, no `@ExpectPlatform` method: this is pure reflection,
unlike the GeckoLib bridge which needs typed subclassing.

### Step 2 - Bundled definitions - DONE

The 14 bundled `data/iceandfire/eyes/*.json` files still name the right entities, but their version
selectors predate CE.

**Version selectors.** Every entry declares `[2.1.13-1.20.1,2.2)`. CE reports `2.1.2`, which sorts
*below* that lower bound (2 < 13), so no entry matches and
`EyeConfigReloadListener.selectForLoadedVersion` falls back to the nearest generation - the eyes
still appear, but every world load logs a misleading "expected after a mod downgrade" warning.
Rewrite the selector to `[2.1.2,2.2)` (what `VersionRangeMatcher.rangeFor("2.1.2")` synthesizes).
Replace, do not add a second entry: our mod is 1.21.1-only, so the 1.20.1-era range is unreachable
and is legacy commentary in data form. This is a mechanical edit of one field in 14 files - use a
script, not the picker.

**Attach tokens.** Checked statically by rebuilding each CE model's box hierarchy from its
`addChild` calls and applying our normalization and suffix-match rules:

| Definition | Tokens resolving | Note |
| --- | --- | --- |
| amphithere, cockatrice, cyclops, ghost, hippocampus, hippogryph, pixie, siren, stymphalian_bird, troll | 10/10 | unchanged field names and hierarchy |
| gorgon | 13/14 | see below |
| fire_dragon, ice_dragon, lightning_dragon | not statically checkable | tabula `.tbl` files are archives; not opened |

Gorgon's fourth left snake head is stored as
`Tail_1/Body/Head/Head_Details/SnakeBaseL4/SnakeBodyL4/SnakeHeadL4`, but CE has no `SnakeHeadL4`
box - the child of `SnakeBodyL4` is named `SnakeHeadR4_1` (an upstream naming slip; the L4 chain is
otherwise intact). Retarget that one head to `.../SnakeBaseL4/SnakeBodyL4/SnakeHeadR4_1`. Without
it, that gorgon head silently renders eyeless.

For the three dragons, every segment of `BodyUpper/Neck1/Neck2/Neck3/Head/HeadFront` exists as a
tabula cube name (confirmed from `getCube(...)` calls in the dragon animators), but the parent chain
itself can only be confirmed in game.

### Step 3 - Verification (manual; requires a build) - PENDING

Resolvers are client-side, so the dedicated-server GameTests cannot cover any of this. Physical
client, NeoForge 21.1.x, with Ice and Fire CE 2.1.2, Uranus 2.3.2+, Jupiter 2.3+:

1. One hand-written model (cockatrice) and one tabula model (fire dragon): eyes attach and track the
   head through idle and flight animation.
2. Gorgon: all 14 snake heads have eyes after the L4 retarget.
3. Baby and adult where the model branches on age (hippogryph, hippocampus, amphithere).
4. Picker: `/sg choose` a dragon, confirm `/sg list parts` enumerates, `[`/`]` cycles, `/sg create`
   places, and `/sg export` round-trips. Expect tabula part order to look arbitrary (see below).
5. `/reload` and a resource-pack reload: caches clear and eyes survive (`Resolvers.clearCaches`).
6. Confirm the server log no longer carries the version-fallback warning for `iceandfire`.

### Step 4 - Coverage expansion (optional, picker work, no code) - PENDING

11 living CE mobs have no definition. By model family, so the effort is predictable:

- `DragonBaseModel` (hand-written, field names): deathworm, dread_beast, dread_scuttler.
- `BipedBaseModel` / `DreadBaseModel`: dread_ghoul, dread_knight, dread_thrall, dread_lich. These
  share the `body/head` shape, so one definition can be adapted across all four.
- `TabulaModel`: sea_serpent.
- Vanilla `HorseModel`: dread_horse - already resolvable today by the existing vanilla resolvers; it
  only needs a definition, probably a copy of the vanilla horse one.
- **hydra: not reachable.** Its nine heads are drawn by `HydraHeadFeatureRenderer` from nine separate
  `HydraHeadModel` instances that are not part of the renderer's parent model, so our layer cannot
  see them. Only the body model is in reach, and it has no head. Leave hydra unsupported.
- stone_statue: not a `LivingEntityRenderer`; out of scope.

### Step 5 - Docs - DONE

Uranus was added to the supported-family sentences in `README.md`, `docs/curseforge-description.md`,
and `docs/modrinth-description.md`, the bundled-mod lists now name Ice and Fire Community Edition,
and `player-view.md` records that its definitions select that mod's 1.21.1 releases and are outside
the blanket "unverified optional-mod selectors" caveat.

`as-built.md` was left alone: it sits at its own stated 12k-character budget, and the fact that one
resolver class serves two libraries is visible from that one file, which is what the document says to
leave out. Add a sentence there only by trimming something else.

### Known limitations to accept, not fix

- `TabulaModel.getAllParts()` returns `cubes.values()` from a name-keyed `HashMap`: duplicate cube
  names collapse (one box becomes unreachable), and enumeration order is hash order rather than
  authoring order, so picker part cycling on dragons will feel arbitrary. Path tokens are unaffected.
- Ice and Fire CE ships for NeoForge only, so this resolver will never fire on our Fabric or Forge
  artifacts unless another Uranus-based mod appears there. The resolver itself is loader-agnostic.

---

## 2. Bug: parent box scale is not canceled for reflected box families

Separate from the Uranus work; affects Citadel today and would affect Uranus identically.

**What happens.** `AdvancedModelBox.render` applies its own `translateAndRotate` (which ends in
`scale(scaleX, scaleY, scaleZ)`) and then, when `scaleChildren` is false - the default - multiplies
in the *inverse* scale before descending to its children, so a parent's scale does not reach them.
`ReflectedBoxResolver.BoxChain.apply` replays `translateAndRotate` for every box from the root down
and never cancels anything, so an eye attached below a scaled ancestor inherits a scale the real
render path discards.

**Where.** `ReflectedBoxResolver` (chain apply and `applyTransform`), for both the Citadel and Uranus
families. Verified present in both libraries' `AdvancedModelBox`.

**When it shows.** Only when an ancestor in the chain has scale != 1 *and* `scaleChildren` is false.
Concrete case in Ice and Fire CE: `HippogryphModel` sets `Head.setShouldScaleChildren(false)` with
`Head` scaled 1.5 for babies, so anything attached below `Head` is drawn 1.5x too large and displaced
along the chain. Cases that set `setShouldScaleChildren(true)` (Hippocampus, Amphithere babies) are
correct as-is, and a scaled *leaf* is also correct, since its own scale genuinely applies.

**Shape of the fix.** `applyTransform` needs to know whether a box is the last link in the chain: for
every non-terminal box whose `scaleChildren` is false, apply `translateAndRotate` and then the
inverse of `scaleX/scaleY/scaleZ` (clamped as the library does, `1/max(scale, 0.0001)`). That means
two more reflective handles per family (the three scale fields and the `scaleChildren` flag) and
passing a terminal flag through `BoxChain.apply`. Worth doing after Uranus lands, so both families
get it from one change.
