# Some Googly Eyes As-Built Orientation

This guide identifies the code boundaries and invariants a maintainer should understand before changing the mod. It is deliberately selective. `player-view.md` describes observable behavior; the code is authoritative when either document is wrong.

It should not contain history and it is not part of a conversation with the user. It should describe the code as it is. It is not a prose version of the code, it is an orientation.

This document describes what is, not necessarily what is desired. Do not take is to be a driving design document.

See [build-env.md](build-env.md) for the Gradle toolchain and module setup; this document covers what
code lives where and why.

## Project at a glance

Some Googly Eyes is an unreleased Minecraft 1.20.1 mod for Forge 47.4.10 and Java 17, mid-conversion
to a dual-loader Architectury Loom build. Its mod id is `somegoogly`; its root package is
`com.github.crittscott.somegoogly`. The declared mod version is `0.8.1`.

The Gradle project has three subprojects: `common`, `forge`, and `fabric`. Forge is fully implemented
and behaviorally unchanged from the pre-conversion single-module build (verified in-game, including
the `AgeableHierarchicalModel` baby-scale case). `fabric` is an empty stub with no code yet.

The mod adds configurable, animated googly eyes to living entities. Eye placement comes from
datapacks; the server owns eligibility and persistent entity state; the client owns model attachment,
physics, and rendering. The mod ships 243 eye-definition files for Minecraft and ten other mod
namespaces.

It registers two items (`googly_eye`, the harvested eye and portable appearance payload; `slimy_eye`,
an applicator made from a Googly Eye and slimeball), one treasure-only shears enchantment
(`optometrist`), one creative tab (`googly`), two recipe serializers (`eye_modifier` for dynamic
appearance edits, `slimy_eye` for the shapeless applicator with NBT transfer), and one command
argument (`maybe_float`, a picker coordinate/angle that also accepts `~`).

It registers no blocks, block entities, menus, entity types, particles, or custom sounds. It does
register client and server commands and a bidirectional network channel. Mod-version mismatches are
allowed by the display test, but the network protocol must match.

The project does not preserve old development data formats. Before release, change the stored or
datapack formats directly and update the bundled data rather than adding compatibility branches.

## Code map

| Area | Module | Owns |
| --- | --- | --- |
| `SomeGoogly` / `SomeGooglyCommon` | forge / common | Mod construction, registration, side-gated client setup, event subscribers; `SomeGooglyCommon` holds just the shared `MOD_ID`/`MOD_NAME`/`LOGGER` constants so `common` code never needs to depend on the entry point class |
| `config/` | split | Server/client TOML, datapack reload and version selection, separate server/client config stores |
| `eye/` | common | Datapack schema, selected head/eye geometry, placement math |
| `eye/state/` | split | Persistent entity state and portable appearance codecs |
| `eye/behavior/` | split | Behavior registry, deterministic behavior influences, server scheduler |
| `item/` | split | Eye items, item NBT, creative tab, tooltips, application verb |
| `recipe/` | split | Eye modifiers and appearance-preserving Slimy Eye recipe |
| `enchant/` | forge | Optometrist registration and shears-only rules |
| `event/` | forge | Spawn decisions, tracking sync, item interactions, reactions, client lifecycle |
| `network/` | forge | One protocol channel, three server-to-client packets, five picker requests |
| `client/` | split | Eye model, item renderer, per-entity wobble trackers, inspection indicator |
| `client/render/` | split | Render gating and common eye drawing |
| `client/render/resolver/` | split | Model-family attachment discovery, canonical tokens, caches |
| `client/compat/` | split | GeckoLib layers, Citadel/LLibrary-facing helpers, Exotic Birds and Alex's Mobs whole-model transforms |
| `client/picker/` | split | Picker state, preview, HUD, keyboard input, export-all |
| `command/` | split | Client `/sg` authoring tree, server `/sg admin` |
| `picker/` | split | Server-side picker authorization, freezing, spawn helpers, and world-datapack export |
| `gametest/` | forge | 67 server-side Forge GameTests |
| `resources/` | split | Eye definitions, recipes, translations, models, textures, access transformer |

### The common/forge boundary

Code stays in `common` unless it touches something only Forge's compile classpath provides: a Forge
API import; `Entity#getPersistentData()` (a member Forge adds to vanilla `Entity` with no import,
invisible to a grep for `net.minecraftforge`); a Forge-only addition to a vanilla class (e.g.
`EnchantmentCategory.create`); a member this mod's own access transformer widens (the AT is applied
only within `forge/`, never `common/`); or GeckoLib, wired as a `forge/`-only dependency. Citadel and
LLibrary stay `common` despite being third-party integrations, because they're reached through runtime
reflection rather than a compile dependency.

Concretely forge-only within each `split` row above: `config/`'s `ServerConfig`, `ClientConfig`,
`ModVersionLookup`, and `EyeConfigReloadListener` (via `ModVersionLookup`); `eye/state/EyeState`;
`eye/behavior/ServerBehaviorScheduler`; `item/`'s `GooglyEyeItem`, `ModCreativeTabs`, `ModItems`,
`SlimyEyeItem`; `recipe/`'s `ModRecipes`, `SlimyEyeRecipe`, `EyeModifierRecipe`; `client/`'s
`EyeInspectIndicator` and `SlimyEyeColors`; `client/render/`'s `EyeRenderGating` and `LayerGooglyEyes`;
every access-transformer-dependent resolver in `client/render/resolver/` (`ModelPartTreeResolver`,
`AgeableListResolver`, `ChildMapResolver`, `HierarchicalResolver`, `RabbitLlamaResolver`,
`TwilightForestResolver`) plus the `Resolvers` dispatcher that instantiates them — `CitadelResolver`,
`LLibraryResolver`, and the `EyeAttachmentResolver` interface itself stay `common`, backed by a small
`common`-side `AttachmentCache` that the interface's default method reads directly; the GeckoLib-facing
classes in `client/compat/` (`GeckoCompat`, `GeckoIntegration`, `GeoBones`, `GooglyGeoLayer` — but not
`AlexsMobsCompat`/`ExoticBirdsCompat`, which stay `common`); every `client/picker/` class except
`EyeDraft`; `command/`'s `GooglyAdminCommand` and `GooglyClientCommands`; `picker/`'s
`PickerExportService` and `PickerFreezeService`; and `resources/META-INF` (`mods.toml`,
`accesstransformer.cfg` — every other resource is `common`).

The intended layering is: datapacks describe placement and defaults; the server selects and owns
runtime truth; packets copy selected definitions and entity state to clients; resolvers enter the
correct animated model-part space; the tracker simulates each eye; the renderer draws the already
resolved state. Items carry appearance between entities but never carry placement.

## Runtime data flow

1. `EyeConfigReloadListener` reads `data/<namespace>/eyes/*.json` during datapack reload.
2. It selects entries by the installed version of the namespace owner and by age, then stores the
   result in `ServerEyeConfigs`.
3. The server sends the selected config set to clients on login and reload. Clients replace
   `ClientEyeConfigs` and invalidate dependent caches.
4. On a living entity's first server join, `ServerEventHandler` stores its eye/no-eye decision and a
   placement-variant roll. Existing stored entities do not reroll.
5. Tracking players receive the entity's complete eye state. Later gameplay mutations broadcast a
   replacement state packet immediately.
6. The server schedules automatic cosmetic behaviors only for tracked mobs. Clients receive seeded
   triggers and fold the active behavior into their local eye simulation.
7. A render layer combines the synced placement, persistent appearance override, local wobble, and
   local visibility preferences.

## Persistent and portable state

### Living entities

Forge persistent entity data holds three keys: `somegoogly:hasGooglyEyes` (boolean),
`somegoogly:eyeVariantRoll` (float; a stable 0..1 roll mapped onto the current age config's weighted
variants), and `somegoogly:eyeOverrides` (a sparse compound of cornea color, iris color, and glow
override).

The initial flag and roll are written once. The flag survives save/load, dimension changes, and
aging. A Slimy Eye application draws a fresh variant roll before turning eyes on. Appearance
overrides apply uniformly to all configured eyes on the entity; there is no per-eye mutable state.

Players use the same fields but never receive the spawn roll's positive result. Because the mod has
no player-clone handler, their eye state is not copied to the replacement player entity after death.

### Eye items

Both items use `EyeProperties`, encoded through the same `AppearanceOverride` codec as entity state:
sparse cornea color, iris color, and an explicit glow flag, removed when empty.
`EyeItemProperties` is the only owner of this item schema. Geometry, entity id, attach point, variant,
and behavior never travel on the item.

### Picker freeze marker

While a picker user edits a mob, `PickerFreezeService` temporarily stores
`somegoogly:pickerPrevNoAi` on that mob and forces `NoAi=true`. Live records permit one frozen mob per
editing player and reject a second editor. Logout, unchoose, and normal server stop restore the
previous value; a stale marker repairs the value after a hard crash or unload.

## Datapack definition boundary

One definition file maps to one entity type:

```text
data/<entity namespace>/eyes/<entity path>.json
```

The schema is `ConfigFile -> entries -> variants -> heads -> eyes`.

- An entry requires `version`, `age`, `enabled`, and `variants`.
- `version` is an exact string or bracket range matched against Minecraft for `minecraft:` files and
  against the owning mod's installed version for other namespaces.
- `age` is `adult`, `baby`, or `any`; the age-specific entry wins over `any`.
- `enabled:false` is an authoritative hard off.
- A variant is a complete arrangement with a relative weight; negative weights are treated as zero.
- A head groups eyes under one `attachPoint`.
- An eye contains placement, scale, depth, aim, cross-eye target, default colors, and glow.

Every codec field is required. Picker exports therefore write a canonical, explicit form rather than
a sparse file whose meaning could drift with code defaults.

When no declared entry matches the installed version, the loader uses the newest generation older
than the installed version, or the oldest generation if the installed version predates all entries.
It logs the mismatch instead of dropping cosmetic placement. Files from namespaces whose mod is not
loaded are ignored. The ender dragon is hard-excluded regardless of datapack content because its
renderer cannot host these layers.

Attach tokens are slash-joined model-part or bone paths, normalized without case or punctuation and
matched by suffix. Thus `head` can match `root/body/head`; a longer path disambiguates duplicate leaf
names. A `#N` segment denotes a part that has no stable name.

## Eligibility and spawn decisions

The server is authoritative for whether an entity can wear eyes.

- A newly initialized non-player may roll only if the master switch is enabled and it has a usable
  enabled config at either life stage.
- Spawn chance comes from an exact entity override, otherwise the first matching wildcard override,
  otherwise `globalPercent`.
- The decision is stored for life. Configuration changes affect newly initialized entities only.
- Using a Slimy Eye is stricter: the target needs a usable config for its current age and must not
  already have eyes.
- A baby with only an adult config can roll eyes at spawn but renders none until it grows. It also
  refuses a Slimy Eye while still a baby.
- Players are excluded from automatic spawn assignment but remain eligible for manual application.

`ServerEyeConfigs.canEverWearEyes`, `ServerEyeConfigs.isEligible`, and
`RuntimeConfig.isUsable` are the shared predicates. Do not duplicate their rules in a new interaction
path.

## Appearance and harvesting boundary

Placement is shared datapack data. Appearance is the portable override layer.

Non-lethal harvest requires Optometrist-enchanted shears. It samples the first configured eye's
effective appearance, creates exactly one Googly Eye item, clears the entity override, turns the
entity's eyes off, and damages the shears. A direct player melee kill with shears uses the same item
construction after passing the configured percentage roll.

Exactly one item is produced regardless of the number of rendered eyes. This keeps the
eye -> Slimy Eye -> entity -> harvest cycle from multiplying items. Because the override is per mob,
harvest intentionally cannot preserve several different per-eye appearances.

`EyeModifierRecipe` accepts exactly one Googly Eye and one modifier. Dye replaces iris color;
glowstone and redstone set glow true or false; cobweb clears the entire override. `SlimyEyeRecipe`
copies the input eye's appearance onto its output.

## Behavior scheduling and simulation

`ServerBehaviorScheduler` is the sole scheduling authority. It keeps transient state only for mobs
being tracked or currently finishing an explicitly triggered behavior. At most one behavior may run
per mob; overlapping triggers are dropped and never queued.

Seven behaviors are registered under the `somegoogly:` namespace: `blink`, `stare`, `cross_eye`, and
`side_eye` draw from the ambient pool; `grow` fires on player-damage chance and `swirl` on trade or
heal; `color_change` is reachable only through the admin command by default. Each carries its own
duration and per-eye effect, defined at the behavior registration site.

Triggers carry the entity id, behavior id, duration, seed, and elapsed ticks. The seed keeps observers
deterministic; elapsed time lets a newly tracking client catch up mid-effect.

The client tracker models each pupil as a point mass constrained to a disk. Gravity is projected from
world down into the animated eye plane; linear and angular acceleration add pseudo-forces. The active
behavior supplies a spring or a visual overlay before integration. Off-screen eyes are not simulated,
and stale trackers are evicted.

## Rendering and model attachment

`EyeRenderGating` is the common decision point for vanilla-style and GeckoLib rendering. Normal eyes
render only when local client settings permit it, the entity has eyes, the entity is visible, and the
selected config is usable. Picker preview mode bypasses the stored eye flag so configured mobs remain
visible while authoring.

`GooglyEyeRenderer` owns drawing after the caller reaches attachment space. It applies datapack
placement, overlays the entity appearance, interpolates simulated state, draws cornea and iris, and
adds a full-bright pass when glow is active. The same model and render types are used by the custom
Googly Eye item renderer.

Attachment resolvers are ordered from strongest naming contract to broadest fallback:
`HierarchicalResolver` (vanilla `HierarchicalModel`, named paths), `TwilightForestResolver` (four
Twilight Forest models that are `AgeableListModel`-family but override its inherited wrap with their
own literals), `AgeableListResolver` (most vanilla ageable/list models), `CitadelResolver`,
`LLibraryResolver` (Mowzie's shaded legacy toolkit), `RabbitLlamaResolver` (`RabbitModel` and
`LlamaModel`, each with its own hand-rolled per-part-group wrap), `ChildMapResolver` (reflection
catch-all for `EntityModel`), and `GeoBones` (GeckoLib baked models, named bone paths).

Some models apply a scale/translate wrap around some or all of their parts inside their own render
method, entirely outside the captured part tree: `AgeableListModel`'s baby head/body wrap,
`AgeableHierarchicalModel`'s and `CamelModel`'s baby root wrap, `RabbitModel`'s/`LlamaModel`'s
per-part-group wraps (baby-only except `RabbitModel`, whose adult render carries its own wrap too),
and Twilight Forest's `DeerModel`/`NewDeerModel`/`PenguinModel`/`BunnyModel`, which extend
`QuadrupedModel`/`HumanoidModel` but replace the inherited wrap with their own hardcoded literals.
`AgeableListResolver`, `HierarchicalResolver`, `RabbitLlamaResolver`, and `TwilightForestResolver`
each replay the relevant wrap as a `NamedRoot` `preTransform`, re-reading the model's age flag inside
the replayed closure rather than at resolve time, since `Resolvers.ATTACHMENTS` caches the resolved
chain per (model instance, token) and one model instance renders every baby and adult of its type.

Part and bone resolutions, including misses, are memoized per model instance and token. Vanilla-side
caches must be cleared when resource reload replaces renderer models. GeckoLib is compile-only and
runtime-gated. Citadel and LLibrary integrations use reflection and create no hard dependency.

The slime renderer receives the eye and picker layers before its translucent outer shell. Exotic
Birds and Alex's Mobs each receive a class-name-keyed whole-model transform shim (`ExoticBirdsCompat`,
`AlexsMobsCompat`) applied to the pose stack before the resolver walk starts, at the `LayerGooglyEyes`/
`PickerLayer` call sites rather than inside a resolver. This is a separate mechanism from a resolver's
own `NamedRoot` `preTransform`: `ReflectedBoxResolver` (Citadel/LLibrary's shared base, see
`CitadelResolver`) has no per-part-group preTransform hook at all, so a Citadel model whose
`renderToBuffer` wraps its *entire* part list — most `Animal`/`TamableAnimal` Alex's Mobs models do,
gated on `young` — needs the wrap reproduced before any box's own `translateAndRotate` is replayed, not
folded into one. A box's own `setScale()` calls (e.g. Grizzly Bear's baby head) need no such shim; a
box's live scale is already part of what `translateAndRotate` replays.

## Network boundary

`NetworkHandler` uses protocol version `6` over one channel with eight direction-locked packets:
server-to-client `EyeStatePacket`, `EyeConfigSyncPacket`, and `EyeBehaviorTriggerPacket`; and
client-to-server picker requests `PickerFreezePacket`, `PickerSpawnPacket`, `PickerSpawnAllPacket`,
`PickerMobPosePacket`, and `PickerExportPacket`.

Client-to-server picker handlers re-check creative mode and validate ids or payloads. `spawnall` also
requires the default-off server option. `PickerMobPosePacket` additionally restricts its target to the
sender's own `PickerFreezeService`-frozen mob and caps a move's offset magnitude at 20 blocks. Export
payloads are limited to 64 KiB. The full config sync warns above 900 KiB because vanilla's clientbound
custom-payload ceiling is 1 MiB.

## Picker and export boundary

The picker is client-driven but not client-authoritative. Keyboard input, drafts, preview rendering,
and `/sg` shaping commands are client systems; freezing, spawning, posing, and single-config export
are server operations authorized again at packet handling.

`/sg export` writes a canonical one-entry, `age:any` definition into:

```text
<world>/datapacks/somegoogly-picker/data/<namespace>/eyes/<entity path>.json
```

It then performs a full datapack reload. Successful exports are limited to one per player per 200
ticks. `/sg exportall` is a separate client-only dump of synced configs plus session drafts into
`<game directory>/somegoogly-export/`; it does not reload or modify the world datapack.

Drafts are retained per entity type for the current connection and seed from the currently selected
age config. They are cleared on disconnect so one server's work cannot be exported into another by
accident.

## Automated verification

The 67 GameTests cover server and shared logic: config matching and fallback, shipped config loading,
eligibility, spawn endpoints, persistent-state helpers, appearance codecs, packet round trips,
variant selection, behavior determinism, both recipes, picker export validation, and picker-freeze
recovery.

They do not verify client rendering, physics appearance, model compatibility, the held-eye indicator,
the complete event integration for harvest/application, or dedicated-server loadability. Those remain
source-derived or manual checks. The project instructions prohibit running the build or GameTests
unless the user explicitly requests it.

The GameTest classes currently live under ordinary `forge/src/main/java/.../gametest/` — moved there
wholesale during the Loom conversion, not yet split into a dedicated `gametest` source set. They still
compile and are still discovered by Forge's `@GameTestHolder` scan, but the proper Loom dev-mod
machinery (a shared `common/src/gametest` source set, a `somegoogly_gametest` dev mod per loader, an
empty-structure fixture) described in `build-env.md` has not been built yet.

## Known boundaries and limitations

- The `fabric` subproject is an empty stub; the mod does not run on Fabric yet. Every Forge-only
  touchpoint called out in "The common/forge boundary" above (registration, networking, config,
  persistent entity data, the access transformer, GeckoLib) still needs a cross-platform replacement
  before it can.
- The ender dragon can never be configured.
- An appearance override affects every eye on a mob uniformly.
- An `AgeableListModel`'s external baby scaling is not part of its captured part tree, so some baby
  placements may be slightly offset.
- Catch-all reflection, Citadel, LLibrary, GeckoLib, and the Exotic Birds/Alex's Mobs shims are
  compatibility surfaces; an upstream model or API change can break attachment without changing an
  entity id.
- `CitadelResolver`'s chain replay always compounds a box's own scale into its descendants, matching
  Citadel's `AdvancedModelBox.render()` only when that box called `setShouldScaleChildren(true)`. Where
  it didn't (e.g. Alex's Mobs' `ModelGeladaMonkey`, whose baby `neck` and child `head` are each scaled
  independently without opting `neck` in), an eye attached below that box is over-scaled relative to
  the real render. Not yet fixed; needs `ReflectedBoxResolver` to know about `scaleChildren`, not a
  model-specific wrap.
- A few mobs (vanilla Rabbit before its data was re-authored; Alex's Mobs' Bison and Tarantula Hawk)
  render babies through an entirely separate model class rather than a scaled adult one. Attach tokens
  still resolve if the baby model mirrors the adult's box names, but an eye position authored against
  the adult model does not carry over and needs its own age-specific datapack entry.
- One entity type that swaps among unrelated models can use only attach tokens shared by the active
  variants; a missing token simply draws no eye on that model.
- The dynamic eye-modifier recipe has no declared ingredient list for recipe viewers to inspect. The
  mod ships no JEI integration.
- Client entity/mod filters are checked both while layers are installed and while they render.
  Hiding is immediate; re-enabling a renderer omitted during layer installation may require a client
  resource reload or restart.
- Player eye state is not copied through death/respawn.

## Maintenance checklist

Before changing a relevant path, check the corresponding invariants:

- Before adding new code, check whether it touches a Forge API import, `Entity#getPersistentData()`, a
  Forge-only addition to a vanilla class, an access-transformer-widened member, or GeckoLib — any of
  those force it into `forge/`; everything else belongs in `common/` (see "The common/forge boundary").
- Keep placement in datapacks and appearance in `AppearanceOverride`; do not put geometry on items or
  mutable entities.
- Use `EyeState` for entity mutations so tracking clients are synchronized.
- Use `EyeItemProperties` for item NBT and remove the empty compound.
- Preserve the one-eye-in, one-eye-out harvest/application economy.
- Keep initial spawn eligibility age-independent, but Slimy Eye application current-age-specific.
- Keep the ender dragon hard exclusion consistent across reload, picker export, and spawn helpers.
- Encode all datapack fields explicitly and update all bundled files when the schema changes.
- Preserve normalized suffix matching and canonical attach tokens across resolvers and picker export.
- Clear model-keyed caches only when models are replaced, not merely when datapack definitions change.
- Read a model's age flag fresh inside a replayed `preTransform` closure, never bake it in when the
  closure is built — the same cached model instance renders every baby and adult of its type. The same
  rule applies to a whole-model compat shim (`ExoticBirdsCompat`, `AlexsMobsCompat`): check `young` at
  call time, not when the table entry is built.
- Keep normal and GeckoLib rendering on the same gate and `GooglyEyeRenderer` drawing path.
- Keep behavior scheduling server-owned, playback deterministic, and overlaps dropped rather than queued.
- Keep packets direction-locked and bump protocol version for any breaking wire change.
- Re-authorize every new picker mutation on the server; creative checks on the client are only UX.
- Scope mob-pose mutations to the sender's `PickerFreezeService`-frozen mob and keep an explicit
  magnitude cap on offsets, rather than trusting an arbitrary client-supplied UUID or distance.
- Keep `spawnall` behind its separate default-off server option.
- Preserve picker freeze recovery on unchoose, logout, unload/crash, and server stop.
- Keep the config-sync payload below vanilla's 1 MiB custom-payload cap.
- Treat external renderer and model-family support as compatibility-sensitive and fail by skipping an
  attachment, not by crashing the client.
- Route user-facing text (command feedback, tooltips, the picker HUD) through `en_us.json` via
  `Component.translatable`/`I18n.get`, not `Component.literal` or raw strings.
