package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.eye.EyeDefinition;
import com.github.crittscott.somegoogly.eye.EyePlacement;
import com.github.crittscott.somegoogly.eye.HeadInfo.HeadConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfigSet;
import com.github.crittscott.somegoogly.eye.HeadInfo.Variant;
import com.github.crittscott.somegoogly.eye.state.EyeAppearance;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Canonical datapack-JSON serialization of eye configs, shared by the client picker's export paths
 * ({@code PickerExporter}) and the server-side export service ({@code PickerExportService}).
 * Deliberately free of client imports so a dedicated server can load it.
 *
 * <p>Output is the <b>complete, canonical</b> form — every field explicit, in the shipped field order,
 * with a version <i>range</i> ({@code [1.20.1,1.21)}). This is deliberate: the files are meant to be
 * dropped into the mod's source data, so they must not rely on the loader's default-elision (the
 * runtime codecs omit any field equal to its default, leaving a sparse file whose meaning silently
 * tracks whatever the code defaults later become). Writing in full pins the authored values.
 */
public final class EyeConfigJsonWriter {

    private EyeConfigJsonWriter() {
    }

    /** One entry object for a versioned config file: version range, age, enabled flag, and variants. */
    public static JsonObject entryJson(String versionRange, String age, boolean enabled, JsonArray variants) {
        JsonObject entry = new JsonObject();
        entry.addProperty("version", versionRange);
        entry.addProperty("age", age);
        entry.addProperty("enabled", enabled);
        entry.add("variants", variants);
        return entry;
    }

    /** Wrap entries as one datapack file object ({@code {"entries": [...]}}). */
    public static JsonObject fileJson(JsonObject... entries) {
        JsonArray array = new JsonArray();
        for (JsonObject entry : entries) {
            array.add(entry);
        }
        JsonObject root = new JsonObject();
        root.add("entries", array);
        return root;
    }

    /** Serialize a whole age-set as a multi-entry file, one entry per non-empty age config, or null. */
    @Nullable
    public static JsonObject setToConfigJson(RuntimeConfigSet set, String versionRange, UnaryOperator<String> canon) {
        JsonArray entries = new JsonArray();
        addAgeEntry(entries, "adult", set.adult, versionRange, canon);
        addAgeEntry(entries, "baby", set.baby, versionRange, canon);
        addAgeEntry(entries, "any", set.any, versionRange, canon);
        if (entries.isEmpty()) {
            return null;
        }
        JsonObject root = new JsonObject();
        root.add("entries", entries);
        return root;
    }

    private static void addAgeEntry(JsonArray entries, String age, RuntimeConfig config, String versionRange,
                                    UnaryOperator<String> canon) {
        if (config == null) {
            return;
        }
        JsonArray variants = variantsJson(config.variants, canon);
        if (variants.isEmpty()) {
            return;
        }
        entries.add(entryJson(versionRange, age, config.enabled, variants));
    }

    /** Serialize variants, mapping each head's attach token through {@code canon} (its canonical form). */
    public static JsonArray variantsJson(List<Variant> variants, UnaryOperator<String> canon) {
        JsonArray out = new JsonArray();
        if (variants == null) {
            return out;
        }
        for (Variant v : variants) {
            if (v == null || v.heads == null || v.heads.isEmpty()) {
                continue;
            }
            JsonArray heads = new JsonArray();
            for (HeadConfig h : v.heads) {
                if (h == null || h.eyes == null || h.eyes.isEmpty()) {
                    continue;
                }
                JsonArray eyes = new JsonArray();
                for (EyeDefinition def : h.eyes) {
                    eyes.add(eyeJson(def));
                }
                JsonObject head = new JsonObject();
                head.addProperty("attachPoint", canon.apply(h.attachPoint));
                head.add("eyes", eyes);
                heads.add(head);
            }
            if (heads.isEmpty()) {
                continue; // skip arrangements with no usable eyes
            }
            JsonObject variant = new JsonObject();
            variant.addProperty("weight", round(v.weight()));
            variant.add("heads", heads);
            out.add(variant);
        }
        return out;
    }

    /** One eye object with every field written explicitly, in the shipped field order. */
    private static JsonObject eyeJson(EyeDefinition def) {
        EyePlacement p = def.placement();
        EyeAppearance a = def.appearance();
        JsonObject o = new JsonObject();
        o.add("position", vec3(p.position()));
        o.addProperty("eyeScale", round(p.eyeScale()));
        o.addProperty("irisScale", round(p.irisScale()));
        o.addProperty("depth", round(p.depth()));
        o.addProperty("inclination", round(p.inclination()));
        o.addProperty("azimuth", round(p.azimuth()));
        // crossTarget is a within-head index; only written when set (default -1 = no cross-eye partner).
        if (p.crossTarget() >= 0) {
            o.addProperty("crossTarget", p.crossTarget());
        }
        o.add("corneaColors", colors(a.cornea()));
        o.add("irisColors", colors(a.iris()));
        o.addProperty("glows", a.glow());
        return o;
    }

    /**
     * Round to one part in a thousand. Several {@code /sg} property args parse as {@code float} (eyeScale,
     * colors, depth, …), so a typed {@code 0.22} widens to {@code 0.2199999988079071} as a double; this
     * snaps such float-widening noise back to the authored value before it's written to the (human-edited)
     * source data.
     */
    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private static JsonArray vec3(Vec3 v) {
        JsonArray array = new JsonArray();
        array.add(round(v.x));
        array.add(round(v.y));
        array.add(round(v.z));
        return array;
    }

    private static JsonArray colors(EyeColor c) {
        JsonArray array = new JsonArray();
        array.add(round(c.r()));
        array.add(round(c.g()));
        array.add(round(c.b()));
        return array;
    }

    /**
     * Turn a loaded version like {@code 1.20.1} into the range {@code [1.20.1,1.21)} — inclusive of the
     * loaded version, exclusive of the next minor — matching the shipped configs. Falls back to the exact
     * version (an exact-match entry) if it can't be parsed into at least major.minor.
     */
    public static String versionRange(String loaded) {
        String[] parts = loaded.split("\\.");
        if (parts.length >= 2) {
            try {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                return "[" + loaded + "," + major + "." + (minor + 1) + ")";
            } catch (NumberFormatException ignored) {
                // not numeric major.minor; fall through to an exact-match entry
            }
        }
        return loaded;
    }
}
