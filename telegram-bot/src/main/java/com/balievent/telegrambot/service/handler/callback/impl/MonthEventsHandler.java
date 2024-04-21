package com.balievent.telegrambot.service.handler.callback.impl;

import com.balievent.telegrambot.constant.CallbackHandlerType;
import com.balievent.telegrambot.constant.TgBotConstants;
import com.balievent.telegrambot.model.entity.UserData;
import com.balievent.telegrambot.service.handler.callback.ButtonCallbackHandler;
import com.balievent.telegrambot.service.handler.common.MediaHandler;
import com.balievent.telegrambot.service.service.EventService;
import com.balievent.telegrambot.service.service.UserDataService;
import com.balievent.telegrambot.util.DateUtil;
import com.balievent.telegrambot.util.KeyboardUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;

@RequiredArgsConstructor
@Service
@Slf4j
public class MonthEventsHandler extends ButtonCallbackHandler {
    private final MediaHandler mediaHandler;
    private final UserDataService userDataService;
    private final EventService eventService;

    @Override
    public CallbackHandlerType getCallbackHandlerType() {
        return CallbackHandlerType.MONTH_EVENTS_PAGE;
    }

    @Override
    public void handle(final Update update) throws TelegramApiException {
        final Long chatId = update.getCallbackQuery().getMessage().getChatId();
        final UserData userData = userDataService.getUserData(chatId);          // Данные о пользователе
        final LocalDate calendarDate = userData.getSearchEventDate();           // Текущая дата
        final String formattedMonth = DateUtil.getFormattedMonth(calendarDate); // Возвращает строковое представление месяца в заданной календарной дате.
        // получаем сообщение, содержащее список событий, сгруппированных по дням для заданного диапазона дат.
        final String detailedEventsForMonth = eventService.getMessageWithEventsGroupedByDayFull(calendarDate, 1, calendarDate.lengthOfMonth());
        // здесь формируется строки /01_04_2024 : 8 events -> в какую дату сколько сообщений добавляем перевод строки
        final String eventListMessage = TgBotConstants.EVENT_LIST_TEMPLATE.formatted(formattedMonth, detailedEventsForMonth);
        // собираем текстовое сообщение за весь месяц
        final EditMessageText editMessageText = EditMessageText.builder()
            .chatId(chatId)
            .messageId(update.getCallbackQuery().getMessage().getMessageId())
            .text(eventListMessage)
            .replyMarkup(KeyboardUtil.createMonthInlineKeyboard(calendarDate))
            .build();

        removeMediaMessage(chatId, userData);           // удаляем группу картинок
        removeTextMessage(update, userData);            // удаляем текст локации
        myTelegramBot.execute(editMessageText);         // перезаписываем ТЕКСТОВОЕ сообщение
        // mediaHandler.handle(chatId, userData);       // создание группы картинок
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
