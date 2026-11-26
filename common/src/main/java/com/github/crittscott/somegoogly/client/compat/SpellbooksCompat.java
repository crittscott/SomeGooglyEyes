package com.github.crittscott.somegoogly.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;

import java.lang.reflect.Field;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Spellbooks' zombie and skeleton summons use GeckoLib while rising, then a private vanilla
 * renderer. Both paths use vanilla part-local eye coordinates so their placement and picker drafts
 * agree.
 */
public final class SpellbooksCompat {

    private static final String SUMMON_PACKAGE =
            "io.redspace.ironsspellbooks.entity.mobs.raise_dead_summons.";
    private static final Set<String> HYBRID_RENDERERS =
            Set.of(
                    SUMMON_PACKAGE + "SummonedSkeletonMultiRenderer",
                    SUMMON_PACKAGE + "SummonedZombieMultiRenderer");

    private SpellbooksCompat() {}

    /**
     * GeckoLib's bone callback is in model coordinates after translating away from the pivot. Move
     * back to that pivot and turn its X/Y axes into the vanilla part frame used by the nested
     * renderer.
     */
    public static void applyPartSpace(PoseStack pose, float pivotX, float pivotY, float pivotZ) {
        pose.translate(pivotX / 16F, pivotY / 16F, pivotZ / 16F);
        pose.mulPose(Axis.ZP.rotationDegrees(180F));
    }

    /** Whether this renderer switches between a GeckoLib model and a private vanilla renderer. */
    public static boolean isHybridRenderer(String rendererClass) {
        return HYBRID_RENDERERS.contains(rendererClass);
    }

    /**
     * The normal renderer behind a hybrid summon, or null for an unrelated or unavailable
     * integration. Spellbooks exposes no accessor for this package-private field, so reflection is
     * limited to its two known renderer classes and failures leave the other renderers available.
     */
    @Nullable
    public static LivingEntityRenderer<?, ?> vanillaRenderer(EntityRenderer<?> renderer) {
        if (!isHybridRenderer(renderer.getClass().getName())) {
            return null;
        }
        try {
            Field field = renderer.getClass().getDeclaredField("vanillaRenderer");
            field.setAccessible(true);
            if (field.get(renderer) instanceof LivingEntityRenderer<?, ?> livingRenderer) {
                return livingRenderer;
            }
            ClientIntegrationFailures.warnOnce(
                    "Spellbooks", "nested-renderer lookup", renderer.getClass().getName(), null);
        } catch (ReflectiveOperationException | LinkageError failure) {
            ClientIntegrationFailures.warnOnce(
                    "Spellbooks", "nested-renderer lookup", renderer.getClass().getName(), failure);
        }
        return null;
    }
}
