package gg.topchdlc.vse.utils.client.targetesp.impl;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.client.targetesp.TargetRendererChoice;
import gg.topchdlc.vse.utils.math.ColorUtility;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

import java.awt.*;

public class DNAHelixTargetRenderer extends TargetRendererChoice {
    final SliderSetting twists = sliderSetting("Twist",1,1,3).increment(0.1f);
    final SliderSetting size = sliderSetting("Size",0.020f,0.020f,0.100f).increment(0.001f);
    final CheckBox useCustomColor = checkbox("Custom color", false);
    final ColorSetting customColor1 = colorSetting("Strand 1", new Color(0xFF6B6B)).visible(useCustomColor::get);
    final ColorSetting customColor2 = colorSetting("Strand 2", new Color(0x4ECDC4)).visible(useCustomColor::get);
    final CheckBox dreamcore = checkbox("Dreamcore", true);
    private static final Identifier BLOOM_TEXTURE =
            Identifier.of("topchdlc", "images/world/bloom.png");
    private LivingEntity lastTarget = null;
    private int lastTargetHurtTime = 0;
    private final long startTime = System.currentTimeMillis();
    private float compressionAmount = 0f;
    private float compressionVelocity = 0f;
    private boolean isCompressing = false;

    public DNAHelixTargetRenderer() {
        super("Orbital");
    }

    @Override
    public void render(MatrixStack stack, VertexConsumerProvider consumers,
                       LivingEntity target, float animation) {
        if (animation <= 0.01f) {
            lastTarget = null;
            compressionAmount = 0f;
            return;
        }

        long now = System.currentTimeMillis();
        float elapsed = (now - startTime) / 1000f;

        if (target != lastTarget) {
            lastTarget = target;
            lastTargetHurtTime = 0;
            compressionAmount = 0f;
            compressionVelocity = 0f;
        }
        detectHit(target);
        updateCompression();

        VertexConsumer consumer = consumers.getBuffer(
                ClientPipelines.TARGET_ESP.apply(BLOOM_TEXTURE));
        GlStateManager._depthMask(false);
        GlStateManager._disableCull();
        
        if (dreamcore.get()) GlStateManager._blendFuncSeparate(
                GlConst.GL_SRC_ALPHA, GlConst.GL_ONE,
                GlConst.GL_ZERO, GlConst.GL_ONE
        );
        GlStateManager._enableBlend();

        renderHelix(stack, consumer, target, elapsed, animation);

        if (dreamcore.get()) GlStateManager._blendFuncSeparate(
                GlConst.GL_ONE, GlConst.GL_ZERO,
                GlConst.GL_ONE, GlConst.GL_ZERO
        );
        GlStateManager._disableBlend();
        
        GlStateManager._enableCull();
        GlStateManager._depthMask(true);
    }


    private void detectHit(LivingEntity target) {
        if (target == null) return;

        if (target.hurtTime > 0 && (lastTargetHurtTime == 0 || target.hurtTime > lastTargetHurtTime)) {
            isCompressing = true;
            compressionVelocity = 2.5f;
        }

        lastTargetHurtTime = target.hurtTime;
    }


    private void updateCompression() {
        if (isCompressing) {
            compressionAmount += compressionVelocity * 0.016f;

            if (compressionAmount >= 0.5f) {
                compressionAmount = 0.5f;
                isCompressing = false;
                compressionVelocity = -3f;
            }
        } else if (compressionAmount != 0f) {
            float springForce = -compressionAmount * 25f;
            float damping = -compressionVelocity * 4f;
            compressionVelocity += (springForce + damping) * 0.016f;
            compressionAmount += compressionVelocity * 0.016f;

            if (Math.abs(compressionAmount) < 0.005f && Math.abs(compressionVelocity) < 0.1f) {
                compressionAmount = 0f;
                compressionVelocity = 0f;
            }
        }
    }


    private void renderHelix(MatrixStack stack, VertexConsumer consumer,
                             LivingEntity target, float elapsed, float globalAnim) {

        float radius = 0.7f;
        float height = 2 * (1f - compressionAmount * 0.6f);
        float radiusBonus = compressionAmount * 0.3f;
        radius += radiusBonus;

        int points = 32;
        float twist = twists.get();
        float rotation = elapsed * 0.8f * 60f;
        float size = 0.045f;

        float baseY = (target.getHeight() - height) * 0.5f;

        Color strand1Color, strand2Color;
        if (useCustomColor.get()) {
            strand1Color = customColor1.get();
            strand2Color = customColor2.get();
        } else {
            strand1Color = ClientSettings.INSTANCE.getColor((int)(elapsed * 30));
            strand2Color = ClientSettings.INSTANCE.getColor((int)(elapsed * 30) + 80);
        }

        renderStrand(stack, consumer, points, radius, height, baseY, twist, rotation,
                0f, strand1Color, globalAnim, size, elapsed);

        renderStrand(stack, consumer, points, radius, height, baseY, twist, rotation,
                (float)Math.PI, strand2Color, globalAnim, size, elapsed);

        renderBridges(stack, consumer, radius, height, baseY, twist, rotation,
                strand1Color, strand2Color, globalAnim, elapsed);
    }

    private void renderStrand(MatrixStack stack, VertexConsumer consumer,
                              int points, float radius, float height, float baseY,
                              float twist, float rotation, float phaseOffset,
                              Color color, float alpha, float size, float elapsed) {

        for (int i = 0; i < points; i++) {
            float t = (float) i / (points - 1);
            float y = baseY + t * height;
            float angle = (float)(t * Math.PI * 2 * twist + Math.toRadians(rotation) + phaseOffset);
            float x = (float)Math.cos(angle) * radius;
            float z = (float)Math.sin(angle) * radius;
            float pulse = 1f + 0.2f * (float)Math.sin(t * 10f + elapsed * 3f);
            float pointSize = size * pulse;
            Color c = ColorUtility.injectAlpha(color, (int)(alpha * 255 * (0.6f + t * 0.4f)));
            for (int layer = 0; layer < 3; layer++) {
                float layerSize = pointSize * (1f + layer * 0.7f);
                float layerAlpha = 1f / (1f + layer * 0.5f);
                Color layerColor = ColorUtility.injectAlpha(c, (int)(c.getAlpha() * layerAlpha));
                stack.push();
                stack.translate(x, y, z);
                stack.multiply(mc.gameRenderer.getCamera().getRotation());
                var m = stack.peek();
                consumer.vertex(m, -layerSize, -layerSize, 0).texture(0, 0).color(layerColor.getRGB());
                consumer.vertex(m, -layerSize,  layerSize, 0).texture(0, 1).color(layerColor.getRGB());
                consumer.vertex(m,  layerSize,  layerSize, 0).texture(1, 1).color(layerColor.getRGB());
                consumer.vertex(m,  layerSize, -layerSize, 0).texture(1, 0).color(layerColor.getRGB());
                stack.pop();
            }
        }
    }

    private void renderBridges(MatrixStack stack, VertexConsumer consumer,
                               float radius, float height, float baseY,
                               float twist, float rotation,
                               Color color1, Color color2, float alpha, float elapsed) {

        int bridges = 8;
        float bridgeSize = 0.045f * 0.6f;

        for (int b = 0; b < bridges; b++) {
            float t = (float)(b + 0.5f) / bridges;
            float y = baseY + t * height;

            float angle1 = (float)(t * Math.PI * 2 * twist + Math.toRadians(rotation));
            float angle2 = angle1 + (float)Math.PI;

            float x1 = (float)Math.cos(angle1) * radius;
            float z1 = (float)Math.sin(angle1) * radius;
            float x2 = (float)Math.cos(angle2) * radius;
            float z2 = (float)Math.sin(angle2) * radius;
            int bridgePoints = 5;
            for (int p = 0; p <= bridgePoints; p++) {
                float bt = (float) p / bridgePoints;
                float bx = x1 + (x2 - x1) * bt;
                float bz = z1 + (z2 - z1) * bt;
                Color c = ColorUtility.blend(color1, color2, bt);
                c = ColorUtility.injectAlpha(c, (int)(alpha * 200));
                float pulse = 1f + 0.15f * (float)Math.sin(bt * Math.PI + elapsed * 4f + b);
                float s = bridgeSize * pulse;
                stack.push();
                stack.translate(bx, y, bz);
                stack.multiply(mc.gameRenderer.getCamera().getRotation());
                var m = stack.peek();
                consumer.vertex(m, -s, -s, 0).texture(0, 0).color(c.getRGB());
                consumer.vertex(m, -s,  s, 0).texture(0, 1).color(c.getRGB());
                consumer.vertex(m,  s,  s, 0).texture(1, 1).color(c.getRGB());
                consumer.vertex(m,  s, -s, 0).texture(1, 0).color(c.getRGB());

                stack.pop();
            }
        }
    }
}