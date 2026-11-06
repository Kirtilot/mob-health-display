package de.kirtilot.mobhealthdisplay.client.screen;

import de.kirtilot.mobhealthdisplay.client.config.MhdSettings;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Slider for one number setting, snapping to the setting's step size. */
public class SettingSlider extends AbstractSliderButton {
    private final MhdSettings.FloatSetting setting;

    public SettingSlider(int x, int y, int width, int height, MhdSettings.FloatSetting setting) {
        super(x, y, width, height, Component.empty(), toSlider(setting, setting.get().get()));
        this.setting = setting;
        updateMessage();
    }

    private static double toSlider(MhdSettings.FloatSetting s, float value) {
        return (value - s.min()) / (s.max() - s.min());
    }

    private float currentValue() {
        float raw = setting.min() + (float) value * (setting.max() - setting.min());
        return Math.round(raw / setting.step()) * setting.step();
    }

    @Override
    protected void updateMessage() {
        float v = currentValue();
        String text;
        if (setting.zeroLabel() != null && v == 0f) {
            text = setting.zeroLabel();
        } else if (setting.step() >= 1f) {
            text = String.valueOf(Math.round(v));
        } else {
            text = String.format(Locale.ROOT, "%.2f", v);
        }
        setMessage(Component.literal(setting.label() + ": " + text));
    }

    @Override
    protected void applyValue() {
        setting.set().accept(currentValue());
    }
}