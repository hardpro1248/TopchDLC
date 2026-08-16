package gg.topchdlc.mixin.client;

import gg.topchdlc.vse.utils.client.mixin.KeyboardHandler;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int action, KeyInput input, CallbackInfo ci) {
        int key = input.key();
        int scancode = input.scancode();
        int modifiers = input.modifiers();

        if (KeyboardHandler.handleKey(key, scancode, action, modifiers)) {
            ci.cancel();
        }
    }

    @Inject(method = "onChar", at = @At("HEAD"), cancellable = true)
    private void onChar(long window, CharInput input, CallbackInfo ci) {
        int codePoint = input.codepoint();
        int modifiers = input.modifiers();

        if (KeyboardHandler.handleChar((char) codePoint, modifiers)) {
            ci.cancel();
        }
    }
}


