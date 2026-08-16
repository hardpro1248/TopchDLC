package gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;
import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick.icon.NurickIcons;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.math.MathUtility;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class StaffNurick extends HudElement {
    private static final String TITLE  = "Staff online";
    private static final float TEXT_SIZEX = 8f;
    private static final float TEXT_SIZE   = 6.5f;
    private static final float ICON_SIZE   = 7.5f;
    private static final float HEADER_H    = 12f;
    private static final float ROW_HEIGHT  = 12f;
    private static final Vector4f ROUND    = new Vector4f(5.5f, 5.5f, 5.5f, 5.5f);

    private final Map<PlayerListEntry, StaffType> staff = new HashMap<>();

    String[] pattern = {
            "admin", "moder", "helper", "staff", "owner", "dev",
            "админ", "модер", "хелпер", "стажер", "куратор",
            "ᴀᴅᴍɪɴ", "ʜᴇʟᴘᴇʀ","ᴍᴏᴅᴇʀ", "ᴏᴡɴᴇʀ"
    };

    public StaffNurick(Drag drag) {
        super("NurickStaff", drag);
        Client.EVENTS.register(this);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        boolean shown = !staff.isEmpty() || mc.currentScreen instanceof ChatScreen;
        animation.setDirection(shown ? Direction.BACKWARDS : Direction.FORWARDS);
        float anim = 1F - animation.getOutput();
        if (anim < 0.05f) return;

        CRenderSystem system = Client.RENDERER.getCrenderSystem();
        system.push(x, y, width, height);
        float originalAlpha = system.alpha();
        system.alpha(anim * originalAlpha);

        float iconW = Client.RENDERER.textWidth(NurickIcons.EYE_OPEN, TextureUse.ICONS_NURIK, ICON_SIZE);
        float titleW = Client.RENDERER.textWidth(TITLE, TextureUse.SFMEDIUM, TEXT_SIZEX);
        float headerPillW = 8f + iconW + 5f + 0.6f + 5f + titleW + 15f;

        float maxContentWidth = headerPillW;
        for (PlayerListEntry entry : staff.keySet()) {
            Text fullText = getPlayerDisplayName(entry);
            float nameW = Client.RENDERER.textWidth(Formatting.strip(fullText.getString()), TextureUse.SFMEDIUM, TEXT_SIZE - 0.5f);
            float w = 18f + nameW + 20f;
            if (w > maxContentWidth) maxContentWidth = w;
        }

        float totalHeight = HEADER_H + 2 + (staff.isEmpty() ? 0 : (staff.size() * ROW_HEIGHT) + 1f);

        Client.RENDERER.blur(x, y, maxContentWidth, totalHeight, ROUND, 12f, 1f);
        Color bg = new Color(15, 15, 20, 160);
        Client.RENDERER.rect(x, y, maxContentWidth, totalHeight, ROUND, 1f, bg, bg, bg, bg);

        float iconHeight = Client.FONTS.get(TextureUse.ICONS_NURIK).getMetrics().baselineHeight() * ICON_SIZE;
        Client.RENDERER.text(NurickIcons.EYE_OPEN, x + 6f, y + (HEADER_H / 2f) - (iconHeight / 2f) + 1.5f, TextureUse.ICONS_NURIK, ICON_SIZE, Color.WHITE) ;

        float sepX = x + 6f + iconW + 5f;
        Color sepCol = new Color(255, 255, 255, 60);
        Client.RENDERER.rect(sepX, y + 1 + (HEADER_H - 7.5f) / 2f, 0.6f, 7.5f, new Vector4f(1), 1f, sepCol, sepCol, sepCol, sepCol);

        float titleY = y + (HEADER_H / 2f) - (Client.FONTS.get(TextureUse.SFMEDIUM).getMetrics().baselineHeight() * TEXT_SIZE / 2f) + 1;
        Client.RENDERER.text(TITLE, sepX + 5.5f, titleY, TextureUse.SFMEDIUM, TEXT_SIZE, Color.WHITE);

        if (!staff.isEmpty()) {
            float currentY = y + HEADER_H + 1;
            for (Map.Entry<PlayerListEntry, StaffType> entry : staff.entrySet()) {
                PlayerListEntry player = entry.getKey();
                StaffType type = entry.getValue();

                SkinTextures textures = mc.getSkinProvider().supplySkinTextures(player.getProfile(), true).get();
                Client.RENDERER.renderHead(textures, x + 6f, currentY + 3f, 7);

                Text fullText = getPlayerDisplayName(player);

                Client.RENDERER.text(fullText, x + 18f, currentY + 3f, TextureUse.SFMEDIUM, TEXT_SIZE - 0.5f);

                if (type == StaffType.SPECTATE) {
                    float textW = Client.RENDERER.textWidth(Formatting.strip(fullText.getString()), TextureUse.SFMEDIUM, TEXT_SIZE - 0.5f);
                    float eyeH = Client.FONTS.get(TextureUse.ICONS_NURIK).getMetrics().baselineHeight() * (ICON_SIZE - 2f);
                    float eyeY = currentY + 3f + (((TEXT_SIZE - 0.5f) / 2f) - (eyeH / 2f));
                    Client.RENDERER.text(NurickIcons.EYE_CLOSED, x + 18f + textW + 5f, eyeY,
                            TextureUse.ICONS_NURIK, ICON_SIZE - 0.4f, new Color(255, 255, 255, 200));
                }

                currentY += ROW_HEIGHT;
            }
        }

        drag.width = MathUtility.linearFps(drag.width, maxContentWidth, 10);
        drag.height = MathUtility.linearFps(drag.height, totalHeight, 10);

        system.alpha(originalAlpha);
        system.pop();
    }


    private Text getPlayerDisplayName(PlayerListEntry player) {
        if (player.getDisplayName() != null) {
            return player.getDisplayName();
        }
        if (player.getScoreboardTeam() != null) {
            MutableText prefix = player.getScoreboardTeam().getPrefix().copy();
            return prefix.append(player.getProfile().name());
        }
        return Text.literal(player.getProfile().name());
    }

    EventBus<EventGameTick> gametick = event -> {
        if (mc.player != null && mc.player.age % 20 == 0) {
            staff.clear();
            if (mc.getNetworkHandler() == null) return;

            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                String name = entry.getProfile().name();
                if (name.equals(mc.player.getGameProfile().name())) continue;

                String display = entry.getDisplayName() != null ? entry.getDisplayName().getString() : "";
                String teamPrefix = entry.getScoreboardTeam() != null ? entry.getScoreboardTeam().getPrefix().getString() : "";
                String fullTextClean = Formatting.strip(display + " " + teamPrefix + " " + name).toLowerCase();

                boolean isStaff = false;
                if (entry.getGameMode() == net.minecraft.world.GameMode.SPECTATOR) {
                    isStaff = true;
                } else {
                    for (String word : pattern) {
                        if (fullTextClean.contains(word.toLowerCase())) {
                            isStaff = true;
                            break;
                        }
                    }
                }
                if (isStaff) {
                    staff.put(entry, entry.getGameMode() == net.minecraft.world.GameMode.SPECTATOR ? StaffType.SPECTATE : StaffType.ACTIVE);
                }
            }
        }
    };

    enum StaffType {
        ACTIVE, SPECTATE
    }
}