package gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick;

import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick.icon.NurickIcons;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.MathUtility;
import net.minecraft.client.gui.screen.ChatScreen;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
/**
 * Create by daun kvass
 */
public class NursultanKeybindsElement extends HudElement {
    private static final float FS       = 4.5f;
    private static final float FSHT     = 5.3f;
    private static final float ICON_S   = 6.5f;
    private static final float PILL_H   = 12f;
    private static final float GAP      = 0.5f;
    private static final float PAD      = 3f;
    private static final float MIN_W    = 10f;
    private static final Vector4f ROUND = new Vector4f(5, 5, 5, 5);

    private float widthAnim = -1f;

    public NursultanKeybindsElement(Drag drag) {
        super("NursultanKeybinds", drag);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        List<Module> active = new ArrayList<>();
        for (Module m : Client.MODULES.getModules()) {
            m.keybindAnimation.setDirection(
                    m.isEnabled() && m.getKey() != -1 ? Direction.BACKWARDS : Direction.FORWARDS);
            if (1f - m.keybindAnimation.getOutput() > 0.01f) active.add(m);
        }

        boolean shown = !active.isEmpty() || mc.currentScreen instanceof ChatScreen;
        animation.setDirection(shown ? Direction.BACKWARDS : Direction.FORWARDS);
        float globalAnim = 1f - animation.getOutput();
        if (globalAnim < 0.01f) return;
        float maxW = MIN_W;
        for (Module m : active) {
            float nameW = Client.RENDERER.textWidth(m.getName(), TextureUse.SFMEDIUM, FS);
            float keyW  = Client.RENDERER.textWidth(m.getKeyText(), TextureUse.SFMEDIUM, FS);
            maxW = Math.max(maxW, nameW + 35f + keyW);
        }

        if (widthAnim < 0) widthAnim = maxW;
        widthAnim = MathUtility.linearFps(widthAnim, maxW, 10f);
        float W = widthAnim;
        Color theme = ClientSettings.INSTANCE.getColor(0);
        Color bg = new Color(15, 15, 20, 160);
        Color ol = new Color(255, 255, 255, 8);
        Color white = new Color(255, 255, 255, (int)(globalAnim * 255));
        Color sepCol = new Color(255, 255, 255, 25);
        float vCenter = PILL_H / 2f;
        float textH = Client.FONTS.get(TextureUse.SFMEDIUM).getMetrics().baselineHeight() * FS;
        float iconH = Client.FONTS.get(TextureUse.ICONS_NURIK).getMetrics().baselineHeight() * ICON_S;
        float iconYOff = vCenter - (iconH / 2f) + 0.5f;
        float textYOff = vCenter - (textH / 2f) + 0.0f;
        float curY = y;
        drawPillBase(x, curY, W, PILL_H, bg, ol, globalAnim);
        Client.RENDERER.text(NurickIcons.KEYBOARD, x + PAD, curY + iconYOff, TextureUse.ICONS_NURIK, ICON_S, theme);
        float iconW = Client.RENDERER.textWidth(NurickIcons.KEYBOARD, TextureUse.ICONS_NURIK, ICON_S);
        float headSepX = x + PAD + iconW + 4f;
        float sepH = 8f;
        Client.RENDERER.rect(headSepX, curY + (PILL_H - sepH) / 2f, 0.5f, sepH, new Vector4f(0), 1f, theme, theme, theme, theme);
        Client.RENDERER.text("Hotkeys", headSepX + 1.5f, curY + textYOff -1f, TextureUse.SFMEDIUM, FSHT, white);
        curY += PILL_H + GAP;
        for (Module m : active) {
            float ra = 1f - m.keybindAnimation.getOutput();
            float alphaMult = ra * globalAnim;
            if (alphaMult < 0.01f) continue;
            float rH = PILL_H * ra;
            Color c = new Color(255, 255, 255, (int)(alphaMult * 255));
            Color s = new Color(255, 255, 255, (int)(alphaMult * 25));
            drawPillBase(x, curY, W, rH, bg, ol, alphaMult);
            Client.RENDERER.text(m.getName(), x + PAD + 1f, curY + textYOff, TextureUse.SFMEDIUM, FS, c);
            float keyW = Client.RENDERER.textWidth(m.getKeyText(), TextureUse.SFMEDIUM, FS);
            float keyX = x + W - keyW - PAD - 1f;
            float sepX = keyX - 5f;
            if (ra > 0.8f) {
                Client.RENDERER.rect(sepX, curY + (PILL_H - sepH) / 2f, 0.5f, sepH, new Vector4f(0), 1f, s, s, s, s);
            }
            Client.RENDERER.text(m.getKeyText(), keyX, curY + textYOff, TextureUse.SFMEDIUM, FS, c);

            curY += rH + GAP;
        }

        drag.width  = W;
        drag.height = curY - y;
    }

    private void drawPillBase(float px, float py, float pw, float ph, Color bg, Color ol, float alpha) {
        Client.RENDERER.blur(px, py, pw, ph, ROUND, 10f, 1f);
        int a = (int)(bg.getAlpha() * alpha);
        Color fBg = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), a);
        Client.RENDERER.rect(px, py, pw, ph, ROUND, 1f, fBg, fBg, fBg, fBg);

      //  int oa = (int)(ol.getAlpha() * alpha);
       // Color fOl = new Color(255, 255, 255, oa);
       // Client.RENDERER.outline(px, py, pw, ph, 0.5f, ROUND, new Vector2f(1f), fOl, fOl, fOl, fOl);
    }
}