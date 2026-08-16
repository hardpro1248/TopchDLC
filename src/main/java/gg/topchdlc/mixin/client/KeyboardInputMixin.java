package gg.topchdlc.mixin.client;


import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.events.list.EventInput;
import gg.topchdlc.vse.shutki.module.modules.impl.player.GuiWalk;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin implements MinecraftHolder {
    @Shadow
    private static float getMovementMultiplier(boolean positive, boolean negative) {
        return 0;
    }

    @Redirect(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/util/PlayerInput;"))
    private PlayerInput onTick(boolean forwardKey, boolean backKey, boolean leftKey, boolean rightKey, boolean jumpKey, boolean sneakKey, boolean sprintKey) {
        forwardKey |= GuiWalk.INSTANCE.handle(mc.options.forwardKey);
        backKey |= GuiWalk.INSTANCE.handle(mc.options.backKey);
        leftKey |= GuiWalk.INSTANCE.handle(mc.options.leftKey);
        rightKey |= GuiWalk.INSTANCE.handle(mc.options.rightKey);
        jumpKey |= GuiWalk.INSTANCE.handle(mc.options.jumpKey);
        sneakKey |= GuiWalk.INSTANCE.handle(mc.options.sneakKey);
        sprintKey |= GuiWalk.INSTANCE.handle(mc.options.sprintKey);
        EventInput event = EventInput.build(getMovementMultiplier(forwardKey, backKey), getMovementMultiplier(leftKey, rightKey), jumpKey, sneakKey, sprintKey);
        Client.EVENTS.post(event);

        forwardKey = event.getForward() >= 1;
        backKey = event.getForward() <= -1;

        leftKey = event.getStrafe() >= 1;
        rightKey = event.getStrafe() <= -1;

        jumpKey = event.isJump();
        sneakKey = event.isSneak();
        sprintKey = event.isSprint();

        if (event.isCancelled()) {
            forwardKey = false;
            backKey = false;
            leftKey = false;
            rightKey = false;
            jumpKey = false;
            sneakKey = false;
        }

        return new PlayerInput(forwardKey, backKey, leftKey, rightKey, jumpKey, sneakKey, sprintKey);
    }
}
