package com.github.crittscott.somegoogly.client.render.resolver;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.EntityModel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Resolver for the {@link AgeableListModel} family — the base of most vanilla mob models (humanoids,
 * quadrupeds, and many others). These keep no named root, but they <i>do</i> expose their parts in two
 * groups, {@code headParts()} and {@code bodyParts()} (made accessible by the mod's access transformer).
 * We name those roots {@code head}/{@code body} (the first of each group) and {@code head1}, {@code body1},
 * … for any extras, then walk each subtree's {@code children} map for deeper real names.
 *
 * <p>This is what gives a cow or a player the stable token {@code head} instead of a positional {@code #0}
 * — the head is a root field whose own name is SRG-mangled in production, so only this group accessor
 * recovers it reliably. It must follow {@link HierarchicalResolver} (named-root models keep their cleaner
 * path) and precede {@link ChildMapResolver} (the positional catch-all).
 *
 * <p>For a young entity, {@link AgeableListModel#renderToBuffer} wraps {@code headParts()} and
 * {@code bodyParts()} each in their own baby-only scale/offset, applied and popped entirely inside that
 * method — invisible to a render layer's {@link PoseStack} otherwise. {@link #headWrap}/{@link #bodyWrap}
 * replay that wrap (made accessible by the mod's access transformer) so a baby's attach points land where
 * the model actually draws them, not where the same tokens sit on an adult.
 */
public class AgeableListResolver extends ModelPartTreeResolver {

    @Override
    public boolean handles(EntityModel<?> model) {
        return model instanceof AgeableListModel;
    }

    @Override
    protected List<NamedRoot> roots(EntityModel<?> model) {
        AgeableListModel<?> ageable = (AgeableListModel<?>) model;
        List<NamedRoot> roots = new ArrayList<>();
        index(roots, "head", ageable.headParts(), headWrap(ageable));
        index(roots, "body", ageable.bodyParts(), bodyWrap(ageable));
        return roots;
    }

    /**
     * The scale/offset {@link AgeableListModel#renderToBuffer} applies around {@code headParts()} for a
     * young entity. {@code young} is re-checked on every call rather than baked in when this closure is
     * built, because {@link AttachmentCache#ATTACHMENTS} caches the resolved chain per (model, token) and the
     * same model instance renders both baby and adult entities of its type.
     */
    private static Consumer<PoseStack> headWrap(AgeableListModel<?> ageable) {
        boolean scaleHead = ageable.scaleHead;
        float babyHeadScale = ageable.babyHeadScale;
        float yOffset = ageable.babyYHeadOffset;
        float zOffset = ageable.babyZHeadOffset;
        return poseStack -> {
            if (!ageable.young) {
                return;
            }
            if (scaleHead) {
                float f = 1.5F / babyHeadScale;
                poseStack.scale(f, f, f);
            }
            poseStack.translate(0.0F, yOffset / 16.0F, zOffset / 16.0F);
        };
    }

    /** Same as {@link #headWrap}, for the scale/offset {@code renderToBuffer} applies around {@code bodyParts()}. */
    private static Consumer<PoseStack> bodyWrap(AgeableListModel<?> ageable) {
        float babyBodyScale = ageable.babyBodyScale;
        float yOffset = ageable.bodyYOffset;
        return poseStack -> {
            if (!ageable.young) {
                return;
            }
            float f = 1.0F / babyBodyScale;
            poseStack.scale(f, f, f);
            poseStack.translate(0.0F, yOffset / 16.0F, 0.0F);
        };
    }
}
