package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehaviors;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Server-authoritative spawn settings. The googly-eye chance lives here (not in the bundled eye
 * datapack), so admins can tune how common eyes are — globally and per entity — without editing the
 * 100+ shipped config files. The eye JSONs only decide <i>where</i> eyes go and whether an entity is
 * eligible at all (their {@code enabled} flag is an authoritative hard on/off, applied in
 * {@code ServerServices}); this class decides <i>how often</i> an eligible entity actually rolls
 * eyes. Also hosts the picker's opt-in gate for the destructive {@code /sg spawnall} grid
 * ({@link #ALLOW_SPAWN_ALL}, enforced server-side in {@code PickerSpawnAllPacket}).
 */
public class ServerConfig {
    public static final ConfigValue<Boolean> ALLOW_SPAWN_ALL = ConfigValue.bool(false);
    public static final ConfigValue<List<String>> AMBIENT_BEHAVIOR_POOL =
            ConfigValue.strings(defaultAmbientBehaviorIds(), ServerConfig::validateBehaviorId);
    public static final ConfigValue<Boolean> AMBIENT_BEHAVIORS = ConfigValue.bool(true);
    public static final ConfigValue<Integer> AMBIENT_MAX_TICKS = ConfigValue.integer(400, 1, 24000);
    public static final ConfigValue<Integer> AMBIENT_MIN_TICKS = ConfigValue.integer(100, 1, 24000);
    public static final ConfigValue<List<String>> ENTITY_OVERRIDES =
            ConfigValue.strings(List.of(), ServerConfig::validateOverride);
    public static final ConfigValue<Integer> GLOBAL_PERCENT = ConfigValue.integer(5, 0, 100);
    public static final ConfigValue<Boolean> GOOGLY_EYES_ENABLED = ConfigValue.bool(true);
    public static final ConfigValue<Integer> GROW_ON_HIT_PERCENT = ConfigValue.integer(20, 0, 100);
    public static final ConfigValue<Integer> HARVEST_ON_KILL_PERCENT = ConfigValue.integer(25, 0, 100);
    public static final ConfigValue<Integer> SWIRL_HEAL_COOLDOWN_TICKS = ConfigValue.integer(200, 1, 24000);
    public static final ConfigValue<Boolean> SWIRL_ON_HEAL = ConfigValue.bool(true);
    public static final ConfigValue<Boolean> SWIRL_ON_TRADE = ConfigValue.bool(true);

    // Compiled view of ENTITY_OVERRIDES, rebuilt whenever a loader or test replaces the immutable list.
    private static List<String> lastParsedSource;
    private static List<Override> overrides = List.of();

    /** One parsed override line. Exact entries match by string equality; wildcard entries by regex. */
    private record Override(boolean exact, String literalId, Pattern pattern, int percent) {
    }

    /**
     * The behaviors the ambient idle timer plays by default: the ambient set only (not the event-driven
     * grow/swirl, nor the dormant color_change).
     */
    private static List<String> defaultAmbientBehaviorIds() {
        return new ArrayList<>(List.of(
                "somegoogly:blink",
                "somegoogly:cross_eye",
                "somegoogly:side_eye",
                "somegoogly:stare"));
    }

    /**
     * The behaviors eligible for ambient play: every registered behavior whose id appears in
     * {@link #AMBIENT_BEHAVIOR_POOL}. Unknown ids in the config are simply ignored. Recomputed each call —
     * it's only hit when a mob's ambient timer fires.
     */
    public static List<EyeBehavior> enabledBehaviors() {
        Set<String> enabled = new LinkedHashSet<>();
        for (String id : AMBIENT_BEHAVIOR_POOL.get()) {
            enabled.add(id);
        }
        List<EyeBehavior> result = new ArrayList<>();
        for (EyeBehavior behavior : EyeBehaviors.all()) {
            if (enabled.contains(behavior.id().toString())) {
                result.add(behavior);
            }
        }
        return result;
    }

    /** Translate a glob (only '*' is special) into an anchored regex by quoting the literal runs. */
    private static String globToRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        int literalStart = 0;
        for (int i = 0; i < glob.length(); i++) {
            if (glob.charAt(i) == '*') {
                regex.append(Pattern.quote(glob.substring(literalStart, i)));
                regex.append(".*");
                literalStart = i + 1;
            }
        }
        regex.append(Pattern.quote(glob.substring(literalStart)));
        return regex.toString();
    }

    /**
     * Parse one 'pattern,percent' line into an {@link Override}, or null if malformed (validation
     * already ran, but reloads can still surface bad entries, so we stay defensive).
     */
    private static Override parse(String entry) {
        String[] split = entry.split(",");
        if (split.length != 2) {
            return null;
        }
        String pattern = split[0].trim();
        int percent;
        try {
            percent = Integer.parseInt(split[1].trim());
        } catch (NumberFormatException e) {
            return null;
        }
        if (pattern.isEmpty() || percent < 0 || percent > 100) {
            return null;
        }
        return pattern.indexOf('*') < 0
                ? new Override(true, pattern, null, percent)
                : new Override(false, null, Pattern.compile(globToRegex(pattern)), percent);
    }

    /**
     * The configured eye chance (0–100) for the given entity: an exact override if one matches, else
     * the first matching wildcard override, else {@link #GLOBAL_PERCENT}.
     */
    public static int percentFor(ResourceLocation entityType) {
        rebuildIfChanged();
        String id = entityType.toString();
        Override firstWildcard = null;
        for (Override o : overrides) {
            if (o.exact) {
                if (o.literalId.equals(id)) {
                    return o.percent; // exact match beats any wildcard, regardless of list order
                }
            } else if (firstWildcard == null && o.pattern.matcher(id).matches()) {
                firstWildcard = o;
            }
        }
        return firstWildcard != null ? firstWildcard.percent : GLOBAL_PERCENT.get();
    }

    private static void rebuildIfChanged() {
        List<String> source = ENTITY_OVERRIDES.get();
        if (source == lastParsedSource) {
            return;
        }
        List<Override> built = new ArrayList<>();
        for (String entry : source) {
            Override parsed = parse(entry);
            if (parsed != null) {
                built.add(parsed);
            }
        }
        overrides = built;
        lastParsedSource = source;
    }

    /** Restore built-in defaults before a loader applies a newly opened world's values. */
    public static void resetDefaults() {
        ALLOW_SPAWN_ALL.reset();
        AMBIENT_BEHAVIOR_POOL.reset();
        AMBIENT_BEHAVIORS.reset();
        AMBIENT_MAX_TICKS.reset();
        AMBIENT_MIN_TICKS.reset();
        ENTITY_OVERRIDES.reset();
        GLOBAL_PERCENT.reset();
        GOOGLY_EYES_ENABLED.reset();
        GROW_ON_HIT_PERCENT.reset();
        HARVEST_ON_KILL_PERCENT.reset();
        SWIRL_HEAL_COOLDOWN_TICKS.reset();
        SWIRL_ON_HEAL.reset();
        SWIRL_ON_TRADE.reset();
        lastParsedSource = null;
        overrides = List.of();
    }

    public static boolean validateBehaviorId(String value) {
        return ResourceLocation.tryParse(value) != null;
    }

    public static boolean validateOverride(String value) {
        String[] split = value.split(",");
        if (split.length != 2) {
            return false;
        }
        String pattern = split[0].trim();
        // Allow the chars legal in an entity id (namespace:path) plus '*' for wildcards.
        if (pattern.isEmpty() || !pattern.matches("[a-z0-9_./:*-]+")) {
            return false;
        }
        try {
            int percent = Integer.parseInt(split[1].trim());
            return percent >= 0 && percent <= 100;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
