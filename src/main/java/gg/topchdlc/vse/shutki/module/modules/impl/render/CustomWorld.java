package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

/**
 * Create by daun kvass
 */
public class CustomWorld extends Module {
    public static final CustomWorld INSTANCE = new CustomWorld();
    private CustomWorld() {
        super("CustomWorld", Category.RENDER, "меняет мир прикол");
    }

    final Group sky = group("Skyt");
    public final CheckBox skyx = sky.add(new CheckBox("Sky Custom",true));
    public final ColorSetting skyColor = sky.add(new ColorSetting("Color",new Color(30, 0, 60)));
    final Group Light = group("Light");
    public final ColorSetting worldColor = Light.add(new ColorSetting("World Tint", new Color(200, 150, 255)));
    public final CheckBox customColor = Light.add(new CheckBox("Custom Light Color",true));
    final Group timeGroup = group("Time setting");
    final EnumSetting<TimeSpec> time = timeGroup.enumSetting("Time", TimeSpec.DAY);
    final SliderSetting customTime = timeGroup.sliderSetting("Custom", 1000, 0, 24000).visible(() -> time.is(TimeSpec.CUSTOM));

    final Group weatherGroup = group("Weather setting");
    final EnumSetting<WeatherSpec> weather = weatherGroup.enumSetting("Weather", WeatherSpec.SUNNY);
    final SliderSetting customRainGradient = weatherGroup.sliderSetting("Custom rain gradient", 0f, 0, 1).increment(.1f).visible(() -> weather.is(WeatherSpec.CUSTOM));
    final SliderSetting customThunderGradient = weatherGroup.sliderSetting("Custom thunder gradient", 0f, 0, 1).increment(.1f).visible(() -> weather.is(WeatherSpec.CUSTOM));

    final Group fogGroup = group("Fog setting");
    public CheckBox useFog = fogGroup.add(new CheckBox("Fog", true));
    public ColorSetting fogColor = fogGroup.add(new ColorSetting("Fog color", Color.PINK));
    public SliderSetting fogEnd = fogGroup.add(new SliderSetting("Fog end (blocks)", 64F, 4F, 512F).increment(4F));
    public SliderSetting fogStart = fogGroup.add(new SliderSetting("Fog start (blocks)", 32F, 0F, 512F).increment(4F));

    float animatedTime = 0;

    public long getTime(long original) {
        if (!isEnabled() || time.is(TimeSpec.NONE)) return original;
        if (time.is(TimeSpec.CUSTOM)) return (long) customTime.get();

        return (long) animatedTime;
    }
    public int getCustomSkyColor() {
        return skyColor.get().getRGB();
    }

    public float getRainGradient() {
        if (!isEnabled() || weather.is(WeatherSpec.NONE)) return -1;
        if (weather.is(WeatherSpec.CUSTOM)) return customRainGradient.get();
        return weather.get().getRainGradient();
    }

    public float getThunderGradient() {
        if (!isEnabled() || weather.is(WeatherSpec.NONE)) return -1;
        if (weather.is(WeatherSpec.CUSTOM)) return customThunderGradient.get();
        return weather.get().getThunderGradient();
    }

    EventBus<Event> events = event -> {
        if (event instanceof Event3D e) {
            animatedTime = MathHelper.lerp(mc.getRenderTickCounter().getTickProgress(true), animatedTime, animatedTime + (time.get().getTime() - animatedTime) * 0.01f);

        }
    };

    @AllArgsConstructor
    @SuppressWarnings("unused")
    @Getter
    enum TimeSpec implements EnumChoice {
        DAY("День", 1000),
        MIDNIGHT("Ночь", 18000),
        NIGHT("Пасмурно", 12300),
        CUSTOM("Своё", -1),
        NONE("Не менять", -1);
        final String renderName;
        final int time;
    }


    @AllArgsConstructor
    @SuppressWarnings("unused")
    @Getter
    enum WeatherSpec implements EnumChoice {
        SUNNY("Солнечно", 0f, 0f),
        RAINY("Дождь", 1f, 0f),
        THUNDER("Гроза", 1f, 1f),
        SNOWY("Снег", 0.9f, 0f),
        CUSTOM("Своё", -1, -1),
        NONE("Не менять", -1, -1);
        final String renderName;
        final float rainGradient;
        final float thunderGradient;
    }
}
