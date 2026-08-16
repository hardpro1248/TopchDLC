package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import net.minecraft.entity.LivingEntity;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventAttack;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Create by daun kvass
 */
public class HitAnimation extends Module {
    public static final HitAnimation INSTANCE = new HitAnimation();


    public final SliderSetting duration     = sliderSetting("Длительность ", 250, 50, 600).increment(10f);
    public final SliderSetting squishAmount = sliderSetting("Сила сжатия", 0.35f, 0.05f, 0.7f).increment(0.05f);
    public static final Map<Integer, Long> HIT_TIMES = new ConcurrentHashMap<>();

    private HitAnimation() {super("HitAnimation", Category.RENDER,"xx");}



    public final EventBus<Event> bus = event -> {

        if (!(event instanceof EventAttack attack)) return;
        if (!(attack.target instanceof LivingEntity living)) return;
        HIT_TIMES.put(living.getId(), System.currentTimeMillis());
    };

    public float[] getSquish(int entityId) {
        Long hitTime = HIT_TIMES.get(entityId);
        if (hitTime == null) return null;
        long elapsed = System.currentTimeMillis() - hitTime;
        float dur = duration.get();
        if (elapsed >= dur) { HIT_TIMES.remove(entityId); return null; }
        float t = elapsed / dur;
        float squish = squishAmount.get() * (float) Math.sin(t * Math.PI);
        return new float[]{ 1f + squish * 0.5f, 1f - squish };
    }
}
