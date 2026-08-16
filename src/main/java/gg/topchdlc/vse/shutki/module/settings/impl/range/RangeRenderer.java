package gg.topchdlc.vse.shutki.module.settings.impl.range;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.ui.UIStyle;
import gg.topchdlc.api.ui.parts.impl.UIRange;
import gg.topchdlc.api.ui.parts.impl.UITextField;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.vse.utils.client.client.ClientColors;

public class RangeRenderer extends SettingRenderer<RangeSetting> {
    private final UITextField textMin = new UITextField(),
            textMax = new UITextField();
    private final UIRange range;

    public RangeRenderer(RangeSetting setting) {
        super(setting);
        range = new UIRange(setting.minimumValue, setting.maximumValue, setting.increment, setting::setMin, setting::setMax, setting::get);
        textMin.setCallback(s -> {
            try {
                setting.setMin(Float.parseFloat(s.replace(",", ".")));
            } catch (NumberFormatException ignored) {}
        });
        textMin.setFinishCallback(s -> updateText(textMin, setting.getMin()));
        textMax.setCallback(s -> {
            try {
                setting.setMax(Float.parseFloat(s.replace(",", ".")));
            } catch (NumberFormatException ignored) {}
        });
        textMax.setFinishCallback(s -> updateText(textMax, setting.getMax()));
    }

    private void updateText(UITextField textField, float v) {
        if (setting.increment % 1.0f == 0f) {
            textField.setText(String.format("%.0f", v));
        } else {
            String s = String.valueOf(setting.increment);
            int dot = s.indexOf('.');
            int decimals = s.length () - 1 - dot;
            textField.setText(String.format("%." + decimals + "f", v));
        }

        textField.style = UIStyle.TRANSPARENT;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        height = Math.max(12, 12 + drawDesc(12, 8, 40));
        Client.RENDERER.text(setting.getName(), x + 2, y + 2, TextureUse.SFMEDIUM, 8, ClientColors.FORE_COLOR);

        float textMinWidth = Math.max(Client.RENDERER.textWidth(textMin.getText(), TextureUse.SFMEDIUM, 8), 12);
        float textMaxWidth = Math.max(Client.RENDERER.textWidth(textMax.getText(), TextureUse.SFMEDIUM, 8), 12);
        float textStart = width - textMaxWidth - 26 - textMinWidth - 12;
        textMin.bound(x + textStart, y + 2, Math.max(26, textMinWidth + 12), 15).render(mouseX, mouseY);
        Client.RENDERER.text("—", x + textStart + textMinWidth + 14, y + 4, TextureUse.SFMEDIUM, 8, ClientColors.FORE_COLOR);
        textMax.bound(x + textStart + textMaxWidth + 26, y + 2, Math.max(26, textMaxWidth + 12), 15).render(mouseX, mouseY);
        range.bound(x + 3, y + 10, width - 10, 15).render(mouseX, mouseY);

        if (!textMin.writing) {
            updateText(textMin, setting.getMin());
        }
        if (!textMax.writing) {
            updateText(textMax, setting.getMax());
        }
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        return range.click(mouseX, mouseY, button) || textMin.click(mouseX, mouseY, button) || textMax.click(mouseX, mouseY, button);
    }

    @Override
    public void chartyped(char ch, int keyCode) {
        textMin.chartyped(ch, keyCode);
        textMax.chartyped(ch, keyCode);
        super.chartyped(ch, keyCode);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        textMin.keyPressed(keyCode, scanCode, modifiers);
        textMax.keyPressed(keyCode, scanCode, modifiers);
        super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void release(int button) {
        range.release(button);
        textMin.release(button);
        textMax.release(button);
    }
}
