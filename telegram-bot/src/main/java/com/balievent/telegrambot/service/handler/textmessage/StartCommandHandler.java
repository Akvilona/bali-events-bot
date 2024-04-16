package com.balievent.telegrambot.service.handler.textmessage;

import com.balievent.telegrambot.constant.TelegramButton;
import com.balievent.telegrambot.constant.TextMessageHandlerType;
import com.balievent.telegrambot.constant.TgBotConstants;
import com.balievent.telegrambot.model.entity.Location;
import com.balievent.telegrambot.model.entity.UserData;
import com.balievent.telegrambot.repository.LocationRepository;
import com.balievent.telegrambot.service.service.EventSearchCriteriaService;
import com.balievent.telegrambot.util.KeyboardUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StartCommandHandler extends TextMessageHandler {

    private final EventSearchCriteriaService eventSearchCriteriaService;
    private final LocationRepository locationRepository;

    @Override
    public TextMessageHandlerType getHandlerType() {
        return TextMessageHandlerType.START_COMMAND;
    }

    @Override
    public void handle(final Update update) throws TelegramApiException {
        final Long chatId = update.getMessage().getChatId();                            // идентификатор пользователя
        final UserData userData = userDataService.saveOrUpdateUserData(chatId);         // сохраняем в user_data.id
        userDataService.saveUserMessageId(update.getMessage().getMessageId(), chatId);  // идентификатор сообщения сохраняем в user_data.last_user_message_id

        final List<String> locationNameList = new ArrayList<>(locationRepository.findAll() // получаем список всех локаций
            .stream()
            .map(Location::getId)
            .toList());

        locationNameList.add(TelegramButton.DESELECT_ALL_LOCATIONS.getCallbackData()); // добавляем кнопку "Deselect all" в список выделяемых объектов следующего окна

        // сохраняем все локации и кнопку "Deselect all" в event_search_criteria.location_name_list
        eventSearchCriteriaService.saveOrUpdateEventSearchCriteria(chatId, locationNameList);

        clearChat(chatId, userData); // удаляем рабочее окно на экране пользователя

        final String searchThisEvents = eventSearchCriteriaService.getSearchEvents(chatId); // получаем критерий поиска

        final SendMessage sendMessage = SendMessage.builder()
            .chatId(chatId)
            .text(TgBotConstants.EVENT_DATE_QUESTION.formatted())
            .replyMarkup(KeyboardUtil.createEventDateSelectionKeyboard(searchThisEvents))
            .build();

        final Message message = myTelegramBot.execute(sendMessage);
        // сохраняем идентификатор сообщения, чтобы все последующие окна перезаписывали это сообщение
        userDataService.updateLastBotMessageId(message.getMessageId(), chatId);
    }

}
