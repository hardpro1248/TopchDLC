package gg.topchdlc.vse.shutki.module.settings.impl.point;

import com.google.gson.JsonObject;
import gg.topchdlc.vse.shutki.module.settings.Setting;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import org.joml.Vector2f;

public class PointSetting extends Setting.Basic<Vector2f, PointSetting> {
    public PointSetting(String name, Vector2f defaultValue) {
        super(name, defaultValue);
    }

    public PointSetting set(Vector2f vec) {
        value = vec;
        onChange();
        return this;
    }

    public Vector2f get() { return value; }

    @Override
    public SettingRenderer<?> wrap() {
        return new PointRenderer(this);
    }

    @Override
    public void save(JsonObject json) {
        String name = this.name.concat(":point");

        JsonObject obj = new JsonObject();
        obj.addProperty("x", value.x);
        obj.addProperty("y", value.y);

        json.add(name, obj);
    }

    @Override
    public void load(JsonObject json) {
        String name = this.name.concat(":point");

        if (json.has(name)) {
            JsonObject obj = json.getAsJsonObject(name);

            this.value = new Vector2f(obj.get("x").getAsFloat(), obj.get("y").getAsFloat());
        }
    }
}