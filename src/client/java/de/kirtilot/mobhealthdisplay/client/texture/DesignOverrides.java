package de.kirtilot.mobhealthdisplay.client.texture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/** The player's own design choices, set with /mhd design. They win over the resource pack. */
public final class DesignOverrides {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("mob_health_display_designs.json");

    /** Same layout as designs.json: { "categories": {...}, "entities": {...} } */
    private static final class Data {
        Map<String, String> categories = new TreeMap<>();
        Map<String, String> entities = new TreeMap<>();
    }

    private static Data data = new Data();

    private DesignOverrides() {}

    public static void load() {
        if (Files.exists(PATH)) {
            try {
                Data loaded = GSON.fromJson(Files.readString(PATH), Data.class);
                if (loaded != null) data = loaded;
            } catch (Exception e) {
                System.err.println("[MobHealthDisplay] Could not read design overrides: " + e);
            }
        }
        if (data.categories == null) data.categories = new TreeMap<>();
        if (data.entities == null) data.entities = new TreeMap<>();
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(data));
        } catch (IOException e) {
            System.err.println("[MobHealthDisplay] Could not save design overrides: " + e);
        }
    }

    public static Map<String, String> categories() {
        return data.categories;
    }

    public static Map<String, String> entities() {
        return data.entities;
    }
}