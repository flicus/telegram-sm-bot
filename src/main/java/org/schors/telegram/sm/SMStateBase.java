package org.schors.telegram.sm;

public abstract class SMStateBase implements SMState {

    @Override
    public boolean isInitial() {
        return false;
    }

    @Override
    public boolean isFinal() {
        return false;
    }
}
