package gg.topchdlc.api.events;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EventPriority {
    int LAST = -50,
            NORMAL = 0,
            FIRST = 50;
    int value() default NORMAL;
}
