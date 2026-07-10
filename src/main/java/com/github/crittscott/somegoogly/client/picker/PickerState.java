package com.github.crittscott.somegoogly.client.picker;

import com.github.crittscott.somegoogly.client.compat.GeckoCompat;
import com.github.crittscott.somegoogly.client.render.resolver.EyeAttachmentResolver;
import com.github.crittscott.somegoogly.client.render.resolver.Resolvers;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.EyeDefinition;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.HeadInfo.HeadConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.Variant;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-world eye-placement authoring state, driven by the {@code /sg} CLI and the keyboard picker,
 * which share this state. Creative-mode only; works in single-player and from a remote client alike —
 * the operations that touch the server (mob freezing, export) go through the C2S picker packets and
 * are re-authorized server-side. Workflow: choose a mob, pick a part to use as the coordinate frame,
 * shape a <i>current eye</i> (position / rotation / properties), then save it to a flat, numbered
 * <i>eye list</i>. Re-select a saved eye to adjust it in place; export sends the list, grouped by
 * part, to the server to be written into the world datapack.
 *
 * <p>Client-only singleton (static state). Each saved eye remembers its own attach part, so the list
 * can span multiple parts; export regroups by part into heads.
 */
public final class PickerState {

    private static boolean active = false;
    private static WeakReference<LivingEntity> target = new WeakReference<>(null);
    private static ResourceLocation targetType;

    private static List<String> parts = new ArrayList<>();
    private static int partIndex = 0;

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
    private static List<DraftVariant> variants = new ArrayList<>(List.of(new DraftVariant()));
    /** Index into {@link #variants} of the variant currently being edited. */
    private static int variantIndex = 0;

    /**
     * The eye being shaped right now, or {@code null} when no draft is being authored — the empty state.
     * A never-configured mob (and a variant whose eyes are all deleted) sits here with no draft, so the
     * preview/HUD show nothing until {@code /sg create} (or {@code select}) starts one. See {@link #clearDraft()}.
     */
    private static EyeDraft currentEye = null;
    /** The part token used as the placement frame, or {@code null} for {@code none}. */
    private static String currentPart = null;
    /** Index into the current variant's eye list that {@code save} writes back to, or {@code -1} to append. */
    private static int selectedIndex = -1;

    private PickerState() {
    }

    // --- read-only views for the HUD, preview layers, and CLI feedback -----------------------------

    /** Whether the picker is on (preview layers, HUD, and keybinds all key off this). */
    public static boolean isActive() {
        return active;
    }

    /** The chosen mob's selectable part tokens, in enumeration order. Read-only. */
    public static List<String> parts() {
        return parts;
    }

    /** Index into {@link #parts()} of the current placement-frame part. */
    public static int partIndex() {
        return partIndex;
    }

    /** The variants being authored for the chosen mob. Read-only; mutate via the variant ops. */
    public static List<DraftVariant> variants() {
        return variants;
    }

    /** Index into {@link #variants()} of the variant being edited. */
    public static int variantIndex() {
        return variantIndex;
    }

    /** The live draft eye, or {@code null} in the empty state. Mutate via the {@code set*} ops. */
    public static EyeDraft currentEye() {
        return currentEye;
    }

    /** The part token used as the placement frame, or {@code null} for {@code none}. */
    public static String currentPart() {
        return currentPart;
    }

    /** Index of the saved eye being edited ({@code -1} = the draft is new/unsaved). */
    public static int selectedIndex() {
        return selectedIndex;
    }

    // --- lifecycle ---------------------------------------------------------------------------------

    /** Turn the picker on (so the preview/gizmo render); choosing a mob is a separate step. */
    public static void activate() {
        active = true;
    }

    /** Turn the picker off and release the frozen mob; the authored drafts stay in memory. */
    public static void deactivate() {
        active = false;
        unlock();
    }

    /** The CLI {@code part none} op. */
    public static void clearPart() {
        currentPart = null;
    }

    /**
     * Drop the live draft, returning to the empty authoring state (no current eye, nothing selected).
     * The preview/HUD then show only the saved eyes, so a variant with zero saved eyes looks empty
     * rather than sprouting a phantom unsaved eye. Authoring resumes on {@code create}/{@code select}.
     */
    public static void clearDraft() {
        currentEye = null;
        selectedIndex = -1;
    }

    /** Whether a live draft eye is currently being shaped (vs the empty state). */
    public static boolean hasDraft() {
        return currentEye != null;
    }

    /** The CLI {@code create x y z} op: start a fresh current eye at the given position. */
    public static void createEye(float x, float y, float z) {
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
            clearDraft(); // the edited eye is gone; return to no-draft rather than a phantom
        } else if (selectedIndex > idx) {
            selectedIndex--;
        }
        if (eyes.isEmpty()) {
            clearDraft(); // zero eyes is a real state — no auto-vivified draft
        }
        return true;
    }

    /**
     * The CLI {@code dupe <n>} op (1-based): copy a saved eye into a fresh unsaved draft — like
     * {@code select}, but nothing is selected, so the next {@code save} appends instead of
     * overwriting. Adopts the source eye's part; replaces any existing unsaved draft.
     */
    public static boolean dupe(int oneBased) {
        List<ListedEye> eyes = currentEyes();
        int idx = oneBased - 1;
        if (idx < 0 || idx >= eyes.size()) {
            return false;
        }
        ListedEye le = eyes.get(idx);
        currentEye = le.eye.copy();
        currentPart = le.part;
        syncPartIndex();
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
        clearDraft();
        return true;
    }

    /**
     * Ask the server to hold the mob still while it's edited. The freeze itself — NoAi capture,
     * restore, crash-proofing — is server-owned ({@code PickerFreezeService}, reached via
     * {@link PickerFreezePacket}), so it works from a remote client and is undone even if this
     * client disappears; the client only requests.
     */
    private static void freeze(LivingEntity clientEntity) {
        send(PickerFreezePacket.freeze(clientEntity.getUUID()));
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

        // Vanilla EntityModel path (hierarchical names or reflection #N). Hold onto the model + resolver
        // so a first-sighting draft can be seeded from the mob's existing config in this same vocabulary.
        List<String> tokens = List.of();
        EntityModel<?> seedModel = null;
        EyeAttachmentResolver seedResolver = null;
        if (renderer instanceof LivingEntityRenderer<?, ?> ler) {
            EntityModel<?> vanillaModel = ler.getModel();
            EyeAttachmentResolver resolver = Resolvers.forModel(vanillaModel);
            if (resolver != null) {
                tokens = resolver.enumerateParts(vanillaModel);
                seedModel = vanillaModel;
                seedResolver = resolver;
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
        currentPart = parts.get(0); // tokens is non-empty (checked above)
        // Switch to this entity's own draft, so each mob keeps its saved eyes across switches and exportall
        // can emit them all. On first sighting, seed from the mob's existing (synced) config so editing an
        // existing placement starts from it rather than a blank slate; a never-configured mob gets an empty
        // draft. The retained draft (computeIfAbsent) wins on re-choose, so in-session edits aren't lost.
        boolean baby = living.isBaby();
        EntityModel<?> model = seedModel;
        EyeAttachmentResolver resolver = seedResolver;
        variants = authored.computeIfAbsent(newType, t -> seedDraft(t, baby, model, resolver));
        variantIndex = 0;
        clearDraft(); // start empty: saved eyes (if any) show, but no phantom draft until create/select
        freeze(living);
        int kept = totalEyeCount();
        return "Chose " + newType + (kept > 0 ? " (" + kept + " eyes)" : "")
                + " — " + parts.size() + " parts.";
    }

    /**
     * Build a fresh draft for a mob being chosen for the first time this session. Seeds from its synced
     * config for the given age ({@link ClientEyeConfigs}) so editing an existing placement starts from it;
     * returns a single empty variant when the mob has no usable config. Attach tokens are canonicalized to
     * the model's enumeration vocabulary (e.g. a bare {@code "head"} snaps to its full path {@code "root/head"}),
     * so the seeded draft, HUD, and export all speak the same token the part picker shows.
     */
    private static List<DraftVariant> seedDraft(ResourceLocation type, boolean baby,
                                                EntityModel<?> model, EyeAttachmentResolver resolver) {
        RuntimeConfig config = ClientEyeConfigs.get(type, baby);
        List<DraftVariant> seeded = new ArrayList<>();
        if (config != null && config.enabled) {
            for (Variant v : config.variants) {
                DraftVariant dv = new DraftVariant();
                dv.weight = v.weight();
                for (HeadConfig head : v.heads) {
                    String raw = head.attachPoint;
                    String part = model != null && resolver != null
                            ? resolver.canonicalToken(model, raw) : raw;
                    int headStart = dv.eyes.size();
                    for (EyeDefinition def : head.eyes) {
                        dv.eyes.add(new ListedEye(part, EyeDraft.fromDefinition(def)));
                    }
                    // Translate each eye's on-disk within-head cross-target back to a flat draft index.
                    for (int e = 0; e < head.eyes.size(); e++) {
                        int within = head.eyes.get(e).placement().crossTarget();
                        if (within >= 0 && within < head.eyes.size()) {
                            dv.eyes.get(headStart + e).eye.crossTarget = headStart + within;
                        }
                    }
                }
                seeded.add(dv);
            }
        }
        if (seeded.isEmpty()) {
            seeded.add(new DraftVariant());
        }
        return seeded;
    }

    /** The CLI {@code variant new} op: append a fresh empty variant and switch to it. Returns its 1-based index. */
    public static int newVariant() {
        variants.add(new DraftVariant());
        variantIndex = variants.size() - 1;
        clearDraft();
        return variantIndex + 1;
    }

    /**
     * Save the current eye: overwrite the selected slot in place, or append a new one if none is
     * selected. Returns false if there is no part to attach to. After an append the new eye becomes
     * selected, so an immediate re-save updates it rather than duplicating it.
     */
    public static boolean save() {
        if (currentPart == null || currentEye == null) {
            return false;
        }
        List<ListedEye> eyes = currentEyes();
        if (selectedIndex >= 0 && selectedIndex < eyes.size()) {
            ListedEye le = eyes.get(selectedIndex);
            le.part = currentPart;
            le.eye = currentEye.copy();
        } else {
            eyes.add(new ListedEye(currentPart, currentEye.copy()));
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
        currentEye = le.eye.copy();
        currentPart = le.part;
        syncPartIndex();
        selectedIndex = idx;
        return true;
    }

    /** The CLI {@code variant <n>} op (1-based): switch to a variant for editing; false if out of range. */
    public static boolean selectVariant(int oneBased) {
        int idx = oneBased - 1;
        if (idx < 0 || idx >= variants.size()) {
            return false;
        }
        variantIndex = idx;
        clearDraft();
        return true;
    }

    public static void setCorneaColor(float r, float g, float b) {
        currentEye.corneaColors = new float[]{r, g, b};
    }

    public static void setEyeScale(float v) {
        currentEye.eyeScale = Math.max(0, v);
    }

    /** Thickness multiplier along the look axis (1 = standard; clamped >= 0). */
    public static void setDepth(float v) {
        currentEye.depth = Math.max(0, v);
    }

    /**
     * The CLI {@code properties crosstarget <n>} op: point the current eye's cross-eye behavior at the eye
     * at 1-based list index {@code oneBased} ({@code <= 0} clears it). The target must be a <i>different</i>
     * eye that shares the current part — cross-eye is same-head only. Returns false if out of range, the
     * edited eye itself, or on a different part.
     */
    public static boolean setCrossTarget(int oneBased) {
        if (currentEye == null) {
            return false;
        }
        if (oneBased <= 0) {
            currentEye.crossTarget = -1;
            return true;
        }
        int idx = oneBased - 1;
        List<ListedEye> eyes = currentEyes();
        if (idx < 0 || idx >= eyes.size() || idx == selectedIndex) {
            return false;
        }
        if (currentPart == null || !currentPart.equals(eyes.get(idx).part)) {
            return false;
        }
        currentEye.crossTarget = idx;
        return true;
    }

    public static void setGlow(boolean v) {
        currentEye.glows = v;
    }

    public static void setIrisColor(float r, float g, float b) {
        currentEye.irisColors = new float[]{r, g, b};
    }

    public static void setIrisScale(float v) {
        currentEye.irisScale = Math.max(0, v);
    }

    /**
     * The CLI {@code part <name>} op; false if no such part. The typed token is suffix-matched against the
     * enumerated path tokens, so a bare leaf (e.g. {@code head}) selects {@code root/body/head}; the
     * selected {@code currentPart} is the full canonical path it matched.
     */
    public static boolean setPartByName(String token) {
        for (int i = 0; i < parts.size(); i++) {
            if (EyeAttachmentResolver.pathMatches(token, parts.get(i))) {
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

    /** The CLI {@code move x y z} op: set absolute position; an empty value leaves that axis unchanged. */
    public static void setPosition(Optional<Float> x, Optional<Float> y, Optional<Float> z) {
        x.ifPresent(v -> currentEye.position[0] = v);
        y.ifPresent(v -> currentEye.position[1] = v);
        z.ifPresent(v -> currentEye.position[2] = v);
    }

    /** The CLI {@code rot inclination azimuth} op; an empty value leaves that angle unchanged. */
    public static void setRotation(Optional<Float> inclination, Optional<Float> azimuth) {
        inclination.ifPresent(v -> currentEye.inclination = v);
        azimuth.ifPresent(v -> currentEye.azimuth = v);
    }

    /** The CLI {@code variant weight <w>} op: set the current variant's relative weight (clamped >= 0). */
    public static void setVariantWeight(double w) {
        currentVariant().weight = Math.max(0, w);
    }

    private static void syncPartIndex() {
        if (currentPart == null) {
            return;
        }
        for (int i = 0; i < parts.size(); i++) {
            if (EyeAttachmentResolver.pathMatches(currentPart, parts.get(i))) {
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
            // Pre-pass: each eye's index within its own part group (= its on-disk within-head index),
            // so a cross-target given as a flat list index can be translated to what the runtime uses.
            int[] withinHead = withinHeadIndices(dv);

            LinkedHashMap<String, HeadConfig> grouped = new LinkedHashMap<>();
            for (int f = 0; f < dv.eyes.size(); f++) {
                ListedEye le = dv.eyes.get(f);
                if (le.part == null) {
                    continue;
                }
                HeadConfig head = grouped.computeIfAbsent(le.part, t -> {
                    HeadConfig h = new HeadConfig();
                    h.attachPoint = t;
                    h.eyes = new ArrayList<>();
                    return h;
                });
                head.eyes.add(le.eye.toDefinition(resolveCrossTarget(dv, f, withinHead)));
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

    /** For each eye in {@code dv}, its 0-based index within its own part group ({@code -1} for no part). */
    private static int[] withinHeadIndices(DraftVariant dv) {
        int[] within = new int[dv.eyes.size()];
        Map<String, Integer> perPartCount = new LinkedHashMap<>();
        for (int f = 0; f < dv.eyes.size(); f++) {
            String part = dv.eyes.get(f).part;
            if (part == null) {
                within[f] = -1;
                continue;
            }
            int c = perPartCount.getOrDefault(part, 0);
            within[f] = c;
            perPartCount.put(part, c + 1);
        }
        return within;
    }

    /**
     * Resolve eye {@code f}'s flat cross-target to a within-head index, or {@code -1} when it has none, is
     * out of range, points at itself, or points at an eye on a different part (cross-eye is same-head only).
     */
    private static int resolveCrossTarget(DraftVariant dv, int f, int[] withinHead) {
        int flat = dv.eyes.get(f).eye.crossTarget;
        if (flat < 0 || flat >= dv.eyes.size() || flat == f) {
            return -1;
        }
        ListedEye self = dv.eyes.get(f);
        ListedEye target = dv.eyes.get(flat);
        if (self.part == null || !self.part.equals(target.part)) {
            return -1;
        }
        return withinHead[flat];
    }

    /** Eyes saved across all variants (the export guard). */
    public static int totalEyeCount() {
        int n = 0;
        for (DraftVariant v : variants) {
            n += v.eyes.size();
        }
        return n;
    }

    /** Ask the server to release this player's frozen mob (restores its pre-picker NoAi). */
    private static void unfreeze() {
        send(PickerFreezePacket.unfreeze());
    }

    /** Send a picker request if a connection exists (guards races around disconnect). */
    private static void send(Object packet) {
        if (Minecraft.getInstance().getConnection() != null) {
            NetworkHandler.INSTANCE.sendToServer(packet);
        }
    }

    /**
     * Reset picker state to its construction state on disconnect, <b>without sending anything</b> — the
     * connection may already be gone, and the server's own logout handling
     * ({@code PickerFreezeService#onPlayerLoggedOut}) releases any frozen mob regardless.
     *
     * <p>The authored drafts go too, along with {@link #targetType}. They were authored against — and
     * seeded from — the server we're leaving, and {@code export} would happily send them to the next
     * server we join, writing one world's eye configs into another's datapack.
     */
    public static void resetOnDisconnect() {
        active = false;
        target = new WeakReference<>(null);
        targetType = null;
        parts = new ArrayList<>();
        partIndex = 0;
        authored.clear();
        variants = new ArrayList<>(List.of(new DraftVariant()));
        variantIndex = 0;
        currentPart = null;
        clearDraft();
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
