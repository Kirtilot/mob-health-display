package de.kirtilot.mobhealthdisplay.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import de.kirtilot.mobhealthdisplay.client.config.MhdConfig;
import de.kirtilot.mobhealthdisplay.client.screen.MhdConfigScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Key bindings, shown under Options > Controls > Key Binds. */
public final class MhdKeybinds {
    private MhdKeybinds() {}

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("mob_health_display", "main"));

    public static final KeyMapping TOGGLE = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.mob_health_display.toggle", InputConstants.KEY_H, CATEGORY));

    // unbound by default to avoid conflicts with other mods
    public static final KeyMapping OPEN_CONFIG = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.mob_health_display.open_config", InputConstants.UNKNOWN.getValue(), CATEGORY));

    private static boolean openConfigRequested;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(MhdKeybinds::onTick);
    }

    /** Opens the settings on the next tick, so the chat screen of /mhd config can close first. */
    public static void requestConfigScreen() {
        openConfigRequested = true;
    }

    private static void onTick(Minecraft client) {
        while (TOGGLE.consumeClick()) {
            MhdConfig cfg = MhdConfig.get();
            cfg.showHearts = !cfg.showHearts;
            MhdConfig.save();

            if (client.player != null) {
                Component state = cfg.showHearts
                        ? Component.literal("ON").withStyle(ChatFormatting.GREEN)
                        : Component.literal("OFF").withStyle(ChatFormatting.RED);
                client.player.sendOverlayMessage(Component.literal("Mob Heart Display: ").append(state));
            }
        }

        while (OPEN_CONFIG.consumeClick()) {
            openConfigRequested = true;
        }

        if (openConfigRequested) {
            openConfigRequested = false;
            client.gui.setScreen(new MhdConfigScreen(client.gui.screen()));
        }
    }
}