package com.github.crittscott.somegoogly.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.function.Supplier;

public class GooglyEyePacket {
    private final int entityId;
    private final boolean hasGooglyEyes;
    private static final Marker MARK = MarkerManager.getMarker(GooglyEyePacket.class.getSimpleName());

    public GooglyEyePacket(int entityId, boolean hasGooglyEyes) {
        this.entityId = entityId;
        this.hasGooglyEyes = hasGooglyEyes;
    }

    public static void encode(GooglyEyePacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId);
        buffer.writeBoolean(packet.hasGooglyEyes);
    }

    public static GooglyEyePacket decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        boolean hasGooglyEyes = buffer.readBoolean();
        return new GooglyEyePacket(entityId, hasGooglyEyes);
    }

    public static void handle(GooglyEyePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {

            if (Minecraft.getInstance().level != null) {
                Entity entity = Minecraft.getInstance().level.getEntity(packet.entityId);
                if (entity instanceof LivingEntity living) {
                    living.getPersistentData().putBoolean("somegoogly:hasGooglyEyes", packet.hasGooglyEyes);
                }
            }
        });
        context.setPacketHandled(true);
    }
}