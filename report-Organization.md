# Report: Organization (26.2)

Applied 2026-11-12 by taking 26.1.1's Organization hunks into 26.2 (diff, then hunk-wise patch or Edit), while other topics edited the same files. Nothing was compiled, built, or tested. Fix lines in review-Organization.md filled in.

## 1. Held eye restates the server's rules
- Deleted `client/HeldEye.java`.
- EyeBehaviorTriggerPacket: `Target` enum (ENTITY 0, MAIN_HAND 1, OFF_HAND 2) between `entityId` and `behavior`; decode rejects an unknown target and any held behavior other than grow or swirl; class javadoc is 26.1.1's. `NETWORK_VERSION` untouched (Networking owns it).
- ServerBehaviorScheduler: `HANDS`, `HandState`, `handBehavior`/`handPacket`, `reactHands`, `tickHands`; hand catch-up on start tracking and resume; `onHurt(LivingEntity, DamageSource)` replaces `onPlayerHurt`; `onHealed` checks the side and uses `canSwirlOnHeal`/`nextHealReadyAt`; `Target.ENTITY` in `catchUpPacket` and `start`; class-javadoc held-eye sentences.
- BehaviorSettings: `AmbientSlot`, `ambientPool`, `ambientSlot`, `canSwirlOnHeal`, `handAmbientBusy`, `handIdentity`, `nextHealReadyAt`, `presentationSeed`, `rollGrowOnHit`, `slotTicks`, plus the class and `drawAmbientTicks` javadoc.
- `ServerConfig.roll(RandomSource, int)` used by the scheduler, EyeItemService's kill harvest (the inverted `>=` is gone), and ServerServices' natural spawn (the float formula is gone).
- Client: `ClientNetworkHandler.handleBehavior` dispatches by target; `ItemModelResolverMixin` calls `handContext`; PresentationEyes has `react`, the reaction overlay, and `BehaviorSettings` slot helpers; GooglyEyeItemRenderer uses `currentDraw().draw()` and `EYE_LOCAL_POSE` in first person.
- Tests: HeldEyeGameTestsLogic identical to 26.1.1's; `TestPlayers.create(template, creative)` already existed (Test-completeness); wrappers pass a template player (Fabric `FakePlayer.get`, Forge `makeMockServerPlayerInLevel`, NeoForge `FakePlayerFactory.getMinecraft`); new `test_environment/held_eye_swirls_when_healed.json`; Fabric `heldEyeSwirlsWhenHealed` gets `maxTicks = 400` and the environment; Forge/NeoForge test_instance JSONs get the environment and `max_ticks` 400; SerializationGameTestsLogic's trigger test is 26.1.1's post-fix version (`Target.ENTITY`, target loop, held non-reaction, unknown target); BehaviorSchedulerGameTestsLogic gets the forced `tick(level, b, null)` edit.
- Deviation: the held_eye test_instance JSONs are LF in 26.2, so LF was kept (26.1.1's are CRLF).

## 2. Client caches over five classes
`ClientEyeRuntime` equals 26.1.1's except `minecraft.gui.screen()` in place of `minecraft.screen`. `HandItemEyes` and `GuiItemEyes` deleted. `ClientLifecycle.tick` makes one call, passing `minecraft.gui.screen()`. Container, GuiItem, and Hotbar Mixins call `beginGuiPresentation`/`beginGuiDraw`/`endItemDraw`; the Hotbar Mixin keeps `Hud`. New test-source `TestEyeRuntime`. GUI, hand, and boat test logic switched to `TestEyeRuntime` caches, `GuiKey`/`GuiKind`/`HUD`, `onBoatState`, and the 3-argument `tick`, keeping 26.2's `EntityTypes`.

## 3. Three "what is being drawn" stacks
One `DrawContext`. `ItemStackRenderStateMixin` uses `beginItemState`/`endItemDraw`. `SkullBlockRendererMixin` opens `beginHead` from `PlacedHolder` and closes with `endItemDraw`. `SkullEyeRenderer` reads `currentDraw()`. New test `nestedDrawContextsRestoreOuterSelection`: logic method, wrappers on all three loaders, Forge/NeoForge `SomeGooglyGameTestMod` registrations, and two CRLF `test_instance/guiitemeyegametests/nested_draw_contexts_restore_outer_selection.json`.

## 6. Loader adapters
Forge/NeoForge `onLivingDamage`/`onLivingHeal`, Fabric's `AFTER_DAMAGE` listener, and Fabric's `HealReactionMixin` each forward in one line; unused `Player` imports removed. NeoForge: only those hunks; no `EntityInteractSpecific` handler.

## 8. Anger split
`OngoingBehavior.ANGER.serverAngry` added; the scheduler's `isAngry` and its imports removed; the shared-anger test calls `serverAngry`.

## 10, 14, 15, 17, 18
- 10: `announce` calls `PickerGate.tellSuccess(source, true, key, args)`.
- 14: new package-private `ForgeConfig.java` and `NeoForgeConfig.java` with `define`; server and client configs call them.
- 15: `eye/EyeAppearance.java` created, `eye/state/EyeAppearance.java` deleted; nine source files' imports updated. Generated `docs/javadoc` still points to the old path.
- 17: `ServerEyeConfigs.isExcluded` added and used by the reload listener, export service (javadoc reworded), and spawn refusal; 26.2's `EntityTypes.ENDER_DRAGON` kept.
- 18: `ClientNetworking` and its three `ClientNetworkingImpl`s moved to `client.platform`, `FabricClientNetworkTransport` to `client.network.fabric`; package lines and references updated in PickerExporter, PickerState, SomeGooglyFabricClient, and the NetworkProtocol javadoc link.

## Docs
- orientation-code.md: held eye, hands, `ClientEyeRuntime`, draw context, hand draw, and trigger-target wording updated, keeping 26.2's labels; 26.1.1's trims ("Successful…/successful harvests", "idle trackers expire") taken to stay under the limit.
- orientation-player.md: held-eye sentence updated.
- Left stale, as 26.1.1 did: docs/framed-eyes-testing.md item 6, inventory-eyes-testing.md item 8, third-person-eyes-testing.md item 6, boat-eyes-testing.md line 38 (old first-person held eye).

## Deviations
Hunks were applied with `diff -u … | patch` or Edit rather than Edit alone; each run touched only the target file's hunks.

## Recalled API facts (unverified)
Fabric `FakePlayer.get`, Forge `GameTestHelper.makeMockServerPlayerInLevel` (assumed present in Forge 65), and NeoForge `FakePlayerFactory.getMinecraft` give usable `ServerPlayer`s; `hurtServer` on a mock player reaches each loader's damage hook; Fabric `AFTER_DAMAGE` does not fire for a killing blow; `LivingHealEvent` and Fabric's `heal` HEAD fire on both sides; item and head submits run synchronously inside the submitting call; `ItemModelResolver.appendItemLayers` receives the holder for first-person hands; `Minecraft.gui.screen()` is 26.2's equivalent of `minecraft.screen`.

## First build settles
Compile of the new ClientEyeRuntime, PresentationEyes, and HeldEyeGameTestsLogic against 26.2; Forge 65 `makeMockServerPlayerInLevel`.

## In-game checks
A second client sees a held eye bulge on hurt and swirl on heal, once per cooldown. First and third person show the same pupil; both hands react independently; gates stop held reactions. GUI, hotbar, and cursor eyes animate independently and keep going while paused. Eyed heads show eyes in GUI, hands, frames, and placed. `/sg spawn` feedback is prefixed and broadcast. Forge/NeoForge configs load and reload; the dragon stays excluded. Production jars start with the moved `ClientNetworkingImpl` on client and dedicated server.

## GameTests
+1 per loader (`nestedDrawContextsRestoreOuterSelection`) on Fabric, Forge, and NeoForge. None removed. The six held-eye tests take a template `ServerPlayer`.
