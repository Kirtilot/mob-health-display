package de.kirtilot.mobhealthdisplay.mixin.client;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** Gives access to the effect particle list that the server sends for every living entity. */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("DATA_EFFECT_PARTICLES")
    static EntityDataAccessor<List<ParticleOptions>> mhd$effectParticles() {
        throw new AssertionError(); // replaced by Mixin
    }
}