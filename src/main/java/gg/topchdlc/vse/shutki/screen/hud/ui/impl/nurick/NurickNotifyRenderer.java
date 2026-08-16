package gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick;

import net.minecraft.client.gui.DrawContext;
import org.joml.Vector4f;
import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.ShaderUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.api.render.system.sys2d.TextureAtlas;
import gg.topchdlc.vse.shutki.screen.hud.notification.Notify;
import net.minecraft.util.Identifier;
import gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick.icon.NurickIcons;

import java.awt.Color;
/**
 * Create by daun kvass
 */
public class NurickNotifyRenderer {
    private static final float NUR_H      = 14f;
    private static final float NUR_IC_S   = 10f;
    private static final float NUR_FONT_S = 6.5f;
    private static final Vector4f ROUND   = new Vector4f(5f, 5f, 5f, 5f);

    public static void render(Notify notify, float lx, float ly, float alpha) {
        CRenderSystem system = Client.RENDERER.getCrenderSystem();

        String textStr = notify.getText().getString();
        float textW = Client.RENDERER.textWidth(textStr, TextureUse.SFMEDIUM, NUR_FONT_S);

        boolean isItemAction = NurickIcons.isItemAction(notify);
        boolean hasItem = notify.getItem() != null || notify.getItemStack() != null;
        float nWidth;
        if (isItemAction && hasItem) {
            nWidth = 8f + NUR_IC_S + 5f + 0.6f + 5f + textW + 8f;
        } else {
            nWidth = 8f + 8f + textW + 10f;
        }

        float drawX = lx - nWidth / 2f;
        Color bg = new Color(15, 15, 20, 180);
        Client.RENDERER.blur(drawX, ly, nWidth, NUR_H, ROUND, 12f, 1f);
        Client.RENDERER.rect(drawX, ly, nWidth, NUR_H, ROUND, 1f, bg, bg, bg, bg);

        float curX = drawX + 7f;

        if (isItemAction && hasItem) {
            drawItem(curX, ly + (NUR_H - NUR_IC_S) / 2f, NUR_IC_S, system, alpha, notify);
            curX += NUR_IC_S + 5f;
            Color sepCol = new Color(255, 255, 255, 255);
            Client.RENDERER.rect(curX, ly + 5.2f, 0.6f, NUR_H - 9f, new Vector4f(0.5f), 1f, sepCol, sepCol, sepCol, sepCol);
            curX += 5.6f;
            Client.RENDERER.text(notify.getText(), curX, ly + (NUR_H / 2.5f) - 2.0f, TextureUse.SFMEDIUM, NUR_FONT_S);

        } else {
            String glyph = NurickIcons.getForNotify(notify);
            Color iconCol = NurickIcons.getColor(glyph);
            Client.RENDERER.text(glyph, curX, ly + (NUR_H / 2f) - 3.5f, TextureUse.ICONS_NURIK, 7.5f, iconCol);
            curX += 11f;
            Client.RENDERER.text(notify.getText(), curX, ly + (NUR_H / 2.5f) - 2.6f, TextureUse.SFMEDIUM, NUR_FONT_S);
        }
    }

    private static void drawItem(float x, float y, float size, CRenderSystem system, float alpha, Notify notify) {
        if (notify.getItem() != null) {
            Identifier id = Identifier.ofVanilla("textures/item/" + notify.getItem() + ".png");
            if (system.getAtlas().has(id.toString())) {
                TextureAtlas.UV uv = system.getAtlas().getUV(id.toString());
                system.shader(ShaderUse.TEXTURE).alpha(alpha).rect(x, y, size, size)
                        .uv(uv.u0, uv.v0, uv.u1, uv.v1).color(Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE).build();
            } else { system.addPrepare(id); }
        } else if (notify.getItemStack() != null) {
            DrawContext dc = Client.RENDERER.getDrawContext();
            if (dc != null) {
                Client.RENDERER.queueTask(() -> {
                    var m = dc.getMatrices(); m.pushMatrix(); m.translate(x, y);
                    m.scale(size / 16f, size / 16f); dc.drawItem(notify.getItemStack(), 0, 0); m.popMatrix();
                });
            }
        }
    }
}