package gg.topchdlc.mixin.screen;

import gg.topchdlc.vse.shutki.screen.hud.ui.impl.main.MinecraftUI;
import gg.topchdlc.vse.shutki.module.modules.impl.render.Interface;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.gui.widget.TextIconButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PressableWidget.class)
public abstract class PressableWidgetMixin extends ClickableWidget {
    public PressableWidgetMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void topchdlc$hookRender(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (Interface.INSTANCE.isEnabled() && MinecraftUI.INSTANCE.isEnabled()) {
            ci.cancel();
            MinecraftUI.renderButton(getX(), getY(), getWidth(), getHeight(), active, isSelected(), (Object)this instanceof TextIconButtonWidget ? null : getMessage());
        }
    }
}
