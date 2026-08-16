package gg.topchdlc.vse.shutki.module.modules.impl.render.worldp;

import gg.topchdlc.api.render.system.ClientPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.events.list.EventAttack;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.MathUtility;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
/**
 * Create by daun kvass
 */
///  хихи стеклышки кста идею подкинул ии и начало далл ии но я немножко до говнокодил и получилось прикольно
public class Shatter extends Module {
    public static final Shatter INSTANCE = new Shatter();

    public enum ShardShape {
        Triangle("Треугольник"),
        Quad("Четырёхугольник"),
        Mixed("Микс");

        private final String name;
        ShardShape(String n) { this.name = n; }
        @Override public String toString() { return name; }
    }

    private final Group spawnGroup = group("Спавн");
    private final SliderSetting shardCount =
            spawnGroup.sliderSetting("Кол-во", 12, 4, 30);
    private final EnumSetting<ShardShape> shardShape =
            spawnGroup.enumSetting("Форма", ShardShape.Mixed);
    private final SliderSetting explosionForce =
            spawnGroup.sliderSetting("Сила", 2.0f, 0.5f, 5.0f).increment(0.1f);
    private final SliderSetting shardSize =
            spawnGroup.sliderSetting("Размер", 0.15f, 0.03f, 0.4f).increment(0.01f);
    private final SliderSetting sizeVariation =
            spawnGroup.sliderSetting("Разброс размера", 0.5f, 0.0f, 1.0f).increment(0.05f);

    private final Group physicsGroup = group("Физика");
    private final SliderSetting lifespan =
            physicsGroup.sliderSetting("Время жизни", 2500, 500, 6000);
    private final SliderSetting gravity =
            physicsGroup.sliderSetting("Гравитация", 1.5f, 0.0f, 4.0f).increment(0.1f);
    private final SliderSetting rotationSpeed =
            physicsGroup.sliderSetting("Скорость вращения", 2.0f, 0.5f, 5.0f).increment(0.1f);
    private final SliderSetting friction =
            physicsGroup.sliderSetting("Трение", 0.98f, 0.9f, 1.0f).increment(0.005f);
    private final CheckBox bounce =
            physicsGroup.checkbox("Отскок", true);

    private final Group visualGroup = group("Визуал");
    private final SliderSetting bloomScale =
            visualGroup.sliderSetting("Bloom размер", 2.0f, 1.0f, 5.0f).increment(0.1f);
    private final SliderSetting bloomBright =
            visualGroup.sliderSetting("Bloom яркость", 0.5f, 0.1f, 1.0f).increment(0.05f);
    private final SliderSetting opacity =
            visualGroup.sliderSetting("Непрозрачность", 0.7f, 0.1f, 1.0f).increment(0.05f);
    private final CheckBox reflections =
            visualGroup.checkbox("Переливание", true);

    private static final Identifier BLOOM_TEXTURE =
            Identifier.of("topchdlc", "images/world/bloom.png");
    private static RenderLayer BLOOM_LAYER;

    private RenderLayer getBloomLayer() {
        if (BLOOM_LAYER == null)
            BLOOM_LAYER = ClientPipelines.NIMB.apply(BLOOM_TEXTURE);
        return BLOOM_LAYER;
    }

    private final List<Shard> shards = new ArrayList<>();

    private Shatter() {
        super("Shatter", Category.RENDER, "Осколки стекла при ударе");
    }

    EventBus<Event> bus = event -> {
        if (event instanceof EventAttack a) onAttack(a);
        else if (event instanceof Event3D e) onRender3D(e);
    };

    private void onAttack(EventAttack attack) {
        if (mc.player == null) return;
        if (!(attack.target instanceof LivingEntity target)) return;

        Vec3d hitPos = target.getEntityPos().add(0, target.getHeight() / 2, 0);
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d hitDir = hitPos.subtract(playerPos).normalize();

        int count = shardCount.getInt();
        float force = explosionForce.get();
        float baseSize = shardSize.get();
        float sizeVar = sizeVariation.get();
        long now = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            float size = baseSize * MathUtility.randomValue(1f - sizeVar, 1f + sizeVar);

            Vec3d spawnPos = hitPos.add(
                    MathHelper.nextDouble(Random.create(), -0.3, 0.3),
                    MathHelper.nextDouble(Random.create(), -0.3, 0.3),
                    MathHelper.nextDouble(Random.create(), -0.3, 0.3)
            );

            double theta = MathUtility.randomValue(0, (float)(Math.PI * 2));
            double phi = Math.acos(MathUtility.randomValue(-1f, 1f));
            double spd = force * MathUtility.randomValue(0.3f, 1.0f);

            Vec3d vel = new Vec3d(
                    Math.sin(phi) * Math.cos(theta) * spd + hitDir.x * force * 0.3,
                    Math.sin(phi) * Math.sin(theta) * spd * 0.6 + force * 0.2,
                    Math.cos(phi) * spd + hitDir.z * force * 0.3
            );

            float rotX = MathUtility.randomValue(-180, 180);
            float rotY = MathUtility.randomValue(-180, 180);
            float rotZ = MathUtility.randomValue(-180, 180);
            float rotSpeedX = MathUtility.randomValue(-1, 1) * rotationSpeed.get() * 100;
            float rotSpeedY = MathUtility.randomValue(-1, 1) * rotationSpeed.get() * 100;
            float rotSpeedZ = MathUtility.randomValue(-1, 1) * rotationSpeed.get() * 100;
            boolean isTriangle;
            if (shardShape.get() == ShardShape.Triangle) isTriangle = true;
            else if (shardShape.get() == ShardShape.Quad) isTriangle = false;
            else isTriangle = Math.random() > 0.5;
            float[][] verts = isTriangle
                    ? generateTriangleVerts(size)
                    : generateQuadVerts(size);
            int color = ClientSettings.INSTANCE.getColor(i * 40).getRGB();
            shards.add(new Shard(
                    spawnPos, vel, verts, isTriangle,
                    rotX, rotY, rotZ, rotSpeedX, rotSpeedY, rotSpeedZ,
                    size, color, now
            ));
        }
    }

    private float[][] generateTriangleVerts(float size) {
        float s = size;
        return new float[][]{
                {MathUtility.randomValue(-s, -s * 0.2f), MathUtility.randomValue(s * 0.3f, s), 0},
                {MathUtility.randomValue(-s, -s * 0.1f), MathUtility.randomValue(-s, -s * 0.2f), 0},
                {MathUtility.randomValue(s * 0.2f, s), MathUtility.randomValue(-s * 0.5f, s * 0.5f), 0},
        };
    }

    private float[][] generateQuadVerts(float size) {
        float s = size;
        return new float[][]{
                {MathUtility.randomValue(-s, -s * 0.2f), MathUtility.randomValue(s * 0.2f, s), 0},
                {MathUtility.randomValue(-s, -s * 0.1f), MathUtility.randomValue(-s, -s * 0.2f), 0},
                {MathUtility.randomValue(s * 0.1f, s), MathUtility.randomValue(-s, -s * 0.1f), 0},
                {MathUtility.randomValue(s * 0.2f, s), MathUtility.randomValue(s * 0.1f, s), 0},
        };
    }

    private void onRender3D(Event3D event) {
        if (shards.isEmpty()) return;

        long now = System.currentTimeMillis();
        int life = lifespan.getInt();
        float grav = gravity.get();
        float fric = friction.get();
        boolean doBounce = bounce.get();
        shards.removeIf(s -> now - s.createdAt > life);

        for (Shard shard : shards) {
            float dt = 0.016f;

            shard.vy -= grav * dt * 0.5f;
            shard.vx *= fric;
            shard.vy *= fric;
            shard.vz *= fric;
            shard.x += shard.vx * dt;
            shard.y += shard.vy * dt;
            shard.z += shard.vz * dt;
            if (doBounce && shard.y < shard.spawnY - 1.0) {
                shard.vy = Math.abs(shard.vy) * 0.4f;
                shard.y = (float)(shard.spawnY - 1.0);
                shard.rotSpeedX *= 0.5f;
                shard.rotSpeedY *= 0.5f;
                shard.rotSpeedZ *= 0.5f;
            }
            float elapsed = (now - shard.createdAt) / 1000f;
            shard.currentRotX = shard.baseRotX + shard.rotSpeedX * elapsed;
            shard.currentRotY = shard.baseRotY + shard.rotSpeedY * elapsed;
            shard.currentRotZ = shard.baseRotZ + shard.rotSpeedZ * elapsed;
        }

        if (shards.isEmpty()) return;

        MatrixStack matrix = event.stack;
        BufferAllocator allocator = new BufferAllocator(524288);
        VertexConsumerProvider.Immediate imm = VertexConsumerProvider.immediate(allocator);

        try {
            VertexConsumer buf = imm.getBuffer(getBloomLayer());

            for (Shard shard : shards) {
                renderShard(matrix, buf, shard, now);
            }

            imm.draw();
        } finally {
            allocator.close();
        }
    }

    private void renderShard(MatrixStack matrix, VertexConsumer buf,
                             Shard shard, long now) {
        long elapsed = now - shard.createdAt;
        int life = lifespan.getInt();
        float progress = elapsed / (float) life;
        float fadeIn = Math.min(1f, elapsed / 100f);
        float fadeOut = 1f - Math.max(0f, (progress - 0.5f) / 0.5f);
        float alpha = fadeIn * fadeOut * fadeOut * opacity.get();
        if (alpha <= 0.01f) return;
        int color = shard.color;
        if (reflections.get()) {
            float hueShift = (float) Math.sin(elapsed * 0.003 + shard.baseRotX) * 0.1f;
            Color c = new Color(color);
            float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
            color = Color.HSBtoRGB(hsb[0] + hueShift, hsb[1], hsb[2]);
        }

        int cr = (color >> 16) & 0xFF;
        int cg = (color >> 8) & 0xFF;
        int cb = color & 0xFF;
        matrix.push();
        Client.RENDERER.setupOrientationMatrix(matrix, shard.x, shard.y, shard.z);
        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(shard.currentRotY));
        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(shard.currentRotX));
        matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(shard.currentRotZ));
        MatrixStack.Entry entry = matrix.peek();
        Matrix4f mat = entry.getPositionMatrix();
        {
            matrix.push();
            Quaternionf invRot = new Quaternionf();
            invRot.rotateZ((float) Math.toRadians(-shard.currentRotZ));
            invRot.rotateX((float) Math.toRadians(-shard.currentRotX));
            invRot.rotateY((float) Math.toRadians(-shard.currentRotY));
            matrix.multiply(invRot);
            matrix.multiply(mc.gameRenderer.getCamera().getRotation());
            MatrixStack.Entry bloomEntry = matrix.peek();
            Matrix4f bloomMat = bloomEntry.getPositionMatrix();
            float bSize = shard.size * bloomScale.get();
            int bAlpha = Math.min(255, Math.max(1, (int)(alpha * bloomBright.get() * 255)));
            for (int i = 0; i < 2; i++) {
                float ls = bSize * (0.5f + i * 0.6f);
                int la = Math.max(1, bAlpha / (1 + i));

                buf.vertex(bloomMat, -ls, -ls, 0).color(cr, cg, cb, la)
                        .texture(0, 1).overlay(OverlayTexture.DEFAULT_UV)
                        .light(15728880).normal(0, 0, 1);
                buf.vertex(bloomMat, ls, -ls, 0).color(cr, cg, cb, la)
                        .texture(1, 1).overlay(OverlayTexture.DEFAULT_UV)
                        .light(15728880).normal(0, 0, 1);
                buf.vertex(bloomMat, ls, ls, 0).color(cr, cg, cb, la)
                        .texture(1, 0).overlay(OverlayTexture.DEFAULT_UV)
                        .light(15728880).normal(0, 0, 1);
                buf.vertex(bloomMat, -ls, ls, 0).color(cr, cg, cb, la)
                        .texture(0, 0).overlay(OverlayTexture.DEFAULT_UV)
                        .light(15728880).normal(0, 0, 1);
            }

            matrix.pop();
        }

        int faceAlpha = Math.min(255, (int)(alpha * 255));
        float[][] v = shard.verts;

        if (shard.isTriangle) {
            buf.vertex(mat, v[0][0], v[0][1], v[0][2]).color(cr, cg, cb, faceAlpha)
                    .texture(0.5f, 0.5f).overlay(OverlayTexture.DEFAULT_UV)
                    .light(15728880).normal(0, 0, 1);
            buf.vertex(mat, v[1][0], v[1][1], v[1][2]).color(cr, cg, cb, faceAlpha)
                    .texture(0.5f, 0.5f).overlay(OverlayTexture.DEFAULT_UV)
                    .light(15728880).normal(0, 0, 1);
            buf.vertex(mat, v[2][0], v[2][1], v[2][2]).color(cr, cg, cb, faceAlpha)
                    .texture(0.5f, 0.5f).overlay(OverlayTexture.DEFAULT_UV)
                    .light(15728880).normal(0, 0, 1);
            buf.vertex(mat, v[2][0], v[2][1], v[2][2]).color(cr, cg, cb, faceAlpha)
                    .texture(0.5f, 0.5f).overlay(OverlayTexture.DEFAULT_UV)
                    .light(15728880).normal(0, 0, 1);
            buf.vertex(mat, v[2][0], v[2][1], v[2][2]).color(cr, cg, cb, faceAlpha)
                    .texture(0.5f, 0.5f).overlay(OverlayTexture.DEFAULT_UV)
                    .light(15728880).normal(0, 0, -1);
            buf.vertex(mat, v[1][0], v[1][1], v[1][2]).color(cr, cg, cb, faceAlpha)
                    .texture(0.5f, 0.5f).overlay(OverlayTexture.DEFAULT_UV)
                    .light(15728880).normal(0, 0, -1);
            buf.vertex(mat, v[0][0], v[0][1], v[0][2]).color(cr, cg, cb, faceAlpha)
                    .texture(0.5f, 0.5f).overlay(OverlayTexture.DEFAULT_UV)
                    .light(15728880).normal(0, 0, -1);
            buf.vertex(mat, v[0][0], v[0][1], v[0][2]).color(cr, cg, cb, faceAlpha)
                    .texture(0.5f, 0.5f).overlay(OverlayTexture.DEFAULT_UV)
                    .light(15728880).normal(0, 0, -1);
        } else {
            buf.vertex(mat, v[0][0], v[0][1], v[0][2]).color(cr, cg, cb, faceAlpha)
                    .texture(0.5f, 0.5f).overlay(OverlayTexture.DEFAULT_UV)
                    .light(15728880).normal(0, 0, 1);
            buf.vertex(mat, v[1][0], v[1][1], v[1][2]).color(cr, cg, cb, faceAlpha)
                    .texture(0.5f, 0.5f).overlay(OverlayTexture.DEFAULT_UV)
                    .light(15728880).normal(0, 0, 1);
            buf.vertex(mat, v[2][0], v[2][1], v[2][2]).color(cr, cg, cb, faceAlpha)
                    .texture(0.5f, 0.5f).overlay(OverlayTexture.DEFAULT_UV)
                    .light(15728880).normal(0, 0, 1);
            buf.vertex(mat, v[3][0], v[3][1], v[3][2]).color(cr, cg, cb, faceAlpha)
                    .texture(0.5f, 0.5f).overlay(OverlayTexture.DEFAULT_UV)
                    .light(15728880).normal(0, 0, 1);

            buf.vertex(mat, v[3][0], v[3][1], v[3][2]).color(cr, cg, cb, faceAlpha)
                    .texture(0.5f, 0.5f).overlay(OverlayTexture.DEFAULT_UV)
                    .light(15728880).normal(0, 0, -1);
            buf.vertex(mat, v[2][0], v[2][1], v[2][2]).color(cr, cg, cb, faceAlpha)
                    .texture(0.5f, 0.5f).overlay(OverlayTexture.DEFAULT_UV)
                    .light(15728880).normal(0, 0, -1);
            buf.vertex(mat, v[1][0], v[1][1], v[1][2]).color(cr, cg, cb, faceAlpha)
                    .texture(0.5f, 0.5f).overlay(OverlayTexture.DEFAULT_UV)
                    .light(15728880).normal(0, 0, -1);
            buf.vertex(mat, v[0][0], v[0][1], v[0][2]).color(cr, cg, cb, faceAlpha)
                    .texture(0.5f, 0.5f).overlay(OverlayTexture.DEFAULT_UV)
                    .light(15728880).normal(0, 0, -1);
        }

        {
            Quaternionf rot = new Quaternionf();
            rot.rotateY((float) Math.toRadians(shard.currentRotY));
            rot.rotateX((float) Math.toRadians(shard.currentRotX));
            rot.rotateZ((float) Math.toRadians(shard.currentRotZ));

            float dotSize = shard.size * 0.2f;
            int dotAlpha = Math.min(255, (int)(alpha * bloomBright.get() * 200));

            if (dotAlpha > 0) {
                for (float[] vert : v) {
                    Vector3f rv = new Vector3f(vert[0], vert[1], vert[2]);
                    rot.transform(rv);
                    matrix.push();
                    Quaternionf invRot = new Quaternionf();
                    invRot.rotateZ((float) Math.toRadians(-shard.currentRotZ));
                    invRot.rotateX((float) Math.toRadians(-shard.currentRotX));
                    invRot.rotateY((float) Math.toRadians(-shard.currentRotY));
                    matrix.translate(vert[0], vert[1], vert[2]);
                    matrix.multiply(invRot);
                    matrix.multiply(mc.gameRenderer.getCamera().getRotation());
                    MatrixStack.Entry dotEntry = matrix.peek();
                    Matrix4f dotMat = dotEntry.getPositionMatrix();
                    buf.vertex(dotMat, -dotSize, -dotSize, 0).color(cr, cg, cb, dotAlpha)
                            .texture(0, 1).overlay(OverlayTexture.DEFAULT_UV)
                            .light(15728880).normal(0, 0, 1);
                    buf.vertex(dotMat, dotSize, -dotSize, 0).color(cr, cg, cb, dotAlpha)
                            .texture(1, 1).overlay(OverlayTexture.DEFAULT_UV)
                            .light(15728880).normal(0, 0, 1);
                    buf.vertex(dotMat, dotSize, dotSize, 0).color(cr, cg, cb, dotAlpha)
                            .texture(1, 0).overlay(OverlayTexture.DEFAULT_UV)
                            .light(15728880).normal(0, 0, 1);
                    buf.vertex(dotMat, -dotSize, dotSize, 0).color(cr, cg, cb, dotAlpha)
                            .texture(0, 0).overlay(OverlayTexture.DEFAULT_UV)
                            .light(15728880).normal(0, 0, 1);

                    matrix.pop();
                }
            }
        }

        matrix.pop();
    }

    @Override
    public void toggle() {
        super.toggle();
        shards.clear();
    }

    private static class Shard {
        float x, y, z;
        float vx, vy, vz;
        final double spawnY;
        final float[][] verts;
        final boolean isTriangle;
        final float baseRotX, baseRotY, baseRotZ;
        float rotSpeedX, rotSpeedY, rotSpeedZ;
        float currentRotX, currentRotY, currentRotZ;
        final float size;
        final int color;
        final long createdAt;

        Shard(Vec3d pos, Vec3d vel, float[][] verts, boolean isTriangle,
              float rotX, float rotY, float rotZ,
              float rotSpeedX, float rotSpeedY, float rotSpeedZ,
              float size, int color, long created) {
            this.x = (float) pos.x;
            this.y = (float) pos.y;
            this.z = (float) pos.z;
            this.vx = (float) vel.x;
            this.vy = (float) vel.y;
            this.vz = (float) vel.z;
            this.spawnY = pos.y;
            this.verts = verts;
            this.isTriangle = isTriangle;
            this.baseRotX = rotX;
            this.baseRotY = rotY;
            this.baseRotZ = rotZ;
            this.rotSpeedX = rotSpeedX;
            this.rotSpeedY = rotSpeedY;
            this.rotSpeedZ = rotSpeedZ;
            this.currentRotX = rotX;
            this.currentRotY = rotY;
            this.currentRotZ = rotZ;
            this.size = size;
            this.color = color;
            this.createdAt = created;
        }
    }
}