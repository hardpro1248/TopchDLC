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
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.*;

public class Crystal2TargetRenderer extends TargetRendererChoice {

    private static final Identifier BLOOM_TEXTURE =
            Identifier.of("topchdlc", "images/world/bloom.png");

    private static final float[][] VERTICES = {
            { 0,  1.5f,  0},
            { 0, -1.5f,  0},
            { 1,  0,     0},
            {-1,  0,     0},
            { 0,  0,     1},
            { 0,  0,    -1},
    };
    private static final int[][] FACES = {
            {0,2,4},{0,4,3},{0,3,5},{0,5,2},
            {1,4,2},{1,3,4},{1,5,3},{1,2,5},
    };

    final CheckBox      twoColors     = checkbox("Two colors", true);
    final CheckBox      useCustomColor = checkbox("Custom color", false);
    final ColorSetting  customColor   = colorSetting("Color", new Color(0x00FFFF)).visible(useCustomColor::get);
    final CheckBox      dreamcore     = checkbox("Dreamcore", true);

    private static final int NUM_LAYERS = 4;

    public Crystal2TargetRenderer() {
        super("Crystal");
    }

    @Override
    public void render(MatrixStack stack, VertexConsumerProvider consumers,
                       LivingEntity target, float animation) {
        if (animation <= 0.01f) return;

        long now = System.currentTimeMillis();
        float timeOffset = (now % 4000) / 4000f * 360f;
        float speed      = 10 / 10f;
        int   count      = 8;
        float size       = 10 / 100f;
        float width      = target.getWidth() * 1.6f;

        int themeColor  = getThemeColor(0);
        int baseColor   = themeColor;
        int secondColor = twoColors.get() ? lerpColor(themeColor, 0xFFFFFFFF, 0.5f) : themeColor;

        VertexConsumer consumer = consumers.getBuffer(
                ClientPipelines.TARGET_ESP.apply(BLOOM_TEXTURE));

        GlStateManager._depthMask(false);
        GlStateManager._disableCull();
        
        if (dreamcore.get()) GlStateManager._blendFuncSeparate(
                GlConst.GL_SRC_ALPHA, GlConst.GL_ONE,
                GlConst.GL_ZERO, GlConst.GL_ONE);
        GlStateManager._enableBlend();

        int crystalIdx = 0;
        for (int layer = 0; layer < NUM_LAYERS; layer++) {
            float baseHeight = target.getHeight() * ((layer + 0.5f) / NUM_LAYERS);
            float angleStep  = 360f / count;

            for (int i = 0; i < count; i++) {
                float angle = i * angleStep + (layer * 25f) + timeOffset * speed;
                float val   = 1.2f - 0.5f * animation;
                float sin   = (float)(Math.sin(Math.toRadians(angle)) * (width * val));
                float cos   = (float)(Math.cos(Math.toRadians(angle)) * (width * val));

                float appear = appearProgress(animation, crystalIdx);
                if (appear >= 0.01f) {
                    float s            = size * appear;
                    float heightOffset = baseHeight + (1f - appear) * -0.5f;

                    int crystalColor = twoColors.get()
                            ? lerpColor(baseColor, secondColor,
                                (float)(Math.sin(Math.toRadians(angle) + crystalIdx) * 0.5f + 0.5f))
                            : baseColor;
                    int alpha = (int)(255 * animation * appear);
                    stack.push();
                    stack.translate(sin, heightOffset, cos);
                    Vector3f dir = new Vector3f(-sin,
                            (float)(target.getHeight() / 2.0 - heightOffset), -cos).normalize();
                    stack.multiply(new Quaternionf().rotationTo(new Vector3f(0f, 1f, 0f), dir));
                    var m = stack.peek();
                    for (int[] face : FACES) {
                        float[] v0 = VERTICES[face[0]];
                        float[] v1 = VERTICES[face[1]];
                        float[] v2 = VERTICES[face[2]];
                        int fc = withAlpha(crystalColor, alpha);
                        consumer.vertex(m, v0[0]*s, v0[1]*s, v0[2]*s).texture(0.5f,0.5f).color(fc);
                        consumer.vertex(m, v1[0]*s, v1[1]*s, v1[2]*s).texture(0.5f,0.5f).color(fc);
                        consumer.vertex(m, v2[0]*s, v2[1]*s, v2[2]*s).texture(0.5f,0.5f).color(fc);
                        consumer.vertex(m, v2[0]*s, v2[1]*s, v2[2]*s).texture(0.5f,0.5f).color(fc);
                    }
                    stack.pop();

                    float bloomAlpha = animation * appear *
                            (0.35f + 0.1f * (float)Math.sin(now / 300.0 + crystalIdx * 0.5));
                    int glowColor = withAlpha(crystalColor, (int)(255 * bloomAlpha));
                    float hs = (size * 6.5f * appear) / 2f;

                    stack.push();
                    stack.translate(sin, heightOffset, cos);
                    stack.multiply(mc.gameRenderer.getCamera().getRotation());
                    var gm = stack.peek();
                    consumer.vertex(gm, -hs, -hs, 0).texture(0, 0).color(glowColor);
                    consumer.vertex(gm, -hs,  hs, 0).texture(0, 1).color(glowColor);
                    consumer.vertex(gm,  hs,  hs, 0).texture(1, 1).color(glowColor);
                    consumer.vertex(gm,  hs, -hs, 0).texture(1, 0).color(glowColor);
                    stack.pop();
                }
                crystalIdx++;
            }
        }

        if (dreamcore.get()) GlStateManager._blendFuncSeparate(
                GlConst.GL_ONE, GlConst.GL_ZERO,
                GlConst.GL_ONE, GlConst.GL_ZERO);
        GlStateManager._disableBlend();
        
        GlStateManager._enableCull();
        GlStateManager._depthMask(true);
    }

    private float appearProgress(float tProgress, int idx) {
        float denom = 1f - idx * 0.03f;
        if (denom <= 0f) return 1f;
        return Math.max(0f, Math.min(1f, (tProgress - idx * 0.03f) / denom));
    }

    private int getThemeColor(int offset) {
        if (useCustomColor.get()) return customColor.get().getRGB();
        return ClientSettings.INSTANCE.getColor(offset).getRGB();
    }
    private static int lerpColor(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int)(ar + (br - ar) * t);
        int g = (int)(ag + (bg - ag) * t);
        int bl = (int)(ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}
