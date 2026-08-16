package gg.topchdlc.vse.shutki.module.modules.impl.render.esp.impl;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.module.modules.impl.render.ESP;
import gg.topchdlc.vse.shutki.module.modules.impl.render.esp.ESPRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.StringHelper;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;

/**
 * Create by daun kvass
 */
public class ItemPlayerRenderer extends ESPRenderer {

    public ItemPlayerRenderer(Align align) {
        super(align);
    }

    @Override
    public Color render(float minX, float minY, float maxX, float maxY, float width, Entity entity) {
        if (!(entity instanceof PlayerEntity player)) return null;
        
        ArrayList<ItemStack> itemsToRender = new ArrayList<>();
        ItemStack offHand = player.getOffHandStack();
        if (!offHand.isEmpty()) itemsToRender.add(offHand);
        if (ESP.INSTANCE.renderArmor.get()) {
            ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
            ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);
            ItemStack leggings = player.getEquippedStack(EquipmentSlot.LEGS);
            ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
            
            if (!helmet.isEmpty()) itemsToRender.add(helmet);
            if (!chestplate.isEmpty()) itemsToRender.add(chestplate);
            if (!leggings.isEmpty()) itemsToRender.add(leggings);
            if (!boots.isEmpty()) itemsToRender.add(boots);
        }

        ItemStack mainHand = player.getMainHandStack();

        if (!mainHand.isEmpty()) itemsToRender.add(mainHand);
        if (!itemsToRender.isEmpty()) {
            float itemSize = 11;
            float boxSize = 13;
            float spacing = 2;
            int itemCount = itemsToRender.size();
            float totalWidth = (boxSize * itemCount) + (spacing * (itemCount - 1));
            float espWidth = (maxX - minX);
            float centerX = minX + espWidth / 2F;
            float startX = centerX - totalWidth / 2F;
            float itemY = minY - boxSize - 15;
            float currentX = startX;
            Vector4f round = new Vector4f(3, 3, 3, 3);

            for (ItemStack item : itemsToRender) {
                Color bgColor = new Color(0, 0, 0, 40);
                Client.RENDERER.rect(currentX, itemY, boxSize, boxSize, round, 1, bgColor, bgColor, bgColor, bgColor);

                float itemX = currentX + (boxSize - itemSize) / 2f;
                float itemYPos = itemY + (boxSize - itemSize) / 2f;
                Client.RENDERER.itemStack(item, itemX, itemYPos, itemSize);

                if (item.isDamageable()) {
                    float durability = 1.0f - ((float) item.getDamage() / item.getMaxDamage());
                    if (durability < 1.0f) {
                        float barH = 1.5f;
                        float barY = itemY - barH - 1.5f;
                        Color durCol = Color.getHSBColor(durability / 3f, 1f, 1f);
                        Color barBg = new Color(0, 0, 0, 160);

                        Client.RENDERER.rect(currentX, barY, boxSize, barH, new Vector4f(0), 1, barBg, barBg, barBg, barBg);
                        Client.RENDERER.rect(currentX, barY, boxSize * durability, barH, new Vector4f(0), 1, durCol, durCol, durCol, durCol);
                    }
                }

                currentX += boxSize + spacing;
            }
        }
        if (!mainHand.isEmpty() || !offHand.isEmpty()) {
            String mainHandName = mainHand.isEmpty() ? "" : StringHelper.stripTextFormat(mainHand.getName().getString());
            String offHandName = offHand.isEmpty() ? "" : StringHelper.stripTextFormat(offHand.getName().getString());
            Color mainHandColor = getItemNameColor(mainHand);
            Color offHandColor = getItemNameColor(offHand);
            
            float espWidth = (maxX - minX);
            float centerX = minX + espWidth / 2F;
            
            if (!mainHandName.isEmpty() && !offHandName.isEmpty()) {
                float mainWidth = Client.RENDERER.textWidth(mainHandName, TextureUse.SFMEDIUM, 6.5F);
                float separatorWidth = Client.RENDERER.textWidth(" | ", TextureUse.SFMEDIUM, 6.5F);
                float offWidth = Client.RENDERER.textWidth(offHandName, TextureUse.SFMEDIUM, 6.5F);
                float totalWidth = mainWidth + separatorWidth + offWidth;
                float currentX = centerX - totalWidth / 2F;
                
                Client.RENDERER.text(mainHandName, currentX, maxY + 2, TextureUse.SFMEDIUM, 6.5F, mainHandColor);
                currentX += mainWidth;
                
                Client.RENDERER.text(" | ", currentX, maxY + 2, TextureUse.SFMEDIUM, 6.5F, new Color(150, 150, 150));
                currentX += separatorWidth;
                
                Client.RENDERER.text(offHandName, currentX, maxY + 2, TextureUse.SFMEDIUM, 6.5F, offHandColor);
            } else if (!mainHandName.isEmpty()) {
                float textWidth = Client.RENDERER.textWidth(mainHandName, TextureUse.SFMEDIUM, 6.5F);
                Client.RENDERER.text(mainHandName, centerX - textWidth / 2F, maxY + 2, TextureUse.SFMEDIUM, 6.5F, mainHandColor);
            } else if (!offHandName.isEmpty()) {
                float textWidth = Client.RENDERER.textWidth(offHandName, TextureUse.SFMEDIUM, 6.5F);
                Client.RENDERER.text(offHandName, centerX - textWidth / 2F, maxY + 2, TextureUse.SFMEDIUM, 6.5F, offHandColor);
            }
        }
        return null;
    }
    private Color getItemNameColor(ItemStack stack) {
        if (stack.isEmpty()) return Color.WHITE;

        try {
            String name = stack.getName().getString();
            String cleanName = StringHelper.stripTextFormat(name);

            if (name.length() >= 2 && name.charAt(0) == '&') {
                char colorCode = name.charAt(1);
                Color color = getColorFromCode(colorCode);
                if (color != null) return color;
            }

            var style = stack.getName().getStyle();
            if (style != null && style.getColor() != null) {
                return new Color(style.getColor().getRgb());
            }
            if (stack.isOf(net.minecraft.item.Items.PLAYER_HEAD)) {
                return new Color(255, 174, 0);
            }

            if (stack.isOf(net.minecraft.item.Items.TOTEM_OF_UNDYING) && stack.hasEnchantments()) {
                return new Color(255, 0, 0);
            }
            if (isSpecialItem(stack, cleanName)) {
                return new Color(0, 158, 141);
            }

        } catch (Exception e) {

        }

        return Color.WHITE;
    }

    private Color getColorFromCode(char code) {
        return switch (code) {
            case '0' -> new Color(0, 0, 0);
            case '1' -> new Color(0, 0, 170);
            case '2' -> new Color(0, 170, 0);
            case '3' -> new Color(0, 170, 170);
            case '4' -> new Color(170, 0, 0);
            case '5' -> new Color(170, 0, 170);
            case '6' -> new Color(255, 170, 0);
            case '7' -> new Color(170, 170, 170);
            case '8' -> new Color(85, 85, 85);
            case '9' -> new Color(85, 85, 255);
            case 'a' -> new Color(85, 255, 85);
            case 'b' -> new Color(85, 255, 255);
            case 'c' -> new Color(255, 85, 85);
            case 'd' -> new Color(255, 85, 255);
            case 'e' -> new Color(255, 255, 85);
            case 'f' -> new Color(255, 255, 255);
            default -> null;
        };
    }
    
    private boolean isSpecialItem(ItemStack stack, String itemName) {
        if (stack.isEmpty()) return false;

        if (stack.getItem() == Items.PLAYER_HEAD) {
            return true;
        }

        if (itemName != null && !itemName.isEmpty()) {
            String lowerName = itemName.toLowerCase();
            return lowerName.contains("сфера") || lowerName.contains("талисман") || 
                   lowerName.contains("sphere") || lowerName.contains("talisman");
        }
        
        return false;
    }
}
