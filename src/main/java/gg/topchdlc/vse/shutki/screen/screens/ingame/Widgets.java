package gg.topchdlc.vse.shutki.screen.screens.ingame;

import gg.topchdlc.api.render.RendererObject;
import gg.topchdlc.api.ui.UIWidget;

import java.util.concurrent.CopyOnWriteArrayList;

public class Widgets extends RendererObject {
    private int lastId;
    private final CopyOnWriteArrayList<UIWidget> UIWidgets = new CopyOnWriteArrayList<>();

    public int genId() {
        return ++lastId;
    }

    public void register(UIWidget uiWidget) {
        if (!UIWidgets.contains(uiWidget)) {
            uiWidget.shouldRemove = false;
            uiWidget.opened();
            UIWidgets.add(uiWidget);
        }
    }
    public void unregister(UIWidget UIWidget) {
        UIWidgets.remove(UIWidget);
    }
    public CopyOnWriteArrayList<UIWidget> get() {
        return UIWidgets;
    }

    public boolean has(UIWidget UIWidget) {
        return UIWidgets.contains(UIWidget);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        for (UIWidget UIWidget : UIWidgets) {
            UIWidget.render(mouseX, mouseY);
        }
        UIWidgets.removeIf(UIWidget::isShouldRemove);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        for (UIWidget UIWidget : UIWidgets) {
            UIWidget.click(mouseX, mouseY, button);
        }
        return super.click(mouseX, mouseY, button);
    }

    @Override
    public void release(int button) {
        for (UIWidget UIWidget : UIWidgets) {
            UIWidget.release(button);
        }
        super.release(button);
    }
}
