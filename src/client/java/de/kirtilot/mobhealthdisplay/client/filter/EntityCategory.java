package de.kirtilot.mobhealthdisplay.client.filter;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

public enum EntityCategory {
    PLAYER, BOSS, TAMED, UNDEAD, HOSTILE, PASSIVE;

    /** Vanilla mobs that count as bosses. */
    private static final Set<EntityType<?>> BOSSES = Set.of(
            EntityTypes.WITHER,
            EntityTypes.ENDER_DRAGON,
            EntityTypes.ELDER_GUARDIAN
    );

    public static EntityCategory of(LivingEntity entity) {
        if (entity instanceof Player) return PLAYER;
        EntityType<?> type = entity.getType();
        if (BOSSES.contains(type)) return BOSS;
        if (entity instanceof TamableAnimal pet && pet.isTame()) return TAMED;
        if (entity.is(EntityTypeTags.UNDEAD)) return UNDEAD;
        if (entity instanceof Enemy) return HOSTILE;
        return PASSIVE;
    }

    public boolean isHostileGroup() {
        return this == BOSS || this == UNDEAD || this == HOSTILE;
    }
}