package com.github.crittscott.somegoogly.client.render.resolver;

import com.github.crittscott.somegoogly.client.compat.ThirdPartyModelWraps;
import com.github.crittscott.somegoogly.config.EyeConfigModel;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.LlamaModel;
import net.minecraft.client.model.RabbitModel;

/**
 * Resolver for {@link RabbitModel} and {@link LlamaModel} — two vanilla models that extend
 * {@link EntityModel} directly (neither {@link net.minecraft.client.model.HierarchicalModel} nor
 * {@link net.minecraft.client.model.AgeableListModel}) and hand-roll their own scale/translate wraps
 * around groups of their top-level parts inside {@code renderToBuffer}, applied and popped entirely
 * outside the part tree, where a render layer's {@link PoseStack} never sees them. This resolver replays
 * each wrap for the parts it encloses, grouped per model rather than by a uniform head/body split as in
 * {@link AgeableListResolver}. It must precede {@link ChildMapResolver}, whose positional fallback has no
 * concept of either wrap.
 *
 * <p>Rabbit's wrap is unusual: its <b>adult</b> render also lives inside its own
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

    private static List<NamedRoot> llamaRoots(LlamaModel<?> llama) {
        Consumer<PoseStack> headWrap = ThirdPartyModelWraps.groupTransform(llama, EyeConfigModel.PART_HEAD);
        Consumer<PoseStack> bodyWrap = ThirdPartyModelWraps.groupTransform(llama, EyeConfigModel.PART_BODY);
        Consumer<PoseStack> legsChestWrap = ThirdPartyModelWraps.groupTransform(llama, "limbs");
        return List.of(
            new NamedRoot(EyeConfigModel.PART_HEAD, llama.head, headWrap),
            new NamedRoot(EyeConfigModel.PART_BODY, llama.body, bodyWrap),
            new NamedRoot("rightHindLeg", llama.rightHindLeg, legsChestWrap),
            new NamedRoot("leftHindLeg", llama.leftHindLeg, legsChestWrap),
            new NamedRoot("rightFrontLeg", llama.rightFrontLeg, legsChestWrap),
            new NamedRoot("leftFrontLeg", llama.leftFrontLeg, legsChestWrap),
            new NamedRoot("rightChest", llama.rightChest, legsChestWrap),
            new NamedRoot("leftChest", llama.leftChest, legsChestWrap)
            );
    }

    private static List<NamedRoot> rabbitRoots(RabbitModel<?> rabbit) {
        Consumer<PoseStack> headWrap = ThirdPartyModelWraps.groupTransform(rabbit, EyeConfigModel.PART_HEAD);
        Consumer<PoseStack> bodyWrap = ThirdPartyModelWraps.groupTransform(rabbit, EyeConfigModel.PART_BODY);
        return List.of(
            new NamedRoot(EyeConfigModel.PART_HEAD, rabbit.head, headWrap),
            new NamedRoot("rightEar", rabbit.rightEar, headWrap),
            new NamedRoot("leftEar", rabbit.leftEar, headWrap),
            new NamedRoot("nose", rabbit.nose, headWrap),
            new NamedRoot("leftRearFoot", rabbit.leftRearFoot, bodyWrap),
            new NamedRoot("rightRearFoot", rabbit.rightRearFoot, bodyWrap),
            new NamedRoot("leftHaunch", rabbit.leftHaunch, bodyWrap),
            new NamedRoot("rightHaunch", rabbit.rightHaunch, bodyWrap),
            new NamedRoot(EyeConfigModel.PART_BODY, rabbit.body, bodyWrap),
            new NamedRoot("leftFrontLeg", rabbit.leftFrontLeg, bodyWrap),
            new NamedRoot("rightFrontLeg", rabbit.rightFrontLeg, bodyWrap),
            new NamedRoot("tail", rabbit.tail, bodyWrap)
            );
    }

    @Override
    protected List<NamedRoot> roots(EntityModel<?> model) {
        if (model instanceof RabbitModel) {
            return rabbitRoots((RabbitModel<?>) model);
        }
        return llamaRoots((LlamaModel<?>) model);
    }
}
