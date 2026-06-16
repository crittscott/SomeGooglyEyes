package com.github.crittscott.somegoogly.picker;

import com.github.crittscott.somegoogly.compat.GeckoCompat;
import com.github.crittscott.somegoogly.head.HeadInfo;
import com.github.crittscott.somegoogly.head.HeadInfo.EyeConfig;
import com.github.crittscott.somegoogly.head.HeadInfo.HeadConfig;
import com.github.crittscott.somegoogly.head.HeadInfo.RuntimeConfig;
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
 * In-world eye-placement authoring state (single-player only), driven by the {@code /sg} CLI and the
 * keyboard picker, which share this state. Workflow: choose a mob, pick a part to use as the
 * coordinate frame, shape a <i>current eye</i> (position / rotation / properties), then save it to a
 * flat, numbered <i>eye list</i>. Re-select a saved eye to adjust it in place; export writes the list,
 * grouped by part, to the world datapack.
 *
 * <p>Client-only singleton (static state). Each saved eye remembers its own attach part, so the list
 * can span multiple parts; export regroups by part into heads.
 */
public final class PickerState {

    /** Draft fields the keyboard value keys adjust, in cycle order. */
    public enum Field {
        POS_X("x"), POS_Y("y"), POS_Z("z"),
        EYE_SCALE("eyeScale"), IRIS_SCALE("irisScale"),
        SIDE_OFFSET("side"),
        INCLINATION("inclination"), AZIMUTH("azimuth");

        public final String label;
        Field(String label) { this.label = label; }

        /** Angle fields are in degrees and want the coarser step ladder. */
        public boolean isAngle() { return this == INCLINATION || this == AZIMUTH; }
    }

    // Step ladders cycled by the step key. Angle fields are in degrees and want coarser steps than the
    // linear fields (block units), so they get their own ladder; stepLadder() picks per field.
    private static final float[] LINEAR_STEPS = {0.001f, 0.01f, 0.05f, 0.1f, 1.0f};
    private static final float[] ANGLE_STEPS = {1f, 5f, 15f, 30f, 45f, 90f};

    public static boolean active = false;
    private static WeakReference<LivingEntity> target = new WeakReference<>(null);
    private static EntityModel<?> model;
    private static ResourceLocation targetType;

    public static List<String> parts = new ArrayList<>();
    public static int partIndex = 0;

    /** One saved eye plus the part token it attaches to. */
    public static final class ListedEye {
        public String part;
        public EyeConfig eye;

        public ListedEye(String part, EyeConfig eye) {
            this.part = part;
            this.eye = eye;
        }
    }

    /** The session eye list, in save order (1-based to the user). */
    public static final List<ListedEye> eyes = new ArrayList<>();

    /** The eye being shaped right now (the "current eye"). */
    public static EyeConfig currentEye = defaultEye();
    /** The part token used as the placement frame, or {@code null} for {@code none}. */
    public static String currentPart = null;
    /** Index into {@link #eyes} that {@code save} writes back to, or {@code -1} to append a fresh eye. */
    public static int selectedIndex = -1;

    public static Field field = Field.POS_X;
    public static int stepIndex = 1; // 0.01

    // AI-freeze bookkeeping (single-player only). Restored on unchoose/exit so NoAi doesn't persist.
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

    /** Choose the entity under the crosshair. Returns a status message. */
    public static String lockOn() {
        Minecraft mc = Minecraft.getInstance();
        Entity looked = mc.crosshairPickEntity;
        if (!(looked instanceof LivingEntity living)) {
            return "Look at a mob, then choose.";
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
        boolean keepWork = newType.equals(targetType) && !eyes.isEmpty();

        unfreeze(); // release a previously frozen mob, if any
        target = new WeakReference<>(living);
        model = vanillaModel; // null for GeckoLib targets
        targetType = newType;
        parts = new ArrayList<>(tokens);
        partIndex = 0;
        currentPart = parts.isEmpty() ? null : parts.get(0);
        if (!keepWork) {
            eyes.clear();
            currentEye = defaultEye();
            selectedIndex = -1;
            field = Field.POS_X;
        }
        freeze(living);
        return "Chose " + newType + (keepWork ? " (kept " + committedCount() + " eyes)" : "")
                + " — " + parts.size() + " parts.";
    }

    /** Stop targeting and release the frozen mob; the saved eye list is kept in memory. */
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

    /** The current placement-frame part token (drives the gizmo / draft preview), or {@code null}. */
    public static String selectedToken() {
        return currentPart;
    }

    // ---- part selection --------------------------------------------------------------------

    public static void cyclePart(int dir) {
        if (parts.isEmpty()) {
            return;
        }
        partIndex = Math.floorMod(partIndex + dir, parts.size());
        currentPart = parts.get(partIndex);
    }

    /** The CLI {@code part none} op. */
    public static void clearPart() {
        currentPart = null;
    }

    /** The CLI {@code part <name>} op; false if no such part. */
    public static boolean setPartByName(String token) {
        String want = EyeAttachmentResolver.normalize(token);
        for (int i = 0; i < parts.size(); i++) {
            if (EyeAttachmentResolver.normalize(parts.get(i)).equals(want)) {
                partIndex = i;
                currentPart = parts.get(i);
                return true;
            }
        }
        return false;
    }

    /** The CLI {@code part <number>} op (1-based); false if out of range. */
    public static boolean setPartByNumber(int oneBased) {
        int idx = oneBased - 1;
        if (idx < 0 || idx >= parts.size()) {
            return false;
        }
        partIndex = idx;
        currentPart = parts.get(idx);
        return true;
    }

    private static void syncPartIndex() {
        if (currentPart == null) {
            return;
        }
        String want = EyeAttachmentResolver.normalize(currentPart);
        for (int i = 0; i < parts.size(); i++) {
            if (EyeAttachmentResolver.normalize(parts.get(i)).equals(want)) {
                partIndex = i;
                return;
            }
        }
    }

    // ---- keyboard field editing ------------------------------------------------------------

    public static void cycleField(int dir) {
        Field[] all = Field.values();
        field = all[Math.floorMod(field.ordinal() + dir, all.length)];
    }

    public static void cycleStep(int dir) {
        stepIndex = Math.floorMod(stepIndex + dir, stepLadder().length);
    }

    public static float step() {
        float[] ladder = stepLadder();
        return ladder[Math.floorMod(stepIndex, ladder.length)];
    }

    /** The step ladder for the currently selected field (angle fields use coarser degree steps). */
    private static float[] stepLadder() {
        return field.isAngle() ? ANGLE_STEPS : LINEAR_STEPS;
    }

    public static double fieldValue() {
        return switch (field) {
            case POS_X -> currentEye.position[0];
            case POS_Y -> currentEye.position[1];
            case POS_Z -> currentEye.position[2];
            case EYE_SCALE -> currentEye.eyeScale;
            case IRIS_SCALE -> currentEye.irisScale;
            case SIDE_OFFSET -> currentEye.sideOffset;
            case INCLINATION -> inclination();
            case AZIMUTH -> azimuth();
        };
    }

    public static void adjust(int dir) {
        nudge(field, step() * dir);
    }

    /** Add {@code delta} to a field — the keyboard adjust keys. */
    public static void nudge(Field target, double delta) {
        switch (target) {
            case POS_X -> currentEye.position[0] += delta;
            case POS_Y -> currentEye.position[1] += delta;
            case POS_Z -> currentEye.position[2] += delta;
            case EYE_SCALE -> currentEye.eyeScale = Math.max(0, currentEye.eyeScale + delta);
            case IRIS_SCALE -> currentEye.irisScale = Math.max(0, currentEye.irisScale + delta);
            case SIDE_OFFSET -> currentEye.sideOffset += delta;
            case INCLINATION -> currentEye.inclination = inclination() + delta;
            case AZIMUTH -> currentEye.azimuth = azimuth() + delta;
        }
    }

    private static double inclination() {
        return currentEye.inclination != null ? currentEye.inclination : HeadInfo.DEFAULT_INCLINATION;
    }

    private static double azimuth() {
        return currentEye.azimuth != null ? currentEye.azimuth : HeadInfo.DEFAULT_AZIMUTH;
    }

    // ---- CLI current-eye ops ---------------------------------------------------------------

    /** The CLI {@code create x y z} op: start a fresh current eye at the given position. */
    public static void createEye(double x, double y, double z) {
        currentEye = defaultEye();
        currentEye.position[0] = x;
        currentEye.position[1] = y;
        currentEye.position[2] = z;
        selectedIndex = -1;
    }

    /** The CLI {@code move x y z} op: set absolute position; {@code null} leaves that axis unchanged. */
    public static void setPosition(Double x, Double y, Double z) {
        if (x != null) {
            currentEye.position[0] = x;
        }
        if (y != null) {
            currentEye.position[1] = y;
        }
        if (z != null) {
            currentEye.position[2] = z;
        }
    }

    /** The CLI {@code rot inclination azimuth} op; {@code null} leaves that angle unchanged. */
    public static void setRotation(Double inclination, Double azimuth) {
        if (inclination != null) {
            currentEye.inclination = inclination;
        }
        if (azimuth != null) {
            currentEye.azimuth = azimuth;
        }
    }

    public static void setEyeScale(double v) {
        currentEye.eyeScale = Math.max(0, v);
    }

    public static void setIrisScale(double v) {
        currentEye.irisScale = Math.max(0, v);
    }

    public static void setCorneaColor(double r, double g, double b) {
        currentEye.corneaColors = new double[]{r, g, b};
    }

    public static void setIrisColor(double r, double g, double b) {
        currentEye.irisColors = new double[]{r, g, b};
    }

    public static void setGlow(boolean v) {
        currentEye.glows = v;
    }

    public static void setInvis(boolean v) {
        currentEye.affectedByInvisibility = v;
    }

    public static void toggleGlow() {
        currentEye.glows = !currentEye.glows;
    }

    public static void toggleInvisibility() {
        currentEye.affectedByInvisibility = !currentEye.affectedByInvisibility;
    }

    // ---- eye list (save / select / delete) -------------------------------------------------

    /**
     * Save the current eye: overwrite the selected slot in place, or append a new one if none is
     * selected. Returns false if there is no part to attach to. After an append the new eye becomes
     * selected, so an immediate re-save updates it rather than duplicating it.
     */
    public static boolean save() {
        if (currentPart == null) {
            return false;
        }
        if (selectedIndex >= 0 && selectedIndex < eyes.size()) {
            ListedEye le = eyes.get(selectedIndex);
            le.part = currentPart;
            le.eye = copy(currentEye);
        } else {
            eyes.add(new ListedEye(currentPart, copy(currentEye)));
            selectedIndex = eyes.size() - 1;
        }
        return true;
    }

    /** Save the current eye and its X-mirror as a symmetric pair (keyboard only). */
    public static boolean saveMirror() {
        if (!save()) {
            return false;
        }
        EyeConfig mirror = copy(currentEye);
        mirror.position[0] = -mirror.position[0];
        mirror.sideOffset = -mirror.sideOffset;
        // Reflect the look direction across the X (left/right) plane: inclination unchanged,
        // azimuth -> 180 - azimuth.
        mirror.azimuth = 180.0 - azimuth();
        eyes.add(new ListedEye(currentPart, mirror));
        selectedIndex = eyes.size() - 1;
        return true;
    }

    /** The CLI {@code select <n>} op (1-based): load a saved eye for further adjustment. */
    public static boolean select(int oneBased) {
        int idx = oneBased - 1;
        if (idx < 0 || idx >= eyes.size()) {
            return false;
        }
        ListedEye le = eyes.get(idx);
        currentEye = copy(le.eye);
        currentPart = le.part;
        syncPartIndex();
        selectedIndex = idx;
        return true;
    }

    /** The CLI {@code delete <n>} op (1-based). */
    public static boolean delete(int oneBased) {
        int idx = oneBased - 1;
        if (idx < 0 || idx >= eyes.size()) {
            return false;
        }
        eyes.remove(idx);
        if (selectedIndex == idx) {
            selectedIndex = -1;
        } else if (selectedIndex > idx) {
            selectedIndex--;
        }
        return true;
    }

    /** Remove the last saved eye (keyboard undo). */
    public static void deleteLast() {
        if (!eyes.isEmpty()) {
            delete(eyes.size());
        }
    }

    public static int committedCount() {
        return eyes.size();
    }

    /** Build the selected runtime config from the saved eyes, grouped by part (for export). */
    public static RuntimeConfig toConfig() {
        RuntimeConfig config = new RuntimeConfig();
        config.enabled = true;
        LinkedHashMap<String, HeadConfig> grouped = new LinkedHashMap<>();
        for (ListedEye le : eyes) {
            if (le.part == null) {
                continue;
            }
            HeadConfig head = grouped.computeIfAbsent(le.part, t -> {
                HeadConfig h = new HeadConfig();
                h.attachPoint = t;
                h.eyes = new ArrayList<>();
                return h;
            });
            head.eyes.add(le.eye);
        }
        config.heads = new ArrayList<>(grouped.values());
        return config;
    }

    // ---- helpers ---------------------------------------------------------------------------

    private static EyeConfig defaultEye() {
        EyeConfig e = new EyeConfig();
        e.position = new double[]{-0.13, -0.25, -0.25};
        e.eyeScale = 0.75;
        e.irisScale = 0.6;
        e.sideOffset = 0.0;
        e.inclination = HeadInfo.DEFAULT_INCLINATION;
        e.azimuth = HeadInfo.DEFAULT_AZIMUTH;
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
        e.inclination = s.inclination != null ? s.inclination : HeadInfo.DEFAULT_INCLINATION;
        e.azimuth = s.azimuth != null ? s.azimuth : HeadInfo.DEFAULT_AZIMUTH;
        e.corneaColors = new double[]{s.corneaColors[0], s.corneaColors[1], s.corneaColors[2]};
        e.irisColors = new double[]{s.irisColors[0], s.irisColors[1], s.irisColors[2]};
        e.glows = s.glows;
        e.affectedByInvisibility = s.affectedByInvisibility;
        return e;
    }
}
