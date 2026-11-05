package de.kirtilot.mobhealthdisplay.client.filter;

import de.kirtilot.mobhealthdisplay.client.config.MhiConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class EntityFilter {

    public static boolean shouldShow(LivingEntity entity, Entity viewer) {
        MhiConfig cfg = MhiConfig.get();
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

        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        if (cfg.whiteList && !cfg.whiteListEntries.contains(id)) return false;
        if (cfg.blackList && cfg.blackListEntries.contains(id)) return false;

        return true;
    }
}