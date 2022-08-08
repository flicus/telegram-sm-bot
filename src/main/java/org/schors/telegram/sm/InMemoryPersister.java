package org.schors.telegram.sm;

import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;

import java.io.Serializable;
import java.util.HashMap;

public class InMemoryPersister implements StateMachinePersist<String, String, Serializable> {

    private final HashMap<Serializable, StateMachineContext<String, String>> contexts = new HashMap<>();

    @Override
    public void write(final StateMachineContext<String, String> context, Serializable contextObj) {
        contexts.put(contextObj, context);
    }

    @Override
    public StateMachineContext<String, String> read(final Serializable contextObj) {
        return contexts.get(contextObj);
    }
}
