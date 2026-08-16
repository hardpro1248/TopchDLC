package gg.topchdlc.vse.shutki.module.modules.impl.render.cosmetic;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.ColorUtility;

import java.awt.*;
import java.util.*;
import java.util.List;

import static gg.topchdlc.MinecraftHolder.mc;

public class Trails {
    public static final Trails INSTANCE = new Trails();
    private boolean enabled = false;
    float height = 1.7f;
    public final SliderSetting duration = new SliderSetting("Время жизни", 2.0f, 0.5f, 5.0f).increment(0.1f);
    public final SliderSetting opacity = new SliderSetting("Прозрачность", 0.4f, 0.1f, 1.0f).increment(0.05f);
    public final CheckBox self = new CheckBox("Для себя", true);
    public final CheckBox friends = new CheckBox("Для друзей", true);
    public final CheckBox useCustomColor = new CheckBox("Свой цвет", false);
    public final ColorSetting colorStart = new ColorSetting("Начало", new Color(0x00A6FF)).visible(useCustomColor::get);
    public final ColorSetting colorEnd = new ColorSetting("Конец", new Color(0xFF00E5)).visible(useCustomColor::get);

    private final Map<UUID, List<Point>> trails = new HashMap<>();
    private BufferAllocator allocator;
    private VertexConsumerProvider.Immediate imm;

    protected void onDisable() {
        trails.clear();
        if (allocator != null) { allocator.close(); allocator = null; imm = null; }
    }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) {
        enabled = v;
        if (!v) trails.clear();
    }

    public EventBus<Event> bus = event -> {
        if (!enabled) return;
        if (event instanceof Event3D e) onRender(e);
    };

    private void onRender(Event3D event) {
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        float maxAge = duration.get() * 1000f;

        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);

        for (PlayerEntity player : mc.world.getPlayers()) {
            boolean isLocal = player == mc.player;
            if (isLocal && !self.get()) continue;
            if (!isLocal && !friends.get()) continue;

            if (isLocal && mc.options.getPerspective().isFirstPerson()) continue;

            List<Point> points = trails.computeIfAbsent(player.getUuid(), k -> new ArrayList<>());

            Vec3d currentPos = player.getLerpedPos(tickDelta).add(0, 0.1, 0);

            if (points.isEmpty() || points.get(points.size() - 1).pos.distanceTo(currentPos) > 0.01) {
                points.add(new Point(currentPos, now));
            }

            points.removeIf(p -> (now - p.time) > maxAge);
        }

        trails.entrySet().removeIf(e -> e.getValue().isEmpty());
        if (trails.isEmpty()) return;

        if (allocator == null) {
            allocator = new BufferAllocator(1 << 20);
            imm = VertexConsumerProvider.immediate(allocator);
        }

        MatrixStack ms = event.stack;
        VertexConsumer buf = imm.getBuffer(ClientPipelines.FILL);

        ms.push();
        Client.RENDERER.toCamera(ms);
        Matrix4f mat = ms.peek().getPositionMatrix();

        for (Map.Entry<UUID, List<Point>> entry : trails.entrySet()) {
            List<Point> points = entry.getValue();
            if (points.size() < 2) continue;

            for (int i = 0; i < points.size() - 1; i++) {
                Point p1 = points.get(i);
                Point p2 = points.get(i + 1);

                float age1 = (float) (now - p1.time) / maxAge;
                float age2 = (float) (now - p2.time) / maxAge;

                Color c1 = getGradientColor(age1);
                Color c2 = getGradientColor(age2);

                drawRibbonSegment(buf, mat, p1.pos, p2.pos, height, c1, c2);
            }
        }

        ms.pop();
        imm.draw();
    }

    private void drawRibbonSegment(VertexConsumer buf, Matrix4f mat, Vec3d p1, Vec3d p2, float h, Color c1, Color c2) {
        int r1 = c1.getRed(), g1 = c1.getGreen(), b1 = c1.getBlue(), a1 = c1.getAlpha();
        int r2 = c2.getRed(), g2 = c2.getGreen(), b2 = c2.getBlue(), a2 = c2.getAlpha();

        buf.vertex(mat, (float)p1.x, (float)p1.y, (float)p1.z).color(r1, g1, b1, a1);
        buf.vertex(mat, (float)p2.x, (float)p2.y, (float)p2.z).color(r2, g2, b2, a2);
        buf.vertex(mat, (float)p2.x, (float)p2.y + h, (float)p2.z).color(r2, g2, b2, a2);
        buf.vertex(mat, (float)p1.x, (float)p1.y + h, (float)p1.z).color(r1, g1, b1, a1);

        buf.vertex(mat, (float)p1.x, (float)p1.y + h, (float)p1.z).color(r1, g1, b1, a1);
        buf.vertex(mat, (float)p2.x, (float)p2.y + h, (float)p2.z).color(r2, g2, b2, a2);
        buf.vertex(mat, (float)p2.x, (float)p2.y, (float)p2.z).color(r2, g2, b2, a2);
        buf.vertex(mat, (float)p1.x, (float)p1.y, (float)p1.z).color(r1, g1, b1, a1);
    }

    private Color getGradientColor(float age) {
        Color base;
        if (useCustomColor.get()) {
            base = ColorUtility.blend(colorStart.get(), colorEnd.get(), age);
        } else {
            base = ClientSettings.INSTANCE.getColor((int)(age * 180));
        }

        float alphaFactor = (1.0f - age) * opacity.get();
        int alpha = MathHelper.clamp((int) (alphaFactor * 255), 0, 255);

        return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
    }

    private static class Point {
        final Vec3d pos;
        final long time;
        Point(Vec3d pos, long time) { this.pos = pos; this.time = time; }
    }
}