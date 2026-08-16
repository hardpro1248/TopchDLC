package gg.topchdlc.api.scripts;

import gg.topchdlc.Client;
import gg.topchdlc.api.scripts.api.bindings.event.IEventProvider;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Tag;
import gg.topchdlc.vse.utils.other.LogUtility;
import lombok.Getter;
import lombok.Setter;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class Script {
    private final String name;
    private final File scriptFile;

    private ScriptedModule scriptedModule;

    private boolean moduleAdded = false;

    private Context scriptctx;

    @Setter
    private IEventProvider eventProvider;

    public Script(String name, File scriptFile) {
        this.name = name;
        this.scriptFile = scriptFile;
    }

    public void close() {
        if (scriptctx != null) {
            try {
                scriptctx.close(true);
            } catch (Exception ignored) {}
            scriptctx = null;
        }
    }

    public void init() {
        close();
        try {
            scriptctx = Context.newBuilder("js")
                    .allowHostAccess(HostAccess.ALL)
                    .allowHostClassLookup(clazz -> clazz.startsWith("um.topchdlc.api.scripts.api.bindings"))
                    .allowHostClassLoading(true)
                    .allowIO(false)
                    .allowNativeAccess(false)
                    .option("engine.WarnInterpreterOnly", "false")
                    .build();

            ScriptBindings.bind(scriptctx, this);

            String source = new String(new FileInputStream(this.scriptFile).readAllBytes());

            scriptctx.eval("js", source);

        } catch (Exception e) {
        }
    }

    @SuppressWarnings("unchecked")
    public void register(Map<String, Object> bindings) {
        String name = bindings.get("name").toString();
        String description = bindings.containsKey("description") ? bindings.get("description").toString() : "";
        List<?> authorsList = (List<?>) bindings.get("authors");
        List<Integer> tags = bindings.containsKey("tags") ? (List<Integer>) bindings.get("tags") : new ArrayList<>();
        String[] authors = authorsList.stream()
                .map(Object::toString)
                .toArray(String[]::new);
        String category = bindings.get("category").toString();
        if (!moduleAdded) {
            this.scriptedModule = new ScriptedModule(name, Category.valueOf(category), -1, authors, description);
            LogUtility.debug("size: " + Client.MODULES.getModules().size());
            Client.MODULES.add(this.scriptedModule);
            LogUtility.debug("size: " + Client.MODULES.getModules().size());
            moduleAdded = true;
            System.out.printf("Registered script: %s (%s -> %s)%n",
                    this.scriptedModule.getName(), name, category);
        } else {
            this.scriptedModule.setAuthors(authors);
            this.scriptedModule.name = name;
            this.scriptedModule.setDescription(description);
            this.scriptedModule.setCategory(Category.valueOf(category));
            System.out.printf("Reloaded script: %s (%s -> %s)%n",
                    this.scriptedModule.getName(), name, category);
        }

        this.scriptedModule.getTags().clear();
        for (int tag : tags) {
            this.scriptedModule.addTag(Tag.values()[tag]);
        }
    }
}
