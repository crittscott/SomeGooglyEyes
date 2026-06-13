package com.github.crittscott.somegoogly.picker;

import com.github.crittscott.somegoogly.compat.GeckoCompat;
import com.github.crittscott.somegoogly.head.HeadInfo.EntityConfig;
import com.github.crittscott.somegoogly.head.HeadInfo.EyeConfig;
import com.github.crittscott.somegoogly.head.HeadInfo.HeadConfig;
import com.github.crittscott.somegoogly.render.resolver.EyeAttachmentResolver;
import com.github.crittscott.somegoogly.render.resolver.Resolvers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * In-world part-picker authoring state (single-player only). Workflow: lock onto a hierarchical mob,
 * cycle its named parts, fiddle a <i>draft</i> eye on the selected part, then commit it — which
 * appends it to the head for that part (creating the head if needed). Commit on a different part to
 * build a multi-head config. Export writes the accumulated config to the world datapack.
 *
 * <p>Client-only singleton (static state). Append-only for v1: to redo a committed eye, remove the
 * last one and re-commit.
 */
public final class PickerState {

    /** Draft fields that the value keys adjust, in cycle order. */
    public enum Field {
        POS_X("posX"), POS_Y("posY"), POS_Z("posZ"),
        EYE_SCALE("eyeScale"), IRIS_SCALE("irisScale"),
        SIDE_OFFSET("sideOffset"), Y_ROT("yRotation"), X_ROT("xRotation");

        public final String label;
        Field(String label) { this.label = label; }
    }

    private static final float[] STEPS = {0.001f, 0.01f, 0.05f, 0.1f, 1.0f};

    public static boolean active = false;
    private static WeakReference<LivingEntity> target = new WeakReference<>(null);
    private static EntityModel<?> model;
    private static ResourceLocation targetType;

    public static List<String> parts = new ArrayList<>();
    public static int partIndex = 0;

    /** Committed heads keyed by part token, insertion-ordered. */
    public static final LinkedHashMap<String, HeadConfig> heads = new LinkedHashMap<>();
    /** Insertion order of commits, for remove-last. */
    private static final List<String> commitOrder = new ArrayList<>();

    public static EyeConfig draft = defaultEye();
    public static Field field = Field.POS_X;
    public static int stepIndex = 1; // 0.01

    // AI-freeze bookkeeping (single-player only). Restored on unlock/exit so NoAi doesn't persist.
    private static int frozenId = -1;
    private static ResourceKey<Level> frozenDim;
    private static boolean frozenPrevNoAi;

    private PickerState() {
    }

    // ---- target / lifecycle ----------------------------------------------------------------

    public static LivingEntity target() {
        return target.get();
    }

    public static EntityModel<?> model() {
        return model;
    }

    public static boolean isActiveTarget(LivingEntity entity) {
        return active && entity == target.get();
    }

    /** Lock onto the entity under the crosshair. Returns a status message. */
    public static String lockOn() {
        Minecraft mc = Minecraft.getInstance();
        Entity looked = mc.crosshairPickEntity;
        if (!(looked instanceof LivingEntity living)) {
            return "Look at a mob, then lock on.";
        }
        EntityRenderer<?> renderer = mc.getEntityRenderDispatcher().getRenderer(living);

        // Vanilla EntityModel path (hierarchical names or reflection #N).
        EntityModel<?> vanillaModel = null;
        List<String> tokens = List.of();
        if (renderer instanceof LivingEntityRenderer<?, ?> ler) {
            vanillaModel = ler.getModel();
            EyeAttachmentResolver resolver = Resolvers.forModel(vanillaModel);
            if (resolver != null) {
                tokens = resolver.enumerateParts(vanillaModel);
            }
        }
        // GeckoLib path (named bones), if vanilla found nothing.
        if (tokens.isEmpty()) {
            List<String> bones = GeckoCompat.enumerate(renderer, living);
            if (!bones.isEmpty()) {
                vanillaModel = null;
                tokens = bones;
            }
        }
        if (tokens.isEmpty()) {
            return "Unsupported model — no reachable vanilla parts or GeckoLib bones.";
        }

        ResourceLocation newType = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        boolean keepWork = newType.equals(targetType) && !heads.isEmpty();

        unfreeze(); // release a previously frozen mob, if any
        target = new WeakReference<>(living);
        model = vanillaModel; // null for GeckoLib targets
        targetType = newType;
        parts = new ArrayList<>(tokens);
        partIndex = 0;
        if (!keepWork) {
            heads.clear();
            commitOrder.clear();
            draft = defaultEye();
            field = Field.POS_X;
        }
        freeze(living);
        return "Locked on " + newType + (keepWork ? " (kept " + committedCount() + " eyes)" : "")
                + " — " + parts.size() + " parts.";
    }

    /** Stop targeting and release the frozen mob; committed work is kept in memory. */
    public static void unlock() {
        unfreeze();
        target = new WeakReference<>(null);
        model = null;
    }

    private static void freeze(LivingEntity clientEntity) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return; // can't freeze a mob on a remote server
        }
        frozenId = clientEntity.getId();
        frozenDim = clientEntity.level().dimension();
        final int id = frozenId;
        final ResourceKey<Level> dim = frozenDim;
        server.execute(() -> {
            ServerLevel level = server.getLevel(dim);
            Entity e = level == null ? null : level.getEntity(id);
            if (e instanceof Mob mob) {
                frozenPrevNoAi = mob.isNoAi();
                mob.setNoAi(true);
                mob.setDeltaMovement(Vec3.ZERO);
            }
        });
    }

    private static void unfreeze() {
        if (frozenId < 0 || frozenDim == null) {
            return;
        }
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        final int id = frozenId;
        final ResourceKey<Level> dim = frozenDim;
        final boolean prev = frozenPrevNoAi;
        frozenId = -1;
        frozenDim = null;
        if (server == null) {
            return;
        }
        server.execute(() -> {
            ServerLevel level = server.getLevel(dim);
            Entity e = level == null ? null : level.getEntity(id);
            if (e instanceof Mob mob) {
                mob.setNoAi(prev);
            }
        });
    }

    public static ResourceLocation targetType() {
        return targetType;
    }

    public static String selectedToken() {
        return parts.isEmpty() ? null : parts.get(Math.floorMod(partIndex, parts.size()));
    }

    // ---- editing ---------------------------------------------------------------------------

    public static void cyclePart(int dir) {
        if (!parts.isEmpty()) {
            partIndex = Math.floorMod(partIndex + dir, parts.size());
        }
    }

    public static void cycleField(int dir) {
        Field[] all = Field.values();
        field = all[Math.floorMod(field.ordinal() + dir, all.length)];
    }

    public static void cycleStep(int dir) {
        stepIndex = Math.floorMod(stepIndex + dir, STEPS.length);
    }

    public static float step() {
        return STEPS[stepIndex];
    }

    public static double fieldValue() {
        return switch (field) {
            case POS_X -> draft.position[0];
            case POS_Y -> draft.position[1];
            case POS_Z -> draft.position[2];
            case EYE_SCALE -> draft.eyeScale;
            case IRIS_SCALE -> draft.irisScale;
            case SIDE_OFFSET -> draft.sideOffset;
            case Y_ROT -> draft.yRotation;
            case X_ROT -> draft.xRotation;
        };
    }

    public static void adjust(int dir) {
        float d = step() * dir;
        switch (field) {
            case POS_X -> draft.position[0] += d;
            case POS_Y -> draft.position[1] += d;
            case POS_Z -> draft.position[2] += d;
            case EYE_SCALE -> draft.eyeScale = Math.max(0, draft.eyeScale + d);
            case IRIS_SCALE -> draft.irisScale = Math.max(0, draft.irisScale + d);
            case SIDE_OFFSET -> draft.sideOffset += d;
            case Y_ROT -> draft.yRotation += d;
            case X_ROT -> draft.xRotation += d;
        }
    }

    public static void toggleGlow() {
        draft.glows = !draft.glows;
    }

    public static void toggleInvisibility() {
        draft.affectedByInvisibility = !draft.affectedByInvisibility;
    }

    // ---- commit / remove -------------------------------------------------------------------

    public static void commit() {
        commitEye(copy(draft));
    }

    /** Commit the draft and its X-mirror in one go (symmetric pair). */
    public static void commitMirror() {
        commitEye(copy(draft));
        EyeConfig mirror = copy(draft);
        mirror.position[0] = -mirror.position[0];
        mirror.sideOffset = -mirror.sideOffset;
        commitEye(mirror);
    }

    private static void commitEye(EyeConfig eye) {
        String token = selectedToken();
        if (token == null) {
            return;
        }
        HeadConfig head = heads.computeIfAbsent(token, t -> {
            HeadConfig h = new HeadConfig();
            h.attachPoint = t;
            h.eyes = new ArrayList<>();
            return h;
        });
        head.eyes.add(eye);
        commitOrder.add(token);
    }

    public static void removeLast() {
        if (commitOrder.isEmpty()) {
            return;
        }
        String token = commitOrder.remove(commitOrder.size() - 1);
        HeadConfig head = heads.get(token);
        if (head != null && head.eyes != null && !head.eyes.isEmpty()) {
            head.eyes.remove(head.eyes.size() - 1);
            if (head.eyes.isEmpty()) {
                heads.remove(token);
            }
        }
    }

    public static int committedCount() {
        return commitOrder.size();
    }

    /** Build the config from the committed heads (for export). */
    public static EntityConfig toConfig() {
        EntityConfig config = new EntityConfig();
        config.enabled = true;
        config.heads = new ArrayList<>(heads.values());
        return config;
    }

    // ---- helpers ---------------------------------------------------------------------------

    private static EyeConfig defaultEye() {
        EyeConfig e = new EyeConfig();
        e.position = new double[]{-0.13, -0.25, -0.25};
        e.eyeScale = 0.75;
        e.irisScale = 0.6;
        e.sideOffset = 0.0;
        e.yRotation = 0.0;
        e.xRotation = 0.0;
        e.corneaColors = new double[]{1.0, 1.0, 1.0};
        e.irisColors = new double[]{0.0, 0.0, 0.0};
        e.glows = false;
        e.affectedByInvisibility = true;
        return e;
    }

    private static EyeConfig copy(EyeConfig s) {
        EyeConfig e = new EyeConfig();
        e.position = new double[]{s.position[0], s.position[1], s.position[2]};
        e.eyeScale = s.eyeScale;
        e.irisScale = s.irisScale;
        e.sideOffset = s.sideOffset;
        e.yRotation = s.yRotation;
        e.xRotation = s.xRotation;
        e.corneaColors = new double[]{s.corneaColors[0], s.corneaColors[1], s.corneaColors[2]};
        e.irisColors = new double[]{s.irisColors[0], s.irisColors[1], s.irisColors[2]};
        e.glows = s.glows;
        e.affectedByInvisibility = s.affectedByInvisibility;
        return e;
    }
}
