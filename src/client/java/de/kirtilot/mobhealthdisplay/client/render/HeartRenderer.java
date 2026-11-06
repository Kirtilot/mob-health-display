package de.kirtilot.mobhealthdisplay.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Draws the heart bar as textured quads facing the camera. */
public final class HeartRenderer {
    private static final Identifier CONTAINER = Identifier.withDefaultNamespace("textures/gui/sprites/hud/heart/container.png");
    private static final Identifier FULL = Identifier.withDefaultNamespace("textures/gui/sprites/hud/heart/full.png");
    private static final Identifier HALF = Identifier.withDefaultNamespace("textures/gui/sprites/hud/heart/half.png");

    private static final float SCALE = 0.025f;      // same scale as name tags
    private static final int HEART_SIZE = 9;        // heart texture is 9x9 pixels
    private static final int HEART_STEP = 8;        // hearts overlap by 1 pixel, like the vanilla HUD
    private static final float LAYER_OFFSET = 0.01f; // draws hearts in front of the containers

    private HeartRenderer() {}

    public static void submit(HeartLayout layout, Vec3 anchor, boolean showBarCount, PoseStack poseStack,
                              SubmitNodeCollector collector, CameraRenderState camera, int light) {
        poseStack.pushPose();
        poseStack.translate(anchor.x, anchor.y + 0.5, anchor.z); // same lift as vanilla name tags
        poseStack.mulPose(new Matrix4f().rotation(camera.orientation));
        poseStack.scale(SCALE, -SCALE, SCALE);

        int slots = layout.totalSlots();
        float left = -(slots * HEART_STEP + 1) / 2f; // centered above the mob

        submitIcons(collector, poseStack, CONTAINER, left, slots, 0f, light);
        submitIcons(collector, poseStack, FULL, left, layout.fullHearts(), LAYER_OFFSET, light);
        if (layout.halfHeart()) {
            submitIcons(collector, poseStack, HALF, left + layout.fullHearts() * HEART_STEP, 1, LAYER_OFFSET, light);
        }

        if (showBarCount && layout.barCount() > 1) {
            Font font = Minecraft.getInstance().font;
            String text = "x" + layout.barCount();
            collector.submitText(poseStack, -font.width(text) / 2f, HEART_SIZE + 1,
                    Component.literal(text).getVisualOrderText(), false, Font.DisplayMode.NORMAL,
                    light, 0xFFFFFFFF, 0, 0);
        }

        poseStack.popPose();
    }

    private static void submitIcons(SubmitNodeCollector collector, PoseStack poseStack, Identifier texture,
                                    float startX, int count, float z, int light) {
        if (count <= 0) return;
        collector.submitCustomGeometry(poseStack, RenderTypes.text(texture), (pose, buffer) -> {
            for (int i = 0; i < count; i++) {
                float x0 = startX + i * HEART_STEP;
                quad(buffer, pose, x0, 0, x0 + HEART_SIZE, HEART_SIZE, z, light);
            }
        });
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose pose,
                             float x0, float y0, float x1, float y1, float z, int light) {
        buffer.addVertex(pose, x0, y0, z).setColor(0xFFFFFFFF).setUv(0, 0).setLight(light);
        buffer.addVertex(pose, x0, y1, z).setColor(0xFFFFFFFF).setUv(0, 1).setLight(light);
        buffer.addVertex(pose, x1, y1, z).setColor(0xFFFFFFFF).setUv(1, 1).setLight(light);
        buffer.addVertex(pose, x1, y0, z).setColor(0xFFFFFFFF).setUv(1, 0).setLight(light);
    }
}