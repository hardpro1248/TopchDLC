package gg.topchdlc.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.topchdlc.vse.shutki.module.modules.impl.helper.AutoSwap;
import gg.topchdlc.api.events.list.EventChangeWorld;
import gg.topchdlc.api.events.list.EventPacketTick;
import gg.topchdlc.api.events.list.EventPostTick;
import gg.topchdlc.vse.shutki.module.modules.impl.player.GuiWalk;
import gg.topchdlc.vse.shutki.module.modules.impl.render.DynamicIsland;
import gg.topchdlc.mixin.accessor.IKeyBinding;
import gg.topchdlc.vse.utils.jni.DwmApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.list.EventGameTick;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static gg.topchdlc.MinecraftHolder.mc;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow protected abstract void render(boolean tick);

    @Shadow public abstract Window getWindow();

    @Shadow @Final public GameOptions options;

    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (Client.INSTANCE == null) {
            Client.INSTANCE = new Client();
        }
        Client.INSTANCE.init();
    }
    @Inject(method = "stop", at = @At("HEAD"))
    private void onStop(CallbackInfo ci) {
        Client.saveAll();

        if (Client.SCRIPTS != null) {
            Client.SCRIPTS.shutdown();
        }
        if (Client.DISCORD_RPC != null) {
            Client.DISCORD_RPC.shutdown();
        }
        if (Client.ChunkScanner != null) {
            Client.ChunkScanner.shutdown();
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;blitToScreen()V", shift = At.Shift.AFTER))
    private void client$onRender(CallbackInfo ci) {
        Client.RENDERER.prepare();
        if (DynamicIsland.INSTANCE != null && DynamicIsland.INSTANCE.isEnabled() && mc.player != null) {
            DynamicIsland.INSTANCE.render(null);
        }

        Client.RENDERER.render();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void client$onTick(CallbackInfo ci) {
        if (mc.player == null || mc.world == null)
            return;
        Client.IS_WINDOW_FOCUSED = mc.isWindowFocused();

        Client.EVENTS.post(EventGameTick.build());
    }

    @Inject(method = "tick", at = @At(value = "TAIL"))
    private void tick(CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof DeathScreen && MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.isAlive()) {
            MinecraftClient.getInstance().currentScreen = null;
        }

        if (mc.player == null || mc.world == null) return;

        Client.EVENTS.post(EventPostTick.build());
    }

    @Inject(method = "getWindowTitle", at = @At("RETURN"), cancellable = true)
    private void getWindowTitle(CallbackInfoReturnable<String> cir) {
        if (!Client.IS_PANIC)
            cir.setReturnValue(String.format("TopchDLC ● %s", cir.getReturnValue().replace("Minecraft", "").replace("*", "").strip()));
    }
    @Inject(method = "onResolutionChanged", at = @At("HEAD"))
    private void onResolutionChanged(CallbackInfo ci) {
        DwmApi.updateDwm(mc.getWindow().isFullscreen(), mc.getWindow().getHandle());
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;runTasks()V", shift = At.Shift.BEFORE))
    private void hookPacketTick(CallbackInfo callbackInfo) {
        Client.EVENTS.post(EventPacketTick.instance);
    }

    @WrapOperation(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;unpressAll()V"))
    private void onSetScreenKeyBindingUnpressAll(Operation<Void> op) {
        if (!GuiWalk.INSTANCE.isEnabled() && !AutoSwap.INSTANCE.isMenuOpen()) {
            op.call();
            return;
        }

        GameOptions options = mc.options;
        for (KeyBinding kb : IKeyBinding.client$getKeysById().values()) {
            if (kb == options.forwardKey) continue;
            if (kb == options.leftKey) continue;
            if (kb == options.rightKey) continue;
            if (kb == options.backKey) continue;
            if (kb == options.sneakKey) continue;
            if (kb == options.sprintKey) continue;
            if (kb == options.jumpKey) continue;
            ((IKeyBinding) kb).client$reset();
        }
    }

    @Inject(method = "setWorld", at = @At("HEAD"))
    private void topchdlc$setWorld(ClientWorld world, CallbackInfo ci) {
        Client.EVENTS.post(new EventChangeWorld(world));
    }
}
