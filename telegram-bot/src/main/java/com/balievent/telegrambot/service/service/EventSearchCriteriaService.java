package com.balievent.telegrambot.service.service;

import com.balievent.telegrambot.constant.TelegramButton;
import com.balievent.telegrambot.exceptions.ErrorCode;
import com.balievent.telegrambot.exceptions.ServiceException;
import com.balievent.telegrambot.model.entity.EventSearchCriteria;
import com.balievent.telegrambot.repository.EventSearchCriteriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventSearchCriteriaService {
    private final EventSearchCriteriaRepository eventSearchCriteriaRepository;

    @Transactional
    public void updateSearchCriteria(final Long chatId,
                                     final String searchCriteria) {
        final EventSearchCriteria eventSearchCriteria = eventSearchCriteriaRepository.findByChatId(chatId)
            .orElseThrow(() -> new ServiceException(ErrorCode.ERR_CODE_999));

        if (searchCriteria.contains("search") && searchCriteria.contains("events")) {
            // сохраняем запрос из первого окна (по какому из шести полей кликнул пользователь)
            eventSearchCriteria.setSearchEvents(searchCriteria);
        }
    }

    public String getSearchEvents(final Long chatId) {
        final EventSearchCriteria eventSearchCriteria = eventSearchCriteriaRepository.findByChatId(chatId)
            .orElseThrow(() -> new ServiceException(ErrorCode.ERR_CODE_999));
        return eventSearchCriteria.getSearchEvents();
    }

    @Transactional
    public EventSearchCriteria toggleLocationName(final Long chatId,
                                                  final String locationName) {
        final EventSearchCriteria eventSearchCriteria = eventSearchCriteriaRepository.findByChatId(chatId)
            .orElseThrow(() -> new ServiceException(ErrorCode.ERR_CODE_999));

        eventSearchCriteria.toggleLocationName(locationName); // сохраняем список локаций
        return eventSearchCriteria;
    }

    @Transactional
    public EventSearchCriteria selectAll(final Long chatId,
                                         final List<String> locationNameList) {
        final EventSearchCriteria eventSearchCriteria = eventSearchCriteriaRepository.findByChatId(chatId)
            .orElseThrow(() -> new ServiceException(ErrorCode.ERR_CODE_999));

        final List<String> list = new ArrayList<>(locationNameList);
        list.add(TelegramButton.DESELECT_ALL_LOCATIONS.getCallbackData());

        eventSearchCriteria.setLocationNameList(list);

        return eventSearchCriteria;
    }

    @Transactional
    public EventSearchCriteria deselectAll(final Long chatId) {
        final EventSearchCriteria eventSearchCriteria = eventSearchCriteriaRepository.findByChatId(chatId)
            .orElseThrow(() -> new ServiceException(ErrorCode.ERR_CODE_999));

        eventSearchCriteria.getLocationNameList().clear();
        eventSearchCriteria.getLocationNameList().add(TelegramButton.SELECT_ALL_LOCATIONS.getCallbackData());
        return eventSearchCriteria;
    }

    @Transactional
    public EventSearchCriteria saveOrUpdateEventSearchCriteria(final Long chatId, final List<String> locationNameList) {
        final Optional<EventSearchCriteria> userDataOptional = eventSearchCriteriaRepository.findByChatId(chatId);
        if (userDataOptional.isPresent()) {
            final EventSearchCriteria userData = userDataOptional.get();
            userData.setLocationNameList(locationNameList); // сохраняем все локации и кнопки в event_search_criteria.location_name_list
            return userData;
        }
        return eventSearchCriteriaRepository.save(EventSearchCriteria.builder()
            .chatId(chatId)
            .build());
    }

    public EventSearchCriteria getEventSearchCriteria(final Long chatId) {
        return eventSearchCriteriaRepository.findByChatId(chatId)
            .orElseThrow(() -> new ServiceException(ErrorCode.ERR_CODE_999));
    }
}
