package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.utils.math.MathUtility;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;

import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.*;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event2D;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Create by daun kvass
 */
public class Prediction extends Module {
    public static final Prediction INSTANCE = new Prediction();

    private final Group typesGroup = group("Типы");
    private final CheckBox pearlsEnabled = typesGroup.checkbox("Эндер-жемчуг", true);
    private final CheckBox arrowsEnabled = typesGroup.checkbox("Стрелы", true);
    private final CheckBox tridentsEnabled = typesGroup.checkbox("Трезубцы", true);

    private final Group visualGroup = group("Визуал");
    private final ColorSetting lineColor = visualGroup.colorSetting("Цвет линии", new Color(255, 255, 255, 90));
    private final ColorSetting landingColor = visualGroup.colorSetting("Цвет точки", new Color(255, 80, 80, 120));
    private final SliderSetting landingSize = visualGroup.sliderSetting("Размер точки", 0.3f, 0.1f, 1f).increment(0.05f);
    private final SliderSetting maxTicks = visualGroup.sliderSetting("Макс. тиков", 300, 50, 500);
    private final CheckBox showName = visualGroup.checkbox("Название предмета", false);
    private final CheckBox showIcon = visualGroup.checkbox("Иконка предмета", true);
    private final CheckBox showTime = visualGroup.checkbox("Время до падения", true);
    private final CheckBox showTrail = visualGroup.checkbox("Линия траектории", true);

    private static class TimeAnimation {
        float currentValue;
        float targetValue;
        float alpha = 1.0f;
        long lastUpdateTime;

        TimeAnimation(float initialValue) {
            this.currentValue = initialValue;
            this.targetValue = initialValue;
            this.lastUpdateTime = System.currentTimeMillis();
        }

        void update(float newTarget) {
            long now = System.currentTimeMillis();
            float deltaTime = (now - lastUpdateTime) / 1000f;
            lastUpdateTime = now;
            if (Math.abs(newTarget - targetValue) > 0.05f) {
                targetValue = newTarget;
                alpha = 0.3f;
            }
            float lerpSpeed = 8f;
            currentValue += (targetValue - currentValue) * Math.min(1f, deltaTime * lerpSpeed);
            if (alpha < 1.0f) {
                alpha += deltaTime * 5f;
                if (alpha > 1.0f) alpha = 1.0f;
            }
        }

        float getValue() {
            return currentValue;
        }

        float getAlpha() {
            return alpha;
        }
    }

    private final java.util.Map<Integer, TimeAnimation> timeAnimations = new java.util.HashMap<>();
    private final List<UIRenderData> uiRenderCache = new ArrayList<>();

    private Prediction() {
        super("Prediction", Category.RENDER, "Показывает место приземления снарядов");
    }

    EventBus<Event> bus = event -> {
        if (event instanceof Event3D e) onRender3D(e);
        if (event instanceof Event2D e) onRender2D(e);
    };

    private void onRender3D(Event3D event) {
        if (mc.world == null || mc.player == null) return;
        MatrixStack stack = event.stack;
        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        BufferAllocator allocator = new BufferAllocator(262144);
        VertexConsumerProvider.Immediate imm = VertexConsumerProvider.immediate(allocator);
        java.util.Set<Integer> activeEntities = new java.util.HashSet<>();
        uiRenderCache.clear();
        try {
            for (Entity entity : mc.world.getEntities()) {
                ProjectileInfo info = getInfo(entity);
                if (info == null) continue;
                activeEntities.add(entity.getId());
                SimResult result = simulate(entity, info);
                if (result == null || result.points.isEmpty()) continue;
                stack.push();
                stack.translate(-cam.x, -cam.y, -cam.z);
                if (showTrail.get() && result.points.size() > 1) {
                    drawTrailLine(stack, imm, result.points);
                }
                if (result.landingPos != null) {
                    drawLandingMarker(stack, imm, result.landingPos);
                }
                stack.pop();
                if (result.landingPos != null) {
                    double dist = cam.distanceTo(result.landingPos);
                    if (dist <= 64) {
                        Vec3d screenPos = worldToScreen(result.landingPos);
                        if (screenPos != null) {
                            uiRenderCache.add(new UIRenderData(
                                    screenPos, info, result.ticksToLand, entity.getId(), dist
                            ));
                        }
                    }
                }
            }

            imm.draw();
        } finally {
            allocator.close();
        }
        timeAnimations.keySet().removeIf(id -> !activeEntities.contains(id));
    }

    private void onRender2D(Event2D event) {
        if (mc.world == null || mc.player == null) return;

        DrawContext context = Client.RENDERER.getDrawContext();
        if (context == null) return;
        for (UIRenderData data : uiRenderCache) {
            drawLandingInfo(context, data);
        }
    }

    private ProjectileInfo getInfo(Entity entity) {
        if (entity instanceof EnderPearlEntity && pearlsEnabled.get()) {
            return new ProjectileInfo(ProjectileType.PEARL, "Эндер-жемчуг", new ItemStack(Items.ENDER_PEARL));
        }
        if ((entity instanceof ArrowEntity || entity instanceof SpectralArrowEntity) && arrowsEnabled.get()) {
            boolean spectral = entity instanceof SpectralArrowEntity;
            String name = spectral ? "Спектральная стрела" : "Стрела";
            ItemStack icon = spectral
                    ? new ItemStack(Items.SPECTRAL_ARROW)
                    : new ItemStack(Items.ARROW);
            return new ProjectileInfo(ProjectileType.ARROW, name, icon);
        }
        if (entity instanceof TridentEntity && tridentsEnabled.get()) {
            return new ProjectileInfo(ProjectileType.TRIDENT, "Трезубец", new ItemStack(Items.TRIDENT));
        }
        return null;
    }

    private SimResult simulate(Entity entity, ProjectileInfo info) {
        List<Vec3d> points = new ArrayList<>();

        double posX = entity.getX();
        double posY = entity.getY();
        double posZ = entity.getZ();

        Vec3d velocity = entity.getVelocity();
        double velX = velocity.x;
        double velY = velocity.y;
        double velZ = velocity.z;

        float gravity = info.type.gravity;
        float drag = info.type.drag;
        int maxT = maxTicks.getInt();

        points.add(new Vec3d(posX, posY, posZ));

        for (int i = 0; i < maxT; i++) {
            double prevX = posX;
            double prevY = posY;
            double prevZ = posZ;

            posX += velX;
            posY += velY;
            posZ += velZ;

            velY -= gravity;
            velX *= drag;
            velY *= drag;
            velZ *= drag;

            points.add(new Vec3d(posX, posY, posZ));

            Vec3d from = new Vec3d(prevX, prevY, prevZ);
            Vec3d to = new Vec3d(posX, posY, posZ);

            var hitResult = mc.world.raycast(new RaycastContext(
                    from, to,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    entity
            ));

            if (hitResult != null && hitResult.getType() != net.minecraft.util.hit.HitResult.Type.MISS) {
                Vec3d hitPos = hitResult.getPos();
                points.add(hitPos);
                return new SimResult(points, hitPos, i + 1);
            }

            if (posY < mc.world.getBottomY() - 10) {
                return new SimResult(points, new Vec3d(posX, mc.world.getBottomY(), posZ), i + 1);
            }
        }

        return new SimResult(points, points.get(points.size() - 1), maxT);
    }

    private void drawTrailLine(MatrixStack stack, VertexConsumerProvider.Immediate imm, List<Vec3d> points) {
        VertexConsumer buf = imm.getBuffer(ClientPipelines.OUTLINE);
        Matrix4f mat = stack.peek().getPositionMatrix();

        Color lc = lineColor.get();
        int r = lc.getRed(), g = lc.getGreen(), b = lc.getBlue();

        for (int i = 0; i < points.size(); i++) {
            Vec3d p = points.get(i);
            float progress = (float) i / (points.size() - 1);
            int a = Math.max(5, (int) (lc.getAlpha() * (1f - progress * 0.85f)));
            buf.vertex(mat, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, a);
        }
    }

    private void drawLandingMarker(MatrixStack stack, VertexConsumerProvider.Immediate imm, Vec3d pos) {
        float size = landingSize.get();
        Color lc = landingColor.get();
        drawCross(stack, imm, pos, size, lc);
        drawCircle(stack, imm, pos, size, lc);
    }

    private void drawCross(MatrixStack stack, VertexConsumerProvider.Immediate imm, Vec3d pos, float size, Color c) {
        VertexConsumer buf = imm.getBuffer(ClientPipelines.OUTLINE);
        Matrix4f mat = stack.peek().getPositionMatrix();
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue(), a = c.getAlpha();
        float x = (float) pos.x, y = (float) pos.y + 0.02f, z = (float) pos.z;

        buf.vertex(mat, x - size, y, z - size).color(r, g, b, a);
        buf.vertex(mat, x + size, y, z + size).color(r, g, b, a);

        imm.draw();
        buf = imm.getBuffer(ClientPipelines.OUTLINE);

        buf.vertex(mat, x + size, y, z - size).color(r, g, b, a);
        buf.vertex(mat, x - size, y, z + size).color(r, g, b, a);
    }

    private void drawCircle(MatrixStack stack, VertexConsumerProvider.Immediate imm, Vec3d pos, float radius, Color c) {
        VertexConsumer buf = imm.getBuffer(ClientPipelines.OUTLINE);
        Matrix4f mat = stack.peek().getPositionMatrix();
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue(), a = c.getAlpha();
        float x = (float) pos.x, y = (float) pos.y + 0.02f, z = (float) pos.z;
        int segments = 24;

        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2.0 / segments) * i;
            float px = x + (float) (Math.cos(angle) * radius);
            float pz = z + (float) (Math.sin(angle) * radius);
            buf.vertex(mat, px, y, pz).color(r, g, b, a);
        }
    }

    private void drawLandingInfo(DrawContext context, UIRenderData data) {
        float screenX = (float) data.screenPos.x;
        float screenY = (float) data.screenPos.y;
        float textSize = (float) Math.max(5f, 7f - data.distance * 0.05f);
        String itemName = data.info.itemName;
        float seconds = data.ticksToLand / 20f;
        TimeAnimation animation = timeAnimations.computeIfAbsent(data.entityId, id -> new TimeAnimation(seconds));
        animation.update(seconds);

        float animatedSeconds = animation.getValue();
        float timeAlpha = animation.getAlpha();

        String timeStr = String.format("%.1fс", animatedSeconds);
        String displayText = "";
        if (showName.get()) {
            displayText = itemName;
        }
        if (showTime.get()) {
            if (!displayText.isEmpty()) displayText += "  ";
            displayText += timeStr;
        }

        float textWidth = Client.RENDERER.textWidth(displayText, TextureUse.SFMEDIUM, textSize);
        float iconSize = textSize + 2;
        float gap = 3f;
        float totalWidth = (showIcon.get() ? iconSize + gap : 0) + textWidth;

        float startX = screenX - totalWidth / 2f;
        float textY = screenY - iconSize / 2f;

        float bgPad = 3f;
        Color bgColor = new Color(0, 0, 0, 50);
        Client.RENDERER.rect(
                startX - bgPad,
                textY - bgPad,
                totalWidth + bgPad * 2,
                iconSize + bgPad * 2,
                new Vector4f(1),
                1f,
                bgColor, bgColor, bgColor, bgColor
        );
        Vector4f round = new Vector4f(5f);
        Client.RENDERER.blur(startX - bgPad, textY - bgPad, totalWidth + bgPad * 2, 0.3f, round, 15f,1);
        Color borderColor = new Color(255, 255, 255, 35);
        Client.RENDERER.rect(startX - bgPad, textY - bgPad, totalWidth + bgPad * 2, 0.3f,
                new Vector4f(2), 1f, borderColor, borderColor, borderColor, borderColor);
        Client.RENDERER.rect(startX - bgPad, textY + iconSize + bgPad - 0.5f, totalWidth + bgPad * 2, 0.3f,
                new Vector4f(2), 1f, borderColor, borderColor, borderColor, borderColor);
        Client.RENDERER.rect(startX - bgPad, textY - bgPad, 0.3f, iconSize + bgPad * 2,
                new Vector4f(2), 1f, borderColor, borderColor, borderColor, borderColor);
        Client.RENDERER.rect(startX + totalWidth + bgPad - 0.5f, textY - bgPad, 0.3f, iconSize + bgPad * 2,
                new Vector4f(2), 1f, borderColor, borderColor, borderColor, borderColor);
        if (showIcon.get()) {
            renderItemIcon(context, data.info.icon, startX, textY, iconSize);
        }

        if (!displayText.isEmpty()) {
            float nameX = startX + (showIcon.get() ? iconSize + gap : 0);
            float nameY = textY + (iconSize - textSize) / 2f;

            if (showName.get() && showTime.get()) {
                float nameWidth = Client.RENDERER.textWidth(itemName, TextureUse.SFMEDIUM, textSize);
                Client.RENDERER.text(itemName, nameX, nameY, TextureUse.SFMEDIUM, textSize,
                        new Color(255, 255, 255, 160));
                float timeX = nameX + nameWidth + Client.RENDERER.textWidth("  ", TextureUse.SFMEDIUM, textSize);
                int timeColorAlpha = (int) (140 * timeAlpha);
                Color timeColor = new Color(
                        landingColor.get().getRed(),
                        landingColor.get().getGreen(),
                        landingColor.get().getBlue(),
                        timeColorAlpha
                );
                Client.RENDERER.text(timeStr, timeX, nameY, TextureUse.SFMEDIUM, textSize, timeColor);
            } else {
                int textAlpha = showTime.get() ? (int) (140 * timeAlpha) : 160;
                Color textColor = showTime.get()
                        ? new Color(landingColor.get().getRed(), landingColor.get().getGreen(), landingColor.get().getBlue(), textAlpha)
                        : new Color(255, 255, 255, 160);
                Client.RENDERER.text(displayText, nameX, nameY, TextureUse.SFMEDIUM, textSize, textColor);
            }
        }
    }

    private void renderItemIcon(DrawContext context, ItemStack itemStack, float x, float y, float size) {
        if (itemStack.isEmpty()) return;

        var matrix = context.getMatrices();
        matrix.pushMatrix();
        matrix.translate(x, y);

        float scale = size / 16f;
        matrix.scale(scale, scale);

        context.drawItem(mc.player, itemStack, 0, 0, 0);
        context.drawStackOverlay(mc.textRenderer, itemStack, 0, 0);

        matrix.popMatrix();
    }

    private Vec3d worldToScreen(Vec3d worldPos) {
        Vec3d screen = MathUtility.worldSpaceToScreenSpace(worldPos);
        if (screen == null || screen.z < 0.0 || screen.z > 1.0) return null;
        return screen;
    }

    enum ProjectileType {
        PEARL(0.03f, 0.99f),
        ARROW(0.05f, 0.99f),
        TRIDENT(0.05f, 0.99f);

        final float gravity;
        final float drag;

        ProjectileType(float gravity, float drag) {
            this.gravity = gravity;
            this.drag = drag;
        }
    }

    private static class ProjectileInfo {
        final ProjectileType type;
        final String itemName;
        final ItemStack icon;

        ProjectileInfo(ProjectileType type, String itemName, ItemStack icon) {
            this.type = type;
            this.itemName = itemName;
            this.icon = icon;
        }
    }

    private static class SimResult {
        final List<Vec3d> points;
        final Vec3d landingPos;
        final int ticksToLand;

        SimResult(List<Vec3d> points, Vec3d landing, int ticksToLand) {
            this.points = points;
            this.landingPos = landing;
            this.ticksToLand = ticksToLand;
        }
    }

    private static class UIRenderData {
        final Vec3d screenPos;
        final ProjectileInfo info;
        final int ticksToLand;
        final int entityId;
        final double distance;

        UIRenderData(Vec3d screenPos, ProjectileInfo info, int ticksToLand, int entityId, double distance) {
            this.screenPos = screenPos;
            this.info = info;
            this.ticksToLand = ticksToLand;
            this.entityId = entityId;
            this.distance = distance;
        }
    }

    @Override
    public void toggle() {
        super.toggle();
        if (!isEnabled()) {
            timeAnimations.clear();
        }
    }
}