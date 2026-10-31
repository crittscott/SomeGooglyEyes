# Minecraft 26.3 port status

The source targets exact Minecraft 26.3 on Fabric, NeoForge, and Forge. It derives from the 26.2 checkout, which was itself never compiled. No 26.3 compilation, GameTest run, client launch, or dedicated-server launch has been performed. The owner runs Gradle, Git, and GitHub operations.

## Dependency baseline

| Component | Pin |
| --- | --- |
| Minecraft | `26.3` |
| Java | `25` |
| Gradle | `9.5.1` |
| Architectury Loom, no remap | `1.17.493` |
| Architectury plugin | `3.5.170` |
| Shadow | `9.4.3` |
| Fabric Loader | `0.19.5` |
| Fabric API | `0.163.0+26.3` |
| Forge | `26.3-66.1.0` |
| NeoForge | `26.3.0.76` |
| GeckoLib compile-only API | `com.geckolib:geckolib-{common,fabric,forge,neoforge}-26.3:5.5.7` |
| NightConfig | `3.8.3` |
| MixinExtras compile-only API | `0.5.3` |
| JSpecify | `1.0.0` |

The Fabric API and NeoForge pins and the unchanged Gradle toolchain follow the neighboring SomeStacks 26.3 build, which compiles and runs on Fabric and NeoForge. Forge `66.1.0` is the recommended 26.3 build. SomeStacks has no Forge module, so Loom `1.17.493` has not been shown to set up Forge 66. GeckoLib `5.5.7` is the newest release published for all four modules. Forge's ranges are `[66.1.0,67)` (Forge) and `[66,67)` (JavaFML); NeoForge's range is `[26.3.0.76,26.3.1)`.

## Source changes

The sources for these changes are the NeoForged 26.3 primer and SomeStacks' 26.3 port.

Pack metadata uses resource format 97.1 and data format 121.0. The combined pack spans `[97, 1]` to `[121, 0]`, and the GameTest fixtures declare `[121, 0]`.

The render-pipeline classes the picker gizmo uses (`RenderPipeline`, `BlendFunction`, `ColorTargetState`, `DepthStencilState`, `CompareOp`) moved from Blaze3d to `com.mojang.renderpearl.api.pipeline`; the gizmo imports and the access widener's `RenderPipelines` descriptors name the new package. `PoseStack#mulPose` became `rotate`, with `rotateDegrees` taking an `Axis`. Input moved from GLFW to SDL scancodes, so the picker key defaults use `InputConstants` key constants. `EnderMan` is now `Enderman`. `LootContext#getOptionalParameter` is now `getOptional`. `ResourceManager#listResources` takes a `Selector` instead of an identifier predicate, so the shipped-definitions GameTest lists eye files through `FileToIdConverter#listMatchingResources`. NeoForge's `LootModifier` takes one `Optional<Holder<LootItemCondition>>` like vanilla, and its synced per-world config type is now `ModConfig.Type.SYNCED`; the explicit `somegoogly-server.toml` file name keeps existing worlds' config files.

All eye-definition files are untouched. Eye-state keys, item component IDs, configuration formats, and protocol version 17 are unchanged. No migration layer is introduced.

## Unverified API assumptions

- `ColorTargetState` moved with the other pipeline classes. The primer's per-class list omits it, but its 26.3 sample code and blanket statement place it there.
- `InputConstants.KEY_LBRACKET` and `KEY_RBRACKET` are the names of the bracket constants. SomeStacks only confirms `KEY_V`.
- `Enderman#isCreepy` survived the rename, which the primer calls "not one-to-one".
- `SkullBlockRenderer#extractRenderState` still takes the `CrumblingOverlay`. The primer moves crumbling out of `submitModel`, which the skull Mixin targets by name only.
- NeoForge's global loot modifier condition is an optional field, so its `head_eyes.json` omits it. Its name is unknown.
- Forge's global loot modifiers still take `LootItemCondition[]` and a `conditions` list. Forge has not yet compiled.
- Forge's `ModConfig.Type.SERVER` still exists.
- GeckoLib 5.5.7's `RenderPassInfo` and `CompileRenderLayers` APIs used by the integration are unchanged.

## Owner verification

Use a Java 25 Gradle host JVM and the checked-in wrapper. Run these from this checkout, addressing failures before proceeding:

1. `clean build` for all three loaders.
2. Fabric, Forge, and NeoForge GameTest servers: 128 Fabric scenarios and 129 each for Forge and NeoForge.
3. Physical client and dedicated-server startup for each loader, including production JAR loading, checking that the Mixins and access rules apply.
4. Functional and visual checks: picker keys (V, the brackets, K) under SDL; slime and magma cube eyes inside the shell; cornea, iris, and glow translucency, including with improved transparency on; eyes on placed heads, held items, and GeckoLib mobs; a broken eyed head dropping an eyed item on every loader; the creepy enderman pulse; the gizmo drawing on top. Load a 26.2 save and confirm its server config is kept.
5. GeckoLib-absent and GeckoLib-present startup on each loader.
6. `generateDocs`, then `collectJars`.
