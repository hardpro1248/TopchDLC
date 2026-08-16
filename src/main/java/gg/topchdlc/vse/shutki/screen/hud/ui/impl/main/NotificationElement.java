package gg.topchdlc.vse.shutki.screen.hud.ui.impl.main;

import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.vse.shutki.module.modules.impl.render.Interface;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.screen.hud.notification.Notify;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import gg.topchdlc.vse.utils.animations.Direction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Create by daun kvass
 */
public class NotificationElement extends HudElement {
    public final EnumSetting<Style> style = settings.enumSetting("Стиль", Style.GLASS).visible(()->Interface.INSTANCE.hudStyle.is(Interface.HudStyle.Main));

    public NotificationElement(Drag drag) {
        super("Notify", drag);
    }

    Notify demo = new Notify(Text.of("Демонстрация уведомления"), "totem_of_undying", 3000L).markDemo();

    @Override
    public void render(int mouseX, int mouseY) {
        CopyOnWriteArrayList<Notify> notifies = Client.NOTIFIES.getNotifies();

        if (mc.currentScreen instanceof ChatScreen && notifies.isEmpty()) {
            demo.getAnimation().setDirection(Direction.FORWARDS);
        } else {
            demo.getAnimation().setDirection(Direction.BACKWARDS);
        }

        if (demo.getAnimation().getOutput() > 0.001) {
            demo.bound(x + width / 2f, y, 0, 15).render(mouseX, mouseY);
        }

        int activeCount = 0;
        for (Notify notif : notifies) {
            if (!notif.isForcedExpire() && notif.getAnimation().getDirection() == Direction.FORWARDS) {
                activeCount++;
            }
        }

        if (activeCount > 2) {
            int toDismiss = activeCount - 2;
            for (Notify notif : notifies) {
                if (!notif.isForcedExpire() && notif.getAnimation().getDirection() == Direction.FORWARDS) {
                    notif.dismiss();
                    toDismiss--;
                    if (toDismiss <= 0) break;
                }
            }
        }

        float currentY = y;
        float spacing = 18f;
        float maxWidth = 120f;

        for (int i = notifies.size() - 1; i >= 0; i--) {
            Notify notif = notifies.get(i);
            float animValue = (float) notif.getAnimation().getOutput();

            notif.bound(x + width / 2f, currentY, 0, 15).render(mouseX, mouseY);

            currentY += (spacing * animValue);
        }

        this.drag.width = width = maxWidth;
        this.drag.height = height = 20;

        notifies.removeIf(Notify::shouldRemove);
    }

    @AllArgsConstructor
    public enum Style implements EnumChoice {
        DARK("Темный"), GLASS("Стекло");
        @Getter final String renderName;
    }
}