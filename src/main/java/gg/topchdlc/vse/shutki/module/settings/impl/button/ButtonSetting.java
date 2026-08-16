package gg.topchdlc.vse.shutki.module.settings.impl.button;

import com.google.gson.JsonObject;
import gg.topchdlc.vse.shutki.module.settings.Setting;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;

public class ButtonSetting extends Setting.Basic<Runnable, ButtonSetting> {
    public ButtonSetting(String name, Runnable action) {
        super(name, action);
    }

    public void execute() {
        if (value != null) {
            value.run();
        }
    }

    @Override
    public SettingRenderer<?> wrap() {
        return new ButtonSettingRenderer(this);
    }

    @Override
    public void save(JsonObject json) {
    }

    @Override
    public void load(JsonObject json) {
    }
}
