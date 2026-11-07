package de.kirtilot.mobhealthdisplay.client.render;

import de.kirtilot.mobhealthdisplay.client.texture.HeartDesign;
import de.kirtilot.mobhealthdisplay.client.texture.HeartType;
import net.minecraft.world.phys.Vec3;

/** Extra data we attach to every entity render state. */
public interface HealthRenderState {
    HeartLayout mhd$getLayout();
    void mhd$setLayout(HeartLayout layout);
    Vec3 mhd$getHeartAnchor();
    void mhd$setHeartAnchor(Vec3 anchor);
    HeartDesign mhd$getDesign();
    void mhd$setDesign(HeartDesign design);
    HeartType mhd$getHeartType();
    void mhd$setHeartType(HeartType type);
}