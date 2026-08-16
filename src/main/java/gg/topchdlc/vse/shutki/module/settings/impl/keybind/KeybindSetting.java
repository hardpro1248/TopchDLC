package gg.topchdlc.vse.shutki.module.settings.impl.keybind;

import com.google.gson.JsonObject;
import gg.topchdlc.api.events.list.EventKey;
import gg.topchdlc.vse.shutki.module.settings.Setting;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;

public class KeybindSetting extends Setting.Basic<Integer, KeybindSetting> {
    public KeybindSetting(String name, int defaultValue) {
        super(name, defaultValue);
    }

    public KeybindSetting bind(int key) {
        this.value = key;
        onChange();
        return this;
    }
    public int getBind() {
        return value;
    }

    public boolean matches(EventKey e) {
        return e.key == value;
    }

    @Override
    public SettingRenderer<?> wrap() {
        return new KeybindRenderer(this);
    }

    @Override
    public void save(JsonObject json) {
        String name = this.name.concat(":keybind");
        json.addProperty(name, value);
    }

    @Override
    public void load(JsonObject json) {
        String name = this.name.concat(":keybind");

        if (json.has(name)) {
            value = json.get(name).getAsInt();
        }
    }
}
