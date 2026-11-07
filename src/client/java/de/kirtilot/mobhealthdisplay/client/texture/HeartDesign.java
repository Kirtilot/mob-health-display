package de.kirtilot.mobhealthdisplay.client.texture;

import net.minecraft.resources.Identifier;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * The textures of one heart design.
 * divider is optional (null if the design has no divider.png): the lines left and right of "xN".
 * Effect textures (poisoned_full.png, frozen_half.png, ...) are optional too:
 * if a design doesn't have one, the vanilla heart of the game is used instead.
 */
public record HeartDesign(Identifier container, Identifier divider,
                          Map<HeartType, Identifier> fullTextures, Map<HeartType, Identifier> halfTextures) {

    public static final String VANILLA_NAME = "vanilla";
    private static final String DESIGN_FOLDER = "textures/designs/";
    private static final String VANILLA_FOLDER = "textures/gui/sprites/hud/heart/";

    /** Normal full heart (used e.g. by the preview in the settings screen). */
    public Identifier full() {
        return full(HeartType.NORMAL);
    }

    public Identifier half() {
        return half(HeartType.NORMAL);
    }

    public Identifier full(HeartType type) {
        return fullTextures.get(type);
    }

    public Identifier half(HeartType type) {
        return halfTextures.get(type);
    }

    /**
     * "undead" becomes mob_health_display:textures/designs/undead/{container,full,half,divider}.png
     * files: names of all png files in the design folder, e.g. "full.png", "poisoned_full.png"
     */
    public static HeartDesign named(String name, Set<String> files) {
        try {
            boolean vanilla = VANILLA_NAME.equals(name);
            String base = DESIGN_FOLDER + name + "/";
            Identifier divider = files.contains("divider.png") ? id(base + "divider.png") : null;
            Identifier container = vanilla ? vanilla("container.png") : id(base + "container.png");

            Map<HeartType, Identifier> fulls = new EnumMap<>(HeartType.class);
            Map<HeartType, Identifier> halves = new EnumMap<>(HeartType.class);
            for (HeartType type : HeartType.values()) {
                fulls.put(type, texture(vanilla, base, files, type.prefix() + "full.png", type));
                halves.put(type, texture(vanilla, base, files, type.prefix() + "half.png", type));
            }
            return new HeartDesign(container, divider, fulls, halves);
        } catch (Exception e) {
            System.err.println("[MobHealthDisplay] Invalid design name '" + name + "', using vanilla");
            return named(VANILLA_NAME, Set.of());
        }
    }

    /** The design's own file if it has one; otherwise the vanilla heart (normal hearts always come from the design). */
    private static Identifier texture(boolean vanilla, String base, Set<String> files, String file, HeartType type) {
        if (vanilla) return vanilla(file);
        if (type == HeartType.NORMAL || files.contains(file)) return id(base + file);
        return vanilla(file);
    }

    /** the heart textures of the game (and of any active resource pack) */
    private static Identifier vanilla(String file) {
        return Identifier.withDefaultNamespace(VANILLA_FOLDER + file);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("mob_health_display", path);
    }
}