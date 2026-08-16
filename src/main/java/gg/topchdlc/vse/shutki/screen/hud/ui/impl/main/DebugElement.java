package gg.topchdlc.vse.shutki.screen.hud.ui.impl.main;

import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.client.get.DebugUtility;

public class DebugElement extends HudElement {
    public DebugElement(Drag drag) {
        super("Debug", drag);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        float oy = 0;
        String[] trim = new String[DebugUtility.traces.size()];
        int a = 0;
        long now = System.currentTimeMillis();
        for (var entry : DebugUtility.traces.entrySet()) {
            oy += draw(entry.getKey(), entry.getValue(), oy);
            if (now - entry.getValue().timestamp() > 5000) {
                trim[a++] = entry.getKey();
            }
        }
        for (String s : trim) {
            if (s == null) break;
            DebugUtility.traces.remove(s);
        }
    }

    float draw(String name, DebugUtility.Trace trace, float oy) {
        float xo = 0;
        Client.RENDERER.text(Long.toString(trace.timestamp()), x, y + oy + 2, TextureUse.SFMEDIUM, 7, ClientColors.DARK_GRAY_COLOR);
        xo += Client.RENDERER.textWidth(Long.toString(trace.timestamp()), TextureUse.SFMEDIUM, 7) + 2;
        Client.RENDERER.text(name, x + xo, y + oy, TextureUse.SFMEDIUM, 9, ClientColors.GREEN);
        xo += Client.RENDERER.textWidth(name, TextureUse.SFMEDIUM, 9) + 2;
        Client.RENDERER.text(trace.content(), x + xo, y + oy + 1, TextureUse.SFMEDIUM, 8, ClientColors.FORE_COLOR);
        return Client.RENDERER.textHeight(TextureUse.SFMEDIUM, 9) + 1;
    }
}
