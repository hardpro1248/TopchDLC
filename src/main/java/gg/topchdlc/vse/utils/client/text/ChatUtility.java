package gg.topchdlc.vse.utils.client.text;

import com.mojang.blaze3d.systems.RenderSystem;
import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.mixin.screen.InsertChatMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import gg.topchdlc.vse.utils.client.client.ClientSettings;

import java.awt.*;

public class ChatUtility implements MinecraftHolder {
    public static void sendDebug(String message) {
        if (Client.IS_DEBUG)
            send(Text.of(message));
    }
    public static void send(String message) {
        send(Text.of(message));
    }

    public static void send(Text message) {
        if (!RenderSystem.isOnRenderThread()) {
            MinecraftClient.getInstance().execute(() -> send(message));
            return;
        }

        if (mc.inGameHud == null) return;
        Text prefix = TextUtility.applyGradient("[topchdlc] ", ClientSettings.INSTANCE.getColor(0), ClientSettings.INSTANCE.getColor(90));
        MutableText combinedText = prefix.copy()
                .append(message);
        ChatHudLine line = new ChatHudLine(MinecraftClient.getInstance().inGameHud.getTicks(), combinedText, null, MessageIndicator.system());
        ((InsertChatMixin) mc.inGameHud.getChatHud()).invokeAddMessage(line);
        ((InsertChatMixin) mc.inGameHud.getChatHud()).invokeAddVisibleMessage(line);
    }
    public static void send(Text message,Color color) {
        if (!RenderSystem.isOnRenderThread()) {
            MinecraftClient.getInstance().execute(() -> send(message));
            return;
        }

        if (mc.inGameHud == null) return;
        Text prefix = TextUtility.applyGradient("[topchdlc] ", ClientSettings.INSTANCE.getColor(0), ClientSettings.INSTANCE.getColor(90));
        MutableText combinedText = prefix.copy()
                .append(message);
        ChatHudLine line = new ChatHudLine(MinecraftClient.getInstance().inGameHud.getTicks(), combinedText, null, MessageIndicator.system());
        ((InsertChatMixin) mc.inGameHud.getChatHud()).invokeAddMessage(line);
        ((InsertChatMixin) mc.inGameHud.getChatHud()).invokeAddVisibleMessage(line);
    }

}