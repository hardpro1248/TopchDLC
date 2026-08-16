package gg.topchdlc.vse.shutki.screen.hud.ui;

import com.google.gson.JsonObject;
import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.RendererObject;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.api.ui.widgets.HudSettingWidget;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.animations.impl.SmoothStepAnimation;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.math.Rectangle;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.screen.ChatScreen;

import java.util.List;

public abstract class HudElement extends RendererObject {
    protected final Drag drag;
    protected Rectangle origin;
    @Getter @Setter
    protected boolean enabled = true;

    @Getter
    private final String name;
    protected final Group settings;

    private float f_animation = 0;
    public boolean expanded = false;

    public SmoothStepAnimation expandAnim = new SmoothStepAnimation(300, 1);

    @Getter
    protected SmoothStepAnimation animation = new SmoothStepAnimation(300, 1);


    private final HudSettingWidget widget;

    public HudElement(String name, Drag drag) {
        this.name = name;
        this.settings = new Group(name);
        this.widget = new HudSettingWidget(this);
        this.drag = drag;
        if (drag != null) {
            this.origin = new Rectangle(drag.x, drag.y, drag.width, drag.height);
        }
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (hover(mouseX, mouseY)) {
            switch (button) {
                case 0 -> {
                    if (drag.canDrag.get()) {

                        drag.dX = (float) (mouseX - drag.x);
                        drag.dY = (float) (mouseY - drag.y);

                        drag.dragging = true;
                    }
                }
                case 1 -> {
                    if (settings.isEmpty()) break;
                    expanded = true;
                    this.widget.bound(x + width / 2f, y + height / 2f, 118, 140);
                }
            }
        }
        return super.click(mouseX, mouseY, button);
    }

    @Override
    public void release(int button) {
        if (drag != null)
            drag.dragging = false;
        super.release(button);
    }

    public void _render(int mouseX, int mouseY) {
        if (drag != null) {
            if (drag.dragging) {
                drag.x = mouseX - drag.dX;
                drag.y = mouseY - drag.dY;
                if (mc.getWindow() != null) {
                    float screenWidth = mc.getWindow().getScaledWidth();
                    float screenHeight = mc.getWindow().getScaledHeight();
                    drag.x = Math.max(0, Math.min(drag.x, screenWidth - drag.width));
                    drag.y = Math.max(0, Math.min(drag.y, screenHeight - drag.height));
                }
            }
            this.x = drag.x;
            this.y = drag.y;
            this.width = drag.width;
            this.height = drag.height;
        }

        CRenderSystem system = Client.RENDERER.getCrenderSystem();
        {
            f_animation = MathUtility.linearFps(f_animation, hover(mouseX, mouseY) && !settings.isEmpty() && !expanded && !drag.dragging && mc.currentScreen instanceof ChatScreen ? 1 : 0, 10);
            float alpha = system.alpha();
            system.alpha(alpha * f_animation);
            Client.RENDERER.textCentered("right click - open settings", this.x + this.width / 2f, this.y - 10, TextureUse.SFMEDIUM, 6, ClientColors.FORE_COLOR);
            system.alpha(alpha);
        }

        if (!settings.isEmpty() && 1.f - expandAnim.getOutput() > 0.1) {
            Client.HUD.registerOverlay(widget);
        } else {
            Client.HUD.unregisterOverlay(widget);
        }

        expandAnim.setDirection(expanded && mc.currentScreen instanceof ChatScreen ? Direction.BACKWARDS : Direction.FORWARDS);
    }

    public List<SettingRenderer<?>> getSettingRenderers() {
        return this.settings.getSettingRenderers();
    }

    public void save(JsonObject json) {
        json.addProperty("_enabled", enabled);
        settings.save(json);
        if (drag != null) {
            JsonObject drag = new JsonObject();
            json.add("_drag", drag);
            this.drag.save(drag);
        }
    }

    public void load(JsonObject json) {
        if (json.has("_enabled")) {
            enabled = json.get("_enabled").getAsBoolean();
        }
        settings.load(json);

        if (drag != null) {
            if (json.has("_drag")) {
                JsonObject drag = json.getAsJsonObject("_drag");
                this.drag.load(drag);
            }
        }
    }

}
