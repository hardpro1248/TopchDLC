package gg.topchdlc.vse.utils.block;

import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.EventPriority;
import gg.topchdlc.api.events.list.EventChangeWorld;
import gg.topchdlc.api.events.list.EventLoadChunk;
import gg.topchdlc.api.events.list.EventReceivePacket;
import gg.topchdlc.vse.utils.other.LogUtility;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

public class ChunkScanner implements MinecraftHolder {
    private ForkJoinPool executor = new ForkJoinPool(Math.max(Runtime.getRuntime().availableProcessors() / 2, 2));
    private final LongSet loadedChunks = new LongOpenHashSet();
    private final ThreadLocal<BlockPos.Mutable> tls_blockpos = ThreadLocal.withInitial(BlockPos.Mutable::new);
    private final List<Subscriber> subscribers = new CopyOnWriteArrayList<>();
    private final List<Subscriber> recordOnUpdateSubscribers = new CopyOnWriteArrayList<>();

    public void subscribe(Subscriber subscriber) {
        if (this.subscribers.contains(subscriber)) return;
        this.subscribers.add(subscriber);
        if (subscriber.recordAllOnUpdate()) recordOnUpdateSubscribers.add(subscriber);

        ClientWorld world = mc.world;
        if (world == null) return;
        List<WorldChunk> chunks = new ObjectArrayList<>(
                this.loadedChunks
                        .longParallelStream()
                        .mapToObj(it -> world.getChunk(ChunkPos.getPackedX(it), ChunkPos.getPackedZ(it)))
                        .filter(chunk -> !chunk.isEmpty())
                        .toList()
        );
        if (chunks.isEmpty()) return;

        executor.execute(() -> this.onNewSubscriber(subscriber, chunks));
    }

    public void unsubscribe(Subscriber subscriber) {
        subscribers.remove(subscriber);
        if (subscriber.recordAllOnUpdate())
            recordOnUpdateSubscribers.remove(subscriber);
    }

    void scanChunkSections(WorldChunk chunk, BiConsumer<BlockPos, BlockState> fn) {
        int sections = chunk.getHighestNonEmptySection() + 1;
        for (int section_idx = 0; section_idx < sections; section_idx++) {
            int start_x = chunk.getPos().getStartX();
            int start_z = chunk.getPos().getStartZ();
            BlockPos.Mutable blockpos = tls_blockpos.get();
            ChunkSection section = chunk.getSection(section_idx);
            for (int section_y = 0; section_y < 16; section_y++) {
                int y = (section_idx + (chunk.getBottomY() >> 4)) << 4 | section_y;
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                    BlockState state = section.getBlockState(x, section_y, z);
                    BlockPos pos = blockpos.set(start_x | x, y, start_z | z);
                    fn.accept(pos, state);
                }
            }
        }
    }

    void onNewSubscriber(Subscriber subscriber, List<WorldChunk> chunks) {
        long now1 = System.nanoTime();
        chunks.forEach(subscriber::onChunkUpdate);
        if (subscriber.recordAllOnUpdate()) {
            for (WorldChunk chunk : chunks) {
                scanChunkSections(chunk, (pos, state) -> subscriber.recordBlock(pos, state, true));
            }
        }
        long now2 = System.nanoTime();
        LogUtility.debug(String.format("Scanning %d chunks for %s took %d ns", chunks.size(), subscriber.getClass().getSimpleName(), now2 - now1));
    }

    void onChunkLoad(WorldChunk chunk) {
        long now1 = System.nanoTime();
        subscribers.forEach(subscriber -> subscriber.onChunkUpdate(chunk));

        if (!recordOnUpdateSubscribers.isEmpty()) {
            scanChunkSections(chunk, (pos, state) -> {
                for (Subscriber subscriber : recordOnUpdateSubscribers) {
                    subscriber.recordBlock(pos, state, true);
                }
            });
        }

        long now2 = System.nanoTime();
        LogUtility.debug(String.format("Scanning chunk{%d;%d} took %d ns", chunk.getPos().x, chunk.getPos().z, now2 - now1));
    }

    void onBlockUpdate(BlockPos pos, BlockState state) {
        subscribers.forEach(subscriber -> subscriber.recordBlock(pos, state, false));
    }

    void onChunkUnload(ChunkPos pos) {
        subscribers.forEach(subscriber -> subscriber.clearChunk(pos));
    }

    void onChunkSectionUpdate(ChunkDeltaUpdateS2CPacket packet) {
        packet.visitUpdates((pos,state) -> subscribers.forEach(subscriber -> subscriber.recordBlock(pos, state, false)));
    }

    @EventPriority(EventPriority.LAST)
    EventBus<Event> events = event -> {
        if (event instanceof EventChangeWorld) {
            executor.shutdownNow();
            executor = new ForkJoinPool(Math.max(Runtime.getRuntime().availableProcessors() / 2, 2));
            loadedChunks.clear();
            subscribers.forEach(Subscriber::clearAllChunks);
        }
        if (event instanceof EventLoadChunk e) {
            if (mc.world == null) return;
            if (!Client.IS_WINDOW_FOCUSED) return;

            WorldChunk chunk = mc.world.getChunk(e.x, e.z);
            if (chunk == null || chunk.isEmpty()) return;

            loadedChunks.add(ChunkPos.toLong(e.x, e.z));
            if (subscribers.isEmpty()) return;
            executor.execute(() -> this.onChunkLoad(chunk));
        }
        if (event instanceof EventReceivePacket e) {
            if (!Client.IS_WINDOW_FOCUSED) return;
            
            switch (e.packet) {
                case BlockUpdateS2CPacket packet -> executor.execute(() -> onBlockUpdate(packet.getPos(), packet.getState()));
                case ChunkDeltaUpdateS2CPacket packet -> executor.execute(() -> onChunkSectionUpdate(packet));
                case UnloadChunkS2CPacket packet -> mc.execute(() -> {
                    loadedChunks.remove(packet.pos().toLong());
                    executor.execute(() -> onChunkUnload(packet.pos()));
                });
                default -> {}
            }
        }
    };

    public void shutdown() {
        executor.shutdownNow();
    }

    public interface Subscriber {
        default boolean recordAllOnUpdate() { return true; }

        void recordBlock(BlockPos pos, BlockState state, boolean isCleared);
        void onChunkUpdate(WorldChunk chunk);
        void clearChunk(ChunkPos pos);
        void clearAllChunks();
    }
}
