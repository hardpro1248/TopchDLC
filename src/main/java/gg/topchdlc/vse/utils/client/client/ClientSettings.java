package gg.topchdlc.vse.utils.client.client;

import com.google.gson.JsonObject;
import gg.topchdlc.Client;
import gg.topchdlc.api.themes.ThemePresets;
import gg.topchdlc.vse.shutki.module.settings.impl.button.ButtonSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.keybind.KeybindSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.math.ColorUtility;
import gg.topchdlc.vse.utils.network.ProxyUtility;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

@FieldDefaults(makeFinal = true)
@SuppressWarnings({"unused", "FieldMayBeFinal"})
public final class ClientSettings extends Group {
    public static final ClientSettings INSTANCE = new ClientSettings();
    private ClientSettings() {
        super("ClientSettings");
    }

    @Override
    public String getLangKey() {
        return "topchdlc.ClientSettings";
    }

    public final KeybindSetting clickguiBind = keybindSetting("ClickGUI", GLFW.GLFW_KEY_RIGHT_SHIFT);
    public final KeybindSetting radialItemKey = keybindSetting("Меню предметов", GLFW.GLFW_KEY_V);
    public final KeybindSetting radialPotionKey = keybindSetting("Меню зелий", GLFW.GLFW_KEY_G);
  //  public final KeybindSetting partyGpsBind = keybindSetting("Party GPS", GLFW.GLFW_KEY_UNKNOWN);
    public final CustomizeSetting customizeSetting = add(new CustomizeSetting());
    public final TargetSettings targetSettings = add(new TargetSettings());
    private final Group theme = group("Theme");
    public Group getTheme() { return theme; }
    public final ProxyUtility proxy = add(new ProxyUtility());
    public final EnumSetting<ColorType> colorType = theme.enumSetting("Color Type", ColorType.Gradient);
    public final SliderSetting colorSpeed = theme.sliderSetting("Color Speed", 9, 1, 30).increment(0.5f).visible(() -> colorType.isAny(ColorType.Gradient, ColorType.Rainbow));
    public final SliderSetting menuOpacity = theme.sliderSetting("Menu Opacity", 135, 30, 255).increment(1f);
    public final EnumSetting<ThemePresets> preset = theme.enumSetting("Preset", ThemePresets.RED).onChanged(this::applyPreset).visible(() -> colorType.isAny(ColorType.Gradient, ColorType.Single));
    private ColorSetting mainColor = theme.colorSetting("Main Color", new Color(24, 99, 237, 255)).onChanged(c -> ClientColors.RED_L = c).visible(() -> colorType.isAny(ColorType.Gradient, ColorType.Single));
    private ColorSetting secondaryColor = theme.colorSetting("Secondary Color", new Color(1, 53, 165, 255)).onChanged(c -> ClientColors.RED = c).visible(() -> colorType.is(ColorType.Gradient));


    private void applyPreset(ThemePresets preset) {
        if (preset == ThemePresets.CUSTOM) {
            ClientColors.RED_L = mainColor.get();
            ClientColors.RED = secondaryColor.get();
        } else {
            ClientColors.RED_L = preset.getMain();
            ClientColors.RED = preset.getSecondary();
        }
    }

    public enum ColorType { Single, Gradient, Rainbow }

    public Color getColor(int index) {
        return switch (colorType.get()) {
            case Single -> ClientColors.RED_L;
            case Gradient -> ColorUtility.transfusionEffect(colorSpeed.getInt(), index, ClientColors.RED_L, ClientColors.RED);
            case Rainbow -> ColorUtility.rainbowEffect(colorSpeed.getInt(), index);
        };
    }
    public Color getColorBright(int index) {
        return switch (colorType.get()) {
            case Single -> ClientColors.RED_L;
            case Gradient -> ColorUtility.transfusionEffect(colorSpeed.getInt(), index, ClientColors.RED_L, ClientColors.RED);
            case Rainbow -> ColorUtility.rainbowEffectBright(colorSpeed.getInt(), index);
        };
    }
    @Override
    public void load(JsonObject json) {

        super.load(json);

        applyPreset(preset.get());

        if (preset.is(ThemePresets.CUSTOM)) {
            ClientColors.RED_L = mainColor.get();
            ClientColors.RED = secondaryColor.get();
        }
    }

}
