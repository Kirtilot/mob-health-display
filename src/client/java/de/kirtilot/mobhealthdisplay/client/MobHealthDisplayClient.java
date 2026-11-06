package de.kirtilot.mobhealthdisplay.client;

import de.kirtilot.mobhealthdisplay.client.command.MhdCommands;
import de.kirtilot.mobhealthdisplay.client.config.MhdConfig;
import net.fabricmc.api.ClientModInitializer;

public class MobHealthDisplayClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MhdConfig.load();
        MhdCommands.register();
    }
}