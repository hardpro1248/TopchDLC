/*package gg.topchdlc.api.party;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import net.minecraft.text.Text;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.utils.client.text.ChatUtility;

import java.awt.Color;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class PartyApiClient implements MinecraftHolder {
//https://kvass.pythonanywhere.com
    private static final String BASE_URL = "x";

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Party-API");
        t.setDaemon(true);
        return t;
    });

    @Getter
    private static volatile List<PartyPlayerPos> cached = List.of();

    public static void postAsync(String path, JsonObject body, Consumer<JsonObject> callback) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .header("Content-Type", "application/json")
                .build();

        HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenAcceptAsync(response -> {
                    try {
                        String text = response.body().trim();
                        if (!text.startsWith("{")) return;
                        JsonObject json = JsonParser.parseString(text).getAsJsonObject();
                        mc.execute(() -> callback.accept(json));
                    } catch (Exception ignored) {}
                }, EXECUTOR)
                .exceptionally(e -> null);
    }

    public static void fetchPartyStateAsync() {
        if (mc.player == null) return;

        JsonObject req = new JsonObject();
        req.addProperty("player", mc.player.getNameForScoreboard());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/party/state"))
                .POST(HttpRequest.BodyPublishers.ofString(req.toString()))
                .header("Content-Type", "application/json")
                .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAcceptAsync(response -> {
                    try {
                        if (response.statusCode() != 200) return;
                        String body = response.body().trim();
                        if (!body.startsWith("{")) return;

                        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                        JsonArray arr = json.getAsJsonArray("members");

                        List<PartyPlayerPos> list = new ArrayList<>();
                        for (JsonElement e : arr) {
                            JsonObject o = e.getAsJsonObject();
                            list.add(new PartyPlayerPos(
                                    o.get("playerId").getAsString(),
                                    o.get("x").getAsDouble(),
                                    o.get("y").getAsDouble(),
                                    o.get("z").getAsDouble()
                            ));
                        }
                        cached = list;
                    } catch (Exception ignored) {}
                }, EXECUTOR)
                .exceptionally(e -> null);
    }

    public static void fetchInvitesAsync() {
        if (mc.player == null) return;

        JsonObject req = new JsonObject();
        req.addProperty("player", mc.player.getNameForScoreboard());

        postAsync("/party/invites", req, json -> {
            JsonArray arr = json.getAsJsonArray("invites");
            for (JsonElement e : arr) {
                String party = e.getAsString();
                ChatUtility.send(Text.literal("Вас пригласили в пати ")
                        .withColor(new Color(180, 180, 180).getRGB())
                        .append(Text.literal(party).withColor(new Color(255, 255, 255).getRGB()))
                        .append(Text.literal(", напишите ").withColor(new Color(180, 180, 180).getRGB()))
                        .append(Text.literal(".party join " + party).withColor(new Color(255, 255, 255).getRGB()))
                );
            }
        });
    }

    public static void sendPositionAsync() {
        if (mc.player == null) return;

        JsonObject j = new JsonObject();
        j.addProperty("player", mc.player.getNameForScoreboard());
        j.addProperty("x", mc.player.getX());
        j.addProperty("y", mc.player.getY());
        j.addProperty("z", mc.player.getZ());

        postAsync("/party/pos", j, ignored -> {});
    }
}*/
