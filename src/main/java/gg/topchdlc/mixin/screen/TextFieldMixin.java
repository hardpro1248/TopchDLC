package gg.topchdlc.mixin.screen;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.topchdlc.vse.shutki.module.modules.impl.render.BetterMinecraft;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(TextFieldWidget.class)
public abstract class TextFieldMixin {

    @Shadow abstract String getText();
    @Shadow private int firstCharacterIndex;

    @Unique private String vitalya$prevText = "";
    @Unique private final Map<Integer, Long> vitalya$charTimestamps = new HashMap<>();

    @Inject(method = "renderWidget", at = @At("HEAD"))
    private void vitalya$onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!BetterMinecraft.INSTANCE.isEnabled() || !BetterMinecraft.INSTANCE.chatCharAnim.get()) return;

        String currentText = this.getText();
        if (!currentText.equals(vitalya$prevText)) {
            long now = System.currentTimeMillis();
            for (int i = 0; i < currentText.length(); i++) {
                if (i >= vitalya$prevText.length() || currentText.charAt(i) != vitalya$prevText.charAt(i)) {
                    vitalya$charTimestamps.put(i, now);
                }
            }
            vitalya$prevText = currentText;
        }
    }

    @WrapOperation(
            method = "renderWidget",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)V"
            )
    )
    private void vitalya$wrapDrawText(DrawContext context, TextRenderer textRenderer, OrderedText orderedText, int x, int y, int color, boolean shadow, Operation<Void> original) {
        if (!BetterMinecraft.INSTANCE.isEnabled() || !BetterMinecraft.INSTANCE.chatCharAnim.get()) {
            original.call(context, textRenderer, orderedText, x, y, color, shadow);
            return;
        }

        vitalya$renderAnimatedOrderedText(context, textRenderer, orderedText, x, y, color, shadow, original);
    }

    @Unique
    private void vitalya$renderAnimatedOrderedText(DrawContext context, TextRenderer textRenderer, OrderedText orderedText, int x, int y, int color, boolean shadow, Operation<Void> original) {
        List<CharInfo> chars = new ArrayList<>();
        orderedText.accept((charIndex, style, codePoint) -> {
            chars.add(new CharInfo(charIndex, style, codePoint));
            return true;
        });

        int currentX = x;
        long now = System.currentTimeMillis();
        float animDuration = BetterMinecraft.INSTANCE.animSpeed.get();
        int fontHeight = textRenderer.fontHeight;

        for (CharInfo ci : chars) {
            String charStr = new String(Character.toChars(ci.codePoint));
            int charWidth = textRenderer.getWidth(charStr);
            int globalIndex = this.firstCharacterIndex + ci.charIndex;

            Long spawnTime = vitalya$charTimestamps.get(globalIndex);
            float progress = spawnTime == null ? 1.0f : Math.min(1.0f, (now - spawnTime) / animDuration);

            OrderedText singleCharText = OrderedText.styledBackwardsVisitedString(charStr, ci.style);

            if (progress < 1.0f) {
                float scale = vitalya$easeOutBack(progress);

                float centerX = currentX + charWidth / 2.0f;
                float centerY = y + fontHeight / 2.0f;

                Matrix3x2fStack matrices = context.getMatrices();
                matrices.pushMatrix();

                matrices.translate(centerX, centerY);
                matrices.scale(scale, scale);
                matrices.translate(-centerX, -centerY);

                original.call(context, textRenderer, singleCharText, currentX, y, color, shadow);

                matrices.popMatrix();
            } else {
                original.call(context, textRenderer, singleCharText, currentX, y, color, shadow);
            }

            currentX += charWidth;
        }
    }

    @Unique
    private static float vitalya$easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }

    @Unique
    private static class CharInfo {
        final int charIndex;
        final Style style;
        final int codePoint;

        CharInfo(int charIndex, Style style, int codePoint) {
            this.charIndex = charIndex;
            this.style = style;
            this.codePoint = codePoint;
        }
    }
}