package gg.topchdlc.vse.utils.client.targetesp.impl;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.client.targetesp.TargetRendererChoice;
import gg.topchdlc.vse.utils.math.ColorUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.LinkedList;
import java.util.Random;

public class PrizrakTargetRenderer extends TargetRendererChoice {
    public PrizrakTargetRenderer() {
        super("Prizrak");
    }

    final CheckBox useCustomColor = checkbox("Custom color", false);
    final ColorSetting customColor = colorSetting("Color", new Color(0)).visible(useCustomColor::get);
    final EnumSetting<Image> image = enumSetting("Image", Image.Glow);
    final CheckBox dreamcore = checkbox("Dreamcore", true);
    final SliderSetting size = sliderSetting("Size", 1f, 0.4f, 10).increment(0.1f);
    final SliderSetting length = sliderSetting("Length", 31, 15, 40);
    final CheckBox kurva = checkbox("Curve", true);
    final SliderSetting trailPower = sliderSetting("Trail Power", 3.6f, 0.1f, 5.0f).increment(0.1f);

    long lastTime = System.currentTimeMillis();
    private final LinkedList<Vec3d> positionHistory = new LinkedList<>();
    private static final int MAX_HISTORY = 300;
    private int lastTargetId = -1;
    private Vec3d smoothPos = null;
    private float smoothTrailFactor = 0f;
    private float appearProgress = 0f;
    private boolean isActive = false;
    private boolean isDisappearing = false;
    private float disappearProgress = 0f;
    private long lastRenderTime = System.currentTimeMillis();
    private float[] scatterDirX, scatterDirY, scatterDirZ, scatterSpeed;
    private void generateScatter(int count) {
        scatterDirX = new float[count];
        scatterDirY = new float[count];
        scatterDirZ = new float[count];
        scatterSpeed = new float[count];
        Random r = new Random();
        for (int i = 0; i < count; i++) {
            scatterDirX[i] = (r.nextFloat() - 0.5f) * 2f;
            scatterDirY[i] = 0.5f + r.nextFloat() * 1.5f;
            scatterDirZ[i] = (r.nextFloat() - 0.5f) * 2f;
            scatterSpeed[i] = 0.5f + r.nextFloat() * 1.5f;
        }
    }

    @Override
    public void render(MatrixStack stack, VertexConsumerProvider consumers, LivingEntity target, float alpha) {
        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastRenderTime) / 1000f, 0.05f);
        lastRenderTime = now;
        Vec3d currentPos = target.getEntityPos();
        if (target.getId() != lastTargetId) {
            positionHistory.clear();
            smoothPos = null;
            lastTargetId = target.getId();
            appearProgress = 0f;
            isActive = true;
            isDisappearing = false;
            disappearProgress = 0f;
            smoothTrailFactor = 0f;
        }
        if (smoothPos == null) {
            smoothPos = currentPos;
        } else {
            double f = 1.0 - Math.pow(0.001, dt);
            smoothPos = new Vec3d(
                    smoothPos.x + (currentPos.x - smoothPos.x) * f,
                    smoothPos.y + (currentPos.y - smoothPos.y) * f,
                    smoothPos.z + (currentPos.z - smoothPos.z) * f
            );
        }
        positionHistory.addFirst(smoothPos);
        while (positionHistory.size() > MAX_HISTORY) positionHistory.removeLast();
        double velocity = 0;
        if (positionHistory.size() > 3) {
            velocity = positionHistory.get(0).distanceTo(positionHistory.get(Math.min(3, positionHistory.size() - 1)));
        }
        float targetTrail = Math.min(1f, (float) (velocity * 8.0));
        if (targetTrail > smoothTrailFactor) {
            smoothTrailFactor += (targetTrail - smoothTrailFactor) * Math.min(1f, dt * 15f);
        } else {
            smoothTrailFactor += (targetTrail - smoothTrailFactor) * Math.min(1f, dt * 3f);
        }
        smoothTrailFactor = Math.max(0f, Math.min(1f, smoothTrailFactor));
        if (alpha > 0.01f && !isDisappearing) {
            isActive = true;
            appearProgress = Math.min(1f, appearProgress + dt * 1.7f);
        }
        if (alpha < 0.01f && isActive && !isDisappearing) {
            isDisappearing = true;
            disappearProgress = 0f;
            generateScatter((int) length.get() * 3);
        }
        if (isDisappearing) {
            disappearProgress = Math.min(1f, disappearProgress + dt * 3f);
            if (disappearProgress >= 1f) {
                isActive = false;
                isDisappearing = false;
                appearProgress = 0f;
                return;
            }
        }
        if (!isActive && appearProgress <= 0f) return;
        float effectiveAlpha = isDisappearing ? (1f - disappearProgress) : (appearProgress * alpha);
        VertexConsumer consumer = consumers.getBuffer(ClientPipelines.TARGET_ESP.apply(image.get().id));
        GlStateManager._depthMask(false);
        GlStateManager._disableCull();
        
        if (dreamcore.get()) GlStateManager._blendFuncSeparate(
                GlConst.GL_SRC_ALPHA, GlConst.GL_ONE, GlConst.GL_ZERO, GlConst.GL_ONE
        );
        GlStateManager._enableBlend();
        stack.translate(0, target.getHeight() * 0.5f + 0.3f, 0);
        int[] counter = {0};
        particle(stack, consumer, (sin, cos) -> new Vec3d(sin, cos, -cos), effectiveAlpha, currentPos, counter);
        particle(stack, consumer, (sin, cos) -> new Vec3d(-sin, sin, -cos), effectiveAlpha, currentPos, counter);
        particle(stack, consumer, (sin, cos) -> new Vec3d(-sin, -sin, cos), effectiveAlpha, currentPos, counter);
        if (dreamcore.get()) GlStateManager._blendFuncSeparate(GlConst.GL_ONE, GlConst.GL_ZERO, GlConst.GL_ONE, GlConst.GL_ZERO);
        GlStateManager._disableBlend();
        
        GlStateManager._enableCull();
        GlStateManager._depthMask(true);
    }

    void particle(MatrixStack stack, VertexConsumer consumer, Transformation transformation,
                  float alpha, Vec3d currentPos, int[] globalCounter) {
        double radius = 0.67 + this.size.get() / this.size.getMax();
        double distance = this.image.is(Image.Triangle) ? 7 : 10.0 + (length.get() * 0.2);
        float size = this.size.get();
        int alphaFactor = 15;
        int count = (int) (this.length.get() * alpha);
        for (int i = 0; i < count; i++) {
            int gIdx = globalCounter[0]++;
            float progress = (float) i / Math.max(count - 1, 1);
            stack.push();
            float delay = progress * 0.7f;
            float adjAppear = Math.max(0f, Math.min(1f,
                    (appearProgress - delay) / Math.max(1f - delay, 0.01f)));
            float appearEased = easeOutBack(adjAppear);
            float appearY = (1f - appearEased) * (-4f - i * 0.25f);
            float sX = 0, sY = 0, sZ = 0;
            if (isDisappearing && scatterDirX != null && gIdx < scatterDirX.length) {
                float e = easeOutQuad(disappearProgress);
                sX = scatterDirX[gIdx] * e * scatterSpeed[gIdx] * 3f;
                sY = scatterDirY[gIdx] * e * scatterSpeed[gIdx] * 3f;
                sZ = scatterDirZ[gIdx] * e * scatterSpeed[gIdx] * 3f;
            }
            double angle = 0.15 * ((System.currentTimeMillis() - lastTime) / 2F - (i * distance)) / 30.0;
            double sin = Math.sin(angle) * radius;
            double cos = Math.cos(angle) * radius;
            stack.translate(transformation.make(sin, cos));
            if (this.kurva.get()) {
                stack.translate(0, Math.sin((angle + i) * 15 * MathUtility.TO_RADIANS) * 0.1, 0);
            }
            if (positionHistory.size() > 2 && !isDisappearing && smoothTrailFactor > 0.01f) {
                float easedP = progress * progress;
                float histIdx = easedP * (positionHistory.size() - 1);
                Vec3d pastPos = getSmoothedPos(histIdx);
                double power = trailPower.get();
                double tx = (pastPos.x - currentPos.x) * progress * power;
                double ty = (pastPos.y - currentPos.y) * progress * power;
                double tz = (pastPos.z - currentPos.z) * progress * power;
                stack.translate(tx * smoothTrailFactor, ty * smoothTrailFactor, tz * smoothTrailFactor);
            }
            stack.translate(sX, appearY + sY, sZ);
            stack.multiply(mc.gameRenderer.getCamera().getRotation());
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                    (float) (System.currentTimeMillis() - lastTime - i * 100F) / 10F));
            stack.translate(size / 2f, size / 2f, 0);
            Color color1, color2;
            if (useCustomColor.get()) {
                color1 = customColor.get();
                color2 = customColor.get();
            } else {
                color1 = ClientSettings.INSTANCE.getColor(0);
                color2 = ClientSettings.INSTANCE.getColor(90);
            }
            float finalA = isDisappearing
                    ? (255 - (i * alphaFactor)) * (1f - disappearProgress)
                    : (255 - (i * alphaFactor)) * alpha * adjAppear;

            color1 = ColorUtility.injectAlpha(color1, finalA);
            color2 = ColorUtility.injectAlpha(color2, finalA);

            var matrix = stack.peek();
            consumer.vertex(matrix, 0, -size, 0).texture(0, 0).color(color2.getRGB());
            consumer.vertex(matrix, -size, -size, 0).texture(0, 1).color(color1.getRGB());
            consumer.vertex(matrix, -size, 0, 0).texture(1, 1).color(color2.getRGB());
            consumer.vertex(matrix, 0, 0, 0).texture(1, 0).color(color1.getRGB());
            stack.pop();
        }
    }
    private Vec3d getSmoothedPos(float index) {
        if (positionHistory.size() < 2)
            return positionHistory.isEmpty() ? Vec3d.ZERO : positionHistory.getFirst();
        int max = positionHistory.size() - 1;
        int i1 = Math.min((int) Math.floor(index), max);
        int i2 = Math.min(i1 + 1, max);
        int i0 = Math.max(i1 - 1, 0);
        int i3 = Math.min(i2 + 1, max);
        float frac = index - i1;
        return catmullRom(positionHistory.get(i0), positionHistory.get(i1),
                positionHistory.get(i2), positionHistory.get(i3), frac);
    }
    private Vec3d catmullRom(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, float t) {
        float t2 = t * t, t3 = t2 * t;
        double x = 0.5 * ((2 * p1.x) + (-p0.x + p2.x) * t + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3);
        double y = 0.5 * ((2 * p1.y) + (-p0.y + p2.y) * t + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3);
        double z = 0.5 * ((2 * p1.z) + (-p0.z + p2.z) * t + (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2 + (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3);
        return new Vec3d(x, y, z);
    }
    private float easeOutBack(float x) {
        float c1 = 1.70158f, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }
    private float easeOutQuad(float x) {
        return 1 - (1 - x) * (1 - x);
    }
    @FunctionalInterface
    interface Transformation {
        Vec3d make(double sin, double cos);
    }
    enum Image {
        Glow("glow"), Triangle("triangle");
        public final Identifier id;
        Image(String img) { id = Identifier.of("topchdlc", "images/world/ghost-" + img + ".png"); }
    }
}