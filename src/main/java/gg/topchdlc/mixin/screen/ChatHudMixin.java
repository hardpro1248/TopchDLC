package gg.topchdlc.mixin.screen;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.topchdlc.vse.shutki.module.modules.impl.render.BetterMinecraft;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.IdentityHashMap;
import java.util.Map;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Unique
    private static final Map<ChatHudLine.Visible, Long> vitalya$lineTimestamps = new IdentityHashMap<>();

    @Unique
    private ChatHud.Backend vitalya$backend = null;

    @Inject(method = "render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V", at = @At("HEAD"))
    private void vitalya$renderHead(ChatHud.Backend drawer, int windowHeight, int currentTick, boolean expanded, CallbackInfo ci) {
        this.vitalya$backend = drawer;
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V", at = @At("TAIL"))
    private void vitalya$renderTail(ChatHud.Backend drawer, int windowHeight, int currentTick, boolean expanded, CallbackInfo ci) {
        this.vitalya$backend = null;
    }

    @WrapOperation(
            method = "forEachVisibleLine",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/ChatHud$LineConsumer;accept(Lnet/minecraft/client/gui/hud/ChatHudLine$Visible;IF)V"
            )
    )
    private void vitalya$wrapLineAccept(@Coerce Object instance, ChatHudLine.Visible line, int y1, float opacity, Operation<Void> original) {
        if (!BetterMinecraft.INSTANCE.isEnabled() || !BetterMinecraft.INSTANCE.chatAnim.get() || vitalya$backend == null) {
            original.call(instance, line, y1, opacity);
            return;
        }

        long now = System.currentTimeMillis();
        long spawnTime = vitalya$lineTimestamps.computeIfAbsent(line, k -> now);
        float duration = BetterMinecraft.INSTANCE.animSpeed.get();
        float progress = Math.min(1.0f, (now - spawnTime) / duration);

        if (progress >= 1.0f) {
            original.call(instance, line, y1, opacity);
            return;
        }

        // Cubic Out Easing для плавного замедления в конце прилета
        float ease = 1.0f - (float) Math.pow(1.0f - progress, 3.0f);

        // Прилет справа налево (начинается со сдвига +80 пикселей вправо)
        float offsetX = (1.0f - ease) * 80.0f;

        // ВАЖНО: updatePose обновляет И живую матрицу (для фона), И кэшированный снимок (для текста)
        vitalya$backend.updatePose(pose -> pose.translate(offsetX, 0.0f));

        original.call(instance, line, y1, opacity * ease);

        vitalya$backend.updatePose(pose -> pose.translate(-offsetX, 0.0f));
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void vitalya$onClear(boolean clearHistory, CallbackInfo ci) {
        vitalya$lineTimestamps.clear();
    }
}