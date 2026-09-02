package com.github.crittscott.somegoogly.client.render.resolver;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.LlamaModel;
import net.minecraft.client.model.RabbitModel;

import java.util.List;
import java.util.function.Consumer;

/**
 * Resolver for {@link RabbitModel} and {@link LlamaModel} — two vanilla models that extend
 * {@link EntityModel} directly (neither {@link net.minecraft.client.model.HierarchicalModel} nor
 * {@link net.minecraft.client.model.AgeableListModel}) and hand-roll their own scale/translate wraps
 * around groups of their top-level parts inside {@code renderToBuffer}, applied and popped entirely
 * outside the part tree — invisible to a render layer's {@link PoseStack} otherwise, the same bug class
 * {@link AgeableListResolver} fixes for its own family, but with a model-specific part grouping instead of
 * a uniform head/body split. Tried before {@link ChildMapResolver}, whose positional fallback has no
 * concept of either wrap.
 *
 * <p>Rabbit's wrap is unusual among this bug family: its <b>adult</b> render also lives inside its own
 * wrap (not just the baby one), so both ages need a replayed transform, split into a head-group
 * ({@code head}, {@code rightEar}, {@code leftEar}, {@code nose}) and a body-group (everything else).
 * Llama's adult render has no wrap at all; its three groups ({@code head}; {@code body}; the four legs
 * plus two chest boxes) each apply only when young.
 */
public class RabbitLlamaResolver extends ModelPartTreeResolver {

    @Override
    public boolean handles(EntityModel<?> model) {
        return model instanceof RabbitModel || model instanceof LlamaModel;
    }

    @Override
    protected List<NamedRoot> roots(EntityModel<?> model) {
        if (model instanceof RabbitModel) {
            return rabbitRoots((RabbitModel<?>) model);
        }
        return llamaRoots((LlamaModel<?>) model);
    }

    private static List<NamedRoot> rabbitRoots(RabbitModel<?> rabbit) {
        Consumer<PoseStack> headWrap = rabbitHeadWrap(rabbit);
        Consumer<PoseStack> bodyWrap = rabbitBodyWrap(rabbit);
        return List.of(
                new NamedRoot("head", rabbit.head, headWrap),
                new NamedRoot("rightEar", rabbit.rightEar, headWrap),
                new NamedRoot("leftEar", rabbit.leftEar, headWrap),
                new NamedRoot("nose", rabbit.nose, headWrap),
                new NamedRoot("leftRearFoot", rabbit.leftRearFoot, bodyWrap),
                new NamedRoot("rightRearFoot", rabbit.rightRearFoot, bodyWrap),
                new NamedRoot("leftHaunch", rabbit.leftHaunch, bodyWrap),
                new NamedRoot("rightHaunch", rabbit.rightHaunch, bodyWrap),
                new NamedRoot("body", rabbit.body, bodyWrap),
                new NamedRoot("leftFrontLeg", rabbit.leftFrontLeg, bodyWrap),
                new NamedRoot("rightFrontLeg", rabbit.rightFrontLeg, bodyWrap),
                new NamedRoot("tail", rabbit.tail, bodyWrap)
        );
    }

    /**
     * {@code head}, {@code rightEar}, {@code leftEar}, {@code nose}: {@code RabbitModel.renderToBuffer}'s
     * head-group wrap when young, its separate (but equally outside-the-tree) adult wrap otherwise.
     * {@code young} is re-checked on every call rather than baked in when this closure is built, because
     * {@link AttachmentCache#ATTACHMENTS} caches the resolved chain per (model, token) and the same model
     * instance renders both baby and adult rabbits.
     */
    private static Consumer<PoseStack> rabbitHeadWrap(RabbitModel<?> rabbit) {
        return poseStack -> {
            if (rabbit.young) {
                poseStack.scale(0.56666666F, 0.56666666F, 0.56666666F);
                poseStack.translate(0.0F, 1.375F, 0.125F);
            } else {
                poseStack.scale(0.6F, 0.6F, 0.6F);
                poseStack.translate(0.0F, 1.0F, 0.0F);
            }
        };
    }

    /**
     * The remaining eight parts: {@code RabbitModel.renderToBuffer}'s body-group wrap when young, the same
     * adult wrap as {@link #rabbitHeadWrap} otherwise (the adult branch renders every part together, so
     * both groups share it).
     */
    private static Consumer<PoseStack> rabbitBodyWrap(RabbitModel<?> rabbit) {
        return poseStack -> {
            if (rabbit.young) {
                poseStack.scale(0.4F, 0.4F, 0.4F);
                poseStack.translate(0.0F, 2.25F, 0.0F);
            } else {
                poseStack.scale(0.6F, 0.6F, 0.6F);
                poseStack.translate(0.0F, 1.0F, 0.0F);
            }
        };
    }

    private static List<NamedRoot> llamaRoots(LlamaModel<?> llama) {
        Consumer<PoseStack> headWrap = llamaYoungWrap(llama, 0.71428573F, 0.64935064F, 0.7936508F, 1.3125F, 0.22F);
        Consumer<PoseStack> bodyWrap = llamaYoungWrap(llama, 0.625F, 0.45454544F, 0.45454544F, 2.0625F, 0.0F);
        Consumer<PoseStack> legsChestWrap = llamaYoungWrap(llama, 0.45454544F, 0.41322312F, 0.45454544F, 2.0625F, 0.0F);
        return List.of(
                new NamedRoot("head", llama.head, headWrap),
                new NamedRoot("body", llama.body, bodyWrap),
                new NamedRoot("rightHindLeg", llama.rightHindLeg, legsChestWrap),
                new NamedRoot("leftHindLeg", llama.leftHindLeg, legsChestWrap),
                new NamedRoot("rightFrontLeg", llama.rightFrontLeg, legsChestWrap),
                new NamedRoot("leftFrontLeg", llama.leftFrontLeg, legsChestWrap),
                new NamedRoot("rightChest", llama.rightChest, legsChestWrap),
                new NamedRoot("leftChest", llama.leftChest, legsChestWrap)
        );
    }

    /**
     * One of {@code LlamaModel.renderToBuffer}'s three independent young-only wraps (head; body; legs +
     * chest) — a no-op on an adult, whose render has no wrap at all. {@code young} is re-checked on every
     * call for the same reason as {@link #rabbitHeadWrap}.
     */
    private static Consumer<PoseStack> llamaYoungWrap(LlamaModel<?> llama, float sx, float sy, float sz,
                                                        float yOffset, float zOffset) {
        return poseStack -> {
            if (!llama.young) {
                return;
            }
            poseStack.scale(sx, sy, sz);
            poseStack.translate(0.0F, yOffset, zOffset);
        };
    }
}
