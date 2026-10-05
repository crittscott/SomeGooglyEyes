package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.registry.ContentRegistrar;

/**
 * Item registry for the mod: the {@link GooglyEyeItem} (the eye itself, an ingredient) and the
 * {@link SlimyEyeItem} it crafts into (the applicator). Both carry the same appearance component.
 */
public final class ModItems {

    public static final ContentRegistrar.Handle<GooglyEyeItem> GOOGLY_EYE = new ContentRegistrar.Handle<>();
    public static final ContentRegistrar.Handle<SlimyEyeItem> SLIMY_EYE = new ContentRegistrar.Handle<>();

    private ModItems() {
    }

    /** Both items are shown in the mod's own creative tab ({@code ModCreativeTabs}), not a vanilla one. */
    public static void register(ContentRegistrar registrar) {
        GOOGLY_EYE.bind(registrar.registerItem("googly_eye", GooglyEyeItem::new));
        SLIMY_EYE.bind(registrar.registerItem("slimy_eye", SlimyEyeItem::new));
    }
}
