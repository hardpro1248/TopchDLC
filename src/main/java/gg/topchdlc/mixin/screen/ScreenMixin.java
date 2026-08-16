package gg.topchdlc.mixin.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.ClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.list.EventChatClick;

import static gg.topchdlc.MinecraftHolder.mc;

/**
 * Created by daun kvass
 */
@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "handleClickEvent", at = @At("HEAD"), cancellable = true)
    private static void onHandleClickEvent(ClickEvent clickEvent, MinecraftClient client, Screen screenAfterRun, CallbackInfo ci) {
        if (clickEvent == null || mc.player == null) return;

        boolean isRun = clickEvent instanceof ClickEvent.RunCommand;
        boolean isSuggest = clickEvent instanceof ClickEvent.SuggestCommand;

        if (isRun || isSuggest) {
            Client.EVENTS.post(EventChatClick.build(clickEvent));
        }

        String rawCommand = null;
        if (clickEvent instanceof ClickEvent.RunCommand(String command)) {
            rawCommand = command;
        } else if (clickEvent instanceof ClickEvent.SuggestCommand(String command)) {
            rawCommand = command;
        }

        if (rawCommand == null) return;

        String cmd = rawCommand.startsWith("/") ? rawCommand.substring(1) : rawCommand;
        mc.keyboard.setClipboard(rawCommand);
        mc.player.networkHandler.sendChatCommand(cmd);

        ci.cancel();
    }
}