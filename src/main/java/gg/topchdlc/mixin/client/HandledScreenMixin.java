package gg.topchdlc.mixin.client;

import gg.topchdlc.api.autobuy.AutoBuyUtil;
import gg.topchdlc.api.autobuy.item.ItemBuy;
import gg.topchdlc.api.events.list.EventMouseDrag;
import gg.topchdlc.api.events.list.EventScroll;
import gg.topchdlc.mixin.accessor.IHandledScreen;
import gg.topchdlc.vse.shutki.module.modules.impl.misc.ItemScroller;
import net.minecraft.client.input.KeyInput;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.text.Text;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.*;
import gg.topchdlc.Client;
import gg.topchdlc.api.autobuy.SellHistoryItem;
import gg.topchdlc.api.events.list.EventClickSlot;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.autobuy.PurchaseHistoryItem;
import gg.topchdlc.vse.shutki.module.modules.impl.helper.AHHelper;
import gg.topchdlc.vse.shutki.module.modules.impl.farm.AutoBuy;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import gg.topchdlc.vse.utils.client.text.ChatUtility;

import java.awt.Color;
import java.util.List;

@Mixin(HandledScreen.class)
public class HandledScreenMixin extends Screen {
    @Shadow
    @Final
    protected ScreenHandler handler;
    @Unique
    @Mutable
    private boolean isAuc;
    @Unique
    @Mutable
    private Slot lowSumSlotId = null;
    @Unique
    @Mutable
    private Slot lowAllSumSlotId = null;

    @Unique
    private ButtonWidget autoBuyButton = null;
    @Unique
    private ButtonWidget parserButton = null;
    @Unique
    private float historyScroll = 0;
    @Unique
    private float historyScrollAnim = 0;
    @Unique
    private PurchaseHistoryItem historyHoveredItem = null;
    @Unique
    private SellHistoryItem sellHistoryHoveredItem = null;
    @Unique
    private float sellHistoryScroll = 0;
    @Unique
    private long lastAhTickMs = 0L;
    @Unique
    private float sellHistoryScrollAnim = 0;

    public HandledScreenMixin(Text title) {
        super(title);
    }


    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
    private void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        EventClickSlot event = EventClickSlot.build(this.handler.syncId, slot != null ? slot.id : slotId, button, actionType);
        EventClickSlot.postedFromScreen = true;
        Client.EVENTS.post(event);
        EventClickSlot.postedFromScreen = false;
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "handledScreenTick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        if (!isAuc && AHHelper.INSTANCE.isEnabled()) {
            isAuc = AutoBuyUtil.isAuction(this.handler);
        }

        long now = System.currentTimeMillis();
        if (now - lastAhTickMs < 100L) return;
        lastAhTickMs = now;

        if (isAuc && AHHelper.INSTANCE.isEnabled()) {
            long lowestTotalPrice = Long.MAX_VALUE;

            lowSumSlotId = null;
            lowAllSumSlotId = null;

            if (AHHelper.INSTANCE.isPricePerUnit()) {
                java.util.Map<String, java.util.List<Slot>> itemGroups = new java.util.HashMap<>();
                for (int i = 0; i < Math.min(44, handler.slots.size()); i++) {
                    Slot slot = handler.slots.get(i);
                    if (slot.getStack().isEmpty()) continue;
                    long totalPrice = AutoBuyUtil.getPrice(slot.getStack());
                    if (totalPrice == Long.MAX_VALUE) continue;
                    if (totalPrice < lowestTotalPrice) {
                        lowestTotalPrice = totalPrice;
                        lowSumSlotId = slot;
                    }
                    String itemKey = slot.getStack().getName().getString();
                    itemGroups.computeIfAbsent(itemKey, k -> new java.util.ArrayList<>()).add(slot);
                }
                for (java.util.List<Slot> slots : itemGroups.values()) {
                    if (slots.size() > 1) {
                        Slot cheapestPerUnit = null;
                        long lowestPricePerUnit = Long.MAX_VALUE;
                        for (Slot slot : slots) {
                            long totalPrice = AutoBuyUtil.getPrice(slot.getStack());
                            int count = slot.getStack().getCount();
                            if (count > 0) {
                                long pricePerUnit = totalPrice / count;
                                if (pricePerUnit < lowestPricePerUnit) {
                                    lowestPricePerUnit = pricePerUnit;
                                    cheapestPerUnit = slot;
                                }
                            }
                        }
                        if (cheapestPerUnit != null) {
                            lowAllSumSlotId = cheapestPerUnit;
                        }
                    }
                }
            } else {
                long lowestPricePerUnit = Long.MAX_VALUE;
                for (int i = 0; i < Math.min(44, handler.slots.size()); i++) {
                    Slot slot = handler.slots.get(i);
                    if (slot.getStack().isEmpty()) continue;
                    long totalPrice = AutoBuyUtil.getPrice(slot.getStack());
                    if (totalPrice == Long.MAX_VALUE) continue;
                    int count = slot.getStack().getCount();
                    int maxStackSize = slot.getStack().getMaxCount();
                    if (totalPrice < lowestTotalPrice) {
                        lowestTotalPrice = totalPrice;
                        lowSumSlotId = slot;
                    }
                    if (maxStackSize > 1 && count > 0) {
                        long pricePerUnit = totalPrice / count;
                        if (pricePerUnit < lowestPricePerUnit) {
                            lowestPricePerUnit = pricePerUnit;
                            lowAllSumSlotId = slot;
                        }
                    }
                }
            }
        } else if (!AHHelper.INSTANCE.isEnabled()) {
            isAuc = false;
            lowSumSlotId = null;
            lowAllSumSlotId = null;
        }
    }

    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void topchdlc$drawSlot(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (!AHHelper.INSTANCE.isEnabled()) return;
        if (!isAuc) return;

        if (slot == lowAllSumSlotId) {
            AHHelper.INSTANCE.renderCheap(context, slot);
        } else if (slot == lowSumSlotId) {
            AHHelper.INSTANCE.renderGood(context, slot);
        }
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void topchdlc$drawMouseoverTooltip(DrawContext context, int x, int y, CallbackInfo ci) {
        if (!AHHelper.INSTANCE.isEnabled() || !AHHelper.INSTANCE.isShowPricePerUnit() || !isAuc) return;
        Slot slot = ((IHandledScreen) this).getFocusedSlot();
        if (slot == null || slot.getStack().isEmpty()) return;
        net.minecraft.item.ItemStack stack = slot.getStack();
        long totalPrice = AutoBuyUtil.getPrice(stack);
        if (totalPrice == Long.MAX_VALUE) return;
        int count = stack.getCount();
        if (count <= 1) return;
        long pricePerUnit = totalPrice / count;
        net.minecraft.item.tooltip.TooltipType tooltipType = MinecraftClient.getInstance().options.advancedItemTooltips
                ? net.minecraft.item.tooltip.TooltipType.ADVANCED
                : net.minecraft.item.tooltip.TooltipType.BASIC;
        java.util.List<Text> lines = stack.getTooltip(
                net.minecraft.item.Item.TooltipContext.create(MinecraftClient.getInstance().world),
                MinecraftClient.getInstance().player,
                tooltipType
        );

        Text pricePerUnitText = Text.literal("§7Цена за единицу: §a$" + formatPrice(pricePerUnit));
        int insertIdx = -1;
        for (int i = 0; i < lines.size(); i++) {
            String lineStr = lines.get(i).getString();
            if (lineStr.contains("Цена:") || lineStr.contains("$")) {
                insertIdx = i + 1;
                break;
            }
        }
        if (insertIdx >= 0 && insertIdx <= lines.size()) {
            lines.add(insertIdx, pricePerUnitText);
        } else {
            lines.add(pricePerUnitText);
        }
        context.drawTooltip(MinecraftClient.getInstance().textRenderer, lines, x, y);
        ci.cancel();
    }

    @Unique
    private String formatPrice(long price) {
        if (price >= 1_000_000) return String.format("%.1fM", price / 1_000_000.0);
        if (price >= 1_000) return String.format("%.1fK", price / 1_000.0);
        return String.valueOf(price);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void topchdlc$init(CallbackInfo ci) {
        if (!((Object)this instanceof GenericContainerScreen)) return;
        if (!AutoBuy.INSTANCE.isEnabled()) return;
        int x = (this.width - 176) / 2 + 176 + 5;
        int y = (this.height - 166) / 2;
        autoBuyButton = ButtonWidget.builder(
                Text.literal(AutoBuy.INSTANCE.isAutoBuyPaused() ? "AutoBuy: OFF" : "AutoBuy: ON"),
                button -> {
                    AutoBuy.INSTANCE.setAutoBuyPaused(!AutoBuy.INSTANCE.isAutoBuyPaused());
                    button.setMessage(Text.literal(AutoBuy.INSTANCE.isAutoBuyPaused() ? "AutoBuy: OFF" : "AutoBuy: ON"));
                    ChatUtility.send(Text.literal("AutoBuy: " + (AutoBuy.INSTANCE.isAutoBuyPaused() ? "Выключен" : "Включен")).withColor((AutoBuy.INSTANCE.isAutoBuyPaused() ? new java.awt.Color(255, 100, 100) : new java.awt.Color(100, 255, 100)).getRGB()));
                }
        ).dimensions(x, y, 80, 20).build();
        parserButton = ButtonWidget.builder(
                Text.literal(AutoBuy.INSTANCE.isParserRunning() ? "Parser: ON" : "Parser: OFF"),
                button -> {
                    if (AutoBuy.INSTANCE.isParserRunning()) {
                        AutoBuy.INSTANCE.stopParser();
                    } else {
                        java.util.List<ItemBuy> targets = AutoBuy.INSTANCE.getAutoParserItems();
                        if (!targets.isEmpty()) {
                            AutoBuy.INSTANCE.startParserForItems(targets);
                        }
                    }
                    button.setMessage(Text.literal(AutoBuy.INSTANCE.isParserRunning() ? "Parser: ON" : "Parser: OFF"));
                }
        ).dimensions(x, y + 25, 80, 20).build();

        this.addDrawableChild(autoBuyButton);
        this.addDrawableChild(parserButton);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void topchdlc$render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (autoBuyButton != null && AutoBuy.INSTANCE.isEnabled()) {
            autoBuyButton.setMessage(Text.literal(AutoBuy.INSTANCE.isAutoBuyPaused() ? "AutoBuy: OFF" : "AutoBuy: ON"));
        }
        if (parserButton != null && AutoBuy.INSTANCE.isEnabled()) {
            parserButton.setMessage(Text.literal(AutoBuy.INSTANCE.isParserRunning() ? "Parser: ON" : "Parser: OFF"));
        }

        if (!isAuc || !AutoBuy.INSTANCE.isEnabled() || Client.AUTOBUY_ITEMS == null || !Client.INITIALIZED) return;
        List<PurchaseHistoryItem> history = Client.AUTOBUY_ITEMS.getPurchaseHistory();
        int guiW = 176;
        int guiH = 222;
        int guiX = (this.width - guiW) / 2;
        int guiY = (this.height - guiH) / 2;

        float panelW = 90;
        float panelH = guiH;
        float panelX = guiX - panelW - 4;
        float panelY = guiY;
        Color bg = new Color(15, 15, 20, 210);
        Client.RENDERER.blur(panelX, panelY, panelW, panelH, new Vector4f(8), 12f, 1f);
        Client.RENDERER.rect(panelX, panelY, panelW, panelH, new Vector4f(8), 1, bg, bg, bg, bg);
        Color border = new Color(255, 255, 255, 20);
        Client.RENDERER.outline(panelX, panelY, panelW, panelH, 0.5f, new Vector4f(8), new Vector2f(1), border, border, border, border);
        Client.RENDERER.textCentered("История", panelX + panelW / 2f, panelY + 8, TextureUse.SFMEDIUM, 6f, Color.WHITE);
        long total = 0;
        for (PurchaseHistoryItem h : history) total += h.price;
        String totalStr = "$" + formatHistoryPrice((int) Math.min(total, Integer.MAX_VALUE));
        float totalW2 = Client.RENDERER.textWidth(totalStr, TextureUse.SFMEDIUM, 5f);
        Client.RENDERER.text(totalStr, panelX + (panelW - totalW2) / 2f, panelY + 18, TextureUse.SFMEDIUM, 5f, new Color(100, 220, 100));

        if (history.isEmpty()) {
            Client.RENDERER.textCentered("Пусто", panelX + panelW / 2f, panelY + panelH / 2f, TextureUse.SFMEDIUM, 5.5f, new Color(80, 80, 80));
            return;
        }

        float listY = panelY + 30;
        float listH2 = panelH - 35;
        float slotSize = 20;
        float slotGap = 3;
        int cols = 4;
        float startX = panelX + (panelW - (cols * slotSize + (cols - 1) * slotGap)) / 2f;

        historyScrollAnim += (historyScroll - historyScrollAnim) * 0.3f;

        Client.RENDERER.getCrenderSystem().push(panelX, listY, panelW, listH2);

        historyHoveredItem = null;
        for (int i = 0; i < history.size(); i++) {
            PurchaseHistoryItem item = history.get(i);
            int row = i / cols;
            int col = i % cols;
            float sx = startX + col * (slotSize + slotGap);
            float sy = listY + row * (slotSize + slotGap) + historyScrollAnim;

            if (sy + slotSize < listY || sy > listY + listH2) continue;

            boolean hovered = mouseX >= sx && mouseX <= sx + slotSize && mouseY >= sy && mouseY <= sy + slotSize;
            Color slotBg = hovered ? new Color(60, 60, 75, 200) : new Color(30, 30, 40, 160);
            Client.RENDERER.rect(sx, sy, slotSize, slotSize, new Vector4f(4), 1, slotBg, slotBg, slotBg, slotBg);

            try {
                if (item.itemStack != null && !item.itemStack.isEmpty()) {
                    var matrices = context.getMatrices();
                    matrices.pushMatrix();
                    float scale = (slotSize - 4) / 16f;
                    matrices.translate(sx + 2, sy + 2);
                    matrices.scale(scale, scale);
                    context.drawItem(item.itemStack, 0, 0);
                    matrices.popMatrix();
                }
            } catch (Exception ignored) {}

            if (hovered) historyHoveredItem = item;
        }

        Client.RENDERER.getCrenderSystem().pop();

        int totalRows = (int) Math.ceil((double) history.size() / cols);
        float maxScroll = Math.max(0, totalRows * (slotSize + slotGap) - listH2 + 5);
        historyScroll = Math.max(-maxScroll, Math.min(0, historyScroll));
        if (historyHoveredItem != null) {
            java.util.List<Text> lines = new java.util.ArrayList<>();
            lines.add(Text.literal(historyHoveredItem.auctionName != null ? historyHoveredItem.auctionName : "?"));
            if (historyHoveredItem.itemStack != null && !historyHoveredItem.itemStack.isEmpty()) {
                LoreComponent lore = historyHoveredItem.itemStack.get(net.minecraft.component.DataComponentTypes.LORE);
                if (lore != null) {
                    for (Text line : lore.lines()) {
                        lines.add(line);
                    }
                }
            }
            lines.add(Text.empty());
            lines.add(Text.literal("§aЦена: $" + formatHistoryPrice(historyHoveredItem.price)));
            lines.add(Text.literal("§7" + formatHistoryTime(historyHoveredItem.timestamp)));
            context.drawTooltip(MinecraftClient.getInstance().textRenderer, lines, mouseX, mouseY);
        }

        java.util.List<SellHistoryItem> sellHist = Client.AUTOBUY_ITEMS.getSellHistory();
        float spW = 90;
        float spH = guiH;
        float spX = guiX + guiW + 4;
        float spY = guiY;
        Color spBg = new Color(15, 15, 20, 210);
        Client.RENDERER.blur(spX, spY, spW, spH, new Vector4f(8), 12f, 1f);
        Client.RENDERER.rect(spX, spY, spW, spH, new Vector4f(8), 1, spBg, spBg, spBg, spBg);
        Client.RENDERER.outline(spX, spY, spW, spH, 0.5f, new Vector4f(8), new Vector2f(1), border, border, border, border);
        Client.RENDERER.textCentered("Продажи", spX + spW / 2f, spY + 8, TextureUse.SFMEDIUM, 6f, Color.WHITE);

        long totalProfit = 0;
        for (SellHistoryItem sh : sellHist) totalProfit += sh.getProfit();
        String profitStr = (totalProfit >= 0 ? "+" : "") + "$" + formatHistoryPrice((int) Math.min(Math.abs(totalProfit), Integer.MAX_VALUE));
        float profitW = Client.RENDERER.textWidth(profitStr, TextureUse.SFMEDIUM, 5f);
        Color profitColor = totalProfit >= 0 ? new Color(100, 220, 100) : new Color(220, 100, 100);
        Client.RENDERER.text(profitStr, spX + (spW - profitW) / 2f, spY + 18, TextureUse.SFMEDIUM, 5f, profitColor);

        if (sellHist.isEmpty()) {
            Client.RENDERER.textCentered("Пусто", spX + spW / 2f, spY + spH / 2f, TextureUse.SFMEDIUM, 5.5f, new Color(80, 80, 80));
        } else {
            float sListY = spY + 30;
            float sListH = spH - 35;
            float sSlotSize = 20;
            float sSlotGap = 3;
            int sCols = 4;
            float sStartX = spX + (spW - (sCols * sSlotSize + (sCols - 1) * sSlotGap)) / 2f;

            sellHistoryScrollAnim += (sellHistoryScroll - sellHistoryScrollAnim) * 0.3f;
            Client.RENDERER.getCrenderSystem().push(spX, sListY, spW, sListH);

            sellHistoryHoveredItem = null;
            for (int i = 0; i < sellHist.size(); i++) {
                SellHistoryItem sh = sellHist.get(i);
                int row = i / sCols;
                int col = i % sCols;
                float sx2 = sStartX + col * (sSlotSize + sSlotGap);
                float sy2 = sListY + row * (sSlotSize + sSlotGap) + sellHistoryScrollAnim;

                if (sy2 + sSlotSize < sListY || sy2 > sListY + sListH) continue;

                boolean hov = mouseX >= sx2 && mouseX <= sx2 + sSlotSize && mouseY >= sy2 && mouseY <= sy2 + sSlotSize;
                Color slotBg2 = hov ? new Color(60, 60, 75, 200) : new Color(30, 30, 40, 160);
                Client.RENDERER.rect(sx2, sy2, sSlotSize, sSlotSize, new Vector4f(4), 1, slotBg2, slotBg2, slotBg2, slotBg2);

                try {
                    if (sh.itemStack != null && !sh.itemStack.isEmpty()) {
                        var matrices = context.getMatrices();
                        matrices.pushMatrix();
                        float scale = (sSlotSize - 4) / 16f;
                        matrices.translate(sx2 + 2, sy2 + 2);
                        matrices.scale(scale, scale);
                        context.drawItem(sh.itemStack, 0, 0);
                        matrices.popMatrix();
                    }
                } catch (Exception ignored) {}

                if (hov) sellHistoryHoveredItem = sh;
            }

            Client.RENDERER.getCrenderSystem().pop();

            int sTotalRows = (int) Math.ceil((double) sellHist.size() / sCols);
            float sMaxScroll = Math.max(0, sTotalRows * (sSlotSize + sSlotGap) - sListH + 5);
            sellHistoryScroll = Math.max(-sMaxScroll, Math.min(0, sellHistoryScroll));
        }

        if (sellHistoryHoveredItem != null) {
            java.util.List<Text> lines2 = new java.util.ArrayList<>();
            lines2.add(Text.literal(sellHistoryHoveredItem.itemName != null ? sellHistoryHoveredItem.itemName : "?"));
            if (sellHistoryHoveredItem.itemStack != null && !sellHistoryHoveredItem.itemStack.isEmpty()) {
                LoreComponent lore2 = sellHistoryHoveredItem.itemStack.get(net.minecraft.component.DataComponentTypes.LORE);
                if (lore2 != null) {
                    for (Text line : lore2.lines()) lines2.add(line);
                }
            }
            lines2.add(Text.empty());
            lines2.add(Text.literal("§eПродано: $" + formatHistoryPrice(sellHistoryHoveredItem.sellPrice)));
            if (sellHistoryHoveredItem.buyPrice > 0) {
                lines2.add(Text.literal("§7Куплено: $" + formatHistoryPrice(sellHistoryHoveredItem.buyPrice)));
                int profit = sellHistoryHoveredItem.getProfit();
                String profitLine = (profit >= 0 ? "§aПрибыль: +$" : "§cУбыток: -$") + formatHistoryPrice(Math.abs(profit));
                lines2.add(Text.literal(profitLine));
            }
            lines2.add(Text.literal("§7" + formatHistoryTime(sellHistoryHoveredItem.timestamp)));
            context.drawTooltip(MinecraftClient.getInstance().textRenderer, lines2, mouseX, mouseY);
        }


        int totalW = 176 + 130;
        int gx = (this.width - totalW) / 2;
        int gy = (this.height - 222) / 2;
        if (Client.AUTOBUY_ITEMS != null && Client.AUTOBUY_ITEMS.isOpened()) {
            Client.AUTOBUY_ITEMS.drawAutoBuyInterface(context, mouseX, mouseY, gx, gy);
        }
    }

    @Unique
    private String formatHistoryPrice(int price) {
        if (price >= 1_000_000) return (price / 1_000_000) + "M";
        if (price >= 1_000) return (price / 1_000) + "K";
        return String.valueOf(price);
    }

    @Unique
    private String formatHistoryTime(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        if (diff < 60_000) return (diff / 1000) + "с назад";
        if (diff < 3_600_000) return (diff / 60_000) + "м назад";
        return (diff / 3_600_000) + "ч назад";
    }
    @Inject(method = "removed", at = @At("HEAD"))
    private void topchdlc$removed(CallbackInfo ci) {
        autoBuyButton = null;
        parserButton = null;
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void topchdlc$mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount, CallbackInfoReturnable<Boolean> cir) {
        if (Client.EVENTS.post(new EventScroll(hAmount, vAmount)).isCancelled()) {
            cir.setReturnValue(true);
            return;
        }

        if (!isAuc || !AutoBuy.INSTANCE.isEnabled() || Client.AUTOBUY_ITEMS == null) return;

        int guiW = 176;
        int guiX = (this.width - guiW) / 2;
        float panelW = 90;
        float panelX = guiX - panelW - 4;

        if (mouseX >= panelX && mouseX <= panelX + panelW) {
            historyScroll += (float) vAmount * 18;
            cir.setReturnValue(true);
        }
        int guiW2 = 176;
        int guiX2 = (this.width - guiW2) / 2;
        float spW = 90;
        float spX = guiX2 + guiW2 + 4;
        if (mouseX >= spX && mouseX <= spX + spW) {
            sellHistoryScroll += (float) vAmount * 18;
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void topchdlc$mouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (Client.AUTOBUY_ITEMS != null && Client.AUTOBUY_ITEMS.isOpened()) {
            double mouseX = click.x();
            double mouseY = click.y();
            int button = click.button();
            int gx = (this.width - 176 - 130) / 2;
            int gy = (this.height - 222) / 2;

            Client.AUTOBUY_ITEMS.onMouseClicked(mouseX, mouseY, button, gx, gy);

            if (mouseX >= gx && mouseX <= gx + 176 + 130 && mouseY >= gy && mouseY <= gy + 222) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void topchdlc$keyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (ItemScroller.INSTANCE
                .onInventoryKeyPressed((HandledScreen<?>) (Object) this, input.key())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void topchdlc$mouseDragged(Click click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> cir) {
        EventMouseDrag event = new EventMouseDrag(click.x(), click.y(), click.button(), offsetX, offsetY);
        Client.EVENTS.post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(true);
        }
    }
}