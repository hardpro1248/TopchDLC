package gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;
import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.mixin.accessor.IItemCooldownEntry;
import gg.topchdlc.mixin.accessor.IItemCooldownManager;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick.icon.NurickIcons;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.math.MathUtility;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CooldownsElementN extends HudElement {

    private static final List<Item> EXCLUDED_ITEMS = Arrays.asList(
            Items.SLIME_BLOCK,
            Items.FIREWORK_ROCKET,
            Items.WATER_BUCKET,
            Items.HONEY_BLOCK

    );


    private static final String TITLE = "Cooldowns";
    private static final float FS = 5.5f;
    private static final float FS_TITLE = 5.5f;
    private static final float ICON_SIZE_H = 6f;
    private static final float HEADER_H = 12f;
    private static final float ROW_H = 12f;
    private static final float PAD = 6f;
    private static final float ITEM_IC_S = 9f;
    private static final Vector4f ROUND = new Vector4f(5.5f, 5.5f, 5.5f, 5.5f);

    private float widthAnim = -1f;

    public CooldownsElementN(Drag drag) {
        super("CooldownsN", drag);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (mc.player == null) return;

        IItemCooldownManager manager = (IItemCooldownManager) mc.player.getItemCooldownManager();
        Map<Object, ?> entries = manager.getEntries();
        int currentTick = manager.getTick();

        List<CooldownInfo> activeCooldowns = new ArrayList<>();

        entries.forEach((key, entryObj) -> {
            Item item = null;
            if (key instanceof Item) {
                item = (Item) key;
            } else if (key instanceof Identifier id) {
                item = Registries.ITEM.get(id);
            }
            if (item != null && !EXCLUDED_ITEMS.contains(item)) {
                IItemCooldownEntry entry = (IItemCooldownEntry) entryObj;
                int remainingTicks = entry.getEndTick() - currentTick;
                if (remainingTicks > 0) {
                    activeCooldowns.add(new CooldownInfo(item, remainingTicks / 20.0f));
                }
            }
        });
        boolean shown = !activeCooldowns.isEmpty() || mc.currentScreen instanceof ChatScreen;
        animation.setDirection(shown ? Direction.BACKWARDS : Direction.FORWARDS);
        float anim = 1f - animation.getOutput();
        if (anim < 0.01f) return;

        float headerIconW = Client.RENDERER.textWidth(NurickIcons.TIMER, TextureUse.ICONS_NURIK, ICON_SIZE_H);
        float headerTextW = Client.RENDERER.textWidth(TITLE, TextureUse.SFMEDIUM, FS_TITLE);
        float minW = PAD + headerIconW + 5f + 0.6f + 5f + headerTextW + PAD + 20f;

        float maxRowW = minW;
        for (CooldownInfo cd : activeCooldowns) {
            float nameW = Client.RENDERER.textWidth(cd.item.getName().getString(), TextureUse.SFMEDIUM, FS);
            float timeW = Client.RENDERER.textWidth(String.format("%.1fs", cd.seconds), TextureUse.SFMEDIUM, FS);
            float currentRowW = PAD + nameW + 20f + ITEM_IC_S + 4f + 1f + 4f + timeW + PAD;
            maxRowW = Math.max(maxRowW, currentRowW);
        }

        if (widthAnim < 0) widthAnim = maxRowW;
        widthAnim = MathUtility.linearFps(widthAnim, maxRowW, 10f);
        float W = widthAnim;
        float totalH = HEADER_H + (!activeCooldowns.isEmpty() ? (activeCooldowns.size() * ROW_H) + 2f : 0);

        CRenderSystem system = Client.RENDERER.getCrenderSystem();
        float sys_a = system.alpha();
        system.alpha(anim * sys_a);

        Client.RENDERER.blur(x, y, W, totalH, ROUND, 12f, 1f);
        Color bg = new Color(15, 15, 20, 160);
        Client.RENDERER.rect(x, y, W, totalH, ROUND, 1f, bg, bg, bg, bg);

        float iconY = y + (HEADER_H / 2f) - (Client.FONTS.get(TextureUse.ICONS_NURIK).getMetrics().baselineHeight() * ICON_SIZE_H / 2f) + 0.5f;
        Client.RENDERER.text(NurickIcons.TIMER, x + PAD, iconY, TextureUse.ICONS_NURIK, ICON_SIZE_H, Color.WHITE);

        float sepX = x + PAD + headerIconW + 5f;
        Color sepCol = new Color(255, 255, 255, 60);
        Client.RENDERER.rect(sepX, y + (HEADER_H - 7.5f) / 2f, 0.6f, 7.5f, new Vector4f(1), 1f, sepCol, sepCol, sepCol, sepCol);

        float titleY = y + (HEADER_H / 2f) - (Client.FONTS.get(TextureUse.SFMEDIUM).getMetrics().baselineHeight() * FS_TITLE / 2f);
        Client.RENDERER.text(TITLE, sepX + 5.5f, titleY, TextureUse.SFMEDIUM, FS_TITLE, Color.WHITE);

        float curY = y + HEADER_H + 1f;
        for (CooldownInfo cd : activeCooldowns) {
            String name = cd.item.getName().getString();
            String timeStr = String.format("%.1fs", cd.seconds);
            float textY = curY + (ROW_H / 2f) - (Client.FONTS.get(TextureUse.SFMEDIUM).getMetrics().baselineHeight() * FS / 2f);

            Client.RENDERER.text(name, x + PAD, textY, TextureUse.SFMEDIUM, FS, Color.WHITE);
            float timeW = Client.RENDERER.textWidth(timeStr, TextureUse.SFMEDIUM, FS);
            float timeX = x + W - PAD - timeW;
            Client.RENDERER.text(timeStr, timeX, textY, TextureUse.SFMEDIUM, FS, new Color(200, 200, 200));

            float barX = timeX - 4.5f;
            Client.RENDERER.rect(barX, curY + (ROW_H - 6f) / 2f, 0.6f, 6f, new Vector4f(1), 1f, sepCol, sepCol, sepCol, sepCol);

            float itemIconX = barX - 4.5f - ITEM_IC_S;
            float itemIconY = curY + (ROW_H - ITEM_IC_S) / 2f;
            drawItem(cd.item, itemIconX, itemIconY);
            curY += ROW_H;
        }

        system.alpha(sys_a);
        drag.width = W;
        drag.height = totalH;
    }

    private void drawItem(Item item, float ix, float iy) {
        DrawContext context = Client.RENDERER.getDrawContext();
        if (context != null) {
            ItemStack stack = item.getDefaultStack();
            Client.RENDERER.queueTask(() -> {
                var matrices = context.getMatrices();
                matrices.pushMatrix();
                matrices.translate(ix, iy);
                float scale = ITEM_IC_S / 16f;
                matrices.scale(scale, scale);
                context.drawItem(mc.player, stack, 0, 0, 0);
                matrices.popMatrix();
            });
        }
    }

    private static class CooldownInfo {
        Item item;
        float seconds;
        public CooldownInfo(Item item, float seconds) {
            this.item = item;
            this.seconds = seconds;
        }
    }
}