package gg.topchdlc.vse.shutki.other.commands;

import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import lombok.Getter;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.Collections;
import java.util.List;

@Getter
public abstract class Command implements MinecraftHolder {
    private String name;
    private String description;
    private List<String> subcommands;

    public Command(String name, String description, String... subcommands) {
        this.name = name;
        this.description = description;
        this.subcommands = subcommands.length > 0 ? List.of(subcommands) : Collections.emptyList();
    }

    public abstract void execute(String[] args);
    protected void error(String error) {
        ChatUtility.send(Text.translatable(error).withColor(new Color(255, 100, 100).getRGB()));
    }
}
