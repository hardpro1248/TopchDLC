package gg.topchdlc.vse.shutki.screen.hud.ui.impl.main;

import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;

public class InventoryHudElement extends HudElement {

    private final EnumSetting<Style> style = settings.enumSetting("Стиль", Style.GLASS);

    private static final int COLS = 9;
    private static final int ROWS = 3;

    private static final float HEIGHT      = 15.5f;
    private static final float SLOT_W      = 18.0f;
    private static final float SPACING     = 1.5f;
    private static final float TEXT_SIZE   = 7.0f;
    private static final float COUNT_SIZE  = 6.5f;

    private static final float TOTAL_W = COLS * SLOT_W;
    private static final float TOTAL_H = (ROWS + 1) * HEIGHT + (ROWS * SPACING);

    public InventoryHudElement(Drag drag) {
        super("InventoryHud", drag);
        if (drag != null) {
            drag.width = TOTAL_W;
            drag.height = TOTAL_H;
        }
    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (mc.player == null) return;

        float alpha = 1.0f;
        float currentY = y;

        drawStyle(x, currentY, TOTAL_W, HEIGHT, alpha);

        Client.RENDERER.text(IconUse.MICROSOFT, x + 5f, currentY + 3.5f, TextureUse.ICONS, TEXT_SIZE, Color.WHITE);
        Client.RENDERER.rect(x + 18f, currentY + 3.5f, 0.5f, HEIGHT - 7f, new Vector4f(0), 1, new Color(255,255,255,40), new Color(255,255,255,40), new Color(255,255,255,40), new Color(255,255,255,40));
        Client.RENDERER.text("Inventory", x + 23f, currentY + 4f, TextureUse.SFMEDIUM, TEXT_SIZE, Color.WHITE);

        currentY += HEIGHT + SPACING;

        for (int row = 0; row < ROWS; row++) {
            drawStyle(x, currentY, TOTAL_W, HEIGHT, alpha);

            for (int col = 1; col < COLS; col++) {
                float lineX = x + col * SLOT_W;
                Color lineCol = new Color(255, 255, 255, (int)(20 * alpha));
                Client.RENDERER.rect(lineX, currentY + 3f, 0.5f, HEIGHT - 6f, new Vector4f(0), 1, lineCol, lineCol, lineCol, lineCol);
            }

            renderInventoryRow(row, currentY, alpha);

            currentY += HEIGHT + SPACING;
        }

        if (drag != null) {
            drag.width = width = TOTAL_W;
            drag.height = height = TOTAL_H;
        }
    }

    private void renderInventoryRow(int row, float rowY, float alpha) {
        DrawContext context = Client.RENDERER.getDrawContext();
        if (context == null) return;

        for (int col = 0; col < COLS; col++) {
            int slotIndex = 9 + row * COLS + col;
            ItemStack stack = mc.player.getInventory().getStack(slotIndex);

            if (!stack.isEmpty()) {
                float itemX = x + col * SLOT_W + (SLOT_W / 2f - 7f);
                float itemY = rowY + (HEIGHT / 2f - 7f);

                int count = stack.getCount();
                String countText = count > 1 ? String.valueOf(count) : "";

                Client.RENDERER.queueTask(() -> {
                    var matrices = context.getMatrices();
                    matrices.pushMatrix();
                    matrices.translate(itemX, itemY);
                    matrices.scale(14f / 16f, 14f / 16f);
                    context.drawItem(stack, 0, 0);
                    matrices.popMatrix();

                    if (!countText.isEmpty()) {
                        float tw = Client.RENDERER.textWidth(countText, TextureUse.SFMEDIUM, COUNT_SIZE);
                        float tx = itemX + 14f - tw;
                        float ty = itemY + 8f;

                        Client.RENDERER.getStack().push();

                        Client.RENDERER.getStack().peek().getPositionMatrix().identity();

                        Client.RENDERER.getCrenderSystem().layer(CRenderSystem.RenderLayer.OVERLAY);
                        Client.RENDERER.getCrenderSystem().z(0.01f);

                        Client.RENDERER.text(countText, tx, ty, TextureUse.SFMEDIUM, COUNT_SIZE,
                                new Color(255, 255, 255, (int)(255 * alpha)));

                        Client.RENDERER.getStack().pop();
                    }
                });
            }
        }
    }

    private void drawStyle(float rx, float ry, float rw, float rh, float alpha) {
        Vector4f round = new Vector4f(4f);
        Client.RENDERER.blur(rx, ry, rw, rh, round, 15f, alpha);
        if (style.get() == Style.DARK) {
            Color bg = new Color(20, 20, 25, (int) (180 * alpha));
            Client.RENDERER.rect(rx, ry, rw, rh, round, 1, bg, bg, bg, bg);
            Color out = new Color(255, 255, 255, (int) (15 * alpha));
            Client.RENDERER.outline(rx, ry, rw, rh, 0.4f, round, new Vector2f(1), out, out, out, out);
        } else {
            Color bg = new Color(62, 62, 71, 0);
            Client.RENDERER.rect(rx, ry, rw, rh, round, 1, bg, bg, bg, bg);
            int outAlpha = MathHelper.clamp((int) (25 * alpha), 0, 255);
            Color outColor = new Color(255, 255, 255, outAlpha);
            Client.RENDERER.outline(rx, ry, rw, rh, 0.4f, round, new Vector2f(1), outColor, outColor, outColor, outColor);
        }
    }

    @AllArgsConstructor
    enum Style implements EnumChoice {
        DARK("Темный"), GLASS("Стекло");
        @Getter final String renderName;
    }
}