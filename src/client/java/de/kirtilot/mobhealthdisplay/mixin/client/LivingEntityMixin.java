package de.kirtilot.mobhealthdisplay.mixin.client;

import de.kirtilot.mobhealthdisplay.client.filter.HealthSyncState;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remembers whether the server has ever sent a health value for this entity (see HealthReader). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements HealthSyncState {
    @Shadow @Final private static EntityDataAccessor<Float> DATA_HEALTH_ID;

    @Unique private boolean mhd$constructed;     // the constructor itself sets health to max, that doesn't count
    @Unique private boolean mhd$healthReceived;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void mhd$afterConstructor(CallbackInfo ci) {
        mhd$constructed = true;
    }

    @Inject(method = "onSyncedDataUpdated", at = @At("HEAD"))
    private void mhd$onSyncedDataUpdated(EntityDataAccessor<?> accessor, CallbackInfo ci) {
        if (mhd$constructed && DATA_HEALTH_ID.equals(accessor)) {
            mhd$healthReceived = true;
        }
    }

    @Override
    public boolean mhd$healthReceived() {
        return mhd$healthReceived;
    }
}