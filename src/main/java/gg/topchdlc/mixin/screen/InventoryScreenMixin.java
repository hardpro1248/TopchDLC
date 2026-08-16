package gg.topchdlc.mixin.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gg.topchdlc.vse.shutki.module.modules.impl.render.BetterMinecraft;

import static gg.topchdlc.MinecraftHolder.mc;

/**
 * Create by daun kvass
 */
@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void topchdlc$invHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        BetterMinecraft mod = BetterMinecraft.INSTANCE;
        if (!mod.isEnabled() || !mod.inventoryAnim.get()) return;

        float t = mod.getInventoryAnim();
        if (t <= 0.001f) {
            ci.cancel();
            return;
        }
        float scale = 0.92f + 0.08f * t;
        float cx = mc.getWindow().getScaledWidth() / 2f;
        float cy = mc.getWindow().getScaledHeight() / 2f;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(-cx, -cy);
    }

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void topchdlc$invTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        BetterMinecraft mod = BetterMinecraft.INSTANCE;
        if (!mod.isEnabled() || !mod.inventoryAnim.get()) return;
        if (mod.getInventoryAnim() <= 0.001f) return;

        context.getMatrices().popMatrix();
    }
}
