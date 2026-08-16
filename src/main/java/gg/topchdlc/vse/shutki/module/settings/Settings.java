package gg.topchdlc.vse.shutki.module.settings;

import com.google.gson.JsonObject;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class Settings implements Iterable<Setting<?>> {
    @Getter
    private final List<Setting<?>> settings = new ObjectArrayList<>();
    @Getter
    private final List<SettingRenderer<?>> settingRenderers = new ObjectArrayList<>();

    public <S extends Setting<?>> S add(S setting) {
        this.settings.add(setting);
        this.settingRenderers.add(setting.wrap());
        return setting;
    }

    public void addAll(Setting<?>... settings) {
        this.settings.addAll(Arrays.asList(settings));
        for (Setting<?> s : settings) {
            this.settingRenderers.add(s.wrap());
        }
    }

    public boolean isEmpty() {
        return this.settings.isEmpty();
    }

    public void addAll(Collection<Setting<?>> settings) {
        this.settings.addAll(settings);
        for (Setting<?> s : settings) {
            this.settingRenderers.add(s.wrap());
        }
    }

    public Group group(String name) {
        return add(new Group(name));
    }

    @Override
    public @NotNull Iterator<Setting<?>> iterator() {
        return settings.iterator();
    }

    public void save(JsonObject json) {
        for (Setting<?> setting : settings) {
            setting.save(json);
        }
    }

    public void load(JsonObject json) {
        for (Setting<?> setting : settings) {
            setting.load(json);
        }
    }
}
