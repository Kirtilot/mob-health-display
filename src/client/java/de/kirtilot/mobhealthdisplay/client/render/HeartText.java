package de.kirtilot.mobhealthdisplay.client.render;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Temporary text version of the heart bar, replaced by textures later. */
public final class HeartText {
    private HeartText() {}

    public static Component of(HeartLayout layout, boolean showBarCount) {
        MutableComponent text = Component.empty();
        int half = layout.halfHeart() ? 1 : 0;
        int empty = layout.totalSlots() - layout.fullHearts() - half;

        text.append(Component.literal("♥".repeat(layout.fullHearts())).withStyle(ChatFormatting.RED));
        if (half == 1) text.append(Component.literal("♥").withStyle(ChatFormatting.LIGHT_PURPLE));
        if (empty > 0) text.append(Component.literal("♥".repeat(empty)).withStyle(ChatFormatting.DARK_GRAY));
        if (showBarCount && layout.barCount() > 1) {
            text.append(Component.literal(" x" + layout.barCount()).withStyle(ChatFormatting.WHITE));
        }
        return text;
    }
}