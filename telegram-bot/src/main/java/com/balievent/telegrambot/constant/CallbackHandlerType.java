package com.balievent.telegrambot.constant;

public enum CallbackHandlerType {
    EVENT_DATE_SELECTION,       // -> public class EventDateQuestionHandler()
    EVENT_LOCATIONS_SELECTION,  // -> public class EventLocationsQuestionHandler()
    MONTH_PAGINATION,           // -> public class MonthPaginationHandler()
    EVENTS_PAGINATION,          // -> public class EventsPaginationHandler()
    SHOW_MORE_OR_LESS_EVENTS,   // -> public class ShowMoreAndLessHandler()
    MONTH_EVENTS_PAGE,          // -> public class MonthEventsHandler()
    DAY_EVENT_PAGE,             // -> public class DayEventsHandler()
    EVENT_START_FILTER          // -> public class StartFilterHandler()

}
