package gg.topchdlc.vse.utils.client.mixin;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.list.EventKey;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

import static gg.topchdlc.MinecraftHolder.mc;

@UtilityClass
public class KeyboardHandler {
    public boolean handleKey(int key, int scancode, int action, int modifiers) {
        EventKey eventKey = EventKey.build(key, action);

        if (!(mc.currentScreen instanceof ChatScreen) && !(mc.currentScreen instanceof InventoryScreen) && key != -1)
            Client.EVENTS.post(eventKey);

        if (action == 1 || action == 2) {
            if (Client.AUTOBUY_CONFIG != null && Client.AUTOBUY_CONFIG.isOpened()) {
                Client.AUTOBUY_CONFIG.keyPressed(key, scancode, modifiers);
                return true;
            }

            if (Client.AUTOBUY_ITEMS != null && Client.AUTOBUY_ITEMS.isOpened()) {
                if (!Client.AUTOBUY_ITEMS.isWriting()) {
                    boolean isMovement = key == mc.options.forwardKey.getDefaultKey().getCode() ||
                            key == mc.options.backKey.getDefaultKey().getCode() ||
                            key == mc.options.leftKey.getDefaultKey().getCode() ||
                            key == mc.options.rightKey.getDefaultKey().getCode() ||
                            key == mc.options.jumpKey.getDefaultKey().getCode() ||
                            key == mc.options.sneakKey.getDefaultKey().getCode() ||
                            key == mc.options.sprintKey.getDefaultKey().getCode();

                    if (isMovement) return false;
                }

                Client.AUTOBUY_ITEMS.keyPressed(key, scancode, modifiers);
                return true;
            }

            if (Client.GLASS_GUI.isOpened()) {
                 {
                    boolean isMovement = key == mc.options.forwardKey.getDefaultKey().getCode() ||
                            key == mc.options.backKey.getDefaultKey().getCode() ||
                            key == mc.options.leftKey.getDefaultKey().getCode() ||
                            key == mc.options.rightKey.getDefaultKey().getCode() ||
                            key == mc.options.jumpKey.getDefaultKey().getCode() ||
                            key == mc.options.sneakKey.getDefaultKey().getCode() ||
                            key == mc.options.sprintKey.getDefaultKey().getCode();

                    if (isMovement) return false;
                }
                Client.GLASS_GUI.keyPressed(key, scancode, modifiers);
                return true;
            }

            if (mc.currentScreen instanceof ChatScreen && Client.GLASS_GUI.WRITING) {
                Client.HUD.keyPressed(key, scancode, modifiers);
                return true;
            }
        }

        if (eventKey.isCancelled() || (Client.GLASS_GUI.WRITING && Client.GLASS_GUI.isOpened()))
            return true;

        return false;
    }

    public boolean handleChar(char codePoint, int modifiers) {
        if (Client.AUTOBUY_CONFIG != null && Client.AUTOBUY_CONFIG.isOpened()) {
            Client.AUTOBUY_CONFIG.chartyped(codePoint, modifiers);
            return true;
        }

        if (Client.AUTOBUY_ITEMS != null && Client.AUTOBUY_ITEMS.isOpened()) {
            Client.AUTOBUY_ITEMS.chartyped(codePoint, modifiers);
            return true;
        }

        if (mc.currentScreen instanceof ChatScreen && Client.GLASS_GUI.WRITING) {
            Client.HUD.charTyped(codePoint, modifiers);
            return true;
        }

        if (Client.GLASS_GUI.isOpened()) {
            Client.GLASS_GUI.chartyped(codePoint, modifiers);
            return true;
        }

        return false;
    }
}