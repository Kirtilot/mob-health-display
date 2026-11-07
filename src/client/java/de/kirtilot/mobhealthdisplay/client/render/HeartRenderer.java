package de.kirtilot.mobhealthdisplay.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.kirtilot.mobhealthdisplay.client.config.MhdConfig;
import de.kirtilot.mobhealthdisplay.client.texture.HeartDesign;
import de.kirtilot.mobhealthdisplay.client.texture.HeartType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

/** Draws the heart rows as textured quads facing the camera, growing upwards like the player HUD. */
public final class HeartRenderer {
    private static final float SCALE = 0.025f;       // same scale as name tags
    private static final int HEART_SIZE = 9;         // heart texture is 9x9 pixels
    private static final int HEART_STEP = 8;         // hearts overlap by 1 pixel, like the vanilla HUD
    private static final int ROW_STEP = 10;          // vertical distance between heart rows
    private static final int COUNT_LINE = 10;        // height of the "x5" line below the hearts
    private static final int DIVIDER_HEIGHT = 3;     // height of the lines left and right of "x5"
    private static final int DIVIDER_GAP = 3;        // space between the lines and the text
    private static final double BOTTOM_LIFT = 0.3;   // fixed gap between the name tag point and the lower edge
    private static final float LAYER_OFFSET = 0.01f; // draws hearts in front of the containers
    private static final int SHRINK_STEPS = 5;       // damage animation without lost.png: 9, 7, 5, 3, 1 pixels

    private HeartRenderer() {}

    /** Position of each heart slot: index 0 is bottom left, rows stack upwards. */
    private record Grid(float left, int top, int rows) {
        float x(int index) {
            return left + (index % HeartLayout.HEARTS_PER_ROW) * HEART_STEP;
        }

        float y(int index) {
            return top + (rows - 1 - index / HeartLayout.HEARTS_PER_ROW) * ROW_STEP;
        }
    }

    public static void submit(HeartLayout layout, HeartDesign design, HeartType type, List<LossAnimator.LostHeart> lostHearts, Vec3 anchor, boolean showBarCount, PoseStack poseStack,
                              SubmitNodeCollector collector, CameraRenderState camera, int light) {
        boolean showCount = showBarCount && layout.barCount() > 1;
        int rows = layout.rowCount();
        int columns = Math.min(layout.totalSlots(), HeartLayout.HEARTS_PER_ROW);
        int heartsHeight = (rows - 1) * ROW_STEP + HEART_SIZE;
        int totalHeight = heartsHeight + (showCount ? COUNT_LINE : 0);
        float rowWidth = columns * HEART_STEP + 1;

        poseStack.pushPose();
        // the lower edge stays fixed, everything is built upwards from there
        poseStack.translate(anchor.x, anchor.y + BOTTOM_LIFT, anchor.z);
        poseStack.mulPose(new Matrix4f().rotation(camera.orientation));
        poseStack.scale(SCALE, -SCALE, SCALE);

        int top = -totalHeight; // y grows downwards here, so negative values are above the lower edge
        Grid grid = new Grid(-rowWidth / 2f, top, rows);

        submitIcons(collector, poseStack, design.container(), grid, 0, layout.totalSlots(), 0f, light);
        submitIcons(collector, poseStack, design.full(type), grid, 0, layout.fullHearts(), LAYER_OFFSET, light);
        if (layout.halfHeart()) {
            submitIcons(collector, poseStack, design.half(type), grid, layout.fullHearts(), 1, LAYER_OFFSET, light);
        }

        for (LossAnimator.LostHeart lost : lostHearts) {
            if (lost.slot() < layout.healthSlots()) submitLostHeart(collector, poseStack, design, type, grid, lost, light);
        }

        // absorption hearts after the health slots; like vanilla they stay black while withered
        HeartType absorptionType = type == HeartType.WITHERED ? HeartType.WITHERED : HeartType.ABSORBING;
        int absorptionStart = layout.healthSlots();
        submitIcons(collector, poseStack, design.full(absorptionType), grid, absorptionStart, layout.absorptionHearts(), LAYER_OFFSET, light);
        if (layout.absorptionHalf()) {
            submitIcons(collector, poseStack, design.half(absorptionType), grid,
                    absorptionStart + layout.absorptionHearts(), 1, LAYER_OFFSET, light);
        }

        if (showCount) {
            Font font = Minecraft.getInstance().font;
            String text = (MhdConfig.get().showCountPrefix ? "x" : "") + layout.barCount();
            int textWidth = font.width(text);
            float textX = -textWidth / 2f;
            float textY = top + heartsHeight + 1;

            collector.submitText(poseStack, textX, textY,
                    Component.literal(text).getVisualOrderText(), false, Font.DisplayMode.NORMAL,
                    light, 0xFFFFFFFF, 0, 0);

            if (MhdConfig.get().showDividers && design.divider() != null) {
                submitDividers(collector, poseStack, design.divider(), -rowWidth / 2f, rowWidth / 2f,
                        textX, textX + textWidth, textY + 3, light);
            }
        }

        poseStack.popPose();
    }

    /** Lines from the left edge of the heart row to the text, and from the text to the right edge. */
    private static void submitDividers(SubmitNodeCollector collector, PoseStack poseStack, Identifier texture,
                                       float rowLeft, float rowRight, float textLeft, float textRight,
                                       float y, int light) {
        float leftEnd = textLeft - DIVIDER_GAP;
        float rightStart = textRight + DIVIDER_GAP - 1; // the font adds 1 pixel of spacing after the last letter
        if (leftEnd - rowLeft < 2) return; // not enough space for lines

        collector.submitCustomGeometry(poseStack, RenderTypes.text(texture), (pose, buffer) -> {
            quad(buffer, pose, rowLeft, y, leftEnd, y + DIVIDER_HEIGHT, 0f, light);
            quad(buffer, pose, rightStart, y, rowRight, y + DIVIDER_HEIGHT, 0f, light);
        });
    }

    /**
     * A lost (half) heart shrinking towards its center.
     * With a lost.png strip: shows its frames one after another.
     * Without: draws the full heart smaller in steps of 2 pixels (9, 7, 5, 3, 1).
     * A lost half only shows that half of the heart.
     */
    private static void submitLostHeart(SubmitNodeCollector collector, PoseStack poseStack, HeartDesign design,
                                        HeartType type, Grid grid, LossAnimator.LostHeart lost, int light) {
        float x0 = grid.x(lost.slot());
        float y0 = grid.y(lost.slot());
        float progress = Math.min(Math.max(lost.progress(), 0f), 0.999f);
        HeartDesign.LostTexture strip = design.lost(type);

        Identifier texture;
        float inset;   // pixels cut off on each side
        float v0;
        float v1;
        if (strip != null) {
            int frame = (int) (progress * strip.frames());
            texture = strip.texture();
            inset = 0f;
            v0 = frame / (float) strip.frames();
            v1 = (frame + 1) / (float) strip.frames();
        } else {
            texture = design.full(type);
            inset = (int) (progress * SHRINK_STEPS);
            v0 = 0f;
            v1 = 1f;
        }

        float mid = x0 + HEART_SIZE / 2f;
        float left = lost.rightHalf() ? mid : x0 + inset;
        float right = lost.rightHalf() ? x0 + HEART_SIZE - inset : mid;
        float u0 = lost.rightHalf() ? 0.5f : 0f;
        float u1 = lost.rightHalf() ? 1f : 0.5f;
        float top = y0 + inset;
        float bottom = y0 + HEART_SIZE - inset;

        collector.submitCustomGeometry(poseStack, RenderTypes.text(texture), (pose, buffer) ->
                quad(buffer, pose, left, top, right, bottom, u0, v0, u1, v1, LAYER_OFFSET, light));
    }

    private static void submitIcons(SubmitNodeCollector collector, PoseStack poseStack, Identifier texture,
                                    Grid grid, int from, int count, float z, int light) {
        if (count <= 0) return;
        collector.submitCustomGeometry(poseStack, RenderTypes.text(texture), (pose, buffer) -> {
            for (int i = from; i < from + count; i++) {
                float x0 = grid.x(i);
                float y0 = grid.y(i);
                quad(buffer, pose, x0, y0, x0 + HEART_SIZE, y0 + HEART_SIZE, z, light);
            }
        });
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose pose,
                             float x0, float y0, float x1, float y1, float z, int light) {
        quad(buffer, pose, x0, y0, x1, y1, 0f, 0f, 1f, 1f, z, light);
    }

    /** Quad with a part of the texture: u0..u1, v0..v1 between 0 and 1. */
    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, float x0, float y0, float x1, float y1,
                             float u0, float v0, float u1, float v1, float z, int light) {
        buffer.addVertex(pose, x0, y0, z).setColor(0xFFFFFFFF).setUv(u0, v0).setLight(light);
        buffer.addVertex(pose, x0, y1, z).setColor(0xFFFFFFFF).setUv(u0, v1).setLight(light);
        buffer.addVertex(pose, x1, y1, z).setColor(0xFFFFFFFF).setUv(u1, v1).setLight(light);
        buffer.addVertex(pose, x1, y0, z).setColor(0xFFFFFFFF).setUv(u1, v0).setLight(light);
    }
}