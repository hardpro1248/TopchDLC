    package gg.topchdlc.vse.shutki.module.modules.impl.misc;

    import net.minecraft.text.MutableText;
    import net.minecraft.text.Style;
    import net.minecraft.text.Text;
    import net.minecraft.text.TextContent;
    import gg.topchdlc.Client;
    import gg.topchdlc.api.events.Event;
    import gg.topchdlc.api.events.EventBus;
    import gg.topchdlc.api.events.list.EventAddMessage;
    import gg.topchdlc.vse.shutki.module.modules.Category;
    import gg.topchdlc.vse.shutki.module.modules.Module;
    import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
    import gg.topchdlc.vse.shutki.module.settings.impl.text.TextSetting;

    import java.util.regex.Pattern;

    /**
     * Create by daun kvass
     */
    public class NameProtect extends Module {
        public static final NameProtect INSTANCE = new NameProtect();

        private final TextSetting fakeName = text("Псевдоним", "kvass");
        private final CheckBox replaceVariants = checkbox("Заменять вариации", true);
        private final CheckBox hideFriends = checkbox("Скрывать друзей", false);

        private NameProtect() {
            super("NameProtect", Category.Misc, "Скрывает ник ");
        }


        public String replace(String input) {
            if (mc.player == null) return input;
            String realName = mc.player.getGameProfile().name();
            String fake = fakeName.getText().isEmpty() ? "kvass" : fakeName.getText();
            String result = applyReplacements(input, realName, fake);
            if (hideFriends.get() && Client.FRIENDS != null) {
                for (String friend : Client.FRIENDS.getFriendList()) {
                    if (!friend.isEmpty()) {
                        result = replaceIgnoreCase(result, friend, fake);
                    }
                }
            }
            return result;
        }

        public String getEntityName(net.minecraft.entity.Entity entity) {
            String name = entity.getName().getString();
            if (!isEnabled() || mc.player == null) return name;
            return replace(name);
        }

        private String applyReplacements(String input, String realName, String fake) {
            String result = replaceIgnoreCase(input, realName, fake);
            if (replaceVariants.get()) {
                String underscored = realName.replace(" ", "_");
                if (!underscored.equalsIgnoreCase(realName))
                    result = replaceIgnoreCase(result, underscored, fake);
                String normalized = normalize(realName);
                if (!normalized.equalsIgnoreCase(realName))
                    result = replaceIgnoreCase(result, normalized, fake);
            }
            return result;
        }

        private String replaceIgnoreCase(String text, String target, String replacement) {
            if (target.isEmpty()) return text;
            return text.replaceAll("(?i)" + Pattern.quote(target), replacement);
        }


        private String normalize(String s) {
            return s.replace('ё', 'е').replace('Ё', 'Е');
        }

        private Text replaceInText(Text node, String realName, String fake) {
            TextContent content = node.getContent();
            Style style = node.getStyle();
            MutableText result = replaceContent(content, style, realName, fake);

            for (Text sibling : node.getSiblings()) {
                result.append(replaceInText(sibling, realName, fake));
            }
            return result;
        }

        private MutableText replaceContent(TextContent content, Style style, String realName, String fake) {
            StringBuilder sb = new StringBuilder();
            content.visit(s -> {
                sb.append(s);
                return java.util.Optional.empty();
            });
            String raw = sb.toString();

            if (!raw.isEmpty()) {
                String replaced = applyReplacements(raw, realName, fake);
                if (!replaced.equals(raw)) {
                    return Text.literal(replaced).setStyle(style);
                }
            }
            return MutableText.of(content).setStyle(style);
        }

        EventBus<Event> events = event -> {
            if (!(event instanceof EventAddMessage e)) return;
            if (mc.player == null) return;

            String realName = mc.player.getGameProfile().name();
            String fake = fakeName.getText().isEmpty() ? "Player" : fakeName.getText();
            String raw = e.text.getString();

            boolean contains = raw.toLowerCase().contains(realName.toLowerCase());
            if (!contains && replaceVariants.get()) {
                contains = raw.toLowerCase().contains(normalize(realName).toLowerCase())
                        || raw.toLowerCase().contains(realName.replace(" ", "_").toLowerCase());
            }
            if (!contains && hideFriends.get() && Client.FRIENDS != null) {
                for (String friend : Client.FRIENDS.getFriendList()) {
                    if (!friend.isEmpty() && raw.toLowerCase().contains(friend.toLowerCase())) {
                        contains = true;
                        break;
                    }
                }
            }

            if (contains) {
                Text replaced = replaceInText(e.text, realName, fake);
                if (hideFriends.get() && Client.FRIENDS != null) {
                    for (String friend : Client.FRIENDS.getFriendList()) {
                        if (!friend.isEmpty()) {
                            replaced = replaceInText(replaced, friend, fake);
                        }
                    }
                }
                e.replacement = replaced;
            }
        };
    }
