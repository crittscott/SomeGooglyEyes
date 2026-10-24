package com.github.crittscott.somegoogly.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * One of the mod's five payloads: its versioned id (from {@link NetworkHandler}) and its body. Each payload
 * class pairs this with a static {@code read(FriendlyByteBuf)} that validates on decode; the loader bridges
 * {@link com.github.crittscott.somegoogly.platform.Networking} and
 * {@link com.github.crittscott.somegoogly.platform.ClientNetworking} send it on that id.
 */
public interface Payload {

    ResourceLocation id();

    void write(FriendlyByteBuf buffer);
}
