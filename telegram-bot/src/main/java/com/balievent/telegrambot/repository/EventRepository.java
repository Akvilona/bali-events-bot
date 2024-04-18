package com.balievent.telegrambot.repository;

import com.balievent.telegrambot.model.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    Event findEventsById(Long id);

    List<Event> findEventsByStartDateBetween(LocalDateTime start, LocalDateTime end);

    Page<Event> findEventsByStartDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    int countEventsByStartDateBetween(LocalDateTime start, LocalDateTime end);

}
