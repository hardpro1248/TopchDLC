package gg.topchdlc.vse.utils.mapper;

import gg.topchdlc.vse.utils.other.LogUtility;
import net.fabricmc.mappings.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import net.fabricmc.mappings.model.V2MappingsProvider;

public class FabricMapper {
    private static final Mappings mappings = loadMappings();

    private static Mappings loadMappings() {
        try {
            Class.forName("net.minecraft.client.MinecraftClient");
            LogUtility.debug("mappings is on debug mode");
            return null;
        } catch (Exception exception) {}
        try (InputStream is = FabricMapper.class.getResourceAsStream("/assets/topchdlc/mappings.tiny");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return V2MappingsProvider.readTinyMappings(reader);
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String remapFieldName(String owner, String intermediaryName) {
        if (mappings == null) {
            return intermediaryName;
        }
        owner = toSlash(owner);
        List<FieldEntry> fields = mappings.getFieldEntries().stream().toList();
        for (FieldEntry field : fields) {
            EntryTriple intermediary = field.get("named");
            if (intermediary != null && intermediary.getOwner().equals(owner) && intermediary.getName().equals(intermediaryName)) {
                EntryTriple named = field.get("intermediary");
                if (named != null) {
                    return toDot(named.getName());
                }
            }
        }
        return intermediaryName;
    }
    public static String remapFieldNameI2N(String owner, String intermediaryName) {
        if (mappings == null) return intermediaryName;

        owner = toSlash(owner);

        List<FieldEntry> fields = mappings.getFieldEntries().stream().toList();
        for (FieldEntry field : fields) {
            EntryTriple intermediary = field.get("named");
            if (intermediary != null && intermediary.getOwner().equals(owner) && intermediary.getName().equals(intermediaryName)) {
                EntryTriple named = field.get("intermediary");
                if (named != null) {
                    return toDot(named.getName());
                }
            }
        }

        return intermediaryName;
    }
    public static String remapClassName(String owner) {
        if (mappings == null) return owner;

        owner = toSlash(owner);

        List<ClassEntry> fields = mappings.getClassEntries().stream().toList();
        for (ClassEntry field : fields) {
            String intermediary = field.get("named");
            if (intermediary != null && intermediary.equals(owner)) {
                String named = field.get("intermediary");
                if (named != null) {
                    return toDot(named);
                }
            }
        }

        return owner;
    }
    public static String remapClassNameI2N(String owner) {
        if (mappings == null) return owner;

        owner = toSlash(owner);

        List<ClassEntry> fields = mappings.getClassEntries().stream().toList();
        for (ClassEntry field : fields) {
            String intermediary = field.get("intermediary");
            if (intermediary != null && intermediary.equals(owner)) {
                String named = field.get("named");
                if (named != null) {
                    return toDot(named);
                }
            }
        }

        return owner;
    }
    public static String remapMethodName(String owner, String intermediaryName) {
        if (mappings == null) return intermediaryName;

        owner = toSlash(owner);

        List<MethodEntry> fields = mappings.getMethodEntries().stream().toList();
        for (MethodEntry field : fields) {
            EntryTriple intermediary = field.get("named");
            if (intermediary != null && intermediary.getOwner().equals(owner) && intermediary.getName().equals(intermediaryName)) {
                EntryTriple named = field.get("intermediary");
                if (named != null) {
                    return toDot(named.getName());
                }
            }
        }

        return intermediaryName;
    }
    public static String remapMethodNameI2N(String owner, String intermediaryName) {
        if (mappings == null) return intermediaryName;

        owner = toSlash(owner);

        List<MethodEntry> fields = mappings.getMethodEntries().stream().toList();
        for (MethodEntry field : fields) {
            EntryTriple intermediary = field.get("intermediary");
            if (intermediary != null && intermediary.getOwner().equals(owner) && intermediary.getName().equals(intermediaryName)) {
                EntryTriple named = field.get("named");
                if (named != null) {
                    return toDot(named.getName());
                }
            }
        }

        return intermediaryName;
    }

    public static String toDot(String s) {
        return s.replace("/", ".");
    }
    public static String toSlash(String s) {
        return s.replace(".", "/");
    }
}
