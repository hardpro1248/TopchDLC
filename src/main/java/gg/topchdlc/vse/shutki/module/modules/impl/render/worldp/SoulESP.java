package gg.topchdlc.vse.shutki.module.modules.impl.render.worldp;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.events.list.EventReceivePacket;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.ColorUtility;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SoulESP extends Module {
    public static final SoulESP INSTANCE = new SoulESP();

    private static final float DURATION_S = 3.0f;
    private static final float HEIGHT = 3.5f;

    private final SliderSetting duration   = sliderSetting("Длительность", 3.0f, 1.0f, 8.0f).increment(0.5f);
    private final SliderSetting riseHeight = sliderSetting("Высота", 3.5f, 1.0f, 8.0f).increment(0.5f);
    private final CheckBox useCustomColor  = checkbox("Custom color", false);
    private final ColorSetting color1      = colorSetting("Цвет 1", new Color(0x00FFFF)).visible(useCustomColor::get);
    private final ColorSetting color2      = colorSetting("Цвет 2", new Color(0xFF00FF)).visible(useCustomColor::get);
    private final CheckBox onDeath         = checkbox("При смерти", true);
    private final CheckBox onTotem         = checkbox("При тотеме", true);

    private final List<Ghost> ghosts = new ArrayList<>();
    private BufferAllocator allocator;
    private VertexConsumerProvider.Immediate imm;

    private SoulESP() {
        super("SoulESP", Category.RENDER, "Призрак игрока при смерти/тотеме");
    }

    @Override
    protected void onDisable() {
        ghosts.clear();
        if (allocator != null) { allocator.close(); allocator = null; imm = null; }
    }

    EventBus<Event> bus = event -> {
        if (event instanceof EventReceivePacket pkt) onPacket(pkt);
        else if (event instanceof Event3D e) onRender(e);
    };

    private void onPacket(EventReceivePacket pkt) {
        if (mc.player == null || mc.world == null) return;
        if (!(pkt.getPacket() instanceof EntityStatusS2CPacket packet)) return;

        byte status = packet.getStatus();
        boolean isDeath = status == 3 && onDeath.get();
        boolean isTotem = status == 35 && onTotem.get();
        if (!isDeath && !isTotem) return;

        Entity entity = packet.getEntity(mc.world);
        if (!(entity instanceof PlayerEntity player)) return;
        if (player == mc.player) return;

        ghosts.add(new Ghost(player.getEntityPos(), player.getBodyYaw(), player.isSneaking(), player.age));
    }

    private void onRender(Event3D event) {
        if (mc.player == null || mc.world == null || ghosts.isEmpty()) return;

        long now = System.currentTimeMillis();
        float durMs = duration.get() * 1000f;
        ghosts.removeIf(g -> (now - g.time) >= durMs);
        if (ghosts.isEmpty()) return;

        if (allocator == null) {
            allocator = new BufferAllocator(1 << 20);
            imm = VertexConsumerProvider.immediate(allocator);
        }

        MatrixStack m = event.stack;
        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        VertexConsumer buf = imm.getBuffer(ClientPipelines.FILL);

        for (Ghost g : ghosts) {
            float t = (now - g.time) / durMs;
            if (t >= 1f) continue;

            float alpha = (1f - t) * 0.6f;
            float rise  = riseHeight.get() * ease(t);

            Color c1 = useCustomColor.get() ? color1.get() : ClientSettings.INSTANCE.getColor(0);
            Color c2 = useCustomColor.get() ? color2.get() : ClientSettings.INSTANCE.getColor(180);
            Color blended = ColorUtility.blend(c1, c2, t);

            float r  = blended.getRed()   / 255f;
            float gr = blended.getGreen() / 255f;
            float b  = blended.getBlue()  / 255f;

            m.push();
            m.translate(g.pos.x - cam.x, g.pos.y - cam.y + rise, g.pos.z - cam.z);
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - g.yaw));
            m.scale(-1f, -1f, 1f);
            m.translate(0, -1.5, 0);

            if (g.sneak) {
                m.translate(0, 0.2, 0);
                m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(28f));
            }

            float u = 1f / 16f;
            float swing = MathHelper.sin(g.phase * 0.6662f) * 0.6f;
            box(buf, m, -4*u, 0,    -2*u, 8*u, 12*u, 4*u, r, gr, b, alpha);
            box(buf, m, -4*u, -8*u, -4*u, 8*u,  8*u, 8*u, r, gr, b, alpha);
            m.push();
            m.translate(-6*u, 2*u, 0);
            m.multiply(RotationAxis.POSITIVE_X.rotation(-swing));
            m.translate(6*u, -2*u, 0);
            box(buf, m, -8*u, -2*u, -2*u, 4*u, 12*u, 4*u, r, gr, b, alpha);
            m.pop();
            m.push();
            m.translate(6*u, 2*u, 0);
            m.multiply(RotationAxis.POSITIVE_X.rotation(swing));
            m.translate(-6*u, -2*u, 0);
            box(buf, m, 4*u, -2*u, -2*u, 4*u, 12*u, 4*u, r, gr, b, alpha);
            m.pop();
            m.push();
            m.translate(-2*u, 12*u, 0);
            m.multiply(RotationAxis.POSITIVE_X.rotation(swing));
            m.translate(2*u, -12*u, 0);
            box(buf, m, -4*u, 12*u, -2*u, 4*u, 12*u, 4*u, r, gr, b, alpha);
            m.pop();
            m.push();
            m.translate(2*u, 12*u, 0);
            m.multiply(RotationAxis.POSITIVE_X.rotation(-swing));
            m.translate(-2*u, -12*u, 0);
            box(buf, m, 0, 12*u, -2*u, 4*u, 12*u, 4*u, r, gr, b, alpha);
            m.pop();

            m.pop();
        }

        imm.draw();
    }

    private void box(VertexConsumer buf, MatrixStack m,
                     float x, float y, float z,
                     float sx, float sy, float sz,
                     float r, float g, float b, float a) {
        Matrix4f mat = m.peek().getPositionMatrix();
        float x2 = x + sx, y2 = y + sy, z2 = z + sz;
        int ri = (int)(r*255), gi = (int)(g*255), bi = (int)(b*255), ai = (int)(a*255);
        buf.vertex(mat, x,  y,  z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y,  z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y2, z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x,  y2, z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y,  z).color(ri, gi, bi, ai);
        buf.vertex(mat, x,  y,  z).color(ri, gi, bi, ai);
        buf.vertex(mat, x,  y2, z).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y2, z).color(ri, gi, bi, ai);
        buf.vertex(mat, x, y,  z).color(ri, gi, bi, ai);
        buf.vertex(mat, x, y,  z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x, y2, z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x, y2, z).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y,  z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y,  z).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y2, z).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y2, z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x,  y2, z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y2, z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y2, z).color(ri, gi, bi, ai);
        buf.vertex(mat, x,  y2, z).color(ri, gi, bi, ai);
        buf.vertex(mat, x,  y, z).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y, z).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y, z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x,  y, z2).color(ri, gi, bi, ai);
    }

    private float ease(float t) {
        return 1f - (float) Math.pow(1f - MathHelper.clamp(t, 0f, 1f), 3);
    }

    private static class Ghost {
        final Vec3d pos;
        final float yaw;
        final boolean sneak;
        final float phase;
        final long time = System.currentTimeMillis();

        Ghost(Vec3d pos, float yaw, boolean sneak, float phase) {
            this.pos   = pos;
            this.yaw   = yaw;
            this.sneak = sneak;
            this.phase = phase;
        }
    }
}
