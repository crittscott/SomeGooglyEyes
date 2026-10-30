package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.client.ModelGooglyEye;
import com.github.crittscott.somegoogly.client.SkullEyes;
import com.github.crittscott.somegoogly.client.compat.ClientIntegrationFailures;
import com.github.crittscott.somegoogly.client.picker.ModelPartVocabulary;
import com.github.crittscott.somegoogly.client.render.resolver.Attachment;
import com.github.crittscott.somegoogly.client.render.resolver.Resolvers;
import com.github.crittscott.somegoogly.config.ClientConfig;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState.Snapshot;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PiglinHeadModel;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Draws the eyes of placed mob heads and head items. Each loader's Mixins bracket a placed head's draw with
 * {@link #begin} and a head item's with {@link #beginItem}, both ending in {@link #end}, and call
 * {@link #render} right after the head model is drawn, in the model's space. A placed head's eyes are live
 * ({@link SkullEyes}); an item's are drawn at rest, pupils centered, like the Googly Eye item's. Worn heads
 * share the model draw without either bracket, so they draw no eyes.
 *
 * <p>The eyes attach to the model's head part. A vanilla head model shares that part with its mob, so the mob's
 * definition places them as they are. The Heads mod's models rebuild their mob's part elsewhere, sometimes at
 * another size or tilt, so each of its heads carries the eyes through a {@link Placement} fitted from the two
 * models' cubes. Honors the client display settings by the head's mob and the server's {@code googlyEyesEnabled}
 * switch.
 */
public final class SkullEyeRenderer {

    /**
     * The part whose cubes stand for a mob's eye part, for mobs whose eyes attach to a part sharing its pivot with
     * a larger one (the chicken's beak, beside its head); any other mob's eye part stands for itself.
     */
    private static final Map<ResourceLocation, String> CUBE_TOKENS = Map.of(
            EntityType.getKey(EntityType.CHICKEN), "head");
    /** The field name the Heads mod's head models keep their head part in. */
    private static final String HEAD_FIELD = "head";
    /** Each Heads-mod model class's field holding its head part, or empty when it has none. */
    private static final Map<Class<?>, Optional<Field>> HEAD_FIELDS = new HashMap<>();
    // Like the mob layers' model, one instance whose iris is moved before each draw on the render thread.
    private static final ModelGooglyEye MODEL = new ModelGooglyEye();
    private static final float PIXELS_PER_BLOCK = 16F;
    /** Fitted placements by Heads-mod head model and eye attach token; empty where fitting failed. */
    private static final Map<SkullModelBase, Map<String, Optional<Placement>>> PLACEMENTS =
            new IdentityHashMap<>();
    /** A head item's eyes: never simulated, so the pupil stays centered. */
    private static final GooglyTracker.EyeInfo REST = rest();

    /** The placed head being drawn, or {@code null} outside a placed head's draw. */
    @Nullable
    private static SkullBlockEntity current;
    /** The head item being drawn, or {@code null} outside a head item's draw. */
    @Nullable
    private static ItemStack currentItem;
    private static float partialTick;

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

    /** A placed head's draw starts. */
    public static void begin(SkullBlockEntity skull, float partialTick) {
        current = skull;
        SkullEyeRenderer.partialTick = partialTick;
    }

    /** A head item's draw starts; any other item's draw is ignored, since it never reaches {@link #render}. */
    public static void beginItem(ItemStack stack) {
        currentItem = stack;
    }

    /** Drop the fitted placements; called on every renderer rebuild, which replaces the head and mob models. */
    public static void clearCaches() {
        PLACEMENTS.clear();
    }

    /** The placed head's or head item's draw ends. */
    public static void end() {
        current = null;
        currentItem = null;
    }

    /**
     * Fit eyes authored on {@code entity}'s part {@code token} to the Heads-mod head part {@code head}, or
     * {@code null} when either model lacks what the fit needs.
     */
    @Nullable
    private static Placement fit(ModelPart head, ResourceLocation entity, String token) {
        LivingEntity sample = ModelPartVocabulary.sampleFor(entity);
        EntityRenderer<?> renderer = sample == null ? null
                : Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(sample);
        if (!(renderer instanceof LivingEntityRenderer<?, ?> livingRenderer)) {
            return null;
        }
        EntityModel<?> mobModel = livingRenderer.getModel();
        Attachment attachment = Resolvers.forModel(mobModel)
                .resolve(mobModel, CUBE_TOKENS.getOrDefault(entity, token));
        ModelPart mobPart = attachment == null ? null : attachment.part();
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

    /** Whether {@code block} is one of the Heads mod's heads, whose eyes need a {@link Placement}. */
    private static boolean isHeadsMod(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(SkullEyeState.HEADS_NAMESPACE);
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
    private static boolean mayShow(@Nullable ResourceLocation entity) {
        return entity != null && !ClientConfig.DISABLE_GOOGLY_EYES.get() && ClientEyeConfigs.googlyEyesEnabled()
                && !ClientConfig.isEntityDisabled(entity);
    }

    /** The cached fit for eyes on {@code entity}'s part {@code token} drawn by the Heads-mod head {@code model}. */
    @Nullable
    private static Placement placement(SkullModelBase model, ModelPart head, ResourceLocation entity,
                                       String token) {
        return PLACEMENTS.computeIfAbsent(model, m -> new HashMap<>()).computeIfAbsent(token, t -> {
            Placement fitted = fit(head, entity, t);
            if (fitted == null) {
                ClientIntegrationFailures.warnOnce("Heads", "eye placement", entity + " " + t,
                        new IllegalStateException("no cube to fit eyes by"));
            }
            return Optional.ofNullable(fitted);
        }).orElse(null);
    }

    /**
     * Draw the current placed head's or head item's eyes, if it has any to show, with {@code pose} in the space
     * {@code model} was just drawn in and the head's {@code packedLight}.
     */
    public static void render(PoseStack pose, MultiBufferSource bufferSource, int packedLight, SkullModelBase model) {
        ModelPart head = headPart(model);
        if (head == null) {
            return;
        }
        SkullBlockEntity skull = current;
        ItemStack stack = currentItem;
        if (skull != null) {
            Block block = skull.getBlockState().getBlock();
            ResourceLocation entity = SkullEyeState.entityFor(block);
            if (!SkullEyeState.hasEyes(skull) || !mayShow(entity)) {
                return;
            }
            SkullEyes eyes = ClientEyeRuntime.skullEyes(skull, entity);
            if (eyes != null) {
                eyes.markRendered(ClientEyeRuntime.clientTicks());
                renderEyes(pose, bufferSource, packedLight, model, head, isHeadsMod(block), entity, eyes.helper,
                        eyes.parts, eyes.eyes, eyes.snapshot.properties(), partialTick);
            }
        } else if (stack != null && stack.getItem() instanceof BlockItem item) {
            ResourceLocation entity = SkullEyeState.entityFor(stack);
            Snapshot snapshot = SkullEyeState.itemSnapshot(stack);
            if (snapshot == null || !mayShow(entity)) {
                return;
            }
            HeadInfo helper = ClientEyeConfigs.resolveHead(entity, snapshot.variantRoll());
            if (helper.hasConfig()) {
                renderEyes(pose, bufferSource, packedLight, model, head, isHeadsMod(item.getBlock()), entity, helper,
                        SkullEyeState.headParts(entity, helper), null, snapshot.properties(), 0F);
            }
        }
    }

    /**
     * Draw the eyes of {@code helper}'s heads {@code parts} on the model's {@code head} part, each simulated by its
     * entry in {@code eyes}, or at rest when {@code eyes} is {@code null}; a Heads-mod head ({@code headsMod}) moves
     * each part's eyes through its {@link Placement} and skips any part it couldn't fit.
     */
    private static void renderEyes(PoseStack pose, MultiBufferSource bufferSource, int packedLight,
                                   SkullModelBase model, ModelPart head, boolean headsMod, ResourceLocation entity,
                                   HeadInfo helper, int[] parts, @Nullable GooglyTracker.EyeInfo[][] eyes,
                                   AppearanceOverride overrides, float partialTick) {
        pose.pushPose();
        head.translateAndRotate(pose);
        for (int part : parts) {
            Placement placement = null;
            if (headsMod) {
                placement = placement(model, head, entity, helper.getAttachToken(part));
                if (placement == null) {
                    continue;
                }
            }
            pose.pushPose();
            if (placement != null) {
                placement.apply(pose);
            }
            int eyeCount = helper.getEyeCount(part);
            for (int eye = 0; eye < eyeCount; eye++) {
                if (helper.getEyeScale(part, eye) > 0F) {
                    GooglyEyeRenderer.renderEye(pose, MODEL, bufferSource, packedLight, OverlayTexture.NO_OVERLAY,
                            eyes == null ? REST : eyes[part][eye], helper, overrides, part, eye, partialTick);
                }
            }
            pose.popPose();
        }
        pose.popPose();
    }

    private static GooglyTracker.EyeInfo rest() {
        GooglyTracker.EyeInfo rest = new GooglyTracker.EyeInfo();
        rest.deltaY = 0F;
        rest.prevDeltaY = 0F;
        return rest;
    }
}
