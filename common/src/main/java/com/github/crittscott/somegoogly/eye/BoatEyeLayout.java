package com.github.crittscott.somegoogly.eye;

import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.Variant;
import com.github.crittscott.somegoogly.config.EyeConfigModel;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractBoat;

/** Vanilla boat eligibility and hull/raft attachment selection, shared by both sides. */
public final class BoatEyeLayout {

    public static final String HULL = "hull";
    public static final String RAFT = "raft";
    public static final Set<EntityType<? extends AbstractBoat>> TYPES = Set.of(
            EntityType.ACACIA_BOAT,
            EntityType.ACACIA_CHEST_BOAT,
            EntityType.BAMBOO_CHEST_RAFT,
            EntityType.BAMBOO_RAFT,
            EntityType.BIRCH_BOAT,
            EntityType.BIRCH_CHEST_BOAT,
            EntityType.CHERRY_BOAT,
            EntityType.CHERRY_CHEST_BOAT,
            EntityType.DARK_OAK_BOAT,
            EntityType.DARK_OAK_CHEST_BOAT,
            EntityType.JUNGLE_BOAT,
            EntityType.JUNGLE_CHEST_BOAT,
            EntityType.MANGROVE_BOAT,
            EntityType.MANGROVE_CHEST_BOAT,
            EntityType.OAK_BOAT,
            EntityType.OAK_CHEST_BOAT,
            EntityType.PALE_OAK_BOAT,
            EntityType.PALE_OAK_CHEST_BOAT,
            EntityType.SPRUCE_BOAT,
            EntityType.SPRUCE_CHEST_BOAT);

    private BoatEyeLayout() {
    }

    /** Both tokens use the hull model's composed space, not an animated paddle or cargo part. */
    public static String attachment(AbstractBoat boat) {
        return boat.getType() == EntityType.BAMBOO_RAFT || boat.getType() == EntityType.BAMBOO_CHEST_RAFT
                ? RAFT : HULL;
    }

    public static boolean isEligible(AbstractBoat boat, @Nullable RuntimeConfig config) {
        return supports(boat) && RuntimeConfig.isUsable(config)
                && config.variants.stream().anyMatch(variant -> matches(variant, attachment(boat)));
    }

    private static boolean matches(Variant variant, String token) {
        return variant.weight > 0 && variant.heads.stream()
                .anyMatch(head -> token.equals(head.attachPoint) && !head.eyes.isEmpty());
    }

    /** Weighted selection considers only variants containing the matching boat geometry. */
    public static HeadInfo resolve(AbstractBoat boat, @Nullable RuntimeConfig config, float roll) {
        String token = attachment(boat);
        return new HeadInfo(config, EyeConfigModel.chooseVariantIndex(config, roll,
                variant -> matches(variant, token)), token);
    }

    public static boolean supports(Entity entity) {
        return entity instanceof AbstractBoat && TYPES.contains(entity.getType());
    }
}
