package gg.topchdlc.vse.shutki.other.commands.impl;

import gg.topchdlc.vse.shutki.other.commands.Command;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collection;

/**
 * Create by daun kvass
 */
public class ParseCommand extends Command {
    public static ParseCommand INSTANCE = new ParseCommand();

    public ParseCommand() {
        super("parse", "Парсит ники");
    }

    @Override
    public void execute(String[] args) {
        if (mc.getNetworkHandler() == null || mc.world == null) {
            error("на сервер");
            return;
        }

        File folder = new File(mc.runDirectory, "ccc");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, "parsed_players.txt");

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            Collection<PlayerListEntry> players = mc.getNetworkHandler().getPlayerList();

            writer.println("Парс таба");
            writer.println("[метод] Полное Имя с Префиксом");
            writer.println("ччччччччччччччччччччччччччччч");

            for (PlayerListEntry player : players) {
                String name = player.getProfile().name();
                String resultLine;
                String method;

                if (player.getDisplayName() != null) {
                    method = "display";
                    resultLine = player.getDisplayName().getString();
                }
                else if (mc.world.getScoreboard().getTeam(name) != null) {
                    method = "getScore";
                    Team team = mc.world.getScoreboard().getTeam(name);
                    resultLine = team.getPrefix().getString() + name + team.getSuffix().getString();
                }
                else {
                    method = "raw";
                    resultLine = name;
                }

                writer.println(method + " " + resultLine.trim());
            }
            ChatUtility.send(Text.literal("все вот тута в этом файле*_* parsed_players.txt")
                    .withColor(new Color(100, 255, 100).getRGB()));

        } catch (Exception e) {
            error("-" + e.getMessage());
        }
    }
}