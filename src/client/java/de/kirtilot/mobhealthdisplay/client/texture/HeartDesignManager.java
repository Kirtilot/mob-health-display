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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Decides which design an entity gets. Order:
 * player's entity override -> pack entity -> player's category override -> pack category -> pack default.
 * Designs that are not available right now (e.g. pack disabled) are skipped.
 */
public final class HeartDesignManager {
    private static final String MOD_ID = "mob_health_display";
    private static final Identifier FILE = Identifier.fromNamespaceAndPath(MOD_ID, "designs.json");
    private static final String DESIGN_FOLDER = "textures/designs/";

    private static String defaultDesign = HeartDesign.VANILLA_NAME;
    private static final Map<String, String> CATEGORIES = new HashMap<>(); // key: category name in lower case
    private static final Map<String, String> ENTITIES = new HashMap<>();
    private static final Map<String, HeartDesign> DESIGNS = new HashMap<>();
    private static final Set<String> AVAILABLE = new TreeSet<>();
    private static final Map<String, Set<String>> FILES = new HashMap<>(); // design name -> its png files

    private HeartDesignManager() {}

    /** Called on game start and whenever resource packs are reloaded (F3+T, pack changes). */
    public static void reload(ResourceManager manager) {
        defaultDesign = HeartDesign.VANILLA_NAME;
        CATEGORIES.clear();
        ENTITIES.clear();
        DESIGNS.clear();
        AVAILABLE.clear();
        FILES.clear();
        AVAILABLE.add(HeartDesign.VANILLA_NAME);

        // textures/designs/<name>/<file>.png -> remember the file for design <name>
        // a design with a full.png is available
        manager.listResources("textures/designs", id -> id.getPath().endsWith(".png")).keySet().forEach(id -> {
            if (!id.getNamespace().equals(MOD_ID)) return;
            String path = id.getPath();
            int slash = path.lastIndexOf('/');
            if (slash < DESIGN_FOLDER.length()) return; // png directly in textures/designs, not in a design folder
            String name = path.substring(DESIGN_FOLDER.length(), slash);
            String file = path.substring(slash + 1);
            FILES.computeIfAbsent(name, n -> new HashSet<>()).add(file);
            if (file.equals("full.png")) AVAILABLE.add(name);
        });

        manager.getResource(FILE).ifPresent(resource -> {
            try (Reader reader = resource.openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

                if (root.has("default")) {
                    defaultDesign = root.get("default").getAsString();
                }
                if (root.has("categories")) {
                    for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("categories").entrySet()) {
                        CATEGORIES.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue().getAsString());
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

    /** The design an entity in the world gets. */
    public static HeartDesign get(LivingEntity entity) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        String category = EntityCategory.of(entity).name().toLowerCase(Locale.ROOT);
        return design(resolve(id, category));
    }

    /** Name of the design for an entity id in the given category (full order, see class comment). */
    public static String resolve(String entityId, String category) {
        return firstAvailable(
                DesignOverrides.entities().get(entityId),
                ENTITIES.get(entityId),
                DesignOverrides.categories().get(category),
                CATEGORIES.get(category),
                defaultDesign);
    }

    /** Name of the design a whole category gets (ignores single-entity entries). */
    public static String resolveCategory(String category) {
        return firstAvailable(
                DesignOverrides.categories().get(category),
                CATEGORIES.get(category),
                defaultDesign);
    }

    public static HeartDesign design(String name) {
        return DESIGNS.computeIfAbsent(name, n -> HeartDesign.named(n, FILES.getOrDefault(n, Set.of())));
    }

    private static String firstAvailable(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && AVAILABLE.contains(candidate)) return candidate;
        }
        return HeartDesign.VANILLA_NAME;
    }

    /** All designs found in the active resource packs, plus "vanilla". */
    public static Set<String> availableDesigns() {
        return Collections.unmodifiableSet(AVAILABLE);
    }

    public static boolean isDesign(String name) {
        return AVAILABLE.contains(name);
    }
}