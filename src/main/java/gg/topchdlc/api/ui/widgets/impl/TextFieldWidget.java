package gg.topchdlc.api.ui.widgets.impl;

import gg.topchdlc.api.ui.UIWidget;
import gg.topchdlc.api.ui.parts.impl.UITextField;

public class TextFieldWidget extends UIWidget {

    private final UITextField textField;

    public TextFieldWidget(UITextField textField) {
        this.textField = textField;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        textField.bound(x, y, width, height).render(mouseX, mouseY);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        textField.keyPressed(keyCode, scanCode, modifiers);
        super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void chartyped(char ch, int keyCode) {
        textField.chartyped(ch, keyCode);
        super.chartyped(ch, keyCode);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        return textField.click(mouseX, mouseY, button);
    }

    @Override
    public void release(int button) {
        textField.release(button);
        super.release(button);
    }
}
