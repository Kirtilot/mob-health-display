package de.kirtilot.mobhealthdisplay.client.filter;

/** Added to every LivingEntity by LivingEntityMixin. */
public interface HealthSyncState {
    /** true once the server has sent a health value for this entity */
    boolean mhd$healthReceived();
}