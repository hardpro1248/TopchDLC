package gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.vse.shutki.module.modules.impl.misc.NameProtect;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ScoreboardNurick extends HudElement {

    public ScoreboardNurick(Drag drag) {
        super("Scoreboard", drag);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (mc.world == null || mc.player == null) return;

        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);

        if (objective == null) return;

        Text titleText = protectText(objective.getDisplayName());
        String titlePlain = Formatting.strip(titleText.getString());

        List<Text> lines = new ArrayList<>();

        Collection<ScoreboardEntry> scores = scoreboard.getScoreboardEntries(objective);
        List<ScoreboardEntry> sortedScores = scores.stream()
                .filter(score -> !score.hidden())
                .sorted(java.util.Comparator.comparingInt(ScoreboardEntry::value)
                        .reversed()
                        .thenComparing(e -> e.owner()))
                .collect(Collectors.toList());

        for (ScoreboardEntry entry : sortedScores) {
            Team team = scoreboard.getScoreHolderTeam(entry.owner());

            Text lineText = entry.display() != null ? entry.display() : Team.decorateName(team, Text.literal(entry.owner()));
            Text protectedLine = protectText(lineText);
            String plain = Formatting.strip(protectedLine.getString()).trim();
            if (plain.isEmpty() || isSeparator(plain)) continue;

            lines.add(protectedLine);
        }

        if (titlePlain.trim().isEmpty() && lines.isEmpty()) return;

        if (Client.FONTS.get(TextureUse.SFMEDIUM) == null) return;

        float titleSize = 7f;
        float entrySize = 6.5f;
        float padX = 8f;
        float padTop = 6f;
        float padBottom = 6f;

        float baseline = Client.FONTS.get(TextureUse.SFMEDIUM).getMetrics().baselineHeight();
        float titleRow = baseline * titleSize + 4f;
        float entryRow = baseline * entrySize + 4f;

        float maxWidth = titlePlain.trim().isEmpty() ? 0f : Client.RENDERER.textWidth(titlePlain, TextureUse.SFMEDIUM, titleSize);
        for (Text line : lines) {
            String s = Formatting.strip(line.getString());
            if (s.isEmpty()) continue;
            float w = Client.RENDERER.textWidth(s, TextureUse.SFMEDIUM, entrySize);
            if (w > maxWidth) maxWidth = w;
        }

        float W = maxWidth + padX * 2f;
        float H = padTop + titleRow + 2f + (lines.isEmpty() ? 0f : lines.size() * entryRow) + padBottom;

        drag.width = W;
        drag.height = H;

        Client.RENDERER.drawHudRect(x, y, W, H);

        CRenderSystem sys = Client.RENDERER.getCrenderSystem();
        float prevA = sys.alpha();

        float currY = y;

        if (!titlePlain.trim().isEmpty()) {
            float titleW = Client.RENDERER.textWidth(titlePlain, TextureUse.SFMEDIUM, titleSize);
            drawColorText(titleText, x + (W - titleW) / 2f, currY + padTop + titleRow * 0.72f, titleSize);
            currY += padTop + titleRow;

            Color sep = new Color(255, 255, 255, 28);
            Client.RENDERER.rect(x + padX, currY + 1f, W - padX * 2f, 0.5f, new Vector4f(0.25f), 1f, sep, sep, sep, sep);
            currY += 4f;
        } else {
            currY += padTop + 2f;
        }

        for (Text line : lines) {
            drawColorText(line, x + padX, currY + entryRow * 0.72f, entrySize);
            currY += entryRow;
        }

        sys.alpha(prevA);
    }

    private Text protectText(Text text) {
        if (!NameProtect.INSTANCE.isEnabled()) return text;

        MutableText result = Text.empty();
        for (Text part : text.getWithStyle(text.getStyle())) {
            String raw = part.getString();
            String replaced = NameProtect.INSTANCE.replace(raw);
            result.append(Text.literal(replaced).setStyle(part.getStyle()));
        }
        return result;
    }

    private boolean isSeparator(String s) {
        String clean = s.replace(" ", "");
        return clean.contains("====") || clean.contains("----") || clean.contains("————");
    }

    private void drawColorText(Text text, float x, float y, float size) {
        float offset = 0;
        for (Text part : text.getWithStyle(text.getStyle())) {
            String content = part.getString().replace("§" , "");

            if (content.isEmpty()) continue;

            Color color = ClientColors.FORE_COLOR;
            if (part.getStyle().getColor() != null) {
                color = new Color(part.getStyle().getColor().getRgb());
            }

            Client.RENDERER.text(content, x + offset, y, TextureUse.SFMEDIUM, size, color);
            offset += Client.RENDERER.textWidth(content, TextureUse.SFMEDIUM, size);
        }
    }
}