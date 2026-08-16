package gg.topchdlc.vse.utils.client.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.utils.other.LogUtility;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.apache.commons.codec.binary.Base64;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Загрузка скина игрока по нику через Mojang API.
 * Используется для кастомной модели персонажа (система скинов с поиском).
 */
public class SkinManager implements MinecraftHolder {
    public static final SkinManager INSTANCE = new SkinManager();

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private Identifier loadedSkin = null;
    private String loadedName = "";
    private boolean loading = false;
    private String error = "";

    private SkinManager() {
    }

    public Identifier getLoadedSkin() {
        return loadedSkin;
    }

    public String getLoadedName() {
        return loadedName;
    }

    public boolean isLoading() {
        return loading;
    }

    public String getError() {
        return error;
    }

    public void applySkin(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            error = "Введите ник игрока";
            return;
        }
        if (loading) return;

        loading = true;
        error = "";
        loadedSkin = null;
        loadedName = "";

        CompletableFuture.runAsync(() -> {
            try {
                String uuid = fetchUuid(playerName);
                if (uuid == null) {
                    finishWithError("Игрок не найден");
                    return;
                }
                String skinUrl = fetchSkinUrl(uuid);
                if (skinUrl == null) {
                    finishWithError("Скин не найден");
                    return;
                }
                NativeImage image = downloadSkin(skinUrl);
                if (image == null) {
                    finishWithError("Не удалось загрузить скин");
                    return;
                }
                mc.execute(() -> {
                    try {
                        Identifier id = Identifier.of("topchdlc", "skins/" + System.currentTimeMillis());
                        mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> "skin", image));
                        loadedSkin = id;
                        loadedName = playerName;
                    } catch (Exception e) {
                        error = "Ошибка: " + e.getMessage();
                        LogUtility.error(e, "registering skin");
                    } finally {
                        loading = false;
                    }
                });
            } catch (Exception e) {
                finishWithError(e.getMessage());
                LogUtility.error(e, "loading skin");
            }
        });
    }

    private void finishWithError(String message) {
        error = message;
        loading = false;
    }

    private String fetchUuid(String name) throws Exception {
        String url = "https://api.mojang.com/users/profiles/minecraft/" + name;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) return null;
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        return json.has("id") ? json.get("id").getAsString() : null;
    }

    private String fetchSkinUrl(String uuid) throws Exception {
        String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) return null;
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!json.has("properties")) return null;
        JsonArray properties = json.getAsJsonArray("properties");
        for (var prop : properties) {
            JsonObject p = prop.getAsJsonObject();
            if ("textures".equals(p.get("name").getAsString())) {
                String value = p.get("value").getAsString();
                String decoded = new String(Base64.decodeBase64(value), StandardCharsets.UTF_8);
                JsonObject textures = JsonParser.parseString(decoded).getAsJsonObject();
                if (textures.has("textures") && textures.getAsJsonObject("textures").has("SKIN")) {
                    return textures.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
                }
            }
        }
        return null;
    }

    private NativeImage downloadSkin(String skinUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(skinUrl)).GET().build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) return null;

            try (InputStream in = response.body()) {
                NativeImage image = NativeImage.read(in);
                return image == null ? null : image;
            }
        } catch (Exception e) {
            LogUtility.error(e, "downloading skin");
            return null;
        }
    }
}
