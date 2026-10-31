# Minecraft 26.2 port status

The source targets exact Minecraft 26.2 on Fabric, Forge, and NeoForge. It derives from the 26.1.2 checkout, which was itself never compiled. No 26.2 compilation, GameTest run, client launch, or dedicated-server launch has been performed. The owner runs Gradle, Git, and GitHub operations.

## Dependency baseline

| Component | Pin |
| --- | --- |
| Minecraft | `26.2` |
| Java | `25` |
| Gradle | `9.5.1` |
| Architectury Loom, no remap | `1.17.493` |
| Architectury plugin | `3.5.170` |
| Shadow | `9.4.3` |
| Fabric Loader | `0.19.5` |
| Fabric API | `0.161.0+26.2` |
| Forge | `26.2-65.1.5` |
| NeoForge | `26.2.0.57` |
| GeckoLib compile-only API | `com.geckolib:geckolib-{common,fabric,forge,neoforge}-26.2:5.5.5` |
| NightConfig | `3.8.3` |
| MixinExtras compile-only API | `0.5.3` |
| JSpecify | `1.0.0` |

The NeoForge and Fabric API pins follow the neighboring SomeStacks 26.2 build, which uses the same Gradle toolchain. NeoForge `26.2.0.57` is the first stable 26.2 build. Forge `65.1.5` is the latest and recommended 26.2 build. GeckoLib `5.5.5` is the newest release published for all three loaders. Its Fabric build requires Fabric Loader `0.19.3` and Fabric API `0.158.0+26.2`, and its Forge build was compiled against Forge `65.1.3`. Its NeoForge floor was not checked. The runtime Minecraft requirement is exact. Forge's ranges are `[65.1.5,66)` (Forge) and `[65,66)` (JavaFML); NeoForge's range is `[26.2.0.57,26.2.1)`.

## Source changes

Pack metadata now uses resource format 88 and data format 107.1. The combined pack spans `[88, 0]` to `[107, 1]`, and the GameTest fixtures declare `[107, 1]`.

The vanilla entity-type constants moved from `EntityType` to `EntityTypes`, so every constant reference now uses `EntityTypes`, and `EntityType` remains only as a type and for its static methods. The per-color dye items are now the `Items.DYE` color collection, so the recipe GameTest picks red dye from it. The open-screen check reads `Minecraft.gui.screen()`. The picker gizmo pipeline binds its color target at index 0 with the blend-only `ColorTargetState` constructor, which takes the default format and writes every channel. Its render setup no longer declares a buffer size. The depth-stencil inversion leaves its always-pass, zero-bias state unchanged.

According to the NeoForged 26.2 primer, nothing changed in the Mixin targets or access-widener entries. `submitCustomGeometry` and `withStyle(ChatFormatting)` still compile in SomeStacks 26.2. Every loader event and HUD class the mod uses exists on its loader's 26.2 branch.

All eye-definition files are untouched. Eye-state keys, item component IDs, configuration formats, and protocol version 17 are unchanged. No migration layer is introduced.

## Unverified API assumptions

- `ColorTargetState(BlendFunction)` exists and defaults to the main target's format. The source is a third-party note about 26.2, not vanilla documentation.
- `RenderPipelines.LINES_SNIPPET` and `RenderPipelines.register` are still present for the access widener.
- `SlimeOuterLayer` still exists under the new `AbstractCubeMobRenderer`.

## Owner verification

Use a Java 25 Gradle host JVM and the checked-in wrapper. Run these from this checkout, addressing failures before proceeding:

1. `clean build` for all three loaders.
2. Fabric, Forge, and NeoForge GameTest servers: 128 Fabric scenarios and 129 each for Forge and NeoForge.
3. Physical client and dedicated-server startup for each loader, including production JAR loading, checking that the Mixins and access rules apply.
4. Functional and visual checks: slime and magma cube eyes inside the shell; cornea, iris, and glow translucency; eyes on placed heads, held items, and GeckoLib mobs; the gizmo drawing on top, under both OpenGL and Vulkan. Load a 26.1.2 save.
5. GeckoLib-absent and GeckoLib-present startup on each loader.
6. `generateDocs`, then `collectJars`.
