package de.kirtilot.mobhealthdisplay.client.filter;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/**
 * Health as we display it.
 *
 * Vanilla bug: when an entity comes into view, the server only sends values that differ from the
 * default, and the default health is 1.0. So an entity with exactly 1 HP (e.g. after poison)
 * arrives without a health value and the client keeps full health.
 * If the server never sent a health value for an entity, its real health must be 1.0.
 */
public final class HealthReader {
    private HealthReader() {}

    public static float health(LivingEntity entity) {
        if (entity == Minecraft.getInstance().player) return entity.getHealth(); // own health comes by its own packet
        if (entity instanceof HealthSyncState sync && !sync.mhd$healthReceived()) {
            return Math.min(1.0f, entity.getMaxHealth());
        }
        return entity.getHealth();
    }
}