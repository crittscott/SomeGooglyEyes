package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.PresentationEyes;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

/** Test-source facades for driving the production runtime clocks without a rendered client. */
final class TestEyeRuntime {
    private TestEyeRuntime() {}

    static final class BoatCache {
        BoatCache() {
            ClientEyeRuntime.clear();
        }

        void clear() {
            ClientEyeRuntime.clear();
        }

        @Nullable
        PresentationEyes find(Boat boat) {
            return ClientEyeRuntime.boatEyes(boat);
        }

        /** Resolve a supported fixture, failing at setup when its presentation is absent. */
        PresentationEyes get(Boat boat) {
            return Objects.requireNonNull(find(boat), "supported fixture must have a presentation");
        }

        void tick(long time, BehaviorSettings settings) {
            ClientEyeRuntime.tickWorld(time, settings);
        }
    }

    static final class GuiCache {
        GuiCache() {
            ClientEyeRuntime.clear();
        }

        void clear() {
            ClientEyeRuntime.clear();
        }

        @Nullable
        PresentationEyes find(ClientEyeRuntime.GuiKey key, ItemStack stack) {
            return ClientEyeRuntime.guiEyes(key, stack);
        }

        /** Resolve a supported fixture, failing at setup when its presentation is absent. */
        PresentationEyes get(ClientEyeRuntime.GuiKey key, ItemStack stack) {
            return Objects.requireNonNull(
                    find(key, stack), "supported fixture must have a presentation");
        }

        void tick(@Nullable Object view, BehaviorSettings settings) {
            ClientEyeRuntime.tickGui(view, settings);
        }
    }

    static final class HandCache {
        HandCache() {
            ClientEyeRuntime.clear();
        }

        void clear() {
            ClientEyeRuntime.clear();
        }

        @Nullable
        PresentationEyes find(LivingEntity holder, InteractionHand hand, ItemStack stack) {
            holder.setItemInHand(hand, stack);
            return ClientEyeRuntime.handEyes(holder, hand, stack);
        }

        /** Resolve a supported fixture, failing at setup when its presentation is absent. */
        PresentationEyes get(LivingEntity holder, InteractionHand hand, ItemStack stack) {
            return Objects.requireNonNull(
                    find(holder, hand, stack), "supported fixture must have a presentation");
        }

        void remove(UUID holder) {
            ClientEyeRuntime.removeHands(holder);
        }

        void tick(long time, boolean simulate, BehaviorSettings settings) {
            if (simulate) ClientEyeRuntime.tickWorld(time, settings);
        }
    }

    /** Build a supported presentation fixture, failing at setup if its layout cannot be created. */
    static PresentationEyes presentation(UUID id, ItemStack stack) {
        return Objects.requireNonNull(
                PresentationEyes.create(id, stack), "supported presentation fixture must resolve");
    }
}
