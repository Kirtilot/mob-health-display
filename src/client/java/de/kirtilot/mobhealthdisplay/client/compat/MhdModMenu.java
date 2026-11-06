package de.kirtilot.mobhealthdisplay.client.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import de.kirtilot.mobhealthdisplay.client.screen.MhdConfigScreen;

/** Adds a settings button to Mod Menu that opens our own settings window. */
public class MhdModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return MhdConfigScreen::new;
    }
}