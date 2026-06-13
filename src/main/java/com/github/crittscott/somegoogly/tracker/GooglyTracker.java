package com.github.crittscott.somegoogly.tracker;

import com.github.crittscott.somegoogly.head.HeadInfo;
import com.github.crittscott.somegoogly.event.ClientEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;
import java.util.Random;

public class GooglyTracker {
    public final LivingEntity parent;
    public final HeadInfo helper;
    public final Random rand;

    public boolean shouldUpdate = true;
    public int lastUpdateRequest;

    public double motionX;
    public double motionY;
    public double motionZ;

    private double prevX, prevY, prevZ;

    public EyeInfo[][] eyes;

    public static class EyeInfo {
        public float prevRotationYaw;
        public float rotationYaw;
        public float prevRotationPitch;
        public float rotationPitch;
        public float prevRotationRoll;
        public float rotationRoll;

        public float prevDeltaX;
        public float prevDeltaY;
        public float deltaX;
        public float deltaY;
        public float momentumX;
        public float momentumY;

        public EyeInfo() {
            prevDeltaY = deltaY = -1F;
        }

        public void update(HeadInfo helper, int head, int eye, GooglyTracker parent, double motionX, double motionY, double motionZ) {
            prevRotationYaw = rotationYaw;
            prevRotationPitch = rotationPitch;
            prevRotationRoll = rotationRoll;

            // Get rotation values directly from the entity instead of through helper methods
            rotationYaw = parent.parent.getYHeadRot();
            rotationPitch = parent.parent.getXRot();
            rotationRoll = 0.0f; // Most entities don't roll

            prevDeltaX = deltaX;
            prevDeltaY = deltaY;

            float yawDiff = rotationYaw - prevRotationYaw;
            float pitchDiff = rotationPitch - prevRotationPitch;
            float rollDiff = rotationRoll - prevRotationRoll;

            momentumY += motionY * 1.5F + (motionX + motionZ) * parent.rand.nextGaussian() * (0.75F) + (pitchDiff / 45F) + (yawDiff / 180F) + rollDiff * parent.rand.nextGaussian() * (0.05F);
            momentumX -= (motionX + motionZ) * parent.rand.nextGaussian() * 0.4F + (yawDiff / 45F) + rollDiff * parent.rand.nextGaussian() * (0.05F);

            float momentumLoss = 0.9F;
            float newDeltaX = deltaX + momentumX;
            float newDeltaY = deltaY + momentumY;
            if (newDeltaX < -1F || newDeltaX > 1F) {
                float newMo = momentumX * -momentumLoss;
                float randFloat = 0.8F + parent.rand.nextFloat() * 0.2F;
                momentumX = newMo * randFloat;
                momentumY += newMo * (randFloat) * (parent.rand.nextFloat() > 0.5F ? 1F : -1F);
            }
            if (newDeltaY < -1F || newDeltaY > 1F) {
                float newMo = momentumY * -momentumLoss;
                float randFloat = 0.8F + parent.rand.nextFloat() * 0.2F;
                momentumY = newMo * randFloat;
                momentumX += newMo * (1F - randFloat) * (parent.rand.nextFloat() > 0.5F ? 1F : -1F);
            } else {
                momentumY -= Mth.clamp(1F + deltaY, 0F, 0.1999F);
            }

            momentumX *= 0.95F;
            deltaX *= 0.95F;

            if (Math.abs(momentumX) < 0.03F) {
                momentumX = 0F;
            }
            if (Math.abs(deltaX) < 0.03F) {
                deltaX = 0F;
            }

            float maxMomentum = 1.3F;
            momentumX = Mth.clamp(momentumX, -maxMomentum, maxMomentum);
            momentumY = Mth.clamp(momentumY, -maxMomentum, maxMomentum);

            deltaX += momentumX;
            deltaY += momentumY;
            deltaX = Mth.clamp(deltaX, -1F, 1F);
            deltaY = Mth.clamp(deltaY, -1F, 1F);
        }
    }

    public GooglyTracker(@Nonnull LivingEntity parent, @Nonnull HeadInfo helper) {
        this.parent = parent;
        this.helper = helper;
        this.rand = new Random(Math.abs(parent.getUUID().hashCode()) * 8134L);
        this.eyes = new EyeInfo[helper.getHeadCount(parent)][];

        for (int i = 0; i < eyes.length; i++) {
            this.eyes[i] = new EyeInfo[helper.getHeadInfo(parent, i).getEyeCount(parent)];
            for (int i1 = 0; i1 < this.eyes[i].length; i1++) {
                this.eyes[i][i1] = new EyeInfo();
            }
        }

        this.prevX = parent.getX();
        this.prevY = parent.getY();
        this.prevZ = parent.getZ();
        update();
    }

    public void update() {
        if (!shouldUpdate) {
            return;
        }
        shouldUpdate = false;

        motionX = parent.getX() - prevX;
        motionY = parent.getY() - prevY;
        motionZ = parent.getZ() - prevZ;

        prevX = parent.getX();
        prevY = parent.getY();
        prevZ = parent.getZ();

        if (helper.multiModel != null) {
            EntityRenderer<?> render = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(parent);
            if (!(render instanceof LivingEntityRenderer)) {
                return;
            }
            LivingEntityRenderer<?, ?> renderer = (LivingEntityRenderer<?, ?>) render;

            if (!helper.setup(parent, renderer)) {
                return;
            }
        }

        for (int i = 0; i < eyes.length; i++) {
            HeadInfo childInfo = helper.getHeadInfo(parent, i);

            for (int i1 = 0; i1 < eyes[i].length; i1++) {
                eyes[i][i1].update(childInfo, i, i1, this, motionX, motionY, motionZ);
            }
        }
    }

    public void setLastUpdateRequest() {
        lastUpdateRequest = ClientEventHandler.clientTicks;
    }

    public void requireUpdate() {
        shouldUpdate = true;
    }
}