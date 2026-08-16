package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RotationAxis;
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

import java.awt.Color;

/**
 * Create by daun kvass
 */
public class FriendMarker extends Module {
    public static final FriendMarker INSTANCE = new FriendMarker();


    public final CheckBox bounce          = checkbox("Bounce Animation", true);
    public final SliderSetting size       = sliderSetting("Size", 0.4f, 0.1f, 1.5f).increment(0.05f);
    public final ColorSetting markerColor = colorSetting("Color", new Color(50, 255, 50, 255));

    private FriendMarker() {super("FriendMarker", Category.RENDER,"над друзьями как из симса ромб");}


    public final EventBus<Event> bus = event -> {
        if (!(event instanceof Event3D e)) return;
        if (mc.world == null || mc.player == null) return;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cam = camera.getCameraPos();
        float tick = mc.getRenderTickCounter().getTickProgress(true);
        long time = System.currentTimeMillis();

        BufferAllocator alloc = new BufferAllocator(65536);
        VertexConsumerProvider.Immediate imm = VertexConsumerProvider.immediate(alloc);
        VertexConsumer vc = imm.getBuffer(ClientPipelines.FILL);
        MatrixStack stack = e.stack;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !Client.FRIENDS.isFriend(player)) continue;
            Vec3d pos = player.getLerpedPos(tick);
            double x = pos.x - cam.x;
            double y = (pos.y + player.getHeight() + 0.6) - cam.y;
            double z = pos.z - cam.z;
            if (bounce.get()) y += Math.sin((time % 2000) / 2000.0 * Math.PI * 2) * 0.15;
            stack.push();
            stack.translate(x, y, z);
            float rotationAngle = ((time % 3000) / 3000.0f) * 360f;
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationAngle));
            float s = size.get();
            Color c = markerColor.get();
            draw3DPlumbob(stack, vc, s * 0.5f, s, c, 255);
            draw3DPlumbob(stack, vc, s, s * 2.0f, c, 120);
            stack.pop();
        }

        imm.draw();
        alloc.close();
    };

    private void draw3DPlumbob(MatrixStack stack, VertexConsumer vc, float w, float h, Color color, int alpha) {
        Matrix4f m = stack.peek().getPositionMatrix();
        float halfW = w / 2f, halfH = h / 2f;
        float p1x = halfW, p1z = 0, p2x = 0, p2z = -halfW, p3x = -halfW, p3z = 0, p4x = 0, p4z = halfW;
        int colorBright  = getArgb(color, alpha, 1.0f);
        int colorMedium  = getArgb(color, alpha, 0.8f);
        int colorDark    = getArgb(color, alpha, 0.6f);
        int colorDarkest = getArgb(color, alpha, 0.45f);
        drawTriangle(vc, m, 0, halfH, 0,  p1x, 0, p1z,  p4x, 0, p4z, colorBright);
        drawTriangle(vc, m, 0, halfH, 0,  p2x, 0, p2z,  p1x, 0, p1z, colorMedium);
        drawTriangle(vc, m, 0, halfH, 0,  p3x, 0, p3z,  p2x, 0, p2z, colorDark);
        drawTriangle(vc, m, 0, halfH, 0,  p4x, 0, p4z,  p3x, 0, p3z, colorMedium);
        drawTriangle(vc, m, 0, -halfH, 0,  p4x, 0, p4z,  p1x, 0, p1z, colorMedium);
        drawTriangle(vc, m, 0, -halfH, 0,  p1x, 0, p1z,  p2x, 0, p2z, colorDark);
        drawTriangle(vc, m, 0, -halfH, 0,  p2x, 0, p2z,  p3x, 0, p3z, colorDarkest);
        drawTriangle(vc, m, 0, -halfH, 0,  p3x, 0, p3z,  p4x, 0, p4z, colorDark);
    }

    private static void drawTriangle(VertexConsumer vc, Matrix4f m,
                                     float x1, float y1, float z1, float x2, float y2, float z2,
                                     float x3, float y3, float z3, int argb) {
        vc.vertex(m, x1, y1, z1).color(argb);
        vc.vertex(m, x2, y2, z2).color(argb);
        vc.vertex(m, x3, y3, z3).color(argb);
        vc.vertex(m, x3, y3, z3).color(argb);
    }

    private int getArgb(Color c, int alpha, float brightness) {
        int r = (int)(c.getRed() * brightness);
        int g = (int)(c.getGreen() * brightness);
        int b = (int)(c.getBlue() * brightness);
        return ((alpha & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
}
