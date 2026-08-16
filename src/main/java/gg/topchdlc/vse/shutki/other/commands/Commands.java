package gg.topchdlc.vse.shutki.other.commands;

import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventChatMessage;
import gg.topchdlc.vse.shutki.other.commands.impl.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;

@Getter
public class Commands implements MinecraftHolder {
    @Setter public static String PREFIX = ".";

    public ArrayList<Command> commands = new ArrayList<>();

    public Commands() {
        this.commands.addAll(Arrays.asList(
                ConfigCommand.INSTANCE,
                HelpCommand.INSTANCE,
                HotswapCommand.INSTANCE,
                FriendCommand.INSTANCE,
                ClientUserCommand.INSTANCE,
                BindCommand.INSTANCE,

                MacrosCommand.INSTANCE,
                GpsCommand.INSTANCE,
                BuilderCommand.INSTANCE,
                ABCommand.INSTANCE,
                WaypointCommand.INSTANCE,
                ParseCommand.INSTANCE,
                PrefixCommand.INSTANCE,
                VClipCommand.INSTANCE
        ));

        Client.EVENTS.register(this);
    }

    EventBus<Event> events = event -> {
        if (event instanceof EventChatMessage ev) {
            if (ev.message.startsWith(PREFIX)) {
                String[] args_raw = ev.message.split(" ");

                String cmd = args_raw[0].substring(PREFIX.length());

                for (Command command : commands) {
                    if (command.getName().equalsIgnoreCase(cmd)) {
                        if (Client.IS_PANIC) return;
                        ev.cancel();
                        command.execute(args_raw);
                    }
                }
            }
        }
    };
}