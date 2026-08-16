package gg.topchdlc.vse.shutki.screen.hud.ui.impl.main;

import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.math.MathUtility;
import com.mojang.authlib.GameProfile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class StaffElement extends HudElement {
    private final EnumSetting<Style> style = settings.enumSetting("Стиль", Style.GLASS);

    private static final float HEIGHT      = 14.5f;
    private static final float PADDING_SIDE = 4.0f;
    private static final float SPACING     = 1.5f;
    private static final float TEXT_SIZE   = 7.0f;
    private static final float ICON_SIZE   = 8.0f;

    private final Map<PlayerListEntry, StaffType> staff = new HashMap<>();

    public StaffElement(Drag drag) {
        super("Staff", drag);
        Client.EVENTS.register(this);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        boolean shown = !staff.isEmpty() || mc.currentScreen instanceof ChatScreen;
        animation.setDirection(shown ? Direction.BACKWARDS : Direction.FORWARDS);

        float animAlpha = MathHelper.clamp(1F - animation.getOutput(), 0f, 1f);
        if (animAlpha < 0.01f) return;

        boolean isRightSide = (x + width / 2f) > (mc.getWindow().getScaledWidth() / 2f);
        float currentY = y;

        renderHeader(currentY, isRightSide, animAlpha);
        currentY += HEIGHT + 1.5f;

        float maxRowWidth = 85;

        for (Map.Entry<PlayerListEntry, StaffType> entry : staff.entrySet()) {
            PlayerListEntry player = entry.getKey();
            StaffType type = entry.getValue();

            Text fullText = getPlayerNameWithPrefix(player);
            String nameString = Formatting.strip(fullText.getString());

            float nameW = Client.RENDERER.textWidth(nameString, TextureUse.SFMEDIUM, TEXT_SIZE);
            float skinW = 9f;
            float rowContentW = PADDING_SIDE + skinW + 2f + nameW + PADDING_SIDE;

            float indicatorW = (type == StaffType.SPECTATE) ? (HEIGHT) : 0f;
            float totalRowW = rowContentW + (indicatorW > 0 ? SPACING + indicatorW : 0);

            maxRowWidth = Math.max(maxRowWidth, totalRowW);
            float rowX = isRightSide ? (x + width - totalRowW) : x;

            drawStyle(rowX, currentY, rowContentW, HEIGHT, animAlpha);

            SkinTextures skin = mc.getSkinProvider().supplySkinTextures(player.getProfile(), true).get();
            Client.RENDERER.renderHead(skin, rowX + PADDING_SIDE, currentY + (HEIGHT/2f - 3.5f), 7);

            int textAlpha = (int)(255 * animAlpha);
            Client.RENDERER.text(fullText.getString(), rowX + PADDING_SIDE + skinW + 2f, currentY + (HEIGHT/2f - TEXT_SIZE/2f) - 0.3f,
                    TextureUse.SFMEDIUM, TEXT_SIZE, new Color(255, 255, 255, textAlpha));

            if (type == StaffType.SPECTATE) {
                float indX = rowX + rowContentW + SPACING;
                drawStyle(indX, currentY, indicatorW, HEIGHT, animAlpha);

                float eyeW = Client.RENDERER.textWidth(IconUse.RENDER.glyph, TextureUse.ICONS, 6f);
                Client.RENDERER.text(IconUse.RENDER.glyph, indX + (indicatorW/2f - eyeW/2f - 1), currentY + (HEIGHT/2f - 3f) - 0.3f,
                        TextureUse.ICONS, 6f, new Color(255, 80, 80, textAlpha));
            }

            currentY += (HEIGHT + 1.0f);
        }

        this.drag.width = width = MathUtility.linearFps(width, maxRowWidth, 10);
        this.drag.height = height = MathUtility.linearFps(height, (currentY - y), 10);
    }

    private void renderHeader(float ry, boolean right, float alpha) {
        String title = "Staff online";
        float iconW = Client.RENDERER.textWidth(IconUse.STAFF.glyph, TextureUse.ICONS, ICON_SIZE);
        float textW = Client.RENDERER.textWidth(title, TextureUse.SFMEDIUM, TEXT_SIZE);
        float headerW = PADDING_SIDE + iconW + SPACING + 0.5f + SPACING + textW + PADDING_SIDE;

        float hx = right ? (x + width - headerW) : x;

        drawStyle(hx, ry, headerW, HEIGHT, alpha);

        float iconX = hx + PADDING_SIDE;
        float lineX = iconX + iconW + SPACING;
        float textX = lineX + 0.5f + SPACING;

        Client.RENDERER.text(IconUse.STAFF.glyph, iconX, ry + (HEIGHT / 2f) - (ICON_SIZE / 2f) - 0.3f, TextureUse.ICONS, ICON_SIZE - 1.3f, Color.WHITE);
        Client.RENDERER.rect(lineX, ry + 3f, 0.5f, HEIGHT - 6f, new Vector4f(0), 1, new Color(255, 255, 255, 50), new Color(255, 255, 255, 50), new Color(255, 255, 255, 50), new Color(255, 255, 255, 50));
        Client.RENDERER.text(title, textX, ry + (HEIGHT / 2f) - (TEXT_SIZE / 2f) - 0.3f, TextureUse.SFMEDIUM, TEXT_SIZE, Color.WHITE);
    }

    private void drawStyle(float rx, float ry, float rw, float rh, float alpha) {
        Vector4f round = new Vector4f(5f);
        Client.RENDERER.blur(rx, ry, rw, rh, round, 15f, alpha);

        if (style.get() == Style.DARK) {
            Color bgColor = new Color(20, 20, 25, (int) (180 * alpha));
            Client.RENDERER.rect(rx, ry, rw, rh, round, 1, bgColor, bgColor, bgColor, bgColor);

            Color outColor = new Color(255, 255, 255, (int) (15 * alpha));
            Client.RENDERER.outline(rx, ry, rw, rh, 0.4f, round, new Vector2f(1), outColor, outColor, outColor, outColor);
        } else {
            Color bgColor = new Color(62, 62, 71, 0);
            Client.RENDERER.rect(rx, ry, rw, rh, round, 1, bgColor, bgColor, bgColor, bgColor);

            int outAlpha = MathHelper.clamp((int) (25 * alpha), 0, 255);
            Color outColor = new Color(255, 255, 255, outAlpha);
            Client.RENDERER.outline(rx, ry, rw, rh, 0.4f, round, new Vector2f(1), outColor, outColor, outColor, outColor);
        }
    }

    private Text getPlayerNameWithPrefix(PlayerListEntry player) {
        MutableText prefix = (MutableText) player.getDisplayName();
        if (prefix == null) {
            if (player.getScoreboardTeam() != null) {
                prefix = player.getScoreboardTeam().getPrefix().copy().append(player.getProfile().name());
            } else {
                prefix = Text.literal(player.getProfile().name());
            }
        }
        return prefix;
    }

    EventBus<EventGameTick> gametick = event -> {
        if (mc.player != null && mc.player.age % 20 == 0) {
            staff.clear();
            if (mc.getNetworkHandler() == null) return;

            String[] staffKeywords = {
                    "admin", "moder", "helper", "staff", "owner",
                    "админ", "модер", "хелпер", "стажер", "куратор",
                    "ᴀᴅᴍɪɴ", "ʜᴇʟᴘᴇʀ","ᴍᴏᴅᴇʀ", "ᴏᴡɴᴇʀ"
            };

            for (PlayerListEntry player : mc.getNetworkHandler().getPlayerList()) {
                String name = player.getProfile().name();
                if (name.equals(mc.player.getGameProfile().name())) continue;

                Text display = player.getDisplayName();
                String displayStr = display != null ? display.getString() : "";
                String prefix = player.getScoreboardTeam() != null ? player.getScoreboardTeam().getPrefix().getString() : "";

                String fullTextClean = Formatting.strip(displayStr + " " + prefix + " " + name).toLowerCase();

                boolean isStaff = false;
                if (player.getGameMode() == GameMode.SPECTATOR) {
                    isStaff = true;
                } else {
                    for (String word : staffKeywords) {
                        if (fullTextClean.contains(word.toLowerCase())) {
                            isStaff = true;
                            break;
                        }
                    }
                }

                if (isStaff) {
                    staff.put(player, player.getGameMode() == GameMode.SPECTATOR ? StaffType.SPECTATE : StaffType.ACTIVE);
                }
            }
        }
    };

    @AllArgsConstructor
    enum Style implements EnumChoice {
        DARK("Темный"), GLASS("Стекло");
        @Getter final String renderName;
    }

    enum StaffType { ACTIVE, SPECTATE }
}