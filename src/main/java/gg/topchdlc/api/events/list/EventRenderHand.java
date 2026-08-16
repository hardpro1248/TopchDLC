package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Create by daun kvass
 */
public class EventRenderHand extends Event {
    public enum Phase { PRE, POST }

    public final Phase phase;
    public final MatrixStack matrices;

    public EventRenderHand(Phase phase, MatrixStack matrices) {
        this.phase = phase;
        this.matrices = matrices;
    }
}