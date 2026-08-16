package gg.topchdlc.vse.shutki.module.modules.impl.movement;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventMove;
import gg.topchdlc.api.events.list.EventNoSlow;
import gg.topchdlc.api.events.list.EventSendPacket;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import gg.topchdlc.vse.utils.player.swap.InventoryUtility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.concurrent.TimeUnit;

public class NoSlowDown extends Module {
    public static NoSlowDown INSTANCE = new NoSlowDown();

    public NoSlowDown() {
        super("No Slow Down", Category.MOVEMENT, "Убирает замедление от предметов");
    }

    public EnumSetting<Modes> mode = enumSetting("Modes", Modes.Default);
    public CheckBox sprint = checkbox("Sprint", true);

    public SliderSetting ticksOnSlow = sliderSetting("Ticks", 1f, 1f, 4f).increment(1)
            .visible(() -> mode.is(Modes.GrimLatest));
    private long lastSwapTime = 0;
    private int ticks = 0;
    private boolean returnSneak = false;

    EventBus<Event> onEvent = event -> {
        if (mc.player == null) return;

        if (event instanceof EventGameTick) {
            if (returnSneak) {
                mc.options.sneakKey.setPressed(false);
                mc.player.setSprinting(true);
                returnSneak = false;
            }

            if (mc.player.isUsingItem()) {
                if (mode.is(Modes.Test)) {
                    {
                        {
                            sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, mc.player.getBlockPos().up(), Direction.UP));
                            //      mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));
                        }
                    }
                }
                if (mode.is(Modes.MusteryGrief)) {
                    if (mc.player.isOnGround() && mc.options.jumpKey.isPressed()) {
                        mc.options.sneakKey.setPressed(true);
                        returnSneak = true;
                    }
                }
              //  if(mode.is(Modes.Lgtest)){
               //     sendPacket( new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, mc.player.getBlockPos().up(), Direction.NORTH));
             //       sendSequencedPacket(id->new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, mc.player.getBlockPos().up(), Direction.NORTH,id));
            //    }


                if (mode.is(Modes.GrimLatest)) ticks++;
            }

            if (!mc.player.isUsingItem() && (mode.is(Modes.Universal) || mode.is(Modes.Slots))) ticks = 0;
        }


        if (event instanceof EventNoSlow eventNoSlow) {
            if (sprint.get() && mc.player.isUsingItem()) {
                mc.player.setSprinting(true);
            }

            switch (mode.get()) {
                case Default,Test -> {
                    eventNoSlow.cancel();
                }
                case MusteryGrief -> {
                    if (mc.player.isOnGround() && mc.options.jumpKey.isPressed()) {
                        eventNoSlow.cancel();
                    }
                }
                case GrimLatest -> {
                    if (ticks >= ticksOnSlow.get()) {
                        eventNoSlow.cancel();
                        ticks = 0;
                    }
                }
                case Universal -> {
                    if (mc.player.hurtTime > 0) ticks = 1;
                    if (ticks == 1 || mc.player.age % 3 == 0) eventNoSlow.cancel();
                }
                case Slots -> {
                    if (shouldSwapSlotsForUse() && mc.player.age % 2 == 1) {
                        sendSlotSwapBatch();
                        eventNoSlow.cancel();
                    }
                }
            }
        }
    };


    private boolean shouldSwapSlotsForUse() {
        if (!mc.player.isUsingItem()) return false;
        if (mc.player.getActiveHand() != Hand.MAIN_HAND) return false;
        UseAction useAction = mc.player.getActiveItem().getUseAction();
        return useAction == UseAction.EAT || useAction == UseAction.DRINK;
    }

    private void sendSlotSwapBatch() {
        if (mc.player.currentScreenHandler == null || mc.getNetworkHandler() == null) return;
        int currentSlot = mc.player.getInventory().getSelectedSlot();
        int swapSlot = (currentSlot + 1) % 9;
        int currentScreenSlot = InventoryUtility.wrapHotbar(currentSlot);
        int swapScreenSlot = InventoryUtility.wrapHotbar(swapSlot);
        if (currentScreenSlot == swapScreenSlot) return;

        NetworkUtility.startQueuingPackets();
        try {
            NetworkUtility.send(InventoryUtility.click(mc.player.currentScreenHandler.syncId, currentScreenSlot, 0, SlotActionType.PICKUP));
            NetworkUtility.send(InventoryUtility.click(mc.player.currentScreenHandler.syncId, swapScreenSlot, 0, SlotActionType.PICKUP));
            NetworkUtility.send(InventoryUtility.click(mc.player.currentScreenHandler.syncId, currentScreenSlot, 0, SlotActionType.PICKUP));
        } finally {
            NetworkUtility.stopQueuingAndFlushSequential();
        }
    }

    @AllArgsConstructor
    @Getter
    public enum Modes implements EnumChoice {
        Default("Vanilla"),
        GrimLatest("Grim Tick"),
        MusteryGrief("MusteryGrief"),
        Test("test"),
        Universal("Universal"),
        Slots("Slots");
        final String renderName;
    }
}