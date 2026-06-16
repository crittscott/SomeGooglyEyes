package com.github.crittscott.somegoogly.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Server-authoritative spawn settings. The googly-eye chance lives here (not in the bundled eye
 * datapack), so admins can tune how common eyes are — globally and per entity — without editing the
 * 100+ shipped config files. The eye JSONs only decide <i>where</i> eyes go and whether an entity is
 * eligible at all (their {@code enabled} flag is an authoritative hard on/off, applied in
 * {@code ServerEventHandler}); this class decides <i>how often</i> an eligible entity actually rolls
 * eyes.
 */
public class ServerConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue GOOGLY_EYES_ENABLED;
    public static final ForgeConfigSpec.IntValue GLOBAL_PERCENT;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_OVERRIDES;

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

        ENTITY_OVERRIDES = BUILDER
                .comment(
                        "Per-entity override chances, one per line as 'entity,percent'.",
                        "The entity id may use '*' as a wildcard, e.g. 'minecraft:zombie,100', 'minecraft:*,5', '*:*_horse,50'.",
                        "Resolution: an exact (wildcard-free) id always wins; otherwise the first matching",
                        "wildcard line wins (so list more specific patterns above broader ones); otherwise globalPercent.")
                .defineList("entityOverrides", ArrayList::new, ServerConfig::validateOverride);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SPEC);
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

    /** One parsed override line. Exact entries match by string equality; wildcard entries by regex. */
    private record Override(boolean exact, String literalId, Pattern pattern, int percent) {
    }
}
