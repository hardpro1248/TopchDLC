package gg.topchdlc.mixin.client;

import gg.topchdlc.api.events.list.EventKey;
import gg.topchdlc.api.events.list.EventScroll;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gg.topchdlc.Client;

import static gg.topchdlc.MinecraftHolder.mc;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Shadow @Final private MinecraftClient client;

    @Shadow public abstract double getScaledX(Window window);
    @Shadow public abstract double getScaledY(Window window);

    @Inject(method = "unlockCursor", at = @At("RETURN"))
    private void onUnlockCursor(CallbackInfo ci) {
        if (mc.player == null) return;

        Window window = mc.getWindow();
        mc.options.forwardKey.setPressed(InputUtil.isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode()));
        mc.options.backKey.setPressed(InputUtil.isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode()));
        mc.options.leftKey.setPressed(InputUtil.isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode()));
        mc.options.rightKey.setPressed(InputUtil.isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode()));
        mc.options.jumpKey.setPressed(InputUtil.isKeyPressed(window, mc.options.jumpKey.getDefaultKey().getCode()));
        mc.options.sprintKey.setPressed(InputUtil.isKeyPressed(window, mc.options.sprintKey.getDefaultKey().getCode()));
    }


    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void client$onMouseButton(long window, MouseInput input, int action, CallbackInfo ci) {
        if (Client.IS_PANIC) return;

        int button = input.button();
        int mouseX = (int) getScaledX(client.getWindow());
        int mouseY = (int) getScaledY(client.getWindow());

        if (Client.BUILDER_TRAINING != null && Client.BUILDER_TRAINING.isOpened()) {
            if (action == 1) {
                Client.BUILDER_TRAINING.click(mouseX, mouseY, button);
            } else if (action == 0) {
                Client.BUILDER_TRAINING.release(button);
            }
            ci.cancel();
            return;
        }

        if (Client.GLASS_GUI.isOpened()) {
            if (action == 1) {
                Client.GLASS_GUI.click(mouseX, mouseY, button);
            } else if (action == 0) {
                Client.GLASS_GUI.release(button);
            }
            ci.cancel();
        } else {
            if (button == -1) return;
            int mouseKeyCode = -100 - button;
            EventKey eventKey = EventKey.build(mouseKeyCode, action);
            if (mc.currentScreen == null)
                Client.EVENTS.post(eventKey);
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void client$onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        int mouseX = (int) getScaledX(client.getWindow());
        int mouseY = (int) getScaledY(client.getWindow());

        if (Client.BUILDER_TRAINING != null && Client.BUILDER_TRAINING.isOpened()) {
            Client.BUILDER_TRAINING.mouseScrolled(mouseX, mouseY, horizontal, vertical);
            ci.cancel();
            return;
        }

        if (Client.GLASS_GUI.isOpened()) {
            Client.GLASS_GUI.mouseScrolled(mouseX, mouseY, horizontal, vertical);
            ci.cancel();
        } else {
            if (mc.currentScreen == null) {
                if (Client.EVENTS.post(new EventScroll(horizontal, vertical)).isCancelled()) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "onCursorPos", at = @At("HEAD"))
    private void client$onCursorPos(long window, double x, double y, CallbackInfo ci) {
        int mouseX = (int) getScaledX(client.getWindow());
        int mouseY = (int) getScaledY(client.getWindow());

        if (Client.BUILDER_TRAINING != null && Client.BUILDER_TRAINING.isOpened()) {
            Client.BUILDER_TRAINING.mouseDragged(mouseX, mouseY);
        } else if (Client.GLASS_GUI.isOpened()) {
            Client.GLASS_GUI.mouseDragged(mouseX, mouseY);
        }
    }
}