package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.particle.ParticlesMode;



/**
 * Разгоняет Minecraft по максимуму: меньше частиц, облака и тени выключены,
 * Vsync выключен, лимит FPS поднят, рендер-дистанция снижена по желанию.
 */
public class Optimization extends Module {
    public static final Optimization INSTANCE = new Optimization();

    public final CheckBox particles = checkbox("Мало частиц", true);
    public final CheckBox clouds = checkbox("Выключить облака", true);
    public final CheckBox entityShadows = checkbox("Выключить тени существ", true);
    public final CheckBox biomeBlend = checkbox("Biome blend 0", true);
    public final CheckBox vsync = checkbox("Выключить Vsync", true);
    public final CheckBox fpsBoost = checkbox("Лимит FPS 260", true);
    public final CheckBox viewDistance = checkbox("Дистанция прорисовки 5", false);
    public final CheckBox fastGraphics = checkbox("Fast graphics", false);
    public final CheckBox ao = checkbox("Выключить AO", true);
    public final CheckBox mega = checkbox("Мега оптимизация", true);



    private Integer prevMaxFps = null;
    private ParticlesMode prevParticles = null;
    private CloudRenderMode prevCloudMode = null;
    private Integer prevCloudDistance = null;
    private Boolean prevShadows = null;
    private Integer prevBiome = null;
    private Boolean prevVsync = null;
    private Integer prevViewDistance = null;
    private Boolean prevAo = null;
    private Integer prevSimulationDistance = null;
    private Double prevEntityScaling = null;
    private Integer prevMipmap = null;
    private Integer prevWeatherRadius = null;
    private GraphicsMode prevGraphics = null;
    private Boolean prevBobView = null;
    private Double prevDistortion = null;




    private int tickCounter = 0;

    private Optimization() {
        super("Optimization", Category.RENDER, "Максимальная оптимизация");
        setEnabled(true, false);
    }

    @Override
    protected void onEnable() {
        try {
            prevMaxFps = mc.options.getMaxFps().getValue();
            prevParticles = mc.options.getParticles().getValue();
            prevCloudMode = mc.options.getCloudRenderMode().getValue();
            prevCloudDistance = mc.options.getCloudRenderDistance().getValue();
            prevShadows = mc.options.getEntityShadows().getValue();
            prevBiome = mc.options.getBiomeBlendRadius().getValue();
            prevVsync = mc.options.getEnableVsync().getValue();
            prevViewDistance = mc.options.getViewDistance().getValue();
            prevAo = mc.options.getAo().getValue();
            prevSimulationDistance = mc.options.getSimulationDistance().getValue();
            prevEntityScaling = mc.options.getEntityDistanceScaling().getValue();
            prevMipmap = mc.options.getMipmapLevels().getValue();
            prevWeatherRadius = mc.options.getWeatherRadius().getValue();
            prevGraphics = mc.options.getPreset().getValue();
            prevBobView = mc.options.getBobView().getValue();
            prevDistortion = mc.options.getDistortionEffectScale().getValue();

        } catch (Exception ignored) {
        }


        apply(true);
    }

    @Override
    protected void onDisable() {
        apply(false);
    }

    EventBus<Event> events = event -> {
        if (event instanceof EventGameTick) {
            if (++tickCounter % 40 == 0) apply(true);
        }
    };

    private void apply(boolean enabled) {
        try {
            if (enabled) {
                if (particles.get()) mc.options.getParticles().setValue(ParticlesMode.MINIMAL);

                if (clouds.get()) {
                    mc.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
                    mc.options.getCloudRenderDistance().setValue(1);
                }

                if (entityShadows.get()) mc.options.getEntityShadows().setValue(false);
                if (biomeBlend.get()) mc.options.getBiomeBlendRadius().setValue(0);
                if (vsync.get()) mc.options.getEnableVsync().setValue(false);
                if (fpsBoost.get()) mc.options.getMaxFps().setValue(260);
                if (viewDistance.get()) mc.options.getViewDistance().setValue(5);
                if (fastGraphics.get()) mc.options.getPreset().setValue(GraphicsMode.FAST);
                if (ao.get()) mc.options.getAo().setValue(false);

                if (mega.get()) {
                    mc.options.getParticles().setValue(ParticlesMode.MINIMAL);
                    mc.options.getPreset().setValue(GraphicsMode.FAST);
                    mc.options.getSimulationDistance().setValue(5);
                    mc.options.getEntityDistanceScaling().setValue(0.5D);
                    mc.options.getMipmapLevels().setValue(0);
                    mc.options.getWeatherRadius().setValue(0);
                    mc.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
                    mc.options.getEntityShadows().setValue(false);
                    mc.options.getBiomeBlendRadius().setValue(0);
                    mc.options.getAo().setValue(false);
                    mc.options.getEnableVsync().setValue(false);
                    mc.options.getMaxFps().setValue(260);
                    mc.options.getBobView().setValue(false);
                    mc.options.getDistortionEffectScale().setValue(0D);
                }

            } else {
                if (prevMaxFps != null) mc.options.getMaxFps().setValue(prevMaxFps);
                if (prevParticles != null) mc.options.getParticles().setValue(prevParticles);
                if (prevCloudMode != null) mc.options.getCloudRenderMode().setValue(prevCloudMode);
                if (prevCloudDistance != null) mc.options.getCloudRenderDistance().setValue(prevCloudDistance);
                if (prevShadows != null) mc.options.getEntityShadows().setValue(prevShadows);
                if (prevBiome != null) mc.options.getBiomeBlendRadius().setValue(prevBiome);
                if (prevVsync != null) mc.options.getEnableVsync().setValue(prevVsync);
                if (prevViewDistance != null) mc.options.getViewDistance().setValue(prevViewDistance);
                if (prevAo != null) mc.options.getAo().setValue(prevAo);
                if (prevSimulationDistance != null) mc.options.getSimulationDistance().setValue(prevSimulationDistance);
                if (prevEntityScaling != null) mc.options.getEntityDistanceScaling().setValue(prevEntityScaling);
                if (prevMipmap != null) mc.options.getMipmapLevels().setValue(prevMipmap);
                if (prevWeatherRadius != null) mc.options.getWeatherRadius().setValue(prevWeatherRadius);
                if (prevGraphics != null) mc.options.getPreset().setValue(prevGraphics);
                if (prevBobView != null) mc.options.getBobView().setValue(prevBobView);
                if (prevDistortion != null) mc.options.getDistortionEffectScale().setValue(prevDistortion);

            }

        } catch (Exception ignored) {
        }
    }
}
