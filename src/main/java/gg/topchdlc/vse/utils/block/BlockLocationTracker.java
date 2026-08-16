package gg.topchdlc.vse.utils.block;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public abstract class BlockLocationTracker<T> implements ChunkScanner.Subscriber {
    @Nullable
    public abstract T getStateFor(BlockPos pos, BlockState state);
    public abstract boolean untrack(BlockPos pos);
    public abstract void track(BlockPos pos, T state);
    public abstract Stream<BlockPos> streamPositions();
    public abstract Stream<Map.Entry<BlockPos, T>> stream();
    public abstract boolean isEmpty();

    @Override
    public final void recordBlock(BlockPos pos, BlockState state, boolean isCleared) {
        T newState = getStateFor(pos, state);
        if (newState == null) {
            if (!isCleared) untrack(pos);
        } else track(pos, newState);
    }

    @Override
    public final void onChunkUpdate(WorldChunk chunk) {}

    public abstract static class BlockPos2State<T> extends BlockLocationTracker<T> {
        private final Map<BlockPos, T> map = new ConcurrentHashMap<>();

        @Override
        public Stream<BlockPos> streamPositions() {
            return map.keySet().stream();
        }

        @Override
        public Stream<Map.Entry<BlockPos, T>> stream() {
            return map.entrySet().stream();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public void track(BlockPos pos, T state) {
            map.put(pos.toImmutable(), state);
            onUpdate();
        }

        @Override
        public boolean untrack(BlockPos pos) {
            if (map.remove(pos) != null) {
                onUpdate();
                return true;
            }
            return false;
        }

        @Override
        public void clearAllChunks() {
            map.clear();
            onUpdate();
        }

        @Override
        public void clearChunk(ChunkPos pos) {
            if (map.keySet().removeIf(it -> it.getX() >= pos.getStartX() && it.getX() < pos.getEndX() && it.getZ() >= pos.getStartX() && it.getZ() < pos.getEndX())) {
                onUpdate();
            }
        }

        public void onUpdate() {}
    }
}
