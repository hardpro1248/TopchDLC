package gg.topchdlc.vse.shutki.module.settings.impl.multitext;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.ui.parts.impl.UITextField;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.math.Rectangle;

public class MultiTextRenderer extends SettingRenderer<MultiTextSetting> {
    public MultiTextRenderer(MultiTextSetting setting) {
        super(setting);
    }

    Rectangle addRect = new Rectangle();
    UITextField[] fields = new UITextField[0];

    @Override
    public void render(int mouseX, int mouseY) {
        float nameheight = Client.RENDERER.textHeight(TextureUse.SFMEDIUM, 8) + Client.RENDERER.textLined(this.setting.getName(), 25, x + 2, y, TextureUse.SFMEDIUM, 8, ClientColors.FORE_COLOR);
        addRect.bound(x, y + nameheight + 2, width, 15);
        Client.RENDERER.drawModuleRect(addRect);
        Client.RENDERER.text(IconUse.ADD.glyph, x + width / 2f - 3, y + addRect.getHeight() / 2f + nameheight - 3, TextureUse.ICONS, 9, ClientColors.ICON_FOREGROUND_COLOR);
        float offsetY = nameheight + 2 + addRect.getHeight() + 2;
        for (var field : fields) {
            field.bound(x, y + offsetY, width - 14, 15).render(mouseX, mouseY);
            Client.RENDERER.text(IconUse.CROSS.glyph, x + width - 13, y + offsetY, TextureUse.ICONS, 13, ClientColors.ICON_FOREGROUND_COLOR);
            offsetY += 15 + 2;
        }
        height = offsetY;
        var strings = setting.get();
        if (fields.length != strings.size()) {
            fields = new UITextField[strings.size()];
            for (int i = 0; i < fields.length; i++) {
                var field = fields[i] = new UITextField();
                field.setText(strings.get(i));

                final int move_i = i;
                field.setCallback(n -> strings.set(move_i, n));
            }
        }
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (addRect.hovered(mouseX, mouseY)) {
            setting.add();
            return true;
        }
        for (int i = 0; i < fields.length; i++) {
            var field = fields[i];
            if (MathUtility.mouseIn(x + width - 13, field.getY() + 4, 13, 15, mouseX, mouseY)) {
                setting.remove(i);
                return true;
            }
            if (field.click(mouseX, mouseY, button)) return true;
        }
        return super.click(mouseX, mouseY, button);
    }

    @Override
    public void release(int button) {
        for (var field : fields) field.release(button);
        super.release(button);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (var field : fields) field.keyPressed(keyCode, scanCode, modifiers);
        super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void chartyped(char ch, int keyCode) {
        for (var field : fields) field.chartyped(ch, keyCode);
        super.chartyped(ch, keyCode);
    }
}
