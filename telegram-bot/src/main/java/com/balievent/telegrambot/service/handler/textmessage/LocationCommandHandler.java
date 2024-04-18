/**
 * Создал Андрей Антонов 4/19/2024 1:43 AM.
 **/

package com.balievent.telegrambot.service.handler.textmessage;

import com.balievent.telegrambot.constant.TextMessageHandlerType;
import com.balievent.telegrambot.model.entity.UserData;
import com.balievent.telegrambot.service.handler.common.MediaHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationCommandHandler extends TextMessageHandler {
    private final MediaHandler mediaHandler;

    @Override
    public TextMessageHandlerType getHandlerType() {
        return TextMessageHandlerType.LOCATION_COMMAND;
    }

    @Override
    public void handle(final Update update) throws TelegramApiException {
        final Long chatId = update.getMessage().getChatId();                            // идентификатор пользователя
        final UserData userData = userDataService.saveOrUpdateUserData(chatId);         // сохраняем в user_data.id
        final String textMessage = update.getMessage().getText();
        // удаляем все картинки
        removeMediaMessage(update.getMessage().getChatId());
        // удаляем собственное сообщение
        removeTextMessage(update);
        // создаем картинку события
        mediaHandler.location(chatId, userData, textMessage); // создаем картинку
    }

    private void removeTextMessage(final Update update) throws TelegramApiException {
        // удаляем это сообщение
        myTelegramBot.execute(DeleteMessage.builder()
            .chatId(update.getMessage().getChatId())
            .messageId(update.getMessage().getMessageId())
            .build());

    }

    public void removeMediaMessage(final Long chatId) {
        final UserData userData = userDataService.getUserData(chatId);
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
