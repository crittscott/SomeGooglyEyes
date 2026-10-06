package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
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
 * ({@link #ALLOW_SPAWN_ALL}, enforced by the server command).
 */
public class ServerConfig {
    public static final String ALLOW_SPAWN_ALL_KEY = "allowSpawnAll";
    public static final boolean ALLOW_SPAWN_ALL_DEFAULT = false;
    public static final String AMBIENT_BEHAVIOR_POOL_KEY = "ambientBehaviorPool";
    public static final List<String> AMBIENT_BEHAVIOR_POOL_DEFAULT = List.of(
            EyeBehavior.BLINK.id().toString(),
            EyeBehavior.CROSS_EYE.id().toString(),
            EyeBehavior.SIDE_EYE.id().toString(),
            EyeBehavior.STARE.id().toString());
    public static final String AMBIENT_BEHAVIORS_KEY = "ambientBehaviors";
    public static final boolean AMBIENT_BEHAVIORS_DEFAULT = true;
    public static final String AMBIENT_MAX_TICKS_KEY = "ambientMaxTicks";
    public static final int AMBIENT_MAX_TICKS_DEFAULT = 800;
    public static final String AMBIENT_MIN_TICKS_KEY = "ambientMinTicks";
    public static final int AMBIENT_MIN_TICKS_DEFAULT = 200;
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
    public static final String SPAWN_EXCLUDED_ENTITIES_KEY = "spawnExcludedEntities";
    public static final List<String> SPAWN_EXCLUDED_ENTITIES_DEFAULT = List.of();
    public static final String SPAWN_EXCLUDED_MODS_KEY = "spawnExcludedMods";
    public static final List<String> SPAWN_EXCLUDED_MODS_DEFAULT = List.of("minecolonies", "create");
    public static final String SWIRL_HEAL_COOLDOWN_TICKS_KEY = "swirlHealCooldownTicks";
    public static final int SWIRL_HEAL_COOLDOWN_TICKS_DEFAULT = 200;
    public static final String SWIRL_ON_HEAL_KEY = "swirlOnHeal";
    public static final boolean SWIRL_ON_HEAL_DEFAULT = true;
    public static final String SWIRL_ON_TRADE_KEY = "swirlOnTrade";
    public static final boolean SWIRL_ON_TRADE_DEFAULT = true;
    public static final int TICKS_MAX = 24000;
    public static final int TICKS_MIN = 1;

    // Config-file comments, one line per comment line, shared by every loader's server config schema.
    public static final String GOOGLY_EYES_ENABLED_COMMENT = """
            Master switch. false stops new mobs from rolling eyes, hides every eye (old and new) on every
            client, and refuses hand-applying or harvesting eyes; existing NBT eye data is left untouched
            and reappears when this is turned back on. Clients already connected when the server picks up a
            change see it only after /reload or reconnecting.""";
    public static final String ENTITY_OVERRIDES_COMMENT = """
            Per-entity eye chances, one entry per line as "entity-pattern,percent" (percent 0-100).
            '*' wildcards the entity id, e.g. "minecraft:zombie,100", "*:*_horse,50", "alexsmobs:*,0".
            An exact id always wins over a wildcard; among wildcards, the first matching line wins.
            Entities matching nothing here use globalPercent. A percent of 0 stops NEW spawns of that
            entity/pattern from rolling eyes; it does not remove eyes already granted, and a player can
            still give the entity eyes by hand with a Slimy Eye.""";
    public static final String ALLOW_SPAWN_ALL_COMMENT = """
            Enables /sg spawnall, which force-spawns every summonable living mob in a grid with no undo.
            WARNING: only enable this on a test or throwaway world. Spawning a mob outside its own mod's
            normal context, as spawnall does, can corrupt or destabilize the world; MineColonies and
            Create mobs are known cases (see spawnExcludedMods below).""";
    public static final String SPAWN_EXCLUDED_MODS_COMMENT = """
            Namespaces that /sg spawn and /sg spawnall skip, one quoted entry per line, e.g. "mekanism".
            MineColonies and Create are excluded by default because their mobs can corrupt or
            destabilize a world if force-spawned outside their mod's normal context. Only remove either
            entry on a world you're prepared to lose. These are authoring commands only; nothing here
            changes eye eligibility or natural spawning.""";
    public static final String SPAWN_EXCLUDED_ENTITIES_COMMENT = """
            Entity ids that /sg spawn and /sg spawnall skip, one quoted entry per line, e.g.
            "minecraft:armor_stand". Same authoring-only scope as spawnExcludedMods.""";

    public static final ConfigValue<Boolean> ALLOW_SPAWN_ALL = ConfigValue.bool(ALLOW_SPAWN_ALL_DEFAULT);
    public static final ConfigValue.Parsed<List<EyeBehavior>> AMBIENT_BEHAVIOR_POOL = ConfigValue.parsedStrings(
            AMBIENT_BEHAVIOR_POOL_DEFAULT, ServerConfig::validateResourceLocation, ServerConfig::parseBehaviorPool);
    public static final ConfigValue<Boolean> AMBIENT_BEHAVIORS = ConfigValue.bool(AMBIENT_BEHAVIORS_DEFAULT);
    public static final ConfigValue<Integer> AMBIENT_MAX_TICKS =
            ConfigValue.integer(AMBIENT_MAX_TICKS_DEFAULT, TICKS_MIN, TICKS_MAX);
    public static final ConfigValue<Integer> AMBIENT_MIN_TICKS =
            ConfigValue.integer(AMBIENT_MIN_TICKS_DEFAULT, TICKS_MIN, TICKS_MAX);
    public static final ConfigValue.Parsed<List<SpawnOverride>> ENTITY_OVERRIDES = ConfigValue.parsedStrings(
            ENTITY_OVERRIDES_DEFAULT, ServerConfig::validateOverride, ServerConfig::parseOverrides);
    public static final ConfigValue<Integer> GLOBAL_PERCENT =
            ConfigValue.integer(GLOBAL_PERCENT_DEFAULT, PERCENT_MIN, PERCENT_MAX);
    public static final ConfigValue<Boolean> GOOGLY_EYES_ENABLED = ConfigValue.bool(GOOGLY_EYES_ENABLED_DEFAULT);
    public static final ConfigValue<Integer> GROW_ON_HIT_PERCENT =
            ConfigValue.integer(GROW_ON_HIT_PERCENT_DEFAULT, PERCENT_MIN, PERCENT_MAX);
    public static final ConfigValue<Integer> HARVEST_ON_KILL_PERCENT =
            ConfigValue.integer(HARVEST_ON_KILL_PERCENT_DEFAULT, PERCENT_MIN, PERCENT_MAX);
    public static final ConfigValue.Parsed<Set<String>> SPAWN_EXCLUDED_ENTITIES = ConfigValue.parsedStrings(
            SPAWN_EXCLUDED_ENTITIES_DEFAULT, ServerConfig::validateResourceLocation, Set::copyOf);
    public static final ConfigValue.Parsed<Set<String>> SPAWN_EXCLUDED_MODS = ConfigValue.parsedStrings(
            SPAWN_EXCLUDED_MODS_DEFAULT, ServerConfig::validateNamespace, Set::copyOf);
    public static final ConfigValue<Integer> SWIRL_HEAL_COOLDOWN_TICKS =
            ConfigValue.integer(SWIRL_HEAL_COOLDOWN_TICKS_DEFAULT, TICKS_MIN, TICKS_MAX);
    public static final ConfigValue<Boolean> SWIRL_ON_HEAL = ConfigValue.bool(SWIRL_ON_HEAL_DEFAULT);
    public static final ConfigValue<Boolean> SWIRL_ON_TRADE = ConfigValue.bool(SWIRL_ON_TRADE_DEFAULT);

    /** One parsed override line. Exact entries match by string equality; wildcard entries by regex. */
    private record SpawnOverride(boolean exact, String literalId, Pattern pattern, int percent) {
    }

    /**
     * The behaviors eligible for ambient play: every registered behavior whose id appears in
     * {@link #AMBIENT_BEHAVIOR_POOL}. Unknown ids in the config are simply ignored.
     */
    public static List<EyeBehavior> enabledBehaviors() {
        return AMBIENT_BEHAVIOR_POOL.parsed();
    }

    private static List<EyeBehavior> parseBehaviorPool(List<String> pool) {
        Set<String> enabled = new LinkedHashSet<>(pool);
        List<EyeBehavior> result = new ArrayList<>();
        for (EyeBehavior behavior : EyeBehavior.values()) {
            if (enabled.contains(behavior.id().toString())) {
                result.add(behavior);
            }
        }
        return List.copyOf(result);
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

    private static List<SpawnOverride> parseOverrides(List<String> entries) {
        List<SpawnOverride> built = new ArrayList<>();
        for (String entry : entries) {
            built.add(parse(entry));
        }
        return List.copyOf(built);
    }

    /**
     * Parse one {@code "entity-pattern,percent"} line: entity-id characters plus {@code *}, and a percent
     * within {@code [PERCENT_MIN, PERCENT_MAX]}. Returns {@code null} for a malformed line.
     */
    @Nullable
    private static SpawnOverride parse(String entry) {
        String[] split = entry.split(",");
        if (split.length != 2) {
            return null;
        }
        String pattern = split[0].trim();
        // Allow the chars legal in an entity id (namespace:path) plus '*' for wildcards.
        if (pattern.isEmpty() || !pattern.matches("[a-z0-9_./:*-]+")) {
            return null;
        }
        int percent;
        try {
            percent = Integer.parseInt(split[1].trim());
        } catch (NumberFormatException e) {
            return null;
        }
        if (percent < PERCENT_MIN || percent > PERCENT_MAX) {
            return null;
        }
        return pattern.indexOf('*') < 0
                ? new SpawnOverride(true, pattern, null, percent)
                : new SpawnOverride(false, null, Pattern.compile(globToRegex(pattern)), percent);
    }

    /**
     * The configured eye chance (0–100) for the given entity: an exact override if one matches, else
     * the first matching wildcard override, else {@link #GLOBAL_PERCENT}.
     */
    public static int percentFor(ResourceLocation entityType) {
        String id = entityType.toString();
        SpawnOverride firstWildcard = null;
        for (SpawnOverride o : ENTITY_OVERRIDES.parsed()) {
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

    /**
     * Whether {@code /sg spawn} and {@code /sg spawnall} should skip this entity type: its exact id is
     * listed in {@link #SPAWN_EXCLUDED_ENTITIES}, or its namespace in {@link #SPAWN_EXCLUDED_MODS}. Purely
     * an authoring-command filter — it has no bearing on eye eligibility or natural spawning.
     */
    public static boolean isSpawnExcluded(ResourceLocation entityType) {
        return SPAWN_EXCLUDED_MODS.parsed().contains(entityType.getNamespace())
                || SPAWN_EXCLUDED_ENTITIES.parsed().contains(entityType.toString());
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
        SPAWN_EXCLUDED_ENTITIES.reset();
        SPAWN_EXCLUDED_MODS.reset();
        SWIRL_HEAL_COOLDOWN_TICKS.reset();
        SWIRL_ON_HEAL.reset();
        SWIRL_ON_TRADE.reset();
    }

    /**
     * Whether a config string parses as a {@link ResourceLocation}; the entry guard for
     * {@link #AMBIENT_BEHAVIOR_POOL}, {@link #SPAWN_EXCLUDED_ENTITIES}, and the client's {@code disabledEntities}.
     */
    public static boolean validateResourceLocation(String value) {
        return ResourceLocation.tryParse(value) != null;
    }

    /**
     * Whether a config string is a bare resource-location namespace; the entry guard for
     * {@link #SPAWN_EXCLUDED_MODS}, the client's {@code disabledMods}, and the {@code /sg spawnall} filter.
     */
    public static boolean validateNamespace(String value) {
        return !value.isEmpty() && ResourceLocation.isValidNamespace(value);
    }

    /** Whether a config string is a well-formed override line ({@link #parse}); the entry guard for {@link #ENTITY_OVERRIDES}. */
    public static boolean validateOverride(String value) {
        return parse(value) != null;
    }
}
