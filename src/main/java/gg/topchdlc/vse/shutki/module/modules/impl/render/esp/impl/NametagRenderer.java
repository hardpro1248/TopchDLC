package gg.topchdlc.vse.shutki.module.modules.impl.render.esp.impl;

import gg.topchdlc.vse.utils.client.get.HealthUtility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.module.modules.impl.misc.NameProtect;
import gg.topchdlc.vse.shutki.module.modules.impl.render.esp.ESPRenderer;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.text.Text;
import org.joml.Vector4f;

import java.awt.*;

public class NametagRenderer extends ESPRenderer {

    public NametagRenderer(Align align) {
        super(align);
    }

    @Override
    public Color render(float minX, float minY, float maxX, float maxY, float width, Entity entity) {
        Text baseText = entity.getCustomName() == null ? entity.getDisplayName() : entity.getCustomName();
        if (entity instanceof ItemEntity e) {
            baseText = e.getStack().getName();
        }

        String protectedNameStr = NameProtect.INSTANCE.getEntityName(entity);
        boolean nameChanged = !protectedNameStr.equals(entity.getName().getString());

        MutableText finalText;
        if (nameChanged) {
            finalText = Text.literal(protectedNameStr);
        } else {
            finalText = baseText.copy();
        }

        if (entity instanceof LivingEntity living) {
            int hp = (int) Math.ceil(HealthUtility.get(living));
            int abs = (int) Math.ceil(living.getAbsorptionAmount());

            finalText.append(Text.literal(" [").formatted(Formatting.GRAY));
            finalText.append(Text.literal(String.valueOf(hp)).formatted(Formatting.RED));

            if (abs > 0) {
                finalText.append(Text.literal("+" + abs).formatted(Formatting.YELLOW));
            }

            finalText.append(Text.literal("]").formatted(Formatting.GRAY));
        }
        String cleanString = Formatting.strip(finalText.getString()).trim();
        float textwidth = Client.RENDERER.textWidth(cleanString, TextureUse.SFMEDIUM, 6.5F);
        float textwidth2 = textwidth + 4F;
        float espwidth = (maxX - minX);
        float f = Client.RENDERER.getCrenderSystem().alpha();
        Color color = entity instanceof AbstractClientPlayerEntity p && Client.FRIENDS.isFriend(p) ? ClientColors.FRIEND_COLOR : ClientColors.BACK_COLOR;
        float bgX = minX + espwidth / 2F - textwidth2 / 2F;
        Client.RENDERER.blur(bgX, minY - 16, textwidth2, 10, new Vector4f(0), 5, 0);
        Client.RENDERER.getCrenderSystem().alpha(0.3F);
        Client.RENDERER.rect(bgX, minY - 16, textwidth2, 10, new Vector4f(0), 1, color, color, color, color);
        Client.RENDERER.getCrenderSystem().alpha(f);
        Client.RENDERER.text(finalText, minX + espwidth / 2F - textwidth / 2F, minY - 15, TextureUse.SFMEDIUM, 6.5F);

        return color;
    }
}