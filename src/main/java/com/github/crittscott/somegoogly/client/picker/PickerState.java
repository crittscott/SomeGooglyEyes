package com.github.crittscott.somegoogly.client.picker;

import com.github.crittscott.somegoogly.client.compat.GeckoCompat;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.HeadInfo.HeadConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfig;
import com.github.crittscott.somegoogly.client.render.resolver.EyeAttachmentResolver;
import com.github.crittscott.somegoogly.client.render.resolver.Resolvers;
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

    public static boolean active = false;
    private static WeakReference<LivingEntity> target = new WeakReference<>(null);
    private static ResourceLocation targetType;

    public static List<String> parts = new ArrayList<>();
    public static int partIndex = 0;

    /** One saved eye plus the part token it attaches to. */
    public static final class ListedEye {
        public String part;
        public EyeDraft eye;

        public ListedEye(String part, EyeDraft eye) {
            this.part = part;
            this.eye = eye;
        }
    }

    /** One weighted placement arrangement being authored: its relative weight plus its own eye list. */
    public static final class DraftVariant {
        public double weight = 1.0;
        public final List<ListedEye> eyes = new ArrayList<>();
    }

    /** The session's placement variants, in order (1-based to the user); always at least one. */
    public static final List<DraftVariant> variants = new ArrayList<>(List.of(new DraftVariant()));
    /** Index into {@link #variants} of the variant currently being edited. */
    public static int variantIndex = 0;

    /** The eye being shaped right now (the "current eye"). */
    public static EyeDraft currentEye = defaultEye();
    /** The part token used as the placement frame, or {@code null} for {@code none}. */
    public static String currentPart = null;
    /** Index into the current variant's eye list that {@code save} writes back to, or {@code -1} to append. */
    public static int selectedIndex = -1;

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
        List<String> tokens = List.of();
        if (renderer instanceof LivingEntityRenderer<?, ?> ler) {
            EntityModel<?> vanillaModel = ler.getModel();
            EyeAttachmentResolver resolver = Resolvers.forModel(vanillaModel);
            if (resolver != null) {
                tokens = resolver.enumerateParts(vanillaModel);
            }
        }
        // GeckoLib path (named bones), if vanilla found nothing.
        if (tokens.isEmpty()) {
            List<String> bones = GeckoCompat.enumerate(renderer, living);
            if (!bones.isEmpty()) {
                tokens = bones;
            }
        }
        if (tokens.isEmpty()) {
            return "Unsupported model — no reachable vanilla parts or GeckoLib bones.";
        }

        ResourceLocation newType = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        boolean keepWork = newType.equals(targetType) && totalEyeCount() > 0;

        unfreeze(); // release a previously frozen mob, if any
        target = new WeakReference<>(living);
        targetType = newType;
        parts = new ArrayList<>(tokens);
        partIndex = 0;
        currentPart = parts.isEmpty() ? null : parts.get(0);
        if (!keepWork) {
            resetWork();
        }
        freeze(living);
        return "Chose " + newType + (keepWork ? " (kept " + totalEyeCount() + " eyes)" : "")
                + " — " + parts.size() + " parts.";
    }

    /** Stop targeting and release the frozen mob; the saved eye list is kept in memory. */
    public static void unlock() {
        unfreeze();
        target = new WeakReference<>(null);
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

    /**
     * Restore a frozen mob's previous NoAi value <b>synchronously on the server thread</b>. Called at
     * server stop (which fires before the final world save), so the picker's forced NoAi is never
     * written to disk. Unlike {@link #unfreeze()} this runs inline rather than via the server task
     * queue, which may no longer drain during shutdown.
     *
     * <p>Does not cover an autosave mid-edit followed by a hard crash (the forced NoAi would persist
     * until the next clean load); that window is intentionally left, since the picker is a
     * single-player authoring tool.
     */
    public static void unfreezeOnStop(MinecraftServer server) {
        if (frozenId < 0 || frozenDim == null || server == null) {
            return;
        }
        ServerLevel level = server.getLevel(frozenDim);
        Entity e = level == null ? null : level.getEntity(frozenId);
        if (e instanceof Mob mob) {
            mob.setNoAi(frozenPrevNoAi);
        }
        frozenId = -1;
        frozenDim = null;
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

    // ---- variants --------------------------------------------------------------------------

    /** The variant currently being edited (never null; the list always holds at least one). */
    public static DraftVariant currentVariant() {
        variantIndex = Math.max(0, Math.min(variantIndex, variants.size() - 1));
        return variants.get(variantIndex);
    }

    /** The current variant's eye list — what save/select/delete and the preview operate on. */
    public static List<ListedEye> currentEyes() {
        return currentVariant().eyes;
    }

    public static int variantCount() {
        return variants.size();
    }

    /** Eyes saved in the current variant. */
    public static int currentEyeCount() {
        return currentEyes().size();
    }

    /** Eyes saved across all variants (the export guard). */
    public static int totalEyeCount() {
        int n = 0;
        for (DraftVariant v : variants) {
            n += v.eyes.size();
        }
        return n;
    }

    /** Reset the whole eye list to a single empty variant. Used on (re)choose and unchoose. */
    public static void resetWork() {
        variants.clear();
        variants.add(new DraftVariant());
        variantIndex = 0;
        currentEye = defaultEye();
        currentPart = parts.isEmpty() ? null : parts.get(partIndex);
        selectedIndex = -1;
    }

    /** The CLI {@code variant new} op: append a fresh empty variant and switch to it. Returns its 1-based index. */
    public static int newVariant() {
        variants.add(new DraftVariant());
        variantIndex = variants.size() - 1;
        currentEye = defaultEye();
        selectedIndex = -1;
        return variantIndex + 1;
    }

    /** The CLI {@code variant <n>} op (1-based): switch to a variant for editing; false if out of range. */
    public static boolean selectVariant(int oneBased) {
        int idx = oneBased - 1;
        if (idx < 0 || idx >= variants.size()) {
            return false;
        }
        variantIndex = idx;
        currentEye = defaultEye();
        selectedIndex = -1;
        return true;
    }

    /**
     * The CLI {@code variant del <n>} op (1-based). Refuses to remove the last variant (there is always
     * at least one). Returns false if out of range or it would empty the list.
     */
    public static boolean deleteVariant(int oneBased) {
        int idx = oneBased - 1;
        if (idx < 0 || idx >= variants.size() || variants.size() <= 1) {
            return false;
        }
        variants.remove(idx);
        if (variantIndex >= idx) {
            variantIndex = Math.max(0, variantIndex - 1);
        }
        currentEye = defaultEye();
        selectedIndex = -1;
        return true;
    }

    /** The CLI {@code variant weight <w>} op: set the current variant's relative weight (clamped >= 0). */
    public static void setVariantWeight(double w) {
        currentVariant().weight = Math.max(0, w);
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
        List<ListedEye> eyes = currentEyes();
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

    /** The CLI {@code select <n>} op (1-based): load a saved eye from the current variant for adjustment. */
    public static boolean select(int oneBased) {
        List<ListedEye> eyes = currentEyes();
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

    /** The CLI {@code delete <n>} op (1-based): remove an eye from the current variant. */
    public static boolean delete(int oneBased) {
        List<ListedEye> eyes = currentEyes();
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

    /** Build the runtime config from all authored variants, each grouped by part into heads (for export). */
    public static RuntimeConfig toConfig() {
        RuntimeConfig config = new RuntimeConfig();
        config.enabled = true;
        config.variants = new ArrayList<>();
        for (DraftVariant dv : variants) {
            LinkedHashMap<String, HeadConfig> grouped = new LinkedHashMap<>();
            for (ListedEye le : dv.eyes) {
                if (le.part == null) {
                    continue;
                }
                HeadConfig head = grouped.computeIfAbsent(le.part, t -> {
                    HeadConfig h = new HeadConfig();
                    h.attachPoint = t;
                    h.eyes = new ArrayList<>();
                    return h;
                });
                head.eyes.add(le.eye.toDefinition());
            }
            if (grouped.isEmpty()) {
                continue; // skip empty arrangements rather than export a variant with no eyes
            }
            HeadInfo.Variant variant = new HeadInfo.Variant();
            variant.weight = dv.weight;
            variant.heads = new ArrayList<>(grouped.values());
            config.variants.add(variant);
        }
        return config;
    }

    // ---- helpers ---------------------------------------------------------------------------

    private static EyeDraft defaultEye() {
        return new EyeDraft();
    }

    private static EyeDraft copy(EyeDraft s) {
        return s.copy();
    }
}
