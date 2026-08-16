package gg.topchdlc.vse.utils.client.targetesp.impl;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.client.targetesp.TargetRendererChoice;
import gg.topchdlc.vse.utils.math.ColorUtility;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

import java.awt.*;

public class DonutTargetRenderer extends TargetRendererChoice {

    private static final float glowSize = 0.11f;
    private static final float ringRadius = 0.55f;
    private static final float bobSpeed = 1.4f;
    private static final float bobHeight = 0.95f;
    private static final float segments = 50;
    private static final float brightness = 2.2f;

    private static final Identifier BLOOM_TEXTURE =
            Identifier.of("topchdlc", "images/world/bloom.png");

    //final SliderSetting ringRadius  = sliderSetting("Ring Radius", 0.55f, 0.2f, 1.5f).increment(0.05f);
    //final SliderSetting glowSize    = sliderSetting("Glow Size",   0.22f, 0.05f, 0.6f).increment(0.01f);
  // final SliderSetting bobHeight   = sliderSetting("Bob Height",  0.5f,  0.05f, 2.0f).increment(0.05f);
    //final SliderSetting bobSpeed    = sliderSetting("Bob Speed",   1.4f,  0.2f,  5.0f).increment(0.1f);
    //final SliderSetting segments    = sliderSetting("Segments",    40,    12,    80);
   // final SliderSetting brightness  = sliderSetting("Brightness",  2.2f,  0.5f,  4.0f).increment(0.1f);
    final CheckBox useCustomColor   = checkbox("Custom color", false);
    final ColorSetting customColor  = colorSetting("Color", new Color(0x00BFFF)).visible(useCustomColor::get);
    final CheckBox dreamcore        = checkbox("Dreamcore", true);

    private final long startTime = System.currentTimeMillis();

    public DonutTargetRenderer() {
        super("Donut");
    }

    @Override
    public void render(MatrixStack stack, VertexConsumerProvider consumers,
                       LivingEntity target, float animation) {
        if (animation <= 0.01f) return;

        float elapsed = (System.currentTimeMillis() - startTime) / 1000f;
        float y = target.getHeight() * 0.5f
                + (float) Math.sin(elapsed * bobSpeed) * bobHeight;
        float pulse = 1f + 0.06f * (float) Math.sin(elapsed * 4.5f);

        Color c1, c2;
        if (useCustomColor.get()) {
            c1 = c2 = customColor.get();
        } else {
            c1 = ClientSettings.INSTANCE.getColor((int)(elapsed * 35));
            c2 = ClientSettings.INSTANCE.getColor((int)(elapsed * 35) + 70);
        }
        c1 = boost(c1);
        c2 = boost(c2);

        VertexConsumer consumer = consumers.getBuffer(
                ClientPipelines.TARGET_ESP.apply(BLOOM_TEXTURE));

        GlStateManager._depthMask(false);
        GlStateManager._disableCull();
        
        if (dreamcore.get()) GlStateManager._blendFuncSeparate(
                GlConst.GL_SRC_ALPHA, GlConst.GL_ONE,
                GlConst.GL_ZERO, GlConst.GL_ONE
        );
        GlStateManager._enableBlend();

        float R = ringRadius * pulse;
        float gs = glowSize;
        int N = (int) segments;
        Quaternionf camRot = mc.gameRenderer.getCamera().getRotation();
        for (int i = 0; i < N; i++) {
            float angle = (float) i / N * (float)(Math.PI * 2);
            float px = (float) Math.cos(angle) * R;
            float pz = (float) Math.sin(angle) * R;

            float t = (float) i / N;
            Color c = ColorUtility.blend(c1, c2, t);
            Color bright = ColorUtility.injectAlpha(c, (int)(animation * 255));

            drawBillboard(stack, consumer, camRot, px, y, pz, gs, bright);
        }
        if (dreamcore.get()) {
            for (int i = 0; i < N; i++) {
                float angle = (float) i / N * (float)(Math.PI * 2);
                float px = (float) Math.cos(angle) * R;
                float pz = (float) Math.sin(angle) * R;

                float t = (float) i / N;
                Color c = ColorUtility.blend(c1, c2, t);
                Color glow = ColorUtility.injectAlpha(c, (int)(animation * 90));

                drawBillboard(stack, consumer, camRot, px, y, pz, gs * 2.2f, glow);
            }

            for (int i = 0; i < N; i++) {
                float angle = (float) i / N * (float)(Math.PI * 2);
                float px = (float) Math.cos(angle) * R;
                float pz = (float) Math.sin(angle) * R;

                float t = (float) i / N;
                Color c = ColorUtility.blend(c1, c2, t);
                Color outer = ColorUtility.injectAlpha(c, (int)(animation * 35));

                drawBillboard(stack, consumer, camRot, px, y, pz, gs * 4.0f, outer);
            }
        }

        if (dreamcore.get()) GlStateManager._blendFuncSeparate(
                GlConst.GL_ONE, GlConst.GL_ZERO,
                GlConst.GL_ONE, GlConst.GL_ZERO
        );
        GlStateManager._disableBlend();
        
        GlStateManager._enableCull();
        GlStateManager._depthMask(true);
    }
    private void drawBillboard(MatrixStack stack, VertexConsumer consumer,
                               Quaternionf camRot,
                               float px, float py, float pz,
                               float size, Color color) {
        stack.push();
        stack.translate(px, py, pz);
        stack.multiply(camRot);
        var m = stack.peek();
        int rgb = color.getRGB();
        consumer.vertex(m, -size, -size, 0).texture(0, 0).color(rgb);
        consumer.vertex(m, -size,  size, 0).texture(0, 1).color(rgb);
        consumer.vertex(m,  size,  size, 0).texture(1, 1).color(rgb);
        consumer.vertex(m,  size, -size, 0).texture(1, 0).color(rgb);
        stack.pop();
    }

    private Color boost(Color c) {
        float f = brightness;
        return new Color(
                Math.min(255, (int)(c.getRed()   * f)),
                Math.min(255, (int)(c.getGreen() * f)),
                Math.min(255, (int)(c.getBlue()  * f)),
                c.getAlpha()
        );
    }
}
