package com.balievent.telegrambot.service.handler.textmessage;

import com.balievent.telegrambot.contant.MyConstants;
import com.balievent.telegrambot.contant.Settings;
import com.balievent.telegrambot.model.entity.Event;
import com.balievent.telegrambot.service.storage.UserDataStorage;
import com.balievent.telegrambot.service.support.EventService;
import com.balievent.telegrambot.util.CommonUtil;
import com.balievent.telegrambot.util.KeyboardUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DateSelectedHandler implements TextMessageHandler {
    private final EventService eventService;
    private final UserDataStorage userDataStorage;

    @Override
    public TextMessageHandlerType getHandlerType() {
        return TextMessageHandlerType.DATE_SELECTED;
    }

    @Override
    public SendMessage handle(final Update update) {
        final LocalDate localDate = userDataStorage.updateWithSelectedDate(update);
        final String eventListToday = getBriefEventsForToday(localDate);

        return SendMessage.builder()
            .chatId(update.getMessage().getChatId())
            .text(String.format("%s %s %n%n %s", MyConstants.LIST_OF_EVENTS_ON, localDate.format(Settings.PRINT_DATE_TIME_FORMATTER), eventListToday))
            .parseMode(ParseMode.HTML)
            .disableWebPagePreview(true)
            .replyMarkup(KeyboardUtil.getPaginationKeyboard())
            .build();
    }

    private String getBriefEventsForToday(final LocalDate localDate) {
        final List<Event> eventList = eventService.findEvents(localDate, 0, 8);

        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < eventList.size(); i++) {
            final Event event = eventList.get(i);
            stringBuilder.append(i + 1).append(". ")
                .append(CommonUtil.getLink(event.getEventName(), event.getEventUrl()))
                .append("\n");
        }
        return stringBuilder.toString();
    }
}