package gg.topchdlc.mixin.client;


import gg.topchdlc.MinecraftHolder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowItem.class)
public abstract class CrossBowItemMixin implements MinecraftHolder {
    @Shadow
    public abstract boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks);

    @Inject(method = "onStoppedUsing", at = @At(value = "RETURN"))
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
    }
}