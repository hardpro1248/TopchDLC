package gg.topchdlc.vse.shutki.screen.hud.ui.impl.main;

import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.multienum.MultiEnumSetting;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.math.MathUtility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PvPResourcesElement extends HudElement {
    public static final PvPResourcesElement INSTANCE = new PvPResourcesElement(
            new Drag("TotemCounter", () -> true).bound(20, 120, 16, 14)
    );
    private final MultiEnumSetting<Element> elements = settings.multiEnumSetting("Элементы",
            Element.TOTEM, Element.CRYSTAL, Element.EXP, Element.GAP, Element.EGAP, Element.ROCKET, Element.OBSIDIAN);

    private final EnumSetting<Style> style = settings.enumSetting("Стиль", Style.DARK);

    public PvPResourcesElement(Drag drag) {
        super("PvPResourcesWidget", drag);
    }

    private static final float ITEM_SIZE = 14f;
    private static final float FONT_SIZE = 6.5f;
    private static final float ELEMENT_WIDTH = 24f;
    private static final float HEIGHT = 28f;

    @Override
    public void render(int mouseX, int mouseY) {
        if (mc.player == null) return;

        List<Element> activeList = new ArrayList<>(elements.get());
        boolean shown = mc.currentScreen instanceof ChatScreen || !activeList.isEmpty();

        animation.setDirection(shown ? Direction.BACKWARDS : Direction.FORWARDS);
        float animAlpha = MathHelper.clamp(1F - animation.getOutput(), 0f, 1f);

        if (animAlpha < 0.01f) return;

        float totalWidth = activeList.size() * ELEMENT_WIDTH;
        if (activeList.isEmpty()) totalWidth = 40f;
        drawBackground(x, y, totalWidth, HEIGHT, animAlpha);

        float currentX = x;

        for (int i = 0; i < activeList.size(); i++) {
            Element el = activeList.get(i);
            int count = getCount(el.item);
            String countText = count > 0 ? String.valueOf(count) : "-";

            float itemX = currentX + (ELEMENT_WIDTH / 2f - ITEM_SIZE / 2f);
            float itemY = y + 4f;
            renderItem(el.item.getDefaultStack(), itemX, itemY, ITEM_SIZE);

            float tw = Client.RENDERER.textWidth(countText, TextureUse.SFMEDIUM, FONT_SIZE);
            float tx = currentX + (ELEMENT_WIDTH / 2f - tw / 2f);
            float ty = y + ITEM_SIZE + 5f;

            Color textColor = (count == 0) ? new Color(255, 80, 80, (int)(255 * animAlpha)) : new Color(255, 255, 255, (int)(255 * animAlpha));
            Client.RENDERER.text(countText, tx, ty, TextureUse.SFMEDIUM, FONT_SIZE, textColor);

            if (i < activeList.size() - 1) {
                float sepX = currentX + ELEMENT_WIDTH;
                int lineAlpha = (style.get() == Style.DARK) ? 30 : 50;
                Color sepColor = new Color(255, 255, 255, (int) (lineAlpha * animAlpha));
                Client.RENDERER.rect(sepX, y + 6f, 0.5f, HEIGHT - 12f, new Vector4f(0), 1, sepColor, sepColor, sepColor, sepColor);
            }

            currentX += ELEMENT_WIDTH;
        }

        this.drag.width = width = MathUtility.linearFps(width, totalWidth, 10);
        this.drag.height = height = HEIGHT;
    }

    private void drawBackground(float rx, float ry, float rw, float rh, float alpha) {
        Vector4f round = new Vector4f(6f);
        Client.RENDERER.blur(rx, ry, rw, rh, round, 15f, alpha);

        if (style.get() == Style.DARK) {
            Color bgColor = new Color(20, 20, 25, (int) (180 * alpha));
            Client.RENDERER.rect(rx, ry, rw, rh, round, 1, bgColor, bgColor, bgColor, bgColor);

            Color outColor = new Color(255, 255, 255, (int) (15 * alpha));
            Client.RENDERER.outline(rx, ry, rw, rh, 0.4f, round, new Vector2f(1), outColor, outColor, outColor, outColor);
        } else {
            Color bgColor = new Color(62, 62, 71, 0);
            Client.RENDERER.rect(rx, ry, rw, rh, round, 1, bgColor, bgColor, bgColor, bgColor);

            int outAlpha = MathHelper.clamp((int) (25 * alpha), 0, 255);
            Color outColor = new Color(255, 255, 255, outAlpha);
            Client.RENDERER.outline(rx, ry, rw, rh, 0.4f, round, new Vector2f(1), outColor, outColor, outColor, outColor);
        }
    }

    private void renderItem(ItemStack stack, float ix, float iy, float size) {
        Client.RENDERER.queueTask(() -> {
            DrawContext context = Client.RENDERER.getDrawContext();
            if (context != null) {
                var matrices = context.getMatrices();
                matrices.pushMatrix();
                matrices.translate(ix, iy);
                float scale = size / 16f;
                matrices.scale(scale, scale);
                context.drawItem(stack, 0, 0);
                matrices.popMatrix();
            }
        });
    }

    private int getCount(Item item) {
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) count += stack.getCount();
        }
        if (mc.player.getOffHandStack().getItem() == item) count += mc.player.getOffHandStack().getCount();
        return count;
    }

    @AllArgsConstructor
    enum Style implements EnumChoice {
        DARK("Темный"), GLASS("Стекло");
        @Getter final String renderName;
    }

    @AllArgsConstructor
    enum Element implements EnumChoice {
        TOTEM("Totem", Items.TOTEM_OF_UNDYING),
        CRYSTAL("Crystal", Items.END_CRYSTAL),
        EXP("XP", Items.EXPERIENCE_BOTTLE),
        GAP("Apple", Items.GOLDEN_APPLE),
        EGAP("Gapple", Items.ENCHANTED_GOLDEN_APPLE),
        ROCKET("Rocket", Items.FIREWORK_ROCKET),
        OBSIDIAN("Obsi", Items.OBSIDIAN);
        @Getter final String renderName;
        final Item item;
    }
}