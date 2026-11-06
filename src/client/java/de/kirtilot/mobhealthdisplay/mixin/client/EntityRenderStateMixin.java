package de.kirtilot.mobhealthdisplay.mixin.client;

import de.kirtilot.mobhealthdisplay.client.render.HealthRenderState;
import de.kirtilot.mobhealthdisplay.client.render.HeartLayout;
import de.kirtilot.mobhealthdisplay.client.texture.HeartDesign;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements HealthRenderState {
    @Unique private HeartLayout mhd$layout;
    @Unique private Vec3 mhd$heartAnchor;
    @Unique private HeartDesign mhd$design;

    @Override public HeartLayout mhd$getLayout() { return mhd$layout; }
    @Override public void mhd$setLayout(HeartLayout layout) { mhd$layout = layout; }
    @Override public Vec3 mhd$getHeartAnchor() { return mhd$heartAnchor; }
    @Override public void mhd$setHeartAnchor(Vec3 anchor) { mhd$heartAnchor = anchor; }
    @Override public HeartDesign mhd$getDesign() { return mhd$design; }
    @Override public void mhd$setDesign(HeartDesign design) { mhd$design = design; }
}