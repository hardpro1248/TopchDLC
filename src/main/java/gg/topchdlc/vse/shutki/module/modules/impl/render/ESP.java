package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event2D;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.render.esp.ESPRenderer;
import gg.topchdlc.vse.shutki.module.modules.impl.render.esp.impl.*;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.multienum.MultiEnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.math.MathUtility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;

public class ESP extends Module {
    public static final ESP INSTANCE = new ESP();
    private ESP() {
        super("Esp Player", Category.RENDER, "Отображение информации на энтити");
    }

    public MultiEnumSetting<ESPEntities> entities = add(new MultiEnumSetting<>("Entities", ESPEntities.class));
    public MultiEnumSetting<ESPElements> elements = add(new MultiEnumSetting<>("Elements", ESPElements.class));
    public CheckBox corners = add(new CheckBox("Corners", false).visible(() -> elements.get(ESPElements.Box)));
    public SliderSetting cornersLenght = add(new SliderSetting("Corner length", 0.3F, 0.1F, 0.4F).increment(0.1F)).visible(() -> corners.getVisible().get() && corners.get());
    public SliderSetting thickness = add(new SliderSetting("Thickness", 1, 0.5F, 2).increment(0.1F)).visible(() -> elements.get(ESPElements.Box));
    public SliderSetting glowExpand = add(new SliderSetting("Glow Expand", 3, 0, 10).increment(0.5F)).visible(() -> elements.get(ESPElements.Glow));
    public SliderSetting glowSmooth = add(new SliderSetting("Glow Smooth", 15, 1, 40).increment(1)).visible(() -> elements.get(ESPElements.Glow));
    public SliderSetting glowAlpha = add(new SliderSetting("Glow Alpha", 0.6F, 0.05F, 1).increment(0.05F)).visible(() -> elements.get(ESPElements.Glow));
    public CheckBox renderArmor = add(new CheckBox("Render Armor", false).visible(() -> elements.get(ESPElements.ItemPlayer)));

    private ArrayList<Entity> toRender = new ArrayList<>();

    EventBus<Event> events = event -> {
        if (event instanceof EventGameTick) {
            toRender.clear();
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof ClientPlayerEntity && mc.options.getPerspective().isFirstPerson()) continue;
                if (entity.isRemoved()) continue;
                if (entities.get().stream().anyMatch(it -> it.supplier.shouldRender(entity)))
                    toRender.add(entity);
            }
        }
        if (event instanceof Event2D e) {
            render2D(e);
        }
    };

    @Override
    public void onEnable() {
        super.onEnable();
        Client.EVENTS.register(events);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Client.EVENTS.unregister(events);
    }

    private void render2D(Event2D e) {
            Iterator<Entity> iterator = toRender.iterator();
            while (iterator.hasNext()) {
                Entity entity = iterator.next();

                Vec3d interp = entity.getLerpedPos(mc.getRenderTickCounter().getTickProgress(true));
                Box box = entity.getBoundingBox().offset(interp.subtract(entity.getEntityPos()));


                Vec3d[] corners = new Vec3d[]{
                        new Vec3d(box.minX, box.minY, box.minZ),
                        new Vec3d(box.minX, box.minY, box.maxZ),
                        new Vec3d(box.maxX, box.minY, box.minZ),
                        new Vec3d(box.maxX, box.minY, box.maxZ),
                        new Vec3d(box.minX, box.maxY, box.minZ),
                        new Vec3d(box.minX, box.maxY, box.maxZ),
                        new Vec3d(box.maxX, box.maxY, box.minZ),
                        new Vec3d(box.maxX, box.maxY, box.maxZ)
                };
                float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
                float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
                boolean anyVisible = false;
                for (Vec3d corner : corners) {
                    Vec3d projected = MathUtility.worldSpaceToScreenSpace(corner);
                    if (projected.z <= 0 || projected.z >= 1) continue;
                    anyVisible = true;
                    minX = (float) Math.min(minX, projected.x);
                    minY = (float) Math.min(minY, projected.y);
                    maxX = (float) Math.max(maxX, projected.x);
                    maxY = (float) Math.max(maxY, projected.y);
                }
                if (!anyVisible) continue;
                if (maxX < 0 || maxY < 0 || minX > mc.getWindow().getScaledWidth() || minY > mc.getWindow().getScaledHeight()) continue;
                for (ESPElements element : elements.get()) {
                    element.renderer.render(minX, minY, maxX, maxY, thickness.get(), entity);
                }
            }
    }

    public boolean dontRenderNametag(Entity entity) {
        if (!elements.get(ESPElements.Nametag)) return false;

        return entities.get().stream().anyMatch(it -> it.supplier.shouldRender(entity));
    }

    @AllArgsConstructor @Getter
    public enum ESPElements implements EnumChoice {
        Box(true, new BoxRenderer(ESPRenderer.Align.TOP)),
        Glow(true, new GlowRenderer(ESPRenderer.Align.TOP)),
        Nametag(true, new NametagRenderer(ESPRenderer.Align.TOP)),
        ItemPlayer(true, new ItemPlayerRenderer(ESPRenderer.Align.BOTTOM)),
        Effects(true, new EffectRenderer(ESPRenderer.Align.BOTTOM)),;
        final boolean defaultEnabled;
        final ESPRenderer renderer;
    }
    @AllArgsConstructor @Getter
    public enum ESPEntities implements EnumChoice {
        Players(true, e -> e instanceof PlayerEntity),
        Other(false, e -> {
            return (e instanceof LivingEntity) && !Players.getSupplier().shouldRender(e);
        });
        final boolean defaultEnabled;
        public final Callback supplier;

        public interface Callback {
            boolean shouldRender(Entity entity);
        }
    }
}
