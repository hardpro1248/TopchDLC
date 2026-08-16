package gg.topchdlc.mixin.render;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gg.topchdlc.vse.shutki.module.modules.impl.misc.NameProtect;
import gg.topchdlc.vse.shutki.module.modules.impl.render.Interface;

@Mixin(InGameHud.class)
public abstract class ScoreboardSidebarMixin {


    @Inject(
            method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderScoreboard(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        if (Interface.INSTANCE.isEnabled() && Interface.INSTANCE.hudStyle.is(Interface.HudStyle.Solution) && Interface.INSTANCE.nursultanElements.get(Interface.HudElements.NursultanScoreboard)) {
            ci.cancel();
        }
    }


    @Redirect(
            method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)V"
            ),
            require = 0
    )
    private void nameprotect$sidebarDrawText1(DrawContext ctx, TextRenderer tr, Text text, int x, int y, int color, boolean shadow) {
        ctx.drawText(tr, replaceNick(text), x, y, color, shadow);
    }

    @Redirect(
            method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)V"
            ),
            require = 0
    )
    private void nameprotect$sidebarDrawText2(DrawContext ctx, TextRenderer tr, Text text, int x, int y, int color, boolean shadow) {
        ctx.drawText(tr, replaceNick(text), x, y, color, shadow);
    }

    private static Text replaceNick(Text text) {
        if (text == null) return null;
        NameProtect np = NameProtect.INSTANCE;
        if (!np.isEnabled()) return text;
        String str = text.getString();
        String replaced = np.replace(str);
        if (replaced.equals(str)) return text;
        return Text.literal(replaced).setStyle(text.getStyle());
    }
}