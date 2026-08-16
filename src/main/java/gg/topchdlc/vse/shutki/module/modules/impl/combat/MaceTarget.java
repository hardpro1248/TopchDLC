package gg.topchdlc.vse.shutki.module.modules.impl.combat;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventInput;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.Tag;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.MovementCorrection;
import gg.topchdlc.vse.rotation.all.impl.DefaultRotation;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import gg.topchdlc.vse.utils.math.Priorities;
import gg.topchdlc.vse.utils.math.RotationUtility;
import gg.topchdlc.vse.utils.math.TimeUtility;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import gg.topchdlc.vse.utils.player.swap.InventoryUtility;
import gg.topchdlc.vse.utils.player.MoveUtility;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

public class MaceTarget extends Module {
    public static final MaceTarget INSTANCE = new MaceTarget();
    private MaceTarget() {
        super("Mace Target", Category.COMBAT, "Подлетает на эликах д", Tag.Sosiski);
    }

    SliderSetting height = sliderSetting("Высотая", 20, 20, 50);
    CheckBox useStuck = checkbox("Air stuck", false);
    EnumSetting<MovementCorrection> correction = enumSetting("Correction", MovementCorrection.STRICT);

    TimeUtility firework = new TimeUtility();
    TimeUtility reset = new TimeUtility();

    Stage stage = Stage.FLYING_UP;

    @Override
    protected void onEnable() {
        super.onEnable();
        stage = Stage.FLYING_UP;
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        TargetsUtility.reset();
    }

    int wait;

    EventBus<Event> events = event -> {
        if (event instanceof EventGameTick) {
            if (!TargetsUtility.isValid()) {
                TargetsUtility.find(128, TargetsUtility.Sort.Distance);
                return;
            }

            if(wait > 0) {
                wait--;
                return;
            }

            boolean hasElytra = mc.player.getEquippedStack(EquipmentSlot.CHEST).get(DataComponentTypes.GLIDER) != null;

            switch (stage) {
                case FLYING_UP -> {
                    if (!hasElytra) {
                        int slot = InventoryUtility.findHotbar(Items.ELYTRA);
                        if (slot != -1) {
                            InventoryUtility.useItem(slot, false);
                            wait = 2;
                        }
                    }

                    if (firework.reached(300, true)) {
                        InventoryUtility.useItem(InventoryUtility.find(Items.FIREWORK_ROCKET), false);
                    }
                    if (mc.player.getY() - TargetsUtility.getTarget().getY() < height.get()) {
                        Angle target = RotationUtility.calcRotate(TargetsUtility.getTarget().getEntityPos().add(0, height.get(), 0), true);
                        Client.ROTATION.rotate(DefaultRotation.INSTANCE, target, 1, false, correction.get(), Priorities.ELYTRATARGET);
                    } else {
                        stage = Stage.TARGETTING;
                    }
                }
                case TARGETTING -> {
                    Angle target = RotationUtility.calcRotate(TargetsUtility.getTarget().getEntityPos(), true);
                    Client.ROTATION.rotate(DefaultRotation.INSTANCE, target, 1, false, correction.get(), Priorities.ELYTRATARGET);
                    if (mc.player.distanceTo(TargetsUtility.getTarget()) < 16) {
                        stage = Stage.ATTACKING;
                    }
                }
                case ATTACKING -> {
                    Angle target = RotationUtility.calcRotate(TargetsUtility.getTarget().getEntityPos(),true);
                    Client.ROTATION.rotate(DefaultRotation.INSTANCE, target, 1, false, correction.get(), Priorities.ELYTRATARGET);

                    if (hasElytra && reset.reached(200)) {
                        int slot = -1;
                        for (int i = 0; i < 46; i++) {
                            ItemStack stack = mc.player.getInventory().getStack(i);

                            EquippableComponent component = stack.get(DataComponentTypes.EQUIPPABLE);
                            if (component == null || component.slot() != EquipmentSlot.CHEST) continue;

                            if (stack.getItem() != Items.ELYTRA) {
                                slot = i;
                                break;
                            }
                        }
                        InventoryUtility.useItem(slot, false);
                        reset.reset();
                    }
                    if (reset.reached(100)) {
                        if (mc.player.distanceTo(TargetsUtility.getTarget()) < 3) {
                            int slot = InventoryUtility.findHotbar(Items.MACE);
                            int prev = mc.player.getInventory().getSelectedSlot();
                            if (slot != prev) NetworkUtility.send(new UpdateSelectedSlotC2SPacket(slot));

                            mc.interactionManager.attackEntity(mc.player, TargetsUtility.getTarget());
                            mc.player.swingHand(Hand.MAIN_HAND);

                            if (slot != prev) NetworkUtility.send(new UpdateSelectedSlotC2SPacket(prev));
                            stage = Stage.FLYING_UP;
                            firework.reset();
                            ChatUtility.sendDebug("[macetarget] flying up!");
                        }
                    }
                }
            }
        }
        if (event instanceof EventInput e) {
            if (TargetsUtility.getTarget() == null) return;

            boolean hasElytra = MoveUtility.hasElytra();
            if (hasElytra) {
                e.setJump(mc.player.age % 2 == 0);
                if (!mc.player.isGliding()) {
                    firework.reset();
                }
            }
        }
    };

    enum Stage {
        FLYING_UP, TARGETTING, ATTACKING
    }
}
