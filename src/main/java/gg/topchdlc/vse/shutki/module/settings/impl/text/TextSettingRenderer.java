package gg.topchdlc.vse.shutki.module.settings.impl.text;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.ui.parts.impl.UITextField;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import lombok.experimental.NonFinal;
import net.minecraft.client.MinecraftClient;

public class TextSettingRenderer extends SettingRenderer<TextSetting> {

    public static boolean hovered = false;
    private boolean hoveredDesc = false;
    private boolean sas = false;
    int MAX_SYMBOLS = 100;

    private int cursor = -1;
    private int selectionEnd = -1;
    private boolean dragging = false;
    private int cursorPosition = 0;
    @NonFinal private float scrollOffset = 0;
    @NonFinal private long lastClickTime = 0;
    @NonFinal private long lastInputTime = System.currentTimeMillis();


    private UITextField textFieldObject = new UITextField();

    private boolean loaded = false;

    public TextSettingRenderer(TextSetting setting) {
        super(setting);

    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (!loaded) {
            textFieldObject.setText(setting.getText());
            textFieldObject.setDesc(setting.getDesc());
            textFieldObject.setCallback(setting::setText);
            textFieldObject.setHideMask(setting.getHideMask());
            loaded = true;
        }

        height = Math.max(20, 20 + drawDesc(Client.RENDERER.textLined(this.setting.getName(), 20, x + 2, y + 4, TextureUse.SFMEDIUM, 8, ClientColors.FORE_COLOR)));

        textFieldObject.bound(x + width / 2F, y + height / 2F - 6.5F, width / 2F, 15)
                .render(mouseX, mouseY);

        if (!textFieldObject.isWriting()) {
            textFieldObject.setText(setting.getText());
        }
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        return textFieldObject.click(mouseX, mouseY, button);
    }

    @Override
    public void release(int button) {
        textFieldObject.release(button);
        super.release(button);
    }


    @Override
    public void chartyped(char ch, int keyCode) {
        textFieldObject.chartyped(ch, keyCode);
        super.chartyped(ch, keyCode);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        textFieldObject.keyPressed(keyCode, scanCode, modifiers);
        super.keyPressed(keyCode, scanCode, modifiers);
    }
}