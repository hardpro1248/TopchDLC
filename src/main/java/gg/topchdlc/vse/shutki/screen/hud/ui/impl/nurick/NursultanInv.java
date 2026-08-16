package gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick;

import gg.topchdlc.vse.shutki.module.modules.impl.render.Interface;
import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event2D;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick.icon.NurickIcons;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import net.minecraft.client.gui.DrawContext;
import org.joml.Vector2f;
import org.joml.Vector4f;
import gg.topchdlc.vse.utils.client.client.ClientSettings;

import java.awt.*;

public class NursultanInv extends HudElement {

    private static final int COLS = 9;
    private static final int ROWS = 3;
    private static final float SLOT_SIZE = 14.3f;
    private static final float SLOT_GAP = 0.5f;
    private static final float PAD_X = 5f;
    private static final float PAD_Y = 5f;
    private static final float ICON_S = 7.7f;
    private static final float HEADER_H = 13f;
    private static final float ROUND = 7f;
    private static final float SCALE = 0.7f;
    private static final float SMOOTH = 1f;
    private static final float CONTENT_W = COLS * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP;
    private static final float TOTAL_W = CONTENT_W + PAD_X * 2f -8;
    private static final float TOTAL_H = HEADER_H + PAD_Y
            + ROWS * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP
            + PAD_Y - 10;

    public NursultanInv(Drag drag) {
        super("NursultanInv", drag);
        Client.EVENTS.register(this);
        if (drag != null) {
            drag.width = TOTAL_W;
            drag.height = TOTAL_H;
        }
    }

    EventBus<Event> on2D = event -> {
        if (event instanceof Event2D) {
            onDraw2D();
        }
    };

    @Override
    public void render(int mouseX, int mouseY) {
        if (drag != null) {
            drag.width = TOTAL_W;
            drag.height = TOTAL_H;
        }
        this.width = TOTAL_W;
        this.height = TOTAL_H;
    }

    private void onDraw2D() {
        if (!isEnabled() || !Interface.INSTANCE.isEnabled() || mc.player == null) return;
        DrawContext context = Client.RENDERER.getDrawContext();
        if (context == null) return;
        float wx = x;
        float wy = y;
        float ww = TOTAL_W;
        float wh = TOTAL_H;
        Client.RENDERER.blur(wx, wy, ww, wh, new Vector4f(ROUND), 16f, SMOOTH);
        Color bg = new Color(8, 8, 12, 185);
        Client.RENDERER.rect(wx, wy, ww, wh, new Vector4f(ROUND), SMOOTH, bg, bg, bg, bg);
        Color outline = new Color(255, 255, 255, 18);
        Client.RENDERER.outline(wx, wy, ww, wh, 0.5f, new Vector4f(ROUND), new Vector2f(1), outline, outline, outline, outline);
        Vector4f headerRound = new Vector4f(0, ROUND, ROUND, 0);
        Client.RENDERER.blur(wx, wy, ww, HEADER_H, headerRound, 20f, SMOOTH);
        Color headerBg = new Color(0, 0, 0, 18);
        Client.RENDERER.rect(wx, wy, ww, HEADER_H, headerRound, SMOOTH, headerBg, headerBg, headerBg, headerBg);
        Color headerOutline = new Color(255, 255, 255, 12);
      //  Client.RENDERER.outline(wx, wy, ww, HEADER_H, 0.5f, headerRound, new Vector2f(1), headerOutline, headerOutline, headerOutline, headerOutline);
        Client.RENDERER.text(NurickIcons.DUCK,wx + 3,wy  + HEADER_H  -11,TextureUse.ICONS_NURIK,ICON_S, ClientSettings.INSTANCE.getColor(0));
        Client.RENDERER.textCentered("Inventory",
                wx + ww / 2f,
                wy + (HEADER_H - Client.RENDERER.textHeight(TextureUse.SFMEDIUM, 6f)) / 2f,
                TextureUse.SFMEDIUM, 6f, ClientColors.FORE_COLOR);
        float startX = wx + PAD_X;
        float startY = wy + HEADER_H + PAD_Y;
      //  Color slotBg = new Color(255, 255, 255, 10);
        //for (int row = 0; row < ROWS; row++) {
       //     for (int col = 0; col < COLS; col++) {
       //         float sx = startX + col * (SLOT_SIZE + SLOT_GAP);
          //      float sy = startY + row * (SLOT_SIZE + SLOT_GAP);
               // Client.RENDERER.rect(sx, sy, SLOT_SIZE, SLOT_SIZE, new Vector4f(3), SMOOTH, slotBg, slotBg, slotBg, slotBg);
        //    }
     //   }

        final float fStartX = startX;
        final float fStartY = startY;
        Client.RENDERER.queueTask(() -> {
            if (mc.player == null) return;
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    int slot = 9 + row * COLS + col;
                    var stack = mc.player.getInventory().getStack(slot);
                    if (stack == null || stack.isEmpty()) continue;

                    float sx = fStartX + col * (SLOT_SIZE + SLOT_GAP);
                    float sy = fStartY + row * (SLOT_SIZE + SLOT_GAP);
                    float itemX = sx + (SLOT_SIZE - 16f) / 2f;
                    float itemY = sy + (SLOT_SIZE - 24f) / 2f;

                    var matrices = context.getMatrices();
                    matrices.pushMatrix();
                    matrices.translate(itemX, itemY);
                    matrices.scale(SCALE, SCALE);
                    context.drawItem(mc.player, stack, 0, 0, slot);
                    context.drawStackOverlay(mc.textRenderer, stack, 0, 0);
                    matrices.popMatrix();
                }
            }
        });
    }
}
