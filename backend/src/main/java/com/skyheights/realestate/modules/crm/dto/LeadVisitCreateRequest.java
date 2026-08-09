package com.skyheights.realestate.modules.crm.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadVisitCreateRequest {

    @NotNull(message = "Lead ID required")
    private Long leadId;

    @NotNull(message = "Property ID required")
    private Long propertyId;

    private Long unitId;

    @NotNull(message = "Scheduled at required")
    @Future(message = "Scheduled time must be in future")
    private Instant scheduledAt;

    private String notes;

    private Long staffId;
}
