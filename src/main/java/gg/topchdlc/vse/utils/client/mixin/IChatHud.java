package gg.topchdlc.vse.utils.client.mixin;

import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;
import java.util.List;

public interface IChatHud {
    void removeClientMessages();
    boolean tryStackMessage(Text message);
    Text getMessageAt(double mouseX, double mouseY);
    List<ChatHudLine> getMessages();
}
