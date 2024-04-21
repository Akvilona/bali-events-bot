/**
 * Создал Андрей Антонов 4/19/2024 1:43 AM.
 **/

package com.balievent.telegrambot.service.handler.textmessage;

import com.balievent.telegrambot.constant.Settings;
import com.balievent.telegrambot.constant.TextMessageHandlerType;
import com.balievent.telegrambot.constant.TgBotConstants;
import com.balievent.telegrambot.model.entity.Event;
import com.balievent.telegrambot.model.entity.UserData;
import com.balievent.telegrambot.service.handler.callback.MessageBuilder;
import com.balievent.telegrambot.service.handler.common.MediaHandler;
import com.balievent.telegrambot.service.service.UserDataService;
import com.balievent.telegrambot.util.KeyboardUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationCommandHandler extends TextMessageHandler {
    private final MediaHandler mediaHandler;
    @Autowired
    private MessageBuilder messageBuilder;
    @Autowired
    private UserDataService userDataService;

    @Override
    public TextMessageHandlerType getHandlerType() {
        return TextMessageHandlerType.LOCATION_COMMAND;
    }

    @Override
    public void handle(final Update update) throws TelegramApiException {
        final Long chatId = update.getMessage().getChatId();                                // идентификатор пользователя
        final UserData userData = userDataService.getUserDataById(chatId);                  // получаем параметры пользователя

        final String textMessage = update.getMessage().getText();                           // текст сообщения например '/2_Amazing_View_Sunset_Party'
        removeMediaMessage(update.getMessage().getChatId());                                // удаляем все картинки или блока или предыдущей локации
        removeTextMessage(update);                                                          // удаляем собственное сообщение
        removeTextMessage(update, userData);                                                // удаляем текст локации
        mediaHandler.location(chatId, userData, textMessage);                               // создаем картинку события
        final LocalDate eventsDateFor = userData.getSearchEventDate();                      // дата события
        final String displayDate = eventsDateFor.format(Settings.PRINT_DATE_TIME_FORMATTER); // форматируем дату из 2024-05-01 -> 01.05.2024

        // получаем список возможных локаций
        final Map<String, Long> locationMap = userData.getLocationMap();
        // Ищем ID локации по тексту сообщения от пользователя
        final Long eventId = locationMap.get(textMessage);
        // создаем кнопку навигации из сообщения для возврата к основному меню
        final ReplyKeyboard replyKeyboard = KeyboardUtil.getDayEventsKeyboard(0, 0);
        // список (одна запись) выбранного события
        final List<Event> eventList = eventService.findEventsById(eventId);
        // создаем заголовок сообщения
        final String title = textMessage.replace("/", "").replace("_", " ");
        // создаем текст сообщения
        final String eventsBriefMessage = messageBuilder.buildEventsMessage(eventList);
        // формируем сообщение для TELEGRAM сервера
        final SendMessage sendMessage = SendMessage.builder()
            .chatId(chatId)
            .text(TgBotConstants.EVENT_NAME_TEMPLATE.formatted(title, displayDate, eventsBriefMessage))
            .parseMode(ParseMode.HTML)
            .replyMarkup(replyKeyboard)
            .disableWebPagePreview(true)
            .build();

        // выводим сообщение пользователю
        final Message message = myTelegramBot.execute(sendMessage);
        // добавляем ID объекта в базу
        userDataService.updateLocationMessageId(message.getMessageId(), chatId);
    }

    private void removeTextMessage(final Update update) throws TelegramApiException {
        // удаляем это сообщение
        myTelegramBot.execute(DeleteMessage.builder()
            .chatId(update.getMessage().getChatId())
            .messageId(update.getMessage().getMessageId())
            .build());
    }

    private void removeTextMessage(final Update update, final UserData userData) throws TelegramApiException {
        // этот метод отрабатывает при нажатии на ТЕКСТОВОЕ сообщение!!!
        final Message message = update.getMessage();
        if (message != null) {
            // Получаем chatId и messageId для сравнения
            final Long chatId = message.getChatId();
            final Integer messageId = userData.getLocationMessageId();

            // Пытаемся удалить сообщение
            try {
                myTelegramBot.execute(DeleteMessage.builder()
                    .chatId(chatId.toString())
                    .messageId(messageId)
                    .build());

                // Удаляем поле из объекта UserData, если сообщение было успешно удалено
                userData.setLocationMessageId(null);
            } catch (TelegramApiException e) {
                // Если возникает ошибка, сообщение не существует
                System.out.println("Сообщение с messageId " + messageId + " не существует.");
            }
        } else {
            System.out.println("Обновление не содержит сообщения.");
        }
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
