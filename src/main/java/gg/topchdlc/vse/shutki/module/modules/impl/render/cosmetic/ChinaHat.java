package gg.topchdlc.vse.shutki.module.modules.impl.render.cosmetic;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.ColorUtility;

import java.awt.*;
import java.util.List;

import static gg.topchdlc.MinecraftHolder.mc;

/**
 * Created by daun kvass
 */
public class ChinaHat {
    public static final ChinaHat INSTANCE = new ChinaHat();

    private boolean enabled = false;

    public enum Mode {
        China("Шляпа"), Nimb("Нимб");
        private final String name;
        Mode(String n) { this.name = n; }
        @Override public String toString() { return name; }
    }

    public final EnumSetting<Mode> mode      = new EnumSetting<>("Режим", Mode.China);
    public final CheckBox self               = new CheckBox("Self", true);
    public final CheckBox friends            = new CheckBox("Friends", true);
    public final CheckBox others             = new CheckBox("Others", false);
    public final CheckBox useCustomColor     = new CheckBox("Custom color", false);
    public final ColorSetting customColor    = new ColorSetting("Color", new Color(0x00FFFF)).visible(useCustomColor::get);

    private static final int   SEGMENTS    = 60;
    private static final float PI2         = (float)(Math.PI * 2);
    private static final float HAT_WIDTH   = 0.62f;
    private static final float CONE_HEIGHT = 0.3f;
    private static final Identifier BLOOM  = Identifier.of("topchdlc", "images/world/bloom.png");
    private static final float NIMB_RADIUS  = 0.32f;
    private static final float NIMB_GLOW    = 0.09f;
    private static final int   NIMB_SEGS    = 50;

    private float nimbAngle = 0f;
    private long  lastTime  = System.currentTimeMillis();

    private ChinaHat() {}

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) {
        enabled = v;
    }

    public final EventBus<Event> bus = event -> {
        if (!enabled) return;
        if (event instanceof Event3D e) {
            onRender(e.stack, e.buffer, mode.get() == Mode.Nimb);
        }
    };

    private void onRender(MatrixStack stack, VertexConsumerProvider bufferSource, boolean isNimb) {
        if (mc.world == null || mc.player == null) return;
        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastTime) / 1000f, 0.1f);
        lastTime = now;
        nimbAngle = (nimbAngle + dt * 55f) % 360f;

        List<PlayerEntity> targets = mc.world.getEntitiesByClass(PlayerEntity.class,
                mc.player.getBoundingBox().expand(64), p -> {
                    if (p == mc.player) return self.get();
                    if (Client.FRIENDS.isFriend(p)) return friends.get();
                    return others.get();
                });
        targets.removeIf(p -> p == mc.player && mc.options.getPerspective().isFirstPerson());
        if (targets.isEmpty()) return;

        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();

        if (!isNimb) {
            for (PlayerEntity p : targets) {
                renderChina(stack, bufferSource, p, p.getLerpedPos(tickDelta), cam);
            }
            if (bufferSource instanceof VertexConsumerProvider.Immediate i) {
                i.draw();
            }
        } else {
            GlStateManager._depthMask(false);
            GlStateManager._disableCull();
            GlStateManager._blendFuncSeparate(GlConst.GL_SRC_ALPHA, GlConst.GL_ONE, GlConst.GL_ZERO, GlConst.GL_ONE);
            GlStateManager._enableBlend();

            for (PlayerEntity p : targets) {
                renderNimb(stack, bufferSource, p, p.getLerpedPos(tickDelta), cam, now);
            }
            if (bufferSource instanceof VertexConsumerProvider.Immediate i) {
                i.draw();
            }

            GlStateManager._blendFuncSeparate(GlConst.GL_ONE, GlConst.GL_ZERO, GlConst.GL_ONE, GlConst.GL_ZERO);
            GlStateManager._disableBlend();
            GlStateManager._enableCull();
            GlStateManager._depthMask(true);
        }
    }

    private void renderChina(MatrixStack stack, VertexConsumerProvider bufferSource,
                             PlayerEntity p, Vec3d pos, Vec3d cam) {
        Color c = getColor(0);
        int centerArgb  = ColorUtility.replAlpha(c.getRGB(), 200);
        int edgeArgb    = ColorUtility.replAlpha(c.getRGB(), 80);
        int outlineArgb = ColorUtility.replAlpha(c.getRGB(), 255);
        double hx = pos.x, hy = pos.y + p.getHeight() + 0.05, hz = pos.z;
        float yaw = p.getHeadYaw();

        stack.push();
        stack.translate(hx - cam.x, hy - cam.y, hz - cam.z);
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        Matrix4f mat = stack.peek().getPositionMatrix();

        VertexConsumer fill1 = bufferSource.getBuffer(ClientPipelines.QUAD);
        for (int i = 0; i < SEGMENTS; i++) {
            float a1 = i * PI2 / SEGMENTS, a2 = (i + 1) * PI2 / SEGMENTS;
            float x1 = -MathHelper.sin(a1) * HAT_WIDTH, z1 = MathHelper.cos(a1) * HAT_WIDTH;
            float x2 = -MathHelper.sin(a2) * HAT_WIDTH, z2 = MathHelper.cos(a2) * HAT_WIDTH;
            fill1.vertex(mat, 0, CONE_HEIGHT, 0).color(centerArgb);
            fill1.vertex(mat, x2, 0, z2).color(edgeArgb);
            fill1.vertex(mat, x1, 0, z1).color(edgeArgb);
            fill1.vertex(mat, 0, CONE_HEIGHT, 0).color(centerArgb);
        }

        VertexConsumer fill2 = bufferSource.getBuffer(ClientPipelines.QUAD);
        for (int i = 0; i < SEGMENTS; i++) {
            float a1 = i * PI2 / SEGMENTS, a2 = (i + 1) * PI2 / SEGMENTS;
            float x1 = -MathHelper.sin(a1) * HAT_WIDTH, z1 = MathHelper.cos(a1) * HAT_WIDTH;
            float x2 = -MathHelper.sin(a2) * HAT_WIDTH, z2 = MathHelper.cos(a2) * HAT_WIDTH;
            fill2.vertex(mat, 0, CONE_HEIGHT, 0).color(centerArgb);
            fill2.vertex(mat, x1, 0, z1).color(edgeArgb);
            fill2.vertex(mat, x2, 0, z2).color(edgeArgb);
            fill2.vertex(mat, 0, CONE_HEIGHT, 0).color(centerArgb);
        }

        VertexConsumer lines = bufferSource.getBuffer(ClientPipelines.OUTLINE);
        for (int i = 0; i < SEGMENTS; i++) {
            float a1 = i * PI2 / SEGMENTS, a2 = (i + 1) * PI2 / SEGMENTS;
            float x1 = -MathHelper.sin(a1) * HAT_WIDTH, z1 = MathHelper.cos(a1) * HAT_WIDTH;
            float x2 = -MathHelper.sin(a2) * HAT_WIDTH, z2 = MathHelper.cos(a2) * HAT_WIDTH;
            lines.vertex(mat, x1, 0, z1).color(outlineArgb);
            lines.vertex(mat, x2, 0, z2).color(outlineArgb);
        }
        stack.pop();
    }

    private void renderNimb(MatrixStack stack, VertexConsumerProvider imm,
                            PlayerEntity p, Vec3d pos, Vec3d cam, long now) {
        float elapsed = now / 1000f;
        Color c1 = getColor((int)(elapsed * 35));
        Color c2 = getColor((int)(elapsed * 35) + 70);
        float pulse = 1f + 0.05f * (float)Math.sin(elapsed * 4.5f);
        float R = NIMB_RADIUS * pulse;
        double hx = pos.x, hy = pos.y + p.getHeight() + 0.22, hz = pos.z;
        VertexConsumer buf = imm.getBuffer(ClientPipelines.NIMB.apply(BLOOM));
        Quaternionf camRot = mc.gameRenderer.getCamera().getRotation();

        for (int i = 0; i < NIMB_SEGS; i++) {
            float angle = (float)i / NIMB_SEGS * PI2 + (float)Math.toRadians(nimbAngle);
            float px = (float)Math.cos(angle) * R, pz = (float)Math.sin(angle) * R;
            Color c = ColorUtility.blend(c1, c2, (float)i / NIMB_SEGS);
            drawBillboard(stack, buf, camRot, hx - cam.x + px, hy - cam.y, hz - cam.z + pz, NIMB_GLOW, ColorUtility.injectAlpha(c, 180));
        }
        for (int i = 0; i < NIMB_SEGS; i++) {
            float angle = (float)i / NIMB_SEGS * PI2 + (float)Math.toRadians(nimbAngle);
            float px = (float)Math.cos(angle) * R, pz = (float)Math.sin(angle) * R;
            Color c = ColorUtility.blend(c1, c2, (float)i / NIMB_SEGS);
            drawBillboard(stack, buf, camRot, hx - cam.x + px, hy - cam.y, hz - cam.z + pz, NIMB_GLOW * 2.2f, ColorUtility.injectAlpha(c, 55));
        }
        for (int i = 0; i < NIMB_SEGS; i++) {
            float angle = (float)i / NIMB_SEGS * PI2 + (float)Math.toRadians(nimbAngle);
            float px = (float)Math.cos(angle) * R, pz = (float)Math.sin(angle) * R;
            Color c = ColorUtility.blend(c1, c2, (float)i / NIMB_SEGS);
            drawBillboard(stack, buf, camRot, hx - cam.x + px, hy - cam.y, hz - cam.z + pz, NIMB_GLOW * 4.0f, ColorUtility.injectAlpha(c, 20));
        }
    }

    private void drawBillboard(MatrixStack stack, VertexConsumer buf, Quaternionf camRot,
                               double wx, double wy, double wz, float size, Color color) {
        stack.push();
        stack.translate(wx, wy, wz);
        stack.multiply(camRot);
        var m = stack.peek();
        int rgb = color.getRGB();
        buf.vertex(m, -size, -size, 0).texture(0, 0).color(rgb);
        buf.vertex(m, -size,  size, 0).texture(0, 1).color(rgb);
        buf.vertex(m,  size,  size, 0).texture(1, 1).color(rgb);
        buf.vertex(m,  size, -size, 0).texture(1, 0).color(rgb);
        stack.pop();
    }

    private Color getColor(int offset) {
        return useCustomColor.get() ? customColor.get() : ClientSettings.INSTANCE.getColor(offset);
    }
}