package gg.topchdlc.vse.utils.lang;

import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import lombok.experimental.UtilityClass;
import net.minecraft.util.Language;

@UtilityClass
public class LangUtility {
    public String get(String key) {
        return Language.getInstance().get(key);
    }

    public String get(String key, String fallback) {
        return Language.getInstance().get(key, fallback);
    }

    public String getEnumChoiceName(Enum<?> value) {
        if (value == null) return "";
        if (value instanceof EnumChoice enumChoice) {
            return get("topchdlc.enum." + enumChoice.getLangClassName() + "." + value.name(), enumChoice.getRenderName());
        }
        String toStringName = value.toString();
        if (toStringName != null && !toStringName.equalsIgnoreCase(value.name())) {
            return toStringName;
        }
        return value.name();
    }
}