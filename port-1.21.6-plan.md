# Port plan: Minecraft 1.21.5 → 1.21.6

This checkout started as a verbatim copy of 1.21.5 (commit `c77d1b2`). 1.21.6 is an unreleased waypoint toward 1.21.8, so there is no new world-migration obligation: the only supported prior release stays 1.21.1, and the existing Fabric migration only has to keep working on the new save API. Eye definitions do not change (they select 1.21.4 and fall back automatically).

Each phase ends at a point where you can build. Phases 1–3 are needed for `common` and Fabric to compile; Forge only compiles after Phase 5. Items marked **Verify** are API facts I could not confirm from published loader docs; check them against the IDE (or tell me) when the phase starts, rather than letting me guess.

## Phase 1 — Versions, metadata, pack formats

1. `gradle.properties`:
   - `minecraft_version = 1.21.6`, `minecraft_version_range = [1.21.6]`, `parchment_minecraft = 1.21.6`, `parchment_version = 2025.06.29` (Verify: newest 1.21.6 Parchment date).
   - `fabric_api_version` → latest `+1.21.6` build; `fabric_loader_version` stays `0.16.14` unless Fabric API demands newer.
   - `forge_compile_version` → latest `1.21.6-56.0.x`; `forge_version_range = [56.0.x,57)`; `forge_loader_version_range = [56,57)`.
   - `neoforge_compile_version` → latest `21.6.x`; `neoforge_version_range = [21.6.x,21.7)`.
   - `geckolib_version` → the GeckoLib build published for 1.21.6 (Verify on Modrinth: artifact names are `geckolib-<loader>-1.21.6`; the version number may or may not change from 5.1.0).
2. Root `build.gradle`: Loom `1.17.493`, Architectury plugin, and Shadow should not need to change. Confirm Architectury Loom's NeoForge/Forge 1.21.6 support at that version; bump only if setup fails.
3. `common/src/main/resources/pack.mcmeta`: `pack_format` 80, `supported_formats` `{min_inclusive: 63, max_inclusive: 80}` (data pack 80, resource pack 63). `common/src/gametest/resources/pack.mcmeta`: `pack_format` 80. Verify both numbers once against the 1.21.6 changelog.
4. Every `[1.21.5]`/`=1.21.5` in loader metadata comes from properties, so nothing else should need text edits; grep afterward for stray `1.21.5`.

**Build checkpoint (you):** Gradle sync/setup on all three loaders, before any Java changes, to confirm the toolchain resolves.

## Phase 2 — Save/load moves to `ValueInput`/`ValueOutput` (common)

Vanilla's entity and block-entity persistence no longer passes `CompoundTag`/`HolderLookup.Provider`. The mod's own NBT stays `CompoundTag` internally (`EyeState.Snapshot.read/write`, `EntityPersistentData`, the Fabric attachment codec); only the edges where vanilla hands us its save stream change.

### `SkullBlockEntityDataMixin`
- `loadAdditional(ValueInput input)`: `somegoogly$data = input.read(SkullEyeState.DATA_KEY, CompoundTag.CODEC).orElse(null);`
- `saveAdditional(ValueOutput output)`: `if (data != null) output.store(SkullEyeState.DATA_KEY, CompoundTag.CODEC, data.copy());`
- `removeComponentsFromTag(ValueOutput output)`: `output.discard(SkullEyeState.DATA_KEY);`
- `applyImplicitComponents`/`collectImplicitComponents`: unchanged, but Verify signatures (`DataComponentGetter`, `DataComponentMap.Builder`).
- Keep storing the raw compound rather than switching to a `Snapshot` codec: the field is also what `SkullEyeState.Holder` exposes and what update tags carry, and a format change would need a migration for released 1.21.1 heads.
- Check `SkullEyeState` and anything else calling `getUpdateTag`/`saveWithoutMetadata`/`saveCustomOnly`; those still return `CompoundTag` in 1.21.6 (Verify) and should be untouched.

### Other common code to sweep
- `grep` for `CompoundTag` parameters on any vanilla override or Mixin injector across `common`, `fabric`, `forge`, `neoforge`. The inventory so far finds only the two Mixins in this phase and Phase 3, but the compiler will flag stragglers.

## Phase 3 — Fabric migration on the new load path

`EntityPersistentDataMigrationMixin` injects at `Entity.load` TAIL, which now takes a `ValueInput`.

1. Mixin: `somegoogly$migratePersistentData(ValueInput input, CallbackInfo callback)` → `FabricEntityDataMigration.migrate(entity, input)`.
2. `FabricEntityDataMigration.migrate(Entity, ValueInput)`:
   - Read the released value as an arbitrary tag, not as a compound, so a non-compound value is neither lost nor reported as a decode problem: `input.read(RELEASED_KEY, ExtraCodecs.NBT)` (Verify that `ExtraCodecs.NBT` is the `Codec<Tag>`; if not, `Codec.PASSTHROUGH` → `dynamic.convert(NbtOps.INSTANCE).getValue()`).
   - Then the existing logic: compound → `setAttached(TYPE, copy)`; anything else → set aside under `SET_ASIDE_KEY` and log.
   - Update the class javadoc ("Called at the end of `Entity.load` with the input the entity was loaded from").
3. The key is never written back: vanilla rebuilds the save from `addAdditionalSaveData`, and Fabric writes the attachment itself, so no change is needed on the save side. Verify that Fabric API 1.21.6 still persists entity attachments marked `persistent` without extra setup.

## Phase 4 — GUI: picker HUD registration

`PickerHud.render(GuiGraphics, int, int)` uses only `fill` and `drawString` with fully opaque ARGB text colors (the backdrop's alpha `0xA0` is fine for `fill`), so the HUD body should compile unchanged under the deferred `GuiRenderState`. Each loader's registration changes:

- **Fabric** (`FabricClientEvents`): replace `HudLayerRegistrationCallback`/`IdentifiedLayer.SUBTITLES` with `HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES, id, (graphics, tickCounter) -> …)`. Same id `somegoogly:picker`. Update orientation-code's "after the vanilla subtitle layer" only if the anchor changes.
- **NeoForge** (`NeoForgeClient.registerGuiLayers`): `RegisterGuiLayersEvent.registerAboveAll(id, layer)` is expected to remain. Verify that the `GuiLayer` lambda shape `(GuiGraphics, DeltaTracker)` is unchanged.
- **Forge** (`ForgeClientBootstrap.registerGuiLayers`): vanilla `LayeredDraw` was removed, so `AddGuiOverlayLayersEvent.getLayeredDraw().add(...)` must change. Verify what Forge 56 offers in its place (its own layered-draw wrapper or a new overlay registration event). This lands together with Phase 5 since that listener registration changes anyway.
- `Gizmo` draws in world space through `MultiBufferSource`, not GUI, and is unaffected by this phase.

## Phase 5 — Forge 56: EventBus 7

Forge 56 replaces the event system. All Forge listener wiring is rewritten; the gameplay code each listener calls is unchanged. Read Forge's EventBus 7 migration guide first and confirm these assumptions:

- Mod bus: `FMLJavaModLoadingContext.getModBusGroup()` (a `BusGroup`) instead of `getModEventBus()`; mod-bus events register via `SomeEvent.getBus(modBusGroup).addListener(...)`.
- Game bus: `SomeEvent.BUS.addListener(...)` instead of `MinecraftForge.EVENT_BUS.addListener(...)`.
- Priority: `net.minecraftforge.eventbus.api.listener.Priority.LOWEST` instead of `EventPriority.LOWEST`.
- Cancellation: cancellable events use a `CancellableEventBus`; a listener that may cancel is a `Predicate` that returns `true` to cancel, instead of calling `setCanceled(true)`. Check whether `setCancellationResult` survives on `PlayerInteractEvent`.
- Supertype listening: check whether a listener for `ModConfigEvent` still receives `Loading`/`Reloading`/`Unloading`, or whether each subtype needs its own listener.
- Some events may have become records (accessor renames such as `getEntity()` → `entity()`); the compiler will list them.
- `Event.Result.DENY` on `RightClickBlock.getUseItem()`: check its replacement type.
- Removal: `addListener` returns a listener handle; `BUS.removeListener(handle)` replaces `unregister(consumer)`.
- Posting: `BUS.post(event)` returns whether the event was cancelled.

Files and what changes:

| File | Change |
| --- | --- |
| `forge/SomeGoogly.java` | Take the mod `BusGroup`; pass it to the registrar, loot modifier, config, and client bootstrap; register `AddReloadListenerEvent` on its own bus. |
| `registry/forge/ForgeContentRegistrar.java`, `server/forge/HeadEyesLootModifier.java` | `DeferredRegister.register(BusGroup)`. |
| `server/forge/ForgeServerEvents.java` | `register()` takes no bus. Each of its 17 listeners moves to its event's `BUS`. The three cancelling `LOWEST` interaction listeners become predicates returning "acted", keeping the cancellation-result call if it still exists. `onRightClickBlock`'s `Result.DENY` check uses the new type. `onLivingDrops` stays `LOWEST`. |
| `config/forge/ForgeServerConfig.java`, `ForgeClientConfig.java` | Config-event listeners on the mod bus group, split per subtype if supertype listening is gone. Unloading must still reset SERVER values. |
| `client/forge/ForgeClientBootstrap.java` | Mod-bus listeners (renderer layers, GUI layer, key mappings) and the six game-bus listeners move to the new API; plus the Phase 4 HUD change. |
| `client/forge/ForgeGeckoLayers.java` | `GeoRenderEvent.Entity.CompileRenderLayers` on its bus (Verify GeckoLib's Forge event bus API for 1.21.6). |
| `network/forge/ForgeNetworkTransport.java` | Check `CustomPayloadEvent` handling and `ChannelBuilder` still compile; Forge's `PayloadChannel` marking-handled behavior must be preserved. |
| Forge gametests: `SomeGooglyGameTestMod` | Register the function `DeferredRegister` with the mod bus group. |
| Forge gametests: `PickerAuthGameTests` | Temporary refusing listeners: `MobSpawnEvent.FinalizeSpawn` (`setSpawnCancelled`, which is not event cancellation, so probably a plain listener) and `EntityJoinLevelEvent` (cancelling predicate). Remove each through its handle in `finally`. |
| Forge gametests: `SkullEyeGameTests.deniedItemUseLeavesHeadsUntouched` | Post through `RightClickBlock.BUS` and assert on `post`'s cancelled return value instead of `event.isCanceled()`. |

Keep the existing javadoc rationale for `LOWEST` (protection mods get first refusal). After the change, orientation-code's "Forge/NeoForge `LOWEST`" wording still holds.

**Build checkpoint (you):** `:forge:build` and `:forge:runGameTestServer`.

## Phase 6 — NeoForge 21.6 review

As far as known, NeoForge's surface the mod uses is stable. Changes come from vanilla (Phases 2 and 4). Work through compiler errors only. Known non-issues: `INBTSerializable` was removed (unused here); `RenderLevelStageEvent` was split into per-stage events (unused); `getPersistentData()` remains.

## Phase 7 — Client rendering and Mixin targets

Compile and run, then fix only what breaks. The 1.21.6 notes that touch areas the mod uses:

- `SkullBlockRendererMixin`: `SkullBlockRenderer.getRenderType` was split into `getSkullRenderType`/`getPlayerSkinRenderType`. The mixin targets `render(SkullBlockEntity, float, PoseStack, MultiBufferSource, int, int, Vec3)` and the `renderSkull` → `SkullModelBase.renderToBuffer` call. Confirm both descriptors still exist, and that `renderSkull`'s parameters still match the injector.
- `SkullSpecialRenderer` is now a `NoDataSpecialModelRenderer` taking only a `RenderType`. Check that head-item eyes (bracketed through `ItemStackRenderStateMixin`/`ItemModelResolverMixin`) still identify the head type.
- `ItemStackRenderState` gained model-identity and oversized-in-GUI state. Check that `GooglyEyeItemRenderer` (a `SpecialModelRenderer`) implements any newly required method, such as `getExtents`.
- `Gizmo`: `RenderPipeline.builder(LINES_SNIPPET)`, `RenderPipelines.register`, `RenderType.create(..., RenderPipeline, CompositeState)` and `ShapeRenderer.renderLineBox` are expected to survive; the uniform type changes do not affect a pipeline built from a vanilla snippet. Confirm that the access widener entries still resolve. Loom reports any that don't match.
- `GooglyEyeRenderer`'s comment about the view rotation living in `RenderSystem` should be checked against the 1.21.6 camera/projection handling. Pupil gravity uses `pose.last().pose()`, which should be unaffected.
- GeckoLib integration (`GeckoIntegration`, `GooglyGeoLayer`, `GeoBones`): fix against the 1.21.6 GeckoLib build if its render-layer API moved.
- Check on Fabric only: `EntityHeadDropMixin` targets `spawnAtLocation(ServerLevel, ItemStack, float)`. 1.21.6 adds a `Vec3`-offset overload. If the float form now delegates to it, the HEAD injection still fires. If vanilla's head-drop paths call the new overload directly, retarget the mixin to it. The existing head-drop gametests (charged-creeper and wither-skeleton heads) catch this.

## Phase 8 — GameTests

1. Entity save/load round trips in `SomeGooglyGameTestsLogic`, `SkullEyeGameTestsLogic`, `PickerSpawnServiceGameTestsLogic`, and Fabric `SomeGooglyGameTests`:
   - Save: `TagValueOutput out = TagValueOutput.createWithContext(reporter, registries); entity.saveWithoutId(out); CompoundTag saved = out.buildResult();`
   - Load: `entity.load(TagValueInput.create(reporter, registries, saved));`
   - Use a `ProblemReporter.ScopedCollector` (with the mod logger) in try-with-resources, the vanilla idiom, so decode problems show up in the test log.
   - Add a small shared helper in the common gametest package (for example `saveEntity`/`loadEntity`) rather than repeating the boilerplate in four files. It is used by both common logic and the Fabric wrappers.
2. `skull.saveWithFullMetadata(registries)` in `SkullEyeGameTestsLogic`: expected to still return `CompoundTag`; adjust if not.
3. The Fabric migration tests (`fabricReleasedEntityDataMigratesOnLoad`, `fabricUnrecognizedReleasedEntityDataIsSetAside`) keep their meaning: they build a 1.21.1-format compound and load it through the new path. Update only the save/load mechanics.
4. Forge gametest changes are listed in Phase 5. NeoForge's are expected to need only the shared helper.
5. Test counts stay 131 on Fabric and 129 on Forge/NeoForge unless a new test is added. None is planned: the port adds no player-visible behavior.
6. Test-instance JSON under `forge/` and `neoforge/` `src/gametest/resources/data/somegoogly/test_instance/`: confirm the format did not change in 1.21.6 (the primer lists only `GameTestTicker`/`prepareTestStructure` changes, so it is probably unchanged).

**Build checkpoint (you):** `gradlews.ps1` (clean build, `runGameTestServer` on all loaders, `generateDocs`, `collectJars`).

## Phase 9 — Documentation and snapshots

- `orientation-build.md`: Minecraft, Parchment, Forge, NeoForge, Fabric API and GeckoLib versions; compatibility ranges; pack formats 80/63; "This checkout targets Minecraft 1.21.6".
- `orientation-code.md`: version identity; Fabric HUD anchor wording, if changed; any change to how Forge registers listeners worth one clause.
- `orientation-player.md`: target version, loader minimums, GeckoLib minimum, and "unverified against 1.21.6 releases of those mods".
- `build-env/`: re-copy every changed build input (`gradle.properties`, any `build.gradle`, wrapper if bumped) and refresh the orientation outlines if headings changed.
- `docs/javadoc` is regenerated by `generateDocs`; no hand edits.
- Commit message (on request): one compact paragraph in the style of `c77d1b2`.

## Manual checks after GameTests pass (you, in game)

- Picker HUD appears and stacks correctly with subtitles on all three loaders.
- Gizmo draws through the mob with depth off.
- Placed heads, head items, and the first-person held eye render their eyes.
- GeckoLib mob with eyes (any GeckoLib mod with a 1.21.6 build).
- Fabric: open a world saved by the 1.21.1 release and confirm that eyed mobs and heads survive. Forge/NeoForge: the same, if a 1.21.1 test world is on hand.
- Forge: a protection-style refusal still blocks Slimy Eye use (the `LOWEST` ordering under EventBus 7).

## Open verification items (collected)

1. Exact latest versions: Forge 56.0.x, NeoForge 21.6.x, Fabric API +1.21.6, Parchment date, and the GeckoLib 1.21.6 artifact.
2. Pack format numbers 80/63.
3. Whether `ExtraCodecs.NBT` is the `Codec<Tag>` (Phase 3).
4. The Forge 56 replacement for `AddGuiOverlayLayersEvent.getLayeredDraw()` (Phase 4).
5. EventBus 7 specifics: mod `BusGroup` accessor, `Priority` package, how predicate cancellation works, supertype listening for `ModConfigEvent`, the `Result` type on `RightClickBlock`, `setCancellationResult`, and listener removal (Phase 5).
6. GeckoLib Forge event registration under EventBus 7.
7. `SkullBlockRenderer.render`/`renderSkull` descriptors and the `SpecialModelRenderer` contract (Phase 7).
8. `spawnAtLocation` delegation (Phase 7).
