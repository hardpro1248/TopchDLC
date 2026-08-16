package gg.topchdlc.vse.shutki.module.settings;

import com.google.gson.JsonObject;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.utils.other.MultiBoolSupplier;
import gg.topchdlc.vse.utils.lang.LangUtility;
import lombok.Getter;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class Setting<T> implements MinecraftHolder {
    protected T value;
    public String name;
    protected final T defaultValue;
    public Group base;
    public String _desc = "";

    public String getDesc() {
        return LangUtility.get(getLangKey() + "._description", "");
    }

    public String getName() {
        return LangUtility.get(getLangKey() + "._name", name);
    }

    public String getLangKey() {
        if (base == null) {
            return name;
        }
        return base.getLangKey() + "." + name;
    }

    @Getter
    protected Supplier<Boolean> visible = () -> true;
    protected Consumer<T> onChanged = ignored -> {};

    protected Setting(String name, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        onChange();
    }

    protected void onChange() {
        onChanged.accept(value);
    }

    public void resetSetting() {
        this.value = defaultValue;
        onChange();
    }

    public abstract SettingRenderer<?> wrap();

    public abstract void save(JsonObject json);

    public abstract void load(JsonObject json);

    public static abstract class Basic<T, Self extends Basic<T, Self>> extends Setting<T> {
        protected Basic(String name, T defaultValue) {
            super(name, defaultValue);
        }

        @SuppressWarnings("unchecked")
        protected Self self() {
            return (Self) this;
        }
        @Deprecated
        public Self desc(String desc) {
            this._desc = desc;
            return self();
        }

        @SafeVarargs
        public final Self visible(Supplier<Boolean>... visible) {
            this.visible = new MultiBoolSupplier(visible);
            return self();
        }

        public Self onChanged(Consumer<T> onChanged) {
            this.onChanged = onChanged;
            return self();
        }
    }
}