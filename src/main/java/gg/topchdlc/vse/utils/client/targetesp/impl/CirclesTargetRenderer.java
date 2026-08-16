package gg.topchdlc.vse.utils.client.targetesp.impl;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.client.targetesp.TargetRendererChoice;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.awt.*;

public class CirclesTargetRenderer extends TargetRendererChoice {

    private static final Identifier CIRCLE_1_TEXTURE = Identifier.of("topchdlc", "images/world/targetesp/circles1.png");
    private static final Identifier CIRCLE_2_TEXTURE = Identifier.of("topchdlc", "images/world/targetesp/circles2.png");

    private final SliderSetting speed = sliderSetting("Speed", 18.0f, 1.0f, 60.0f).increment(1.0f);
    private final CheckBox useCustomColor = checkbox("Custom color", false);
    private final ColorSetting customColor = colorSetting("Color", new Color(0)).visible(useCustomColor::get);
    private final CheckBox pulse = checkbox("Pulse", true);
    private final SliderSetting glowLayers = sliderSetting("Glow Layers", 40, 10, 80).increment(1);

    public CirclesTargetRenderer() {
        super("Circles");
    }

    @Override
    public void render(MatrixStack stack, VertexConsumerProvider consumers, LivingEntity target, float alpha) {
        if (alpha <= 0.01f) return;

        float ticks = mc.player != null ? mc.player.age + mc.getRenderTickCounter().getTickProgress(true) : 0;
        float rotation = ticks * speed.get() * 1.0f;
        float counterRotation = -ticks * speed.get() * 1.0f;
        float pulseValue = pulse.get() ? 1.0f + 0.06f * MathHelper.sin(ticks * 0.12f) : 1.0f;
        float scale = (0.82f + target.getWidth() * 0.55f) * alpha * pulseValue;

        Color c1 = useCustomColor.get() ? customColor.get() : ClientSettings.INSTANCE.getColor(0);
        Color c2 = useCustomColor.get() ? customColor.get() : ClientSettings.INSTANCE.getColor(90);

        GlStateManager._depthMask(false);
        GlStateManager._disableCull();
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GlConst.GL_SRC_ALPHA, GlConst.GL_ONE, GlConst.GL_ZERO, GlConst.GL_ONE);

        VertexConsumer texConsumer = consumers.getBuffer(ClientPipelines.TARGET_ESP.apply(CIRCLE_1_TEXTURE));
        VertexConsumer texConsumer2 = consumers.getBuffer(ClientPipelines.TARGET_ESP.apply(CIRCLE_2_TEXTURE));

        renderTextureLayer(stack, texConsumer, rotation, scale * 0.8f, alpha, c1, c2);
        renderTextureLayer(stack, texConsumer2, counterRotation, scale * 0.99f, alpha, c2, c1);

        renderGlowLayer(stack, texConsumer, rotation, scale * 1.60f, alpha * 1.05f, c1, c2);
        renderGlowLayer(stack, texConsumer2, counterRotation, scale * 1.98f, alpha * 0.95f, c2, c1);

        GlStateManager._blendFuncSeparate(GlConst.GL_ONE, GlConst.GL_ZERO, GlConst.GL_ONE, GlConst.GL_ZERO);
        GlStateManager._disableBlend();
        GlStateManager._enableCull();
        GlStateManager._depthMask(true);
    }

    private void renderTextureLayer(MatrixStack stack, VertexConsumer consumer, float rotation, float scale, float alpha, Color c1, Color c2) {
        if (scale <= 0.01f || alpha <= 0.01f) return;

        stack.push();
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        stack.scale(scale, scale, scale);

        Matrix4f matrix = stack.peek().getPositionMatrix();
        int color270 = injectAlpha(c2, alpha * 255);
        int color0 = injectAlpha(c1, alpha * 255);
        int color180 = injectAlpha(c2, alpha * 255);
        int color90 = injectAlpha(c1, alpha * 255);

        vertex(consumer, matrix, -1.0f, 1.0f, 0.0f, 0.0f, 1.0f, color270);
        vertex(consumer, matrix, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, color0);
        vertex(consumer, matrix, 1.0f, -1.0f, 0.0f, 1.0f, 0.0f, color180);
        vertex(consumer, matrix, -1.0f, -1.0f, 0.0f, 0.0f, 0.0f, color90);

        stack.pop();
    }

    private void renderGlowLayer(MatrixStack stack, VertexConsumer consumer, float rotation, float radius, float alpha, Color c1, Color c2) {
        if (alpha <= 0.01f || radius <= 0.01f) return;

        stack.push();
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));

        Matrix4f matrix = stack.peek().getPositionMatrix();

        int layers = (int) glowLayers.get();
        float maxHeight = radius * 0.1f;
        float expand = radius * 0.28f;

        for (int i = 0; i < layers; i++) {
            float progress = (float) i / (float) layers;
            float yOffset = maxHeight * progress;
            float layerAlpha = alpha * (1.0f - progress) * (1.0f - progress) * 0.14f;
            if (layerAlpha <= 0.01f) continue;

            float layerRadius = radius + expand * progress;
            float half = layerRadius / 2.0f;
            float zOffset = -yOffset;

            int color0 = injectAlpha(c1, layerAlpha * 255);
            int color90 = injectAlpha(c2, layerAlpha * 255);
            int color180 = injectAlpha(c2, layerAlpha * 255);
            int color270 = injectAlpha(c1, layerAlpha * 255);

            vertex(consumer, matrix, -half, half, zOffset, 0.0f, 1.0f, color270);
            vertex(consumer, matrix, half, half, zOffset, 1.0f, 1.0f, color0);
            vertex(consumer, matrix, half, -half, zOffset, 1.0f, 0.0f, color180);
            vertex(consumer, matrix, -half, -half, zOffset, 0.0f, 0.0f, color90);
        }

        stack.pop();
    }

    private int injectAlpha(Color color, float alpha) {
        return (color.getRGB() & 0x00FFFFFF) | (MathHelper.clamp((int) alpha, 0, 255) << 24);
    }

    private void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, float u, float v, int color) {
        consumer.vertex(matrix, x, y, z).texture(u, v).color(color);
    }
}
