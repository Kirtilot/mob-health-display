package de.kirtilot.mobhealthdisplay.client;

import de.kirtilot.mobhealthdisplay.client.command.MhdCommands;
import de.kirtilot.mobhealthdisplay.client.config.MhdConfig;
import de.kirtilot.mobhealthdisplay.client.input.MhdKeybinds;
import de.kirtilot.mobhealthdisplay.client.texture.HeartDesignManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import de.kirtilot.mobhealthdisplay.client.texture.DesignOverrides;

public class MobHealthDisplayClient implements ClientModInitializer {
    public static final String MOD_ID = "mob_health_display";

    @Override
    public void onInitializeClient() {
        MhdConfig.load();
        DesignOverrides.load();
        MhdCommands.register();
        MhdKeybinds.register();

        // re-read designs.json whenever resource packs are (re)loaded
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Identifier.fromNamespaceAndPath(MOD_ID, "designs"),
                (ResourceManagerReloadListener) HeartDesignManager::reload);

        // optional built-in pack "Mob Hearts" (src/client/resources/resourcepacks/mob_hearts)
        ResourceLoader.registerBuiltinPack(
                Identifier.fromNamespaceAndPath(MOD_ID, "mob_hearts"),
                FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow(),
                Component.literal("Mob Hearts"),
                PackActivationType.NORMAL);
    }
}