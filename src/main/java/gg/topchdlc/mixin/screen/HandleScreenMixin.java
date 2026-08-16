package gg.topchdlc.mixin.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gg.topchdlc.vse.shutki.module.modules.impl.render.BetterMinecraft;
/**
 * Create by daun kvass
 */
@Mixin(HandledScreen.class)
public abstract class HandleScreenMixin extends Screen {

    protected HandleScreenMixin(net.minecraft.text.Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void topchdlc$animHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        BetterMinecraft mod = BetterMinecraft.INSTANCE;
        if (!mod.isEnabled() || !mod.inventoryAnim.get()) return;

        float t = mod.getInventoryAnim();
        if (t <= 0.001f) {
            ci.cancel();
            return;
        }
        float scale = 0.92f + 0.08f * t;
        float cx = this.width / 2f;
        float cy = this.height / 2f;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(-cx, -cy);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void topchdlc$animTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        BetterMinecraft mod = BetterMinecraft.INSTANCE;
        if (!mod.isEnabled() || !mod.inventoryAnim.get()) return;
        if (mod.getInventoryAnim() <= 0.001f) return;

        context.getMatrices().popMatrix();
    }
}
