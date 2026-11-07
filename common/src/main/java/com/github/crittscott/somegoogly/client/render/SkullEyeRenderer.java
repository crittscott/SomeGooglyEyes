package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.client.PresentationEyes;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState.Snapshot;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.client.compat.ClientIntegrationFailures;
import com.github.crittscott.somegoogly.client.picker.ModelPartVocabulary;
import com.github.crittscott.somegoogly.client.render.resolver.RootModelResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import org.joml.Quaternionf;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.object.skull.PiglinHeadModel;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jspecify.annotations.Nullable;

/**
 * Submits the eyes of placed mob heads and head items. A placed head's eyes are chosen while its render state
 * is extracted ({@link #extract}) and kept on that state ({@link PlacedHolder}). Mixins bracket a placed head's
 * submission with {@link #begin} and a head item's with {@link #beginItem}, both ending in {@link #end}, and
 * call {@link #render} right after the head model is submitted, in the model's space. A placed head's eyes are
 * live ({@link PresentationEyes}). Head items in the GUI, in third-person hands, or in frames have independent
 * pupils and ambient expressions. Unanimated items have centered pupils. Worn heads share the model submission
 * outside both brackets, so they draw no eyes.
 *
 * <p>The eyes attach to the model's head part, which a vanilla head model shares with its mob, so the mob's
 * definition places them as they are. Heads-mod models use a cached cube fit to carry eyes to their
 * rebuilt head parts. Honors the client display settings by the head's mob and the server's
 * {@code googlyEyesEnabled} switch.
 */
public final class SkullEyeRenderer {

    /**
     * The part whose cubes stand for a mob's eye part, for mobs whose eyes attach to a part sharing its pivot with
     * a larger one (the chicken's beak, beside its head); any other mob's eye part stands for itself.
     */
    private static final Map<Identifier, String> CUBE_TOKENS = Map.of(
            EntityType.getKey(EntityType.CHICKEN), "head",
            EntityType.getKey(EntityType.SLIME), "root");
    /** The field name the Heads mod's head models keep their head part in. */
    private static final String HEAD_FIELD = "head";
    /** Each Heads-mod model class's field holding its head part, or empty when it has none. */
    private static final Map<Class<?>, Optional<Field>> HEAD_FIELDS = new HashMap<>();
    private static final float PIXELS_PER_BLOCK = 16F;
    private static final float ENDERMAN_EYE_CLEARANCE = 0.5F / PIXELS_PER_BLOCK;
    private static final float SNOW_GOLEM_EYE_SETBACK = 0.06F;
    /** Fitted placements by Heads-mod head model and eye attach token; empty where fitting failed. */
    private static final Map<ModelPart, Map<String, Optional<Placement>>> PLACEMENTS =
            new IdentityHashMap<>();

    private static final Deque<DrawContext> DRAWS = new ArrayDeque<>();

    private static final class DrawContext {
        final @Nullable PresentationEyes eyes;
        final @Nullable ItemStack item;
        final float partialTick;
        boolean itemEyesSubmitted;

        DrawContext(@Nullable PresentationEyes eyes, @Nullable ItemStack item, float partialTick) {
            this.eyes = eyes;
            this.item = item;
            this.partialTick = partialTick;
        }
    }

    private SkullEyeRenderer() {
    }

    /** A cube and the parts below a searched part that lead to it, outermost first. */
    private record FoundCube(ModelPart.Cube cube, ModelPart[] chain) {

        float depth() {
            return cube.maxZ - cube.minZ;
        }

        float height() {
            return cube.maxY - cube.minY;
        }

        float volume() {
            return width() * height() * depth();
        }

        float width() {
            return cube.maxX - cube.minX;
        }
    }

    /**
     * How a Heads-mod head model carries eyes authored for its mob's part: from the head part, down
     * {@code headsChain} to the head's main cube, then the mob's main cube mapped onto it (front-face centers
     * matched, scaled by the smaller of the width and height ratios), then back up {@code mobChain} to the mob's
     * eye part.
     */
    private record Placement(ModelPart[] headsChain, float headsX, float headsY, float headsZ, float scale,
                             float mobX, float mobY, float mobZ, ModelPart[] mobChain) {

        void apply(PoseStack pose) {
            for (ModelPart part : headsChain) {
                part.translateAndRotate(pose);
            }
            pose.translate(headsX, headsY, headsZ);
            pose.scale(scale, scale, scale);
            pose.translate(-mobX, -mobY, -mobZ);
            for (int i = mobChain.length - 1; i >= 0; i--) {
                ModelPart part = mobChain[i];
                if (part.xRot != 0F || part.yRot != 0F || part.zRot != 0F) {
                    pose.mulPose(new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot).invert());
                }
                pose.translate(-part.x / PIXELS_PER_BLOCK, -part.y / PIXELS_PER_BLOCK, -part.z / PIXELS_PER_BLOCK);
            }
        }
    }

    /** An item render state's link to the stack it was last filled from, added by a Mixin. */
    public interface ItemHolder {

        /** The stack, or {@code null} when the state is clear. */
        @Nullable
        ItemStack somegoogly$item();

        void somegoogly$setItem(@Nullable ItemStack stack);
    }

    /** A placed head's render state's link to the eyes chosen when it was extracted, added by a Mixin. */
    public interface PlacedHolder {

        /** The eyes to draw, or {@code null} when the head shows none. */
        @Nullable
        PresentationEyes somegoogly$eyes();

        float somegoogly$partialTick();

        void somegoogly$setEyes(@Nullable PresentationEyes eyes, float partialTick);
    }

    /**
     * The eyes of the placed head {@code skull} to draw this frame, or {@code null} when it has none to show, as
     * its render state is extracted.
     */
    @Nullable
    public static PresentationEyes extract(BlockEntity skull) {
        Identifier entity = SkullEyeState.entityFor(skull.getBlockState().getBlock());
        if (!SkullEyeState.hasEyes(skull) || !mayShow(entity)) {
            return null;
        }
        PresentationEyes eyes = ClientEyeRuntime.skullEyes(skull, entity);
        if (eyes != null) {
            eyes.markRendered(ClientEyeRuntime.clientTicks());
        }
        return eyes;
    }

    /** A placed head's submission starts. */
    public static void begin(PlacedHolder state) {
        DRAWS.push(new DrawContext(state.somegoogly$eyes(), null, state.somegoogly$partialTick()));
    }

    /** An item's submission starts; any but a head item's is ignored, since it never reaches {@link #render}. */
    public static void beginItem(@Nullable ItemStack stack) {
        DRAWS.push(new DrawContext(null, stack, 0F));
    }

    /** Drop fits when renderer rebuilds replace the head and mob models. */
    public static void clearCaches() {
        PLACEMENTS.clear();
    }

    /** The placed head's or head item's submission ends. */
    public static void end() {
        DRAWS.pop();
    }

    /**
     * Fit eyes authored on {@code entity}'s part {@code token} to the Heads-mod head part {@code head}, or
     * {@code null} when either model lacks what the fit needs.
     */
    @Nullable
    private static Placement fit(ModelPart head, Identifier entity, String token) {
        LivingEntity sample = ModelPartVocabulary.sampleFor(entity);
        EntityRenderer<?, ?> renderer = sample == null ? null
                : Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(sample);
        if (!(renderer instanceof LivingEntityRenderer<?, ?, ?> livingRenderer)) {
            return null;
        }
        EntityModel<?> mobModel = ModelPartVocabulary.modelFor(livingRenderer, sample);
        ModelPart mobPart = RootModelResolver.part(mobModel, CUBE_TOKENS.getOrDefault(entity, token));
        FoundCube mob = mobPart == null ? null : largestCube(mobPart, new ArrayList<>());
        FoundCube heads = largestCube(head, new ArrayList<>());
        if (mob == null || heads == null || mob.width() <= 0F || mob.height() <= 0F) {
            return null;
        }
        float scale = Math.min(heads.width() / mob.width(), heads.height() / mob.height());
        return new Placement(heads.chain(),
                (heads.cube().minX + heads.cube().maxX) / 2F / PIXELS_PER_BLOCK,
                (heads.cube().minY + heads.cube().maxY) / 2F / PIXELS_PER_BLOCK,
                heads.cube().minZ / PIXELS_PER_BLOCK, scale,
                (mob.cube().minX + mob.cube().maxX) / 2F / PIXELS_PER_BLOCK,
                (mob.cube().minY + mob.cube().maxY) / 2F / PIXELS_PER_BLOCK,
                mob.cube().minZ / PIXELS_PER_BLOCK, mob.chain());
    }

    /** {@code type}'s nearest {@value #HEAD_FIELD} field holding a {@link ModelPart}, made accessible. */
    private static Optional<Field> headField(Class<?> type) {
        for (Class<?> c = type; c != null && c != SkullModelBase.class; c = c.getSuperclass()) {
            try {
                Field field = c.getDeclaredField(HEAD_FIELD);
                if (field.getType() == ModelPart.class) {
                    field.setAccessible(true);
                    return Optional.of(field);
                }
            } catch (NoSuchFieldException ignored) {
                // Look in the superclass.
            } catch (RuntimeException failure) {
                ClientIntegrationFailures.warnOnce("Heads", "head part lookup", type.getName(), failure);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /** The head part of a model eyes can attach to, or {@code null} for any other head model (the dragon's). */
    @Nullable
    private static ModelPart headPart(SkullModelBase model) {
        if (model instanceof SkullModel skull) {
            return skull.head;
        }
        if (model instanceof PiglinHeadModel piglin) {
            return piglin.head;
        }
        Optional<Field> field = HEAD_FIELDS.computeIfAbsent(model.getClass(), SkullEyeRenderer::headField);
        try {
            return field.isPresent() ? (ModelPart) field.get().get(model) : null;
        } catch (ReflectiveOperationException failure) {
            ClientIntegrationFailures.warnOnce("Heads", "head part lookup", model.getClass().getName(), failure);
            return null;
        }
    }

    /**
     * The largest cube in {@code part} and its descendants, with the parts below {@code part} that lead to it;
     * {@code path} holds those leading to {@code part} itself.
     */
    @Nullable
    private static FoundCube largestCube(ModelPart part, List<ModelPart> path) {
        FoundCube best = null;
        for (ModelPart.Cube cube : part.cubes) {
            FoundCube found = new FoundCube(cube, path.toArray(new ModelPart[0]));
            if (best == null || found.volume() > best.volume()) {
                best = found;
            }
        }
        for (ModelPart child : part.children.values()) {
            path.add(child);
            FoundCube found = largestCube(child, path);
            path.remove(path.size() - 1);
            if (found != null && (best == null || found.volume() > best.volume())) {
                best = found;
            }
        }
        return best;
    }

    /** Whether eyes of {@code entity}'s kind of head may show on this client now. */
    private static boolean mayShow(@Nullable Identifier entity) {
        return entity != null && ClientEyeConfigs.eyesVisible(entity);
    }

    /** The cached fit for eyes on {@code entity}'s part {@code token} drawn by the Heads-mod head {@code model}. */
    @Nullable
    private static Placement placement(ModelPart head, Identifier entity,
                                       String token) {
        return PLACEMENTS.computeIfAbsent(head, m -> new HashMap<>()).computeIfAbsent(token, t -> {
            Placement fitted = fit(head, entity, t);
            if (fitted == null) {
                ClientIntegrationFailures.warnOnce("Heads", "eye placement", entity + " " + t,
                        new IllegalStateException("no cube to fit eyes by"));
            }
            return Optional.ofNullable(fitted);
        }).orElse(null);
    }

    /**
     * Submit the current placed head's or head item's eyes, if it has any to show, with {@code pose} in the space
     * {@code model} was just submitted in, the head's {@code packedLight}, and its extracted model state. The model is posed only when it is drawn, after submission, so it is posed here
     * the same way first.
     */
    public static void render(PoseStack pose, SubmitNodeCollector collector, int packedLight, SkullModelBase model,
                              SkullModelBase.State modelState) {
        DrawContext draw = DRAWS.peek();
        if (draw == null || draw.eyes == null && (draw.item == null || draw.itemEyesSubmitted)) {
            return;
        }
        ModelPart head = headPart(model);
        if (head != null) {
            model.setupAnim(modelState);
            renderHead(pose, collector, packedLight, head, false);
        }
    }

    public static void renderTrophy(PoseStack pose, SubmitNodeCollector collector, int packedLight, ModelPart head) {
        renderHead(pose, collector, packedLight, head, true);
    }

    private static void renderHead(PoseStack pose, SubmitNodeCollector collector, int packedLight,
                                   ModelPart head, boolean trophy) {
        DrawContext draw = DRAWS.peek();
        if (draw == null || draw.eyes == null && (draw.item == null || draw.itemEyesSubmitted)) {
            return;
        }
        PresentationEyes eyes = draw.eyes;
        ItemStack stack = draw.item;
        if (eyes != null) {
            renderEyes(pose, collector, packedLight, head, eyes.headsMod || trophy && !SkullEyeState.HYDRA_TROPHY_LAYOUT.equals(eyes.entity),
                    eyes.entity, eyes.helper, eyes.parts, eyes.eyes,
                    eyes.snapshot.properties(), draw.partialTick);
        } else {
            Identifier entity = SkullEyeState.entityFor(stack);
            if (!mayShow(entity)) {
                return;
            }
            boolean headsMod = trophy ? !SkullEyeState.HYDRA_TROPHY_LAYOUT.equals(entity)
                    : stack.getItem() instanceof BlockItem item && SkullEyeState.isHeadsMod(item.getBlock());
            PresentationEyes.Draw itemDraw = ClientEyeRuntime.itemDraw(stack);
            PresentationEyes animated = itemDraw.eyes();
            if (animated != null) {
                draw.itemEyesSubmitted = true;
                renderEyes(pose, collector, packedLight, head, headsMod, entity, animated.helper, animated.parts,
                        animated.eyes, animated.properties, itemDraw.partialTick());
                return;
            }
            Snapshot snapshot = SkullEyeState.itemSnapshot(stack);
            if (snapshot != null) {
                HeadInfo helper = ClientEyeConfigs.resolveHead(entity, snapshot.variantRoll());
                if (helper.hasConfig()) {
                    draw.itemEyesSubmitted = true;
                    renderEyes(pose, collector, packedLight, head, headsMod, entity, helper,
                            SkullEyeState.headParts(entity, helper), null, snapshot.properties(), 0F);
                }
            }
        }
    }

    /**
     * Submit the eyes of {@code helper}'s heads {@code parts} on the model's {@code head} part, each simulated by its
     * entry in {@code eyes}, or at rest when {@code eyes} is {@code null}.
     */
    private static void renderEyes(PoseStack pose, SubmitNodeCollector collector, int packedLight,
                                   ModelPart head, boolean headsMod, Identifier entity, HeadInfo helper, int[] parts, GooglyTracker.EyeInfo @Nullable [][] eyes,
                                   AppearanceOverride overrides, float partialTick) {
        pose.pushPose();
        head.translateAndRotate(pose);
        // The Enderman Skull's face uses the mob layout's head-local coordinates.
        boolean endermanFace = headsMod && EntityType.getKey(EntityType.ENDERMAN).equals(entity);
        boolean snowGolemFace = headsMod && EntityType.getKey(EntityType.SNOW_GOLEM).equals(entity);
        for (int part : parts) {
            Placement placement = null;
            if (headsMod && !endermanFace) {
                placement = placement(head, entity, helper.getAttachToken(part));
                if (placement == null) {
                    continue;
                }
            }
            pose.pushPose();
            if (endermanFace) {
                pose.translate(0F, 0F, -ENDERMAN_EYE_CLEARANCE);
            } else if (placement != null) {
                placement.apply(pose);
                if (snowGolemFace) {
                    pose.translate(0F, 0F, SNOW_GOLEM_EYE_SETBACK);
                }
            }
            GooglyEyeRenderer.renderHead(pose, collector, packedLight, OverlayTexture.NO_OVERLAY,
                    eyes == null ? null : eyes[part], helper, overrides, part, partialTick);
            pose.popPose();
        }
        pose.popPose();
    }
}
