package de.kirtilot.mobhealthdisplay.client.screen;

import de.kirtilot.mobhealthdisplay.client.config.MhdConfig;
import de.kirtilot.mobhealthdisplay.client.config.MhdSettings;
import de.kirtilot.mobhealthdisplay.client.filter.EntityCategory;
import de.kirtilot.mobhealthdisplay.client.filter.EntityFilter;
import de.kirtilot.mobhealthdisplay.client.texture.DesignOverrides;
import de.kirtilot.mobhealthdisplay.client.texture.HeartDesign;
import de.kirtilot.mobhealthdisplay.client.texture.HeartDesignManager;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Settings window in an inventory-style panel, opened by key bind, /mhd config or Mod Menu. */
public class MhdConfigScreen extends Screen {
    private static final Identifier PANEL = Identifier.fromNamespaceAndPath("mob_health_display", "settings_panel");

    private static final int BUTTON_WIDTH = 150;
    private static final int ROW = 24;          // button height 20 + gap 4
    private static final int GAP = 4;
    private static final int PADDING = 8;
    private static final int COLUMNS = 2;
    private static final int PANEL_WIDTH = COLUMNS * BUTTON_WIDTH + GAP + 2 * PADDING;
    private static final int CONTENT_WIDTH = PANEL_WIDTH - 2 * PADDING;
    private static final int TAB_WIDTH = (CONTENT_WIDTH - 2 * GAP) / 3;

    private static final int MAX_LIST_ROWS = 7;  // visible list rows on a big screen, scroll with the mouse wheel
    private static final int MIN_LIST_ROWS = 2;  // never fewer, even on a tiny screen
    private static final int SCREEN_MARGIN = 4;  // free space above and below the panel
    private static final int FIXED_HEIGHT = PADDING + 14 + ROW + GAP + GAP + 20 + PADDING; // title, tabs, bottom buttons
    private static final int LIST_ROW = 22;      // height of one list row
    private static final int TOGGLE_WIDTH = 50;
    private static final int DESIGN_BUTTON_WIDTH = 110;
    private static final int PREVIEW_SIZE = 9;   // heart texture size
    private static final int SCROLLBAR = 6;
    private static final int FILTER_WIDTH = 110;

    private static final int TITLE_COLOR = 0xFF404040;    // dark grey like inventory titles, no shadow
    private static final int LIST_BACKGROUND = 0xFF8B8B8B; // like an inventory slot
    private static final int LIST_STRIPE = 0xFF7F7F7F;
    private static final int SCROLL_TRACK = 0xFF555555;
    private static final int SCROLL_THUMB = 0xFFC6C6C6;

    private static final String PACK_DEFAULT = ""; // design button value meaning "no own choice"
    private static final List<String> CATEGORY_NAMES = Arrays.stream(EntityCategory.values())
            .map(c -> c.name().toLowerCase(Locale.ROOT))
            .toList();

    private enum Tab { GENERAL, LISTS, DESIGNS }
    private enum DesignMode { CATEGORIES, ENTITIES }

    private final Screen parent;
    private Tab tab = Tab.GENERAL;
    private int panelLeft;
    private int panelTop;
    private int panelHeight;
    private int listRows = MAX_LIST_ROWS; // visible rows of the list tabs, depends on the screen height
    private int generalRows;              // visible setting rows of the General tab
    private int generalScroll;            // first visible setting row of the General tab
    private int contentTop;

    // all living entity types, sorted by display name
    private final List<String> allEntityIds = new ArrayList<>();
    private final Map<String, Component> entityNames = new HashMap<>();
    private final Map<String, EntityType<?>> entityTypes = new HashMap<>();

    // state of the list tabs, kept when the window is rebuilt
    private boolean editingWhitelist = false;
    private boolean selectedOnly = false;
    private DesignMode designMode = DesignMode.CATEGORIES;
    private String search = "";
    private int scroll;
    private int listLeft;
    private int listTop;
    private final List<CycleButton<Boolean>> rowToggles = new ArrayList<>();
    private final List<CycleButton<String>> rowDesigns = new ArrayList<>();
    private List<String> shownIds = List.of();
    private Button blacklistButton;
    private Button whitelistButton;

    public MhdConfigScreen(Screen parent) {
        super(Component.literal("Mob Heart Display"));
        this.parent = parent;

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!EntityFilter.hasHealth(type)) continue; // skip boats, arrows, items, ...
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            allEntityIds.add(id);
            entityNames.put(id, type.getDescription());
            entityTypes.put(id, type);
        }
        allEntityIds.sort(Comparator.comparing(id -> entityNames.get(id).getString()));
    }

    @Override
    protected void init() {
        // the content shrinks (and scrolls) when the screen is too small for everything
        int available = this.height - 2 * SCREEN_MARGIN - FIXED_HEIGHT;
        int totalRows = rows(MhdSettings.SCREEN_ORDER.size());
        generalRows = Math.max(1, Math.min(totalRows, available / ROW));
        generalScroll = Math.max(0, Math.min(generalScroll, totalRows - generalRows));
        listRows = Math.max(MIN_LIST_ROWS, Math.min(MAX_LIST_ROWS, (available - 2 * ROW) / LIST_ROW));

        int contentHeight = switch (tab) {
            case GENERAL -> generalRows * ROW;
            case LISTS, DESIGNS -> 2 * ROW + listRows * LIST_ROW;
        };
        // title + tabs + content + bottom buttons
        panelHeight = PADDING + 14 + ROW + GAP + contentHeight + GAP + 20 + PADDING;
        panelLeft = (this.width - PANEL_WIDTH) / 2;
        panelTop = (this.height - panelHeight) / 2;

        int left = panelLeft + PADDING;
        int y = panelTop + PADDING + 14; // the title itself is drawn in extractRenderState

        addTabButton(Tab.GENERAL, "General", left, y);
        addTabButton(Tab.LISTS, "Lists", left + TAB_WIDTH + GAP, y);
        addTabButton(Tab.DESIGNS, "Designs", left + 2 * (TAB_WIDTH + GAP), y);
        y += ROW + GAP;
        contentTop = y;

        rowToggles.clear();
        rowDesigns.clear();
        switch (tab) {
            case GENERAL -> buildGeneralTab(left, y);
            case LISTS -> buildListsTab(left, y);
            case DESIGNS -> buildDesignsTab(left, y);
        }

        int bottomY = panelTop + panelHeight - PADDING - 20;
        if (tab == Tab.DESIGNS) {
            int half = (CONTENT_WIDTH - GAP) / 2;
            addRenderableWidget(Button.builder(Component.literal("Reset all"), b -> resetAllDesigns())
                    .bounds(left, bottomY, half, 20).build());
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                    .bounds(left + half + GAP, bottomY, CONTENT_WIDTH - half - GAP, 20).build());
        } else {
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                    .bounds(left, bottomY, CONTENT_WIDTH, 20).build());
        }
    }

    // ---------- General tab ----------

    private void buildGeneralTab(int left, int y) {
        List<MhdSettings.Setting> settings = MhdSettings.SCREEN_ORDER;
        int from = generalScroll * COLUMNS;
        int to = Math.min(settings.size(), from + generalRows * COLUMNS);
        for (int i = from; i < to; i++) {
            int x = columnX(left, i);
            int rowY = y + (i / COLUMNS - generalScroll) * ROW;
            if (settings.get(i) instanceof MhdSettings.BoolSetting s) {
                addRenderableWidget(CycleButton.onOffBuilder(s.get().get())
                        .create(x, rowY, BUTTON_WIDTH, 20,
                                Component.literal(s.label()), (button, value) -> s.set().accept(value)));
            } else if (settings.get(i) instanceof MhdSettings.FloatSetting s) {
                addRenderableWidget(new SettingSlider(x, rowY, BUTTON_WIDTH, 20, s));
            }
        }
    }

    // ---------- Lists tab ----------

    private void buildListsTab(int left, int y) {
        blacklistButton = addModeButton(false, left, y);
        whitelistButton = addModeButton(true, left + BUTTON_WIDTH + GAP, y);
        y += ROW;

        EditBox searchBox = addSearchBox(left, y, CONTENT_WIDTH - FILTER_WIDTH - GAP);
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
        int toggleX = left + CONTENT_WIDTH - SCROLLBAR - 2 - TOGGLE_WIDTH;
        for (int i = 0; i < listRows; i++) {
            final int row = i;
            rowToggles.add(addRenderableWidget(CycleButton.onOffBuilder(false)
                    .displayOnlyValue()
                    .create(toggleX, y + 1 + i * LIST_ROW, TOGGLE_WIDTH, 20, Component.literal("In list"),
                            (button, value) -> setInList(row, value))));
        }

        activateSearch(searchBox);
    }

    private List<String> currentList() {
        return editingWhitelist ? MhdConfig.get().whiteListEntries : MhdConfig.get().blackListEntries;
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
        if (blacklistButton == null || whitelistButton == null) return;
        blacklistButton.setMessage(Component.literal("Blacklist (" + MhdConfig.get().blackListEntries.size() + ")"));
        whitelistButton.setMessage(Component.literal("Whitelist (" + MhdConfig.get().whiteListEntries.size() + ")"));
    }

    // ---------- Designs tab ----------

    private void buildDesignsTab(int left, int y) {
        addDesignModeButton(DesignMode.CATEGORIES, "Categories", left, y);
        addDesignModeButton(DesignMode.ENTITIES, "Mobs", left + BUTTON_WIDTH + GAP, y);
        y += ROW;

        EditBox searchBox = designMode == DesignMode.ENTITIES ? addSearchBox(left, y, CONTENT_WIDTH) : null;
        y += ROW;

        listLeft = left;
        listTop = y;

        List<String> values = new ArrayList<>();
        values.add(PACK_DEFAULT);
        values.addAll(HeartDesignManager.availableDesigns());

        int buttonX = designButtonX();
        for (int i = 0; i < listRows; i++) {
            final int row = i;
            rowDesigns.add(addRenderableWidget(CycleButton.<String>builder(MhdConfigScreen::designLabel, PACK_DEFAULT)
                    .withValues(values)
                    .displayOnlyValue()
                    .create(buttonX, y + 1 + i * LIST_ROW, DESIGN_BUTTON_WIDTH, 20, Component.literal("Design"),
                            (button, value) -> setDesign(row, value))));
        }

        activateSearch(searchBox);
    }

    private static Component designLabel(String value) {
        if (value.isEmpty()) return Component.literal("Pack default");
        return Component.literal(value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1));
    }

    private Map<String, String> currentOverrides() {
        return designMode == DesignMode.CATEGORIES ? DesignOverrides.categories() : DesignOverrides.entities();
    }

    private void setDesign(int row, String design) {
        int index = scroll + row;
        if (index >= shownIds.size()) return;

        String key = shownIds.get(index);
        if (design.isEmpty()) {
            currentOverrides().remove(key);
        } else {
            currentOverrides().put(key, design);
        }
        DesignOverrides.save();
    }

    private void resetAllDesigns() {
        DesignOverrides.categories().clear();
        DesignOverrides.entities().clear();
        DesignOverrides.save();
        refreshRows();
    }

    /** The design an entry really gets right now, for the preview heart. */
    private HeartDesign previewDesign(String key) {
        if (designMode == DesignMode.CATEGORIES) {
            return HeartDesignManager.design(HeartDesignManager.resolveCategory(key));
        }
        String category = EntityFilter.categoryName(entityTypes.get(key));
        return HeartDesignManager.design(HeartDesignManager.resolve(key, category));
    }

    private int designButtonX() {
        return listLeft + CONTENT_WIDTH - SCROLLBAR - 2 - DESIGN_BUTTON_WIDTH;
    }

    // ---------- shared list logic (Lists + Designs) ----------

    private EditBox addSearchBox(int x, int y, int width) {
        EditBox searchBox = new EditBox(this.font, x, y, width, 20, Component.literal("Search"));
        searchBox.setHint(Component.literal("Search mobs..."));
        searchBox.setMaxLength(64);
        searchBox.setValue(search);
        return addRenderableWidget(searchBox);
    }

    /** Hooks up the search box after the rows exist, then fills the rows. */
    private void activateSearch(EditBox searchBox) {
        if (searchBox != null) {
            searchBox.setResponder(text -> {
                search = text;
                scroll = 0;
                refreshRows();
            });
        }
        refreshRows();
    }

    private List<String> searchedEntities() {
        String query = search.trim().toLowerCase(Locale.ROOT);
        return allEntityIds.stream()
                .filter(id -> query.isEmpty() || id.contains(query)
                        || entityNames.get(id).getString().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private void refreshRows() {
        if (tab == Tab.LISTS) {
            List<String> selected = currentList();
            shownIds = searchedEntities().stream()
                    .filter(id -> !selectedOnly || selected.contains(id))
                    .toList();
            scroll = clampScroll(scroll, shownIds.size());
            for (int i = 0; i < rowToggles.size(); i++) {
                int index = scroll + i;
                CycleButton<Boolean> toggle = rowToggles.get(i);
                toggle.visible = index < shownIds.size();
                if (toggle.visible) toggle.setValue(selected.contains(shownIds.get(index)));
            }
            updateModeLabels();
        } else if (tab == Tab.DESIGNS) {
            shownIds = designMode == DesignMode.CATEGORIES ? CATEGORY_NAMES : searchedEntities();
            scroll = clampScroll(scroll, shownIds.size());
            Map<String, String> overrides = currentOverrides();
            for (int i = 0; i < rowDesigns.size(); i++) {
                int index = scroll + i;
                CycleButton<String> button = rowDesigns.get(i);
                button.visible = index < shownIds.size();
                if (button.visible) button.setValue(overrides.getOrDefault(shownIds.get(index), PACK_DEFAULT));
            }
        }
    }

    private Component rowLabel(String key) {
        if (tab == Tab.DESIGNS && designMode == DesignMode.CATEGORIES) {
            return Component.literal(key.substring(0, 1).toUpperCase(Locale.ROOT) + key.substring(1));
        }
        return entityNames.get(key);
    }

    private int clampScroll(int scroll, int size) {
        return Math.max(0, Math.min(scroll, size - listRows));
    }

    private void extractList(GuiGraphicsExtractor graphics) {
        int listHeight = listRows * LIST_ROW;
        int right = listLeft + CONTENT_WIDTH;
        int trackX = right - SCROLLBAR;

        graphics.fill(listLeft, listTop, right, listTop + listHeight, LIST_BACKGROUND);

        for (int i = 0; i < listRows; i++) {
            int index = scroll + i;
            if (index >= shownIds.size()) break;
            String key = shownIds.get(index);
            int rowY = listTop + i * LIST_ROW;

            if (i % 2 == 1) {
                graphics.fill(listLeft, rowY, trackX, rowY + LIST_ROW, LIST_STRIPE);
            }
            graphics.text(this.font, rowLabel(key).getVisualOrderText(), listLeft + 6, rowY + 7, 0xFFFFFFFF, true);

            if (tab == Tab.DESIGNS) {
                HeartDesign design = previewDesign(key);
                int px = designButtonX() - PREVIEW_SIZE - 4;
                int py = rowY + (LIST_ROW - PREVIEW_SIZE) / 2;
                graphics.blit(RenderPipelines.GUI_TEXTURED, design.container(), px, py, 0f, 0f, PREVIEW_SIZE, PREVIEW_SIZE, PREVIEW_SIZE, PREVIEW_SIZE);
                graphics.blit(RenderPipelines.GUI_TEXTURED, design.full(), px, py, 0f, 0f, PREVIEW_SIZE, PREVIEW_SIZE, PREVIEW_SIZE, PREVIEW_SIZE);
            }
        }

        if (shownIds.isEmpty()) {
            Component empty = Component.literal("No mobs found");
            graphics.text(this.font, empty.getVisualOrderText(),
                    listLeft + (CONTENT_WIDTH - this.font.width(empty)) / 2, listTop + listHeight / 2 - 4, 0xFFFFFFFF, true);
        }

        // scrollbar
        graphics.fill(trackX, listTop, right, listTop + listHeight, SCROLL_TRACK);
        if (shownIds.size() > listRows) {
            int thumbHeight = Math.max(10, listHeight * listRows / shownIds.size());
            int thumbY = listTop + (listHeight - thumbHeight) * scroll / (shownIds.size() - listRows);
            graphics.fill(trackX, thumbY, right, thumbY + thumbHeight, SCROLL_THUMB);
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        boolean overList = x >= listLeft && x < listLeft + CONTENT_WIDTH
                && y >= listTop && y < listTop + listRows * LIST_ROW;
        if (tab != Tab.GENERAL && overList) {
            scroll += scrollY > 0 ? -1 : 1;
            refreshRows();
            return true;
        }
        int totalRows = rows(MhdSettings.SCREEN_ORDER.size());
        boolean overPanel = x >= panelLeft && x < panelLeft + PANEL_WIDTH && y >= panelTop && y < panelTop + panelHeight;
        if (tab == Tab.GENERAL && overPanel && totalRows > generalRows) {
            int newScroll = Math.max(0, Math.min(generalScroll + (scrollY > 0 ? -1 : 1), totalRows - generalRows));
            if (newScroll != generalScroll) {
                generalScroll = newScroll;
                rebuildWidgets();
            }
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
        if (tab != Tab.GENERAL) {
            extractList(graphics);
        } else {
            extractGeneralScrollbar(graphics);
        }
        super.extractRenderState(graphics, mouseX, mouseY, a); // buttons on top
    }

    /** Thin scrollbar in the right border of the panel, only when not all settings fit. */
    private void extractGeneralScrollbar(GuiGraphicsExtractor graphics) {
        int totalRows = rows(MhdSettings.SCREEN_ORDER.size());
        if (totalRows <= generalRows) return;
        int x = panelLeft + PADDING + CONTENT_WIDTH + 1;
        int trackHeight = generalRows * ROW - GAP;
        int thumbHeight = Math.max(10, trackHeight * generalRows / totalRows);
        int thumbY = contentTop + (trackHeight - thumbHeight) * generalScroll / (totalRows - generalRows);
        graphics.fill(x, contentTop, x + 3, contentTop + trackHeight, SCROLL_TRACK);
        graphics.fill(x, thumbY, x + 3, thumbY + thumbHeight, SCROLL_THUMB);
    }

    private void addTabButton(Tab target, String name, int x, int y) {
        Button button = Button.builder(Component.literal(name), b -> {
            tab = target;
            scroll = 0;
            rebuildWidgets();
        }).bounds(x, y, TAB_WIDTH, 20).build();
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

    private void addDesignModeButton(DesignMode mode, String name, int x, int y) {
        Button button = Button.builder(Component.literal(name), b -> {
            designMode = mode;
            scroll = 0;
            rebuildWidgets();
        }).bounds(x, y, BUTTON_WIDTH, 20).build();
        button.active = designMode != mode;
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
        super.extractBackground(graphics, mouseX, mouseY, a); // like vanilla: panorama without a world, then blur
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL, panelLeft, panelTop, PANEL_WIDTH, panelHeight);
    }

    @Override
    public void onClose() {
        MhdConfig.save();
        this.minecraft.gui.setScreen(parent);
    }
}