package de.kirtilot.mobhealthdisplay.client.screen;

import de.kirtilot.mobhealthdisplay.client.config.MhdConfig;
import de.kirtilot.mobhealthdisplay.client.config.MhdSettings;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Settings window, opened by key bind, /mhd config or (later) Mod Menu. */
public class MhdConfigScreen extends Screen {
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 4;
    private static final int COLUMNS = 2;

    private final Screen parent;

    public MhdConfigScreen(Screen parent) {
        super(Component.literal("Mob Health Display Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int titleWidth = this.font.width(this.title);
        addRenderableWidget(new StringWidget((this.width - titleWidth) / 2, 15, titleWidth, 9, this.title, this.font));

        int left = (this.width - (COLUMNS * BUTTON_WIDTH + GAP)) / 2;
        int top = 40;

        List<MhdSettings.BoolSetting> settings = MhdSettings.BOOLEANS;
        for (int i = 0; i < settings.size(); i++) {
            MhdSettings.BoolSetting s = settings.get(i);
            int x = left + (i % COLUMNS) * (BUTTON_WIDTH + GAP);
            int y = top + (i / COLUMNS) * (BUTTON_HEIGHT + GAP);
            addRenderableWidget(CycleButton.onOffBuilder(s.get().get())
                    .create(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal(s.label()),
                            (button, value) -> s.set().accept(value)));
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds((this.width - 200) / 2, this.height - 28, 200, 20)
                .build());
    }

    @Override
    public void onClose() {
        MhdConfig.save();
        this.minecraft.gui.setScreen(parent);
    }
}