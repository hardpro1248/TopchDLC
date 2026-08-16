package gg.topchdlc.vse.shutki.screen.hud.ui.impl.main;

import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.mixin.accessor.IItemCooldownEntry;
import gg.topchdlc.mixin.accessor.IItemCooldownManager;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
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
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class CooldownsElement extends HudElement {

    private static final List<Item> EXCLUDED_ITEMS = Arrays.asList(
            Items.SLIME_BLOCK,
            Items.FIREWORK_ROCKET,
            Items.WATER_BUCKET,
            Items.HONEY_BLOCK
    );

    private final EnumSetting<Style> style = settings.enumSetting("Стиль", Style.GLASS);

    private static final float HEIGHT      = 14.5f;
    private static final float PADDING_SIDE = 4.0f;
    private static final float SPACING     = 1.5f;
    private static final float TEXT_SIZE   = 7.0f;
    private static final float ICON_SIZE   = 8.0f;
    private static final float BAR_WIDTH   = 60.0f;

    private final Map<Item, CooldownData> cooldowns = new HashMap<>();

    public CooldownsElement(Drag drag) {
        super("Cooldowns", drag);
    }

    private static class CooldownData {
        float remainingSeconds;
        float maxSeconds;

        CooldownData(float remainingSeconds, float maxSeconds) {
            this.remainingSeconds = remainingSeconds;
            this.maxSeconds = maxSeconds;
        }
    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (mc.player == null) return;

        updateCooldownsPrecise();

        boolean shown = !cooldowns.isEmpty() || mc.currentScreen instanceof ChatScreen;
        animation.setDirection(shown ? Direction.BACKWARDS : Direction.FORWARDS);

        float animAlpha = MathHelper.clamp(1F - animation.getOutput(), 0f, 1f);
        if (animAlpha < 0.01f) return;

        boolean isRightSide = (x + width / 2f) > (mc.getWindow().getScaledWidth() / 2f);
        float currentY = y;

        renderHeader(currentY, isRightSide, animAlpha);
        currentY += HEIGHT + 1.5f;

        float maxRowWidth = 85;

        for (Map.Entry<Item, CooldownData> entry : cooldowns.entrySet()) {
            Item item = entry.getKey();
            CooldownData data = entry.getValue();

            float totalRowW = HEIGHT + SPACING + BAR_WIDTH;
            maxRowWidth = Math.max(maxRowWidth, totalRowW);
            float rowX = isRightSide ? (x + width - totalRowW) : x;

            renderCooldownRow(rowX, currentY, item, data, animAlpha, isRightSide);

            currentY += (HEIGHT + 1.0f);
        }

        this.drag.width = width = MathUtility.linearFps(width, maxRowWidth, 10);
        this.drag.height = height = MathUtility.linearFps(height, (currentY - y), 10);
    }

    private void renderHeader(float ry, boolean right, float alpha) {
        String title = "Cooldowns";
        float iconW = Client.RENDERER.textWidth(IconUse.CLOCK.glyph, TextureUse.ICONS, ICON_SIZE);
        float textW = Client.RENDERER.textWidth(title, TextureUse.SFMEDIUM, TEXT_SIZE);
        float headerW = PADDING_SIDE + iconW + SPACING + 0.5f + SPACING + textW + PADDING_SIDE;

        float hx = right ? (x + width - headerW) : x;

        drawStyle(hx, ry, headerW, HEIGHT, alpha);

        float iconX = hx + PADDING_SIDE;
        float lineX = iconX + iconW + SPACING;
        float textX = lineX + 0.5f + SPACING;

        int textAlpha = (int)(255 * alpha);

        Client.RENDERER.text(IconUse.CLOCK.glyph, iconX, ry + (HEIGHT / 2f) - (ICON_SIZE / 2f) + 0.5f,
                TextureUse.ICONS, ICON_SIZE, new Color(255, 255, 255, textAlpha));
        Client.RENDERER.rect(lineX, ry + 3f, 0.5f, HEIGHT - 6f, new Vector4f(0), 1,
                new Color(255, 255, 255, 50), new Color(255, 255, 255, 50),
                new Color(255, 255, 255, 50), new Color(255, 255, 255, 50));
        Client.RENDERER.text(title, textX, ry + (HEIGHT / 2f) - (TEXT_SIZE / 2f) - 0.3f,
                TextureUse.SFMEDIUM, TEXT_SIZE, new Color(255, 255, 255, textAlpha));
    }

    private void renderCooldownRow(float rx, float ry, Item item, CooldownData data, float alpha, boolean right) {
        float iconX = right ? rx + BAR_WIDTH + SPACING : rx;
        float barX = right ? rx : rx + HEIGHT + SPACING;

        drawStyle(iconX, ry, HEIGHT, HEIGHT, alpha);

        ItemStack stack = item.getDefaultStack();
        Client.RENDERER.queueTask(() -> {
            DrawContext context = Client.RENDERER.getDrawContext();
            if (context != null) {
                var matrices = context.getMatrices();
                matrices.pushMatrix();
                matrices.translate(iconX + 1.5f, ry + 1.5f);
                matrices.scale(10f / 16f, 10f / 16f);
                context.drawItem(stack, 0, 0);
                matrices.popMatrix();
            }
        });

        drawStyle(barX, ry, BAR_WIDTH, HEIGHT, alpha);

        float progress = MathHelper.clamp(data.remainingSeconds / data.maxSeconds, 0f, 1f);
        float progressW = (BAR_WIDTH - 2f) * progress;

        Color barEmpty = new Color(255, 255, 255, (int)(15 * alpha));
        Client.RENDERER.rect(barX + 1f, ry + HEIGHT - 3f, BAR_WIDTH - 2f, 1.5f, new Vector4f(1), 1, barEmpty, barEmpty, barEmpty, barEmpty);

        Color barColor = getCooldownColor(data.remainingSeconds, alpha);
        Client.RENDERER.rect(barX + 1f, ry + HEIGHT - 3f, progressW, 1.5f, new Vector4f(1), 1, barColor, barColor, barColor, barColor);

        String timeText = String.format("%.1fs", data.remainingSeconds);
        float tw = Client.RENDERER.textWidth(timeText, TextureUse.SFMEDIUM, TEXT_SIZE - 1f);
        Client.RENDERER.text(timeText, barX + (BAR_WIDTH/2f - tw/2f), ry + (HEIGHT/2f - (TEXT_SIZE-1f)/2f) - 1f,
                TextureUse.SFMEDIUM, TEXT_SIZE - 1f, new Color(255, 255, 255, (int)(255 * alpha)));
    }

    private void drawStyle(float rx, float ry, float rw, float rh, float alpha) {
        Vector4f round = new Vector4f(5f);
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

    private void updateCooldownsPrecise() {
        cooldowns.clear();
        if (mc.player == null) return;
        IItemCooldownManager manager = (IItemCooldownManager) mc.player.getItemCooldownManager();
        Map<Object, ?> entries = manager.getEntries();
        int currentTick = manager.getTick();
        entries.forEach((key, entryObj) -> {
            Item item = (key instanceof Item) ? (Item) key : (key instanceof Identifier id ? Registries.ITEM.get(id) : null);
            if (item != null && !EXCLUDED_ITEMS.contains(item)) {
                IItemCooldownEntry entry = (IItemCooldownEntry) entryObj;
                int remainingTicks = entry.getEndTick() - currentTick;
                if (remainingTicks > 0) {
                    cooldowns.put(item, new CooldownData(remainingTicks / 20.0f, (entry.getEndTick() - entry.getStartTick()) / 20.0f));
                }
            }
        });
    }

    private Color getCooldownColor(float seconds, float alpha) {
        int r, g, b;
        if (seconds > 5) { r = 255; g = 100; b = 100; }
        else if (seconds > 2) { r = 255; g = 200; b = 100; }
        else { r = 150; g = 255; b = 150; }
        return new Color(r, g, b, (int)(200 * alpha));
    }

    @AllArgsConstructor
    enum Style implements EnumChoice {
        DARK("Темный"), GLASS("Стекло");
        @Getter final String renderName;
    }
}