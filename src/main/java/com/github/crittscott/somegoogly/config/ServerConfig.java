package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehaviors;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

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
 * {@code ServerEventHandler}); this class decides <i>how often</i> an eligible entity actually rolls
 * eyes. Also hosts the picker's opt-in gate for the destructive {@code /sg spawnall} grid
 * ({@link #ALLOW_SPAWN_ALL}, enforced server-side in {@code PickerSpawnAllPacket}).
 */
public class ServerConfig {
    public static final ForgeConfigSpec.BooleanValue ALLOW_SPAWN_ALL;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> AMBIENT_BEHAVIOR_POOL;
    public static final ForgeConfigSpec.BooleanValue AMBIENT_BEHAVIORS;
    public static final ForgeConfigSpec.IntValue AMBIENT_MAX_TICKS;
    public static final ForgeConfigSpec.IntValue AMBIENT_MIN_TICKS;
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_OVERRIDES;
    public static final ForgeConfigSpec.IntValue GLOBAL_PERCENT;
    public static final ForgeConfigSpec.BooleanValue GOOGLY_EYES_ENABLED;
    public static final ForgeConfigSpec.IntValue GROW_ON_HIT_PERCENT;
    public static final ForgeConfigSpec.IntValue HARVEST_ON_KILL_PERCENT;
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue SWIRL_HEAL_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.BooleanValue SWIRL_ON_HEAL;
    public static final ForgeConfigSpec.BooleanValue SWIRL_ON_TRADE;

    // Compiled view of ENTITY_OVERRIDES, rebuilt whenever the underlying config list instance changes
    // (ForgeConfigSpec hands back a fresh list on (re)load, so identity comparison detects reloads).
    private static List<? extends String> lastParsedSource;
    private static List<Override> overrides = List.of();

    static {
        BUILDER.push("Server Settings");

        GOOGLY_EYES_ENABLED = BUILDER
                .comment("Enable googly eyes globally")
                .define("googlyEyesEnabled", true);

        GLOBAL_PERCENT = BUILDER
                .comment("Default percentage chance for an eligible entity to get googly eyes (overridable per entity below)")
                .defineInRange("globalPercent", 2, 0, 100);

        HARVEST_ON_KILL_PERCENT = BUILDER
                .comment("Percentage chance that an eyed mob killed by a player wielding plain shears drops its googly eyes.",
                        "The Optometrist enchantment bypasses this by letting shears harvest eyes non-lethally on right-click.")
                .defineInRange("harvestOnKillPercent", 25, 0, 100);

        ENTITY_OVERRIDES = BUILDER
                .comment(
                        "Per-entity override chances, one per line as 'entity,percent'.",
                        "The entity id may use '*' as a wildcard, e.g. 'minecraft:zombie,100', 'minecraft:*,5', '*:*_horse,50'.",
                        "Resolution: an exact (wildcard-free) id always wins; otherwise the first matching",
                        "wildcard line wins (so list more specific patterns above broader ones); otherwise globalPercent.")
                .defineList("entityOverrides", ArrayList::new, ServerConfig::validateOverride);

        BUILDER.pop();
        BUILDER.push("Behaviors");

        AMBIENT_BEHAVIORS = BUILDER
                .comment("Idle eye expressions: every so often an eyed, player-tracked mob plays a random",
                        "enabled behavior. Event-driven behaviors (grow on hit, swirl on trade/heal) are",
                        "separate and controlled by their own settings below.")
                .define("ambientBehaviors", true);

        AMBIENT_MIN_TICKS = BUILDER
                .comment("Minimum idle ticks between ambient behaviors on a mob (20 ticks = 1 second).")
                .defineInRange("ambientMinTicks", 60, 1, 24000);

        AMBIENT_MAX_TICKS = BUILDER
                .comment("Maximum idle ticks between ambient behaviors on a mob (20 ticks = 1 second).")
                .defineInRange("ambientMaxTicks", 200, 1, 24000);

        AMBIENT_BEHAVIOR_POOL = BUILDER
                .comment("Which behaviors the idle timer may play, by id. Remove a line to drop it from the",
                        "ambient pool. Event-driven behaviors (grow on hit, swirl on trade/heal) are not in this",
                        "pool and are controlled by their own settings below; color_change ships in neither.")
                .defineList("ambientBehaviorPool", defaultAmbientBehaviorIds(), ServerConfig::validateBehaviorId);

        GROW_ON_HIT_PERCENT = BUILDER
                .comment("Percentage chance an eyed mob plays the 'grow' behavior when a player damages it.",
                        "0 disables grow-on-hit. Only fires while the mob is tracked by a player (i.e. visible).")
                .defineInRange("growOnHitPercent", 20, 0, 100);

        SWIRL_ON_TRADE = BUILDER
                .comment("When true, completing a trade with an eyed villager (or wandering trader) plays 'swirl'.")
                .define("swirlOnTrade", true);

        SWIRL_ON_HEAL = BUILDER
                .comment("When true, an eyed mob being healed plays 'swirl' (rate-limited by the cooldown below).")
                .define("swirlOnHeal", true);

        SWIRL_HEAL_COOLDOWN_TICKS = BUILDER
                .comment("Minimum ticks between heal-triggered swirls on a single mob (20 ticks = 1 second).",
                        "Keep this comfortably above the swirl animation length so a regenerating mob doesn't",
                        "swirl back-to-back.")
                .defineInRange("swirlHealCooldownTicks", 200, 1, 24000);

        BUILDER.pop();
        BUILDER.push("Picker");

        ALLOW_SPAWN_ALL = BUILDER
                .comment("Allow creative players to use /sg spawnall, which spawns one of every living",
                        "entity type in a grid and freely overwrites blocks with sandstone platforms.",
                        "No undo — enable only on throwaway authoring worlds.")
                .define("allowSpawnAll", false);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    /** One parsed override line. Exact entries match by string equality; wildcard entries by regex. */
    private record Override(boolean exact, String literalId, Pattern pattern, int percent) {
    }

    /** The behaviors the ambient idle timer plays by default: the ambient set only (not the event-driven
     *  grow/swirl, nor the dormant color_change). */
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

    /** Parse one 'pattern,percent' line into an {@link Override}, or null if malformed (validation
     *  already ran, but reloads can still surface bad entries, so we stay defensive). */
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
        List<? extends String> source = ENTITY_OVERRIDES.get();
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

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SPEC);
    }

    private static boolean validateBehaviorId(Object o) {
        return o instanceof String s && ResourceLocation.tryParse(s) != null;
    }

    private static boolean validateOverride(Object o) {
        if (!(o instanceof String s)) {
            return false;
        }
        String[] split = s.split(",");
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
