package gg.topchdlc.vse.utils.client.text;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;

public class TextAnimation {
    public TextAnimation() {
        Client.EVENTS.register(this);
    }

    String[] texts;

    StringBuilder builder = new StringBuilder();
    int delay = 1, delayTick = 0;
    int interval = 5, intervalTick = 0;
    int index = 0, charindex = 0;

    public TextAnimation delay(int delay) {
        this.delay = delay;
        return this;
    }
    public TextAnimation interval(int interval) {
        this.interval = interval;
        return this;
    }
    public TextAnimation texts(String... texts) {
        this.texts = texts;
        return this;
    }
    public String current() {
        return texts[Math.min(texts.length - 1, index)];
    }
    public TextAnimation reset() {
        index = 0;
        charindex = 0;
        builder.setLength(0);
        return this;
    }
    public boolean done() {
        return index >= texts.length;
    }

    public String get() {
        return builder.toString();
    }

    EventBus<EventGameTick> event = e -> {
          if (intervalTick > 0) {
              intervalTick--;
              return;
          }
          if (delayTick > 0) {
              delayTick--;
              return;
          }

        if (index >= texts.length) {
            return;
        }

          if (delayTick == 0 && !texts[index].isEmpty() && charindex < texts[index].length()) {
              delayTick = delay;
              builder.append(texts[index].charAt(charindex));
              charindex++;
          }
          if (charindex >= texts[index].length()) {
              charindex = 0;
              index++;
              intervalTick = interval;
          }
    };
}
