package gg.topchdlc.vse.shutki.screen.hud.ui.impl.main;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;

import java.awt.*;

public class MinecraftUI extends HudElement {
    public static final MinecraftUI INSTANCE = new MinecraftUI();
    private MinecraftUI() {
        super("Minecraft UI", null);
    }

    @Override
    public void render(int mouseX, int mouseY) {}

    static final float round = 13, smooth = 1, thickness = 1, roundSmall = 7, alpha = 200;
    private static void renderLiquidGlassBase(float x, float y, float width, float height, Vector4f radius, boolean hover, boolean enabled) {
        float time = (System.currentTimeMillis() % 10000) / 10000f;
        float phase = (time * 1.2f) % 1f;
        Client.RENDERER.blur(x, y, width, height, radius, 25f, 1f);
        int bgAlpha = enabled ? 45 : 60;
        Color bg = new Color(12, 12, 16, bgAlpha);
        Client.RENDERER.rect(x, y, width, height, radius, smooth, bg, bg, bg, bg);
        int innerAlpha = 10 + (int) (5 * Math.sin(phase * Math.PI * 2));
        Color innerGlow = new Color(180, 195, 220, innerAlpha);
        Client.RENDERER.glow(x, y, width, height, radius, 5f,
                innerGlow, innerGlow, innerGlow, innerGlow);
        int edgeAlpha = 8 + (int) (4 * Math.sin(phase * Math.PI * 3));
        Color edgeColor = new Color(200, 210, 230, edgeAlpha);
        Vector4f innerRadius = shrink(radius, 1f);
        Client.RENDERER.glow(x + 1f, y + 1f, width - 2f, height - 2f,
                innerRadius, 2.5f, edgeColor, edgeColor, edgeColor, edgeColor);
        if (hover && enabled) {
            int hoverGlowAlpha = 30 + (int) (15 * Math.sin(phase * Math.PI * 2));
            Color hoverGlow = new Color(160, 190, 255, hoverGlowAlpha);
            Client.RENDERER.glow(x, y, width, height, radius, 10f,
                    hoverGlow, hoverGlow, hoverGlow, hoverGlow);
            Color hoverFill = new Color(180, 200, 255, 18);
            Client.RENDERER.rect(x, y, width, height, radius, smooth,
                    hoverFill, hoverFill, hoverFill, hoverFill);
            int hoverEdgeAlpha = 35 + (int) (15 * Math.sin(phase * Math.PI * 2.5));
            Color hoverEdge = new Color(200, 220, 255, hoverEdgeAlpha);
            Client.RENDERER.glow(x + 0.5f, y + 0.5f, width - 1f, height - 1f,
                    shrink(radius, 0.5f), 2f,
                    hoverEdge, hoverEdge, hoverEdge, hoverEdge);
        }
        if (!enabled) {
            Color disabledOverlay = new Color(0, 0, 0, 40);
            Client.RENDERER.rect(x, y, width, height, radius, smooth,
                    disabledOverlay, disabledOverlay, disabledOverlay, disabledOverlay);
        }
    }
    private static Vector4f shrink(Vector4f v, float a) {
        return new Vector4f(
                Math.max(0, v.x - a), Math.max(0, v.y - a),
                Math.max(0, v.z - a), Math.max(0, v.w - a));
    }
    public static void renderButton(float x, float y, float width, float height, boolean enabled, boolean hover, Text message) {
        CRenderSystem.RenderLayer layer = Client.RENDERER.getCrenderSystem().layer();
        Client.RENDERER.getCrenderSystem().layerTex(
                Client.RENDERER.getCrenderSystem().getAtlas().getGlId()
        ).layer(CRenderSystem.RenderLayer.OVERLAY);
        Client.RENDERER.getCrenderSystem().push(x, y, width, height);
        Vector4f radius = new Vector4f(round);
        renderLiquidGlassBase(x, y, width, height, radius, hover, enabled);
        if (message != null) {
            float textWidth = Client.RENDERER.textWidth(message.getString(), TextureUse.SFMEDIUM, 9);
            float textHeight = Client.RENDERER.textHeight(TextureUse.SFMEDIUM, 9);

            int textAlpha = enabled ? 240 : 120;
            Color textColor = hover && enabled
                    ? new Color(230, 240, 255, textAlpha)
                    : new Color(210, 210, 215, textAlpha);

            Client.RENDERER.text(message.getString(),
                    x + scrollText(width, textWidth) + width / 2f - Math.min(width, textWidth) / 2f,
                    y + height / 2f - textHeight / 2f,
                    TextureUse.SFMEDIUM, 9f, textColor);
        }

        Client.RENDERER.getCrenderSystem().pop();
        Client.RENDERER.getCrenderSystem().layer(layer);
    }
    public static float scrollText(float width, float textWidth) {
        if (textWidth > width) {
            return MathHelper.clamp(
                    (float) Math.sin(System.currentTimeMillis() / 10000.0) * textWidth,
                    -textWidth * 0.8f, 10);
        }
        return 0;
    }
    public static void renderSlider(float x, float y, float width, float height, boolean enabled, boolean hover, double value, Text message) {
        CRenderSystem.RenderLayer layer = Client.RENDERER.getCrenderSystem().layer();
        Client.RENDERER.getCrenderSystem().layer(CRenderSystem.RenderLayer.OVERLAY);
        Vector4f radius = new Vector4f(round);
        renderLiquidGlassBase(x, y, width, height, radius, hover, enabled);
        float thumbSize = 8f;
        float thumbX = x + (float) value * (width - thumbSize);
        float thumbY = y + height / 2f - thumbSize / 2f;
        Vector4f thumbRadius = new Vector4f(roundSmall);
        Client.RENDERER.blur(thumbX, thumbY, thumbSize, thumbSize, thumbRadius, 15f, 1f);
        Color thumbBg = new Color(180, 195, 220, 80);
        Client.RENDERER.rect(thumbX, thumbY, thumbSize, thumbSize, thumbRadius, 1,
                thumbBg, thumbBg, thumbBg, thumbBg);
        int thumbGlowAlpha = hover ? 40 : 20;
        Color thumbGlow = new Color(170, 195, 240, thumbGlowAlpha);
        Client.RENDERER.glow(thumbX, thumbY, thumbSize, thumbSize, thumbRadius, 4f,
                thumbGlow, thumbGlow, thumbGlow, thumbGlow);
        if (message != null) {
            float textWidth = Client.RENDERER.textWidth(message.getString(), TextureUse.SFMEDIUM, 9);
            float textHeight = Client.RENDERER.textHeight(TextureUse.SFMEDIUM, 9);
            int textAlpha = enabled ? 240 : 120;
            Color textColor = new Color(210, 210, 215, textAlpha);
            Client.RENDERER.text(message.getString(),
                    x + scrollText(width, textWidth) + width / 2f - Math.min(width, textWidth) / 2f,
                    y + height / 2f - textHeight / 2f,
                    TextureUse.SFMEDIUM, 9f, textColor);
        }
        Client.RENDERER.getCrenderSystem().layer(layer);
    }
}