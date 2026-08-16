package gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import org.joml.Vector4f;
import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick.icon.NurickIcons;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.client.mixin.IStatusEffectInstance;
import gg.topchdlc.vse.utils.client.text.TextUtility;
import gg.topchdlc.vse.utils.math.MathUtility;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
/**
 * Create by daun kvass
 */
public class EffectsElementN extends HudElement {
    private static final String TITLE       = "Potions";

    private static final float FS         = 5.5f;
    private static final float FSX         = 5.5f;
    private static final float ICON_SIZE_H = 6f;
    private static final float HEADER_H   = 12f;
    private static final float ROW_H      = 10f;
    private static final float PAD        = 6f;
    private static final float POTION_IC_S = 9f;
    private static final Vector4f ROUND   = new Vector4f(5.5f, 5.5f, 5.5f, 5.5f);

    private final List<StatusEffectInstance> effects = new ArrayList<>();
    private float widthAnim = -1f;

    public EffectsElementN(Drag drag) {
        super("EffectsN", drag);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (mc.player == null) return;

        Collection<StatusEffectInstance> localEffects = mc.player.getStatusEffects();
        for (StatusEffectInstance effect : localEffects) {
            if (effects.stream().noneMatch(e -> e.getEffectType().equals(effect.getEffectType()))) {
                effects.add(effect);
            }
        }

        float totalLerp = 0;
        for (StatusEffectInstance effect : effects) {
            IStatusEffectInstance iLerp = (IStatusEffectInstance) effect;
            boolean isActive = localEffects.stream().anyMatch(e -> e.getEffectType().equals(effect.getEffectType()));
            iLerp.setLerp(MathUtility.linearFps(iLerp.getLerp(), isActive ? 1 : 0, 10));
            totalLerp += iLerp.getLerp();
        }

        boolean shown = !effects.isEmpty() || mc.currentScreen instanceof ChatScreen;
        animation.setDirection(shown || totalLerp > 0.1 ? Direction.BACKWARDS : Direction.FORWARDS);
        float anim = 1f - animation.getOutput();

        if (anim < 0.01f) {
            if (effects.stream().allMatch(e -> ((IStatusEffectInstance) e).getLerp() < 0.01f)) {
                effects.clear();
            }
            return;
        }
        float headerIconW = Client.RENDERER.textWidth(NurickIcons.POTION, TextureUse.ICONS_NURIK, ICON_SIZE_H);
        float headerTextW = Client.RENDERER.textWidth(TITLE, TextureUse.SFMEDIUM, FSX);
        float minW = PAD + headerIconW + 5f + 0.6f + 5f + headerTextW + PAD + 10f;

        float maxRowW = minW;
        for (StatusEffectInstance effect : effects) {
            if (((IStatusEffectInstance) effect).getLerp() < 0.01f) continue;
            String name = Text.translatable(effect.getTranslationKey()).getString() + " " + getRomanAmplifier(effect);
            String time = TextUtility.ticksToTime(effect.getDuration());
            float nameW = Client.RENDERER.textWidth(name, TextureUse.SFMEDIUM, FS - 0.5f);
            float timeW = Client.RENDERER.textWidth(time, TextureUse.SFMEDIUM, FS - 0.5f);
            float currentRowW = PAD + nameW + 15f + timeW + POTION_IC_S + PAD;
            maxRowW = Math.max(maxRowW, currentRowW);
        }

        if (widthAnim < 0) widthAnim = maxRowW;
        widthAnim = MathUtility.linearFps(widthAnim, maxRowW, 10f);
        float W = widthAnim;

        float rowsHeight = 0;
        for (StatusEffectInstance effect : effects) {
            rowsHeight += (((IStatusEffectInstance) effect).getLerp()) * ROW_H;
        }
        float totalH = HEADER_H + (rowsHeight > 0 ? rowsHeight + 3f : 0);

        CRenderSystem system = Client.RENDERER.getCrenderSystem();
        float sys_a = system.alpha();
        system.alpha(anim * sys_a);
        system.push(x, y, W, totalH);
        Color bg = new Color(15, 15, 20, 160);
        Client.RENDERER.blur(x, y, W, totalH, ROUND, 12f, 1f);
        Client.RENDERER.rect(x, y, W, totalH, ROUND, 1f, bg, bg, bg, bg);
        float iconHeight = Client.FONTS.get(TextureUse.ICONS_NURIK).getMetrics().baselineHeight() * ICON_SIZE_H;
        Client.RENDERER.text(NurickIcons.POTION, x + PAD, y + (HEADER_H / 2f) - (iconHeight / 2f) + 0.5f, TextureUse.ICONS_NURIK, ICON_SIZE_H, Color.WHITE);

        float sepX = x + PAD + headerIconW + 5f;
        Color sepCol = new Color(255, 255, 255, 60);
        Client.RENDERER.rect(sepX, y + (HEADER_H - 7.5f) / 2f, 0.6f, 7.5f, new Vector4f(1), 1f, sepCol, sepCol, sepCol, sepCol);

        float titleY = y + (HEADER_H / 2f) - (Client.FONTS.get(TextureUse.SFMEDIUM).getMetrics().baselineHeight() * FS / 2f);
        Client.RENDERER.text(TITLE, sepX + 5.5f, titleY, TextureUse.SFMEDIUM, FS, Color.WHITE);
        float curY = y + HEADER_H + 1f;
        for (StatusEffectInstance effect : effects) {
            float lerp = ((IStatusEffectInstance) effect).getLerp();
            if (lerp < 0.01f) continue;

            float rH = ROW_H * lerp;
            system.alpha(anim * lerp * sys_a);

            String name = Text.translatable(effect.getTranslationKey()).getString() + " " + getRomanAmplifier(effect);
            float textY = curY + (rH / 2f) - (Client.FONTS.get(TextureUse.SFMEDIUM).getMetrics().baselineHeight() * (FS - 0.5f) / 2f);

            Client.RENDERER.text(name, x + PAD, textY, TextureUse.SFMEDIUM, FS - 0.5f, Color.WHITE);

            float iconX = x + W - PAD - POTION_IC_S;
            Client.RENDERER.textureRaw(
                    InGameHud.getEffectTexture(effect.getEffectType()).withPrefixedPath("textures/").withSuffixedPath(".png"),
                    iconX, curY + (rH - POTION_IC_S) / 2f, POTION_IC_S, POTION_IC_S, 0,
                    new Vector4f(0), Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
            String timeText = TextUtility.ticksToTime(effect.getDuration());
            float timeW = Client.RENDERER.textWidth(timeText, TextureUse.SFMEDIUM, FS - 0.5f);
            Client.RENDERER.text(timeText, iconX - timeW - 5f, textY, TextureUse.SFMEDIUM, FS - 0.5f, new Color(200, 200, 200));

            curY += rH;
        }

        system.pop();
        system.alpha(sys_a);

        drag.width = W;
        drag.height = totalH;

        effects.removeIf(e -> ((IStatusEffectInstance) e).getLerp() <= 0.05F && !localEffects.contains(e));
    }

    private String getRomanAmplifier(StatusEffectInstance effect) {
        int amplifier = effect.getAmplifier();
        if (amplifier == 0) return "";
        if (amplifier < 0 || amplifier >= 10) return String.valueOf(amplifier + 1);
        return new String[]{"", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"}[amplifier];
    }
}