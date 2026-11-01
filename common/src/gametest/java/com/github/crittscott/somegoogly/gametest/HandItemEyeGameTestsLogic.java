package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.client.HandEyeAnimations;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.UUID;

/** Hand simulation checks; holder attachment and extracted draws need the client checklist. */
public final class HandItemEyeGameTestsLogic {

    private static final UUID FIRST = new UUID(1L, 2L);
    private static final int IDLE_TICKS = 12;
    private static final int OBSERVATION_TICKS = 400;
    private static final UUID SECOND = new UUID(3L, 4L);

    private HandItemEyeGameTestsLogic() {
    }

    private static ItemStack eye() {
        return new ItemStack(ModContent.GOOGLY_EYE.get());
    }

    /**
     * Replacement and eligibility gates retire the former hand state. In game: swap a held eye for a
     * glowing copy, a plain skull, and an ordinary item; no previous layout or color persists.
     */
    public static void handEyesReplaceContentsAndHonorGates(GameTestHelper helper) {
        HandEyeAnimations cache = new HandEyeAnimations();
        ItemStack stack = eye();
        var first = cache.get(FIRST, InteractionHand.MAIN_HAND, stack, true);
        ItemStack copy = stack.copy();
        copy.setCount(2);
        helper.assertTrue(cache.get(FIRST, InteractionHand.MAIN_HAND, copy, true) == first,
                Component.literal("stack copies and counts retain the hand simulation"));
        EyeItemProperties.set(copy, AppearanceOverride.EMPTY.withGlow(true));
        var changed = cache.get(FIRST, InteractionHand.MAIN_HAND, copy, true);
        helper.assertTrue(changed != first, Component.literal("appearance changes replace the hand simulation"));
        helper.assertTrue(cache.get(FIRST, InteractionHand.MAIN_HAND, copy, false) == null,
                Component.literal("master/client gates retire hand animation"));
        helper.assertTrue(cache.get(FIRST, InteractionHand.MAIN_HAND, new ItemStack(Items.SKELETON_SKULL), true) == null,
                Component.literal("plain heads do not acquire eyes"));
        helper.assertTrue(cache.get(FIRST, InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND), true) == null,
                Component.literal("ordinary items are unsupported"));
        helper.assertTrue(cache.get(FIRST, InteractionHand.MAIN_HAND, ItemStack.EMPTY, true) == null,
                Component.literal("empty hands are unsupported"));
        helper.succeed();
    }

    /**
     * Unloading, hidden hands, and resource/world resets drop disposable state. In game: hide a holder,
     * unload its chunk, reload resources, and reconnect; no old hand state remains attached.
     */
    public static void handEyesRetireUnloadedAndHiddenHolders(GameTestHelper helper) {
        HandEyeAnimations cache = new HandEyeAnimations();
        var first = cache.get(FIRST, InteractionHand.MAIN_HAND, eye(), true);
        var offhand = cache.get(FIRST, InteractionHand.OFF_HAND, eye(), true);
        var other = cache.get(SECOND, InteractionHand.MAIN_HAND, eye(), true);
        cache.remove(FIRST);
        helper.assertTrue(cache.get(FIRST, InteractionHand.MAIN_HAND, eye(), true) != first
                        && cache.get(FIRST, InteractionHand.OFF_HAND, eye(), true) != offhand,
                Component.literal("entity removal drops both hands"));
        helper.assertTrue(cache.get(SECOND, InteractionHand.MAIN_HAND, eye(), true) == other,
                Component.literal("another holder survives removal"));
        for (int tick = 0; tick < IDLE_TICKS; tick++) {
            cache.tick(tick, true, BehaviorSettings.NONE);
        }
        helper.assertTrue(cache.get(SECOND, InteractionHand.MAIN_HAND, eye(), true) != other,
                Component.literal("unseen hands expire"));
        var current = cache.get(SECOND, InteractionHand.MAIN_HAND, eye(), true);
        cache.clear();
        helper.assertTrue(cache.get(SECOND, InteractionHand.MAIN_HAND, eye(), true) != current,
                Component.literal("resource/world reset drops hand state"));
        helper.succeed();
    }

    /**
     * Holders and logical hands have independent pupils and expression seeds. In game: F5 with eyes in
     * both hands beside another player holding eyes; all presentations animate independently.
     */
    public static void handEyesSeparateHoldersAndHands(GameTestHelper helper) {
        HandEyeAnimations cache = new HandEyeAnimations();
        var main = cache.get(FIRST, InteractionHand.MAIN_HAND, eye(), true);
        var offhand = cache.get(FIRST, InteractionHand.OFF_HAND, eye(), true);
        var other = cache.get(SECOND, InteractionHand.MAIN_HAND, eye(), true);
        helper.assertTrue(main != offhand && main != other && offhand != other,
                Component.literal("each holder and hand owns its pupils"));
        helper.assertTrue(main.eye.itemGravity && !main.eye.guiGravity
                        && offhand.eye.itemGravity && !offhand.eye.guiGravity,
                Component.literal("hand pupils use world orientation"));
        helper.succeed();
    }

    /**
     * The world clock advances ambient expressions and pause freezes them. In game: pause in F5,
     * resume, and compare with inventory animation; only the world presentation freezes.
     */
    public static void handEyesUseWorldClockAndPause(GameTestHelper helper) {
        HandEyeAnimations cache = new HandEyeAnimations();
        var eye = cache.get(FIRST, InteractionHand.MAIN_HAND, eye(), true);
        BehaviorSettings defaults = BehaviorSettings.DEFAULTS;
        BehaviorSettings blink = new BehaviorSettings(true, 1, 1, List.of(EyeBehavior.BLINK),
                0, false, defaults.swirlHealCooldownTicks(), defaults.ongoing());
        cache.tick(1L, true, blink);
        helper.assertTrue(eye.active != null && eye.active.age == 1, Component.literal("world time selects expression age"));
        cache.get(FIRST, InteractionHand.MAIN_HAND, eye(), true);
        cache.tick(2L, false, blink);
        helper.assertTrue(eye.active.age == 1, Component.literal("pause freezes the hand expression"));
        cache.get(FIRST, InteractionHand.MAIN_HAND, eye(), true);
        cache.tick(2L, true, blink);
        helper.assertTrue(eye.active.age == 2, Component.literal("resume follows world time"));
        cache.get(FIRST, InteractionHand.MAIN_HAND, eye(), true);
        cache.tick(3L, true, BehaviorSettings.NONE);
        helper.assertTrue(eye.active == null, Component.literal("ambient off removes expressions while physics remains"));
        helper.succeed();
    }

    /**
     * Eyed head items retain their configured layout and paired ambient pool. In game: hold an eyed
     * skeleton skull in F5, shorten the ambient interval, and select cross-eye; its pupils cross.
     */
    public static void handHeadUsesConfiguredLayout(GameTestHelper helper) {
        var definitions = ClientEyeConfigs.all();
        BehaviorSettings behaviors = ClientEyeConfigs.behaviors();
        boolean enabled = ClientEyeConfigs.googlyEyesEnabled();
        try {
            ClientEyeConfigs.replaceAll(ServerEyeConfigs.all(), true, BehaviorSettings.DEFAULTS);
            HandEyeAnimations cache = new HandEyeAnimations();
            ItemStack head = new ItemStack(Items.SKELETON_SKULL);
            SkullEyeState.stampItem(head, new Snapshot(true, 0F, AppearanceOverride.EMPTY.withGlow(true)));
            var eyes = cache.get(FIRST, InteractionHand.MAIN_HAND, head, true);
            helper.assertTrue(eyes != null && eyes.helper != null, Component.literal("eyed heads resolve their configured layout"));
            for (int part : eyes.parts) {
                for (var pupil : eyes.eyes[part]) {
                    helper.assertTrue(pupil.itemGravity && !pupil.guiGravity, Component.literal("each head pupil uses world gravity"));
                }
            }
            BehaviorSettings defaults = BehaviorSettings.DEFAULTS;
            BehaviorSettings cross = new BehaviorSettings(true, 200, 200, List.of(EyeBehavior.CROSS_EYE),
                    0, false, defaults.swirlHealCooldownTicks(), defaults.ongoing());
            boolean played = false;
            for (int tick = 0; tick < OBSERVATION_TICKS; tick++) {
                cache.get(FIRST, InteractionHand.MAIN_HAND, head, true);
                cache.tick(tick, true, cross);
                played |= eyes.active != null;
            }
            helper.assertTrue(played, Component.literal("a paired hand-held head can play cross-eye"));
            helper.succeed();
        } finally {
            ClientEyeConfigs.replaceAll(definitions, enabled, behaviors);
        }
    }
}
