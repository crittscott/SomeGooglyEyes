# Report: Organization (1.21.11)

Applied 2026-11-11 by hand-porting 1.21.8's changes. Nothing was compiled, built, or tested; everything here comes from reading the source. Each file was diffed against 1.21.8 and only this topic's hunks taken. Taken whole (diff showed only this topic or `ResourceLocation`→`Identifier`/jspecify renames): ServerBehaviorScheduler (keeps "5%" instead of 1.21.8's `{@link ServerConfig#GLOBAL_PERCENT}`), EyeBehaviorTriggerPacket (keeps 1.21.11's "Purely transient" sentence), BehaviorSettings (without 1.21.8's extra `write` javadoc line), eye/EyeAppearance, client.platform.ClientNetworking and its three impls, client.network.fabric.FabricClientNetworkTransport, ForgeConfig/NeoForgeConfig and the four Forge/NeoForge config classes, test-source TestEyeRuntime and HeldEyeGameTestsLogic, the three GuiItemEyeGameTests and Fabric/NeoForge HeldEyeGameTests (Forge's with `makeMockServerPlayerInLevel`).

## 1. Held eye restates the server's rules
- Deleted `HeldEye`.
- `EyeBehaviorTriggerPacket.Target` (ENTITY 0, MAIN_HAND 1, OFF_HAND 2); wire `(entityId, target, behavior, seed, elapsed)`; decode rejects an unknown target and any held behavior but grow/swirl. `NETWORK_VERSION` stays "19".
- Scheduler: `HandState`, `handBehavior`/`handPacket`, `reactHands`, `tickHands`, hand catch-up on start tracking and resume; `onHurt(LivingEntity, DamageSource)` replaces `onPlayerHurt`; `onHealed` checks the side and calls `reactHands`; hand timing uses `clock(holder)`.
- BehaviorSettings: `AmbientSlot`, `ambientPool`, `ambientSlot`, `canSwirlOnHeal`, `handAmbientBusy`, `handIdentity`, `nextHealReadyAt`, `presentationSeed`, `rollGrowOnHit`, `slotTicks`; corrected `drawAmbientTicks` javadoc.
- `ServerConfig.roll` used by the scheduler, the kill harvest (replacing the inverted `>=`), and natural spawning.
- Client: `ClientEyeRuntime.handContext`/`handEyes`/`setHandBehavior`/`removeHands`, `HAND_BEHAVIORS`, `HANDS`; `PresentationEyes.react` and `reaction`/`ambientExpression` overlay (`uuidSeed`, `SLOT_SEED_STEP` removed); `ClientNetworkHandler` dispatches by target; `ItemModelResolverMixin` calls `handContext` (first person included).
- GooglyEyeItemRenderer keeps its submit path; only eye selection (`currentDraw().draw()`) and first-person `EYE_LOCAL_POSE`/`itemGravity` capture ported; `heldEye()` fallback gone.
- Correctness 11: `handContext` and `tick` honor `eyesVisible`; `reactHands` honors `googlyEyesEnabled`.
- Tests: 1.21.8's HeldEyeGameTestsLogic and test-source `TestPlayers` (`template.level()`); wrappers pass a template player (Fabric `FakePlayer.get`, Forge `helper.makeMockServerPlayerInLevel()`, NeoForge `FakePlayerFactory.getMinecraft`); `heldEyeSwirlsWhenHealed` waits real ticks (new `test_environment/held_eye_swirls_when_healed.json`, Fabric `maxTicks = 400` + environment, Forge/NeoForge JSON environment + `max_ticks` 400); `behaviorTriggerPacketRoundTrips` is 1.21.8's (targets, non-reaction on a held eye, unknown target); BehaviorSchedulerGameTestsLogic calls `onHurt(cow, damageSources().playerAttack(player))`.
- Deviations: `TestPlayers.create(template, creative)` drops 1.21.8's permission parameter and `hasPermissions(int)` override (1.21.11 uses `Permissions`/`PermissionSet`, see PickerGate); Forge template is `makeMockServerPlayerInLevel()` (no `survivalPlayer` in 1.21.11's Forge tests).

## 2. Client caches spread over five classes
`ClientEyeRuntime` owns boat, frame, head, hand, GUI, and mob caches with one `tick(level, simulate, activeView)` (`tickGui`, `tickWorld`), one `clear`, one `onEntityRemoved`. Deleted `HandItemEyes`, `GuiItemEyes`, `BoatSimulations`. `ClientLifecycle.tick` makes one call. Test-source `TestEyeRuntime` used by GUI, hand, boat gametests. Kept 1.21.11's Heads-aware `skullEyes` and `vehicle.boat.AbstractBoat`. Behavior change: `clientTicks()` advances only on unpaused world ticks, so PickerHud's tick animation freezes on pause.

## 3. Three "what is being drawn" stacks
- One `ClientEyeRuntime.DrawContext(head, item, partialTick, draw, guiKey)` replaces `DRAWS`, `GuiItemEyes.PRESENTATIONS`, and SkullEyeRenderer's `current`/`currentItem`/`partialTick`/`begin`/`beginItem`/`end`.
- Hooks: ItemStackRenderStateMixin brackets `submit` with `beginItemState`/`endItemDraw`; SkullBlockRendererMixin keeps its `submit`/`submitSkull` targets and opens `beginHead(eyes, partialTick)` from the `PlacedHolder` SkullBlockRenderStateMixin fills at extraction; GUI mixins call `beginGuiDraw`/`beginGuiPresentation`/`endItemDraw`.
- Deviation: a placed head's context carries the eyes chosen at extraction rather than 1.21.8's `SkullBlockEntity`.
- Deferred-pass check: every read of the current draw is inside the scope (`SkullEyeRenderer.render` from `submitSkull` inside `submit`; the special renderer's `submit` inside `ItemStackRenderState.submit`; GooglyEyeRenderer's custom-geometry callbacks capture their values at submit time). No selection had to be stored on a submitted node.
- Deviation: the head item's "eyes on the first model only" guard is `itemEyesSubmitted == draw` (context identity), since `beginItem` no longer resets a boolean.
- `SkullEyeRenderer` reads `draw.draw()` directly. Javadocs of SkullEyeRenderer and SkullBlockRendererMixin updated.
- `nestedDrawContextsRestoreOuterSelection`: logic, three wrappers, Forge/NeoForge registrations, two `test_instance/guiitemeyegametests/` JSONs (max_ticks 40).

## 4, 5, 9, 12
None.

## 6. Loader adapters forward in one line
Forge/NeoForge `onLivingDamage` → `onHurt(event.getEntity(), event.getSource())`, `onLivingHeal` → `onHealed(event.getEntity())`; Fabric `AFTER_DAMAGE` → `onHurt(entity, source)`; HealReactionMixin → `onHealed((LivingEntity) (Object) this)`. Forge/NeoForge ServerEvents now equal 1.21.8's; Fabric's differs only by port renames.

## 7
Done by Correctness 1.

## 8. Anger split
`OngoingBehavior.ANGER.serverAngry(LivingEntity)`; the scheduler's `isAngry` is gone; the shared-anger test calls it.

## 10. Picker message prefix written twice
`PickerSpawnService.announce` calls `PickerGate.tellSuccess(source, true, key, args)`.

## 14. Loader config adapters
Package-private `ForgeConfig.define`/`NeoForgeConfig.define` (NeoForge keeps its `defineList` overload); both config classes per loader call them; Correctness 18's `applyValues` kept.

## 15. EyeAppearance package
Moved to `eye`; imports updated in EyeDraft, GooglyEyeRenderer, and Boat/EyeConfigLimits/PickerExport/Serialization/SkullEye test logic; AppearanceOverride gains the import its javadoc links need; EyeDefinition and HeadInfo drop theirs.

## 17. Ender-dragon exclusion
`ServerEyeConfigs.isExcluded(Identifier)` at reload, export (javadoc "mirroring" replaced), and spawn refusal.

## 18. Client-only classes outside client packages
`client.platform.ClientNetworking` with `client.platform.{fabric,forge,neoforge}.ClientNetworkingImpl`; `client.network.fabric.FabricClientNetworkTransport`; references updated in PickerExporter, PickerState, the NetworkProtocol javadoc, SomeGooglyFabricClient. No entrypoint, Mixin JSON, or doc names them.

## Docs
orientation-code.md (held-eye reactions per hand; ClientEyeRuntime owns all simulations and one nested draw context; triggers carry a target; Heads sentence trimmed; GameTest counts Fabric 191, Forge 195, NeoForge 197; 11,987 characters); orientation-build.md (Forge 195, NeoForge 197); orientation-player.md (held eyes react to hurt/heal for every viewer, first and third person share each hand's eye; 14,945 characters); review-Organization.md.

## Recalled API facts (unverified)
The special model renderer's `submit` and `SkullBlockRenderer.submitSkull` run synchronously inside the item's/head's `submit` and only custom-geometry callbacks are deferred; `ItemModelResolver.appendItemLayers` receives the holder as `ItemOwner` for first-person hands; Forge `makeMockServerPlayerInLevel()` returns a usable `ServerPlayer`; 1.21.11 `ServerPlayer.level()` is `ServerLevel` and `hasPermissions(int)` is replaced by `permissions()`; Fabric `AFTER_DAMAGE` does not fire on a killing blow; Forge `LivingHealEvent`/Fabric `heal` HEAD fire on both sides; `ItemStack.isSameItemSameComponents` and `Minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false)` exist.

## Left to other topics
TestPlayers has no permission override (Test-completeness 6 must add one with 1.21.11's `PermissionSet` API if needed); 1.21.8's mixin javadoc rewrites (Javadocs 4) and gametest javadoc rewrites (Javadocs 2/Test-completeness) not taken.

## Clash points
ServerBehaviorScheduler, BehaviorSchedulerGameTestsLogic, SerializationGameTestsLogic, GUI/Hand/Boat/HeldEye test logic, SomeGooglyGameTestMod files, PresentationEyes, GooglyEyeItemRenderer, SkullEyeRenderer, ClientEyeRuntime, client mixins, ServerConfig, ServerEyeConfigs, EyeItemService, ServerServices, PickerSpawnService, PickerExportService, EyeConfigReloadListener, loader ServerEvents and config classes, orientation-code.md (~13 characters of headroom).

## In-game checks
A second client sees a holder's held eye bulge on any damage and swirl on heal (once per cooldown); first and third person show the same pupil; both hands react independently; `googlyEyesEnabled = false` and `disableGooglyEyes` stop held reactions; GUI slot, hotbar, and cursor eyes animate independently and keep running while paused; eyed head items in GUI, hands, frames and placed vanilla and Heads-mod heads show eyes; a wool-carrying head item gets eyes once; picker HUD animation pauses with the game; `/sg spawn` and `spawnall` feedback is prefixed and broadcast; Forge/NeoForge configs load and reload; production jars start with the moved ClientNetworkingImpl; the dragon stays excluded on reload, export, spawn.

## Files modified
See review-Organization.md Fix lines; summary: common client (ClientEyeRuntime, ClientLifecycle, ClientNetworkHandler, PresentationEyes, GooglyEyeItemRenderer, SkullEyeRenderer, PickerExporter, PickerState, EyeDraft, GooglyEyeRenderer; deleted HeldEye, HandItemEyes, GuiItemEyes, platform/ClientNetworking; new client/platform/ClientNetworking), mixins (Container, GuiItem, Hotbar, ItemModelResolver, ItemStackRenderState, SkullBlockRenderer), eye (BehaviorSettings, OngoingBehavior, ServerBehaviorScheduler, EyeDefinition, HeadInfo, AppearanceOverride; EyeAppearance moved), network (EyeBehaviorTriggerPacket, NetworkProtocol), config (ServerConfig, ServerEyeConfigs, EyeConfigReloadListener), EyeItemService, ServerServices, PickerSpawnService, PickerExportService; gametest logic (BehaviorScheduler, BoatEye, GuiItemEye, HandItemEye, HeldEye, Serialization, EyeConfigLimits, PickerExport, SkullEye), new TestEyeRuntime, TestPlayers, held_eye_swirls_when_healed environment; fabric (FabricServerEvents, HealReactionMixin, SomeGooglyFabricClient, moved ClientNetworkingImpl and FabricClientNetworkTransport, GuiItemEyeGameTests, HeldEyeGameTests); forge and neoforge (ServerEvents, new ForgeConfig/NeoForgeConfig, server and client config classes, moved ClientNetworkingImpl, GuiItemEyeGameTests, HeldEyeGameTests, SomeGooglyGameTestMod, two JSONs each); docs (orientation-code, orientation-build, orientation-player, review-Organization).
