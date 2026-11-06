package de.kirtilot.mobhealthdisplay.client.texture;

import net.minecraft.resources.Identifier;

/** The three textures of one heart design. */
public record HeartDesign(Identifier container, Identifier full, Identifier half) {

    public static final String VANILLA_NAME = "vanilla";

    /** Uses the normal heart textures of the game (and of any active resource pack). */
    public static final HeartDesign VANILLA = new HeartDesign(
            Identifier.withDefaultNamespace("textures/gui/sprites/hud/heart/container.png"),
            Identifier.withDefaultNamespace("textures/gui/sprites/hud/heart/full.png"),
            Identifier.withDefaultNamespace("textures/gui/sprites/hud/heart/half.png"));

    /** "undead" becomes mob_health_display:textures/designs/undead/{container,full,half}.png */
    public static HeartDesign named(String name) {
        if (VANILLA_NAME.equals(name)) return VANILLA;
        try {
            String base = "textures/designs/" + name + "/";
            return new HeartDesign(id(base + "container.png"), id(base + "full.png"), id(base + "half.png"));
        } catch (Exception e) {
            System.err.println("[MobHealthDisplay] Invalid design name '" + name + "', using vanilla");
            return VANILLA;
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("mob_health_display", path);
    }
}