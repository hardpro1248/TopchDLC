package gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.joml.Vector2f;
import org.joml.Vector4f;
import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;

import java.awt.*;

public class ArmorNurick extends HudElement {

    private static final float SCALE     = 1.1f;
    private static final float ITEM_SIZE = 17.0f;
    private static final float GAP       = 3.5f;
    private static final float BAR_H     = 1.2f;

    private final ColorSetting backgroundColor = settings.colorSetting("Цвет фона", new Color(55, 235, 120, 45));

    public ArmorNurick(Drag drag) {
        super("ArmorNurick", drag);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (mc.player == null) return;

        EquipmentSlot[] slots = {
                EquipmentSlot.FEET,
                EquipmentSlot.LEGS,
                EquipmentSlot.CHEST,
                EquipmentSlot.HEAD
        };

        float total = (ITEM_SIZE * 4) + (GAP * 3);
        drag.width = total;
        drag.height = ITEM_SIZE + BAR_H + 2f;

        Color bg = backgroundColor.get();
        Client.RENDERER.blur(x, y, total, drag.height, new Vector4f(4f), 15f, 1f);
        Client.RENDERER.rect(x, y, total, drag.height, new Vector4f(4f), 1,
                new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), bg.getAlpha()),
                new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), bg.getAlpha()),
                new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), bg.getAlpha()),
                new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), bg.getAlpha()));
        Client.RENDERER.outline(x, y, total, drag.height, 0.4f, new Vector4f(4f), new Vector2f(1),
                new Color(255, 255, 255, 25),
                new Color(255, 255, 255, 25),
                new Color(255, 255, 255, 25),
                new Color(255, 255, 255, 25));

        float curX = x;

        for (EquipmentSlot slot : slots) {

            ItemStack stack = mc.player.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                DrawContext context = Client.RENDERER.getDrawContext();
                if (context != null) {

                    float itemX = curX;
                    float itemY = y;

                    Client.RENDERER.queueTask(() -> {
                        var matrices = context.getMatrices();
                        matrices.pushMatrix();
                        matrices.translate(itemX, itemY);
                        matrices.scale(SCALE, SCALE);

                        context.drawItem(stack, 0, 0);
                        matrices.popMatrix();
                    });
                    if (stack.isDamageable() && stack.getDamage() > 0) {
                        float damage = (float) stack.getDamage() / stack.getMaxDamage();
                        float durability = 1.0f - damage;
                        float barY = y + ITEM_SIZE + 0.5f;
                        Color durCol = Color.getHSBColor(durability / 3f, 1f, 1f);
                        Client.RENDERER.rect(curX, barY, ITEM_SIZE, BAR_H, new Vector4f(0), 1f,
                                new Color(0,0,0,160), new Color(0,0,0,160), new Color(0,0,0,160), new Color(0,0,0,160));

                        Client.RENDERER.rect(curX, barY, ITEM_SIZE * durability, BAR_H, new Vector4f(0), 1f,
                                durCol, durCol, durCol, durCol);
                    }
                }
            }
            curX += ITEM_SIZE + GAP;
        }
    }
}