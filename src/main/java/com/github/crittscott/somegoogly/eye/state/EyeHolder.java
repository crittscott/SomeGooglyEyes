package com.github.crittscott.somegoogly.eye.state;

/**
 * A thing that can carry eyes — the attachment seam between an eye item and whatever it's applied to.
 *
 * <p>The only concrete holder today is {@link EntityEyeHolder} (a mob, backed by {@link EyeState}).
 * Harvest/reattach logic is written against this interface, not {@code EyeState} directly, so future
 * holders slot in without touching that logic.
 *
 * <p>FUTURE holders behind this same seam:
 * <ul>
 *   <li>an item frame's framed stack (render eyes on an eye item displayed in a frame),</li>
 *   <li>a held/dropped eye-item stack (render the eye on the item model itself),</li>
 *   <li>a mob head block/item that "remembers" the eyes it dropped with.</li>
 * </ul>
 * Each would store/read its {@link AppearanceOverride} from its own backing (stack NBT, block entity, …)
 * while reusing the same harvest/reattach verbs.
 */
public interface EyeHolder {

    /** Whether eyes are currently present/shown on this holder. */
    boolean hasEyes();

    /** Turn the eyes on/off (for a mob this is the spawn/harvest flag; for an item, presence). */
    void setHasEyes(boolean hasEyes);

    /** The holder's current appearance ({@link AppearanceOverride#EMPTY} when none). */
    AppearanceOverride getEyeProperties();

    /** Replace the holder's appearance. */
    void setEyeProperties(AppearanceOverride properties);
}
