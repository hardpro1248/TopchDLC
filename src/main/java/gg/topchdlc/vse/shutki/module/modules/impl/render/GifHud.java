package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event2D;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.gif.GifRenderer;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;

public class GifHud extends Module {
    public static final GifHud INSTANCE = new GifHud();

    private final SliderSetting sizeW = sliderSetting("Ширина", 150, 10, 1000).increment(1);
    private final SliderSetting sizeH = sliderSetting("Высота", 150, 10, 1000).increment(1);
    private final SliderSetting rounding = sliderSetting("Закругление", 8, 0, 50).increment(1);

    public final Drag drag = new Drag("GifHud", this::isEnabled);

    private GifRenderer.GifData gifData;

    private GifHud() {
        super("GifHud", Category.RENDER, "x");

        drag.x = 100;
        drag.y = 100;
    }

    @Override
    public void onEnable() {
        if (gifData == null) {
            gifData = GifRenderer.loadGif("images/ui/title/anime.gif");
        }
        super.onEnable();
    }

    EventBus<Event> bus = event -> {
        if (event instanceof Event2D) {
            onRender2D();
        }
    };

    private void onRender2D() {
        if (gifData == null) return;
        drag.bound(drag.x, drag.y, sizeW.get(), sizeH.get());
        MatrixStack matrices = Client.RENDERER.getStack();
        GifRenderer.renderGif(
                matrices,
                gifData,
                drag.x,
                drag.y,
                sizeW.get(),
                sizeH.get(),
                rounding.get(),
                Color.WHITE.getRGB()
        );
    }
}