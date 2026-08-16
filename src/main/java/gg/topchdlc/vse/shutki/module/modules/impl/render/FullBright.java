package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;

/**
 * Create by daun kvass
 */
public class FullBright extends Module {
    public static final FullBright INSTANCE = new FullBright();

    private FullBright() {
        super("FullBright", Category.Misc, "ssss");
        setEnabled(true, false);
    }

    public CheckBox dynamic = checkbox("Dynamic", true);
    public SliderSetting maxGamma = sliderSetting("Max Gamma", 1.0f, 0.1f, 10f).increment(0.1f);

    public float currentGamma = 1.0f;
    private static final float SPEED = 0.3f;

    @Override
    protected void onEnable() {
        currentGamma = dynamic.get() ? 1.0f : (float) maxGamma.get();
        if (mc.worldRenderer != null) mc.worldRenderer.reload();
    }

    @Override
    protected void onDisable() {
        currentGamma = 1.0f;
        if (mc.worldRenderer != null) mc.worldRenderer.reload();
    }

    EventBus<EventGameTick> tick = event -> {
        if (!isEnabled()) return;

        float target;
        float maxValue = (float) maxGamma.get();

        if (dynamic.get() && mc.player != null && mc.world != null) {
            BlockPos pos = mc.player.getBlockPos();
            int blockLight = mc.world.getLightLevel(LightType.BLOCK, pos);
            int skyLight   = mc.world.getLightLevel(LightType.SKY, pos);
            int light = Math.max(blockLight, skyLight);

            float base = (float) (double) mc.options.getGamma().getValue();
            float t = light / 15.0f;

            target = maxValue + t * (base - maxValue);
        } else {
            target = maxValue;
        }

        currentGamma += (target - currentGamma) * SPEED;
    };
}