package de.kirtilot.mobhealthdisplay.client.config;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Every setting in one place, shared by the /mhd command and the settings screen. */
public final class MhdSettings {
    private MhdSettings() {}

    public record BoolSetting(String name, String label, Supplier<Boolean> get, Consumer<Boolean> set) {}

    /** zeroLabel: text shown instead of "0" (e.g. "Off"), or null for a plain number. */
    public record FloatSetting(String name, String label, Supplier<Float> get, Consumer<Float> set,
                               float min, float max, float step, String zeroLabel) {}

    public static final List<BoolSetting> BOOLEANS = List.of(
            new BoolSetting("showHearts", "Show Hearts", () -> MhdConfig.get().showHearts, v -> MhdConfig.get().showHearts = v),
            new BoolSetting("showHostile", "Hostile Mobs", () -> MhdConfig.get().showHostile, v -> MhdConfig.get().showHostile = v),
            new BoolSetting("showPassive", "Passive Mobs", () -> MhdConfig.get().showPassive, v -> MhdConfig.get().showPassive = v),
            new BoolSetting("showSelf", "Show Yourself", () -> MhdConfig.get().showSelf, v -> MhdConfig.get().showSelf = v),
            new BoolSetting("onlyShowDamaged", "Only Damaged", () -> MhdConfig.get().onlyShowDamaged, v -> MhdConfig.get().onlyShowDamaged = v),
            new BoolSetting("showBarCount", "Bar Count", () -> MhdConfig.get().showBarCount, v -> MhdConfig.get().showBarCount = v),
            new BoolSetting("showCountPrefix", "Count Prefix", () -> MhdConfig.get().showCountPrefix, v -> MhdConfig.get().showCountPrefix = v),
            new BoolSetting("showDividers", "Divider Lines", () -> MhdConfig.get().showDividers, v -> MhdConfig.get().showDividers = v),
            new BoolSetting("showEffects", "Effect Hearts", () -> MhdConfig.get().showEffects, v -> MhdConfig.get().showEffects = v),
            new BoolSetting("showAbsorption", "Absorption", () -> MhdConfig.get().showAbsorption, v -> MhdConfig.get().showAbsorption = v),
            new BoolSetting("blackList", "Blacklist", () -> MhdConfig.get().blackList, v -> MhdConfig.get().blackList = v),
            new BoolSetting("whiteList", "Whitelist", () -> MhdConfig.get().whiteList, v -> MhdConfig.get().whiteList = v)
    );

    public static final List<FloatSetting> FLOATS = List.of(
            new FloatSetting("height", "Height Offset", () -> MhdConfig.get().height, v -> MhdConfig.get().height = v, -2f, 2f, 0.05f, null),
            new FloatSetting("heartRows", "Heart Rows", () -> MhdConfig.get().heartRows, v -> MhdConfig.get().heartRows = v, 1f, 5f, 1f, null),
            new FloatSetting("hideAfterSeconds", "Hide After (s)", () -> MhdConfig.get().hideAfterSeconds, v -> MhdConfig.get().hideAfterSeconds = v, 0f, 60f, 1f, "Off"),
            new FloatSetting("maxRange", "Max Range", () -> MhdConfig.get().maxRange, v -> MhdConfig.get().maxRange = v, 0f, 128f, 1f, "Unlimited")
    );
}