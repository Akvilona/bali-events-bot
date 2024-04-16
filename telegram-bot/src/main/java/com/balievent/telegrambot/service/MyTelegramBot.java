package com.balievent.telegrambot.service;

import com.balievent.telegrambot.configuration.TelegramBotProperties;
import com.balievent.telegrambot.constant.CallbackHandlerType;
import com.balievent.telegrambot.constant.TelegramButton;
import com.balievent.telegrambot.constant.TextMessageHandlerType;
import com.balievent.telegrambot.constant.TgBotConstants;
import com.balievent.telegrambot.exceptions.ServiceException;
import com.balievent.telegrambot.service.handler.callback.ButtonCallbackHandler;
import com.balievent.telegrambot.service.handler.textmessage.TextMessageHandler;
import com.balievent.telegrambot.util.DateUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class MyTelegramBot extends TelegramLongPollingBot {
    private final Map<TextMessageHandlerType, TextMessageHandler> textMessageHandlers;
    private final Map<CallbackHandlerType, ButtonCallbackHandler> callbackHandlers;
    private final TelegramBotProperties telegramBotProperties;

    public MyTelegramBot(
        final @Lazy Map<TextMessageHandlerType, TextMessageHandler> textMessageHandlers,
        final @Lazy Map<CallbackHandlerType, ButtonCallbackHandler> callbackHandlers,
        final TelegramBotProperties telegramBotProperties
    ) {
        super(telegramBotProperties.getToken());
        this.textMessageHandlers = textMessageHandlers;
        this.telegramBotProperties = telegramBotProperties;
        this.callbackHandlers = callbackHandlers;
    }

    @Override
    public String getBotUsername() {
        return telegramBotProperties.getUsername();
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(final Update update) {
        try {
            if (update.hasCallbackQuery()) {
                processCallbackQuery(update);   // ВХОД ПО НАЖАТИЮ НА КНОПКУ
            } else {
                processTextMessage(update);     // ВХОД ДЛЯ НОВОЫХ ОБЪЕКТОВ
            }
        } catch (ServiceException e) {
            log.error("ServiceException {}", e.getMessage(), e);
            execute(SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .text(e.getMessage())
                .build());
        }
    }

    private void processTextMessage(final Update update) throws TelegramApiException {
        final String messageText = update.getMessage().getText(); // ТЕКСТ СООБЩЕНИЯ
        if (messageText.contains("/start")) {
            textMessageHandlers.get(TextMessageHandlerType.START_COMMAND).handle(update); // СОЗДАНИЕ ФИЛЬТРА ИЗ (6-И) КНОПОК ЭТО public class StartCommandHandler()
            return;
        }

        if (DateUtil.isCalendarMonthChanged(messageText)) {
            textMessageHandlers.get(TextMessageHandlerType.CALENDAR_MONTH_CHANGED).handle(update);
        } else if (DateUtil.isDateSelected(messageText)) {
            textMessageHandlers.get(TextMessageHandlerType.DATE_SELECTED).handle(update);
        } else {
            execute(DeleteMessage.builder()
                .chatId(update.getMessage().getChatId())
                .messageId(update.getMessage().getMessageId())
                .build());
        }
    }

    private void processCallbackQuery(final Update update) throws TelegramApiException {
        // проверяем  нажатие кнопки на втором окне
        if (eventLocationFilterProcess(update)) {
            return;
        }
        // получаем имя нажатой кнопки с первого окна (нажата одна из шести кнопок)
        final String clickedButtonName = update.getCallbackQuery().getData().toUpperCase(Locale.ROOT);

        final CallbackHandlerType callbackHandlerType = TelegramButton.valueOf(clickedButtonName).getCallbackHandlerType();
        callbackHandlers.get(callbackHandlerType).handle(update);
    }

    //Метод который обрабатывает фильтры по локация
    //Это исключительно для фильтрации локаций, чтобы не попадать в обработчик кнопок
    //Сначала обрабатываем копку Next, потом по тексту сообщения
    private boolean eventLocationFilterProcess(final Update update) throws TelegramApiException {
        //Проверку на MONTH_EVENTS_PAGE делаем отдельно раньше для сценария с выбором локации и нажатии на кнопку Next
        //(чтобы не попадать снова в хендлер с выбором локации)
        if (TelegramButton.MONTH_EVENTS_PAGE.getCallbackData().equals(update.getCallbackQuery().getData())) {
            // Попадаем сюда если пользователь выбрал кнопку Next -> MONTH_EVENTS_PAGE
            callbackHandlers.get(CallbackHandlerType.MONTH_EVENTS_PAGE).handle(update);
            return true;

            //Проверка по содержанию сообщения из-за того, что callback с локациями динамический и нельзя на него завязываться
        } else if (update.getCallbackQuery().getMessage() instanceof Message message
            && TgBotConstants.EVENT_LOCATIONS_QUESTION.equals(message.getText())) {
            callbackHandlers.get(CallbackHandlerType.EVENT_LOCATIONS_SELECTION).handle(update);
            return true;
        }
        return false;
    }

}

