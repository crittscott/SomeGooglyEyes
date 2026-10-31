# Minecraft 1.21.11 port status

The source port is written. Common and Fabric production sources compiled, and the owner reports a successful Forge GameTest server run on Forge 61.1.5 with no warnings noticed. Forge 61.2.4 failed during Minecraft bootstrap before tests executed. Other loader startup and in-game behavior remain unverified. Gradle, Git, and GitHub operations remain with the project owner.

## Dependency baseline

| Component | Pin |
| --- | --- |
| Minecraft | `1.21.11` |
| Java | `21` |
| Parchment | `1.21.11:2025.12.20` |
| Fabric Loader | `0.18.4` |
| Fabric API | `0.141.6+1.21.11` |
| Forge | `1.21.11-61.1.5` |
| NeoForge | `21.11.48` |
| GeckoLib | `5.4.5`, Minecraft `1.21.11`, common and all three loaders |
| JSpecify | `1.0.0` |

The Minecraft, loader, and optional-integration ranges match these pins. Existing Gradle and Architectury tooling is retained. Artifact metadata was checked against the publishers' Maven repositories; this is not a Gradle resolution check.

## Source changes

- Production and GameTest code use `Identifier`, renamed buffer methods, the renamed resource-key accessor, and the documented entity/model package moves.
- Picker authorization queries `Permissions.COMMANDS_GAMEMASTER` in the player's permission set while retaining creative mode. Slimy Eye application checks the level's PvP setting.
- Eye rendering uses `RenderTypes`; the custom gizmo type uses `RenderSetup` and per-vertex line width. The cube markers use the same line-emission path as the axes. The access widener names the new render API and skull packages.
- Special item extents use `Consumer<Vector3fc>` throughout. Nullability annotations use JSpecify, including type-use placement for nested types and nullable arrays.
- GeckoLib uses `cache.model`, `GeoBone.name()`/`children()`, and `RenderPassInfo` per-bone callbacks. Bone tasks are deduplicated by identity; extracted eye data and copied picker drafts remain state-owned. The loader-specific compile-layer events retain their existing registration APIs.
- Shared pack metadata covers resource format 75.0 and data format 94.1. Its lower bound remains 64 so both pack readers permit the legacy metadata fields. GameTest metadata and generated picker packs use full major/minor versions without legacy fields.
- README and orientation files describe the target, and changed build inputs are mirrored in `build-env/`.

The [vanilla migration primer](https://docs.neoforged.net/primer/docs/1.21.11/) documents the Minecraft API changes. GeckoLib's own published `1.21.11` sources were consulted for its integration. Under case-specific permission, the published NeoForge and Forge HUD event/callback sources were checked: both callbacks still take graphics and a delta tracker, so their existing lambdas are retained.

## Build and startup checkpoints

Forge 61.1.5 is the checked-in compile/development baseline and minimum runtime version. The owner verified a successful GameTest server run with this version after 61.2.4 failed with `NoSuchMethodError: ByteBufCodecs$11.<init>()`. The precise cause of the 61.2.4 failure remains unresolved; the version comparison does not establish that Forge's build-tool rewrite caused it. The next Forge run needs no property overrides.

Run these from this checkout, addressing failures before moving to in-game verification:

1. Resolve the updated Gradle project and build all three loader modules.
2. Run Fabric's GameTest server: 128 registered scenarios.
3. Forge's GameTest server run succeeded on 61.1.5; run NeoForge's GameTest server. Each has 129 registered scenarios.
4. Launch each client and a dedicated server. Check Mixin injection and access-widener/transformer startup, not only compilation.
5. Regenerate documentation with `generateDocs` after the code compiles. Existing generated Javadoc has not been regenerated.

The exact vanilla Mixin injection points have not been inspected in Minecraft sources. Startup checks must confirm the living-entity, skull, item-state, milking, shearing, trade, and drop hooks.

## In-game verification

- Check ordinary/glowing eyes and expressions on several mobs of the same type, adult/baby models, moving heads, slime layers, and sleeping-villager lids.
- Check placed heads, piglin heads, eyed versus plain head items, first-person held-eye animation, inventory extents, and other item display contexts.
- Check picker keys, HUD, gizmo visibility through models, attachment selection, adult/baby drafts, export/reload, and exported pack metadata.
- Check creative operator authorization, refusal of creative nonoperators and survival operators, and server rejection of unauthorized picker requests.
- Check eye application/harvesting, protection cancellation, PvP/team restrictions, head drops, configuration reloads, joins, and reconnects on each loader.
- Check startup without GeckoLib, then bone attachment, animation, picker previews, and resource reload with GeckoLib and a compatible entity mod installed.
- Load a real save from the preceding released mod version and check entity, item, and head eye data. The mod's persistent keys, component IDs, codecs, and wire protocol version are unchanged.

No new mob definitions or model-placement corrections are included. Existing bundled Minecraft definitions still select the 1.21.4 generation; placement changes need observations from the in-game audit. Camel husk, nautilus, parched, and zombie nautilus have no bundled definitions, alongside the omissions listed in `orientation-player.md`.
