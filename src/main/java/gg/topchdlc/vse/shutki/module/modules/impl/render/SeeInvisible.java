package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;

/**
 * Create by daun kvass
 */
public class SeeInvisible extends Module {
    public static final SeeInvisible INSTANCE = new SeeInvisible();

    public final SliderSetting alpha = sliderSetting("Прозрачность", 100f, 1f, 255f)
            .increment(1f);

    public SeeInvisible() {
        super("SeeInvisible", Category.RENDER, "инвизки от да нихк");
    }
}