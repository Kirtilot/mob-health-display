package de.kirtilot.mobhealthdisplay.client.screen;

import de.kirtilot.mobhealthdisplay.client.config.MhdConfig;
import de.kirtilot.mobhealthdisplay.client.config.MhdSettings;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Settings window in an inventory-style panel, opened by key bind or /mhd config. */
public class MhdConfigScreen extends Screen {
    private static final Identifier PANEL = Identifier.fromNamespaceAndPath("mob_health_display", "settings_panel");

    private static final int BUTTON_WIDTH = 150;
    private static final int ROW = 24;      // button height 20 + gap 4
    private static final int GAP = 4;
    private static final int PADDING = 8;
    private static final int COLUMNS = 2;
    private static final int PANEL_WIDTH = COLUMNS * BUTTON_WIDTH + GAP + 2 * PADDING;

    private enum Tab { GENERAL, LISTS }

    private final Screen parent;
    private Tab tab = Tab.GENERAL;
    private int panelLeft;
    private int panelTop;
    private int panelHeight;

    public MhdConfigScreen(Screen parent) {
        super(Component.literal("Mob Health Display"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int contentRows = switch (tab) {
            case GENERAL -> rows(MhdSettings.BOOLEANS.size()) + rows(MhdSettings.FLOATS.size());
            case LISTS -> 1;
        };
        // title + tabs + content + done button
        panelHeight = PADDING + 14 + ROW + GAP + contentRows * ROW + GAP + 20 + PADDING;
        panelLeft = (this.width - PANEL_WIDTH) / 2;
        panelTop = (this.height - panelHeight) / 2;

        int left = panelLeft + PADDING;
        int y = panelTop + PADDING;

        // title
        Component titleText = this.title.copy().withColor(0x404040); // dark grey like inventory titles
        int titleWidth = this.font.width(titleText);
        StringWidget title = new StringWidget(panelLeft + (PANEL_WIDTH - titleWidth) / 2, y, titleWidth, 9, titleText, this.font);
        addRenderableWidget(title);
        y += 14;

        // tab buttons, the selected one is greyed out
        addTabButton(Tab.GENERAL, "General", left, y);
        addTabButton(Tab.LISTS, "Lists", left + BUTTON_WIDTH + GAP, y);
        y += ROW + GAP;

        switch (tab) {
            case GENERAL -> y = buildGeneralTab(left, y);
            case LISTS -> y = buildListsTab(left, y);
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(left, panelTop + panelHeight - PADDING - 20, PANEL_WIDTH - 2 * PADDING, 20)
                .build());
    }

    private int buildGeneralTab(int left, int y) {
        List<MhdSettings.BoolSetting> bools = MhdSettings.BOOLEANS;
        for (int i = 0; i < bools.size(); i++) {
            MhdSettings.BoolSetting s = bools.get(i);
            addRenderableWidget(CycleButton.onOffBuilder(s.get().get())
                    .create(columnX(left, i), y + (i / COLUMNS) * ROW, BUTTON_WIDTH, 20,
                            Component.literal(s.label()), (button, value) -> s.set().accept(value)));
        }
        y += rows(bools.size()) * ROW;

        List<MhdSettings.FloatSetting> floats = MhdSettings.FLOATS;
        for (int i = 0; i < floats.size(); i++) {
            addRenderableWidget(new SettingSlider(columnX(left, i), y + (i / COLUMNS) * ROW, BUTTON_WIDTH, 20, floats.get(i)));
        }
        return y + rows(floats.size()) * ROW;
    }

    private int buildListsTab(int left, int y) {
        Component text = Component.literal("Coming soon").withColor(0x404040);
        int w = this.font.width(text);
        StringWidget label = new StringWidget(panelLeft + (PANEL_WIDTH - w) / 2, y + 6, w, 9, text, this.font);
        addRenderableWidget(label);
        return y + ROW;
    }

    private void addTabButton(Tab target, String name, int x, int y) {
        Button button = Button.builder(Component.literal(name), b -> {
            tab = target;
            rebuildWidgets();
        }).bounds(x, y, BUTTON_WIDTH, 20).build();
        button.active = tab != target;
        addRenderableWidget(button);
    }

    private static int columnX(int left, int index) {
        return left + (index % COLUMNS) * (BUTTON_WIDTH + GAP);
    }

    private static int rows(int count) {
        return (count + COLUMNS - 1) / COLUMNS;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        extractBlurredBackground(graphics);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL, panelLeft, panelTop, PANEL_WIDTH, panelHeight);
    }

    @Override
    public void onClose() {
        MhdConfig.save();
        this.minecraft.gui.setScreen(parent);
    }
}