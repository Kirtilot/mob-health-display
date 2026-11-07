package de.kirtilot.mobhealthdisplay.client.filter;

import de.kirtilot.mobhealthdisplay.client.texture.HeartType;
import de.kirtilot.mobhealthdisplay.mixin.client.LivingEntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Finds out which heart type an entity should get. Same order as vanilla: poison, wither, frozen.
 *
 * The client only knows the real effects of its own player. For all other entities we look at
 * the effect particles: the server keeps sending them, and every effect has its own particle color.
 * Effects without particles (e.g. /effect ... true) can't be seen this way.
 */
public final class EffectDetector {
    private static final int COLOR_TOLERANCE = 2; // per color channel, against rounding differences

    private EffectDetector() {}

    public static HeartType of(LivingEntity entity) {
        boolean poisoned;
        boolean withered;

        if (entity == Minecraft.getInstance().player) {
            poisoned = entity.hasEffect(MobEffects.POISON);
            withered = entity.hasEffect(MobEffects.WITHER);
        } else {
            List<ParticleOptions> particles = entity.getEntityData().get(LivingEntityAccessor.mhd$effectParticles());
            poisoned = hasParticle(particles, MobEffects.POISON);
            withered = hasParticle(particles, MobEffects.WITHER);
        }

        if (poisoned) return HeartType.POISONED;
        if (withered) return HeartType.WITHERED;
        if (entity.isFullyFrozen()) return HeartType.FROZEN;
        return HeartType.NORMAL;
    }

    private static boolean hasParticle(List<ParticleOptions> particles, Holder<MobEffect> effect) {
        int target = effect.value().getColor();
        for (ParticleOptions particle : particles) {
            if (particle instanceof ColorParticleOption color
                    && color.getType() == ParticleTypes.ENTITY_EFFECT
                    && close(color.getRed(), target >> 16)
                    && close(color.getGreen(), target >> 8)
                    && close(color.getBlue(), target)) {
                return true;
            }
        }
        return false;
    }

    /** channel: 0..1 from the particle, target: 0..255 in the lowest 8 bits */
    private static boolean close(float channel, int target) {
        return Math.abs(Math.round(channel * 255f) - (target & 0xFF)) <= COLOR_TOLERANCE;
    }
}