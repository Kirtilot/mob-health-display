package de.kirtilot.mobhealthdisplay.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MhiConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve("mob_health_display.json");
    private static MhiConfig instance = new MhiConfig();

    public boolean showHearts = true;
    public boolean showHostile = true;
    public boolean showPassive = true;
    public boolean showSelf = false;
    public boolean onlyShowDamaged = false;

    public boolean blackList = false;
    public boolean whiteList = false;
    public List<String> blackListEntries = new ArrayList<>();
    public List<String> whiteListEntries = new ArrayList<>();

    public boolean showBarCount = true;
    public float height = 0.0f;
    public float hpPerBar = 20.0f;

    public static MhiConfig get() {
        return instance;
    }

    public static void load() {
        if (Files.exists(PATH)) {
            try {
                MhiConfig loaded = GSON.fromJson(Files.readString(PATH), MhiConfig.class);
                if (loaded != null) instance = loaded;
            } catch (Exception e) {
                System.err.println("[MobHealthDisplay] Could not read config, using defaults: " + e);
            }
        }
        save(); // writes defaults and adds fields that are new in this version
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(instance));
        } catch (IOException e) {
            System.err.println("[MobHealthDisplay] Could not save config: " + e);
        }
    }
}