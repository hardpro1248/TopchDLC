package gg.topchdlc.mixin.client;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.list.EventBlockShape;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static gg.topchdlc.MinecraftHolder.mc;

@Mixin(AbstractBlock.class)
public class AbstractBlockMixin {
    @Shadow
    @Final
    protected boolean collidable;

    @Inject(method = "getCollisionShape", at = @At("RETURN"), cancellable = true)
    private void getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (Client.EVENTS == null || cir.getReturnValue() == null/* || state.getOutlineShape(world, pos).isEmpty()*/ || mc.isInSingleplayer())
            return;
        VoxelShape vx = this.collidable ? state.getOutlineShape(world, pos) : VoxelShapes.empty();

        EventBlockShape shape = EventBlockShape.build(pos, state, vx);
        Client.EVENTS.post(shape);
        if (shape.isCancelled()) {
            cir.setReturnValue(VoxelShapes.empty());
            return;
        }
        cir.setReturnValue(shape.shape);
    }
}