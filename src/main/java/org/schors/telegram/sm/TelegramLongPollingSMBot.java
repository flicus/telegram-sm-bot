package org.schors.telegram.sm;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.session.Session;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.session.TelegramLongPollingSessionBot;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
public abstract class TelegramLongPollingSMBot extends TelegramLongPollingSessionBot {

    @Autowired
    private StateMachine<String, String> sm;

    @Autowired
    private StateMachinePersister<String, String, Serializable> persister;

    @Autowired
    private ObjectProvider<List<SMEvent>> smEvents;

    private SMEvent defaultEvent;

    @PostConstruct
    private void init() {
        defaultEvent = new SMEventBase() {
            @Override
            public String name() {
                return "UNKNOWN";
            }

            @Override
            public Predicate<Update> matcher() {
                return update -> false;
            }
        };
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update, Optional<Session> session) {
        List<SMEvent> events = smEvents.getIfAvailable();
        if (session.isPresent()) {
            SMEvent event = events.stream()
                    .filter(smEvent -> smEvent.matcher().test(update))
                    .sorted((o1, o2) -> o1.order() > o2.order() ? 1 : -1)
                    .findFirst()
                    .orElse(defaultEvent);

            if (session.get().getAttribute("initiated") != null) {
                persister.restore(sm, session.get().getId());
            } else {
                sm.getExtendedState().getVariables().put("session", session.get());
                sm.getExtendedState().getVariables().put("bot", this);
                session.get().setAttribute("initiated", true);
            }
            sm.getExtendedState().getVariables().put("update", update);
            log.debug("Using SM: {}, state before event: {}", sm.getUuid(), sm.getState().getId());
            StateMachineEventResult<String, String> result = sm.sendEvent(Mono.just(MessageBuilder.withPayload(event.name()).build())).blockLast();
            log.debug("Result: {}, {}", result.getResultType(), result.getMessage());
            log.debug("State after event: {}", sm.getState().getId());
            persister.persist(sm, session.get().getId());
        }
    }
}
