package gg.topchdlc.mixin.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gg.topchdlc.vse.shutki.module.modules.impl.render.DynamicIsland;

import java.util.Map;
import java.util.UUID;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private Map<UUID, ClientBossBar> bossBars;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void topchdlc$render(DrawContext context, CallbackInfo ci) {
        if (!DynamicIsland.INSTANCE.isEnabled()) {
            return;
        }

        ci.cancel();

        if (this.bossBars.isEmpty()) {
            return;
        }

        DynamicIsland.INSTANCE.renderBossBars(context, this.bossBars);
    }
}