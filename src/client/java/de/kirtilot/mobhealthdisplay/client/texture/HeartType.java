package de.kirtilot.mobhealthdisplay.client.texture;

/**
 * Which heart textures are used, like the vanilla HUD.
 * The prefix is put in front of the file name: "poisoned_" + "full.png" = poisoned_full.png
 */
public enum HeartType {
    NORMAL(""),
    POISONED("poisoned_"),
    WITHERED("withered_"),
    FROZEN("frozen_"),
    ABSORBING("absorbing_"); // used later for absorption hearts

    private final String prefix;

    HeartType(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }
}