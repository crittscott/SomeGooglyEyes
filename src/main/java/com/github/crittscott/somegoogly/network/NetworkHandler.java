package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGoogly;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "2";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SomeGoogly.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;

        INSTANCE.registerMessage(
                id++,
                EyeStatePacket.class,
                EyeStatePacket::encode,
                EyeStatePacket::decode,
                EyeStatePacket::handle
        );

        INSTANCE.registerMessage(
                id++,
                EyeConfigSyncPacket.class,
                EyeConfigSyncPacket::encode,
                EyeConfigSyncPacket::decode,
                EyeConfigSyncPacket::handle
        );
    }
}