package gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick;

import net.minecraft.client.gui.DrawContext;
import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import gg.topchdlc.vse.utils.animations.Direction;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Create by daun kvass
 */
public class TotemNUrick extends HudElement {
    public static final TotemNUrick INSTANCE = new TotemNUrick(
            new Drag("TotemCounter", () -> true).bound(20, 120, 16, 14)
    );

    private TotemNUrick(Drag drag) {
        super("TotemCounter", drag);
    }
    private static final float SCALE = 0.7f;
    private static final float SCALE_T = 0.8f;
    private int totemCount = 0;

    private int countTotems() {
        if (mc.player == null) return 0;

        int count = 0;

        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                count += stack.getCount();
            }
        }

        ItemStack offhand = mc.player.getOffHandStack();
        if (offhand.getItem() == Items.TOTEM_OF_UNDYING) {
            count += offhand.getCount();
        }

        return count;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (mc.player == null) return;
        DrawContext context = Client.RENDERER.getDrawContext();
        if (context == null) return;
        totemCount = countTotems();
        boolean shown = (mc.currentScreen instanceof ChatScreen || totemCount > 0);
        animation.setDirection(shown ? Direction.FORWARDS : Direction.BACKWARDS);
        animation.setDuration(300);
        float alpha = animation.getOutput();
        if (alpha < 0.01f) return;
        float x = getX();
        float y = getY();
        Client.RENDERER.queueTask(() -> {
            var matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(x, y);
            matrices.scale(SCALE, SCALE);
            ItemStack totemStack = new ItemStack(Items.TOTEM_OF_UNDYING);
            context.drawItem(totemStack, 0, 0);
            String countText = String.valueOf(totemCount);
            matrices.translate(10, 10);
            matrices.scale(SCALE_T, SCALE_T);

            context.drawText(mc.textRenderer, countText, 0, 0, -1, true);

            matrices.popMatrix();
        });
    }

}