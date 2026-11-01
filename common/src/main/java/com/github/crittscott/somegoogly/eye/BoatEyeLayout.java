package com.github.crittscott.somegoogly.eye;

import com.github.crittscott.somegoogly.config.EyeConfigModel;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.Variant;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;

import java.util.Set;
import javax.annotation.Nullable;

/** Vanilla boat eligibility and hull/raft attachment selection, shared by both sides. */
public final class BoatEyeLayout {

    public static final String HULL = "hull";
    public static final String RAFT = "raft";
    private static final Set<Boat.Type> TYPES = Set.of(Boat.Type.ACACIA, Boat.Type.BAMBOO,
            Boat.Type.BIRCH, Boat.Type.CHERRY, Boat.Type.DARK_OAK, Boat.Type.JUNGLE,
            Boat.Type.MANGROVE, Boat.Type.OAK, Boat.Type.SPRUCE);

    private BoatEyeLayout() {
    }

    /** Both tokens use the hull model's composed space, not an animated paddle or cargo part. */
    public static String attachment(Boat boat) {
        return boat.getVariant() == Boat.Type.BAMBOO ? RAFT : HULL;
    }

    public static boolean isEligible(Boat boat, @Nullable RuntimeConfig config) {
        return supports(boat) && RuntimeConfig.isUsable(config)
                && config.variants.stream().anyMatch(variant -> matches(variant, attachment(boat)));
    }

    private static boolean matches(Variant variant, String token) {
        return variant.weight > 0 && variant.heads.stream()
                .anyMatch(head -> token.equals(head.attachPoint) && !head.eyes.isEmpty());
    }

    /** Weighted selection considers only variants with a usable attachment for this boat's geometry. */
    public static HeadInfo resolve(Boat boat, @Nullable RuntimeConfig config, float roll) {
        String token = attachment(boat);
        return new HeadInfo(config, EyeConfigModel.chooseVariantIndex(config, roll,
                variant -> matches(variant, token)), token);
    }

    /** Registry and wood checks exclude modded entities and loader-added boat variants. */
    public static boolean supports(Entity entity) {
        return entity instanceof Boat boat
                && (boat.getType() == EntityType.BOAT || boat.getType() == EntityType.CHEST_BOAT)
                && TYPES.contains(boat.getVariant());
    }
}
