package gg.topchdlc.vse.shutki.module.modules.impl.render.esp.impl;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.module.modules.impl.render.esp.ESPRenderer;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.client.text.TextUtility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import org.joml.Vector4f;

import java.awt.*;

public class EffectRenderer extends ESPRenderer {
    public EffectRenderer(Align align) {
        super(align);
    }

    @Override
    public Color render(float minX, float minY, float maxX, float maxY, float width, Entity entity) {
        float off = 5;
        if (entity instanceof LivingEntity e) {
            for (StatusEffectInstance s : e.getStatusEffects()) {
                if (s.getDuration() == 0) continue;

                String text = Text.translatable(s.getTranslationKey()).getString();
                String duration = TextUtility.ticksToTime(s.getDuration());
                float textWidth = Client.RENDERER.textWidth(text, TextureUse.SFMEDIUM, 7);
                float durWidth = Client.RENDERER.textWidth(duration, TextureUse.SFMEDIUM, 7);
                float wd = (maxX - minX);
                float f = Client.RENDERER.getCrenderSystem().alpha();

                float alwd = (textWidth + durWidth) + 24;

                Client.RENDERER.blur(minX + wd / 2F - alwd / 2F - 2.5F, maxY + off - 1, alwd, 10.5F, new Vector4f(0), 5, 0);
                Client.RENDERER.getCrenderSystem().alpha(0.3F);
                Client.RENDERER.rect(minX + wd / 2F - alwd / 2F - 2.5F, maxY + off - 1, alwd, 10.5F, new Vector4f(0), 1, ClientColors.BACK_COLOR, ClientColors.BACK_COLOR, ClientColors.BACK_COLOR, ClientColors.BACK_COLOR);
                Client.RENDERER.getCrenderSystem().alpha(f);

                Client.RENDERER.textCentered(String.format("%s %s", text, s.getAmplifier() + 1), minX + wd / 2F - durWidth / 2F - 2, maxY + off, TextureUse.SFMEDIUM, 7, new Color(s.getEffectType().value().getColor()));
                Client.RENDERER.text(duration, minX + wd / 2F + textWidth / 2F - 6, maxY + off, TextureUse.SFMEDIUM, 7, ClientColors.FORE_COLOR);

                boolean beneficial = s.getEffectType().value().isBeneficial();

                Client.RENDERER.text(beneficial ? IconUse.UP : IconUse.DOWN, minX + wd / 2F - textWidth / 2F - 12 - durWidth / 2F, maxY + off, TextureUse.ICONS, 7,
                        beneficial ? ClientColors.GREEN : ClientColors.UI_RED);

                off += 10;
            }
        }
        return null;
    }
}
