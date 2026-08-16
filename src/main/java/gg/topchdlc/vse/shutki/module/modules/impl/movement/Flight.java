package gg.topchdlc.vse.shutki.module.modules.impl.movement;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.flight.GrimGlideFly;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.flight.JumpFly;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.flight.MotionFly;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.ChoiceSetting;
import gg.topchdlc.vse.utils.math.TimeUtility;
import gg.topchdlc.vse.utils.network.BlinkUtility;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

public class Flight extends Module {
    public static final Flight INSTANCE = new Flight();
    private Flight() {
        super("Flight", Category.MOVEMENT, "Позволяет летать");
    }

    public ChoiceSetting<Choice> mode = choiceSetting("Mode", 0,  new GrimGlideFly(),new MotionFly(),new JumpFly());
    int ticks = 0;
    long handle = 0;
    final TimeUtility time = new TimeUtility();
    final BlinkUtility blink = new BlinkUtility();
    boolean shouldSpoofPing = false;
    Vec3d savedPos = Vec3d.ZERO;
    ArrayList<BlockPos> sentPlaces = new ArrayList<>();
    @Override
    protected void onEnable() {
        super.onEnable();
        ticks = 0;
        shouldSpoofPing = false;
        handle = 0;
        savedPos = mc.player.getEntityPos();
        blink.clear();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        Client.TIMER = 1;
    }

    EventBus<Event> events = event -> {
        mode.onEvent(event);
    };
}
