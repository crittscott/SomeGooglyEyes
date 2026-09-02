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
 * datapack), so admins can tune how common eyes are — globally and per entity — without touching the
 * shipped eye-definition datapacks. Those JSONs only decide <i>where</i> eyes go and whether an entity is
 * eligible at all (their {@code enabled} flag is an authoritative hard on/off, applied in
 * {@code ServerServices}); this class decides <i>how often</i> an eligible entity actually rolls
 * eyes. Also hosts the picker's opt-in gate for the destructive {@code /sg spawnall} grid
 * ({@link #ALLOW_SPAWN_ALL}, enforced server-side in {@code PickerSpawnAllPacket}).
 */
public class ServerConfig {
    public static final String ALLOW_SPAWN_ALL_KEY = "allowSpawnAll";
    public static final boolean ALLOW_SPAWN_ALL_DEFAULT = false;
    public static final String AMBIENT_BEHAVIOR_POOL_KEY = "ambientBehaviorPool";
    public static final List<String> AMBIENT_BEHAVIOR_POOL_DEFAULT = List.of(
            EyeBehaviors.BLINK.id().toString(),
            EyeBehaviors.CROSS_EYE.id().toString(),
            EyeBehaviors.SIDE_EYE.id().toString(),
            EyeBehaviors.STARE.id().toString());
    public static final String AMBIENT_BEHAVIORS_KEY = "ambientBehaviors";
    public static final boolean AMBIENT_BEHAVIORS_DEFAULT = true;
    public static final String AMBIENT_MAX_TICKS_KEY = "ambientMaxTicks";
    public static final int AMBIENT_MAX_TICKS_DEFAULT = 400;
    public static final String AMBIENT_MIN_TICKS_KEY = "ambientMinTicks";
    public static final int AMBIENT_MIN_TICKS_DEFAULT = 100;
    public static final String ENTITY_OVERRIDES_KEY = "entityOverrides";
    public static final List<String> ENTITY_OVERRIDES_DEFAULT = List.of();
    public static final String GLOBAL_PERCENT_KEY = "globalPercent";
    public static final int GLOBAL_PERCENT_DEFAULT = 5;
    public static final String GOOGLY_EYES_ENABLED_KEY = "googlyEyesEnabled";
    public static final boolean GOOGLY_EYES_ENABLED_DEFAULT = true;
    public static final String GROW_ON_HIT_PERCENT_KEY = "growOnHitPercent";
    public static final int GROW_ON_HIT_PERCENT_DEFAULT = 20;
    public static final String HARVEST_ON_KILL_PERCENT_KEY = "harvestOnKillPercent";
    public static final int HARVEST_ON_KILL_PERCENT_DEFAULT = 25;
    public static final int PERCENT_MAX = 100;
    public static final int PERCENT_MIN = 0;
    public static final String SWIRL_HEAL_COOLDOWN_TICKS_KEY = "swirlHealCooldownTicks";
    public static final int SWIRL_HEAL_COOLDOWN_TICKS_DEFAULT = 200;
    public static final String SWIRL_ON_HEAL_KEY = "swirlOnHeal";
    public static final boolean SWIRL_ON_HEAL_DEFAULT = true;
    public static final String SWIRL_ON_TRADE_KEY = "swirlOnTrade";
    public static final boolean SWIRL_ON_TRADE_DEFAULT = true;
    public static final int TICKS_MAX = 24000;
    public static final int TICKS_MIN = 1;

    public static final ConfigValue<Boolean> ALLOW_SPAWN_ALL = ConfigValue.bool(ALLOW_SPAWN_ALL_DEFAULT);
    public static final ConfigValue<List<String>> AMBIENT_BEHAVIOR_POOL =
            ConfigValue.strings(AMBIENT_BEHAVIOR_POOL_DEFAULT, ServerConfig::validateBehaviorId);
    public static final ConfigValue<Boolean> AMBIENT_BEHAVIORS = ConfigValue.bool(AMBIENT_BEHAVIORS_DEFAULT);
    public static final ConfigValue<Integer> AMBIENT_MAX_TICKS =
            ConfigValue.integer(AMBIENT_MAX_TICKS_DEFAULT, TICKS_MIN, TICKS_MAX);
    public static final ConfigValue<Integer> AMBIENT_MIN_TICKS =
            ConfigValue.integer(AMBIENT_MIN_TICKS_DEFAULT, TICKS_MIN, TICKS_MAX);
    public static final ConfigValue<List<String>> ENTITY_OVERRIDES =
            ConfigValue.strings(ENTITY_OVERRIDES_DEFAULT, ServerConfig::validateOverride);
    public static final ConfigValue<Integer> GLOBAL_PERCENT =
            ConfigValue.integer(GLOBAL_PERCENT_DEFAULT, PERCENT_MIN, PERCENT_MAX);
    public static final ConfigValue<Boolean> GOOGLY_EYES_ENABLED = ConfigValue.bool(GOOGLY_EYES_ENABLED_DEFAULT);
    public static final ConfigValue<Integer> GROW_ON_HIT_PERCENT =
            ConfigValue.integer(GROW_ON_HIT_PERCENT_DEFAULT, PERCENT_MIN, PERCENT_MAX);
    public static final ConfigValue<Integer> HARVEST_ON_KILL_PERCENT =
            ConfigValue.integer(HARVEST_ON_KILL_PERCENT_DEFAULT, PERCENT_MIN, PERCENT_MAX);
    public static final ConfigValue<Integer> SWIRL_HEAL_COOLDOWN_TICKS =
            ConfigValue.integer(SWIRL_HEAL_COOLDOWN_TICKS_DEFAULT, TICKS_MIN, TICKS_MAX);
    public static final ConfigValue<Boolean> SWIRL_ON_HEAL = ConfigValue.bool(SWIRL_ON_HEAL_DEFAULT);
    public static final ConfigValue<Boolean> SWIRL_ON_TRADE = ConfigValue.bool(SWIRL_ON_TRADE_DEFAULT);

    // Compiled view of ENTITY_OVERRIDES, rebuilt whenever a loader or test replaces the immutable list.
    private static List<String> lastParsedSource;
    private static List<Override> overrides = List.of();

    /** One parsed override line. Exact entries match by string equality; wildcard entries by regex. */
    private record Override(boolean exact, String literalId, Pattern pattern, int percent) {
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
        if (pattern.isEmpty() || percent < PERCENT_MIN || percent > PERCENT_MAX) {
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

    /** Whether a config string parses as a {@link ResourceLocation}; the entry guard for {@link #AMBIENT_BEHAVIOR_POOL}. */
    public static boolean validateBehaviorId(String value) {
        return ResourceLocation.tryParse(value) != null;
    }

    /**
     * Whether a config string is a well-formed {@code "entity-pattern,percent"} override: entity-id
     * characters plus {@code *}, and a percent within {@code [PERCENT_MIN, PERCENT_MAX]}. The entry guard
     * for {@link #ENTITY_OVERRIDES}.
     */
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
            return percent >= PERCENT_MIN && percent <= PERCENT_MAX;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
