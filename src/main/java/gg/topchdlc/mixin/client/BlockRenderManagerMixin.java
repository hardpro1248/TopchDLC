package gg.topchdlc.mixin.client;

import gg.topchdlc.vse.shutki.module.modules.impl.render.Removals;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;

@Mixin(BlockRenderManager.class)
public class BlockRenderManagerMixin {

    @Unique
    private static final Set<BlockState> VEGETATION_BLOCKS = Set.of(
        Blocks.GRASS_BLOCK.getDefaultState(),
        Blocks.TALL_GRASS.getDefaultState(),
        Blocks.FERN.getDefaultState(),
        Blocks.LARGE_FERN.getDefaultState(),
        Blocks.DANDELION.getDefaultState(),
        Blocks.POPPY.getDefaultState(),
        Blocks.BLUE_ORCHID.getDefaultState(),
        Blocks.ALLIUM.getDefaultState(),
        Blocks.AZURE_BLUET.getDefaultState(),
        Blocks.RED_TULIP.getDefaultState(),
        Blocks.ORANGE_TULIP.getDefaultState(),
        Blocks.WHITE_TULIP.getDefaultState(),
        Blocks.PINK_TULIP.getDefaultState(),
        Blocks.OXEYE_DAISY.getDefaultState(),
        Blocks.CORNFLOWER.getDefaultState(),
        Blocks.LILY_OF_THE_VALLEY.getDefaultState(),
        Blocks.WITHER_ROSE.getDefaultState(),
        Blocks.SUNFLOWER.getDefaultState(),
        Blocks.LILAC.getDefaultState(),
        Blocks.ROSE_BUSH.getDefaultState(),
        Blocks.PEONY.getDefaultState(),
        Blocks.SHORT_GRASS.getDefaultState(),
        Blocks.DEAD_BUSH.getDefaultState(),
        Blocks.BROWN_MUSHROOM.getDefaultState(),
        Blocks.RED_MUSHROOM.getDefaultState(),
        Blocks.CRIMSON_FUNGUS.getDefaultState(),
        Blocks.WARPED_FUNGUS.getDefaultState(),
        Blocks.CRIMSON_ROOTS.getDefaultState(),
        Blocks.WARPED_ROOTS.getDefaultState(),
        Blocks.NETHER_SPROUTS.getDefaultState(),
        Blocks.HANGING_ROOTS.getDefaultState(),
        Blocks.SUGAR_CANE.getDefaultState(),
        Blocks.BAMBOO.getDefaultState(),
        Blocks.BAMBOO_SAPLING.getDefaultState(),
        Blocks.SEAGRASS.getDefaultState(),
        Blocks.TALL_SEAGRASS.getDefaultState(),
        Blocks.SEA_PICKLE.getDefaultState(),
        Blocks.KELP.getDefaultState(),
        Blocks.KELP_PLANT.getDefaultState(),
        Blocks.VINE.getDefaultState(),
        Blocks.LILY_PAD.getDefaultState(),
        Blocks.SPORE_BLOSSOM.getDefaultState(),
        Blocks.PINK_PETALS.getDefaultState()
    );

    @Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true)
    private void onRenderBlock(
            BlockState state, BlockPos pos, BlockRenderView world, MatrixStack matrices, VertexConsumer vertexConsumer, boolean cull, List<BlockModelPart> parts, CallbackInfo ci
    ) {
        if (Removals.INSTANCE.removals.get(Removals.Removal.Vegetation) && Removals.INSTANCE.isEnabled()) {
            if (VEGETATION_BLOCKS.contains(state)) {
                ci.cancel();
            }
        }
    }
}