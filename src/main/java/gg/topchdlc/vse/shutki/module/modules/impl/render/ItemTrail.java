package gg.topchdlc.vse.shutki.module.modules.impl.render;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.math.ColorUtility;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ItemTrail extends Module {
    public static final ItemTrail INSTANCE = new ItemTrail();
    private static final Identifier GLOW_TEXTURE = Identifier.of("topchdlc", "images/world/bloom.png");

    private final Group settings = group("Настройки");
    private final CheckBox items = settings.checkbox("Предметы", true);
    private final CheckBox projectiles = settings.checkbox("Снаряды", true);

    private final Group renderGroup = group("Визуал");
    private final ColorSetting color = renderGroup.colorSetting("Цвет", new Color(0x63FFFF));
    private final SliderSetting thickness = renderGroup.sliderSetting("Толщина", 0.15f, 0.01f, 0.5f);

    private BufferAllocator allocator;
    private VertexConsumerProvider.Immediate imm;

    private ItemTrail() {
        super("ItemTrail", Category.RENDER, "Дорожка за предметом");
    }

    @Override
    protected void onDisable() {
        if (allocator != null) {
            allocator.close();
            allocator = null;
        }
    }

    public final EventBus<Event> bus = event -> {
        if (event instanceof Event3D e) onRender3D(e);
    };

    private void onRender3D(Event3D event) {
        if (mc.world == null || mc.player == null) return;

        if (allocator == null) {
            allocator = new BufferAllocator(1 << 20);
            imm = VertexConsumerProvider.immediate(allocator);
        }

        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        Quaternionf camRot = mc.gameRenderer.getCamera().getRotation();

        RenderLayer layer = ClientPipelines.NIMB.apply(GLOW_TEXTURE);
        VertexConsumer buf = imm.getBuffer(layer);

        GlStateManager._depthMask(false);
        GlStateManager._disableCull();
        
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GlConst.GL_SRC_ALPHA, GlConst.GL_ONE, GlConst.GL_ZERO, GlConst.GL_ONE);

        MatrixStack stack = event.stack;
        stack.push();
        stack.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f mat = stack.peek().getPositionMatrix();

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;

            if (entity instanceof FireworkRocketEntity) continue;

            if (isStationary(entity)) continue;

            boolean isItem = entity instanceof ItemEntity && items.get();
            boolean isProj = entity instanceof ProjectileEntity && projectiles.get();

            if (isItem || isProj) {
                renderStableBeam(entity, buf, mat, camRot, tickDelta);
            }
        }

        stack.pop();
        imm.draw();

        GlStateManager._blendFuncSeparate(GlConst.GL_ONE, GlConst.GL_ZERO, GlConst.GL_ONE, GlConst.GL_ZERO);
        GlStateManager._disableBlend();
        
        GlStateManager._enableCull();
        GlStateManager._depthMask(true);
    }

    private boolean isStationary(Entity e) {
        if (e instanceof PersistentProjectileEntity ppe && ppe.isOnGround()) return true;
        return e.isOnGround() && e.getVelocity().lengthSquared() < 0.0001;
    }

    private void renderStableBeam(Entity entity, VertexConsumer buf, Matrix4f mat, Quaternionf camRot, float delta) {
        List<Vec3d> points = simulateStable(entity, delta);
        if (points.size() < 2) return;

        int rgb = color.get().getRGB();
        float baseWidth = thickness.get();
        drawGlowPoint(buf, mat, points.get(0), camRot, baseWidth * 2.5f, rgb, 0.7f);
        float[] angles = {0, 60, 120};
        for (float a : angles) drawStrip(buf, mat, points, baseWidth * 3.8f, rgb, 0.14f, a);
        for (float a : angles) drawStrip(buf, mat, points, baseWidth * 1.6f, rgb, 0.38f, a);
        for (float a : angles) drawStrip(buf, mat, points, baseWidth * 0.5f, 0xFFFFFF, 0.75f, a);
        Vec3d lastPos = points.get(points.size() - 1);
        drawGlowPoint(buf, mat, lastPos, camRot, baseWidth * 5.0f, rgb, 0.6f);
        drawGlowPoint(buf, mat, lastPos, camRot, baseWidth * 2.0f, 0xFFFFFF, 1.0f);
    }

    private void drawGlowPoint(VertexConsumer buf, Matrix4f mat, Vec3d pos, Quaternionf camRot, float size, int rgb, float aMult) {
        Matrix4f hMat = new Matrix4f(mat).translate((float)pos.x, (float)pos.y, (float)pos.z).rotate(camRot);
        float h = size / 2f;
        int col = ColorUtility.replAlpha(rgb, (int)(color.get().getAlpha() * aMult));
        buf.vertex(hMat, -h, -h, 0).texture(0, 0).color(col);
        buf.vertex(hMat, h, -h, 0).texture(1, 0).color(col);
        buf.vertex(hMat, h, h, 0).texture(1, 1).color(col);
        buf.vertex(hMat, -h, h, 0).texture(0, 1).color(col);
    }

    private void drawStrip(VertexConsumer buf, Matrix4f mat, List<Vec3d> points, float width, int rgb, float alpha, float angle) {
        int count = points.size();
        for (int i = 0; i < count - 1; i++) {
            Vec3d p1 = points.get(i);
            Vec3d p2 = points.get(i + 1);

            Vec3d dir = p2.subtract(p1).normalize();
            Vec3d up = new Vec3d(0, 1, 0);
            if (Math.abs(dir.y) > 0.99) up = new Vec3d(1, 0, 0);
            Vec3d side = dir.crossProduct(up).normalize();

            Vector3f sJoml = new Vector3f((float)side.x, (float)side.y, (float)side.z);
            sJoml.rotateAxis((float)Math.toRadians(angle), (float)dir.x, (float)dir.y, (float)dir.z);
            side = new Vec3d(sJoml.x(), sJoml.y(), sJoml.z());

            float t1 = (float) i / (count - 1);
            float t2 = (float) (i + 1) / (count - 1);

            float w1 = width * (1.0f - t1 * 0.2f);
            float w2 = width * (1.0f - t2 * 0.2f);

            int col1 = ColorUtility.replAlpha(rgb, (int) (255 * (1f - t1 * 0.2f) * alpha));
            int col2 = ColorUtility.replAlpha(rgb, (int) (255 * (1f - t2 * 0.2f) * alpha));

            buf.vertex(mat, (float)(p1.x + side.x * w1), (float)(p1.y + side.y * w1), (float)(p1.z + side.z * w1)).texture(0.5f, 0).color(col1);
            buf.vertex(mat, (float)(p1.x - side.x * w1), (float)(p1.y - side.y * w1), (float)(p1.z - side.z * w1)).texture(0.5f, 1).color(col1);
            buf.vertex(mat, (float)(p2.x - side.x * w2), (float)(p2.y - side.y * w2), (float)(p2.z - side.z * w2)).texture(0.5f, 1).color(col2);
            buf.vertex(mat, (float)(p2.x + side.x * w2), (float)(p2.y + side.y * w2), (float)(p2.z + side.z * w2)).texture(0.5f, 0).color(col2);
        }
    }

    private List<Vec3d> simulateStable(Entity entity, float delta) {
        List<Vec3d> pts = new ArrayList<>();
        Vec3d pos = entity.getLerpedPos(delta);
        Vec3d vel = entity.getVelocity();

        float gravity = (entity instanceof ItemEntity) ? 0.04f :
                (entity instanceof PotionEntity || entity instanceof ExperienceBottleEntity) ? 0.05f : 0.03f;
        float drag = 0.99f;

        double px = pos.x, py = pos.y, pz = pos.z;
        double vx = vel.x, vy = vel.y, vz = vel.z;
        pts.add(new Vec3d(px, py, pz));

        int steps = 300;
        for (int i = 0; i < steps; i++) {
            vx *= drag; vy *= drag; vz *= drag;
            vy -= gravity;

            Vec3d next = new Vec3d(px + vx, py + vy, pz + vz);
            RaycastContext ctx = new RaycastContext(new Vec3d(px, py, pz), next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity);
            var hit = mc.world.raycast(ctx);

            if (hit.getType() != HitResult.Type.MISS) {
                pts.add(hit.getPos());
                break;
            }

            pts.add(next);
            px = next.x; py = next.y; pz = next.z;
            if (py < mc.world.getBottomY() - 5) break;
        }
        return pts;
    }
}