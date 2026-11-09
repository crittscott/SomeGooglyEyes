package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.eye.BoatEyeLayout;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.registry.ModContent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.ToLongFunction;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Loader-neutral owner of mob, boat, placed-head, framed-item, and held eye simulations and their lifecycle, and of
 * the scope that tells an item draw which animation it belongs to.
 */
public final class ClientEyeRuntime {

    // Anger survives render-tracker eviction but not entity unloading or client world replacement.
    private static final Set<Integer> ANGRY = new HashSet<>();
    private static final Map<LivingEntity, ReceivedBehavior> BEHAVIORS = new HashMap<>();
    private static final Map<Boat, BoatEntry> BOATS = new HashMap<>();
    /** The enclosing item draws' animations; the innermost is in {@link #currentDraw}. */
    private static final Deque<DrawContext> DRAWS = new ArrayDeque<>();

    /** Evict a simulation once it has gone unshown for more than this many of its cache's ticks. */
    private static final int EVICT_IDLE_TICKS = 10;
    private static final Map<ItemFrame, PresentationEyes> FRAMES = new HashMap<>();
    private static final Map<GuiKey, GuiEntry> GUI = new HashMap<>();
    private static final Map<HandKey, HandBehavior> HAND_BEHAVIORS = new HashMap<>();
    private static final Map<HandKey, HandEntry> HANDS = new HashMap<>();
    public static final Object HUD = new Object();
    private static final long NANOS_PER_TICK = 1_000_000_000L / SharedConstants.TICKS_PER_SECOND;
    /** Simulate only what was shown this tick or last (freezes off-screen eyes). */
    private static final int SIMULATE_IDLE_TICKS = 1;
    /** Placed heads' eyes by position, under the same render-driven eviction and simulation as mob trackers. */
    private static final Map<BlockPos, PresentationEyes> SKULLS = new HashMap<>();
    private static final Map<LivingEntity, GooglyTracker> TRACKERS = new HashMap<>();
    private static int clientTicks;
    private static DrawContext current = new DrawContext(null, null, 0F, PresentationEyes.Draw.NONE, null);
    private static long guiSeed;
    private static long guiTicks;
    private static long lastGuiTickNanos = System.nanoTime();
    private static long simulationTicks;

    private ClientEyeRuntime() {
    }

    public enum GuiKind { CURSOR, HOTBAR, POSITION, SLOT }

    public record DrawContext(@Nullable BlockEntity block, @Nullable ItemStack item, float partialTick,
        PresentationEyes.Draw draw, @Nullable GuiKey guiKey) {
    }

    public record GuiKey(Object view, GuiKind kind, int x, int y, int seed) {
    }

    private record HandBehavior(LivingEntity holder, ReceivedBehavior received) {
    }

    private record HandKey(UUID holder, InteractionHand hand) {
    }

    /** Entity-lifetime expression timing, independent of any disposable render tracker. */
    record ReceivedBehavior(EyeBehavior behavior, long seed, long startedAt) {
    }

    private static final class BoatEntry {
        final String attachment;
        final RuntimeConfig config;
        final PresentationEyes eyes;
        long lastShown;

        BoatEntry(String attachment, RuntimeConfig config, PresentationEyes eyes) {
            this.attachment = attachment;
            this.config = config;
            this.eyes = eyes;
        }

        boolean matches(Boat boat, RuntimeConfig current) {
            return config == current && attachment.equals(BoatEyeLayout.attachment(boat));
        }
    }

    private static final class GuiEntry {
        final PresentationEyes eyes;
        long lastShown;
        GuiEntry(PresentationEyes eyes) { this.eyes = eyes; }
    }

    private static final class HandEntry {
        final PresentationEyes eyes;
        final LivingEntity holder;
        long lastShown;
        HandEntry(PresentationEyes eyes, LivingEntity holder) { this.eyes = eyes; this.holder = holder; }
    }

    /** {@code frame}'s item draw starts, with its eye if the item has one; ended by {@link #endItemDraw}. */
    public static void beginFrame(ItemFrame frame, float partialTick) {
        PresentationEyes eyes = frameEye(frame);
        beginItemDraw(eyes == null ? PresentationEyes.Draw.NONE : new PresentationEyes.Draw(eyes, partialTick));
    }

    public static void beginGuiDraw(ItemStack stack, int x, int y, int seed) {
        beginItemDraw(PresentationEyes.supports(stack) ? guiDraw(stack, x, y, seed) : PresentationEyes.Draw.NONE);
    }

    /** GUI identity and selected animations inherit through the one nested draw stack. */
    public static void beginGuiPresentation(Object view, GuiKind kind, int index) {
        push(new DrawContext(current.block(), current.item(), current.partialTick(), current.draw(),
            new GuiKey(view, kind, index, 0, 0)));
    }

    public static void beginHead(BlockEntity block, float partialTick) {
        push(new DrawContext(block, null, partialTick, current.draw(), current.guiKey()));
    }

    /** Unsupported built-in renderers may cancel before RETURN; only head items own this bracket. */
    public static void beginHeadItem(ItemStack stack) {
        if (SkullEyeState.entityFor(stack) != null) {
            push(new DrawContext(null, stack, 0F, current.draw(), current.guiKey()));
        }
    }

    /** An item draw starts, animated by {@code draw}; ended by {@link #endItemDraw}. */
    public static void beginItemDraw(PresentationEyes.Draw draw) {
        push(new DrawContext(current.block(), current.item(), current.partialTick(), draw, current.guiKey()));
    }

    /** The current boat layout, rebuilt only when its state, geometry, or definition changes. */
    @Nullable
    public static PresentationEyes boatEyes(Boat boat) {
        var type = EntityType.getKey(boat.getType());
        RuntimeConfig config = ClientEyeConfigs.get(type, false);
        if (!mayShow(boat)) {
            BOATS.remove(boat);
            return null;
        }
        BoatEntry entry = BOATS.get(boat);
        if (entry == null || !entry.matches(boat, config)) {
            if (!BoatEyeLayout.isEligible(boat, config)) {
                BOATS.remove(boat);
                return null;
            }
            var snapshot = EyeState.snapshot(boat);
            HeadInfo helper = BoatEyeLayout.resolve(boat, config, snapshot.variantRoll());
            entry = new BoatEntry(BoatEyeLayout.attachment(boat), config,
                PresentationEyes.create(boat.getUUID(), type, snapshot, helper));
            BOATS.put(boat, entry);
        }
        entry.lastShown = clientTicks;
        return entry.eyes;
    }

    /** Drop render simulations while retaining entity-lifetime reports. */
    public static void clear() {
        GUI.clear();
        HANDS.clear();
        guiTicks = 0;
        BOATS.clear();
        FRAMES.clear();
        SKULLS.clear();
        TRACKERS.clear();
    }

    /** The world-presentation counter advanced on each unpaused simulation tick. */
    public static int clientTicks() {
        return clientTicks;
    }

    public static DrawContext currentDraw() {
        return current;
    }

    public static void endHeadItem(ItemStack stack) {
        if (current.item() == stack) endItemDraw();
    }

    /** The current item draw is over. */
    public static void endItemDraw() {
        current = DRAWS.pop();
    }

    /** The rendered frame's eye, or none while its item or the global switches disallow animation. */
    @Nullable
    private static PresentationEyes frameEye(ItemFrame frame) {
        if (!ClientEyeConfigs.eyesVisible() || frame.isRemoved() || !PresentationEyes.supports(frame.getItem())) {
            FRAMES.remove(frame);
            return null;
        }
        PresentationEyes eye = FRAMES.get(frame);
        if (eye == null || !eye.matches(frame.getItem())) {
            eye = PresentationEyes.create(frame.getUUID(), frame.getItem());
            if (eye == null) {
                FRAMES.remove(frame);
                return null;
            }
            FRAMES.put(frame, eye);
        }
        eye.markRendered(clientTicks);
        return eye;
    }

    /**
     * The tracker for this entity's current placement, creating one on first use and replacing any
     * existing tracker whose {@link HeadInfo} no longer matches (the eye array is shaped from it, so a
     * variant or age change cannot reuse the old tracker).
     */
    public static GooglyTracker get(LivingEntity living, HeadInfo helper) {
        GooglyTracker tracker = TRACKERS.get(living);
        if (tracker == null || !tracker.matches(helper)) {
            tracker = new GooglyTracker(living, helper, ANGRY.contains(living.getId()), ClientEyeConfigs.behaviors());
            TRACKERS.put(living, tracker);
            synchronizeBehavior(living, tracker);
        }
        return tracker;
    }

    private static PresentationEyes.Draw guiDraw(ItemStack stack, int x, int y, int seed) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !ClientEyeConfigs.eyesVisible()) {
            return PresentationEyes.Draw.NONE;
        }
        Object view = minecraft.screen;
        GuiKey key = current.guiKey();
        if (key == null) {
            key = new GuiKey(view == null ? HUD : view, GuiKind.POSITION, x, y, seed);
        }
        if (key.kind() == GuiKind.HOTBAR) {
            key = new GuiKey(HUD, GuiKind.HOTBAR, seed, 0, 0);
        }
        PresentationEyes eyes = guiEyes(key, stack);
        return eyes == null ? PresentationEyes.Draw.NONE : new PresentationEyes.Draw(eyes,
            Mth.clamp((float) (System.nanoTime() - lastGuiTickNanos) / NANOS_PER_TICK, 0F, 1F));
    }

    public static PresentationEyes guiEyes(GuiKey key, ItemStack stack) {
        GuiEntry entry = GUI.get(key);
        if (entry == null || !entry.eyes.matches(stack)) {
            PresentationEyes eyes = PresentationEyes.create(new UUID(0L, ++guiSeed), stack);
            if (eyes == null) {
                GUI.remove(key);
                return null;
            }
            eyes.useGuiGravity();
            entry = new GuiEntry(eyes);
            GUI.put(key, entry);
        }
        entry.lastShown = guiTicks;
        return entry.eyes;
    }

    public static PresentationEyes.Draw handContext(@Nullable LivingEntity holder, ItemStack stack,
        ItemDisplayContext display) {
        if (holder == null || (display != ItemDisplayContext.THIRD_PERSON_LEFT_HAND
            && display != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
            && display != ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            && display != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) || !PresentationEyes.supports(stack)) {
            return PresentationEyes.Draw.NONE;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !ClientEyeConfigs.eyesVisible()) {
            return PresentationEyes.Draw.NONE;
        }
        HumanoidArm arm = (display == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || display == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
        InteractionHand hand = arm == holder.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if ((display == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || display == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            && !stack.is(ModContent.GOOGLY_EYE.get())) return PresentationEyes.Draw.NONE;
        PresentationEyes eyes = handEyes(holder, hand, stack);
        return eyes == null ? PresentationEyes.Draw.NONE : new PresentationEyes.Draw(eyes, minecraft.getFrameTime());
    }

    public static PresentationEyes handEyes(LivingEntity holder, InteractionHand hand, ItemStack stack) {
        HandKey key = new HandKey(holder.getUUID(), hand);
        if (holder.isRemoved()) {
            HANDS.remove(key);
            return null;
        }
        HandEntry entry = HANDS.get(key);
        if (entry == null || entry.holder != holder || !entry.eyes.matches(stack)) {
            UUID seed = BehaviorSettings.handIdentity(holder.getUUID(), hand);
            PresentationEyes eyes = PresentationEyes.create(seed, stack);
            if (eyes == null) {
                HANDS.remove(key);
                return null;
            }
            entry = new HandEntry(eyes, holder);
            HANDS.put(key, entry);
        }
        entry.lastShown = clientTicks;
        return entry.eyes;
    }

    /** The animation for {@code stack} in the current item draw, if its state still matches. */
    public static PresentationEyes.Draw itemDraw(ItemStack stack) {
        return current.draw().eyes() != null && current.draw().eyes().matches(stack)
        ? current.draw() : PresentationEyes.Draw.NONE;
    }

    private static boolean mayShow(Boat boat) {
        return ClientEyeConfigs.eyesVisible(EntityType.getKey(boat.getType()))
        && !boat.isInvisible() && !boat.isRemoved() && EyeState.hasEyes(boat);
    }

    /** A boat's eye state arrived from the server; a differing state replaces its simulation. */
    public static void onBoatState(Entity boat, EyeState.Snapshot snapshot) {
        BoatEntry entry = BOATS.get(boat);
        if (entry != null && !entry.eyes.snapshot.equals(snapshot)) {
            BOATS.remove(boat);
        }
    }

    /** Forget a removed entity's simulation and server-reported anger. */
    public static void onEntityRemoved(Entity entity) {
        BOATS.remove(entity);
        removeHands(entity.getUUID());
        HAND_BEHAVIORS.keySet().removeIf(key -> key.holder().equals(entity.getUUID()));
        FRAMES.remove(entity);
        TRACKERS.remove(entity);
        ANGRY.remove(entity.getId());
        BEHAVIORS.remove(entity);
    }

    /** Forget simulations and anger when the client replaces or unloads its level. */
    public static void onLevelChanged() {
        clear();
        ANGRY.clear();
        BEHAVIORS.clear();
        HAND_BEHAVIORS.clear();
        DRAWS.clear();
        current = new DrawContext(null, null, 0F, PresentationEyes.Draw.NONE, null);
        simulationTicks = 0;
    }

    /** The live tracker for this entity, or {@code null} if none exists yet (no rebuild on mismatch). */
    @Nullable
    public static GooglyTracker peek(LivingEntity living) {
        return TRACKERS.get(living);
    }

    private static void push(DrawContext frame) {
        DRAWS.push(current);
        current = frame;
    }

    public static void removeHands(UUID holder) {
        HANDS.keySet().removeIf(key -> key.holder().equals(holder));
    }

    /** Record the server-reported anger of {@code living} and pass it to its live tracker, if any. */
    public static void setAngry(LivingEntity living, boolean angry) {
        if (angry) {
            ANGRY.add(living.getId());
        } else {
            ANGRY.remove(living.getId());
        }
        GooglyTracker tracker = TRACKERS.get(living);
        if (tracker != null) {
            tracker.angry = angry;
        }
    }

    /** Retain the latest server expression even before this known entity has rendered. */
    public static void setBehavior(LivingEntity living, EyeBehavior behavior, long seed, int elapsed) {
        BEHAVIORS.put(living, new ReceivedBehavior(behavior, seed, simulationTicks - elapsed));
        GooglyTracker tracker = TRACKERS.get(living);
        if (tracker != null) {
            synchronizeBehavior(living, tracker);
        }
    }

    /** Keep received hand reactions independent of disposable presentation entries. */
    public static void setHandBehavior(LivingEntity holder, InteractionHand hand, EyeBehavior behavior, long seed, int elapsed) {
        HAND_BEHAVIORS.put(new HandKey(holder.getUUID(), hand),
            new HandBehavior(holder, new ReceivedBehavior(behavior, seed, simulationTicks - elapsed)));
    }

    /**
     * The eyes of the eyed head {@code skull}, a head of {@code entity}'s kind, creating them on first use and
     * replacing any built for another kind of head at the same position or from another state; {@code null}
     * when the client's definitions give it nothing to draw. A load with unchanged state keeps the pupils.
     */
    @Nullable
    public static PresentationEyes skullEyes(BlockEntity skull, ResourceLocation entity) {
        PresentationEyes eyes = SKULLS.get(skull.getBlockPos());
        CompoundTag data = SkullEyeState.data(skull);
        if (eyes != null && eyes.headData != data) {
            if (SkullEyeState.snapshot(skull).equals(eyes.snapshot)) {
                eyes.headData = data;
            } else {
                eyes = null;
            }
        }
        if (eyes == null || !eyes.entity.equals(entity)) {
            var snapshot = SkullEyeState.snapshot(skull);
            var layout = ClientEyeConfigs.resolveHead(entity, snapshot.variantRoll());
            int[] parts = SkullEyeState.headParts(entity, layout);
            if (parts.length == 0) {
                SKULLS.remove(skull.getBlockPos());
                return null;
            }
            eyes = PresentationEyes.placedHead(Mth.getSeed(skull.getBlockPos()), entity, snapshot, layout, parts);
            eyes.headData = data;
            SKULLS.put(skull.getBlockPos().immutable(), eyes);
        }
        return eyes;
    }

    /**
     * The render-driven lifecycle every eye cache shares, on that cache's own clock: an entry unshown past
     * {@link #EVICT_IDLE_TICKS} or now {@code stale} is removed, and one shown within
     * {@link #SIMULATE_IDLE_TICKS} steps.
     */
    static <K, V> void sweep(Map<K, V> entries, long now, ToLongFunction<V> lastShown,
        BiPredicate<K, V> stale, BiConsumer<K, V> step) {
        Iterator<Map.Entry<K, V>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<K, V> entry = iterator.next();
            long idle = now - lastShown.applyAsLong(entry.getValue());
            if (idle > EVICT_IDLE_TICKS || stale.test(entry.getKey(), entry.getValue())) {
                iterator.remove();
            } else if (idle <= SIMULATE_IDLE_TICKS) {
                step.accept(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void synchronizeBehavior(LivingEntity living, GooglyTracker tracker) {
        ReceivedBehavior received = BEHAVIORS.get(living);
        if (received == null) {
            return;
        }
        long elapsed = simulationTicks - received.startedAt();
        if (elapsed >= received.behavior().defaultDuration()) {
            BEHAVIORS.remove(living);
            tracker.active = null;
            tracker.receivedBehavior = null;
            return;
        }
        if (tracker.receivedBehavior != received) {
            tracker.install(received.behavior(), received.behavior().defaultDuration(), received.seed(), (int) elapsed);
            tracker.receivedBehavior = received;
        } else if (tracker.active != null) {
            tracker.active.age = (int) elapsed;
        }
    }

    /**
     * Advance the tick counter, evict any tracker idle past {@link #EVICT_IDLE_TICKS}, and step the
     * physics of those rendered this tick or last when {@code simulate} is true. The client lifecycle
     * disables simulation while there is no level or the game is paused.
     */
    public static void tick(@Nullable Level level, boolean simulate, @Nullable Object activeView) {
        lastGuiTickNanos = System.nanoTime();
        if (level == null) {
            clear();
            return;
        }
        BehaviorSettings settings = ClientEyeConfigs.behaviors();
        if (!ClientEyeConfigs.eyesVisible()) {
            clear();
            HAND_BEHAVIORS.clear();
            if (simulate) tickWorld(level.getGameTime(), settings);
            return;
        }
        tickGui(activeView, settings);
        if (simulate) tickWorld(level.getGameTime(), settings);
    }

    /** Advance received expression time once per unpaused simulation tick, even for unrendered entities. */
    private static void tickBehaviors() {
        simulationTicks++;
        BEHAVIORS.entrySet().removeIf(entry -> {
            boolean expired = simulationTicks - entry.getValue().startedAt() >= entry.getValue().behavior().defaultDuration();
            if (expired) {
                GooglyTracker tracker = TRACKERS.get(entry.getKey());
                if (tracker != null) {
                    tracker.active = null;
                    tracker.receivedBehavior = null;
                }
            }
            return expired;
        });
    }

    /** Advance view time even while the world is paused. */
    public static void tickGui(@Nullable Object view, BehaviorSettings settings) {
        guiTicks++;
        sweep(GUI, guiTicks, entry -> entry.lastShown,
            (key, entry) -> key.view() != HUD && key.view() != view,
            (key, entry) -> entry.eyes.tick(guiTicks, settings));
    }

    /** A single world clock serves every world-owned presentation cache. */
    public static void tickWorld(long gameTime, BehaviorSettings settings) {
        clientTicks++;
        tickBehaviors();
        HAND_BEHAVIORS.entrySet().removeIf(entry -> {
            HandBehavior report = entry.getValue();
            return report.holder().isRemoved() || !report.holder().getItemInHand(entry.getKey().hand()).is(ModContent.GOOGLY_EYE.get())
            || simulationTicks - report.received().startedAt() >= report.received().behavior().defaultDuration();
        });
        sweep(HANDS, clientTicks, entry -> entry.lastShown,
            (key, entry) -> entry.holder.isRemoved() || !entry.eyes.matches(entry.holder.getItemInHand(key.hand())),
            (key, entry) -> {
            HandBehavior report = HAND_BEHAVIORS.get(key);
            ReceivedBehavior received = report == null ? null : report.received();
            if (received != null) entry.eyes.react(received.behavior(), received.seed(),
                (int) (simulationTicks - received.startedAt()));
            entry.eyes.tick(gameTime, settings, entry.holder);
        });
        sweep(BOATS, clientTicks, entry -> entry.lastShown,
            (boat, entry) -> !mayShow(boat) || !entry.matches(boat,
            ClientEyeConfigs.get(EntityType.getKey(boat.getType()), false)),
            (boat, entry) -> entry.eyes.tick(gameTime, settings, boat));
        sweep(FRAMES, clientTicks, eye -> eye.lastRenderTick,
            (frame, eye) -> frame.isRemoved(), (frame, eye) -> eye.tick(gameTime, settings));
        sweep(SKULLS, clientTicks, eyes -> eyes.lastRenderTick, (pos, eyes) -> false,
            (pos, eyes) -> eyes.tick(gameTime, settings));
        sweep(TRACKERS, clientTicks, tracker -> tracker.lastRenderTick, (living, tracker) -> false,
            (living, tracker) -> { synchronizeBehavior(living, tracker); tracker.update(settings); });
    }

}
