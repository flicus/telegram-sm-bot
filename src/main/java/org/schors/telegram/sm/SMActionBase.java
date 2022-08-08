package org.schors.telegram.sm;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Map;

public abstract class SMActionBase implements Action<String, String> {

    protected Update update(StateContext<String, String> context) {
        return get("update", context);
    }

    protected AbsSender bot(StateContext<String, String> context) {
        return get("bot", context);
    }

    protected <T> T get(String id, StateContext<String, String> context) {
        return (T) context.getExtendedState().getVariables().get(id);
    }

    protected Long chatId(StateContext<String, String> context) {
        return update(context).getMessage().getChatId();
    }

    protected Map<Object, Object> vars(StateContext<String, String> context) {
        return context.getExtendedState().getVariables();
    }

    protected void reply(String text, StateContext<String, String> context) throws TelegramApiException {
        bot(context)
                .executeAsync(SendMessage.builder()
                        .chatId(chatId(context))
                        .text(text)
                        .build());
    }
}
