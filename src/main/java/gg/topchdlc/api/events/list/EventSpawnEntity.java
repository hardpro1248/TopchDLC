package gg.topchdlc.api.events.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.Entity;

@Getter
@AllArgsConstructor
public class EventSpawnEntity {
    private Entity entity;

}