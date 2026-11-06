package de.kirtilot.mobhealthdisplay.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.kirtilot.mobhealthdisplay.client.config.MhiConfig;
import de.kirtilot.mobhealthdisplay.client.filter.EntityFilter;
import de.kirtilot.mobhealthdisplay.client.render.HealthRenderState;
import de.kirtilot.mobhealthdisplay.client.render.HeartLayout;
import de.kirtilot.mobhealthdisplay.client.render.HeartRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    // Phase 1: collect data (health, filter result, position)
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void mhd$extract(Entity entity, EntityRenderState state, float partialTicks, CallbackInfo ci) {
        HealthRenderState hs = (HealthRenderState) state;
        hs.mhd$setLayout(null);
        hs.mhd$setHeartAnchor(null);

        if (!(entity instanceof LivingEntity living)) return;
        if (living.isInvisible()) return;
        if (!EntityFilter.shouldShow(living, Minecraft.getInstance().getCameraEntity())) return;

        Vec3 anchor = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(partialTicks));
        if (anchor == null) return;

        MhiConfig cfg = MhiConfig.get();
        double extra = cfg.height + (state.nameTag != null ? 0.3 : 0.0); // move above a visible name

        hs.mhd$setLayout(HeartLayout.of(living.getHealth(), living.getMaxHealth(), cfg.hpPerBar));
        hs.mhd$setHeartAnchor(anchor.add(0, extra, 0));
    }

    // Phase 2: draw
    @Inject(method = "submit", at = @At("TAIL"))
    private void mhd$submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                            CameraRenderState camera, CallbackInfo ci) {
        HealthRenderState hs = (HealthRenderState) state;
        HeartLayout layout = hs.mhd$getLayout();
        Vec3 anchor = hs.mhd$getHeartAnchor();
        if (layout == null || anchor == null) return;

        HeartRenderer.submit(layout, anchor, MhiConfig.get().showBarCount, poseStack, collector, camera, state.lightCoords);
    }
}