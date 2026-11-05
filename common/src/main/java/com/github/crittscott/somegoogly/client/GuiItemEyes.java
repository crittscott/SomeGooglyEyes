package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;

/** Scopes GUI presentation keys and animation around item drawing. */
public final class GuiItemEyes {

    private static final Deque<PresentationEyes.Draw> DRAWS = new ArrayDeque<>();
    private static final long NANOS_PER_TICK = 50_000_000L;
    private static final Deque<GuiEyeAnimations.Key> PRESENTATIONS = new ArrayDeque<>();
    private static final GuiEyeAnimations SIMULATIONS = new GuiEyeAnimations();

    private static PresentationEyes.Draw current = PresentationEyes.Draw.NONE;
    private static long lastTickNanos = System.nanoTime();

    private GuiItemEyes() {
    }

    /** Called by the GUI's core item method, covering slots, the hotbar, and generic previews. */
    public static void beginDraw(ItemStack stack, int x, int y, int seed) {
        Minecraft minecraft = Minecraft.getInstance();
        Object view = minecraft.screen;
        GuiEyeAnimations.Key key = PRESENTATIONS.peek();
        if (key == null) {
            key = new GuiEyeAnimations.Key(view == null ? GuiEyeAnimations.HUD : view,
                    GuiEyeAnimations.POSITION, x, y, seed);
        }
        if (key.kind() == GuiEyeAnimations.HOTBAR) {
            key = new GuiEyeAnimations.Key(GuiEyeAnimations.HUD, GuiEyeAnimations.HOTBAR, seed, 0, 0);
        }
        boolean enabled = minecraft.level != null && ClientEyeConfigs.eyesVisible();
        PresentationEyes eyes = SIMULATIONS.get(key, stack, enabled);
        float partialTick = Mth.clamp((float) (System.nanoTime() - lastTickNanos) / NANOS_PER_TICK, 0F, 1F);
        DRAWS.push(current);
        current = eyes == null ? PresentationEyes.Draw.NONE : new PresentationEyes.Draw(eyes, partialTick);
    }

    /** Slots and carried stacks retain one simulation while their on-screen coordinates change. */
    public static void beginPresentation(Object view, int kind, int index) {
        PRESENTATIONS.push(new GuiEyeAnimations.Key(view, kind, index, 0, 0));
    }

    public static void clear() {
        SIMULATIONS.clear();
    }

    public static PresentationEyes.Draw current() {
        return current;
    }

    public static void endDraw() {
        current = DRAWS.pop();
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
