package gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick;

import cc.snais.Info;
import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.multienum.MultiEnumSetting;
import gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick.icon.NurickIcons;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.player.MoveUtility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Nullables;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
/**
 * Create by daun kvass
 */
public class NursultanWatermark extends HudElement {
    private final MultiEnumSetting<Element> elements = settings.multiEnumSetting("Elements",
            Element.FPS, Element.COORDS, Element.PING, Element.BPS,Element.IP);

    private static final float TEXT_SIZE = 6f;
    private static final float LOGO_SIZE = 6.5f;
    private static final float PILL_H = 15f;
    private static final float PAD_X = 6f;
    private static final float INNER_GAP = 4f;
    private static final float SEP_GAP = 4f;
    private static final String SEPARATOR = "|";

    private static final Vector4f ROUND = new Vector4f(5.5f, 5.5f, 5.5f, 5.5f);
    private static final Identifier LOGO = Identifier.of("topchdlc", "images/ui/logo.png");
    private static final float LOGO_TEXTURE_SIZE = 7f;

    public NursultanWatermark(Drag drag) {
        super("NursultanWatermark", drag);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (mc.player == null) return;

        Color theme = ClientSettings.INSTANCE.getColor(0);
        Color white = new Color(255, 255, 255, 255);
        Color sep   = new Color(255, 255, 255, 60);
        List<ElementData> data = new ArrayList<>();
        for (Element element : elements.get()) {
            String value = element.getValue.get();
            if (value != null && !value.isEmpty()) {
                data.add(new ElementData(element.icon, value));
            }
        }

        float logoW = LOGO_TEXTURE_SIZE
                + INNER_GAP
                + Client.RENDERER.textWidth("TopchDLC", TextureUse.SFMEDIUM, TEXT_SIZE);
        float sepW = Client.RENDERER.textWidth(SEPARATOR, TextureUse.SFMEDIUM, TEXT_SIZE);
        float contentW = logoW;

        for (int i = 0; i < data.size(); i++) {
            ElementData elementData = data.get(i);
            contentW += SEP_GAP + sepW + SEP_GAP;
            contentW += Client.RENDERER.textWidth(elementData.icon, TextureUse.ICONS_NURIK, LOGO_SIZE)
                    + INNER_GAP
                    + Client.RENDERER.textWidth(elementData.value, TextureUse.SFMEDIUM, TEXT_SIZE);
        }

        float pillW = PAD_X * 2f + contentW;
        Client.RENDERER.blur(x, y, pillW, PILL_H, ROUND, 12f, 1f);
        Color bg = new Color(15, 15, 20, 160);
        Client.RENDERER.rect(x, y, pillW, PILL_H, ROUND, 1f, bg, bg, bg, bg);

        float iconHeight = Client.FONTS.get(TextureUse.ICONS_NURIK).getMetrics().baselineHeight() * LOGO_SIZE;
        float textHeight = Client.FONTS.get(TextureUse.SFMEDIUM).getMetrics().baselineHeight() * TEXT_SIZE;
        float iconY = y + (PILL_H / 2f) - (iconHeight / 2f) + 0.5f;
        float textY = y + (PILL_H / 2f) - (textHeight / 2f);

        float curX = x + PAD_X;
        Client.RENDERER.texture(LOGO, curX, y + (PILL_H / 2f) - (LOGO_TEXTURE_SIZE / 2f), LOGO_TEXTURE_SIZE, LOGO_TEXTURE_SIZE,
                1f, new Vector4f(0), Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
        curX += LOGO_TEXTURE_SIZE + INNER_GAP;
        Client.RENDERER.text("TopchDLC", curX, textY, TextureUse.SFMEDIUM, TEXT_SIZE, white);
        curX += Client.RENDERER.textWidth("TopchDLC", TextureUse.SFMEDIUM, TEXT_SIZE);

        for (ElementData elementData : data) {
            Client.RENDERER.text(SEPARATOR, curX + SEP_GAP, textY, TextureUse.SFMEDIUM, TEXT_SIZE, sep);
            curX += SEP_GAP + sepW + SEP_GAP;
            curX = drawEntry(curX, iconY, textY, elementData.icon, elementData.value, theme, white);
        }

        drag.width = pillW;
        drag.height = PILL_H;
    }

    private float drawEntry(float px, float iconY, float textY, String icon, String text, Color iconCol, Color textCol) {
        float iconW = Client.RENDERER.textWidth(icon, TextureUse.ICONS_NURIK, LOGO_SIZE);
        Client.RENDERER.text(icon, px, iconY, TextureUse.ICONS_NURIK, LOGO_SIZE, iconCol);
        float textX = px + iconW + INNER_GAP;
        Client.RENDERER.text(text, textX, textY, TextureUse.SFMEDIUM, TEXT_SIZE, textCol);
        return textX + Client.RENDERER.textWidth(text, TextureUse.SFMEDIUM, TEXT_SIZE);
    }

    private record ElementData(String icon, String value) {
    }

    @AllArgsConstructor
    private enum Element implements EnumChoice {
        FPS("FPS", NurickIcons.FPS, () -> mc.getCurrentFps() + " FPS"),
        COORDS("Coords", NurickIcons.COMPASS, () -> (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ()),
        PING("Ping", NurickIcons.PING, () -> {
            Integer latency = Nullables.map(mc.getNetworkHandler(),
                    nh -> Nullables.map(nh.getPlayerListEntry(mc.player.getUuid()), PlayerListEntry::getLatency));
            return (latency == null ? "0" : latency) + " Ping";
        }),
        BPS("BPS", NurickIcons.BPS, () -> String.format("%.2f BPS", MoveUtility.getBPS(mc.player))),
        IP("Ip",NurickIcons.SATELLITE, Info::getServ);

        @Getter
        private final String renderName;
        private final String icon;
        private final Supplier<String> getValue;
    }
}
