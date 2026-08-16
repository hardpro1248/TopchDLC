package gg.topchdlc.api.ui.widgets.impl;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;
import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.RendererObject;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.api.ui.UIWidget;
import gg.topchdlc.api.ui.parts.impl.UITextField;
import gg.topchdlc.vse.shutki.module.settings.impl.blockcolorlist.BlockColorListRenderer;
import gg.topchdlc.vse.shutki.module.settings.impl.blockcolorlist.BlockColorListSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.animations.impl.SmoothStepAnimation;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.math.Rectangle;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BlockColorListWidget extends UIWidget {

    private final BlockColorListRenderer renderer;
    private final Drag drag = new Drag("BlockColorList", () -> true);

    private final ArrayList<BlockRow> rows = new ArrayList<>();
    private final Map<Item, Rectangle> hitboxes = new HashMap<>();

    private final Rectangle addButton = new Rectangle();
    private final Rectangle cancelButton = new Rectangle();
    private final UITextField search = new UITextField();
    private String searchText = "";

    private boolean isAdding = false;
    private float scroll, scrollAnim;
    private final SmoothStepAnimation swapAnim = new SmoothStepAnimation(300, 1);
    private BlockRow pickerRow = null;
    private ColorPickerWidget activePicker = null;

    public BlockColorListWidget(BlockColorListRenderer renderer) {
        this.renderer = renderer;
        search.setDesc("Search block...");
        search.setCallback(str -> searchText = str);
        syncRows();
    }

    public void syncRows() {
        rows.clear();
        for (var entry : renderer.getSetting().entries()) {
            rows.add(new BlockRow(entry.getKey(), entry.getValue(), renderer.getSetting()));
        }
    }
    @Override
    public void render(int mouseX, int mouseY) {
        if (drag.dragging) {
            x = mouseX - drag.dX;
            y = mouseY - drag.dY;
        }

        float pAnim = 1f - animation.getOutput();
        float pSwap = 1f - swapAnim.getOutput();

        Client.RENDERER.getStack().push();
        MathUtility.scale(Client.RENDERER.getStack(), x + width / 2f, y + height / 2f, pAnim * 0.5f + 0.5f);

        CRenderSystem sys = Client.RENDERER.getCrenderSystem();
        float prevAlpha = sys.alpha();
        sys.alpha(pAnim * prevAlpha);
        Client.RENDERER.rect(x, y, width, height, new Vector4f(6), 1,
                ClientColors.GUI_BACKGROUND, ClientColors.GUI_BACKGROUND, ClientColors.GUI_BACKGROUND, ClientColors.GUI_BACKGROUND);
        Client.RENDERER.outline(x, y, width, height, 1, new Vector4f(6), new org.joml.Vector2f(1),
                ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE);
        Client.RENDERER.text(renderer.getSetting().getName(), x + 6, y + 6, TextureUse.SFMEDIUM, 9, ClientColors.FORE_COLOR);
        Client.RENDERER.text(IconUse.CROSS, x + width - 16, y + 6, TextureUse.ICONS, 9, ClientColors.FORE_COLOR);
        Client.RENDERER.rect(x + 5, y + 23, width - 10, 1, new Vector4f(0), 1,
                ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR);

        sys.push(x, y, width, height);
        Client.RENDERER.getStack().push();
        Client.RENDERER.getStack().translate(-pSwap * width * 2, 0, 0);

        float scrollHeight = 0;
        {
            float off = 0;
            sys.push(x, y + 30, width, height - 30);
            for (BlockRow row : rows) {
                row.bound(x, y + 30 + off - scrollAnim, width, 20).render(mouseX, mouseY);
                off += 24;
            }
            scrollHeight = off;

            addButton.bound(x + 4, y + 30 + off - scrollAnim, width - 8, 20);
            sys.hatch(0.16f);
            Client.RENDERER.outline(addButton.getX(), addButton.getY(), addButton.getWidth(), addButton.getHeight(),
                    1, new Vector4f(6), new org.joml.Vector2f(1),
                    ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE);
            sys.hatch(0);
            Client.RENDERER.textCentered("+", x + width / 2f - 2, y + 27 + off - scrollAnim,
                    TextureUse.SFMEDIUM, 20, ClientColors.DARK_GRAY_COLOR);
            sys.pop();
        }
        Client.RENDERER.getStack().translate(pSwap * width * 2, 0, 0);
        Client.RENDERER.getStack().translate((1f - pSwap) * width * 2, 0, 0);
        if (pSwap > 0.1f) {
            cancelButton.bound(x + 4, y + height - 24, width - 8, 20);
            Client.RENDERER.rect(cancelButton, new Vector4f(6), 1,
                    ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE);
            Client.RENDERER.textCentered("Cancel",
                    cancelButton.getX() + cancelButton.getWidth() / 2f - 1,
                    cancelButton.getY() + cancelButton.getHeight() / 2f - 4,
                    TextureUse.SFMEDIUM, 8, ClientColors.DARK_GRAY_COLOR);

            search.bound(x + 4, y + 30, width - 8, 20).render(mouseX, mouseY);

            sys.push(x, y + 52, width, height - 52 - 24);
            float originAlpha = sys.alpha();
            float off = 0;
            for (Item item : Registries.ITEM) {
                if (!(item instanceof BlockItem)) continue;
                if (item == Items.AIR) continue;
                if (!searchText.isEmpty() && !item.getName().getString()
                        .toLowerCase().replace(" ", "")
                        .contains(searchText.toLowerCase().replace(" ", ""))) {
                    hitboxes.computeIfAbsent(item, k -> new Rectangle()).bound(-999, -999, 0, 0);
                    continue;
                }
                Rectangle rect = hitboxes.computeIfAbsent(item, k -> new Rectangle());
                if (55 + off - scrollAnim < height && off - scrollAnim + 20 > 0) {
                    rect.bound(x + 4, y + 55 + off - scrollAnim, width - 8, 20);
                    sys.alpha(originAlpha * 0.3f);
                    Client.RENDERER.rect(rect, new Vector4f(6), 1,
                            ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE);
                    sys.alpha(originAlpha);
                    try {
                        Client.RENDERER.texture(item, rect.getX() + 4, rect.getY() + 2, 16, 16, 1,
                                new Vector4f(0), Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
                    } catch (Exception ignored) {}
                    Client.RENDERER.text(item.getName(), rect.getX() + 24, rect.getY() + 5, TextureUse.SFMEDIUM, 8);
                } else {
                    rect.bound(-999, -999, 0, 0);
                }
                off += 24;
            }
            if (isAdding) scrollHeight = off - 20;
            sys.pop();
        }
        Client.RENDERER.getStack().pop();
        sys.pop();
        sys.alpha(prevAlpha);
        Client.RENDERER.getStack().pop();
        swapAnim.setDirection(isAdding ? Direction.BACKWARDS : Direction.FORWARDS);
        scrollAnim = MathUtility.linearFps(scrollAnim, scroll, 10);
        scroll = MathHelper.clamp(scroll, 0, Math.max(0, scrollHeight - (height - 30)));
        rows.removeIf(BlockRow::shouldRemove);
        if (pAnim < 0.1f && animation.getDirection() == Direction.FORWARDS) {
            shouldRemove = true;
        }
        if (activePicker != null) {
            activePicker.render(mouseX, mouseY);
        }
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (shouldRemove) return true;
        if (activePicker != null) {
            boolean consumed = activePicker.click(mouseX, mouseY, button);
            if (activePicker.shouldRemove) {
                activePicker = null;
                pickerRow = null;
            }
            if (consumed) return true;
        }

        if (MathUtility.mouseIn(x, y, width, 20, mouseX, mouseY)) {
            drag.dragging = true;
            drag.dX = mouseX - x;
            drag.dY = mouseY - y;
        }

        if (!isAdding) {
            if (addButton.hovered(mouseX, mouseY)) {
                isAdding = true;
                scroll = 0;
                return true;
            }
            for (BlockRow row : rows) {
                if (row.click(mouseX, mouseY, button)) {
                    if (row.wantsColorPicker) {
                        row.wantsColorPicker = false;
                        openPickerFor(row, mouseX, mouseY);
                    }
                    return true;
                }
            }
        } else {
            if (cancelButton.hovered(mouseX, mouseY)) {
                closeAdding();
                return true;
            }
            search.click(mouseX, mouseY, button);
            if (MathUtility.mouseIn(x, y + 52, width, height - 62, mouseX, mouseY)) {
                for (Map.Entry<Item, Rectangle> entry : hitboxes.entrySet()) {
                    if (entry.getValue().hovered(mouseX, mouseY)) {
                        Block block = ((BlockItem) entry.getKey()).getBlock();
                        renderer.getSetting().add(block, new Color(255, 0, 0, 150));
                        rows.add(new BlockRow(Registries.BLOCK.getId(block), new Color(255, 0, 0, 150), renderer.getSetting()));
                        closeAdding();
                        return true;
                    }
                }
            }
        }

        if (!hover(mouseX, mouseY) || MathUtility.mouseIn(x + width - 16, y + 6, 9, 9, mouseX, mouseY)) {
            animation.setDirection(Direction.FORWARDS);
        }
        return true;
    }

    private void openPickerFor(BlockRow row, int mouseX, int mouseY) {
        pickerRow = row;
        activePicker = new ColorPickerWidget(row.colorSetting);
        activePicker.bound(x + width + 2, row.getY(), 140, 160);
        activePicker.expanded = true;
        activePicker.shouldRemove = false;
    }

    private void closeAdding() {
        isAdding = false;
        scroll = 0;
        search.setText("");
        searchText = "";
    }

    @Override
    public void release(int button) {
        super.release(button);
        search.release(button);
        drag.dragging = false;
        if (activePicker != null) activePicker.release(button);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        super.keyPressed(keyCode, scanCode, modifiers);
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

    public static class BlockRow extends RendererObject {
        private final Identifier blockId;
        private final BlockColorListSetting setting;
        final ColorSetting colorSetting;
        boolean wantsColorPicker = false;
        private boolean remove = false;
        private final Rectangle colorRect = new Rectangle();
        BlockRow(Identifier blockId, Color color, BlockColorListSetting setting) {
            this.blockId = blockId;
            this.setting = setting;
            this.colorSetting = new ColorSetting("Color", color);
            this.colorSetting.onChanged(c -> setting.setColor(blockId, c));
        }

        boolean shouldRemove() {
            if (remove) {
                setting.remove(blockId);
                return true;
            }
            return false;
        }

        @Override
        public void render(int mouseX, int mouseY) {
            CRenderSystem sys = Client.RENDERER.getCrenderSystem();
            float originAlpha = sys.alpha();
            sys.alpha(originAlpha * 0.3f);
            Client.RENDERER.rect(x + 4, y, width - 8, 20, new Vector4f(6), 1,
                    ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE);
            sys.alpha(originAlpha);
            Item blockItem = BlockColorListSetting.getBlockItem(blockId);
            if (blockItem != null) {
                try {
                    Client.RENDERER.texture(blockItem, x + 6, y + 2, 16, 16, 1,
                            new Vector4f(0), Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
                } catch (Exception ignored) {}
            }
            Block block = net.minecraft.registry.Registries.BLOCK.get(blockId);
            Client.RENDERER.text(block.getName(), x + 26, y + 5, TextureUse.SFMEDIUM, 8);
            colorRect.bound(x + width - 22, y + 4, 12, 12);
            Color c = colorSetting.get();
            Client.RENDERER.rect(colorRect, new Vector4f(3), 1, c, c, c, c);
            Client.RENDERER.outline(colorRect.getX(), colorRect.getY(), colorRect.getWidth(), colorRect.getHeight(),
                    1, new Vector4f(3), new org.joml.Vector2f(1),
                    ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE);
        }

        @Override
        public boolean click(int mouseX, int mouseY, int button) {
            if (!hover(mouseX, mouseY)) return false;
            if (button == 1) {
                remove = true;
                return true;
            }
            if (button == 0 && colorRect.hovered(mouseX, mouseY)) {
                wantsColorPicker = true;
                return true;
            }
            return false;
        }
    }
}
