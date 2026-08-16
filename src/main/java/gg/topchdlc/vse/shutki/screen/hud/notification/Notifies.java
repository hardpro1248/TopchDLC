package gg.topchdlc.vse.shutki.screen.hud.notification;

import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.render.system.IconUse;
import lombok.Getter;
import net.minecraft.text.Text;

import java.util.concurrent.CopyOnWriteArrayList;

public class Notifies implements MinecraftHolder {
    @Getter
    final CopyOnWriteArrayList<Notify> notifies = new CopyOnWriteArrayList<Notify>();

    public Notifies() {}

    public void add(Text text, IconUse icon, long duration) {
        notifies.add(new Notify(text, icon, duration));
    }
    public void addItem(Text text, String item, long duration) {
        notifies.add(new Notify(text, item, duration));
    }
    public void addItem(Text text, net.minecraft.item.ItemStack stack, long duration) {
        notifies.add(new Notify(text, stack, duration));
    }

}
