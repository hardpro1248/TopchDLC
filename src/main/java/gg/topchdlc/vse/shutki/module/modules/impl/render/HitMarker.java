package gg.topchdlc.vse.shutki.module.modules.impl.render;

import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.events.list.EventAttack;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.ColorUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class HitMarker extends Module {
    public static final HitMarker INSTANCE = new HitMarker();

    public final SliderSetting count = sliderSetting("Количество", 2, 1, 10).increment(1);
    public final SliderSetting lifetime = sliderSetting("Время жизни (мс)", 800, 200, 3000).increment(50);
    public final SliderSetting scale = sliderSetting("Размер", 0.35f, 0.1f, 1.5f);
    public final CheckBox themeColor = checkbox("Цвет от темы", true);

    private static final Identifier CROSS_TEXTURE = Identifier.of("topchdlc", "images/world/pt/cross.png");
    private static final Identifier BLOOM_TEXTURE = Identifier.of("topchdlc", "images/world/pt/bloom.png");

    private final List<MarkerInstance> markers = new ArrayList<>();
    private final Random random = new Random();

    private BufferAllocator allocator;
    private VertexConsumerProvider.Immediate imm;

    private HitMarker() {
        super("HitMarker", Category.RENDER, "Отображает хитмаркеры в 3D мире при ударе");
    }

    private static class MarkerInstance {
        final double x, y, z;
        final long spawnTime;
        final long maxAge;
        final float size;
        final int color;

        MarkerInstance(double x, double y, double z, long maxAge, float size, int color) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.spawnTime = System.currentTimeMillis();
            this.maxAge = maxAge;
            this.size = size;
            this.color = color;
        }

        float getAlpha() {
            long age = System.currentTimeMillis() - spawnTime;
            if (age >= maxAge) return 0f;
            return 1.0f - ((float) age / maxAge);
        }

        boolean isDead() {
            return System.currentTimeMillis() - spawnTime >= maxAge;
        }
    }

    @Override
    public void toggle() {
        super.toggle();
        markers.clear();
        if (!isEnabled() && allocator != null) {
            allocator.close();
            allocator = null;
            imm = null;
        }
    }

    EventBus<Event> bus = event -> {
        if (event instanceof EventAttack e) onAttack(e);
        else if (event instanceof EventGameTick) onTick();
        else if (event instanceof Event3D e) onRender3D(e);
    };

    private void onAttack(EventAttack event) {
        if (event.target == null || mc.player == null) return;

        Entity target = event.target;
        Vec3d playerEye = mc.player.getEyePos();
        Vec3d lookDir = mc.player.getRotationVec(1.0f);
        Box bb = target.getBoundingBox();

        Optional<Vec3d> rayHit = bb.raycast(playerEye, playerEye.add(lookDir.multiply(8.0)));

        Vec3d baseHitPos;
        if (rayHit.isPresent()) {
            baseHitPos = rayHit.get();
        } else {
            double distToTarget = playerEye.distanceTo(bb.getCenter());
            Vec3d projectedPos = playerEye.add(lookDir.multiply(distToTarget));

            double hitX = MathHelper.clamp(projectedPos.x, bb.minX, bb.maxX);
            double hitY = MathHelper.clamp(projectedPos.y, bb.minY, bb.maxY);
            double hitZ = MathHelper.clamp(projectedPos.z, bb.minZ, bb.maxZ) ;
            baseHitPos = new Vec3d(hitX, hitY, hitZ);
        }

        Vec3d viewDir = baseHitPos.subtract(playerEye).normalize();
        Vec3d right = viewDir.crossProduct(new Vec3d(0, 1, 0));
        if (right.lengthSquared() < 0.001) {
            right = new Vec3d(1, 0, 0);
        } else {
            right = right.normalize();
        }
        Vec3d up = right.crossProduct(viewDir).normalize();

        int spawnCount = count.getInt();
        long life = (long) lifetime.get();
        float baseScale = (float) scale.get();

        for (int i = 0; i < spawnCount; i++) {
            Vec3d markerPos;

            if (i == 0) {
                markerPos = baseHitPos;
            } else {
                double spreadRight = (random.nextDouble() - 0.5) * 0.9;
                double spreadUp = (random.nextDouble() - 0.5) * 0.8;

                markerPos = baseHitPos.add(right.multiply(spreadRight)).add(up.multiply(spreadUp));
            }

            int color = themeColor.get()
                    ? ClientSettings.INSTANCE.getColor(i * 40).getRGB()
                    : 0xFFFFFFFF;

            markers.add(new MarkerInstance(
                    markerPos.x,
                    markerPos.y,
                    markerPos.z,
                    life,
                    baseScale,
                    color
            ));
        }
    }

    private void onTick() {
        markers.removeIf(MarkerInstance::isDead);
    }

    private void onRender3D(Event3D event) {
        if (markers.isEmpty() || mc.player == null) return;

        if (allocator == null) {
            allocator = new BufferAllocator(1 << 20);
            imm = VertexConsumerProvider.immediate(allocator);
        }

        MatrixStack stack = event.stack;
        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();

        RenderLayer bloomLayer = ClientPipelines.PARTICLES.apply(BLOOM_TEXTURE);
        VertexConsumer bloomBuf = imm.getBuffer(bloomLayer);

        for (MarkerInstance marker : markers) {
            float alpha = marker.getAlpha();
            if (alpha <= 0.001f) continue;

            stack.push();
            stack.translate(marker.x - cam.x, marker.y - cam.y, marker.z - cam.z);
            stack.multiply(mc.gameRenderer.getCamera().getRotation());

            Matrix4f mat = stack.peek().getPositionMatrix();
            float bloomSize = marker.size * 1.6f;
            int bloomColor = ColorUtility.replAlpha(marker.color, (int) (alpha * 0.25f * 255));

            emitQuad(bloomBuf, mat, bloomSize, bloomColor);
            stack.pop();
        }
        imm.draw(bloomLayer);

        RenderLayer crossLayer = ClientPipelines.PARTICLES.apply(CROSS_TEXTURE);
        VertexConsumer crossBuf = imm.getBuffer(crossLayer);

        for (MarkerInstance marker : markers) {
            float alpha = marker.getAlpha();
            if (alpha <= 0.001f) continue;

            stack.push();
            stack.translate(marker.x - cam.x, marker.y - cam.y, marker.z - cam.z);
            stack.multiply(mc.gameRenderer.getCamera().getRotation());

            Matrix4f mat = stack.peek().getPositionMatrix();
            int crossColor = ColorUtility.replAlpha(marker.color, (int) (alpha * 255));

            emitQuad(crossBuf, mat, marker.size, crossColor);
            stack.pop();
        }
        imm.draw(crossLayer);
    }

    private static void emitQuad(VertexConsumer buf, Matrix4f mat, float size, int argb) {
        float h = size * 0.5f;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int a = (argb >>> 24);

        buf.vertex(mat, -h, -h, 0).texture(0, 1).color(r, g, b, a);
        buf.vertex(mat,  h, -h, 0).texture(1, 1).color(r, g, b, a);
        buf.vertex(mat,  h,  h, 0).texture(1, 0).color(r, g, b, a);
        buf.vertex(mat, -h,  h, 0).texture(0, 0).color(r, g, b, a);
    }
}