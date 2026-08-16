package gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.joml.Vector4f;
import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.vse.shutki.module.modules.impl.helper.AssistModule;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.client.text.TextUtility;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
/**
 * Create by daun kvass
 */
public class BindsNurick extends HudElement {

    private static final float SLOT_SIZE   = 8f;
    private static final float PILL_H      = 14f;
    private static final float FS          = 6.5f;
    private static final float COUNT_FS    = 5f;
    private static final float GAP         = 2f;
    private static final Vector4f ROUND    = new Vector4f(4f, 4f, 4f, 4f);

    public BindsNurick(Drag drag) {
        super("NurickBinds", drag);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (mc.player == null) return;
        List<Object[]> entries = AssistModule.INSTANCE.getBindEntries();
        List<Object[]> active = new ArrayList<>();
        for (Object[] e : entries) {
            if ((int) e[1] != -1) active.add(e);
        }
        boolean shown = mc.currentScreen instanceof ChatScreen || !active.isEmpty();
        animation.setDirection(shown ? Direction.BACKWARDS : Direction.FORWARDS);
        float anim = 1f - animation.getOutput();
        if (anim < 0.01f || active.isEmpty()) return;

        CRenderSystem sys = Client.RENDERER.getCrenderSystem();
        float prevAlpha = sys.alpha();
        sys.alpha(anim * prevAlpha);

        float curX = x;
        float curY = y;
        List<float[]> itemPositions = new ArrayList<>();
        List<ItemStack> itemStacks = new ArrayList<>();

        for (Object[] entry : active) {
            Item item = (Item) entry[0];
            int keyCode = (int) entry[1];
            String bindStr = TextUtility.keyToString(keyCode).toUpperCase();
            int itemCount = 0;
            ItemStack sampleStack = ItemStack.EMPTY;
            for (int s = 0; s < mc.player.getInventory().size(); s++) {
                ItemStack stack = mc.player.getInventory().getStack(s);
                if (!stack.isEmpty() && stack.getItem() == item) {
                    itemCount += stack.getCount();
                    if (sampleStack.isEmpty()) sampleStack = stack;
                }
            }
            if (sampleStack.isEmpty()) sampleStack = new ItemStack(item);

            boolean onCooldown = mc.player.getItemCooldownManager().isCoolingDown(sampleStack);
            float bindW = Client.RENDERER.textWidth(bindStr, TextureUse.SFMEDIUM, FS);
            float pillW = 5f + SLOT_SIZE + 4f + 0.6f + 4f + bindW + 5f;
            Color bg = new Color(15, 15, 20, 160);
            Client.RENDERER.blur(curX, curY, pillW, PILL_H, ROUND, 12f, 1f);
            Client.RENDERER.rect(curX, curY, pillW, PILL_H, ROUND, 1f, bg, bg, bg, bg);
            float itemX = curX + 4f;
            float itemY = curY + (PILL_H - SLOT_SIZE) / 2f;
            float itemYX = curY + (PILL_H - SLOT_SIZE) / 3f;
            float scale = SLOT_SIZE / 14f;

            itemPositions.add(new float[]{itemX, itemY, scale});
            itemStacks.add(sampleStack);
            String countStr = String.valueOf(itemCount);
            Color countColor = itemCount == 0 ? new Color(255, 60, 60) : Color.WHITE;
            Client.RENDERER.text(countStr, itemX + SLOT_SIZE - 2f, itemYX + SLOT_SIZE - 3f, TextureUse.SFMEDIUM, COUNT_FS, countColor);
            float sepX = itemX + SLOT_SIZE + 4f;
            Color sepCol = new Color(255, 255, 255, 45);
            Client.RENDERER.rect(sepX, curY + 4f, 0.6f, PILL_H - 8f, new Vector4f(0.5f), 1f, sepCol, sepCol, sepCol, sepCol);
            float textY = curY + (PILL_H / 2f) - (Client.FONTS.get(TextureUse.SFMEDIUM).getMetrics().baselineHeight() * FS / 2f);
            Color bindColor = onCooldown ? new Color(230, 200, 50) : Color.WHITE;
            Client.RENDERER.text(bindStr, sepX + 4.5f, textY, TextureUse.SFMEDIUM, FS, bindColor);

            curX += pillW + GAP;
        }
        DrawContext context = Client.RENDERER.getDrawContext();
        if (context != null && !itemStacks.isEmpty()) {
            final List<float[]> finalPositions = itemPositions;
            final List<ItemStack> finalStacks = itemStacks;
            Client.RENDERER.queueTask(() -> {
                for (int i = 0; i < finalStacks.size(); i++) {
                    float ix = finalPositions.get(i)[0];
                    float iy = finalPositions.get(i)[1];
                    float sc = finalPositions.get(i)[2];
                    var matrices = context.getMatrices();
                    matrices.pushMatrix();
                    matrices.translate(ix, iy);
                    matrices.scale(sc, sc);
                    context.drawItem(mc.player, finalStacks.get(i), 0, 0, i + 100);
                    matrices.popMatrix();
                }
            });

        }
        drag.width = curX - x;
        drag.height = PILL_H;

        sys.alpha(prevAlpha);
    }
}