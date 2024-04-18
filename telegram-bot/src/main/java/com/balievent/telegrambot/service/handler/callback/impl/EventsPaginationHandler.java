package com.balievent.telegrambot.service.handler.callback.impl;

import com.balievent.telegrambot.constant.CallbackHandlerType;
import com.balievent.telegrambot.constant.Settings;
import com.balievent.telegrambot.constant.TelegramButton;
import com.balievent.telegrambot.constant.TgBotConstants;
import com.balievent.telegrambot.exceptions.ErrorCode;
import com.balievent.telegrambot.exceptions.ServiceException;
import com.balievent.telegrambot.model.entity.Event;
import com.balievent.telegrambot.model.entity.UserData;
import com.balievent.telegrambot.service.handler.callback.ButtonCallbackHandler;
import com.balievent.telegrambot.service.handler.callback.MessageBuilder;
import com.balievent.telegrambot.service.handler.common.MediaHandler;
import com.balievent.telegrambot.service.service.EventService;
import com.balievent.telegrambot.service.service.UserDataService;
import com.balievent.telegrambot.util.KeyboardUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventsPaginationHandler extends ButtonCallbackHandler {

    private final EventService eventService;

    private final UserDataService userDataService;

    private final MediaHandler mediaHandler;

    private final MessageBuilder messageBuilder;

    @Override
    public CallbackHandlerType getCallbackHandlerType() {
        return CallbackHandlerType.EVENTS_PAGINATION;
    }

    private UserData updateUserData(final Update update) {
        final String string = update.getCallbackQuery().getData();
        final Long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (TelegramButton.FIRST_EVENTS_PAGE.getCallbackData().equals(string)) {
            return userDataService.updateCurrentPage(chatId, 1);
        } else if (TelegramButton.NEXT_EVENTS_PAGE.getCallbackData().equals(string)) {
            return userDataService.incrementCurrentPage(chatId);
        } else if (TelegramButton.PREVIOUS_EVENTS_PAGE.getCallbackData().equals(string)) {
            return userDataService.decrementCurrentPage(chatId);
        } else if (TelegramButton.LAST_EVENTS_PAGE.getCallbackData().equals(string)) {
            final int pageCount = userDataService.getUserData(chatId).getTotalEventPages();
            return userDataService.updateCurrentPage(chatId, pageCount);
        }
        throw new ServiceException(ErrorCode.ERR_CODE_999);
    }

    @Override
    public void handle(final Update update) throws TelegramApiException {
        final UserData userData = updateUserData(update);
        // получаем список событий на указанную дату, но не больше восьми
        // здесь происходит ошибка когда нажимаешь на кнопку <[1/3] потому что userData.getCurrentEventPage() = 0
        final List<Event> eventList = eventService.findEvents(userData.getSearchEventDate(), userData.getCurrentEventPage() - 1, Settings.PAGE_SIZE);
        final Long chatId = update.getCallbackQuery().getMessage().getChatId();
        // получаем список событий по которым можно кликнуть (Этот список сохранен в Базе Данных в поле postgres.public.user_data.location_map )
        final String eventsBriefMessage = messageBuilder.buildBriefEventsMessage(userData.getCurrentEventPage(), eventList, chatId);
        // форматируем указанную дату из такого формата '2024-04-13' -> в такой '13.04.2024'
        final String formattedDate = userData.getSearchEventDate().format(Settings.PRINT_DATE_TIME_FORMATTER);

        final EditMessageText editMessageText = EditMessageText.builder()
            .chatId(chatId)
            .messageId(update.getCallbackQuery().getMessage().getMessageId())
            .text(TgBotConstants.EVENT_LIST_TEMPLATE.formatted(formattedDate, eventsBriefMessage))  // формируем сообщение  'List of events on: 13.04.2024'
            .parseMode(ParseMode.HTML)                                                              // + список всех событий на эту дату
            .disableWebPagePreview(true)
            .replyMarkup(KeyboardUtil.getDayEventsKeyboard(userData.getCurrentEventPage(), userData.getTotalEventPages())) // добавляем три кнопки
            .build();

        myTelegramBot.execute(editMessageText); // отправляем на сервер сформированное сообщение

        removeMediaMessage(chatId, userData); // удаляем блок картинок
        mediaHandler.handle(chatId, userData); // создаем блок картинок
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
