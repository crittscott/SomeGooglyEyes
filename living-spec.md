# Some Googly Eyes: Architecture Overview

> **Status:** implementation-grounded overview.  
> **Scope:** current Java implementation and shipped data format, reviewed from the existing living specification.  
> **Audience:** AI assistants and maintainers who need a compact mental model of the mod before working on a small part of the codebase. This is not intended to restate every implementation branch or test case.

## 1. Purpose

Some Googly Eyes adds configurable, animated googly eyes to living Minecraft entities.

The core design separates four concerns:

- **Datapacks** describe where eyes attach to an entity model and what their default appearance is.
- **The server** decides whether a particular entity has eyes, persists that state, chooses stable placement variants, and synchronizes authoritative data.
- **The client** renders eyes, runs the local wobble simulation, applies local visibility preferences, and hosts the in-world placement picker.
- **Eye items** carry portable appearance only. They do not carry placement. When an appearance is applied to an entity, the entity's datapack placement is used.

The mod id is `somegoogly`.

## 2. Status vocabulary

- **Implemented:** present in normal runtime registration.
- **Partial:** present but intentionally limited.
- **Experimental:** implemented, but not verified by runtime compatibility testing.
- **Deferred:** architectural seam or intended feature without complete gameplay implementation.

Use these labels conservatively. In particular, dedicated-server support and external renderer compatibility should not be promoted without a recorded runtime check.

## 3. Architectural ownership

This is a **both-sides mod**.

The server owns datapack loading, definition selection, entity eye state, appearance overrides, spawn rolls, gameplay mutations, behavior scheduling, and synchronization. A server without the client can own state but cannot render anything.

The client owns rendering, wobble simulation, local render vetoes, picker previews, model attachment resolution, GeckoLib rendering integration, and most authoring commands. A client without the server can render only from server-selected definitions and synchronized entity state.

Package layout reflects this split. Client-only systems live under `com.github.crittscott.somegoogly.client`; shared and server-side systems live under `com.github.crittscott.somegoogly`. The package boundary is a useful orientation aid, but runtime side ownership is the real rule.

## 4. Runtime lifecycle

The normal flow is:

1. Server reload reads `data/<namespace>/eyes/*.json` from active datapacks.
2. Definitions are selected by namespace mod version and age selector.
3. When an entity first joins a server level, the server may assign eyes and stores a stable placement-variant roll.
4. Selected definitions are synced to clients during datapack sync.
5. Entity eye state is synced when a client starts tracking that entity and whenever the state changes.
6. Client render layers combine synced placement, synced entity state, local wobble, optional behavior playback, and local preferences.

The server-selected datapack configuration is authoritative. The client should not invent placements outside picker preview mode.

## 5. Persistent entity state

Living entities may carry these Forge persistent-data keys:

| Key | Purpose |
| --- | --- |
| `somegoogly:hasGooglyEyes` | Whether the entity currently has eyes. |
| `somegoogly:eyeVariantRoll` | Stable random value used to select one weighted placement variant. |
| `somegoogly:eyeOverrides` | Optional appearance override: cornea color, iris color, and glow. |

The eye flag is initially a spawn-time decision, but gameplay can later change it. The variant roll is stable for the entity's life so the visual arrangement does not reroll on save/load, tracking changes, dimension changes, or aging.

Eligibility for initial eyes depends on whether the server has a usable enabled definition for the entity's current or alternate age. This prevents an entity from being permanently excluded just because it is currently a baby or adult while only the other age has a definition.

Players are a special case. They have a datapack definition and can render eyes, but they are excluded from the at-spawn roll. They start without eyes and can gain them only through the splash potion. Because the state is ordinary persistent data, player eyes are lost on respawn.

## 6. Datapack definition model

An entity's eye file is located at:

```text
data/<entity namespace>/eyes/<entity path>.json
```

For example, `minecraft:axolotl` maps to `data/minecraft/eyes/axolotl.json`.

The supported top-level shape is:

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
              "eyes": [ ... ]
            }
          ]
        }
      ]
    }
  ]
}
```

Each entry is selected by namespace version and age. `age` may be `adult`, `baby`, or `any`; an age-specific entry takes precedence over `any`. `enabled:false` is a server-authoritative disable.

`variants` are complete arrangements. A single arrangement is still represented as a one-element `variants` list. Variant weights are relative, and the stored per-entity roll maps deterministically onto the selected age configuration.

Each variant contains one or more `heads`. A head is an attachment group and must name an `attachPoint`. The token is the resolver's canonical vocabulary: a model part or bone name when available, or `#N` for index-only reflection models.

Each eye is a flat object containing placement, scale, direction, color, glow, and invisibility behavior. The important architectural point is that placement lives in datapack geometry, while appearance can be overlaid by per-entity or item-derived overrides.

Legacy entry-level `heads` is not a supported placement shape. Current data uses `variants`.

## 7. Configuration

Server configuration controls the creation and behavior of eyed entities. The major categories are:

- global enable/disable for new spawn decisions;
- global and per-entity spawn percentages;
- harvest chance when killed with shears;
- ambient behavior scheduling;
- event-driven behavior toggles and cooldowns.

Changing spawn percentages affects newly initialized eligible entities. It does not reroll stored entities.

Client configuration is local and only affects rendering. It can disable all googly eyes, specific entity ids, or entire mod namespaces. These preferences are not authoritative gameplay state and are not synced back to the server.

## 8. Rendering and attachment

The client adds googly-eye render layers to vanilla living-entity renderers, including player skin models. GeckoLib entity renderers receive a separate compatibility layer when GeckoLib is present.

A render pass requires all of the following:

- the entity is not currently being previewed by the picker;
- local client preferences allow rendering;
- the entity has synced eye state;
- there is a synced enabled definition for the entity;
- the model has a usable attachment resolver;
- the eye is not suppressed by invisibility or invalid scale.

Rendering combines datapack placement, effective appearance, local wobble state, at most one active cosmetic behavior, and normal/glowing passes as needed.

Attachment is resolved by model type:

| Resolver | Target | Token style | Status |
| --- | --- | --- | --- |
| Hierarchical | `HierarchicalModel` | part names | Implemented, but cube-less pivot parts are not selectable |
| Citadel | Citadel/LLibrary-style models | box names or `#index` | Implemented, compatibility-sensitive |
| Reflection fallback | other vanilla-style models | `#index` only | Implemented, brittle by design |
| GeckoLib | `GeoEntityRenderer` models | bone names | Implemented, version-sensitive |

The reflection fallback intentionally trades robustness for broad coverage. External renderer integrations should be treated as compatibility surfaces, not core guarantees.

## 9. Wobble and cosmetic behaviors

Wobble is client-only and transient. It is not persisted or synchronized. The same simulation concept drives mob eyes and held eye items, so they feel consistent.

The server owns behavior scheduling and triggers; the client owns visual playback. A behavior trigger identifies the entity, behavior id, duration, seed, and elapsed time. The seed makes playback deterministic across observers, and elapsed time lets newly tracking clients join an in-progress effect.

Built-in behavior ids are:

- `stare`
- `blink`
- `side_eye`
- `cross_eye`
- `grow`
- `swirl`
- `color_change`

Ambient behaviors are selected from the configured ambient pool. Event-driven behaviors currently include `grow` when a player damages an eyed mob and `swirl` for trade or heal events. `color_change` is registered for debug/admin triggering but is not part of normal ambient or event tracks.

Only one behavior runs on an entity at a time. Later triggers are dropped while one is active. Behaviors are cosmetic and do not persist.

## 10. Gameplay systems

### Eye item

`somegoogly:googly_eye` is a 3D item. Its NBT stores sparse appearance overrides: cornea color, iris color, and glow. It stores no placement, attachment, rotation, scale, or entity-specific geometry.

In hand, the item uses the same wobble concept as mob eyes. In static item contexts, the iris is centered.

### Harvest

Eyed mobs can be harvested in two ways:

- right-click with Optometrist-enchanted shears removes eyes non-lethally;
- direct shears kill may drop eye items according to the server harvest chance.

Harvested eye items carry the mob's effective appearance. Current overrides are per-mob, not per-eye, so asymmetric colors are not preserved as separate item properties.

Players with eyes are living entities for these systems and can be harvested like mobs.

### Crafting

The special `eye_modifier` recipe modifies a googly-eye item's appearance. Dyes set iris color, glowstone forces glow on, redstone forces glow off, and cobweb clears overrides back to datapack defaults. The recipe preserves unrelated NBT.

### Potion

A custom brewing recipe turns an awkward splash potion plus a googly-eye item into the `somegoogly:googly_eyes` splash potion and copies the eye item's appearance to the output.

On impact, the server chooses one nearby eligible eyeless living entity, including players, applies the potion appearance, and turns eyes on. This is the only normal gameplay path for giving an existing eyeless entity eyes.

### Enchantment

`somegoogly:optometrist` is a treasure-only shears enchantment used for non-lethal harvesting.

### JEI

JEI integration registers a representative brewing display for the custom splash-potion recipe. The real recipe still copies item appearance NBT. The custom `eye_modifier` recipe is not currently exposed through JEI and remains Partial.

## 11. Picker and authoring workflow

The picker is a client-driven in-world authoring tool. It is intended for creative-mode placement work. Writing datapack output is restricted to a single-player integrated server.

The basic workflow is:

1. Enable the picker.
2. Lock a living entity target.
3. Choose an attachment part.
4. Add, move, aim, scale, color, and save draft eyes.
5. Export the selected entity or export all known configs.

`/sg export` writes the committed target into the single-player world's datapack and reloads it:

```text
<world>/datapacks/somegoogly-picker/data/<namespace>/eyes/<entity>.json
```

`/sg exportall` writes all synced configs plus in-session drafts to:

```text
<gameDir>/somegoogly-export/data/<namespace>/eyes/<entity>.json
```

Picker drafts are retained per entity type for the session. Drafts seed from existing synced config when available, so editing an existing placement starts from current data instead of a blank state.

The picker also includes a single-player spawn grid command for authoring and debugging many living entity types. It is a development convenience and does not affect shipped runtime behavior.

Known picker limits: remote/multiplayer export is deferred, cube-less pivot authoring depends on resolver support, and a crash can theoretically persist temporary `NoAi` state used while freezing targets.

## 12. Commands

`/sg` is the user-facing root.

Client-side picker commands handle authoring and export. Server-side `/sg admin` commands are operator-only development tools for toggling eyes, changing appearance, and triggering behaviors on the looked-at living entity.

The client and server subtrees share the same root but occupy distinct paths. Single-player command fall-through is confirmed; dedicated-server client-to-server command behavior is not verified here.

## 13. Network protocol

The mod uses one `SimpleChannel` with protocol version `3` and three server-to-client packet types:

| Packet | Purpose |
| --- | --- |
| `EyeStatePacket` | Per-entity eye flag, variant roll, and optional appearance override. |
| `EyeConfigSyncPacket` | Server-selected runtime geometry definitions. |
| `EyeBehaviorTriggerPacket` | Transient cosmetic behavior trigger data. |

Clients clear synced definitions and render trackers on disconnect. Malformed config-sync entries are logged and skipped rather than aborting the whole payload.

## 14. Non-goals, seams, and compatibility limits

Current important seams and limits:

| Item | Status | Architectural meaning |
| --- | --- | --- |
| Per-eye mutable appearance | Deferred | Overrides currently apply uniformly to a mob. |
| Additional `EyeHolder` implementations | Deferred | The abstraction anticipates non-entity holders, but entities are the only concrete gameplay holder. |
| Dedicated-server compatibility certification | Experimental | Side guards exist, but runtime loadability is not recorded as verified. |
| Generic model attachment | Partial | Reflection and external renderer support favor coverage over perfect robustness. |
| JEI support for `eye_modifier` | Partial | Brewing is represented; modifier crafting is not. |

Do not treat source-derived compatibility as proven runtime behavior unless a test or manual check has recorded it.

## 15. Automated tests

A server-side Forge GameTest suite exists under `src/main/java/com/github/crittscott/somegoogly/gametest/` and runs headless through `runGameTestServer`.

The suite is intentionally server-only. It covers core data and server logic such as version selection, variant selection, serialization, packet round trips, entity state helpers, behavior determinism, server config matching, spawn roll endpoints, recipe transforms, and shipped config loading.

It does not cover client rendering, wobble, GeckoLib behavior, picker behavior, harvest integration, potion target selection, all datapack reload edge cases, behavior scheduler internals, or dedicated-server loadability. Those areas remain source-derived or manual unless separately verified.

## 16. Maintenance rules

Update this overview when a change alters the system's public or architectural contract, including:

- datapack schema or selection rules;
- server/client ownership;
- persisted NBT keys;
- packet contents;
- gameplay behavior for items, recipes, potions, enchantments, commands, picker, or harvesting;
- resolver support or compatibility status;
- status labels such as Implemented, Partial, Experimental, or Deferred;
- GameTest coverage at the level needed to avoid misleading confidence claims.

Keep this file as an overview. If a detail is only needed to understand a single implementation branch, prefer code comments, tests, or a focused subsystem note instead of adding it here.
