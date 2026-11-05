package de.kirtilot.mobhealthdisplay.client;

import de.kirtilot.mobhealthdisplay.client.config.MhiConfig;
import net.fabricmc.api.ClientModInitializer;

public class MobHealthDisplayClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MhiConfig.load();
    }
}