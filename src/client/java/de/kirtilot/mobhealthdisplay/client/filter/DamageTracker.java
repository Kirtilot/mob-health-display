package de.kirtilot.mobhealthdisplay.client.filter;

import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Remembers when each entity last lost health, for "Hide After".
 * Weak keys: entries disappear automatically once the entity is gone.
 */
public final class DamageTracker {
    private record Entry(float health, long lastDamageMillis) {}

    private static final Map<LivingEntity, Entry> ENTRIES = new WeakHashMap<>();

    private DamageTracker() {}

    /** Called every frame for every visible living entity. */
    public static void update(LivingEntity entity) {
        float health = entity.getHealth();
        Entry old = ENTRIES.get(entity);
        if (old == null) {
            ENTRIES.put(entity, new Entry(health, entity.hurtTime > 0 ? now() : 0L));
            return;
        }
        // hurtTime > 0 while the red hurt flash is playing
        boolean damaged = health < old.health() || entity.hurtTime > 0;
        if (damaged || health != old.health()) {
            ENTRIES.put(entity, new Entry(health, damaged ? now() : old.lastDamageMillis()));
        }
    }

    public static boolean damagedWithin(LivingEntity entity, float seconds) {
        Entry entry = ENTRIES.get(entity);
        return entry != null && entry.lastDamageMillis() > 0
                && now() - entry.lastDamageMillis() <= (long) (seconds * 1000);
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}