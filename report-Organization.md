# Report: Organization (26.3)

Applied 2026-11-12 from 26.2's post-fix Organization hunks (26.2/report-Organization.md exists), diffing each 26.2 file against 26.3's. Where a file differed only by this topic plus 26.3's API, 26.2's file was copied and 26.3's API lines restored; elsewhere hunks were applied by hand. Nothing was compiled, built, or tested. Fix lines in review-Organization.md filled in.

## 1. Held eye restates the server's rules
- Deleted `client/HeldEye.java`.
- EyeBehaviorTriggerPacket (from 26.2): `Target` enum (ENTITY 0, MAIN_HAND 1, OFF_HAND 2) between `entityId` and `behavior`; decode rejects an unknown target and any held behavior other than grow or swirl. `NETWORK_VERSION` stays 19.
- ServerBehaviorScheduler (from 26.2): `HANDS`, `HandState`, `handBehavior`/`handPacket`, `reactHands`, `tickHands`; hand catch-up on start tracking and resume; `Target.ENTITY` in `catchUpPacket` and `start` (Networking leftovers); `onHurt(LivingEntity, DamageSource)` replaces `onPlayerHurt`; `onHealed` checks the side and uses `canSwirlOnHeal`/`nextHealReadyAt`, replacing the 26.1.1 cooldown form Reinvention kept. Class javadoc keeps 26.3's "hurt/milked → grow" and "5%" wording (Javadocs 5/7).
- BehaviorSettings (from 26.2): `AmbientSlot`, `ambientPool`, `ambientSlot`, `canSwirlOnHeal`, `handAmbientBusy`, `handIdentity`, `nextHealReadyAt`, `presentationSeed`, `rollGrowOnHit`, `slotTicks`; class and `drawAmbientTicks` javadoc updated; `write` javadoc left to Javadocs 8.
- `ServerConfig.roll(RandomSource, int)` used by the scheduler, EyeItemService's kill harvest (inverted `>=` gone), and ServerServices' natural spawn (float formula gone). Settles Correctness's leftover `roll`.
- Client: ClientNetworkHandler `handleBehavior` dispatches by target ("five" left to Javadocs); ItemModelResolverMixin calls `handContext`; PresentationEyes adds `react` and the reaction overlay and uses the BehaviorSettings slot helpers; GooglyEyeItemRenderer uses `currentDraw().draw()` and `EYE_LOCAL_POSE` in first person, keeping 26.3's `pose.rotateDegrees(Axis.XP, 180)` at both sites.
- Tests: HeldEyeGameTestsLogic copied from 26.2 (calls `onPlayerJoined`, already in 26.3). New test-source `TestPlayers` with only `create(template, creative)` (26.2's permission overload and `GAMEMASTER`/`NO_PERMISSIONS` belong to Test-completeness 6). Wrappers pass a template player: Fabric `FakePlayer.get`, Forge `helper.makeMockServerPlayerInLevel()`, NeoForge `FakePlayerFactory.getMinecraft`. New `test_environment/held_eye_swirls_when_healed.json`; Fabric `heldEyeSwirlsWhenHealed` gets `maxTicks = 400` and the environment, Forge/NeoForge test_instance JSONs get the environment and `max_ticks` 400. SerializationGameTestsLogic copied from 26.2, so `behaviorTriggerPacketRoundTrips` covers the target loop, a held non-reaction, and an unknown target (Networking leftover). BehaviorSchedulerGameTestsLogic forced edits only: `onHurt(cow, helper.getLevel().damageSources().playerAttack(player))` and four `tick(level, b, null)` calls.

## 2. Client caches over five classes
- `ClientEyeRuntime` copied from 26.2 (already reads `minecraft.gui.screen()`); `HandItemEyes` and `GuiItemEyes` deleted.
- `ClientLifecycle.tick` makes one call, passing `minecraft.gui.screen()`.
- Container, GuiItem, and Hotbar Mixins call `beginGuiPresentation`/`beginGuiDraw`/`endItemDraw`; Hotbar keeps `Hud`. Their class javadocs stay at 26.3's text (Javadocs 4).
- New test-source `TestEyeRuntime`. BoatEyeGameTestsLogic uses `TestEyeRuntime.BoatCache`, `ClientEyeRuntime.onBoatState`, 3-argument `tick`; HandItemEyeGameTestsLogic `TestEyeRuntime.HandCache`; GuiItemEyeGameTestsLogic `TestEyeRuntime.GuiCache` and `ClientEyeRuntime.GuiKey`/`GuiKind`/`HUD`. Test javadocs keep 26.3's text.

## 3. Three "what is being drawn" stacks
- One `ClientEyeRuntime.DrawContext`. `ItemStackRenderStateMixin` uses `beginItemState`/`endItemDraw`; `SkullBlockRendererMixin` opens `beginHead` from `PlacedHolder` and closes with `endItemDraw` (class javadoc updated); `SkullEyeRenderer` reads `currentDraw()` (`current`/`currentItem`/`partialTick`/`begin`/`beginItem`/`end` gone).
- New test `nestedDrawContextsRestoreOuterSelection`: logic method, wrappers on all three loaders, Forge/NeoForge registrations, two `test_instance/guiitemeyegametests/nested_draw_contexts_restore_outer_selection.json`.
- None of these hunks touch a pose call or a renderpearl import.

## 6. Loader adapters
Forge `ForgeServerEvents` and NeoForge `NeoForgeServerEvents` (`onLivingDamage`/`onLivingHeal`), Fabric's `AFTER_DAMAGE` listener, and Fabric's `HealReactionMixin` each forward in one line to `onHurt(entity, source)` / `onHealed(entity)`; the mixin's `isClientSide` check now lives in `onHealed`. Unused `Player` imports removed. NeoForge still routes through `EntityInteract`.

## 8. Anger split
`OngoingBehavior.ANGER.serverAngry(LivingEntity)` added (imports `NeutralMob`, `MemoryModuleType`, `AbstractPiglin`), keeping 26.3's `Enderman`. The scheduler's `isAngry` and imports are gone; the shared-anger test calls `OngoingBehavior.ANGER.serverAngry`. 26.3's `Enderman` locals and `assertTrue(sleeper.startSleeping(head), …)` kept.

## 10, 14, 15, 17, 18
- 10: `PickerSpawnService.announce` calls `PickerGate.tellSuccess(source, true, key, args)`.
- 14: new package-private `ForgeConfig` and `NeoForgeConfig` with `define`; server and client configs call them. NeoForgeServerConfig keeps 26.3's `ModConfig.Type.SYNCED` and its javadoc.
- 15: `eye/EyeAppearance.java` created, `eye/state/EyeAppearance.java` deleted; imports updated in EyeDraft, GooglyEyeRenderer, AppearanceOverride, and the BoatEye, EyeConfigLimits, PickerExport, Serialization, and SkullEye test logic; EyeDefinition and HeadInfo dropped their import.
- 17: `ServerEyeConfigs.isExcluded(Identifier)` used by EyeConfigReloadListener, PickerExportService ("mirroring" javadoc reworded), and `PickerSpawnService.refusal`.
- 18: `ClientNetworking` and its three `ClientNetworkingImpl`s moved to `client.platform[.loader]`; `FabricClientNetworkTransport` moved to `client.network.fabric`. References updated in PickerExporter, PickerState, SomeGooglyFabricClient, and the NetworkProtocol javadoc link. PickerExporter's cooldown-link javadoc left to Javadocs.

## Docs
- orientation-code.md: held eye, hands, `ClientEyeRuntime`, draw context, hand draw, and trigger-target lines taken from 26.2 with 26.3's labels; 11,976 of 12,000 characters.
- orientation-player.md: held-eye sentence updated; 14,940 of 15,000.
- Left stale as 26.2 and 26.1.1 did: old first-person held-eye items in docs/*-testing.md and generated `docs/javadoc`. No build-env/ change.

## Deviations
Files copied from 26.2 carry 26.2's line endings and sed-edited files became LF (`.gitattributes` `* text=auto` normalizes them). Javadocs and Test-completeness hunks in the same files left as 26.3 had them.

## Recalled API facts (unverified)
Fabric `FakePlayer.get`, Forge `GameTestHelper.makeMockServerPlayerInLevel` (assumed in Forge 66), and NeoForge `FakePlayerFactory.getMinecraft` give usable `ServerPlayer`s; `hurtServer` on a mock player reaches each loader's damage hook; Fabric `AFTER_DAMAGE` does not fire for a killing blow; `LivingHealEvent` and Fabric's `heal` HEAD fire on both sides; item and head submits run synchronously inside the submitting call; `ItemModelResolver.appendItemLayers` receives the holder for first-person hands; `Minecraft.gui.screen()` exists in 26.3.

## First build settles
The new ClientEyeRuntime, PresentationEyes, GooglyEyeItemRenderer (with `rotateDegrees`), and HeldEyeGameTestsLogic compile against 26.3; Forge 66 `makeMockServerPlayerInLevel` exists; Architectury resolves the moved `client.platform.<loader>.ClientNetworkingImpl`.

## In-game checks
- A second client sees a held eye bulge on hurt and swirl on heal, once per cooldown.
- First and third person show the same pupil; both hands react independently; config gates stop held reactions.
- GUI, hotbar, and cursor eyes animate independently and keep going while paused.
- Eyed heads show eyes in GUI, hands, frames, and placed.
- `/sg spawn` feedback is prefixed and broadcast.
- Forge/NeoForge configs load and reload, and NeoForge's SYNCED config still syncs.
- The ender dragon stays excluded.
- Production jars start with the moved `ClientNetworkingImpl` on a client and on a dedicated server.

## GameTests
+1 per loader (`nestedDrawContextsRestoreOuterSelection`) on Fabric, Forge, and NeoForge. None removed. The six held-eye tests now take a template `ServerPlayer`.
