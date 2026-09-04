package com.github.crittscott.somegoogly.client.render.resolver;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.EntityModel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Resolver for four Twilight Forest mob models — {@code DeerModel}, {@code NewDeerModel},
 * {@code PenguinModel}, and {@code BunnyModel} (Dwarf Rabbit) — that extend an {@link AgeableListModel}
 * family base ({@code QuadrupedModel}/{@code HumanoidModel}) but override {@code renderToBuffer} with
 * their own hand-rolled, hardcoded baby-only scale/translate wraps instead of relying on the inherited
 * formula {@link AgeableListResolver} replays. Being {@code instanceof AgeableListModel}, they would
 * otherwise be claimed by {@link AgeableListResolver}, which would replay the wrong wrap — the generic
 * formula built from the base class's constructor-supplied fields, not these subclasses' own literals.
 * This resolver intercepts the four specific classes first and replays their actual wraps instead.
 *
 * <p>No compile-time dependency on Twilight Forest is needed: everything used ({@code young},
 * {@code headParts()}, {@code bodyParts()}) is inherited vanilla API, made accessible the same way it is
 * for {@link AgeableListResolver}, so only the model's class name has to be checked.
 *
 * <p>All four wraps are young-only; every adult render here has no wrap (unlike vanilla's
 * {@code RabbitModel} — see {@link RabbitLlamaResolver}). {@code DeerModel} and {@code NewDeerModel} share
 * identical wrap literals; which one actually renders a given Deer is chosen per-client by an optional
 * Twilight Forest resource pack, not fixed, so both need covering. {@code NewDeerModel} additionally
 * overrides {@code headParts()} to expose its {@code neck} part rather than the head cube nested beneath
 * it; {@code headParts()} is still the right group to wrap here, since {@code neck} is the actual root
 * Twilight Forest renders inside the wrap.
 */
public class TwilightForestResolver extends ModelPartTreeResolver {

    private static final String DEER_MODEL = "twilightforest.client.model.entity.DeerModel";
    private static final String NEW_DEER_MODEL = "twilightforest.client.model.entity.newmodels.NewDeerModel";
    private static final String PENGUIN_MODEL = "twilightforest.client.model.entity.PenguinModel";
    private static final String BUNNY_MODEL = "twilightforest.client.model.entity.BunnyModel";

    @Override
    public boolean handles(EntityModel<?> model) {
        if (!(model instanceof AgeableListModel)) {
            return false;
        }
        String name = model.getClass().getName();
        return name.equals(DEER_MODEL) || name.equals(NEW_DEER_MODEL)
                || name.equals(PENGUIN_MODEL) || name.equals(BUNNY_MODEL);
    }

    @Override
    protected List<NamedRoot> roots(EntityModel<?> model) {
        AgeableListModel<?> ageable = (AgeableListModel<?>) model;
        String name = model.getClass().getName();

        Consumer<PoseStack> headWrap;
        Consumer<PoseStack> bodyWrap;
        if (name.equals(PENGUIN_MODEL)) {
            // PenguinModel.renderToBuffer wraps both groups in the same young-only scale/translate.
            Consumer<PoseStack> wrap = youngWrap(ageable, 0.5F, 0.5F, 0.5F, 0.0F, 1.5F, 0.0F);
            headWrap = wrap;
            bodyWrap = wrap;
        } else if (name.equals(BUNNY_MODEL)) {
            headWrap = youngWrap(ageable, 0.85F, 0.85F, 0.85F, 0.0F, 0.25F, 0.0F);
            bodyWrap = youngWrap(ageable, 0.8F, 0.8F, 0.8F, 0.0F, 0.37F, 0.0F);
        } else {
            // DeerModel and NewDeerModel: identical wrap literals in both classes' renderToBuffer.
            headWrap = youngWrap(ageable, 0.75F, 0.75F, 0.75F, 0.0F, 0.95F, 0.15F);
            bodyWrap = youngWrap(ageable, 0.5F, 0.5F, 0.5F, 0.0F, 1.5F, 0.0F);
        }

        List<NamedRoot> roots = new ArrayList<>();
        index(roots, "head", ageable.headParts(), headWrap);
        index(roots, "body", ageable.bodyParts(), bodyWrap);
        return roots;
    }

    /**
     * A young-gated uniform-axis scale followed by a translate. {@code young} is re-checked on every call
     * rather than baked in when this closure is built, because {@link AttachmentCache#ATTACHMENTS} caches the
     * resolved chain per (model, token) and the same model instance renders every baby and adult of its
     * type.
     */
    private static Consumer<PoseStack> youngWrap(AgeableListModel<?> ageable, float sx, float sy, float sz,
                                                   float tx, float ty, float tz) {
        return poseStack -> {
            if (!ageable.young) {
                return;
            }
            poseStack.scale(sx, sy, sz);
            poseStack.translate(tx, ty, tz);
        };
    }
}
