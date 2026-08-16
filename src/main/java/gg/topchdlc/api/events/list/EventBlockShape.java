package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;

public class EventBlockShape extends Event {
    static EventBlockShape instance = new EventBlockShape();
    public BlockPos pos;
    public BlockState state;
    public VoxelShape shape;

    public static EventBlockShape build(BlockPos pos, BlockState state, VoxelShape shape) {
        instance.pos = pos;
        instance.state = state;
        instance.shape = shape;
        instance.reset();
        return instance;
    }
}