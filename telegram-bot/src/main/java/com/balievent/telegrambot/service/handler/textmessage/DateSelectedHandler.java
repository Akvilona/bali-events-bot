package com.balievent.telegrambot.service.handler.textmessage;

import com.balievent.telegrambot.constant.Settings;
import com.balievent.telegrambot.constant.TextMessageHandlerType;
import com.balievent.telegrambot.constant.TgBotConstants;
import com.balievent.telegrambot.model.entity.Event;
import com.balievent.telegrambot.model.entity.UserData;
import com.balievent.telegrambot.service.handler.callback.MessageBuilder;
import com.balievent.telegrambot.service.handler.common.MediaHandler;
import com.balievent.telegrambot.util.KeyboardUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DateSelectedHandler extends TextMessageHandler {
    private final MediaHandler mediaHandler;
    @Autowired
    private MessageBuilder messageBuilder;

    @Override
    public TextMessageHandlerType getHandlerType() {
        return TextMessageHandlerType.DATE_SELECTED;
    }

    @Override
    public void handle(final Update update) throws TelegramApiException {
        final Long chatId = update.getMessage().getChatId();
        // сохраняем указанную дату и возвращаем поля таблицы user_data для данного пользователя
        final UserData userData = userDataService.updateCalendarDate(update, false);
        userDataService.saveUserMessageId(update.getMessage().getMessageId(), chatId);      // сохраняем ID сообщения в postgres.public.user_data.last_user_message_id

        clearChat(chatId, userData);                                                        // удаляем с экрана все сохраненные объекты (картинки)
        removeTextMessageDate(update);    //!!!! почему-то не удаляется               // удаляем текст локации
        final LocalDate eventsDateFor = userData.getSearchEventDate();                      // дата запроса

        final int currentPage = 1;                                                          //Всегда начинаем с первой страницы
        final List<Event> eventList = eventService.findEvents(eventsDateFor, currentPage - 1, Settings.PAGE_SIZE); // получаем список событий для одной страницы
        final int eventCount = eventService.countEvents(eventsDateFor);                     // получаем общее количество событий
        final int pageCount = (eventCount + Settings.PAGE_SIZE - 1) / Settings.PAGE_SIZE;   // получаем номер текущей страницы

        userDataService.updatePageInfo(chatId, pageCount, currentPage);                     // сохраняем общее количество страниц и текущую страницу

        final ReplyKeyboard replyKeyboard = KeyboardUtil.getDayEventsKeyboard(currentPage, pageCount); // создаем кнопки навигации по страницам
        final String displayDate = eventsDateFor.format(Settings.PRINT_DATE_TIME_FORMATTER); // форматируем дату из 2024-05-01 -> 01.05.2024
        // формирует строки по названия локаций за один день Пример: /1_Amazing_View_Sunset_Party
        final String eventsBriefMessage = messageBuilder.buildBriefEventsMessage(currentPage, eventList, chatId);
        // формируем сообщение для TELEGRAM сервера
        userDataService.saveUserMessageId(update.getMessage().getMessageId(), chatId);
        // сохраняем текущую страницу и общее количество страниц
        userDataService.updatePageInfo(chatId, pageCount, currentPage);

        final SendMessage sendMessage = SendMessage.builder()
            .chatId(chatId)
            .text(TgBotConstants.EVENT_LIST_TEMPLATE.formatted(displayDate, eventsBriefMessage))
            .parseMode(ParseMode.HTML)
            .replyMarkup(replyKeyboard)
            .disableWebPagePreview(true)
            .build();

        final Message message = myTelegramBot.execute(sendMessage);
        userDataService.updateLastBotMessageId(message.getMessageId(), chatId);     // сохранили MessageId в postgres.public.user_data.last_bot_message_id
        mediaHandler.handle(chatId, userData);                                      // создание группы картинок
    }

    private void removeTextMessageDate(final Update update) throws TelegramApiException {
        // этот метод отрабатывает при нажатии на ТЕКСТОВОЕ сообщение!!!
        final Message message = update.getMessage();
        if (message != null) {
            // Получаем chatId и messageId для сравнения
            final Long chatId = message.getChatId();
            final Integer messageId = update.getMessage().getMessageId();

            // Пытаемся удалить сообщение
            try {
                myTelegramBot.execute(DeleteMessage.builder()
                    .chatId(chatId.toString())
                    .messageId(messageId)
                    .build());

            } catch (TelegramApiException e) {
                // Если возникает ошибка, сообщение не существует
                System.out.println("Сообщение с messageId " + messageId + " не существует.");
            }
        } else {
            System.out.println("Обновление не содержит сообщения.");
        }
    }

    /*private void removeTextMessage(final Update update, final UserData userData) throws TelegramApiException {
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
    }*/
}
