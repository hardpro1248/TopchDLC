package gg.topchdlc.api.discord;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import cc.snais.RPCData;
import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.discord.jni.DiscordEventHandlers;
import gg.topchdlc.api.discord.jni.DiscordRPC;
import gg.topchdlc.api.discord.jni.DiscordRichPresence;
import gg.topchdlc.api.render.util.UrlTextureLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class DiscordRPCManager implements MinecraftHolder {
    private static final String APPLICATION_ID = "1502713681031467078";

    private static final long RETRY_INTERVAL_MS = 15_000L;

    private final DiscordRPC rpc = DiscordRPC.INSTANCE;
    private RPCData userData;
    private volatile boolean started = false;
    private volatile boolean discordReady = false;
    private volatile boolean shutdownRequested = false;
    private Thread thread;
    private final DiscordRichPresence presence = new DiscordRichPresence();
    private long startTimestamp;

    public DiscordRPCManager() {
        initialize();
    }
    
    private void initialize() {
        try {
            startTimestamp = System.currentTimeMillis() / 1000;
            startRpc();
        } catch (Exception e) {
        }
    }
    
    private void startRpc() {
        if (thread != null && thread.isAlive()) return;

        DiscordEventHandlers handlers = new DiscordEventHandlers();
        handlers.ready = (user) -> {
            if (user.userId != null && user.username != null) {
                if (userData != null && userData.getAvatar() != null) {
                    UrlTextureLoader.invalidate(userData.getAvatar());
                }
                String avatarUrl = null;
                if (user.avatar != null && !user.avatar.isEmpty()) {
                    String ext = user.avatar.startsWith("a_") ? ".gif" : ".png";
                    avatarUrl = "https://cdn.discordapp.com/avatars/" + user.userId + "/" + user.avatar + ext + "?size=128";
                }
                long id;
                try { id = Long.parseLong(user.userId); } catch (NumberFormatException e) { id = 0L; }
                userData = new RPCData(user.username, id, avatarUrl);
                discordReady = true;
            }
        };

        thread = new Thread(() -> {
            while (!shutdownRequested) {
                try {
                    rpc.Discord_Initialize(APPLICATION_ID, handlers, true, "");
                    started = true;
                    presence.startTimestamp = startTimestamp;
                    presence.largeImageKey = "topchdlc_logo";
                    presence.largeImageText = "TopchDLC Client";
                    updatePresenceFields();
                    rpc.Discord_UpdatePresence(presence);
                    while (!shutdownRequested) {
                        if (Client.IS_WINDOW_FOCUSED) {
                            rpc.Discord_RunCallbacks();
                            Thread.sleep(2000L);
                        } else {
                            Thread.sleep(10000L);
                        }
                    }
                    break;

                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    started = false;
                    try { rpc.Discord_Shutdown(); } catch (Exception ignored) {}
                    try { Thread.sleep(RETRY_INTERVAL_MS); } catch (InterruptedException ie) { break; }
                }
            }
        }, "Discord-RPC-Callback");
        thread.setDaemon(true);
        thread.start();
    }
    
    public void updatePresence(String state,String details) {
        if (Client.IS_PANIC || !started) return;
        try {
            presence.state = state;
          presence.details = details;
            updatePresenceFields();
            rpc.Discord_UpdatePresence(presence);
        } catch (Exception ignored) {}
    }
    public String getAvatarUrl() {
        return userData != null ? userData.getAvatar() : null;
    }
    private void updatePresenceFields() {
        if (userData != null) {
            if (presence.details == null || presence.details.isEmpty()) {
                presence.details = "User: " + userData.getName();
            }
        }
        presence.button_label_1 = "ne";
        presence.button_url_1 = "https://www.youtube.com/@qu4ick";
        presence.button_label_2 = "Tg developer";
        presence.button_url_2 = "https://t.me/Yn0Gasai";
    }
    
    public void updateCallback() {
        if (Client.IS_PANIC) return;
    }

    
    public String getUserName() {
        return userData != null ? userData.getName() : "Kvass";
    }

    
    public boolean isInitialized() {
        return started && discordReady;
    }
    
    public void shutdown() {
        shutdownRequested = true;
        started = false;
        try {
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
            rpc.Discord_Shutdown();
        } catch (Exception ignored) {}
    }
}