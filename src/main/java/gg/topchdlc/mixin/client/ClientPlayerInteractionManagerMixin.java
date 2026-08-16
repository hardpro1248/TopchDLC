package gg.topchdlc.mixin.client;


import gg.topchdlc.vse.shutki.module.modules.impl.helper.AssistModule;
import net.minecraft.item.Items;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.list.EventAttack;
import gg.topchdlc.api.events.list.EventClickSlot;
import net.minecraft.screen.slot.SlotActionType;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.CrystalAuto;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.Hitbox;
import gg.topchdlc.vse.shutki.module.modules.impl.player.NoInteract;
import gg.topchdlc.vse.rotation.Angle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

import static gg.topchdlc.MinecraftHolder.mc;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Shadow private GameMode gameMode;

    @Shadow protected abstract void syncSelectedSlot();

    @Shadow protected abstract void sendSequencedPacket(ClientWorld world, SequencedPacketCreator packetCreator);

    @Shadow @Final private MinecraftClient client;
    @Shadow
    private int blockBreakingCooldown;
    @Shadow
    @Final
    private static Logger LOGGER;

    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    private void clickSlot(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (!EventClickSlot.postedFromScreen) {
            EventClickSlot event = EventClickSlot.build(syncId, slotId, button, actionType);
            Client.EVENTS.post(event);
            if (event.isCancelled()) ci.cancel();
        }
    }

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    public void client$attackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        Hitbox.INSTANCE.trySnap(target);
        Hitbox.INSTANCE.onAttack(target);

        EventAttack attack = EventAttack.build(target);
        Client.EVENTS.post(attack);

        if (attack.isCancelled()) ci.cancel();
    }
    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void client$interactItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (NoInteract.INSTANCE.shouldBlock()) {
            cir.setReturnValue(ActionResult.PASS);
            return;
        }
        cir.cancel();

        Angle angle = Client.ROTATION.getRotate();

        if (this.gameMode == GameMode.SPECTATOR) {
            cir.setReturnValue(ActionResult.PASS);
        } else {
            this.syncSelectedSlot();
            MutableObject<ActionResult> mutableObject = new MutableObject<>();
            this.sendSequencedPacket(this.client.world, sequence -> {
                PlayerInteractItemC2SPacket playerInteractItemC2SPacket = new PlayerInteractItemC2SPacket(hand, sequence, angle.getYaw(), angle.getPitch());
                ItemStack itemStack = player.getStackInHand(hand);
                if (player.getItemCooldownManager().isCoolingDown(itemStack)) {
                    mutableObject.setValue(ActionResult.PASS);
                    return playerInteractItemC2SPacket;
                } else {
                    ActionResult actionResult = itemStack.use(this.client.world, player, hand);
                    ItemStack itemStack2;
                    if (actionResult instanceof ActionResult.Success success) {
                        itemStack2 = (ItemStack) Objects.requireNonNullElseGet(success.getNewHandStack(), () -> player.getStackInHand(hand));
                    } else {
                        itemStack2 = player.getStackInHand(hand);
                    }

                    if (itemStack2 != itemStack) {
                        player.setStackInHand(hand, itemStack2);
                    }

                    mutableObject.setValue(actionResult);
                    return playerInteractItemC2SPacket;
                }
            });
            cir.setReturnValue(mutableObject.getValue());
        }

    }

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void client$interactBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (NoInteract.INSTANCE.shouldBlock()) {
            cir.setReturnValue(ActionResult.PASS);
        }
        ItemStack held = player.getStackInHand(hand);
        if (held.getItem() == Items.OBSIDIAN) {
            BlockPos placePos = hitResult.getBlockPos().offset(hitResult.getSide());
            if (player.getEntityWorld().getBlockState(placePos).isAir()) {
                CrystalAuto.INSTANCE.onObsidianPlaced(placePos);
            }
        }
        if(mc.player != null && AssistModule.INSTANCE.cancelSettings.get().contains(AssistModule.CancelMode.NoBallPlace) && AssistModule.INSTANCE.isEnabled()
                && ((mc.player.getOffHandStack().getItem() == Items.PLAYER_HEAD && hand == Hand.OFF_HAND) || (mc.player.getMainHandStack().getItem() == Items.PLAYER_HEAD && hand == Hand.MAIN_HAND)))
            cir.setReturnValue(ActionResult.PASS);
    }

    @Inject(method = "interactEntity", at = @At("HEAD"), cancellable = true)
    private void client$interactEntity(PlayerEntity player, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (NoInteract.INSTANCE.shouldBlock()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }
}
