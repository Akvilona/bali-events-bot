package com.balievent.telegrambot.service.service;

import com.balievent.telegrambot.exceptions.ErrorCode;
import com.balievent.telegrambot.exceptions.ServiceException;
import com.balievent.telegrambot.model.entity.UserData;
import com.balievent.telegrambot.repository.UserDataRepository;
import com.balievent.telegrambot.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UserDataService {
    private final UserDataRepository userDataRepository;

    private static UserData getDefaultUserData(final Long chatId) {
        return UserData.builder()
            .id(chatId)
            .searchEventDate(LocalDate.now())
            .currentEventPage(1)
            .totalEventPages(1)
            .build();
    }

    /**
     * для пользователя обновляем хранилище добавляя дату или измененный календарный месяц.
     *
     * @param update         - the update event from Telegram
     * @param isMonthChanged - a flag indicating whether the calendar month has changed
     * @return the updated local date
     */
    @Transactional
    public UserData updateCalendarDate(final Update update, final boolean isMonthChanged) {
        final Long chatId = update.getMessage().getChatId();
        final String text = update.getMessage().getText();

        final UserData userData = getUserData(chatId);
        // получаем из строки дату или берем данные из старого значения
        final LocalDate localDate = isMonthChanged
                                    ? DateUtil.convertToDateTimeCalendarMonthChanged(text, userData.getSearchEventDate())
                                    : DateUtil.parseSelectedDate(text, userData.getSearchEventDate());

        userData.setSearchEventDate(localDate); // сохраняем в базу дату запроса -> postgres.public.user_data.search_event_date

        return userData;
    }

    public UserData getUserData(final Long chatId) {
        return userDataRepository.findById(chatId)
            .orElseThrow(() -> new ServiceException(ErrorCode.ERR_CODE_001));
    }

    @Transactional
    public UserData saveOrUpdateUserData(final Long chatId) {
        final Optional<UserData> userDataOptional = userDataRepository.findById(chatId);
        if (userDataOptional.isPresent()) {
            final UserData userData = userDataOptional.get();
            userData.setSearchEventDate(LocalDate.now());
            userData.setCurrentEventPage(1);
            userData.setTotalEventPages(1);
            return userData;
        }
        return userDataRepository.save(getDefaultUserData(chatId));
    }

    @Transactional
    public UserData incrementCurrentPage(final Long chatId) {
        final UserData userData = getUserData(chatId);
        userData.setCurrentEventPage(userData.getCurrentEventPage() + 1);
        return userData;
    }

    @Transactional
    public UserData decrementCurrentPage(final Long chatId) {
        final UserData userData = userDataRepository.findById(chatId)
            .orElseThrow(() -> new ServiceException(ErrorCode.ERR_CODE_001));
        userData.setCurrentEventPage(userData.getCurrentEventPage() - 1);
        return userData;
    }

    @Transactional
    public UserData addMonthAndGetUserData(final Long chatId) {
        final UserData userData = userDataRepository.findById(chatId)
            .orElseThrow(() -> new ServiceException(ErrorCode.ERR_CODE_001));
        userData.setSearchEventDate(userData.getSearchEventDate().plusMonths(1));
        return userData;
    }

    @Transactional
    public UserData substractMonthAndGetUserData(final Long chatId) {
        final UserData userData = userDataRepository.findById(chatId)
            .orElseThrow(() -> new ServiceException(ErrorCode.ERR_CODE_001));
        userData.setSearchEventDate(userData.getSearchEventDate().minusMonths(1));
        return userData;
    }

    @Transactional
    public UserData updateCurrentPage(final Long chatId, final int page) {
        final UserData userData = getUserData(chatId);
        userData.setCurrentEventPage(page);
        return userData;
    }

    @Transactional
    public void updatePageInfo(final Long chatId, final int pageMax, final int currentPage) {
        final UserData userData = getUserData(chatId);
        userData.setCurrentEventPage(currentPage);
        userData.setTotalEventPages(pageMax);
    }

    @Transactional
    public void updateMediaIdList(final List<Message> mediaIds, final Long chatId) {
        final List<Integer> idList = mediaIds.stream()
            .map(Message::getMessageId)
            .toList();

        final UserData userData = getUserData(chatId); // получаем запись пользователя
        userData.setMediaMessageIdList(idList); // сохраняем идентификаторы картинок в postgres.public.user_data.media_message_id_list
    }

    @Transactional
    public void updateLastBotMessageId(final Integer messageId, final Long chatId) {
        final UserData userData = getUserData(chatId);
        userData.setLastBotMessageId(messageId);
    }

    @Transactional
    public void saveUserMessageId(final Integer messageId, final Long chatId) {
        final UserData userData = getUserData(chatId);
        userData.setLastUserMessageId(messageId);
    }

    public List<Integer> getAllMessageIdsForDelete(final UserData userData) {
        return Stream.of(
                Optional.ofNullable(userData.getLastUserMessageId()).stream(),
                Optional.ofNullable(userData.getLastBotMessageId()).stream(),
                userData.getMediaMessageIdList().stream()
            )
            .flatMap(i -> i)
            .filter(Objects::nonNull)
            .toList();
    }

    @Transactional
    public void saveOrUpdateLocationMap(final Map<String, Long> locationMap, final Long chatId) {
        final UserData userData = getUserData(chatId);
        userData.setLocationMap(locationMap);
    }

    public Map<String, Long> getLocationMap(final Long chatId) {
        final UserData userData = getUserData(chatId);
        return userData != null ? userData.getLocationMap() : new HashMap<>();
    }
}
