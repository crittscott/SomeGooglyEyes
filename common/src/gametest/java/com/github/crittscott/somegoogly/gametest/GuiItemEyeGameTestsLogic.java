package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.PresentationEyes;
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.UUID;

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
     * Inventory icons keep their own clock, so their expressions continue while the world is paused. In game: in single
     * player with {@code ambientBehaviorPool = ["somegoogly:blink"]} and {@code ambientMinTicks = 1} and {@code
     * ambientMaxTicks = 1} in {@code somegoogly-server.toml}, open your inventory with a Googly Eye in it and press Esc
     * to pause; the icon keeps blinking while mobs behind the menu stand still.
     */
    public static void guiEyesAdvanceWithoutWorldTicks(GameTestHelper helper) {
        TestEyeRuntime.GuiCache cache = new TestEyeRuntime.GuiCache();
        Object view = new Object();
        var key = new ClientEyeRuntime.GuiKey(view, ClientEyeRuntime.GuiKind.SLOT, 0, 0, 0);
        ItemStack stack = eye();
        var presentation = cache.get(key, stack);
        BehaviorSettings defaults = TestBehaviorSettings.DEFAULTS;
        BehaviorSettings blink = new BehaviorSettings(true, 1, 1, List.of(EyeBehavior.BLINK),
                0, false, defaults.swirlHealCooldownTicks(), defaults.ongoing());
        cache.tick(view, blink);
        helper.assertTrue(presentation.active != null && presentation.active.age == 1,
                Component.literal("the first UI tick joins its blink without consulting world time"));
        cache.get(key, stack);
        cache.tick(view, blink);
        helper.assertTrue(presentation.active.age == 2, Component.literal("UI time advances independently"));
        helper.succeed();
    }

    /**
     * Only eye items get animated icons, and hiding eyes discards icon state, so showing them again starts fresh. In
     * game: put a Googly Eye, a plain skull, and a diamond in your inventory; only the eye's pupil moves. Set {@code
     * disableGooglyEyes = true} in {@code somegoogly-client.toml}; the icon is drawn plain. Set it back; the pupil
     * moves again, starting from rest.
     */
    public static void guiEyesHonorEligibilityAndSwitch(GameTestHelper helper) {
        TestEyeRuntime.GuiCache cache = new TestEyeRuntime.GuiCache();
        var key = new ClientEyeRuntime.GuiKey(new Object(), ClientEyeRuntime.GuiKind.SLOT, 0, 0, 0);
        var first = cache.get(key, eye());
        cache.clear();
        var restored = cache.get(key, eye());
        helper.assertTrue(restored != first, Component.literal("reenabling creates fresh state"));
        helper.assertTrue(cache.get(key, new ItemStack(Items.DIAMOND)) == null,
                Component.literal("ordinary items have no eye simulation"));
        helper.assertTrue(cache.get(key, new ItemStack(Items.SKELETON_SKULL)) == null,
                Component.literal("plain heads have no eye simulation"));
        helper.assertTrue(cache.get(key, ItemStack.EMPTY) == null, Component.literal("empty slots retire their presentation"));
        helper.succeed();
    }

    /**
     * A slot keeps its eye's simulation when the stack is copied or its count changes, and the pupil falls toward
     * screen-down. In game: with a stack of Googly Eyes in a slot, right-click to pick up half and put it back; the
     * slot's pupil keeps moving smoothly instead of jumping back to rest.
     */
    public static void guiEyesKeepPresentationIdentity(GameTestHelper helper) {
        TestEyeRuntime.GuiCache cache = new TestEyeRuntime.GuiCache();
        var key = new ClientEyeRuntime.GuiKey(new Object(), ClientEyeRuntime.GuiKind.SLOT, 3, 0, 0);
        ItemStack stack = eye();
        var first = cache.get(key, stack);
        ItemStack copy = stack.copy();
        copy.setCount(2);
        var second = cache.get(key, copy);
        helper.assertTrue(first == second,
                Component.literal("copies and counts do not create per-frame simulations"));
        helper.assertTrue(first.eye.guiGravity && !first.eye.itemGravity,
                Component.literal("GUI pupils use screen orientation rather than mounted world orientation"));
        helper.succeed();
    }

    /**
     * A changed appearance gives a slot's eye a new simulation with the new look. In game: put a Googly Eye in a slot,
     * then replace it with a dyed or glowing one; the icon shows the new color or glow at once.
     */
    public static void guiEyesReplaceChangedContents(GameTestHelper helper) {
        TestEyeRuntime.GuiCache cache = new TestEyeRuntime.GuiCache();
        var key = new ClientEyeRuntime.GuiKey(new Object(), ClientEyeRuntime.GuiKind.SLOT, 0, 0, 0);
        ItemStack stack = eye();
        var first = cache.get(key, stack);
        EyeItemProperties.set(stack, AppearanceOverride.EMPTY.withGlow(true));
        var changed = cache.get(key, stack);
        helper.assertTrue(first != changed,
                Component.literal("appearance changes replace the simulation"));
        helper.succeed();
    }

    /**
     * Closing a screen frees its icons' state, the hotbar's icons keep theirs, and icons out of view expire. No visible
     * in-game form, since a fresh pupil starts at rest where the old one settled; guards that closed chests, changed
     * screens, and creative entries scrolled out of view leave no state behind.
     */
    public static void guiEyesRetireClosedAndHiddenViews(GameTestHelper helper) {
        TestEyeRuntime.GuiCache cache = new TestEyeRuntime.GuiCache();
        Object view = new Object();
        var screenKey = new ClientEyeRuntime.GuiKey(view, ClientEyeRuntime.GuiKind.SLOT, 0, 0, 0);
        var hudKey = new ClientEyeRuntime.GuiKey(ClientEyeRuntime.HUD, ClientEyeRuntime.GuiKind.POSITION, 0, 0, 0);
        var screen = cache.get(screenKey, eye());
        var hud = cache.get(hudKey, eye());
        cache.tick(new Object(), BehaviorSettings.NONE);
        helper.assertTrue(cache.get(screenKey, eye()) != screen,
                Component.literal("departing a screen retires its simulations"));
        helper.assertTrue(cache.get(hudKey, eye()) == hud,
                Component.literal("the HUD simulation belongs to its own view"));
        for (int tick = 0; tick < IDLE_TICKS; tick++) {
            cache.tick(null, BehaviorSettings.NONE);
        }
        helper.assertTrue(cache.get(hudKey, eye()) != hud,
                Component.literal("unseen presentations are evicted"));
        var current = cache.get(hudKey, eye());
        cache.clear();
        helper.assertTrue(cache.get(hudKey, eye()) != current,
                Component.literal("resource/world resets invalidate GUI state"));
        helper.succeed();
    }

    /**
     * Each slot and the carried stack animate on their own. In game: fill several slots with Googly Eyes and pick up
     * another stack with the cursor; the slots blink at different times, and the carried eye keeps its own pupil and
     * expressions as you drag it across the occupied slots.
     */
    public static void guiEyesSeparateSlotsAndCursor(GameTestHelper helper) {
        TestEyeRuntime.GuiCache cache = new TestEyeRuntime.GuiCache();
        Object view = new Object();
        var first = cache.get(new ClientEyeRuntime.GuiKey(view, ClientEyeRuntime.GuiKind.SLOT, 0, 0, 0), eye());
        var second = cache.get(new ClientEyeRuntime.GuiKey(view, ClientEyeRuntime.GuiKind.SLOT, 1, 0, 0), eye());
        var cursorKey = new ClientEyeRuntime.GuiKey(view, ClientEyeRuntime.GuiKind.CURSOR, 0, 0, 0);
        var cursor = cache.get(cursorKey, eye());
        helper.assertTrue(first != second && cursor != first, Component.literal("presentations do not share pupils"));
        helper.assertTrue(cache.get(cursorKey, eye().copy()) == cursor,
                Component.literal("the cursor simulation is independent of its moving coordinates"));
        var hotbarKey = new ClientEyeRuntime.GuiKey(ClientEyeRuntime.HUD, ClientEyeRuntime.GuiKind.HOTBAR, 1, 0, 0);
        var hotbar = cache.get(hotbarKey, eye());
        cache.tick(view, BehaviorSettings.NONE);
        helper.assertTrue(cache.get(hotbarKey, eye()) == hotbar,
                Component.literal("hotbar slot seeds remain stable while another screen is open"));
        helper.succeed();
    }

    /**
     * An eyed head in the inventory shows its mob's eyes in place, and both eyes can blink and cross, independent of
     * the same head in a frame. In game: with {@code ambientBehaviorPool = ["somegoogly:blink",
     * "somegoogly:cross_eye"]} in {@code somegoogly-server.toml}, put eyed skeleton skulls in your inventory and a
     * chest and frame another; the icons show both eyes at normal icon size, blink and cross, and do so at different
     * times from the framed skull.
     */
    public static void guiHeadUsesConfiguredLayout(GameTestHelper helper) {
        var definitions = ClientEyeConfigs.all();
        BehaviorSettings behaviors = ClientEyeConfigs.behaviors();
        boolean enabled = ClientEyeConfigs.googlyEyesEnabled();
        try {
            ClientEyeConfigs.replaceAll(ServerEyeConfigs.all());
            ClientEyeConfigs.applySettings(true, TestBehaviorSettings.DEFAULTS);
            TestEyeRuntime.GuiCache cache = new TestEyeRuntime.GuiCache();
            Object view = new Object();
            var key = new ClientEyeRuntime.GuiKey(view, ClientEyeRuntime.GuiKind.SLOT, 0, 0, 0);
            ItemStack head = new ItemStack(Items.SKELETON_SKULL);
            SkullEyeState.stampItem(head, new Snapshot(true, 0F, AppearanceOverride.EMPTY.withGlow(true)));
            var presentation = cache.get(key, head);
            helper.assertTrue(presentation != null && presentation.helper != null,
                    Component.literal("an eyed GUI head resolves its configured layout"));
            for (int part : presentation.parts) {
                for (var pupil : presentation.eyes[part]) {
                    helper.assertTrue(pupil.guiGravity && !pupil.itemGravity, Component.literal("each head pupil has GUI orientation"));
                }
            }
            BehaviorSettings defaults = TestBehaviorSettings.DEFAULTS;
            BehaviorSettings cross = new BehaviorSettings(true, 200, 200, List.of(EyeBehavior.CROSS_EYE),
                    0, false, defaults.swirlHealCooldownTicks(), defaults.ongoing());
            boolean played = false;
            for (int tick = 0; tick < OBSERVATION_TICKS; tick++) {
                cache.get(key, head);
                cache.tick(view, cross);
                played |= presentation.active != null;
            }
            helper.assertTrue(played, Component.literal("a paired GUI head can play cross-eye"));
            helper.succeed();
        } finally {
            ClientEyeConfigs.replaceAll(definitions);
            ClientEyeConfigs.applySettings(enabled, behaviors);
        }
    }

    /**
     * One nested draw context carries GUI identity, the selected animation, and the item being drawn; closing an
     * inner scope restores the enclosing one. A GUI item's animation is selected inside its slot's scope while its
     * render state is filled, and the state carries that selection to its later draw, which no GUI scope encloses.
     * In game: put eyed heads and Googly Eyes in inventory slots, the hotbar, and item frames; each animates on its
     * own and a head drawn inside another item's slot keeps its eyes.
     */
    public static void nestedDrawContextsRestoreOuterSelection(GameTestHelper helper) {
        Object view = new Object();
        PresentationEyes eyes = PresentationEyes.create(new UUID(1L, 2L), eye());
        PresentationEyes.Draw stored;
        ClientEyeRuntime.beginGuiPresentation(view, ClientEyeRuntime.GuiKind.SLOT, 3);
        try {
            ClientEyeRuntime.beginItemDraw(new PresentationEyes.Draw(eyes, 0.5F));
            try {
                stored = ClientEyeRuntime.itemDraw(eye());
                helper.assertTrue(stored.eyes() == eyes && ClientEyeRuntime.currentDraw().guiKey().view() == view,
                        Component.literal("the slot's item draw selects its animation while the render state is filled"));
            } finally {
                ClientEyeRuntime.endItemDraw();
            }
            helper.assertTrue(ClientEyeRuntime.currentDraw().guiKey().x() == 3
                    && ClientEyeRuntime.currentDraw().draw().eyes() == null,
                    Component.literal("closing the item restores its slot's scope"));
        } finally {
            ClientEyeRuntime.endItemDraw();
        }
        helper.assertTrue(ClientEyeRuntime.currentDraw().guiKey() == null, Component.literal("closing the GUI scope restores the outside"));

        ItemStack stack = eye();
        ClientEyeRuntime.beginItemState(stack, stored);
        try {
            ItemStack head = new ItemStack(Items.SKELETON_SKULL);
            ClientEyeRuntime.beginItemState(head, PresentationEyes.Draw.NONE);
            try {
                ClientEyeRuntime.DrawContext context = ClientEyeRuntime.currentDraw();
                helper.assertTrue(context.item() == head && context.draw().eyes() == null,
                        Component.literal("a nested item state carries its own stack and extracted animation"));
            } finally {
                ClientEyeRuntime.endItemDraw();
            }
            ClientEyeRuntime.DrawContext context = ClientEyeRuntime.currentDraw();
            helper.assertTrue(context.item() == stack && context.draw().eyes() == eyes && context.guiKey() == null,
                    Component.literal("the deferred draw keeps the animation selected at extraction"));
        } finally {
            ClientEyeRuntime.endItemDraw();
        }
        helper.assertTrue(ClientEyeRuntime.currentDraw().item() == null
                && ClientEyeRuntime.currentDraw().draw().eyes() == null, Component.literal("closing the draw restores the outside"));
        helper.succeed();
    }
}
