package gg.topchdlc.vse.shutki.module.settings.impl.enumsetting;

import com.google.gson.JsonObject;
import gg.topchdlc.vse.shutki.module.settings.Setting;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.Function;

@Accessors(fluent = true, chain = true)
public class EnumSetting<E extends Enum<E>> extends Setting.Basic<E, EnumSetting<E>> {
    @Setter
    private Function<E, E> onChange = e -> e;

    public EnumSetting(String name, E value) {
        super(name, value);
        select(value);
    }

    public E get() {
        return value;
    }

    public EnumSetting<E> select(E value) {
        value = onChange.apply(value);
        this.value = value;
        onChange();
        return this;
    }
    public EnumSetting<E> select(int ord) {
        E value = get().getDeclaringClass().getEnumConstants()[ord];
        value = onChange.apply(value);
        this.value = value;
        onChange();
        return this;
    }

    public boolean is(E is) {
        return this.value == is;
    }
    public boolean isAny(E... is) {
        for (E iz : is) if (iz == value) return true;
        return false;
    }
    public boolean is(int is) {
        return is(this.value.getDeclaringClass().getEnumConstants()[is]);
    }

    @Override
    public SettingRenderer<?> wrap() {
        return new EnumRenderer<>(this);
    }

    @Override
    public void save(JsonObject json) {
        String name = this.name.concat(":enum");
        json.addProperty(name, this.value.name());
    }

    @Override
    public void load(JsonObject json) {
        String name = this.name.concat(":enum");
        if (json.has(name)) {
            String v = json.get(name).getAsString();
            for (E c : this.value.getDeclaringClass().getEnumConstants()) {
                if (c.name().equalsIgnoreCase(v)) {
                    this.select(c);
                }
            }
        }
    }
}
