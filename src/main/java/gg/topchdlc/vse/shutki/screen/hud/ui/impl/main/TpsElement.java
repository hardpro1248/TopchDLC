package gg.topchdlc.vse.shutki.screen.hud.ui.impl.main;

import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.multienum.MultiEnumSetting;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Create by daun kvass
 */
public class TpsElement extends HudElement implements MinecraftHolder {
    private final MultiEnumSetting<Element> elements = settings.multiEnumSetting("Элементы", Element.Tps);
    private final EnumSetting<Style> style = settings.enumSetting("Стиль", Style.GLASS);

    public TpsElement(Drag drag) {
        super("Tps Widget", drag);
    }

    private static final float ICON_SIZE = 8.0f;
    private static final float TEXT_SIZE = 7.0f;
    private static final float HEIGHT = 14.5f;
    private static final float PADDING_SIDE = 5f;
    private static final float SPACING = 3.5f;

    @Override
    public void render(int mouseX, int mouseY) {
        List<ElementData> activeElements = new ArrayList<>();

        for (Element element : elements.get()) {
            String val = element.getValue.get();
            if (val == null) continue;

            float iconW = Client.RENDERER.textWidth(element.icon, TextureUse.ICONS, ICON_SIZE);
            float textW = Client.RENDERER.textWidth(val, TextureUse.SFMEDIUM, TEXT_SIZE);
            float totalW = PADDING_SIDE + iconW + SPACING + 0.5f + SPACING + textW + PADDING_SIDE;

            activeElements.add(new ElementData(element, val, iconW, textW, totalW));
        }

        if (activeElements.isEmpty()) return;

        float currentY = y;
        float maxWidth = 0;

        for (ElementData data : activeElements) {
            float rowX = x;
            float rowW = data.totalWidth;

            drawStyle(rowX, currentY, rowW, HEIGHT, 1.0f);

            float iconX = rowX + PADDING_SIDE;
            float lineX = iconX + data.iconWidth + SPACING;
            float textX = lineX + 0.5f + SPACING;

            float textY = currentY + (HEIGHT / 2f) - (TEXT_SIZE / 2f) - 0.3f;
            float iconY = currentY + (HEIGHT / 2f) - (ICON_SIZE / 2f) - 0.3f;

            Client.RENDERER.text(data.element.icon, iconX, iconY-0.6f, TextureUse.ICONS, ICON_SIZE, Color.WHITE);

            Color lineColor = new Color(255, 255, 255, 50);
            Client.RENDERER.rect(lineX, currentY + 3f, 0.5f, HEIGHT - 6f, new Vector4f(0), 1, lineColor, lineColor, lineColor, lineColor);

            Client.RENDERER.text(data.value, textX, textY, TextureUse.SFMEDIUM, TEXT_SIZE, Color.WHITE);

            currentY += HEIGHT + 2;
            maxWidth = Math.max(maxWidth, rowW);
        }

        this.drag.width = maxWidth;
        this.drag.height = (currentY - y) - 2;
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

            int outAlpha = MathHelper.clamp((int) (25 * alpha), 0, 255);
            Color outColor = new Color(255, 255, 255, outAlpha);
            Client.RENDERER.outline(rx, ry, rw, rh, 0.4f, round, new Vector2f(1), outColor, outColor, outColor, outColor);
        }
    }

    private static class ElementData {
        final Element element;
        final String value;
        final float iconWidth;
        final float textWidth;
        final float totalWidth;

        ElementData(Element element, String value, float iconWidth, float textWidth, float totalWidth) {
            this.element = element;
            this.value = value;
            this.iconWidth = iconWidth;
            this.textWidth = textWidth;
            this.totalWidth = totalWidth;
        }
    }

    @AllArgsConstructor
    enum Style implements EnumChoice {
        DARK("Темный"), GLASS("Стекло");
        @Getter final String renderName;
    }

    @AllArgsConstructor
    enum Element implements EnumChoice {
        Tps("Tps", "O", () -> {
            float value = NetworkUtility.getTpsFactor();
            String tps = String.valueOf(Math.ceil(value * 10F) / 10F);
            if (value == -1) return "unk.";
            if (value >= 19.9) return tps + "*";
            return tps + " t/s";
        });
        @Getter final String renderName;
        final String icon;
        final Supplier<String> getValue;
    }
}