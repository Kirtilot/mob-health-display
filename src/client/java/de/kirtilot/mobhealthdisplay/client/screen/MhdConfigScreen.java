package de.kirtilot.mobhealthdisplay.client.screen;

import de.kirtilot.mobhealthdisplay.client.config.MhdConfig;
import de.kirtilot.mobhealthdisplay.client.config.MhdSettings;
import de.kirtilot.mobhealthdisplay.client.filter.EntityFilter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Settings window in an inventory-style panel, opened by key bind or /mhd config. */
public class MhdConfigScreen extends Screen {
    private static final Identifier PANEL = Identifier.fromNamespaceAndPath("mob_health_display", "settings_panel");

    private static final int BUTTON_WIDTH = 150;
    private static final int ROW = 24;          // button height 20 + gap 4
    private static final int GAP = 4;
    private static final int PADDING = 8;
    private static final int COLUMNS = 2;
    private static final int PANEL_WIDTH = COLUMNS * BUTTON_WIDTH + GAP + 2 * PADDING;
    private static final int CONTENT_WIDTH = PANEL_WIDTH - 2 * PADDING;

    private static final int LIST_ROWS = 7;      // visible rows, scroll with the mouse wheel
    private static final int LIST_ROW = 22;      // height of one list row
    private static final int TOGGLE_WIDTH = 50;
    private static final int SCROLLBAR = 6;
    private static final int FILTER_WIDTH = 110;

    private static final int TITLE_COLOR = 0xFF404040;    // dark grey like inventory titles, no shadow
    private static final int LIST_BACKGROUND = 0xFF8B8B8B; // like an inventory slot
    private static final int LIST_STRIPE = 0xFF7F7F7F;
    private static final int SCROLL_TRACK = 0xFF555555;
    private static final int SCROLL_THUMB = 0xFFC6C6C6;

    private enum Tab { GENERAL, LISTS }

    private final Screen parent;
    private Tab tab = Tab.GENERAL;
    private int panelLeft;
    private int panelTop;
    private int panelHeight;

    // state of the "Lists" tab, kept when the window is rebuilt
    private final List<String> allEntityIds = new ArrayList<>();
    private final Map<String, Component> entityNames = new HashMap<>();
    private boolean editingWhitelist = false;
    private boolean selectedOnly = false;
    private String search = "";
    private int scroll;
    private int listLeft;
    private int listTop;
    private final List<CycleButton<Boolean>> rowToggles = new ArrayList<>();
    private List<String> shownIds = List.of();
    private Button blacklistButton;
    private Button whitelistButton;

    public MhdConfigScreen(Screen parent) {
        super(Component.literal("Mob Health Display"));
        this.parent = parent;

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!EntityFilter.hasHealth(type)) continue; // skip boats, arrows, items, ...
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            allEntityIds.add(id);
            entityNames.put(id, type.getDescription());
        }
        allEntityIds.sort(Comparator.comparing(id -> entityNames.get(id).getString()));
    }

    @Override
    protected void init() {
        int contentHeight = switch (tab) {
            case GENERAL -> (rows(MhdSettings.BOOLEANS.size()) + rows(MhdSettings.FLOATS.size())) * ROW;
            case LISTS -> 2 * ROW + LIST_ROWS * LIST_ROW;
        };
        // title + tabs + content + done button
        panelHeight = PADDING + 14 + ROW + GAP + contentHeight + GAP + 20 + PADDING;
        panelLeft = (this.width - PANEL_WIDTH) / 2;
        panelTop = (this.height - panelHeight) / 2;

        int left = panelLeft + PADDING;
        int y = panelTop + PADDING + 14; // the title itself is drawn in extractRenderState

        addTabButton(Tab.GENERAL, "General", left, y);
        addTabButton(Tab.LISTS, "Lists", left + BUTTON_WIDTH + GAP, y);
        y += ROW + GAP;

        switch (tab) {
            case GENERAL -> buildGeneralTab(left, y);
            case LISTS -> buildListsTab(left, y);
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(left, panelTop + panelHeight - PADDING - 20, CONTENT_WIDTH, 20)
                .build());
    }

    // ---------- General tab ----------

    private void buildGeneralTab(int left, int y) {
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
    }

    // ---------- Lists tab ----------

    private void buildListsTab(int left, int y) {
        blacklistButton = addModeButton(false, left, y);
        whitelistButton = addModeButton(true, left + BUTTON_WIDTH + GAP, y);
        y += ROW;

        EditBox searchBox = new EditBox(this.font, left, y, CONTENT_WIDTH - FILTER_WIDTH - GAP, 20, Component.literal("Search"));
        searchBox.setHint(Component.literal("Search mobs..."));
        searchBox.setMaxLength(64);
        searchBox.setValue(search);
        addRenderableWidget(searchBox);

        addRenderableWidget(CycleButton.onOffBuilder(selectedOnly)
                .create(left + CONTENT_WIDTH - FILTER_WIDTH, y, FILTER_WIDTH, 20, Component.literal("Selected only"),
                        (button, value) -> {
                            selectedOnly = value;
                            scroll = 0;
                            refreshRows();
                        }));
        y += ROW;

        listLeft = left;
        listTop = y;
        rowToggles.clear();
        int toggleX = left + CONTENT_WIDTH - SCROLLBAR - 2 - TOGGLE_WIDTH;
        for (int i = 0; i < LIST_ROWS; i++) {
            final int row = i;
            rowToggles.add(addRenderableWidget(CycleButton.onOffBuilder(false)
                    .displayOnlyValue()
                    .create(toggleX, y + 1 + i * LIST_ROW, TOGGLE_WIDTH, 20, Component.literal("In list"),
                            (button, value) -> setInList(row, value))));
        }

        // set the responder last, so the rows already exist when it fires
        searchBox.setResponder(text -> {
            search = text;
            scroll = 0;
            refreshRows();
        });
        refreshRows();
    }

    private List<String> currentList() {
        return editingWhitelist ? MhdConfig.get().whiteListEntries : MhdConfig.get().blackListEntries;
    }

    private void refreshRows() {
        List<String> selected = currentList();
        String query = search.trim().toLowerCase(Locale.ROOT);

        shownIds = allEntityIds.stream()
                .filter(id -> !selectedOnly || selected.contains(id))
                .filter(id -> query.isEmpty() || id.contains(query)
                        || entityNames.get(id).getString().toLowerCase(Locale.ROOT).contains(query))
                .toList();

        scroll = Math.max(0, Math.min(scroll, shownIds.size() - LIST_ROWS));
        for (int i = 0; i < rowToggles.size(); i++) {
            int index = scroll + i;
            CycleButton<Boolean> toggle = rowToggles.get(i);
            toggle.visible = index < shownIds.size();
            if (toggle.visible) {
                toggle.setValue(selected.contains(shownIds.get(index)));
            }
        }
        updateModeLabels();
    }

    private void setInList(int row, boolean inList) {
        int index = scroll + row;
        if (index >= shownIds.size()) return;

        String id = shownIds.get(index);
        List<String> list = currentList();
        if (inList && !list.contains(id)) list.add(id);
        if (!inList) list.remove(id);
        MhdConfig.save();

        if (selectedOnly) {
            refreshRows(); // a deselected mob disappears from the filtered view
        } else {
            updateModeLabels();
        }
    }

    private void updateModeLabels() {
        blacklistButton.setMessage(Component.literal("Blacklist (" + MhdConfig.get().blackListEntries.size() + ")"));
        whitelistButton.setMessage(Component.literal("Whitelist (" + MhdConfig.get().whiteListEntries.size() + ")"));
    }

    private void extractList(GuiGraphicsExtractor graphics) {
        int listHeight = LIST_ROWS * LIST_ROW;
        int right = listLeft + CONTENT_WIDTH;
        int trackX = right - SCROLLBAR;

        graphics.fill(listLeft, listTop, right, listTop + listHeight, LIST_BACKGROUND);

        for (int i = 0; i < LIST_ROWS; i++) {
            int index = scroll + i;
            if (index >= shownIds.size()) break;
            int rowY = listTop + i * LIST_ROW;
            if (i % 2 == 1) {
                graphics.fill(listLeft, rowY, trackX, rowY + LIST_ROW, LIST_STRIPE);
            }
            graphics.text(this.font, entityNames.get(shownIds.get(index)).getVisualOrderText(),
                    listLeft + 6, rowY + 7, 0xFFFFFFFF, true);
        }

        if (shownIds.isEmpty()) {
            Component empty = Component.literal("No mobs found");
            graphics.text(this.font, empty.getVisualOrderText(),
                    listLeft + (CONTENT_WIDTH - this.font.width(empty)) / 2, listTop + listHeight / 2 - 4, 0xFFFFFFFF, true);
        }

        // scrollbar
        graphics.fill(trackX, listTop, right, listTop + listHeight, SCROLL_TRACK);
        if (shownIds.size() > LIST_ROWS) {
            int thumbHeight = Math.max(10, listHeight * LIST_ROWS / shownIds.size());
            int thumbY = listTop + (listHeight - thumbHeight) * scroll / (shownIds.size() - LIST_ROWS);
            graphics.fill(trackX, thumbY, right, thumbY + thumbHeight, SCROLL_THUMB);
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        boolean overList = x >= listLeft && x < listLeft + CONTENT_WIDTH
                && y >= listTop && y < listTop + LIST_ROWS * LIST_ROW;
        if (tab == Tab.LISTS && overList) {
            scroll += scrollY > 0 ? -1 : 1;
            refreshRows();
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    // ---------- shared ----------

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int titleWidth = this.font.width(this.title);
        graphics.text(this.font, this.title.getVisualOrderText(),
                panelLeft + (PANEL_WIDTH - titleWidth) / 2, panelTop + PADDING, TITLE_COLOR, false);
        if (tab == Tab.LISTS) {
            extractList(graphics);
        }
        super.extractRenderState(graphics, mouseX, mouseY, a); // buttons on top
    }

    private void addTabButton(Tab target, String name, int x, int y) {
        Button button = Button.builder(Component.literal(name), b -> {
            tab = target;
            rebuildWidgets();
        }).bounds(x, y, BUTTON_WIDTH, 20).build();
        button.active = tab != target;
        addRenderableWidget(button);
    }

    private Button addModeButton(boolean whitelist, int x, int y) {
        Button button = Button.builder(Component.empty(), b -> {
            editingWhitelist = whitelist;
            scroll = 0;
            rebuildWidgets();
        }).bounds(x, y, BUTTON_WIDTH, 20).build();
        button.active = editingWhitelist != whitelist;
        return addRenderableWidget(button);
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