package com.balievent.telegrambot.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(chain = true)
@Getter
@Setter
@ToString

@Entity
@Table(name = "user_data")
public class UserData {
    @Id
    private Long id;

    @Column(name = "search_event_date")
    private LocalDate searchEventDate;

    @Column(name = "current_event_page")
    private Integer currentEventPage;

    @Column(name = "total_event_pages")
    private Integer totalEventPages;

    @Column(name = "last_bot_message_id")
    private Integer lastBotMessageId;

    @Column(name = "last_user_message_id")
    private Integer lastUserMessageId;

    @Column(name = "location_message_id")
    private Integer locationMessageId;

    @Column(name = "media_message_id_list")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private List<Integer> mediaMessageIdList = new ArrayList<>();

    @Column(name = "location_map")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private Map<String, Long> locationMap = new HashMap<>();
}
