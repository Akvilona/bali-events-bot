package com.balievent.telegrambot.service.handler.common;

import com.balievent.telegrambot.constant.Settings;
import com.balievent.telegrambot.model.entity.UserData;
import com.balievent.telegrambot.service.MyTelegramBot;
import com.balievent.telegrambot.service.service.EventService;
import com.balievent.telegrambot.service.service.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaHandler {
    private final MyTelegramBot myTelegramBot;
    private final EventService eventService;
    private final UserDataService userDataService;

    public void handle(final Long chatId, final UserData userData) {
        try {
            final List<InputMediaPhoto> eventPhotos = findEventPhotos(userData); // получаем список ссылок фотографий на текущую дату
            if (eventPhotos.isEmpty()) {
                log.info("No event photos found for chatId: {}", chatId);
                return;
            }
            // в зависимости от количества фотографий отсылаем блок фотографий пользователю
            final List<Message> messageList = eventPhotos.size() == 1
                                              ? sendSinglePhoto(chatId, eventPhotos)
                                              : sendMultiplePhotos(chatId, eventPhotos);
            // сохраняем список идентификаторов фотографий
            userDataService.updateMediaIdList(messageList, chatId);
        } catch (TelegramApiException e) {
            log.error("Failed to send media", e);
        }
    }

    private SendMediaGroup handleMultipleMedia(final Long chatId, final List<InputMediaPhoto> eventPhotos) {
        return SendMediaGroup.builder()
            .chatId(chatId)
            .medias(new ArrayList<>(eventPhotos))
            .build();
    }

    private SendPhoto handleSingleMedia(final Long chatId, final List<InputMediaPhoto> eventPhotos) {
        return SendPhoto.builder()
            .chatId(chatId)
            .photo(new InputFile(eventPhotos.getFirst().getMedia()))
            .build();
    }

    private List<InputMediaPhoto> findEventPhotos(final UserData userData) {
        final int currentPageIndex = userData.getCurrentEventPage() - 1; // текущая страница
        // ищем записи на текущую дату для текущей страницы, и указанным количеством на этой странице
        return eventService.findEvents(userData.getSearchEventDate(), currentPageIndex, Settings.PAGE_SIZE)
            .stream()
            .map(event -> {
                final InputMediaPhoto inputMediaPhoto = new InputMediaPhoto(); // создаем контейнер TELEGRAM фотографий
                inputMediaPhoto.setMedia(event.getImageUrl()); // берем ссылку на фото из postgres.public.event.event_url
                return inputMediaPhoto;
            })
            .toList(); // добавляем в список
    }

    private List<Message> sendSinglePhoto(final Long chatId,
                                          final List<InputMediaPhoto> eventPhotos) throws TelegramApiException {
        final Message message = myTelegramBot.execute(handleSingleMedia(chatId, eventPhotos));
        return List.of(message);
    }

    private List<Message> sendMultiplePhotos(final Long chatId,
                                             final List<InputMediaPhoto> eventPhotos) throws TelegramApiException {
        final SendMediaGroup sendMediaGroup = handleMultipleMedia(chatId, eventPhotos);
        return myTelegramBot.execute(sendMediaGroup);
    }

    public void location(final Long chatId, final UserData userData, final String textMessage) {
        final Map<String, Long> locationMap = userData.getLocationMap();
        final Long value = locationMap.get(textMessage);
        try {
            final List<InputMediaPhoto> eventPhotos = getEventsById(value); // получаем список из одной фотографии
            if (eventPhotos.isEmpty()) {
                log.info("No event photos found for chatId: {}", chatId);
                return;
            }
            // в зависимости от количества фотографий отсылаем блок фотографий пользователю
            final List<Message> messageList = eventPhotos.size() == 1
                                              ? sendSinglePhoto(chatId, eventPhotos)
                                              : sendMultiplePhotos(chatId, eventPhotos);
            // сохраняем список идентификаторов фотографий
            userDataService.updateMediaIdList(messageList, chatId);
        } catch (TelegramApiException e) {
            log.error("Failed to send media", e);
        }
    }

    private List<InputMediaPhoto> getEventsById(final Long userEventsId) {
        // ищем запись по идентификатору
        return eventService.findEventsById(userEventsId)
            .stream()
            .map(event -> {
                final InputMediaPhoto inputMediaPhoto = new InputMediaPhoto(); // создаем контейнер TELEGRAM фотографий
                inputMediaPhoto.setMedia(event.getImageUrl()); // берем ссылку на фото из postgres.public.event.event_url
                return inputMediaPhoto;
            })
            .toList(); // добавляем в список
    }
}
