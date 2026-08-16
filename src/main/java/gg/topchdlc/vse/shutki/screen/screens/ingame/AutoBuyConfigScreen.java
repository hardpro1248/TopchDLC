package gg.topchdlc.vse.shutki.screen.screens.ingame;

import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.autobuy.item.ItemBuy;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.RendererObject;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.vse.shutki.module.modules.impl.farm.AutoBuy;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.animations.impl.SmoothStepAnimation;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.MathUtility;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AutoBuyConfigScreen extends RendererObject implements MinecraftHolder {
    private boolean isOpened = false;
    public Drag drag;
    private SmoothStepAnimation animation;
    
    private float WIDTH = 700;
    private float HEIGHT = 450;
    private final float CATEGORY_WIDTH = 120;
    private final float ITEM_WIDTH = 250;
    private final float SETTINGS_WIDTH = 280;
    
    private ItemBuy.Category currentCategory = ItemBuy.Category.FUNTIME;
    private ItemBuy selectedItem = null;
    
    private float itemScroll = 0;
    private float itemScrollAnim = 0;
    
    private String priceInput = "";
    private boolean priceInputFocused = false;
    
    public AutoBuyConfigScreen() {
        this.drag = new Drag("AutoBuyConfig", () -> this.isOpened);
        this.animation = new SmoothStepAnimation(200, 1);
        
        drag.x = window.getScaledWidth() / 2f - WIDTH / 2f;
        drag.y = window.getScaledHeight() / 2f - HEIGHT / 2f;
        drag.width = WIDTH;
        drag.height = HEIGHT;
    }
    
    public void setOpened(boolean opened) {
        this.isOpened = opened;
        if (opened) {
            if (selectedItem != null) {
                priceInput = String.valueOf(AutoBuy.INSTANCE.getItemMaxPrice(selectedItem.getDisplayName()));
            }
        } else {
            priceInputFocused = false;
        }
    }
    
    public boolean isOpened() {
        return isOpened;
    }
    
    @Override
    public void render(int mouseX, int mouseY) {
        if (!Client.INITIALIZED) return;
        
        Client.RENDERER.getCrenderSystem().layer(CRenderSystem.RenderLayer.OVERLAY);
        
        animation.setDirection(isOpened ? Direction.BACKWARDS : Direction.FORWARDS);
        float anim = 1f - animation.getOutput();
        if (anim < 0.05f) return;
        
        if (drag.dragging) {
            drag.x = MathUtility.linear(drag.x, mouseX - drag.dX, 0.5f);
            drag.y = MathUtility.linear(drag.y, mouseY - drag.dY, 0.5f);
            float screenWidth = window.getScaledWidth();
            float screenHeight = window.getScaledHeight();
            drag.x = Math.max(0, Math.min(drag.x, screenWidth - drag.width));
            drag.y = Math.max(0, Math.min(drag.y, screenHeight - drag.height));
        }
        
        itemScrollAnim = MathUtility.linearFps(itemScrollAnim, itemScroll, 10f);
        
        MatrixStack stack = Client.RENDERER.getStack();
        Client.RENDERER.getCrenderSystem().alpha(anim);
        
        stack.push();
        
        try {
            MathUtility.scale(stack, drag.x + WIDTH / 2f, drag.y + HEIGHT / 2f, anim * 0.05f + 0.95f);

            Client.RENDERER.blur(drag.x, drag.y, WIDTH, HEIGHT, new Vector4f(14), 30f, 1f);
            Color dimColor = new Color(0, 0, 0, (int)(160 * anim));
            Client.RENDERER.rect(drag.x, drag.y, WIDTH, HEIGHT, new Vector4f(14), 1, dimColor, dimColor, dimColor, dimColor);
            Color glassBg = new Color(25, 25, 30, 200);
            Client.RENDERER.rect(drag.x, drag.y, WIDTH, HEIGHT, new Vector4f(14), 1, glassBg, glassBg, glassBg, glassBg);
            Color borderColor = new Color(255, 255, 255, 20);
            Client.RENDERER.outline(drag.x, drag.y, WIDTH, HEIGHT, 0.5f, new Vector4f(14), new Vector2f(1), borderColor, borderColor, borderColor, borderColor);
            renderHeader(mouseX, mouseY);
            renderCategories(mouseX, mouseY);
            renderItems(mouseX, mouseY);
            if (selectedItem != null) {
                renderSettings(mouseX, mouseY);
            }
            
        } finally {
            stack.pop();
            Client.RENDERER.getCrenderSystem().alpha(1f);
        }
    }
    
    private void renderHeader(int mouseX, int mouseY) {
        float headerX = drag.x + 10;
        float headerY = drag.y + 10;

        Client.RENDERER.text("AutoBuy Configuration", headerX, headerY, TextureUse.SFMEDIUM, 9, Color.WHITE);
        float closeX = drag.x + WIDTH - 30;
        float closeY = drag.y + 10;
        float closeSize = 20;
        
        boolean closeHovered = MathUtility.mouseIn(closeX, closeY, closeSize, closeSize, mouseX, mouseY);
        Color closeBg = closeHovered ? new Color(255, 60, 60, 180) : new Color(255, 60, 60, 120);
        Client.RENDERER.rect(closeX, closeY, closeSize, closeSize, new Vector4f(6), 1, closeBg, closeBg, closeBg, closeBg);
        Client.RENDERER.text(IconUse.GEAR, closeX + 5, closeY + 4, TextureUse.ICONS, 8, Color.WHITE);
    }
    
    private void renderCategories(int mouseX, int mouseY) {
        float catX = drag.x + 10;
        float catY = drag.y + 45;
        float catW = CATEGORY_WIDTH;
        float catH = HEIGHT - 55;

        Color catBg = new Color(0, 0, 0, 255);
        Client.RENDERER.rect(catX, catY, catW, catH, new Vector4f(10), 1, catBg, catBg, catBg, catBg);
        Color catBorder = new Color(255, 255, 255, 10);
        Client.RENDERER.outline(catX, catY, catW, catH, 0.5f, new Vector4f(10), new Vector2f(1), catBorder, catBorder, catBorder, catBorder);
        float btnY = catY + 10;
        float btnH = 30;
        float btnGap = 5;
        for (ItemBuy.Category cat : ItemBuy.Category.values()) {
            boolean hovered = MathUtility.mouseIn(catX + 8, btnY, catW - 16, btnH, mouseX, mouseY);
            boolean selected = cat == currentCategory;
            if (selected) {
                Color accent = ClientSettings.INSTANCE.getColor(0);
                Client.RENDERER.rect(catX + 8, btnY, 3, btnH, new Vector4f(1.5f, 0, 0, 1.5f), 1, accent, accent, accent, accent);
            }
            Color textColor;
            if (selected) {
                textColor = Color.WHITE;
                Color glowColor = new Color(255, 255, 255, 50);
                Client.RENDERER.text(cat.name(), catX + 20, btnY + 10, TextureUse.SFMEDIUM, 7, glowColor);
            } else if (hovered) {
                textColor = new Color(200, 200, 200);
            } else {
                textColor = new Color(140, 140, 140);
            }
            Client.RENDERER.text(cat.name(), catX + 20, btnY + 10, TextureUse.SFMEDIUM, 7, textColor);
            
            btnY += btnH + btnGap;
        }
    }
    
    private void renderItems(int mouseX, int mouseY) {
        List<ItemBuy> items = getFilteredItems();
        
        float contentX = drag.x + CATEGORY_WIDTH + 15;
        float contentY = drag.y + 45;
        float contentWidth = ITEM_WIDTH;
        float contentHeight = HEIGHT - 55;
        Color itemsBg = new Color(18, 18, 22, 180);
        Client.RENDERER.blur(contentX, contentY, contentWidth, contentHeight, new Vector4f(10), 20f, 1f);
        Client.RENDERER.rect(contentX, contentY, contentWidth, contentHeight, new Vector4f(10), 1, itemsBg, itemsBg, itemsBg, itemsBg);
        Color itemsBorder = new Color(255, 255, 255, 10);
        Client.RENDERER.outline(contentX, contentY, contentWidth, contentHeight, 0.5f, new Vector4f(10), new Vector2f(1), itemsBorder, itemsBorder, itemsBorder, itemsBorder);
        Client.RENDERER.text("Items (" + items.size() + ")", contentX + 10, contentY + 10, TextureUse.SFMEDIUM, 7.5f, Color.WHITE);
        Client.RENDERER.getCrenderSystem().push(contentX + 8, contentY + 30, contentWidth - 16, contentHeight - 35);
        float offsetY = itemScrollAnim;
        float itemHeight = 35;
        float itemGap = 3;
        for (ItemBuy item : items) {
            float itemY = contentY + 30 + offsetY;
            boolean hovered = MathUtility.mouseIn(contentX + 8, itemY, contentWidth - 16, itemHeight, mouseX, mouseY);
            boolean isSelected = selectedItem == item;
            boolean enabled = AutoBuy.INSTANCE.isItemEnabled(item.getDisplayName());
            if (hovered || isSelected) {
                Client.RENDERER.blur(contentX + 8, itemY, contentWidth - 16, itemHeight, new Vector4f(6), 12f, 1f);
            }
            Color cardBg;
            if (isSelected) {
                cardBg = new Color(255, 255, 255, 25);
            } else if (enabled) {
                cardBg = hovered ? new Color(75, 255, 75, 30) : new Color(75, 255, 75, 15);
            } else {
                cardBg = hovered ? new Color(30, 30, 35, 180) : new Color(0, 0, 0, 0);
            }
            Client.RENDERER.rect(contentX + 8, itemY, contentWidth - 16, itemHeight, new Vector4f(6), 1, cardBg, cardBg, cardBg, cardBg);
            try {
                net.minecraft.item.ItemStack stack = item.getItemStack();
                if (stack != null && !stack.isEmpty()) {
                    float iconX = contentX + 12;
                    float iconY = itemY + 9;
                    float iconSize = 16;
                    renderItemStack(stack, iconX, iconY, iconSize);
                }
            } catch (Exception ignored) {}
            Color textColor = enabled ? Color.WHITE : new Color(150, 150, 150);
            String displayName = item.getDisplayName();
            if (displayName.length() > 25) {
                displayName = displayName.substring(0, 22) + "...";
            }
            Client.RENDERER.text(displayName, contentX + 35, itemY + 7, TextureUse.SFMEDIUM, 6.5f, textColor);
            int maxPrice = AutoBuy.INSTANCE.getItemMaxPrice(item.getDisplayName());
            String priceText = "Max: $" + formatPrice(maxPrice);
            Client.RENDERER.text(priceText, contentX + 35, itemY + 19, TextureUse.SFMEDIUM, 5.5f, new Color(150, 150, 150));
            float checkX = contentX + contentWidth - 30;
            float checkY = itemY + 10;
            float checkSize = 15;
            Color checkBg = enabled ? ClientSettings.INSTANCE.getColor(0) : new Color(60, 60, 60);
            Client.RENDERER.rect(checkX, checkY, checkSize, checkSize, new Vector4f(3), 1, checkBg, checkBg, checkBg, checkBg);
            if (enabled) {
                Client.RENDERER.text(IconUse.CHECK, checkX + 3, checkY + 2, TextureUse.ICONS, 7, Color.WHITE);
            }
            
            offsetY += itemHeight + itemGap;
        }
        
        float maxScroll = Math.max(0, items.size() * (itemHeight + itemGap) - contentHeight + 40);
        itemScroll = Math.max(-maxScroll, Math.min(0, itemScroll));
        
        Client.RENDERER.getCrenderSystem().pop();
    }
    
    private void renderSettings(int mouseX, int mouseY) {
        float contentX = drag.x + CATEGORY_WIDTH + ITEM_WIDTH + 20;
        float contentY = drag.y + 45;
        float contentWidth = SETTINGS_WIDTH;
        float contentHeight = HEIGHT - 55;
        Color settingsBg = new Color(18, 18, 22, 180);
        Client.RENDERER.blur(contentX, contentY, contentWidth, contentHeight, new Vector4f(10), 20f, 1f);
        Client.RENDERER.rect(contentX, contentY, contentWidth, contentHeight, new Vector4f(10), 1, settingsBg, settingsBg, settingsBg, settingsBg);
        Color settingsBorder = new Color(255, 255, 255, 10);
        Client.RENDERER.outline(contentX, contentY, contentWidth, contentHeight, 0.5f, new Vector4f(10), new Vector2f(1), settingsBorder, settingsBorder, settingsBorder, settingsBorder);
        Client.RENDERER.text("Settings", contentX + 10, contentY + 10, TextureUse.SFMEDIUM, 7.5f, Color.WHITE);
        float settingsY = contentY + 35;
        if (selectedItem == null) {
            Client.RENDERER.textCentered("Выберите предмет", contentX + contentWidth / 2f, settingsY + 50, TextureUse.SFMEDIUM, 7, new Color(150, 150, 150));
            return;
        }
        try {
            net.minecraft.item.ItemStack stack = selectedItem.getItemStack();
            if (stack != null && !stack.isEmpty()) {
                float iconSize = 48;
                float iconX = contentX + (contentWidth - iconSize) / 2f;
                renderItemStack(stack, iconX, settingsY, iconSize);
                settingsY += iconSize + 15;
            }
        } catch (Exception ignored) {}
        Client.RENDERER.textCentered(selectedItem.getDisplayName(), contentX + contentWidth / 2f, settingsY, TextureUse.SFMEDIUM, 7, Color.WHITE);
        settingsY += 25;
        Client.RENDERER.text("Enabled:", contentX + 15, settingsY, TextureUse.SFMEDIUM, 6.5f, new Color(200, 200, 200));
        boolean enabled = AutoBuy.INSTANCE.isItemEnabled(selectedItem.getDisplayName());
        float toggleX = contentX + contentWidth - 60;
        float toggleW = 45;
        float toggleH = 22;
        boolean toggleHovered = MathUtility.mouseIn(toggleX, settingsY - 3, toggleW, toggleH, mouseX, mouseY);
        Color toggleBg = enabled ? ClientSettings.INSTANCE.getColor(0) : new Color(60, 60, 60);
        if (toggleHovered) {
            toggleBg = new Color(toggleBg.getRed(), toggleBg.getGreen(), toggleBg.getBlue(), Math.min(255, toggleBg.getAlpha() + 40));
        }
        Client.RENDERER.rect(toggleX, settingsY - 3, toggleW, toggleH, new Vector4f(11), 1, toggleBg, toggleBg, toggleBg, toggleBg);
        String toggleText = enabled ? "ON" : "OFF";
        Client.RENDERER.textCentered(toggleText, toggleX + toggleW / 2f, settingsY + 3, TextureUse.SFMEDIUM, 6, Color.WHITE);
        settingsY += 35;
        Client.RENDERER.text("Max Price ($):", contentX + 15, settingsY, TextureUse.SFMEDIUM, 6.5f, new Color(200, 200, 200));
        settingsY += 20;
        float inputX = contentX + 15;
        float inputW = contentWidth - 30;
        float inputH = 30;
        boolean inputHovered = MathUtility.mouseIn(inputX, settingsY, inputW, inputH, mouseX, mouseY);
        Color inputBg = priceInputFocused ? new Color(255, 255, 255, 25) : new Color(255, 255, 255, 12);
        Client.RENDERER.rect(inputX, settingsY, inputW, inputH, new Vector4f(8), 1, inputBg, inputBg, inputBg, inputBg);
        Color inputBorder = new Color(255, 255, 255, priceInputFocused ? 50 : 25);
        Client.RENDERER.outline(inputX, settingsY, inputW, inputH, 0.5f, new Vector4f(8), new Vector2f(1), inputBorder, inputBorder, inputBorder, inputBorder);
        String displayText = priceInputFocused ? priceInput : String.valueOf(AutoBuy.INSTANCE.getItemMaxPrice(selectedItem.getDisplayName()));
        if (displayText.isEmpty() && priceInputFocused) {
            displayText = "Enter price...";
            Client.RENDERER.text(displayText, inputX + 10, settingsY + 10, TextureUse.SFMEDIUM, 6.5f, new Color(100, 100, 100));
        } else {
            Client.RENDERER.text(displayText, inputX + 10, settingsY + 10, TextureUse.SFMEDIUM, 6.5f, Color.WHITE);
        }
        if (priceInputFocused && System.currentTimeMillis() % 1000 < 500) {
            float cursorX = inputX + 10 + Client.RENDERER.textWidth(priceInput, TextureUse.SFMEDIUM, 6.5f);
            Client.RENDERER.rect(cursorX, settingsY + 8, 1, 14, new Vector4f(0), 1, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
        }
        settingsY += 45;
        float applyW = 100;
        float applyH = 28;
        float applyX = contentX + (contentWidth - applyW) / 2f;
        boolean applyHovered = MathUtility.mouseIn(applyX, settingsY, applyW, applyH, mouseX, mouseY);
        Color applyBg = applyHovered ? new Color(75, 200, 75, 200) : new Color(75, 200, 75, 150);
        Client.RENDERER.rect(applyX, settingsY, applyW, applyH, new Vector4f(8), 1, applyBg, applyBg, applyBg, applyBg);
        Client.RENDERER.textCentered("Apply", applyX + applyW / 2f, settingsY + 9, TextureUse.SFMEDIUM, 7, Color.WHITE);
        settingsY += 40;
        Client.RENDERER.text("Quick Set:", contentX + 15, settingsY, TextureUse.SFMEDIUM, 6f, new Color(150, 150, 150));
        settingsY += 18;
        
        int[] quickPrices = {100, 500, 1000, 5000, 10000, 50000};
        float btnW = (contentWidth - 40) / 3f - 5;
        float btnH = 24;
        float btnX = contentX + 15;
        
        for (int i = 0; i < quickPrices.length; i++) {
            int price = quickPrices[i];
            
            if (i > 0 && i % 3 == 0) {
                btnX = contentX + 15;
                settingsY += btnH + 5;
            }
            
            boolean btnHovered = MathUtility.mouseIn(btnX, settingsY, btnW, btnH, mouseX, mouseY);
            Color btnBg = btnHovered ? new Color(80, 80, 90, 200) : new Color(60, 60, 70, 150);
            Client.RENDERER.rect(btnX, settingsY, btnW, btnH, new Vector4f(6), 1, btnBg, btnBg, btnBg, btnBg);
            
            String priceText = formatPrice(price);
            Client.RENDERER.textCentered("$" + priceText, btnX + btnW / 2f, settingsY + 7, TextureUse.SFMEDIUM, 6f, Color.WHITE);
            
            btnX += btnW + 5;
        }
    }
    
    private List<ItemBuy> getFilteredItems() {
        List<ItemBuy> allItems = AutoBuy.INSTANCE.getAllItems();
        List<ItemBuy> filtered = new ArrayList<>();
        
        for (ItemBuy item : allItems) {
            if (item.getCategory() == currentCategory) {
                filtered.add(item);
            }
        }
        
        return filtered;
    }
    
    private String formatPrice(int price) {
        if (price >= 1000000) {
            return (price / 1000000) + "M";
        } else if (price >= 1000) {
            return (price / 1000) + "K";
        }
        return String.valueOf(price);
    }
    
    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (!isOpened) return false;
        float closeX = drag.x + WIDTH - 30;
        float closeY = drag.y + 10;
        float closeSize = 20;
        if (MathUtility.mouseIn(closeX, closeY, closeSize, closeSize, mouseX, mouseY)) {
            setOpened(false);
            mc.mouse.lockCursor();
            return true;
        }
        float catX = drag.x + 10;
        float catY = drag.y + 45;
        float catW = CATEGORY_WIDTH;
        float btnY = catY + 10;
        float btnH = 30;
        float btnGap = 5;
        
        for (ItemBuy.Category cat : ItemBuy.Category.values()) {
            if (MathUtility.mouseIn(catX + 8, btnY, catW - 16, btnH, mouseX, mouseY)) {
                currentCategory = cat;
                itemScroll = 0;
                selectedItem = null;
                return true;
            }
            btnY += btnH + btnGap;
        }
        List<ItemBuy> items = getFilteredItems();
        float contentX = drag.x + CATEGORY_WIDTH + 15;
        float contentY = drag.y + 45;
        float contentWidth = ITEM_WIDTH;
        float contentHeight = HEIGHT - 55;
        float offsetY = itemScrollAnim;
        float itemHeight = 35;
        float itemGap = 3;
        float scrollAreaX = contentX + 8;
        float scrollAreaY = contentY + 30;
        float scrollAreaWidth = contentWidth - 16;
        float scrollAreaHeight = contentHeight - 35;
        
        if (MathUtility.mouseIn(scrollAreaX, scrollAreaY, scrollAreaWidth, scrollAreaHeight, mouseX, mouseY)) {
            for (ItemBuy item : items) {
                float itemY = contentY + 30 + offsetY;
                if (itemY + itemHeight < scrollAreaY || itemY > scrollAreaY + scrollAreaHeight) {
                    offsetY += itemHeight + itemGap;
                    continue;
                }
                float checkX = contentX + contentWidth - 30;
                float checkY = itemY + 10;
                float checkSize = 15;
                
                if (MathUtility.mouseIn(checkX, checkY, checkSize, checkSize, mouseX, mouseY)) {
                    boolean enabled = AutoBuy.INSTANCE.isItemEnabled(item.getDisplayName());
                    AutoBuy.INSTANCE.setItemEnabled(item.getDisplayName(), !enabled);
                    return true;
                }
                if (MathUtility.mouseIn(contentX + 8, itemY, contentWidth - 16, itemHeight, mouseX, mouseY)) {
                    selectedItem = item;
                    priceInput = String.valueOf(AutoBuy.INSTANCE.getItemMaxPrice(item.getDisplayName()));
                    priceInputFocused = false;
                    return true;
                }
                
                offsetY += itemHeight + itemGap;
            }
        }
        if (selectedItem != null) {
            float settingsX = drag.x + CATEGORY_WIDTH + ITEM_WIDTH + 20;
            float settingsY = drag.y + 45;
            float settingsWidth = SETTINGS_WIDTH;
            float toggleX = settingsX + settingsWidth - 60;
            float toggleY = settingsY + 35 + 48 + 15 + 25 - 3;
            float toggleW = 45;
            float toggleH = 22;
            if (MathUtility.mouseIn(toggleX, toggleY, toggleW, toggleH, mouseX, mouseY)) {
                boolean enabled = AutoBuy.INSTANCE.isItemEnabled(selectedItem.getDisplayName());
                AutoBuy.INSTANCE.setItemEnabled(selectedItem.getDisplayName(), !enabled);
                return true;
            }
            float inputX = settingsX + 15;
            float inputY = settingsY + 35 + 48 + 15 + 25 + 35 + 20;
            float inputW = settingsWidth - 30;
            float inputH = 30;
            
            if (MathUtility.mouseIn(inputX, inputY, inputW, inputH, mouseX, mouseY)) {
                priceInputFocused = true;
                priceInput = String.valueOf(AutoBuy.INSTANCE.getItemMaxPrice(selectedItem.getDisplayName()));
                return true;
            }
            float applyW = 100;
            float applyH = 28;
            float applyX = settingsX + (settingsWidth - applyW) / 2f;
            float applyY = inputY + 45;
            
            if (MathUtility.mouseIn(applyX, applyY, applyW, applyH, mouseX, mouseY)) {
                try {
                    int price = Integer.parseInt(priceInput);
                    if (price > 0) {
                        AutoBuy.INSTANCE.setItemMaxPrice(selectedItem.getDisplayName(), price);
                    }
                } catch (NumberFormatException ignored) {}
                priceInputFocused = false;
                return true;
            }
            int[] quickPrices = {100, 500, 1000, 5000, 10000, 50000};
            float quickBtnW = (settingsWidth - 40) / 3f - 5;
            float quickBtnH = 24;
            float quickBtnX = settingsX + 15;
            float quickBtnY = applyY + 40 + 18;
            
            for (int i = 0; i < quickPrices.length; i++) {
                int price = quickPrices[i];
                
                if (i > 0 && i % 3 == 0) {
                    quickBtnX = settingsX + 15;
                    quickBtnY += quickBtnH + 5;
                }
                if (MathUtility.mouseIn(quickBtnX, quickBtnY, quickBtnW, quickBtnH, mouseX, mouseY)) {
                    AutoBuy.INSTANCE.setItemMaxPrice(selectedItem.getDisplayName(), price);
                    priceInput = String.valueOf(price);
                    return true;
                }
                
                quickBtnX += quickBtnW + 5;
            }
        }
        if (MathUtility.mouseIn(drag.x, drag.y, WIDTH, 40, mouseX, mouseY)) {
            drag.dX = mouseX - drag.x;
            drag.dY = mouseY - drag.y;
            drag.dragging = true;
            return true;
        }
        
        return false;
    }
    
    @Override
    public void release(int button) {
        drag.dragging = false;
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isOpened) return false;
        float contentX = drag.x + CATEGORY_WIDTH + 15;
        float contentY = drag.y + 45;
        float contentWidth = ITEM_WIDTH;
        float contentHeight = HEIGHT - 55;
        
        if (MathUtility.mouseIn(contentX, contentY, contentWidth, contentHeight, (int)mouseX, (int)mouseY)) {
            itemScroll += (float) verticalAmount * 20;
            return true;
        }
        
        return true;
    }
    
    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isOpened) return;
        
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (priceInputFocused) {
                priceInputFocused = false;
            } else {
                setOpened(false);
                mc.mouse.lockCursor();
            }
            return;
        }
        
        if (!priceInputFocused) return;
        
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !priceInput.isEmpty()) {
            priceInput = priceInput.substring(0, priceInput.length() - 1);
        } else if (keyCode == GLFW.GLFW_KEY_ENTER) {
            try {
                int price = Integer.parseInt(priceInput);
                if (price > 0 && selectedItem != null) {
                    AutoBuy.INSTANCE.setItemMaxPrice(selectedItem.getDisplayName(), price);
                }
            } catch (NumberFormatException ignored) {}
            priceInputFocused = false;
        }
    }
    
    @Override
    public void chartyped(char ch, int keyCode) {
        if (!isOpened || !priceInputFocused) return;
        
        if (Character.isDigit(ch)) {
            priceInput += ch;
        }
    }
    
    private void renderItemStack(net.minecraft.item.ItemStack stack, float x, float y, float size) {
        if (stack == null || stack.isEmpty()) return;
    }
}
