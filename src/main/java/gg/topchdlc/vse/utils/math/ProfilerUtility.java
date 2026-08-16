package gg.topchdlc.vse.utils.math;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ProfilerUtility {
    final static Map<String, Long> times = new HashMap<>();
    final static Map<String, Long> results = new HashMap<>();

    public static void push(String name) {
        times.put(name, System.nanoTime());
    }
    public static void pop(String name) {
        results.put(name, (System.nanoTime() - times.get(name)) / 1_000_000);
    }

    public static long get(String name) {
        return results.getOrDefault(name, -1L);
    }
    public static Set<Map.Entry<String, Long>> getAll() {
        return results.entrySet();
    }
}
