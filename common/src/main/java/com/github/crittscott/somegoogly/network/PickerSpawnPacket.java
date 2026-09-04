package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.picker.PickerGate;
import com.github.crittscott.somegoogly.picker.PickerSpawnService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

/**
 * Client → server: "spawn one {@code entityTypeId} at the block I'm looking at" — the wire half of
 * {@code /sg spawn}. Creative-gated ({@link PickerGate}); placement, fit-checking, and feedback
 * are {@link PickerSpawnService#spawnOne} on the server thread. The id is validated against the entity
 * registry here ({@code ENTITY_TYPE} is a defaulted registry, so an unchecked lookup would silently
 * hand back a pig for garbage input).
 */
public class PickerSpawnPacket {

    private final ResourceLocation typeId;

    public PickerSpawnPacket(ResourceLocation typeId) {
        this.typeId = typeId;
    }

    public static PickerSpawnPacket decode(FriendlyByteBuf buffer) {
        return new PickerSpawnPacket(buffer.readResourceLocation());
    }

    public static void encode(PickerSpawnPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.typeId);
    }

    public static void handle(PickerSpawnPacket packet, NetworkTransport.Context context) {
        context.queue(() -> {
            ServerPlayer sender = context.player();
            if (!PickerGate.creative(sender)) {
                return;
            }
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(packet.typeId)) {
                sender.sendSystemMessage(Component.translatable("somegoogly.command.picker.unknown_entity_type", packet.typeId));
                return;
            }
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(packet.typeId);
            PickerSpawnService.spawnOne(sender, type);
        });
    }
}
