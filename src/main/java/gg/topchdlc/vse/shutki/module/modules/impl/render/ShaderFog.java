package gg.topchdlc.vse.shutki.module.modules.impl.render;

import com.mojang.blaze3d.textures.GpuTextureView;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.render.impl.ShaderFogPipeline;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.awt.Color;

public class ShaderFog extends Module {
    public static final ShaderFog INSTANCE = new ShaderFog();

    final Group mainGroup = group("Shader Fog Settings");
    public final EnumSetting<ShaderMode> mode = mainGroup.enumSetting("Mode", ShaderMode.CAUSTIC);
    public final SliderSetting speed = mainGroup.sliderSetting("Speed", 1.0f, 0.1f, 5.0f).increment(0.1f);
    public final SliderSetting scale = mainGroup.sliderSetting("Scale", 5.0f, 1.0f, 20.0f).increment(0.5f);
    public final SliderSetting intensity = mainGroup.sliderSetting("Intensity", 0.01f, 0.001f, 0.05f).increment(0.001f);
    public final SliderSetting alpha = mainGroup.sliderSetting("Alpha", 1.0f, 0.3f, 1.0f).increment(0.05f);
    public final ColorSetting skyColor = mainGroup.add(new ColorSetting("Color", new Color(0, 150, 255)));

    private final ShaderFogPipeline pipeline = new ShaderFogPipeline();
    private long startMillis = -1;

    private ShaderFog() {
        super("ShaderFog", Category.RENDER, "Заменяет обычное небо шейдерным туманом");
    }

    @Override
    public void onDisable() {
        startMillis = -1;
    }

    public void renderShader() {
        if (!isEnabled() || mc.player == null || mc.world == null) return;
        if (startMillis < 0) startMillis = System.currentTimeMillis();

        float time = (System.currentTimeMillis() - startMillis) / 1000.0f;
        float fw = mc.getWindow().getFramebufferWidth();
        float fh = mc.getWindow().getFramebufferHeight();

        Color color = skyColor.get();
        net.minecraft.client.render.Camera cam = mc.gameRenderer.getCamera();
        float yawRad = (float) Math.toRadians(-cam.getYaw());
        float pitchRad = (float) Math.toRadians(cam.getPitch());
        float fov = (float) mc.options.getFov().getValue().intValue();

        ShaderMode currentMode = mode.get();

        GpuTextureView screenTexture = mc.getFramebuffer().getColorAttachmentView();
        if (screenTexture != null) {
            pipeline.render(screenTexture, currentMode, (int) fw, (int) fh, yawRad, pitchRad, color,
                    time, alpha.get(), speed.get(), scale.get(), intensity.get(), fov);
        }
    }

    EventBus<Event> events = event -> {};

    @AllArgsConstructor
    @Getter
    public enum ShaderMode implements EnumChoice {
        CAUSTIC("Caustic"),
        DRAIN("Drain"),
        NEBULA("Nebula"),
        PLASMA("Plasma"),
        BLOOM("Bloom");

        private final String renderName;
    }
}