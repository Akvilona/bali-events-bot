package com.balievent.telegrambot.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@Getter
@Setter
@ToString

@Entity
@Table(name = "event_search_criteria")
public class EventSearchCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", unique = true, nullable = false)
    private Long chatId;

    @Column(name = "event_location_name_list_filter")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private List<String> eventLocationNameListFiler = new ArrayList<>();

    @Column(name = "event_date_filter")
    private String eventDateFilter;

    public void toggleLocationName(final String locationName) {

        if (eventLocationNameListFiler.contains(locationName)) {
            eventLocationNameListFiler.remove(locationName);
        } else {
            eventLocationNameListFiler.add(locationName);
        }
    }
}
