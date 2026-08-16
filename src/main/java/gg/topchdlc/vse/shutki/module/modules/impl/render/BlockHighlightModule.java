package gg.topchdlc.vse.shutki.module.modules.impl.render;

import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.render.BlockHighlightRenderer;

import java.awt.*;

public class BlockHighlightModule extends Module {
    public static final BlockHighlightModule INSTANCE = new BlockHighlightModule();
    private net.minecraft.util.shape.VoxelShape currentShape;

    public enum Mode {
        Default("Обычный"),
        Grow("Рост"),
        Slide("Скольжение");

        private final String name;
        Mode(String n) { this.name = n; }
        @Override public String toString() { return name; }
    }

    private static final float LINE_WIDTH = 1.5f;
    private static final float EXPAND = 0.006f;
    private static final float SWEEP_SPEED = 0.1f;
    private static final float SWEEP_WIDTH = 0.19f;
    private static final float SWEEP_ALPHA = 0.45f;

    private final CheckBox useClientColor = checkbox("Цвет клиента", true);
    private final ColorSetting outlineColor = colorSetting("Обводка", new Color(255, 255, 255, 200)).visible(()->!useClientColor.get());
    private final ColorSetting fillColor = colorSetting("Заливка", new Color(255, 255, 255, 40)).visible(()->!useClientColor.get());
    private final EnumSetting<Mode> mode = enumSetting("Режим", Mode.Default);
    private final SliderSetting speed = sliderSetting("Скорость", 8f, 1f, 67f).increment(0.5f);
    private final CheckBox useShader = checkbox("Шейдерный эффект", false);

    private double animMinX, animMinY, animMinZ;
    private double animMaxX, animMaxY, animMaxZ;
    private double targetMinX, targetMinY, targetMinZ;
    private double targetMaxX, targetMaxY, targetMaxZ;
    private boolean initialized = false;
    private boolean hasTarget = false;
    private float appearProgress = 0f;
    private BlockPos lastPos = null;
    private long lastRenderTime = System.currentTimeMillis();
    private float sweepProgress = 0f;

    private BlockHighlightModule() {
        super("BlockHighlight", Category.RENDER, "Красивая обводка блоков");
    }

    @Override
    protected void onEnable() {
        BlockHighlightRenderer.getInstance().setEnabled(useShader.get());
        updateRendererSettings();
    }

    @Override
    protected void onDisable() {
        BlockHighlightRenderer.getInstance().setEnabled(false);
    }

    EventBus<Event> bus = event -> {
        if (event instanceof Event3D e) {
            updateRendererSettings();
            onRender3D(e);
        }
    };

    private void updateRendererSettings() {
        BlockHighlightRenderer renderer = BlockHighlightRenderer.getInstance();
        boolean needsShader = useShader.get() && isEnabled();
        renderer.setEnabled(needsShader);
        if (needsShader) {
            renderer.setIntensity(0.8f);
            renderer.setRadius(0.35f);
            renderer.setSpeed(1.7f);
            renderer.setFlameHeight(0.6f);
            renderer.setTrailFade(0.8f);
            renderer.setTrailSoftness(1.35f);
            renderer.setTrailBlurRadius(1.55f);
            renderer.setGlowFade(0.68f);
            renderer.setGlowSoftness(1.3f);
            renderer.setGlowBlurRadius(3);
            renderer.setSmoke(0.6f);
        }
    }

    private void onRender3D(Event3D event) {
        if (mc.world == null || mc.player == null) return;

        BlockPos targetPos = null;
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            targetPos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
        }

        if (targetPos == null) {
            hasTarget = false;
        } else {
            hasTarget = true;
            if (lastPos == null || !lastPos.equals(targetPos)) {
                lastPos = targetPos;
                var state = mc.world.getBlockState(targetPos);
                currentShape = state.getOutlineShape(mc.world, targetPos);
                updateTarget(currentShape.getBoundingBox().offset(targetPos));
            }
        }

        render(event.stack);
    }

    private void updateTarget(Box box) {
        targetMinX = box.minX;
        targetMinY = box.minY;
        targetMinZ = box.minZ;
        targetMaxX = box.maxX;
        targetMaxY = box.maxY;
        targetMaxZ = box.maxZ;
        if (!initialized) {
            switch (mode.get()) {
                case Grow -> {
                    double cx = (targetMinX + targetMaxX) / 2;
                    double cy = (targetMinY + targetMaxY) / 2;
                    double cz = (targetMinZ + targetMaxZ) / 2;
                    animMinX = cx; animMinY = cy; animMinZ = cz;
                    animMaxX = cx; animMaxY = cy; animMaxZ = cz;
                }
                case Slide -> {
                    animMinX = targetMinX;
                    animMinY = targetMinY + 1.0;
                    animMinZ = targetMinZ;
                    animMaxX = targetMaxX;
                    animMaxY = targetMaxY + 1.0;
                    animMaxZ = targetMaxZ;
                }
                default -> {
                    animMinX = targetMinX;
                    animMinY = targetMinY;
                    animMinZ = targetMinZ;
                    animMaxX = targetMaxX;
                    animMaxY = targetMaxY;
                    animMaxZ = targetMaxZ;
                }
            }
            appearProgress = 0f;
            sweepProgress = 0f;
            initialized = true;
        }
    }

    private void render(MatrixStack stack) {
        if (!initialized) return;
        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastRenderTime) / 1000f, 0.05f);
        lastRenderTime = now;
        if (hasTarget) {
            appearProgress = Math.min(1f, appearProgress + dt * 4f);
        } else {
            appearProgress = Math.max(0f, appearProgress - dt * 5f);
            if (appearProgress <= 0.01f) {
                appearProgress = 0f;
                initialized = false;
                return;
            }
        }
        sweepProgress += dt * SWEEP_SPEED;
        if (sweepProgress > 1f) sweepProgress -= 1f;
        float spd = speed.get();
        float factor = (float) (1.0 - Math.pow(0.001, dt * spd * 0.1));
        animMinX += (targetMinX - animMinX) * factor;
        animMinY += (targetMinY - animMinY) * factor;
        animMinZ += (targetMinZ - animMinZ) * factor;
        animMaxX += (targetMaxX - animMaxX) * factor;
        animMaxY += (targetMaxY - animMaxY) * factor;
        animMaxZ += (targetMaxZ - animMaxZ) * factor;
        float appearEased = easeOutBack(appearProgress);
        double fMinX, fMinY, fMinZ, fMaxX, fMaxY, fMaxZ;
        if (mode.get() == Mode.Grow) {
            double cx = (animMinX + animMaxX) / 2;
            double cy = (animMinY + animMaxY) / 2;
            double cz = (animMinZ + animMaxZ) / 2;
            double hx = (animMaxX - animMinX) / 2 * appearEased;
            double hy = (animMaxY - animMinY) / 2 * appearEased;
            double hz = (animMaxZ - animMinZ) / 2 * appearEased;
            fMinX = cx - hx - EXPAND;
            fMinY = cy - hy - EXPAND;
            fMinZ = cz - hz - EXPAND;
            fMaxX = cx + hx + EXPAND;
            fMaxY = cy + hy + EXPAND;
            fMaxZ = cz + hz + EXPAND;
        } else {
            fMinX = animMinX - EXPAND;
            fMinY = animMinY - EXPAND;
            fMinZ = animMinZ - EXPAND;
            fMaxX = animMaxX + EXPAND;
            fMaxY = animMaxY + EXPAND;
            fMaxZ = animMaxZ + EXPAND;
        }
        float alphaMul = mode.get() == Mode.Grow ? appearEased : Math.min(1f, appearProgress * 2f);
        Color oc, fc;
        if (useClientColor.get()) {
            Color base = ClientSettings.INSTANCE.getColor(0);
            oc = new Color(base.getRed(), base.getGreen(), base.getBlue(),
                    Math.max(1, Math.min(255, (int) (200 * alphaMul))));
            fc = new Color(base.getRed(), base.getGreen(), base.getBlue(),
                    Math.max(1, Math.min(255, (int) (40 * alphaMul))));
        } else {
            oc = new Color(outlineColor.get().getRed(), outlineColor.get().getGreen(),
                    outlineColor.get().getBlue(),
                    Math.max(1, Math.min(255, (int) (outlineColor.get().getAlpha() * alphaMul))));
            fc = new Color(fillColor.get().getRed(), fillColor.get().getGreen(),
                    fillColor.get().getBlue(),
                    Math.max(1, Math.min(255, (int) (fillColor.get().getAlpha() * alphaMul))));
        }
        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        stack.push();
        stack.translate(-cam.x, -cam.y, -cam.z);
        BufferAllocator allocator = new BufferAllocator(262144);
        VertexConsumerProvider.Immediate imm = VertexConsumerProvider.immediate(allocator);
        try {
            if (currentShape == null || currentShape.isEmpty()) return;

            double totalTargetW = targetMaxX - targetMinX;
            double totalTargetH = targetMaxY - targetMinY;
            double totalTargetD = targetMaxZ - targetMinZ;

            double totalAnimW = fMaxX - fMinX;
            double totalAnimH = fMaxY - fMinY;
            double totalAnimD = fMaxZ - fMinZ;

            double animCenterX = (fMinX + fMaxX) / 2.0;
            double animCenterY = (fMinY + fMaxY) / 2.0;
            double animCenterZ = (fMinZ + fMaxZ) / 2.0;

            double scaleX = totalTargetW > 0 ? totalAnimW / totalTargetW : 1;
            double scaleY = totalTargetH > 0 ? totalAnimH / totalTargetH : 1;
            double scaleZ = totalTargetD > 0 ? totalAnimD / totalTargetD : 1;

            boolean runShader = useShader.get();
            if (runShader) {
                BlockHighlightRenderer.getInstance().captureBefore();
            }

            currentShape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
                double worldMinX = minX + lastPos.getX();
                double worldMinY = minY + lastPos.getY();
                double worldMinZ = minZ + lastPos.getZ();
                double worldMaxX = maxX + lastPos.getX();
                double worldMaxY = maxY + lastPos.getY();
                double worldMaxZ = maxZ + lastPos.getZ();

                double partCenterX = (worldMinX + worldMaxX) / 2.0;
                double partCenterY = (worldMinY + worldMaxY) / 2.0;
                double partCenterZ = (worldMinZ + worldMaxZ) / 2.0;

                double offX = (partCenterX - ((targetMinX + targetMaxX) / 2.0)) * scaleX;
                double offY = (partCenterY - ((targetMinY + targetMaxY) / 2.0)) * scaleY;
                double offZ = (partCenterZ - ((targetMinZ + targetMaxZ) / 2.0)) * scaleZ;

                double finalW = (worldMaxX - worldMinX) * scaleX;
                double finalH = (worldMaxY - worldMinY) * scaleY;
                double finalD = (worldMaxZ - worldMinZ) * scaleZ;

                double bMinX = animCenterX + offX - finalW / 2.0;
                double bMinY = animCenterY + offY - finalH / 2.0;
                double bMinZ = animCenterZ + offZ - finalD / 2.0;
                double bMaxX = animCenterX + offX + finalW / 2.0;
                double bMaxY = animCenterY + offY + finalH / 2.0;
                double bMaxZ = animCenterZ + offZ + finalD / 2.0;

                drawFilledBox(stack, imm, bMinX, bMinY, bMinZ, bMaxX, bMaxY, bMaxZ, fc);
                if (alphaMul > 0.01f) {
                    drawGlassSweep(stack, imm, bMinX, bMinY, bMinZ, bMaxX, bMaxY, bMaxZ, alphaMul);
                }
                drawOutlineBox(stack, imm, bMinX, bMinY, bMinZ, bMaxX, bMaxY, bMaxZ, oc);
            });

            imm.draw();

            if (runShader) {
                BlockHighlightRenderer.getInstance().captureAfter();
                BlockHighlightRenderer.getInstance().renderGlowEffect(oc);
            }

        } finally {
            allocator.close();
        }
        stack.pop();
    }

    private void drawGlassSweep(MatrixStack stack, VertexConsumerProvider.Immediate imm,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                float alphaMul) {
        VertexConsumer buf = imm.getBuffer(RenderLayers.debugQuads());
        Matrix4f mat = stack.peek().getPositionMatrix();
        float fx1 = (float) x1, fy1 = (float) y1, fz1 = (float) z1;
        float fx2 = (float) x2, fy2 = (float) y2, fz2 = (float) z2;
        float sizeX = fx2 - fx1;
        if (sizeX < 0.001f) return;
        float raw = sweepProgress * 2f;
        float sweepPos;
        if (raw <= 1f) {
            sweepPos = easeInOutCubic(raw);
        } else {
            sweepPos = easeInOutCubic(2f - raw);
        }
        float halfW = SWEEP_WIDTH * 0.5f;
        float maxA = SWEEP_ALPHA * alphaMul;
        float sweepX = fx1 - halfW * sizeX + (sizeX + SWEEP_WIDTH * sizeX) * sweepPos;
        drawSweepQuad(buf, mat, fx1, sizeX, sweepX, halfW, maxA,
                fx1, fy1, fz1, fx1, fy2, fz1, fx2, fy2, fz1, fx2, fy1, fz1);
        drawSweepQuad(buf, mat, fx1, sizeX, sweepX, halfW, maxA,
                fx1, fy1, fz2, fx2, fy1, fz2, fx2, fy2, fz2, fx1, fy2, fz2);
        drawSweepFace(buf, mat, sweepX, fx1, halfW, sizeX, maxA,
                fx1, fy1, fz1, fx1, fy1, fz2, fx1, fy2, fz2, fx1, fy2, fz1);
        drawSweepFace(buf, mat, sweepX, fx2, halfW, sizeX, maxA,
                fx2, fy1, fz1, fx2, fy2, fz1, fx2, fy2, fz2, fx2, fy1, fz2);
        drawSweepQuad(buf, mat, fx1, sizeX, sweepX, halfW, maxA,
                fx1, fy2, fz1, fx1, fy2, fz2, fx2, fy2, fz2, fx2, fy2, fz1);
        drawSweepQuad(buf, mat, fx1, sizeX, sweepX, halfW, maxA,
                fx1, fy1, fz1, fx2, fy1, fz1, fx2, fy1, fz2, fx1, fy1, fz2);
    }

    private void drawSweepQuad(VertexConsumer buf, Matrix4f mat,
                               float minX, float sizeX, float sweepX, float halfW, float maxA,
                               float v1x, float v1y, float v1z,
                               float v2x, float v2y, float v2z,
                               float v3x, float v3y, float v3z,
                               float v4x, float v4y, float v4z) {
        int a1 = calcGlassAlpha(v1x, minX, sizeX, sweepX, halfW, maxA);
        int a2 = calcGlassAlpha(v2x, minX, sizeX, sweepX, halfW, maxA);
        int a3 = calcGlassAlpha(v3x, minX, sizeX, sweepX, halfW, maxA);
        int a4 = calcGlassAlpha(v4x, minX, sizeX, sweepX, halfW, maxA);
        if (a1 == 0 && a2 == 0 && a3 == 0 && a4 == 0) return;
        buf.vertex(mat, v1x, v1y, v1z).color(255, 253, 248, a1);
        buf.vertex(mat, v2x, v2y, v2z).color(255, 253, 248, a2);
        buf.vertex(mat, v3x, v3y, v3z).color(255, 253, 248, a3);
        buf.vertex(mat, v4x, v4y, v4z).color(255, 253, 248, a4);
    }

    private void drawSweepFace(VertexConsumer buf, Matrix4f mat,
                               float sweepX, float faceX, float halfW, float sizeX, float maxA,
                               float v1x, float v1y, float v1z,
                               float v2x, float v2y, float v2z,
                               float v3x, float v3y, float v3z,
                               float v4x, float v4y, float v4z) {
        float dist = Math.abs(faceX - sweepX);
        float halfWWorld = halfW * sizeX;
        if (dist >= halfWWorld) return;
        float t = 1f - (dist / halfWWorld);
        float broad = t * t * (3f - 2f * t) * 0.5f;
        float sharp = t * t * t * t * 0.5f;
        float a = (broad + sharp) * maxA;
        int ai = Math.max(0, Math.min(255, (int) (a * 255)));
        if (ai == 0) return;
        buf.vertex(mat, v1x, v1y, v1z).color(255, 253, 248, ai);
        buf.vertex(mat, v2x, v2y, v2z).color(255, 253, 248, ai);
        buf.vertex(mat, v3x, v3y, v3z).color(255, 253, 248, ai);
        buf.vertex(mat, v4x, v4y, v4z).color(255, 253, 248, ai);
    }

    private int calcGlassAlpha(float vx, float minX, float sizeX,
                               float sweepX, float halfW, float maxA) {
        float dist = Math.abs(vx - sweepX);
        float halfWWorld = halfW * sizeX;
        if (dist >= halfWWorld) return 0;
        float t = 1f - (dist / halfWWorld);
        float broad = t * t * (3f - 2f * t) * 0.5f;
        float sharp = t * t * t * t * 0.5f;
        float a = (broad + sharp) * maxA;
        return Math.max(0, Math.min(255, (int) (a * 255)));
    }

    private void drawFilledBox(MatrixStack stack, VertexConsumerProvider.Immediate imm,
                               double x1, double y1, double z1,
                               double x2, double y2, double z2, Color c) {
        VertexConsumer buf = imm.getBuffer(RenderLayers.debugQuads());
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue(), a = c.getAlpha();
        Matrix4f mat = stack.peek().getPositionMatrix();
        float fx1 = (float) x1, fy1 = (float) y1, fz1 = (float) z1;
        float fx2 = (float) x2, fy2 = (float) y2, fz2 = (float) z2;
        buf.vertex(mat, fx1, fy1, fz1).color(r, g, b, a);
        buf.vertex(mat, fx2, fy1, fz1).color(r, g, b, a);
        buf.vertex(mat, fx2, fy1, fz2).color(r, g, b, a);
        buf.vertex(mat, fx1, fy1, fz2).color(r, g, b, a);
        buf.vertex(mat, fx1, fy2, fz1).color(r, g, b, a);
        buf.vertex(mat, fx1, fy2, fz2).color(r, g, b, a);
        buf.vertex(mat, fx2, fy2, fz2).color(r, g, b, a);
        buf.vertex(mat, fx2, fy2, fz1).color(r, g, b, a);
        buf.vertex(mat, fx1, fy1, fz1).color(r, g, b, a);
        buf.vertex(mat, fx1, fy2, fz1).color(r, g, b, a);
        buf.vertex(mat, fx2, fy2, fz1).color(r, g, b, a);
        buf.vertex(mat, fx2, fy1, fz1).color(r, g, b, a);
        buf.vertex(mat, fx1, fy1, fz2).color(r, g, b, a);
        buf.vertex(mat, fx2, fy1, fz2).color(r, g, b, a);
        buf.vertex(mat, fx2, fy2, fz2).color(r, g, b, a);
        buf.vertex(mat, fx1, fy2, fz2).color(r, g, b, a);
        buf.vertex(mat, fx1, fy1, fz1).color(r, g, b, a);
        buf.vertex(mat, fx1, fy1, fz2).color(r, g, b, a);
        buf.vertex(mat, fx1, fy2, fz2).color(r, g, b, a);
        buf.vertex(mat, fx1, fy2, fz1).color(r, g, b, a);
        buf.vertex(mat, fx2, fy1, fz1).color(r, g, b, a);
        buf.vertex(mat, fx2, fy2, fz1).color(r, g, b, a);
        buf.vertex(mat, fx2, fy2, fz2).color(r, g, b, a);
        buf.vertex(mat, fx2, fy1, fz2).color(r, g, b, a);
    }

    private void drawOutlineBox(MatrixStack stack, VertexConsumerProvider.Immediate imm,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2, Color c) {
        VertexConsumer buf = imm.getBuffer(RenderLayers.linesTranslucent());
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue(), a = c.getAlpha();
        Matrix4f mat = stack.peek().getPositionMatrix();
        float fx1 = (float) x1, fy1 = (float) y1, fz1 = (float) z1;
        float fx2 = (float) x2, fy2 = (float) y2, fz2 = (float) z2;
        float lw = LINE_WIDTH;

        drawLine(buf, mat, fx1, fy1, fz1, fx2, fy1, fz1, r, g, b, a, lw);
        drawLine(buf, mat, fx2, fy1, fz1, fx2, fy1, fz2, r, g, b, a, lw);
        drawLine(buf, mat, fx2, fy1, fz2, fx1, fy1, fz2, r, g, b, a, lw);
        drawLine(buf, mat, fx1, fy1, fz2, fx1, fy1, fz1, r, g, b, a, lw);

        drawLine(buf, mat, fx1, fy2, fz1, fx2, fy2, fz1, r, g, b, a, lw);
        drawLine(buf, mat, fx2, fy2, fz1, fx2, fy2, fz2, r, g, b, a, lw);
        drawLine(buf, mat, fx2, fy2, fz2, fx1, fy2, fz2, r, g, b, a, lw);
        drawLine(buf, mat, fx1, fy2, fz2, fx1, fy2, fz1, r, g, b, a, lw);

        drawLine(buf, mat, fx1, fy1, fz1, fx1, fy2, fz1, r, g, b, a, lw);
        drawLine(buf, mat, fx2, fy1, fz1, fx2, fy2, fz1, r, g, b, a, lw);
        drawLine(buf, mat, fx2, fy1, fz2, fx2, fy2, fz2, r, g, b, a, lw);
        drawLine(buf, mat, fx1, fy1, fz2, fx1, fy2, fz2, r, g, b, a, lw);
    }

    private void drawLine(VertexConsumer buf, Matrix4f mat, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a, float lw) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0.0001f) {
            dx /= len;
            dy /= len;
            dz /= len;
        }
        buf.vertex(mat, x1, y1, z1).color(r, g, b, a).normal(dx, dy, dz).lineWidth(lw);
        buf.vertex(mat, x2, y2, z2).color(r, g, b, a).normal(dx, dy, dz).lineWidth(lw);
    }

    private Box getBlockBox(BlockPos pos) {
        if (mc.world == null) return new Box(pos);
        var state = mc.world.getBlockState(pos);
        if (state.isAir()) return new Box(pos);
        try {
            var shape = state.getOutlineShape(mc.world, pos);
            if (shape.isEmpty()) return new Box(pos);
            return shape.getBoundingBox().offset(pos);
        } catch (Exception e) {
            return new Box(pos);
        }
    }

    private float easeOutBack(float x) {
        float c1 = 1.70158f, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }

    private float easeInOutSine(float x) {
        return -(float) (Math.cos(Math.PI * x) - 1) / 2f;
    }

    private float easeInOutCubic(float x) {
        return x < 0.5f
                ? 4f * x * x * x
                : 1f - (float) Math.pow(-2f * x + 2f, 3) / 2f;
    }

    @Override
    public void toggle() {
        super.toggle();
        initialized = false;
        hasTarget = false;
        lastPos = null;
        appearProgress = 0f;
        sweepProgress = 0f;
        BlockHighlightRenderer.getInstance().setEnabled(false);
    }
}