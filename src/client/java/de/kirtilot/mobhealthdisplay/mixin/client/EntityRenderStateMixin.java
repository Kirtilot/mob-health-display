package de.kirtilot.mobhealthdisplay.mixin.client;

import de.kirtilot.mobhealthdisplay.client.render.HealthRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements HealthRenderState {
    @Unique private Component mhd$heartText;
    @Unique private Vec3 mhd$heartAnchor;

    @Override public Component mhd$getHeartText() { return mhd$heartText; }
    @Override public void mhd$setHeartText(Component text) { mhd$heartText = text; }
    @Override public Vec3 mhd$getHeartAnchor() { return mhd$heartAnchor; }
    @Override public void mhd$setHeartAnchor(Vec3 anchor) { mhd$heartAnchor = anchor; }
}