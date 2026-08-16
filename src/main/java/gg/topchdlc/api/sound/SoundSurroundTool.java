package gg.topchdlc.api.sound;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;


public class SoundSurroundTool {
    @Getter @Setter
    private ClientPlayerEntity player;
    @Getter @Setter
    private boolean rtxDebug = false;
    @Getter @Setter
    private boolean tooPerfomance = false;
    @Getter
    private final List<Vec3d> listOfTestVecs = new ArrayList<>();

    public static SoundSurroundTool build() {
        return new SoundSurroundTool();
    }


    public float[] getGainArgsFromWorld() {
        if (player == null || player.getEntityWorld() == null) {
            return new float[]{0.0F, 0.0F, 1.0F, 1.0F};
        }

        World world = player.getEntityWorld();
        BlockPos playerPos = player.getBlockPos();

        int scanRadius = tooPerfomance ? 3 : 5;
        int scanHeight = tooPerfomance ? 2 : 3;

        int solidBlocks = 0;
        int totalBlocks = 0;
        int airBlocks = 0;

        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int y = -scanHeight; y <= scanHeight; y++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    BlockState state = world.getBlockState(checkPos);
                    totalBlocks++;

                    if (!state.isAir()) {
                        solidBlocks++;
                        
                        if (rtxDebug && listOfTestVecs.size() < 100) {
                            listOfTestVecs.add(new Vec3d(checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5));
                        }
                    } else {
                        airBlocks++;
                    }
                }
            }
        }

        float enclosureRatio = totalBlocks > 0 ? (float) solidBlocks / totalBlocks : 0.0F;
        float echoLevel = MathHelper.clamp(enclosureRatio * 2.0F, 0.0F, 1.5F);
        float reflectionLevel = MathHelper.clamp(enclosureRatio * 1.5F, 0.0F, 1.0F);
        float lowPassGain = MathHelper.clamp(1.0F - (enclosureRatio * 0.3F), 0.5F, 1.0F);
        float lowPassGainHF = MathHelper.clamp(1.0F - (enclosureRatio * 0.5F), 0.3F, 1.0F);
        if (player.isSubmergedInWater()) {
            echoLevel *= 0.3F;
            reflectionLevel *= 0.5F;
            lowPassGain *= 0.6F;
            lowPassGainHF *= 0.4F;
        }

        if (player.isInLava()) {
            echoLevel *= 0.2F;
            reflectionLevel *= 0.3F;
            lowPassGain *= 0.5F;
            lowPassGainHF *= 0.3F;
        }

        return new float[]{echoLevel, reflectionLevel, lowPassGain, lowPassGainHF};
    }
}
