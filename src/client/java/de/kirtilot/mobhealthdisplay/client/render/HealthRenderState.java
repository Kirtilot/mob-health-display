package de.kirtilot.mobhealthdisplay.client.render;

import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/** Extra data we attach to every entity render state. */
public interface HealthRenderState {
    Component mhd$getHeartText();
    void mhd$setHeartText(Component text);
    Vec3 mhd$getHeartAnchor();
    void mhd$setHeartAnchor(Vec3 anchor);
}