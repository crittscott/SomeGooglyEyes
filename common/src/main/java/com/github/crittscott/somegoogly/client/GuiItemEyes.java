package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.config.ClientConfig;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;
import org.jspecify.annotations.Nullable;

/** Scopes GUI presentation identity during extraction and animation during item drawing. */
public final class GuiItemEyes {

    private static final Deque<Context> DRAWS = new ArrayDeque<>();
    public static final Context EMPTY = new Context(null, new Object(), 0F);
    private static final long NANOS_PER_TICK = 50_000_000L;
    private static final Deque<GuiEyeAnimations.Key> PRESENTATIONS = new ArrayDeque<>();
    private static final GuiEyeAnimations SIMULATIONS = new GuiEyeAnimations();

    private static Context current = EMPTY;
    private static long lastTickNanos = System.nanoTime();

    private GuiItemEyes() {
    }

    public record Context(@Nullable ItemEyes eyes, Object identity, float partialTick) {
    }

    /** Implemented by item render states so GUI context survives deferred submission. */
    public interface Holder {
        Context somegoogly$guiEyes();

        void somegoogly$setGuiEyes(Context context);
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
        boolean enabled = minecraft.level != null && ClientEyeConfigs.googlyEyesEnabled()
                && !ClientConfig.DISABLE_GOOGLY_EYES.get();
        GuiEyeAnimations.Presentation presentation = SIMULATIONS.get(key, stack, enabled);
        float partialTick = Mth.clamp((float) (System.nanoTime() - lastTickNanos) / NANOS_PER_TICK, 0F, 1F);
        beginRender(presentation == null ? EMPTY
                : new Context(presentation.eyes(), presentation.identity(), partialTick));
    }

    /** Slots and carried stacks retain one identity while their on-screen coordinates change. */
    public static void beginPresentation(Object view, int kind, int index) {
        PRESENTATIONS.push(new GuiEyeAnimations.Key(view, kind, index, 0, 0));
    }

    public static void beginRender(Context context) {
        DRAWS.push(current);
        current = context;
    }

    public static void clear() {
        SIMULATIONS.clear();
    }

    public static Context current() {
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
        if (minecraft.level == null || !ClientEyeConfigs.googlyEyesEnabled()
                || ClientConfig.DISABLE_GOOGLY_EYES.get()) {
            clear();
        } else {
            SIMULATIONS.tick(minecraft.screen, ClientEyeConfigs.behaviors());
        }
    }
}
