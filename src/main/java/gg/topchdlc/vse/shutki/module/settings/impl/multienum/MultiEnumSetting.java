package gg.topchdlc.vse.shutki.module.settings.impl.multienum;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.Setting;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import lombok.Getter;

import java.util.*;

public class MultiEnumSetting<E extends Enum<E>> extends Setting.Basic<SortedSet<E>, MultiEnumSetting<E>> {
    private final HashMap<String, E> constants = new HashMap<>();
    @Getter
    private final ArrayList<E> enumEntries = new ArrayList<>();

    @Getter
    private MultiEnumCallback<E> callback = (a, b) -> {};

    public MultiEnumSetting(String name, Class<E> clazz) {
        super(name, new TreeSet<>(Comparator.comparingInt(Enum::ordinal)));
        initConstants(clazz);
    }

    @SafeVarargs
    public MultiEnumSetting(String name, E... defaults) {
        super(name, new TreeSet<>(Comparator.comparingInt(Enum::ordinal)));
        initConstants(defaults[0].getDeclaringClass());
        value.addAll(List.of(defaults));
    }

    private void initConstants(Class<E> clazz) {
        for (E c : clazz.getEnumConstants()) {
            constants.put(c.name().toLowerCase(Locale.ROOT), c);
            if (c instanceof EnumChoice enumChoice) {
                constants.put(enumChoice.getRenderName().toLowerCase(Locale.ROOT), c);
                if (enumChoice.isDefaultEnabled()) value.add(c);
            }
            enumEntries.add(c);
        }
    }

    public MultiEnumSetting<E> onChange(MultiEnumCallback<E> callback) {
        this.callback = callback;
        return this;
    }

    public Set<E> get() {
        return this.value;
    }

    public boolean isEmpty() {
        return value.isEmpty();
    }

    public void toggle(E entry) {
        if (!value.remove(entry)) {
            callback.callback(entry, true);
            value.add(entry);
        } else {
            callback.callback(entry, false);
        }
    }
    public void toggle(int entry) {
        toggle(enumEntries.get(entry));
    }

    public boolean get(E entry) {
        return value.contains(entry);
    }

    @SafeVarargs
    public final MultiEnumSetting<E> select(E... entries) {
        value.addAll(Arrays.asList(entries));
        return this;
    }

    @Override
    public SettingRenderer<?> wrap() {
        return new MultiEnumRenderer<>(this);
    }

    @Override
    public void save(JsonObject json) {
        String name = this.name.concat(":multi");

        JsonArray array = new JsonArray();
        for (E value : value) {
            array.add(value.name());
        }
        json.add(name, array);
    }

    @Override
    public void load(JsonObject json) {
        String name1 = this.name.concat(":multi");
        if (json.has(name1)) {
            HashSet<String> values = new HashSet<String>();
            JsonArray array = json.get(name1).getAsJsonArray();
            for (JsonElement v : array) {
                String name = v.getAsString();
                values.add(name);
            }
            this.value.clear();
            for (String v : values) {
                this.value.add(this.constants.get(v.toLowerCase(Locale.ROOT)));
            }
        }
    }

    public interface MultiEnumCallback<E extends Enum<E>> {
        void callback(E mode, boolean value);
    }
}
