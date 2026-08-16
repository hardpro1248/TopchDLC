package gg.topchdlc.mixin.movement;

import gg.topchdlc.vse.shutki.module.modules.impl.movement.WaterSpeed;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static gg.topchdlc.MinecraftHolder.mc;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    
    @Shadow
    private int jumpingCooldown;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @ModifyVariable(method = "setSprinting", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private boolean setSprintingHook(boolean sprinting) {
        if (WaterSpeed.INSTANCE.isEnabled() && WaterSpeed.INSTANCE.mode.is(WaterSpeed.Mode.Legit)) {
            Object self = this;
            if (self == mc.player) {
                BlockPos blockPos = BlockPos.ofFloored(mc.player.getEntityPos().add(0, -0.5, 0));
                if (mc.player.isTouchingWater() || mc.world.getBlockState(blockPos).getBlock() instanceof FluidBlock)
                    return true;
            }
        }

        return sprinting;
    }

}
