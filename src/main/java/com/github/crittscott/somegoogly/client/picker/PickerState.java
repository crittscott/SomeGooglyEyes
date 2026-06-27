package com.github.crittscott.somegoogly.client.picker;

import com.github.crittscott.somegoogly.client.compat.GeckoCompat;
import com.github.crittscott.somegoogly.client.render.resolver.EyeAttachmentResolver;
import com.github.crittscott.somegoogly.client.render.resolver.Resolvers;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.HeadInfo.HeadConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfig;
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
import java.util.Map;

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

    /**
     * Per-entity authored drafts, retained across mob switches so one picker session can author many
     * mobs and {@code exportall} can emit them all. Keyed by entity type; the entry for the chosen mob
     * is the live {@link #variants} list.
     */
    private static final Map<ResourceLocation, List<DraftVariant>> authored = new LinkedHashMap<>();

    /**
     * The placement variants of the mob currently being edited (1-based to the user); always at least
     * one. Reassigned by {@link #lockOn()} to point at the chosen entity's entry in {@link #authored},
     * so saves land directly in that entity's retained draft.
     */
    public static List<DraftVariant> variants = new ArrayList<>(List.of(new DraftVariant()));
    /** Index into {@link #variants} of the variant currently being edited. */
    public static int variantIndex = 0;

    /** The eye being shaped right now (the "current eye"). */
    public static EyeDraft currentEye = defaultEye();
    /** The part token used as the placement frame, or {@code null} for {@code none}. */
    public static String currentPart = null;
    /** Index into the current variant's eye list that {@code save} writes back to, or {@code -1} to append. */
    public static int selectedIndex = -1;

    // AI-freeze bookkeeping (single-player only). A picker-targeted mob is held NoAi=true while edited and
    // restored on unchoose/exit so the forced flag never persists. The pre-picker NoAi value is captured
    // AND read back only on the server thread (inside the freeze/unfreeze tasks below) — never on the client
    // thread — so an unchoose queued right after a choose can't read and restore a stale default before the
    // freeze task has captured the real value. One object per freeze, held by reference; published volatile.
    private static volatile Frozen frozen;

    /** A picker-frozen mob: its id + dimension, and the NoAi value it had before the picker forced it on. */
    private static final class Frozen {
        private final ResourceKey<Level> dim;
        private final int id;
        // Both set on the server thread in freeze(); read on the server thread in unfreeze()/unfreezeOnStop().
        private boolean captured;
        private boolean prevNoAi;

        private Frozen(int id, ResourceKey<Level> dim) {
            this.id = id;
            this.dim = dim;
        }
    }

    private PickerState() {
    }

    /** The CLI {@code part none} op. */
    public static void clearPart() {
        currentPart = null;
    }

    private static EyeDraft copy(EyeDraft s) {
        return s.copy();
    }

    /** The CLI {@code create x y z} op: start a fresh current eye at the given position. */
    public static void createEye(double x, double y, double z) {
        currentEye = defaultEye();
        currentEye.position[0] = x;
        currentEye.position[1] = y;
        currentEye.position[2] = z;
        selectedIndex = -1;
    }

    /** Eyes saved in the current variant. */
    public static int currentEyeCount() {
        return currentEyes().size();
    }

    /** The current variant's eye list — what save/select/delete and the preview operate on. */
    public static List<ListedEye> currentEyes() {
        return currentVariant().eyes;
    }

    /** The variant currently being edited (never null; the list always holds at least one). */
    public static DraftVariant currentVariant() {
        variantIndex = Math.max(0, Math.min(variantIndex, variants.size() - 1));
        return variants.get(variantIndex);
    }

    public static void cyclePart(int dir) {
        if (parts.isEmpty()) {
            return;
        }
        partIndex = Math.floorMod(partIndex + dir, parts.size());
        currentPart = parts.get(partIndex);
    }

    private static EyeDraft defaultEye() {
        return new EyeDraft();
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

    private static void freeze(LivingEntity clientEntity) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return; // can't freeze a mob on a remote server
        }
        Frozen f = new Frozen(clientEntity.getId(), clientEntity.level().dimension());
        frozen = f;
        // Capture the pre-picker NoAi into this same object on the server thread; the matching unfreeze
        // task is queued after this one, so it reads the value this task wrote (never a stale default).
        server.execute(() -> {
            ServerLevel level = server.getLevel(f.dim);
            Entity e = level == null ? null : level.getEntity(f.id);
            if (e instanceof Mob mob) {
                f.prevNoAi = mob.isNoAi();
                f.captured = true;
                mob.setNoAi(true);
                mob.setDeltaMovement(Vec3.ZERO);
            }
        });
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

        unfreeze(); // release a previously frozen mob, if any
        target = new WeakReference<>(living);
        targetType = newType;
        parts = new ArrayList<>(tokens);
        partIndex = 0;
        currentPart = parts.isEmpty() ? null : parts.get(0);
        // Switch to this entity's own draft (an empty one on first sighting), so each mob keeps its saved
        // eyes across switches and exportall can emit them all.
        variants = authored.computeIfAbsent(newType, t -> new ArrayList<>(List.of(new DraftVariant())));
        variantIndex = 0;
        currentEye = defaultEye();
        selectedIndex = -1;
        freeze(living);
        int kept = totalEyeCount();
        return "Chose " + newType + (kept > 0 ? " (kept " + kept + " eyes)" : "")
                + " — " + parts.size() + " parts.";
    }

    /** The CLI {@code variant new} op: append a fresh empty variant and switch to it. Returns its 1-based index. */
    public static int newVariant() {
        variants.add(new DraftVariant());
        variantIndex = variants.size() - 1;
        currentEye = defaultEye();
        selectedIndex = -1;
        return variantIndex + 1;
    }

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

    /** The current placement-frame part token (drives the gizmo / draft preview), or {@code null}. */
    public static String selectedToken() {
        return currentPart;
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

    public static void setCorneaColor(double r, double g, double b) {
        currentEye.corneaColors = new double[]{r, g, b};
    }

    public static void setEyeScale(double v) {
        currentEye.eyeScale = Math.max(0, v);
    }

    public static void setGlow(boolean v) {
        currentEye.glows = v;
    }

    public static void setInvis(boolean v) {
        currentEye.affectedByInvisibility = v;
    }

    public static void setIrisColor(double r, double g, double b) {
        currentEye.irisColors = new double[]{r, g, b};
    }

    public static void setIrisScale(double v) {
        currentEye.irisScale = Math.max(0, v);
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

    /** The CLI {@code variant weight <w>} op: set the current variant's relative weight (clamped >= 0). */
    public static void setVariantWeight(double w) {
        currentVariant().weight = Math.max(0, w);
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

    public static LivingEntity target() {
        return target.get();
    }

    public static ResourceLocation targetType() {
        return targetType;
    }

    /** Build the runtime config for the mob being edited, grouped by part into heads (for export). */
    public static RuntimeConfig toConfig() {
        return toConfig(variants);
    }

    /**
     * Every authored entity's draft as a runtime config, keyed by type, skipping mobs that were chosen
     * but have no saved eyes. Lets {@code exportall} emit mobs that were saved but never individually
     * exported.
     */
    public static Map<ResourceLocation, RuntimeConfig> authoredConfigs() {
        Map<ResourceLocation, RuntimeConfig> out = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, List<DraftVariant>> entry : authored.entrySet()) {
            RuntimeConfig config = toConfig(entry.getValue());
            if (!config.variants.isEmpty()) {
                out.put(entry.getKey(), config);
            }
        }
        return out;
    }

    /** Group a draft's variants by part into heads, dropping any variant that ended up with no eyes. */
    private static RuntimeConfig toConfig(List<DraftVariant> draftVariants) {
        RuntimeConfig config = new RuntimeConfig();
        config.enabled = true;
        config.variants = new ArrayList<>();
        for (DraftVariant dv : draftVariants) {
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

    /** Eyes saved across all variants (the export guard). */
    public static int totalEyeCount() {
        int n = 0;
        for (DraftVariant v : variants) {
            n += v.eyes.size();
        }
        return n;
    }

    private static void unfreeze() {
        Frozen f = frozen;
        if (f == null) {
            return;
        }
        frozen = null;
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return;
        }
        // Restore on the server thread, reading prevNoAi from the same object the freeze task wrote. The
        // freeze task was queued first, so by the time this runs the capture has happened.
        server.execute(() -> {
            ServerLevel level = server.getLevel(f.dim);
            Entity e = level == null ? null : level.getEntity(f.id);
            if (e instanceof Mob mob && f.captured) {
                mob.setNoAi(f.prevNoAi);
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
        Frozen f = frozen;
        if (f == null || server == null) {
            return;
        }
        frozen = null;
        // Only restore if the freeze task actually ran and forced NoAi; if it never captured, the mob was
        // never frozen, so there is nothing to undo (and prevNoAi would be a meaningless default).
        if (!f.captured) {
            return;
        }
        ServerLevel level = server.getLevel(f.dim);
        Entity e = level == null ? null : level.getEntity(f.id);
        if (e instanceof Mob mob) {
            mob.setNoAi(f.prevNoAi);
        }
    }

    /** Stop targeting and release the frozen mob; the saved eye list is kept in memory. */
    public static void unlock() {
        unfreeze();
        target = new WeakReference<>(null);
    }

    public static int variantCount() {
        return variants.size();
    }
}
