package gg.topchdlc.vse.shutki.module.modules;

import lombok.Getter;

import java.awt.*;

@Getter
public enum Tag {
    Sosiski("Sosiski", new Color(220, 20, 60));
    private final String name;
    private final Color color;
    Tag(String name, Color color) {
        this.name = name;
        this.color = color;
    }

}