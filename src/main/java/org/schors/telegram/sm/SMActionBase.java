package org.schors.telegram.sm;

import lombok.SneakyThrows;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Map;
import java.util.Optional;

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

    protected ContextWrapper context(StateContext<String, String> context) {
        return new ContextWrapper() {
            private StateContext<String, String> context;
            @Override
            public ContextWrapper context(StateContext<String, String> context) {
                this.context = context;
                return this;
            }

            @SneakyThrows
            @Override
            public ContextWrapper confirmCallback() {
                bot(context).executeAsync(AnswerCallbackQuery.builder()
                        .callbackQueryId(update(context).getCallbackQuery().getId())
                        .build());
                return this;
            }

            @SneakyThrows
            @Override
            public ContextWrapper sendBusy(String action) {
                bot(context).executeAsync(SendChatAction.builder().chatId(chatId(context)).action(action).build());
                return this;
            }

            @SneakyThrows
            @Override
            public ContextWrapper reply(String text) {
                bot(context)
                        .executeAsync(SendMessage.builder()
                                .chatId(chatId(context))
                                .text(text)
                                .build());
                return this;
            }

            @Override
            public ContextWrapper put(String id, Object value) {
                vars(context).put(id, value);
                return this;
            }

            @Override
            public ContextWrapper storeMessageText() {
                vars(context).put("msg", update(context).getMessage().getText());
                return this;
            }

            @Override
            public ContextWrapper storeInlineData() {
                vars(context).put("cmd", update(context).getCallbackQuery().getData());
                return this;
            }

            @Override
            public ContextWrapper storeMessageId() {
                vars(context).put("msgId", update(context).getMessage().getMessageId());
                return this;
            }

            @SneakyThrows
            @Override
            public ContextWrapper sendMessage(SendMessage message) {
                bot(context).execute(message);
                return this;
            }

            @SneakyThrows
            @Override
            public ContextWrapper sendMessageAndStoreId(SendMessage message) {
                Message res = bot(context).execute(message);
                vars(context).put("msgId", res.getMessageId());
                return this;
            }

        }.context(context);
    }

    protected Long chatId(StateContext<String, String> context) {
        return Optional
                .ofNullable(update(context).getMessage())
                .map(message -> message.getChatId())
                .orElseGet(() -> update(context).getCallbackQuery().getMessage().getChatId());
    }

    protected Map<Object, Object> vars(StateContext<String, String> context) {
        return context.getExtendedState().getVariables();
    }

    public interface ContextWrapper {
        ContextWrapper context(StateContext<String, String> context);
        ContextWrapper confirmCallback();
        ContextWrapper sendBusy(String action);
        ContextWrapper reply(String text);
        ContextWrapper put(String id, Object value);
        ContextWrapper storeMessageText();
        ContextWrapper storeInlineData();
        ContextWrapper storeMessageId();
        ContextWrapper sendMessage(SendMessage message);
        ContextWrapper sendMessageAndStoreId(SendMessage message);
    }
}
