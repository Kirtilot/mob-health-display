package de.kirtilot.mobhealthdisplay.client.filter;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

public enum EntityCategory {
    PLAYER, BOSS, TAMED, UNDEAD, HOSTILE, PASSIVE;

    public static EntityCategory of(LivingEntity entity) {
        if (entity instanceof Player) return PLAYER;
        EntityType<?> type = entity.getType();
        if (type == EntityTypes.WITHER || type == EntityTypes.ENDER_DRAGON) return BOSS;
        if (entity instanceof TamableAnimal pet && pet.isTame()) return TAMED;
        if (entity.is(EntityTypeTags.UNDEAD)) return UNDEAD;
        if (entity instanceof Enemy) return HOSTILE;
        return PASSIVE;
    }

    public boolean isHostileGroup() {
        return this == BOSS || this == UNDEAD || this == HOSTILE;
    }
}