package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

/** Scopes GUI presentation keys around item drawing, and owns the view-keyed simulations those keys select. */
public final class GuiItemEyes {

    public static final Object HUD = new Object();

    private static final long NANOS_PER_TICK = 1_000_000_000L / SharedConstants.TICKS_PER_SECOND;
    private static final Deque<Key> PRESENTATIONS = new ArrayDeque<>();
    private static final Simulations SIMULATIONS = new Simulations();

    private static long lastTickNanos = System.nanoTime();

    private GuiItemEyes() {
    }

    /** What a {@link Key} identifies an item by. */
    public enum Kind {
        CURSOR, HOTBAR, POSITION, SLOT
    }

    /** Coordinates are used only for generic previews; slots and cursor items use logical indexes. */
    public record Key(Object view, Kind kind, int x, int y, int seed) {
    }

    /** View-owned item simulations with a UI clock independent of world ticks and pause. */
    public static final class Simulations {

        private final Map<Key, Entry> entries = new HashMap<>();
        private long nextSeed;
        private long ticks;

        private static final class Entry {
            final PresentationEyes eyes;
            long lastShown;

            Entry(PresentationEyes eyes) {
                this.eyes = eyes;
            }
        }

        /** Drop view-owned simulations, including ones built from old eye definitions. */
        public void clear() {
            entries.clear();
        }

        /** Request a visible presentation, replacing one built for another item; unsupported items have none. */
        @Nullable
        public PresentationEyes get(Key key, ItemStack stack) {
            Entry entry = entries.get(key);
            if (entry == null || !entry.eyes.matches(stack)) {
                PresentationEyes eyes = PresentationEyes.create(new UUID(0L, ++nextSeed), stack);
                if (eyes == null) {
                    entries.remove(key);
                    return null;
                }
                eyes.useGuiGravity();
                entry = new Entry(eyes);
                entries.put(key, entry);
            }
            entry.lastShown = ticks;
            return entry.eyes;
        }

        /** Advance the UI clock even when the world is paused; hidden and departed views do no simulation. */
        public void tick(@Nullable Object activeView, BehaviorSettings settings) {
            ticks++;
            ClientEyeRuntime.sweep(entries, ticks, entry -> entry.lastShown,
                    (key, entry) -> key.view() != HUD && key.view() != activeView,
                    (key, entry) -> entry.eyes.tick(ticks, settings));
        }
    }

    /**
     * The animation of a supported item's GUI draw, if eyes are visible. A slot's former simulation stays until
     * idle eviction or the next {@link #tick} with eyes hidden.
     */
    private static PresentationEyes.Draw animatedDraw(ItemStack stack, int x, int y, int seed) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !ClientEyeConfigs.eyesVisible()) {
            return PresentationEyes.Draw.NONE;
        }
        Object view = minecraft.screen;
        Key key = PRESENTATIONS.peek();
        if (key == null) {
            key = new Key(view == null ? HUD : view, Kind.POSITION, x, y, seed);
        }
        if (key.kind() == Kind.HOTBAR) {
            key = new Key(HUD, Kind.HOTBAR, seed, 0, 0);
        }
        PresentationEyes eyes = SIMULATIONS.get(key, stack);
        return eyes == null ? PresentationEyes.Draw.NONE : new PresentationEyes.Draw(eyes,
                Mth.clamp((float) (System.nanoTime() - lastTickNanos) / NANOS_PER_TICK, 0F, 1F));
    }

    /** Called by the GUI's core item method, covering slots, the hotbar, and generic previews. */
    public static void beginDraw(ItemStack stack, int x, int y, int seed) {
        ClientEyeRuntime.beginItemDraw(PresentationEyes.supports(stack) ? animatedDraw(stack, x, y, seed)
                : PresentationEyes.Draw.NONE);
    }

    /** Slots and carried stacks retain one simulation while their on-screen coordinates change. */
    public static void beginPresentation(Object view, Kind kind, int index) {
        PRESENTATIONS.push(new Key(view, kind, index, 0, 0));
    }

    public static void clear() {
        SIMULATIONS.clear();
    }

    public static void endPresentation() {
        PRESENTATIONS.pop();
    }

    /** GUI simulation advances on client ticks regardless of the world's pause state. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        lastTickNanos = System.nanoTime();
        if (minecraft.level == null || !ClientEyeConfigs.eyesVisible()) {
            clear();
        } else {
            SIMULATIONS.tick(minecraft.screen, ClientEyeConfigs.behaviors());
        }
    }
}
