# Report: Organization (26.1.1)

Applied 2026-11-11 by porting 26.1's changes; each file diffed against 26.1, only this topic's hunks taken. Nothing was compiled, built, or tested. Fix lines and intro in review-Organization.md filled in.

Copied whole from 26.1 (remaining diff only this topic, or new): common ClientEyeRuntime, ClientLifecycle, PresentationEyes, render/GooglyEyeItemRenderer, render/SkullEyeRenderer, SkullBlockRendererMixin, EyeBehaviorTriggerPacket, eye/EyeAppearance; ServerBehaviorScheduler (javadoc "hurt/milked/sheared" and `{@link ServerConfig#GLOBAL_PERCENT}` reverted for Javadocs) and BehaviorSettings (`write` javadoc dropped for Javadocs); moved client/platform/ClientNetworking, client/platform/{fabric,forge,neoforge}/ClientNetworkingImpl, client/network/fabric/FabricClientNetworkTransport; ForgeConfig, NeoForgeConfig (new), Forge/NeoForge Server/Client configs, Forge/NeoForge ServerEvents, FabricServerEvents, HealReactionMixin; gametests HeldEyeGameTestsLogic, SerializationGameTestsLogic, EyeConfigLimitsGameTestsLogic, TestEyeRuntime (new), the three loaders' HeldEyeGameTests and GuiItemEyeGameTests, test_environment/held_eye_swirls_when_healed.json (new).

## 1. Held eye restates the server's rules
- HeldEye deleted. `EyeBehaviorTriggerPacket.Target` (ENTITY 0, MAIN_HAND 1, OFF_HAND 2) between `entityId` and `behavior`; decode rejects an unknown target and any held behavior other than grow or swirl. Protocol stays "19".
- Scheduler: `HandState`, `handBehavior`/`handPacket`, `reactHands`, `tickHands`; hand catch-up on start tracking and resume; hand timing uses `clock(holder)`; `onHurt(LivingEntity, DamageSource)` replaces `onPlayerHurt`; `onHealed` checks the side and takes its cooldown from `BehaviorSettings.canSwirlOnHeal`/`nextHealReadyAt`.
- `ServerConfig.roll(RandomSource, int)` used by the scheduler, EyeItemService's kill harvest (replacing the inverted `>=`), and ServerServices' natural spawn.
- Client: `ClientNetworkHandler.handleBehavior` dispatches by target; `ItemModelResolverMixin` calls `ClientEyeRuntime.handContext`; GooglyEyeItemRenderer selects the eye with `currentDraw().draw()` and uses `EYE_LOCAL_POSE` in first person.
- Tests: `TestPlayers.create(template, creative)` (hand-written); template players from Fabric `FakePlayer.get`, Forge `helper.makeMockServerPlayerInLevel()`, NeoForge `FakePlayerFactory.getMinecraft`; `heldEyeSwirlsWhenHealed` gets `maxTicks = 400` and the environment on Fabric and in the Forge/NeoForge test_instance JSONs (CRLF kept).
- Deviations (as 26.1): `TestPlayers` has no `PermissionSet` overload; BehaviorSchedulerGameTestsLogic got only forced edits (`onPlayerHurt(cow)` → `onHurt(cow, ...playerAttack(player))`, `isAngry` → `OngoingBehavior.ANGER.serverAngry`, `tick(level, b)` → `tick(level, b, null)`).

## 2. Client caches spread over five classes
`ClientEyeRuntime` owns all client caches; HandItemEyes and GuiItemEyes deleted; `ClientLifecycle.tick` makes one call. Container, GUI-item, and hotbar Mixins call `beginGuiPresentation`/`beginGuiDraw`/`endItemDraw`. GUI, hand, and boat test logic use `TestEyeRuntime` caches, `ClientEyeRuntime.GuiKey`/`GuiKind`/`HUD`, `onBoatState`, `tick(level, false, null)`. Mixin and test javadoc rewrites left for Javadocs and Test-completeness.

## 3. Three "what is being drawn" stacks
One `DrawContext`; `ItemStackRenderStateMixin` brackets `submit` with `beginItemState`/`endItemDraw`; SkullBlockRendererMixin opens `beginHead` from `PlacedHolder`; SkullEyeRenderer reads `currentDraw()`. New test `nestedDrawContextsRestoreOuterSelection` in GuiItemEyeGameTestsLogic with wrappers on each loader, registered in Forge/NeoForge `SomeGooglyGameTestMod`, with two CRLF test_instance JSONs.

## 4, 5, 9, 12
None. 7 done by Correctness 1.

## 6. Loader adapters forward in one line
Forge/NeoForge/Fabric damage and heal hooks and Fabric's HealReactionMixin forward in one line; unused `Player` imports gone.

## 8. Anger split
`OngoingBehavior.ANGER.serverAngry(LivingEntity)` replaces the scheduler's `isAngry`; the shared-anger test calls it.

## 10, 14, 15, 17, 18
- 10: `PickerSpawnService.announce` calls `PickerGate.tellSuccess(source, true, key, args)`.
- 14: package-private `ForgeConfig.define`/`NeoForgeConfig.define`, used by server and client configs.
- 15: EyeAppearance moved to `eye`; imports updated in EyeDraft, GooglyEyeRenderer, AppearanceOverride, and Boat, EyeConfigLimits, PickerExport, Serialization, SkullEye test logic; EyeDefinition and HeadInfo dropped theirs.
- 17: `ServerEyeConfigs.isExcluded(Identifier)` in EyeConfigReloadListener, PickerExportService ("mirroring" javadoc replaced), and `PickerSpawnService.refusal`.
- 18: package moves done; references updated in PickerExporter, PickerState, SomeGooglyFabricClient, and the NetworkProtocol javadoc link.

## Docs
orientation-code.md edited by hand with 26.1's wording and 26.1.1's labels; body 11,996 characters (took 26.1's trims "Successful … successful harvests" and "idle trackers expire"). orientation-player.md held-eye sentence updated; 14,887 characters. Left stale as in 26.1: docs/framed-eyes-testing.md item 6 and docs/inventory-eyes-testing.md item 8 (old first-person held eye).

## Left to other topics
Test-completeness: `TestPlayers` permission overload, remaining direct `on*` calls in BehaviorSchedulerGameTestsLogic, GUI/hand/boat test javadoc rewrites. Javadocs: Mixin class-javadoc rewrites, `BehaviorSettings.write` javadoc, scheduler "5%" and "hurt/milked/sheared" wording, wording hunks in ClientNetworkHandler, PickerExporter, EyeBehavior.

## GameTests
+1 per loader (`nestedDrawContextsRestoreOuterSelection`); none removed; the six held-eye tests take a template `ServerPlayer`.

## First build settles
26.1-copied ClientEyeRuntime, PresentationEyes, HeldEyeGameTestsLogic compile against 26.1.1; Forge 63 still has `makeMockServerPlayerInLevel`.

## Recalled API facts (unverified)
Fabric `FakePlayer.get`, Forge `makeMockServerPlayerInLevel`, NeoForge `FakePlayerFactory.getMinecraft` give usable `ServerPlayer`s; `hurtServer` on a mock player reaches each loader's damage hook; Fabric `AFTER_DAMAGE` does not fire for a killing blow; `LivingHealEvent` and Fabric's `heal` HEAD fire on both sides; item and head submits run synchronously inside the submitting call; `ItemModelResolver.appendItemLayers` receives the holder for first-person hands.

## In-game checks
A second client sees a held eye bulge on hurt and swirl on heal once per cooldown; first and third person show the same pupil and both hands react independently; gates stop held reactions. GUI, hotbar, and cursor eyes animate independently and keep going while paused; the picker HUD animation pauses with the game. Eyed heads show eyes in GUI, hands, frames, and placed. `/sg spawn` feedback is prefixed and broadcast. Forge/NeoForge configs load and reload; the dragon stays excluded. Production jars start with the moved `ClientNetworkingImpl`.
