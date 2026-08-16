package gg.topchdlc.vse.shutki.screen.hud.ui.impl.main;

import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import gg.topchdlc.vse.utils.client.mixin.IStatusEffectInstance;
import gg.topchdlc.vse.utils.client.text.TextUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EffectsElement extends HudElement {
    public static final EffectsElement INSTANCE = new EffectsElement(new Drag("Effects Widget", () -> true).bound(20, 20, 100, 100));

    private final EnumSetting<Style> style = settings.enumSetting("Стиль", Style.GLASS);

    private static final float HEIGHT = 14.5f;
    private static final float PADDING_SIDE = 4f;
    private static final float SPACING = 0.5f;
    private static final float TEXT_SIZE = 7.0f;
    private static final float ICON_SIZE = 9.0f;

    private final List<StatusEffectInstance> activeEffects = new ArrayList<>();

    private EffectsElement(Drag drag) {
        super("Effects Widget", drag);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        Collection<StatusEffectInstance> mcEffects = mc.player.getStatusEffects();
        for (StatusEffectInstance effect : mcEffects) {
            if (!activeEffects.contains(effect)) activeEffects.add(effect);
        }
        activeEffects.removeIf(e -> !mcEffects.contains(e) && ((IStatusEffectInstance) e).getLerp() <= 0.01f);

        if (activeEffects.isEmpty() && !(mc.currentScreen instanceof ChatScreen)) return;

        boolean isRightSide = (x + width / 2f) > (mc.getWindow().getScaledWidth() / 2f);
        float currentY = y;

        renderPotionsHeader(currentY, isRightSide);
        currentY += HEIGHT + 4f;

        float maxRowWidth = 80;
        List<StatusEffectInstance> sorted = activeEffects.stream()
                .sorted(Comparator.comparingInt(StatusEffectInstance::getDuration).reversed())
                .collect(Collectors.toList());

        for (StatusEffectInstance effect : sorted) {
            IStatusEffectInstance lerpEffect = (IStatusEffectInstance) effect;
            lerpEffect.setLerp(MathUtility.linearFps(lerpEffect.getLerp(), mcEffects.contains(effect) ? 1f : 0f, 10));
            float alpha = MathHelper.clamp(lerpEffect.getLerp(), 0f, 1f);
            if (alpha <= 0.01f) continue;

            String name = Text.translatable(effect.getTranslationKey()).getString();
            if (effect.getAmplifier() > 0) name += " " + (effect.getAmplifier() + 1);
            String time = TextUtility.ticksToTime(effect.getDuration());

            float nameW = Client.RENDERER.textWidth(name, TextureUse.SFMEDIUM, TEXT_SIZE) + (PADDING_SIDE * 2);
            float timeW = Client.RENDERER.textWidth(time, TextureUse.SFMEDIUM, TEXT_SIZE) + (PADDING_SIDE * 2);

            float totalW = HEIGHT + SPACING + nameW + SPACING + timeW;
            maxRowWidth = Math.max(maxRowWidth, totalW);

            float rowX = isRightSide ? (x + width - totalW) : x;
            renderEffectRow(rowX, currentY, nameW, timeW, alpha, effect, name, time, isRightSide);

            currentY += (HEIGHT + 2) * alpha;
        }

        this.drag.width = width = maxRowWidth;
        this.drag.height = height = (currentY - y);
    }

    private void renderPotionsHeader(float ry, boolean right) {
        String title = "Potions";
        float iconW = Client.RENDERER.textWidth(IconUse.POTION.glyph, TextureUse.ICONS, ICON_SIZE);
        float textW = Client.RENDERER.textWidth(title, TextureUse.SFMEDIUM, TEXT_SIZE);
        float headerW = PADDING_SIDE + iconW + SPACING + 0.5f + SPACING + textW + PADDING_SIDE;

        float hx = right ? (x + width - headerW) : x;

        drawStyle(hx, ry+3, headerW, HEIGHT, 1f);

        float iconX = hx + PADDING_SIDE ;
        float lineX = iconX + iconW + SPACING;
        float textX = lineX + 0.5f + SPACING;

        Client.RENDERER.text(IconUse.POTION.glyph, iconX -2, ry + (HEIGHT/2f) - (ICON_SIZE/2f) +3 , TextureUse.ICONS, ICON_SIZE -1.2f, Color.WHITE);
        Client.RENDERER.rect(lineX, ry + 3f, 0.5f, HEIGHT - 6f, new Vector4f(0), 1, new Color(255,255,255,50), new Color(255,255,255,50), new Color(255,255,255,50), new Color(255,255,255,50));
        Client.RENDERER.text(title, textX, ry + (HEIGHT/2f) - (TEXT_SIZE/2f) +2.7f, TextureUse.SFMEDIUM, TEXT_SIZE, Color.WHITE);
    }

    private void renderEffectRow(float rx, float ry, float nW, float tW, float alpha, StatusEffectInstance effect, String name, String time, boolean right) {
        if (!right) {
            drawStyle(rx, ry, HEIGHT, HEIGHT, alpha);
            drawIcon(effect, rx + (HEIGHT/2f - ICON_SIZE/2f), ry + (HEIGHT/2f - ICON_SIZE/2f), alpha);

            float nameX = rx + HEIGHT + SPACING;
            drawStyle(nameX, ry, nW, HEIGHT, alpha);
            drawCenteredText(name, nameX, ry, nW, alpha);

            float timeX = nameX + nW + SPACING;
            drawStyle(timeX, ry, tW-5, HEIGHT, alpha);
            drawCenteredText(time, timeX-3, ry, tW, alpha);
        } else {
            drawStyle(rx, ry, tW, HEIGHT, alpha);
            drawCenteredText(time, rx, ry, tW, alpha);

            float nameX = rx + tW + SPACING;
            drawStyle(nameX, ry, nW, HEIGHT, alpha);
            drawCenteredText(name, nameX, ry, nW, alpha);

            float iconBoxX = nameX + nW + SPACING;
            drawStyle(iconBoxX, ry, HEIGHT, HEIGHT, alpha);
            drawIcon(effect, (float)Math.floor(iconBoxX + (HEIGHT/2f - ICON_SIZE/2f)), (float)Math.floor(ry + (HEIGHT/2f - ICON_SIZE/2f)), alpha);
        }
    }

    private void drawStyle(float rx, float ry, float rw, float rh, float alpha) {
        Vector4f round = new Vector4f(6f);
        Client.RENDERER.blur(rx, ry, rw, rh, round, 15f, alpha);

        if (style.get() == Style.DARK) {
            Color bgColor = new Color(20, 20, 25, (int) (180 * alpha));
            Client.RENDERER.rect(rx, ry, rw, rh, round, 1, bgColor, bgColor, bgColor, bgColor);

            Color outColor = new Color(255, 255, 255, (int) (15 * alpha));
            Client.RENDERER.outline(rx, ry, rw, rh, 0.4f, round, new Vector2f(1), outColor, outColor, outColor, outColor);
        } else {
            Color bgColor = new Color(62, 62, 71, 0);
            Client.RENDERER.rect(rx, ry, rw, rh, round, 1, bgColor, bgColor, bgColor, bgColor);

            int outAlpha = MathHelper.clamp((int)(25 * alpha), 0, 255);
            Color outColor = new Color(255, 255, 255, outAlpha);
            Client.RENDERER.outline(rx, ry, rw, rh, 0.4f, round, new Vector2f(1), outColor, outColor, outColor, outColor);
        }
    }

    private void drawCenteredText(String text, float tx, float ty, float tw, float alpha) {
        float xOff = tx + (tw / 2f) - (Client.RENDERER.textWidth(text, TextureUse.SFMEDIUM, TEXT_SIZE) / 2f);
        int textAlpha = MathHelper.clamp((int)(255 * alpha), 0, 255);
        Client.RENDERER.text(text, xOff, ty + (HEIGHT / 2f) - (TEXT_SIZE / 2f) - 0.3f, TextureUse.SFMEDIUM, TEXT_SIZE, new Color(255,255,255, textAlpha));
    }

    private void drawIcon(StatusEffectInstance effect, float ix, float iy, float alpha) {
        int iconAlpha = MathHelper.clamp((int)(255 * alpha), 0, 255);
        Color c = new Color(255, 255, 255, iconAlpha);

        var effectID = InGameHud.getEffectTexture(effect.getEffectType());
        Identifier texturePath = effectID.withPrefixedPath("textures/").withSuffixedPath(".png");

        Client.RENDERER.textureRaw(
                texturePath,
                (float)Math.floor(ix+0.74f),
                (float)Math.floor(iy+1),
                (float)Math.floor(ICON_SIZE),
                (float)Math.floor(ICON_SIZE),
                0,
                new Vector4f(0, 0, 1, 1),
                c, c, c, c
        );
    }

    @AllArgsConstructor
    enum Style implements EnumChoice {
        DARK("Темный"), GLASS("Стекло");
        @Getter final String renderName;
    }
}