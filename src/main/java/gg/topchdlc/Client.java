package gg.topchdlc;

import cc.snais.Info;
import gg.topchdlc.api.network.packets.ChallengePacket;
import gg.topchdlc.api.network.packets.ChallengeResponsePacket;
import gg.topchdlc.api.network.packets.VersionPacket;
import gg.topchdlc.vse.shutki.screen.screens.ingame.AutoBuyItemsScreen;
import gg.topchdlc.vse.shutki.screen.screens.ingame.BuilderTrainingGui;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.client.mixin.IChatHud;
import gg.topchdlc.vse.utils.client.sounds.SoundUtility;
import lombok.Getter;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.util.Identifier;
import gg.topchdlc.api.discord.DiscordRPCManager;
//import gg.topchdlc.api.party.PartyManager;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.friends.Friends;
import gg.topchdlc.api.friends.ClientUsers;

import gg.topchdlc.api.macros.Macros;
import gg.topchdlc.api.scripts.Scripts;
import gg.topchdlc.api.sound.SoundMixFilter;
import gg.topchdlc.vse.shutki.other.macros.MacroManager;
import gg.topchdlc.vse.shutki.screen.screens.ingame.AutoBuyConfigScreen;
import gg.topchdlc.vse.shutki.other.commands.Commands;
import gg.topchdlc.api.configs.Configs;
import gg.topchdlc.vse.shutki.screen.hud.notification.Notifies;
import gg.topchdlc.vse.shutki.screen.hud.ui.Hud;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.utils.block.ChunkScanner;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import gg.topchdlc.vse.utils.math.TimeUtility;
import gg.topchdlc.vse.utils.other.Scheduler;
import gg.topchdlc.vse.utils.client.sounds.MySoundEvents;
import net.fabricmc.api.ModInitializer;
import gg.topchdlc.api.drags.Drags;
import gg.topchdlc.api.events.Events;
import gg.topchdlc.vse.shutki.module.modules.Modules;
import gg.topchdlc.api.render.ClientRenderer;
import gg.topchdlc.api.render.msdf.Fonts;
import gg.topchdlc.vse.rotation.RotationHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Nullables;
import net.minecraft.util.Util;
import gg.topchdlc.vse.shutki.screen.screens.ingame.GlassClickGui;

import java.nio.file.Path;
import java.util.Random;

import static gg.topchdlc.MinecraftHolder.mc;


/**
 * Create by daun kvass
 */
@Getter
public class Client implements ModInitializer {
    public static Client INSTANCE;

    @Override
    public void onInitialize() {
        MySoundEvents.init();
        PayloadTypeRegistry.playS2C().register(ChallengePacket.ID, ChallengePacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ChallengeResponsePacket.ID, ChallengeResponsePacket.CODEC);
        PayloadTypeRegistry.playC2S().register(VersionPacket.ID, VersionPacket.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ChallengePacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                ClientPlayNetworking.send(new ChallengeResponsePacket(payload.id()));
            });
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!client.isIntegratedServerRunning()) {
                sender.sendPacket(new VersionPacket(1, 0, 0, false));
            }
        });

    }
    /**
     * MANAGERS
     */
    public static final String NAME = "TopchDLC", VER = "hyu", TYPE = "DEV";
    private static final String MOD_ID = "topchdlc";
    public static ClientRenderer RENDERER;
    public static Fonts FONTS;
    public static Events EVENTS;
    public static Commands COMMANDS;
    public static Modules MODULES;
    public static Drags DRAGS;
    public static GlassClickGui GLASS_GUI;
    public static BuilderTrainingGui BUILDER_TRAINING;
    public static AutoBuyConfigScreen AUTOBUY_CONFIG;
    public static AutoBuyItemsScreen AUTOBUY_ITEMS;
    public static RotationHandler ROTATION;
    public static Configs CONFIG;
    public static Hud HUD;
    public static Scripts SCRIPTS;
    public static Scheduler SCHEDULER;
    public static Notifies NOTIFIES;
    public static Friends FRIENDS;
    public static ClientUsers CLIENT_USERS;
    public static SoundMixFilter RTX_ENGINE;

    public static Macros MACROS;
    public static MacroManager MACRO_MANAGER;
    public static ChunkScanner ChunkScanner;
    public static DiscordRPCManager DISCORD_RPC;
    public static float TIMER = 1.0f;
    public static boolean INITIALIZED = false;
    public static boolean IS_DEBUG = false;
    public static boolean IS_PANIC = false;
    public static boolean IS_WINDOW_FOCUSED = true;
    public static final Path CLIENT_DIR = getClientDir();
    private static Path getClientDir() {
        return switch (Util.getOperatingSystem()) {
            case WINDOWS -> Path.of(System.getProperty("user.home"), "topchdlc");
            default -> Path.of(System.getProperty("user.home"), "topchdlc");
        };
    }

    public void init() {
        /*
         * MANAGERS INITIALIZATION
         */
        try {
            org.lwjgl.opengl.GL43.glDebugMessageControl(
                org.lwjgl.opengl.GL43.GL_DONT_CARE,
                org.lwjgl.opengl.GL43.GL_DONT_CARE,
                org.lwjgl.opengl.GL43.GL_DONT_CARE,
                (int[]) null,
                false
            );
        } catch (Exception ignored) {}
        
        CONFIG = new Configs();
        RENDERER = new ClientRenderer();
        FONTS = new Fonts();
        EVENTS = new Events();
        HUD = new Hud();
        FRIENDS = new Friends();
        CLIENT_USERS = new ClientUsers();
        COMMANDS = new Commands();

        MACROS = new Macros();
        MACRO_MANAGER = new MacroManager();
        MODULES = new Modules();
        DRAGS = new Drags();
        BUILDER_TRAINING = new BuilderTrainingGui();
        GLASS_GUI = new GlassClickGui();
        AUTOBUY_CONFIG = new AutoBuyConfigScreen();
        AUTOBUY_ITEMS = new AutoBuyItemsScreen();
        ROTATION = new RotationHandler();
        SCRIPTS = new Scripts();
        SCHEDULER = new Scheduler();
        NOTIFIES = new Notifies();
        ChunkScanner = new ChunkScanner();
        DISCORD_RPC = new DiscordRPCManager();
       // new PartyManager();
        FRIENDS.load();
        CLIENT_USERS.load();
        SCRIPTS.RELOAD();

        if (CLIENT_DIR.resolve("configs/default.gW").toFile().exists())
            CONFIG.load("default");
        INITIALIZED = true;
        EVENTS.register(this);
        EVENTS.register(ChunkScanner);
        if (DISCORD_RPC != null) {
            EventBus<Event> onEvent = event -> {
                if (event instanceof EventGameTick eventGameTick)
                    DISCORD_RPC.updatePresence(Info.NAME , Info.getServ());

            };
        }
    }
    public static void saveAll() {
        CONFIG.save("default");
        FRIENDS.save();
        CLIENT_USERS.save();
    }

    public static Client getInstance() {
        return INSTANCE;
    }
    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
