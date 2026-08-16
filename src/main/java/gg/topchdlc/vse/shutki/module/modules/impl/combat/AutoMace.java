package gg.topchdlc.vse.shutki.module.modules.impl.combat;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventAttack;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.client.MinecraftClient;

public class AutoMace extends Module {
    public static final AutoMace INSTANCE = new AutoMace();

    public AutoMace() {
        super("Automace", Category.COMBAT, "67");
    }
    private final CheckBox legit = checkbox("Legit",true);

    EventBus<Event> events = event -> {
        if (event instanceof EventAttack) {
            onAttack((EventAttack) event);
        }
    };

    private void onAttack(EventAttack event) {
        if (mc.player == null) return;

        int maceSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.MACE) {
                maceSlot = i;
                break;
            }
        }

        if (maceSlot == -1) return;

        int oldSlot = mc.player.getInventory().getSelectedSlot();
        if(legit.get()){
            mc.player.getInventory().setSelectedSlot(maceSlot);
            Client.SCHEDULER.scheduleOnce(()->{
                mc.player.getInventory().setSelectedSlot(oldSlot);
            },2);
        } else  {
            if (maceSlot != oldSlot) {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
            }

            mc.player.getInventory().setSelectedSlot(oldSlot);
           // mc.player.getInventory().setSelectedSlot(maceSlot);
        }



    }
}