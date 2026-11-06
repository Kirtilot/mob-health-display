package de.kirtilot.mobhealthdisplay.client.filter;

import de.kirtilot.mobhealthdisplay.client.config.MhdConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class EntityFilter {

    /** Remembers per entity type whether it has health, so each type is only checked once. */
    private static final Map<EntityType<?>, Boolean> HAS_HEALTH = new HashMap<>();

    public static boolean shouldShow(LivingEntity entity, Entity viewer) {
        DamageTracker.update(entity); // always track, even if hidden right now

        MhdConfig cfg = MhdConfig.get();
        if (!cfg.showHearts || !entity.isAlive()) return false;

        if (entity == viewer) {
            if (!cfg.showSelf) return false;
        } else {
            EntityCategory category = EntityCategory.of(entity);
            if (category.isHostileGroup() && !cfg.showHostile) return false;
            if ((category == EntityCategory.PASSIVE || category == EntityCategory.TAMED)
                    && !cfg.showPassive) return false;
        }

        if (cfg.onlyShowDamaged && entity.getHealth() >= entity.getMaxHealth()) return false;
        if (cfg.hideAfterSeconds > 0 && !DamageTracker.damagedWithin(entity, cfg.hideAfterSeconds)) return false;

        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        if (cfg.whiteList && !cfg.whiteListEntries.contains(id)) return false;
        if (cfg.blackList && cfg.blackListEntries.contains(id)) return false;

        return true;
    }

    /**
     * True for entity types that have health, so boats, arrows and items are left out of lists.
     * Creates one test entity per type (never added to the world) and checks if it is a LivingEntity.
     */
    public static boolean hasHealth(EntityType<?> type) {
        if (type == EntityTypes.PLAYER) return true; // players cannot be created this way
        Level level = Minecraft.getInstance().level;
        if (level == null) return true; // no world loaded: cannot check, so show everything

        return HAS_HEALTH.computeIfAbsent(type, t -> {
            try {
                return t.create(level, EntitySpawnReason.LOAD) instanceof LivingEntity;
            } catch (Throwable e) {
                return false; // some special entities cannot be created on their own
            }
        });
    }
}