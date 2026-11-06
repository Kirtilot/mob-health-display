package de.kirtilot.mobhealthdisplay.client.texture;

import net.minecraft.resources.Identifier;

/**
 * The textures of one heart design.
 * divider is optional (null if the design has no divider.png): the lines left and right of "xN".
 */
public record HeartDesign(Identifier container, Identifier full, Identifier half, Identifier divider) {

    public static final String VANILLA_NAME = "vanilla";
    private static final String DESIGN_FOLDER = "textures/designs/";

    // the normal heart textures of the game (and of any active resource pack)
    private static final Identifier VANILLA_CONTAINER = Identifier.withDefaultNamespace("textures/gui/sprites/hud/heart/container.png");
    private static final Identifier VANILLA_FULL = Identifier.withDefaultNamespace("textures/gui/sprites/hud/heart/full.png");
    private static final Identifier VANILLA_HALF = Identifier.withDefaultNamespace("textures/gui/sprites/hud/heart/half.png");

    /** "undead" becomes mob_health_display:textures/designs/undead/{container,full,half,divider}.png */
    public static HeartDesign named(String name, boolean hasDivider) {
        try {
            String base = DESIGN_FOLDER + name + "/";
            Identifier divider = hasDivider ? id(base + "divider.png") : null;
            if (VANILLA_NAME.equals(name)) {
                return new HeartDesign(VANILLA_CONTAINER, VANILLA_FULL, VANILLA_HALF, divider);
            }
            return new HeartDesign(id(base + "container.png"), id(base + "full.png"), id(base + "half.png"), divider);
        } catch (Exception e) {
            System.err.println("[MobHealthDisplay] Invalid design name '" + name + "', using vanilla");
            return new HeartDesign(VANILLA_CONTAINER, VANILLA_FULL, VANILLA_HALF, null);
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("mob_health_display", path);
    }
}