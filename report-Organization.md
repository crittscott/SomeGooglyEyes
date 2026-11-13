# Report: Organization (26.1.2)
Applied 2026-11-12, unbuilt (nothing compiled, built, or tested; source inspection only). Method: each 26.1.1 file diffed against 26.1.2 with `diff --strip-trailing-cr`; a file was copied whole only where the whole remaining diff was this topic's and it had no 26.1.2 addition, otherwise only this topic's hunks were taken by hand. Javadocs and Test-completeness hunks in the same files (mixin class javadocs, test javadoc rewrites, `TestPlayers` permission overload, real-trigger rewrites in BehaviorSchedulerGameTestsLogic, `BehaviorSettings.write` javadoc, scheduler "hurt/milked/sheared" and `GLOBAL_PERCENT` wording, ClientNetworkHandler "five" wording, PickerExporter cooldown link) were left. Line endings kept (CRLF files converted back after copying).

Copied whole from 26.1.1: EyeBehaviorTriggerPacket, OngoingBehavior, ServerServices, BehaviorSettings (minus `write` javadoc), ServerBehaviorScheduler (two javadoc phrases reverted), ClientLifecycle, ClientEyeRuntime (then re-patched for 26.1.2's head code), GooglyEyeItemRenderer, ItemStackRenderStateMixin, SkullBlockRendererMixin, EyeConfigReloadListener, PickerExportService, PickerSpawnService, eye/EyeAppearance, ForgeConfig, NeoForgeConfig, Forge/NeoForge Server/Client configs, Forge/NeoForge/Fabric server events (NeoForge's Heads-mod comment restored), HealReactionMixin, the five moved client-networking classes; gametest HeldEyeGameTestsLogic, SerializationGameTestsLogic, EyeConfigLimitsGameTestsLogic (CRLF restored), TestEyeRuntime, the three loaders' HeldEyeGameTests and GuiItemEyeGameTests (CRLF restored), the Forge/NeoForge `held_eye_swirls_when_healed` and `nested_draw_contexts_restore_outer_selection` test_instance JSONs, common `test_environment/held_eye_swirls_when_healed.json`.

## 1. Held eye restates the server's rules
- `client/HeldEye` deleted. `EyeBehaviorTriggerPacket` gains `Target` (ENTITY 0, MAIN_HAND 1, OFF_HAND 2); decode rejects an unknown target and any held behavior but grow/swirl; protocol stays "19" (no bump, per the brief).
- `ServerBehaviorScheduler`: `HandState`, `handBehavior`/`handPacket`, `reactHands`, `tickHands`, hand catch-up on start tracking and resume, all on `clock(holder)`; `onHurt(LivingEntity, DamageSource)` replaces `onPlayerHurt`; `onHealed` checks the side and uses `BehaviorSettings.canSwirlOnHeal`/`nextHealReadyAt`. Its "hurt/milked" and "5%" class-javadoc wording kept for Javadocs, as 26.1.1 did.
- `BehaviorSettings`: `AmbientSlot`, `ambientPool`, `ambientSlot`, `canSwirlOnHeal`, `handAmbientBusy`, `handIdentity`, `nextHealReadyAt`, `presentationSeed`, `rollGrowOnHit`, `slotTicks`.
- `ServerConfig.roll(RandomSource, int)` added (hunk only; the `availableBehaviors` comment hunk belongs to another topic) and used by the scheduler, `EyeItemService`'s kill harvest (inverted `>=` gone), and `ServerServices`' natural spawn.
- Client: `ClientNetworkHandler.handleBehavior` dispatches by target (code hunks only); `ItemModelResolverMixin` calls `ClientEyeRuntime.handContext`; `GooglyEyeItemRenderer` uses `currentDraw().draw()` and `EYE_LOCAL_POSE` in first person; `PresentationEyes` takes the `reaction`/`ambientExpression` fields, `react`, and the `BehaviorSettings` ambient helpers by hunk, keeping 26.1.2's `headsMod` field, `headParts(entity, helper)`, and the `placedHead(..., headsMod)` parameter.
- Tests: `HeldEyeGameTestsLogic` copied; new test-source `TestPlayers.create(template, creative)` written by hand without the permission overload (Test-completeness adds it); wrappers on all three loaders pass a template player (Fabric `FakePlayer.get`, Forge `makeMockServerPlayerInLevel`, NeoForge `FakePlayerFactory.getMinecraft`); `heldEyeSwirlsWhenHealed` gets `maxTicks = 400` and the new ninth test_environment on Fabric and in the Forge/NeoForge test_instance JSONs. `SerializationGameTestsLogic` copied (target round trips, held non-reaction, unknown target). `BehaviorSchedulerGameTestsLogic` got forced edits only: `onPlayerHurt(cow)` became `onHurt(cow, helper.getLevel().damageSources().playerAttack(player))`, `isAngry` became `OngoingBehavior.ANGER.serverAngry`, `ClientEyeRuntime.tick(level, b)` became `tick(level, b, null)`.
- NeoForge `TwilightHeadGameTests` and the Heads tests in `SkullEyeGameTestsLogic` reference nothing removed (searched for HeldEye, HandItemEyes, GuiItemEyes, BoatSimulations, onPlayerHurt, isAngry, itemDraw(), SkullEyeRenderer.begin/beginItem/end).

## 2. Client caches spread over five classes
- `ClientEyeRuntime` copied from 26.1.1 and re-patched for 26.1.2: `skullEyes(BlockEntity, Identifier)` with `SkullEyeState.headParts(entity, layout)` and `PresentationEyes.placedHead(..., SkullEyeState.isHeadsMod(skull.getBlockState().getBlock()))`, `BlockEntity` import instead of `SkullBlockEntity` (the Heads-aware form of 1.21.11, so Twilight trophies, skull candles, and Heads heads keep eyes). Otherwise as 26.1.1: boat, frame, head, hand, GUI, and mob caches; `tick(level, simulate, activeView)`, `tickGui`, `tickWorld`; `BoatSimulations` gone.
- `HandItemEyes`, `GuiItemEyes` deleted; `ClientLifecycle` makes one call with `minecraft.screen`.
- Container, GUI-item, and hotbar Mixins call `beginGuiPresentation`/`beginGuiDraw`/`endItemDraw` (code hunks only; class javadocs left to Javadocs).
- Test-source `TestEyeRuntime` copied; GUI, hand, and boat test logic switched to `TestEyeRuntime` caches, `ClientEyeRuntime.GuiKey`/`GuiKind`/`HUD`, `onBoatState`, `tick(level, false, null)`; their code now equals 26.1.1's, javadoc rewrites left to Test-completeness/Javadocs.

## 3. Three "what is being drawn" stacks
- One `ClientEyeRuntime.DrawContext`. `ItemStackRenderStateMixin` brackets `submit` with `beginItemState`/`endItemDraw`; `SkullBlockRendererMixin` follows 26.1.1 (`beginHead` from the `PlacedHolder` at `submit` HEAD, `endItemDraw` at RETURN).
- `SkullEyeRenderer` edited by hand, not copied: its own `DRAWS` deque, `DrawContext` class, `begin`, `beginItem`, and `end` removed; the Heads `fit`/`Placement`/`headField`/`headPart` code, `PLACEMENTS`, `clearCaches`, `renderTrophy`, and the Heads/Twilight branches kept; `render` and `renderHead` read `ClientEyeRuntime.currentDraw()` (`head()`, `item()`, `partialTick()`, `draw()`). Class javadoc names the new brackets. Deviation from 26.1.1: 26.1.2's per-context `itemEyesSubmitted` flag (item eyes go on the first submitted model only) cannot live on the immutable record, so it became a static `ClientEyeRuntime.@Nullable DrawContext itemEyesSubmitted` compared by identity, as 1.21.11 does, behind a small `hasEyesToDraw(draw)` helper used by both `render` and `renderHead`. As 26.1.1, the item animation comes from `draw.draw()` instead of `ClientEyeRuntime.itemDraw(stack)`.
- NeoForge `TwilightTrophyRendererMixin` and `TwilightSkullCandleRendererMixin` bracket `submit` with `ClientEyeRuntime.beginHead(placed.somegoogly$eyes(), placed.somegoogly$partialTick())` at HEAD and `ClientEyeRuntime.endItemDraw()` at RETURN, as 26.1.1's `SkullBlockRendererMixin` and 1.21.1's Twilight renderer mixins do.
- Trophy-model brackets: `TwilightTrophyModelMixin` and `TwilightKnightTrophyModelMixin` call `renderTrophy` from a `@ModifyArgs` on the `submitModelPart` call inside the model's `renderTrophy`, which receives the `SubmitNodeCollector` and so runs during a submission: for a placed trophy inside `TrophyRenderer.submit` (now bracketed by `beginHead`), for a trophy item inside `ItemStackRenderState.submit` (bracketed by `beginItemState`). This is the same assumption the old `SkullEyeRenderer` stack already relied on; Twilight's own call path is not inspected (third-party source).
- `nestedDrawContextsRestoreOuterSelection` added to `GuiItemEyeGameTestsLogic` (method copied from 26.1.1), the three `GuiItemEyeGameTests` wrappers, Forge/NeoForge `SomeGooglyGameTestMod` registrations, and two `test_instance/guiitemeyegametests/nested_draw_contexts_restore_outer_selection.json` (CRLF like their neighbors).

## 4. Baby-model compensation
None.

## 5. Model attachment resolution atomized
None.

## 6. Loader adapters
Forge `ForgeServerEvents`, Fabric `FabricServerEvents` and `HealReactionMixin` copied; NeoForge `NeoForgeServerEvents` copied with its 26.1.2 comment "such as the Heads mod's" restored. `onHurt(entity, source)` and `onHealed(entity)` forward in one line; unused `Player` imports gone.

## 7. Eligibility restated in EyeItemService
None separately; covered by Correctness 1.

## 8. Anger split
`OngoingBehavior.ANGER.serverAngry(LivingEntity)` added (OngoingBehavior copied); the scheduler's `isAngry` is gone; `BehaviorSchedulerGameTestsLogic`'s anger test calls `OngoingBehavior.ANGER.serverAngry`.

## 9. Platform-bridge classes
None.

## 10. Picker message prefix
`PickerSpawnService.announce` calls `PickerGate.tellSuccess(source, true, key, args)` (PickerSpawnService copied; its diff was only findings 10 and 17).

## 12. Duplicate definition stores
None.

## 14. Loader config adapters
New package-private `ForgeConfig.define` and `NeoForgeConfig.define`; Forge/NeoForge Server/Client configs call them. 26.1.2's `define` bodies were identical to 26.1.1's, so there were no 26.1.2-specific config lines to keep; the files were copied.

## 15. EyeAppearance package
`EyeAppearance` moved to `eye` (26.1.1's file copied, `eye/state/EyeAppearance.java` deleted); imports updated in EyeDraft, GooglyEyeRenderer, AppearanceOverride (added), and the Boat, EyeConfigLimits, PickerExport, Serialization, SkullEye test logic; EyeDefinition drops its import. HeadInfo already had no `EyeAppearance` import in 26.1.2 (though the class was in `eye.state` until now), so it needed no edit and compiles once the type is in `eye`.

## 17. Ender-dragon exclusion
`ServerEyeConfigs.isExcluded(Identifier)` added (hunk only; the `variantIndex(entity, ...)` line is 26.1.2's and stays); used by `EyeConfigReloadListener`, `PickerExportService` ("mirroring" javadoc replaced), and `PickerSpawnService.refusal`.

## 18. Client-only classes outside `client`
- `client.platform.ClientNetworking` and `client.platform.{fabric,forge,neoforge}.ClientNetworkingImpl` (old files deleted; the `@ExpectPlatform` Impl names follow the package); `client.network.fabric.FabricClientNetworkTransport` (imports `FabricNetworkTransport`); imports updated in PickerExporter, PickerState, SomeGooglyFabricClient, and the NetworkProtocol javadoc link.
- 26.1.2 addition, as 1.21.1: the five client Twilight mixins (TwilightKnightTrophyModelMixin, TwilightSkullCandleRendererMixin, TwilightTrophyModelMixin, TwilightTrophyRendererMixin, TwilightTrophyRenderStateMixin) moved to `com.github.crittscott.somegoogly.mixin.neoforge.twilight.client` (neoforge/.../mixin/neoforge/twilight/client/), after finding 3's edits; listed as `client.<Name>` in neoforge/src/main/resources/somegoogly-twilight.mixins.json, which keeps one `package` (`mixin.neoforge.twilight`); TwilightSkullConversionMixin and TwilightTrophyDataMixin stay in `mixins`.

## Docs
- orientation-code.md (by hand, 26.1.2 labels): scheduler sentence gains "held eyes get per-hand hurt/heal reactions"; `HeldEye` sentence gone; `PresentationEyes` caches for "items, hands, placed heads, and boats"; `ClientEyeRuntime` "owns every client simulation, keeps mob and hand reports apart from them"; "keeps one nested draw context for item and head submission"; hand draw shared by both views; triggers carry target. To fit, took 26.1.1's trims ("Successful ... successful harvests", "idle ones expire"). Body 11,995 characters (was 11,998). 26.1.2's Heads, Twilight, and Forge GeckoLib wording kept.
- orientation-player.md: held-eye sentence updated to 26.1.1's wording; body 14,813 characters.
- orientation-build.md: no Organization change beyond GameTest counts, left for the parent. build-env/ untouched (no covered file changed).
- Left stale as 26.1.1 did: docs/framed-eyes-testing.md and docs/inventory-eyes-testing.md items describing the old first-person held eye. CHANGELOG unchanged (26.1.1's changelog has no held-eye line either).

## Left for other topics / the parent
- Test-completeness: `TestPlayers` permission overload; real-trigger rewrites in BehaviorSchedulerGameTestsLogic (the forced `onHurt(cow, playerAttack(player))` call will become `cow.hurtServer(...)`); GUI/hand/boat/held test javadoc rewrites.
- Javadocs: Container/GUI-item/hotbar Mixin class javadocs; `BehaviorSettings.write` javadoc; scheduler "hurt/milked/sheared" and `{@link ServerConfig#GLOBAL_PERCENT}` wording; ClientNetworkHandler "the server-to-client payloads" wording; PickerExporter cooldown link; EyeBehavior javadocs; a `renderTrophy` javadoc if wanted.
- Not mine, still differing from 26.1.1: `ServerConfig.availableBehaviors` in the ambient-pool comment.
- Parent: GameTest counts in orientation-code.md and orientation-build.md. Organization adds one test per loader (`nestedDrawContextsRestoreOuterSelection`) and removes none.

## Recalled API facts and checks only a build or in-game test can settle
- Fabric `FakePlayer.get`, Forge `GameTestHelper.makeMockServerPlayerInLevel`, NeoForge `FakePlayerFactory.getMinecraft` exist on these loader versions (Forge 64.1.5, NeoForge 26.1.2.112) and give usable `ServerPlayer`s; `hurtServer` on a mock player reaches each loader's damage hook.
- Fabric `AFTER_DAMAGE` does not fire for a killing blow; `LivingHealEvent` and Fabric's `heal` HEAD fire on both sides (hence the side check in `onHealed`/`onHurt`).
- Item and head submits run synchronously inside the submitting call, so `currentDraw()` is valid in `SkullEyeRenderer.render`/`renderTrophy`; `ItemModelResolver.appendItemLayers` receives the holder for first-person hands.
- Twilight Forest's `TrophyRenderer.submit` and `SkullCandleRenderer.submit` descriptors (unchanged) and that the trophy models' `renderTrophy` runs inside them (placed) or inside `ItemStackRenderState.submit` (items).
- Mixin accepts a `client.<Name>` subpackage entry under one config `package` (1.21.1 already does this).
- First build settles: the `ClientEyeRuntime.@Nullable DrawContext` type-use annotation in SkullEyeRenderer; HeadInfo compiling with `EyeAppearance` now in its package.

## In-game checks
- A second client sees a held eye bulge on hurt and swirl on heal once per cooldown; first and third person show the same pupil; both hands react independently; server switches stop held reactions.
- GUI, hotbar, and cursor eyes animate independently and keep going while paused.
- Eyed vanilla heads, Heads-mod heads, Twilight trophies, and Twilight skull candles show eyes placed, in GUI, in hands, and in frames; a head item's eyes draw once (not on its second model).
- `/sg spawn` feedback is prefixed and broadcast; the dragon stays excluded; Forge/NeoForge configs load and reload.
- Production jars start on client and dedicated server with the moved `ClientNetworkingImpl`s and, on NeoForge with Twilight Forest, the moved client Twilight mixins (and without Twilight Forest).

## Fix lines
1: `client/HeldEye` deleted. `EyeBehaviorTriggerPacket` copied: `Target` (ENTITY 0, MAIN_HAND 1, OFF_HAND 2); decode rejects an unknown target and any held behavior but grow/swirl; protocol stays "19". `ServerBehaviorScheduler` copied with "hurt/milked" and "5%" javadoc wording kept for Javadocs: `HandState`, `handBehavior`/`handPacket`, `reactHands`, `tickHands`, hand catch-up, all on `clock(holder)`; `onHurt(LivingEntity, DamageSource)` replaces `onPlayerHurt`; `onHealed` checks the side and uses `canSwirlOnHeal`/`nextHealReadyAt`. `BehaviorSettings` copied without the `write` javadoc. `ServerConfig.roll` added and used by the scheduler, `EyeItemService`'s kill harvest, and `ServerServices`. Client: `ClientEyeRuntime`, `GooglyEyeItemRenderer` as 26.1.1; `PresentationEyes` by hunk, keeping 26.1.2's `headsMod` and `headParts(entity, helper)`; `ClientNetworkHandler` dispatches by target; `ItemModelResolverMixin` calls `handContext`. Tests: `HeldEyeGameTestsLogic` and `SerializationGameTestsLogic` copied; test-source `TestPlayers.create(template, creative)`; template-player wrappers on all three loaders; ninth test_environment `held_eye_swirls_when_healed`, Fabric `maxTicks = 400`, Forge/NeoForge test_instance environment and `max_ticks` 400; `BehaviorSchedulerGameTestsLogic` forced edits only. Twilight and Heads tests needed no change. Unbuilt.
2: `ClientEyeRuntime` copied from 26.1.1 and re-patched to keep 26.1.2's `skullEyes(BlockEntity, Identifier)` with `headParts(entity, layout)` and the `isHeadsMod` placed-head flag (1.21.11's Heads-aware form); `HandItemEyes`, `GuiItemEyes`, `BoatSimulations` gone; `ClientLifecycle` makes one call. Container, GUI-item, and hotbar Mixins call `beginGuiPresentation`/`beginGuiDraw`/`endItemDraw` (code hunks only). Test-source `TestEyeRuntime`; GUI, hand, and boat test logic use its caches, `GuiKey`/`GuiKind`/`HUD`, `onBoatState`, `tick(level, false, null)`. Unbuilt.
3: One `ClientEyeRuntime.DrawContext`; `ItemStackRenderStateMixin` and `SkullBlockRendererMixin` as 26.1.1. `SkullEyeRenderer` edited, not copied: its `DRAWS`, `DrawContext`, `begin`, `beginItem`, `end` removed, Heads fit code, `clearCaches`, and `renderTrophy` kept, `render`/`renderHead` read `currentDraw()`; the first-model-only item flag became a static identity-compared `itemEyesSubmitted` as in 1.21.11. NeoForge `TwilightTrophyRendererMixin` and `TwilightSkullCandleRendererMixin` bracket `submit` with `beginHead(placed.somegoogly$eyes(), placed.somegoogly$partialTick())`/`endItemDraw()`, as 1.21.1; the trophy-model mixins' `renderTrophy` runs inside those brackets or the item-state bracket. `nestedDrawContextsRestoreOuterSelection` with three wrappers, two registrations, two CRLF JSONs. Unbuilt.
6: Forge/NeoForge `ServerEvents` and Fabric `FabricServerEvents`/`HealReactionMixin` as 26.1.1 (NeoForge keeps its Heads-mod drop comment): `onHurt(entity, source)` and `onHealed(entity)` in one line each; unused `Player` imports gone. Unbuilt.
8: `OngoingBehavior.ANGER.serverAngry(LivingEntity)` added; the scheduler's `isAngry` is gone; the shared-anger test in `BehaviorSchedulerGameTestsLogic` calls the new method. Unbuilt.
10: `PickerSpawnService.announce` calls `PickerGate.tellSuccess(source, true, key, args)`. Unbuilt.
14: New package-private `ForgeConfig.define` and `NeoForgeConfig.define`; `ForgeServerConfig`, `ForgeClientConfig`, `NeoForgeServerConfig`, `NeoForgeClientConfig` call them (26.1.2 had no config lines of its own here, so the files were copied). Unbuilt.
15: `EyeAppearance` moved to `eye` (old file deleted); imports updated in EyeDraft, GooglyEyeRenderer, AppearanceOverride, and the Boat, EyeConfigLimits, PickerExport, Serialization, SkullEye test logic; EyeDefinition drops its import; HeadInfo already had none. Unbuilt.
17: `ServerEyeConfigs.isExcluded(Identifier)` used by `EyeConfigReloadListener`, `PickerExportService` ("mirroring" javadoc replaced), and `PickerSpawnService.refusal`. Unbuilt.
18: `client.platform.ClientNetworking` and `client.platform.{fabric,forge,neoforge}.ClientNetworkingImpl` (old files deleted); `client.network.fabric.FabricClientNetworkTransport` (imports `FabricNetworkTransport`); imports updated in PickerExporter, PickerState, SomeGooglyFabricClient, and the NetworkProtocol javadoc link. As 1.21.1, the five client Twilight mixins moved to `mixin.neoforge.twilight.client` and listed as `client.<Name>` in somegoogly-twilight.mixins.json (one config, one package). Unbuilt.
