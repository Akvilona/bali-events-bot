package com.balievent.telegrambot.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum TelegramButton {
    LETS_GO("month_events_page", "Let's go! 🚀", CallbackHandlerType.MONTH_EVENTS_PAGE),

    SHOW_MONTH_FULL("show_month_full", "Show more ➕", CallbackHandlerType.SHOW_MORE_OR_LESS_EVENTS),
    SHOW_MONTH_LESS("show_month_less", "Show less ➖", CallbackHandlerType.SHOW_MORE_OR_LESS_EVENTS),

    //Events page buttons
    FIRST_EVENTS_PAGE("first_events_page", "<< [1/%s]", CallbackHandlerType.EVENTS_PAGINATION),
    PREVIOUS_EVENTS_PAGE("previous_events_page", "< [%s/%s]", CallbackHandlerType.EVENTS_PAGINATION),
    NEXT_EVENTS_PAGE("next_events_page", "> [%s/%s]", CallbackHandlerType.EVENTS_PAGINATION),
    LAST_EVENTS_PAGE("last_events_page", ">> [%s/%s]", CallbackHandlerType.EVENTS_PAGINATION),

    //Month page buttons
    DAY_EVENT_PAGE("month_events_day", "Back to the list for the day 📅", CallbackHandlerType.DAY_EVENT_PAGE),
    MONTH_EVENTS_PAGE("month_events_page", "Back to the list for the month 📅", CallbackHandlerType.MONTH_EVENTS_PAGE),

    PREVIOUS_MONTH_PAGE("previous_month_page", "%s", CallbackHandlerType.MONTH_PAGINATION),
    NEXT_MONTH_PAGE("next_month_page", "%s", CallbackHandlerType.MONTH_PAGINATION),

    //choose event date buttons
    SEARCH_TODAY_EVENTS("search_today_events", "Today", CallbackHandlerType.EVENT_DATE_SELECTION),
    SEARCH_TOMORROW_EVENTS("search_tomorrow_events", "Tomorrow", CallbackHandlerType.EVENT_DATE_SELECTION),
    SEARCH_THIS_WEEK_EVENTS("search_this_week_events", "This week", CallbackHandlerType.EVENT_DATE_SELECTION),
    SEARCH_NEXT_WEEK_EVENTS("search_next_week_events", "Next week", CallbackHandlerType.EVENT_DATE_SELECTION),
    SEARCH_ON_THIS_WEEKEND_EVENTS("search_on_this_weekend_events", "On this weekend", CallbackHandlerType.EVENT_DATE_SELECTION),
    SEARCH_SHOW_ALL_EVENTS("search_show_all_events", "Show all this month", CallbackHandlerType.EVENT_DATE_SELECTION),
    //    SEARCH_PICK_DATE_EVENTS("search_pick_date_events", "Pick date", CallbackHandlerType.EVENT_DATE_SELECTION);

    EVENT_LOCATIONS_NEXT("month_events_page", "Next ➡️", CallbackHandlerType.MONTH_EVENTS_PAGE),
    EVENT_START_FILTER("event_start_filter", "Filter", CallbackHandlerType.EVENT_START_FILTER),

    SELECT_ALL_LOCATIONS("select_all_locations", "Select all", CallbackHandlerType.EVENT_LOCATIONS_SELECTION),
    DESELECT_ALL_LOCATIONS("deselect_all_locations", "Deselect all", CallbackHandlerType.EVENT_LOCATIONS_SELECTION);

    private final String callbackData;
    private final String buttonText;
    private final CallbackHandlerType callbackHandlerType;

}
