package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Server-authoritative spawn settings. The googly-eye chance lives here (not in the bundled eye
 * datapack), so admins can tune how common eyes are — globally and per entity — without touching the
 * shipped eye-definition datapacks. Those JSONs only decide <i>where</i> eyes go and whether an entity is
 * eligible at all (their {@code enabled} flag is an authoritative hard on/off, applied by
 * {@code EyeConfigModel.RuntimeConfig.isUsable}); this class decides <i>how often</i> an eligible entity actually rolls
 * eyes. Also hosts the picker's opt-in gate for the destructive {@code /sg spawnall} grid
 * ({@link #ALLOW_SPAWN_ALL}, enforced by the server command).
 */
public class ServerConfig {
    public static final int PERCENT_MAX = 100;
    public static final int PERCENT_MIN = 0;
    public static final int TICKS_MAX = SharedConstants.TICKS_PER_GAME_DAY;
    public static final int TICKS_MIN = 1;

    // TOML file and section names, shared by every loader's server config. The section names are those the
    // released 0.8.2 files use, so a world's existing file keeps its values when Forge corrects it against the spec.
    public static final String FILE_NAME = SomeGooglyCommon.MOD_ID + "-server.toml";
    public static final String SECTION_SERVER = "Server Settings";
    public static final String SECTION_BEHAVIORS = "Behaviors";
    public static final String SECTION_PICKER = "Picker";

    // Precedes the settings, whose initializers fill it.
    private static final List<ConfigValue<?>> SETTINGS = new ArrayList<>();

    // Declaration order is file order, section by section, so these keep the order of the file they describe.
    /** Master switch for spawning, rendering, applying, and harvesting eyes. */
    public static final ConfigValue<Boolean> GOOGLY_EYES_ENABLED = add(ConfigValue.bool(
            SECTION_SERVER, "googlyEyesEnabled", true, """
            Master switch. false stops new mobs from rolling eyes, hides every eye (old and new) on every
            client, and refuses hand-applying or harvesting eyes; existing NBT eye data is left untouched
            and reappears when this is turned back on. Connected clients see a change as soon as the server
            applies it: on Forge when the edited file is reloaded, on Fabric at the next /reload."""));

    /** Spawn chance (0–100) for entities no override matches. */
    public static final ConfigValue<Integer> GLOBAL_PERCENT = add(ConfigValue.integer(
            SECTION_SERVER, "globalPercent", 5, PERCENT_MIN, PERCENT_MAX, """
            Chance (0-100) that a newly spawned eligible mob gets eyes, for entities not matched by
            entityOverrides. Changing it affects only mobs spawned afterwards."""));

    /** Chance (0–100) that a shears kill of an eyed mob drops a Googly Eye. */
    public static final ConfigValue<Integer> HARVEST_ON_KILL_PERCENT = add(ConfigValue.integer(
            SECTION_SERVER, "harvestOnKillPercent", 25, PERCENT_MIN, PERCENT_MAX, """
            Chance (0-100) that killing an eyed mob with a direct melee blow from shears drops a Googly Eye."""));

    /** Per-entity spawn chances as {@code "entity-pattern,percent"} lines; read through {@link #percentFor}. */
    public static final ConfigValue.Parsed<List<SpawnOverride>> ENTITY_OVERRIDES = add(ConfigValue.parsedStrings(
            SECTION_SERVER, "entityOverrides", List.of(),
            ServerConfig::validateOverride, ServerConfig::parseOverrides, """
            Per-entity eye chances, one entry per line as "entity-pattern,percent" (percent 0-100).
            '*' wildcards the entity id, e.g. "minecraft:zombie,100", "*:*_horse,50", "alexsmobs:*,0".
            An exact id always wins over a wildcard; among wildcards, the first matching line wins.
            Entities matching nothing here use globalPercent. A percent of 0 stops NEW spawns of that
            entity/pattern from rolling eyes; it does not remove eyes already granted, and a player can
            still give the entity eyes by hand with a Slimy Eye."""));

    /** Whether eyed mobs play idle expressions. */
    public static final ConfigValue<Boolean> AMBIENT_BEHAVIORS = add(ConfigValue.bool(
            SECTION_BEHAVIORS, "ambientBehaviors", true, """
            Whether eyed mobs, placed heads, and a held Googly Eye play idle expressions (chosen from
            ambientBehaviorPool) on their own."""));

    /** Shortest wait in ticks between one mob's idle expressions. */
    public static final ConfigValue<Integer> AMBIENT_MIN_TICKS = add(ConfigValue.integer(
            SECTION_BEHAVIORS, "ambientMinTicks", 200, TICKS_MIN, TICKS_MAX, """
            Shortest wait, in ticks, between one mob's idle expressions."""));

    /** Longest wait in ticks between one mob's idle expressions. */
    public static final ConfigValue<Integer> AMBIENT_MAX_TICKS = add(ConfigValue.integer(
            SECTION_BEHAVIORS, "ambientMaxTicks", 800, TICKS_MIN, TICKS_MAX, """
            Longest wait, in ticks, between one mob's idle expressions."""));

    /** Behavior ids idle mobs choose from, parsed to the registered behaviors they name. */
    public static final ConfigValue.Parsed<List<EyeBehavior>> AMBIENT_BEHAVIOR_POOL = add(ConfigValue.parsedStrings(
            SECTION_BEHAVIORS, "ambientBehaviorPool", List.of(EyeBehavior.BLINK.id().toString(), EyeBehavior.CROSS_EYE.id().toString(),
                    EyeBehavior.SIDE_EYE.id().toString(), EyeBehavior.STARE.id().toString()),
            ServerConfig::validateResourceLocation, ServerConfig::parseBehaviorPool, """
            Expressions idle mobs choose from, e.g. "somegoogly:blink". Available: blink, cross_eye,
            side_eye, stare, grow, love, swirl, color_change. Unknown ids are ignored."""));

    /** Chance (0–100) that player damage makes an eyed mob's eyes bulge. */
    public static final ConfigValue<Integer> GROW_ON_HIT_PERCENT = add(ConfigValue.integer(
            SECTION_BEHAVIORS, "growOnHitPercent", 20, PERCENT_MIN, PERCENT_MAX, """
            Chance (0-100) that an eyed mob's eyes bulge when a player damages it, and that a held
            Googly Eye bulges when its holder is hurt."""));

    /** Whether milking makes an eyed mob's eyes bulge. */
    public static final ConfigValue<Boolean> GROW_ON_MILK = add(ConfigValue.bool(
            SECTION_BEHAVIORS, "growOnMilk", true, """
            Whether an eyed cow's, mooshroom's, or goat's eyes bulge when it is milked."""));

    /** Whether shearing makes an eyed sheep's eyes bulge. */
    public static final ConfigValue<Boolean> GROW_ON_SHEAR = add(ConfigValue.bool(
            SECTION_BEHAVIORS, "growOnShear", true, """
            Whether an eyed sheep's eyes bulge when it is sheared."""));

    /** Whether a vanilla affection event makes an eyed mob's pupils swell. */
    public static final ConfigValue<Boolean> LOVE_ON_AFFECTION = add(ConfigValue.bool(
            SECTION_BEHAVIORS, "loveOnAffection", true, """
            Whether an eyed mob's pupils swell when it shows affection: an animal entering love mode,
            villagers breeding, an iron golem offering a flower, or an ocelot coming to trust a player."""));

    /** Whether angry eyed mobs show red pupils; also whether the server shares anger vanilla keeps to itself. */
    public static final ConfigValue<Boolean> ANGRY_EYES = add(ConfigValue.bool(
            SECTION_BEHAVIORS, "angryEyes", true, """
            Whether an eyed mob that is attacking, or angry without attacking, such as a provoked iron
            golem, piglin, or zombified piglin, shows red pupils. For the second kind the server checks
            each watched eyed mob once per tick and tells clients only when its anger starts or ends;
            with this off it checks nothing."""));

    /** Whether a sleeping mob squints, or a sleeping villager lowers its lids. */
    public static final ConfigValue<Boolean> SQUINT_ON_SLEEP = add(ConfigValue.bool(
            SECTION_BEHAVIORS, "squintOnSleep", true, """
            Whether a sleeping eyed mob's eyes squint, or, for a villager, its eyelids close to a slit.
            A sleeping mob plays no expressions either way."""));

    /** Whether a goat lowering its head to ram squints. */
    public static final ConfigValue<Boolean> SQUINT_ON_RAM = add(ConfigValue.bool(
            SECTION_BEHAVIORS, "squintOnRam", true, """
            Whether an eyed goat's eyes squint while it lowers its head to ram."""));

    /** Whether an archer drawing its bow closes one eye. */
    public static final ConfigValue<Boolean> WINK_ON_AIM = add(ConfigValue.bool(
            SECTION_BEHAVIORS, "winkOnAim", true, """
            Whether an eyed skeleton drawing its bow, or an eyed player drawing a bow, closes the eye
            away from the arm holding it."""));

    /** Whether a swelling creeper's pupils rattle. */
    public static final ConfigValue<Boolean> RATTLE_ON_SWELL = add(ConfigValue.bool(
            SECTION_BEHAVIORS, "rattleOnSwell", true, """
            Whether a swelling eyed creeper's pupils rattle, harder the closer it is to exploding."""));

    /** Whether a creepy enderman's pupils pulse. */
    public static final ConfigValue<Boolean> PULSE_WHEN_CREEPY = add(ConfigValue.bool(
            SECTION_BEHAVIORS, "pulseWhenCreepy", true, """
            Whether a creepy eyed enderman's pupils, while it stares down whoever looked at it, pulse."""));

    /** Whether a completed trade makes the trader's eyes swirl. */
    public static final ConfigValue<Boolean> SWIRL_ON_TRADE = add(ConfigValue.bool(
            SECTION_BEHAVIORS, "swirlOnTrade", true, """
            Whether a villager's or wandering trader's eyes swirl when a trade completes."""));

    /** Whether healing makes an eyed mob's eyes swirl. */
    public static final ConfigValue<Boolean> SWIRL_ON_HEAL = add(ConfigValue.bool(
            SECTION_BEHAVIORS, "swirlOnHeal", true, """
            Whether an eyed mob's eyes, or a held Googly Eye, swirl when it, or its holder, is healed."""));

    /** Minimum ticks between one mob's healing swirls. */
    public static final ConfigValue<Integer> SWIRL_HEAL_COOLDOWN_TICKS = add(ConfigValue.integer(
            SECTION_BEHAVIORS, "swirlHealCooldownTicks", 200, TICKS_MIN, TICKS_MAX, """
            Minimum ticks between one mob's, or one held eye's, healing swirls."""));

    /** Whether {@code /sg spawnall} may run. */
    public static final ConfigValue<Boolean> ALLOW_SPAWN_ALL = add(ConfigValue.bool(
            SECTION_PICKER, "allowSpawnAll", false, """
            Enables /sg spawnall, which force-spawns every summonable living mob in a grid with no undo.
            WARNING: only enable this on a test or throwaway world. Spawning a mob outside its own mod's
            normal context, as spawnall does, can corrupt or destabilize the world; MineColonies and
            Create mobs are known cases (see spawnExcludedMods below)."""));

    /** Namespaces the spawn commands skip. */
    public static final ConfigValue.Parsed<Set<String>> SPAWN_EXCLUDED_MODS = add(ConfigValue.parsedStrings(
            SECTION_PICKER, "spawnExcludedMods", List.of("minecolonies", "create"), ServerConfig::validateNamespace, Set::copyOf, """
            Namespaces that /sg spawn and /sg spawnall skip, one quoted entry per line, e.g. "mekanism".
            MineColonies and Create are excluded by default because their mobs can corrupt or
            destabilize a world if force-spawned outside their mod's normal context. Only remove either
            entry on a world you're prepared to lose. These are authoring commands only; nothing here
            changes eye eligibility or natural spawning."""));

    /** Entity ids the spawn commands skip. */
    public static final ConfigValue.Parsed<Set<String>> SPAWN_EXCLUDED_ENTITIES = add(ConfigValue.parsedStrings(
            SECTION_PICKER, "spawnExcludedEntities", List.of(), ServerConfig::validateResourceLocation, Set::copyOf, """
            Entity ids that /sg spawn and /sg spawnall skip, one quoted entry per line, e.g.
            "minecraft:armor_stand". Same authoring-only scope as spawnExcludedMods."""));

    /** One parsed override line. Exact entries match by string equality; wildcard entries by regex. */
    private record SpawnOverride(boolean exact, String literalId, Pattern pattern, int percent) {
    }

    private static <V extends ConfigValue<?>> V add(V setting) {
        SETTINGS.add(setting);
        return setting;
    }

    /** The expressions the default {@link #AMBIENT_BEHAVIOR_POOL} names. */
    public static List<EyeBehavior> defaultBehaviors() {
        return parseBehaviorPool(AMBIENT_BEHAVIOR_POOL.defaultValue());
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
        SETTINGS.forEach(ConfigValue::reset);
    }

    /** Every setting, in the order and sections of the server config file. */
    public static List<ConfigValue<?>> settings() {
        return Collections.unmodifiableList(SETTINGS);
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
        // A namespace is valid exactly when it builds a location with any valid path.
        return !value.isEmpty() && ResourceLocation.tryBuild(value, "x") != null;
    }

    /** Whether a config string is a well-formed override line ({@link #parse}); the entry guard for {@link #ENTITY_OVERRIDES}. */
    public static boolean validateOverride(String value) {
        return parse(value) != null;
    }
}
