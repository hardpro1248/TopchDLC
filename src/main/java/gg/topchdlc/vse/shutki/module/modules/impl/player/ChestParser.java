package gg.topchdlc.vse.shutki.module.modules.impl.player;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.utils.block.ChunkScanner;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Create by daun kvass
 */
public class ChestParser extends Module implements ChunkScanner.Subscriber {
    public static final ChestParser INSTANCE = new ChestParser();

    private final CheckBox chatNotification = checkbox("Уведомление в чат", true);
    private final CheckBox saveTrapped = checkbox("Парсить сундуки-ловушки", true);

    private final Set<BlockPos> parsedChests = ConcurrentHashMap.newKeySet();
    private Path filePath;

    private ChestParser() {
        super("ChestParser", Category.Misc, "ччч");
    }

    @Override
    protected void onEnable() {
        filePath = Client.CLIENT_DIR.resolve("chests.txt");
        parsedChests.clear();

        loadExistingChests();

        ChunkScanner scanner = Client.ChunkScanner;
        if (scanner != null) {
            scanner.subscribe(this);
            ChatUtility.send("in base load" + parsedChests.size() + "unique");
        }
    }

    @Override
    protected void onDisable() {
        ChunkScanner scanner = Client.ChunkScanner;
        if (scanner != null) {
            scanner.unsubscribe(this);
        }
        parsedChests.clear();
    }

    EventBus<Event> bus = event -> {
    };

    @Override
    public void recordBlock(BlockPos pos, BlockState state, boolean isCleared) {
        if (isCleared || mc.world == null) return;

        boolean isChest = state.isOf(Blocks.CHEST);
        boolean isTrappedChest = saveTrapped.get() && state.isOf(Blocks.TRAPPED_CHEST);

        if (isChest || isTrappedChest) {
            BlockPos immutablePos = pos.toImmutable();

            if (!parsedChests.contains(immutablePos)) {
                saveChestToFile(immutablePos);
            }
        }
    }

    @Override
    public void onChunkUpdate(WorldChunk chunk) {
    }

    @Override
    public void clearChunk(ChunkPos pos) {
    }

    @Override
    public void clearAllChunks() {
    }

    private void loadExistingChests() {
        if (filePath == null) return;
        File file = filePath.toFile();
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] coords = line.split(" ");
                if (coords.length >= 3) {
                    try {
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        int z = Integer.parseInt(coords[2]);
                        parsedChests.add(new BlockPos(x, y, z));
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) {
            System.err.println("fail to read base " + e.getMessage());
        }
    }

    private synchronized void saveChestToFile(BlockPos pos) {
        if (parsedChests.contains(pos)) return;

        parsedChests.add(pos);

        try {
            Files.createDirectories(filePath.getParent());
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath.toFile(), true), StandardCharsets.UTF_8))) {
                writer.write(pos.getX() + " " + pos.getY() + " " + pos.getZ());
                writer.newLine();
            }

            if (chatNotification.get()) {
                ChatUtility.send("+ new chest" + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " all " + parsedChests.size() + ")");
            }
        } catch (Exception e) {
            System.err.println("fail " + e.getMessage());
        }
    }
}