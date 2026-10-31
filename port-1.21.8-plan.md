# Port plan: Minecraft 1.21.6 → 1.21.8 — remaining work

Written, not yet built:

- Versions bumped: Minecraft 1.21.8, Parchment 2025.09.14, Forge 58.1.26, NeoForge 21.8.57, Fabric API 0.136.1, GeckoLib 5.2.2. Pack formats 81 and 64..81.
- Forge picker HUD now goes through `AddGuiOverlayLayersEvent` (`getLayeredDraw().add(id, layer)`, with no anchor, so it draws last). `GuiPickerHudMixin` and `somegoogly-forge.mixins.json` are deleted.
- 1.21.8 migrates only from 1.21.4. The Fabric entity-data migration (`FabricEntityDataMigration` and its Mixin) is removed, along with the Fabric config-header migration (`FabricConfigMigration`). Both converted only 1.21.1 data, which 1.21.4 already rewrites. Their three Fabric gametests are removed too, leaving Fabric with 128. Key completion with `.bak` stays.
- Changelog, README, orientation files, and `build-env/` updated.

## Build checkpoints (you)

1. Gradle sync on all three loaders.
2. `:fabric:build` and `:fabric:runGameTestServer` (128 tests).
3. `:forge:build` and `:forge:runGameTestServer` (129 tests). Forge 57.0.2 moved GameTest servers to vanilla's entry point. If no tests run, check the `gameTestServer` run in `forge/build.gradle`.
4. `:neoforge:build` and `:neoforge:runGameTestServer` (129 tests).
5. `gradlews.ps1`.

## What the compiler or Mixin startup may flag

| Area | Assumption in the code | If wrong |
| --- | --- | --- |
| `ItemModelResolverMixin` | `appendModelIdentityElement` is still on `ItemStackRenderState` in 1.21.7+ | Call it on `TrackingItemRenderState` through `instanceof` |
| `ItemStackRenderStateMixin` | `ItemStackRenderState.clear()` still exists | Retarget to its replacement |
| `ForgeClientBootstrap.addGuiLayers` | The layer lambda is `(GuiGraphics, DeltaTracker)` | Match the real shape |
| `SkullBlockEntityDataMixin` (1.21.6) | `ValueOutput.discard(String)` exists | Use the real key-removal API |
| `GooglyEyeItemRenderer` (1.21.6) | `getExtents(Set<Vector3f>)` | Match the signature |
| `SkullBlockRendererMixin` (1.21.6) | `render(…, Vec3)` and `renderSkull` descriptors | Adjust them |
| Fabric `EntityHeadDropMixin` (1.21.6) | `spawnAtLocation(ServerLevel, ItemStack, float)` sees head drops | Retarget to the `Vec3` overload |
| Gametests (1.21.6) | `saveWithFullMetadata` / `loadStatic` still use `CompoundTag` | Use `TagValueOutput` / `TagValueInput` |
| Forge listeners (1.21.6) | EventBus 7 details: `Priority`, predicate cancellation, `setCancellationResult`, `ModConfigEvent` subtypes, removal handles | Follow the EventBus 7 migration guide |
| Forge GeckoLib (1.21.6) | `CompileRenderLayers` registration under EventBus 7 | Follow GeckoLib's Forge module |
| Fabric | Fabric API 0.136.1 accepts Loader 0.16.14 | Raise `fabric_loader_version` |

## Content for the release (in game)

1. **Vanilla audit:** run `/sg spawnall minecraft` in a throwaway world and check eye placement. Pay particular attention to the 1.21.5 warm and cold cow, pig, and chicken models, adults and babies.
2. **Retag unchanged definitions:** list the vanilla mobs whose placement is still right, and I'll retag them to `"[1.21.4,)"` by script. That ends the misleading "optional-mod definitions" fallback warning at every reload. Re-export any that are off with `/sg exportall`.
3. **Happy Ghast and Ghastling:** author definitions in the picker (adult and baby) and export them. Then remove the happy ghast from the "no definition" lists in orientation-player and the changelog.

## Manual checks (you)

- Picker HUD draws above the rest of the HUD on all three loaders. On Forge this checks the new layer.
- An eyed head and a plain head of the same type draw differently in the hotbar (model identity).
- Placed heads, head items, the held eye, the gizmo, and a GeckoLib mob all render eyes.
- A protection-style refusal blocks Slimy Eye use on Forge.
- A world saved by the 1.21.4 release (Fabric especially) keeps its eyed mobs, heads, eye items, and server config values.

Delete this file once the port is released.
