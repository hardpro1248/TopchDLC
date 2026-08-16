package gg.topchdlc.api.party;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import gg.topchdlc.vse.shutki.other.commands.impl.GpsCommand;
import gg.topchdlc.vse.utils.client.client.ClientColors;

import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;

import java.util.List;

import static gg.topchdlc.MinecraftHolder.mc;

public class PartyGpsScreen extends Screen {

    private final List<PartyPlayerPos> members;
    private int hovered = -1;

    private static final int ITEM_H = 22;
    private static final int PADDING = 10;

    public PartyGpsScreen(List<PartyPlayerPos> members) {
        super(Text.empty());
        this.members = members;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int w = 160;
        int itemCount = members.size();
        int totalH = PADDING * 2 + itemCount * ITEM_H + 20;
        int x = (mc.getWindow().getScaledWidth() - w) / 2;
        int y = (mc.getWindow().getScaledHeight() - totalH) / 2;
        ctx.fill(x, y, x + w, y + totalH, 0xCC111111);
        ctx.fill(x, y, x + w, y + 1, ClientColors.FORE_COLOR.getRGB() | 0xFF000000);
        ctx.drawCenteredTextWithShadow(mc.textRenderer, "Party GPS", x + w / 2, y + PADDING, 0xFFFFFFFF);

        hovered = -1;
        for (int i = 0; i < itemCount; i++) {
            int iy = y + PADDING + 18 + i * ITEM_H;
            boolean over = mouseX >= x + PADDING && mouseX <= x + w - PADDING
                    && mouseY >= iy && mouseY <= iy + ITEM_H - 2;
            if (over) hovered = i;

            int bg = over ? 0xAA334455 : 0x55222222;
            ctx.fill(x + PADDING, iy, x + w - PADDING, iy + ITEM_H - 2, bg);

            PartyPlayerPos p = members.get(i);
            String dist = String.format("%.0fm", Math.sqrt(
                    Math.pow(p.x() - mc.player.getX(), 2) +
                            Math.pow(p.z() - mc.player.getZ(), 2)
            ));
            ctx.drawTextWithShadow(mc.textRenderer, p.playerId(), x + PADDING + 4, iy + 6, 0xFFFFFFFF);
            ctx.drawTextWithShadow(mc.textRenderer, dist, x + w - PADDING - mc.textRenderer.getWidth(dist) - 4, iy + 6, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (hovered >= 0 && hovered < members.size()) {
            PartyPlayerPos p = members.get(hovered);
            GpsCommand.INSTANCE.setTarget(p.x(), p.z());
            this.close();
            return true;
        }
        this.close();
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (keyInput.key() == 256) {
            this.close();
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean shouldPause() { return false; }
}