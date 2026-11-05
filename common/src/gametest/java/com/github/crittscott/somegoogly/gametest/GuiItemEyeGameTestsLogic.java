package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.client.GuiEyeAnimations;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState.Snapshot;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** GUI ownership and clock tests; actual icon drawing and atlas behavior need the client checklist. */
public final class GuiItemEyeGameTestsLogic {

    private static final int IDLE_TICKS = 12;
    private static final int OBSERVATION_TICKS = 400;

    private GuiItemEyeGameTestsLogic() {
    }

    private static ItemStack eye() {
        return new ItemStack(ModContent.GOOGLY_EYE.get());
    }

    /**
     * The UI clock advances expressions without world ticks. In game: view an animated inventory
     * preview in a pausing screen; its eyes continue expressing while world entities are paused.
     */
    public static void guiEyesAdvanceWithoutWorldTicks(GameTestHelper helper) {
        GuiEyeAnimations cache = new GuiEyeAnimations();
        Object view = new Object();
        var key = new GuiEyeAnimations.Key(view, GuiEyeAnimations.SLOT, 0, 0, 0);
        ItemStack stack = eye();
        var presentation = cache.get(key, stack, true);
        BehaviorSettings defaults = BehaviorSettings.DEFAULTS;
        BehaviorSettings blink = new BehaviorSettings(true, 1, 1, List.of(EyeBehavior.BLINK),
                0, false, defaults.swirlHealCooldownTicks(), defaults.ongoing());
        cache.tick(view, blink);
        helper.assertTrue(presentation.active != null && presentation.active.age == 1,
                "the first UI tick joins its blink without consulting world time");
        cache.get(key, stack, true);
        cache.tick(view, blink);
        helper.assertTrue(presentation.active.age == 2, "UI time advances independently");
        helper.succeed();
    }

    /**
     * Disabled and ordinary items retire their old presentation. In game: disable eye animation, then
     * replace a visible eye with a diamond; restoring the eye produces a fresh live presentation.
     */
    public static void guiEyesHonorEligibilityAndSwitch(GameTestHelper helper) {
        GuiEyeAnimations cache = new GuiEyeAnimations();
        var key = new GuiEyeAnimations.Key(new Object(), GuiEyeAnimations.SLOT, 0, 0, 0);
        var first = cache.get(key, eye(), true);
        helper.assertTrue(cache.get(key, eye(), false) == null, "disabled GUI animation supplies no simulation");
        var restored = cache.get(key, eye(), true);
        helper.assertTrue(restored != first, "reenabling creates fresh state");
        helper.assertTrue(cache.get(key, new ItemStack(Items.DIAMOND), true) == null,
                "ordinary items have no eye simulation");
        helper.assertTrue(cache.get(key, new ItemStack(Items.SKELETON_SKULL), true) == null,
                "plain heads have no eye simulation");
        helper.assertTrue(cache.get(key, ItemStack.EMPTY, true) == null, "empty slots retire their presentation");
        helper.succeed();
    }

    /**
     * Stack copies and count changes preserve a slot's simulation.
     * In game: split and merge a stack in one slot; its pupil does not restart on each rendered frame.
     */
    public static void guiEyesKeepPresentationIdentity(GameTestHelper helper) {
        GuiEyeAnimations cache = new GuiEyeAnimations();
        var key = new GuiEyeAnimations.Key(new Object(), GuiEyeAnimations.SLOT, 3, 0, 0);
        ItemStack stack = eye();
        var first = cache.get(key, stack, true);
        ItemStack copy = stack.copy();
        copy.setCount(2);
        var second = cache.get(key, copy, true);
        helper.assertTrue(first == second,
                "copies and counts do not create per-frame simulations");
        helper.assertTrue(first.eye.guiGravity && !first.eye.itemGravity,
                "GUI pupils use screen orientation rather than mounted world orientation");
        helper.succeed();
    }

    /**
     * A changed appearance replaces the simulation. In game: replace a normal eye in one slot
     * with a glowing or dyed copy; its icon immediately shows the new appearance.
     */
    public static void guiEyesReplaceChangedContents(GameTestHelper helper) {
        GuiEyeAnimations cache = new GuiEyeAnimations();
        var key = new GuiEyeAnimations.Key(new Object(), GuiEyeAnimations.SLOT, 0, 0, 0);
        ItemStack stack = eye();
        var first = cache.get(key, stack, true);
        EyeItemProperties.set(stack, AppearanceOverride.EMPTY.withGlow(true));
        var changed = cache.get(key, stack, true);
        helper.assertTrue(first != changed,
                "appearance changes replace the simulation");
        helper.succeed();
    }

    /**
     * Closing a view drops its trackers while a visible HUD presentation survives. Hidden icons expire.
     * In game: close a chest, change screens, and scroll creative entries out of view; no old icon state leaks.
     */
    public static void guiEyesRetireClosedAndHiddenViews(GameTestHelper helper) {
        GuiEyeAnimations cache = new GuiEyeAnimations();
        Object view = new Object();
        var screenKey = new GuiEyeAnimations.Key(view, GuiEyeAnimations.SLOT, 0, 0, 0);
        var hudKey = new GuiEyeAnimations.Key(GuiEyeAnimations.HUD, GuiEyeAnimations.POSITION, 0, 0, 0);
        var screen = cache.get(screenKey, eye(), true);
        var hud = cache.get(hudKey, eye(), true);
        cache.tick(new Object(), BehaviorSettings.NONE);
        helper.assertTrue(cache.get(screenKey, eye(), true) != screen,
                "departing a screen retires its simulations");
        helper.assertTrue(cache.get(hudKey, eye(), true) == hud,
                "the HUD simulation belongs to its own view");
        for (int tick = 0; tick < IDLE_TICKS; tick++) {
            cache.tick(null, BehaviorSettings.NONE);
        }
        helper.assertTrue(cache.get(hudKey, eye(), true) != hud,
                "unseen presentations are evicted");
        var current = cache.get(hudKey, eye(), true);
        cache.clear();
        helper.assertTrue(cache.get(hudKey, eye(), true) != current,
                "resource/world resets invalidate GUI state");
        helper.succeed();
    }

    /**
     * Neighboring copies and a carried stack have independent animation. In game:
     * drag an eye across matching occupied slots; the cursor eye keeps its own expression while moving.
     */
    public static void guiEyesSeparateSlotsAndCursor(GameTestHelper helper) {
        GuiEyeAnimations cache = new GuiEyeAnimations();
        Object view = new Object();
        var first = cache.get(new GuiEyeAnimations.Key(view, GuiEyeAnimations.SLOT, 0, 0, 0), eye(), true);
        var second = cache.get(new GuiEyeAnimations.Key(view, GuiEyeAnimations.SLOT, 1, 0, 0), eye(), true);
        var cursorKey = new GuiEyeAnimations.Key(view, GuiEyeAnimations.CURSOR, 0, 0, 0);
        var cursor = cache.get(cursorKey, eye(), true);
        helper.assertTrue(first != second && cursor != first, "presentations do not share pupils");
        helper.assertTrue(cache.get(cursorKey, eye().copy(), true) == cursor,
                "the cursor simulation is independent of its moving coordinates");
        var hotbarKey = new GuiEyeAnimations.Key(GuiEyeAnimations.HUD, GuiEyeAnimations.HOTBAR, 1, 0, 0);
        var hotbar = cache.get(hotbarKey, eye(), true);
        cache.tick(view, BehaviorSettings.NONE);
        helper.assertTrue(cache.get(hotbarKey, eye(), true) == hotbar,
                "hotbar slot seeds remain stable while another screen is open");
        helper.succeed();
    }

    /**
     * GUI heads retain their actual layout and paired expressions, with GUI gravity on each pupil.
     * In game: place eyed skulls in inventory and a chest; their configured pupils blink and cross-eye
     * at normal icon size, independently of the same heads in frames.
     */
    public static void guiHeadUsesConfiguredLayout(GameTestHelper helper) {
        var definitions = ClientEyeConfigs.all();
        BehaviorSettings behaviors = ClientEyeConfigs.behaviors();
        boolean enabled = ClientEyeConfigs.googlyEyesEnabled();
        try {
            ClientEyeConfigs.replaceAll(ServerEyeConfigs.all(), true, BehaviorSettings.DEFAULTS);
            GuiEyeAnimations cache = new GuiEyeAnimations();
            Object view = new Object();
            var key = new GuiEyeAnimations.Key(view, GuiEyeAnimations.SLOT, 0, 0, 0);
            ItemStack head = new ItemStack(Items.SKELETON_SKULL);
            SkullEyeState.stampItem(head, new Snapshot(true, 0F, AppearanceOverride.EMPTY.withGlow(true)));
            var presentation = cache.get(key, head, true);
            helper.assertTrue(presentation != null && presentation.helper != null,
                    "an eyed GUI head resolves its configured layout");
            for (int part : presentation.parts) {
                for (var pupil : presentation.eyes[part]) {
                    helper.assertTrue(pupil.guiGravity && !pupil.itemGravity, "each head pupil has GUI orientation");
                }
            }
            BehaviorSettings defaults = BehaviorSettings.DEFAULTS;
            BehaviorSettings cross = new BehaviorSettings(true, 200, 200, List.of(EyeBehavior.CROSS_EYE),
                    0, false, defaults.swirlHealCooldownTicks(), defaults.ongoing());
            boolean played = false;
            for (int tick = 0; tick < OBSERVATION_TICKS; tick++) {
                cache.get(key, head, true);
                cache.tick(view, cross);
                played |= presentation.active != null;
            }
            helper.assertTrue(played, "a paired GUI head can play cross-eye");
            helper.succeed();
        } finally {
            ClientEyeConfigs.replaceAll(definitions, enabled, behaviors);
        }
    }
}
