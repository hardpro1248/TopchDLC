package gg.topchdlc.mixin.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.ShaderUse;

import java.awt.*;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
    protected TitleScreenMixin() {
        super(Text.empty());
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        if (Client.IS_PANIC) {
            return;
        }

        MinecraftClient.getInstance().setScreen(new gg.topchdlc.vse.shutki.screen.screens.main.TitleScreen());
        Client.RENDERER.getCrenderSystem().shader(ShaderUse.MSDF).texture(0).uv(0, 0, 1, 1).rect(0, 0, 200, 200).color(Color.GREEN, Color.GREEN, Color.GREEN, Color.GREEN).build();
    }
}