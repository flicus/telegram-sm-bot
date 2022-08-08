package org.schors.telegram.sm;

public abstract class SMEventBase implements SMEvent {
    @Override
    public int order() {
        return 0;
    }
}
