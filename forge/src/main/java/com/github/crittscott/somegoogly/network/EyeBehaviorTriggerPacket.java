package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehaviors;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server → client: "play behavior {@code behaviorId} on entity {@code entityId} for {@code duration}
 * ticks, seeded with {@code seed}." Sent to a mob's trackers when the server scheduler starts a behavior
 * (and to a newly-tracking player mid-effect, with the remaining duration), so every viewer animates the
 * same thing in lock-step. Purely transient — the trigger is the only thing sent; the client runs the
 * animation locally.
 *
 * <p>The client drops the trigger if it has no tracker for the mob yet (not currently rendering it) or if
 * a behavior is already playing — the same "one at a time, non-interruptable" rule the server enforces.
 */
public class EyeBehaviorTriggerPacket {

    private final ResourceLocation behaviorId;
    private final int duration;
    private final int elapsed;
    private final int entityId;
    private final long seed;

    public EyeBehaviorTriggerPacket(int entityId, ResourceLocation behaviorId, int duration, long seed, int elapsed) {
        this.entityId = entityId;
        this.behaviorId = behaviorId;
        this.duration = duration;
        this.seed = seed;
        this.elapsed = elapsed;
    }

    public static EyeBehaviorTriggerPacket decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        ResourceLocation behaviorId = buffer.readResourceLocation();
        int duration = buffer.readVarInt();
        long seed = buffer.readLong();
        int elapsed = buffer.readVarInt();
        return new EyeBehaviorTriggerPacket(entityId, behaviorId, duration, seed, elapsed);
    }

    public static void encode(EyeBehaviorTriggerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId);
        buffer.writeResourceLocation(packet.behaviorId);
        buffer.writeVarInt(packet.duration);
        buffer.writeLong(packet.seed);
        buffer.writeVarInt(packet.elapsed);
    }

    public static void handle(EyeBehaviorTriggerPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                // Client-only: start the behavior on the mob's tracker. Guarded so the dedicated server
                // never class-loads client classes via this path.
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> play(packet)));
        context.setPacketHandled(true);
    }

    private static void play(EyeBehaviorTriggerPacket packet) {
        EyeBehavior behavior = EyeBehaviors.byId(packet.behaviorId);
        if (behavior == null) {
            return; // unknown id (build skew)
        }
        net.minecraft.client.multiplayer.ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Entity entity = level.getEntity(packet.entityId);
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        // Only act if a tracker already exists (the mob is being rendered). No tracker → drop: an
        // off-screen mob has nothing to animate, and the server still counts it busy for the duration.
        GooglyTracker tracker = SomeGoogly.clientEventHandler.peekGooglyTracker(living);
        if (tracker != null) {
            tracker.startBehavior(behavior, packet.duration, packet.seed, packet.elapsed);
        }
    }
}
