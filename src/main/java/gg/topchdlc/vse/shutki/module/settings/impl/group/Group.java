package gg.topchdlc.vse.shutki.module.settings.impl.group;

import com.google.gson.JsonObject;
import gg.topchdlc.vse.shutki.module.settings.Setting;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.vse.shutki.module.settings.impl.blocklist.BlockListSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBoxRenderer;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.ChoiceSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.MultiChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.keybind.KeybindSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.multienum.MultiEnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.multitext.MultiTextSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.point.PointSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.range.RangeSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.binder.BinderSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.itemlist.ItemListSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.text.TextSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.button.ButtonSetting;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.animations.impl.SmoothStepAnimation;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import org.joml.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Group extends Setting.Basic<List<Setting<?>>, Group> {
    public Group(String name) {
        super(name, new ArrayList<>());
    }
    protected Group(String name, boolean defaultToggle) {
        super(name, new ArrayList<>());
        toggleable(defaultToggle);
    }
    public Group(String name, List<Setting<?>> defaultValue) {
        super(name, new ArrayList<>(defaultValue));
    }

    @Getter
    private final List<SettingRenderer<?>> settingRenderers = new ArrayList<>();
    public CheckBox enabled;
    CheckBoxRenderer enabledRenderer;

    @Getter
    private boolean expanded = true;
    public SmoothStepAnimation expandAnim = new SmoothStepAnimation(150, 1);

    public Group expanded(boolean expanded) {
        this.expanded = expanded;
        expandAnim.setDirection(expanded ? Direction.BACKWARDS : Direction.FORWARDS);
        return this;
    }

    public boolean isExpandable() {
        return !isEmpty();
    }

    public Group toggleable(boolean enabled) {
        if (this.enabled == null) {
            this.enabled = checkbox("Enabled", enabled);
            enabledRenderer = new CheckBoxRenderer(this.enabled);
        }
        return this;
    }

    public CheckBox checkbox(String name, boolean defaultValue) {
        return add(new CheckBox(name, defaultValue));
    }
    public TextSetting text(String name, String text) {
        return add(new TextSetting(name, text));
    }

    public <S extends Setting<?>> S add(S setting) {
        if (value == null) value = new ArrayList<>();
        setting.base = this;
        value.add(setting);
        settingRenderers.add(setting.wrap());
        return setting;
    }

    @Override
    public void resetSetting() {
        for (var setting : value) {
            setting.resetSetting();
        }
    }

    @Override
    public SettingRenderer<?> wrap() {
        return new GroupRenderer(this);
    }

    public void addAll(Setting<?>... settings) {
        if (value == null) value = new ArrayList<>();
        this.value.addAll(List.of(settings));
        for (Setting<?> setting : settings) {
            this.settingRenderers.add(setting.wrap());
        }
    }

    static final String EXPANDED = "_expanded";

    @Override
    public void save(JsonObject json) {
        String name = this.name.concat(":group");

        JsonObject groupJson = new JsonObject();
        json.add(name, groupJson);
        groupJson.addProperty(EXPANDED, expanded);
        for (Setting<?> setting : value) {
            setting.save(groupJson);
        }
    }

    @Override
    public void load(JsonObject json) {
        String name = this.name.concat(":group");

        if (json.has(name) && json.get(name).isJsonObject()) {
            JsonObject groupJson = json.getAsJsonObject(name);
            if (groupJson.has(EXPANDED) && groupJson.get(EXPANDED).isJsonPrimitive()) expanded(groupJson.get(EXPANDED).getAsBoolean());

            for (Setting<?> setting : value) {
                setting.load(groupJson);
            }
        }
    }

    public final List<Setting<?>> getSettings() {
        return value;
    }

    public boolean isEnabled() {
        return enabled == null || enabled.get();
    }

    public boolean isEmpty() {
        return value.isEmpty();
    }

    public Group group(String name) {
        return add(new Group(name));
    }

    @SafeVarargs
    public final <C extends Choice> MultiChoice<C> multiChoice(C... choices) {
        addAll(choices);
        for (C choice : choices) {
            choice.base = this;
            choice.toggleable(false);
        }
        return new MultiChoice<>(choices);
    }

    public SliderSetting sliderSetting(String name, float defaultValue, float min, float max) {
        return add(new SliderSetting(name, defaultValue, min, max));
    }

    public <E extends Enum<E>> EnumSetting<E> enumSetting(String name, E defaultValue) {
        return add(new EnumSetting<>(name, defaultValue));
    }

    @SafeVarargs
    public final <C extends Choice> ChoiceSetting<C> choiceSetting(String name, int defaultValue, C... choices) {
        return add(new ChoiceSetting<>(name, defaultValue, choices));
    }

    @SafeVarargs
    public final <E extends Enum<E>> MultiEnumSetting<E> multiEnumSetting(String name, E... defaults) {
        return add(new MultiEnumSetting<>(name, defaults));
    }

    public <E extends Enum<E>> MultiEnumSetting<E> multiEnumSetting(String name, Class<E> clazz) {
        return add(new MultiEnumSetting<>(name, clazz));
    }

    public MultiTextSetting multiTextSetting(String name, String... defaults) {
        return add(new MultiTextSetting(name, defaults));
    }

    public KeybindSetting keybindSetting(String name, int key) {
        return add(new KeybindSetting(name, key));
    }

    public ColorSetting colorSetting(String name, Color color) {
        return add(new ColorSetting(name, color));
    }

    public BinderSetting binderSetting(String name, Map<Item, Integer> defaultValue) {
        return add(new BinderSetting(name, defaultValue.entrySet().stream().map(entry -> Map.entry(Registries.ITEM.getId(entry.getKey()), entry.getValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
    }

    public ItemListSetting itemListSetting(String name, Item... defaults) {
        return add(new ItemListSetting(name, defaults));
    }
    public BlockListSetting blockListSetting(String name, Block... defaults) {
        return add(new BlockListSetting(name, defaults));
    }

    public PointSetting pointSetting(String name, Vector2f defaultValue) {
        return add(new PointSetting(name, defaultValue));
    }

    public RangeSetting rangeSetting(String name, float defaultMin, float defaultMax, float min, float max, float increment) {
        return add(new RangeSetting(name, defaultMin, defaultMax, min, max, increment));
    }

    public ButtonSetting button(String name, Runnable action) {
        return add(new ButtonSetting(name, action));
    }
}
