# Minecraft 26.1.2 port status

The source targets exact Minecraft 26.1.2 on Fabric, Forge, and NeoForge. It derives from the 26.1.1 checkout, which was itself never compiled. No 26.1.2 compilation, GameTest run, client launch, or dedicated-server launch has been performed. The owner runs Gradle, Git, and GitHub operations.

## Dependency baseline

| Component | Pin |
| --- | --- |
| Minecraft | `26.1.2` |
| Java | `25` |
| Gradle | `9.5.1` |
| Architectury Loom, no remap | `1.17.493` |
| Architectury plugin | `3.5.170` |
| Shadow | `9.4.3` |
| Fabric Loader | `0.19.5` |
| Fabric API | `0.155.3+26.1.2` |
| Forge | `26.1.2-64.1.5` |
| NeoForge | `26.1.2.109` |
| GeckoLib compile-only API | `com.geckolib:geckolib-{common,fabric,forge,neoforge}-26.1.2:5.5.2` |
| NightConfig | `3.8.3` |
| MixinExtras compile-only API | `0.5.3` |
| JSpecify | `1.0.0` |

The NeoForge pin follows the neighboring SomeStacks 26.1.2 build. Fabric API `0.155.3` is the latest 26.1.2 release and satisfies GeckoLib 5.5.2's `0.153.0` requirement. GeckoLib's Gradle metadata brings that requirement into resolution, so an older pin leaves Fabric's injected `EntityLoadData` interface without its class. Forge `64.1.5` is the latest and recommended 26.1.2 build. The runtime Minecraft requirement is exact. Forge's ranges are `[64.1.5,65)` (Forge) and `[64,65)` (JavaFML); NeoForge's range is `[26.1.2.109,26.1.3)`.

## Source changes

26.1.2 is a vanilla hotfix. Resource format 84 and data format 101.1 are unchanged, so pack metadata and GameTest fixtures are unchanged. Every NeoForge class and nested event type the mod imports exists on NeoForge's `26.1.x` branch. NeoForge's `BreakBlockEvent` rename and its block-properties supplier change do not touch this mod. NeoForge's `LootModifier` now carries a codec-read `priority`, which `HeadEyesLootModifier` passes through.

Forge 64 changes that touch APIs the mod uses are additive: `ItemTooltipEvent` gains context and display getters, and Forge's hotbar rendering is split into components, which leaves the unanchored picker HUD layer unaffected. No Mixin targets `Gui`.

GeckoLib now publishes a Forge build for 26.1.2. Forge again compiles against it, registers `ForgeGeckoLayers` on GeckoLib's `CompileEntityRenderLayersEvent.BUS` when GeckoLib is present, and declares GeckoLib as an optional client dependency. The 5.5 to 5.5.2 changes are additive (plus bug fixes), so the common integration is unchanged.

All eye-definition files are untouched. Eye-state keys, item component IDs, configuration formats, and protocol version 17 are unchanged. No migration layer is introduced.

## Owner verification

Use a Java 25 Gradle host JVM and the checked-in wrapper. Run these from this checkout, addressing failures before proceeding:

1. `clean build` for all three loaders.
2. Fabric, Forge, and NeoForge GameTest servers: 128 Fabric scenarios and 129 each for Forge and NeoForge.
3. Physical client and dedicated-server startup for each loader, including production JAR loading, checking that the Mixins and access rules apply.
4. Functional and visual checks as for 26.1.1, including loading a 26.1.1 save.
5. GeckoLib-absent startup on each loader. GeckoLib-present integration on all three loaders, including Forge.
6. `generateDocs`, then `collectJars`.
