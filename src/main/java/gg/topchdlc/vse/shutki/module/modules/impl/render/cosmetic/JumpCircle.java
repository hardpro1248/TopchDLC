package gg.topchdlc.vse.shutki.module.modules.impl.render.cosmetic;

import org.joml.Matrix4f;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.ColorUtility;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static gg.topchdlc.MinecraftHolder.mc;

public class JumpCircle {
    public static final JumpCircle INSTANCE = new JumpCircle();

    private boolean enabled = false;

    public final EnumSetting<Image> image = new EnumSetting<>("Image", Image.Circle);

    public enum Image {
        Jump("jump"),
        Circle("circle");
        final Identifier id;
        Image(String image) { id = Identifier.of("topchdlc", "images/world/" + image + ".png"); }
    }

    static class Circle {
        public final float randomRotation = (float) (Math.random() * 360f);
        public final Vec3d pos;
        public final long start = System.currentTimeMillis();
        public final long durationMs;
        public final float startSize = 0.3f, endSize = 2.0f;

        public Circle(Vec3d pos, Image mode) {
            this.pos = pos;
            this.durationMs = (mode == Image.Jump) ? 1000 : 1800;
        }

        public float t() { return Math.min(1f, (System.currentTimeMillis() - start) / (float) durationMs); }

        private float easeOutQuint(float x) { float i = 1f - x; return 1f - i * i * i * i * i; }
        private float easeOutQuad(float x) { return 1 - (1 - x) * (1 - x); }

        public boolean alive() { return System.currentTimeMillis() - start <= durationMs; }

        public float size(Image mode) {
            if (mode == Image.Jump) {
                return startSize + (endSize - startSize) * easeOutQuad(t());
            } else {
                float k = easeOutQuint(t());
                float base = startSize + (1.8f - startSize) * k;
                return base + (float) Math.sin(System.currentTimeMillis() * 0.008) * 0.06f * (1f - t());
            }
        }

        public float alpha(Image mode) {
            if (mode == Image.Jump) {
                return 1f - t();
            } else {
                float t = t();
                if (t < 0.3f) return 1f;
                else if (t < 0.6f) {
                    float local = (t - 0.3f) / 0.3f;
                    return 1f - 0.15f * (1f - (float)Math.cos((local * Math.PI) / 2.0));
                }
                else {
                    float local = (t - 0.6f) / 0.4f;
                    float x = Math.max(0f, Math.min(1f, local));
                    return 0.85f * (1f - x * x * (3f - 2f * x));
                }
            }
        }
    }

    private final List<Circle> circles = new CopyOnWriteArrayList<>();
    private boolean wasOnGround = false;

    private JumpCircle() {}

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) {
        enabled = v;
        if (!v) circles.clear();
    }

    public final EventBus<Event> events = event -> {
        if (!enabled) return;
        if (event instanceof EventGameTick) {
            if (mc.player == null || mc.world == null) return;
            circles.removeIf(c -> !c.alive());
            boolean onGround = mc.player.isOnGround();
            double vy = mc.player.getVelocity().y;
            if (wasOnGround && !onGround && vy > 0.08) {
                Vec3d origin = mc.player.getEntityPos().add(0, 0.1, 0);
                double yBelow = raycastDownY(origin, 3.5);
                double y = Double.isFinite(yBelow) ? yBelow + 0.01 : Math.floor(mc.player.getBoundingBox().minY) + 0.01;
                circles.add(new Circle(new Vec3d(origin.x, y, origin.z), image.get()));
            }
            wasOnGround = onGround;
        }
        if (event instanceof Event3D e) {
            if (circles.isEmpty()) return;

            MatrixStack stack = e.stack;
            Image currentMode = image.get();

            VertexConsumer consumer = e.buffer.getBuffer(ClientPipelines.TARGET_ESP.apply(currentMode.id));
            Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();

            for (var c : circles) {
                float alpha = c.alpha(currentMode);
                if (alpha <= 0.001f) continue;

                float sz = c.size(currentMode);
                int a = (int)(alpha * 255);

                Color c1 = ColorUtility.injectAlpha(ClientSettings.INSTANCE.getColor(0), a);
                Color c2 = ColorUtility.injectAlpha(ClientSettings.INSTANCE.getColor(90), a);

                stack.push();
                stack.translate(c.pos.x - camPos.x, c.pos.y - camPos.y, c.pos.z - camPos.z);

                if (currentMode == Image.Jump) {
                    stack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(c.randomRotation));
                }

                Matrix4f model = stack.peek().getPositionMatrix();
                drawVertex(consumer, model, -sz, -sz, 0, 0, c1.getRGB());
                drawVertex(consumer, model, -sz,  sz, 0, 1, c2.getRGB());
                drawVertex(consumer, model,  sz,  sz, 1, 1, c1.getRGB());
                drawVertex(consumer, model,  sz, -sz, 1, 0, c2.getRGB());

                stack.pop();
            }
        }

    };
    private void drawVertex(VertexConsumer vc, Matrix4f matrix, float x, float z, float u, float v, int color) {
        vc.vertex(matrix, x, 0, z)
                .color(color)
                .texture(u, v)
                .overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(15728880)
                .normal(0, 1, 0);
    }

    private double raycastDownY(Vec3d origin, double max) {
        for (double dy = 0; dy <= max; dy += 0.05) {
            Vec3d p = origin.subtract(0, dy, 0);
            BlockPos pos = BlockPos.ofFloored(p);
            BlockState state = mc.world.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(mc.world, pos);
            if (!shape.isEmpty()) { Box box = shape.getBoundingBox(); return pos.getY() + box.maxY; }
        }
        return Double.NaN;
    }
}