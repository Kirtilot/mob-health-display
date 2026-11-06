package de.kirtilot.mobhealthdisplay.client.config;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Every setting in one place, shared by the /mhd command and the settings screen. */
public final class MhdSettings {
    private MhdSettings() {}

    public record BoolSetting(String name, String label, Supplier<Boolean> get, Consumer<Boolean> set) {}
    public record FloatSetting(String name, String label, Supplier<Float> get, Consumer<Float> set, float min, float max) {}

    public static final List<BoolSetting> BOOLEANS = List.of(
            new BoolSetting("showHearts", "Show Hearts", () -> MhdConfig.get().showHearts, v -> MhdConfig.get().showHearts = v),
            new BoolSetting("showHostile", "Hostile Mobs", () -> MhdConfig.get().showHostile, v -> MhdConfig.get().showHostile = v),
            new BoolSetting("showPassive", "Passive Mobs", () -> MhdConfig.get().showPassive, v -> MhdConfig.get().showPassive = v),
            new BoolSetting("showSelf", "Show Yourself", () -> MhdConfig.get().showSelf, v -> MhdConfig.get().showSelf = v),
            new BoolSetting("onlyShowDamaged", "Only Damaged", () -> MhdConfig.get().onlyShowDamaged, v -> MhdConfig.get().onlyShowDamaged = v),
            new BoolSetting("showBarCount", "Bar Count", () -> MhdConfig.get().showBarCount, v -> MhdConfig.get().showBarCount = v),
            new BoolSetting("blackList", "Blacklist", () -> MhdConfig.get().blackList, v -> MhdConfig.get().blackList = v),
            new BoolSetting("whiteList", "Whitelist", () -> MhdConfig.get().whiteList, v -> MhdConfig.get().whiteList = v)
    );

    public static final List<FloatSetting> FLOATS = List.of(
            new FloatSetting("height", "Height Offset", () -> MhdConfig.get().height, v -> MhdConfig.get().height = v, -5f, 5f),
            new FloatSetting("hpPerBar", "HP per Bar", () -> MhdConfig.get().hpPerBar, v -> MhdConfig.get().hpPerBar = v, 1f, 1000f)
    );
}