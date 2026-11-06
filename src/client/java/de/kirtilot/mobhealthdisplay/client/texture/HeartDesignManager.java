package de.kirtilot.mobhealthdisplay.client.texture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.kirtilot.mobhealthdisplay.client.filter.EntityCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.LivingEntity;

import java.io.Reader;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Reads assets/mob_health_display/designs.json from the active resource packs
 * and decides which design an entity gets: single entity -> category -> default.
 */
public final class HeartDesignManager {
    private static final Identifier FILE = Identifier.fromNamespaceAndPath("mob_health_display", "designs.json");

    private static String defaultDesign = HeartDesign.VANILLA_NAME;
    private static final Map<EntityCategory, String> CATEGORIES = new EnumMap<>(EntityCategory.class);
    private static final Map<String, String> ENTITIES = new HashMap<>();
    private static final Map<String, HeartDesign> DESIGNS = new HashMap<>();

    private HeartDesignManager() {}

    /** Called on game start and whenever resource packs are reloaded (F3+T, pack changes). */
    public static void reload(ResourceManager manager) {
        defaultDesign = HeartDesign.VANILLA_NAME;
        CATEGORIES.clear();
        ENTITIES.clear();
        DESIGNS.clear();

        manager.getResource(FILE).ifPresent(resource -> {
            try (Reader reader = resource.openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

                if (root.has("default")) {
                    defaultDesign = root.get("default").getAsString();
                }
                if (root.has("categories")) {
                    for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("categories").entrySet()) {
                        try {
                            CATEGORIES.put(EntityCategory.valueOf(e.getKey().toUpperCase(Locale.ROOT)), e.getValue().getAsString());
                        } catch (IllegalArgumentException unknownCategory) {
                            System.err.println("[MobHealthDisplay] Unknown category in designs.json: " + e.getKey());
                        }
                    }
                }
                if (root.has("entities")) {
                    for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("entities").entrySet()) {
                        ENTITIES.put(e.getKey(), e.getValue().getAsString());
                    }
                }
            } catch (Exception e) {
                System.err.println("[MobHealthDisplay] Could not read designs.json: " + e);
            }
        });
    }

    public static HeartDesign get(LivingEntity entity) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        String name = ENTITIES.get(id);
        if (name == null) name = CATEGORIES.get(EntityCategory.of(entity));
        if (name == null) name = defaultDesign;
        return DESIGNS.computeIfAbsent(name, HeartDesign::named);
    }
}