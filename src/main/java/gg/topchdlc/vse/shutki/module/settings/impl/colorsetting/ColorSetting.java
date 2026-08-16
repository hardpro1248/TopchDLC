package gg.topchdlc.vse.shutki.module.settings.impl.colorsetting;

import com.google.gson.JsonObject;
import gg.topchdlc.vse.shutki.module.settings.Setting;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;

import java.awt.*;

public class ColorSetting extends Setting.Basic<Color, ColorSetting> {
    public ColorSetting(String name, Color defaultValue) {
        super(name, defaultValue);
    }

    @Override
    public void resetSetting() {
        this.color(defaultValue);
    }

    public ColorSetting color(Color color) {
        this.value = color;
        onChange();
        return this;
    }

    public Color get() {
        return value;
    }

    @Override
    public SettingRenderer<?> wrap() {
        return new ColorRenderer(this);
    }

    @Override
    public void save(JsonObject json) {
        String name = this.name.concat(":color");
        json.addProperty(name, value.getRGB());
    }

    @Override
    public void load(JsonObject json) {
        String name = this.name.concat(":color");

        if (json.has(name)) {
            color(new Color(json.get(name).getAsInt(), true));
        }
    }
}
