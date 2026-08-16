package gg.topchdlc.vse.shutki.screen.screens.ingame;

import org.joml.Vector2f;
import org.joml.Vector4f;
import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.ui.UIWidget;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.MathUtility;

import java.awt.*;

public class BindPopupWidget extends UIWidget {

    private final Module module;
    private final GlassClickGui gui;

    private static final float W       = 110f;
    private static final float PAD     = 6f;
    private static final float TITLE_H = 12f;
    private static final float KEY_H   = 16f;
    private static final float SEP_H   = 1f;
    private static final float BTN_H   = 16f;
    private static final float TOTAL_H = PAD + TITLE_H + 4f + KEY_H + 5f + SEP_H + 5f + BTN_H + PAD;
    private static float savedX = -1f;
    private static float savedY = -1f;
    private float px, py;
    private boolean dragging = false;
    private float dragOffX, dragOffY;

    private boolean capturing = false;

    public BindPopupWidget(Module module, GlassClickGui gui, float anchorX, float anchorY) {
        this.module = module;
        this.gui    = gui;
        this.px = savedX >= 0 ? savedX : anchorX;
        this.py = savedY >= 0 ? savedY : anchorY;
        bound(px, py, W, TOTAL_H);
    }

    @Override
    public void opened() {
        animation.reset();
    }

    @Override
    public void render(int mouseX, int mouseY) {
        bound(px, py, W, TOTAL_H);
        Color bg     = new Color(20, 20, 24, 245);
        Color border = new Color(255, 255, 255, 14);
        Client.RENDERER.blur(px, py, W, TOTAL_H, new Vector4f(10), 22f, 1f);
        Client.RENDERER.rect(px, py, W, TOTAL_H, new Vector4f(10), 1, bg, bg, bg, bg);
        Client.RENDERER.outline(px, py, W, TOTAL_H, 0.5f, new Vector4f(10), new Vector2f(1), border, border, border, border);
        float cy = py + PAD;
        float tw = Client.RENDERER.textWidth(module.getName(), TextureUse.SFMEDIUM, 5f);
        Client.RENDERER.text(module.getName(), px + (W - tw) / 2f, cy + 2f, TextureUse.SFMEDIUM, 5f, Color.WHITE);
        cy += TITLE_H + 4f;
        float kbX = px + PAD;
        float kbW = W - PAD * 2f;
        boolean kbHov = MathUtility.mouseIn(kbX, cy, kbW, KEY_H, mouseX, mouseY);
        Color kbBg = capturing
                ? new Color(50, 50, 58, 220)
                : kbHov ? new Color(55, 55, 62, 220) : new Color(42, 42, 48, 220);
        Client.RENDERER.rect(kbX, cy, kbW, KEY_H, new Vector4f(4), 1, kbBg, kbBg, kbBg, kbBg);
        String keyLabel;
        Color keyColor;
        if (capturing) {
            int dots = (int)((System.currentTimeMillis() / 400) % 4);
            keyLabel = ". ".repeat(dots + 1).trim();
            keyColor = new Color(200, 200, 200);
        } else {
            keyLabel = module.getKey() == -1 ? "· · ·" : module.getKeyText();
            keyColor = module.getKey() == -1 ? new Color(140, 140, 140) : Color.WHITE;
        }
        float klw = Client.RENDERER.textWidth(keyLabel, TextureUse.SFMEDIUM, 5.5f);
        Client.RENDERER.text(keyLabel, kbX + (kbW - klw) / 2f, cy + KEY_H / 2f - 3f, TextureUse.SFMEDIUM, 5.5f, keyColor);
        cy += KEY_H + 5f;
        Color sep = new Color(255, 255, 255, 20);
        Client.RENDERER.rect(px + PAD, cy, W - PAD * 2f, SEP_H, new Vector4f(0), 1, sep, sep, sep, sep);
        cy += SEP_H + 5f;
        float btnW   = (W - PAD * 2f - 4f) / 2f;
        float holdX   = px + PAD;
        float toggleX = holdX + btnW + 4f;
        renderModeBtn("Hold",   holdX,   cy, btnW, BTN_H, module.getBindType() == Module.BindType.HOLD,  mouseX, mouseY);
        renderModeBtn("Toggle", toggleX, cy, btnW, BTN_H, module.getBindType() == Module.BindType.PRESS, mouseX, mouseY);
    }

    private void renderModeBtn(String text, float bx, float by, float bw, float bh,
                                boolean selected, int mx, int my) {
        boolean hov = MathUtility.mouseIn(bx, by, bw, bh, mx, my);
        Color accent = ClientSettings.INSTANCE.getColor(0);
        Color bg;
        if (selected) {
            bg = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 200);
        } else {
            bg = hov ? new Color(55, 55, 62, 220) : new Color(42, 42, 48, 220);
        }
        Client.RENDERER.rect(bx, by, bw, bh, new Vector4f(4), 1, bg, bg, bg, bg);
        float tw = Client.RENDERER.textWidth(text, TextureUse.SFMEDIUM, 5f);
        Color tc = selected ? Color.WHITE : new Color(160, 160, 160);
        Client.RENDERER.text(text, bx + (bw - tw) / 2f, by + bh / 2f - 3f, TextureUse.SFMEDIUM, 5f, tc);
    }
    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (capturing) {
            module.setKey(-100 - button);
            capturing = false;
            gui.BINDING = false;
            gui.bindingModule = null;
            return true;
        }

        if (!MathUtility.mouseIn(px, py, W, TOTAL_H, mouseX, mouseY)) {
            close();
            return false;
        }

        float cy = py + PAD + TITLE_H + 4f;
        float kbX = px + PAD;
        float kbW = W - PAD * 2f;
        if (MathUtility.mouseIn(kbX, cy, kbW, KEY_H, mouseX, mouseY)) {
            capturing = true;
            gui.BINDING = true;
            gui.bindingModule = module;
            return true;
        }
        cy += KEY_H + 5f + SEP_H + 5f;
        float btnW   = (W - PAD * 2f - 4f) / 2f;
        float holdX   = px + PAD;
        float toggleX = holdX + btnW + 4f;

        if (MathUtility.mouseIn(holdX, cy, btnW, BTN_H, mouseX, mouseY)) {
            module.setBindType(Module.BindType.HOLD);
            return true;
        }
        if (MathUtility.mouseIn(toggleX, cy, btnW, BTN_H, mouseX, mouseY)) {
            module.setBindType(Module.BindType.PRESS);
            return true;
        }
        if (button == 0 && MathUtility.mouseIn(px, py, W, PAD + TITLE_H, mouseX, mouseY)) {
            dragging = true;
            dragOffX = mouseX - px;
            dragOffY = mouseY - py;
        }
        return true;
    }

    @Override
    public void mouseDragged(int mouseX, int mouseY) {
        if (dragging) {
            px = mouseX - dragOffX;
            py = mouseY - dragOffY;
            savedX = px;
            savedY = py;
            bound(px, py, W, TOTAL_H);
        }
    }

    @Override
    public void release(int button) {
        dragging = false;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturing) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE
                    || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE
                    || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE) {
                module.setKey(-1);
            } else {
                module.setKey(keyCode);
            }
            capturing = false;
            gui.BINDING = false;
            gui.bindingModule = null;
            return;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            close();
        }
    }
    public void mouseButtonBind(int button) {
        if (!capturing) return;
        module.setKey(-100 - button);
        capturing = false;
        close();
    }

    private void close() {
        shouldRemove = true;
        capturing = false;
        gui.BINDING = false;
        gui.bindingModule = null;
    }
}
