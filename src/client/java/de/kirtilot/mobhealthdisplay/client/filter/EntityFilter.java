package de.kirtilot.mobhealthdisplay.client.filter;

import de.kirtilot.mobhealthdisplay.client.config.MhdConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class EntityFilter {

    /** One test entity per type (never added to the world), empty if the type has no health. */
    private static final Map<EntityType<?>, Optional<LivingEntity>> SAMPLES = new HashMap<>();

    public static boolean shouldShow(LivingEntity entity, Entity viewer) {
        DamageTracker.update(entity); // always track, even if hidden right now

        MhdConfig cfg = MhdConfig.get();
        if (!cfg.showHearts || !entity.isAlive()) return false;
        if (Minecraft.getInstance().gui.hud.isHidden()) return false; // F1

        boolean self = entity == viewer;
        if (self) {
            if (!cfg.showSelf) return false;
        } else {
            // other players: hidden while sneaking, in spectator or in creative (they take no damage)
            if (entity.isDiscrete()) return false;
            if (entity instanceof Player player && (player.isSpectator() || player.isCreative())) return false;

            EntityCategory category = EntityCategory.of(entity);
            if (category.isHostileGroup() && !cfg.showHostile) return false;
            if ((category == EntityCategory.PASSIVE || category == EntityCategory.TAMED)
                    && !cfg.showPassive) return false;

            if (cfg.maxRange > 0 && viewer != null
                    && entity.distanceToSqr(viewer) > cfg.maxRange * cfg.maxRange) return false;
        }

        if (cfg.onlyShowDamaged && entity.getHealth() >= entity.getMaxHealth()) return false;
        if (cfg.hideAfterSeconds > 0 && !DamageTracker.damagedWithin(entity, cfg.hideAfterSeconds)) return false;

        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        if (cfg.whiteList && !cfg.whiteListEntries.contains(id)) return false;
        if (cfg.blackList && cfg.blackListEntries.contains(id)) return false;

        return true;
    }

    /** True for entity types that have health, so boats, arrows and items are left out of lists. */
    public static boolean hasHealth(EntityType<?> type) {
        if (type == EntityTypes.PLAYER) return true; // players cannot be created this way
        if (Minecraft.getInstance().level == null) return true; // no world loaded: cannot check, so show everything
        return sample(type) != null;
    }

    /** Category of an entity type, e.g. "undead". Used for the design preview in the settings window. */
    public static String categoryName(EntityType<?> type) {
        if (type == EntityTypes.PLAYER) return "player";
        LivingEntity sample = sample(type);
        EntityCategory category = sample == null ? EntityCategory.PASSIVE : EntityCategory.of(sample);
        return category.name().toLowerCase(Locale.ROOT);
    }

    private static LivingEntity sample(EntityType<?> type) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return null;
        return SAMPLES.computeIfAbsent(type, t -> {
            try {
                return t.create(level, EntitySpawnReason.LOAD) instanceof LivingEntity living
                        ? Optional.of(living) : Optional.empty();
            } catch (Throwable e) {
                return Optional.empty(); // some special entities cannot be created on their own
            }
        }).orElse(null);
    }
}