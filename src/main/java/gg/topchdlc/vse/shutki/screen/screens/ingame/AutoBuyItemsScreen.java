package gg.topchdlc.vse.shutki.screen.screens.ingame;

import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.autobuy.AutoBuyUtil;
import gg.topchdlc.api.autobuy.PurchaseHistoryItem;
import gg.topchdlc.api.autobuy.SellHistoryItem;
import gg.topchdlc.api.autobuy.enchantes.Enchant;
import gg.topchdlc.api.autobuy.item.ItemBuy;
import gg.topchdlc.api.autobuy.item.Lore.LoreAttributeItemBuy;
import gg.topchdlc.api.autobuy.item.Lore.LoreEnchantItemBuy;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.vse.shutki.module.modules.impl.farm.AutoBuy;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.Click;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AutoBuyItemsScreen extends Screen implements MinecraftHolder {

    private static final Identifier CHEST_TEXTURE = Identifier.of("minecraft", "textures/gui/container/generic_54.png");

    private final List<PurchaseHistoryItem> purchaseHistory = new ArrayList<>();
    private final List<SellHistoryItem> sellHistory = new ArrayList<>();

    private final int bgWidth = 176;
    private final int bgHeight = 222;
    public int guiX, guiY;

    private ItemBuy selectedItem = null;
    private float scrollAmount = 0;
    private float maxScroll = 0;

    private boolean showAny = true;
    private boolean showFuntime = true;
    private boolean showHollyworld = true;

    private boolean editingMaxPrice = false;
    private boolean editingSellPrice = false;
    private String currentInput = "";

    public AutoBuyItemsScreen() {
        super(Text.of("AutoBuy Settings"));
    }

    public boolean isOpened() { return mc.currentScreen == this; }
    public void setOpened(boolean opened) {
        if (opened) mc.setScreen(this);
        else if (mc.currentScreen == this) mc.setScreen(null);
    }
    public boolean isWriting() { return editingMaxPrice || editingSellPrice; }

    public void click(int mx, int my, int btn) { this.handleMouseClicked(mx, my, btn); }
    public void release(int btn) { }
    public void mouseDragged(int mx, int my) { }
    public void chartyped(char chr, int mod) { this.handleCharTyped(chr, mod); }
    public void render(int mx, int my) { }

    public boolean keyPressed(int key, int scancode, int modifiers) {
        return this.handleKeyPressed(key, scancode, modifiers);
    }

    public List<PurchaseHistoryItem> getPurchaseHistory() { return purchaseHistory; }
    public List<SellHistoryItem> getSellHistory() { return sellHistory; }
    public void clearPurchaseHistory() { purchaseHistory.clear(); }
    public void addPurchaseHistoryItem(ItemStack s, int p) { purchaseHistory.add(new PurchaseHistoryItem(s.copy(), p, System.currentTimeMillis())); }
    public void addSellHistoryItem(ItemStack s, int c, int p) { sellHistory.add(new SellHistoryItem(s.copy(), p, (int) System.currentTimeMillis())); }

    @Override
    protected void init() {
        this.guiX = (this.width - bgWidth - 130) / 2;
        this.guiY = (this.height - bgHeight) / 2;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        Client.RENDERER.getCrenderSystem().layer(CRenderSystem.RenderLayer.OVERLAY);

        context.fill(0, 0, this.width, this.height, 0x90000000);

        renderFilterTabs(context, mouseX, mouseY);

        context.drawTexture(RenderPipelines.GUI_TEXTURED, CHEST_TEXTURE, guiX, guiY, 0.0f, 0.0f, bgWidth, bgHeight, 256, 256);
        Client.RENDERER.text("AutoBuy Manager", guiX + 8, guiY + 6, TextureUse.SFMEDIUM, 7f, Color.DARK_GRAY);

        context.enableScissor(guiX + 7, guiY + 17, guiX + 169, guiY + 125);


        float currentY = guiY + 18 + scrollAmount;
        if (showFuntime) currentY = drawCategoryGroup(context, "FUNTIME", AutoBuy.INSTANCE.getManager().getFuntime(), currentY);
        if (showHollyworld) currentY = drawCategoryGroup(context, "HOLLYWORLD", AutoBuy.INSTANCE.getManager().getHollyworld(), currentY);
        if (showAny) currentY = drawCategoryGroup(context, "ANY", AutoBuy.INSTANCE.getManager().getVanilla(), currentY);

        float totalH = (currentY - scrollAmount) - (guiY + 18);
        this.maxScroll = Math.max(0, totalH - 108);

        context.disableScissor();
        drawAutoBuyInterface(context, mouseX, mouseY, guiX, guiY);

        renderTooltips(context, mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);
    }

    private float drawCategoryGroup(DrawContext context, String title, List<ItemBuy> items, float startY) {
        if (items.isEmpty()) return startY;

        Client.RENDERER.text(title, guiX + 10, startY + 5, TextureUse.SFMEDIUM, 6f, Color.YELLOW);

        float itemStartY = startY + 18;

        for (int i = 0; i < items.size(); i++) {
            int col = i % 9;
            int row = i / 9;
            int ix = guiX + 8 + col * 18;
            int iy = (int) (itemStartY + row * 18);

            if (iy + 18 > guiY + 17 && iy < guiY + 125) {
                ItemBuy item = items.get(i);
                if (selectedItem == item) context.fill(ix, iy, ix + 16, iy + 16, 0x60FFFFFF);

                context.drawItem(item.getItemStack(), ix, iy);

                context.fill(ix + 13, iy + 13, ix + 16, iy + 16, AutoBuy.INSTANCE.isItemEnabled(item) ? 0xFF00FF00 : 0xFFFF0000);
                if (AutoBuy.INSTANCE.isAutoParserItem(item)) context.fill(ix, iy + 13, ix + 3, iy + 16, 0xFFFFFF00);
            }
        }

        int rows = (int) Math.ceil(items.size() / 9.0);
        return itemStartY + (rows * 18);
    }

    private void renderFilterTabs(DrawContext context, int mx, int my) {
        int bx = guiX; int by = guiY - 22;
        drawTab(context, bx, by, 50, "Any", showAny, mx, my);
        drawTab(context, bx + 53, by, 55, "FunTime", showFuntime, mx, my);
        drawTab(context, bx + 111, by, 65, "HollyWorld", showHollyworld, mx, my);
    }

    private void drawTab(DrawContext context, int bx, int by, int bw, String text, boolean active, int mx, int my) {
        boolean h = mx >= bx && mx <= bx + bw && my >= by && my <= by + 18;
        context.fill(bx, by, bx + bw, by + 18, active ? 0xFF444444 : 0xFF151515);
        context.drawStrokedRectangle(bx, by, bw, 18, h ? 0xFFFFFFFF : 0xFFAAAAAA);
        float tw = Client.RENDERER.textWidth(text, TextureUse.SFMEDIUM, 6.5f);
        Client.RENDERER.text(text, bx + (bw - tw) / 2f, by + 5, TextureUse.SFMEDIUM, 6.5f, active ? Color.WHITE : Color.GRAY);
    }

    public void drawAutoBuyInterface(DrawContext context, int mouseX, int mouseY, int gx, int gy) {
        if (selectedItem == null) return;
        int px = gx + bgWidth + 5; int pw = 130;
        context.fill(px, gy, px + pw, gy + bgHeight, 0xEE000000);
        context.drawStrokedRectangle(px, gy, pw, bgHeight, 0xFFAAAAAA);

        String nameToShow = selectedItem.getItemStack().contains(net.minecraft.component.DataComponentTypes.CUSTOM_NAME)
                ? selectedItem.getItemStack().getName().getString()
                : selectedItem.getDisplayName();

        Client.RENDERER.text("Настройки", px + 5, gy + 8, TextureUse.SFMEDIUM, 8f, Color.WHITE);
        Client.RENDERER.text(nameToShow, px + 5, gy + 18, TextureUse.SFMEDIUM, 6f, Color.CYAN);

        int nbtY = gy + 32;
        context.fill(px + 5, nbtY, px + pw - 5, nbtY + 65, 0x40FFFFFF);
        renderItemNbtDetails(px + 8, nbtY + 4, selectedItem);

        int curY = gy + 95;
        drawStandardBtn(context, px + 5, curY, pw - 10, "Найти на AH", mouseX, mouseY, 0xFF333333);
        curY += 25;
        String pStr = editingMaxPrice ? currentInput + "_" : String.valueOf(AutoBuy.INSTANCE.getItemMaxPrice(AutoBuy.INSTANCE.getItemKey(selectedItem)));
        drawStandardInput(context, px + 5, curY, pw - 10, "Купить до:", pStr, editingMaxPrice);
        curY += 40;
        boolean sell = AutoBuy.INSTANCE.isItemAutoSellEnabled(selectedItem);
        drawStandardBtn(context, px + 5, curY, pw - 10, "AutoSell: " + (sell ? "ВКЛ" : "ВЫКЛ"), mouseX, mouseY, sell ? 0xFF00AA00 : 0xFFAA0000);
        if (sell) {
            curY += 25;
            if (AutoBuy.INSTANCE.isItemSellModeCustom(selectedItem)) {
                String sStr = editingSellPrice ? currentInput + "_" : String.valueOf(AutoBuy.INSTANCE.getItemCustomSellPrice(AutoBuy.INSTANCE.getItemKey(selectedItem)));
                drawStandardInput(context, px + 5, curY, pw - 10, "Продать за:", sStr, editingSellPrice);
            } else {
                drawStandardBtn(context, px + 5, curY, pw - 10, "??", mouseX, mouseY, 0xFFFFAA00);
            }
        }
    }


    private void renderItemNbtDetails(int sx, int sy, ItemBuy item) {
        int ly = sy;
        ItemStack stack = item.getItemStack();

        List<Enchant> enchants = new ArrayList<>();
        enchants.addAll(AutoBuyUtil.getEnchants(stack));
        if (item instanceof LoreEnchantItemBuy le) {
            for (Enchant e : le.getEnchants()) {
                if (enchants.stream().noneMatch(existing -> existing.getName().equals(e.getName()))) {
                    enchants.add(e);
                }
            }
        }

        if (!enchants.isEmpty()) {
            Client.RENDERER.text("Зачарования:", sx, ly, TextureUse.SFMEDIUM, 5.5f, Color.YELLOW);
            ly += 7;
            for (int i = 0; i < Math.min(5, enchants.size()); i++) {
                Enchant e = enchants.get(i);
                String name = e.getName().replace("minecraft:", "").replace("_", " ");
                Client.RENDERER.text("• " + name + " " + e.getMinLevel(), sx + 4, ly, TextureUse.SFMEDIUM, 5f, Color.LIGHT_GRAY);
                ly += 6;
            }
        }
        AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);

        if (modifiers != null && !modifiers.modifiers().isEmpty()) {
            if (ly > sy + 25) ly += 4;
            Client.RENDERER.text("Атрибуты:", sx, ly, TextureUse.SFMEDIUM, 5.5f, Color.ORANGE);
            ly += 7;

            for (AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
                if (ly > sy + 55) break;
                String attrName = entry.attribute().getKey().map(k -> k.getValue().getPath()).orElse("attr");
                double val = entry.modifier().value();

                if (attrName.contains("attack_damage")) attrName = "Урон";
                else if (attrName.contains("movement_speed")) attrName = "Скорость";
                else if (attrName.contains("max_health")) attrName = "ХП";

                String sign = val > 0 ? "+" : "";
                Client.RENDERER.text("• " + attrName + ": " + sign + val, sx + 4, ly, TextureUse.SFMEDIUM, 4.5f, new Color(150, 255, 150));
                ly += 6;
            }
        }
        else if (item instanceof LoreAttributeItemBuy la && !la.getRequiredAttributes().isEmpty()) {
            ly += 4;
            Client.RENDERER.text("Требуемые атрибуты:", sx, ly, TextureUse.SFMEDIUM, 5.5f, Color.ORANGE);
            ly += 7;

            for (String attrKey : la.getRequiredAttributes().keySet()) {
                if (ly > sy + 55) break;
                String shortKey = attrKey.replace("minecraft:generic.", "").replace("minecraft:", "");
                Client.RENDERER.text("• " + shortKey, sx + 4, ly, TextureUse.SFMEDIUM, 4.5f, Color.GRAY);
                ly += 6;
            }
        }

        if (enchants.isEmpty() && modifiers == null) {
            Client.RENDERER.text("Стандартный предмет", sx, ly + 10, TextureUse.SFMEDIUM, 5f, Color.DARK_GRAY);
        }
    }

    private void drawStandardBtn(DrawContext context, int bx, int by, int bw, String text, int mx, int my, int color) {
        boolean h = mx >= bx && mx <= bx + bw && my >= by && my <= by + 20;
        context.fill(bx, by, bx + bw, by + 20, h ? 0xFF444444 : 0xFF222222);
        context.drawStrokedRectangle(bx, by, bw, 20, 0xFFAAAAAA);
        float tw = Client.RENDERER.textWidth(text, TextureUse.SFMEDIUM, 7f);
        Client.RENDERER.text(text, bx + (bw - tw) / 2f, by + 6, TextureUse.SFMEDIUM, 7f, Color.WHITE);
    }

    private void drawStandardInput(DrawContext context, int bx, int by, int bw, String label, String val, boolean active) {
        Client.RENDERER.text(label, bx, by - 10, TextureUse.SFMEDIUM, 6f, Color.LIGHT_GRAY);
        context.fill(bx, by, bx + bw, by + 18, 0xFF000000);
        context.drawStrokedRectangle(bx, by, bw, 18, active ? 0xFFFFFFFF : 0xFF555555);
        Client.RENDERER.text(val, bx + 5, by + 5, TextureUse.SFMEDIUM, 7f, Color.WHITE);
    }

    private void renderTooltips(DrawContext context, int mx, int my) {
        float currentY = guiY + 18 + scrollAmount;
        if (showFuntime) currentY = checkT(context, AutoBuy.INSTANCE.getManager().getFuntime(), currentY, mx, my);
        if (showHollyworld) currentY = checkT(context, AutoBuy.INSTANCE.getManager().getHollyworld(), currentY, mx, my);
        if (showAny) currentY = checkT(context, AutoBuy.INSTANCE.getManager().getVanilla(), currentY, mx, my);
    }

    private float checkT(DrawContext context, List<ItemBuy> items, float sY, int mx, int my) {
        float iYS = sY + 18;
        for (int i = 0; i < items.size(); i++) {
            int ix = guiX + 8 + (i % 9) * 18;
            int iy = (int) (iYS + (i / 9) * 18);
            if (iy + 18 > guiY + 17 && iy < guiY + 125 && mx >= ix && mx <= ix + 16 && my >= iy && my <= iy + 16) {
                context.drawItemTooltip(textRenderer, items.get(i).getItemStack(), mx, my);
            }
        }
        int rows = (int) Math.ceil(items.size() / 9.0);
        return iYS + (rows * 18);
    }

    @Override
    public boolean mouseClicked(Click click, boolean inside) {
        if (this.handleMouseClicked(click.x(), click.y(), click.button())) {
            return true;
        }
        return super.mouseClicked(click, inside);
    }

    private boolean handleMouseClicked(double mx, double my, int button) {
        if (my >= guiY - 22 && my <= guiY - 4) {
            if (mx >= guiX && mx <= guiX + 50) { showAny = !showAny; scrollAmount = 0; return true; }
            if (mx >= guiX + 53 && mx <= guiX + 108) { showFuntime = !showFuntime; scrollAmount = 0; return true; }
            if (mx >= guiX + 111 && mx <= guiX + 176) { showHollyworld = !showHollyworld; scrollAmount = 0; return true; }
        }
        float curY = guiY + 18 + scrollAmount;
        if (showFuntime) curY = hGroup(AutoBuy.INSTANCE.getManager().getFuntime(), curY, mx, my, button);
        if (showHollyworld) curY = hGroup(AutoBuy.INSTANCE.getManager().getHollyworld(), curY, mx, my, button);
        if (showAny) curY = hGroup(AutoBuy.INSTANCE.getManager().getVanilla(), curY, mx, my, button);
        onMouseClicked(mx, my, button, guiX, guiY);
        return false;
    }

    private float hGroup(List<ItemBuy> items, float sY, double mx, double my, int btn) {
        float iYS = sY + 18;
        for (int i = 0; i < items.size(); i++) {
            int ix = guiX + 8 + (i % 9) * 18;
            int iy = (int) (iYS + (i / 9) * 18);
            if (iy + 18 > guiY + 17 && iy < guiY + 125 && mx >= ix && mx <= ix + 16 && my >= iy && my <= iy + 16) {
                selectedItem = items.get(i);
                editingMaxPrice = editingSellPrice = false;
                if (btn == 1) AutoBuy.INSTANCE.setItemEnabled(selectedItem, !AutoBuy.INSTANCE.isItemEnabled(selectedItem));
                if (btn == 2) AutoBuy.INSTANCE.toggleAutoParserItem(selectedItem);
            }
        }
        int rows = (int) Math.ceil(items.size() / 9.0);
        return iYS + (rows * 18);
    }

    public void onMouseClicked(double mx, double my, int button, int gx, int gy) {
        if (selectedItem == null || mx < gx + bgWidth) return;
        int px = gx + bgWidth + 5; int curY = gy + 95;
        if (isH(px+5, curY, 120, 20, mx, my)) { NetworkUtility.sendCommand("ah search " + selectedItem.getDisplayName()); setOpened(false); }
        curY += 25;
        if (isH(px+5, curY, 120, 16, mx, my)) { editingMaxPrice = true; editingSellPrice = false; currentInput = ""; }
        curY += 35;
        if (isH(px+5, curY, 120, 18, mx, my)) AutoBuy.INSTANCE.setItemAutoSellEnabled(selectedItem, !AutoBuy.INSTANCE.isItemAutoSellEnabled(selectedItem));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scrollAmount += (float) (v * 18);
        scrollAmount = MathHelper.clamp(scrollAmount, -maxScroll, 0);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (this.handleKeyPressed(keyInput.key(), keyInput.scancode(), keyInput.modifiers())) {
            return true;
        }
        return super.keyPressed(keyInput);
    }

    private boolean handleKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingMaxPrice || editingSellPrice) {
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                if (!currentInput.isEmpty() && selectedItem != null) {
                    int val = Integer.parseInt(currentInput);
                    if (editingMaxPrice) AutoBuy.INSTANCE.setItemMaxPrice(AutoBuy.INSTANCE.getItemKey(selectedItem), val);
                    else AutoBuy.INSTANCE.setItemCustomSellPrice(AutoBuy.INSTANCE.getItemKey(selectedItem), val);
                }
                editingMaxPrice = editingSellPrice = false; return true;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !currentInput.isEmpty()) {
                currentInput = currentInput.substring(0, currentInput.length() - 1); return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { this.setOpened(false); return true; }
        return false;
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        if (this.handleCharTyped(charInput.codepoint(), charInput.modifiers())) {
            return true;
        }
        return super.charTyped(charInput);
    }

    private boolean handleCharTyped(int codepoint, int modifiers) {
        if ((editingMaxPrice || editingSellPrice) && Character.isDigit(codepoint) && currentInput.length() < 9) {
            currentInput += (char) codepoint;
            return true;
        }
        return false;
    }

    private boolean isH(int x, int y, int w, int h, double mx, double my) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }
    @Override
    public boolean shouldPause() { return false; }
}