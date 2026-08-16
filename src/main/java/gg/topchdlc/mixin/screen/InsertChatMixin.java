package gg.topchdlc.mixin.screen;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChatHud.class)
public interface InsertChatMixin {
    @Invoker("addMessage")
    void invokeAddMessage(ChatHudLine message);

    @Invoker("addVisibleMessage")
    void invokeAddVisibleMessage(ChatHudLine message);
}
