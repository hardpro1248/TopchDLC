package gg.topchdlc.vse.shutki.module.settings.impl.slider;

import gg.topchdlc.api.ui.UIStyle;
import gg.topchdlc.api.ui.parts.impl.UISlider;
import gg.topchdlc.api.ui.parts.impl.UITextField;
import gg.topchdlc.Client;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.utils.client.client.ClientColors;

import java.util.Locale;

/**
 * Create by daun kvass
 */
public class SliderRenderer extends SettingRenderer<SliderSetting> {
    private final UITextField textField = new UITextField();
    private final UISlider slider;

    private void updateText() {
        if (setting.getIncrement() % 1.0f == 0f) {
            textField.setText(String.format(Locale.US, "%.0f", setting.get()));
        } else {
            String s = String.valueOf(setting.getIncrement());
            int dot = s.indexOf('.');
            int decimals = s.length() - 1 - dot;
            textField.setText(String.format(Locale.US, "%." + decimals + "f", setting.get()));
        }

        textField.style = UIStyle.TRANSPARENT;
    }

    public SliderRenderer(SliderSetting setting) {
        super(setting);

        slider = new UISlider(setting::get, setting.getMin(), setting.getMax(), setting::getIncrement, (value) -> {
            setting.setValue(value);
            updateText();
        });

        textField.setCallback((s) -> {
            try {
                setting.setValue(Float.parseFloat(s.replace(",", ".")));
            } catch (NumberFormatException ignored) {
            }
        });

        textField.setFinishCallback((s) -> {
            updateText();
        });
    }

    @Override
    public void render(int mouseX, int mouseY) {
        float leftPadding = 2f;
        float rightPadding = 4f;
        float rowHeight = 14f;

        float startX = x + leftPadding;
        float endX = x + width - rightPadding;
        float centerY = y + (rowHeight / 2F);

        String text = textField.getText();
        float textFontSize = 6.0f;
        float textWidth = Client.RENDERER.textWidth(text, TextureUse.SFMEDIUM, textFontSize);

        float fieldWidth = Math.max(26f, textWidth + 16f);
        float fieldX = endX - fieldWidth;
        float fieldY = centerY - 6f;

        textField.bound(fieldX, fieldY, fieldWidth, 12).render(mouseX, mouseY);

        float nameFontSize = 6.5f;
        String name = setting.getName();
        float nameWidth = Client.RENDERER.textWidth(name, TextureUse.SFMEDIUM, nameFontSize);
        float nameY = centerY - 3.0f;
        Client.RENDERER.text(name, startX, nameY, TextureUse.SFMEDIUM, nameFontSize, ClientColors.FORE_COLOR);

        float gapLeft = 6f;
        float gapRight = 2f;

        float sliderX = startX + nameWidth + gapLeft;
        float sliderWidth = fieldX - gapRight - sliderX;
        float sliderHeight = 10f;
        float sliderY = centerY - (sliderHeight / 2F);

        if (sliderWidth > 10) {
            slider.bound(sliderX, sliderY, sliderWidth, sliderHeight).render(mouseX, mouseY);
        }

        this.height = (int) rowHeight;

        if (!textField.writing) {
            updateText();
        }
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        return slider.click(mouseX, mouseY, button) || textField.click(mouseX, mouseY, button);
    }

    @Override
    public void chartyped(char ch, int keyCode) {
        textField.chartyped(ch, keyCode);
        super.chartyped(ch, keyCode);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        textField.keyPressed(keyCode, scanCode, modifiers);
        super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void release(int button) {
        slider.release(button);
        textField.release(button);
    }
}