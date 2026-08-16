package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event2D;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.render.esp.ESPRenderer;
import gg.topchdlc.vse.shutki.module.modules.impl.render.esp.impl.*;
import gg.topchdlc.vse.utils.math.MathUtility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

public class ItemEsp extends Module {
    public static final ItemEsp INSTANCE = new ItemEsp();

    private ItemEsp() {
        super("ItemNameTag", Category.RENDER, "Отображение имен предметов");
    }

    private final ArrayList<Entity> toRender = new ArrayList<>();
    private final ESPRenderer nametagRenderer = new NametagRenderer(ESPRenderer.Align.TOP);

    EventBus<Event> events = event -> {
        if (event instanceof EventGameTick) {
            toRender.clear();
            if (mc.world == null) return;

            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof ItemEntity && !entity.isRemoved()) {
                    toRender.add(entity);
                }
            }
        }
        if (event instanceof Event2D e) {
            if (toRender.isEmpty()) return;

            for (Entity entity : toRender) {
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

                if (anyVisible && maxX >= 0 && maxY >= 0 && minX <= mc.getWindow().getScaledWidth() && minY <= mc.getWindow().getScaledHeight()) {
                    nametagRenderer.render(minX, minY, maxX, maxY, 1.0f, entity);
                }
            }
        }
    };

    public boolean dontRenderNametag(Entity entity) {
        return entity instanceof ItemEntity;
    }
}