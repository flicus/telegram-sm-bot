package org.schors.telegram.sm;


import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.target.CommonsPool2TargetSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.config.StateMachineBuilder.Builder;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.persist.DefaultStateMachinePersister;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class SMConfiguration {

    private static final Action<String, String> errorAction = context -> {
        Exception exception = context.getException();
        log.error("Error in action: ", exception);
    };
    @Autowired
    private List<SMActionBase> smActions;
    @Autowired
    private List<SMEvent> smEvents;
    @Autowired
    private List<SMState> smStates;
    @Value("${smPoolSize:3}")
    private int smPoolSize;

    @Bean(name = "stateMachineTarget")
    @Scope(scopeName = "prototype")
    public StateMachine<String, String> stateMachineTarget() throws Exception {

        Builder<String, String> builder = StateMachineBuilder.builder();

        builder.configureConfiguration()
                .withConfiguration()
                .autoStartup(true);

        builder.configureStates()
                .withStates()
                .initial(getInitialState(smStates))
                .states(smStates.stream()
                        .map((state) -> state.getName())
                        .collect(Collectors.toSet()));

        StateMachineTransitionConfigurer<String, String> configurer = builder.configureTransitions();
        for (Action<String, String> action : smActions) {
            SMAction[] annotations = action.getClass().getAnnotationsByType(SMAction.class);
            if (annotations != null) {
                for (SMAction smAction : annotations) {
                    configurer
                            .withExternal()
                            .source(smAction.source())
                            .target(smAction.target())
                            .event(smAction.event())
                            .action(action, errorAction);
                }
            }
        }

        StateMachine<String, String> res = builder.build();
        res.addStateListener(new StateMachineListenerAdapter<>() {

            @Override
            public void stateMachineError(StateMachine<String, String> stateMachine, Exception exception) {
                log.debug("SM error: " + exception.getMessage());
            }

            @Override
            public void eventNotAccepted(Message<String> event) {
                log.debug("Wrong event: " + event);
            }

            @Override
            public void stateMachineStarted(StateMachine<String, String> stateMachine) {
                log.debug("SM started: " + stateMachine.getUuid());
            }

            @Override
            public void stateChanged(State<String, String> from, State<String, String> to) {
                log.debug(from.getId() + " -> " + to.getId());
            }

            @Override
            public void stateEntered(State<String, String> state) {
                log.debug("S: -> " + state.getId());
            }

            @Override
            public void stateExited(State<String, String> state) {
                log.debug("S: " + state.getId() + " ->");
            }

            @Override
            public void transition(Transition<String, String> transition) {
                log.debug("T: " + transition.getSource().getId() + " -> " + transition.getTarget().getId());
                log.debug("T: event: " + transition.getTrigger().getEvent());
                log.debug("T: actions: " + transition.getActions().size());
            }

            @Override
            public void transitionStarted(Transition<String, String> transition) {
                log.debug("T: started " + transition.getSource().getId() + " -> " + transition.getTarget().getId());
            }

            @Override
            public void transitionEnded(Transition<String, String> transition) {
                log.debug("T: ended " + transition.getSource().getId() + " -> " + transition.getTarget().getId());
            }
        });

        return res;
    }

    private String getInitialState(List<SMState> smStates) {
        return smStates.stream()
                .filter((state) -> state.isInitial())
                .map((state) -> state.getName())
                .findFirst()
                .get();
    }

    @Bean
    @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public ProxyFactoryBean stateMachine() {
        ProxyFactoryBean pfb = new ProxyFactoryBean();
        pfb.setTargetSource(poolTargetSource());
        return pfb;
    }

    @Bean
    public CommonsPool2TargetSource poolTargetSource() {
        CommonsPool2TargetSource pool = new CommonsPool2TargetSource();
        pool.setMaxSize(smPoolSize);
        pool.setTargetBeanName("stateMachineTarget");
        return pool;
    }

    @Bean
    public StateMachinePersister<String, String, Serializable> persister() {
        return new DefaultStateMachinePersister<>(new InMemoryPersister());
    }

}
