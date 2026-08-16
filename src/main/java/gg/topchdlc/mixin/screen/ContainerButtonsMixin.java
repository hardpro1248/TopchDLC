package gg.topchdlc.mixin.screen;

import gg.topchdlc.vse.utils.player.ContainerUtility;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class ContainerButtonsMixin extends Screen {

    @Unique private static final int BUTTON_HEIGHT = 14;
    @Unique private static final int BUTTON_GAP = 4;

    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;

    protected ContainerButtonsMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void topchdlc$addContainerButtons(CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        ScreenHandler handler = ((HandledScreen<?>) self).getScreenHandler();
        int buttonY = this.y - BUTTON_HEIGHT - BUTTON_GAP;

        if (self instanceof InventoryScreen) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Выбросить все"),
                            button -> ContainerUtility.dropInventory(handler))
                    .dimensions(this.x, buttonY, this.backgroundWidth, BUTTON_HEIGHT)
                    .build());
        } else if (self instanceof GenericContainerScreen || self instanceof ShulkerBoxScreen) {
            int half = (this.backgroundWidth - BUTTON_GAP) / 2;
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Сложить все"),
                            button -> ContainerUtility.depositAll(handler))
                    .dimensions(this.x, buttonY, half, BUTTON_HEIGHT)
                    .build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Забрать все"),
                            button -> ContainerUtility.takeAll(handler))
                    .dimensions(this.x + half + BUTTON_GAP, buttonY, half, BUTTON_HEIGHT)
                    .build());
        }
    }
}
