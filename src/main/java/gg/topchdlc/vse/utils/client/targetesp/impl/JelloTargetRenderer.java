package gg.topchdlc.vse.utils.client.targetesp.impl;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.client.targetesp.TargetRendererChoice;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;

import java.awt.*;

public class JelloTargetRenderer extends TargetRendererChoice {

    private static final Identifier BLOOM_TEXTURE =
            Identifier.of("topchdlc", "images/world/bloom.png");

    final CheckBox useCustomColor = checkbox("Custom color", false);
    final ColorSetting customColor = colorSetting("Color", new Color(0x00FFFF)).visible(useCustomColor::get);
    final CheckBox dreamcore = checkbox("Dreamcore", true);

    private float jelloMoving = 0f;
    private final long startTime = System.currentTimeMillis();

    public JelloTargetRenderer() {
        super("Jello");
    }

    @Override
    public void render(MatrixStack stack, VertexConsumerProvider consumers,
                       LivingEntity target, float animation) {
        if (animation <= 0.0f) return;

        jelloMoving += 0.8f;

        float elapsed = (System.currentTimeMillis() - startTime) / 1000f;
        float alphaAnim = easeOutCubic(animation);

        Color themeColor = useCustomColor.get()
                ? customColor.get()
                : ClientSettings.INSTANCE.getColor((int)(elapsed * 35));

        int color = themeColor.getRGB();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        float entityWidth = target.getWidth() * 1.65f;
        float entityHeight = target.getHeight() - 0.15f;

        Quaternionf camRot = mc.gameRenderer.getCamera().getRotation();
        float camYaw = mc.gameRenderer.getCamera().getYaw();
        float camPitch = mc.gameRenderer.getCamera().getPitch();

        VertexConsumer consumer = consumers.getBuffer(
                ClientPipelines.TARGET_ESP.apply(BLOOM_TEXTURE));

        GlStateManager._depthMask(false);
        GlStateManager._disableCull();
        
        if (dreamcore.get()) GlStateManager._blendFuncSeparate(
                GlConst.GL_SRC_ALPHA, GlConst.GL_ONE,
                GlConst.GL_ZERO, GlConst.GL_ONE
        );
        GlStateManager._enableBlend();
        float sizeBase = 0.2f;
        for (int i = 0; i < 360; i += 2) {
            float scale = Math.max(0.5f, 0.7f - 0.2f * alphaAnim);
            double rad = Math.toRadians(i + jelloMoving);
            float xOffset = (float)(Math.cos(rad) * entityWidth * scale);
            float zOffset = (float)(Math.sin(rad) * entityWidth * scale);

            for (int j = 0; j < 15; j++) {
                float yOffsetLayer = entityHeight / 1.7f
                        + (entityHeight / 2.0f) * (float) Math.cos(Math.toRadians(jelloMoving / 1.5f + j * 2.0f));

                int finalAlpha = (int)(255 * alphaAnim * ((float) j / 6.4f) * 0.05f);
                if (finalAlpha <= 0) continue;

                stack.push();
                stack.translate(xOffset, yOffsetLayer, zOffset);
                stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camYaw));
                stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camPitch));

                var m = stack.peek();
                float h = sizeBase / 2.0f;
                consumer.vertex(m, -h, -h, 0).texture(0, 0).color(r, g, b, finalAlpha);
                consumer.vertex(m,  h, -h, 0).texture(1, 0).color(r, g, b, finalAlpha);
                consumer.vertex(m,  h,  h, 0).texture(1, 1).color(r, g, b, finalAlpha);
                consumer.vertex(m, -h,  h, 0).texture(0, 1).color(r, g, b, finalAlpha);
                stack.pop();
            }
        }
        float sizeLarge = 0.2f;
        for (int i = 0; i < 360; i += 2) {
            float scale = Math.max(0.5f, 0.7f - 0.2f * alphaAnim);
            double rad = Math.toRadians(i + jelloMoving);
            float xOffset = (float)(Math.cos(rad) * entityWidth * scale);
            float zOffset = (float)(Math.sin(rad) * entityWidth * scale);
            float yOffset = entityHeight / 1.75f
                    + (entityHeight / 2.0f) * (float) Math.cos(Math.toRadians(jelloMoving / 1.5f + 30.0f));

            int finalAlpha = (int)(255 * alphaAnim * 0.2f);
            if (finalAlpha <= 0) continue;

            stack.push();
            stack.translate(xOffset, yOffset, zOffset);
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camYaw));
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camPitch));

            var m = stack.peek();
            float h = sizeLarge / 2.0f;
            consumer.vertex(m, -h, -h, 0).texture(0, 0).color(r, g, b, finalAlpha);
            consumer.vertex(m,  h, -h, 0).texture(1, 0).color(r, g, b, finalAlpha);
            consumer.vertex(m,  h,  h, 0).texture(1, 1).color(r, g, b, finalAlpha);
            consumer.vertex(m, -h,  h, 0).texture(0, 1).color(r, g, b, finalAlpha);
            stack.pop();
        }

        if (dreamcore.get()) GlStateManager._blendFuncSeparate(
                GlConst.GL_ONE, GlConst.GL_ZERO,
                GlConst.GL_ONE, GlConst.GL_ZERO
        );
        GlStateManager._disableBlend();
        
        GlStateManager._enableCull();
        GlStateManager._depthMask(true);
    }

    private float easeOutCubic(float t) {
        return 1f - (float) Math.pow(1 - t, 3);
    }
}
