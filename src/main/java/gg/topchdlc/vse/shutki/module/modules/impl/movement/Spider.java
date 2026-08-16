package gg.topchdlc.vse.shutki.module.modules.impl.movement;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.spider.SpiderGrimGround;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.spider.SpiderMotion;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.ChoiceSetting;

public class Spider extends Module {
    public static final Spider INSTANCE = new Spider();
    private Spider() {
        super("Spider", Category.MOVEMENT, "Позволяет преодолять вертикальные препятствия");
    }

    private final ChoiceSetting<Choice> mode = choiceSetting("Mode", 0, new SpiderGrimGround(),new SpiderMotion());

    EventBus<Event> events = event -> {
        mode.onEvent(event);
    };

    @Override
    protected void onDisable() {
        Client.TIMER = 1f;
    }
}
