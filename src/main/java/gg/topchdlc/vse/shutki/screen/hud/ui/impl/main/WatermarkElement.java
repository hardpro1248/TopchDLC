package gg.topchdlc.vse.shutki.screen.hud.ui.impl.main;

import cc.snais.Info;
import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;

import java.awt.*;

public class WatermarkElement extends HudElement implements MinecraftHolder {

    private static final Identifier GUI_IMAGE = Identifier.of("topchdlc", "images/ui/gui.png");

    private final EnumSetting<Style> style = settings.enumSetting("Стиль", Style.GLASS);

    private static final float AVATAR_SIZE = 28.5f;
    private static final float ROW_HEIGHT  = 13.5f;
    private static final float SPACING     = 1.5f;
    private static final float TEXT_SIZE   = 7.0f;
    private static final float PADDING     = 5.0f;

    public WatermarkElement(Drag drag) {
        super("Watermark", drag);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (mc.player == null) return;

        String clientName = Info.Client;
        String userName = mc.getSession().getUsername();
        String serverIp = Info.getServ();

        float clientW = Client.RENDERER.textWidth(clientName, TextureUse.SFMEDIUM, TEXT_SIZE + 0.5f) + PADDING * 2;
        float userW = Client.RENDERER.textWidth(userName, TextureUse.SFMEDIUM, TEXT_SIZE) + PADDING * 2;
        float serverW = Client.RENDERER.textWidth(serverIp, TextureUse.SFMEDIUM, TEXT_SIZE) + PADDING * 2;

        float rightPartW = Math.max(clientW, userW + SPACING + serverW);
        float totalWidth = AVATAR_SIZE + SPACING + rightPartW;

        drawStyle(x, y, AVATAR_SIZE, AVATAR_SIZE, 1f);
        Client.RENDERER.textureRaw(GUI_IMAGE, x + 2.5f, y + 2.5f, AVATAR_SIZE - 5f, AVATAR_SIZE - 5f, 1f, new Vector4f(4f), Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);

        float rightX = x + AVATAR_SIZE + SPACING;
        drawStyle(rightX, y, clientW, ROW_HEIGHT, 1f);
        Client.RENDERER.text(clientName, rightX + PADDING, y + (ROW_HEIGHT / 2f - (TEXT_SIZE + 0.5f) / 2f) - 0.3f,
                TextureUse.SFMEDIUM, TEXT_SIZE + 0.5f, Color.WHITE);

        float bottomY = y + ROW_HEIGHT + SPACING;

        drawStyle(rightX, bottomY, userW, ROW_HEIGHT, 1f);
        Client.RENDERER.text(userName, rightX + PADDING, bottomY + (ROW_HEIGHT / 2f - TEXT_SIZE / 2f) - 0.3f,
                TextureUse.SFMEDIUM, TEXT_SIZE, Color.WHITE);

        float serverX = rightX + userW + SPACING;
        drawStyle(serverX, bottomY, serverW, ROW_HEIGHT, 1f);
        Client.RENDERER.text(serverIp, serverX + PADDING, bottomY + (ROW_HEIGHT / 2f - TEXT_SIZE / 2f) - 0.3f,
                TextureUse.SFMEDIUM, TEXT_SIZE, Color.WHITE);

        this.drag.width = width = totalWidth;
        this.drag.height = height = (bottomY + ROW_HEIGHT) - y;
    }

    private void drawStyle(float rx, float ry, float rw, float rh, float alpha) {
        Vector4f round = new Vector4f(4.5f);
        Client.RENDERER.blur(rx, ry, rw, rh, round, 15f, alpha);

        if (style.get() == Style.DARK) {
            Color bg = new Color(20, 20, 25, (int) (180 * alpha));
            Client.RENDERER.rect(rx, ry, rw, rh, round, 1, bg, bg, bg, bg);
            Color out = new Color(255, 255, 255, (int) (15 * alpha));
            Client.RENDERER.outline(rx, ry, rw, rh, 0.4f, round, new org.joml.Vector2f(1), out, out, out, out);
        } else {
            Color bg = new Color(62, 62, 71, 0);
            Client.RENDERER.rect(rx, ry, rw, rh, round, 1, bg, bg, bg, bg);
            int outAlpha = MathHelper.clamp((int) (25 * alpha), 0, 255);
            Color out = new Color(255, 255, 255, outAlpha);
            Client.RENDERER.outline(rx, ry, rw, rh, 0.4f, round, new org.joml.Vector2f(1), out, out, out, out);
        }
    }

    @AllArgsConstructor
    enum Style implements EnumChoice {
        DARK("Темный"), GLASS("Стекло");
        @Getter final String renderName;
    }
}