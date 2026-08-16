/*package gg.topchdlc.vse.shutki.other.commands.impl;

import com.google.gson.JsonObject;
import net.minecraft.text.Text;
import gg.topchdlc.api.party.PartyApiClient;
import gg.topchdlc.vse.shutki.other.commands.Command;
import gg.topchdlc.vse.utils.client.text.ChatUtility;

import java.awt.Color;

public class PartyCommand extends Command {
    public static final PartyCommand INSTANCE = new PartyCommand();

    public PartyCommand() {
        super("party", "manage party", "create", "invite", "join", "leave", "disband", "list", "kick");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) { printHelp(); return; }

        switch (args[1].toLowerCase()) {
            case "create" -> {
                JsonObject req = new JsonObject();
                req.addProperty("leader", nick());
                PartyApiClient.postAsync("/party/create", req, json -> {
                    if (json.has("error")) { send("Ошибка: " + json.get("error").getAsString()); return; }
                    String code = json.get("code").getAsString();
                    send("Пати создано! Код для входа: " + code);
                    send("Скажи другу: .party join " + code);
                });
            }
            case "invite" -> {
                if (args.length < 3) { error("Использование: .party invite <игрок>"); return; }
                JsonObject req = new JsonObject();
                req.addProperty("leader", nick());
                req.addProperty("target", args[2]);
                PartyApiClient.postAsync("/party/invite", req, json ->
                        send("Приглашение отправлено: " + args[2])
                );
            }
            case "join" -> {
                if (args.length < 3) { error("Использование: .party join <код>"); return; }
                JsonObject req = new JsonObject();
                req.addProperty("player", nick());
                req.addProperty("code", args[2].toUpperCase());
                PartyApiClient.postAsync("/party/join", req, json -> {
                    if (json.has("error")) { send("Ошибка: " + json.get("error").getAsString()); return; }
                    send("Вы вошли в пати");
                });
            }
            case "leave" -> {
                JsonObject req = new JsonObject();
                req.addProperty("player", nick());
                PartyApiClient.postAsync("/party/leave", req, json ->
                        send("Вы покинули пати")
                );
            }
            case "disband" -> {
                JsonObject req = new JsonObject();
                req.addProperty("leader", nick());
                PartyApiClient.postAsync("/party/disband", req, json ->
                        send("Пати распущено")
                );
            }
            case "kick" -> {
                if (args.length < 3) { error("Использование: .party kick <игрок>"); return; }
                JsonObject req = new JsonObject();
                req.addProperty("leader", nick());
                req.addProperty("target", args[2]);
                PartyApiClient.postAsync("/party/kick", req, json ->
                        send("Кикнут: " + args[2])
                );
            }
            case "list" -> {
                JsonObject req = new JsonObject();
                req.addProperty("player", nick());
                PartyApiClient.postAsync("/party/list", req, json -> {
                    String msg = json.get("message").getAsString();
                    String code = json.has("code") ? json.get("code").getAsString() : "?";
                    send("Код пати: " + code);
                    send("Игроки: " + msg);
                });
            }
            default -> printHelp();
        }
    }

    private String nick() {
        return mc.player.getNameForScoreboard();
    }

    private void send(String msg) {
        ChatUtility.send(Text.literal(msg).withColor(new Color(180, 180, 180).getRGB()));
    }

    private void printHelp() {
        send(".party create | join <код> | leave | disband | list | invite <p> | kick <p>");
    }
}
*/