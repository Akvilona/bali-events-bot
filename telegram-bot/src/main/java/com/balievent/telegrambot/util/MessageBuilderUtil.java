package com.balievent.telegrambot.util;

import com.balievent.telegrambot.constant.Settings;
import com.balievent.telegrambot.model.entity.Event;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@UtilityClass
public class MessageBuilderUtil {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd_MM_yyyy");

    public static String buildBriefEventsMessage(final int currentPage, final List<Event> eventList) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < eventList.size(); i++) {
            final Event event = eventList.get(i);
            stringBuilder.append(1 + i + Settings.PAGE_SIZE * (currentPage - 1)).append(". ")
                .append(CommonUtil.getLink(event.getEventName(), event.getEventUrl()))
                .append("%n");
        }

        return stringBuilder.toString();
    }

    public static String formatMessageForEventsGroupedByDay(final Map<LocalDate, List<Event>> eventMap) {
        final Map<LocalDate, List<Event>> reverseSortedMap = new TreeMap<>(eventMap);

        final StringBuilder stringBuilder = new StringBuilder();
        reverseSortedMap.forEach((key, value) ->
            stringBuilder.append("/").append(key.format(DATE_TIME_FORMATTER))
                .append(" : ")
                .append(value.size())
                .append(" events")
                .append("%n"));

        if (stringBuilder.isEmpty()) {
            stringBuilder.append("No events");
        }

        return stringBuilder.toString();
    }
}
