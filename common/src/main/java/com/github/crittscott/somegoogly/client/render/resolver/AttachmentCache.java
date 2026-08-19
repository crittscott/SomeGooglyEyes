package com.github.crittscott.somegoogly.client.render.resolver;

import net.minecraft.client.model.EntityModel;

/**
 * Attach points resolved once per (model instance, token) and replayed every frame, shared by every
 * {@link EyeAttachmentResolver}. Entries live as long as their model does: a datapack reload must
 * <b>not</b> clear this (new configs change which token is asked for, not what a token names inside a
 * model), while a resource reload must (see the platform-specific resolver dispatch, which owns the
 * concrete resolver list and calls {@link ModelMemo#clear()} on a resource reload).
 */
final class AttachmentCache {
    static final ModelMemo<EntityModel<?>, Attachment> ATTACHMENTS = new ModelMemo<>();

    private AttachmentCache() {
    }
}
