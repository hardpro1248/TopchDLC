package gg.topchdlc.vse.utils.player;



import lombok.experimental.UtilityClass;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.World;

import static gg.topchdlc.MinecraftHolder.mc;

@UtilityClass
public class PlayerUtility {

    public boolean isMoving() {
        Vec2f vector2f = mc.player.input.getMovementInput();
        return (vector2f.x != 0.0F || vector2f.y != 0.0F) || mc.player.input.playerInput.forward();
    }

    public static boolean isOnSolidGround(LivingEntity entity, int checkDepth) {
        BlockPos entityPos = entity.getBlockPos();
        World world = entity.getEntityWorld();

        for (int i = 1; i <= checkDepth; i++) {
            BlockPos checkPos = entityPos.down(i);
            BlockState blockState = world.getBlockState(checkPos);

            if (!blockState.isAir() &&
                    !blockState.getFluidState().isEmpty() &&
                    blockState.blocksMovement()) {
                return true;
            }
        }
        return false;
    }

    public double compareArmor(LivingEntity entity) {
        return -entity.getArmor();
    }
    
    public static boolean isBlockSolid(double x, double y, double z) {
        if (mc.world == null) return false;
        BlockPos pos = BlockPos.ofFloored(x, y, z);
        return mc.world.getBlockState(pos).isFullCube(mc.world, pos);
    }
}
