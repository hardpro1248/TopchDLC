package gg.topchdlc.api.telegram;

import com.google.gson.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Create by daun kvass
 */
public final class TelegramPoller {

    private volatile boolean running = false;
    private Thread thread;
    private long lastUpdateId = -1;

    private String token;
    private String chatId;
    private String proxyHost;
    private int proxyPort;
    private String proxyUser;
    private String proxyPass;

    private Consumer<String> onCommand;

    public void start(String token, String chatId,
                      String proxyHost, int proxyPort, String proxyUser, String proxyPass,
                      Consumer<String> onCommand) {
        if (running) return;
        this.token = token;
        this.chatId = chatId;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyPass = proxyPass;
        this.onCommand = onCommand;
        this.running = true;
        this.lastUpdateId = -1;

        thread = new Thread(this::loop, "TelegramPoller");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        if (thread != null) thread.interrupt();
    }

    public boolean isRunning() { return running; }

    private void loop() {
        while (running) {
            try {
                String response = getUpdates();
                if (response != null) handleUpdates(response);
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                System.out.println("TgPoller error: " + e.getMessage());
                try { Thread.sleep(5000); } catch (InterruptedException ignored) { break; }
            }
        }
    }

    private String getUpdates() throws Exception {
        String urlStr = "https://api.telegram.org/bot" + token.trim() + "/getUpdates?timeout=1";
        if (lastUpdateId >= 0) urlStr += "&offset=" + (lastUpdateId + 1);

        URL url = new URL(urlStr);
        HttpURLConnection conn = openConnection(url);
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int code = conn.getResponseCode();
        if (code != 200) {
            conn.disconnect();
            return null;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        conn.disconnect();
        return sb.toString();
    }

    private void handleUpdates(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.get("ok").getAsBoolean()) return;
            JsonArray results = root.getAsJsonArray("result");
            for (JsonElement el : results) {
                JsonObject update = el.getAsJsonObject();
                long updateId = update.get("update_id").getAsLong();
                if (updateId <= lastUpdateId) continue;
                lastUpdateId = updateId;
                if (!update.has("message")) continue;
                JsonObject message = update.getAsJsonObject("message");
                if (message.has("chat")) {
                    String msgChatId = message.getAsJsonObject("chat").get("id").getAsString();
                    if (!msgChatId.equals(chatId.trim())) continue;
                }

                if (!message.has("text")) continue;
                String text = message.get("text").getAsString().trim();
                if (onCommand != null) onCommand.accept(text);
            }
        } catch (Exception e) {
            System.out.println("TgPoller parse error: " + e.getMessage());
        }
    }

    private HttpURLConnection openConnection(URL url) throws Exception {
        if (proxyHost != null && !proxyHost.isBlank() && proxyPort > 0) {
            if (proxyUser != null && !proxyUser.isBlank()) {
                final String user = proxyUser;
                final String pass = proxyPass != null ? proxyPass : "";
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        if (getRequestingHost().equalsIgnoreCase(proxyHost))
                            return new PasswordAuthentication(user, pass.toCharArray());
                        return null;
                    }
                });
            }
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort));
            return (HttpURLConnection) url.openConnection(proxy);
        }
        return (HttpURLConnection) url.openConnection();
    }
}
