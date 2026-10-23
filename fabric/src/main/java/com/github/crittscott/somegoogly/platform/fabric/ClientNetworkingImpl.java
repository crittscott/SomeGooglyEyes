package com.github.crittscott.somegoogly.platform.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Fabric serverbound sends. */
public final class ClientNetworkingImpl {

    private ClientNetworkingImpl() {
    }

    public static boolean canSendToServer(ResourceLocation id) {
        return ClientPlayNetworking.canSend(id);
    }

    public static void sendToServer(ResourceLocation id, FriendlyByteBuf buffer) {
        ClientPlayNetworking.send(id, buffer);
    }
}
