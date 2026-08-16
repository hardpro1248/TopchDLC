package gg.topchdlc.vse.shutki.screen.hud.ui.impl.main;

import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ArmorHudElement extends HudElement {
    private final EnumSetting<Style> style = settings.enumSetting("Стиль", Style.GLASS);
    private final gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting backgroundColor = settings.colorSetting("Цвет фона", new Color(55, 235, 120, 55));
    private static final float SLOT_W = 28.0f;
    private static final float HEIGHT = 28.0f;
    private static final float TEXT_SIZE = 7.0f;

    public ArmorHudElement(Drag drag) {
        super("ArmorHud", drag);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (mc.player == null) return;

        List<ItemStack> armorPieces = new ArrayList<>();
        armorPieces.add(mc.player.getEquippedStack(EquipmentSlot.HEAD));
        armorPieces.add(mc.player.getEquippedStack(EquipmentSlot.CHEST));
        armorPieces.add(mc.player.getEquippedStack(EquipmentSlot.LEGS));
        armorPieces.add(mc.player.getEquippedStack(EquipmentSlot.FEET));
        armorPieces.removeIf(ItemStack::isEmpty);

        if (armorPieces.isEmpty()) return;

        float alpha = 1.0f;
        float totalWidth = armorPieces.size() * SLOT_W;

        drawStyle(x, y, totalWidth, HEIGHT, alpha);

        for (int i = 0; i < armorPieces.size(); i++) {
            ItemStack stack = armorPieces.get(i);
            float slotX = x + (i * SLOT_W);

            renderArmorItem(stack, slotX + 6f, y + 6f);

            if (stack.isDamageable()) {
                renderDurability(stack, slotX + 3f, y + HEIGHT - 3.2f, SLOT_W - 5f, alpha);
            }

            if (i < armorPieces.size() - 1) {
                float sepX = slotX + SLOT_W;
                Color sepCol = new Color(255, 255, 255, (int)(25 * alpha));
                Client.RENDERER.rect(sepX, y + 4f, 0.5f, HEIGHT - 8f, new Vector4f(0), 1, sepCol, sepCol, sepCol, sepCol);
            }
        }

        if (drag != null) {
            drag.width = width = totalWidth;
            drag.height = height = HEIGHT;
        }
    }

    private void renderDurability(ItemStack stack, float bx, float by, float bw, float alpha) {
        float damage = (float) stack.getDamage();
        float maxDamage = (float) stack.getMaxDamage();
        float progress = MathHelper.clamp(1.0f - (damage / maxDamage), 0f, 1f);

        Color color;
        if (progress > 0.6f) color = new Color(100, 255, 100, (int)(200 * alpha));
        else if (progress > 0.25f) color = new Color(255, 220, 80, (int)(200 * alpha));
        else color = new Color(255, 80, 80, (int)(200 * alpha));

        Client.RENDERER.rect(bx, by, bw, 1.5f, new Vector4f(1), 1,
                new Color(0, 0, 0, (int)(100 * alpha)), new Color(0, 0, 0, (int)(100 * alpha)),
                new Color(0, 0, 0, (int)(100 * alpha)), new Color(0, 0, 0, (int)(100 * alpha)));

        Client.RENDERER.rect(bx, by, bw * progress, 1.7f, new Vector4f(1), 1, color, color, color, color);
    }

    private void renderArmorItem(ItemStack stack, float ix, float iy) {
        DrawContext context = Client.RENDERER.getDrawContext();
        if (context != null) {
            Client.RENDERER.queueTask(() -> {
                var matrices = context.getMatrices();
                matrices.pushMatrix();
                matrices.translate(ix, iy);
                context.drawItem(stack, 0, 0);
                matrices.popMatrix();
            });
        }
    }

    private void drawStyle(float rx, float ry, float rw, float rh, float alpha) {
        Vector4f round = new Vector4f(5f);
        Client.RENDERER.blur(rx, ry, rw, rh, round, 15f, alpha);

        Color base = backgroundColor.get();
        if (style.get() == Style.DARK) {
            Color bg = new Color(base.getRed(), base.getGreen(), base.getBlue(), (int) Math.min(255, base.getAlpha() * alpha));
            Client.RENDERER.rect(rx, ry, rw, rh, round, 1, bg, bg, bg, bg);
            Color out = new Color(255, 255, 255, (int) (15 * alpha));
            Client.RENDERER.outline(rx, ry, rw, rh, 0.4f, round, new Vector2f(1), out, out, out, out);
        } else {
            Color bg = new Color(base.getRed(), base.getGreen(), base.getBlue(), (int) (base.getAlpha() * alpha));
            Client.RENDERER.rect(rx, ry, rw, rh, round, 1, bg, bg, bg, bg);
            int outAlpha = MathHelper.clamp((int) (25 * alpha), 0, 255);
            Color out = new Color(255, 255, 255, outAlpha);
            Client.RENDERER.outline(rx, ry, rw, rh, 0.4f, round, new Vector2f(1), out, out, out, out);
        }
    }

    @AllArgsConstructor
    enum Style implements EnumChoice {
        DARK("Темный"), GLASS("Стекло");
        @Getter final String renderName;
    }
}