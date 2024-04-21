/**
 * Создал Андрей Антонов 4/18/2024 11:43 PM.
 **/

package com.balievent.telegrambot.service.handler.callback;

import com.balievent.telegrambot.constant.Settings;
import com.balievent.telegrambot.model.entity.Event;
import com.balievent.telegrambot.service.service.UserDataService;
import com.balievent.telegrambot.util.CommonUtil;
import com.balievent.telegrambot.util.GetGoogleMapLinkUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@Slf4j
public class MessageBuilder {
    @Autowired
    private UserDataService userDataService;

    public String buildBriefEventsMessage(final int currentPage, final List<Event> eventList, final Long chatId) {
        // это цикл по всем событиям на текущий день.
        final Map<String, Long> locationMap = new HashMap<>();
        final StringBuilder result = new StringBuilder();
        String line = "";
        for (int i = 0; i < eventList.size(); i++) {
            final Event event = eventList.get(i);

            line = "/"
                + (1 + i + Settings.PAGE_SIZE * (currentPage - 1))
                + "_"
                + processString(event.getEventName())
                + "\n";

            result.append(line);
            locationMap.put(line.trim(), event.getId());
        }
        // сохраняем структуру в базе
        userDataService.saveOrUpdateLocationMap(locationMap, chatId);

        // locationMap нужно сохранить пользователю в базу
        return result.toString();
    }

    public static String processString(final String input) {
        // Удаляем все символы, кроме цифр, букв и пробелов
        String processed = input.replaceAll("[^\\p{Alnum} ]", "");
        // Заменяем пробелы на подчеркивания
        processed = processed.replace(" ", "_")
            .replace("__", "_");
        return processed;
    }

    public static String formatMessageForEventsGroupedByDay(final Map<LocalDate, List<Event>> eventMap) {
        final Map<LocalDate, List<Event>> reverseSortedMap = new TreeMap<>(eventMap);

        final StringBuilder stringBuilder = new StringBuilder();
        reverseSortedMap.forEach((key, value) ->
            stringBuilder.append("/").append(key.format(DateTimeFormatter.ofPattern(getDdMmYyyy())))
                .append(" : ")
                .append(value.size())
                .append(" events")
                .append("\n"));

        if (stringBuilder.isEmpty()) {
            stringBuilder.append("No events");
        }

        return stringBuilder.toString();
    }

    private static String getDdMmYyyy() {
        return "dd_MM_yyyy";
    }

    public boolean isRequestLocalMap(final Update update) {
        final String messageText = update.getMessage().getText().trim(); // ТЕКСТ СООБЩЕНИЯ
        final Map<String, Long> locationMap = userDataService.getLocationMap(update.getMessage().getChatId()); // список возможны переходов
        return locationMap.containsKey(messageText);
    }

    public String buildEventsMessage(final List<Event> eventList) {
        // это цикл по всем событиям на текущий день на данной локации.
        final StringBuilder result = new StringBuilder();
        String line = "";

        for (int i = 0; i < eventList.size(); i++) {

            final Event event = eventList.get(i);

            line = "NAME: " + event.getEventName() + "\n"
                + "Time: Begin: " + event.getStartDate().toLocalTime().toString() + "\n"
                + "Time: End: " + event.getEndDate().toLocalTime().toString() + "\n"
                + CommonUtil.getLink("Buy Tickets Now!", event.getEventUrl()) + "\n"
                + GetGoogleMapLinkUtil.getGoogleMap("Location on Google map", event.getCoordinates()) + "\n";

            result.append(line);
        }
        return result.toString();
    }
}
