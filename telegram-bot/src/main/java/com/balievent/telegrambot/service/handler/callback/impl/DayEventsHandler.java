package com.balievent.telegrambot.service.handler.callback.impl;

import com.balievent.telegrambot.constant.CallbackHandlerType;
import com.balievent.telegrambot.model.entity.UserData;
import com.balievent.telegrambot.service.handler.callback.ButtonCallbackHandler;
import com.balievent.telegrambot.service.handler.common.MediaHandler;
import com.balievent.telegrambot.service.service.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@RequiredArgsConstructor
@Service
@Slf4j
public class DayEventsHandler extends ButtonCallbackHandler {
    private final MediaHandler mediaHandler;
    private final UserDataService userDataService;

    @Override
    public CallbackHandlerType getCallbackHandlerType() {
        return CallbackHandlerType.DAY_EVENT_PAGE;
    }

    @Override
    public void handle(final Update update) throws TelegramApiException {
        final Long chatId = update.getCallbackQuery().getMessage().getChatId();
        // Данные о пользователе
        final UserData userData = userDataService.getUserData(chatId);

        removeMediaMessage(chatId, userData);           // удаляем одну картинку
        removeTextMessage(update, userData);            // удаляем текст локации
        mediaHandler.handle(chatId, userData);          // создание группы картинок
    }

    private void removeTextMessage(final Update update, final UserData userData) throws TelegramApiException {
        // этот метод отрабатывает при нажатии по КНОПКЕ!!!!
        final CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            final String chatId = callbackQuery.getMessage().getChatId().toString();
            final Integer messageId = userData.getLocationMessageId();

            // Пытаемся удалить сообщение
            try {
                myTelegramBot.execute(DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .build());

                // Удаляем поле из объекта UserData, если сообщение было успешно удалено
                userData.setLocationMessageId(null);
            } catch (TelegramApiException e) {
                // Если возникает ошибка, сообщение не существует
                System.out.println("Сообщение с messageId " + messageId + " не существует.");
            }
        } else {
            System.out.println("Обновление не содержит CallbackQuery.");
        }
    }

    private void removeMediaMessage(final Long chatId, final UserData userData) {
        if (CollectionUtils.isEmpty(userData.getMediaMessageIdList())) {
            return;
        }
        try {
            myTelegramBot.execute(DeleteMessages.builder()
                .chatId(chatId)
                .messageIds(userData.getMediaMessageIdList())
                .build());

        } catch (TelegramApiException e) {
            log.error("Media message not found {}", e.getMessage());
        }
    }
}
