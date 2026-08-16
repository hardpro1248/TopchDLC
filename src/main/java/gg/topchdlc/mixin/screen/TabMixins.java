package gg.topchdlc.mixin.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gg.topchdlc.vse.shutki.module.modules.impl.render.BetterMinecraft;
/**
 * Create by daun kvass
 */
@Mixin(InGameHud.class)
public class TabMixins {
    @Redirect(
        method = "renderPlayerList",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"),
        require = 0
    )
    private boolean topchdlc$forceTabDuringAnim(KeyBinding keyBinding) {
        BetterMinecraft mod = BetterMinecraft.INSTANCE;
        if (mod.isEnabled() && mod.tabAnim.get() && mod.getTabAnim() > 0.001f) {
            return true;
        }
        return keyBinding.isPressed();
    }

    @Inject(method = "renderPlayerList", at = @At("HEAD"), cancellable = true)
    private void topchdlc$tabHead(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        BetterMinecraft mod = BetterMinecraft.INSTANCE;
        if (!mod.isEnabled() || !mod.tabAnim.get()) return;

        float t = mod.getTabAnim();
        if (t <= 0.001f) {
            ci.cancel();
            return;
        }
        float offsetY = -60f * (1f - t);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0, offsetY);
    }

    @Inject(method = "renderPlayerList", at = @At("TAIL"))
    private void topchdlc$tabTail(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        BetterMinecraft mod = BetterMinecraft.INSTANCE;
        if (!mod.isEnabled() || !mod.tabAnim.get()) return;
        if (mod.getTabAnim() <= 0.001f) return;

        context.getMatrices().popMatrix();
    }
}
