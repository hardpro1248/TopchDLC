package gg.topchdlc.api.ui.widgets.impl;

import gg.topchdlc.Client;
import gg.topchdlc.api.autobuy.AutoBuyManager;
import gg.topchdlc.api.autobuy.item.ItemBuy;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.RendererObject;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.api.ui.UIWidget;
import gg.topchdlc.api.ui.parts.impl.UITextField;
import gg.topchdlc.vse.shutki.module.modules.impl.farm.AutoBuy;
import gg.topchdlc.vse.shutki.module.settings.impl.binder.BinderRenderer;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.animations.impl.SmoothStepAnimation;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.client.text.TextUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.math.Rectangle;
import lombok.Getter;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BinderWidget extends UIWidget {

    private final BinderRenderer renderer;
    public boolean expanded = false;

    private final Drag drag = new Drag("Binder", () -> true);
    public enum ItemFilter {
        ALL("All"),
        FT("FT");
        
        private final String name;
        ItemFilter(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }
    private ItemFilter currentFilter = ItemFilter.ALL;
    private Rectangle filterButton = new Rectangle();
    
    public BinderWidget(BinderRenderer renderer) {
        this.renderer = renderer;
        search.setDesc("Search item...");
        search.setCallback(str -> searchText = str);

        syncRenderers();
    }
    public void syncRenderers() {
        renderers.clear();
        for (Map.Entry<Identifier, Integer> entry: this.renderer.getSetting().getEntry()) {
            Item item = Registries.ITEM.get(entry.getKey());
            if (item != null && item != Items.AIR) {
                renderers.add(new ItemRenderer(item, this.renderer));
            }
        }
    }

    private final Map<Item, Rectangle> hitboxes = new HashMap<>();
    private ArrayList<ItemRenderer> renderers = new ArrayList<>();
    private Rectangle addButton = new Rectangle();
    private Rectangle cancelButton = new Rectangle();
    private UITextField search = new UITextField();
    private String searchText = "";

    private float scroll, scrollAnim;

    private SmoothStepAnimation swapAnim = new SmoothStepAnimation(300, 1);
    private boolean isAdding = false;

    @Override
    public void render(int mouseX, int mouseY) {
        if (drag.dragging) {
            x = mouseX - drag.dX;
            y = mouseY - drag.dY;
        }

        float pExpandAnim = 1.f - animation.getOutput();
        float pSwapAnim = 1.f - swapAnim.getOutput();
        Client.RENDERER.getStack().push();
        MathUtility.scale(Client.RENDERER.getStack(), x + width / 2f, y + height / 2f, pExpandAnim * 0.5f + 0.5f);
        CRenderSystem system = Client.RENDERER.getCrenderSystem();
        float prevAlpha = system.alpha();
        system.alpha(pExpandAnim * prevAlpha);
        Client.RENDERER.rect(x, y, width, height, new Vector4f(6), 1, ClientColors.GUI_BACKGROUND, ClientColors.GUI_BACKGROUND, ClientColors.GUI_BACKGROUND, ClientColors.GUI_BACKGROUND);
        Client.RENDERER.outline(x, y, width, height, 1, new Vector4f(6), new Vector2f(1), ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE);
        Client.RENDERER.text(this.renderer.getSetting().getName(), x + 6, y + 6, TextureUse.SFMEDIUM, 9, ClientColors.FORE_COLOR);
        filterButton.bound(x + width - 50, y + 5, 30, 10);
        Client.RENDERER.rect(filterButton, new Vector4f(3), 1, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE);
        Client.RENDERER.textCentered(currentFilter.toString(), filterButton.getX() + filterButton.getWidth() / 2f, filterButton.getY() + 2, TextureUse.SFMEDIUM, 6, ClientColors.FORE_COLOR);
        Client.RENDERER.text(IconUse.CROSS, x + width - 16, y + 6, TextureUse.ICONS, 9, ClientColors.FORE_COLOR);
        Client.RENDERER.rect(x + 5, y + 23, width - 10, 1, new Vector4f(0), 1, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR);
        system.push(x, y, width, height);
        Client.RENDERER.getStack().push();
        Client.RENDERER.getStack().translate(-pSwapAnim * width * 2, 0, 0);
        float scrollHeight = 0;

        {
            float off = 0;


            system.push(x, y + 30, width, height - 30);
            for (ItemRenderer itemRenderer : renderers) {
                itemRenderer.bound(x, y + 30 + off - scrollAnim, width, 20)
                        .render(mouseX, mouseY);
                off += itemRenderer.getHeight() + 4;
            }
            scrollHeight = off;
            addButton.bound(x + 4, y + 30 + off - scrollAnim, width - 8, 20);
            system.hatch(0.16f);
            Client.RENDERER.outline(addButton.getX(), addButton.getY(), addButton.getWidth(), addButton.getHeight(), 1, new Vector4f(6), new Vector2f(1, 1), ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE);
            system.hatch(0);
            Client.RENDERER.textCentered("+", x + width / 2f - 2, y + 27 + off - scrollAnim, TextureUse.SFMEDIUM, 20, ClientColors.DARK_GRAY_COLOR);
            system.pop();
        }
        Client.RENDERER.getStack().translate(pSwapAnim * width * 2, 0, 0);
        Client.RENDERER.getStack().translate((1.f - pSwapAnim) * width * 2, 0, 0);
        {
            if (pSwapAnim > 0.1) {
                cancelButton.bound(x + 4, y + height - 24, width - 8, 20);
                Client.RENDERER.rect(cancelButton, new Vector4f(6), 1, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE);
                Client.RENDERER.textCentered("Cancel", cancelButton.getX() + cancelButton.getWidth() / 2f - 1, cancelButton.getY() + cancelButton.getHeight() / 2f - 4, TextureUse.SFMEDIUM, 8, ClientColors.DARK_GRAY_COLOR);
                search.bound(x + 4, y + 30, width - 8, 20).render(mouseX, mouseY);
                Iterator<Item> iterator = Registries.ITEM.iterator();
                float off = 0;
                Vector4f b = new Vector4f(0);
                Vector4f c = new Vector4f(6);
                system.push(x, y + 52, width, height - 52 - 24);
                float originAlpha = system.alpha();
                ArrayList<Item> ftItems = new ArrayList<>();
                if (currentFilter == ItemFilter.FT) {
                    AutoBuyManager manager = AutoBuy.INSTANCE.getManager();
                    for (ItemBuy itemBuy : manager.getFuntime()) {
                        Item item = itemBuy.getItemStack().getItem();
                        if (item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE || 
                            item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS) {
                            continue;
                        }
                        if (item == Items.NETHERITE_SWORD || item == Items.NETHERITE_PICKAXE || 
                            item == Items.CROSSBOW || item == Items.TRIDENT || item == Items.MACE) {
                            continue;
                        }
                        if (item == Items.PLAYER_HEAD || item == Items.TOTEM_OF_UNDYING) {
                            continue;
                        }
                        ftItems.add(item);
                    }
                }
                
                while (iterator.hasNext()) {
                    Item item = iterator.next();
                    if (item == Items.AIR || (item instanceof BlockItem)) continue;
                    if (currentFilter == ItemFilter.FT && !ftItems.contains(item)) {
                        continue;
                    }
                    Rectangle rect = hitboxes.computeIfAbsent(item, k -> new Rectangle());
                    if (!searchText.isEmpty() && !item.getName().getString().toLowerCase().replace(" ", "").contains(searchText.toLowerCase().replace(" ", ""))) {
                        rect.bound(-999, -999, 0, 0);
                        continue;
                    }
                    if (55 + off - scrollAnim < height && off - scrollAnim + 20 > 0) {
                        rect.bound(x + 4, y + 55 + off - scrollAnim, width - 8, 20);
                        system.alpha(originAlpha * 0.3f);
                        Client.RENDERER.rect(rect, c, 1, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE);
                        system.alpha(originAlpha);
                        
                        try {
                            Client.RENDERER.texture(item, rect.getX() + 4, rect.getY() + 2, 16, 16, 1, new Vector4f(0), Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
                        } catch (Exception ignored) {}
                        
                        Client.RENDERER.text(item.getName(), rect.getX() + 24, rect.getY() + 5, TextureUse.SFMEDIUM, 8);
                    }
                    off += 24;
                }
                if (isAdding)
                    scrollHeight = off - 20;

                system.pop();
            }
        }
        Client.RENDERER.getStack().pop();
        system.pop();
        system.alpha(prevAlpha);
        Client.RENDERER.getStack().pop();
        swapAnim.setDirection(isAdding ? Direction.BACKWARDS : Direction.FORWARDS);
        scrollAnim = MathUtility.linearFps(scrollAnim, scroll, 10);
        scroll = MathHelper.clamp(scroll, 0, scrollHeight);
        for (ItemRenderer renderer1: renderers) {
            if (renderer1.shouldRemove) {
                this.renderer.getSetting().bind(renderer1.item, -1);
            }
        }
        renderers.removeIf(ItemRenderer::removed);
        if (pExpandAnim < 0.1 && animation.getDirection() == Direction.FORWARDS) {
            shouldRemove = true;
        }
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (shouldRemove) return true;

        boolean anyBinding = false;
        if (MathUtility.mouseIn(x, y, width, 20, mouseX, mouseY)) {
            drag.dragging = true;
            drag.dX = (mouseX - x);
            drag.dY = (mouseY - y);
        }
        if (filterButton.hovered(mouseX, mouseY) && button == 0) {
            currentFilter = currentFilter == ItemFilter.ALL ? ItemFilter.FT : ItemFilter.ALL;
            scroll = 0;
            return true;
        }
        if (!isAdding) {
            if (addButton.hovered(mouseX, mouseY)) {
                isAdding = true;
                scroll = 0;
                return true;
            }
            for (ItemRenderer itemRenderer: renderers) {
                anyBinding |= itemRenderer.isBinding();
                if (itemRenderer.click(mouseX, mouseY, button)) return true;
            }
        } else {
            if (cancelButton.hovered(mouseX, mouseY)) {
                isAdding = false;
                scroll = 0;
                search.setText("");
                searchText = "";
                return true;
            }
            search.click(mouseX, mouseY, button);
            if (MathUtility.mouseIn(x, y + 52, width, height - 62, mouseX, mouseY)) {
                for (Map.Entry<Item, Rectangle> entry : hitboxes.entrySet()) {
                    if (entry.getValue().hovered(mouseX, mouseY)) {
                        this.renderer.getSetting().bind(entry.getKey(), -1);
                        this.renderers.add(new ItemRenderer(entry.getKey(), this.renderer));
                        isAdding = false;
                        scroll = 0;
                        search.setText("");
                        searchText = "";
                        return true;
                    }
                }
            }
        }

        if ((!hover(mouseX, mouseY) || MathUtility.mouseIn(x + width - 16, y + 6, 9, 9, mouseX, mouseY)) && !anyBinding) {
            animation.setDirection(Direction.FORWARDS);
            return true;
        }
        return true;
    }

    @Override
    public void release(int button) {
        super.release(button);

        search.release(button);

        drag.dragging = false;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        super.keyPressed(keyCode, scanCode, modifiers);

        for (ItemRenderer itemRenderer: renderers) {
            itemRenderer.keyPressed(keyCode, scanCode, modifiers);
        }

        search.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void chartyped(char ch, int keyCode) {
        super.chartyped(ch, keyCode);
        search.chartyped(ch, keyCode);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (hover((int) mouseX, (int) mouseY)) {
            scroll -= (float) (verticalAmount * 10);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private static class ItemRenderer extends RendererObject {
        private Item item;

        @Getter
        private boolean binding = false;
        private BinderRenderer renderer;
        private boolean shouldRemove = false;

        private Rectangle bindbox = new Rectangle();

        public boolean removed() {
            return shouldRemove || this.renderer.getSetting().get(this.item) == -2;
        }

        public ItemRenderer(Item item, BinderRenderer renderer) {
            this.item = item;
            this.renderer = renderer;
        }

        @Override
        public void render(int mouseX, int mouseY) {
            CRenderSystem system = Client.RENDERER.getCrenderSystem();
            int bindValue = renderer.getSetting().get(item);
            String textBind = binding ? "..." : (bindValue == -1 ? "✓" : TextUtility.keyToString(bindValue));
            float textWidth = Math.max(20, Client.RENDERER.textWidth(textBind, TextureUse.SFMEDIUM, 8));
            float originAlpha = system.alpha();
            bindbox.bound(x + 4, y, width - 8, 20);
            system.alpha(originAlpha * 0.3f);
            Client.RENDERER.rect(bindbox, new Vector4f(6), 1, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE);
            system.alpha(originAlpha);
            
            try {
                Client.RENDERER.texture(item, x + 6, y + 2, 16, 16, 1, new Vector4f(0), Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
            } catch (Exception ignored) {}
            
            Client.RENDERER.text(item.getName(), x + 26, y + 5, TextureUse.SFMEDIUM, 8);
            if (bindValue == -1) {
                Client.RENDERER.drawModuleRect(x + width - textWidth - 8, y + 4, textWidth, 12);
                Client.RENDERER.textCentered(textBind, x + width - textWidth / 2f - 9, y + 6, TextureUse.SFMEDIUM, 6, ClientColors.MAIN_COLOR);
            } else if (bindValue != -2) {
                Client.RENDERER.drawModuleRect(x + width - textWidth - 8, y + 4, textWidth, 12);
                Client.RENDERER.textCentered(textBind, x + width - textWidth / 2f - 9, y + 6, TextureUse.SFMEDIUM, 6, ClientColors.DARK_GRAY_COLOR);
            }
        }

        @Override
        public boolean click(int mouseX, int mouseY, int button) {
            if (binding) {
                int mouseKeyCode = -100 - button;
                this.renderer.getSetting().bind(item, mouseKeyCode);
                binding = false;
                return true;
            }
            if (hover(mouseX, mouseY)) {
                if (button == 1) {
                    shouldRemove = true;
                    return true;
                }
                if (bindbox.hovered(mouseX, mouseY)) {
                    binding = true;
                }
            }
            return super.click(mouseX, mouseY, button);
        }

        @Override
        public void keyPressed(int keyCode, int scanCode, int modifiers) {
            if (binding) {
                if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    keyCode = -1;
                }
                this.renderer.getSetting().bind(this.item, keyCode);
                binding = false;
            }
            super.keyPressed(keyCode, scanCode, modifiers);
        }
    }
}
