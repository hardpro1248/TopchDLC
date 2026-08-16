package gg.topchdlc.api.events.list;



import gg.topchdlc.api.events.Event;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
    @Setter
    public class EventInput extends Event {
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        static EventInput instance = new EventInput();

        private float strafe, forward;
        private boolean jump;
        private boolean sprint;
        private boolean sneak;

        public EventInput() {
        }

        public static EventInput build(float forward, float strafe, boolean jump, boolean shift, boolean sprint) {
            instance.setStrafe(strafe);
            instance.setForward(forward);
            instance.setJump(jump);
            instance.setSneak(shift);
            instance.setSprint(sprint);
            instance.reset();
            return instance;
        }
    }
