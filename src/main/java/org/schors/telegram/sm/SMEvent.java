package org.schors.telegram.sm;

import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.function.Predicate;

public interface SMEvent {
    int order();

    String name();

    Predicate<Update> matcher();
}
