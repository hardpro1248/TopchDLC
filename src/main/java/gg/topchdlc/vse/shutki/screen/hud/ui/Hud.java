package gg.topchdlc.vse.shutki.screen.hud.ui;

import com.google.gson.JsonObject;
import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventKey;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.api.ui.UIWidget;
import gg.topchdlc.vse.shutki.module.modules.impl.render.Interface;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

@Getter
public class Hud implements MinecraftHolder {
    public Hud() {
    }

    @Getter
    private final ArrayList<HudElement> elements = new ArrayList<>();
    ArrayList<UIWidget> overlays = new ArrayList<>();

    public void register(HudElement... elements) {
        this.elements.addAll(Arrays.asList(elements));
    }

    public void registerOverlay(UIWidget r) {
        if (!overlays.contains(r)) overlays.add(r);
    }

    public void unregisterOverlay(UIWidget w) {
        overlays.remove(w);
    }
    public HudElement getElement(String name) {
        for (HudElement element : elements) {
            if (element.getName().equalsIgnoreCase(name)) {
                return element;
            }
        }
        return null;
    }

    public void render(int mouseX, int mouseY) {
        Client.RENDERER.getCrenderSystem().layerTex(Client.RENDERER.getCrenderSystem().getAtlas().getGlId()).layer(CRenderSystem.RenderLayer.HUD);

        Interface.HudStyle activeStyle = Interface.INSTANCE.hudStyle.get();

        for (HudElement element : elements) {
            if (!element.isEnabled()) continue;
            Interface.HudElements hudEl = Interface.findByElement(element);
            if (hudEl != null && hudEl.getStyle() != activeStyle) continue;
            element.render(mouseX, mouseY);
            element._render(mouseX, mouseY);
        }

        for (UIWidget w : overlays) {
            w.render(mouseX, mouseY);
        }
        overlays.removeIf(UIWidget::isShouldRemove);

        Client.DRAGS.update(mouseX, mouseY);
    }

    public boolean click(int mouseX, int mouseY, int button) {
        if (!Interface.INSTANCE.isEnabled()) return false;

        for (UIWidget w : overlays.reversed()) {
            if (w.click(mouseX, mouseY, button)) return true;
        }

        Interface.HudStyle activeStyle = Interface.INSTANCE.isEnabled()
                ? Interface.INSTANCE.hudStyle.get() : null;

        for (HudElement element : elements) {
            if (activeStyle != null) {
                Interface.HudElements hudEl = Interface.findByElement(element);
                if (hudEl != null && hudEl.getStyle() != activeStyle) continue;
            }
            if (element.click(mouseX, mouseY, button)) return true;
        }

        Client.DRAGS.click(mouseX, mouseY, button);
        return false;
    }

    public void release(int button) {
        if (!Interface.INSTANCE.isEnabled()) return;
        Iterator<UIWidget> iterator = overlays.reversed().iterator();
        while (iterator.hasNext()) {
            UIWidget w = iterator.next();
            w.release(button);
        }
        for (HudElement element : elements) {
            element.release(button);
        }

        Client.DRAGS.release(0, 0);
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!Interface.INSTANCE.isEnabled()) return;

        for (UIWidget w : overlays.reversed()) {
            w.keyPressed(keyCode, scanCode, modifiers);
        }

        for (HudElement element : elements) {
            element.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    EventBus<EventKey> keyEvent = key -> {
        if (key.action == 1) {

        }
    };

    public void charTyped(char ch, int modifiers) {
        if (!Interface.INSTANCE.isEnabled()) return;

        for (UIWidget w : overlays.reversed()) {
            w.chartyped(ch, modifiers);
        }

        for (HudElement element : elements) {
            element.chartyped(ch, modifiers);
        }
    }

    public void save(JsonObject gson) {
        JsonObject json = new JsonObject();
        gson.add("HudSettings", json);
        for (HudElement element : elements) {
            JsonObject j = new JsonObject();
            json.add(element.getName(), j);
            element.save(j);
        }
    }

    public void load(JsonObject gson) {
        if (gson.has("HudSettings")) {
            JsonObject json = gson.getAsJsonObject("HudSettings");
            for (HudElement element : elements) {
                if (json.has(element.getName())) {
                    JsonObject j = json.getAsJsonObject(element.getName());
                    element.load(j);
                }
            }
        }
    }
}
