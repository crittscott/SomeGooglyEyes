package com.github.crittscott.somegoogly.platform.forge;

import com.github.crittscott.somegoogly.network.forge.ForgeNetworkTransport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Forge serverbound sends through the mod's payload channel. */
public final class ClientNetworkingImpl {

    private ClientNetworkingImpl() {
    }

    /** Every payload shares the one channel, so the server accepts {@code id} exactly when it has the channel. */
    public static boolean canSendToServer(ResourceLocation id) {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        return listener != null && ForgeNetworkTransport.isRemotePresent(listener.getConnection());
    }

    public static void sendToServer(ResourceLocation id, FriendlyByteBuf buffer) {
        ForgeNetworkTransport.sendToServer(id, buffer);
    }
}
