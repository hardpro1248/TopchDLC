package gg.topchdlc.vse.utils.math;

import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.shutki.module.modules.Module;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

public class RequestHandler<T> implements MinecraftHolder {
    private int tick = 0;
    private final Queue<Request<T>> requests = new PriorityBlockingQueue<>(11, Comparator.comparing(req -> -req.priority));

    public void tick() {
        tick(1);
    }

    public void tick(int delta) {
        tick += delta;
    }

    public void request(Request<T> request) {
        requests.removeIf(it -> it.provider == request.provider);
        request.ticks += tick;
        requests.add(request);
    }

    public void request(T value, int ticks, int priority, Module provider) {
        request(new Request<>(ticks, priority, provider, value));
    }

    @Nullable
    public T get() {
        Request<T> top = requests.peek();
        if (top == null) return null;
        if (mc.isOnThread()) {
            while (top.ticks <= tick || !top.provider.isEnabled()) {
                requests.remove();
                top = requests.peek();
                if (top == null) return null;
            }
        }
        return top.value;
    }

    @AllArgsConstructor @Getter @Data
    public static final class Request<T> {
        private int ticks;
        private final int priority;
        private final Module provider;
        private final T value;
    }
}
