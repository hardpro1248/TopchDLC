package gg.topchdlc.vse.utils.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Create by daun kvass
 */
public class TextureExtractor {
    private static Field renderSetupField;
    private static Field texturesField;

    public static Identifier getTexture(RenderLayer layer) {
        if (layer == null) return null;
        try {
            if (renderSetupField == null) {
                for (Field f : RenderLayer.class.getDeclaredFields()) {
                    if (f.getType() == RenderSetup.class) {
                        f.setAccessible(true);
                        renderSetupField = f;
                        break;
                    }
                }
            }
            if (renderSetupField != null) {
                RenderSetup setup = (RenderSetup) renderSetupField.get(layer);
                if (setup != null) {
                    if (texturesField == null) {
                        for (Field f : RenderSetup.class.getDeclaredFields()) {
                            if (Map.class.isAssignableFrom(f.getType())) {
                                f.setAccessible(true);
                                texturesField = f;
                                break;
                            }
                        }
                    }
                    if (texturesField != null) {
                        Map<?, ?> texturesMap = (Map<?, ?>) texturesField.get(setup);
                        if (texturesMap != null && texturesMap.containsKey("Sampler0")) {
                            Object spec = texturesMap.get("Sampler0");
                            if (spec != null) {
                                for (java.lang.reflect.Method m : spec.getClass().getDeclaredMethods()) {
                                    if (m.getReturnType() == Identifier.class && m.getParameterCount() == 0) {
                                        m.setAccessible(true);
                                        return (Identifier) m.invoke(spec);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
    }
}