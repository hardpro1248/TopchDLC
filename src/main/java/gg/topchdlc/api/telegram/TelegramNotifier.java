package gg.topchdlc.api.telegram;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
/**
 * Create by daun kvass
 */
public final class TelegramNotifier {

    private TelegramNotifier() {}

    public static void sendAsync(String token, String chatId, String text) {
        sendAsync(token, chatId, text, null, null, -1, null, null);
    }

    public static void sendAsync(String token, String chatId, String text, Consumer<String> onResult) {
        sendAsync(token, chatId, text, onResult, null, -1, null, null);
    }

    public static void sendAsync(String token, String chatId, String text, Consumer<String> onResult,
                                 String proxyHost, int proxyPort, String proxyUser, String proxyPass) {
        if (token == null || token.isBlank() || chatId == null || chatId.isBlank()) {
            if (onResult != null) onResult.accept("Токен или Chat ID пустые");
            return;
        }
        Thread thread = new Thread(() -> {
            System.out.println("Tg sending, proxy=" + (proxyHost != null && !proxyHost.isBlank() ? proxyHost + ":" + proxyPort : "none"));
            String error = send(token, chatId, text, proxyHost, proxyPort, proxyUser, proxyPass);
            System.out.println("Tg result: " + (error == null ? "OK" : error));
            if (onResult != null) onResult.accept(error);
        }, "TelegramNotifier");
        thread.setDaemon(true);
        thread.start();
    }

    private static String send(String token, String chatId, String text,
                                String proxyHost, int proxyPort, String proxyUser, String proxyPass) {
        try {
            URL url = new URL("https://api.telegram.org/bot" + token.trim() + "/sendMessage");

            HttpURLConnection conn;
            if (proxyHost != null && !proxyHost.isBlank() && proxyPort > 0) {
                if (proxyUser != null && !proxyUser.isBlank()) {
                    final String user = proxyUser;
                    final String pass = proxyPass != null ? proxyPass : "";
                    Authenticator.setDefault(new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            if (getRequestingHost().equalsIgnoreCase(proxyHost)) {
                                return new PasswordAuthentication(user, pass.toCharArray());
                            }
                            return null;
                        }
                    });
                }
                Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort));
                conn = (HttpURLConnection) url.openConnection(proxy);
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            String escaped = text
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n");

            String body = "{\"chat_id\":\"" + chatId.trim() + "\",\"text\":\"" + escaped + "\"}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
                conn.disconnect();
                return "HTTP " + code + ": " + sb;
            }
            conn.disconnect();
            return null;
        } catch (Exception e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }
}
