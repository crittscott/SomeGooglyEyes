package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.PresentationEyes;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Test-source facades for driving the production runtime's caches and clocks without a rendered client. */
final class TestEyeRuntime {

    private TestEyeRuntime() {
    }

    static final class BoatCache {
        BoatCache() {
            ClientEyeRuntime.clear();
        }

        void clear() {
            ClientEyeRuntime.clear();
        }

        PresentationEyes get(AbstractBoat boat) {
            return ClientEyeRuntime.boatEyes(boat);
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

        PresentationEyes get(ClientEyeRuntime.GuiKey key, ItemStack stack) {
            return ClientEyeRuntime.guiEyes(key, stack);
        }

        void tick(Object view, BehaviorSettings settings) {
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

        PresentationEyes get(LivingEntity holder, InteractionHand hand, ItemStack stack) {
            holder.setItemInHand(hand, stack);
            return ClientEyeRuntime.handEyes(holder, hand, stack);
        }

        void remove(UUID holder) {
            ClientEyeRuntime.removeHands(holder);
        }

        void tick(long time, boolean simulate, BehaviorSettings settings) {
            if (simulate) {
                ClientEyeRuntime.tickWorld(time, settings);
            }
        }
    }
}
