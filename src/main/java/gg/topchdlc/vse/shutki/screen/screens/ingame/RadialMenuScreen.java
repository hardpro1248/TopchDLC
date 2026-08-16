package gg.topchdlc.vse.shutki.screen.screens.ingame;

import gg.topchdlc.Client;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public class RadialMenuScreen extends Screen {
    private final RadialMenuWidget widget;

    public RadialMenuScreen(RadialMenuWidget widget) {
        super(Text.empty());
        this.widget = widget;
    }

    @Override
    protected void init() {
        super.init();
        widget.opened();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Client.RENDERER.setDrawContext(context);
        widget.render(mouseX, mouseY);
        Client.RENDERER.runTasks();
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean inside) {
        if (widget.click((int) click.x(), (int) click.y(), click.button())) {
            return true;
        }
        return super.mouseClicked(click, inside);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}