package org.schors.telegram.sm;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(SMActions.class)
public @interface SMAction {
    String source();

    String target();

    String event();
}
