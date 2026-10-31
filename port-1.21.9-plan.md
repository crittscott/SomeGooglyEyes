# Port plan: Minecraft 1.21.8 → 1.21.9 — remaining work

Written, not yet built:

- Versions bumped: Minecraft 1.21.9, Parchment 2025.10.05, Forge 59.0.5, NeoForge 21.9.16-beta, Fabric Loader 0.17.2, Fabric API 0.134.1. The pack metadata now uses `min_format`/`max_format` (64–88), and keeps the legacy `pack_format`/`supported_formats` that this range still requires. The GameTest pack declares only 88.
- GeckoLib has no 1.21.9 build. The code compiles against `geckolib-*-1.21.10:5.3-alpha-3` (`geckolib_minecraft`) and is ported to its submit API and its `CompileEntityRenderLayersEvent`. It can't load on 1.21.9 and gets tested at 1.21.10.
- Rendering moved to `SubmitNodeCollector`. Each eye part is a `submitCustomGeometry` call with its values captured. `ModelGooglyEye` is static and stateless (its old `moveIris` state is gone). `LayerGooglyEyes`, `Gizmo`, `GooglyEyeItemRenderer`, and `GooglyGeoLayer` all submit.
- Placed heads: a new `SkullBlockRenderStateMixin` carries the eyes chosen in `extractRenderState`, and `submit` is bracketed. `SkullEyeRenderer.render` poses the skull model before it walks the head part. Head-item Mixins are retargeted to `ItemStackRenderState.submit` and `appendItemLayers(…, ItemOwner, …)`.
- The picker keys use a `KeyMapping.Category` (`somegoogly:picker`, lang key `key.category.somegoogly.picker`). NeoForge registers it through `registerCategory`. Fabric and Forge register it with vanilla's `KeyMapping.Category.register`.
- Fabric reload listeners moved to resource-loader v1 (`ResourceLoader.registerReloader`). `FabricEyeConfigReloadListener` is deleted.
- `GameProfile.name()`, `hand.asEquipmentSlot()`, and the `PlayerModelType` loops in the Forge and NeoForge `AddLayers` handlers.
- README, orientation files, and `build-env/` updated.

## Build checkpoints (you)

1. Gradle sync on all three loaders.
2. `:fabric:build` and `:fabric:runGameTestServer` (128 tests).
3. `:forge:build` and `:forge:runGameTestServer` (129 tests).
4. `:neoforge:build` and `:neoforge:runGameTestServer` (129 tests).
5. `gradlews.ps1`.

## What the compiler or Mixin startup may flag

| Area | Assumption in the code | If wrong |
| --- | --- | --- |
| `SkullBlockRendererMixin` | `extractRenderState(SkullBlockEntity, SkullBlockRenderState, float, Vec3, CrumblingOverlay)` and `submit(SkullBlockRenderState, PoseStack, SubmitNodeCollector, CameraRenderState)` descriptors. `submitSkull(Direction, float yRot, float mouth, PoseStack, SubmitNodeCollector, int light, SkullModelBase, RenderType, int outline, CrumblingOverlay)` submits the model and then calls `PoseStack.popPose()` once | Match the real descriptors and parameter list |
| `SkullEyeRenderer.render` | `SkullModelBase.State` has a no-arg constructor and `yRot`/`animationPos` fields | Build the state the way `submitSkull` does |
| `ItemStackRenderStateMixin` | `submit(PoseStack, SubmitNodeCollector, int light, int overlay, int outline)` | Match the parameters |
| `GooglyEyeItemRenderer` | `SpecialModelRenderer.submit(T, ItemDisplayContext, PoseStack, SubmitNodeCollector, int, int, boolean, int)` and `Unbaked.bake(SpecialModelRenderer.BakingContext)` | Match the signatures |
| `LayerGooglyEyes` | `RenderLayer.submit(PoseStack, SubmitNodeCollector, int, S, float, float)`. `LivingEntityRenderer.getOverlayCoords` is unchanged | Match the signature |
| `Gizmo` | `ShapeRenderer.renderLineBox(PoseStack.Pose, …)` keeps its float arguments | Adjust the call |
| Key category | `KeyMapping.Category` is a public record with a `ResourceLocation` constructor and a static `register(ResourceLocation)`. Forge accepts vanilla registration, and Fabric doesn't register the category itself | Use the loader's own registration |
| `pack.mcmeta` | A range of 64–88 with the legacy fields loads both as a data pack (88) and a resource pack (69) | Read the log's pack-metadata error |
| GeckoLib | The 5.3-alpha-3 API matches the `1.21.10` branch: `addRenderData(…, partialTick)`, `addPerBoneRender(…, didRenderModel, …)`, the 8-argument `PerBoneRender`, and `CompileEntityRenderLayersEvent` per loader. Fabric's remap of the 1.21.10 jar resolves | Follow the alpha's real API |

## Content (in game)

1. **Copper golem:** author adult definitions in the picker and export them.
2. **Mannequin:** mannequins have their own renderers, one per player model type. Forge's `AddLayers` exposes them (`getMannequinRenderer`), so Forge installs the eye layer on them. NeoForge's handler still installs only player renderers; check whether its event has an equivalent. Then decide whether mannequins share the player's definition. They are listed as having none for now.
3. The vanilla audit and happy ghast work from the 1.21.8 plan are still open.

## Manual checks (you)

- Mob eyes follow head motion when several mobs of the same type are in view. This checks the `setupAnim` call in `LayerGooglyEyes`.
- Each eye has its own pupil position and colors. Glowing eyes, sleeping-villager lids, picker previews, and the gizmo all draw.
- Placed heads (piglin ears especially), eyed versus plain head items in the hotbar, and the held eye all render correctly.
- The picker key category shows "Some Googly Eyes" under Controls on every loader.
- A charged creeper killing an eyed skeleton drops an eyed head on every loader (also covered by a gametest).

Delete this file once the port is built and verified.
