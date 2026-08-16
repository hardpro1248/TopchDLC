package gg.topchdlc.vse.shutki.module.modules.impl.render;

import java.awt.Color;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.render.HandShaderRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * Create by daun kvass
 */
public final class HandShaderModule extends Module {
    public static final HandShaderModule INSTANCE = new HandShaderModule();

    private final Group main = group("Main");
    public final CheckBox fireEffect = main.checkbox("Fire Effect", true);

    private final SliderSetting intensity = main.sliderSetting("Intensity", 0.1f, 0.8f, 1.5f).increment(0.1f).visible(fireEffect::get);
    private final SliderSetting speed = main.sliderSetting("Speed", 0.2f, 1.0f, 3f).increment(0.1f).visible(fireEffect::get);
    private final SliderSetting length = main.sliderSetting("Length", 0.1f, 0.55f, 1f).increment(0.01f).visible(fireEffect::get);
    private final SliderSetting smoke = main.sliderSetting("Smoke", 0.0f, 0.55f, 0.8f).increment(0.01f).visible(fireEffect::get);
    private final CheckBox useItemColor = main.checkbox("Item Color", true);
    private final CheckBox useThemeColor = main.checkbox("Theme Color", true).visible(() -> !useItemColor.get());

    private final Group trail = group("Trail");
    private final SliderSetting trailFade = trail.sliderSetting("Trail Fade", 0.55f, 0.84f, 0.96f).increment(0.01f).visible(fireEffect::get);
    private final SliderSetting trailSoftness = trail.sliderSetting("Trail Softness", 0.45f, 1.35f, 2.5f).increment(0.01f).visible(fireEffect::get);
    private final SliderSetting trailBlur = trail.sliderSetting("Trail Blur", 0.2f, 1.55f, 3f).increment(0.01f).visible(fireEffect::get);

    private final Group hand = group("Hand");
    private final SliderSetting handFade = hand.sliderSetting("Hand Fade", 0.45f, 0.68f, 0.9f).increment(0.01f).visible(fireEffect::get);
    private final SliderSetting handSoftness = hand.sliderSetting("Hand Softness", 0.45f, 1.3f, 2.5f).increment(0.1f).visible(fireEffect::get);
    private final SliderSetting handBlur = hand.sliderSetting("Hand Blur", 0.2f, 1.4f, 3f).increment(0.1f).visible(fireEffect::get);

    private final Group outlineGroup = group("Outline");
    public final CheckBox outlineEnabled = outlineGroup.checkbox("Enable Outline", false);
    public final EnumSetting<OutlineMode> outlineMode = outlineGroup.enumSetting("Style", OutlineMode.GLOW).visible(outlineEnabled::get);
    public final SliderSetting outlineWidth = outlineGroup.sliderSetting("Aura Width", 3.0f, 1.0f, 8.0f).increment(0.1f).visible(outlineEnabled::get);
    public final SliderSetting outlineGlow = outlineGroup.sliderSetting("Glow Power", 1.8f, 0.2f, 4.0f).increment(0.1f).visible(() -> outlineEnabled.get() && outlineMode.get() != OutlineMode.SOLID);
    public final SliderSetting outlineAlpha = outlineGroup.sliderSetting("Alpha", 0.85f, 0.1f, 1.0f).increment(0.05f).visible(outlineEnabled::get);

    public final CheckBox useCustomOutlineColor = outlineGroup.checkbox("Custom Color", false).visible(outlineEnabled::get);
    public final ColorSetting outlineCustomColor = outlineGroup.colorSetting("Color", new Color(160, 60, 255, 255)).visible(() -> outlineEnabled.get() && useCustomOutlineColor.get());
    public final CheckBox outlineRainbow = outlineGroup.checkbox("Rainbow Color", false).visible(() -> outlineEnabled.get() && !useCustomOutlineColor.get());

    private final Group fillGroup = group("Item Fill");
    public final CheckBox fillItem = fillGroup.checkbox("Fill Item/Hand", false);
    public final EnumSetting<RenderMode> fillRenderMode = fillGroup.enumSetting("Render Mode", RenderMode.REPLACE);
    public final EnumSetting<FillMode> fillMode = fillGroup.enumSetting("Fill Mode", FillMode.SHADER);

    public final EnumSetting<ShaderMode> fillShaderMode = fillGroup.enumSetting("Shader Mode", ShaderMode.BLOOM).visible(() -> fillMode.is(FillMode.SHADER));
    public final EnumSetting<BlendMode> fillBlendMode = fillGroup.enumSetting("Blend Mode", BlendMode.LIGHTNING).visible(() -> fillMode.is(FillMode.SHADER));

    public final SliderSetting fillSpeed = fillGroup.sliderSetting("Shader Speed", 1.0f, 0.1f, 5.0f).increment(0.1f).visible(() -> fillMode.is(FillMode.SHADER));
    public final SliderSetting fillScale = fillGroup.sliderSetting("Shader Scale", 5.0f, 1.0f, 20.0f).increment(0.1f).visible(() -> fillMode.is(FillMode.SHADER));
    public final SliderSetting fillIntensity = fillGroup.sliderSetting("Shader Intensity", 0.01f, 0.001f, 0.05f).increment(0.001f).visible(() -> fillMode.is(FillMode.SHADER));
    public final SliderSetting fillAlpha = fillGroup.sliderSetting("Fill Alpha", 1.0f, 0.1f, 1.0f).increment(0.05f);

    public final CheckBox useCustomFillColor = fillGroup.checkbox("Custom Color", false);
    public final ColorSetting fillCustomColor = fillGroup.colorSetting("Color", new Color(0, 200, 255, 255)).visible(useCustomFillColor::get);

    public static boolean isRenderingShader = false;

    private HandShaderModule() {
        super("HandShader", Category.RENDER, "Добавляет эффекты и шейдеры на руку и предметы");
    }

    @Override
    protected void onEnable() {
        updateRendererSettings();
    }

    @Override
    protected void onDisable() {
        HandShaderRenderer.getInstance().setEnabled(false);
        super.onDisable();
    }

    EventBus<Event> eventEventBus = event -> {
        if (event instanceof EventGameTick) updateRendererSettings();
    };

    private void updateRendererSettings() {
        HandShaderRenderer renderer = HandShaderRenderer.getInstance();
        boolean shouldEnableRenderer = isEnabled() && (fireEffect.get() || outlineEnabled.get());
        renderer.setEnabled(shouldEnableRenderer);

        if (shouldEnableRenderer) {
            renderer.setFireEffectEnabled(fireEffect.get());
            renderer.setIntensity(intensity.get());
            renderer.setRadius(0.35f);
            renderer.setSpeed(speed.get());
            renderer.setFlameHeight(length.get());
            renderer.setTrailFade(trailFade.get());
            renderer.setTrailSoftness(trailSoftness.get());
            renderer.setTrailBlurRadius(trailBlur.get());
            renderer.setHandFade(handFade.get());
            renderer.setHandSoftness(handSoftness.get());
            renderer.setHandBlurRadius(handBlur.get());
            renderer.setSmoke(smoke.get());

            Color color = getHandEffectColor();
            renderer.setColor(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, 1.0f);

            renderer.setOutlineEnabled(outlineEnabled.get());
            renderer.setOutlineWidth(outlineWidth.get());
            renderer.setOutlineGlow(outlineGlow.get());

            int modeInt = 1;
            if (outlineMode.get() == OutlineMode.SOLID) modeInt = 0;
            else if (outlineMode.get() == OutlineMode.SMOOTH) modeInt = 2;
            renderer.setOutlineMode(modeInt);

            Color outColor = getOutlineColor();
            renderer.setOutlineColor(
                    outColor.getRed() / 255.0f,
                    outColor.getGreen() / 255.0f,
                    outColor.getBlue() / 255.0f,
                    (outColor.getAlpha() / 255.0f) * outlineAlpha.get()
            );
        }
    }

    public Color getOutlineColor() {
        if (useCustomOutlineColor.get()) {
            return outlineCustomColor.get();
        } else if (outlineRainbow.get()) {
            float hue = (System.currentTimeMillis() % 3000L) / 3000.0f;
            return Color.getHSBColor(hue, 0.8f, 1.0f);
        } else {
            return getHandEffectColor();
        }
    }

    public Color getHandEffectColor() {
        if (useItemColor.get()) {
            return getHeldItemColor();
        } else if (useThemeColor.get()) {
            return ClientSettings.INSTANCE.getColor(0);
        } else {
            return new Color(0x6633FF);
        }
    }

    public void updateUniforms() {
        Color color = useCustomFillColor.get()
                ? fillCustomColor.get()
                : getHandEffectColor();

        ClientPipelines.updateShaderUniforms(
                fillAlpha.get(),
                fillSpeed.get(),
                fillScale.get(),
                fillIntensity.get(),
                color
        );
    }

    public static int getCustomTint() {
        Color baseColor = INSTANCE.useCustomFillColor.get()
                ? INSTANCE.fillCustomColor.get()
                : INSTANCE.getHandEffectColor();

        int alphaVal = (int) (INSTANCE.fillAlpha.get() * 255.0f);
        alphaVal = Math.max(1, Math.min(255, alphaVal));
        return (alphaVal << 24) | (baseColor.getRed() << 16) | (baseColor.getGreen() << 8) | baseColor.getBlue();
    }

    public static RenderLayer getCustomShaderLayer(Identifier texture) {
        ShaderMode shaderMode = INSTANCE.fillShaderMode.get();
        BlendMode blendMode = INSTANCE.fillBlendMode.get();

        if (shaderMode == ShaderMode.CAUSTIC) {
            return blendMode == BlendMode.LIGHTNING
                    ? ClientPipelines.getDynamicChamsLayer("caustic_depth_add", ClientPipelines.CHAMS_CAUSTIC_DEPTH_ADD_PL, texture)
                    : ClientPipelines.getDynamicChamsLayer("caustic_depth_trans", ClientPipelines.CHAMS_CAUSTIC_DEPTH_TRANS_PL, texture);
        } else if (shaderMode == ShaderMode.NEBULA) {
            return blendMode == BlendMode.LIGHTNING
                    ? ClientPipelines.getDynamicChamsLayer("nebula_depth_add", ClientPipelines.CHAMS_NEBULA_DEPTH_ADD_PL, texture)
                    : ClientPipelines.getDynamicChamsLayer("nebula_depth_trans", ClientPipelines.CHAMS_NEBULA_DEPTH_TRANS_PL, texture);
        } else if (shaderMode == ShaderMode.GLASS) {
            return blendMode == BlendMode.LIGHTNING
                    ? ClientPipelines.getDynamicChamsLayer("glass_depth_add", ClientPipelines.CHAMS_GLASS_DEPTH_ADD_PL, texture)
                    : ClientPipelines.getDynamicChamsLayer("glass_depth_trans", ClientPipelines.CHAMS_GLASS_DEPTH_TRANS_PL, texture);
        } else if (shaderMode == ShaderMode.PLASMA) {
            return blendMode == BlendMode.LIGHTNING
                    ? ClientPipelines.getDynamicChamsLayer("plasma_depth_add", ClientPipelines.CHAMS_PLASMA_DEPTH_ADD_PL, texture)
                    : ClientPipelines.getDynamicChamsLayer("plasma_depth_trans", ClientPipelines.CHAMS_PLASMA_DEPTH_TRANS_PL, texture);
        } else if (shaderMode == ShaderMode.BLOOM) {
            return blendMode == BlendMode.LIGHTNING
                    ? ClientPipelines.getDynamicChamsLayer("bloom_depth_add", ClientPipelines.CHAMS_BLOOM_DEPTH_ADD_PL, texture)
                    : ClientPipelines.getDynamicChamsLayer("bloom_depth_trans", ClientPipelines.CHAMS_BLOOM_DEPTH_TRANS_PL, texture);
        } else {
            return blendMode == BlendMode.LIGHTNING
                    ? ClientPipelines.getDynamicChamsLayer("water_depth_add", ClientPipelines.CHAMS_WATER_DEPTH_ADD_PL, texture)
                    : ClientPipelines.getDynamicChamsLayer("water_depth_trans", ClientPipelines.Models_Water_Depth_Trans_pl, texture);
        }
    }

    public Color getHeldItemColor() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return Color.WHITE;

        ItemStack stack = mc.player.getMainHandStack();
        if (stack.isEmpty()) stack = mc.player.getOffHandStack();
        if (stack.isEmpty()) return Color.WHITE;

        String path = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).getPath();
        if (path.contains("diamond")) return new Color(0x55DDE0);
        if (path.contains("netherite")) return new Color(0x5B4A67);
        if (path.contains("gold")) return new Color(0xFFD45A);
        if (path.contains("iron")) return new Color(0xD8DEE8);
        if (path.contains("emerald")) return new Color(0x35D06F);
        if (path.contains("redstone")) return new Color(0xE23B3B);
        if (path.contains("lapis")) return new Color(0x3156D4);
        return new Color(0xE6E6E6);
    }

    public enum OutlineMode implements EnumChoice {
        GLOW("Glow Aura"), SMOOTH("Smooth"), SOLID("Solid");
        private final String renderName;
        OutlineMode(String name) { this.renderName = name; }
        @Override public String getRenderName() { return renderName; }
    }

    public enum RenderMode implements EnumChoice {
        REPLACE("Replace"), OVERLAY("Overlay");
        private final String renderName;
        RenderMode(String name) { this.renderName = name; }
        @Override public String getRenderName() { return renderName; }
    }

    public enum BlendMode implements EnumChoice {
        TRANSLUCENT("Translucent"), LIGHTNING("Lightning");
        private final String renderName;
        BlendMode(String name) { this.renderName = name; }
        @Override public String getRenderName() { return renderName; }
    }

    public enum FillMode implements EnumChoice {
        NORMAL("Normal"), SHADER("Shader");
        private final String renderName;
        FillMode(String name) { this.renderName = name; }
        @Override public String getRenderName() { return renderName; }
    }

    public enum ShaderMode implements EnumChoice {
         WATER("Water"), CAUSTIC("Caustic"), NEBULA("Nebula"), GLASS("Glass"), PLASMA("Plasma"), BLOOM("Bloom");
        private final String renderName;
        ShaderMode(String name) { this.renderName = name; }
        @Override public String getRenderName() { return renderName; }
    }
}