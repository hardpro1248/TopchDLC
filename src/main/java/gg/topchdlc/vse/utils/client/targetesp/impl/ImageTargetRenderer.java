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
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.*;

public class ImageTargetRenderer extends TargetRendererChoice {

    private final EnumSetting<Mode> crystalMode = enumSetting("Mode", Mode.Default);
    private final CheckBox useCustomColor = checkbox("Custom color", false);
    private final ColorSetting customColor = colorSetting("Color", new Color(0)).visible(useCustomColor::get);
    private final SliderSetting speed = sliderSetting("Speed", 360, 0, 360);
    private final CheckBox dreamcore = checkbox("Dreamcore", true);
    private final SliderSetting brightness = sliderSetting("Brightness", 3f, 0.5f, 3.0f).increment(0.1f);
    private final CheckBox pulse = checkbox("Pulse", true);
    private final SliderSetting pulseSpeed = sliderSetting("Pulse speed", 2.0f, 0.5f, 5.0f).increment(0.1f).visible(pulse::get);

    public enum Mode { Default, Nur, Delta, Test, Image2 }

    private static final Identifier IMAGE_TEXTURE = Identifier.of("topchdlc", "images/world/targetesp/target.png");
    private static final Identifier IMAGE_TEXTURE2 = Identifier.of("topchdlc", "images/world/targetesp/targetn.png");
    private static final Identifier IMAGE_NURICK = Identifier.of("topchdlc", "images/world/targetesp/nur.png");
    private static final Identifier IMAGE_DElLTA = Identifier.of("topchdlc", "images/world/targetesp/target1.png");
    private static final Identifier IMAGE_TEST = Identifier.of("topchdlc", "images/world/targetesp/aimbot.png");

    public ImageTargetRenderer() {
        super("Image");
    }

    @Override
    public void render(MatrixStack stack, VertexConsumerProvider consumers, LivingEntity target, float alpha) {
        setupRenderStates();
        Identifier texture = getTexture(crystalMode.get());
        VertexConsumer consumer = consumers.getBuffer(ClientPipelines.TARGET_ESP.apply(texture));
        Quaternionf cameraRotation = mc.gameRenderer.getCamera().getRotation();
        float baseSize = 0.7f + (1f - alpha) * 0.5f;
        float pulseValue = pulse.get() ? 0.85f + 0.15f * (float) Math.sin(System.currentTimeMillis() / 1000.0 * pulseSpeed.get() * Math.PI) : 1.0f;
        float rotAngle = (float) Math.sin(System.currentTimeMillis() / 1000d) * speed.get();

        Color color1 = useCustomColor.get() ? customColor.get() : ClientSettings.INSTANCE.getColor(0);
        Color color2 = useCustomColor.get() ? customColor.get() : ClientSettings.INSTANCE.getColor(90);
        color1 = ColorUtility.injectAlpha(boostBrightness(color1, brightness.get()), (int) (alpha * 255));
        color2 = ColorUtility.injectAlpha(boostBrightness(color2, brightness.get()), (int) (alpha * 255));

        int passes = dreamcore.get() ? 2 : 1;
        for (int pass = 0; pass < passes; pass++) {
            renderPass(stack, consumer, target, color1, color2, cameraRotation, rotAngle, baseSize, pulseValue, pass);
        }

        resetRenderStates();
    }

    private void renderPass(MatrixStack stack, VertexConsumer consumer, LivingEntity target, Color c1, Color c2,
                            Quaternionf camRot, float rot, float baseSize, float pulse, int pass) {
        float passAlpha = pass == 0 ? 1.0f : 0.5f;
        float passSize = baseSize * (pass == 0 ? 1.0f : 1.05f) * pulse;

        Color pc1 = ColorUtility.injectAlpha(c1, (int) (c1.getAlpha() * passAlpha));
        Color pc2 = ColorUtility.injectAlpha(c2, (int) (c2.getAlpha() * passAlpha));

        stack.push();
        stack.translate(0, target.getHeight() / 2f, 0);
        stack.multiply(camRot);
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rot));
        stack.scale(passSize, passSize, 1);

        var matrix = stack.peek().getPositionMatrix();
        consumer.vertex(matrix, -1, -1, 0).color(pc2.getRGB()).texture(0, 0);
        consumer.vertex(matrix, -1, 1, 0).color(pc1.getRGB()).texture(0, 1);
        consumer.vertex(matrix, 1, 1, 0).color(pc2.getRGB()).texture(1, 1);
        consumer.vertex(matrix, 1, -1, 0).color(pc1.getRGB()).texture(1, 0);

        stack.pop();
    }

    private Identifier getTexture(Mode mode) {
        return switch (mode) {
            case Default -> IMAGE_TEXTURE;
            case Nur -> IMAGE_NURICK;
            case Delta -> IMAGE_DElLTA;
            case Test -> IMAGE_TEST;
            case Image2 -> IMAGE_TEXTURE2;
        };
    }

    private void setupRenderStates() {
        GlStateManager._depthMask(false);
        GlStateManager._disableCull();
        
        if (dreamcore.get()) {
            GlStateManager._blendFuncSeparate(GlConst.GL_SRC_ALPHA, GlConst.GL_ONE, GlConst.GL_ZERO, GlConst.GL_ONE);
        }
        GlStateManager._enableBlend();
    }

    private void resetRenderStates() {
        GlStateManager._depthMask(true);
        if (dreamcore.get()) {
            GlStateManager._blendFuncSeparate(GlConst.GL_ONE, GlConst.GL_ZERO, GlConst.GL_ONE, GlConst.GL_ZERO);
        }
        GlStateManager._disableBlend();
        
        GlStateManager._enableCull();
    }

    private Color boostBrightness(Color color, float factor) {
        int r = Math.min(255, (int) (color.getRed() * factor));
        int g = Math.min(255, (int) (color.getGreen() * factor));
        int b = Math.min(255, (int) (color.getBlue() * factor));
        return new Color(r, g, b, color.getAlpha());
    }
}