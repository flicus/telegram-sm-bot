package org.schors.telegram.sm;

public interface SMState {
    String getName();

    boolean isInitial();

    boolean isFinal();
}
