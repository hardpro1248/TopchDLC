package gg.topchdlc.mixin.client;

import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.AntiWeb;
import gg.topchdlc.vse.utils.client.mixin.IEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.CobwebBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CobwebBlock.class)
public class CobwebBlockMixin {
    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    private void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity, EntityCollisionHandler handler, boolean bl, CallbackInfo ci) {
        if (entity == MinecraftHolder.mc.player
                && AntiWeb.INSTANCE.isEnabled()
                && AntiWeb.INSTANCE.noCollisionActive) {
            ci.cancel();
            return;
        }
        ((IEntity)entity).client$setWeb(true);
    }
}
