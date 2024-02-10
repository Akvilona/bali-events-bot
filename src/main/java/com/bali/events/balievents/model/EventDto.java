package com.bali.events.balievents.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDto {
    private String externalId;
    private String eventName;
    private String locationName;
    private String locationAddress;
    private String startDate;
    private String endDate;
    private String eventUrl;
    private String imageUrl;
    private String serviceName;
}
